package com.example.springai.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @ClassName WeatherTool
 * @Description 调用天气 工具类，对接和风天气API
 * @Author chenxin
 * @Date 2026/6/3 16:31
 */

@Component
public class WeatherTool {

    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);

    @Value("${weather.api.key:}")
    private String apiKey;

    @Value("${weather.api.base-url:https://api.qweather.com}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 天气查询工具方法，调用和风天气API获取实时天气
     *
     * @param city 城市名称（中文，如"北京"、"上海"）
     * @return 天气信息字符串，包含温度、体感温度、天气状况、湿度、风向风速、气压等
     */
    @Description("查询指定城市的实时天气，参数为城市中文名称（如北京、上海）")
    public String getWeather(String city) {
        log.info("调用天气API：{}", city);
        try {
            // 先通过城市名查询获取城市ID
            String locationId = getCityId(city);
            if (locationId == null) {
                return "未找到城市：" + city + "，请检查城市名称是否正确";
            }

            // 调用和风天气实时天气API
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();

            String responseJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v7/weather/now")
                            .queryParam("location", locationId)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析天气响应JSON
            JsonNode root = objectMapper.readTree(responseJson);
            String code = root.path("code").asText();

            if (!"200".equals(code)) {
                log.error("天气API返回错误，code: {}, city: {}", code, city);
                return "天气查询失败，错误码：" + code;
            }

            JsonNode now = root.path("now");
            String temp = now.path("temp").asText();           // 实时温度
            String feelsLike = now.path("feelsLike").asText(); // 体感温度
            String text = now.path("text").asText();           // 天气状况文字
            String humidity = now.path("humidity").asText();   // 相对湿度
            String windDir = now.path("windDir").asText();     // 风向
            String windScale = now.path("windScale").asText(); // 风力等级
            String pressure = now.path("pressure").asText();   // 气压
            String vis = now.path("vis").asText();             // 能见度

            return String.format("%s当前天气：%s，气温%s℃（体感%s℃），湿度%s%%，%s%s级，气压%shPa，能见度%skm",
                    city, text, temp, feelsLike, humidity, windDir, windScale, pressure, vis);

        } catch (Exception e) {
            log.error("调用天气API异常，city: {}", city, e);
            return "天气查询失败：" + e.getMessage();
        }
    }

    /**
     * 通过城市名称查询和风天气城市ID
     *
     * @param city 城市中文名称
     * @return 城市ID，未找到返回null
     */
    private String getCityId(String city) {
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();

            String responseJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/geo/v2/city/lookup")
                            .queryParam("location", city)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseJson);
            String code = root.path("code").asText();

            if (!"200".equals(code)) {
                log.error("城市查询API返回错误，code: {}, city: {}", code, city);
                return null;
            }

            JsonNode locationNode = root.path("location");
            if (locationNode.isArray() && locationNode.size() > 0) {
                return locationNode.get(0).path("id").asText();
            }

            return null;
        } catch (Exception e) {
            log.error("查询城市ID异常，city: {}", city, e);
            return null;
        }
    }
}
