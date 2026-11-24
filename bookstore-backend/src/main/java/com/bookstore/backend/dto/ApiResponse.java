package com.bookstore.backend.dto;

/**
 * 【数据传输对象】统一API响应格式
 *
 * 【设计目的】
 * 前后端约定统一的响应格式，所有接口都返回这个结构
 * 前端只需要按照固定格式解析即可
 *
 * 【JSON格式】
 * {
 *   "code": 200,           // 状态码：200成功，404未找到，500服务器错误
 *   "message": "success",  // 提示信息
 *   "data": { ... }        // 实际数据
 * }
 */
public class ApiResponse {

    // 【状态码】约定：200成功，404未找到，500服务器错误
    private int code;

    // 【提示信息】用于展示给用户的信息
    private String message;

    // 【数据】实际返回的数据，可以是任意类型（List、Map、String等）
    private Object data;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 【快捷方法】成功响应，带数据
    public static ApiResponse success(Object data) {
        return new ApiResponse(200, "success", data);
    }

    // 【快捷方法】成功响应，不带数据
    public static ApiResponse success() {
        return new ApiResponse(200, "success", null);
    }

    // 【快捷方法】错误响应
    public static ApiResponse error(String message) {
        return new ApiResponse(500, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}