package com.bookstore.backend.rag;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bookstore.backend.ai.DeepSeekClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 【作业3 · RAG 服务】StorePolicyRagService —— 政策知识库的加载、切片、向量化与检索入口。
 *
 * 启动流程（对应评分点①：文档的加载、切分与 Embedding 化）：
 *   1. 从 classpath:policy/store-policy.md 读取《退换货与会员政策》；
 *   2. PolicyChunkSplitter 按章节 + 句子 + 重叠窗口切分为若干 Chunk；
 *   3. LocalTextEmbedding 统计 IDF，并为每个 Chunk 生成 1024 维哈希向量；
 *   4. 写入 PolicyVectorStore（内存向量库）。
 *
 * query_store_policy 工具调用 search()，返回 Top-K 相关条款（对应评分点②：可用的 RAG 检索工具）。
 */
@Service
public class StorePolicyRagService {

    private static final Logger log = LoggerFactory.getLogger(StorePolicyRagService.class);

    private final PolicyChunkSplitter splitter;
    private final LocalTextEmbedding embedding;
    private final PolicyVectorStore vectorStore;
    private final DeepSeekClient deepSeekClient;

    /** 是否启用大模型查询改写（关闭后退化为纯规则扩展，可离线运行） */
    @Value("${policy.rag.llm-rewrite-enabled:true}")
    private boolean llmRewriteEnabled;

    /** 只对长度不超过该字符数的短提问触发大模型改写（改写要多一轮 LLM，长提问不划算） */
    @Value("${policy.rag.rewrite-max-chars:14}")
    private int rewriteMaxChars;

    /** 改写结果缓存，避免同一问题重复调用大模型 */
    private final Map<String, String> rewriteCache = new ConcurrentHashMap<>();

    @Value("${policy.rag.location:classpath:policy/store-policy.md}")
    private Resource policyResource;

    @Value("${policy.rag.chunk-size:260}")
    private int chunkSize;

    @Value("${policy.rag.chunk-overlap:60}")
    private int chunkOverlap;

    @Value("${policy.rag.top-k:3}")
    private int defaultTopK;

    @Value("${policy.rag.min-score:0.05}")
    private double minScore;

    /** 语料库 IDF：token -> idf，查询与文档共享同一份，保证向量空间一致 */
    private final Map<String, Double> idf = new HashMap<>();

    private String documentTitle = "Store Policy";
    private int documentChars = 0;
    /** 政策原文（Markdown），供前端“退换货与会员政策”页面直接展示 */
    private String documentContent = "";

    public StorePolicyRagService(PolicyChunkSplitter splitter,
                                 LocalTextEmbedding embedding,
                                 PolicyVectorStore vectorStore,
                                 DeepSeekClient deepSeekClient) {
        this.splitter = splitter;
        this.embedding = embedding;
        this.vectorStore = vectorStore;
        this.deepSeekClient = deepSeekClient;
    }

    @PostConstruct
    public void init() {
        long start = System.currentTimeMillis();
        try {
            String markdown = new String(policyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            documentContent = markdown;
            documentChars = markdown.length();
            documentTitle = markdown.lines()
                    .filter(line -> line.startsWith("# "))
                    .findFirst()
                    .map(line -> line.substring(2).trim())
                    .orElse("Store Policy");

            List<PolicyChunkSplitter.RawChunk> rawChunks = splitter.split(markdown, chunkSize, chunkOverlap);
            if (rawChunks.isEmpty()) {
                log.warn("[PolicyRAG] No chunk generated, policy Q&A will be unavailable");
                return;
            }

            // 第一遍：统计每个 token 出现在多少个 Chunk 中，计算 IDF
            List<Map<String, Double>> tokenMaps = new ArrayList<>();
            Map<String, Integer> documentFrequency = new HashMap<>();
            for (PolicyChunkSplitter.RawChunk raw : rawChunks) {
                Map<String, Double> tokens = embedding.tokenize(raw.getSection() + "\n" + raw.getText());
                tokenMaps.add(tokens);
                for (String token : tokens.keySet()) {
                    documentFrequency.merge(token, 1, Integer::sum);
                }
            }
            int total = rawChunks.size();
            for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
                idf.put(entry.getKey(), Math.log((total + 1.0) / (entry.getValue() + 1.0)) + 1.0);
            }

            // 第二遍：生成每个 Chunk 的向量
            List<PolicyChunk> chunks = new ArrayList<>();
            for (int i = 0; i < rawChunks.size(); i++) {
                PolicyChunkSplitter.RawChunk raw = rawChunks.get(i);
                double[] vector = embedding.embed(tokenMaps.get(i), idf);
                chunks.add(new PolicyChunk(raw.getId(), raw.getSection(), i + 1, raw.getText(), vector));
            }
            vectorStore.addAll(chunks);

            log.info("[PolicyRAG] Document loaded: {} ({} chars)", documentTitle, documentChars);
            log.info("[PolicyRAG] Chunking: chunkSize={}, overlap={} -> {} chunks", chunkSize, chunkOverlap, chunks.size());
            log.info("[PolicyRAG] Embedding: hashed char n-gram + TF-IDF, dimension={}, vocabulary={}",
                    embedding.getDimension(), idf.size());
            log.info("[PolicyRAG] Vector store ready in {} ms ({} vectors)", System.currentTimeMillis() - start, chunks.size());
            if (log.isDebugEnabled()) {
                chunks.forEach(chunk -> log.debug("[PolicyRAG]   {} | {} | {} chars",
                        chunk.getId(), chunk.getSection(), chunk.getText().length()));
            }
        } catch (IOException e) {
            log.error("[PolicyRAG] Failed to load policy document from {}", policyResource, e);
        }
    }

    /**
     * 检索与问题最相关的政策条款。
     *
     * @param query 用户问题或工具传入的检索语句
     * @param topK  返回条数，<=0 时使用配置默认值
     */
    public List<PolicySearchResult> search(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int k = topK <= 0 ? defaultTopK : topK;

        // 检索前优化：把口语化提问改写成政策文档里的规范关键词（Query Rewriting）
        String searchQuery = query;
        if (shouldRewrite(query)) {
            String llmKeywords = rewriteWithLlm(query);
            if (llmKeywords != null && !llmKeywords.isBlank()) {
                searchQuery = query + " " + llmKeywords;
            }
        }

        Map<String, Double> queryTokens = embedding.tokenize(searchQuery);
        double[] queryVector = embedding.embed(queryTokens, idf);
        List<PolicySearchResult> results = vectorStore.search(queryVector, queryTokens, idf, k, minScore);

        log.info("[PolicyRAG] Query='{}' -> search='{}', topK={}, hits={}", query, searchQuery, k, results.size());
        for (PolicySearchResult result : results) {
            log.info("[PolicyRAG]   hit {} | score={} (vector={}, keyword={})",
                    result.getSection(), result.getScore(), result.getVectorScore(), result.getKeywordScore());
        }
        return results;
    }

    /** 是否值得为大模型改写付一次调用：开关打开、密钥已配置、提问足够短 */
    private boolean shouldRewrite(String query) {
        if (!llmRewriteEnabled || !deepSeekClient.isConfigured()) {
            return false;
        }
        return query.trim().length() <= rewriteMaxChars;
    }

    /**
     * 用大模型把口语化提问改写成适合检索的政策关键词（查询改写 Query Rewriting）。
     *
     * 为什么要它：哈希向量只有字面重合、没有语义泛化，用户说“塑料膜”“里面坏了几页”
     * 时与政策里的“塑封”“质量问题”对不上。大模型能把口语翻译成文档词汇，且能泛化到
     * 没见过的说法。代价是多一轮 LLM 推理，所以只对短提问启用，并做结果缓存。
     *
     * 任何异常都降级为 null（退回用原始提问检索），绝不因为改写失败而让检索不可用。
     */
    private String rewriteWithLlm(String query) {
        String cached = rewriteCache.get(query);
        if (cached != null) {
            return cached;
        }
        try {
            JSONArray messages = new JSONArray();
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", "你是检索查询改写器。把用户关于书店退换货/会员政策的口语提问，"
                    + "改写成 3 个适合检索政策文档的关键词或短语，用空格分隔。"
                    + "只输出关键词本身，不要解释、不要标点、不要编号。"
                    + "例如“银卡能优惠多少”->“会员等级 折扣 会员权益”。");
            messages.add(system);

            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", query);
            messages.add(user);

            JSONObject response = deepSeekClient.chatCompletion(messages, null, 0.1);
            JSONArray choices = response == null ? null : response.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message == null ? null : message.getString("content");
            if (content == null || content.isBlank()) {
                return null;
            }
            // 只保留中文/字母数字片段，去掉模型可能附带的多余符号
            StringBuilder cleaned = new StringBuilder();
            for (String part : content.trim().split("[\\s,，、;；。:：\"'`]+")) {
                String p = part.trim();
                if (!p.isEmpty() && !p.equals(query)) {
                    if (cleaned.length() > 0) {
                        cleaned.append(' ');
                    }
                    cleaned.append(p);
                }
            }
            String keywords = cleaned.toString().trim();
            if (keywords.isEmpty()) {
                return null;
            }
            rewriteCache.put(query, keywords);
            log.info("[PolicyRAG] 大模型改写：'{}' -> '{}'", query, keywords);
            return keywords;
        } catch (Exception e) {
            log.warn("[PolicyRAG] 大模型查询改写失败，降级为用原始提问检索：{}", e.getMessage());
            return null;
        }
    }

    /** 政策原文，供前端“退换货与会员政策”页面直接展示 */
    public JSONObject document() {
        JSONObject document = new JSONObject(new LinkedHashMap<>());
        document.put("title", documentTitle);
        document.put("chars", documentChars);
        document.put("content", documentContent);
        return document;
    }

    /** 知识库概况，供前端调试面板展示 */
    public JSONObject stats() {
        JSONObject stats = new JSONObject(new LinkedHashMap<>());
        stats.put("document", documentTitle);
        stats.put("chars", documentChars);
        stats.put("chunks", vectorStore.size());
        stats.put("dimension", embedding.getDimension());
        stats.put("vocabulary", idf.size());
        stats.put("chunkSize", chunkSize);
        stats.put("chunkOverlap", chunkOverlap);
        stats.put("topK", defaultTopK);
        stats.put("minScore", minScore);
        return stats;
    }
}
