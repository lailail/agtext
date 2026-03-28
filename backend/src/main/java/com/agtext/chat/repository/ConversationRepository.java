package com.agtext.chat.repository;

import com.agtext.chat.domain.Conversation;
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
public class ConversationRepository {
  private final JdbcTemplate jdbcTemplate;

  public ConversationRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(String title) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "insert into conversations(title, status) values (?, ?)", new String[] {"id"});
          ps.setString(1, title);
          ps.setString(2, "active");
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create conversation");
    }
    return key.longValue();
  }

  public Optional<Conversation> findById(long id) {
    List<Conversation> rows =
        jdbcTemplate.query("select * from conversations where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  public List<Conversation> list(int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    return jdbcTemplate.query(
        "select * from conversations order by id desc limit ? offset ?",
        mapper(),
        pageSize,
        offset);
  }

  public long countAll() {
    Long total = jdbcTemplate.queryForObject("select count(*) from conversations", Long.class);
    return total == null ? 0 : total;
  }

  private RowMapper<Conversation> mapper() {
    return (rs, rowNum) -> mapConversation(rs);
  }

  private static Conversation mapConversation(ResultSet rs) throws SQLException {
    return new Conversation(
        rs.getLong("id"),
        rs.getString("title"),
        rs.getString("status"),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
