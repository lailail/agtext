package com.agtext.tool.platform.domain;

public record ToolDefinition(
    String name,
    String description,
    ToolType type,
    boolean requiresConfirmation,
    long timeoutMs,
    String inputSchema,
    String resultSchema) {}
