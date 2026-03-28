package com.agtext.model.config;

import com.agtext.model.provider.OpenAiCompatibleChatProvider;
import com.agtext.model.provider.OpenAiCompatibleEmbeddingProvider;
import com.agtext.model.provider.mock.MockChatModelProvider;
import com.agtext.model.provider.mock.MockEmbeddingProvider;
import com.agtext.model.service.EmbeddingRegistry;
import com.agtext.model.service.EmbeddingSettingsProperties;
import com.agtext.model.service.ModelRegistry;
import com.agtext.model.service.ModelSettingsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模型层配置类：
 * 负责解析配置文件中的模型/向量引擎配置，并根据这些配置动态注册具体的 Provider 实现。
 */
@Configuration
@EnableConfigurationProperties({ModelSettingsProperties.class, EmbeddingSettingsProperties.class})
public class ModelConfig {

    /**
     * 初始化聊天模型注册表 (ModelRegistry)
     * 逻辑：扫描配置 -> 过滤无效配置 -> 注册 OpenAI 兼容供应商 -> (可选) 注册 Mock 供应商
     */
    @Bean
    public ModelRegistry modelRegistry(ModelSettingsProperties props) {
        ModelRegistry registry = new ModelRegistry();

        // 1. 遍历配置文件中定义的多个 provider（如 openai, deepseek, local-llm 等）
        props.providers().forEach((name, cfg) -> {
            // 2. 只有配置了有效的 baseUrl 和 apiKey，才会创建对应的提供者
            if (cfg != null
                    && cfg.baseUrl() != null
                    && !cfg.baseUrl().isBlank()
                    && cfg.apiKey() != null
                    && !cfg.apiKey().isBlank()) {

                // 注册符合 OpenAI API 标准的聊天模型实现
                registry.register(
                        new OpenAiCompatibleChatProvider(name, cfg.baseUrl(), cfg.apiKey()));
            }
        });

        // 3. 开发环境特殊处理：如果默认供应商设为 "mock"，则注入一个不产生真实扣费的模拟实现
        if ("mock".equalsIgnoreCase(props.defaultProvider())) {
            registry.register(new MockChatModelProvider());
        }
        return registry;
    }

    /**
     * 初始化向量模型注册表 (EmbeddingRegistry)
     * 用于 RAG（检索增强生成）流程中的文本向量化操作。
     */
    @Bean
    public EmbeddingRegistry embeddingRegistry(EmbeddingSettingsProperties props) {
        EmbeddingRegistry registry = new EmbeddingRegistry();

        // 1. 同样遍历向量服务的配置项
        props.providers().forEach((name, cfg) -> {
            if (cfg != null
                    && cfg.baseUrl() != null
                    && !cfg.baseUrl().isBlank()
                    && cfg.apiKey() != null
                    && !cfg.apiKey().isBlank()) {

                // 注册符合 OpenAI API 标准的 Embedding 实现
                registry.register(
                        new OpenAiCompatibleEmbeddingProvider(name, cfg.baseUrl(), cfg.apiKey()));
            }
        });

        // 2. Mock 支持：用于本地测试，通常返回固定长度的随机向量
        if ("mock".equalsIgnoreCase(props.defaultProvider())) {
            registry.register(new MockEmbeddingProvider());
        }
        return registry;
    }
}