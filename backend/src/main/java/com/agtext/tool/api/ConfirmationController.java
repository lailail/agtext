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

@RestController
@RequestMapping("/api/confirmations")
public class ConfirmationController {
  private static final String PREFIX = "cnf_";
  private final ConfirmationService service;

  public ConfirmationController(ConfirmationService service) {
    this.service = service;
  }

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
    return toDto(item);
  }

  @PostMapping("/{id}/approve")
  public void approve(@PathVariable("id") String id) {
    service.approve(IdCodec.decode(PREFIX, id));
  }

  @PostMapping("/{id}/deny")
  public void deny(@PathVariable("id") String id) {
    service.deny(IdCodec.decode(PREFIX, id));
  }

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

  public record CreateConfirmationRequest(
      String actionType, String refType, String refId, String summary, String payload) {}

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
