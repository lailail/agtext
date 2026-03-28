package com.agtext.knowledge.domain;

import java.time.Instant;

/**
 * 知识库领域模型：代表一个逻辑上的知识集合。
 * 在 RAG（检索增强生成）流程中，知识库作为文档的容器，是权限隔离与语义检索的基本单位。
 * * @param id 数据库主键 ID
 * @param name 知识库名称（如“产品手册”、“技术文档”）
 * @param description 知识库描述，用于记录该库的用途或覆盖范围
 * @param createdAt 创建时间（UTC）
 * @param updatedAt 最后一次更新时间（UTC），包括名称修改或内部文档变动触发的更新
 */
public record KnowledgeBase(
        long id, String name, String description, Instant createdAt, Instant updatedAt) {}