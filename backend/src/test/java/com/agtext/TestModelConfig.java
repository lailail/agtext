package com.agtext;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.model.provider.ChatModelProvider;
import com.agtext.model.service.ModelRegistry;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestModelConfig {
  @Bean
  @Primary
  public ModelRegistry testModelRegistry() {
    ModelRegistry registry = new ModelRegistry();
    registry.register(
        new ChatModelProvider() {
          @Override
          public String name() {
            return "mock";
          }

          @Override
          public ModelResponse chat(String model, List<ChatMessage> messages) {
            String lastUser =
                messages.stream()
                    .filter(m -> "user".equals(m.role()))
                    .reduce((a, b) -> b)
                    .map(ChatMessage::content)
                    .orElse("");
            return new ModelResponse("mock", model, "mock:" + lastUser);
          }
        });
    return registry;
  }
}
