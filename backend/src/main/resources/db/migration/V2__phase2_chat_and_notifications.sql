alter table conversations add column status varchar(32) not null default 'active';

alter table messages add column provider varchar(64) null;
alter table messages add column model_name varchar(128) null;
alter table messages add column tokens int null;

alter table notifications add column type varchar(64) null;

alter table confirmations add column actor varchar(32) null;
alter table confirmations add column action_type varchar(128) null;
alter table confirmations add column ref_type varchar(64) null;
alter table confirmations add column ref_id varchar(128) null;
alter table confirmations add column summary text null;
alter table confirmations add column payload text null;

alter table execution_records add column actor varchar(32) null;
alter table execution_records add column source varchar(32) null;
alter table execution_records add column action_type varchar(128) null;
alter table execution_records add column idempotency_key varchar(128) null;
alter table execution_records add column status varchar(32) null;
alter table execution_records add column error_code varchar(64) null;
