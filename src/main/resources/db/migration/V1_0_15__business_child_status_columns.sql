ALTER TABLE famora.business_daily_sales_items
  ADD COLUMN IF NOT EXISTS status varchar(30);

UPDATE famora.business_daily_sales_items
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE famora.business_daily_sales_items
  ALTER COLUMN status SET DEFAULT 'ACTIVE',
  ALTER COLUMN status SET NOT NULL;

ALTER TABLE famora.business_daily_loss_items
  ADD COLUMN IF NOT EXISTS status varchar(30);

UPDATE famora.business_daily_loss_items
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE famora.business_daily_loss_items
  ALTER COLUMN status SET DEFAULT 'ACTIVE',
  ALTER COLUMN status SET NOT NULL;

ALTER TABLE famora.business_daily_payment_breakdowns
  ADD COLUMN IF NOT EXISTS status varchar(30);

UPDATE famora.business_daily_payment_breakdowns
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE famora.business_daily_payment_breakdowns
  ALTER COLUMN status SET DEFAULT 'ACTIVE',
  ALTER COLUMN status SET NOT NULL;

ALTER TABLE famora.business_daily_report_revisions
  ADD COLUMN IF NOT EXISTS status varchar(30);

UPDATE famora.business_daily_report_revisions
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE famora.business_daily_report_revisions
  ALTER COLUMN status SET DEFAULT 'ACTIVE',
  ALTER COLUMN status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_business_daily_sales_items_report_status
ON famora.business_daily_sales_items(daily_report_id, status);

CREATE INDEX IF NOT EXISTS idx_business_daily_loss_items_report_status
ON famora.business_daily_loss_items(daily_report_id, status);

CREATE INDEX IF NOT EXISTS idx_business_daily_payment_breakdowns_report_status
ON famora.business_daily_payment_breakdowns(daily_report_id, status);

CREATE INDEX IF NOT EXISTS idx_business_daily_report_revisions_report_status
ON famora.business_daily_report_revisions(daily_report_id, status);
