# 外部API调用和实现方案

## 一、整体架构

```
用户提问 "北京天气怎么样"
        │
        ▼
  ChatController 接收请求
        │
        ▼
  ChatService 判断流式/非流式
        │
        ├── 非流式（chat/chatSimple）──→ Spring AI ChatClient 自动处理工具调用
        │
        └── 流式（chatStream/chatStreamWithContext）
                │
                ▼
         ① 非流式请求 + tools定义 → 检测模型是否要调用工具
                │
                ├── 模型直接回答 → 流式返回
                │
                └── 模型返回 tool_calls
                        │
                        ▼
                 ② 执行 WeatherTool.getWeather(city)
                        │
                        ▼
                 ③ 工具结果加入消息上下文，流式请求最终回答
                        │
                        ▼
                 SSE 推送给客户端
```

## 二、外部API说明

### 2.1 和风天气API

本次对接的是[和风天气](https://www.qweather.com/)(去平台注册相关的api_key)实时天气接口，包含两个API：

| API | 路径 | 作用 |
|-----|------|------|
| 城市查询 | `/geo/v2/city/lookup` | 将中文城市名转为城市ID |
| 实时天气 | `/v7/weather/now` | 根据城市ID获取实时天气数据 |

### 2.2 API响应示例

**城市查询响应：**
```json
{
  "code": "200",
  "location": [
    {
      "name": "北京",
      "id": "101010100",
      "lat": "39.90499",
      "lon": "116.40529",
      "adm2": "北京",
      "adm1": "北京市",
      "country": "中国"
    }
  ]
}
```

**实时天气响应：**
```json
{
  "code": "200",
  "now": {
    "temp": "24",
    "feelsLike": "18",
    "text": "阴",
    "windDir": "东北风",
    "windScale": "5",
    "humidity": "40",
    "pressure": "998",
    "vis": "30"
  }
}
```

## 三、实现细节

### 3.1 配置项（application.yml）

```yaml
weather:
  api:
    key: ${WEATHER_API_KEY:你的API_KEY}
    base-url: https://你的自定义host.qweatherapi.com
```

### 3.2 WeatherTool 工具类

位置：`com/example/springai/utils/WeatherTool.java`

核心要点：
- 使用 `@Component` 注解交给 Spring 管理
- 使用 `@Description` 注解描述方法功能（Spring AI 通过此注解识别工具）
- 使用 `WebClient` 发起 HTTP 请求调用外部 API
- 两步调用：先查城市ID，再查天气

```java
@Description("查询指定城市的实时天气，参数为城市中文名称（如北京、上海）")
public String getWeather(String city) {
    // 1. 城市名 → 城市ID
    String locationId = getCityId(city);
    // 2. 城市ID → 实时天气
    // 3. 解析JSON，格式化返回
}
```

### 3.3 工具注册方式

**方式一：Spring AI ChatClient 注册（非流式）**

```java
this.chatClient = chatClientBuilder
        .defaultFunctions("getWeather")  // 方法名即函数名
        .build();
```

**方式二：手动定义 tools JSON（流式）**

```java
private List<Map<String, Object>> getToolDefinitions() {
    // 构建 OpenAI function calling 格式的工具定义
    Map<String, Object> function = new HashMap<>();
    function.put("name", "getWeather");
    function.put("description", "查询指定城市的实时天气，参数为城市中文名称（如北京、上海）");

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("type", "object");
    parameters.put("properties", Map.of(
        "city", Map.of("type", "string", "description", "城市中文名称")
    ));
    parameters.put("required", List.of("city"));
    function.put("parameters", parameters);

    return List.of(Map.of("type", "function", "function", function));
}
```

## 四、大模型调用外部API的流程

### 4.1 OpenAI Function Calling 协议

大模型调用外部工具遵循 OpenAI 的 Function Calling 协议，流程如下：

```
① 客户端发送请求，附带 tools 字段定义可用工具
        │
        ▼
② 大模型分析用户意图，判断是否需要调用工具
        │
        ├── 不需要 → 直接返回文本回答
        │
        └── 需要 → 返回 tool_calls（包含函数名和参数）
                │
                ▼
③ 客户端执行工具函数，获取结果
                │
                ▼
④ 客户端将工具结果以 role=tool 发送给大模型
                │
                ▼
⑤ 大模型基于工具结果生成最终回答
```

### 4.2 请求体中的 tools 定义

```json
{
  "model": "mimo-v2.5-pro",
  "messages": [{"role": "user", "content": "北京天气怎么样"}],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "getWeather",
        "description": "查询指定城市的实时天气，参数为城市中文名称（如北京、上海）",
        "parameters": {
          "type": "object",
          "properties": {
            "city": {
              "type": "string",
              "description": "城市中文名称"
            }
          },
          "required": ["city"]
        }
      }
    }
  ]
}
```

### 4.3 模型返回的 tool_calls

当模型决定调用工具时，返回格式如下：

```json
{
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": null,
        "tool_calls": [
          {
            "id": "call_xxx",
            "type": "function",
            "function": {
              "name": "getWeather",
              "arguments": "{\"city\": \"北京\"}"
            }
          }
        ]
      },
      "finish_reason": "tool_calls"
    }
  ]
}
```

### 4.4 工具结果回传

客户端执行工具后，将结果以 `tool` 角色回传：

```json
{
  "messages": [
    {"role": "user", "content": "北京天气怎么样"},
    {"role": "assistant", "content": "", "tool_calls": [...]},
    {"role": "tool", "content": "北京当前天气：阴，气温24℃（体感18℃），湿度40%，东北风5级..."}
  ]
}
```

模型收到工具结果后，生成最终的自然语言回答。

## 五、扩展指南

如需新增其他外部API工具，按以下步骤操作：

1. **新建工具类**：在 `utils/` 下创建新的 `@Component` 类
2. **添加方法**：用 `@Description` 注解描述功能，参数名要清晰
3. **注册工具**：
   - 非流式：在 `ChatService` 构造函数的 `defaultFunctions()` 中加入方法名
   - 流式：在 `getToolDefinitions()` 中加入新的工具定义
4. **路由执行**：在 `executeToolFunction()` 中添加新函数的路由

## 六、异常分析与解决方案

### 6.1 异常现象

启动应用后，流式请求时控制台报错：

```
WebClientResponseException$BadRequest: 400 Bad Request from POST http://192.168.40.113:8088/v1/chat/completions
```

### 6.2 根因分析

流式请求的工具调用流程中，`callWithToolSupport` 方法会先发送一个**带 `tools` 字段的非流式请求**给 AI 模型，用于检测模型是否需要调用工具。

请求体示例：
```json
{
  "model": "mimo-v2.5-pro",
  "messages": [...],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "getWeather",
        "description": "查询指定城市的实时天气...",
        "parameters": {...}
      }
    }
  ]
}
```

**问题在于**：`mimo-v2.5-pro`（小米）模型及其 API 网关不支持 OpenAI Function Calling 协议，不认识请求体中的 `tools` 字段，因此直接返回了 `400 Bad Request`。

### 6.3 解决方案一：更换支持 Function Calling 的模型

将模型切换为支持 `tool_calls` 的模型，如通义千问 `qwen3.6-plus`。

**修改 application.yml：**
```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: qwen3.6-plus
```

**各模型 Function Calling 支持情况：**

| 模型 | 支持情况 |
|------|---------|
| qwen3.6-plus / qwen3.5-plus | ✅ 支持 |
| glm-5 | ✅ 支持 |
| MiniMax-M2.7 / M2.7-highspeed | ✅ 支持 |
| MiniMax-M2.5 / M2.5-highspeed | ⚠️ 部分支持 |
| doubao-seed-2.0-pro / code | ⚠️ 需测试 |
| mimo-v2.5-pro / mimo-v2-pro | ❌ 不支持 |

### 6.4 解决方案二：代码层面做容错降级

在 `callWithToolSupport` 方法中捕获异常，当模型不支持 `tools` 参数时自动降级为普通请求，避免 400 错误导致整个流式响应中断。

**核心代码：**
```java
private String callWithToolSupport(List<Map<String, Object>> messages) {
    try {
        // 先尝试带tools的请求
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", false);
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
        return null;  // 返回null，跳过工具调用，直接流式返回
    }
    // ... 解析tool_calls
}
```

**降级后的行为：**
- 模型支持 `tools` → 正常走工具调用流程
- 模型不支持 `tools` → 捕获异常，返回 `null`，跳过工具调用，直接以流式方式返回模型的回答（无工具增强）

### 6.5 两种方案对比

| | 方案一：换模型 | 方案二：容错降级 |
|---|---|---|
| 工具调用 | ✅ 完整生效 | ❌ 不生效，降级为普通对话 |
| 改动范围 | 仅改配置 | 改代码 |
| 适用场景 | 需要工具调用功能 | 保证服务不报错即可 |
| 推荐程度 | ⭐⭐⭐ 推荐 | ⭐⭐ 兜底方案 |

**最佳实践**：两种方案结合使用 — 选择支持 Function Calling 的模型（方案一），同时在代码中做好容错处理（方案二），确保切换模型时服务不会因 400 错误而中断。
