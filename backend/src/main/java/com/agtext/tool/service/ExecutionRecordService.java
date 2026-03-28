package com.agtext.tool.service;

import com.agtext.tool.domain.ExecutionRecord;
import com.agtext.tool.repository.ExecutionRecordRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionRecordService {
  private final ExecutionRecordRepository repo;

  public ExecutionRecordService(ExecutionRecordRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public void record(
      String actor,
      String source,
      String actionType,
      String refType,
      String refId,
      String idempotencyKey,
      String inputSummary,
      String outputSummary,
      String status,
      String errorCode,
      Long durationMs) {
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

  @Transactional(readOnly = true)
  public List<ExecutionRecord> list(String refType, String refId, int page, int pageSize) {
    return repo.list(refType, refId, page, pageSize);
  }

  @Transactional(readOnly = true)
  public long count(String refType, String refId) {
    return repo.count(refType, refId);
  }
}
