package com.example.springai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springai.entity.AiUser;

/**
 * AI用户服务接口
 *
 * 继承MyBatis-Plus的IService，提供基础CRUD及扩展方法
 */
public interface AiUserService extends IService<AiUser> {

    /**
     * 用户注册
     *
     * 注册新用户，密码使用BCrypt加密存储
     *
     * @param userName 用户名
     * @param password 密码
     * @param phone 电话号码
     * @return 注册成功后的用户对象
     * @throws RuntimeException 如果用户名已存在
     */
    AiUser register(String userName, String password, String phone);

    /**
     * 用户登录
     *
     * 验证用户名和密码，返回用户信息
     *
     * @param userName 用户名
     * @param password 密码
     * @return 登录成功的用户对象
     * @throws RuntimeException 如果用户名不存在或密码错误
     */
    AiUser login(String userName, String password);

    /**
     * 根据用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象，不存在则返回null
     */
    AiUser findByUserName(String userName);
}