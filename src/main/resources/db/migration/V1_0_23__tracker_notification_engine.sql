CREATE TABLE IF NOT EXISTS famora.trackers
(
    id                          uuid PRIMARY KEY,
    scope_type                  varchar(30)              NOT NULL,
    family_id                   uuid REFERENCES famora.families (id),
    business_id                 uuid REFERENCES famora.businesses (id),
    owner_user_id               uuid                     NOT NULL REFERENCES famora.users (id),
    source_module               varchar(50)              NOT NULL,
    source_entity_type          varchar(80),
    source_entity_id            uuid,
    title                       varchar(180)             NOT NULL,
    description                 text,
    tracker_type                varchar(80)              NOT NULL,
    category                    varchar(80)              NOT NULL,
    assigned_user_id            uuid REFERENCES famora.users (id),
    assigned_family_member_id   uuid REFERENCES famora.family_members (id),
    assigned_business_member_id uuid REFERENCES famora.business_members (id),
    start_date                  date                     NOT NULL,
    due_date                    date,
    reminder_time               time,
    timezone                    varchar(80)              NOT NULL DEFAULT 'Asia/Kuala_Lumpur',
    frequency                   varchar(30)              NOT NULL,
    interval_value              integer                  NOT NULL DEFAULT 1,
    days_of_week                varchar(120),
    day_of_month                integer,
    notify_delay_minutes        integer                  NOT NULL DEFAULT 0,
    visibility                  varchar(30)              NOT NULL DEFAULT 'FAMILY',
    status                      varchar(30)              NOT NULL DEFAULT 'ACTIVE',
    created_by_user_id          uuid                     NOT NULL REFERENCES famora.users (id),
    updated_by_user_id          uuid REFERENCES famora.users (id),
    created_at                  timestamp with time zone NOT NULL,
    updated_at                  timestamp with time zone NOT NULL,
    CONSTRAINT chk_trackers_scope_family CHECK (scope_type <> 'FAMILY' OR family_id IS NOT NULL),
    CONSTRAINT chk_trackers_scope_business CHECK (scope_type <> 'BUSINESS' OR business_id IS NOT NULL),
    CONSTRAINT chk_trackers_notify_delay CHECK (notify_delay_minutes >= 0),
    CONSTRAINT chk_trackers_interval CHECK (interval_value >= 1),
    CONSTRAINT chk_trackers_day_of_month CHECK (day_of_month IS NULL OR day_of_month BETWEEN 1 AND 31)
);

CREATE TABLE IF NOT EXISTS famora.tracker_logs
(
    id                uuid PRIMARY KEY,
    tracker_id        uuid                     NOT NULL REFERENCES famora.trackers (id),
    scope_type        varchar(30)              NOT NULL,
    family_id         uuid REFERENCES famora.families (id),
    business_id       uuid REFERENCES famora.businesses (id),
    logged_by_user_id uuid                     NOT NULL REFERENCES famora.users (id),
    log_date          date                     NOT NULL,
    status            varchar(30)              NOT NULL,
    value             varchar(100),
    notes             text,
    created_at        timestamp with time zone NOT NULL,
    updated_at        timestamp with time zone NOT NULL,
    CONSTRAINT uk_tracker_logs_tracker_date_user UNIQUE (tracker_id, log_date, logged_by_user_id)
);

CREATE TABLE IF NOT EXISTS famora.scheduled_notifications
(
    id                 uuid PRIMARY KEY,
    tracker_id         uuid REFERENCES famora.trackers (id),
    scope_type         varchar(30)              NOT NULL,
    family_id          uuid REFERENCES famora.families (id),
    business_id        uuid REFERENCES famora.businesses (id),
    receiver_user_id   uuid                     NOT NULL REFERENCES famora.users (id),
    title              varchar(180)             NOT NULL,
    body               text,
    scheduled_at       timestamp with time zone NOT NULL,
    channel            varchar(30)              NOT NULL DEFAULT 'PUSH',
    delivery_status    varchar(30)              NOT NULL DEFAULT 'PENDING',
    read_status        varchar(30)              NOT NULL DEFAULT 'UNREAD',
    source_module      varchar(50)              NOT NULL,
    source_entity_type varchar(80),
    source_entity_id   uuid,
    created_at         timestamp with time zone NOT NULL,
    updated_at         timestamp with time zone NOT NULL,
    sent_at            timestamp with time zone,
    read_at            timestamp with time zone
);

CREATE INDEX IF NOT EXISTS idx_trackers_owner_status
    ON famora.trackers (owner_user_id, status);
CREATE INDEX IF NOT EXISTS idx_trackers_scope_family_status
    ON famora.trackers (scope_type, family_id, status);
CREATE INDEX IF NOT EXISTS idx_trackers_scope_business_status
    ON famora.trackers (scope_type, business_id, status);
CREATE INDEX IF NOT EXISTS idx_trackers_source
    ON famora.trackers (source_module, source_entity_type, source_entity_id);
CREATE INDEX IF NOT EXISTS idx_trackers_assigned_user_status
    ON famora.trackers (assigned_user_id, status);
CREATE INDEX IF NOT EXISTS idx_trackers_start_date
    ON famora.trackers (start_date);
CREATE INDEX IF NOT EXISTS idx_trackers_due_date
    ON famora.trackers (due_date);

CREATE INDEX IF NOT EXISTS idx_tracker_logs_tracker_date
    ON famora.tracker_logs (tracker_id, log_date);
CREATE INDEX IF NOT EXISTS idx_tracker_logs_family_date
    ON famora.tracker_logs (family_id, log_date);
CREATE INDEX IF NOT EXISTS idx_tracker_logs_business_date
    ON famora.tracker_logs (business_id, log_date);

CREATE INDEX IF NOT EXISTS idx_notifications_receiver_scheduled
    ON famora.scheduled_notifications (receiver_user_id, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_notifications_delivery_scheduled
    ON famora.scheduled_notifications (delivery_status, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_notifications_read_receiver
    ON famora.scheduled_notifications (read_status, receiver_user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_tracker
    ON famora.scheduled_notifications (tracker_id);
