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

/**
 * 维基百科搜索工具
 * 利用 Wikipedia Open API 提供结构化的事实搜索能力
 */
@Component
public class WikipediaSearchTool implements ToolHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  // 硬编码中文维基百科基础地址
  private static final String BASE = "https://zh.wikipedia.org";

  /**
   * 工具元数据定义
   * 设定为 READ 类型，最大结果数 limit 限制在 1-10 之间
   */
  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
            "web.search",
            "基于 Wikipedia 的简单搜索（无需额外 API Key）。",
            ToolType.READ,
            false, // 无需二次确认
            10_000, // 10s 超时
            """
            {"type":"object","properties":{"query":{"type":"string"},"limit":{"type":"integer"}},"required":["query"]}
            """,
            """
            {"type":"object","properties":{"query":{"type":"string"},"results":{"type":"array"}},"required":["query","results"]}
            """);
  }

  @Override
  public ToolResult execute(ToolContext ctx, JsonNode input) throws Exception {
    // 1. 参数提取与校验
    String query = input == null || input.get("query") == null ? null : input.get("query").asText();
    if (query == null || query.isBlank()) {
      throw new IllegalArgumentException("query is required");
    }
    // 限制单次搜索结果数量，平衡 Token 消耗与信息密度
    int limit = input != null && input.get("limit") != null ? input.get("limit").asInt(5) : 5;
    limit = Math.max(1, Math.min(10, limit));

    // 2. 构造 Wikipedia API 请求 URL
    // 使用 action=query&list=search 接口，返回包含摘要（snippet）的结果
    String url =
            BASE
                    + "/w/api.php?action=query&list=search&format=json&origin=*&srlimit="
                    + limit
                    + "&srsearch="
                    + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);

    // 3. 安全策略拦截：校验 Wikipedia 域名是否在允许列表中
    URI uri = URI.create(url);
    ToolUrlPolicy.enforceDomainAllowlist(ctx.security(), uri);

    // 4. 发起异步 HTTP GET 请求
    HttpRequest req =
            HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(8)) // 请求超时 8s
                    .header("Accept", "application/json")
                    .GET()
                    .build();

    // 使用上下文提供的全局 HttpClient 发送请求
    HttpResponse<String> resp =
            ctx.http().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new IllegalStateException("HTTP " + resp.statusCode());
    }

    // 5. 解析 API 响应结果
    JsonNode root = MAPPER.readTree(resp.body());
    JsonNode arr = root.path("query").path("search");
    ArrayNode results = MAPPER.createArrayNode();

    if (arr.isArray()) {
      for (JsonNode it : arr) {
        ObjectNode row = MAPPER.createObjectNode();
        long pageId = it.path("pageid").asLong();
        String title = it.path("title").asText();
        // API 返回的 snippet 包含 HTML 标签（如 <span class="searchmatch">）
        String snippet = it.path("snippet").asText();

        row.put("pageId", pageId);
        row.put("title", title);
        // 正则清洗：去除 HTML 标签，仅保留纯文本供 Agent 阅读
        row.put("snippet", snippet.replaceAll("<[^>]+>", ""));
        // 构造词条直达链接
        row.put("url", BASE + "/?curid=" + pageId);
        results.add(row);
      }
    }

    // 6. 构造执行结果
    ObjectNode data = MAPPER.createObjectNode();
    data.put("query", query);
    data.set("results", results);
    return new ToolResult("search results: " + results.size(), data);
  }
}