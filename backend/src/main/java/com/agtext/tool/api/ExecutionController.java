package com.agtext.tool.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.tool.domain.ExecutionRecord;
import com.agtext.tool.service.ExecutionRecordService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {
  private static final String PREFIX = "exe_";
  private final ExecutionRecordService service;

  public ExecutionController(ExecutionRecordService service) {
    this.service = service;
  }

  @GetMapping
  public PageResponse<ExecutionRecordDto> list(
      @RequestParam(name = "ref_type", required = false) String refType,
      @RequestParam(name = "ref_id", required = false) String refId,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    List<ExecutionRecordDto> items =
        service.list(refType, refId, page, pageSize).stream()
            .map(ExecutionController::toDto)
            .toList();
    long total = service.count(refType, refId);
    return new PageResponse<>(items, page, pageSize, total);
  }

  private static ExecutionRecordDto toDto(ExecutionRecord e) {
    return new ExecutionRecordDto(
        IdCodec.encode(PREFIX, e.id()),
        e.actor(),
        e.source(),
        e.actionType(),
        e.refType(),
        e.refId(),
        e.idempotencyKey(),
        e.inputSummary(),
        e.outputSummary(),
        e.status(),
        e.errorCode(),
        e.durationMs(),
        e.createdAt());
  }

  public record ExecutionRecordDto(
      String id,
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
      Long durationMs,
      Instant createdAt) {}
}
