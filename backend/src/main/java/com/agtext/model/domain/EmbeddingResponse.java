package com.agtext.model.domain;

/**
 * 向量化响应结果模型：
 * 采用 Java Record 实现，封装了模型生成的稠密向量（Dense Vector）及其元数据。
 */
public record EmbeddingResponse(
        /**
         * 供应商名称：
         * 记录生成该向量的 AI 服务商（如 "openai", "huggingface", "local-ollama"）。
         * 用于在多模型环境下确保检索时的模型一致性。
         */
        String provider,

        /**
         * 模型 ID：
         * 生成向量的具体模型全名（如 "text-embedding-3-small" 或 "bge-m3"）。
         * 严肃性提示：不同模型生成的向量维度和语义空间不同，不可混用。
         */
        String model,

        /**
         * 特征向量：
         * 文本的数学表示，通常是一个固定长度的浮点数数组。
         * 该数组将被存入向量数据库（如 Milvus）或用于计算余弦相似度。
         */
        float[] vector
) {}