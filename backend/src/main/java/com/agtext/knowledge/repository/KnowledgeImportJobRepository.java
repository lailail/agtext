package com.agtext.knowledge.repository;

import com.agtext.knowledge.domain.KnowledgeImportJob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeImportJobRepository {
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeImportJobRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(long knowledgeBaseId, Long documentId) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "insert into knowledge_import_jobs(knowledge_base_id, document_id, status, stage, progress, started_at) values (?,?,?,?,?,?)",
                  new String[] {"id"});
          ps.setLong(1, knowledgeBaseId);
          if (documentId == null) {
            ps.setObject(2, null);
          } else {
            ps.setLong(2, documentId);
          }
          ps.setString(3, "queued");
          ps.setString(4, "queued");
          ps.setInt(5, 0);
          ps.setObject(6, null);
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create import job");
    }
    return key.longValue();
  }

  public Optional<KnowledgeImportJob> findById(long id) {
    List<KnowledgeImportJob> rows =
        jdbcTemplate.query("select * from knowledge_import_jobs where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  public List<KnowledgeImportJob> list(
      Long knowledgeBaseId, Long documentId, String status, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    StringBuilder sql = new StringBuilder("select * from knowledge_import_jobs where 1=1");
    List<Object> args = new ArrayList<>();
    if (knowledgeBaseId != null) {
      sql.append(" and knowledge_base_id=?");
      args.add(knowledgeBaseId);
    }
    if (documentId != null) {
      sql.append(" and document_id=?");
      args.add(documentId);
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    sql.append(" order by id desc limit ? offset ?");
    args.add(pageSize);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), mapper(), args.toArray());
  }

  public long count(Long knowledgeBaseId, Long documentId, String status) {
    StringBuilder sql = new StringBuilder("select count(*) from knowledge_import_jobs where 1=1");
    List<Object> args = new ArrayList<>();
    if (knowledgeBaseId != null) {
      sql.append(" and knowledge_base_id=?");
      args.add(knowledgeBaseId);
    }
    if (documentId != null) {
      sql.append(" and document_id=?");
      args.add(documentId);
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return total == null ? 0 : total;
  }

  public void markRunning(long jobId, String stage, int progress) {
    jdbcTemplate.update(
        "update knowledge_import_jobs set status='running', stage=?, progress=?, started_at=coalesce(started_at, current_timestamp) where id=?",
        stage,
        progress,
        jobId);
  }

  public void markSucceeded(long jobId) {
    jdbcTemplate.update(
        "update knowledge_import_jobs set status='succeeded', stage='done', progress=100, finished_at=current_timestamp where id=?",
        jobId);
  }

  public void markFailed(long jobId, String stage, String errorMessage) {
    jdbcTemplate.update(
        "update knowledge_import_jobs set status='failed', stage=?, error_message=?, finished_at=current_timestamp where id=?",
        stage,
        errorMessage,
        jobId);
  }

  public void cancel(long jobId) {
    jdbcTemplate.update(
        "update knowledge_import_jobs set status='cancelled', cancelled_at=current_timestamp, finished_at=current_timestamp where id=?",
        jobId);
  }

  private RowMapper<KnowledgeImportJob> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  private static KnowledgeImportJob map(ResultSet rs) throws SQLException {
    return new KnowledgeImportJob(
        rs.getLong("id"),
        rs.getLong("knowledge_base_id"),
        (Long) rs.getObject("document_id"),
        rs.getString("status"),
        toInstant(rs.getTimestamp("started_at")),
        toInstant(rs.getTimestamp("finished_at")),
        rs.getString("stage"),
        (Integer) rs.getObject("progress"),
        rs.getString("error_message"),
        toInstant(rs.getTimestamp("cancelled_at")),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
