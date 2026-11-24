package com.bookstore.backend.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bookstore.backend.agent.tool.AgentTool;
import com.bookstore.backend.agent.tool.QueryStorePolicyTool;
import com.bookstore.backend.agent.tool.SearchBookCatalogTool;
import com.bookstore.backend.ai.DeepSeekClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 【作业3 · 核心】BookstoreAgentService —— 书店导购 Agent 的 ReAct 循环。
 *
 * ReAct = Reason + Act。每一轮：
 *   1. 把 System Prompt + 历史对话 + 用户问题发给大模型；
  *   2. 模型输出 Thought（思考）与 Action（要调用的工具）+ Action Input（参数）；
 *   3. 本地执行工具，得到 Observation（观察结果）；
 *   4. 把 Observation 追加回上下文，进入下一轮；
 *   5. 模型认为信息足够时输出 Final Answer，循环结束。
 *
 * 与"原生 Function Calling"相比，这里采用文本协议版 ReAct：
 * 思考过程（Thought）会显式出现在模型输出、后端日志和前端轨迹面板中，
 * 能够完整展示 Thought -> Action -> Observation 的执行链路（评分点⑤）。
 *
 * 两个工具（评分点④：根据意图正确路由）：
 *   - search_book_catalog：模糊条件查书、荐书；
 *   - query_store_policy ：RAG 检索退换货与会员政策。
 */
@Service
public class BookstoreAgentService {

    private static final Logger log = LoggerFactory.getLogger(BookstoreAgentService.class);

    private static final String FORMAT_REMINDER =
            "Your previous output did not follow the required ReAct format. "
            + "Reply with exactly one of the following two formats:\n"
            + "Thought: <reasoning>\nAction: <tool_name>\nAction Input: <a valid JSON object>\n"
            + "or\nThought: <reasoning>\nFinal Answer: <answer to the user>";

    private final DeepSeekClient deepSeekClient;
    private final ReActOutputParser parser;
    private final List<AgentTool> tools;

    @Value("${agent.max-steps:6}")
    private int maxSteps;

    /** 问题中出现这些词时，必须依赖工具数据，不能由模型直接作答 */
    private static final List<String> TOOL_HINTS = List.of(
            "书", "买", "购", "推荐", "作者", "书名", "科幻", "小说", "微服务", "库存", "价格",
            "退", "换", "会员", "积分", "优惠", "折扣", "发票", "配送", "物流", "塑封", "拆封", "运费", "客服");

    @Value("${agent.temperature:0.2}")
    private double temperature;

    public BookstoreAgentService(DeepSeekClient deepSeekClient,
                                 ReActOutputParser parser,
                                 SearchBookCatalogTool searchBookCatalogTool,
                                 QueryStorePolicyTool queryStorePolicyTool) {
        this.deepSeekClient = deepSeekClient;
        this.parser = parser;
        this.tools = List.of(searchBookCatalogTool, queryStorePolicyTool);
    }

    /**
     * 处理一次导购对话，返回最终回答与完整的 ReAct 轨迹。
     */
    // ReAct 主循环：每一轮先让模型思考并决定调用哪个工具，本地执行完把结果作为观察回传给模型，
    // 再进入下一轮；最多 6 步，模型输出 Final Answer 就结束。
    public AgentChatResponse chat(AgentChatRequest request) throws Exception {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("User message cannot be empty");
        }

        String userQuestion = request.getMessage().trim();
        JSONArray messages = buildInitialMessages(request);
        List<ReActStep> steps = new ArrayList<>();

        String finalReply = null;
        String finalThought = null;
        int round = 0;
        boolean toolUseReminded = false;

        log.info("[ReAct] ============================================================");
        log.info("[ReAct] 新会话 | 用户问题：{}", userQuestion);
        log.info("[ReAct] 可用工具：{}", tools.stream().map(AgentTool::name).toList());

        while (round < maxSteps) {
            round++;
            // ===== 第 1 步：把整段上下文发给大模型 =====
            String output = callModel(messages);
            log.debug("[ReAct] Step {} | 原始模型输出:\n{}", round, output);

            // ===== 第 2 步：解析模型输出，拆出 Thought / Action / Action Input / Final Answer =====
            ReActOutputParser.Result parsed = parser.parse(output);

            // ===== 第 3 步：判断是否结束——模型给了 Final Answer 就跳出循环 =====
            if (parsed.hasFinalAnswer()) {
                // 防御：模型偶尔会在没有调用任何工具时直接作答，导致前端 ReAct 轨迹为空。
                // 涉及找书/政策的问题先强制它调用一次工具，只提醒一次避免死循环。
                if (steps.isEmpty() && !toolUseReminded && requiresTool(userQuestion)) {
                    toolUseReminded = true;
                    log.warn("[ReAct] Step {} | 模型未调用工具就直接作答，回传提醒要求其调用工具", round);
                    messages.add(assistantMessage(output));
                    messages.add(userMessage("Observation: [SYSTEM] 你还没有调用任何工具，不能直接回答涉及书目或政策的问题。"
                            + "请先输出 Thought 与 Action，调用 search_book_catalog 或 query_store_policy 获取真实数据，再输出 Final Answer。"));
                    continue;
                }
                finalReply = parsed.getFinalAnswer();
                finalThought = parsed.getThought();
                log.info("[ReAct] Step {} | 最终思考 (Final Thought)：{}", round, finalThought);
                log.info("[ReAct] Step {} | 最终回答 (Final Answer)：\n{}", round, finalReply);
                break;
            }

            // ===== 第 4 步：模型没按 ReAct 格式输出，把格式要求作为 Observation 回传让它自我纠正 =====
            if (!parsed.hasAction()) {
                log.warn("[ReAct] Step {} | 模型输出不符合 ReAct 协议：{}", round, parsed.getInputError());
                messages.add(assistantMessage(output));
                messages.add(userMessage("Observation: [FORMAT ERROR] " + FORMAT_REMINDER));
                continue;
            }

            // ===== 第 5 步：取出这三个要素 =====
            String thought = parsed.getThought();
            String action = parsed.getAction();
            String actionInput = parsed.getRawInput();

            log.info("[ReAct] Step {} | 思考 (Thought)：{}", round, thought);
            log.info("[ReAct] Step {} | 行动 (Action)：{}", round, action);
            log.info("[ReAct] Step {} | 行动参数 (Action Input)：{}", round, actionInput);

            // ===== 第 6 步：先把模型这一轮的思考与行动写进上下文，后面才能接 Observation =====
            messages.add(assistantMessage(output));

            long start = System.currentTimeMillis();
            String observation;
            boolean success;

            // ===== 第 7 步：执行工具——按工具名找到本地实现并调用（失败也返回结构化错误） =====
            if (parsed.getInputError() != null) {
                observation = errorJson(parsed.getInputError());
                success = false;
            } else {
                AgentTool tool = findTool(action);
                if (tool == null) {
                    observation = errorJson("Unknown tool '" + action + "'. Available tools: "
                            + tools.stream().map(AgentTool::name).toList()
                            + ". Please choose one of the available tools.");
                    success = false;
                } else {
                    try {
                        observation = tool.execute(parsed.getArguments());
                        success = isSuccess(observation);
                    } catch (Exception e) {
                        log.error("[ReAct] Tool '{}' execution failed", action, e);
                        observation = errorJson("Tool '" + action + "' execution failed: " + e.getMessage());
                        success = false;
                    }
                }
            }

            long duration = System.currentTimeMillis() - start;
            log.info("[ReAct] Step {} | 观察结果 (Observation, success={}, {} ms)：\n{}", round, success, duration, observation);

            // ===== 第 8 步：把这一轮的思考/行动/观察记进轨迹，前端用来展示 ReAct 过程 =====
            steps.add(new ReActStep(steps.size() + 1, thought, action, actionInput,
                    observation, success, duration));

            // ===== 第 9 步：把 Observation 回传给模型，进入下一轮循环 =====
            messages.add(userMessage("Observation: " + observation));
        }

        // 兜底：达到最大步数仍未给出 Final Answer，强制模型基于已有观察作答
        if (finalReply == null) {
            log.warn("[ReAct] 已达到最大步数 ({})，强制模型给出最终回答", maxSteps);
            messages.add(userMessage("Observation: [SYSTEM] Maximum ReAct steps reached. "
                    + "Do not call any more tools. Output 'Final Answer:' now based only on the observations above."));
            String output = callModel(messages);
            ReActOutputParser.Result parsed = parser.parse(output);
            finalReply = parsed.hasFinalAnswer() ? parsed.getFinalAnswer() : output.trim();
            finalThought = parsed.getThought();
            log.info("[ReAct] 强制最终回答 (Forced Final Answer)：\n{}", finalReply);
        }

        log.info("[ReAct] 会话结束 | rounds={}, toolCalls={}", round, steps.size());
        log.info("[ReAct] ============================================================");

        return new AgentChatResponse(finalReply, finalThought, steps, round);
    }

    /**
     * System Prompt 由两部分动态拼装：
     *  - 工具清单：工具名 + 能力说明 + 参数 JSON Schema；
     *  - ReAct 协议：输出格式、路由规则、异常处理规则。
     */
    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是拾光书店的智能导购 Agent「书灵」，既能为用户推荐图书，也能解答本店的退换货与会员政策。\n\n");

        prompt.append("## 可用工具\n\n");
        for (AgentTool tool : tools) {
            prompt.append("### ").append(tool.name()).append("\n");
            prompt.append(tool.description()).append("\n");
            prompt.append("参数 Schema：").append(tool.parameterSchema().toJSONString()).append("\n\n");
        }

        prompt.append("## 工作方式（ReAct）\n");
        prompt.append("你必须先思考再行动。每次只输出一个 Action，等待系统返回 Observation 后再继续。\n");
        prompt.append("严格使用下面的格式，不要输出格式之外的额外说明：\n\n");
        prompt.append("Thought: <你的推理过程>\n");
        prompt.append("Action: <工具名>\n");
        prompt.append("Action Input: <单行合法 JSON>\n\n");
        prompt.append("当你已经拿到足够信息、可以回答用户时，输出：\n\n");
        prompt.append("Thought: <最后的推理>\n");
        prompt.append("Final Answer: <面向用户的完整中文回答>\n\n");
        prompt.append("格式示例（只示意格式，不要照抄内容）：\n");
        prompt.append("Thought: 用户想找科幻小说，我先检索书库。\n");
        prompt.append("Action: search_book_catalog\n");
        prompt.append("Action Input: {\"genre\":\"科幻\",\"limit\":3}\n");
        prompt.append("（系统返回 Observation 后，你继续输出下一个 Thought 与 Action，或输出 Final Answer。）\n\n");

        prompt.append("## 规则\n");
        prompt.append("1. 选书、荐书、按作者/书名/题材找书，必须先调用 search_book_catalog；不要凭记忆编造书目、价格或库存。\n");
        prompt.append("2. 退换货、退款、运费、拆封塑封、会员等级与折扣、积分、优惠券、发票、配送等政策问题，必须先调用 query_store_policy；回答必须严格依据检索到的条款，并注明章节名。\n");
        prompt.append("3. 一个用户请求可能同时包含选书和政策问题，必须分别调用 search_book_catalog 和 query_store_policy，全部拿到 Observation 后再输出 Final Answer。\n");
        prompt.append("4. Action Input 必须是合法 JSON，不能包含注释、Markdown 代码块或多余文字。\n");
        prompt.append("5. 如果工具返回 success=false 或 total=0，如实告知用户，并给出放宽条件的建议，绝对不能虚构书目或政策。\n");
        prompt.append("6. Final Answer 用简洁友好的中文，适当分点；推荐图书时给出书名、作者、价格、库存和一句推荐理由；回答政策时引用章节名。\n");
        prompt.append("7. 不要向用户暴露工具名、Observation、JSON 原文或内部推理格式。\n");
        prompt.append("8. 一次最多输出一个 Action，不要在一次回复中同时写两个 Action；在系统返回 Observation 之前，不要假设任何 Action 已经执行。\n");
        prompt.append("9. 涉及找书、荐书或本店政策的问题，必须先调用至少一个工具；在没有任何 Observation 的情况下直接输出 Final Answer 属于违规。\n");
        return prompt.toString();
    }

    /** 组装 messages：system + 历史纯文本对话 + 本轮用户输入 */
    private JSONArray buildInitialMessages(AgentChatRequest request) {
        JSONArray messages = new JSONArray();

        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", buildSystemPrompt());
        messages.add(system);

        if (request.getHistory() != null) {
            for (AgentChatRequest.HistoryMessage history : request.getHistory()) {
                if (history == null || history.getContent() == null || history.getContent().isBlank()) {
                    continue;
                }
                String role = "assistant".equals(history.getRole()) ? "assistant" : "user";
                messages.add(message(role, history.getContent()));
            }
        }

        messages.add(message("user", request.getMessage().trim()));
        return messages;
    }

    private String callModel(JSONArray messages) throws Exception {
        JSONObject response = deepSeekClient.chatCompletion(messages, null, temperature);
        JSONArray choices = response == null ? null : response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("LLM response has no choices: " + response);
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String content = message == null ? null : message.getString("content");
        if (content == null || content.isBlank()) {
            throw new RuntimeException("LLM returned empty content");
        }
        return content.trim();
    }

    private AgentTool findTool(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.replace("`", "").trim();
        return tools.stream()
                .filter(tool -> tool.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }

    private JSONObject message(String role, String content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private JSONObject assistantMessage(String content) {
        return message("assistant", content);
    }

    private JSONObject userMessage(String content) {
        return message("user", content);
    }

    /** 判断问题是否必须依赖工具数据（找书 / 政策），用于拦截“不调工具直接作答” */
    private boolean requiresTool(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        return TOOL_HINTS.stream().anyMatch(question::contains);
    }

    private boolean isSuccess(String observation) {
        try {
            JSONObject json = JSON.parseObject(observation);
            return json != null && Boolean.TRUE.equals(json.getBoolean("success"));
        } catch (Exception e) {
            return false;
        }
    }

    private String errorJson(String message) {
        JSONObject json = new JSONObject();
        json.put("success", false);
        json.put("error", message);
        return json.toJSONString();
    }
}
