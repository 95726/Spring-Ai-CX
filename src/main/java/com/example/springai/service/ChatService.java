package com.example.springai.service;

import com.example.springai.dto.ChatResponse;
import com.example.springai.dto.MessageDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天服务类
 *
 * 封装了对 AI 模型的调用逻辑，使用 Spring AI 的 ChatClient 与 AI 模型进行交互。
 * 对于流式请求，使用自定义的 WebClient 来避免 MiniMax API 与 Spring AI 的兼容性问题。
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final MarkdownService markdownService;

    @Value("${spring.ai.openai.base-url:http://192.168.40.113:8088}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:mimo-v2.5-pro}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:1000}")
    private Integer maxTokens;

    @Value("${spring.ai.chat.system-prompt:你是一个有帮助的AI助手，请简洁明了地回答问题，避免冗余内容。}")
    private String systemPrompt;

    public ChatService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper, MarkdownService markdownService) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.markdownService = markdownService;
    }

    /**
     * 发送聊天消息并返回完整的响应对象
     *
     * 将用户消息发送给 AI 模型，获取回复后封装为 ChatResponse 对象返回。
     * 适用于需要获取额外信息（如模型名称、token 使用量等）的场景。
     *
     * @param message 用户输入的聊天消息
     * @return ChatResponse 包含 AI 回复、模型名称和 token 使用量的响应对象
     */
    public ChatResponse chat(String message) {
        String response = chatClient.prompt()
                .user(message)
                .call()
                .content();

        return new ChatResponse(response, model, 0L);
    }

    /**
     * 发送聊天消息并返回简单的字符串响应
     *
     * 这是 chat() 方法的简化版本，直接返回 AI 模型的回复文本，
     * 不包含任何额外的元数据信息。
     *
     * 适用于只需要获取 AI 回复内容、不关心模型信息或 token 使用量的场景。
     *
     * @param message 用户输入的聊天消息
     * @return String AI 模型的回复内容
     */
    public String chatSimple(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 对历史消息生成摘要
     *
     * 将消息列表格式化为文本，调用AI模型生成简洁的对话摘要。
     * 摘要用于在上下文窗口有限时保留早期对话的关键信息。
     *
     * @param messages 需要摘要的历史消息列表
     * @return AI生成的摘要文本
     */
    public String summarizeMessages(List<MessageDTO> messages) {
        // 格式化消息为文本
        StringBuilder conversationText = new StringBuilder();
        for (MessageDTO msg : messages) {
            String roleLabel = "user".equals(msg.getRole()) ? "用户" : "助手";
            String content = (msg.getOriginalContent() != null && !msg.getOriginalContent().isEmpty())
                    ? msg.getOriginalContent()
                    : msg.getContent();
            conversationText.append(roleLabel).append(": ").append(content).append("\n");
        }

        // 构建摘要prompt
        String prompt = "请将以下对话历史总结为一段简洁的摘要（200字以内），保留关键信息、用户需求和重要结论：\n\n"
                + conversationText.toString();

        log.info("正在生成对话摘要，消息数: {}", messages.size());
        String summary = chatSimple(prompt);
        log.info("对话摘要生成完成，摘要长度: {}", summary.length());
        return summary;
    }

    /**
     * 发送聊天消息并以流式方式返回响应
     *
     * 使用自定义 WebClient 直接调用 OpenAI 兼容的 API，避免 Spring AI
     * 对 MiniMax API 流式响应格式解析的兼容性问题。
     *
     * 流式响应格式：每个 chunk 返回 "data: {...}" 格式的 SSE 数据，
     * 解析其中的 content 字段并实时推送给客户端。
     *
     * @param message 用户输入的聊天消息
     * @return Flux<String> 流式响应，每个元素代表一个文本片段
     */
    public Flux<String> chatStream(String message) {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", true);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", message)
        ));

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        return webClient.post()
                .uri("/v1/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(line -> System.out.println("收到原始数据: " + line))  // 调试日志
                .filter(line -> line != null && !line.isEmpty())
                .filter(line -> !line.equals("data: [DONE]"))
                .mapNotNull(line -> {
                    try {
                        // 处理 SSE 格式数据
                        String jsonStr = line;
                        if (line.startsWith("data:")) {
                            jsonStr = line.substring(5).trim();
                        }

                        if (jsonStr.isEmpty() || jsonStr.equals("[DONE]")) {
                            return null;
                        }

                        JsonNode jsonNode = objectMapper.readTree(jsonStr);
                        JsonNode choices = jsonNode.path("choices");
                        if (choices.isArray() && choices.size() > 0) {
                            JsonNode choice = choices.get(0);
                            // 尝试 delta 或 message 字段
                            JsonNode contentNode = choice.path("delta").path("content");
                            if (contentNode.isMissingNode() || contentNode.isNull()) {
                                contentNode = choice.path("message").path("content");
                            }
                            if (!contentNode.isMissingNode() && contentNode.isTextual()) {
                                String content = contentNode.asText();
                                System.out.println("解析内容: " + content);  // 谧试日志
                                return content;
                            }
                        }
                        return null;
                    } catch (Exception e) {
                        System.out.println("解析错误: " + e.getMessage() + ", 行: " + line);
                        return null;
                    }
                })
                .filter(content -> content != null && !content.isEmpty())
                .doOnError(error -> System.out.println("流式请求错误: " + error.getMessage()))
                .doOnComplete(() -> System.out.println("流式请求完成"));
    }

    /**
     * 发送带上下文的聊天消息并以流式方式返回响应
     *
     * 构建包含历史消息的完整请求体，发送给AI模型并实时解析流式响应。
     * 历史消息用于保持多轮对话的上下文连贯性。
     * 如果提供摘要，会将其作为system消息插入到消息列表最前面。
     *
     * @param message         用户输入的聊天消息
     * @param historyMessages 历史消息列表，包含之前的用户和助手消息
     * @param summary         对话摘要，可为null
     * @return Flux<String> 流式响应，每个元素代表一个文本片段
     */
    public Flux<String> chatStreamWithContext(String message, List<MessageDTO> historyMessages, String summary) {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", true);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        // 构建消息列表：先添加系统提示词，再添加摘要，再添加历史消息，最后添加当前用户消息
        List<Map<String, String>> messages = new ArrayList<>();

        // 添加系统提示词，引导模型控制回复长度
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        // 如果有摘要，作为system消息插入最前面
        if (summary != null && !summary.isEmpty()) {
            messages.add(Map.of("role", "system", "content", "以下是之前对话的摘要：\n" + summary));
        }

        if (historyMessages != null && !historyMessages.isEmpty()) {
            // 将历史消息转换为API所需格式，优先使用originalContent（markdown原文）发送给AI
            messages.addAll(historyMessages.stream()
                    .map(m -> {
                        String contentToSend = (m.getOriginalContent() != null && !m.getOriginalContent().isEmpty())
                                ? m.getOriginalContent()
                                : m.getContent();
                        return Map.of("role", m.getRole(), "content", contentToSend);
                    })
                    .collect(Collectors.toList()));
        }
        // 添加当前用户消息
        messages.add(Map.of("role", "user", "content", message));
        requestBody.put("messages", messages);

        log.info("发送带上下文的流式请求，历史消息数: {}, 当前消息: {}",
                historyMessages != null ? historyMessages.size() : 0, message);

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        return webClient.post()
                .uri("/v1/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(line -> log.debug("收到原始数据: {}", line))
                .filter(line -> line != null && !line.isEmpty())
                .filter(line -> !line.equals("data: [DONE]"))
                .mapNotNull(line -> {
                    try {
                        // 处理 SSE 格式数据，提取JSON内容
                        String jsonStr = line;
                        if (line.startsWith("data:")) {
                            jsonStr = line.substring(5).trim();
                        }

                        if (jsonStr.isEmpty() || jsonStr.equals("[DONE]")) {
                            return null;
                        }

                        // 解析JSON获取content字段
                        JsonNode jsonNode = objectMapper.readTree(jsonStr);
                        JsonNode choices = jsonNode.path("choices");
                        if (choices.isArray() && choices.size() > 0) {
                            JsonNode choice = choices.get(0);
                            // 优先尝试delta字段（流式格式），其次尝试message字段（非流式格式）
                            JsonNode contentNode = choice.path("delta").path("content");
                            if (contentNode.isMissingNode() || contentNode.isNull()) {
                                contentNode = choice.path("message").path("content");
                            }
                            if (!contentNode.isMissingNode() && contentNode.isTextual()) {
                                String content = contentNode.asText();
                                log.debug("解析内容: {}", content);
                                return content;
                            }
                        }
                        return null;
                    } catch (Exception e) {
                        log.warn("解析错误: {}, 行: {}", e.getMessage(), line);
                        return null;
                    }
                })
                .filter(content -> content != null && !content.isEmpty())
                .doOnError(error -> log.error("流式请求错误: {}", error.getMessage()))
                .doOnComplete(() -> log.info("带上下文的流式请求完成"));
    }
}