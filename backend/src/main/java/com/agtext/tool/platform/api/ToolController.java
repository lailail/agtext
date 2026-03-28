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

@RestController
@RequestMapping("/api/tools")
public class ToolController {
  private final ToolRegistry registry;
  private final ToolExecutionService executor;

  public ToolController(ToolRegistry registry, ToolExecutionService executor) {
    this.registry = registry;
    this.executor = executor;
  }

  @GetMapping
  public List<ToolDefinition> list() {
    return registry.listDefinitions().stream().toList();
  }

  @PostMapping("/execute")
  public ToolExecutionService.ExecuteResult execute(@RequestBody ExecuteToolRequest req) {
    return executor.execute(
        "user",
        new ToolExecutionService.ExecuteRequest(req.name(), req.input(), req.confirmationId()));
  }

  public record ExecuteToolRequest(String name, JsonNode input, String confirmationId) {}
}
