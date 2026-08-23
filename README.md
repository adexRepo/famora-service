# Famora Service

Famora Service is a Java 21 / Spring Boot backend using PostgreSQL, Flyway, JWT,
MinIO, and AES-GCM encryption for Vault data.

## Configuration

The application uses one profile-agnostic `application.yml`. Runtime differences are supplied
through environment variables; deployment values are owned by the Jenkins shared library. The
application intentionally fails fast when required credentials or cryptographic keys are missing.

### Required environment variables

| Variable | Requirement | Notes |
| --- | --- | --- |
| `DB_URL` | Required | PostgreSQL JDBC URL, including the `famora` schema configuration. |
| `DB_USERNAME` | Required | Dedicated database role; do not use the PostgreSQL superuser. |
| `DB_PASSWORD` | Required | Database password from the deployment secret store. |
| `JWT_SECRET` | Required | Standard Base64 that decodes to at least 32 random bytes. |
| `VAULT_ENCRYPTION_KEY` | Required | Standard Base64 that decodes to exactly 16, 24, or 32 bytes. Use 32 bytes for new environments. |
| `MINIO_ENDPOINT` | Required | MinIO endpoint. Use HTTPS outside trusted internal networks. |
| `MINIO_ACCESS_KEY` | Required | Dedicated MinIO access key. |
| `MINIO_SECRET_KEY` | Required | MinIO secret key from the deployment secret store. |
| `MINIO_BUCKET` | Required | Existing or application-managed bucket name. |
| `ACCOUNT_DELETION_SUPPORT_EMAIL` | Release-required | Monitored address shown on the public `/api/v1/account-deletion` page. The page returns `500` when it is absent. |
| `CORS_ALLOWED_ORIGINS` | Web-release-required | Comma-separated HTTPS origins allowed for REST and WebSocket browser clients. |

Optional operational variables are documented by their placeholders in
`src/main/resources/application.yml`, including server port, pool sizing, upload limits, token
lifetimes, storage paths, and external API settings. `JWT_ISSUER` defaults to the non-secret
identifier `famoraApp`.

### Generate cryptographic secrets

Generate values on an administrator workstation or through the infrastructure secret manager. Do
not paste generated output into source files, tickets, chat, or deployment logs.

OpenSSL:

```bash
openssl rand -base64 32
```

PowerShell with .NET cryptographic randomness:

```powershell
$keyBytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($keyBytes)
[Convert]::ToBase64String($keyBytes)
$rng.Dispose()
```

Run the command independently for `JWT_SECRET` and `VAULT_ENCRYPTION_KEY`; never reuse one value for
both purposes.

## Local development

Start PostgreSQL and MinIO locally, generate two temporary development-only keys, and export the
complete runtime configuration in the current shell:

```powershell
function New-FamoraSecret {
  $keyBytes = New-Object byte[] 32
  $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
  $rng.GetBytes($keyBytes)
  $rng.Dispose()
  [Convert]::ToBase64String($keyBytes)
}

$env:DB_URL = 'jdbc:postgresql://localhost:5432/postgres?currentSchema=famora'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = 'postgres'
$env:JWT_SECRET = New-FamoraSecret
$env:VAULT_ENCRYPTION_KEY = New-FamoraSecret
$env:MINIO_ENDPOINT = 'http://localhost:9000'
$env:MINIO_ACCESS_KEY = 'minioadmin'
$env:MINIO_SECRET_KEY = 'minioadmin123'
$env:MINIO_BUCKET = 'famora'
mvn spring-boot:run
```

Adjust the database and MinIO variables when your setup differs. Shell environment values are not
persisted in the repository. Tests that start a Spring application context must supply temporary
runtime values in the test process; focused unit tests that do not create the application context
need no infrastructure credentials.

## Staging and production deployment

1. Store every required value in the platform secret manager with separate values per environment.
2. Inject secrets as environment variables at runtime; do not bake them into an image or manifest.
3. Use a least-privilege PostgreSQL role and dedicated MinIO credentials.
4. Restrict access to environment inspection, process metadata, logs, and deployment configuration.
5. Deploy first to staging and confirm startup fails when each required secret is intentionally
   omitted from a validation deployment.
6. Confirm production logs remain at INFO or higher, Hibernate SQL output is disabled, and error
   responses do not include exception details or stack traces.

There is no Docker, Compose, or CI/CD deployment definition in this repository. Jenkins generates
the runtime env file from
`resources/deployment/<environment>/famora-service.yaml` in the shared library; that file is the
environment-specific configuration source of truth.

## Vault key lifecycle

`VAULT_ENCRYPTION_KEY` protects existing Vault ciphertext and is not interchangeable with the JWT
secret. Back it up through the approved secret-management process. Losing it makes existing Vault
data unreadable.

Do not rotate the Vault key by simply replacing the environment variable. Set the new key as
`VAULT_ENCRYPTION_KEY` and keep the old key in `VAULT_PREVIOUS_ENCRYPTION_KEYS`. Ciphertext carries a
non-secret key ID and the scheduled worker re-encrypts legacy values in bounded batches. Follow
`docs/security-operations.md` and do not retire a previous key until the documented database scan
returns zero.

Vault encryption remains `AES/GCM/NoPadding` with a fresh 12-byte IV and a 128-bit authentication
tag for each encryption operation.

## Verification

```powershell
mvn test
mvn -DskipTests compile
git diff --check
```
