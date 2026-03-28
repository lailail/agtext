package com.agtext.tool.api;

import com.agtext.common.ids.IdCodec;
import com.agtext.tool.domain.ConfirmationItem;
import com.agtext.tool.service.ConfirmationService;
import java.time.Instant;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 确认单（待办确认）API 控制层
 * 负责处理外部请求的接入、参数解包及 ID 的外部安全编码
 */
@RestController
@RequestMapping("/api/confirmations")
public class ConfirmationController {
  // 外部 ID 编码前缀，用于增强 ID 的可读性与安全性
  private static final String PREFIX = "cnf_";
  private final ConfirmationService service;

  public ConfirmationController(ConfirmationService service) {
    this.service = service;
  }

  /**
   * 创建确认单
   * 支持通过请求头中的 Idempotency-Key 实现重试幂等性控制
   */
  @PostMapping
  public ConfirmationItemDto create(
          @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
          @RequestBody CreateConfirmationRequest req) {
    ConfirmationItem item =
            service.create(
                    idempotencyKey,
                    req.actionType(),
                    req.refType(),
                    req.refId(),
                    req.summary(),
                    req.payload());
    // 将内部领域模型转换为外部传输 DTO
    return toDto(item);
  }

  /**
   * 批准确认单
   * 接收编码后的字符串 ID（如 cnf_xxx），解码为原始长整型 ID 后调用业务层
   */
  @PostMapping("/{id}/approve")
  public void approve(@PathVariable("id") String id) {
    service.approve(IdCodec.decode(PREFIX, id));
  }

  /**
   * 拒绝确认单
   * 逻辑同上：执行 ID 解码及业务触发
   */
  @PostMapping("/{id}/deny")
  public void deny(@PathVariable("id") String id) {
    service.deny(IdCodec.decode(PREFIX, id));
  }

  /**
   * 实体模型向 DTO 转换的映射逻辑
   * 关键点：使用 IdCodec.encode 对内部 ID 进行混淆或编码
   */
  private static ConfirmationItemDto toDto(ConfirmationItem c) {
    return new ConfirmationItemDto(
            IdCodec.encode(PREFIX, c.id()),
            c.status(),
            c.actionType(),
            c.refType(),
            c.refId(),
            c.summary(),
            c.payload(),
            c.createdAt());
  }

  /**
   * 创建请求的封装对象（Java Record）
   */
  public record CreateConfirmationRequest(
          String actionType, String refType, String refId, String summary, String payload) {}

  /**
   * 响应数据对象（Java Record）
   */
  public record ConfirmationItemDto(
          String id,
          String status,
          String actionType,
          String refType,
          String refId,
          String summary,
          String payload,
          Instant createdAt) {}
}