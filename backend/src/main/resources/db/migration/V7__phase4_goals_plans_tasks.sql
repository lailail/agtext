create table if not exists goal_items (
  id bigint auto_increment primary key,
  title varchar(255) not null,
  description text null,
  status varchar(32) not null default 'active',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);
create index idx_goal_items_status on goal_items(status);

create table if not exists plan_items (
  id bigint auto_increment primary key,
  goal_id bigint null,
  title varchar(255) not null,
  description text null,
  status varchar(32) not null default 'active',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint fk_plan_items_goal_id foreign key (goal_id) references goal_items(id)
);
create index idx_plan_items_goal_id on plan_items(goal_id);
create index idx_plan_items_status on plan_items(status);

alter table task_items add column plan_id bigint null;
alter table task_items add column goal_id bigint null;
alter table task_items add column inbox boolean not null default false;

create index idx_task_items_plan_id on task_items(plan_id);
create index idx_task_items_goal_id on task_items(goal_id);
create index idx_task_items_inbox on task_items(inbox);

alter table task_items add constraint fk_task_items_plan_id foreign key (plan_id) references plan_items(id);
alter table task_items add constraint fk_task_items_goal_id foreign key (goal_id) references goal_items(id);

