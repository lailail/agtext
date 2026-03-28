package com.agtext.tool.platform.tools;

import com.agtext.tool.platform.domain.ToolDefinition;
import com.agtext.tool.platform.domain.ToolResult;
import com.agtext.tool.platform.domain.ToolType;
import com.agtext.tool.platform.service.ToolContext;
import com.agtext.tool.platform.service.ToolHandler;
import com.agtext.tool.platform.service.ToolUrlPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class WikipediaSearchTool implements ToolHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String BASE = "https://zh.wikipedia.org";

  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
        "web.search",
        "基于 Wikipedia 的简单搜索（无需额外 API Key）。",
        ToolType.READ,
        false,
        10_000,
        """
        {"type":"object","properties":{"query":{"type":"string"},"limit":{"type":"integer"}},"required":["query"]}
        """,
        """
        {"type":"object","properties":{"query":{"type":"string"},"results":{"type":"array"}},"required":["query","results"]}
        """);
  }

  @Override
  public ToolResult execute(ToolContext ctx, JsonNode input) throws Exception {
    String query = input == null || input.get("query") == null ? null : input.get("query").asText();
    if (query == null || query.isBlank()) {
      throw new IllegalArgumentException("query is required");
    }
    int limit = input != null && input.get("limit") != null ? input.get("limit").asInt(5) : 5;
    limit = Math.max(1, Math.min(10, limit));

    String url =
        BASE
            + "/w/api.php?action=query&list=search&format=json&origin=*&srlimit="
            + limit
            + "&srsearch="
            + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
    URI uri = URI.create(url);
    ToolUrlPolicy.enforceDomainAllowlist(ctx.security(), uri);

    HttpRequest req =
        HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(8))
            .header("Accept", "application/json")
            .GET()
            .build();
    HttpResponse<String> resp =
        ctx.http().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new IllegalStateException("HTTP " + resp.statusCode());
    }
    JsonNode root = MAPPER.readTree(resp.body());
    JsonNode arr = root.path("query").path("search");
    ArrayNode results = MAPPER.createArrayNode();
    if (arr.isArray()) {
      for (JsonNode it : arr) {
        ObjectNode row = MAPPER.createObjectNode();
        long pageId = it.path("pageid").asLong();
        String title = it.path("title").asText();
        String snippet = it.path("snippet").asText();
        row.put("pageId", pageId);
        row.put("title", title);
        row.put("snippet", snippet.replaceAll("<[^>]+>", ""));
        row.put("url", BASE + "/?curid=" + pageId);
        results.add(row);
      }
    }

    ObjectNode data = MAPPER.createObjectNode();
    data.put("query", query);
    data.set("results", results);
    return new ToolResult("search results: " + results.size(), data);
  }
}
