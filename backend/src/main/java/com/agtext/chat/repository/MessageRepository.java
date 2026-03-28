package com.agtext.chat.repository;

import com.agtext.chat.domain.Message;
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
public class MessageRepository {
  private final JdbcTemplate jdbcTemplate;

  public MessageRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(
      long conversationId,
      String role,
      String content,
      String provider,
      String modelName,
      Integer tokens) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "insert into messages(conversation_id, role, content, provider, model_name, tokens) values (?,?,?,?,?,?)",
                  new String[] {"id"});
          ps.setLong(1, conversationId);
          ps.setString(2, role);
          ps.setString(3, content);
          ps.setString(4, provider);
          ps.setString(5, modelName);
          if (tokens == null) {
            ps.setObject(6, null);
          } else {
            ps.setInt(6, tokens);
          }
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create message");
    }
    return key.longValue();
  }

  public List<Message> listByConversationId(long conversationId) {
    return jdbcTemplate.query(
        "select * from messages where conversation_id=? order by id asc", mapper(), conversationId);
  }

  public List<Message> listRecentByConversationId(long conversationId, int limit) {
    return jdbcTemplate.query(
        "select * from messages where conversation_id=? order by id desc limit ?",
        mapper(),
        conversationId,
        limit);
  }

  private RowMapper<Message> mapper() {
    return (rs, rowNum) -> mapMessage(rs);
  }

  private static Message mapMessage(ResultSet rs) throws SQLException {
    Integer tokens = (Integer) rs.getObject("tokens");
    return new Message(
        rs.getLong("id"),
        rs.getLong("conversation_id"),
        rs.getString("role"),
        rs.getString("content"),
        rs.getString("provider"),
        rs.getString("model_name"),
        tokens,
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
