package com.example.springai.controller;

import com.example.springai.dto.MessageDTO;
import com.example.springai.service.ConversationService;
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
 * 所有接口支持跨域访问，符合阿里巴巴Java开发规范的Controller层设计。
 *
 * @author Spring AI Demo
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/conversation")
@CrossOrigin(origins = "*")
public class ConversationController {

    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);

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
     * 返回会话详情，包括消息列表、创建时间、最后活跃时间和消息总数。
     *
     * @param sessionId 会话ID，格式为 conv-{uuid}
     * @return ResponseEntity 包含会话详情的响应
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getConversation(@PathVariable String sessionId) {
        log.info("获取会话详情: {}", sessionId);

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
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有会话列表
     *
     * 返回所有会话的简要信息列表，每个会话包含：
     * - sessionId: 会话ID
     * - lastActiveAt: 最后活跃时间戳
     * - messageCount: 消息总数
     * - preview: 最后一条消息的预览（最多50字符）
     *
     * @return ResponseEntity 包含会话列表的响应
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllConversations() {
        log.info("获取所有会话列表");

        List<Map<String, Object>> conversations = conversationService.getAllConversations();
        Map<String, Object> response = new HashMap<>();
        response.put("conversations", conversations);

        return ResponseEntity.ok(response);
    }

    /**
     * 删除指定会话
     *
     * 删除会话的所有数据，包括消息历史和元数据。
     *
     * @param sessionId 会话ID，格式为 conv-{uuid}
     * @return ResponseEntity 包含删除结果的响应
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteConversation(@PathVariable String sessionId) {
        log.info("删除会话: {}", sessionId);

        boolean success = conversationService.deleteSession(sessionId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "会话已删除" : "会话不存在");

        return ResponseEntity.ok(response);
    }
}