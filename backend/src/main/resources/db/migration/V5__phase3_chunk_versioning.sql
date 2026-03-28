alter table knowledge_chunks add column import_job_id bigint null;

update knowledge_chunks
set import_job_id =
    (select coalesce(latest_import_job_id, active_import_job_id) from knowledge_documents where id = knowledge_document_id)
where import_job_id is null;

drop index uk_knowledge_chunks_doc_index on knowledge_chunks;
create unique index uk_knowledge_chunks_doc_job_index on knowledge_chunks(knowledge_document_id, import_job_id, chunk_index);

