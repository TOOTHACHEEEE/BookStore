package com.bookstore.backend.rag;

/**
 * 【作业3 · RAG 数据模型】一个政策知识片段（Chunk）。
 *
 * 每个 Chunk 保留：
 *  - 所属章节标题 section：便于回答时引用条款出处；
 *  - 片段正文 text：真正回传给大模型的上下文；
 *  - 向量 vector：由 LocalTextEmbedding 生成的稠密向量，用于余弦相似度检索。
 */
public class PolicyChunk {

    private final String id;
    private final String section;
    private final int index;
    private final String text;
    private final double[] vector;

    public PolicyChunk(String id, String section, int index, String text, double[] vector) {
        this.id = id;
        this.section = section;
        this.index = index;
        this.text = text;
        this.vector = vector;
    }

    public String getId() {
        return id;
    }

    public String getSection() {
        return section;
    }

    public int getIndex() {
        return index;
    }

    public String getText() {
        return text;
    }

    public double[] getVector() {
        return vector;
    }

    /** 参与检索的文本：章节标题 + 正文，标题中的关键词同样可以被命中 */
    public String getSearchableText() {
        return section + "\n" + text;
    }
}
