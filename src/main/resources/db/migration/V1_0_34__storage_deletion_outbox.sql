CREATE TABLE famora.storage_deletion_outbox
(
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_type    varchar(30)              NOT NULL,
    bucket_name     varchar(100),
    object_key      text,
    storage_path    text,
    attempts        integer                  NOT NULL DEFAULT 0,
    next_attempt_at timestamp with time zone NOT NULL DEFAULT now(),
    locked_at       timestamp with time zone,
    processed_at    timestamp with time zone,
    last_error      varchar(120),
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT chk_storage_deletion_location CHECK (
      (storage_type = 'MINIO' AND bucket_name IS NOT NULL AND object_key IS NOT NULL)
      OR (storage_type = 'MFT' AND storage_path IS NOT NULL)
      OR (storage_type = 'BACKUP_TEMP' AND storage_path IS NOT NULL)
    )
);

CREATE INDEX idx_storage_deletion_outbox_due
    ON famora.storage_deletion_outbox (next_attempt_at, created_at)
    WHERE processed_at IS NULL;
