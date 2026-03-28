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

/**
 * 执行记录 API 控制层
 * 负责系统操作执行轨迹（Executions）的查询与展示
 */
@RestController
@RequestMapping("/api/executions")
public class ExecutionController {
  // 外部公开 ID 的前缀，用于区分资源类型并隐藏原始自增 ID
  private static final String PREFIX = "exe_";
  private final ExecutionRecordService service;

  public ExecutionController(ExecutionRecordService service) {
    this.service = service;
  }

  /**
   * 分页查询执行记录
   * 支持按引用类型（ref_type）和引用 ID（ref_id）进行过滤
   * 默认分页参数：第 1 页，每页 20 条
   */
  @GetMapping
  public PageResponse<ExecutionRecordDto> list(
          @RequestParam(name = "ref_type", required = false) String refType,
          @RequestParam(name = "ref_id", required = false) String refId,
          @RequestParam(name = "page", defaultValue = "1") int page,
          @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {

    // 调用业务层获取领域模型列表并流式转换为 DTO
    List<ExecutionRecordDto> items =
            service.list(refType, refId, page, pageSize).stream()
                    .map(ExecutionController::toDto)
                    .toList();

    // 获取符合条件的记录总数，用于前端分页器计算
    long total = service.count(refType, refId);

    return new PageResponse<>(items, page, pageSize, total);
  }

  /**
   * 实体模型向 DTO 转换的映射逻辑
   * 包含：ID 编码、耗时统计及执行状态等元数据包装
   */
  private static ExecutionRecordDto toDto(ExecutionRecord e) {
    return new ExecutionRecordDto(
            IdCodec.encode(PREFIX, e.id()), // 将内部 Long ID 转换为外部前缀字符串
            e.actor(),                      // 执行者标识
            e.source(),                     // 调用源
            e.actionType(),                 // 动作类型
            e.refType(),                    // 关联资源类型
            e.refId(),                      // 关联资源 ID
            e.idempotencyKey(),             // 幂等键
            e.inputSummary(),               // 输入摘要
            e.outputSummary(),              // 输出摘要
            e.status(),                     // 执行状态（success/failed 等）
            e.errorCode(),                  // 错误码（如有）
            e.durationMs(),                 // 运行耗时（毫秒）
            e.createdAt());                 // 创建时间
  }

  /**
   * 执行记录数据传输对象（Java Record）
   * 严格对应前端展示所需的字段结构
   */
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