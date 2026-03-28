package com.agtext.knowledge.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.knowledge.domain.KnowledgeDocument;
import com.agtext.knowledge.service.KnowledgeBaseService;
import com.agtext.knowledge.service.KnowledgeDocumentService;
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
@RequestMapping("/api/knowledge")
public class KnowledgeDocumentController {
  private static final String KB_PREFIX = "kb_";
  private static final String DOC_PREFIX = "doc_";

  private final KnowledgeBaseService bases;
  private final KnowledgeDocumentService docs;

  public KnowledgeDocumentController(KnowledgeBaseService bases, KnowledgeDocumentService docs) {
    this.bases = bases;
    this.docs = docs;
  }

  @PostMapping("/bases/{baseId}/documents")
  public KnowledgeDocumentItem create(
      @PathVariable("baseId") String baseId, @RequestBody CreateKnowledgeDocumentRequest req) {
    long kbId = IdCodec.decode(KB_PREFIX, baseId);
    bases.get(kbId);
    KnowledgeDocument doc = docs.create(kbId, req.sourceType(), req.sourceUri(), req.title());
    return toItem(doc);
  }

  @GetMapping("/bases/{baseId}/documents")
  public PageResponse<KnowledgeDocumentItem> listByBase(
      @PathVariable("baseId") String baseId,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    long kbId = IdCodec.decode(KB_PREFIX, baseId);
    bases.get(kbId);
    List<KnowledgeDocumentItem> items =
        docs.listByBaseId(kbId, page, pageSize).stream().map(this::toItem).toList();
    return new PageResponse<>(items, page, pageSize, docs.countByBaseId(kbId));
  }

  @GetMapping("/documents/{id}")
  public KnowledgeDocumentItem get(@PathVariable("id") String id) {
    long raw = IdCodec.decode(DOC_PREFIX, id);
    return toItem(docs.get(raw));
  }

  private KnowledgeDocumentItem toItem(KnowledgeDocument doc) {
    return new KnowledgeDocumentItem(
        IdCodec.encode(DOC_PREFIX, doc.id()),
        IdCodec.encode(KB_PREFIX, doc.knowledgeBaseId()),
        doc.sourceType(),
        doc.sourceUri(),
        doc.title(),
        doc.status(),
        doc.parseStatus(),
        doc.indexStatus(),
        doc.errorMessage(),
        doc.latestImportJobId() == null ? null : IdCodec.encode("job_", doc.latestImportJobId()),
        doc.createdAt(),
        doc.updatedAt());
  }

  public record CreateKnowledgeDocumentRequest(String sourceType, String sourceUri, String title) {}

  public record KnowledgeDocumentItem(
      String id,
      String knowledgeBaseId,
      String sourceType,
      String sourceUri,
      String title,
      String status,
      String parseStatus,
      String indexStatus,
      String errorMessage,
      String latestImportJobId,
      Instant createdAt,
      Instant updatedAt) {}
}
