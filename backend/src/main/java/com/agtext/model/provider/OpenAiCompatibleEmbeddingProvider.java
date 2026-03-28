package com.agtext.model.provider;

import com.agtext.model.domain.EmbeddingResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * OpenAI 兼容的向量提供者实现：
 * 负责通过 HTTP 调用将文本转换为高维向量。
 * 这是 RAG（检索增强生成）系统中实现语义检索的底层技术支撑。
 */
public class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {
  private final String name;
  private final RestClient restClient;

  public OpenAiCompatibleEmbeddingProvider(String name, String baseUrl, String apiKey) {
    this.name = Objects.requireNonNull(name);
    // 1. 初始化 RestClient，预配置 OpenAI 标准请求头（鉴权与内容类型）
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
   * 执行向量化提取
   * 流程：构建请求 -> 发送 POST -> 解析 JSON -> 转换数据类型 (Double to float)
   */
  @Override
  public EmbeddingResponse embed(String model, String input) {
    // 2. 构造 OpenAI 标准的 Embedding 请求对象
    EmbeddingsRequest req = new EmbeddingsRequest(model, input);

    // 3. 发起同步 HTTP 调用，目标端点为标准的 /v1/embeddings
    EmbeddingsResponse resp =
            restClient
                    .post()
                    .uri("/v1/embeddings")
                    .body(req)
                    .retrieve()
                    .body(EmbeddingsResponse.class);

    // 4. 健壮性检查：确保响应结构完整
    if (resp == null
            || resp.data() == null
            || resp.data().isEmpty()
            || resp.data().get(0) == null) {
      throw new IllegalArgumentException("EMBEDDING_EMPTY_RESPONSE");
    }

    // 5. 提取向量数据
    List<Double> vec = resp.data().get(0).embedding();
    if (vec == null || vec.isEmpty()) {
      throw new IllegalArgumentException("EMBEDDING_EMPTY_VECTOR");
    }

    // 6. 数据类型转换：将 List<Double> 转换为高效的 float[]
    // 严肃性提示：内存中存储大量向量时，float[] 比 Double 对象节省约 4 倍内存
    float[] f = new float[vec.size()];
    for (int i = 0; i < vec.size(); i++) {
      f[i] = vec.get(i).floatValue();
    }

    return new EmbeddingResponse(name, model, f);
  }

  /**
   * 路径标准化：去除 URL 尾部斜杠，确保拼接 URI 时不会出现 "//v1/..." 的错误
   */
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

  // --- OpenAI API 内部映射 DTO (Data Transfer Objects) ---

  /**
   * 请求模型：{"model": "...", "input": "..."}
   */
  public record EmbeddingsRequest(String model, String input) {}

  /**
   * 响应模型：映射 OpenAI 返回的 JSON 结构中的 data 数组
   */
  public record EmbeddingsResponse(List<Item> data) {
    public record Item(List<Double> embedding) {}
  }
}