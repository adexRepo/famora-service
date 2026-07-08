ALTER TABLE famora.users
    ADD COLUMN IF NOT EXISTS date_of_birth date;

ALTER TABLE famora.businesses
    ADD COLUMN IF NOT EXISTS address text,
    ADD COLUMN IF NOT EXISTS contact varchar(80);
