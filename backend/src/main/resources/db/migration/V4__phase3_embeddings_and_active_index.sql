alter table knowledge_documents add column active_import_job_id bigint null;
create index idx_knowledge_documents_active_import_job_id on knowledge_documents(active_import_job_id);

alter table knowledge_chunks add column chunk_hash varchar(64) null;
create index idx_knowledge_chunks_doc_hash on knowledge_chunks(knowledge_document_id, chunk_hash);

create table if not exists knowledge_chunk_embeddings (
  id bigint auto_increment primary key,
  knowledge_chunk_id bigint not null,
  import_job_id bigint not null,
  provider varchar(64) not null,
  model varchar(128) not null,
  dim int null,
  vector_json longtext not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint fk_kce_chunk foreign key (knowledge_chunk_id) references knowledge_chunks(id),
  constraint fk_kce_job foreign key (import_job_id) references knowledge_import_jobs(id)
);
create unique index uk_kce_chunk_job_model on knowledge_chunk_embeddings(knowledge_chunk_id, import_job_id, provider, model);
create index idx_kce_job on knowledge_chunk_embeddings(import_job_id);

