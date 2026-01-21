create table donation (
  id uuid primary key,
  amount_cents integer not null,
  currency varchar(3) not null,
  status varchar(20) not null,
  provider varchar(20) not null,
  payment_method varchar(20) not null,
  email varchar(320),
  stripe_checkout_session_id varchar(255),
  stripe_payment_intent_id varchar(255),
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create index idx_donation_created_at on donation(created_at);
create index idx_donation_status on donation(status);
create index idx_donation_stripe_session on donation(stripe_checkout_session_id);
