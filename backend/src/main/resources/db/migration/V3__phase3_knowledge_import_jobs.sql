alter table knowledge_documents add column parse_status varchar(32) null;
alter table knowledge_documents add column index_status varchar(32) null;
alter table knowledge_documents add column error_message text null;
alter table knowledge_documents add column content_hash varchar(64) null;
alter table knowledge_documents add column latest_import_job_id bigint null;

alter table knowledge_import_jobs add column document_id bigint null;
alter table knowledge_import_jobs add column stage varchar(32) null;
alter table knowledge_import_jobs add column progress int null;
alter table knowledge_import_jobs add column error_message text null;
alter table knowledge_import_jobs add column cancelled_at timestamp null;

create index idx_knowledge_import_jobs_document_id on knowledge_import_jobs(document_id);

alter table parse_reports add column job_id bigint null;
alter table parse_reports add column extracted_chars bigint null;
alter table parse_reports add column page_count int null;
alter table parse_reports add column chunk_count int null;
alter table parse_reports add column parser_name varchar(64) null;
alter table parse_reports add column failed_at varchar(32) null;
alter table parse_reports add column sample_preview text null;

create index idx_parse_reports_job_id on parse_reports(job_id);

