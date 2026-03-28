package com.agtext.knowledge.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.knowledge.domain.KnowledgeImportJob;
import com.agtext.knowledge.domain.ParseReport;
import com.agtext.knowledge.repository.KnowledgeImportJobRepository;
import com.agtext.knowledge.repository.ParseReportRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeImportJobService {
  private final KnowledgeImportJobRepository jobs;
  private final ParseReportRepository reports;

  public KnowledgeImportJobService(
      KnowledgeImportJobRepository jobs, ParseReportRepository reports) {
    this.jobs = jobs;
    this.reports = reports;
  }

  @Transactional(readOnly = true)
  public List<KnowledgeImportJob> list(
      Long knowledgeBaseId, Long documentId, String status, int page, int pageSize) {
    return jobs.list(knowledgeBaseId, documentId, status, page, pageSize);
  }

  @Transactional(readOnly = true)
  public long count(Long knowledgeBaseId, Long documentId, String status) {
    return jobs.count(knowledgeBaseId, documentId, status);
  }

  @Transactional(readOnly = true)
  public KnowledgeImportJob get(long id) {
    return jobs.findById(id)
        .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Import job not found"));
  }

  @Transactional(readOnly = true)
  public ParseReport getLatestReport(long jobId) {
    return reports
        .findLatestByJobId(jobId)
        .orElseThrow(
            () -> new NotFoundException("PARSE_REPORT_NOT_FOUND", "Parse report not found"));
  }
}
