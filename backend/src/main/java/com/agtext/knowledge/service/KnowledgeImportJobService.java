package com.agtext.knowledge.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.knowledge.domain.KnowledgeImportJob;
import com.agtext.knowledge.domain.ParseReport;
import com.agtext.knowledge.repository.KnowledgeImportJobRepository;
import com.agtext.knowledge.repository.ParseReportRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识导入任务服务类：负责追踪异步处理流水线的状态及解析报告。
 * 核心逻辑：
 * 1. 监控中心：提供多维度的任务列表查询与状态统计。
 * 2. 质量审计：提供文档解析后的量化报告查询，用于核对解析精度（如字符数、分片数）。
 */
@Service
public class KnowledgeImportJobService {
  private final KnowledgeImportJobRepository jobs;
  private final ParseReportRepository reports;

  public KnowledgeImportJobService(
          KnowledgeImportJobRepository jobs, ParseReportRepository reports) {
    this.jobs = jobs;
    this.reports = reports;
  }

  /**
   * 分页查询导入任务列表。
   * 支持按知识库 ID、文档 ID 或任务状态（queued, running, succeeded, failed）进行组合过滤。
   */
  @Transactional(readOnly = true)
  public List<KnowledgeImportJob> list(
          Long knowledgeBaseId, Long documentId, String status, int page, int pageSize) {
    return jobs.list(knowledgeBaseId, documentId, status, page, pageSize);
  }

  /**
   * 统计符合过滤条件的任务总数。
   * 用于前端分页组件计算总页数及展示进度概览。
   */
  @Transactional(readOnly = true)
  public long count(Long knowledgeBaseId, Long documentId, String status) {
    return jobs.count(knowledgeBaseId, documentId, status);
  }

  /**
   * 获取特定任务的当前详情。
   * @throws NotFoundException 当任务 ID 不存在时，抛出业务异常。
   */
  @Transactional(readOnly = true)
  public KnowledgeImportJob get(long id) {
    return jobs.findById(id)
            .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Import job not found"));
  }

  /**
   * 获取指定任务生成的最新解析报告。
   * 业务要点：解析报告包含了从非结构化数据（如 PDF）中提取的元数据指标，是 RAG 系统性能调优的重要参考。
   * @throws NotFoundException 若任务尚未产生报告（如尚在排队中或抓取阶段），抛出此异常。
   */
  @Transactional(readOnly = true)
  public ParseReport getLatestReport(long jobId) {
    return reports
            .findLatestByJobId(jobId)
            .orElseThrow(
                    () -> new NotFoundException("PARSE_REPORT_NOT_FOUND", "Parse report not found"));
  }
}