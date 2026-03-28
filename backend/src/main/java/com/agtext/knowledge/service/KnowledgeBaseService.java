package com.agtext.knowledge.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.knowledge.domain.KnowledgeBase;
import com.agtext.knowledge.repository.KnowledgeBaseRepository;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识库业务服务类：协调知识库实体的生命周期管理。
 * 核心逻辑：
 * 1. 封装业务校验规则（如名称必填、重名检查）。
 * 2. 统一处理持久层异常并转换为业务异常。
 * 3. 维护数据库事务的一致性。
 */
@Service
public class KnowledgeBaseService {
  private final KnowledgeBaseRepository repo;

  public KnowledgeBaseService(KnowledgeBaseRepository repo) {
    this.repo = repo;
  }

  /**
   * 创建知识库：
   * 逻辑：
   * 1. 基础校验：确保名称非空。
   * 2. 冲突检查：捕获 DuplicateKeyException 以识别同名知识库。
   * 3. 数据回查：创建成功后立即从数据库重新加载完整对象（包含自增 ID 和时间戳）。
   * @throws IllegalArgumentException 当名称为空或名称已存在时抛出
   */
  @Transactional
  public KnowledgeBase create(String name, String description) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    long id;
    try {
      // 执行插入，同时去除名称首尾空格
      id = repo.create(name.trim(), description);
    } catch (DuplicateKeyException e) {
      // 映射数据库唯一索引冲突为业务异常
      throw new IllegalArgumentException("knowledge base name already exists");
    }
    // 确保数据实时性，返回持久化后的完整实体
    return repo.findById(id)
            .orElseThrow(() -> new IllegalStateException("Knowledge base not found after creation"));
  }

  /**
   * 获取指定知识库详情：
   * @param id 数据库内部长整型 ID
   * @throws NotFoundException 当 ID 不存在时抛出，用于触发 404 响应
   */
  @Transactional(readOnly = true)
  public KnowledgeBase get(long id) {
    return repo.findById(id)
            .orElseThrow(() -> new NotFoundException("KB_NOT_FOUND", "Knowledge base not found"));
  }

  /**
   * 分页获取知识库列表。
   */
  @Transactional(readOnly = true)
  public List<KnowledgeBase> list(int page, int pageSize) {
    return repo.list(page, pageSize);
  }

  /**
   * 统计知识库总数。
   */
  @Transactional(readOnly = true)
  public long countAll() {
    return repo.countAll();
  }
}