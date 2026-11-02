CREATE TABLE client (
    id uuid PRIMARY KEY,
    external_id text NOT NULL UNIQUE,
    name text NOT NULL,
    status text NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED')),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE account (
    id uuid PRIMARY KEY,
    client_id uuid NOT NULL REFERENCES client (id) ON DELETE RESTRICT,
    currency text NOT NULL CHECK (currency = 'RUB'),
    balance numeric(19, 2) NOT NULL DEFAULT 0,
    status text NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT account_balance_valid CHECK (balance >= 0 AND balance <> 'NaN'::numeric),
    CONSTRAINT account_closed_zero CHECK (status <> 'CLOSED' OR balance = 0)
);

CREATE TABLE transfer (
    id uuid PRIMARY KEY,
    source_account_id uuid NOT NULL REFERENCES account (id) ON DELETE RESTRICT,
    target_account_id uuid NOT NULL REFERENCES account (id) ON DELETE RESTRICT,
    amount numeric(19, 2) NOT NULL,
    currency text NOT NULL CHECK (currency = 'RUB'),
    status text NOT NULL CHECK (status = 'COMPLETED'),
    created_at timestamptz NOT NULL,
    completed_at timestamptz NOT NULL,
    CONSTRAINT transfer_amount_valid CHECK (amount > 0 AND amount <> 'NaN'::numeric),
    CONSTRAINT transfer_different_accounts CHECK (source_account_id <> target_account_id),
    CONSTRAINT transfer_time_order CHECK (completed_at >= created_at)
);

CREATE TABLE ledger_entry (
    id uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES account (id) ON DELETE RESTRICT,
    operation_id uuid NOT NULL,
    operation_type text NOT NULL CHECK (
        operation_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT')
    ),
    amount numeric(19, 2) NOT NULL,
    balance_after numeric(19, 2) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT ledger_amount_sign CHECK (
        amount <> 'NaN'::numeric AND (
            (operation_type IN ('DEPOSIT', 'TRANSFER_IN') AND amount > 0)
            OR (operation_type IN ('WITHDRAWAL', 'TRANSFER_OUT') AND amount < 0)
        )
    ),
    CONSTRAINT ledger_balance_valid CHECK (balance_after >= 0 AND balance_after <> 'NaN'::numeric),
    CONSTRAINT ledger_operation_account_unique UNIQUE (operation_id, account_id)
);

CREATE TABLE idempotency_record (
  id uuid PRIMARY KEY,
  idempotency_key text NOT NULL,
  subject_id uuid NOT NULL,
  operation_type text NOT NULL CHECK (
      operation_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')
      ),
  request_hash text NOT NULL,
  response_status smallint NOT NULL,
  CONSTRAINT response_status_valid CHECK (response_status BETWEEN 100 AND 599),
  response_body jsonb NOT NULL,
  created_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,

  CONSTRAINT dates_valid CHECK (expires_at > created_at),
  CONSTRAINT compound_key_uniq UNIQUE (subject_id, operation_type, idempotency_key)
);

CREATE TABLE outbox_event (
    id uuid PRIMARY KEY,
    aggregate_type text NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type text NOT NULL CHECK (
        event_type IN ('MoneyDeposited', 'MoneyWithdrawn', 'TransferCompleted')
        ),
    payload jsonb NOT NULL,
    created_at timestamptz NOT NULL,
    published_at timestamptz,
    attempts smallint NOT NULL DEFAULT 0 CHECK (attempts >= 0),

    CONSTRAINT outbox_publication_time_valid CHECK (published_at IS NULL OR published_at >= created_at)
);

CREATE TABLE audit_log (
    id uuid PRIMARY KEY,
    subject_id uuid NOT NULL,
    subject_role text NOT NULL CHECK (subject_role IN ('CUSTOMER', 'OPERATOR')),
    action text NOT NULL,
    resource_type text NOT NULL,
    resource_id uuid NOT NULL,
    result text NOT NULL CHECK (result IN ('SUCCESS', 'FAILURE')),
    correlation_id uuid NOT NULL,
    failure_reason text,
    created_at timestamptz NOT NULL,

    CONSTRAINT audit_failure_reason_valid CHECK (
        (result = 'SUCCESS' AND failure_reason IS NULL)
        OR (result = 'FAILURE' AND failure_reason IS NOT NULL)
    )
);

