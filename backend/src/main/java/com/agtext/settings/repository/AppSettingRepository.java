package com.agtext.settings.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 系统设置持久层：
 * 负责应用动态参数（如模型切换、工具开关、白名单等）在数据库中的存储与读取。
 * 采用极简的 Key-Value 结构设计。
 */
@Repository
public class AppSettingRepository {
  private final JdbcTemplate jdbcTemplate;

  public AppSettingRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 根据键（Key）查询配置值（Value）
   * @param key 配置项名称
   * @return 包含配置值的 Optional，若不存在则返回 Optional.empty()
   */
  public Optional<String> get(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    // 查询 v 字段，使用 Lambda 表达式作为 RowMapper 提取单行结果
    List<String> rows =
            jdbcTemplate.query(
                    "select v from app_settings where k=?", (rs, rowNum) -> rs.getString("v"), key);
    return rows.stream().findFirst();
  }

  /**
   * 更新或插入配置项（Upsert 逻辑）
   * 采用“先更新、若失败则插入”的策略，确保配置项的唯一性与存在性。
   * * @param key   配置键，不能为空
   * @param value 配置值字符串（通常为纯文本、布尔值或 JSON 数组）
   */
  public void upsert(String key, String value) {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("key is required");
    }

    // 1. 尝试更新已有记录，同步更新时间戳
    int updated =
            jdbcTemplate.update(
                    "update app_settings set v=?, updated_at=current_timestamp where k=?", value, key);

    // 2. 如果更新行数为 0，说明该 Key 不存在，执行插入
    if (updated > 0) {
      return;
    }

    // 插入新记录，数据库通常通过触发器或默认值处理 created_at/updated_at
    jdbcTemplate.update("insert into app_settings(k, v) values (?, ?)", key, value);
  }
}