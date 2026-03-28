package com.agtext.tool.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.tool.domain.ConfirmationItem;
import com.agtext.tool.repository.ConfirmationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 确认单业务服务
 * 封装确认单的创建、审批流转及幂等校验逻辑
 */
@Service
public class ConfirmationService {
  private final ConfirmationRepository repo;

  public ConfirmationService(ConfirmationRepository repo) {
    this.repo = repo;
  }

  /**
   * 创建确认单
   * 采用“先查后增”的策略实现幂等性：如果传入了相同的 idempotencyKey 且记录已存在，则直接返回旧记录
   */
  @Transactional
  public ConfirmationItem create(
          String idempotencyKey,
          String actionType,
          String refType,
          String refId,
          String summary,
          String payload) {
    // 幂等检查：防止 AI Agent 在网络超时重试时生成重复的待办审批
    return repo.findByIdempotencyKey(idempotencyKey)
            .orElseGet(() -> repo.create(idempotencyKey, actionType, refType, refId, summary, payload));
  }

  /**
   * 批准确认单
   * 将状态更改为 "approved"，后续拦截器将放行携带此 ID 的写操作
   */
  @Transactional
  public void approve(long id) {
    get(id); // 确保记录存在，否则抛出 NotFoundException
    repo.updateStatus(id, "approved");
  }

  /**
   * 拒绝确认单
   * 将状态更改为 "denied"，关联的写操作将被永久拦截
   */
  @Transactional
  public void deny(long id) {
    get(id);
    repo.updateStatus(id, "denied");
  }

  /**
   * 获取确认单详情
   * 使用 readOnly = true 优化只读事务性能
   */
  @Transactional(readOnly = true)
  public ConfirmationItem get(long id) {
    return repo.findById(id)
            .orElseThrow(
                    () -> new NotFoundException("CONFIRMATION_NOT_FOUND", "Confirmation not found"));
  }
}