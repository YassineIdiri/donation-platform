create table users (
  id uuid primary key,

  email varchar(254) not null,
  password_hash varchar(255) not null,

  -- utile même si tu n'as qu'un admin aujourd'hui (ça te laisse une porte propre)
  is_admin boolean not null default false,

  created_at timestamptz not null default now()
);

-- email unique en mode "case-insensitive"
create unique index ux_users_email_lower on users (lower(email));
create index ix_users_created_at on users(created_at);
