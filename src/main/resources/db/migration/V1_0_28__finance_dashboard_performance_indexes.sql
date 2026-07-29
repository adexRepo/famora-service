CREATE INDEX IF NOT EXISTS idx_finance_transactions_family_status_currency_date
    ON famora.finance_transactions (family_id, status, currency, transaction_date, created_at);

CREATE INDEX IF NOT EXISTS idx_finance_transactions_family_status_date_currency_type
    ON famora.finance_transactions (family_id, status, transaction_date, currency, type);

CREATE INDEX IF NOT EXISTS idx_finance_debts_family_status_type_debt_status_currency
    ON famora.finance_debts (family_id, status, debt_type, debt_status, currency);
