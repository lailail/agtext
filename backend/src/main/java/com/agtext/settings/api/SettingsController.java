package com.agtext.settings.api;

import com.agtext.model.service.ModelSettingsProperties;
import com.agtext.settings.service.AppSettingsService;
import com.agtext.tool.platform.service.ToolRegistry;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
  private final AppSettingsService settings;
  private final ToolRegistry tools;
  private final ModelSettingsProperties modelProps;

  public SettingsController(
      AppSettingsService settings, ToolRegistry tools, ModelSettingsProperties modelProps) {
    this.settings = settings;
    this.tools = tools;
    this.modelProps = modelProps;
  }

  @GetMapping("/tools")
  public ToolSettingsResponse toolSettings() {
    List<ToolItem> list =
        tools.listDefinitions().stream()
            .map(
                d ->
                    new ToolItem(
                        d.name(),
                        d.description(),
                        d.type().name(),
                        d.requiresConfirmation(),
                        isToolEnabled(d.name())))
            .toList();
    List<String> allow = settings.getStringListJson("tool.domainAllowlist").orElse(List.of());
    return new ToolSettingsResponse(allow, list);
  }

  @PostMapping("/tools/domain-allowlist")
  public void updateDomainAllowlist(@RequestBody DomainAllowlistRequest req) {
    settings.setStringListJson("tool.domainAllowlist", req.domains());
  }

  @PostMapping("/tools/{name}/enabled")
  public void setToolEnabled(@PathVariable("name") String name, @RequestBody EnabledRequest req) {
    settings.setBoolean("tool.enabled." + name, req.enabled());
  }

  @GetMapping("/models")
  public ModelSettingsResponse modelSettings() {
    String defaultProvider =
        settings.getString("model.defaultProvider").orElse(modelProps.defaultProvider());
    List<ModelProviderItem> providers =
        modelProps.providers().entrySet().stream()
            .map(
                e -> {
                  String name = e.getKey();
                  ModelSettingsProperties.ProviderConfig cfg = e.getValue();
                  String model =
                      settings
                          .getString("model.providers." + name + ".model")
                          .orElse(cfg == null ? null : cfg.model());
                  String baseUrl = cfg == null ? null : cfg.baseUrl();
                  boolean apiKeyConfigured =
                      cfg != null && cfg.apiKey() != null && !cfg.apiKey().isBlank();
                  return new ModelProviderItem(name, baseUrl, apiKeyConfigured, model);
                })
            .toList();
    return new ModelSettingsResponse(defaultProvider, modelProps.fallbackModel(), providers);
  }

  @PostMapping("/models/default-provider")
  public void updateDefaultProvider(@RequestBody DefaultProviderRequest req) {
    settings.setString("model.defaultProvider", req.defaultProvider());
  }

  @PostMapping("/models/providers/{name}")
  public void updateProviderModel(
      @PathVariable("name") String name, @RequestBody ProviderModelRequest req) {
    settings.setString("model.providers." + name + ".model", req.model());
  }

  private boolean isToolEnabled(String name) {
    return settings.getBoolean("tool.enabled." + name).orElse(true);
  }

  public record ToolSettingsResponse(List<String> domainAllowlist, List<ToolItem> tools) {}

  public record ToolItem(
      String name,
      String description,
      String type,
      boolean requiresConfirmation,
      boolean enabled) {}

  public record DomainAllowlistRequest(List<String> domains) {}

  public record EnabledRequest(boolean enabled) {}

  public record ModelSettingsResponse(
      String defaultProvider, String fallbackModel, List<ModelProviderItem> providers) {}

  public record ModelProviderItem(
      String name, String baseUrl, boolean apiKeyConfigured, String model) {}

  public record DefaultProviderRequest(String defaultProvider) {}

  public record ProviderModelRequest(String model) {}
}
