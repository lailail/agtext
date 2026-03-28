package com.agtext.knowledge.service;

import com.agtext.knowledge.repository.KnowledgeChunkEmbeddingRepository;
import com.agtext.model.service.EmbeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 知识检索服务：实现基于向量相似度的语义搜索。
 */
@Service
public class KnowledgeRetrievalService {
  private final EmbeddingService embeddings; // 向量生成服务（对接 OpenAI/HuggingFace 等）
  private final KnowledgeChunkEmbeddingRepository embeddingRepo; // 向量存储仓库
  private final ObjectMapper objectMapper; // 用于解析存储在数据库中的 JSON 向量

  public KnowledgeRetrievalService(
          EmbeddingService embeddings,
          KnowledgeChunkEmbeddingRepository embeddingRepo,
          ObjectMapper objectMapper) {
    this.embeddings = embeddings;
    this.embeddingRepo = embeddingRepo;
    this.objectMapper = objectMapper;
  }

  /**
   * 执行检索
   * @param knowledgeBaseId 目标知识库ID
   * @param query 用户输入的搜索词或问题
   * @param topK 返回相关度最高的切片数量
   * @return 检索命中的结果列表（按相似度降序排列）
   */
  public List<RetrievalHit> retrieve(long knowledgeBaseId, String query, int topK) {
    if (query == null || query.isBlank()) {
      return List.of();
    }

    // 1. 将用户查询文本转化为向量
    var q = embeddings.embed(null, null, query);
    float[] qv = q.vector();

    // 2. 向量归一化：为了后续直接通过点积（Dot Product）计算余弦相似度
    normalizeInPlace(qv);

    // 3. 从数据库加载指定知识库中所有“活跃任务”产生的向量行
    // 注意：在数据量极大时，此处直接 listRows 会有性能瓶颈，通常会引入向量数据库（如 Milvus/Pinecone）
    List<KnowledgeChunkEmbeddingRepository.EmbeddingRow> rows =
            embeddingRepo.listRowsByBaseAndActiveJob(knowledgeBaseId);

    // 4. 流式处理：计算分值 -> 过滤异常 -> 排序 -> 取前 K 个
    return rows.stream()
            .map(r -> toHit(qv, r)) // 将原始行转换为带分数的命中对象
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingDouble(RetrievalHit::score).reversed()) // 相似度从高到低
            .limit(Math.max(1, topK))
            .toList();
  }

  /**
   * 计算查询向量与存储向量的相似度
   */
  private RetrievalHit toHit(float[] qv, KnowledgeChunkEmbeddingRepository.EmbeddingRow row) {
    try {
      // 1. 反序列化存储在 DB 中的向量 JSON 字符串为 float 数组
      float[] cv = objectMapper.readValue(row.vectorJson(), float[].class);

      // 2. 归一化存储向量
      normalizeInPlace(cv);

      // 3. 计算点积：在两个向量都已归一化的情况下，点积即等于余弦相似度
      double score = dot(qv, cv);

      // 4. 生成内容摘要（前 240 字符）
      String excerpt = excerpt(row.content(), 240);

      return new RetrievalHit(
              row.documentId(), row.documentTitle(), row.sourceUri(), row.chunkId(), excerpt, score);
    } catch (Exception e) {
      // 忽略解析失败的向量行
      return null;
    }
  }

  /**
   * 向量点积计算：$\sum (a_i \times b_i)$
   */
  private static double dot(float[] a, float[] b) {
    int n = Math.min(a.length, b.length);
    double s = 0.0;
    for (int i = 0; i < n; i++) {
      s += (double) a[i] * (double) b[i];
    }
    return s;
  }

  /**
   * 原地执行 L2 归一化：将向量长度缩放为 1
   * 公式：$v_{new} = \frac{v}{\sqrt{\sum v_i^2}}$
   */
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

  /**
   * 文本摘要处理
   */
  private static String excerpt(String content, int maxLen) {
    if (content == null) return "";
    String s = content.trim();
    if (s.length() <= maxLen) return s;
    return s.substring(0, maxLen);
  }

  /**
   * 检索命中的数据模型
   */
  public record RetrievalHit(
          long documentId,
          String documentTitle,
          String sourceUri,
          long chunkId,
          String excerpt,
          double score) {}
}