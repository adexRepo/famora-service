-- =========================================================
-- Famora MVP 2 - Normalize audit columns
-- Safe migration:
-- - Adds created_by / updated_by
-- - Adds created_at / updated_at
-- - Adds status where needed
-- - Adds visibility only for visible entities
-- - Backfills created_by only from columns that exist
-- - Does not fail if users table is empty
-- - Sets NOT NULL only when no NULL remains
-- =========================================================


-- =========================================================
-- 0. VISIBILITY + STATUS
-- =========================================================
-- VisibleFamilyScopedEntity:
-- - vault_items
-- - notes
-- - files
-- - documents
--
-- FamilyScopedEntity only:
-- - finance_transactions
-- - emergency_contacts
-- =========================================================


-- =========================================================
-- VAULT ITEMS visibility/status
-- Default vault visibility: PRIVATE
-- =========================================================

ALTER TABLE famora.vault_items
ADD COLUMN IF NOT EXISTS visibility VARCHAR(30);

UPDATE famora.vault_items
SET visibility = 'PRIVATE'
WHERE visibility IS NULL;

ALTER TABLE famora.vault_items
ALTER COLUMN visibility SET DEFAULT 'PRIVATE';

ALTER TABLE famora.vault_items
ALTER COLUMN visibility SET NOT NULL;


ALTER TABLE famora.vault_items
ADD COLUMN IF NOT EXISTS status VARCHAR(30);

UPDATE famora.vault_items
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE famora.vault_items
ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE famora.vault_items
ALTER COLUMN status SET NOT NULL;


-- =========================================================
-- NOTES visibility/status
-- Default note visibility: FAMILY
-- =========================================================

ALTER TABLE famora.notes
ADD COLUMN IF NOT EXISTS visibility VARCHAR(30);

UPDATE famora.notes
SET visibility = 'FAMILY'
WHERE visibility IS NULL;

ALTER TABLE famora.notes
ALTER COLUMN visibility SET DEFAULT 'FAMILY';

ALTER TABLE famora.notes
ALTER COLUMN visibility SET NOT NULL;


ALTER TABLE famora.notes
ADD COLUMN IF NOT EXISTS status VARCHAR(30);

UPDATE famora.notes
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE famora.notes
ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE famora.notes
ALTER COLUMN status SET NOT NULL;


-- =========================================================
-- FINANCE TRANSACTIONS status only
-- No visibility for MVP
-- =========================================================

ALTER TABLE famora.finance_transactions
ADD COLUMN IF NOT EXISTS status VARCHAR(30);

UPDATE famora.finance_transactions
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE famora.finance_transactions
ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE famora.finance_transactions
ALTER COLUMN status SET NOT NULL;


-- =========================================================
-- FILES visibility/status
-- Default file visibility: PRIVATE
-- =========================================================

ALTER TABLE famora.files
ADD COLUMN IF NOT EXISTS visibility VARCHAR(30);

UPDATE famora.files
SET visibility = 'PRIVATE'
WHERE visibility IS NULL;

ALTER TABLE famora.files
ALTER COLUMN visibility SET DEFAULT 'PRIVATE';

ALTER TABLE famora.files
ALTER COLUMN visibility SET NOT NULL;

ALTER TABLE famora.files
ADD COLUMN IF NOT EXISTS status VARCHAR(30);

UPDATE famora.files
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE famora.files
ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE famora.files
ALTER COLUMN status SET NOT NULL;


-- =========================================================
-- DOCUMENTS visibility/status
-- Default document visibility: OWNER_ONLY
-- =========================================================

ALTER TABLE famora.documents
ADD COLUMN IF NOT EXISTS visibility VARCHAR(30);

UPDATE famora.documents
SET visibility = 'OWNER_ONLY'
WHERE visibility IS NULL;

ALTER TABLE famora.documents
ALTER COLUMN visibility SET DEFAULT 'OWNER_ONLY';

ALTER TABLE famora.documents
ALTER COLUMN visibility SET NOT NULL;


ALTER TABLE famora.documents
ADD COLUMN IF NOT EXISTS status VARCHAR(30);

UPDATE famora.documents
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE famora.documents
ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE famora.documents
ALTER COLUMN status SET NOT NULL;


-- =========================================================
-- EMERGENCY CONTACTS status only
-- No visibility for MVP
-- =========================================================

ALTER TABLE famora.emergency_contacts
ADD COLUMN IF NOT EXISTS status VARCHAR(30);

UPDATE famora.emergency_contacts
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE famora.emergency_contacts
ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE famora.emergency_contacts
ALTER COLUMN status SET NOT NULL;



-- =========================================================
-- 1. VAULT ITEMS audit columns
-- =========================================================

ALTER TABLE famora.vault_items
ADD COLUMN IF NOT EXISTS created_by UUID;

ALTER TABLE famora.vault_items
ADD COLUMN IF NOT EXISTS updated_by UUID;

ALTER TABLE famora.vault_items
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE famora.vault_items
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();


-- Backfill from created_by_user_id only if column exists
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'famora'
      AND table_name = 'vault_items'
      AND column_name = 'created_by_user_id'
  ) THEN
    EXECUTE '
      UPDATE famora.vault_items
      SET created_by = created_by_user_id
      WHERE created_by IS NULL
        AND created_by_user_id IS NOT NULL
    ';
  END IF;
END $$;


-- Fallback to first user only if users table has rows
UPDATE famora.vault_items
SET created_by = (
    SELECT id
    FROM famora.users
    ORDER BY created_at ASC NULLS LAST, id ASC
    LIMIT 1
)
WHERE created_by IS NULL
  AND EXISTS (SELECT 1 FROM famora.users);


-- Set NOT NULL only if safe
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM famora.vault_items
    WHERE created_by IS NULL
  ) THEN
    ALTER TABLE famora.vault_items
    ALTER COLUMN created_by SET NOT NULL;
  ELSE
    RAISE NOTICE 'vault_items.created_by still has NULL values. NOT NULL was skipped.';
  END IF;
END $$;


-- FK constraints
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_vault_items_created_by'
  ) THEN
    ALTER TABLE famora.vault_items
    ADD CONSTRAINT fk_vault_items_created_by
    FOREIGN KEY (created_by) REFERENCES famora.users(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_vault_items_updated_by'
  ) THEN
    ALTER TABLE famora.vault_items
    ADD CONSTRAINT fk_vault_items_updated_by
    FOREIGN KEY (updated_by) REFERENCES famora.users(id);
  END IF;
END $$;



-- =========================================================
-- 2. NOTES audit columns
-- =========================================================

ALTER TABLE famora.notes
ADD COLUMN IF NOT EXISTS created_by UUID;

ALTER TABLE famora.notes
ADD COLUMN IF NOT EXISTS updated_by UUID;

ALTER TABLE famora.notes
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE famora.notes
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();


-- Backfill from created_by_user_id only if column exists
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'famora'
      AND table_name = 'notes'
      AND column_name = 'created_by_user_id'
  ) THEN
    EXECUTE '
      UPDATE famora.notes
      SET created_by = created_by_user_id
      WHERE created_by IS NULL
        AND created_by_user_id IS NOT NULL
    ';
  END IF;
END $$;


UPDATE famora.notes
SET created_by = (
    SELECT id
    FROM famora.users
    ORDER BY created_at ASC NULLS LAST, id ASC
    LIMIT 1
)
WHERE created_by IS NULL
  AND EXISTS (SELECT 1 FROM famora.users);


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM famora.notes
    WHERE created_by IS NULL
  ) THEN
    ALTER TABLE famora.notes
    ALTER COLUMN created_by SET NOT NULL;
  ELSE
    RAISE NOTICE 'notes.created_by still has NULL values. NOT NULL was skipped.';
  END IF;
END $$;


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_notes_created_by'
  ) THEN
    ALTER TABLE famora.notes
    ADD CONSTRAINT fk_notes_created_by
    FOREIGN KEY (created_by) REFERENCES famora.users(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_notes_updated_by'
  ) THEN
    ALTER TABLE famora.notes
    ADD CONSTRAINT fk_notes_updated_by
    FOREIGN KEY (updated_by) REFERENCES famora.users(id);
  END IF;
END $$;



-- =========================================================
-- 3. FINANCE TRANSACTIONS audit columns
-- =========================================================

ALTER TABLE famora.finance_transactions
ADD COLUMN IF NOT EXISTS created_by UUID;

ALTER TABLE famora.finance_transactions
ADD COLUMN IF NOT EXISTS updated_by UUID;

ALTER TABLE famora.finance_transactions
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE famora.finance_transactions
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();


DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'famora'
      AND table_name = 'finance_transactions'
      AND column_name = 'created_by_user_id'
  ) THEN
    EXECUTE '
      UPDATE famora.finance_transactions
      SET created_by = created_by_user_id
      WHERE created_by IS NULL
        AND created_by_user_id IS NOT NULL
    ';
  END IF;
END $$;


UPDATE famora.finance_transactions
SET created_by = (
    SELECT id
    FROM famora.users
    ORDER BY created_at ASC NULLS LAST, id ASC
    LIMIT 1
)
WHERE created_by IS NULL
  AND EXISTS (SELECT 1 FROM famora.users);


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM famora.finance_transactions
    WHERE created_by IS NULL
  ) THEN
    ALTER TABLE famora.finance_transactions
    ALTER COLUMN created_by SET NOT NULL;
  ELSE
    RAISE NOTICE 'finance_transactions.created_by still has NULL values. NOT NULL was skipped.';
  END IF;
END $$;


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_finance_transactions_created_by'
  ) THEN
    ALTER TABLE famora.finance_transactions
    ADD CONSTRAINT fk_finance_transactions_created_by
    FOREIGN KEY (created_by) REFERENCES famora.users(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_finance_transactions_updated_by'
  ) THEN
    ALTER TABLE famora.finance_transactions
    ADD CONSTRAINT fk_finance_transactions_updated_by
    FOREIGN KEY (updated_by) REFERENCES famora.users(id);
  END IF;
END $$;



-- =========================================================
-- 4. FILES audit columns
-- =========================================================

ALTER TABLE famora.files
ADD COLUMN IF NOT EXISTS created_by UUID;

ALTER TABLE famora.files
ADD COLUMN IF NOT EXISTS updated_by UUID;

ALTER TABLE famora.files
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE famora.files
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Backfill from uploaded_by_user_id only if column exists
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'famora'
      AND table_name = 'files'
      AND column_name = 'uploaded_by_user_id'
  ) THEN
    EXECUTE '
      UPDATE famora.files
      SET created_by = uploaded_by_user_id
      WHERE created_by IS NULL
        AND uploaded_by_user_id IS NOT NULL
    ';
  END IF;
END $$;


-- Backfill from created_by_user_id only if column exists
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'famora'
      AND table_name = 'files'
      AND column_name = 'created_by_user_id'
  ) THEN
    EXECUTE '
      UPDATE famora.files
      SET created_by = created_by_user_id
      WHERE created_by IS NULL
        AND created_by_user_id IS NOT NULL
    ';
  END IF;
END $$;


UPDATE famora.files
SET created_by = (
    SELECT id
    FROM famora.users
    ORDER BY created_at ASC NULLS LAST, id ASC
    LIMIT 1
)
WHERE created_by IS NULL
  AND EXISTS (SELECT 1 FROM famora.users);


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM famora.files
    WHERE created_by IS NULL
  ) THEN
    ALTER TABLE famora.files
    ALTER COLUMN created_by SET NOT NULL;
  ELSE
    RAISE NOTICE 'files.created_by still has NULL values. NOT NULL was skipped.';
  END IF;
END $$;


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_files_created_by'
  ) THEN
    ALTER TABLE famora.files
    ADD CONSTRAINT fk_files_created_by
    FOREIGN KEY (created_by) REFERENCES famora.users(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_files_updated_by'
  ) THEN
    ALTER TABLE famora.files
    ADD CONSTRAINT fk_files_updated_by
    FOREIGN KEY (updated_by) REFERENCES famora.users(id);
  END IF;
END $$;



-- =========================================================
-- 5. DOCUMENTS audit columns
-- =========================================================

ALTER TABLE famora.documents
ADD COLUMN IF NOT EXISTS created_by UUID;

ALTER TABLE famora.documents
ADD COLUMN IF NOT EXISTS updated_by UUID;

ALTER TABLE famora.documents
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE famora.documents
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();


-- Backfill from linked files.created_by if files table has created_by
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'famora'
      AND table_name = 'files'
      AND column_name = 'created_by'
  ) THEN
    EXECUTE '
      UPDATE famora.documents d
      SET created_by = f.created_by
      FROM famora.files f
      WHERE d.file_id = f.id
        AND d.created_by IS NULL
        AND f.created_by IS NOT NULL
    ';
  END IF;
END $$;


-- Backfill from owner_user_id only if column exists
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'famora'
      AND table_name = 'documents'
      AND column_name = 'owner_user_id'
  ) THEN
    EXECUTE '
      UPDATE famora.documents
      SET created_by = owner_user_id
      WHERE created_by IS NULL
        AND owner_user_id IS NOT NULL
    ';
  END IF;
END $$;


UPDATE famora.documents
SET created_by = (
    SELECT id
    FROM famora.users
    ORDER BY created_at ASC NULLS LAST, id ASC
    LIMIT 1
)
WHERE created_by IS NULL
  AND EXISTS (SELECT 1 FROM famora.users);


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM famora.documents
    WHERE created_by IS NULL
  ) THEN
    ALTER TABLE famora.documents
    ALTER COLUMN created_by SET NOT NULL;
  ELSE
    RAISE NOTICE 'documents.created_by still has NULL values. NOT NULL was skipped.';
  END IF;
END $$;


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_documents_created_by'
  ) THEN
    ALTER TABLE famora.documents
    ADD CONSTRAINT fk_documents_created_by
    FOREIGN KEY (created_by) REFERENCES famora.users(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_documents_updated_by'
  ) THEN
    ALTER TABLE famora.documents
    ADD CONSTRAINT fk_documents_updated_by
    FOREIGN KEY (updated_by) REFERENCES famora.users(id);
  END IF;
END $$;



-- =========================================================
-- 6. EMERGENCY CONTACTS audit columns
-- =========================================================

ALTER TABLE famora.emergency_contacts
ADD COLUMN IF NOT EXISTS created_by UUID;

ALTER TABLE famora.emergency_contacts
ADD COLUMN IF NOT EXISTS updated_by UUID;

ALTER TABLE famora.emergency_contacts
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE famora.emergency_contacts
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();


DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'famora'
      AND table_name = 'emergency_contacts'
      AND column_name = 'created_by_user_id'
  ) THEN
    EXECUTE '
      UPDATE famora.emergency_contacts
      SET created_by = created_by_user_id
      WHERE created_by IS NULL
        AND created_by_user_id IS NOT NULL
    ';
  END IF;
END $$;


UPDATE famora.emergency_contacts
SET created_by = (
    SELECT id
    FROM famora.users
    ORDER BY created_at ASC NULLS LAST, id ASC
    LIMIT 1
)
WHERE created_by IS NULL
  AND EXISTS (SELECT 1 FROM famora.users);


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM famora.emergency_contacts
    WHERE created_by IS NULL
  ) THEN
    ALTER TABLE famora.emergency_contacts
    ALTER COLUMN created_by SET NOT NULL;
  ELSE
    RAISE NOTICE 'emergency_contacts.created_by still has NULL values. NOT NULL was skipped.';
  END IF;
END $$;


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_emergency_contacts_created_by'
  ) THEN
    ALTER TABLE famora.emergency_contacts
    ADD CONSTRAINT fk_emergency_contacts_created_by
    FOREIGN KEY (created_by) REFERENCES famora.users(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_emergency_contacts_updated_by'
  ) THEN
    ALTER TABLE famora.emergency_contacts
    ADD CONSTRAINT fk_emergency_contacts_updated_by
    FOREIGN KEY (updated_by) REFERENCES famora.users(id);
  END IF;
END $$;
