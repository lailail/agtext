package com.agtext.model.provider;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class OpenAiCompatibleChatProvider implements ChatModelProvider {
  private final String name;
  private final String baseUrl;
  private final String apiKey;
  private final RestClient restClient;

  public OpenAiCompatibleChatProvider(String name, String baseUrl, String apiKey) {
    this.name = Objects.requireNonNull(name);
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.apiKey = Objects.requireNonNull(apiKey);
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

  @Override
  public ModelResponse chat(String model, List<ChatMessage> messages) {
    var request = new OpenAiChatCompletionsRequest(model, messages);
    OpenAiChatCompletionsResponse response =
        restClient
            .post()
            .uri("/v1/chat/completions")
            .body(request)
            .retrieve()
            .body(OpenAiChatCompletionsResponse.class);
    if (response == null
        || response.choices() == null
        || response.choices().isEmpty()
        || response.choices().get(0).message() == null) {
      throw new IllegalArgumentException("MODEL_EMPTY_RESPONSE");
    }
    String content = response.choices().get(0).message().content();
    return new ModelResponse(name, model, content == null ? "" : content);
  }

  private static String normalizeBaseUrl(String baseUrl) {
    String trimmed = baseUrl.trim();
    if (trimmed.endsWith("/")) {
      return trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }

  public record OpenAiChatCompletionsRequest(String model, List<ChatMessage> messages) {}

  public record OpenAiChatCompletionsResponse(List<Choice> choices) {
    public record Choice(Message message) {}

    public record Message(String role, String content) {}
  }
}
