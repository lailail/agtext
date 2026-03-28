package com.agtext.model.provider.mock;

import com.agtext.model.domain.EmbeddingResponse;
import com.agtext.model.provider.EmbeddingProvider;

public class MockEmbeddingProvider implements EmbeddingProvider {
  @Override
  public String name() {
    return "mock";
  }

  @Override
  public EmbeddingResponse embed(String model, String input) {
    float[] vec = new float[8];
    String text = input == null ? "" : input;
    for (int i = 0; i < vec.length; i++) {
      vec[i] = ((text.hashCode() >> i) & 1) == 1 ? 1.0f : 0.0f;
    }
    return new EmbeddingResponse("mock", model, vec);
  }
}
