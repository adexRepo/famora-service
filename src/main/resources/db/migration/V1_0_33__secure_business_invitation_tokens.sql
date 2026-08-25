ALTER TABLE famora.business_invitations
    RENAME COLUMN invitation_code TO invitation_code_hash;

ALTER TABLE famora.business_invitations
    ALTER COLUMN invitation_code_hash TYPE varchar(64);

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $migration$
DECLARE
    pgcrypto_schema text;
BEGIN
    SELECT namespace.nspname
      INTO pgcrypto_schema
      FROM pg_catalog.pg_extension extension
      JOIN pg_catalog.pg_namespace namespace
        ON namespace.oid = extension.extnamespace
     WHERE extension.extname = 'pgcrypto';

    IF pgcrypto_schema IS NULL THEN
        RAISE EXCEPTION 'Required PostgreSQL extension pgcrypto is not installed';
    END IF;

    EXECUTE format(
        'UPDATE famora.business_invitations '
        'SET invitation_code_hash = encode(%I.digest(convert_to(trim(invitation_code_hash), ''UTF8''), ''sha256''::text), ''hex'') '
        'WHERE invitation_code_hash !~ ''^[0-9a-f]{64}$''',
        pgcrypto_schema
    );
END
$migration$;

ALTER TABLE famora.business_invitations
    RENAME CONSTRAINT ux_business_invitations_invitation_code
    TO ux_business_invitations_invitation_code_hash;
