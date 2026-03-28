package com.agtext.tool.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.tool.domain.NotificationItem;
import com.agtext.tool.service.NotificationService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知系统 API 控制层
 * 负责用户通知的查询、已读回执处理及归档操作
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  // 通知资源 ID 的外部编码前缀
  private static final String PREFIX = "ntf_";
  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  /**
   * 分页获取通知列表
   * 支持按通知状态（如 unread, read）和通知类型（type）进行过滤
   */
  @GetMapping
  public PageResponse<NotificationItemDto> list(
          @RequestParam(name = "status", required = false) String status,
          @RequestParam(name = "type", required = false) String type,
          @RequestParam(name = "page", defaultValue = "1") int page,
          @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {

    // 调用业务层获取通知列表并转换为 DTO，确保内部 ID 不直接暴露
    List<NotificationItemDto> items =
            service.list(status, type, page, pageSize).stream()
                    .map(NotificationController::toDto)
                    .toList();

    // 获取筛选后的总条数，用于前端分页
    long total = service.count(status, type);
    return new PageResponse<>(items, page, pageSize, total);
  }

  /**
   * 获取当前用户的未读通知总数
   * 用于前端导航栏或小红点提醒的数值显示
   */
  @GetMapping("/unread-count")
  public UnreadCountDto unreadCount() {
    return new UnreadCountDto(service.unreadCount());
  }

  /**
   * 标记单条通知为已读
   * @param id 外部编码后的通知 ID
   */
  @PostMapping("/{id}/read")
  public void markRead(@PathVariable("id") String id) {
    service.markRead(IdCodec.decode(PREFIX, id));
  }

  /**
   * 归档单条通知
   * 归档后通常不再出现在常规通知列表中
   */
  @PostMapping("/{id}/archive")
  public void archive(@PathVariable("id") String id) {
    service.archive(IdCodec.decode(PREFIX, id));
  }

  /**
   * 将 NotificationItem 领域对象转换为 DTO
   * 包含对关联引用（refType, refId）的透传，便于前端点击通知后跳转
   */
  private static NotificationItemDto toDto(NotificationItem n) {
    return new NotificationItemDto(
            IdCodec.encode(PREFIX, n.id()), // ID 编码安全处理
            n.type(),                       // 通知分类（系统、提醒、任务等）
            n.title(),                      // 标题
            n.content(),                    // 正文
            n.refType(),                    // 关联资源类型（如 Task）
            n.refId(),                      // 关联资源 ID
            n.status(),                     // 状态（unread/read/archived）
            n.createdAt());                 // 发送时间
  }

  /**
   * 未读数响应对象（Java Record）
   */
  public record UnreadCountDto(long count) {}

  /**
   * 通知详情数据传输对象（Java Record）
   */
  public record NotificationItemDto(
          String id,
          String type,
          String title,
          String content,
          String refType,
          String refId,
          String status,
          Instant createdAt) {}
}