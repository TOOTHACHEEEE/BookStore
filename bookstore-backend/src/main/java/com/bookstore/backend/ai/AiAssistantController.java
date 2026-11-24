package com.bookstore.backend.ai;

import com.bookstore.backend.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【作业2 · 控制层】智能客服助手对话接口。
 *
 * 前端智能小助手把用户问题 POST 到 /api/v1/ai/chat，
 * 后端完成整套 Function Calling 流程后，返回：
 *   { code, message, data: { reply, toolCalls:[...], rounds } }
 * 其中 data.reply 是模型生成的自然语言回复，data.toolCalls 是被拦截的函数调用轨迹。
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiAssistantController {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantController.class);

    @Autowired
    private AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public ApiResponse chat(@RequestBody ChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return new ApiResponse(400, "User message cannot be empty", null);
        }
        log.info("Controller: AI assistant received question: {}", request.getMessage());
        try {
            ChatResponse response = aiAssistantService.chat(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("AI assistant processing failed", e);
            return ApiResponse.error("AI assistant temporarily unable to reply: " + e.getMessage());
        }
    }
}
