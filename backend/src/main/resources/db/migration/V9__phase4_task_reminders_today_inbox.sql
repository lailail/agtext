alter table task_items add column remind_at timestamp null;
alter table task_items add column snooze_until timestamp null;
create index idx_task_items_remind_at_status on task_items(remind_at, status);

alter table task_reminder_events add column type varchar(32) not null default 'remind_at';
alter table task_reminder_events add column status varchar(32) not null default 'triggered';
create unique index uk_task_reminder_events_task_remind_type on task_reminder_events(task_item_id, remind_at, type);

