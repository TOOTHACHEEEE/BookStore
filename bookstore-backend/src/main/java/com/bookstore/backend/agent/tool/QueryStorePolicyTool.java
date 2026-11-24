package com.bookstore.backend.agent.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bookstore.backend.rag.PolicySearchResult;
import com.bookstore.backend.rag.StorePolicyRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 【作业3 · Tool 2】query_store_policy —— 调用 RAG 检索《退换货与会员政策》。
 *
 * 工具的职责只有"检索"，不做回答：
 *  - 输入：自然语言问题（例如"拆了塑封还能退吗"）；
 *  - 处理：StorePolicyRagService 对问题做向量化，在政策向量库中检索 Top-K 条款；
 *  - 输出：条款原文 + 所属章节 + 相似度得分，交给模型组织最终回答。
 *
 * 这样把"知识检索"与"语言生成"解耦，模型只负责基于检索到的条款作答，降低政策幻觉。
 */
@Component
public class QueryStorePolicyTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(QueryStorePolicyTool.class);

    private final StorePolicyRagService ragService;

    public QueryStorePolicyTool(StorePolicyRagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public String name() {
        return "query_store_policy";
    }

    @Override
    public String description() {
        return "检索本店《退换货与会员政策》知识库（RAG）。当用户询问退货、换货、退款、运费、塑封拆封、"
                + "会员等级、会员折扣、积分、优惠券、发票、配送等政策问题时使用。返回 Top-K 相关条款原文、"
                + "所属章节与相似度得分；回答必须严格依据检索结果，不要编造政策。";
    }

    @Override
    public JSONObject parameterSchema() {
        JSONObject properties = new JSONObject(new LinkedHashMap<>());
        properties.put("query", property("string",
                "要检索的政策问题或关键词，例如“拆了塑封还能退吗”“金卡会员折扣”“退款多久到账”"));
        properties.put("top_k", property("integer",
                "返回条款数量，默认 3，最大 5"));

        JSONArray required = new JSONArray();
        required.add("query");

        JSONObject schema = new JSONObject(new LinkedHashMap<>());
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    // 工具入口：只做检索、不做回答。把检索到的条款原文、所属章节和相似度返回给大模型，
    // 由模型基于这些条款组织最终答案，所以政策回答有出处、可追溯。
    @Override
    public String execute(JSONObject args) {
        JSONObject safeArgs = args == null ? new JSONObject() : args;
        String query = trimToNull(safeArgs.getString("query"));
        if (query == null) {
            query = trimToNull(safeArgs.getString("question"));
        }
        if (query == null) {
            return errorJson("Parameter 'query' is required. Please provide the policy question to search.");
        }

        int topK = safeArgs.getIntValue("top_k", 3);
        if (topK <= 0 || topK > 5) {
            topK = 3;
        }

        log.info("[Tool:query_store_policy] query='{}', topK={}", query, topK);
        List<PolicySearchResult> hits = ragService.search(query, topK);
        if (hits.isEmpty()) {
            return errorJson("No relevant clause found in the store policy document for query: " + query
                    + ". Please tell the user this specific case is not covered and suggest contacting customer service.");
        }

        JSONArray clauses = new JSONArray();
        for (PolicySearchResult hit : hits) {
            JSONObject clause = new JSONObject(new LinkedHashMap<>());
            clause.put("chunkId", hit.getChunkId());
            clause.put("section", hit.getSection());
            clause.put("score", hit.getScore());
            clause.put("content", hit.getContent());
            clauses.add(clause);
        }

        JSONObject result = new JSONObject(new LinkedHashMap<>());
        result.put("success", true);
        result.put("query", query);
        result.put("topK", hits.size());
        result.put("clauses", clauses);
        result.put("hint", "Answer strictly based on the clauses above and cite the section name. "
                + "If the clauses do not cover the question, say so honestly and suggest contacting customer service.");
        return result.toJSONString();
    }

    private JSONObject property(String type, String description) {
        JSONObject property = new JSONObject(new LinkedHashMap<>());
        property.put("type", type);
        property.put("description", description);
        return property;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
