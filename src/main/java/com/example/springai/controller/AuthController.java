package com.example.springai.controller;

import com.example.springai.dto.AuthResponse;
import com.example.springai.dto.LoginRequest;
import com.example.springai.dto.RegisterRequest;
import com.example.springai.entity.AiUser;
import com.example.springai.service.AiUserService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * 提供用户登录、注册、登出等认证相关的REST接口
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /**
     * Session中存储当前用户的Key
     */
    private static final String SESSION_USER_KEY = "currentUser";

    @Autowired
    private AiUserService aiUserService;

    /**
     * 用户注册
     *
     * 注册新用户，用户名不能重复。
     *
     * @param request 注册请求，包含用户名、密码和电话号码
     * @return 注册结果
     */
    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        log.info("用户注册请求: userName={}", request.getUserName());

        try {
            // 调用注册服务
            AiUser user = aiUserService.register(
                    request.getUserName(),
                    request.getPassword(),
                    request.getPhone()
            );

            log.info("用户注册成功: userId={}, userName={}", user.getId(), user.getUserName());
            return new AuthResponse(true, "注册成功", user.getUserName(), user.getId());
        } catch (RuntimeException e) {
            log.warn("用户注册失败: {}", e.getMessage());
            return new AuthResponse(false, e.getMessage());
        }
    }

    /**
     * 用户登录
     *
     * 验证用户名和密码，登录成功后将用户信息存入Session。
     *
     * @param request 登录请求，包含用户名和密码
     * @param session HTTP会话
     * @return 登录结果
     */
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request, HttpSession session) {
        log.info("用户登录请求: userName={}", request.getUserName());

        try {
            // 调用登录服务
            AiUser user = aiUserService.login(request.getUserName(), request.getPassword());

            // 将用户信息存入Session
            session.setAttribute(SESSION_USER_KEY, user);

            log.info("用户登录成功: userId={}, userName={}", user.getId(), user.getUserName());
            return new AuthResponse(true, "登录成功", user.getUserName(), user.getId());
        } catch (RuntimeException e) {
            log.warn("用户登录失败: {}", e.getMessage());
            return new AuthResponse(false, e.getMessage());
        }
    }

    /**
     * 用户登出
     *
     * 清除Session中的用户信息。
     *
     * @param session HTTP会话
     * @return 登出结果
     */
    @PostMapping("/logout")
    public AuthResponse logout(HttpSession session) {
        // 获取当前用户信息用于日志
        AiUser currentUser = (AiUser) session.getAttribute(SESSION_USER_KEY);
        if (currentUser != null) {
            log.info("用户登出: userId={}, userName={}", currentUser.getId(), currentUser.getUserName());
        }

        // 清除Session
        session.invalidate();

        return new AuthResponse(true, "登出成功");
    }

    /**
     * 获取当前登录用户
     *
     * 从Session中获取当前登录的用户信息。
     *
     * @param session HTTP会话
     * @return 当前用户信息，未登录则返回失败响应
     */
    @GetMapping("/current")
    public AuthResponse getCurrentUser(HttpSession session) {
        // 从Session获取用户
        AiUser user = (AiUser) session.getAttribute(SESSION_USER_KEY);

        if (user == null) {
            return new AuthResponse(false, "未登录");
        }

        return new AuthResponse(true, "已登录", user.getUserName(), user.getId());
    }
}