package com.agtext.knowledge.domain;

import java.time.Instant;

/**
 * 知识导入任务领域模型：记录知识文档从上传到向量化的异步执行状态。
 * 核心逻辑：
 * 1. 任务状态追踪：记录任务的全局执行状态（Status）与具体的业务执行阶段（Stage）。
 * 2. 进度监控：提供进度百分比（Progress）用于前端进度条展示。
 * 3. 容错与审计：记录任务的启动、完成、取消时间及失败后的错误堆栈。
 *
 * @param id 任务唯一标识 ID
 * @param knowledgeBaseId 所属知识库 ID
 * @param documentId 关联的文档 ID（任务启动后或完成后生成的文档引用）
 * @param status 任务全局状态：如 'PENDING'（排队中）, 'RUNNING'（执行中）, 'COMPLETED'（已成功）, 'FAILED'（失败）, 'CANCELLED'（已取消）
 * @param startedAt 任务实际开始进入执行器的时间（UTC）
 * @param finishedAt 任务终结时间（成功或失败后的记录时间）
 * @param stage 当前业务子阶段：如 'FETCHING'（下载中）, 'PARSING'（解析中）, 'CHUNKING'（切片中）, 'INDEXING'（索引中）
 * @param progress 任务进度百分比（0-100）
 * @param errorMessage 任务失败时的核心错误描述，用于后台审计与用户提示
 * @param cancelledAt 如果任务被用户手动取消，记录取消的具体时间
 * @param createdAt 记录创建时间
 * @param updatedAt 记录最后更新时间
 */
public record KnowledgeImportJob(
        long id,
        long knowledgeBaseId,
        Long documentId,
        String status,
        Instant startedAt,
        Instant finishedAt,
        String stage,
        Integer progress,
        String errorMessage,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt) {}