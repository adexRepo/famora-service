ALTER TABLE famora.users
  ADD COLUMN role varchar(30) NOT NULL DEFAULT 'USER';

ALTER TABLE famora.users
  ADD CONSTRAINT chk_users_role
  CHECK (role IN ('USER', 'ADMIN'));
