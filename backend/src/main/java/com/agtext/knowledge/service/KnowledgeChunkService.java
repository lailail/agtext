package com.agtext.knowledge.service;

import com.agtext.knowledge.domain.KnowledgeChunk;
import com.agtext.knowledge.repository.KnowledgeChunkRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识切片服务：提供对已处理文本片段的查询能力。
 * 核心逻辑：
 * 1. 作为 RAG 流程中“检索前预览”的数据入口。
 * 2. 严格区分文档的全量切片与特定导入任务（版本）产生的切片。
 */
@Service
public class KnowledgeChunkService {
  private final KnowledgeChunkRepository repo;

  public KnowledgeChunkService(KnowledgeChunkRepository repo) {
    this.repo = repo;
  }

  /**
   * 按文档 ID 分页查询切片。
   * 通常用于在后台管理系统中查看该文档关联的所有历史或当前切片。
   */
  @Transactional(readOnly = true)
  public List<KnowledgeChunk> listByDocumentId(long documentId, int page, int pageSize) {
    return repo.listByDocumentId(documentId, page, pageSize);
  }

  /**
   * 按文档 ID 及导入任务 ID 分页查询切片。
   * 业务意义：当文档处于重索引（Re-indexing）状态时，可以通过此方法精确定位某一次处理批次产生的切片，
   * 从而实现版本回溯或解析效果比对。
   */
  @Transactional(readOnly = true)
  public List<KnowledgeChunk> listByDocumentIdAndImportJobId(
          long documentId, long importJobId, int page, int pageSize) {
    return repo.listByDocumentIdAndImportJobId(documentId, importJobId, page, pageSize);
  }

  /**
   * 统计指定文档下的切片总数。
   */
  @Transactional(readOnly = true)
  public long countByDocumentId(long documentId) {
    return repo.countByDocumentId(documentId);
  }

  /**
   * 统计指定导入任务生成的切片总数。
   */
  @Transactional(readOnly = true)
  public long countByDocumentIdAndImportJobId(long documentId, long importJobId) {
    return repo.countByDocumentIdAndImportJobId(documentId, importJobId);
  }
}