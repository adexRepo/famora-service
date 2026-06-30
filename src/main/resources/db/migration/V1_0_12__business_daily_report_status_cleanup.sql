-- Split report workflow status from the inherited generic AuditableEntity status.

ALTER TABLE business_daily_reports
  ADD COLUMN IF NOT EXISTS report_status varchar(30);

UPDATE business_daily_reports
SET report_status = CASE
  WHEN status = 'DELETED' THEN 'VOIDED'
  WHEN status IN (
    'DRAFT',
    'SUBMITTED',
    'APPROVED',
    'REJECTED',
    'REVISION_REQUESTED',
    'VOIDED'
  ) THEN status
  ELSE 'DRAFT'
END
WHERE report_status IS NULL;

UPDATE business_daily_reports
SET status = 'ACTIVE'
WHERE status IN (
  'DRAFT',
  'SUBMITTED',
  'APPROVED',
  'REJECTED',
  'REVISION_REQUESTED',
  'VOIDED'
);

ALTER TABLE business_daily_reports
  ALTER COLUMN status SET DEFAULT 'ACTIVE',
  ALTER COLUMN report_status SET DEFAULT 'DRAFT',
  ALTER COLUMN report_status SET NOT NULL;

DROP INDEX IF EXISTS ux_business_daily_reports_business_date_shift_active;

CREATE UNIQUE INDEX ux_business_daily_reports_business_date_shift_active
ON business_daily_reports (business_id, report_date, shift)
WHERE status <> 'DELETED'
  AND report_status <> 'VOIDED';
