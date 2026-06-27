-- =========================================================
-- Replace deleted_at soft delete with status-based soft delete
-- Applies to users, families, and business tables
-- =========================================================

-- =========================================================
-- 1. Backfill status from deleted_at
-- =========================================================

UPDATE famora.vault_items
SET status = 'DELETED'
WHERE deleted_at IS NOT NULL;

UPDATE famora.notes
SET status = 'DELETED'
WHERE deleted_at IS NOT NULL;

UPDATE famora.finance_transactions
SET status = 'DELETED'
WHERE deleted_at IS NOT NULL;

UPDATE famora.users
SET status = 'DELETED'
WHERE deleted_at IS NOT NULL;

UPDATE famora.families
SET status = 'DELETED'
WHERE deleted_at IS NOT NULL;


-- =========================================================
-- 2. Ensure status is valid
-- =========================================================

UPDATE famora.vault_items
SET status = 'ACTIVE'
WHERE status IS NULL;

UPDATE famora.notes
SET status = 'ACTIVE'
WHERE status IS NULL;

UPDATE famora.finance_transactions
SET status = 'ACTIVE'
WHERE status IS NULL;

UPDATE famora.users
SET status = 'ACTIVE'
WHERE status IS NULL;

UPDATE famora.families
SET status = 'ACTIVE'
WHERE status IS NULL;


ALTER TABLE famora.vault_items
ALTER COLUMN status SET DEFAULT 'ACTIVE',
ALTER COLUMN status SET NOT NULL;

ALTER TABLE famora.notes
ALTER COLUMN status SET DEFAULT 'ACTIVE',
ALTER COLUMN status SET NOT NULL;

ALTER TABLE famora.finance_transactions
ALTER COLUMN status SET DEFAULT 'ACTIVE',
ALTER COLUMN status SET NOT NULL;

ALTER TABLE famora.users
ALTER COLUMN status SET DEFAULT 'ACTIVE',
ALTER COLUMN status SET NOT NULL;

ALTER TABLE famora.families
ALTER COLUMN status SET DEFAULT 'ACTIVE',
ALTER COLUMN status SET NOT NULL;


-- =========================================================
-- 3. Drop indexes that depend on deleted_at
-- Add more names here if pg_indexes shows more deleted_at indexes
-- =========================================================

DROP INDEX IF EXISTS famora.idx_vault_items_deleted_at;
DROP INDEX IF EXISTS famora.idx_vault_items_family_deleted_at;

DROP INDEX IF EXISTS famora.idx_notes_deleted_at;
DROP INDEX IF EXISTS famora.idx_notes_family_deleted_at;

DROP INDEX IF EXISTS famora.idx_finance_transactions_deleted_at;
DROP INDEX IF EXISTS famora.idx_finance_transactions_family_deleted_at;

DROP INDEX IF EXISTS famora.idx_users_deleted_at;
DROP INDEX IF EXISTS famora.idx_users_status_deleted_at;

DROP INDEX IF EXISTS famora.idx_families_deleted_at;
DROP INDEX IF EXISTS famora.idx_families_status_deleted_at;


-- =========================================================
-- 4. Drop deleted_at columns
-- =========================================================

ALTER TABLE famora.vault_items
DROP COLUMN IF EXISTS deleted_at;

ALTER TABLE famora.notes
DROP COLUMN IF EXISTS deleted_at;

ALTER TABLE famora.finance_transactions
DROP COLUMN IF EXISTS deleted_at;

ALTER TABLE famora.users
DROP COLUMN IF EXISTS deleted_at;

ALTER TABLE famora.families
DROP COLUMN IF EXISTS deleted_at;


-- =========================================================
-- 5. Vault items indexes
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_vault_items_family_status_visibility
ON famora.vault_items (family_id, status, visibility);

CREATE INDEX IF NOT EXISTS idx_vault_items_family_status_created_at
ON famora.vault_items (family_id, status, created_at DESC);


-- =========================================================
-- 6. Notes indexes
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_notes_family_status_visibility
ON famora.notes (family_id, status, visibility);

CREATE INDEX IF NOT EXISTS idx_notes_family_status_category
ON famora.notes (family_id, status, category);

CREATE INDEX IF NOT EXISTS idx_notes_family_status_created_at
ON famora.notes (family_id, status, created_at DESC);


-- =========================================================
-- 7. Finance transactions indexes
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_finance_transactions_family_status_date
ON famora.finance_transactions (family_id, status, transaction_date DESC, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_finance_transactions_family_status_type_date
ON famora.finance_transactions (family_id, status, type, transaction_date DESC);

CREATE INDEX IF NOT EXISTS idx_finance_transactions_family_status_category_date
ON famora.finance_transactions (family_id, status, category, transaction_date DESC);


-- =========================================================
-- 8. Files indexes
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_files_family_status_visibility
ON famora.files (family_id, status, visibility);

CREATE INDEX IF NOT EXISTS idx_files_family_status_file_type
ON famora.files (family_id, status, file_type);

CREATE INDEX IF NOT EXISTS idx_files_family_status_created_at
ON famora.files (family_id, status, created_at DESC);


-- =========================================================
-- 9. Documents indexes
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_documents_family_status_visibility
ON famora.documents (family_id, status, visibility);

CREATE INDEX IF NOT EXISTS idx_documents_family_status_document_type
ON famora.documents (family_id, status, document_type);

CREATE INDEX IF NOT EXISTS idx_documents_family_status_expiry_date
ON famora.documents (family_id, status, expiry_date);


-- =========================================================
-- 10. Emergency contacts indexes
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_emergency_contacts_family_status_category
ON famora.emergency_contacts (family_id, status, category);

CREATE INDEX IF NOT EXISTS idx_emergency_contacts_family_status_created_at
ON famora.emergency_contacts (family_id, status, created_at DESC);


-- =========================================================
-- 11. Users indexes
-- =========================================================

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower
ON famora.users (lower(email));

CREATE INDEX IF NOT EXISTS idx_users_status_created_at
ON famora.users (status, created_at DESC);


-- =========================================================
-- 12. Families indexes
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_families_status_created_at
ON famora.families (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_families_name_lower
ON famora.families (lower(name));


-- =========================================================
-- 13. Family members indexes
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_family_members_user_status
ON famora.family_members (user_id, status);

CREATE INDEX IF NOT EXISTS idx_family_members_family_status
ON famora.family_members (family_id, status);

CREATE INDEX IF NOT EXISTS idx_family_members_family_user_status
ON famora.family_members (family_id, user_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS ux_family_members_active_family_user
ON famora.family_members (family_id, user_id)
WHERE status = 'ACTIVE';


-- =========================================================
-- 14. Family invitations indexes
-- =========================================================

CREATE UNIQUE INDEX IF NOT EXISTS ux_family_invitations_invite_code
ON famora.family_invitations (invite_code);

CREATE INDEX IF NOT EXISTS idx_family_invitations_family_status
ON famora.family_invitations (family_id, status);

CREATE INDEX IF NOT EXISTS idx_family_invitations_status_expires_at
ON famora.family_invitations (status, expires_at);

CREATE INDEX IF NOT EXISTS idx_family_invitations_created_by
ON famora.family_invitations (created_by);
