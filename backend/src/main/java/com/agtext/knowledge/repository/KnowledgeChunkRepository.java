package com.agtext.knowledge.repository;

import com.agtext.knowledge.domain.KnowledgeChunk;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeChunkRepository {
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeChunkRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(
      long documentId, long importJobId, int chunkIndex, String content, String chunkHash) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "insert into knowledge_chunks(knowledge_document_id, import_job_id, chunk_index, content, chunk_hash) values (?,?,?,?,?)",
                  new String[] {"id"});
          ps.setLong(1, documentId);
          ps.setLong(2, importJobId);
          ps.setInt(3, chunkIndex);
          ps.setString(4, content);
          ps.setString(5, chunkHash);
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create knowledge chunk");
    }
    return key.longValue();
  }

  public List<KnowledgeChunk> listAllByDocumentId(long documentId) {
    return jdbcTemplate.query(
        "select * from knowledge_chunks where knowledge_document_id=? order by chunk_index asc",
        mapper(),
        documentId);
  }

  public List<KnowledgeChunk> listAllByDocumentIdAndImportJobId(long documentId, long importJobId) {
    return jdbcTemplate.query(
        "select * from knowledge_chunks where knowledge_document_id=? and import_job_id=? order by chunk_index asc",
        mapper(),
        documentId,
        importJobId);
  }

  public List<KnowledgeChunk> listByDocumentId(long documentId, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    return jdbcTemplate.query(
        "select * from knowledge_chunks where knowledge_document_id=? order by chunk_index asc limit ? offset ?",
        mapper(),
        documentId,
        pageSize,
        offset);
  }

  public List<KnowledgeChunk> listByDocumentIdAndImportJobId(
      long documentId, long importJobId, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    return jdbcTemplate.query(
        "select * from knowledge_chunks where knowledge_document_id=? and import_job_id=? order by chunk_index asc limit ? offset ?",
        mapper(),
        documentId,
        importJobId,
        pageSize,
        offset);
  }

  public long countByDocumentId(long documentId) {
    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from knowledge_chunks where knowledge_document_id=?",
            Long.class,
            documentId);
    return total == null ? 0 : total;
  }

  public long countByDocumentIdAndImportJobId(long documentId, long importJobId) {
    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from knowledge_chunks where knowledge_document_id=? and import_job_id=?",
            Long.class,
            documentId,
            importJobId);
    return total == null ? 0 : total;
  }

  private RowMapper<KnowledgeChunk> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  private static KnowledgeChunk map(ResultSet rs) throws SQLException {
    return new KnowledgeChunk(
        rs.getLong("id"),
        rs.getLong("knowledge_document_id"),
        (Long) rs.getObject("import_job_id"),
        rs.getInt("chunk_index"),
        rs.getString("content"),
        rs.getString("chunk_hash"),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
