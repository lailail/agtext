package com.agtext.task;

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
class TaskPlanningIntegrationTest {
  @Autowired private TestRestTemplate rest;

  @Test
  void shouldDecomposeAndCreatePlanWithTasks() {
    CreateFromObjectiveResponse r =
        post(
            "/api/plans/from-objective",
            new FromObjectiveRequest(null, "步骤1\n步骤2\n步骤3", null, null, 3),
            CreateFromObjectiveResponse.class);

    assertThat(r).isNotNull();
    assertThat(r.plan().id()).startsWith("plan_");
    assertThat(r.tasks()).hasSize(3);
    assertThat(r.tasks().get(0).id()).startsWith("task_");
  }

  private <T> T post(String path, Object req, Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return rest.postForObject(path, new HttpEntity<>(req, headers), responseType);
  }

  private record FromObjectiveRequest(
      String goalId, String objective, String provider, String model, Integer maxSteps) {}

  private record CreateFromObjectiveResponse(PlanItem plan, List<TaskItem> tasks) {}

  private record PlanItem(String id, String title) {}

  private record TaskItem(String id, String title) {}
}
