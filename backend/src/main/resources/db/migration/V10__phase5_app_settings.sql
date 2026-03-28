create table if not exists app_settings (
  id bigint auto_increment primary key,
  k varchar(128) not null,
  v text null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);
create unique index uk_app_settings_k on app_settings(k);

