package com.bookstore.backend.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 【作业2 · 核心服务】AiAssistantService —— Function Calling 主循环
 *
 * 完整还原了大模型工具调用的闭环流程：
 *   1. 组装 messages（system + history + user），并携带 tools（两个 Function 的 Schema）调用大模型；
 *   2. 【拦截并解析】大模型返回的 tool_calls，打印出"需要调用的函数名及参数"； —— 评分点②
 *   3. 【本地模拟执行】对应函数，拿到结果；                                    —— 评分点③
 *   4. 把函数结果以 role=tool 的消息【回传给大模型上下文】，再次请求生成最终回答； —— 评分点④
 *   5. 循环直到模型不再请求工具，返回自然语言回复 + 完整调用轨迹。
 *
 * 异常处理（评分点⑤）由 AssistantTools 内部完成（错误 ISBN / 接口超时），
 * 错误结果同样回传给模型，触发模型的 Self-Correction（如实告知并给出建议）。
 */
@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    private static final String SYSTEM_PROMPT =
            "你是电子书店的智能客服小助手「书童」。你可以调用工具来查询本店图书的实时库存(check_inventory)"
            + "以及竞争对手的售价(get_competitor_price)，帮助用户解答关于图书库存、是否有货、价格与比价的问题。"
            + "规则：1) 当需要实时数据时必须调用相应函数，不要凭空编造库存或价格；"
            + "2) 拿到函数返回结果后，用简洁、友好的中文自然语言回复用户；"
            + "3) 如果函数返回 success=false（例如 ISBN 不存在或比价接口超时），请如实告知用户并进行自我纠正，"
            + "例如建议用户核对 ISBN、改用书名查询或稍后重试，绝不能虚构数据。";

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private AssistantTools assistantTools;

    @Value("${deepseek.max-tool-rounds:5}")
    private int maxToolRounds;

    /**
     * 处理一次用户对话，返回模型最终回复与函数调用轨迹。
     */
    public ChatResponse chat(ChatRequest request) throws Exception {
        JSONArray tools = assistantTools.buildToolSchemas();
        JSONArray messages = buildInitialMessages(request);
        List<ToolCallRecord> traces = new ArrayList<>();

        String finalReply = null;
        int round = 0;

        // Function Calling 主循环：模型可能连续多轮请求不同工具
        while (round < maxToolRounds) {
            round++;
            JSONObject response = deepSeekClient.chatCompletion(messages, tools);
            JSONObject message = extractAssistantMessage(response);
            if (message == null) {
                throw new RuntimeException("大模型响应格式异常，无法解析 choices[0].message：" + response);
            }

            JSONArray toolCalls = message.getJSONArray("tool_calls");

            // Case 1: Model did not request tools -> got final natural language reply
            if (toolCalls == null || toolCalls.isEmpty()) {
                finalReply = message.getString("content");
                log.info("=== Round {}: Model generated final reply (no tool calls) ===", round);
                break;
            }

            // Case 2: Model requests function calls -> intercept, print, execute, return results
            log.info("=== Round {}: Intercepted {} Function Call(s) ===", round, toolCalls.size());
            // 必须先把带 tool_calls 的 assistant 消息加入上下文，再追加 tool 结果消息
            messages.add(message);

            for (int i = 0; i < toolCalls.size(); i++) {
                JSONObject toolCall = toolCalls.getJSONObject(i);
                String callId = toolCall.getString("id");
                JSONObject function = toolCall.getJSONObject("function");
                String functionName = function == null ? null : function.getString("name");
                String argumentsJson = function == null ? null : function.getString("arguments");

                // [Print function name and parameters] - Required by homework
                log.info("  >> Function to call: {}", functionName);
                log.info("  >> Arguments: {}", argumentsJson);

                JSONObject args = parseArguments(argumentsJson);

                // 【本地模拟执行】
                long start = System.currentTimeMillis();
                String resultJson = assistantTools.execute(functionName, args);
                long duration = System.currentTimeMillis() - start;
                boolean success = isSuccess(resultJson);

                log.info("  << Execution result ({}ms, success={}): {}", duration, success, resultJson);

                // 记录轨迹，供前端"打印"展示
                traces.add(new ToolCallRecord(callId, functionName, argumentsJson, resultJson, success, duration));

                // 【把函数执行结果回传给大模型上下文】—— role=tool 消息
                JSONObject toolMessage = new JSONObject();
                toolMessage.put("role", "tool");
                toolMessage.put("tool_call_id", callId);
                toolMessage.put("content", resultJson);
                messages.add(toolMessage);
            }
            // 循环继续：带着工具结果再次请求模型生成回答（评分点④）
        }

        // 兜底：达到最大轮数仍未拿到文本回复时，强制模型基于已有工具结果作答（不再提供工具）
        if (finalReply == null) {
            log.warn("Reached max tool call rounds {}, forcing final reply", maxToolRounds);
            JSONObject response = deepSeekClient.chatCompletion(messages, null);
            JSONObject message = extractAssistantMessage(response);
            finalReply = message == null ? "抱歉，我暂时无法生成回复，请稍后再试。" : message.getString("content");
        }

        return new ChatResponse(finalReply, traces, round);
    }

    /** 组装初始 messages：system 提示词 + 历史对话 + 本轮用户输入 */
    private JSONArray buildInitialMessages(ChatRequest request) {
        JSONArray messages = new JSONArray();

        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", SYSTEM_PROMPT);
        messages.add(system);

        if (request.getHistory() != null) {
            for (ChatRequest.HistoryMessage h : request.getHistory()) {
                if (h.getContent() == null || h.getContent().isBlank()) {
                    continue;
                }
                String role = "assistant".equals(h.getRole()) ? "assistant" : "user";
                JSONObject m = new JSONObject();
                m.put("role", role);
                m.put("content", h.getContent());
                messages.add(m);
            }
        }

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", request.getMessage());
        messages.add(user);
        return messages;
    }

    /** 从原始响应中取出 choices[0].message */
    private JSONObject extractAssistantMessage(JSONObject response) {
        if (response == null) {
            return null;
        }
        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.getJSONObject(0).getJSONObject("message");
    }

    /** 安全解析 arguments 字符串为 JSONObject */
    private JSONObject parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(argumentsJson);
        } catch (Exception e) {
            log.warn("Failed to parse function arguments, using empty: {}", argumentsJson, e);
            return new JSONObject();
        }
    }

    /** 从函数返回结果中判断 success 字段 */
    private boolean isSuccess(String resultJson) {
        try {
            JSONObject obj = JSON.parseObject(resultJson);
            return obj != null && Boolean.TRUE.equals(obj.getBoolean("success"));
        } catch (Exception e) {
            return false;
        }
    }
}
