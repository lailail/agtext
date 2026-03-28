package com.agtext.model.provider;

import com.agtext.model.domain.EmbeddingResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {
  private final String name;
  private final RestClient restClient;

  public OpenAiCompatibleEmbeddingProvider(String name, String baseUrl, String apiKey) {
    this.name = Objects.requireNonNull(name);
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
  public EmbeddingResponse embed(String model, String input) {
    EmbeddingsRequest req = new EmbeddingsRequest(model, input);
    EmbeddingsResponse resp =
        restClient.post().uri("/v1/embeddings").body(req).retrieve().body(EmbeddingsResponse.class);
    if (resp == null
        || resp.data() == null
        || resp.data().isEmpty()
        || resp.data().get(0) == null) {
      throw new IllegalArgumentException("EMBEDDING_EMPTY_RESPONSE");
    }
    List<Double> vec = resp.data().get(0).embedding();
    if (vec == null || vec.isEmpty()) {
      throw new IllegalArgumentException("EMBEDDING_EMPTY_VECTOR");
    }
    float[] f = new float[vec.size()];
    for (int i = 0; i < vec.size(); i++) {
      f[i] = vec.get(i).floatValue();
    }
    return new EmbeddingResponse(name, model, f);
  }

  private static String normalizeBaseUrl(String baseUrl) {
    if (baseUrl == null) {
      throw new IllegalArgumentException("baseUrl is required");
    }
    String trimmed = baseUrl.trim();
    if (trimmed.endsWith("/")) {
      return trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }

  public record EmbeddingsRequest(String model, String input) {}

  public record EmbeddingsResponse(List<Item> data) {
    public record Item(List<Double> embedding) {}
  }
}
