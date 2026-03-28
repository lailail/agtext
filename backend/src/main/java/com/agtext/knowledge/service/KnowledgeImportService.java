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

  @Transactional
  public KnowledgeImportJob importMarkdown(long baseId, String title, String content) {
    bases.get(baseId);
    String path = storage.writeText("knowledge/raw", safeTitle(title), content);
    KnowledgeDocument doc =
        documents.create(baseId, "markdown", "file://" + path, title == null ? "markdown" : title);
    long jobId = jobRepo.create(baseId, doc.id());
    try {
      runParse(jobId, doc.id(), "markdown", content, null, "markdown");
    } catch (RuntimeException e) {
      // runParse already records failure details
    }
    return jobRepo.findById(jobId).orElseThrow(() -> new IllegalStateException("Job not found"));
  }

  @Transactional
  public KnowledgeImportJob importWeb(long baseId, String url, String titleOverride) {
    bases.get(baseId);
    var uri = UrlSafety.requireSafeHttpUrl(url);
    KnowledgeDocument doc =
        documents.create(
            baseId, "web", uri.toString(), titleOverride == null ? uri.toString() : titleOverride);
    long jobId = jobRepo.create(baseId, doc.id());
    try {
      jobRepo.markRunning(jobId, "fetch", 5);
      String fetched = fetchUrlAsText(uri.toString());
      runParse(jobId, doc.id(), "web", fetched, null, "jsoup");
    } catch (RuntimeException e) {
      String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      reportRepo.create(doc.id(), jobId, "failed", null, null, null, "jsoup", "fetch", null);
      documentRepo.updateAfterParse(doc.id(), "failed", "failed", "not_indexed", msg, null, jobId);
      jobRepo.markFailed(jobId, "fetch", msg);
    }
    return jobRepo.findById(jobId).orElseThrow(() -> new IllegalStateException("Job not found"));
  }

  @Transactional
  public KnowledgeImportJob importPdf(long baseId, String fileName, InputStream inputStream) {
    bases.get(baseId);
    byte[] bytes = readAllBytes(inputStream);
    String path = storage.writeBytes("knowledge/raw", safeTitle(fileName), bytes, "pdf");
    KnowledgeDocument doc = documents.create(baseId, "pdf", "file://" + path, fileName);
    long jobId = jobRepo.create(baseId, doc.id());
    try {
      jobRepo.markRunning(jobId, "parse", 10);
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

  @Transactional
  public void cancel(long jobId) {
    KnowledgeImportJob job =
        jobRepo
            .findById(jobId)
            .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Import job not found"));
    if ("succeeded".equalsIgnoreCase(job.status()) || "failed".equalsIgnoreCase(job.status())) {
      return;
    }
    jobRepo.cancel(jobId);
  }

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
    if ("web".equalsIgnoreCase(sourceType)) {
      try {
        jobRepo.markRunning(jobId, "fetch", 5);
        content = fetchUrlAsText(doc.sourceUri());
      } catch (RuntimeException e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        reportRepo.create(doc.id(), jobId, "failed", null, null, null, "jsoup", "fetch", null);
        documentRepo.updateAfterParse(
            doc.id(), "failed", "failed", "not_indexed", msg, null, jobId);
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

      normalized = TextChunker.normalize(extractedText);
      if (normalized.isBlank()) {
        throw new IllegalArgumentException("No text extracted");
      }
      contentHash = sha256(normalized);

      jobRepo.markRunning(jobId, "chunk", 50);
      chunks = TextChunker.chunk(normalized, 1000, 100);

      jobRepo.markRunning(jobId, "store", 70);
      for (int i = 0; i < chunks.size(); i++) {
        String chunk = chunks.get(i);
        chunkRepo.create(documentId, jobId, i, chunk, sha256(chunk));
      }

      jobRepo.markRunning(jobId, "report", 90);
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

      documentRepo.updateAfterParse(
          documentId, "parsed", "parsed", "not_indexed", null, contentHash, jobId);
    } catch (RuntimeException e) {
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
      indexer.buildIndex(jobId, documentId);
    } catch (RuntimeException e) {
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
      return Jsoup.parse(html).text();
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalArgumentException("Fetch failed: " + e.getMessage(), e);
    }
  }

  private static String extractPdfText(String path) {
    try (PDDocument document = Loader.loadPDF(java.nio.file.Path.of(path).toFile())) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(document);
    } catch (IOException e) {
      throw new IllegalArgumentException("PDF parse failed: " + e.getMessage(), e);
    }
  }

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

  private static String sha256(String text) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Hash failed", e);
    }
  }

  private static String safeTitle(String title) {
    if (title == null || title.isBlank()) {
      return "doc";
    }
    return title;
  }
}
