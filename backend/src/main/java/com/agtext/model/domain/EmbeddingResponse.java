package com.agtext.model.domain;

public record EmbeddingResponse(String provider, String model, float[] vector) {}
