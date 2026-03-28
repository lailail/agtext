package com.agtext.knowledge.domain;

import java.time.Instant;

/**
 * 知识分片向量模型：存储文本分片（Chunk）经过 Embedding 模型计算后的高维向量表示。
 * 核心逻辑：
 * 1. 实现文本与向量的一对多关联（同一分片可使用不同模型生成向量）。
 * 2. 记录模型元数据（Provider/Model/Dim），确保检索时的模型一致性。
 * * @param id 向量记录唯一标识
 * @param knowledgeChunkId 关联的文本分片 ID
 * @param importJobId 关联的导入任务 ID，用于批处理追踪与清理
 * @param provider 向量化服务供应商（如 OpenAI, DashScope, LocalHuggingFace）
 * @param model 使用的特定 Embedding 模型名称（如 text-embedding-3-small）
 * @param dim 向量维度（如 1536, 768, 1024），检索时查询向量与库内向量维度必须一致
 * @param vectorJson 向量数据的 JSON 字符串表示（如 [0.12, -0.05, ...]），用于兼容不支持原生向量类型的数据库
 * @param createdAt 创建时间
 * @param updatedAt 最后更新时间
 */
public record KnowledgeChunkEmbedding(
        long id,
        long knowledgeChunkId,
        long importJobId,
        String provider,
        String model,
        Integer dim,
        String vectorJson,
        Instant createdAt,
        Instant updatedAt) {}