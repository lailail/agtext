package com.agtext.knowledge.domain;

import java.time.Instant;

/**
 * 解析报告领域模型：记录文档从原始格式（如 PDF, HTML）转换为结构化文本后的详细指标。
 * 核心逻辑：
 * 1. 质量审计：通过记录提取字符数、页数、切片数，评估解析算法的有效性。
 * 2. 调试追溯：记录使用的解析器名称及失败位置，便于针对特定格式优化解析策略。
 * 3. 结果预览：提供文本采样，允许用户在不查看完整分片的情况下快速核对解析准确度。
 *
 * @param id 报告唯一标识 ID
 * @param knowledgeDocumentId 关联的文档 ID
 * @param jobId 关联的导入任务 ID，用于标识该报告属于哪一次处理批次
 * @param summary 解析执行情况的简要总结（如：解析成功，部分图片文字无法提取）
 * @param extractedChars 从文档中成功提取的总有效字符数，是衡量解析质量的关键指标
 * @param pageCount 文档的总页数（仅对分页格式如 PDF, Docx 有效）
 * @param chunkCount 最终生成的知识分片（Chunk）总数
 * @param parserName 所使用的具体解析技术或组件名称（如：Apache Tika, PyMuPDF, OCR-Processor）
 * @param failedAt 记录解析中断的具体位置或逻辑点，用于排查复杂文档的崩溃原因
 * @param samplePreview 提取文本的前 N 个字符采样，用于展示给用户进行快速核验
 * @param createdAt 记录创建时间
 * @param updatedAt 记录最后更新时间
 */
public record ParseReport(
        long id,
        long knowledgeDocumentId,
        Long jobId,
        String summary,
        Long extractedChars,
        Integer pageCount,
        Integer chunkCount,
        String parserName,
        String failedAt,
        String samplePreview,
        Instant createdAt,
        Instant updatedAt) {}