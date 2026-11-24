package com.bookstore.backend.agent;

/**
 * 【作业3 · DTO】一次 ReAct 步骤的完整轨迹。
 * 前端按 Thought -> Action -> Action Input -> Observation 的顺序展示，
 * 后端日志也会打印同样的链路，直接对应作业评分点⑤。
 */
public class ReActStep {

    private int step;              // 第几步
    private String thought;        // 思考
    private String action;         // 行动（工具名）
    private String actionInput;    // 行动参数
    private String observation;    // 工具返回结果
    private boolean success;       // 工具是否执行成功
    private long durationMs;       // 工具执行耗时

    public ReActStep() {
    }

    public ReActStep(int step, String thought, String action, String actionInput,
                     String observation, boolean success, long durationMs) {
        this.step = step;
        this.thought = thought;
        this.action = action;
        this.actionInput = actionInput;
        this.observation = observation;
        this.success = success;
        this.durationMs = durationMs;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getThought() {
        return thought;
    }

    public void setThought(String thought) {
        this.thought = thought;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionInput() {
        return actionInput;
    }

    public void setActionInput(String actionInput) {
        this.actionInput = actionInput;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}
