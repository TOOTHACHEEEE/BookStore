package com.bookstore.backend.ai;

import java.util.List;

/**
 * 【作业2 · DTO】返回给前端智能助手的响应。
 * reply：模型最终生成的自然语言回复（显示在对话气泡中）；
 * toolCalls：本轮对话中被拦截并执行的 Function Call 轨迹（前端"打印"出来）；
 * rounds：模型与工具交互的轮数。
 */
public class ChatResponse {

    private String reply;
    private List<ToolCallRecord> toolCalls;
    private int rounds;

    public ChatResponse() {
    }

    public ChatResponse(String reply, List<ToolCallRecord> toolCalls, int rounds) {
        this.reply = reply;
        this.toolCalls = toolCalls;
        this.rounds = rounds;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<ToolCallRecord> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCallRecord> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }
}
