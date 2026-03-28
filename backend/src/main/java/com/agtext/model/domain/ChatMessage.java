package com.agtext.model.domain;

/**
 * 聊天消息领域模型：
 * 采用 Java Record 实现，确保单条消息在内存中的不可变性。
 * 该结构是构建对话上下文（Context Window）的最小原子单位。
 */
public record ChatMessage(
        /**
         * 角色：标识消息的发送者。
         * 常用取值：'system' (系统提示词), 'user' (用户输入), 'assistant' (模型回复)。
         */
        String role,

        /**
         * 内容：具体的文本消息。
         */
        String content
) {

  /**
   * 创建系统级消息：
   * 用于设定 AI 的行为准则、人格特质或提取任务的指令约束。
   */
  public static ChatMessage system(String content) {
    return new ChatMessage("system", content);
  }

  /**
   * 创建用户消息：
   * 承载人类用户的真实输入或问题。
   */
  public static ChatMessage user(String content) {
    return new ChatMessage("user", content);
  }

  /**
   * 创建助手消息：
   * 记录模型之前的回答，用于构建多轮对话的历史背景。
   */
  public static ChatMessage assistant(String content) {
    return new ChatMessage("assistant", content);
  }
}