-- Famora MVP 3A.1
-- Business Daily Report Workflow + Revision Snapshot + Lightweight Audit
--
-- Assumptions:
-- 1. business_daily_reports already exists.
-- 2. business_daily_reports has columns: id, business_id, status, created_at, updated_at.
-- 3. audit_logs already exists. If the table uses different column names, adjust indexes accordingly.
-- 4. AuditableEntity uses created_by, updated_by, created_at, updated_at.
-- 5. Enum values are managed in Java only; this migration does NOT add enum CHECK constraints.

-- ---------------------------------------------------------
-- 1. Daily report workflow columns
-- ---------------------------------------------------------

ALTER TABLE business_daily_reports
  ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS submitted_at timestamptz NULL,
  ADD COLUMN IF NOT EXISTS revision_requested_at timestamptz NULL,
  ADD COLUMN IF NOT EXISTS revision_requested_by_user_id uuid NULL REFERENCES users(id),
  ADD COLUMN IF NOT EXISTS revision_reason text NULL,
  ADD COLUMN IF NOT EXISTS voided_by_user_id uuid NULL REFERENCES users(id),
  ADD COLUMN IF NOT EXISTS voided_at timestamptz NULL,
  ADD COLUMN IF NOT EXISTS void_reason text NULL;

-- If old MVP used DELETED for report soft delete, normalize it into VOIDED.
UPDATE business_daily_reports
SET status = 'VOIDED'
WHERE status = 'DELETED';

-- Ensure composite FK target exists for revision and report children.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'ux_business_daily_reports_id_business_id'
  ) THEN
    ALTER TABLE business_daily_reports
      ADD CONSTRAINT ux_business_daily_reports_id_business_id UNIQUE (id, business_id);
  END IF;
END $$;

-- ---------------------------------------------------------
-- 2. Dedicated full snapshot table
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS business_daily_report_revisions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  business_id uuid NOT NULL REFERENCES businesses(id),
  daily_report_id uuid NOT NULL,

  revision_number integer NOT NULL,

  change_type varchar(80) NOT NULL,

  old_status varchar(30) NULL,
  new_status varchar(30) NULL,

  changed_by_user_id uuid NOT NULL REFERENCES users(id),
  change_reason text NULL,

  snapshot_json text NOT NULL,

  created_by uuid NOT NULL REFERENCES users(id),
  updated_by uuid NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,

  CONSTRAINT fk_business_daily_report_revisions_report_business
    FOREIGN KEY (daily_report_id, business_id)
    REFERENCES business_daily_reports(id, business_id),

  CONSTRAINT ux_business_daily_report_revisions_report_number
    UNIQUE (daily_report_id, revision_number),

  CONSTRAINT chk_business_daily_report_revisions_number
    CHECK (revision_number > 0)
);

CREATE INDEX IF NOT EXISTS idx_business_daily_report_revisions_business_report
ON business_daily_report_revisions(business_id, daily_report_id);

CREATE INDEX IF NOT EXISTS idx_business_daily_report_revisions_report_created_at
ON business_daily_report_revisions(daily_report_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_business_daily_report_revisions_changed_by_created_at
ON business_daily_report_revisions(changed_by_user_id, created_at DESC);

-- ---------------------------------------------------------
-- 3. Lightweight business audit support
-- ---------------------------------------------------------

ALTER TABLE audit_logs
  ADD COLUMN IF NOT EXISTS business_id uuid NULL REFERENCES businesses(id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_business_id_created_at
ON audit_logs(business_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_type_entity_id
ON audit_logs(entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action_created_at
ON audit_logs(action, created_at DESC);

-- Optional: create only if these columns exist in your audit_logs table.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'audit_logs' AND column_name = 'user_id'
  ) THEN
    CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id_created_at
    ON audit_logs(user_id, created_at DESC);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'audit_logs' AND column_name = 'family_id'
  ) THEN
    CREATE INDEX IF NOT EXISTS idx_audit_logs_family_id_created_at
    ON audit_logs(family_id, created_at DESC);
  END IF;
END $$;
