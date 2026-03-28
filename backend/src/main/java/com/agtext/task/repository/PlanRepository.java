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

/**
 * 计划（Plan）持久层实现
 * 职责：管理 plan_items 表的数据访问，处理计划与其归属目标（Goal）的逻辑关联。
 */
@Repository
public class PlanRepository {
  private final JdbcTemplate jdbcTemplate;

  public PlanRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 创建新计划
   * @param goalId 关联的目标ID，可为 null
   * @return 数据库生成的自增 ID
   * 备注：使用 ps.setObject 处理可空的 goalId 字段。
   */
  public long create(Long goalId, String title, String description) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
            connection -> {
              var ps =
                      connection.prepareStatement(
                              "insert into plan_items(goal_id, title, description, status) values (?,?,?,?)",
                              new String[] {"id"});
              ps.setObject(1, goalId); // 处理可选的目标关联
              ps.setString(2, title);
              ps.setString(3, description);
              ps.setString(4, "active");
              return ps;
            },
            keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create plan: No ID returned.");
    }
    return key.longValue();
  }

  /**
   * 按 ID 查询计划详情
   */
  public Optional<PlanItem> findById(long id) {
    List<PlanItem> rows = jdbcTemplate.query("select * from plan_items where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  /**
   * 分页查询计划列表
   * 支持按 goalId 筛选，以便在特定目标视角下查看计划。
   */
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

  /**
   * 统计满足条件的计划总数，用于分页计算。
   */
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

  /**
   * 更新计划信息
   * 允许重新关联目标 ID 或修改基本属性。
   */
  public void update(long id, Long goalId, String title, String description, String status) {
    jdbcTemplate.update(
            "update plan_items set goal_id=?, title=?, description=?, status=? where id=?",
            goalId,
            title,
            description,
            status,
            id);
  }

  // --- 内部映射辅助 ---

  private RowMapper<PlanItem> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  /**
   * 将数据库结果集行转换为 PlanItem record
   * 实事求是：goal_id 使用 getObject 以正确处理数据库中的 NULL。
   */
  private static PlanItem map(ResultSet rs) throws SQLException {
    return new PlanItem(
            rs.getLong("id"),
            (Long) rs.getObject("goal_id"), // 获取可空的外键 ID
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