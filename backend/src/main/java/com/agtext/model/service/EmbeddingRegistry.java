package com.agtext.model.service;

import com.agtext.model.provider.EmbeddingProvider;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量提供者注册表：
 * 负责管理系统中所有可用的 EmbeddingProvider 实例（如 OpenAI, Ollama, Mock 等）。
 * 它充当了“服务发现”的角色，使得业务层可以根据供应商名称动态获取对应的实现。
 */
public class EmbeddingRegistry {
  /**
   * 线程安全的映射表：
   * Key: 供应商名称（如 "openai"）
   * Value: 对应的 EmbeddingProvider 实现实例
   */
  private final Map<String, EmbeddingProvider> providers = new ConcurrentHashMap<>();

  /**
   * 注册一个新的向量提供者：
   * 通常在应用启动时的配置类（如 ModelConfig）中调用。
   * @param provider 具体的提供者实例
   */
  public void register(EmbeddingProvider provider) {
    if (provider != null && provider.name() != null) {
      providers.put(provider.name(), provider);
    }
  }

  /**
   * 根据名称查找对应的提供者：
   * 使用 Optional 包装返回结果，实事求是地处理“找不到对应提供者”的情况，
   * 强制调用方处理缺失逻辑，避免 NullPointerException。
   * * @param name 供应商名称
   * @return 匹配的提供者（可能为空）
   */
  public Optional<EmbeddingProvider> find(String name) {
    if (name == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(providers.get(name));
  }
}