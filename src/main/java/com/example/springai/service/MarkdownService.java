package com.example.springai.service;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Markdown转换服务
 *
 * 提供Markdown文本到HTML的转换功能，用于将AI返回的Markdown格式内容
 * 转换为HTML格式，以便前端直接渲染显示。
 */
@Service
public class MarkdownService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownService.class);

    private final Parser parser;
    private final HtmlRenderer renderer;

    /**
     * 构造函数
     *
     * 初始化Flexmark解析器和HTML渲染器
     */
    public MarkdownService() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    /**
     * 将Markdown文本转换为HTML
     *
     * 将传入的Markdown格式文本转换为HTML标签，便于前端直接渲染。
     * 如果转换过程中发生异常，返回原始文本内容。
     *
     * @param markdown Markdown格式的文本内容
     * @return HTML格式的文本内容
     */
    public String convertToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return markdown;
        }

        try {
            Document document = parser.parse(markdown);
            String html = renderer.render(document);
            log.debug("Markdown转HTML成功，原长度: {}, 转换后长度: {}", markdown.length(), html.length());
            return html;
        } catch (Exception e) {
            log.warn("Markdown转HTML失败，使用原始文本: {}", e.getMessage());
            return markdown;
        }
    }
}
