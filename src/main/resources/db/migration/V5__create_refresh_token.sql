create table refresh_token (
  id uuid primary key,

  user_id uuid not null references users(id) on delete cascade,

  token_hash varchar(64) not null unique, -- SHA-256 hex
  created_at timestamptz not null default now(),
  expires_at timestamptz not null,

  revoked_at timestamptz null,
  replaced_by uuid null references refresh_token(id),

  user_agent text null,
  ip text null
);

create index ix_refresh_token_user on refresh_token(user_id);
create index ix_refresh_token_expires on refresh_token(expires_at);

-- optionnel mais bien : éviter des tokens "déjà expirés" à l'insert
alter table refresh_token
  add constraint ck_refresh_token_expires_after_created
  check (expires_at > created_at);
