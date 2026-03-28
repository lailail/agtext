package com.agtext.model.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.model.provider.ChatModelProvider;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 聊天模型核心服务：
 * 作为系统与大语言模型（LLM）交互的唯一门面（Facade）。
 * 负责解析模型选择策略、调度注册表中的 Provider 并返回统一的 ModelResponse。
 */
@Service
public class ModelService {
  private final ModelRegistry registry; // 存放具体供应商实现的注册表
  private final ModelSettings settings; // 存放 YAML 配置及默认值逻辑的对象

  public ModelService(ModelRegistry registry, ModelSettings settings) {
    this.registry = registry;
    this.settings = settings;
  }

  /**
   * 执行对话生成请求
   * * 逻辑优先级：
   * 1. 方法参数显式指定 (Override) >
   * 2. 配置文件中该供应商的专属模型 >
   * 3. 系统全局默认模型
   * * @param providerOverride 手动指定的供应商名称（如 "deepseek"），传 null 则用默认
   * @param modelOverride    手动指定的模型 ID（如 "gpt-4o"），传 null 则用默认
   * @param messages         完整的对话上下文列表
   * @return 包含模型生成文本及元数据的响应对象
   */
  public ModelResponse chat(
          String providerOverride, String modelOverride, List<ChatMessage> messages) {

    // 1. 确定最终使用的供应商名称
    String providerName =
            providerOverride == null || providerOverride.isBlank()
                    ? settings.defaultProvider()
                    : providerOverride;

    // 2. 确定最终使用的模型名称
    // settings.defaultModelFor(providerName) 会尝试获取该供应商下的特定默认配置
    String modelName =
            modelOverride == null || modelOverride.isBlank()
                    ? settings.defaultModelFor(providerName)
                    : modelOverride;

    // 3. 从注册表中检索对应的 Provider 实例
    // 实事求是地处理“供应商未注册”的情况，抛出业务级 404 异常
    ChatModelProvider provider =
            registry
                    .find(providerName)
                    .orElseThrow(
                            () ->
                                    new NotFoundException(
                                            "MODEL_PROVIDER_NOT_FOUND", "Unknown model provider: " + providerName));

    // 4. 调用底层的具体实现（如 OpenAICompatibleChatProvider）执行 HTTP 交互
    return provider.chat(modelName, messages);
  }
}