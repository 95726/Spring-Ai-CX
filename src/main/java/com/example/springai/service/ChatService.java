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

    @Value("${spring.ai.openai.base-url:http://192.168.40.113:8088}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:MiniMax-M2.7-highspeed}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:1000}")
    private Integer maxTokens;

    public ChatService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
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
     *
     * @param message         用户输入的聊天消息
     * @param historyMessages 历史消息列表，包含之前的用户和助手消息
     * @return Flux<String> 流式响应，每个元素代表一个文本片段
     */
    public Flux<String> chatStreamWithContext(String message, List<MessageDTO> historyMessages) {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", true);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        // 构建消息列表：先添加历史消息，再添加当前用户消息
        List<Map<String, String>> messages = new ArrayList<>();
        if (historyMessages != null && !historyMessages.isEmpty()) {
            // 将历史消息转换为API所需格式
            messages.addAll(historyMessages.stream()
                    .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
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