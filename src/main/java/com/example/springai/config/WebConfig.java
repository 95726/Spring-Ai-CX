package com.example.springai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 *
 * 配置字符编码和内容协商，确保流式响应正确处理 UTF-8 编码。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置内容协商
     *
     * 设置默认编码为 UTF-8，解决流式响应中文乱码问题。
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .ignoreAcceptHeader(false)
                .defaultContentType(org.springframework.http.MediaType.TEXT_HTML)
                .mediaType("event-stream", org.springframework.http.MediaType.valueOf("text/event-stream;charset=UTF-8"));
    }
}