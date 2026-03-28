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

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  private static final String PREFIX = "ntf_";
  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @GetMapping
  public PageResponse<NotificationItemDto> list(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "type", required = false) String type,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    List<NotificationItemDto> items =
        service.list(status, type, page, pageSize).stream()
            .map(NotificationController::toDto)
            .toList();
    long total = service.count(status, type);
    return new PageResponse<>(items, page, pageSize, total);
  }

  @GetMapping("/unread-count")
  public UnreadCountDto unreadCount() {
    return new UnreadCountDto(service.unreadCount());
  }

  @PostMapping("/{id}/read")
  public void markRead(@PathVariable("id") String id) {
    service.markRead(IdCodec.decode(PREFIX, id));
  }

  @PostMapping("/{id}/archive")
  public void archive(@PathVariable("id") String id) {
    service.archive(IdCodec.decode(PREFIX, id));
  }

  private static NotificationItemDto toDto(NotificationItem n) {
    return new NotificationItemDto(
        IdCodec.encode(PREFIX, n.id()),
        n.type(),
        n.title(),
        n.content(),
        n.refType(),
        n.refId(),
        n.status(),
        n.createdAt());
  }

  public record UnreadCountDto(long count) {}

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
