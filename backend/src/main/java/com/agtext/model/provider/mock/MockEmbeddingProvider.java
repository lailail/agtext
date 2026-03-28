package com.agtext.model.provider.mock;

import com.agtext.model.domain.EmbeddingResponse;
import com.agtext.model.provider.EmbeddingProvider;

/**
 * 模拟向量模型提供者：
 * 用于在开发和测试环境下替代真实的 Embedding API（如 OpenAI text-embedding-3-small）。
 * 它通过确定性的算法将字符串转换为固定长度的浮点数组，模拟向量空间的映射。
 */
public class MockEmbeddingProvider implements EmbeddingProvider {

  /**
   * 供应商标识：
   * 返回 "mock"，由 EmbeddingRegistry 用于匹配配置文件中的 provider 名称。
   */
  @Override
  public String name() {
    return "mock";
  }

  /**
   * 模拟向量化逻辑：
   * 行为：将输入文本通过位运算转换为一个 8 维的 0/1 向量。
   * 特点：相同的输入文字永远产生相同的“伪向量”，这对于单元测试中的断言（Assertion）至关重要。
   */
  @Override
  public EmbeddingResponse embed(String model, String input) {
    // 1. 定义固定维度（此处为 8 维，真实模型通常为 768 或 1536 维）
    float[] vec = new float[8];
    String text = input == null ? "" : input;

    // 2. 确定性哈希映射算法
    // 通过对字符串哈希值的位操作来填充数组，确保了语义无关但结果可重复
    for (int i = 0; i < vec.length; i++) {
      // 提取哈希值的第 i 位，若为 1 则填入 1.0f，否则为 0.0f
      vec[i] = ((text.hashCode() >> i) & 1) == 1 ? 1.0f : 0.0f;
    }

    // 3. 构造响应，标记 provider 为 "mock"
    return new EmbeddingResponse("mock", model, vec);
  }
}