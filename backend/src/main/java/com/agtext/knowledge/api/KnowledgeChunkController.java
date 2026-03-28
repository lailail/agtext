package com.agtext.knowledge.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.knowledge.domain.KnowledgeChunk;
import com.agtext.knowledge.service.KnowledgeChunkService;
import com.agtext.knowledge.service.KnowledgeDocumentService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识分片控制器：负责查看已解析并切分后的知识文档片段。
 * 核心逻辑：
 * 1. 支持按文档 ID（Document）检索分片。
 * 2. 支持按导入任务 ID（Import Job）检索，用于查看特定解析版本的切分结果。
 */
@RestController
@RequestMapping("/api/knowledge/documents/{documentId}/chunks")
public class KnowledgeChunkController {
  private static final String DOC_PREFIX = "doc_"; // 文档 ID 前缀
  private static final String CHK_PREFIX = "chk_"; // 分片 ID 前缀
  private final KnowledgeDocumentService docs;
  private final KnowledgeChunkService chunks;

  public KnowledgeChunkController(KnowledgeDocumentService docs, KnowledgeChunkService chunks) {
    this.docs = docs;
    this.chunks = chunks;
  }

  /**
   * 分页获取文档的分片列表：
   * @param documentId 外部文档 ID
   * @param importJobId 可选：指定导入任务 ID。若不传，则默认尝试使用文档最新的导入任务。
   * @param page 当前页码
   * @param pageSize 每页条数（默认 50）
   */
  @GetMapping
  public PageResponse<KnowledgeChunkItem> list(
          @PathVariable("documentId") String documentId,
          @RequestParam(name = "job_id", required = false) String importJobId,
          @RequestParam(name = "page", defaultValue = "1") int page,
          @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {

    // 1. 解码文档 ID
    long docId = IdCodec.decode(DOC_PREFIX, documentId);
    var doc = docs.get(docId);

    // 2. 确定解析任务 ID (Import Job ID)
    // 逻辑：优先使用请求参数中的 job_id；若无，则回退到文档记录的最新一次成功导入的 job_id
    Long rawJobId = null;
    if (importJobId != null && !importJobId.isBlank()) {
      rawJobId = IdCodec.decode("job_", importJobId);
    } else if (doc.latestImportJobId() != null) {
      rawJobId = doc.latestImportJobId();
    }

    List<KnowledgeChunkItem> items;
    long total;

    // 3. 执行查询逻辑
    // 如果存在任务 ID，则查询该任务生成的特定分片版本（防止查询到旧版本或正在解析中的脏数据）
    if (rawJobId != null) {
      items =
              chunks.listByDocumentIdAndImportJobId(docId, rawJobId, page, pageSize).stream()
                      .map(this::toItem)
                      .toList();
      total = chunks.countByDocumentIdAndImportJobId(docId, rawJobId);
    } else {
      // 兜底策略：若无任务 ID 记录，则查询该文档关联的所有分片
      items = chunks.listByDocumentId(docId, page, pageSize).stream().map(this::toItem).toList();
      total = chunks.countByDocumentId(docId);
    }

    return new PageResponse<>(items, page, pageSize, total);
  }

  /**
   * 实体转换逻辑：将领域模型 KnowledgeChunk 转换为带编码 ID 的 DTO。
   */
  private KnowledgeChunkItem toItem(KnowledgeChunk c) {
    return new KnowledgeChunkItem(
            IdCodec.encode(CHK_PREFIX, c.id()),
            IdCodec.encode(DOC_PREFIX, c.knowledgeDocumentId()),
            c.importJobId() == null ? null : IdCodec.encode("job_", c.importJobId()),
            c.chunkIndex(), // 原始文档中的切片顺序索引
            c.content(),    // 分片的文本内容
            c.createdAt(),
            c.updatedAt());
  }

  /**
   * 知识分片视图模型
   * @param chunkIndex 切片在原文档中的位置索引，用于按序重组文本
   */
  public record KnowledgeChunkItem(
          String id,
          String knowledgeDocumentId,
          String importJobId,
          int chunkIndex,
          String content,
          Instant createdAt,
          Instant updatedAt) {}
}