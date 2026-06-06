-- =========================================================
-- Convert PostgreSQL enum columns to VARCHAR for JPA compatibility
-- =========================================================

ALTER TABLE famora.users
    ALTER COLUMN status TYPE VARCHAR(30) USING status::text;

ALTER TABLE famora.families
    ALTER COLUMN status TYPE VARCHAR(30) USING status::text;

ALTER TABLE famora.family_members
    ALTER COLUMN role TYPE VARCHAR(30) USING role::text,
    ALTER COLUMN status TYPE VARCHAR(30) USING status::text;

ALTER TABLE famora.family_invitations
    ALTER COLUMN role TYPE VARCHAR(30) USING role::text,
    ALTER COLUMN status TYPE VARCHAR(30) USING status::text;

ALTER TABLE famora.finance_transactions
    ALTER COLUMN type TYPE VARCHAR(30) USING type::text;

ALTER TABLE famora.audit_logs
    ALTER COLUMN action TYPE VARCHAR(80) USING action::text;

-- =========================================================
-- Add check constraints to keep enum-like validation
-- =========================================================

ALTER TABLE famora.users
    ADD CONSTRAINT chk_users_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED'));

ALTER TABLE famora.families
    ADD CONSTRAINT chk_families_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'));

ALTER TABLE famora.family_members
    ADD CONSTRAINT chk_family_members_role
    CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'));

ALTER TABLE famora.family_members
    ADD CONSTRAINT chk_family_members_status
    CHECK (status IN ('ACTIVE', 'PENDING', 'REMOVED', 'LEFT'));

ALTER TABLE famora.family_invitations
    ADD CONSTRAINT chk_family_invitations_role
    CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'));

ALTER TABLE famora.family_invitations
    ADD CONSTRAINT chk_family_invitations_status
    CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'REVOKED'));

ALTER TABLE famora.finance_transactions
    ADD CONSTRAINT chk_finance_transactions_type
    CHECK (type IN ('INCOME', 'EXPENSE'));

ALTER TABLE famora.audit_logs
    ADD CONSTRAINT chk_audit_logs_action
    CHECK (
        action IN (
            'USER_REGISTERED',
            'USER_LOGGED_IN',
            'USER_LOGGED_OUT',

            'FAMILY_CREATED',
            'FAMILY_UPDATED',
            'FAMILY_MEMBER_INVITED',
            'FAMILY_MEMBER_JOINED',
            'FAMILY_MEMBER_REMOVED',
            'FAMILY_MEMBER_ROLE_UPDATED',

            'VAULT_ITEM_CREATED',
            'VAULT_ITEM_UPDATED',
            'VAULT_ITEM_DELETED',
            'VAULT_ITEM_VIEWED',

            'NOTE_CREATED',
            'NOTE_UPDATED',
            'NOTE_DELETED',

            'FINANCE_TRANSACTION_CREATED',
            'FINANCE_TRANSACTION_UPDATED',
            'FINANCE_TRANSACTION_DELETED'
        )
    );
