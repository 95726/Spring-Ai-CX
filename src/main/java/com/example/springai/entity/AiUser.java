package com.example.springai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI用户实体类
 *
 * 对应数据库表 ai_user，存储用户基本信息
 */
@Data
@TableName("AI_USER")
public class AiUser {

    /**
     * 主键ID（32位UUID）
     */
    @TableId
    private String id;

    /**
     * 用户编号
     */
    private String userBh;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 电话号码
     */
    private String phone;

    /**
     * 用户状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    private String createName;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    private String updateName;
}