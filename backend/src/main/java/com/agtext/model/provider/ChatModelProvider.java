package com.agtext.model.provider;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import java.util.List;

/**
 * 聊天模型供应商接口：
 * 定义了对接大语言模型（LLM）的通用标准。
 * 通过该接口，业务层可以无视底层是调用云端 API 还是本地推理引擎。
 */
public interface ChatModelProvider {

  /**
   * 供应商唯一标识：
   * 用于在配置和注册表中区分不同的服务商（例如 "openai", "deepseek", "mock"）。
   * @return 供应商名称字符串
   */
  String name();

  /**
   * 执行对话推理：
   * 将结构化的对话历史发送给模型，并获取生成的响应。
   * * @param model    具体的模型 ID（例如 "gpt-4o", "deepseek-chat"）
   * @param messages 完整的对话上下文列表（包含 System, User, Assistant 角色）
   * @return 统一格式的 ModelResponse 对象
   * @throws RuntimeException 当网络连接失败、API 鉴权错误或触发内容审核时抛出
   */
  ModelResponse chat(String model, List<ChatMessage> messages);
}