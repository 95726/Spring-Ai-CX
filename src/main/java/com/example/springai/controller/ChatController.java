package com.example.springai.controller;

import com.example.springai.dto.ChatRequest;
import com.example.springai.dto.ChatResponse;
import com.example.springai.dto.ConversationRequest;
import com.example.springai.dto.MessageDTO;
import com.example.springai.service.ChatService;
import com.example.springai.service.ConversationService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {


    private final ChatService chatService;
    private final ConversationService conversationService;

    public ChatController(ChatService chatService, ConversationService conversationService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
    }

    /**
     * 问答接口 - POST请求
     * @param request 包含用户消息的请求体
     * @return AI响应
     */
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.getMessage());
    }

    /**
     * 简单问答接口 - GET请求
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
     * 使用 Server-Sent Events (SSE) 方式返回流式响应，
     * AI 生成的每个 token 会实时推送给客户端。
     * 适用于需要实时展示回复进度的场景（如聊天界面）。
     *
     * 注意：使用 UTF-8 编码确保中文等非 ASCII 字符正确显示
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
     * 如果请求中不包含sessionId，会创建新会话；
     * 如果包含sessionId，会继续该会话的上下文。
     *
     * SSE响应格式：
     * - 每个数据片段以 "data:内容\n\n" 格式发送
     * - 流结束时发送 "data:[DONE]\n\n"
     * - 最后发送会话ID "data:{"sessionId":"..."}\n\n"
     *
     * @param request 包含用户消息和可选sessionId的请求体
     * @return Flux<String> 流式响应，格式为SSE
     */
    @PostMapping("/stream/context")
    public Flux<String> chatStreamWithContext(@RequestBody ConversationRequest request) {
        String sessionId = request.getSessionId();

        // 如果没有sessionId或sessionId不存在，创建新会话
        if (sessionId == null || sessionId.isEmpty() || !conversationService.sessionExists(sessionId)) {
            sessionId = conversationService.createSession();
        }

        // 保存用户消息到Redis
        conversationService.addMessage(sessionId, MessageDTO.userMessage(request.getMessage()));

        // 获取历史消息用于构建上下文
        List<MessageDTO> historyMessages = conversationService.getMessages(sessionId);

        // 用于收集完整的AI响应
        StringBuilder fullResponse = new StringBuilder();
        String finalSessionId = sessionId;

        // 调用流式服务，收集响应内容并保存AI消息
        return chatService.chatStreamWithContext(request.getMessage(), historyMessages)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    // 流式完成后，保存完整的AI响应到Redis
                    conversationService.addMessage(finalSessionId, MessageDTO.assistantMessage(fullResponse.toString()));
                })
                .map(chunk -> "data:" + chunk + "\n\n")
                .concatWith(Flux.just("data:[DONE]\n\n"))
                .concatWith(Flux.just("data:{\"sessionId\":\"" + finalSessionId + "\"}\n\n"))
                .delayElements(Duration.ofMillis(10));
    }
}