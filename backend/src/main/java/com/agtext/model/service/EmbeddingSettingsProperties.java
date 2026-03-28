package com.agtext.model.service;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.embedding")
public record EmbeddingSettingsProperties(
    String defaultProvider, String defaultModel, Map<String, ProviderConfig> providers) {
  public record ProviderConfig(String baseUrl, String apiKey, String model) {}
}
