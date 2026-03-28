package com.agtext.tool.platform.service;

import com.agtext.tool.platform.domain.ToolDefinition;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 工具注册表服务
 * 负责统一管理系统中所有可用的工具实例，提供工具发现与元数据检索功能
 */
@Service
public class ToolRegistry {
  /**
   * 工具名与处理器实例的映射表
   * 使用 LinkedHashMap 保持工具注册的原始顺序（通常是 Bean 的加载顺序）
   */
  private final Map<String, ToolHandler> toolsByName;

  /**
   * 构造函数：利用 Spring 的自动装配功能
   * 这里的 List<ToolHandler> 会自动注入 IOC 容器中所有实现了该接口的 Bean
   */
  public ToolRegistry(List<ToolHandler> tools) {
    Map<String, ToolHandler> map = new LinkedHashMap<>();
    for (ToolHandler t : tools) {
      String name = t.definition().name();
      // 严谨性检查：忽略未定义名称或名称为空的无效工具
      if (name == null || name.isBlank()) {
        continue;
      }
      // 如果存在重名工具，后加载的 Bean 会覆盖前者（实事求是地说，此处可考虑增加重名警告逻辑）
      map.put(name, t);
    }
    // 使用 Map.copyOf 创建一个不可变映射，确保运行时的线程安全与数据一致性
    this.toolsByName = Map.copyOf(map);
  }

  /**
   * 获取所有已注册工具的元数据定义列表
   * 常用于向 AI Agent 展示其具备的能力范围（Capability Discovery）
   */
  public Collection<ToolDefinition> listDefinitions() {
    return toolsByName.values().stream().map(t -> t.definition()).toList();
  }

  /**
   * 根据工具名称查找对应的处理器实例
   * 返回 Optional 以便调用方（如 ToolExecutionService）优雅地处理工具不存在的情况
   */
  public Optional<ToolHandler> find(String name) {
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(toolsByName.get(name));
  }
}