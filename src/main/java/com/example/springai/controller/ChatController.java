package com.example.springai.controller;

import com.example.springai.dto.ChatRequest;
import com.example.springai.dto.ChatResponse;
import com.example.springai.dto.ConversationRequest;
import com.example.springai.dto.MessageDTO;
import com.example.springai.entity.AiUser;
import com.example.springai.service.ChatService;
import com.example.springai.service.ConversationService;
import com.example.springai.service.MarkdownService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * 聊天控制器
 *
 * 提供与AI模型交互的REST接口，支持普通请求和流式请求。
 * 会话与当前登录用户绑定。
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final String SESSION_USER_KEY = "currentUser";

    private final ChatService chatService;
    private final ConversationService conversationService;
    private final MarkdownService markdownService;

    public ChatController(ChatService chatService, ConversationService conversationService, MarkdownService markdownService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
        this.markdownService = markdownService;
    }

    /**
     * 问答接口 - POST请求
     *
     * @param request 包含用户消息的请求体
     * @return AI响应
     */
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.getMessage());
    }

    /**
     * 简单问答接口 - GET请求
     *
     * @param message 用户消息
     * @return AI响应字符串
     */
    @GetMapping
    public String chatSimple(@RequestParam String message) {
        return chatService.chatSimple(message);
    }

    /**
     * 流式问答接口 - GET请求
     *
     * 使用Server-Sent Events (SSE)方式返回流式响应，
     * AI生成的每个token会实时推送给客户端。
     *
     * @param message 用户消息
     * @return Flux<String> 流式响应
     */
    @GetMapping("/stream")
    public Flux<String> chatStream(@RequestParam String message) {
        return chatService.chatStream(message)
                .map(chunk -> "data:" + chunk + "\n\n")
                .concatWith(Flux.just("data:[DONE]\n\n"))
                .delayElements(Duration.ofMillis(10));
    }

    /**
     * 带上下文的流式问答接口 - POST请求
     *
     * 支持多轮对话，历史消息存储在Redis中。
     * 会话与当前登录用户绑定，只有登录用户才能访问自己的会话。
     * 如果请求中不包含sessionId，会创建新会话；
     * 如果包含sessionId，会验证会话是否属于当前用户。
     *
     * @param request 包含用户消息和可选sessionId的请求体
     * @param session HTTP会话，用于获取当前登录用户
     * @return Flux<String> 流式响应，格式为SSE
     */
    @PostMapping("/stream/context")
    public Flux<String> chatStreamWithContext(@RequestBody ConversationRequest request, HttpSession session) {
        // 获取当前登录用户
        AiUser currentUser = (AiUser) session.getAttribute(SESSION_USER_KEY);
        if (currentUser == null) {
            return Flux.just("data:{\"error\":\"未登录\"}\n\n");
        }

        String userId = currentUser.getId();
        String sessionId = request.getSessionId();

        // 如果没有sessionId或sessionId不存在或不属于该用户，创建新会话
        if (sessionId == null || sessionId.isEmpty() ||
                !conversationService.sessionExistsAndBelongsToUser(sessionId, userId)) {
            sessionId = conversationService.createSession(userId);
        }

        // 保存用户消息到Redis
        conversationService.addMessage(sessionId, MessageDTO.userMessage(request.getMessage()));

        // 获取历史消息用于构建上下文
        List<MessageDTO> historyMessages = conversationService.getMessages(sessionId);

        // 用于收集完整的AI响应
        StringBuilder fullResponse = new StringBuilder();
        String finalSessionId = sessionId;

        // 调用流式服务，收集响应内容并保存AI消息
        // 流式返回markdown片段给前端，前端使用marked.parse()实时渲染
        // 历史记录中同时保存HTML版本和原始markdown
        return chatService.chatStreamWithContext(request.getMessage(), historyMessages)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    // 流式完成后，保存 markdown 原文到历史记录
                    // content: markdown原文（前端用marked.parse渲染显示）
                    // originalContent: 保持与content一致，用于AI上下文
                    conversationService.addMessage(finalSessionId, MessageDTO.assistantMessage(fullResponse.toString(), fullResponse.toString()));
                    log.info("AI响应已保存，Markdown长度: {}", fullResponse.length());
                })
                // 流式返回markdown原文给前端，前端使用marked.parse()实时渲染
                .map(chunk -> "data:" + chunk + "\n\n")
                .concatWith(Flux.just("data:[DONE]\n\n"))
                .concatWith(Flux.just("data:{\"sessionId\":\"" + finalSessionId + "\"}\n\n"))
                .delayElements(Duration.ofMillis(10));
    }
}