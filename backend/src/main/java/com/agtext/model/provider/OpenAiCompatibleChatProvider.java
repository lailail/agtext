package com.agtext.model.provider;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * OpenAI 兼容聊天供应商实现：
 * 该类将通用的 ChatMessage 转换为 OpenAI 标准的 /v1/chat/completions 请求格式。
 * 支持所有兼容 OpenAI 协议的后端（如 DeepSeek, Azure OpenAI, 百度文心兼容层等）。
 */
public class OpenAiCompatibleChatProvider implements ChatModelProvider {
  private final String name;
  private final String baseUrl;
  private final String apiKey;
  private final RestClient restClient; // Spring 6.1+ 引入的高性能同步 HTTP 客户端

  public OpenAiCompatibleChatProvider(String name, String baseUrl, String apiKey) {
    this.name = Objects.requireNonNull(name);
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.apiKey = Objects.requireNonNull(apiKey);

    // 1. 初始化 HTTP 客户端，预配置 BaseURL 和 Bearer Token 鉴权头
    this.restClient =
            RestClient.builder()
                    .baseUrl(normalizeBaseUrl(baseUrl))
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
  }

  @Override
  public String name() {
    return name;
  }

  /**
   * 执行聊天请求
   * 流程：构建 DTO -> 发送 POST -> 解析 JSON -> 提取 Content
   */
  @Override
  public ModelResponse chat(String model, List<ChatMessage> messages) {
    // 2. 创建符合 OpenAI API 格式的请求对象
    var request = new OpenAiChatCompletionsRequest(model, messages);

    // 3. 发起同步 HTTP 调用
    OpenAiChatCompletionsResponse response =
            restClient
                    .post()
                    .uri("/v1/chat/completions") // 标准 OpenAI 聊天路径
                    .body(request)
                    .retrieve()
                    .body(OpenAiChatCompletionsResponse.class); // 自动反序列化 JSON

    // 4. 防御性检查：验证响应结构的完整性，防止模型返回空数据或格式错误
    if (response == null
            || response.choices() == null
            || response.choices().isEmpty()
            || response.choices().get(0).message() == null) {
      throw new IllegalArgumentException("MODEL_EMPTY_RESPONSE");
    }

    // 5. 提取模型生成的文本内容
    String content = response.choices().get(0).message().content();
    return new ModelResponse(name, model, content == null ? "" : content);
  }

  /**
   * 路径标准化：移除 URL 末尾多余的斜杠，防止与 uri() 中的路径拼接出现双斜杠（//v1/...）
   */
  private static String normalizeBaseUrl(String baseUrl) {
    String trimmed = baseUrl.trim();
    if (trimmed.endsWith("/")) {
      return trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }

  // --- 内部 DTO (Data Transfer Objects) 用于映射 OpenAI JSON 结构 ---

  /**
   * 请求结构：{"model": "...", "messages": [...]}
   */
  public record OpenAiChatCompletionsRequest(String model, List<ChatMessage> messages) {}

  /**
   * 响应结构：对应 OpenAI 返回的 choices 数组
   */
  public record OpenAiChatCompletionsResponse(List<Choice> choices) {
    public record Choice(Message message) {}

    public record Message(String role, String content) {}
  }
}