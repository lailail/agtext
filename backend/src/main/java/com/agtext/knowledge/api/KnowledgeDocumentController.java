package com.agtext.knowledge.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.knowledge.domain.KnowledgeDocument;
import com.agtext.knowledge.service.KnowledgeBaseService;
import com.agtext.knowledge.service.KnowledgeDocumentService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识文档控制器：负责知识库内具体文档的上传登记、列表查询及状态追踪
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeDocumentController {
  private static final String KB_PREFIX = "kb_";   // 知识库 ID 前缀
  private static final String DOC_PREFIX = "doc_"; // 文档 ID 前缀

  private final KnowledgeBaseService bases;
  private final KnowledgeDocumentService docs;

  public KnowledgeDocumentController(KnowledgeBaseService bases, KnowledgeDocumentService docs) {
    this.bases = bases;
    this.docs = docs;
  }

  /**
   * 在指定知识库中创建/登记新文档
   * @param baseId 外部知识库 ID (kb_...)
   * @param req 包含源类型（如 file/url）、源路径及标题的请求体
   */
  @PostMapping("/bases/{baseId}/documents")
  public KnowledgeDocumentItem create(
          @PathVariable("baseId") String baseId, @RequestBody CreateKnowledgeDocumentRequest req) {
    // 1. 解码并校验知识库是否存在
    long kbId = IdCodec.decode(KB_PREFIX, baseId);
    bases.get(kbId); // 若不存在会抛出 NotFoundException

    // 2. 调用服务层创建文档记录
    KnowledgeDocument doc = docs.create(kbId, req.sourceType(), req.sourceUri(), req.title());
    return toItem(doc);
  }

  /**
   * 分页获取指定知识库下的文档列表
   */
  @GetMapping("/bases/{baseId}/documents")
  public PageResponse<KnowledgeDocumentItem> listByBase(
          @PathVariable("baseId") String baseId,
          @RequestParam(name = "page", defaultValue = "1") int page,
          @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    // 1. 解码知识库 ID
    long kbId = IdCodec.decode(KB_PREFIX, baseId);
    bases.get(kbId);

    // 2. 执行分页查询并转换 DTO
    List<KnowledgeDocumentItem> items =
            docs.listByBaseId(kbId, page, pageSize).stream().map(this::toItem).toList();

    // 3. 封装标准分页响应
    return new PageResponse<>(items, page, pageSize, docs.countByBaseId(kbId));
  }

  /**
   * 获取单个文档的详细信息（包含解析与索引状态）
   * @param id 外部文档 ID (doc_...)
   */
  @GetMapping("/documents/{id}")
  public KnowledgeDocumentItem get(@PathVariable("id") String id) {
    long raw = IdCodec.decode(DOC_PREFIX, id);
    return toItem(docs.get(raw));
  }

  /**
   * 领域模型向 DTO 的转换逻辑
   * 重点处理：多重状态标识（解析、索引）及最新导入任务 ID 的编码
   */
  private KnowledgeDocumentItem toItem(KnowledgeDocument doc) {
    return new KnowledgeDocumentItem(
            IdCodec.encode(DOC_PREFIX, doc.id()),
            IdCodec.encode(KB_PREFIX, doc.knowledgeBaseId()),
            doc.sourceType(),
            doc.sourceUri(),
            doc.title(),
            doc.status(),        // 文档总体业务状态
            doc.parseStatus(),   // 文本解析状态（待处理/解析中/已完成/失败）
            doc.indexStatus(),   // 向量索引状态（待处理/同步中/已同步）
            doc.errorMessage(),  // 失败时的错误详情
            doc.latestImportJobId() == null ? null : IdCodec.encode("job_", doc.latestImportJobId()),
            doc.createdAt(),
            doc.updatedAt());
  }

  /**
   * 创建文档请求载体
   */
  public record CreateKnowledgeDocumentRequest(String sourceType, String sourceUri, String title) {}

  /**
   * 文档信息视图模型
   * 包含了 RAG 流程中关键的状态追踪字段
   */
  public record KnowledgeDocumentItem(
          String id,
          String knowledgeBaseId,
          String sourceType,
          String sourceUri,
          String title,
          String status,       // 业务可用状态
          String parseStatus,  // 解析环节状态
          String indexStatus,  // 向量化环节状态
          String errorMessage, // 异常追溯信息
          String latestImportJobId, // 关联的异步处理任务 ID
          Instant createdAt,
          Instant updatedAt) {}
}