CREATE TABLE IF NOT EXISTS famora.finance_debts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id uuid NOT NULL REFERENCES famora.families(id),
    created_by uuid NOT NULL REFERENCES famora.users(id),
    updated_by uuid REFERENCES famora.users(id),
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    debt_type varchar(30) NOT NULL,
    debt_status varchar(30) NOT NULL,
    counterparty_name varchar(180) NOT NULL,
    principal_amount numeric(19, 2) NOT NULL,
    paid_amount numeric(19, 2) NOT NULL DEFAULT 0,
    remaining_amount numeric(19, 2) NOT NULL,
    currency varchar(3) NOT NULL,
    borrowed_date date NOT NULL,
    due_date date,
    notes text,
    attachment_file_id uuid REFERENCES famora.files(id),
    principal_finance_transaction_id uuid REFERENCES famora.finance_transactions(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_finance_debts_type
        CHECK (debt_type IN ('RECEIVABLE', 'PAYABLE')),
    CONSTRAINT chk_finance_debts_debt_status
        CHECK (debt_status IN ('OPEN', 'PARTIAL', 'PAID', 'CANCELLED')),
    CONSTRAINT chk_finance_debts_amounts
        CHECK (principal_amount > 0 AND paid_amount >= 0 AND remaining_amount >= 0)
);

CREATE TABLE IF NOT EXISTS famora.finance_debt_payments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id uuid NOT NULL REFERENCES famora.families(id),
    created_by uuid NOT NULL REFERENCES famora.users(id),
    updated_by uuid REFERENCES famora.users(id),
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    debt_id uuid NOT NULL REFERENCES famora.finance_debts(id),
    amount numeric(19, 2) NOT NULL,
    payment_date date NOT NULL,
    notes text,
    attachment_file_id uuid REFERENCES famora.files(id),
    finance_transaction_id uuid REFERENCES famora.finance_transactions(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_finance_debt_payments_amount
        CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_finance_debts_family_status_type
ON famora.finance_debts (family_id, status, debt_type);

CREATE INDEX IF NOT EXISTS idx_finance_debts_family_status_debt_status
ON famora.finance_debts (family_id, status, debt_status);

CREATE INDEX IF NOT EXISTS idx_finance_debts_family_due_date
ON famora.finance_debts (family_id, due_date);

CREATE INDEX IF NOT EXISTS idx_finance_debt_payments_debt_status_date
ON famora.finance_debt_payments (debt_id, status, payment_date);
