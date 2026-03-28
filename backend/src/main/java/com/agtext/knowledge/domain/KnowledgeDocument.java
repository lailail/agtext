package com.agtext.knowledge.domain;

import java.time.Instant;

/**
 * 知识文档领域模型：代表知识库中一个独立的知识来源（如一个 PDF 文件、一个网页或一段 Markdown）。
 * 核心逻辑：
 * 1. 维护文档的元数据及来源追溯（Source Tracking）。
 * 2. 通过多维状态位（Status/Parse/Index）精细化追踪 RAG 处理流水线的进度。
 * 3. 利用 Hash 机制实现内容去重与增量更新。
 * * @param id 文档唯一标识 ID
 * @param knowledgeBaseId 所属知识库 ID
 * @param sourceType 来源类型：如 'file', 'url', 'markdown'，决定了后续使用哪种解析器
 * @param sourceUri 来源定位符：文件路径、URL 地址或云存储 Key
 * @param title 文档标题，用于 UI 展示及检索时的来源引用说明
 * @param status 文档整体业务状态：如 'NORMAL'（正常）, 'DISABLED'（禁用）, 'DELETED'（已删除）
 * @param parseStatus 文本解析状态：'PENDING'（待解析）, 'PARSING'（解析中）, 'COMPLETED'（解析成功）, 'FAILED'（解析失败）
 * @param indexStatus 向量索引状态：'PENDING'（待索引）, 'INDEXING'（索引中）, 'COMPLETED'（索引成功）, 'FAILED'（索引失败）
 * @param errorMessage 记录处理过程中的异常堆栈或错误简述，方便排查解析/索引失败的原因
 * @param contentHash 文档原始内容的哈希值（如 MD5/SHA256），用于检测内容重复上传或版本变更
 * @param latestImportJobId 最近一次完成（不论成功失败）的导入任务 ID
 * @param activeImportJobId 当前正在运行中的导入任务 ID，用于防止同一文档并发触发多个解析任务
 * @param createdAt 创建时间（UTC）
 * @param updatedAt 最后更新时间（UTC）
 */
public record KnowledgeDocument(
        long id,
        long knowledgeBaseId,
        String sourceType,
        String sourceUri,
        String title,
        String status,
        String parseStatus,
        String indexStatus,
        String errorMessage,
        String contentHash,
        Long latestImportJobId,
        Long activeImportJobId,
        Instant createdAt,
        Instant updatedAt) {}