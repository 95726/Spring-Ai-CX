package com.example.springai.dto;

import java.io.Serializable;

/**
 * 消息数据传输对象
 *
 * 用于表示聊天会话中的单条消息，包含角色、内容和时间戳。
 * 符合阿里巴巴Java开发规范的POJO定义。
 *
 * @author Spring AI Demo
 * @since 1.0.0
 */
public class MessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息角色：user(用户) 或 assistant(助手)
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 默认构造函数
     */
    public MessageDTO() {
    }

    /**
     * 全参构造函数
     *
     * @param role      消息角色
     * @param content   消息内容
     * @param timestamp 消息时间戳
     */
    public MessageDTO(String role, String content, Long timestamp) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    /**
     * 创建用户消息的静态工厂方法
     *
     * 自动设置角色为"user"，时间戳为当前时间。
     *
     * @param content 消息内容
     * @return 用户消息对象
     */
    public static MessageDTO userMessage(String content) {
        return new MessageDTO("user", content, System.currentTimeMillis());
    }

    /**
     * 创建助手消息的静态工厂方法
     *
     * 自动设置角色为"assistant"，时间戳为当前时间。
     *
     * @param content 消息内容
     * @return 助手消息对象
     */
    public static MessageDTO assistantMessage(String content) {
        return new MessageDTO("assistant", content, System.currentTimeMillis());
    }

    /**
     * 获取消息角色
     *
     * @return 消息角色（user或assistant）
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置消息角色
     *
     * @param role 消息角色
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取消息内容
     *
     * @return 消息内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息内容
     *
     * @param content 消息内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取消息时间戳
     *
     * @return 消息时间戳（毫秒）
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * 设置消息时间戳
     *
     * @param timestamp 消息时间戳
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}