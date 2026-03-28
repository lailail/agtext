package com.agtext.chat.service;

import com.agtext.chat.domain.Message;
import com.agtext.chat.repository.MessageRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息管理服务：负责对话记录的持久化存储与检索。
 * 该服务是构建 LLM 上下文的核心数据来源。
 */
@Service
public class MessageService {
  private final MessageRepository repo;

  public MessageService(MessageRepository repo) {
    this.repo = repo;
  }

  /**
   * 创建并持久化消息。
   * * @param conversationId 所属会话 ID
   * @param role 角色（user, assistant, system, tool 等）
   * @param content 消息正文
   * @param provider AI 供应商名称（仅在助理回复时记录，如 OpenAI, Anthropic）
   * @param modelName 使用的模型名称（如 gpt-4, claude-3）
   * @param tokens 消耗的 Token 数量（用于后续审计或计费分析）
   * @return 生成的消息唯一标识 ID
   */
  @Transactional
  public long create(
          long conversationId,
          String role,
          String content,
          String provider,
          String modelName,
          Integer tokens) {
    return repo.create(conversationId, role, content, provider, modelName, tokens);
  }

  /**
   * 获取指定会话的全量消息历史。
   * 通常用于前端加载完整的聊天界面。
   */
  @Transactional(readOnly = true)
  public List<Message> listByConversationId(long conversationId) {
    return repo.listByConversationId(conversationId);
  }

  /**
   * 获取指定会话最近的 N 条消息。
   * 核心逻辑：用于构建大模型的上下文 (Context Window)，防止超出模型最大输入限制。
   * * @param limit 获取消息的数量上限
   */
  @Transactional(readOnly = true)
  public List<Message> listRecent(long conversationId, int limit) {
    return repo.listRecentByConversationId(conversationId, limit);
  }
}