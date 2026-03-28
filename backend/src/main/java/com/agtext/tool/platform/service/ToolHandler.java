package com.agtext.tool.platform.service;

import com.agtext.tool.platform.domain.ToolDefinition;
import com.agtext.tool.platform.domain.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;

public interface ToolHandler {
  ToolDefinition definition();

  ToolResult execute(ToolContext ctx, JsonNode input) throws Exception;
}
