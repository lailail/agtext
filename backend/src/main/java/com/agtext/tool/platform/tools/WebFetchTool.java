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

@Component
public class WebFetchTool implements ToolHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
        "web.fetch",
        "抓取网页并抽取可读文本（HTML 将去标签）。",
        ToolType.READ,
        false,
        10_000,
        """
        {"type":"object","properties":{"url":{"type":"string"},"maxChars":{"type":"integer"}},"required":["url"]}
        """,
        """
        {"type":"object","properties":{"url":{"type":"string"},"title":{"type":"string"},"text":{"type":"string"}},"required":["url","text"]}
        """);
  }

  @Override
  public ToolResult execute(ToolContext ctx, JsonNode input) throws Exception {
    String url = text(input, "url");
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("url is required");
    }

    URI uri = URI.create(url.trim());
    ToolUrlPolicy.enforceDomainAllowlist(ctx.security(), uri);

    int maxChars = intValue(input, "maxChars", 12_000);
    maxChars = Math.max(500, Math.min(100_000, maxChars));

    Document doc =
        Jsoup.connect(uri.toString())
            .userAgent("agtext/0.1")
            .timeout((int) Duration.ofSeconds(8).toMillis())
            .get();

    String title = doc.title();
    String text = normalize(doc.text());
    if (text.length() > maxChars) {
      text = text.substring(0, maxChars);
    }

    ObjectNode data = MAPPER.createObjectNode();
    data.put("url", uri.toString());
    data.put("title", title == null ? "" : title);
    data.put("text", text);
    return new ToolResult("fetched " + uri, data);
  }

  private static String normalize(String t) {
    if (t == null) {
      return "";
    }
    return t.replaceAll("\\s+", " ").trim();
  }

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
