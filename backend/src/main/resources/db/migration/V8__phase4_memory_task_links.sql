alter table memory_items add column related_goal_id bigint null;
alter table memory_items add column related_plan_id bigint null;
alter table memory_items add column related_task_id bigint null;

create index idx_memory_items_related_goal_id on memory_items(related_goal_id);
create index idx_memory_items_related_plan_id on memory_items(related_plan_id);
create index idx_memory_items_related_task_id on memory_items(related_task_id);

alter table memory_items add constraint fk_memory_items_related_goal_id foreign key (related_goal_id) references goal_items(id);
alter table memory_items add constraint fk_memory_items_related_plan_id foreign key (related_plan_id) references plan_items(id);
alter table memory_items add constraint fk_memory_items_related_task_id foreign key (related_task_id) references task_items(id);

