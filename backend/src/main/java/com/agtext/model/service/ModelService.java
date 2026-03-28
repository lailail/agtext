package com.agtext.model.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.model.provider.ChatModelProvider;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ModelService {
  private final ModelRegistry registry;
  private final ModelSettings settings;

  public ModelService(ModelRegistry registry, ModelSettings settings) {
    this.registry = registry;
    this.settings = settings;
  }

  public ModelResponse chat(
      String providerOverride, String modelOverride, List<ChatMessage> messages) {
    String providerName =
        providerOverride == null || providerOverride.isBlank()
            ? settings.defaultProvider()
            : providerOverride;
    String modelName =
        modelOverride == null || modelOverride.isBlank()
            ? settings.defaultModelFor(providerName)
            : modelOverride;

    ChatModelProvider provider =
        registry
            .find(providerName)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "MODEL_PROVIDER_NOT_FOUND", "Unknown model provider: " + providerName));
    return provider.chat(modelName, messages);
  }
}
