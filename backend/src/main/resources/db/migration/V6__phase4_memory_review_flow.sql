alter table memory_items add column title varchar(255) null;
alter table memory_items add column source_type varchar(32) null;
alter table memory_items add column source_conversation_id bigint null;
alter table memory_items add column source_message_id bigint null;
alter table memory_items add column candidate_reason text null;
alter table memory_items add column reviewer_note text null;
alter table memory_items add column reviewed_at timestamp null;
alter table memory_items add column approved_at timestamp null;
alter table memory_items add column disabled_at timestamp null;

create index idx_memory_items_status on memory_items(status);
create index idx_memory_items_source_conv on memory_items(source_conversation_id);

