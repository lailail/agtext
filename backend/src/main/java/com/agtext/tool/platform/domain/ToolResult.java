package com.agtext.tool.platform.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolResult(String summary, JsonNode data) {}
