ALTER TABLE famora.family_invitations
    ALTER COLUMN invite_code TYPE varchar(64);

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- pgcrypto may already be installed in public or in the application's schema.
-- Resolve its actual schema instead of relying on the connection search_path.
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
        'UPDATE famora.family_invitations '
        'SET invite_code = encode(%I.digest(convert_to(upper(trim(invite_code)), ''UTF8''), ''sha256''::text), ''hex'') '
        'WHERE invite_code !~ ''^[0-9a-f]{64}$''',
        pgcrypto_schema
    );
END
$migration$;
