package com.agtext.knowledge.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切片工具类：
 * 将长文档拆分为适合 LLM 窗口大小和向量化模型处理的短块。
 */
public final class TextChunker {

  // 私有构造函数，防止工具类被实例化
  private TextChunker() {}

  /**
   * 核心切片逻辑
   * @param text 原始输入文本
   * @param maxChars 每个切片的最大字符数（Chunk Size）
   * @param overlap 相邻切片之间的重叠字符数，用于保持语义连续性
   * @return 切片后的字符串列表
   */
  public static List<String> chunk(String text, int maxChars, int overlap) {
    if (text == null) {
      return List.of();
    }

    // 1. 预处理：标准化换行符并去除首尾空格
    String normalized = normalize(text);
    if (normalized.isBlank()) {
      return List.of();
    }

    // 2. 参数安全校验与限制
    // 强制最小切片为 200 字符；限制重叠度最高不超过切片大小的一半，防止死循环或无效切片
    int safeMax = Math.max(200, maxChars);
    int safeOverlap = Math.max(0, Math.min(overlap, safeMax / 2));

    List<String> chunks = new ArrayList<>();
    int start = 0;

    // 3. 滑动窗口切片算法
    while (start < normalized.length()) {
      // 确定当前块的结束位置
      int end = Math.min(normalized.length(), start + safeMax);

      // 提取子串
      String slice = normalized.substring(start, end).trim();
      if (!slice.isBlank()) {
        chunks.add(slice);
      }

      // 如果已经处理到文本末尾，跳出循环
      if (end >= normalized.length()) {
        break;
      }

      // 计算下一个窗口的起始位置：当前结束位置减去重叠长度
      // 这确保了下一块的开头包含上一块的结尾，避免语义在边界处断裂
      start = Math.max(0, end - safeOverlap);
    }
    return chunks;
  }

  /**
   * 标准化文本：
   * 将 Windows (\r\n) 和旧版 Mac (\r) 换行符统一替换为 Linux 风格 (\n)。
   */
  public static String normalize(String text) {
    if (text == null) return "";
    return text.replace("\r\n", "\n").replace('\r', '\n').trim();
  }
}