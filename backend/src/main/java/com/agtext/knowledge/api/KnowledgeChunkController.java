package com.agtext.knowledge.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.knowledge.domain.KnowledgeChunk;
import com.agtext.knowledge.service.KnowledgeChunkService;
import com.agtext.knowledge.service.KnowledgeDocumentService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge/documents/{documentId}/chunks")
public class KnowledgeChunkController {
  private static final String DOC_PREFIX = "doc_";
  private static final String CHK_PREFIX = "chk_";
  private final KnowledgeDocumentService docs;
  private final KnowledgeChunkService chunks;

  public KnowledgeChunkController(KnowledgeDocumentService docs, KnowledgeChunkService chunks) {
    this.docs = docs;
    this.chunks = chunks;
  }

  @GetMapping
  public PageResponse<KnowledgeChunkItem> list(
      @PathVariable("documentId") String documentId,
      @RequestParam(name = "job_id", required = false) String importJobId,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {
    long docId = IdCodec.decode(DOC_PREFIX, documentId);
    var doc = docs.get(docId);
    Long rawJobId = null;
    if (importJobId != null && !importJobId.isBlank()) {
      rawJobId = IdCodec.decode("job_", importJobId);
    } else if (doc.latestImportJobId() != null) {
      rawJobId = doc.latestImportJobId();
    }

    List<KnowledgeChunkItem> items;
    long total;
    if (rawJobId != null) {
      items =
          chunks.listByDocumentIdAndImportJobId(docId, rawJobId, page, pageSize).stream()
              .map(this::toItem)
              .toList();
      total = chunks.countByDocumentIdAndImportJobId(docId, rawJobId);
    } else {
      items = chunks.listByDocumentId(docId, page, pageSize).stream().map(this::toItem).toList();
      total = chunks.countByDocumentId(docId);
    }
    return new PageResponse<>(items, page, pageSize, total);
  }

  private KnowledgeChunkItem toItem(KnowledgeChunk c) {
    return new KnowledgeChunkItem(
        IdCodec.encode(CHK_PREFIX, c.id()),
        IdCodec.encode(DOC_PREFIX, c.knowledgeDocumentId()),
        c.importJobId() == null ? null : IdCodec.encode("job_", c.importJobId()),
        c.chunkIndex(),
        c.content(),
        c.createdAt(),
        c.updatedAt());
  }

  public record KnowledgeChunkItem(
      String id,
      String knowledgeDocumentId,
      String importJobId,
      int chunkIndex,
      String content,
      Instant createdAt,
      Instant updatedAt) {}
}
