package com.agtext.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.agtext.TestModelConfig;
import com.agtext.settings.service.AppSettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
class ToolPlatformIntegrationTest {
  @Autowired private TestRestTemplate rest;
  @Autowired private AppSettingsService settings;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @AfterEach
  void resetOverrides() {
    settings.setStringListJson("tool.domainAllowlist", List.of());
    settings.setBoolean("tool.enabled.web.fetch", true);
  }

  @Test
  void shouldListTools() {
    ToolDef[] tools = rest.getForObject("/api/tools", ToolDef[].class);
    assertThat(tools).isNotNull();
    List<String> names = List.of(tools).stream().map(t -> t.name).toList();
    assertThat(names).contains("web.fetch", "web.search", "doc.parse", "content.generate");
  }

  @Test
  void shouldExecuteContentGenerate() throws Exception {
    JsonNode input =
        MAPPER.readTree("{\"instruction\":\"hello\",\"provider\":\"mock\",\"model\":\"m\"}");
    ExecuteResult r =
        post(
            "/api/tools/execute",
            new ExecuteReq("content.generate", input, null),
            ExecuteResult.class);
    assertThat(r.status).isEqualTo("succeeded");
    assertThat(r.data).isNotNull();
    assertThat(r.data.get("content").asText()).isEqualTo("mock:hello");
  }

  @Test
  void shouldRespectToolEnabledOverride() throws Exception {
    settings.setBoolean("tool.enabled.web.fetch", false);

    JsonNode input = MAPPER.readTree("{\"url\":\"https://example.com\"}");
    ExecuteResult r =
        post("/api/tools/execute", new ExecuteReq("web.fetch", input, null), ExecuteResult.class);

    assertThat(r.status).isEqualTo("failed");
    assertThat(r.errorCode).isEqualTo("TOOL_DISABLED");
  }

  @Test
  void shouldRespectDomainAllowlistOverride() throws Exception {
    settings.setStringListJson("tool.domainAllowlist", List.of("allowed.test"));

    JsonNode input = MAPPER.readTree("{\"url\":\"https://example.com\"}");
    ExecuteResult r =
        post("/api/tools/execute", new ExecuteReq("web.fetch", input, null), ExecuteResult.class);

    assertThat(r.status).isEqualTo("failed");
    assertThat(r.errorCode).isEqualTo("IllegalArgumentException");
    assertThat(r.errorMessage).contains("domain not allowed");
  }

  private <T> T post(String path, Object req, Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return rest.postForObject(path, new HttpEntity<>(req, headers), responseType);
  }

  private static class ToolDef {
    public String name;
  }

  private record ExecuteReq(String name, JsonNode input, String confirmationId) {}

  private static class ExecuteResult {
    public String status;
    public String confirmationId;
    public String summary;
    public String errorCode;
    public String errorMessage;
    public JsonNode data;
  }
}
