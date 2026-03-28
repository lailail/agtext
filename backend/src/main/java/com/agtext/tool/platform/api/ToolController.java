package com.agtext.tool.platform.api;

import com.agtext.tool.platform.domain.ToolDefinition;
import com.agtext.tool.platform.service.ToolExecutionService;
import com.agtext.tool.platform.service.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工具平台控制层
 * 提供工具元数据查询及统一的工具执行接口
 */
@RestController
@RequestMapping("/api/tools")
public class ToolController {
  private final ToolRegistry registry;
  private final ToolExecutionService executor;

  public ToolController(ToolRegistry registry, ToolExecutionService executor) {
    this.registry = registry;
    this.executor = executor;
  }

  /**
   * 获取所有可用工具的定义
   * 通常返回工具名称、描述、入参 Schema（如 JSON Schema），供 Agent 识别如何调用
   */
  @GetMapping
  public List<ToolDefinition> list() {
    return registry.listDefinitions().stream().toList();
  }

  /**
   * 统一执行工具接口
   * 采用硬编码的 "user" 作为默认执行主体，将具体的执行逻辑委托给执行服务
   */
  @PostMapping("/execute")
  public ToolExecutionService.ExecuteResult execute(@RequestBody ExecuteToolRequest req) {
    return executor.execute(
            "user", // 当前版本默认 Actor 为 user
            new ToolExecutionService.ExecuteRequest(
                    req.name(),           // 工具的唯一标识名
                    req.input(),          // 工具执行所需的结构化参数（JsonNode）
                    req.confirmationId()  // 关联的确认单 ID（若该工具需要二次确认）
            ));
  }

  /**
   * 执行请求封装
   * @param name 工具名称
   * @param input 工具入参
   * @param confirmationId 对应的确认单标识符
   */
  public record ExecuteToolRequest(String name, JsonNode input, String confirmationId) {}
}