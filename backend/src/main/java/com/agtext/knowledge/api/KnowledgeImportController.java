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

/**
 * 知识导入控制器：负责多种数据源（Markdown, Web, PDF）的 RAG 预处理任务触发。
 * 核心逻辑：
 * 1. 采用异步任务模式，接口调用后返回 Job 记录，实际解析和索引在后台进行。
 * 2. 提供导入任务的状态追踪、重试机制及解析报告查询。
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeImportController {
  private static final String KB_PREFIX = "kb_";   // 知识库前缀
  private static final String DOC_PREFIX = "doc_"; // 文档前缀
  private static final String JOB_PREFIX = "job_"; // 异步任务前缀

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

  /**
   * 导入 Markdown 内容：直接接收文本并启动异步解析流。
   */
  @PostMapping("/bases/{baseId}/imports/markdown")
  public ImportJobItem importMarkdown(
          @PathVariable("baseId") String baseId, @RequestBody ImportMarkdownRequest req) {
    long kbId = IdCodec.decode(KB_PREFIX, baseId);
    KnowledgeImportJob job = imports.importMarkdown(kbId, req.title(), req.content());
    return toItem(job);
  }

  /**
   * 导入网页内容：根据 URL 爬取内容并启动异步流。
   */
  @PostMapping("/bases/{baseId}/imports/web")
  public ImportJobItem importWeb(
          @PathVariable("baseId") String baseId, @RequestBody ImportWebRequest req) {
    long kbId = IdCodec.decode(KB_PREFIX, baseId);
    KnowledgeImportJob job = imports.importWeb(kbId, req.url(), req.title());
    return toItem(job);
  }

  /**
   * 导入 PDF 文件：处理文件流并暂存，随后触发异步解析任务。
   */
  @PostMapping("/bases/{baseId}/imports/pdf")
  public ImportJobItem importPdf(
          @PathVariable("baseId") String baseId, @RequestPart("file") MultipartFile file)
          throws IOException {
    long kbId = IdCodec.decode(KB_PREFIX, baseId);
    // 使用文件原始名称作为默认标题
    KnowledgeImportJob job =
            imports.importPdf(kbId, file.getOriginalFilename(), file.getInputStream());
    return toItem(job);
  }

  /**
   * 获取导入任务列表：支持按知识库、文档或任务状态进行多维过滤。
   */
  @GetMapping("/import-jobs")
  public PageResponse<ImportJobItem> listJobs(
          @RequestParam(name = "knowledge_base_id", required = false) String knowledgeBaseId,
          @RequestParam(name = "document_id", required = false) String documentId,
          @RequestParam(name = "status", required = false) String status,
          @RequestParam(name = "page", defaultValue = "1") int page,
          @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {

    // ID 解码与防御性判断
    Long kbRaw = (knowledgeBaseId == null || knowledgeBaseId.isBlank())
            ? null : IdCodec.decode(KB_PREFIX, knowledgeBaseId);
    Long docRaw = (documentId == null || documentId.isBlank())
            ? null : IdCodec.decode(DOC_PREFIX, documentId);

    List<ImportJobItem> items =
            jobs.list(kbRaw, docRaw, status, page, pageSize).stream().map(this::toItem).toList();
    long total = jobs.count(kbRaw, docRaw, status);

    return new PageResponse<>(items, page, pageSize, total);
  }

  /**
   * 重试任务：针对失败的任务（如网络波动导致的 OCR 或向量化失败）重新启动。
   */
  @PostMapping("/import-jobs/{id}/retry")
  public ImportJobItem retry(@PathVariable("id") String id) {
    long raw = IdCodec.decode(JOB_PREFIX, id);
    return toItem(imports.retry(raw));
  }

  /**
   * 取消任务：停止正在进行的解析或索引任务，清理临时资源。
   */
  @PostMapping("/import-jobs/{id}/cancel")
  public void cancel(@PathVariable("id") String id) {
    long raw = IdCodec.decode(JOB_PREFIX, id);
    imports.cancel(raw);
  }

  /**
   * 文档重索引：在分片策略或模型参数变更后，对现有文档重新执行切片和向量化。
   */
  @PostMapping("/documents/{documentId}/reindex")
  public ImportJobItem reindex(@PathVariable("documentId") String documentId) {
    long raw = IdCodec.decode(DOC_PREFIX, documentId);
    KnowledgeImportJob job = reindex.reindex(raw);
    return toItem(job);
  }

  /**
   * 获取解析报告：查询任务完成后的详细指标，如字符数、页数、切片数量等。
   */
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
            r.parserName(),   // 使用的具体解析器名称
            r.failedAt(),     // 如果解析中途失败，记录失败位置
            r.samplePreview(), // 文本内容样例预览
            r.createdAt());
  }

  /**
   * 将领域模型任务转换为外部 DTO，包含进度百分比及阶段描述。
   */
  private ImportJobItem toItem(KnowledgeImportJob job) {
    return new ImportJobItem(
            IdCodec.encode(JOB_PREFIX, job.id()),
            IdCodec.encode(KB_PREFIX, job.knowledgeBaseId()),
            job.documentId() == null ? null : IdCodec.encode(DOC_PREFIX, job.documentId()),
            job.status(),   // 任务状态：PENDING, RUNNING, COMPLETED, FAILED
            job.stage(),    // 当前阶段：FETCHING, PARSING, CHUNKING, INDEXING
            job.progress(), // 进度 (0-100)
            job.errorMessage(),
            job.createdAt(),
            job.updatedAt());
  }

  // --- 请求与响应定义 ---

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

  /**
   * 解析报告 DTO：为前端提供文档处理后的量化反馈。
   */
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