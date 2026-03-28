package com.agtext.knowledge.service;

import java.util.ArrayList;
import java.util.List;

public final class TextChunker {
  private TextChunker() {}

  public static List<String> chunk(String text, int maxChars, int overlap) {
    if (text == null) {
      return List.of();
    }
    String normalized = normalize(text);
    if (normalized.isBlank()) {
      return List.of();
    }
    int safeMax = Math.max(200, maxChars);
    int safeOverlap = Math.max(0, Math.min(overlap, safeMax / 2));

    List<String> chunks = new ArrayList<>();
    int start = 0;
    while (start < normalized.length()) {
      int end = Math.min(normalized.length(), start + safeMax);
      String slice = normalized.substring(start, end).trim();
      if (!slice.isBlank()) {
        chunks.add(slice);
      }
      if (end >= normalized.length()) {
        break;
      }
      start = Math.max(0, end - safeOverlap);
    }
    return chunks;
  }

  public static String normalize(String text) {
    return text.replace("\r\n", "\n").replace('\r', '\n').trim();
  }
}
