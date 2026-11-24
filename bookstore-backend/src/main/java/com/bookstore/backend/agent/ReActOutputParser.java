package com.bookstore.backend.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 【作业3 · ReAct 解析器】ReActOutputParser。
 *
 * 负责把模型输出的纯文本拆成 ReAct 的三个要素：
 *   Thought       -> 推理过程
 *   Action        -> 工具名
 *   Action Input  -> 工具参数（JSON）
 * 以及终止标记：
 *   Final Answer  -> 最终回答
 *
 * 解析器做了充分的容错：
 *  - 同时识别中英文标签（Thought/思考、Action/行动、Final Answer/最终答案）；
 *  - 支持 Action Input 被 ```json 代码块包裹；
 *  - 使用括号配对扫描提取第一个完整 JSON 对象，忽略 JSON 后面的解释性文字；
 *  - 若 Action Input 只是普通字符串（如 "微服务"），自动包装为 {"query": "微服务"}。
 *
 * 解析失败不会抛异常，而是把错误信息交给上层作为 Observation 回传给模型，让模型自我纠正。
 */
@Component
public class ReActOutputParser {

    private static final Logger log = LoggerFactory.getLogger(ReActOutputParser.class);

    private static final Pattern FINAL_ANSWER =
            Pattern.compile("(?im)^\\s*[*_`#>\\-]*\\s*(?:Final Answer|最终答案)\\s*[*_`]*\\s*[:：]\\s*[*_`#>\\-]*\\s*");
    private static final Pattern ACTION =
            Pattern.compile("(?im)^\\s*[*_`#>\\-]*\\s*(?:Action|行动)\\s*[*_`]*\\s*[:：]\\s*[*_`#>\\-]*\\s*`?([A-Za-z_][A-Za-z0-9_]*)`?[^\\n]*$");
    private static final Pattern ACTION_INPUT =
            Pattern.compile("(?is)[*_`]*\\s*(?:Action\\s*Input|行动输入)\\s*[*_`]*\\s*[:：]\\s*[*_`#>\\-]*\\s*");
    private static final Pattern NEXT_THOUGHT =
            Pattern.compile("(?im)^\\s*[*_`#>\\-]*\\s*(?:Thought|思考)\\s*[*_`]*\\s*[:：]");
    private static final Pattern THOUGHT_PREFIX =
            Pattern.compile("(?is)^\\s*[*_`#>\\-]*\\s*(?:Thought|思考)\\s*[*_`]*\\s*[:：]\\s*[*_`#>\\-]*\\s*");
    private static final Pattern CODE_FENCE =
            Pattern.compile("(?is)```(?:json)?\\s*(.*?)```");

    // 把模型输出的一段纯文本解析成四个字段：思考 Thought、工具名 Action、
    // 行动参数 Action Input、最终答案 Final Answer；解析失败不抛异常，作为观察结果回传给模型自我纠正。
    public Result parse(String text) {
        Result result = new Result();
        if (text == null || text.isBlank()) {
            result.inputError = "Model returned empty output.";
            return result;
        }
        String output = text.trim();

        // 1. 优先识别 Final Answer：出现即代表本轮 ReAct 结束
        Matcher finalMatcher = FINAL_ANSWER.matcher(output);
        if (finalMatcher.find()) {
            String answer = output.substring(finalMatcher.end()).trim();
            if (!answer.isEmpty()) {
                result.finalAnswer = answer;
                result.thought = cleanThought(output.substring(0, finalMatcher.start()));
                return result;
            }
        }

        // 2. 识别 Action
        Matcher actionMatcher = ACTION.matcher(output);
        if (!actionMatcher.find()) {
            result.inputError = "No Action or Final Answer found. Please follow the ReAct format.";
            return result;
        }
        result.action = actionMatcher.group(1).trim();
        result.thought = cleanThought(output.substring(0, actionMatcher.start()));

        // 3. 识别 Action Input（截断到下一个 Thought 之前）
        String tail = output.substring(actionMatcher.end());
        Matcher nextThought = NEXT_THOUGHT.matcher(tail);
        if (nextThought.find()) {
            tail = tail.substring(0, nextThought.start());
        }

        Matcher inputMatcher = ACTION_INPUT.matcher(tail);
        if (!inputMatcher.find()) {
            result.inputError = "Missing 'Action Input:' after Action '" + result.action
                    + "'. Expected a JSON object, e.g. {\"query\":\"...\"}.";
            return result;
        }

        String rawInput = tail.substring(inputMatcher.end()).trim();
        result.rawInput = rawInput;

        // 去掉可能的 Markdown 代码围栏
        Matcher fence = CODE_FENCE.matcher(rawInput);
        if (fence.find()) {
            rawInput = fence.group(1).trim();
        }

        // 提取第一个完整的 JSON 对象
        String json = extractJsonObject(rawInput);
        if (json == null) {
            // 容错：模型只写了普通字符串，自动包装成 query 参数
            String plain = stripQuotes(rawInput);
            if (plain.isEmpty()) {
                result.inputError = "Action Input is empty. Expected a JSON object, e.g. {\"query\":\"...\"}.";
                return result;
            }
            JSONObject args = new JSONObject();
            args.put("query", plain);
            result.arguments = args;
            result.rawInput = plain;
            return result;
        }

        try {
            result.arguments = JSON.parseObject(json);
            result.rawInput = json;
        } catch (Exception e) {
            log.warn("[ReAct] Failed to parse Action Input as JSON: {}", json, e);
            result.inputError = "Action Input is not valid JSON (" + e.getMessage()
                    + "). Expected an object like {\"query\":\"...\"}.";
        }
        return result;
    }

    /** 用括号配对扫描提取第一个完整 JSON 对象，正确处理字符串中的 { } 与转义引号 */
    private String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        if (start < 0) {
            return null;
        }
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int i = start; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return raw.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private String cleanThought(String text) {
        String thought = THOUGHT_PREFIX.matcher(text).replaceFirst("").trim();
        return thought.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
    }

    private String stripQuotes(String text) {
        return text.trim().replaceAll("^[\"'“”‘’]", "").replaceAll("[\"'“”‘’]$", "").trim();
    }

    /** 解析结果 */
    public static class Result {
        private String thought = "";
        private String action;
        private String rawInput;
        private JSONObject arguments;
        private String finalAnswer;
        private String inputError;

        public boolean hasFinalAnswer() {
            return finalAnswer != null && !finalAnswer.isBlank();
        }

        public boolean hasAction() {
            return action != null && !action.isBlank();
        }

        public String getThought() {
            return thought;
        }

        public String getAction() {
            return action;
        }

        public String getRawInput() {
            return rawInput;
        }

        public JSONObject getArguments() {
            return arguments;
        }

        public String getFinalAnswer() {
            return finalAnswer;
        }

        public String getInputError() {
            return inputError;
        }
    }
}
