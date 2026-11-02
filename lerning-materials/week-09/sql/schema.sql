-- Учебные временные таблицы. Запускать внутри BEGIN; см. laboratory.sql.
CREATE TEMP TABLE client (
    id uuid PRIMARY KEY,
    external_id text NOT NULL UNIQUE,
    name text NOT NULL,
    status text NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED')),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
) ON COMMIT DROP;

CREATE TEMP TABLE account (
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
) ON COMMIT DROP;

CREATE TEMP TABLE transfer (
    id uuid PRIMARY KEY,
    source_account_id uuid NOT NULL REFERENCES account (id) ON DELETE RESTRICT,
    target_account_id uuid NOT NULL REFERENCES account (id) ON DELETE RESTRICT,
    amount numeric(19, 2) NOT NULL,
    currency text NOT NULL CHECK (currency = 'RUB'),
    status text NOT NULL CHECK (status = 'COMPLETED'),
    description text,
    created_at timestamptz NOT NULL,
    completed_at timestamptz NOT NULL,
    CONSTRAINT transfer_amount_valid CHECK (amount > 0 AND amount <> 'NaN'::numeric),
    CONSTRAINT transfer_different_accounts CHECK (source_account_id <> target_account_id),
    CONSTRAINT transfer_time_order CHECK (completed_at >= created_at)
) ON COMMIT DROP;

CREATE TEMP TABLE ledger_entry (
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
) ON COMMIT DROP;
