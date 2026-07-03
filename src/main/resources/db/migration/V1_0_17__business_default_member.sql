ALTER TABLE famora.business_members
  ADD COLUMN IF NOT EXISTS is_default boolean;

UPDATE famora.business_members
SET is_default = false
WHERE is_default IS NULL;

WITH ranked_members AS (
  SELECT
    bm.id,
    row_number() OVER (
      PARTITION BY bm.user_id
      ORDER BY bm.joined_at NULLS LAST, bm.created_at, bm.id
    ) AS rn
  FROM famora.business_members bm
  WHERE bm.status = 'ACTIVE'
    AND NOT EXISTS (
      SELECT 1
      FROM famora.business_members existing_default
      WHERE existing_default.user_id = bm.user_id
        AND existing_default.status = 'ACTIVE'
        AND existing_default.is_default = true
    )
)
UPDATE famora.business_members business_member
SET is_default = true
FROM ranked_members
WHERE business_member.id = ranked_members.id
  AND ranked_members.rn = 1;

ALTER TABLE famora.business_members
  ALTER COLUMN is_default SET DEFAULT false,
  ALTER COLUMN is_default SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_business_members_user_default_active
ON famora.business_members(user_id)
WHERE is_default = true
  AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_business_members_user_default
ON famora.business_members(user_id, is_default);
