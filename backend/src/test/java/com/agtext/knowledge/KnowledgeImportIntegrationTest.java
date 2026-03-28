package com.agtext.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import com.agtext.TestEmbeddingConfig;
import com.agtext.TestModelConfig;
import com.agtext.common.api.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestModelConfig.class, TestEmbeddingConfig.class})
class KnowledgeImportIntegrationTest {
  @Autowired private TestRestTemplate rest;

  @Test
  void shouldImportMarkdownAndCreateChunksAndReport() {
    KnowledgeBaseItem kb =
        post("/api/knowledge/bases", new CreateKbRequest("kb1", "desc"), KnowledgeBaseItem.class);
    ImportJobItem job =
        post(
            "/api/knowledge/bases/" + kb.id() + "/imports/markdown",
            new ImportMarkdownRequest("doc1", "hello\n\nworld"),
            ImportJobItem.class);
    assertThat(job.status()).isEqualTo("succeeded");
    assertThat(job.documentId()).startsWith("doc_");

    PageResponse<KnowledgeDocumentItem> docs =
        getPage(
            "/api/knowledge/bases/" + kb.id() + "/documents?page=1&page_size=20",
            new ParameterizedTypeReference<PageResponse<KnowledgeDocumentItem>>() {});
    assertThat(docs.items()).hasSize(1);
    assertThat(docs.items().get(0).status()).isEqualTo("ready");

    PageResponse<KnowledgeChunkItem> chunks =
        getPage(
            "/api/knowledge/documents/" + job.documentId() + "/chunks?page=1&page_size=50",
            new ParameterizedTypeReference<PageResponse<KnowledgeChunkItem>>() {});
    assertThat(chunks.total()).isGreaterThan(0);

    ParseReportItem report =
        get("/api/knowledge/import-jobs/" + job.id() + "/parse-report", ParseReportItem.class);
    assertThat(report.chunkCount()).isNotNull();
    assertThat(report.chunkCount()).isGreaterThan(0);
    assertThat(report.extractedChars()).isGreaterThan(0);
    assertThat(report.parserName()).isEqualTo("markdown");
  }

  @Test
  void shouldRecordFailureReasonWhenNoTextExtracted() {
    KnowledgeBaseItem kb =
        post("/api/knowledge/bases", new CreateKbRequest("kb2", null), KnowledgeBaseItem.class);
    ImportJobItem job =
        post(
            "/api/knowledge/bases/" + kb.id() + "/imports/markdown",
            new ImportMarkdownRequest("empty", "   \n "),
            ImportJobItem.class);
    assertThat(job.status()).isEqualTo("failed");
    assertThat(job.errorMessage()).contains("No text extracted");

    ParseReportItem report =
        get("/api/knowledge/import-jobs/" + job.id() + "/parse-report", ParseReportItem.class);
    assertThat(report.failedAt()).isEqualTo("parse");
  }

  private <T> T post(String path, Object body, Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return rest.postForObject(path, new HttpEntity<>(body, headers), responseType);
  }

  private <T> T get(String path, Class<T> responseType) {
    return rest.getForObject(path, responseType);
  }

  private <T> PageResponse<T> getPage(
      String path, ParameterizedTypeReference<PageResponse<T>> type) {
    return rest.exchange(path, HttpMethod.GET, HttpEntity.EMPTY, type).getBody();
  }

  private record CreateKbRequest(String name, String description) {}

  private record KnowledgeBaseItem(String id, String name, String description) {}

  private record ImportMarkdownRequest(String title, String content) {}

  private record ImportJobItem(
      String id,
      String knowledgeBaseId,
      String documentId,
      String status,
      String stage,
      Integer progress,
      String errorMessage) {}

  private record KnowledgeDocumentItem(String id, String status) {}

  private record KnowledgeChunkItem(String id, int chunkIndex, String content) {}

  private record ParseReportItem(
      String jobId,
      String documentId,
      Integer pageCount,
      Long extractedChars,
      Integer chunkCount,
      String parserName,
      String failedAt,
      String samplePreview,
      String createdAt) {}
}
