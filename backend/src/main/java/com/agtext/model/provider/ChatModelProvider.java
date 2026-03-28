package com.agtext.model.provider;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import java.util.List;

public interface ChatModelProvider {
  String name();

  ModelResponse chat(String model, List<ChatMessage> messages);
}
