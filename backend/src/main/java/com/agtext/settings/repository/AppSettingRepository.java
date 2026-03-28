package com.agtext.settings.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AppSettingRepository {
  private final JdbcTemplate jdbcTemplate;

  public AppSettingRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<String> get(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    List<String> rows =
        jdbcTemplate.query(
            "select v from app_settings where k=?", (rs, rowNum) -> rs.getString("v"), key);
    return rows.stream().findFirst();
  }

  public void upsert(String key, String value) {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("key is required");
    }
    int updated =
        jdbcTemplate.update(
            "update app_settings set v=?, updated_at=current_timestamp where k=?", value, key);
    if (updated > 0) {
      return;
    }
    jdbcTemplate.update("insert into app_settings(k, v) values (?, ?)", key, value);
  }
}
