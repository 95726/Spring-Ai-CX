package com.example.springai.dto;

/**
 * 登录请求DTO
 *
 * 包含用户登录所需的用户名和密码
 */
public class LoginRequest {

    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}