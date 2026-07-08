ALTER TABLE famora.users
    ADD COLUMN IF NOT EXISTS password_changed_at timestamp with time zone;
