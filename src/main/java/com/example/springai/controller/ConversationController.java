package com.example.springai.controller;

import com.example.springai.dto.MessageDTO;
import com.example.springai.entity.AiUser;
import com.example.springai.service.ConversationService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话管理控制器
 *
 * 提供会话历史查询、删除等功能的REST接口。
 * 所有会话与用户绑定，只有登录用户才能访问自己的会话。
 */
@RestController
@RequestMapping("/api/conversation")
@CrossOrigin(origins = "*")
public class ConversationController {

    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);

    private static final String SESSION_USER_KEY = "currentUser";

    private final ConversationService conversationService;

    /**
     * 构造函数
     *
     * @param conversationService 会话管理服务
     */
    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * 获取指定会话的历史消息
     *
     * 验证会话是否属于当前用户，返回会话详情。
     *
     * @param sessionId 会话ID，格式为 conv-{uuid}
     * @param session HTTP会话，用于获取当前登录用户
     * @return ResponseEntity 包含会话详情的响应
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getConversation(@PathVariable String sessionId, HttpSession session) {
        log.info("获取会话详情: {}", sessionId);

        // 获取当前登录用户
        AiUser currentUser = (AiUser) session.getAttribute(SESSION_USER_KEY);
        if (currentUser == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "未登录");
            return ResponseEntity.status(401).body(response);
        }

        // 验证会话是否属于该用户
        if (!conversationService.sessionExistsAndBelongsToUser(sessionId, currentUser.getId())) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "会话不存在");
            return ResponseEntity.status(404).body(response);
        }

        // 获取消息列表
        List<MessageDTO> messages = conversationService.getMessages(sessionId);
        // 获取元数据
        Map<String, String> meta = conversationService.getMetaData(sessionId);

        // 构建响应对象
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("messages", messages);

        // 如果元数据存在，添加元数据信息
        if (!meta.isEmpty()) {
            response.put("createdAt", Long.parseLong(meta.getOrDefault("createdAt", "0")));
            response.put("lastActiveAt", Long.parseLong(meta.getOrDefault("lastActiveAt", "0")));
            response.put("messageCount", Integer.parseInt(meta.getOrDefault("messageCount", "0")));
            response.put("userId", meta.getOrDefault("userId", ""));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户的所有会话列表
     *
     * 返回当前登录用户的会话列表，每个会话包含：
     * - sessionId: 会话ID
     * - lastActiveAt: 最后活跃时间戳
     * - messageCount: 消息总数
     * - preview: 最后一条消息的预览（最多50字符）
     *
     * @param session HTTP会话，用于获取当前登录用户
     * @return ResponseEntity 包含会话列表的响应
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getUserConversations(HttpSession session) {
        log.info("获取用户会话列表");

        // 获取当前登录用户
        AiUser currentUser = (AiUser) session.getAttribute(SESSION_USER_KEY);
        if (currentUser == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "未登录");
            return ResponseEntity.status(401).body(response);
        }

        List<Map<String, Object>> conversations = conversationService.getUserConversations(currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("conversations", conversations);

        return ResponseEntity.ok(response);
    }

    /**
     * 删除指定会话
     *
     * 验证会话是否属于当前用户，删除会话的所有数据。
     *
     * @param sessionId 会话ID，格式为 conv-{uuid}
     * @param session HTTP会话，用于获取当前登录用户
     * @return ResponseEntity 包含删除结果的响应
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteConversation(@PathVariable String sessionId, HttpSession session) {
        log.info("删除会话: {}", sessionId);

        // 获取当前登录用户
        AiUser currentUser = (AiUser) session.getAttribute(SESSION_USER_KEY);
        if (currentUser == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "未登录");
            return ResponseEntity.status(401).body(response);
        }

        // 验证会话是否属于该用户
        if (!conversationService.sessionExistsAndBelongsToUser(sessionId, currentUser.getId())) {
            // 检查会话是否真的存在（可能是数据过期被Redis清理了）
            if (conversationService.sessionExists(sessionId)) {
                // 会话存在但不属于该用户
                Map<String, Object> response = new HashMap<>();
                response.put("error", "会话不存在");
                return ResponseEntity.status(404).body(response);
            }
            // 会话不存在（已过期），清理用户列表中的残留引用
            String userConversationsKey = "user:conversations:" + currentUser.getId();
            conversationService.getRedisTemplate().opsForSet().remove(userConversationsKey, sessionId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "会话已删除");
            return ResponseEntity.ok(response);
        }

        boolean success = conversationService.deleteSession(sessionId, currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "会话已删除" : "会话不存在");

        return ResponseEntity.ok(response);
    }
}