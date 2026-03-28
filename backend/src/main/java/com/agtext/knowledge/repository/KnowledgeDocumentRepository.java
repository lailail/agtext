package com.agtext.knowledge.repository;

import com.agtext.knowledge.domain.KnowledgeDocument;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeDocumentRepository {
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeDocumentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(long knowledgeBaseId, String sourceType, String sourceUri, String title) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "insert into knowledge_documents(knowledge_base_id, source_type, source_uri, title, status) values (?,?,?,?,?)",
                  new String[] {"id"});
          ps.setLong(1, knowledgeBaseId);
          ps.setString(2, sourceType);
          ps.setString(3, sourceUri);
          ps.setString(4, title);
          ps.setString(5, "pending");
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create knowledge document");
    }
    return key.longValue();
  }

  public Optional<KnowledgeDocument> findById(long id) {
    List<KnowledgeDocument> rows =
        jdbcTemplate.query("select * from knowledge_documents where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  public void updateAfterParse(
      long id,
      String status,
      String parseStatus,
      String indexStatus,
      String errorMessage,
      String contentHash,
      Long latestImportJobId) {
    jdbcTemplate.update(
        "update knowledge_documents set status=?, parse_status=?, index_status=?, error_message=?, content_hash=?, latest_import_job_id=? where id=?",
        status,
        parseStatus,
        indexStatus,
        errorMessage,
        contentHash,
        latestImportJobId,
        id);
  }

  public void updateIndexReady(long id, long jobId, String indexStatus) {
    jdbcTemplate.update(
        "update knowledge_documents set status='ready', index_status=?, active_import_job_id=?, latest_import_job_id=? where id=?",
        indexStatus,
        jobId,
        jobId,
        id);
  }

  public void updateActiveIndex(long id, long jobId, String indexStatus) {
    jdbcTemplate.update(
        "update knowledge_documents set status='ready', index_status=?, active_import_job_id=? where id=?",
        indexStatus,
        jobId,
        id);
  }

  public List<KnowledgeDocument> listByBaseId(long knowledgeBaseId, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    return jdbcTemplate.query(
        "select * from knowledge_documents where knowledge_base_id=? order by id desc limit ? offset ?",
        mapper(),
        knowledgeBaseId,
        pageSize,
        offset);
  }

  public long countByBaseId(long knowledgeBaseId) {
    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from knowledge_documents where knowledge_base_id=?",
            Long.class,
            knowledgeBaseId);
    return total == null ? 0 : total;
  }

  private RowMapper<KnowledgeDocument> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  private static KnowledgeDocument map(ResultSet rs) throws SQLException {
    return new KnowledgeDocument(
        rs.getLong("id"),
        rs.getLong("knowledge_base_id"),
        rs.getString("source_type"),
        rs.getString("source_uri"),
        rs.getString("title"),
        rs.getString("status"),
        rs.getString("parse_status"),
        rs.getString("index_status"),
        rs.getString("error_message"),
        rs.getString("content_hash"),
        (Long) rs.getObject("latest_import_job_id"),
        (Long) rs.getObject("active_import_job_id"),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
