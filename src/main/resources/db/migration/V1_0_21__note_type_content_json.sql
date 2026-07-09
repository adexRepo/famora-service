ALTER TABLE famora.notes
    ADD COLUMN IF NOT EXISTS note_type varchar(30) NOT NULL DEFAULT 'TEXT',
    ADD COLUMN IF NOT EXISTS content_json jsonb;

CREATE INDEX IF NOT EXISTS idx_notes_family_note_type_status
    ON famora.notes (family_id, note_type, status);
