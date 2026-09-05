ALTER TABLE famora.users
  ADD COLUMN backup_quota_override_enabled boolean NOT NULL DEFAULT false,
  ADD COLUMN backup_quota_override_bytes bigint,
  ADD COLUMN backup_storage_used_bytes bigint NOT NULL DEFAULT 0,
  ADD COLUMN backup_storage_reserved_bytes bigint NOT NULL DEFAULT 0;

UPDATE famora.users user_account
SET backup_storage_used_bytes = usage.used_bytes
FROM (
  SELECT item.created_by AS user_id, COALESCE(SUM(item.file_size), 0) AS used_bytes
  FROM famora.backup_upload_items item
  JOIN famora.files file_asset ON file_asset.id = item.file_asset_id
  WHERE item.status = 'ACTIVE'
    AND item.item_status = 'COMPLETED'
    AND file_asset.status = 'ACTIVE'
  GROUP BY item.created_by
) usage
WHERE user_account.id = usage.user_id;

UPDATE famora.users user_account
SET backup_storage_reserved_bytes = usage.reserved_bytes
FROM (
  SELECT item.created_by AS user_id, COALESCE(SUM(item.file_size), 0) AS reserved_bytes
  FROM famora.backup_upload_items item
  JOIN famora.backup_upload_sessions session ON session.id = item.session_id
  WHERE item.status = 'ACTIVE'
    AND session.status = 'ACTIVE'
    AND session.upload_status NOT IN ('COMPLETED', 'CANCELLED')
    AND item.item_status NOT IN ('COMPLETED', 'CANCELLED')
  GROUP BY item.created_by
) usage
WHERE user_account.id = usage.user_id;

ALTER TABLE famora.users
  ADD CONSTRAINT chk_users_backup_quota_override_bytes
    CHECK (backup_quota_override_bytes IS NULL OR backup_quota_override_bytes > 0),
  ADD CONSTRAINT chk_users_backup_storage_used_bytes
    CHECK (backup_storage_used_bytes >= 0),
  ADD CONSTRAINT chk_users_backup_storage_reserved_bytes
    CHECK (backup_storage_reserved_bytes >= 0),
  ADD CONSTRAINT chk_users_backup_quota_override_configuration
    CHECK (
      (backup_quota_override_enabled AND backup_quota_override_bytes IS NOT NULL)
      OR (NOT backup_quota_override_enabled AND backup_quota_override_bytes IS NULL)
    );
