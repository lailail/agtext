package com.agtext.model.service;

import com.agtext.settings.service.AppSettingsService;
import org.springframework.stereotype.Component;

@Component
public class ModelSettings {
  private final ModelSettingsProperties props;
  private final AppSettingsService appSettings;

  public ModelSettings(ModelSettingsProperties props, AppSettingsService appSettings) {
    this.props = props;
    this.appSettings = appSettings;
  }

  public String defaultProvider() {
    return appSettings.getString("model.defaultProvider").orElse(props.defaultProvider());
  }

  public String defaultModelFor(String provider) {
    String override = appSettings.getString("model.providers." + provider + ".model").orElse(null);
    if (override != null && !override.isBlank()) {
      return override;
    }
    ModelSettingsProperties.ProviderConfig cfg = props.providers().get(provider);
    if (cfg == null || cfg.model() == null || cfg.model().isBlank()) {
      return props.fallbackModel();
    }
    return cfg.model();
  }
}
