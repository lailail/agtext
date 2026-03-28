package com.agtext.knowledge.service;

import com.agtext.knowledge.repository.KnowledgeChunkEmbeddingRepository;
import com.agtext.knowledge.repository.KnowledgeChunkRepository;
import com.agtext.knowledge.repository.KnowledgeDocumentRepository;
import com.agtext.knowledge.repository.KnowledgeImportJobRepository;
import com.agtext.model.domain.EmbeddingResponse;
import com.agtext.model.service.EmbeddingService;
import com.agtext.model.service.EmbeddingSettingsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeEmbeddingIndexService {
  private final EmbeddingService embeddings;
  private final EmbeddingSettingsProperties embeddingProps;
  private final ObjectMapper objectMapper;
  private final KnowledgeChunkRepository chunks;
  private final KnowledgeChunkEmbeddingRepository chunkEmbeddings;
  private final KnowledgeDocumentRepository documents;
  private final KnowledgeImportJobRepository jobs;

  public KnowledgeEmbeddingIndexService(
      EmbeddingService embeddings,
      EmbeddingSettingsProperties embeddingProps,
      ObjectMapper objectMapper,
      KnowledgeChunkRepository chunks,
      KnowledgeChunkEmbeddingRepository chunkEmbeddings,
      KnowledgeDocumentRepository documents,
      KnowledgeImportJobRepository jobs) {
    this.embeddings = embeddings;
    this.embeddingProps = embeddingProps;
    this.objectMapper = objectMapper;
    this.chunks = chunks;
    this.chunkEmbeddings = chunkEmbeddings;
    this.documents = documents;
    this.jobs = jobs;
  }

  @Transactional
  public void buildIndex(long jobId, long documentId) {
    buildIndex(jobId, documentId, jobId, true);
  }

  @Transactional
  public void buildIndex(long jobId, long documentId, long sourceChunkJobId) {
    buildIndex(jobId, documentId, sourceChunkJobId, false);
  }

  private void buildIndex(
      long jobId, long documentId, long sourceChunkJobId, boolean updateLatestImportJobId) {
    jobs.findById(jobId).orElseThrow(() -> new IllegalStateException("Job not found"));
    jobs.markRunning(jobId, "embed", 80);

    chunkEmbeddings.deleteByJobId(jobId);
    var list = chunks.listAllByDocumentIdAndImportJobId(documentId, sourceChunkJobId);
    String provider = resolveProvider(null);
    String model = resolveModel(provider, null);
    Set<String> seenHashes = new HashSet<>();
    for (var c : list) {
      if (c.chunkHash() != null && !c.chunkHash().isBlank() && !seenHashes.add(c.chunkHash())) {
        continue;
      }
      var existing =
          chunkEmbeddings.findExistingVectorByDocumentAndChunkHash(
              documentId, c.chunkHash(), provider, model);
      if (existing.isPresent()) {
        chunkEmbeddings.upsert(
            c.id(), jobId, provider, model, existing.get().dim(), existing.get().vectorJson());
        continue;
      }

      EmbeddingResponse e = embeddings.embed(provider, model, c.content());
      String json;
      try {
        json = objectMapper.writeValueAsString(e.vector());
      } catch (Exception ex) {
        throw new IllegalStateException("Failed to serialize embedding", ex);
      }
      chunkEmbeddings.upsert(c.id(), jobId, e.provider(), e.model(), e.vector().length, json);
    }

    jobs.markRunning(jobId, "index", 95);
    if (updateLatestImportJobId) {
      documents.updateIndexReady(documentId, jobId, "ready");
    } else {
      documents.updateActiveIndex(documentId, jobId, "ready");
    }
    jobs.markSucceeded(jobId);
  }

  private String resolveProvider(String providerOverride) {
    return providerOverride == null || providerOverride.isBlank()
        ? embeddingProps.defaultProvider()
        : providerOverride;
  }

  private String resolveModel(String provider, String modelOverride) {
    if (modelOverride != null && !modelOverride.isBlank()) {
      return modelOverride;
    }
    EmbeddingSettingsProperties.ProviderConfig cfg = embeddingProps.providers().get(provider);
    if (cfg != null && cfg.model() != null && !cfg.model().isBlank()) {
      return cfg.model();
    }
    return embeddingProps.defaultModel();
  }
}
