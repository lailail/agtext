package com.agtext.task.repository;

import com.agtext.task.domain.PlanItem;
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
public class PlanRepository {
  private final JdbcTemplate jdbcTemplate;

  public PlanRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(Long goalId, String title, String description) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "insert into plan_items(goal_id, title, description, status) values (?,?,?,?)",
                  new String[] {"id"});
          ps.setObject(1, goalId);
          ps.setString(2, title);
          ps.setString(3, description);
          ps.setString(4, "active");
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create plan");
    }
    return key.longValue();
  }

  public Optional<PlanItem> findById(long id) {
    List<PlanItem> rows = jdbcTemplate.query("select * from plan_items where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  public List<PlanItem> list(Long goalId, String status, int page, int pageSize) {
    int offset = Math.max(0, (page - 1) * pageSize);
    StringBuilder sql = new StringBuilder("select * from plan_items where 1=1");
    List<Object> args = new ArrayList<>();
    if (goalId != null) {
      sql.append(" and goal_id=?");
      args.add(goalId);
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

  public long count(Long goalId, String status) {
    StringBuilder sql = new StringBuilder("select count(*) from plan_items where 1=1");
    List<Object> args = new ArrayList<>();
    if (goalId != null) {
      sql.append(" and goal_id=?");
      args.add(goalId);
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and status=?");
      args.add(status);
    }
    Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return total == null ? 0 : total;
  }

  public void update(long id, Long goalId, String title, String description, String status) {
    jdbcTemplate.update(
        "update plan_items set goal_id=?, title=?, description=?, status=? where id=?",
        goalId,
        title,
        description,
        status,
        id);
  }

  private RowMapper<PlanItem> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  private static PlanItem map(ResultSet rs) throws SQLException {
    return new PlanItem(
        rs.getLong("id"),
        (Long) rs.getObject("goal_id"),
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
