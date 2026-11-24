package com.bookstore.backend.agent;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.rag.PolicySearchResult;
import com.bookstore.backend.rag.StorePolicyRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 【作业3 · 控制层】导购 Agent 与政策 RAG 接口。
 *
 *  POST /api/v1/agent/chat            ReAct 导购对话（返回 reply + steps 轨迹）
 *  GET  /api/v1/agent/policy/document 政策原文（供“退换货与会员政策”页面展示）
 *  GET  /api/v1/agent/policy/search   政策 RAG 检索调试（返回 Top-K 条款与相似度）
 *  GET  /api/v1/agent/policy/stats    政策知识库概况（切片数、向量维度等）
 */
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final BookstoreAgentService agentService;
    private final StorePolicyRagService ragService;

    public AgentController(BookstoreAgentService agentService, StorePolicyRagService ragService) {
        this.agentService = agentService;
        this.ragService = ragService;
    }

    @PostMapping("/chat")
    public ApiResponse chat(@RequestBody AgentChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return new ApiResponse(400, "User message cannot be empty", null);
        }
        log.info("Controller: guide agent received question: {}", request.getMessage());
        try {
            AgentChatResponse response = agentService.chat(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Guide agent processing failed", e);
            return ApiResponse.error("Guide agent temporarily unable to reply: " + e.getMessage());
        }
    }

    @GetMapping("/policy/document")
    public ApiResponse policyDocument() {
        return ApiResponse.success(ragService.document());
    }

    /** RAG 检索调试接口：不经过大模型，直接观察"问题 -> 向量检索 -> 条款"的效果 */
    @GetMapping("/policy/search")
    public ApiResponse searchPolicy(@RequestParam("q") String query,
                                    @RequestParam(value = "topK", defaultValue = "3") int topK) {
        if (query == null || query.isBlank()) {
            return new ApiResponse(400, "Query parameter 'q' cannot be empty", null);
        }
        try {
            List<PolicySearchResult> results = ragService.search(query, topK);
            return ApiResponse.success(results);
        } catch (Exception e) {
            log.error("Policy RAG search failed", e);
            return ApiResponse.error("Policy search failed: " + e.getMessage());
        }
    }

    @GetMapping("/policy/stats")
    public ApiResponse policyStats() {
        return ApiResponse.success(ragService.stats());
    }
}
