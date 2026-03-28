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

/**
 * 系统设置控制器：
 * 提供对工具（Tools）和模型（Models）配置的动态增删改查接口。
 * 它通过 AppSettingsService 将配置持久化到数据库，从而实现无需重启服务的热更新。
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {
  private final AppSettingsService settings;      // 动态配置存储服务
  private final ToolRegistry tools;              // 已注册的 AI 工具注册表
  private final ModelSettingsProperties modelProps; // YAML 中的模型初始配置

  public SettingsController(
          AppSettingsService settings, ToolRegistry tools, ModelSettingsProperties modelProps) {
    this.settings = settings;
    this.tools = tools;
    this.modelProps = modelProps;
  }

  /**
   * 获取工具设置列表：
   * 包含当前系统支持的所有工具及其启用状态、类型和域名白名单。
   */
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
    // 从动态配置中读取域名白名单，若无则返回空列表
    List<String> allow = settings.getStringListJson("tool.domainAllowlist").orElse(List.of());
    return new ToolSettingsResponse(allow, list);
  }

  /**
   * 更新工具域名白名单：
   * 用于限制 AI 工具可以访问的外部 API 范围。
   */
  @PostMapping("/tools/domain-allowlist")
  public void updateDomainAllowlist(@RequestBody DomainAllowlistRequest req) {
    settings.setStringListJson("tool.domainAllowlist", req.domains());
  }

  /**
   * 开启或关闭特定工具：
   * 配置键格式：tool.enabled.{toolName}
   */
  @PostMapping("/tools/{name}/enabled")
  public void setToolEnabled(@PathVariable("name") String name, @RequestBody EnabledRequest req) {
    settings.setBoolean("tool.enabled." + name, req.enabled());
  }

  /**
   * 获取模型配置概览：
   * 整合了 YAML 静态配置与数据库动态覆盖值，返回当前生效的供应商和模型列表。
   */
  @GetMapping("/models")
  public ModelSettingsResponse modelSettings() {
    // 优先从数据库读取默认供应商，否则回退到 YAML
    String defaultProvider =
            settings.getString("model.defaultProvider").orElse(modelProps.defaultProvider());

    List<ModelProviderItem> providers =
            modelProps.providers().entrySet().stream()
                    .map(
                            e -> {
                              String name = e.getKey();
                              ModelSettingsProperties.ProviderConfig cfg = e.getValue();
                              // 检查是否有针对该供应商的动态模型覆盖
                              String model =
                                      settings
                                              .getString("model.providers." + name + ".model")
                                              .orElse(cfg == null ? null : cfg.model());
                              String baseUrl = cfg == null ? null : cfg.baseUrl();
                              // 敏感信息处理：仅告知前端 API Key 是否已配置，不返回具体明文
                              boolean apiKeyConfigured =
                                      cfg != null && cfg.apiKey() != null && !cfg.apiKey().isBlank();
                              return new ModelProviderItem(name, baseUrl, apiKeyConfigured, model);
                            })
                    .toList();
    return new ModelSettingsResponse(defaultProvider, modelProps.fallbackModel(), providers);
  }

  /**
   * 动态更新全局默认供应商
   */
  @PostMapping("/models/default-provider")
  public void updateDefaultProvider(@RequestBody DefaultProviderRequest req) {
    settings.setString("model.defaultProvider", req.defaultProvider());
  }

  /**
   * 动态更新特定供应商的模型 ID
   */
  @PostMapping("/models/providers/{name}")
  public void updateProviderModel(
          @PathVariable("name") String name, @RequestBody ProviderModelRequest req) {
    settings.setString("model.providers." + name + ".model", req.model());
  }

  /**
   * 辅助方法：判断工具是否启用，默认开启
   */
  private boolean isToolEnabled(String name) {
    return settings.getBoolean("tool.enabled." + name).orElse(true);
  }

  // --- 响应与请求 DTO (Data Transfer Objects) 定义 ---
  // 使用 Record 确保接口定义的不可变性和简洁性

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