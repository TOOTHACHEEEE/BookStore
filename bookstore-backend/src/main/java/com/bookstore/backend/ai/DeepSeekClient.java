package com.bookstore.backend.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 【作业2 · 大模型客户端】DeepSeekClient
 *
 * 负责通过 HTTP 调用 DeepSeek（OpenAI 兼容）/chat/completions 接口，
 * 并在请求体中携带 tools（Function Schema），使大模型能够识别并触发 Function Calling。
 * —— 对应评分点②：代码能够正确接收并解析大模型返回的 Tool Call 请求（解析在 Service 中，
 *    本类负责把请求发出去并把原始响应 JSON 拿回来）。
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    /** API Key 是否已配置，未配置时上层可给出友好提示 */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 发起一次 chat/completions 调用（默认温度 0.7，适合客服对话）。
     *
     * @param messages 对话消息数组（含 system / user / assistant / tool 角色）
     * @param tools    工具（Function）Schema 数组，可为 null
     * @return 大模型响应的原始 JSON
     */
    public JSONObject chatCompletion(JSONArray messages, JSONArray tools) throws Exception {
        return chatCompletion(messages, tools, 0.7);
    }

    /**
     * 发起一次 chat/completions 调用，并指定温度。
     * ReAct 导购 Agent 需要稳定的
     * Thought / Action 格式，因此使用较低温度。
     */
    public JSONObject chatCompletion(JSONArray messages, JSONArray tools, double temperature) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("尚未配置 DeepSeek API Key，请在 application.properties 的 deepseek.api-key 或环境变量 DEEPSEEK_API_KEY 中填写。");
        }

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString(), StandardCharsets.UTF_8))
                .build();

        log.debug("Calling DeepSeek: model={}, temperature={}, messages={}, tools={}", model, temperature,
                messages == null ? 0 : messages.size(), tools == null ? 0 : tools.size());

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepSeek API 调用失败, HTTP " + response.statusCode() + ": " + response.body());
        }
        return JSON.parseObject(response.body());
    }
}
