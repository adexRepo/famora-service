CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE user_status AS ENUM ('ACTIVE','INACTIVE','LOCKED','DELETED');
CREATE TYPE family_status AS ENUM ('ACTIVE','INACTIVE','DELETED');
CREATE TYPE family_member_role AS ENUM ('OWNER','ADMIN','MEMBER','VIEWER');
CREATE TYPE family_member_status AS ENUM ('ACTIVE','PENDING','REMOVED','LEFT');
CREATE TYPE invitation_status AS ENUM ('ACTIVE','USED','EXPIRED','REVOKED');
CREATE TYPE finance_transaction_type AS ENUM ('INCOME','EXPENSE');
CREATE TYPE audit_action AS ENUM (
    'USER_REGISTERED','USER_LOGGED_IN','USER_LOGGED_OUT',
    'FAMILY_CREATED','FAMILY_UPDATED','FAMILY_MEMBER_INVITED','FAMILY_MEMBER_JOINED','FAMILY_MEMBER_REMOVED','FAMILY_MEMBER_ROLE_UPDATED',
    'VAULT_ITEM_CREATED','VAULT_ITEM_UPDATED','VAULT_ITEM_DELETED','VAULT_ITEM_VIEWED',
    'NOTE_CREATED','NOTE_UPDATED','NOTE_DELETED',
    'FINANCE_TRANSACTION_CREATED','FINANCE_TRANSACTION_UPDATED','FINANCE_TRANSACTION_DELETED'
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL,
    password_hash TEXT NOT NULL,
    status user_status NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_users_email UNIQUE (email)
);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_hash TEXT NOT NULL,
    device_id VARCHAR(150),
    device_name VARCHAR(150),
    ip_address VARCHAR(80),
    user_agent TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_sessions_refresh_token_hash UNIQUE (refresh_token_hash)
);
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX idx_user_sessions_revoked_at ON user_sessions(revoked_at);

CREATE TABLE families (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    status family_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_families_owner_user_id ON families(owner_user_id);
CREATE INDEX idx_families_status ON families(status);
CREATE INDEX idx_families_deleted_at ON families(deleted_at);

CREATE TABLE family_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role family_member_role NOT NULL DEFAULT 'MEMBER',
    status family_member_status NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMPTZ,
    removed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_family_members_family_user UNIQUE (family_id, user_id)
);
CREATE INDEX idx_family_members_family_id ON family_members(family_id);
CREATE INDEX idx_family_members_user_id ON family_members(user_id);
CREATE INDEX idx_family_members_role ON family_members(role);
CREATE INDEX idx_family_members_status ON family_members(status);

CREATE TABLE family_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    invite_code VARCHAR(50) NOT NULL,
    role family_member_role NOT NULL DEFAULT 'MEMBER',
    status invitation_status NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMPTZ NOT NULL,
    used_by_user_id UUID REFERENCES users(id),
    used_at TIMESTAMPTZ,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_family_invitations_invite_code UNIQUE (invite_code)
);
CREATE INDEX idx_family_invitations_family_id ON family_invitations(family_id);
CREATE INDEX idx_family_invitations_invite_code ON family_invitations(invite_code);
CREATE INDEX idx_family_invitations_status ON family_invitations(status);
CREATE INDEX idx_family_invitations_expires_at ON family_invitations(expires_at);

CREATE TABLE vault_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    username VARCHAR(180),
    encrypted_secret TEXT NOT NULL,
    url TEXT,
    notes TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_vault_items_family_id ON vault_items(family_id);
CREATE INDEX idx_vault_items_created_by ON vault_items(created_by);
CREATE INDEX idx_vault_items_deleted_at ON vault_items(deleted_at);
CREATE INDEX idx_vault_items_family_title ON vault_items(family_id, title);

CREATE TABLE notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(80),
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_notes_family_id ON notes(family_id);
CREATE INDEX idx_notes_created_by ON notes(created_by);
CREATE INDEX idx_notes_category ON notes(category);
CREATE INDEX idx_notes_deleted_at ON notes(deleted_at);
CREATE INDEX idx_notes_family_title ON notes(family_id, title);

CREATE TABLE finance_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    type finance_transaction_type NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'MYR',
    category VARCHAR(100) NOT NULL,
    description TEXT,
    transaction_date DATE NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_finance_transactions_amount_positive CHECK (amount > 0)
);
CREATE INDEX idx_finance_transactions_family_id ON finance_transactions(family_id);
CREATE INDEX idx_finance_transactions_type ON finance_transactions(type);
CREATE INDEX idx_finance_transactions_category ON finance_transactions(category);
CREATE INDEX idx_finance_transactions_transaction_date ON finance_transactions(transaction_date);
CREATE INDEX idx_finance_transactions_deleted_at ON finance_transactions(deleted_at);
CREATE INDEX idx_finance_transactions_family_month ON finance_transactions(family_id, transaction_date);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID REFERENCES families(id) ON DELETE SET NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action audit_action NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,
    ip_address VARCHAR(80),
    user_agent TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_family_id ON audit_logs(family_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_set_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_user_sessions_set_updated_at BEFORE UPDATE ON user_sessions FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_families_set_updated_at BEFORE UPDATE ON families FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_family_members_set_updated_at BEFORE UPDATE ON family_members FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_family_invitations_set_updated_at BEFORE UPDATE ON family_invitations FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_vault_items_set_updated_at BEFORE UPDATE ON vault_items FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_notes_set_updated_at BEFORE UPDATE ON notes FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_finance_transactions_set_updated_at BEFORE UPDATE ON finance_transactions FOR EACH ROW EXECUTE FUNCTION set_updated_at();
