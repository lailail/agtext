package com.agtext.settings.service;

import com.agtext.settings.repository.AppSettingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AppSettingsService {
  private final AppSettingRepository repo;
  private final ObjectMapper objectMapper;

  public AppSettingsService(AppSettingRepository repo, ObjectMapper objectMapper) {
    this.repo = repo;
    this.objectMapper = objectMapper;
  }

  public Optional<String> getString(String key) {
    return repo.get(key);
  }

  public void setString(String key, String value) {
    repo.upsert(key, value);
  }

  public Optional<Boolean> getBoolean(String key) {
    return repo.get(key).map(v -> "true".equalsIgnoreCase(v) || "1".equals(v));
  }

  public void setBoolean(String key, boolean value) {
    repo.upsert(key, value ? "true" : "false");
  }

  public Optional<List<String>> getStringListJson(String key) {
    return repo.get(key).map(this::parseListJson);
  }

  public void setStringListJson(String key, List<String> value) {
    try {
      repo.upsert(key, objectMapper.writeValueAsString(value == null ? List.of() : value));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid value");
    }
  }

  private List<String> parseListJson(String v) {
    if (v == null || v.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(v, new TypeReference<List<String>>() {});
    } catch (Exception e) {
      return List.of();
    }
  }
}
