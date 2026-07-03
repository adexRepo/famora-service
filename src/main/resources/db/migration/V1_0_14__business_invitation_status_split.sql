ALTER TABLE famora.business_invitations
  ADD COLUMN IF NOT EXISTS invitation_status varchar(30);

UPDATE famora.business_invitations
SET invitation_status = CASE
  WHEN status IN ('PENDING', 'ACCEPTED', 'CANCELLED', 'EXPIRED') THEN status
  ELSE 'PENDING'
END
WHERE invitation_status IS NULL;

UPDATE famora.business_invitations
SET status = 'ACTIVE'
WHERE status IS NULL
  OR status IN ('PENDING', 'ACCEPTED', 'CANCELLED', 'EXPIRED');

ALTER TABLE famora.business_invitations
  ALTER COLUMN invitation_status SET DEFAULT 'PENDING',
  ALTER COLUMN invitation_status SET NOT NULL,
  ALTER COLUMN status SET DEFAULT 'ACTIVE',
  ALTER COLUMN status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_business_invitations_business_id_invitation_status
ON famora.business_invitations(business_id, invitation_status);
