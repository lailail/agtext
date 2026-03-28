package com.agtext.model.service;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量模型全局配置属性：
 * 映射配置文件中以 'app.embedding' 为前缀的各项参数。
 * 使用 Record 确保配置项在加载后具备不可变性（Immutable）。
 */
@ConfigurationProperties(prefix = "app.embedding")
public record EmbeddingSettingsProperties(
        /**
         * 全局默认供应商：
         * 当业务代码未明确指定 provider 时，系统默认选用的服务商（如 "openai" 或 "deepseek"）。
         */
        String defaultProvider,

        /**
         * 全局默认模型名：
         * 当特定供应商未配置 model 且调用方未指定时，作为最后的兜底模型名称。
         */
        String defaultModel,

        /**
         * 供应商详细配置映射：
         * Key: 供应商标识（对应 Registry 中的 name）。
         * Value: 包含 BaseUrl, ApiKey 等敏感信息的配置对象。
         */
        Map<String, ProviderConfig> providers
) {
  /**
   * 具体的供应商配置明细：
   * 包含连接 AI 服务所需的端点、凭证以及该供应商推荐使用的默认模型。
   */
  public record ProviderConfig(
          /**
           * API 基础路径：
           * 例如 "https://api.openai.com" 或私有化部署的端点。
           */
          String baseUrl,

          /**
           * 鉴权密钥：
           * 用于 API 请求头的 Authorization 字段。
           */
          String apiKey,

          /**
           * 供应商级别的默认模型：
           * 例如在 openai 供应商下默认使用 "text-embedding-3-small"。
           */
          String model
  ) {}
}