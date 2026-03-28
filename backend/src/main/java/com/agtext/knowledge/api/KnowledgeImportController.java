package com.agtext.knowledge.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.knowledge.domain.KnowledgeImportJob;
import com.agtext.knowledge.domain.ParseReport;
import com.agtext.knowledge.service.KnowledgeImportJobService;
import com.agtext.knowledge.service.KnowledgeImportService;
import com.agtext.knowledge.service.KnowledgeReindexService;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeImportController {
  private static final String KB_PREFIX = "kb_";
  private static final String DOC_PREFIX = "doc_";
  private static final String JOB_PREFIX = "job_";

  private final KnowledgeImportService imports;
  private final KnowledgeImportJobService jobs;
  private final KnowledgeReindexService reindex;

  public KnowledgeImportController(
      KnowledgeImportService imports,
      KnowledgeImportJobService jobs,
      KnowledgeReindexService reindex) {
    this.imports = imports;
    this.jobs = jobs;
    this.reindex = reindex;
  }

  @PostMapping("/bases/{baseId}/imports/markdown")
  public ImportJobItem importMarkdown(
      @PathVariable("baseId") String baseId, @RequestBody ImportMarkdownRequest req) {
    long kbId = IdCodec.decode(KB_PREFIX, baseId);
    KnowledgeImportJob job = imports.importMarkdown(kbId, req.title(), req.content());
    return toItem(job);
  }

  @PostMapping("/bases/{baseId}/imports/web")
  public ImportJobItem importWeb(
      @PathVariable("baseId") String baseId, @RequestBody ImportWebRequest req) {
    long kbId = IdCodec.decode(KB_PREFIX, baseId);
    KnowledgeImportJob job = imports.importWeb(kbId, req.url(), req.title());
    return toItem(job);
  }

  @PostMapping("/bases/{baseId}/imports/pdf")
  public ImportJobItem importPdf(
      @PathVariable("baseId") String baseId, @RequestPart("file") MultipartFile file)
      throws IOException {
    long kbId = IdCodec.decode(KB_PREFIX, baseId);
    KnowledgeImportJob job =
        imports.importPdf(kbId, file.getOriginalFilename(), file.getInputStream());
    return toItem(job);
  }

  @GetMapping("/import-jobs")
  public PageResponse<ImportJobItem> listJobs(
      @RequestParam(name = "knowledge_base_id", required = false) String knowledgeBaseId,
      @RequestParam(name = "document_id", required = false) String documentId,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    Long kbRaw =
        knowledgeBaseId == null || knowledgeBaseId.isBlank()
            ? null
            : IdCodec.decode(KB_PREFIX, knowledgeBaseId);
    Long docRaw =
        documentId == null || documentId.isBlank() ? null : IdCodec.decode(DOC_PREFIX, documentId);
    List<ImportJobItem> items =
        jobs.list(kbRaw, docRaw, status, page, pageSize).stream().map(this::toItem).toList();
    long total = jobs.count(kbRaw, docRaw, status);
    return new PageResponse<>(items, page, pageSize, total);
  }

  @PostMapping("/import-jobs/{id}/retry")
  public ImportJobItem retry(@PathVariable("id") String id) {
    long raw = IdCodec.decode(JOB_PREFIX, id);
    return toItem(imports.retry(raw));
  }

  @PostMapping("/import-jobs/{id}/cancel")
  public void cancel(@PathVariable("id") String id) {
    long raw = IdCodec.decode(JOB_PREFIX, id);
    imports.cancel(raw);
  }

  @PostMapping("/documents/{documentId}/reindex")
  public ImportJobItem reindex(@PathVariable("documentId") String documentId) {
    long raw = IdCodec.decode(DOC_PREFIX, documentId);
    KnowledgeImportJob job = reindex.reindex(raw);
    return toItem(job);
  }

  @GetMapping("/import-jobs/{id}/parse-report")
  public ParseReportItem parseReport(@PathVariable("id") String id) {
    long raw = IdCodec.decode(JOB_PREFIX, id);
    ParseReport r = jobs.getLatestReport(raw);
    return new ParseReportItem(
        IdCodec.encode(JOB_PREFIX, raw),
        IdCodec.encode(DOC_PREFIX, r.knowledgeDocumentId()),
        r.pageCount(),
        r.extractedChars(),
        r.chunkCount(),
        r.parserName(),
        r.failedAt(),
        r.samplePreview(),
        r.createdAt());
  }

  private ImportJobItem toItem(KnowledgeImportJob job) {
    return new ImportJobItem(
        IdCodec.encode(JOB_PREFIX, job.id()),
        IdCodec.encode(KB_PREFIX, job.knowledgeBaseId()),
        job.documentId() == null ? null : IdCodec.encode(DOC_PREFIX, job.documentId()),
        job.status(),
        job.stage(),
        job.progress(),
        job.errorMessage(),
        job.createdAt(),
        job.updatedAt());
  }

  public record ImportMarkdownRequest(String title, String content) {}

  public record ImportWebRequest(String url, String title) {}

  public record ImportJobItem(
      String id,
      String knowledgeBaseId,
      String documentId,
      String status,
      String stage,
      Integer progress,
      String errorMessage,
      Instant createdAt,
      Instant updatedAt) {}

  public record ParseReportItem(
      String jobId,
      String documentId,
      Integer pageCount,
      Long extractedChars,
      Integer chunkCount,
      String parserName,
      String failedAt,
      String samplePreview,
      Instant createdAt) {}
}
