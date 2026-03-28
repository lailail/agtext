package com.agtext.model.provider;

import com.agtext.model.domain.EmbeddingResponse;

/**
 * 向量模型供应商接口：
 * 定义了将非结构化文本转换为数学向量（Embedding）的标准操作。
 * 在 RAG 流程中，该接口用于文档入库时的“切片向量化”以及搜索时的“查询向量化”。
 */
public interface EmbeddingProvider {

  /**
   * 供应商唯一标识：
   * 用于在配置（application.yml）中区分不同的服务提供商（如 "openai", "local-ollama", "mock"）。
   * @return 供应商名称
   */
  String name();

  /**
   * 执行向量化提取：
   * 将输入的文本字符串映射为一个固定维度的浮点数数组。
   * * @param model 具体的向量模型名称（如 "text-embedding-3-small", "bge-m3"）
   * @param input 待转化的原始文本
   * @return 包含向量数据及元数据的 EmbeddingResponse 对象
   * @throws RuntimeException 当 API 请求超时、文本超出模型最大 Token 限制或鉴权失败时抛出
   */
  EmbeddingResponse embed(String model, String input);
}