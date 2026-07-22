CREATE TABLE IF NOT EXISTS famora.backup_upload_sessions
(
    id              uuid PRIMARY KEY,
    family_id       uuid                     NOT NULL REFERENCES famora.families (id),
    upload_status   varchar(30)              NOT NULL,
    total_files     integer                  NOT NULL DEFAULT 0,
    completed_files integer                  NOT NULL DEFAULT 0,
    failed_files    integer                  NOT NULL DEFAULT 0,
    total_bytes     bigint                   NOT NULL DEFAULT 0,
    uploaded_bytes  bigint                   NOT NULL DEFAULT 0,
    category        varchar(100),
    notes           text,
    visibility      varchar(30)              NOT NULL DEFAULT 'PRIVATE',
    metadata_json   jsonb,
    completed_at    timestamp with time zone,
    cancelled_at    timestamp with time zone,
    status          varchar(30)              NOT NULL DEFAULT 'ACTIVE',
    created_by      uuid                     NOT NULL REFERENCES famora.users (id),
    updated_by      uuid REFERENCES famora.users (id),
    created_at      timestamp with time zone NOT NULL,
    updated_at      timestamp with time zone NOT NULL
);

CREATE TABLE IF NOT EXISTS famora.backup_upload_items
(
    id                 uuid PRIMARY KEY,
    session_id         uuid                     NOT NULL REFERENCES famora.backup_upload_sessions (id),
    family_id          uuid                     NOT NULL REFERENCES famora.families (id),
    file_asset_id      uuid REFERENCES famora.files (id),
    client_file_id     varchar(120),
    original_name      varchar(255)             NOT NULL,
    original_mime_type varchar(120),
    file_size          bigint                   NOT NULL,
    expected_sha256    varchar(64),
    chunk_size         bigint                   NOT NULL,
    total_chunks       integer                  NOT NULL,
    received_chunks    integer                  NOT NULL DEFAULT 0,
    uploaded_bytes     bigint                   NOT NULL DEFAULT 0,
    item_status        varchar(30)              NOT NULL,
    category           varchar(100),
    notes              text,
    visibility         varchar(30)              NOT NULL DEFAULT 'PRIVATE',
    assembled_sha256   varchar(64),
    metadata_json      jsonb,
    completed_at       timestamp with time zone,
    status             varchar(30)              NOT NULL DEFAULT 'ACTIVE',
    created_by         uuid                     NOT NULL REFERENCES famora.users (id),
    updated_by         uuid REFERENCES famora.users (id),
    created_at         timestamp with time zone NOT NULL,
    updated_at         timestamp with time zone NOT NULL,
    CONSTRAINT chk_backup_items_file_size CHECK (file_size > 0),
    CONSTRAINT chk_backup_items_chunk_size CHECK (chunk_size > 0),
    CONSTRAINT chk_backup_items_total_chunks CHECK (total_chunks > 0)
);

CREATE TABLE IF NOT EXISTS famora.backup_upload_chunks
(
    id           uuid PRIMARY KEY,
    session_id   uuid                     NOT NULL REFERENCES famora.backup_upload_sessions (id),
    item_id      uuid                     NOT NULL REFERENCES famora.backup_upload_items (id),
    chunk_number integer                  NOT NULL,
    chunk_size   bigint                   NOT NULL,
    sha256       varchar(64),
    storage_path text                     NOT NULL,
    status       varchar(30)              NOT NULL DEFAULT 'ACTIVE',
    created_by   uuid                     NOT NULL REFERENCES famora.users (id),
    updated_by   uuid REFERENCES famora.users (id),
    created_at   timestamp with time zone NOT NULL,
    updated_at   timestamp with time zone NOT NULL,
    CONSTRAINT uk_backup_chunks_item_number UNIQUE (item_id, chunk_number),
    CONSTRAINT chk_backup_chunks_number CHECK (chunk_number > 0),
    CONSTRAINT chk_backup_chunks_size CHECK (chunk_size > 0)
);

CREATE INDEX IF NOT EXISTS idx_backup_sessions_family_status
    ON famora.backup_upload_sessions (family_id, upload_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_backup_items_session_status
    ON famora.backup_upload_items (session_id, item_status);
CREATE INDEX IF NOT EXISTS idx_backup_items_family_status
    ON famora.backup_upload_items (family_id, item_status);
CREATE INDEX IF NOT EXISTS idx_backup_chunks_item_number
    ON famora.backup_upload_chunks (item_id, chunk_number);
