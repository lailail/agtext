package com.agtext.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.agtext.settings.service.AppSettingsService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SettingsIntegrationTest {
  @Autowired private TestRestTemplate rest;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AppSettingsService settings;

  @AfterEach
  void cleanup() {
    deleteKey("tool.domainAllowlist");
    deleteKey("tool.enabled.web.fetch");
    deleteKey("model.defaultProvider");
  }

  @Test
  void shouldGetAndUpdateToolSettings() {
    ToolSettingsResponse before =
        rest.getForObject("/api/settings/tools", ToolSettingsResponse.class);
    assertThat(before).isNotNull();
    assertThat(before.tools).isNotEmpty();
    assertThat(before.domainAllowlist).isNotNull();

    post(
        "/api/settings/tools/domain-allowlist", new DomainAllowlistRequest(List.of("example.com")));

    post("/api/settings/tools/web.fetch/enabled", new EnabledRequest(false));

    ToolSettingsResponse after =
        rest.getForObject("/api/settings/tools", ToolSettingsResponse.class);
    assertThat(after).isNotNull();
    assertThat(after.domainAllowlist).containsExactly("example.com");
    assertThat(after.tools).anyMatch(t -> "web.fetch".equals(t.name) && !t.enabled);
  }

  @Test
  void shouldGetAndUpdateModelSettingsDefaultProvider() {
    ModelSettingsResponse before =
        rest.getForObject("/api/settings/models", ModelSettingsResponse.class);
    assertThat(before).isNotNull();
    assertThat(before.defaultProvider).isNotBlank();

    // test profile providers are empty; we only validate defaultProvider override path
    post("/api/settings/models/default-provider", new DefaultProviderRequest("mock2"));

    ModelSettingsResponse after =
        rest.getForObject("/api/settings/models", ModelSettingsResponse.class);
    assertThat(after).isNotNull();
    assertThat(after.defaultProvider).isEqualTo("mock2");

    // ensure persistence layer sees it too
    assertThat(settings.getString("model.defaultProvider")).contains("mock2");
  }

  private void deleteKey(String key) {
    jdbc.update("delete from app_settings where k=?", key);
  }

  private void post(String path, Object body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    rest.postForEntity(path, new HttpEntity<>(body, headers), Void.class);
  }

  public static class ToolSettingsResponse {
    public List<String> domainAllowlist;
    public List<ToolItem> tools;
  }

  public static class ToolItem {
    public String name;
    public boolean enabled;
  }

  public record DomainAllowlistRequest(List<String> domains) {}

  public record EnabledRequest(boolean enabled) {}

  public static class ModelSettingsResponse {
    public String defaultProvider;
    public String fallbackModel;
    public List<Object> providers;
  }

  public record DefaultProviderRequest(String defaultProvider) {}
}
