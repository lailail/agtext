package com.agtext.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.agtext.TestModelConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestModelConfig.class)
class MemoryIntegrationTest {
  @Autowired private TestRestTemplate rest;

  @Test
  void shouldCreateApproveAndListMemoryItems() {
    MemoryItemResponse created =
        post(
            "/api/memory/items",
            new CreateMemoryRequest(null, "用户喜欢咖啡", "manual", null, null, "seed"),
            MemoryItemResponse.class);
    assertThat(created.id()).startsWith("mem_");
    assertThat(created.status()).isEqualTo("candidate");

    MemoryItemResponse approved =
        post(
            "/api/memory/items/" + created.id() + "/approve",
            new ReviewRequest("ok"),
            MemoryItemResponse.class);
    assertThat(approved.status()).isEqualTo("approved");

    PageResponse page =
        rest.getForObject(
            "/api/memory/items?status=approved&page=1&page_size=20", PageResponse.class);
    assertThat(page).isNotNull();
    assertThat(page.total).isGreaterThanOrEqualTo(1);
  }

  @Test
  void shouldLinkMemoryToTask() {
    TaskItemResponse task =
        post(
            "/api/tasks",
            new CreateTaskRequest(null, null, false, "买咖啡", null, "todo", 1, null),
            TaskItemResponse.class);
    assertThat(task.id()).startsWith("task_");

    MemoryItemWithLinkResponse mem =
        post(
            "/api/memory/items",
            new CreateMemoryRequest(null, "需要买咖啡", "manual", null, null, "seed"),
            MemoryItemWithLinkResponse.class);
    assertThat(mem.id()).startsWith("mem_");

    MemoryItemWithLinkResponse linked =
        post(
            "/api/memory/items/" + mem.id() + "/link",
            new LinkRequest(null, null, task.id()),
            MemoryItemWithLinkResponse.class);
    assertThat(linked.relatedTaskId()).isEqualTo(task.id());
  }

  private <T> T post(String path, Object req, Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return rest.postForObject(path, new HttpEntity<>(req, headers), responseType);
  }

  private record CreateMemoryRequest(
      String title,
      String content,
      String sourceType,
      Long sourceConversationId,
      Long sourceMessageId,
      String candidateReason) {}

  private record ReviewRequest(String reviewerNote) {}

  private record MemoryItemResponse(String id, String title, String content, String status) {}

  private record MemoryItemWithLinkResponse(String id, String relatedTaskId) {}

  private record CreateTaskRequest(
      String planId,
      String goalId,
      Boolean inbox,
      String title,
      String description,
      String status,
      Integer priority,
      String dueAt) {}

  private record TaskItemResponse(String id) {}

  private record LinkRequest(String goalId, String planId, String taskId) {}

  private static class PageResponse {
    public List<Object> items;
    public int page;
    public int pageSize;
    public long total;
  }
}
