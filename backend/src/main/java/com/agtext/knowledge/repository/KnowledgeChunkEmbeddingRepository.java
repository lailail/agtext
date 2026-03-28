package com.agtext.knowledge.repository;

import com.agtext.knowledge.domain.KnowledgeChunkEmbedding;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 知识分片向量持久层：负责管理分片经过 Embedding 模型处理后的高维向量数据。
 * 该类不仅负责基础的 CRUD，还包含了支持“增量索引”和“全量同步”的核心查询逻辑。
 */
@Repository
public class KnowledgeChunkEmbeddingRepository {
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeChunkEmbeddingRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 插入或更新向量记录（Upsert 模式）。
   * 逻辑：如果 chunkId 对应的向量已存在，则更新维度和向量数据，防止重复处理导致的异常。
   */
  public void upsert(
          long chunkId, long jobId, String provider, String model, Integer dim, String vectorJson) {
    jdbcTemplate.update(
            "insert into knowledge_chunk_embeddings(knowledge_chunk_id, import_job_id, provider, model, dim, vector_json) values (?,?,?,?,?,?) "
                    + "on duplicate key update dim=values(dim), vector_json=values(vector_json)",
            chunkId,
            jobId,
            provider,
            model,
            dim,
            vectorJson);
  }

  /**
   * 按任务 ID 删除向量。
   * 通常用于清理导入失败的任务残留数据，或在重新索引时进行局部清理。
   */
  public void deleteByJobId(long jobId) {
    jdbcTemplate.update("delete from knowledge_chunk_embeddings where import_job_id=?", jobId);
  }

  /**
   * 获取特定任务产生的所有向量记录。
   */
  public List<KnowledgeChunkEmbedding> listByJobId(long jobId) {
    return jdbcTemplate.query(
            "select * from knowledge_chunk_embeddings where import_job_id=?", mapper(), jobId);
  }

  /**
   * 获取指定知识库下所有处于“活跃状态（Active）”任务的向量详情。
   * 核心逻辑：
   * 1. 通过 JOIN 关联文档、分片和向量表。
   * 2. 严格过滤 kd.active_import_job_id，确保只获取当前生效版本的向量。
   * 3. 该查询通常用于将关系型数据库中的向量数据同步到专用的向量数据库（如 Milvus/Elasticsearch）。
   */
  public List<EmbeddingRow> listRowsByBaseAndActiveJob(long baseId) {
    return jdbcTemplate.query(
            "select kce.vector_json, kc.id as chunk_id, kc.content, kd.id as document_id, kd.title as document_title, kd.source_uri "
                    + "from knowledge_documents kd "
                    + "join knowledge_chunks kc on kc.knowledge_document_id=kd.id "
                    + "join knowledge_chunk_embeddings kce on kce.knowledge_chunk_id=kc.id and kce.import_job_id=kd.active_import_job_id "
                    + "where kd.knowledge_base_id=? and kd.active_import_job_id is not null",
            (rs, rowNum) ->
                    new EmbeddingRow(
                            rs.getString("vector_json"),
                            rs.getLong("chunk_id"),
                            rs.getString("content"),
                            rs.getLong("document_id"),
                            rs.getString("document_title"),
                            rs.getString("source_uri")),
            baseId);
  }

  /**
   * 根据文档 ID 和内容哈希值查找已存在的向量。
   * 核心逻辑（增量索引/缓存优化）：
   * 1. RAG 优化：如果文档内容未变（Hash 一致），且使用的模型一致，则直接复用旧向量。
   * 2. 目的：极大地降低调用 Embedding 外部接口（如 OpenAI）产生的 Token 费用和时间开销。
   */
  public Optional<ExistingVector> findExistingVectorByDocumentAndChunkHash(
          long documentId, String chunkHash, String provider, String model) {
    if (chunkHash == null || chunkHash.isBlank()) {
      return Optional.empty();
    }
    List<ExistingVector> rows =
            jdbcTemplate.query(
                    "select kce.vector_json, kce.dim "
                            + "from knowledge_chunks kc "
                            + "join knowledge_chunk_embeddings kce on kce.knowledge_chunk_id=kc.id "
                            + "where kc.knowledge_document_id=? and kc.chunk_hash=? and kce.provider=? and kce.model=? "
                            + "order by kce.updated_at desc limit 1",
                    (rs, rowNum) ->
                            new ExistingVector(rs.getString("vector_json"), (Integer) rs.getObject("dim")),
                    documentId,
                    chunkHash,
                    provider,
                    model);
    return rows.stream().findFirst();
  }

  private RowMapper<KnowledgeChunkEmbedding> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  /**
   * 结果集到领域模型的映射逻辑。
   */
  private static KnowledgeChunkEmbedding map(ResultSet rs) throws SQLException {
    return new KnowledgeChunkEmbedding(
            rs.getLong("id"),
            rs.getLong("knowledge_chunk_id"),
            rs.getLong("import_job_id"),
            rs.getString("provider"),
            rs.getString("model"),
            (Integer) rs.getObject("dim"), // 使用 getObject 处理可能为 null 的 Integer
            rs.getString("vector_json"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }

  /**
   * 数据传输对象（DTO）：用于将向量连同其上下文信息（文档标题、正文、URI）一次性查出。
   * 适用于向量数据库同步逻辑。
   */
  public record EmbeddingRow(
          String vectorJson,
          long chunkId,
          String content,
          long documentId,
          String documentTitle,
          String sourceUri) {}

  /**
   * 增量更新查找结果载体。
   */
  public record ExistingVector(String vectorJson, Integer dim) {}
}