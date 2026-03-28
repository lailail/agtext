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

@Component
public class DocParseTool implements ToolHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
        "doc.parse",
        "解析文档（支持 URL 指向的 PDF/HTML/Markdown，返回抽取文本预览）。",
        ToolType.READ,
        false,
        15_000,
        """
        {"type":"object","properties":{"url":{"type":"string"},"maxChars":{"type":"integer"}},"required":["url"]}
        """,
        """
        {"type":"object","properties":{"url":{"type":"string"},"type":{"type":"string"},"text":{"type":"string"}},"required":["url","type","text"]}
        """);
  }

  @Override
  public ToolResult execute(ToolContext ctx, JsonNode input) throws Exception {
    String url = input == null || input.get("url") == null ? null : input.get("url").asText();
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("url is required");
    }
    int maxChars =
        input != null && input.get("maxChars") != null
            ? input.get("maxChars").asInt(12_000)
            : 12_000;
    maxChars = Math.max(500, Math.min(200_000, maxChars));

    URI uri = URI.create(url.trim());
    ToolUrlPolicy.enforceDomainAllowlist(ctx.security(), uri);

    HttpRequest req =
        HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(12))
            .header("Accept", "*/*")
            .GET()
            .build();
    HttpResponse<byte[]> resp = ctx.http().send(req, HttpResponse.BodyHandlers.ofByteArray());
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new IllegalStateException("HTTP " + resp.statusCode());
    }
    String contentType = resp.headers().firstValue("content-type").orElse("");
    byte[] body = resp.body() == null ? new byte[0] : resp.body();

    String type;
    String text;
    if (contentType.toLowerCase().contains("pdf") || uri.getPath().toLowerCase().endsWith(".pdf")) {
      type = "pdf";
      text = parsePdf(body);
    } else if (contentType.toLowerCase().contains("html")
        || uri.getPath().toLowerCase().endsWith(".html")) {
      type = "html";
      text = Jsoup.parse(new String(body, StandardCharsets.UTF_8)).text();
    } else {
      type = "text";
      text = new String(body, StandardCharsets.UTF_8);
    }
    text = text.replaceAll("\\s+", " ").trim();
    if (text.length() > maxChars) {
      text = text.substring(0, maxChars);
    }

    ObjectNode data = MAPPER.createObjectNode();
    data.put("url", uri.toString());
    data.put("type", type);
    data.put("text", text);
    return new ToolResult("parsed " + type, data);
  }

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
