package com.agtext.memory.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.memory")
public record MemorySettingsProperties(
    boolean extractionEnabled,
    int maxApprovedMemoriesInPrompt,
    int maxCandidatesPerTurn,
    String extractionProvider,
    String extractionModel) {}
