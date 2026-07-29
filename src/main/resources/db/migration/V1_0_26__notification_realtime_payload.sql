ALTER TABLE famora.scheduled_notifications
    ADD COLUMN IF NOT EXISTS notification_type varchar(80),
    ADD COLUMN IF NOT EXISTS entity_type varchar(80),
    ADD COLUMN IF NOT EXISTS payload_json jsonb;

UPDATE famora.scheduled_notifications
SET notification_type = 'TRACKER_DUE_SOON'
WHERE notification_type IS NULL;

ALTER TABLE famora.scheduled_notifications
    ALTER COLUMN notification_type SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_receiver_created
    ON famora.scheduled_notifications (receiver_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_type_receiver
    ON famora.scheduled_notifications (notification_type, receiver_user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_entity
    ON famora.scheduled_notifications (entity_type, source_entity_id);
