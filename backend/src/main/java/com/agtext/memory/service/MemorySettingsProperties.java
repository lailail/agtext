package com.agtext.memory.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 记忆模块配置属性：
 * 用于定义 AI 记忆提取、检索及上下文注入的各项阈值和开关。
 */
@ConfigurationProperties(prefix = "app.memory")
public record MemorySettingsProperties(
        /**
         * 提取开关：
         * 是否允许系统自动从对话中分析并提取记忆候选（Candidates）。
         * 关闭后，MemoryExtractionService 将直接返回空列表。
         */
        boolean extractionEnabled,

        /**
         * 检索上限：
         * 在构造 AI 会话 Prompt 时，最多允许注入多少条已审核（Approved）的记忆条目。
         * 该参数用于平衡上下文的丰富度与 Token 消耗。
         */
        int maxApprovedMemoriesInPrompt,

        /**
         * 提取限制：
         * 每一轮对话中，LLM 最多允许提取多少条新的记忆候选。
         * 防止模型产生过多的冗余信息。
         */
        int maxCandidatesPerTurn,

        /**
         * 提取模型供应商：
         * 指定用于执行记忆提取任务的 AI 供应商（如 "openai", "deepseek", "anthropic"）。
         */
        String extractionProvider,

        /**
         * 提取模型名称：
         * 指定具体的模型 ID（如 "gpt-4o", "claude-3-5-sonnet"）。
         * 通常建议使用指令遵循能力较强的模型进行 JSON 格式的记忆提取。
         */
        String extractionModel
) {}