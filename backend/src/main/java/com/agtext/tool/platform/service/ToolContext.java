package com.agtext.tool.platform.service;

import com.agtext.model.service.ModelService;
import java.net.http.HttpClient;

/**
 * 工具执行上下文（Tool Context）
 * 封装工具运行所需的外部依赖和系统资源，实现工具实现逻辑与底层基础设施的分离
 */
public record ToolContext(
        /**
         * 全局 HTTP 客户端
         * 供 HTTP 类型的工具（如集成第三方 API）发起远程请求，通常由 ToolPlatformConfig 初始化并配置了超时时间
         */
        HttpClient http,

        /**
         * AI 模型服务
         * 供需要“智能处理”能力的工具调用（例如：文本摘要工具、翻译工具、实体提取工具），实现工具内部的 LLM 交互
         */
        ModelService models,

        /**
         * 安全配置属性
         * 包含工具执行的安全边界信息（如敏感词过滤开关、操作频率限制、Actor 权限白名单等）
         */
        ToolSecurityProperties security
) {}