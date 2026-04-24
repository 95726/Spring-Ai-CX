package com.example.springai.dto;

/**
 * 注册请求DTO
 *
 * 包含用户注册所需的各项信息
 */
public class RegisterRequest {

    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 电话号码
     */
    private String phone;

    public RegisterRequest() {
    }

    public RegisterRequest(String userName, String password, String phone) {
        this.userName = userName;
        this.password = password;
        this.phone = phone;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}