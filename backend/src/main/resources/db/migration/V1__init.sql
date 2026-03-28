create table if not exists conversations (
  id bigint auto_increment primary key,
  title varchar(255) null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create table if not exists messages (
  id bigint auto_increment primary key,
  conversation_id bigint not null,
  role varchar(32) not null,
  content text not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint fk_messages_conversation_id foreign key (conversation_id) references conversations(id)
);
create index idx_messages_conversation_id_created_at on messages(conversation_id, created_at);

create table if not exists notifications (
  id bigint auto_increment primary key,
  ref_type varchar(64) null,
  ref_id varchar(128) null,
  title varchar(255) not null,
  content text null,
  status varchar(32) not null default 'unread',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);
create index idx_notifications_status_created_at on notifications(status, created_at);

create table if not exists confirmations (
  id bigint auto_increment primary key,
  requester varchar(32) not null,
  action varchar(128) not null,
  idempotency_key varchar(128) not null,
  payload_summary text null,
  status varchar(32) not null default 'pending',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint uk_confirmations_idempotency_key unique (idempotency_key)
);

create table if not exists execution_records (
  id bigint auto_increment primary key,
  initiator varchar(32) not null,
  ref_type varchar(64) null,
  ref_id varchar(128) null,
  input_summary text null,
  output_summary text null,
  duration_ms bigint null,
  result varchar(32) not null,
  failure_reason text null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);
create index idx_execution_records_ref on execution_records(ref_type, ref_id);

create table if not exists task_items (
  id bigint auto_increment primary key,
  title varchar(255) not null,
  description text null,
  status varchar(32) not null default 'todo',
  priority int not null default 0,
  due_at timestamp null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);
create index idx_task_items_status_due_at on task_items(status, due_at);

create table if not exists task_reminder_events (
  id bigint auto_increment primary key,
  task_item_id bigint not null,
  remind_at timestamp not null,
  fired_at timestamp null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint fk_task_reminder_events_task_item_id foreign key (task_item_id) references task_items(id)
);
create index idx_task_reminder_events_remind_at on task_reminder_events(remind_at);

create table if not exists knowledge_bases (
  id bigint auto_increment primary key,
  name varchar(255) not null,
  description text null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);
create unique index uk_knowledge_bases_name on knowledge_bases(name);

create table if not exists knowledge_documents (
  id bigint auto_increment primary key,
  knowledge_base_id bigint not null,
  source_type varchar(32) not null,
  source_uri varchar(1024) not null,
  title varchar(255) null,
  status varchar(32) not null default 'pending',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint fk_knowledge_documents_kb foreign key (knowledge_base_id) references knowledge_bases(id)
);
create index idx_knowledge_documents_kb_status on knowledge_documents(knowledge_base_id, status);

create table if not exists knowledge_chunks (
  id bigint auto_increment primary key,
  knowledge_document_id bigint not null,
  chunk_index int not null,
  content text not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint fk_knowledge_chunks_doc foreign key (knowledge_document_id) references knowledge_documents(id)
);
create unique index uk_knowledge_chunks_doc_index on knowledge_chunks(knowledge_document_id, chunk_index);

create table if not exists knowledge_import_jobs (
  id bigint auto_increment primary key,
  knowledge_base_id bigint not null,
  status varchar(32) not null default 'queued',
  started_at timestamp null,
  finished_at timestamp null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint fk_knowledge_import_jobs_kb foreign key (knowledge_base_id) references knowledge_bases(id)
);
create index idx_knowledge_import_jobs_kb_status on knowledge_import_jobs(knowledge_base_id, status);

create table if not exists parse_reports (
  id bigint auto_increment primary key,
  knowledge_document_id bigint not null,
  summary text null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint fk_parse_reports_doc foreign key (knowledge_document_id) references knowledge_documents(id)
);

create table if not exists memory_items (
  id bigint auto_increment primary key,
  content text not null,
  status varchar(32) not null default 'candidate',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

