package com.example.springai.service;

import com.example.springai.dto.MessageDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 会话管理服务
 *
 * 提供会话的创建、查询、存储和删除功能，使用Redis作为持久化存储。
 * 会话消息存储在Redis List中，元数据存储在Redis Hash中。
 * 会话与用户绑定，支持按用户查询会话列表。
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    /** 用户会话列表Key前缀 */
    private static final String USER_CONVERSATIONS_PREFIX = "user:conversations:";

    /** 会话消息Key前缀 */
    private static final String CONVERSATION_PREFIX = "conversation:";

    /** 会话元数据Key后缀 */
    private static final String META_SUFFIX = ":meta";

    /** 会话消息Key后缀 */
    private static final String MESSAGES_SUFFIX = ":messages";

    /** 会话过期时间：24小时 */
    private static final long CONVERSATION_TTL_HOURS = 24;

    /** Session ID前缀 */
    private static final String SESSION_ID_PREFIX = "conv-";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     *
     * @param redisTemplate Redis操作模板
     * @param objectMapper  JSON序列化工具
     */
    public ConversationService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建新会话并绑定用户
     *
     * 生成唯一的会话ID（格式：conv-{uuid}），并初始化会话元数据存储到Redis。
     * 同时将会话ID添加到用户的会话列表中。
     *
     * @param userId 用户ID
     * @return 新创建的会话ID
     */
    public String createSession(String userId) {
        String sessionId = SESSION_ID_PREFIX + UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        // 初始化元数据并存入Redis Hash，包含用户ID
        String metaKey = getMetaKey(sessionId);
        Map<String, String> meta = new HashMap<>();
        meta.put("createdAt", String.valueOf(now));
        meta.put("lastActiveAt", String.valueOf(now));
        meta.put("messageCount", "0");
        meta.put("userId", userId);
        redisTemplate.opsForHash().putAll(metaKey, meta);
        redisTemplate.expire(metaKey, CONVERSATION_TTL_HOURS, TimeUnit.HOURS);

        // 将会话ID添加到用户的会话列表
        String userConversationsKey = getUserConversationsKey(userId);
        redisTemplate.opsForSet().add(userConversationsKey, sessionId);
        redisTemplate.expire(userConversationsKey, CONVERSATION_TTL_HOURS, TimeUnit.HOURS);

        log.info("创建新会话: {}, 用户ID: {}", sessionId, userId);
        return sessionId;
    }

    /**
     * 添加消息到会话
     *
     * 将用户或助手的消息添加到会话历史记录中，序列化为JSON后存入Redis List，
     * 同时更新会话元数据中的消息计数和最后活跃时间。
     *
     * @param sessionId 会话ID
     * @param message   消息对象
     * @throws IllegalArgumentException 如果会话ID为空
     * @throws RuntimeException         如果消息序列化失败
     */
    public void addMessage(String sessionId, MessageDTO message) {
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }

        String messagesKey = getMessagesKey(sessionId);
        String metaKey = getMetaKey(sessionId);

        try {
            // 序列化消息并存入Redis List（从右侧添加）
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(messagesKey, messageJson);
            redisTemplate.expire(messagesKey, CONVERSATION_TTL_HOURS, TimeUnit.HOURS);

            // 更新元数据：增加消息计数、更新最后活跃时间
            redisTemplate.opsForHash().increment(metaKey, "messageCount", 1);
            redisTemplate.opsForHash().put(metaKey, "lastActiveAt", String.valueOf(System.currentTimeMillis()));
            redisTemplate.expire(metaKey, CONVERSATION_TTL_HOURS, TimeUnit.HOURS);

            // 更新用户会话列表过期时间
            String userId = getMetaData(sessionId).get("userId");
            if (userId != null) {
                String userConversationsKey = getUserConversationsKey(userId);
                redisTemplate.expire(userConversationsKey, CONVERSATION_TTL_HOURS, TimeUnit.HOURS);
            }

            log.debug("添加消息到会话 {}: role={}, content长度={}", sessionId, message.getRole(), message.getContent().length());
        } catch (JsonProcessingException e) {
            log.error("消息序列化失败: {}", e.getMessage());
            throw new RuntimeException("消息序列化失败", e);
        }
    }

    /**
     * 获取会话历史消息
     *
     * 从Redis List中获取指定会话的所有历史消息，反序列化后返回消息列表。
     *
     * @param sessionId 会话ID
     * @return 消息列表，如果会话不存在或无消息则返回空列表
     */
    public List<MessageDTO> getMessages(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return Collections.emptyList();
        }

        String messagesKey = getMessagesKey(sessionId);
        List<String> messageJsonList = redisTemplate.opsForList().range(messagesKey, 0, -1);

        if (messageJsonList == null || messageJsonList.isEmpty()) {
            return Collections.emptyList();
        }

        // 反序列化每条消息
        return messageJsonList.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, MessageDTO.class);
                    } catch (JsonProcessingException e) {
                        log.warn("消息反序列化失败: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取会话元数据
     *
     * 从Redis Hash中获取会话的元数据信息，包括创建时间、最后活跃时间、消息计数和用户ID。
     *
     * @param sessionId 会话ID
     * @return 元数据Map，如果会话不存在则返回空Map
     */
    public Map<String, String> getMetaData(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return Collections.emptyMap();
        }

        String metaKey = getMetaKey(sessionId);
        Map<Object, Object> rawMeta = redisTemplate.opsForHash().entries(metaKey);

        // 转换Object类型为String类型
        Map<String, String> meta = new HashMap<>();
        rawMeta.forEach((k, v) -> meta.put(k.toString(), v.toString()));
        return meta;
    }

    /**
     * 检查会话是否存在且属于指定用户
     *
     * 通过检查元数据Key是否存在来判断会话是否有效，同时验证用户归属。
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return true如果会话存在且属于该用户，false如果不存在或不属于该用户
     */
    public boolean sessionExistsAndBelongsToUser(String sessionId, String userId) {
        if (sessionId == null || sessionId.isEmpty() || userId == null) {
            return false;
        }

        // 检查会话是否存在
        String metaKey = getMetaKey(sessionId);
        Boolean exists = redisTemplate.hasKey(metaKey);
        if (!Boolean.TRUE.equals(exists)) {
            return false;
        }

        // 检查会话是否属于该用户
        Map<String, String> meta = getMetaData(sessionId);
        String sessionUserId = meta.get("userId");
        return userId.equals(sessionUserId);
    }

    /**
     * 检查会话是否存在
     *
     * 通过检查元数据Key是否存在来判断会话是否有效。
     *
     * @param sessionId 会话ID
     * @return true如果会话存在，false如果不存在
     */
    public boolean sessionExists(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        String metaKey = getMetaKey(sessionId);
        Boolean exists = redisTemplate.hasKey(metaKey);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 删除会话
     *
     * 删除指定会话的所有数据，包括消息历史（Redis List）、元数据（Redis Hash），
     * 以及用户会话列表中的会话ID。
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return true如果删除成功，false如果会话ID为空
     */
    public boolean deleteSession(String sessionId, String userId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }

        String messagesKey = getMessagesKey(sessionId);
        String metaKey = getMetaKey(sessionId);

        redisTemplate.delete(messagesKey);
        redisTemplate.delete(metaKey);

        // 从用户会话列表中移除
        if (userId != null) {
            String userConversationsKey = getUserConversationsKey(userId);
            redisTemplate.opsForSet().remove(userConversationsKey, sessionId);
        }

        log.info("删除会话: {}, 用户ID: {}", sessionId, userId);
        return true;
    }

    /**
     * 获取用户的会话列表
     *
     * 从Redis Set中获取用户的所有会话ID，返回简要信息列表，
     * 包括最后活跃时间、消息数量和最后一条消息预览。
     *
     * @param userId 用户ID
     * @return 会话列表，按最后活跃时间倒序排列
     */
    public List<Map<String, Object>> getUserConversations(String userId) {
        if (userId == null || userId.isEmpty()) {
            return Collections.emptyList();
        }

        String userConversationsKey = getUserConversationsKey(userId);
        Set<String> sessionIds = redisTemplate.opsForSet().members(userConversationsKey);

        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (String sessionId : sessionIds) {
            Map<String, Object> convInfo = new HashMap<>();
            convInfo.put("sessionId", sessionId);

            // 获取元数据
            Map<String, String> meta = getMetaData(sessionId);
            long lastActiveAt = Long.parseLong(meta.getOrDefault("lastActiveAt", "0"));
            int messageCount = Integer.parseInt(meta.getOrDefault("messageCount", "0"));
            convInfo.put("lastActiveAt", lastActiveAt);
            convInfo.put("messageCount", messageCount);

            // 获取最后一条消息作为预览（最多50字符）
            List<MessageDTO> messages = getMessages(sessionId);
            if (!messages.isEmpty()) {
                String lastContent = messages.get(messages.size() - 1).getContent();
                String preview = lastContent.length() > 50 ? lastContent.substring(0, 50) + "..." : lastContent;
                convInfo.put("preview", preview);
            }

            conversations.add(convInfo);
        }

        // 按最后活跃时间倒序排列
        conversations.sort((a, b) -> Long.compare((Long) b.get("lastActiveAt"), (Long) a.get("lastActiveAt")));
        return conversations;
    }

    /**
     * 获取所有会话列表（无用户过滤）
     *
     * 扫描Redis中所有会话消息Key，提取会话ID并返回简要信息列表。
     *
     * @return 会话列表，按最后活跃时间倒序排列
     */
    public List<Map<String, Object>> getAllConversations() {
        // 扫描所有会话消息Key
        Set<String> keys = redisTemplate.keys(CONVERSATION_PREFIX + "*" + MESSAGES_SUFFIX);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (String key : keys) {
            // 从Key中提取sessionId
            String sessionId = extractSessionIdFromKey(key);
            if (sessionId != null) {
                Map<String, Object> convInfo = new HashMap<>();
                convInfo.put("sessionId", sessionId);

                // 获取元数据
                Map<String, String> meta = getMetaData(sessionId);
                long lastActiveAt = Long.parseLong(meta.getOrDefault("lastActiveAt", "0"));
                int messageCount = Integer.parseInt(meta.getOrDefault("messageCount", "0"));
                convInfo.put("lastActiveAt", lastActiveAt);
                convInfo.put("messageCount", messageCount);

                // 获取最后一条消息作为预览（最多50字符）
                List<MessageDTO> messages = getMessages(sessionId);
                if (!messages.isEmpty()) {
                    String lastContent = messages.get(messages.size() - 1).getContent();
                    String preview = lastContent.length() > 50 ? lastContent.substring(0, 50) + "..." : lastContent;
                    convInfo.put("preview", preview);
                }

                conversations.add(convInfo);
            }
        }

        // 按最后活跃时间倒序排列
        conversations.sort((a, b) -> Long.compare((Long) b.get("lastActiveAt"), (Long) a.get("lastActiveAt")));
        return conversations;
    }

    /**
     * 构建用户会话列表Key
     *
     * 格式：user:conversations:{userId}
     *
     * @param userId 用户ID
     * @return Redis Key
     */
    private String getUserConversationsKey(String userId) {
        return USER_CONVERSATIONS_PREFIX + userId;
    }

    /**
     * 构建消息Key
     *
     * 格式：conversation:{sessionId}:messages
     *
     * @param sessionId 会话ID
     * @return Redis Key
     */
    private String getMessagesKey(String sessionId) {
        return CONVERSATION_PREFIX + sessionId + MESSAGES_SUFFIX;
    }

    /**
     * 构建元数据Key
     *
     * 格式：conversation:{sessionId}:meta
     *
     * @param sessionId 会话ID
     * @return Redis Key
     */
    private String getMetaKey(String sessionId) {
        return CONVERSATION_PREFIX + sessionId + META_SUFFIX;
    }

    /**
     * 从Key中提取SessionId
     *
     * 输入格式：conversation:{sessionId}:messages
     * 输出格式：{sessionId}
     *
     * @param key Redis Key
     * @return SessionId，如果格式不匹配则返回null
     */
    private String extractSessionIdFromKey(String key) {
        if (key != null && key.startsWith(CONVERSATION_PREFIX) && key.endsWith(MESSAGES_SUFFIX)) {
            int start = CONVERSATION_PREFIX.length();
            int end = key.length() - MESSAGES_SUFFIX.length();
            if (start < end) {
                return key.substring(start, end);
            }
        }
        return null;
    }
}