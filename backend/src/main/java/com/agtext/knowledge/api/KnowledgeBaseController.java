package com.agtext.knowledge.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.knowledge.domain.KnowledgeBase;
import com.agtext.knowledge.service.KnowledgeBaseService;
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
@RequestMapping("/api/knowledge/bases")
public class KnowledgeBaseController {
  private static final String PREFIX = "kb_";
  private final KnowledgeBaseService service;

  public KnowledgeBaseController(KnowledgeBaseService service) {
    this.service = service;
  }

  @PostMapping
  public KnowledgeBaseItem create(@RequestBody CreateKnowledgeBaseRequest req) {
    KnowledgeBase kb = service.create(req.name(), req.description());
    return toItem(kb);
  }

  @GetMapping
  public PageResponse<KnowledgeBaseItem> list(
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    List<KnowledgeBaseItem> items =
        service.list(page, pageSize).stream().map(this::toItem).toList();
    return new PageResponse<>(items, page, pageSize, service.countAll());
  }

  @GetMapping("/{id}")
  public KnowledgeBaseItem get(@PathVariable("id") String id) {
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.get(raw));
  }

  private KnowledgeBaseItem toItem(KnowledgeBase kb) {
    return new KnowledgeBaseItem(
        IdCodec.encode(PREFIX, kb.id()),
        kb.name(),
        kb.description(),
        kb.createdAt(),
        kb.updatedAt());
  }

  public record CreateKnowledgeBaseRequest(String name, String description) {}

  public record KnowledgeBaseItem(
      String id, String name, String description, Instant createdAt, Instant updatedAt) {}
}
