package com.agtext.tool.repository;

import com.agtext.tool.domain.ConfirmationItem;
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
public class ConfirmationRepository {
  private final JdbcTemplate jdbcTemplate;

  public ConfirmationRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<ConfirmationItem> findByIdempotencyKey(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    List<ConfirmationItem> rows =
        jdbcTemplate.query("select * from confirmations where idempotency_key=?", mapper(), key);
    return rows.stream().findFirst();
  }

  public ConfirmationItem create(
      String idempotencyKey,
      String actionType,
      String refType,
      String refId,
      String summary,
      String payload) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "insert into confirmations(requester, action, idempotency_key, payload_summary, status, actor, action_type, ref_type, ref_id, summary, payload) values (?,?,?,?,?,?,?,?,?,?,?)",
                  new String[] {"id"});
          ps.setString(1, "agent");
          ps.setString(2, actionType);
          ps.setString(3, idempotencyKey);
          ps.setString(4, summary);
          ps.setString(5, "pending");
          ps.setString(6, "agent");
          ps.setString(7, actionType);
          ps.setString(8, refType);
          ps.setString(9, refId);
          ps.setString(10, summary);
          ps.setString(11, payload);
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create confirmation");
    }
    return findById(key.longValue())
        .orElseThrow(() -> new IllegalStateException("Confirmation not found"));
  }

  public Optional<ConfirmationItem> findById(long id) {
    List<ConfirmationItem> rows =
        jdbcTemplate.query("select * from confirmations where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  public void updateStatus(long id, String status) {
    jdbcTemplate.update("update confirmations set status=? where id=?", status, id);
  }

  private RowMapper<ConfirmationItem> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  private static ConfirmationItem map(ResultSet rs) throws SQLException {
    String actionType = rs.getString("action_type");
    if (actionType == null || actionType.isBlank()) {
      actionType = rs.getString("action");
    }
    String summary = rs.getString("summary");
    if (summary == null || summary.isBlank()) {
      summary = rs.getString("payload_summary");
    }
    return new ConfirmationItem(
        rs.getLong("id"),
        rs.getString("status"),
        actionType,
        rs.getString("ref_type"),
        rs.getString("ref_id"),
        summary,
        rs.getString("payload"),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
