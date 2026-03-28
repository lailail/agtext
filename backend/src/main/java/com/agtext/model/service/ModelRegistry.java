package com.agtext.model.service;

import com.agtext.model.provider.ChatModelProvider;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天模型注册表：
 * 负责统一管理和检索系统中所有已加载的 ChatModelProvider 实例。
 * 它是实现多模型供应商动态切换（Multi-Provider Strategy）的核心基础。
 */
public class ModelRegistry {
  /**
   * 线程安全的映射容器：
   * Key: 供应商名称（例如 "openai", "deepseek", "mock"）
   * Value: 实现了 ChatModelProvider 接口的具体供应商实例
   * * 使用 ConcurrentHashMap 确保在应用启动注册阶段或热更新配置时，并发读取操作的安全性。
   */
  private final Map<String, ChatModelProvider> providers = new ConcurrentHashMap<>();

  /**
   * 注册供应商实例：
   * 将一个具体的 Provider 实现挂载到注册表中。
   * 如果存在同名供应商，旧的实例将被覆盖。
   * * @param provider 待注册的提供者实例
   */
  public void register(ChatModelProvider provider) {
    if (provider != null && provider.name() != null) {
      providers.put(provider.name(), provider);
    }
  }

  /**
   * 按名称查找供应商：
   * 实事求是地处理“未找到”的情况。使用 Optional 强制调用方（如 ModelService）
   * 必须考虑供应商未定义时的兜底逻辑，有效规避 NullPointerException。
   * * @param name 供应商标识符
   * @return 包含供应商实例的 Optional 容器
   */
  public Optional<ChatModelProvider> find(String name) {
    if (name == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(providers.get(name));
  }
}