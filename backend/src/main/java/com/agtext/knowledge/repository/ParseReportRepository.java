package com.agtext.knowledge.repository;

import com.agtext.knowledge.domain.ParseReport;
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
public class ParseReportRepository {
  private final JdbcTemplate jdbcTemplate;

  public ParseReportRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(
      long documentId,
      Long jobId,
      String summary,
      Long extractedChars,
      Integer pageCount,
      Integer chunkCount,
      String parserName,
      String failedAt,
      String samplePreview) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "insert into parse_reports(knowledge_document_id, job_id, summary, extracted_chars, page_count, chunk_count, parser_name, failed_at, sample_preview) values (?,?,?,?,?,?,?,?,?)",
                  new String[] {"id"});
          ps.setLong(1, documentId);
          if (jobId == null) {
            ps.setObject(2, null);
          } else {
            ps.setLong(2, jobId);
          }
          ps.setString(3, summary);
          ps.setObject(4, extractedChars);
          ps.setObject(5, pageCount);
          ps.setObject(6, chunkCount);
          ps.setString(7, parserName);
          ps.setString(8, failedAt);
          ps.setString(9, samplePreview);
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create parse report");
    }
    return key.longValue();
  }

  public Optional<ParseReport> findLatestByJobId(long jobId) {
    List<ParseReport> rows =
        jdbcTemplate.query(
            "select * from parse_reports where job_id=? order by id desc limit 1", mapper(), jobId);
    return rows.stream().findFirst();
  }

  private RowMapper<ParseReport> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  private static ParseReport map(ResultSet rs) throws SQLException {
    return new ParseReport(
        rs.getLong("id"),
        rs.getLong("knowledge_document_id"),
        (Long) rs.getObject("job_id"),
        rs.getString("summary"),
        (Long) rs.getObject("extracted_chars"),
        (Integer) rs.getObject("page_count"),
        (Integer) rs.getObject("chunk_count"),
        rs.getString("parser_name"),
        rs.getString("failed_at"),
        rs.getString("sample_preview"),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
