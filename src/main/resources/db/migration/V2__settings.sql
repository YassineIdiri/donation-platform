create table app_settings (
  id bigserial primary key,
  settings_key varchar(50) not null unique,
  settings_json jsonb not null,
  updated_at timestamptz not null default now()
);

insert into app_settings(settings_key, settings_json)
values (
  'PUBLIC_UI',
  '{
    "title": "Association Solidaire",
    "primaryColor": "#0ea5e9",
    "suggestedAmounts": [10, 20, 50, 100]
  }'::jsonb
);
