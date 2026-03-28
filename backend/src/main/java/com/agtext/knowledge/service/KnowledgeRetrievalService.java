package com.agtext.knowledge.service;

import com.agtext.knowledge.repository.KnowledgeChunkEmbeddingRepository;
import com.agtext.model.service.EmbeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRetrievalService {
  private final EmbeddingService embeddings;
  private final KnowledgeChunkEmbeddingRepository embeddingRepo;
  private final ObjectMapper objectMapper;

  public KnowledgeRetrievalService(
      EmbeddingService embeddings,
      KnowledgeChunkEmbeddingRepository embeddingRepo,
      ObjectMapper objectMapper) {
    this.embeddings = embeddings;
    this.embeddingRepo = embeddingRepo;
    this.objectMapper = objectMapper;
  }

  public List<RetrievalHit> retrieve(long knowledgeBaseId, String query, int topK) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    var q = embeddings.embed(null, null, query);
    float[] qv = q.vector();
    normalizeInPlace(qv);

    List<KnowledgeChunkEmbeddingRepository.EmbeddingRow> rows =
        embeddingRepo.listRowsByBaseAndActiveJob(knowledgeBaseId);

    return rows.stream()
        .map(r -> toHit(qv, r))
        .filter(Objects::nonNull)
        .sorted(Comparator.comparingDouble(RetrievalHit::score).reversed())
        .limit(Math.max(1, topK))
        .toList();
  }

  private RetrievalHit toHit(float[] qv, KnowledgeChunkEmbeddingRepository.EmbeddingRow row) {
    try {
      float[] cv = objectMapper.readValue(row.vectorJson(), float[].class);
      normalizeInPlace(cv);
      double score = dot(qv, cv);
      String excerpt = excerpt(row.content(), 240);
      return new RetrievalHit(
          row.documentId(), row.documentTitle(), row.sourceUri(), row.chunkId(), excerpt, score);
    } catch (Exception e) {
      return null;
    }
  }

  private static double dot(float[] a, float[] b) {
    int n = Math.min(a.length, b.length);
    double s = 0.0;
    for (int i = 0; i < n; i++) {
      s += (double) a[i] * (double) b[i];
    }
    return s;
  }

  private static void normalizeInPlace(float[] v) {
    if (v == null || v.length == 0) return;
    double sum = 0.0;
    for (float x : v) {
      sum += (double) x * (double) x;
    }
    double norm = Math.sqrt(sum);
    if (norm <= 0) return;
    for (int i = 0; i < v.length; i++) {
      v[i] = (float) (v[i] / norm);
    }
  }

  private static String excerpt(String content, int maxLen) {
    if (content == null) return "";
    String s = content.trim();
    if (s.length() <= maxLen) return s;
    return s.substring(0, maxLen);
  }

  public record RetrievalHit(
      long documentId,
      String documentTitle,
      String sourceUri,
      long chunkId,
      String excerpt,
      double score) {}
}
