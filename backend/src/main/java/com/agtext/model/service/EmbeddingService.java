package com.agtext.model.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.model.domain.EmbeddingResponse;
import com.agtext.model.provider.EmbeddingProvider;
import org.springframework.stereotype.Service;

/**
 * 向量核心服务：
 * 对外提供统一的向量化接口。它屏蔽了底层多个注册供应商（Registry）和
 * 复杂配置（Properties）的细节，支持按需动态切换模型。
 */
@Service
public class EmbeddingService {
  private final EmbeddingRegistry registry; // 存放具体实现类的注册表
  private final EmbeddingSettingsProperties props; // 存放 YAML 配置的属性对象

  public EmbeddingService(EmbeddingRegistry registry, EmbeddingSettingsProperties props) {
    this.registry = registry;
    this.props = props;
  }

  /**
   * 执行向量化操作
   * 逻辑优先级：方法参数指定的 provider/model > 配置文件中该 provider 的专属 model > 全局默认值
   * * @param providerOverride 手动指定的供应商（可为 null，则使用默认值）
   * @param modelOverride    手动指定的模型名称（可为 null，则使用默认值）
   * @param input            待向量化的原始文本内容
   * @return 包含向量数组的响应对象
   */
  public EmbeddingResponse embed(String providerOverride, String modelOverride, String input) {
    // 1. 确定供应商：如果调用方没传，就去配置里读 app.embedding.default-provider
    String provider =
            providerOverride == null || providerOverride.isBlank()
                    ? props.defaultProvider()
                    : providerOverride;

    // 2. 从注册表中查找对应的实现类
    // 严肃性提示：如果配置了一个没有对应实现类的 provider，将抛出业务异常
    EmbeddingProvider p =
            registry
                    .find(provider)
                    .orElseThrow(
                            () ->
                                    new NotFoundException(
                                            "EMBEDDING_PROVIDER_NOT_FOUND", "Unknown embedding provider: " + provider));

    // 3. 确定具体模型名称
    String model =
            modelOverride == null || modelOverride.isBlank()
                    ? defaultModelFor(provider)
                    : modelOverride;

    // 4. 调用底层 Provider 执行真正的 HTTP 请求或本地计算
    return p.embed(model, input);
  }

  /**
   * 确定特定供应商的默认模型逻辑
   */
  private String defaultModelFor(String provider) {
    // 优先读取该供应商下特定的模型配置（如 openai 的 text-embedding-3-small）
    EmbeddingSettingsProperties.ProviderConfig cfg = props.providers().get(provider);
    if (cfg != null && cfg.model() != null && !cfg.model().isBlank()) {
      return cfg.model();
    }
    // 最后回退到全局定义的默认模型名
    return props.defaultModel();
  }
}