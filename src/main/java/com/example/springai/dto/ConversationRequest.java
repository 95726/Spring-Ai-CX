package com.example.springai.dto;

/**
 * 带上下文的聊天请求DTO
 *
 * 包含用户消息和可选的会话ID，用于支持多轮对话。
 * 如果不传sessionId，服务端会创建新会话；
 * 如果传入sessionId，会继续该会话的上下文。
 *
 * @author Spring AI Demo
 * @since 1.0.0
 */
public class ConversationRequest {

    /**
     * 用户输入的消息
     */
    private String message;

    /**
     * 会话ID（可选）
     * 用于支持多轮对话上下文，格式为 conv-{uuid}
     */
    private String sessionId;

    /**
     * 默认构造函数
     */
    public ConversationRequest() {
    }

    /**
     * 全参构造函数
     *
     * @param message   用户消息
     * @param sessionId 会话ID（可选）
     */
    public ConversationRequest(String message, String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
    }

    /**
     * 获取用户消息
     *
     * @return 用户输入的消息内容
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置用户消息
     *
     * @param message 用户消息
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取会话ID
     *
     * @return 会话ID，可能为null
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话ID
     *
     * @param sessionId 会话ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}