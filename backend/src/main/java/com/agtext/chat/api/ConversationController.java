package com.agtext.chat.api;

import com.agtext.chat.domain.Conversation;
import com.agtext.chat.service.ConversationService;
import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
  private static final String PREFIX = "cnv_";
  private final ConversationService service;

  public ConversationController(ConversationService service) {
    this.service = service;
  }

  @PostMapping
  public ConversationItem create(@RequestBody CreateConversationRequest req) {
    Conversation c = service.create(req == null ? null : req.title());
    return toItem(c);
  }

  @GetMapping
  public PageResponse<ConversationItem> list(
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    List<ConversationItem> items = service.list(page, pageSize).stream().map(this::toItem).toList();
    return new PageResponse<>(items, page, pageSize, service.countAll());
  }

  @GetMapping("/{id}")
  public ConversationItem get(@PathVariable("id") String id) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.get(raw));
  }

  private ConversationItem toItem(Conversation c) {
    return new ConversationItem(
        IdCodec.encode(PREFIX, c.id()), c.title(), c.status(), c.createdAt(), c.updatedAt());
  }

  public record CreateConversationRequest(String title) {}

  public record ConversationItem(
      String id, String title, String status, Instant createdAt, Instant updatedAt) {}
}
