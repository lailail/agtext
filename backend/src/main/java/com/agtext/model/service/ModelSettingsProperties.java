package com.agtext.model.service;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.model")
public record ModelSettingsProperties(
    String defaultProvider, String fallbackModel, Map<String, ProviderConfig> providers) {
  public record ProviderConfig(String baseUrl, String apiKey, String model) {}
}
