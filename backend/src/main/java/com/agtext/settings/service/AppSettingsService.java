package com.agtext.settings.service;

import com.agtext.settings.repository.AppSettingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 应用设置服务：
 * 提供强类型的配置访问接口。负责将数据库存储的原始字符串转换为业务所需的
 * 字符串、布尔值或复杂的 JSON 对象列表。
 */
@Service
public class AppSettingsService {
  private final AppSettingRepository repo;
  private final ObjectMapper objectMapper; // 用于处理 JSON 序列化与反序列化

  public AppSettingsService(AppSettingRepository repo, ObjectMapper objectMapper) {
    this.repo = repo;
    this.objectMapper = objectMapper;
  }

  /**
   * 获取字符串配置：
   * 直接从数据库读取原始值。
   */
  public Optional<String> getString(String key) {
    return repo.get(key);
  }

  /**
   * 保存字符串配置
   */
  public void setString(String key, String value) {
    repo.upsert(key, value);
  }

  /**
   * 获取布尔配置：
   * 兼容多种真值表示法（"true" 或 "1"）。
   * 用于控制 AI 工具开关、功能实验室特性等。
   */
  public Optional<Boolean> getBoolean(String key) {
    return repo.get(key).map(v -> "true".equalsIgnoreCase(v) || "1".equals(v));
  }

  /**
   * 保存布尔配置：
   * 统一转换为小写 "true" / "false" 字符串进行存储。
   */
  public void setBoolean(String key, boolean value) {
    repo.upsert(key, value ? "true" : "false");
  }

  /**
   * 获取 JSON 格式的字符串列表：
   * 用于存储域名白名单、禁选词表等结构化配置。
   */
  public Optional<List<String>> getStringListJson(String key) {
    return repo.get(key).map(this::parseListJson);
  }

  /**
   * 以 JSON 格式保存字符串列表
   * @throws IllegalArgumentException 当序列化失败时抛出
   */
  public void setStringListJson(String key, List<String> value) {
    try {
      // 若传入为 null 则存储为空 JSON 数组 "[]"
      repo.upsert(key, objectMapper.writeValueAsString(value == null ? List.of() : value));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid value");
    }
  }

  /**
   * 内部解析方法：
   * 鲁棒性处理：解析失败时返回空列表而非抛出异常，防止损坏的配置导致整个系统崩溃。
   */
  private List<String> parseListJson(String v) {
    if (v == null || v.isBlank()) {
      return List.of();
    }
    try {
      // 使用 TypeReference 处理泛型擦除，确保返回 List<String>
      return objectMapper.readValue(v, new TypeReference<List<String>>() {});
    } catch (Exception e) {
      return List.of();
    }
  }
}