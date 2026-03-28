package com.agtext.knowledge.api;

import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
import com.agtext.knowledge.domain.KnowledgeBase;
import com.agtext.knowledge.service.KnowledgeBaseService;
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
 * 知识库管理接口：负责知识库（Knowledge Base）的增删改查路由。
 * 核心逻辑：
 * 1. 屏蔽内部 Long 类型 ID，通过 IdCodec 与前端交互带前缀的字符串 ID。
 * 2. 实现领域模型（KnowledgeBase）到视图对象（KnowledgeBaseItem）的转换。
 */
@RestController
@RequestMapping("/api/knowledge/bases")
public class KnowledgeBaseController {
  private static final String PREFIX = "kb_"; // 知识库 ID 的统一外部前缀
  private final KnowledgeBaseService service;

  public KnowledgeBaseController(KnowledgeBaseService service) {
    this.service = service;
  }

  /**
   * 创建知识库：
   * 接收 JSON 格式的创建请求，调用 Service 层逻辑并返回转换后的视图对象。
   */
  @PostMapping
  public KnowledgeBaseItem create(@RequestBody CreateKnowledgeBaseRequest req) {
    KnowledgeBase kb = service.create(req.name(), req.description());
    return toItem(kb);
  }

  /**
   * 分页查询知识库列表：
   * @param page 当前页码（从 1 开始，默认 1）
   * @param pageSize 每页条数（默认 20）
   * @return 包含分页元数据和 KnowledgeBaseItem 列表的标准响应
   */
  @GetMapping
  public PageResponse<KnowledgeBaseItem> list(
          @RequestParam(name = "page", defaultValue = "1") int page,
          @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    // 1. 调用 Service 分页查询领域实体
    List<KnowledgeBaseItem> items =
            service.list(page, pageSize).stream().map(this::toItem).toList();

    // 2. 组装标准分页响应，包含总记录数统计
    return new PageResponse<>(items, page, pageSize, service.countAll());
  }

  /**
   * 获取知识库详情：
   * @param id 外部字符串 ID（如 "kb_123"）
   */
  @GetMapping("/{id}")
  public KnowledgeBaseItem get(@PathVariable("id") String id) {
    // 1. 将外部带前缀的 ID 解码为数据库内部的长整型 ID
    long raw = IdCodec.decode(PREFIX, id);
    // 2. 查询并转换为 DTO 返回
    return toItem(service.get(raw));
  }

  /**
   * 视图对象转换逻辑：
   * 负责将持久化层返回的 KnowledgeBase 实体映射为面向 API 的 KnowledgeBaseItem Record。
   * 关键步骤：在此处调用 IdCodec.encode 为 ID 增加业务前缀。
   */
  private KnowledgeBaseItem toItem(KnowledgeBase kb) {
    return new KnowledgeBaseItem(
            IdCodec.encode(PREFIX, kb.id()),
            kb.name(),
            kb.description(),
            kb.createdAt(),
            kb.updatedAt());
  }

  /**
   * 创建知识库的请求载体
   */
  public record CreateKnowledgeBaseRequest(String name, String description) {}

  /**
   * 知识库对外展示的视图模型
   */
  public record KnowledgeBaseItem(
          String id, String name, String description, Instant createdAt, Instant updatedAt) {}
}