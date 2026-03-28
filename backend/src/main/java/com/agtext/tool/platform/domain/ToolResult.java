package com.agtext.tool.platform.domain;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 工具执行结果实体（Tool Result）
 * 封装工具运行成功后的返回内容，支持结构化数据与非结构化摘要
 */
public record ToolResult(
        // 执行摘要：对执行结果的简短文字描述（如 "成功更新 5 条任务" 或 "查询到北京气温 15°C"）
        // 供 AI Agent 直接引用或在 UI 界面通知中展示
        String summary,

        // 结构化数据载荷：工具返回的完整原始数据，以 Jackson 的 JsonNode 存储
        // 允许不同工具返回迥异的 JSON 结构，便于后续业务逻辑解析或 Agent 进行二次推理
        JsonNode data
) {}