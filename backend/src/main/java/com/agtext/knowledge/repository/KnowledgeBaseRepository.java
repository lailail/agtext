package com.agtext.knowledge.repository;

import com.agtext.knowledge.domain.KnowledgeBase;
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
public class KnowledgeBaseRepository {
  private final JdbcTemplate jdbcTemplate;

  public KnowledgeBaseRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(String name, String description) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "insert into knowledge_bases(name, description) values (?, ?)",
                  new String[] {"id"});
          ps.setString(1, name);
          ps.setString(2, description);
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create knowledge base");
    }
    return key.longValue();
  }

  public Optional<KnowledgeBase> findById(long id) {
    List<KnowledgeBase> rows =
        jdbcTemplate.query("select * from knowledge_bases where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  public List<KnowledgeBase> list(int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    return jdbcTemplate.query(
        "select * from knowledge_bases order by id desc limit ? offset ?",
        mapper(),
        pageSize,
        offset);
  }

  public long countAll() {
    Long total = jdbcTemplate.queryForObject("select count(*) from knowledge_bases", Long.class);
    return total == null ? 0 : total;
  }

  private RowMapper<KnowledgeBase> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  private static KnowledgeBase map(ResultSet rs) throws SQLException {
    return new KnowledgeBase(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getString("description"),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
