package com.agtext.model.domain;

/**
 * 模型响应结果模型：
 * 采用 Java Record 实现，统一了不同 AI 供应商（OpenAI, Anthropic, DeepSeek 等）的回包格式。
 * 确保业务层无需关心底层 API 的原始 JSON 结构。
 */
public record ModelResponse(
        /**
         * 供应商名称：
         * 标识此次响应来自哪个服务商（如 "openai", "azure", "local-llm"）。
         * 用于审计、计费统计或根据供应商特性进行后处理。
         */
        String provider,

        /**
         * 模型名称：
         * 实际执行推理的模型全名（如 "gpt-4o-2024-05-13"）。
         * 严肃性提示：由于 API 别名（Alias）的存在，此处返回的具体版本号对排查“幻觉”或性能波动至关重要。
         */
        String model,

        /**
         * 响应内容：
         * 模型生成的纯文本回复。
         * 若为结构化提取任务（如 MemoryExtractionService），此字段将包含待解析的 JSON 字符串。
         */
        String content
) {}