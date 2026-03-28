package com.agtext.knowledge.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.knowledge.domain.KnowledgeDocument;
import com.agtext.knowledge.repository.KnowledgeDocumentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识文档服务类：管理知识库内原始文档的元数据。
 * 该服务是文档处理生命周期的起点，负责文档的初步登记，而不涉及具体的异步解析逻辑。
 */
@Service
public class KnowledgeDocumentService {
  private final KnowledgeDocumentRepository repo;

  public KnowledgeDocumentService(KnowledgeDocumentRepository repo) {
    this.repo = repo;
  }

  /**
   * 登记新文档：
   * 在进入异步处理流水线前，首先在数据库中建立文档记录。
   * @param knowledgeBaseId 所属知识库 ID
   * @param sourceType 来源类型（如 file, url, markdown）
   * @param sourceUri 资源定位符（路径或链接）
   * @param title 文档标题
   * @throws IllegalArgumentException 当来源类型或路径为空时抛出
   */
  @Transactional
  public KnowledgeDocument create(
          long knowledgeBaseId, String sourceType, String sourceUri, String title) {
    // 1. 业务准入校验：必须明确文档来源及其定位信息
    if (sourceType == null || sourceType.isBlank()) {
      throw new IllegalArgumentException("sourceType is required");
    }
    if (sourceUri == null || sourceUri.isBlank()) {
      throw new IllegalArgumentException("sourceUri is required");
    }

    // 2. 持久化记录：初始状态由 Repository 层默认设为 'pending'
    long id = repo.create(knowledgeBaseId, sourceType.trim(), sourceUri.trim(), title);

    // 3. 完整性回查：确保返回包含自增 ID 和默认状态的实体对象
    return repo.findById(id)
            .orElseThrow(() -> new IllegalStateException("Knowledge document not found after creation"));
  }

  /**
   * 获取文档详情：
   * @throws NotFoundException 若 ID 不存在，抛出此异常以便全局处理器返回 404 状态
   */
  @Transactional(readOnly = true)
  public KnowledgeDocument get(long id) {
    return repo.findById(id)
            .orElseThrow(() -> new NotFoundException("DOC_NOT_FOUND", "Knowledge document not found"));
  }

  /**
   * 分页获取指定知识库下的文档列表。
   */
  @Transactional(readOnly = true)
  public List<KnowledgeDocument> listByBaseId(long baseId, int page, int pageSize) {
    return repo.listByBaseId(baseId, page, pageSize);
  }

  /**
   * 统计指定知识库下的文档总数。
   */
  @Transactional(readOnly = true)
  public long countByBaseId(long baseId) {
    return repo.countByBaseId(baseId);
  }
}