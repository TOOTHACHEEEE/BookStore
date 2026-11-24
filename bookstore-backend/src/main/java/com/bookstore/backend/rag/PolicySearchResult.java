package com.bookstore.backend.rag;

import java.util.List;

/**
 * 【作业3 · RAG 检索结果】返回给 query_store_policy 工具与前端调试面板。
 *
 * score        最终得分（向量余弦相似度 + 关键词覆盖率的加权）
 * vectorScore  纯向量余弦相似度
 * keywordScore 关键词覆盖率
 * vectorPreview 向量前若干维的取值，用于在界面上直观展示"文本已 Embedding 化"
 */
public class PolicySearchResult {

    private String chunkId;
    private String section;
    private double score;
    private double vectorScore;
    private double keywordScore;
    private String content;
    private int vectorDim;
    private List<Double> vectorPreview;

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getVectorScore() {
        return vectorScore;
    }

    public void setVectorScore(double vectorScore) {
        this.vectorScore = vectorScore;
    }

    public double getKeywordScore() {
        return keywordScore;
    }

    public void setKeywordScore(double keywordScore) {
        this.keywordScore = keywordScore;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getVectorDim() {
        return vectorDim;
    }

    public void setVectorDim(int vectorDim) {
        this.vectorDim = vectorDim;
    }

    public List<Double> getVectorPreview() {
        return vectorPreview;
    }

    public void setVectorPreview(List<Double> vectorPreview) {
        this.vectorPreview = vectorPreview;
    }
}
