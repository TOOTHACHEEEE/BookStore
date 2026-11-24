package com.bookstore.backend.agent;

import java.util.List;

/**
 * 【作业3 · DTO】导购 Agent 的响应体。
 * reply：最终自然语言回答；
 * steps：完整的 ReAct 轨迹（Thought -> Action -> Observation）；
 * finalThought：输出 Final Answer 前的最后一次思考；
 * rounds：本轮模型调用次数。
 */
public class AgentChatResponse {

    private String reply;
    private String finalThought;
    private List<ReActStep> steps;
    private int rounds;
    private String mode = "ReAct";

    public AgentChatResponse() {
    }

    public AgentChatResponse(String reply, String finalThought, List<ReActStep> steps, int rounds) {
        this.reply = reply;
        this.finalThought = finalThought;
        this.steps = steps;
        this.rounds = rounds;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getFinalThought() {
        return finalThought;
    }

    public void setFinalThought(String finalThought) {
        this.finalThought = finalThought;
    }

    public List<ReActStep> getSteps() {
        return steps;
    }

    public void setSteps(List<ReActStep> steps) {
        this.steps = steps;
    }

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
