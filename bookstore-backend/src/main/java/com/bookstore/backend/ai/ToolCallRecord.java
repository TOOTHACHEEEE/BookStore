package com.bookstore.backend.ai;

/**
 * 【作业2 · DTO】一次被拦截并执行的 Function Call 记录。
 * 前端会把这份"函数调用轨迹"打印出来，直观展示：
 *   模型要调用哪个函数(functionName)、传了什么参数(argumentsJson)、
 *   本地模拟执行的结果(resultJson)、是否成功(success)、耗时(durationMs)。
 */
public class ToolCallRecord {

    private String id;             // 大模型返回的 tool_call id
    private String functionName;   // 需要调用的函数名
    private String argumentsJson;  // 函数参数（JSON 字符串）
    private String resultJson;     // 本地模拟执行后的返回结果（JSON 字符串）
    private boolean success;       // 执行是否成功
    private long durationMs;       // 执行耗时

    public ToolCallRecord() {
    }

    public ToolCallRecord(String id, String functionName, String argumentsJson,
                          String resultJson, boolean success, long durationMs) {
        this.id = id;
        this.functionName = functionName;
        this.argumentsJson = argumentsJson;
        this.resultJson = resultJson;
        this.success = success;
        this.durationMs = durationMs;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }

    public void setArgumentsJson(String argumentsJson) {
        this.argumentsJson = argumentsJson;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
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
