package com.agtext.tool.repository;

import com.agtext.tool.domain.NotificationItem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository {
  private final JdbcTemplate jdbcTemplate;

  public NotificationRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(
      String type, String title, String content, String refType, String refId, String status) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "insert into notifications(type, title, content, ref_type, ref_id, status) values (?,?,?,?,?,?)",
                  new String[] {"id"});
          ps.setString(1, type);
          ps.setString(2, title);
          ps.setString(3, content);
          ps.setString(4, refType);
          ps.setString(5, refId);
          ps.setString(6, status);
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create notification");
    }
    return key.longValue();
  }

  public List<NotificationItem> list(String status, String type, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    StringBuilder sql = new StringBuilder("select * from notifications where 1=1");
    List<Object> args = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    if (type != null && !type.isBlank()) {
      sql.append(" and type=?");
      args.add(type);
    }
    sql.append(" order by id desc limit ? offset ?");
    args.add(pageSize);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), mapper(), args.toArray());
  }

  public long count(String status, String type) {
    StringBuilder sql = new StringBuilder("select count(*) from notifications where 1=1");
    List<Object> args = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    if (type != null && !type.isBlank()) {
      sql.append(" and type=?");
      args.add(type);
    }
    Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return total == null ? 0 : total;
  }

  public long countUnread() {
    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from notifications where status='unread'", Long.class);
    return total == null ? 0 : total;
  }

  public void updateStatus(long id, String status) {
    jdbcTemplate.update("update notifications set status=? where id=?", status, id);
  }

  private RowMapper<NotificationItem> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  private static NotificationItem map(ResultSet rs) throws SQLException {
    return new NotificationItem(
        rs.getLong("id"),
        rs.getString("type"),
        rs.getString("title"),
        rs.getString("content"),
        rs.getString("ref_type"),
        rs.getString("ref_id"),
        rs.getString("status"),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
