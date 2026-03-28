package com.agtext.tool.platform.service;

import com.agtext.tool.platform.domain.ToolDefinition;
import com.agtext.tool.platform.domain.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 工具处理器接口
 * 凡是需要在工具平台中运行的功能组件，都必须实现此接口
 */
public interface ToolHandler {

  /**
   * 获取工具的元数据定义
   * 返回包括名称、描述、入参 Schema、超时时间等在内的静态配置信息
   * 该信息通常用于 ToolRegistry 的注册和 AI Agent 的能力自省
   */
  ToolDefinition definition();

  /**
   * 执行工具的具体业务逻辑
   * * @param ctx 执行上下文，提供 HttpClient、ModelService 等系统基础设施
   * @param input 经过初步格式校验的结构化输入参数（JsonNode）
   * @return 包含执行摘要和结果数据的 ToolResult 对象
   * @throws Exception 允许抛出异常，由 ToolExecutionService 统一捕获并转译为 errorCode
   */
  ToolResult execute(ToolContext ctx, JsonNode input) throws Exception;
}