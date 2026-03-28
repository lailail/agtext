package com.agtext.knowledge.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.knowledge.domain.KnowledgeDocument;
import com.agtext.knowledge.domain.KnowledgeImportJob;
import com.agtext.knowledge.repository.KnowledgeChunkRepository;
import com.agtext.knowledge.repository.KnowledgeDocumentRepository;
import com.agtext.knowledge.repository.KnowledgeImportJobRepository;
import com.agtext.knowledge.repository.ParseReportRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识导入服务：负责将不同来源（Markdown, Web, PDF）的内容转换、切片并编入索引。
 */
@Service
public class KnowledgeImportService {
  private final KnowledgeBaseService bases;
  private final KnowledgeDocumentService documents;
  private final KnowledgeDocumentRepository documentRepo;
  private final KnowledgeChunkRepository chunkRepo;
  private final KnowledgeImportJobRepository jobRepo;
  private final ParseReportRepository reportRepo;
  private final KnowledgeEmbeddingIndexService indexer;
  private final StorageService storage;

  // 使用 Java 11+ HttpClient 处理网络请求
  private final HttpClient httpClient =
          HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public KnowledgeImportService(
          KnowledgeBaseService bases,
          KnowledgeDocumentService documents,
          KnowledgeDocumentRepository documentRepo,
          KnowledgeChunkRepository chunkRepo,
          KnowledgeImportJobRepository jobRepo,
          ParseReportRepository reportRepo,
          KnowledgeEmbeddingIndexService indexer,
          StorageService storage) {
    this.bases = bases;
    this.documents = documents;
    this.documentRepo = documentRepo;
    this.chunkRepo = chunkRepo;
    this.jobRepo = jobRepo;
    this.reportRepo = reportRepo;
    this.indexer = indexer;
    this.storage = storage;
  }

  /**
   * 导入 Markdown 格式文本
   * @param baseId 知识库ID
   * @param title 文档标题
   * @param content Markdown原始内容
   */
  @Transactional
  public KnowledgeImportJob importMarkdown(long baseId, String title, String content) {
    bases.get(baseId); // 校验知识库是否存在
    // 1. 将原始内容存入物理存储
    String path = storage.writeText("knowledge/raw", safeTitle(title), content);
    // 2. 创建文档元数据记录
    KnowledgeDocument doc =
            documents.create(baseId, "markdown", "file://" + path, title == null ? "markdown" : title);
    // 3. 创建异步导入任务记录
    long jobId = jobRepo.create(baseId, doc.id());
    try {
      // 4. 执行解析、切片与索引
      runParse(jobId, doc.id(), "markdown", content, null, "markdown");
    } catch (RuntimeException e) {
      // runParse 内部已处理异常记录，此处捕获防止事务不必要的回滚（取决于具体业务设计）
    }
    return jobRepo.findById(jobId).orElseThrow(() -> new IllegalStateException("Job not found"));
  }

  /**
   * 导入网页内容
   * @param url 目标URL
   */
  @Transactional
  public KnowledgeImportJob importWeb(long baseId, String url, String titleOverride) {
    bases.get(baseId);
    var uri = UrlSafety.requireSafeHttpUrl(url); // 安全性校验（防SSRF等）

    KnowledgeDocument doc =
            documents.create(
                    baseId, "web", uri.toString(), titleOverride == null ? uri.toString() : titleOverride);
    long jobId = jobRepo.create(baseId, doc.id());

    try {
      jobRepo.markRunning(jobId, "fetch", 5); // 标记任务进入抓取阶段，进度5%
      String fetched = fetchUrlAsText(uri.toString());
      runParse(jobId, doc.id(), "web", fetched, null, "jsoup");
    } catch (RuntimeException e) {
      // 捕获抓取或解析阶段的异常并记录到数据库
      String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      reportRepo.create(doc.id(), jobId, "failed", null, null, null, "jsoup", "fetch", null);
      documentRepo.updateAfterParse(doc.id(), "failed", "failed", "not_indexed", msg, null, jobId);
      jobRepo.markFailed(jobId, "fetch", msg);
    }
    return jobRepo.findById(jobId).orElseThrow(() -> new IllegalStateException("Job not found"));
  }

  /**
   * 导入 PDF 文件
   * @param inputStream PDF文件流
   */
  @Transactional
  public KnowledgeImportJob importPdf(long baseId, String fileName, InputStream inputStream) {
    bases.get(baseId);
    byte[] bytes = readAllBytes(inputStream);
    // 1. 保存原始PDF文件
    String path = storage.writeBytes("knowledge/raw", safeTitle(fileName), bytes, "pdf");
    KnowledgeDocument doc = documents.create(baseId, "pdf", "file://" + path, fileName);
    long jobId = jobRepo.create(baseId, doc.id());

    try {
      jobRepo.markRunning(jobId, "parse", 10);
      // 2. 使用 PDFBox 提取文本内容
      String extracted = extractPdfText(path);
      runParse(jobId, doc.id(), "pdf", extracted, countPdfPages(path), "pdfbox");
    } catch (RuntimeException e) {
      String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      reportRepo.create(doc.id(), jobId, "failed", null, null, null, "pdfbox", "parse", null);
      documentRepo.updateAfterParse(doc.id(), "failed", "failed", "not_indexed", msg, null, jobId);
      jobRepo.markFailed(jobId, "parse", msg);
    }
    return jobRepo.findById(jobId).orElseThrow(() -> new IllegalStateException("Job not found"));
  }

  /**
   * 取消正在进行的导入任务
   */
  @Transactional
  public void cancel(long jobId) {
    KnowledgeImportJob job =
            jobRepo
                    .findById(jobId)
                    .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Import job not found"));
    // 已完成或已失败的任务不可取消
    if ("succeeded".equalsIgnoreCase(job.status()) || "failed".equalsIgnoreCase(job.status())) {
      return;
    }
    jobRepo.cancel(jobId);
  }

  /**
   * 重试导入任务
   * 根据文档的 sourceType 重新获取内容并走一遍 runParse 流程
   */
  @Transactional
  public KnowledgeImportJob retry(long jobId) {
    KnowledgeImportJob job =
            jobRepo
                    .findById(jobId)
                    .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Import job not found"));
    if (job.documentId() == null) {
      throw new IllegalArgumentException("Job has no document");
    }

    KnowledgeDocument doc = documents.get(job.documentId());
    String sourceType = doc.sourceType();
    String content;
    Integer pageCount = null;
    String parser = "retry";

    // 根据不同类型恢复待解析内容
    if ("web".equalsIgnoreCase(sourceType)) {
      try {
        jobRepo.markRunning(jobId, "fetch", 5);
        content = fetchUrlAsText(doc.sourceUri());
      } catch (RuntimeException e) {
        // 重试失败同样需要记录错误状态
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        reportRepo.create(doc.id(), jobId, "failed", null, null, null, "jsoup", "fetch", null);
        documentRepo.updateAfterParse(doc.id(), "failed", "failed", "not_indexed", msg, null, jobId);
        jobRepo.markFailed(jobId, "fetch", msg);
        throw e;
      }
      parser = "jsoup";
    } else if ("markdown".equalsIgnoreCase(sourceType) || "pdf".equalsIgnoreCase(sourceType)) {
      if (doc.sourceUri() == null || !doc.sourceUri().startsWith("file://")) {
        throw new IllegalArgumentException("Unsupported sourceUri for retry");
      }
      String path = doc.sourceUri().substring("file://".length());
      if ("pdf".equalsIgnoreCase(sourceType)) {
        pageCount = countPdfPages(path);
        content = extractPdfText(path);
        parser = "pdfbox";
      } else {
        content = storage.readText(path);
        parser = "markdown";
      }
    } else {
      throw new IllegalArgumentException("Unsupported sourceType for retry: " + sourceType);
    }

    jobRepo.markRunning(jobId, "parse", 10);
    runParse(jobId, doc.id(), sourceType, content, pageCount, parser);
    return jobRepo.findById(jobId).orElseThrow(() -> new IllegalStateException("Job not found"));
  }

  /**
   * 核心流水线：标准化 -> 哈希校验 -> 切片 -> 存储切片 -> 记录报告 -> 向量索引
   */
  private void runParse(
          long jobId,
          long documentId,
          String sourceType,
          String extractedText,
          Integer pageCount,
          String parserName) {
    String contentHash = null;
    String normalized = null;
    List<String> chunks = null;
    try {
      jobRepo.markRunning(jobId, "parse", 20);

      // 1. 文本规范化（去除多余空格、特殊字符处理等）
      normalized = TextChunker.normalize(extractedText);
      if (normalized.isBlank()) {
        throw new IllegalArgumentException("No text extracted");
      }
      // 2. 计算文本指纹，用于去重或版本校验
      contentHash = sha256(normalized);

      jobRepo.markRunning(jobId, "chunk", 50);
      // 3. 核心切片逻辑：按 1000 字符大小、100 重叠度进行物理切片
      chunks = TextChunker.chunk(normalized, 1000, 100);

      jobRepo.markRunning(jobId, "store", 70);
      // 4. 持久化所有切片
      for (int i = 0; i < chunks.size(); i++) {
        String chunk = chunks.get(i);
        chunkRepo.create(documentId, jobId, i, chunk, sha256(chunk));
      }

      jobRepo.markRunning(jobId, "report", 90);
      // 5. 生成解析报告及预览文本
      String preview = normalized.substring(0, Math.min(800, normalized.length()));
      reportRepo.create(
              documentId,
              jobId,
              "ok",
              (long) normalized.length(),
              pageCount,
              chunks.size(),
              parserName,
              null,
              preview);

      // 更新文档状态为已解析
      documentRepo.updateAfterParse(
              documentId, "parsed", "parsed", "not_indexed", null, contentHash, jobId);
    } catch (RuntimeException e) {
      // 记录解析阶段失败详情
      String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      reportRepo.create(
              documentId,
              jobId,
              "failed",
              extractedText == null ? null : (long) extractedText.length(),
              pageCount,
              null,
              parserName,
              "parse",
              null);
      documentRepo.updateAfterParse(
              documentId, "failed", "failed", "not_indexed", msg, null, jobId);
      jobRepo.markFailed(jobId, "parse", msg);
      throw e;
    }

    try {
      // 6. 调用向量索引服务（如编入 Pinecone, Milvus 等）
      indexer.buildIndex(jobId, documentId);
    } catch (RuntimeException e) {
      // 索引阶段失败：文档已解析但未成功入库索引
      String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      reportRepo.create(
              documentId,
              jobId,
              "failed",
              normalized == null ? null : (long) normalized.length(),
              pageCount,
              chunks == null ? null : chunks.size(),
              parserName,
              "index",
              null);
      documentRepo.updateAfterParse(
              documentId, "parsed", "parsed", "failed", msg, contentHash, jobId);
      jobRepo.markFailed(jobId, "index", msg);
      throw e;
    }
  }

  /**
   * 抓取网页并提取纯文本
   */
  private String fetchUrlAsText(String url) {
    try {
      HttpRequest request =
              HttpRequest.newBuilder()
                      .uri(UrlSafety.requireSafeHttpUrl(url))
                      .timeout(Duration.ofSeconds(20))
                      .header("User-Agent", "agtext/1.0")
                      .GET()
                      .build();
      HttpResponse<String> resp =
              httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (resp.statusCode() >= 400) {
        throw new IllegalArgumentException("Fetch failed: HTTP " + resp.statusCode());
      }
      String html = resp.body() == null ? "" : resp.body();
      // 使用 Jsoup 移除 HTML 标签获取纯文本
      return Jsoup.parse(html).text();
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalArgumentException("Fetch failed: " + e.getMessage(), e);
    }
  }

  /**
   * PDF 文本提取
   */
  private static String extractPdfText(String path) {
    try (PDDocument document = Loader.loadPDF(java.nio.file.Path.of(path).toFile())) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(document);
    } catch (IOException e) {
      throw new IllegalArgumentException("PDF parse failed: " + e.getMessage(), e);
    }
  }

  /**
   * 获取 PDF 总页数
   */
  private static int countPdfPages(String path) {
    try (PDDocument document = Loader.loadPDF(java.nio.file.Path.of(path).toFile())) {
      return document.getNumberOfPages();
    } catch (IOException e) {
      return 0;
    }
  }

  private static byte[] readAllBytes(InputStream in) {
    try {
      return in.readAllBytes();
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to read stream", e);
    }
  }

  /**
   * 计算 SHA-256 哈希值
   */
  private static String sha256(String text) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Hash failed", e);
    }
  }

  /**
   * 标题安全处理
   */
  private static String safeTitle(String title) {
    if (title == null || title.isBlank()) {
      return "doc";
    }
    return title;
  }
}