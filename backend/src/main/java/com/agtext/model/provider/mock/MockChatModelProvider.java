package com.agtext.model.provider.mock;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.model.provider.ChatModelProvider;
import java.util.List;

public class MockChatModelProvider implements ChatModelProvider {
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
}
