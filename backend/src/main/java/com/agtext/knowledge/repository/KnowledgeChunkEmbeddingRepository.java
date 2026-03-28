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

@Repository
public class KnowledgeChunkEmbeddingRepository {
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeChunkEmbeddingRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

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

  public void deleteByJobId(long jobId) {
    jdbcTemplate.update("delete from knowledge_chunk_embeddings where import_job_id=?", jobId);
  }

  public List<KnowledgeChunkEmbedding> listByJobId(long jobId) {
    return jdbcTemplate.query(
        "select * from knowledge_chunk_embeddings where import_job_id=?", mapper(), jobId);
  }

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

  private static KnowledgeChunkEmbedding map(ResultSet rs) throws SQLException {
    return new KnowledgeChunkEmbedding(
        rs.getLong("id"),
        rs.getLong("knowledge_chunk_id"),
        rs.getLong("import_job_id"),
        rs.getString("provider"),
        rs.getString("model"),
        (Integer) rs.getObject("dim"),
        rs.getString("vector_json"),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }

  public record EmbeddingRow(
      String vectorJson,
      long chunkId,
      String content,
      long documentId,
      String documentTitle,
      String sourceUri) {}

  public record ExistingVector(String vectorJson, Integer dim) {}
}
