ALTER TABLE famora.users
  ADD COLUMN IF NOT EXISTS family_limit_override_enabled boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS max_family_override integer;

ALTER TABLE famora.users
  ADD CONSTRAINT chk_users_max_family_override_positive
  CHECK (max_family_override IS NULL OR max_family_override > 0);

CREATE TABLE IF NOT EXISTS famora.family_leave_requests (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  family_id uuid NOT NULL REFERENCES famora.families(id),
  requester_user_id uuid NOT NULL REFERENCES famora.users(id),
  request_status varchar(30) NOT NULL DEFAULT 'PENDING',
  reason text,
  review_reason text,
  reviewed_by_user_id uuid REFERENCES famora.users(id),
  reviewed_at timestamptz,
  cancelled_by_user_id uuid REFERENCES famora.users(id),
  cancelled_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_family_leave_requests_pending
ON famora.family_leave_requests(family_id, requester_user_id)
WHERE request_status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_family_leave_requests_family_status
ON famora.family_leave_requests(family_id, request_status);

CREATE INDEX IF NOT EXISTS idx_family_leave_requests_requester_status
ON famora.family_leave_requests(requester_user_id, request_status);

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM pg_proc p
    JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE p.proname = 'set_updated_at'
      AND n.nspname = 'famora'
  ) THEN
    DROP TRIGGER IF EXISTS trg_family_leave_requests_set_updated_at
      ON famora.family_leave_requests;
    CREATE TRIGGER trg_family_leave_requests_set_updated_at
      BEFORE UPDATE ON famora.family_leave_requests
      FOR EACH ROW EXECUTE FUNCTION famora.set_updated_at();
  END IF;
END $$;
