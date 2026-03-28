package com.agtext.knowledge.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.knowledge.domain.KnowledgeDocument;
import com.agtext.knowledge.domain.KnowledgeImportJob;
import com.agtext.knowledge.repository.KnowledgeImportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识重索引服务：
 * 当向量模型更新、索引配置变更或索引丢失时，用于重新触发索引构建流程。
 * 该服务复用已有的文档切片（Chunks），避免了昂贵的 PDF 解析或网页抓取开销。
 */
@Service
public class KnowledgeReindexService {
  private final KnowledgeDocumentService documents;
  private final KnowledgeImportJobRepository jobs;
  private final KnowledgeEmbeddingIndexService indexer;

  public KnowledgeReindexService(
          KnowledgeDocumentService documents,
          KnowledgeImportJobRepository jobs,
          KnowledgeEmbeddingIndexService indexer) {
    this.documents = documents;
    this.jobs = jobs;
    this.indexer = indexer;
  }

  /**
   * 执行重索引逻辑
   * @param documentId 目标文档ID
   * @return 新生成的导入任务对象
   */
  @Transactional
  public KnowledgeImportJob reindex(long documentId) {
    // 1. 获取文档元数据
    KnowledgeDocument doc = documents.get(documentId);

    // 2. 确定数据源任务 ID
    // 优先使用最近一次导入任务（latest），如果没有则回退到当前活跃任务（active）
    // 这是为了找到该文档已经完成切片并存储在数据库中的 Chunk 数据源
    Long sourceChunkJobId =
            doc.latestImportJobId() != null ? doc.latestImportJobId() : doc.activeImportJobId();

    // 3. 防御性检查：如果文档从未被成功解析过（没有 Job ID），则无法重索引
    if (sourceChunkJobId == null) {
      throw new NotFoundException("DOC_NO_CONTENT", "Document has no imported content to reindex");
    }

    // 4. 创建一个新的导入任务记录，用于追踪本次重索引的进度和状态
    long jobId = jobs.create(doc.knowledgeBaseId(), doc.id());

    try {
      // 5. 调用索引引擎重新构建向量索引
      // 注意：此处传入了 sourceChunkJobId，意味着 indexer 将直接读取该 Job 对应的旧切片
      indexer.buildIndex(jobId, doc.id(), sourceChunkJobId);
    } catch (RuntimeException e) {
      // 6. 异常处理：记录失败阶段及错误信息
      String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      jobs.markFailed(jobId, "index", msg);
    }

    // 7. 返回最新的任务记录
    return jobs.findById(jobId).orElseThrow(() -> new IllegalStateException("Job not found"));
  }
}