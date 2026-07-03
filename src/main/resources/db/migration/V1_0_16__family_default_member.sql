ALTER TABLE famora.family_members
  ADD COLUMN IF NOT EXISTS is_default boolean;

UPDATE famora.family_members
SET is_default = false
WHERE is_default IS NULL;

WITH ranked_members AS (
  SELECT
    fm.id,
    row_number() OVER (
      PARTITION BY fm.user_id
      ORDER BY fm.joined_at NULLS LAST, fm.created_at, fm.id
    ) AS rn
  FROM famora.family_members fm
  WHERE fm.status = 'ACTIVE'
    AND NOT EXISTS (
      SELECT 1
      FROM famora.family_members existing_default
      WHERE existing_default.user_id = fm.user_id
        AND existing_default.status = 'ACTIVE'
        AND existing_default.is_default = true
    )
)
UPDATE famora.family_members family_member
SET is_default = true
FROM ranked_members
WHERE family_member.id = ranked_members.id
  AND ranked_members.rn = 1;

ALTER TABLE famora.family_members
  ALTER COLUMN is_default SET DEFAULT false,
  ALTER COLUMN is_default SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_family_members_user_default_active
ON famora.family_members(user_id)
WHERE is_default = true
  AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_family_members_user_default
ON famora.family_members(user_id, is_default);
