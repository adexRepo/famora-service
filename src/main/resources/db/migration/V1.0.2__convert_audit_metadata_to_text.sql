ALTER TABLE famora.audit_logs
    ALTER COLUMN metadata TYPE TEXT USING metadata::text;
