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

/**
 * 目标（Goal）持久层实现
 * 职责：负责 GoalItem 领域对象与数据库表 goal_items 之间的物理映射。
 * 技术实现：基于 Spring JdbcTemplate，手动管理 SQL 与参数绑定，规避 ORM 框架的复杂性。
 */
@Repository
public class GoalRepository {
  private final JdbcTemplate jdbcTemplate;

  public GoalRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 创建新目标
   * @return 数据库生成的自增长 ID
   * 逻辑：显式指定返回 "id" 字段以填充 KeyHolder，若主键获取失败则抛出 IllegalStateException。
   */
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
              ps.setString(3, "active"); // 初始状态默认为活跃
              return ps;
            },
            keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("Failed to create goal: No ID returned.");
    }
    return key.longValue();
  }

  /**
   * 根据 ID 查询目标
   * 返回 Optional 以强制调用方处理目标不存在的情况。
   */
  public Optional<GoalItem> findById(long id) {
    List<GoalItem> rows = jdbcTemplate.query("select * from goal_items where id=?", mapper(), id);
    return rows.stream().findFirst();
  }

  /**
   * 分页查询目标列表
   * @param status 过滤状态（可选）
   * @param page 页码（从 1 开始）
   * @param pageSize 每页条数
   * 实现：通过 StringBuilder 动态拼接 SQL，使用 limit/offset 语法进行物理分页。
   */
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

  /**
   * 统计符合条件的目标总数
   * 用于前端分页器计算总页数。
   */
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

  /**
   * 全量更新目标信息
   * 注意：此操作覆盖 title, description 和 status，通常应在事务上下文中调用。
   */
  public void update(long id, String title, String description, String status) {
    jdbcTemplate.update(
            "update goal_items set title=?, description=?, status=? where id=?",
            title,
            description,
            status,
            id);
  }

  // --- 映射逻辑 ---

  private RowMapper<GoalItem> mapper() {
    return (rs, rowNum) -> map(rs);
  }

  /**
   * 结果集手动映射
   * 严格对应 goal_items 表字段与 GoalItem record 构造函数参数。
   */
  private static GoalItem map(ResultSet rs) throws SQLException {
    return new GoalItem(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("status"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")));
  }

  /**
   * 兼容 JDBC Timestamp 与 Java 8+ Instant 的转换
   */
  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}