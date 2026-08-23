# Security operations

## Vault encryption key

`VAULT_ENCRYPTION_KEY` is a standard Base64 AES key (32 random bytes recommended). Store it in
the deployment platform's managed secret store or KMS-backed secret integration. Never commit it,
print it, or pass it through ordinary application logs.

Rotation is online and backward-compatible:

1. Back up the database and verify the backup can be restored.
2. Generate a new 32-byte key in the managed secret store.
3. Set `VAULT_ENCRYPTION_KEY` to the new key and add the old key to the comma-separated
   `VAULT_PREVIOUS_ENCRYPTION_KEYS` secret.
4. Deploy and verify that an existing vault item can be revealed and a newly written item can be
   revealed after a restart.
5. The scheduled rotation worker rewrites up to 100 legacy values every five minutes. Keep the
   previous key configured until this query returns zero:

   ```sql
   SELECT count(*)
   FROM famora.vault_items
   WHERE encrypted_secret NOT LIKE 'v2:<active-key-id>:%';
   ```

   The active key ID is the 16-character value encoded only as part of the ciphertext format; it is
   the first eight bytes of the key's SHA-256 digest and is not key material. It can be read from a
   newly written ciphertext. Never place the key itself in SQL or logs.
6. Remove the previous key in a later deployment and verify again.

The current design is server-side encryption at rest. It is not zero-knowledge or end-to-end
encryption. Compromise of all configured application keys together with the database exposes vault
plaintext. Per-family envelope encryption backed by KMS remains the longer-term isolation boundary.

## WebSocket tickets

Browser-compatible WebSocket handshakes use a random, single-use ticket with a 30-second lifetime;
access and refresh tokens must never be placed in the WebSocket URL. Configure reverse proxies and
APM tooling to redact the `ticket` query parameter from access logs. The STOMP `CONNECT` frame still
performs the authoritative authentication and rejects missing, expired, consumed, or inactive-user
tickets.
