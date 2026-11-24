package com.bookstore.backend.rag;

import com.bookstore.backend.rag.PolicyChunkSplitter.RawChunk;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【作业3 · RAG 单元测试】
 * 验证：政策文档能被正确加载、切片、向量化，并且常见政策问题的 Top-1 命中正确章节。
 * 该测试不启动 Spring 容器，也不依赖 MySQL，可独立运行：
 *   mvnw -Dtest=PolicyRagSearchTest test
 */
class PolicyRagSearchTest {

    @Test
    void shouldChunkEmbedAndRetrieveRelevantPolicyClauses() throws Exception {
        LocalTextEmbedding embedding = new LocalTextEmbedding(1024);
        PolicyChunkSplitter splitter = new PolicyChunkSplitter();
        PolicyVectorStore store = new PolicyVectorStore(embedding);

        String markdown;
        try (InputStream input = getClass().getResourceAsStream("/policy/store-policy.md")) {
            assertNotNull(input, "policy document should exist on classpath");
            markdown = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<RawChunk> rawChunks = splitter.split(markdown, 260, 60);
        assertTrue(rawChunks.size() >= 10, "policy document should be split into at least 10 chunks");

        // 统计 IDF
        Map<String, Integer> documentFrequency = new HashMap<>();
        List<Map<String, Double>> tokenMaps = new ArrayList<>();
        for (RawChunk raw : rawChunks) {
            Map<String, Double> tokens = embedding.tokenize(raw.getSection() + "\n" + raw.getText());
            tokenMaps.add(tokens);
            for (String token : tokens.keySet()) {
                documentFrequency.merge(token, 1, Integer::sum);
            }
        }
        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            idf.put(entry.getKey(), Math.log((rawChunks.size() + 1.0) / (entry.getValue() + 1.0)) + 1.0);
        }

        for (int i = 0; i < rawChunks.size(); i++) {
            RawChunk raw = rawChunks.get(i);
            store.addAll(List.of(new PolicyChunk(raw.getId(), raw.getSection(), i + 1, raw.getText(),
                    embedding.embed(tokenMaps.get(i), idf))));
        }

        System.out.println("========== Policy RAG index ==========");
        System.out.println("chunks=" + rawChunks.size() + ", dimension=" + embedding.getDimension()
                + ", vocabulary=" + idf.size());
        for (RawChunk raw : rawChunks) {
            System.out.printf("  %s | %-24s | %d chars%n",
                    raw.getId(), raw.getSection(), raw.getText().length());
        }

        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("拆了塑封还能退吗", "塑封");
        cases.put("金卡会员有什么折扣", "会员");
        cases.put("退款多久能到账", "退款");
        cases.put("书缺页了怎么办", "质量");
        cases.put("七天无理由退货", "无理由");
        cases.put("优惠券退货后会退吗", "优惠券");
        cases.put("积分怎么抵扣", "积分");
        cases.put("换货运费谁承担", "运费");

        System.out.println("========== Retrieval results（只用原始提问，不启用大模型改写）==========");
        for (Map.Entry<String, String> testCase : cases.entrySet()) {
            String query = testCase.getKey();
            Map<String, Double> queryTokens = embedding.tokenize(query);
            double[] queryVector = embedding.embed(queryTokens, idf);
            List<PolicySearchResult> results = store.search(queryVector, queryTokens, idf, 3, 0.05);

            assertFalse(results.isEmpty(), "should retrieve clauses for: " + query);
            PolicySearchResult top = results.get(0);
            System.out.printf("%nQUERY: %s%n", query);
            for (PolicySearchResult result : results) {
                System.out.printf("  score=%.4f (vector=%.4f, keyword=%.4f) | %s%n",
                        result.getScore(), result.getVectorScore(), result.getKeywordScore(), result.getSection());
            }
            assertTrue(top.getSection().contains(testCase.getValue())
                            || top.getContent().contains(testCase.getValue()),
                    "top-1 clause should contain '" + testCase.getValue() + "' for query: " + query
                            + ", but was: " + top.getSection());
        }
    }
}
