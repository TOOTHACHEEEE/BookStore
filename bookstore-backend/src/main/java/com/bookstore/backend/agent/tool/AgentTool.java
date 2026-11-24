package com.bookstore.backend.agent.tool;

import com.alibaba.fastjson2.JSONObject;

/**
 * 【作业3 · 工具层】AgentTool —— ReAct Agent 可调用工具的统一定义。
 *
 * 每个工具需要提供：
 *  - name()            工具名（模型在 Action 中输出这个名字）
 *  - description()     给模型看的能力说明（会拼进 System Prompt）
 *  - parameterSchema() 参数 JSON Schema（告诉模型 Action Input 该怎么写）
 *  - execute()         本地真实执行逻辑，返回 JSON 字符串作为 Observation
 */
public interface AgentTool {

    String name();

    String description();

    JSONObject parameterSchema();

    String execute(JSONObject args);

    /** 统一的结构化错误返回：模型看到 success=false 后会自我纠正或如实告知用户 */
    default String errorJson(String message) {
        JSONObject obj = new JSONObject();
        obj.put("success", false);
        obj.put("error", message);
        return obj.toJSONString();
    }
}
