package com.agtext.model.provider;

import com.agtext.model.domain.EmbeddingResponse;

public interface EmbeddingProvider {
  String name();

  EmbeddingResponse embed(String model, String input);
}
