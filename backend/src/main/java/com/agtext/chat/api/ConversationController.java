package com.agtext.chat.api;

import com.agtext.chat.domain.Conversation;
import com.agtext.chat.service.ConversationService;
import com.agtext.common.api.PageResponse;
import com.agtext.common.ids.IdCodec;
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
 * 会话管理控制器。
 * 提供对话列表、会话详情以及会话创建的持久化维护接口。
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
  // 定义会话资源唯一的混淆前缀，确保 ID 解码的类型安全性
  private static final String PREFIX = "cnv_";
  private final ConversationService service;

  public ConversationController(ConversationService service) {
    this.service = service;
  }

  /**
   * 创建新的对话会话。
   * @param req 包含可选的会话标题
   * @return 转换后的会话简报（包含加密 ID）
   */
  @PostMapping
  public ConversationItem create(@RequestBody CreateConversationRequest req) {
    Conversation c = service.create(req == null ? null : req.title());
    return toItem(c);
  }

  /**
   * 分页获取当前用户的对话列表。
   * @param page     页码，从 1 开始
   * @param pageSize 每页条数
   * @return 包含分页元数据和会话简报列表的响应体
   */
  @GetMapping
  public PageResponse<ConversationItem> list(
          @RequestParam(name = "page", defaultValue = "1") int page,
          @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    // 将 Domain 层的实体列表映射为 API 层的 Item 对象，避免泄露数据库内部结构
    List<ConversationItem> items = service.list(page, pageSize).stream().map(this::toItem).toList();
    // 结合总记录数构造分页响应
    return new PageResponse<>(items, page, pageSize, service.countAll());
  }

  /**
   * 根据外部加密 ID 获取特定会话详情。
   * @param id 外部传递的 "cnv_" 前缀混淆 ID
   */
  @GetMapping("/{id}")
  public ConversationItem get(@PathVariable("id") String id) {
    // 强制执行前缀校验并反序列化为原始 Long ID
    long raw = IdCodec.decode(PREFIX, id);
    return toItem(service.get(raw));
  }

  /**
   * 内部转换方法：将领域实体（Domain Entity）转换为数据传输对象（DTO）。
   * 职责：统一对 ID 进行加密处理，并选择性暴露更新时间等字段。
   */
  private ConversationItem toItem(Conversation c) {
    return new ConversationItem(
            IdCodec.encode(PREFIX, c.id()),
            c.title(),
            c.status(),
            c.createdAt(),
            c.updatedAt());
  }

  /**
   * 创建请求载体
   */
  public record CreateConversationRequest(String title) {}

  /**
   * 会话列表项载体
   * @param id        混淆后的字符串 ID
   * @param status    会话状态（如：active, archived）
   * @param createdAt 创建时间戳
   */
  public record ConversationItem(
          String id, String title, String status, Instant createdAt, Instant updatedAt) {}
}