package com.agtext.tool.service;

import com.agtext.tool.domain.ExecutionRecord;
import com.agtext.tool.repository.ExecutionRecordRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 执行记录业务服务
 * 提供工具执行历史的写入、检索及统计功能
 */
@Service
public class ExecutionRecordService {
  private final ExecutionRecordRepository repo;

  public ExecutionRecordService(ExecutionRecordRepository repo) {
    this.repo = repo;
  }

  /**
   * 记录一次工具调用的执行审计
   * 被 ToolExecutionService 在工具运行结束（无论成功或失败）后同步调用
   */
  @Transactional
  public void record(
          String actor,          // 执行主体（如 "user" 或 AI Agent 的 ID）
          String source,         // 调用源（如 "tool", "api"）
          String actionType,     // 操作细分类型（如 "tool.execute.wikipedia"）
          String refType,        // 关联业务类型（如 "Task"）
          String refId,          // 关联业务 ID
          String idempotencyKey, // 幂等键，用于链路追踪
          String inputSummary,   // 输入参数简述（防止记录过大的原始 JSON）
          String outputSummary,  // 输出结果简述
          String status,         // 执行状态（succeeded/failed）
          String errorCode,      // 错误码（如 TOOL_TIMEOUT）
          Long durationMs) {     // 执行耗时（毫秒）

    repo.create(
            actor,
            source,
            actionType,
            refType,
            refId,
            idempotencyKey,
            inputSummary,
            outputSummary,
            status,
            errorCode,
            durationMs);
  }

  /**
   * 分页获取特定业务对象的执行轨迹
   */
  @Transactional(readOnly = true)
  public List<ExecutionRecord> list(String refType, String refId, int page, int pageSize) {
    return repo.list(refType, refId, page, pageSize);
  }

  /**
   * 统计特定业务对象的执行记录总数
   */
  @Transactional(readOnly = true)
  public long count(String refType, String refId) {
    return repo.count(refType, refId);
  }
}