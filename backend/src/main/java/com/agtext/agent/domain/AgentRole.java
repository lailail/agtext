package com.agtext.agent.domain;

/**
 * 定义 Agent 系统中的核心功能角色。
 * 用于在策略模式中区分不同类型的处理逻辑或模型提示词（Prompt）模版。
 */
public enum AgentRole {
  /**
   * 主控角色（调度中心）。
   * 负责接收原始请求、解析意图，并决定是否调用其他子 Agent。
   */
  MAIN,

  /**
   * 检索角色（RAG 增强）。
   * 专注于从向量数据库或外部知识库中提取相关上下文信息。
   */
  RETRIEVAL,

  /**
   * 规划角色。
   * 负责将复杂任务拆解为可执行的子任务序列（Task Decomposition）。
   */
  PLANNING,

  /**
   * 生成/写作角色。
   * 专注于文本润色、格式化输出或根据给定大纲进行内容扩充。
   */
  WRITING
}