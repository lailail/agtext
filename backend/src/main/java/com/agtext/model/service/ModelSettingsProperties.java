package com.agtext.model.service;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 聊天模型全局配置属性：
 * 负责定义系统接入的所有 AI 供应商（OpenAI, DeepSeek 等）的连接参数。
 * 使用 Record 确保配置项在应用运行期间是不可变的（Immutable），增强了并发安全性。
 */
@ConfigurationProperties(prefix = "app.model")
public record ModelSettingsProperties(
        /**
         * 默认供应商标识：
         * 当业务请求未指定供应商时，系统首选的服务商名称（例如 "openai"）。
         */
        String defaultProvider,

        /**
         * 全局兜底模型 ID：
         * 当某个供应商未配置具体的 model 且调用方也未指定时，作为系统最后的运行边界。
         * 例如："gpt-4o-mini" 或 "deepseek-chat"。
         */
        String fallbackModel,

        /**
         * 供应商详细配置映射：
         * Key: 供应商唯一标识（如 "openai", "local-llama"）。
         * Value: 包含 BaseUrl, ApiKey 等敏感信息的配置明细。
         */
        Map<String, ProviderConfig> providers
) {
  /**
   * 具体的供应商连接配置：
   * 定义了访问特定 AI 引擎所需的物理参数。
   */
  public record ProviderConfig(
          /**
           * API 基础路径：
           * 例如 "https://api.openai.com" 或私有化部署的端点路径。
           */
          String baseUrl,

          /**
           * 身份验证密钥：
           * 用于构造 HTTP 请求头中的 Authorization: Bearer {apiKey}。
           */
          String apiKey,

          /**
           * 供应商级别的默认模型：
           * 指定在该供应商下默认调用的模型 ID。
           */
          String model
  ) {}
}