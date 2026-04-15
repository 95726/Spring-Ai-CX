package com.example.springai.controller;

import com.example.springai.dto.ChatRequest;
import com.example.springai.dto.ChatResponse;
import com.example.springai.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {


    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String message) {
        return chatService.chatStream(message)
                .map(chunk -> "data:" + chunk + "\n\n")
                .concatWith(Flux.just("data:[DONE]\n\n"))
                .delayElements(Duration.ofMillis(10));
    }
}