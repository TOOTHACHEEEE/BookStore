package com.bookstore.backend.ai;

import java.util.List;

/**
 * 【作业2 · DTO】前端智能助手发来的对话请求。
 * message：本轮用户输入；history：可选的历史对话（role + content），用于多轮上下文。
 */
public class ChatRequest {

    private String message;
    private List<HistoryMessage> history;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<HistoryMessage> getHistory() {
        return history;
    }

    public void setHistory(List<HistoryMessage> history) {
        this.history = history;
    }

    /** 历史消息条目 */
    public static class HistoryMessage {
        private String role;    // user / assistant
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
