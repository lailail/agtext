package com.agtext.knowledge.domain;

import java.time.Instant;

/**
 * 知识分片领域模型：RAG 系统中进行向量化（Embedding）处理和语义检索的最小原子单位。
 * 该模型承载了经过清洗、切分后的文本片段。
 * * @param id 分片唯一标识 ID
 * @param knowledgeDocumentId 关联的文档 ID，建立分片与原始文件的一对多关系
 * @param importJobId 关联的导入任务 ID，用于追踪该分片产生的特定批次，方便进行版本回滚或脏数据清理
 * @param chunkIndex 在原始文档中的物理顺序索引（从 0 开始），在召回后可用于还原上下文
 * @param content 核心文本内容：这是最终会被喂给大模型（LLM）作为 Prompt 上下文的原文
 * @param chunkHash 内容哈希值：用于检测内容变动，实现增量索引（Incremental RAG）并避免重复导入
 * @param createdAt 创建时间（UTC）
 * @param updatedAt 最后更新时间（UTC）
 */
public record KnowledgeChunk(
        long id,
        long knowledgeDocumentId,
        Long importJobId,
        int chunkIndex,
        String content,
        String chunkHash,
        Instant createdAt,
        Instant updatedAt) {}