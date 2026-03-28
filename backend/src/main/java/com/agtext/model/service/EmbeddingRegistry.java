package com.agtext.model.service;

import com.agtext.model.provider.EmbeddingProvider;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class EmbeddingRegistry {
  private final Map<String, EmbeddingProvider> providers = new ConcurrentHashMap<>();

  public void register(EmbeddingProvider provider) {
    providers.put(provider.name(), provider);
  }

  public Optional<EmbeddingProvider> find(String name) {
    return Optional.ofNullable(providers.get(name));
  }
}
