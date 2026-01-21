create table tax_receipt (
  id uuid primary key,
  donation_id uuid not null references donation(id) on delete cascade,

  receipt_number bigserial not null unique,

  status varchar(20) not null,

  donor_full_name varchar(180) not null,
  donor_address text not null,
  email varchar(254) not null,

  requested_at timestamptz not null default now(),
  issued_at timestamptz null,

  pdf_path text null
);

create unique index ux_tax_receipt_donation on tax_receipt(donation_id);

create index ix_tax_receipt_status on tax_receipt(status);
create index ix_tax_receipt_requested_at on tax_receipt(requested_at);
