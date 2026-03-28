package com.agtext.model.config;

import com.agtext.model.provider.OpenAiCompatibleChatProvider;
import com.agtext.model.provider.OpenAiCompatibleEmbeddingProvider;
import com.agtext.model.provider.mock.MockChatModelProvider;
import com.agtext.model.provider.mock.MockEmbeddingProvider;
import com.agtext.model.service.EmbeddingRegistry;
import com.agtext.model.service.EmbeddingSettingsProperties;
import com.agtext.model.service.ModelRegistry;
import com.agtext.model.service.ModelSettingsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ModelSettingsProperties.class, EmbeddingSettingsProperties.class})
public class ModelConfig {
  @Bean
  public ModelRegistry modelRegistry(ModelSettingsProperties props) {
    ModelRegistry registry = new ModelRegistry();
    props
        .providers()
        .forEach(
            (name, cfg) -> {
              if (cfg != null
                  && cfg.baseUrl() != null
                  && !cfg.baseUrl().isBlank()
                  && cfg.apiKey() != null
                  && !cfg.apiKey().isBlank()) {
                registry.register(
                    new OpenAiCompatibleChatProvider(name, cfg.baseUrl(), cfg.apiKey()));
              }
            });

    if ("mock".equalsIgnoreCase(props.defaultProvider())) {
      registry.register(new MockChatModelProvider());
    }
    return registry;
  }

  @Bean
  public EmbeddingRegistry embeddingRegistry(EmbeddingSettingsProperties props) {
    EmbeddingRegistry registry = new EmbeddingRegistry();
    props
        .providers()
        .forEach(
            (name, cfg) -> {
              if (cfg != null
                  && cfg.baseUrl() != null
                  && !cfg.baseUrl().isBlank()
                  && cfg.apiKey() != null
                  && !cfg.apiKey().isBlank()) {
                registry.register(
                    new OpenAiCompatibleEmbeddingProvider(name, cfg.baseUrl(), cfg.apiKey()));
              }
            });

    if ("mock".equalsIgnoreCase(props.defaultProvider())) {
      registry.register(new MockEmbeddingProvider());
    }
    return registry;
  }
}
