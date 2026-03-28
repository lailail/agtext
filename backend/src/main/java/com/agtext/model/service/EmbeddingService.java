package com.agtext.model.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.model.domain.EmbeddingResponse;
import com.agtext.model.provider.EmbeddingProvider;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
  private final EmbeddingRegistry registry;
  private final EmbeddingSettingsProperties props;

  public EmbeddingService(EmbeddingRegistry registry, EmbeddingSettingsProperties props) {
    this.registry = registry;
    this.props = props;
  }

  public EmbeddingResponse embed(String providerOverride, String modelOverride, String input) {
    String provider =
        providerOverride == null || providerOverride.isBlank()
            ? props.defaultProvider()
            : providerOverride;
    EmbeddingProvider p =
        registry
            .find(provider)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "EMBEDDING_PROVIDER_NOT_FOUND", "Unknown embedding provider: " + provider));
    String model =
        modelOverride == null || modelOverride.isBlank()
            ? defaultModelFor(provider)
            : modelOverride;
    return p.embed(model, input);
  }

  private String defaultModelFor(String provider) {
    EmbeddingSettingsProperties.ProviderConfig cfg = props.providers().get(provider);
    if (cfg != null && cfg.model() != null && !cfg.model().isBlank()) {
      return cfg.model();
    }
    return props.defaultModel();
  }
}
