package com.agtext.model.service;

import com.agtext.model.provider.ChatModelProvider;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ModelRegistry {
  private final Map<String, ChatModelProvider> providers = new ConcurrentHashMap<>();

  public void register(ChatModelProvider provider) {
    providers.put(provider.name(), provider);
  }

  public Optional<ChatModelProvider> find(String name) {
    return Optional.ofNullable(providers.get(name));
  }
}
