CREATE TABLE IF NOT EXISTS famora.notification_preferences
(
    id                    uuid PRIMARY KEY,
    user_id               uuid                     NOT NULL REFERENCES famora.users (id),
    notification_type     varchar(80)              NOT NULL,
    in_app_enabled        boolean                  NOT NULL DEFAULT true,
    push_enabled          boolean                  NOT NULL DEFAULT false,
    email_enabled         boolean                  NOT NULL DEFAULT false,
    quiet_hours_enabled   boolean                  NOT NULL DEFAULT false,
    quiet_hours_start     time,
    quiet_hours_end       time,
    timezone              varchar(80)              NOT NULL DEFAULT 'Asia/Jakarta',
    created_at            timestamp with time zone NOT NULL,
    updated_at            timestamp with time zone NOT NULL,
    CONSTRAINT uk_notification_preferences_user_type UNIQUE (user_id, notification_type)
);

CREATE INDEX IF NOT EXISTS idx_notification_preferences_user
    ON famora.notification_preferences (user_id);
