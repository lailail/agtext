package com.agtext.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.agtext.model.service.ModelSettings;
import com.agtext.model.service.ModelSettingsProperties;
import com.agtext.settings.service.AppSettingsService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ModelSettingsTest {
  @Test
  void shouldUseOverrideProviderAndModelWhenPresent() {
    AppSettingsService appSettings = Mockito.mock(AppSettingsService.class);
    ModelSettingsProperties props =
        new ModelSettingsProperties(
            "openai",
            "fallback",
            Map.of("openai", new ModelSettingsProperties.ProviderConfig("u", "k", "gpt-5.1")));

    when(appSettings.getString("model.defaultProvider")).thenReturn(Optional.of("mock"));
    when(appSettings.getString("model.providers.openai.model")).thenReturn(Optional.of("m2"));

    ModelSettings settings = new ModelSettings(props, appSettings);
    assertThat(settings.defaultProvider()).isEqualTo("mock");
    assertThat(settings.defaultModelFor("openai")).isEqualTo("m2");
  }

  @Test
  void shouldFallbackWhenProviderMissing() {
    AppSettingsService appSettings = Mockito.mock(AppSettingsService.class);
    ModelSettingsProperties props = new ModelSettingsProperties("openai", "fallback", Map.of());
    when(appSettings.getString("model.providers.none.model")).thenReturn(Optional.empty());

    ModelSettings settings = new ModelSettings(props, appSettings);
    assertThat(settings.defaultModelFor("none")).isEqualTo("fallback");
  }
}
