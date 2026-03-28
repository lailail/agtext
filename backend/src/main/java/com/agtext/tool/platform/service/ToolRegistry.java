package com.agtext.tool.platform.service;

import com.agtext.tool.platform.domain.ToolDefinition;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ToolRegistry {
  private final Map<String, ToolHandler> toolsByName;

  public ToolRegistry(List<ToolHandler> tools) {
    Map<String, ToolHandler> map = new LinkedHashMap<>();
    for (ToolHandler t : tools) {
      String name = t.definition().name();
      if (name == null || name.isBlank()) {
        continue;
      }
      map.put(name, t);
    }
    this.toolsByName = Map.copyOf(map);
  }

  public Collection<ToolDefinition> listDefinitions() {
    return toolsByName.values().stream().map(t -> t.definition()).toList();
  }

  public Optional<ToolHandler> find(String name) {
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(toolsByName.get(name));
  }
}
