package com.bookstore.backend.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 【作业3 · Embedding 层】LocalTextEmbedding —— 轻量本地文本向量化器。
 *
 * 为什么不用在线 Embedding API？
 *  - DeepSeek 目前只提供 Chat 接口，不提供 Embedding 接口；
 *  - 作业要求的是"简易 RAG 流程"，本实现用经典的 Feature Hashing（特征哈希）把文本映射为定长稠密向量，
 *    不依赖任何外部服务，离线可复现，启动时即可完成全部政策文档的向量化。
 *
 * 算法（针对中文优化）：
 *  1. 分词：英文/数字按单词切分；中文按"单字 + 相邻二元组（bigram）"切分，
 *     例如"退换货" -> 退 / 换 / 货 / 退换 / 换货，bigram 更能表达短语语义；
 *  2. 词频加权：同一个词在文本中出现多次时累加权重，单字权重 0.5、bigram/英文词权重 1.0；
 *  3. TF-IDF：乘以语料库 IDF（由 StorePolicyRagService 统计），压低"的、了、我"等高频噪声；
 *  4. 特征哈希：token.hashCode() 映射到固定维度（默认 1024 维），并用符号哈希减少碰撞偏差；
 *  5. L2 归一化：归一化后两个向量的点积即为余弦相似度。
 */
@Component
public class LocalTextEmbedding {

    private static final Pattern ASCII_WORD = Pattern.compile("[a-z0-9]+");
    private static final Pattern CJK_RUN = Pattern.compile("[\\u4e00-\\u9fa5]+");

    private final int dimension;

    public LocalTextEmbedding(@Value("${policy.rag.dimension:1024}") int dimension) {
        this.dimension = Math.max(128, dimension);
    }

    public int getDimension() {
        return dimension;
    }

    /**
     * 文本 -> token 权重表（未做 IDF）。key 为 token，value 为该 token 的累计权重。
     */
    public Map<String, Double> tokenize(String text) {
        Map<String, Double> tokens = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        String lower = text.toLowerCase(Locale.ROOT);

        Matcher ascii = ASCII_WORD.matcher(lower);
        while (ascii.find()) {
            merge(tokens, ascii.group(), 1.0);
        }

        Matcher cjk = CJK_RUN.matcher(lower);
        while (cjk.find()) {
            String run = cjk.group();
            for (int i = 0; i < run.length(); i++) {
                merge(tokens, String.valueOf(run.charAt(i)), 0.5);
                if (i + 1 < run.length()) {
                    merge(tokens, run.substring(i, i + 2), 1.0);
                }
            }
        }
        return tokens;
    }

    /**
     * 将 token 权重表编码为稠密向量。
     *
     * @param tokens token 权重表
     * @param idf    语料库 IDF 表；查询阶段传入同一份 IDF，保证查询向量与文档向量在同一空间
     */
    // 把文本变成向量：每个词的词频乘上 IDF 作为权重，用哈希把词映射到 1024 维里的固定位置，
    // 最后做 L2 归一化，归一化之后两个向量的点积就是余弦相似度。
    public double[] embed(Map<String, Double> tokens, Map<String, Double> idf) {
        double[] vector = new double[dimension];
        for (Map.Entry<String, Double> entry : tokens.entrySet()) {
            String token = entry.getKey();
            double weight = entry.getValue() * idf.getOrDefault(token, 1.0);
            if (weight == 0.0) {
                continue;
            }
            int hash = token.hashCode();
            int index = Math.floorMod(hash, dimension);
            // 符号哈希：同一个 token 永远落到相同的位置和符号，降低哈希碰撞带来的系统性偏差
            double sign = ((hash >>> 16) & 1) == 0 ? 1.0 : -1.0;
            vector[index] += sign * weight;
        }
        normalize(vector);
        return vector;
    }

    /** 便捷方法：直接对文本做向量化（不带 IDF） */
    public double[] embed(String text) {
        return embed(tokenize(text), Map.of());
    }

    /** 两个已归一化向量的余弦相似度（等于点积） */
    public double cosine(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    private void merge(Map<String, Double> tokens, String token, double weight) {
        tokens.merge(token, weight, Double::sum);
    }

    private void normalize(double[] vector) {
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }
}
