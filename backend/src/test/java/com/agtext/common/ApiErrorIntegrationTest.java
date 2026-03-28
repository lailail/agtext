package com.agtext.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.agtext.TestModelConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestModelConfig.class)
class ApiErrorIntegrationTest {
  @Autowired private TestRestTemplate rest;

  @Test
  void shouldReturnErrorEnvelopeForNotFound() {
    ResponseEntity<Map> res = rest.getForEntity("/api/tasks/task_999999999", Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(404);
    assertThat(res.getBody()).isNotNull();
    assertThat(res.getBody()).containsKey("error");
    Object error = res.getBody().get("error");
    assertThat(error).isInstanceOf(Map.class);
    assertThat(((Map<?, ?>) error).get("code")).isEqualTo("TASK_NOT_FOUND");
  }

  @Test
  void shouldReturnErrorEnvelopeForBadRequest() {
    ResponseEntity<Map> res = rest.getForEntity("/api/tasks/not_a_task_id", Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(400);
    assertThat(res.getBody()).isNotNull();
    assertThat(((Map<?, ?>) res.getBody().get("error")).get("code")).isEqualTo("BAD_REQUEST");
  }
}
