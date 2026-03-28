package com.agtext.tool.platform.domain;

/**
 * 工具定义实体（Tool Definition）
 * 用于向 AI Agent 或前端描述工具的功能、参数约束及安全策略
 */
public record ToolDefinition(
        // 工具的唯一标识符（如 "get_weather", "send_email"），Agent 通过此名称发起调用
        String name,

        // 工具的功能描述：供 LLM（大语言模型）阅读，帮助其判断在何种场景下使用该工具
        String description,

        // 工具的技术实现类型：例如 LOCAL（本地方法调用）, HTTP（远程 API 调用）
        ToolType type,

        // 安全开关：若为 true，则 Agent 调用该工具前必须先创建并获得人工批准的 ConfirmationItem
        boolean requiresConfirmation,

        // 执行超时限制：单位为毫秒（ms），防止工具因逻辑死循环或外部接口响应过慢而挂起系统
        long timeoutMs,

        // 输入参数的结构定义：通常为 JSON Schema 字符串，用于校验 Agent 传参的合规性
        String inputSchema,

        // 输出结果的结构定义：描述工具返回数据的格式，便于 Agent 解析和后续链路处理
        String resultSchema) {}