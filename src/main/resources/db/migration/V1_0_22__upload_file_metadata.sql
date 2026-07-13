ALTER TABLE famora.files
    ADD COLUMN IF NOT EXISTS original_extension varchar(20),
    ADD COLUMN IF NOT EXISTS original_mime_type varchar(120),
    ADD COLUMN IF NOT EXISTS metadata_json jsonb;

ALTER TABLE famora.business_daily_report_photos
    ADD COLUMN IF NOT EXISTS original_extension varchar(20),
    ADD COLUMN IF NOT EXISTS original_mime_type varchar(120),
    ADD COLUMN IF NOT EXISTS metadata_json jsonb;
