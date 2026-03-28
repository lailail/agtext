package com.agtext.task.repository;

import com.agtext.task.domain.GoalItem;
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
public class GoalRepository {
  private final JdbcTemplate jdbcTemplate;

  public GoalRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(String title, String description) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "insert into goal_items(title, description, status) values (?,?,?)",
                  new String[] {"id"});
          ps.setString(1, title);
          ps.setString(2, description);
          ps.setString(3, "active");
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create goal");
    }
    return key.longValue();
  }

  public Optional<GoalItem> findById(long id) {
    List<GoalItem> rows = jdbcTemplate.query("select * from goal_items where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  public List<GoalItem> list(String status, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    StringBuilder sql = new StringBuilder("select * from goal_items where 1=1");
    List<Object> args = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    sql.append(" order by id desc limit ? offset ?");
    args.add(pageSize);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), mapper(), args.toArray());
  }

  public long count(String status) {
    StringBuilder sql = new StringBuilder("select count(*) from goal_items where 1=1");
    List<Object> args = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return total == null ? 0 : total;
  }

  public void update(long id, String title, String description, String status) {
    jdbcTemplate.update(
        "update goal_items set title=?, description=?, status=? where id=?",
        title,
        description,
        status,
        id);
  }

  private RowMapper<GoalItem> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  private static GoalItem map(ResultSet rs) throws SQLException {
    return new GoalItem(
        rs.getLong("id"),
        rs.getString("title"),
        rs.getString("description"),
        rs.getString("status"),
        toInstant(rs.getTimestamp("created_at")),
        toInstant(rs.getTimestamp("updated_at")));
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
