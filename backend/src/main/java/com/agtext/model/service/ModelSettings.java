package com.agtext.model.service;

import com.agtext.settings.service.AppSettingsService;
import org.springframework.stereotype.Component;

/**
 * 模型设置决策组件：
 * 负责解析和计算当前环境下应当选用的 AI 供应商及其对应的模型 ID。
 * 它整合了静态配置 (YAML) 与动态配置 (数据库/AppSettings)，支持线上热更新模型切换。
 */
@Component
public class ModelSettings {
  private final ModelSettingsProperties props;    // 映射自 application.yml 的静态配置
  private final AppSettingsService appSettings;   // 动态配置服务（通常来自数据库或配置中心）

  public ModelSettings(ModelSettingsProperties props, AppSettingsService appSettings) {
    this.props = props;
    this.appSettings = appSettings;
  }

  /**
   * 获取当前默认的模型供应商（如 "openai", "deepseek"）
   * 决策优先级：
   * 1. 动态设置 (appSettings) 中的 "model.defaultProvider"
   * 2. 静态设置 (props) 中的 defaultProvider
   * * @return 最终选定的供应商名称
   */
  public String defaultProvider() {
    return appSettings.getString("model.defaultProvider").orElse(props.defaultProvider());
  }

  /**
   * 获取指定供应商下的默认模型 ID（如 "gpt-4o", "deepseek-chat"）
   * 决策优先级：
   * 1. 动态设置中的具体供应商模型覆盖（如 model.providers.openai.model）
   * 2. 静态配置 (YAML) 中该供应商下的 model 字段
   * 3. 静态配置 (YAML) 中的全局兜底模型 (fallbackModel)
   * * @param provider 供应商名称
   * @return 最终执行推理的模型 ID
   */
  public String defaultModelFor(String provider) {
    // 1. 尝试从动态配置中获取覆盖值（允许管理员在不重启服务的情况下切换模型）
    String override = appSettings.getString("model.providers." + provider + ".model").orElse(null);
    if (override != null && !override.isBlank()) {
      return override;
    }

    // 2. 查找 YAML 中该供应商的特定配置
    ModelSettingsProperties.ProviderConfig cfg = props.providers().get(provider);

    // 3. 如果该供应商未配置特定模型，则返回全局兜底模型 (fallbackModel)
    // 严肃性提示：fallbackModel 确保了即使配置缺失，系统也不会因找不到模型而崩溃
    if (cfg == null || cfg.model() == null || cfg.model().isBlank()) {
      return props.fallbackModel();
    }

    return cfg.model();
  }
}