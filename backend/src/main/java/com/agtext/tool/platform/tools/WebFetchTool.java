package com.agtext.tool.platform.tools;

import com.agtext.tool.platform.domain.ToolDefinition;
import com.agtext.tool.platform.domain.ToolResult;
import com.agtext.tool.platform.domain.ToolType;
import com.agtext.tool.platform.service.ToolContext;
import com.agtext.tool.platform.service.ToolHandler;
import com.agtext.tool.platform.service.ToolUrlPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.time.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/**
 * 网页抓取工具
 * 使用 Jsoup 直接连接并解析网页内容，提取纯文本摘要
 */
@Component
public class WebFetchTool implements ToolHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * 工具元数据定义
   * 设定为 READ 类型，超时时间为较短的 10 秒
   */
  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
            "web.fetch",
            "抓取网页并抽取可读文本（HTML 将去标签）。",
            ToolType.READ,
            false, // 无需人工确认
            10_000, // 10s 超时
            """
            {"type":"object","properties":{"url":{"type":"string"},"maxChars":{"type":"integer"}},"required":["url"]}
            """,
            """
            {"type":"object","properties":{"url":{"type":"string"},"title":{"type":"string"},"text":{"type":"string"}},"required":["url","text"]}
            """);
  }

  @Override
  public ToolResult execute(ToolContext ctx, JsonNode input) throws Exception {
    // 1. 参数提取与必填项校验
    String url = text(input, "url");
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("url is required");
    }

    // 2. 安全策略检查：在发起连接前强制校验域名白名单
    URI uri = URI.create(url.trim());
    ToolUrlPolicy.enforceDomainAllowlist(ctx.security(), uri);

    // 3. 字符长度限制初始化（范围 500 - 100,000）
    int maxChars = intValue(input, "maxChars", 12_000);
    maxChars = Math.max(500, Math.min(100_000, maxChars));

    // 4. 执行网页抓取
    // 使用 Jsoup 自带的连接能力，并设置 User-Agent 以减少被反爬虫拦截的概率
    Document doc =
            Jsoup.connect(uri.toString())
                    .userAgent("agtext/0.1")
                    .timeout((int) Duration.ofSeconds(8).toMillis()) // 网络连接超时设置为 8 秒，留出 2 秒余量给解析逻辑
                    .get();

    // 5. 内容处理：提取网页标题和去标签后的纯文本
    String title = doc.title();
    String text = normalize(doc.text()); // 规范化处理：去除多余空格和换行

    // 6. 按需截断
    if (text.length() > maxChars) {
      text = text.substring(0, maxChars);
    }

    // 7. 构造结构化结果数据
    ObjectNode data = MAPPER.createObjectNode();
    data.put("url", uri.toString());
    data.put("title", title == null ? "" : title);
    data.put("text", text);
    return new ToolResult("fetched " + uri, data);
  }

  /**
   * 规范化文本处理：将连续的空白字符替换为单个空格，并去除首尾空白
   */
  private static String normalize(String t) {
    if (t == null) {
      return "";
    }
    return t.replaceAll("\\s+", " ").trim();
  }

  /**
   * 安全获取 JsonNode 字段文本值
   */
  private static String text(JsonNode input, String field) {
    if (input == null) {
      return null;
    }
    JsonNode n = input.get(field);
    if (n == null || n.isNull()) {
      return null;
    }
    return n.asText();
  }

  /**
   * 安全获取 JsonNode 字段整数值，支持默认值
   */
  private static int intValue(JsonNode input, String field, int defaultValue) {
    if (input == null) {
      return defaultValue;
    }
    JsonNode n = input.get(field);
    if (n == null || n.isNull()) {
      return defaultValue;
    }
    return n.asInt(defaultValue);
  }
}