package com.example.springai.service;

import com.example.springai.dto.ChatResponse;
import com.example.springai.dto.MessageDTO;
import com.example.springai.utils.WeatherTool;
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
    private final WeatherTool weatherTool;

    @Value("${spring.ai.openai.base-url:http://192.168.40.113:8088}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:qwen3.6-plus}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:1000}")
    private Integer maxTokens;

    @Value("${spring.ai.chat.system-prompt:你是一个有帮助的AI助手，请简洁明了地回答问题，避免冗余内容。}")
    private String systemPrompt;

    public ChatService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper, MarkdownService markdownService, WeatherTool weatherTool) {
        this.chatClient = chatClientBuilder
                .defaultFunctions("getWeather")
                .build();
        this.objectMapper = objectMapper;
        this.markdownService = markdownService;
        this.weatherTool = weatherTool;
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
     * 发送聊天消息并以流式方式返回响应（支持工具调用）
     *
     * 先通过非流式请求检测模型是否需要调用工具，如果需要则执行工具并将结果
     * 作为上下文再次请求模型，最终以流式方式返回给客户端。
     *
     * @param message 用户输入的聊天消息
     * @return Flux<String> 流式响应，每个元素代表一个文本片段
     */
    public Flux<String> chatStream(String message) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", message));

        // 先用非流式请求检测是否需要调用工具
        String toolResult = callWithToolSupport(messages);

        if (toolResult != null) {
            // 工具已执行，把结果加入消息列表，再流式请求最终回答
            messages.add(buildAssistantMessageWithTool(toolResult));
            messages.add(Map.of("role", "tool", "content", toolResult));
        }

        // 流式请求最终回答
        return streamChatCompletion(messages);
    }

    /**
     * 发送带上下文的聊天消息并以流式方式返回响应（支持工具调用）
     *
     * 构建包含历史消息的完整消息列表，先通过非流式请求检测模型是否需要调用工具，
     * 如果需要则执行工具并将结果加入上下文，最终以流式方式返回给客户端。
     *
     * @param message         用户输入的聊天消息
     * @param historyMessages 历史消息列表，包含之前的用户和助手消息
     * @param summary         对话摘要，可为null
     * @return Flux<String> 流式响应，每个元素代表一个文本片段
     */
    public Flux<String> chatStreamWithContext(String message, List<MessageDTO> historyMessages, String summary) {
        // 构建消息列表
        List<Map<String, Object>> messages = new ArrayList<>();

        // 添加系统提示词
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        // 如果有摘要，作为system消息插入
        if (summary != null && !summary.isEmpty()) {
            messages.add(Map.of("role", "system", "content", "以下是之前对话的摘要：\n" + summary));
        }

        // 添加历史消息
        if (historyMessages != null && !historyMessages.isEmpty()) {
            for (MessageDTO m : historyMessages) {
                String contentToSend = (m.getOriginalContent() != null && !m.getOriginalContent().isEmpty())
                        ? m.getOriginalContent()
                        : m.getContent();
                messages.add(Map.of("role", m.getRole(), "content", contentToSend));
            }
        }

        // 添加当前用户消息
        messages.add(Map.of("role", "user", "content", message));

        log.info("发送带上下文的流式请求，历史消息数: {}, 当前消息: {}",
                historyMessages != null ? historyMessages.size() : 0, message);

        // 先用非流式请求检测是否需要调用工具
        String toolResult = callWithToolSupport(messages);

        if (toolResult != null) {
            // 工具已执行，把结果加入消息列表，再流式请求最终回答
            messages.add(buildAssistantMessageWithTool(toolResult));
            messages.add(Map.of("role", "tool", "content", toolResult));
        }

        // 流式请求最终回答
        return streamChatCompletion(messages);
    }

    /**
     * 获取工具定义列表（OpenAI function calling 格式）
     *
     * @return 工具定义列表
     */
    private List<Map<String, Object>> getToolDefinitions() {
        Map<String, Object> function = new HashMap<>();
        function.put("name", "getWeather");
        function.put("description", "查询指定城市的实时天气，参数为城市中文名称（如北京、上海）");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> cityProp = new HashMap<>();
        cityProp.put("type", "string");
        cityProp.put("description", "城市中文名称");
        properties.put("city", cityProp);

        parameters.put("properties", properties);
        parameters.put("required", List.of("city"));
        function.put("parameters", parameters);

        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        tool.put("function", function);

        return List.of(tool);
    }

    /**
     * 以非流式方式发送请求，检测模型是否需要调用工具
     *
     * 如果模型返回tool_calls，执行对应的工具函数并返回结果；
     * 如果模型直接返回文本内容，返回null表示无需工具调用。
     *
     * @param messages 消息列表
     * @return 工具执行结果，null表示模型未调用工具
     */
    private String callWithToolSupport(List<Map<String, Object>> messages) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        String responseJson;
        try {
            // 先尝试带tools的请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("stream", false);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("messages", messages);
            requestBody.put("tools", getToolDefinitions());

            responseJson = webClient.post()
                    .uri("/v1/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            // 模型不支持tools参数，回退到普通请求
            log.warn("模型不支持tools参数，回退到普通请求: {}", e.getMessage());
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                return null;
            }

            JsonNode choice = choices.get(0);
            JsonNode messageNode = choice.path("message");

            // 检查是否有tool_calls
            JsonNode toolCalls = messageNode.path("tool_calls");
            if (toolCalls.isArray() && toolCalls.size() > 0) {
                // 取第一个工具调用
                JsonNode toolCall = toolCalls.get(0);
                String functionName = toolCall.path("function").path("name").asText();
                String arguments = toolCall.path("function").path("arguments").asText();

                log.info("模型调用工具: {}, 参数: {}", functionName, arguments);

                // 执行工具函数
                return executeToolFunction(functionName, arguments);
            }

            return null;
        } catch (Exception e) {
            log.error("解析工具调用响应异常", e);
            return null;
        }
    }

    /**
     * 执行工具函数
     *
     * 根据函数名和JSON参数，调用对应的工具方法并返回结果。
     *
     * @param functionName 函数名
     * @param arguments    JSON格式的参数
     * @return 工具执行结果
     */
    private String executeToolFunction(String functionName, String arguments) {
        try {
            if ("getWeather".equals(functionName)) {
                JsonNode argsNode = objectMapper.readTree(arguments);
                String city = argsNode.path("city").asText();
                return weatherTool.getWeather(city);
            }
            log.warn("未知的工具函数: {}", functionName);
            return "未知的工具函数：" + functionName;
        } catch (Exception e) {
            log.error("执行工具函数异常, function: {}", functionName, e);
            return "工具执行失败：" + e.getMessage();
        }
    }

    /**
     * 构建包含工具调用信息的assistant消息
     *
     * @param toolResult 工具执行结果
     * @return assistant消息Map
     */
    private Map<String, Object> buildAssistantMessageWithTool(String toolResult) {
        Map<String, Object> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", "");
        // tool_calls信息省略，部分模型可自动关联
        return assistantMsg;
    }

    /**
     * 以流式方式发送请求并返回Flux响应
     *
     * @param messages 消息列表
     * @return Flux<String> 流式响应
     */
    private Flux<String> streamChatCompletion(List<Map<String, Object>> messages) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", true);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("messages", messages);

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
                            JsonNode contentNode = choice.path("delta").path("content");
                            if (contentNode.isMissingNode() || contentNode.isNull()) {
                                contentNode = choice.path("message").path("content");
                            }
                            if (!contentNode.isMissingNode() && contentNode.isTextual()) {
                                return contentNode.asText();
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
                .doOnComplete(() -> log.info("流式请求完成"));
    }
}