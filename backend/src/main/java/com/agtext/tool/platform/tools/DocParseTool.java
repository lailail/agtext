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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/**
 * 文档解析工具
 * 支持通过 URL 下载并解析 PDF、HTML 和普通文本内容
 */
@Component
public class DocParseTool implements ToolHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * 工具元数据定义
   * 设定为 READ 类型，超时时间 15 秒，最大字符数限制在输入 Schema 中定义
   */
  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
            "doc.parse",
            "解析文档（支持 URL 指向的 PDF/HTML/Markdown，返回抽取文本预览）。",
            ToolType.READ,
            false, // 无需人工确认
            15_000, // 15s 超时
            """
            {"type":"object","properties":{"url":{"type":"string"},"maxChars":{"type":"integer"}},"required":["url"]}
            """,
            """
            {"type":"object","properties":{"url":{"type":"string"},"type":{"type":"string"},"text":{"type":"string"}},"required":["url","type","text"]}
            """);
  }

  @Override
  public ToolResult execute(ToolContext ctx, JsonNode input) throws Exception {
    // 1. 参数提取与字符长度限制初始化（默认 12,000 字符，范围 500 - 200,000）
    String url = input == null || input.get("url") == null ? null : input.get("url").asText();
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("url is required");
    }
    int maxChars =
            input != null && input.get("maxChars") != null
                    ? input.get("maxChars").asInt(12_000)
                    : 12_000;
    maxChars = Math.max(500, Math.min(200_000, maxChars));

    // 2. 安全准入检查：强制执行域名白名单校验，防止 SSRF 攻击
    URI uri = URI.create(url.trim());
    ToolUrlPolicy.enforceDomainAllowlist(ctx.security(), uri);

    // 3. 发起远程 HTTP GET 请求
    HttpRequest req =
            HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(12)) // 网络请求超时需略短于工具总超时
                    .header("Accept", "*/*")
                    .GET()
                    .build();

    // 使用上下文提供的 HttpClient 发送请求并获取字节数组
    HttpResponse<byte[]> resp = ctx.http().send(req, HttpResponse.BodyHandlers.ofByteArray());
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new IllegalStateException("HTTP " + resp.statusCode());
    }

    // 4. 内容类型识别与多分支解析逻辑
    String contentType = resp.headers().firstValue("content-type").orElse("");
    byte[] body = resp.body() == null ? new byte[0] : resp.body();

    String type;
    String text;
    // 分支 A: PDF 解析（依赖 PDFBox）
    if (contentType.toLowerCase().contains("pdf") || uri.getPath().toLowerCase().endsWith(".pdf")) {
      type = "pdf";
      text = parsePdf(body);
    }
    // 分支 B: HTML 解析（使用 Jsoup 提取纯文本，去除标签）
    else if (contentType.toLowerCase().contains("html")
            || uri.getPath().toLowerCase().endsWith(".html")) {
      type = "html";
      text = Jsoup.parse(new String(body, StandardCharsets.UTF_8)).text();
    }
    // 分支 C: 普通文本或 Markdown
    else {
      type = "text";
      text = new String(body, StandardCharsets.UTF_8);
    }

    // 5. 后处理：规范化空白字符、按需截断
    text = text.replaceAll("\\s+", " ").trim();
    if (text.length() > maxChars) {
      text = text.substring(0, maxChars);
    }

    // 6. 构造结构化结果
    ObjectNode data = MAPPER.createObjectNode();
    data.put("url", uri.toString());
    data.put("type", type);
    data.put("text", text);
    return new ToolResult("parsed " + type, data);
  }

  /**
   * 调用 PDFBox 加载并提取 PDF 文本内容
   */
  private static String parsePdf(byte[] bytes) throws Exception {
    if (bytes.length == 0) {
      return "";
    }
    try (PDDocument doc = Loader.loadPDF(bytes)) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(doc);
    }
  }
}