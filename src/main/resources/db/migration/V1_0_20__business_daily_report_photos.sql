CREATE TABLE IF NOT EXISTS famora.business_daily_report_photos
(
    id              uuid PRIMARY KEY,
    business_id     uuid                     NOT NULL REFERENCES famora.businesses (id),
    daily_report_id uuid                     NOT NULL REFERENCES famora.business_daily_reports (id),
    original_name   varchar(255)             NOT NULL,
    stored_name     varchar(255)             NOT NULL,
    storage_type    varchar(30)              NOT NULL,
    storage_path    text,
    bucket_name     varchar(100),
    object_key      text,
    file_type       varchar(30)              NOT NULL,
    mime_type       varchar(120)             NOT NULL,
    file_size       bigint                   NOT NULL,
    file_hash       text,
    created_at      timestamp with time zone NOT NULL,
    updated_at      timestamp with time zone NOT NULL,
    created_by      uuid                     NOT NULL REFERENCES famora.users (id),
    updated_by      uuid REFERENCES famora.users (id),
    status          varchar(30)              NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_business_daily_report_photos_report
    ON famora.business_daily_report_photos (business_id, daily_report_id, status, created_at);
