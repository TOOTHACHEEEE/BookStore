package com.bookstore.backend.agent;

import java.util.List;

/**
 * 【作业3 · DTO】导购 Agent 的请求体。
 * message：本轮用户输入；history：此前的纯文本对话（user / assistant），用于多轮上下文。
 */
public class AgentChatRequest {

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

    public static class HistoryMessage {
        private String role;
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
