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

/**
 * 知识向量索引服务：负责将文本分片转换为高维向量并持久化。
 * 核心逻辑：
 * 1. 成本优化：通过哈希校验和历史版本对比，最大限度减少对外部 Embedding API（如 OpenAI）的重复调用。
 * 2. 状态驱动：实时更新异步任务进度及文档的可检索状态。
 * 3. 兼容性：支持多供应商、多模型配置及增量/存量索引构建。
 */
@Service
public class KnowledgeEmbeddingIndexService {
  private final EmbeddingService embeddings; // 外部模型接口服务
  private final EmbeddingSettingsProperties embeddingProps; // 默认模型配置
  private final ObjectMapper objectMapper; // 向量序列化工具
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

  /**
   * 构建全量索引：针对新文档或重新上传的完整流程。
   */
  @Transactional
  public void buildIndex(long jobId, long documentId) {
    buildIndex(jobId, documentId, jobId, true);
  }

  /**
   * 基于指定分片版本构建索引：通常用于对现有分片更换 Embedding 模型进行重索引。
   */
  @Transactional
  public void buildIndex(long jobId, long documentId, long sourceChunkJobId) {
    buildIndex(jobId, documentId, sourceChunkJobId, false);
  }

  /**
   * 索引构建核心私有逻辑
   * @param sourceChunkJobId 文本分片的来源任务 ID
   * @param updateLatestImportJobId 是否同步更新文档的最后一次导入记录标识
   */
  private void buildIndex(
          long jobId, long documentId, long sourceChunkJobId, boolean updateLatestImportJobId) {
    // 1. 环境准备与任务初始化
    jobs.findById(jobId).orElseThrow(() -> new IllegalStateException("Job not found"));
    jobs.markRunning(jobId, "embed", 80); // 进入向量化阶段，设定进度为 80%

    // 2. 清理当前任务可能存在的历史残留向量数据（保证重试幂等性）
    chunkEmbeddings.deleteByJobId(jobId);

    // 3. 获取待处理的文本分片列表
    var list = chunks.listAllByDocumentIdAndImportJobId(documentId, sourceChunkJobId);

    // 4. 解析 Embedding 模型参数（供应商、模型名）
    String provider = resolveProvider(null);
    String model = resolveModel(provider, null);

    // 5. 遍历分片并执行向量化
    Set<String> seenHashes = new HashSet<>(); // 用于当前文档内的分片去重
    for (var c : list) {
      // 5.1 文档内查重：避免同一文档中内容相同的分片重复计算向量
      if (c.chunkHash() != null && !c.chunkHash().isBlank() && !seenHashes.add(c.chunkHash())) {
        continue;
      }

      // 5.2 历史跨任务查重（核心成本控制）：
      // 如果该文档的其他任务已经对相同哈希的内容生成过相同模型的向量，则直接复用。
      var existing =
              chunkEmbeddings.findExistingVectorByDocumentAndChunkHash(
                      documentId, c.chunkHash(), provider, model);

      if (existing.isPresent()) {
        chunkEmbeddings.upsert(
                c.id(), jobId, provider, model, existing.get().dim(), existing.get().vectorJson());
        continue;
      }

      // 5.3 调用外部 API：计算 Embedding 向量
      EmbeddingResponse e = embeddings.embed(provider, model, c.content());
      String json;
      try {
        json = objectMapper.writeValueAsString(e.vector());
      } catch (Exception ex) {
        throw new IllegalStateException("Failed to serialize embedding", ex);
      }

      // 5.4 存储新生成的向量记录
      chunkEmbeddings.upsert(c.id(), jobId, e.provider(), e.model(), e.vector().length, json);
    }

    // 6. 状态更新与收尾
    jobs.markRunning(jobId, "index", 95); // 进入索引就绪阶段
    if (updateLatestImportJobId) {
      // 场景：新文档导入，直接使该索引生效
      documents.updateIndexReady(documentId, jobId, "ready");
    } else {
      // 场景：重索引，仅更新当前生效的索引版本
      documents.updateActiveIndex(documentId, jobId, "ready");
    }
    jobs.markSucceeded(jobId); // 任务全部成功
  }

  /**
   * 动态解析向量供应商配置
   */
  private String resolveProvider(String providerOverride) {
    return providerOverride == null || providerOverride.isBlank()
            ? embeddingProps.defaultProvider()
            : providerOverride;
  }

  /**
   * 动态解析模型配置：根据供应商匹配其对应的默认模型或全局默认模型
   */
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