package com.example.springai.dto;

/**
 * 认证响应DTO
 *
 * 包含认证操作的结果信息
 */
public class AuthResponse {

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 提示消息
     */
    private String message;

    /**
     * 用户名（登录成功时返回）
     */
    private String userName;

    /**
     * 用户ID（登录成功时返回）
     */
    private String userId;

    public AuthResponse() {
    }

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuthResponse(boolean success, String message, String userName, String userId) {
        this.success = success;
        this.message = message;
        this.userName = userName;
        this.userId = userId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}