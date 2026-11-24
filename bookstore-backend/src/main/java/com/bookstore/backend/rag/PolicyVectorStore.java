package com.bookstore.backend.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 【作业3 · 向量库】PolicyVectorStore —— 内存向量存储与相似度检索。
 *
 * 政策文档体量很小（十几到几十个 Chunk），使用内存向量库即可：
 *  - 写入：启动时一次性写入所有 Chunk 及其向量；
 *  - 检索：对查询向量与每个 Chunk 向量计算余弦相似度，再叠加关键词覆盖率，排序取 Top-K。
 *
 * 之所以在向量相似度之外再叠加关键词覆盖率，是因为短查询（如"塑封"）在哈希向量中
 * 可能被长文档稀释，而精确词命中恰恰是政策问答最看重的信号。
 */
@Component
public class PolicyVectorStore {

    private static final double VECTOR_WEIGHT = 0.72;
    private static final double KEYWORD_WEIGHT = 0.28;
    private static final int VECTOR_PREVIEW_SIZE = 6;

    private final LocalTextEmbedding embedding;
    private final List<PolicyChunk> chunks = new CopyOnWriteArrayList<>();

    public PolicyVectorStore(LocalTextEmbedding embedding) {
        this.embedding = embedding;
    }

    public void addAll(List<PolicyChunk> newChunks) {
        chunks.addAll(newChunks);
    }

    public int size() {
        return chunks.size();
    }

    public List<PolicyChunk> getChunks() {
        return List.copyOf(chunks);
    }

    /**
     * 相似度检索。
     *
     * @param queryVector 查询向量
     * @param queryTokens 查询 token 权重表（用于关键词覆盖率）
     * @param idf         语料库 IDF 表
     * @param topK        返回条数
     * @param minScore    最低得分阈值，低于该值的片段不返回
     */
    // 检索：对每一个块算「0.72 × 余弦相似度 + 0.28 × 关键词覆盖率」，丢掉低于阈值的，
    // 按得分从高到低排序，取前 K 条返回。
    public List<PolicySearchResult> search(double[] queryVector, Map<String, Double> queryTokens,
                                           Map<String, Double> idf, int topK, double minScore) {
        if (queryVector == null || chunks.isEmpty()) {
            return List.of();
        }
        int limit = Math.max(1, Math.min(topK, 10));

        List<ScoredChunk> scored = new ArrayList<>();
        for (PolicyChunk chunk : chunks) {
            double vectorScore = embedding.cosine(queryVector, chunk.getVector());
            double keywordScore = keywordCoverage(queryTokens, idf, chunk.getSearchableText().toLowerCase());
            double score = VECTOR_WEIGHT * vectorScore + KEYWORD_WEIGHT * keywordScore;
            if (score >= minScore) {
                scored.add(new ScoredChunk(chunk, score, vectorScore, keywordScore));
            }
        }

        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());

        List<PolicySearchResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scored.size()); i++) {
            ScoredChunk sc = scored.get(i);
            PolicySearchResult result = new PolicySearchResult();
            result.setChunkId(sc.chunk.getId());
            result.setSection(sc.chunk.getSection());
            result.setScore(round(sc.score));
            result.setVectorScore(round(sc.vectorScore));
            result.setKeywordScore(round(sc.keywordScore));
            result.setContent(sc.chunk.getText());
            result.setVectorDim(sc.chunk.getVector().length);
            result.setVectorPreview(preview(sc.chunk.getVector()));
            results.add(result);
        }
        return results;
    }

    /** 查询词在片段中的加权覆盖率：命中词权重之和 / 全部查询词权重之和 */
    private double keywordCoverage(Map<String, Double> queryTokens, Map<String, Double> idf, String text) {
        double total = 0.0;
        double matched = 0.0;
        for (Map.Entry<String, Double> entry : queryTokens.entrySet()) {
            double weight = entry.getValue() * idf.getOrDefault(entry.getKey(), 1.0);
            total += weight;
            if (text.contains(entry.getKey())) {
                matched += weight;
            }
        }
        return total == 0.0 ? 0.0 : matched / total;
    }

    private List<Double> preview(double[] vector) {
        List<Double> preview = new ArrayList<>();
        for (int i = 0; i < Math.min(VECTOR_PREVIEW_SIZE, vector.length); i++) {
            preview.add(round(vector[i]));
        }
        return preview;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private static class ScoredChunk {
        private final PolicyChunk chunk;
        private final double score;
        private final double vectorScore;
        private final double keywordScore;

        private ScoredChunk(PolicyChunk chunk, double score, double vectorScore, double keywordScore) {
            this.chunk = chunk;
            this.score = score;
            this.vectorScore = vectorScore;
            this.keywordScore = keywordScore;
        }

        private double score() {
            return score;
        }
    }
}
