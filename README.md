# Famora Service MVP 1 Skeleton

Spring Boot biasa (bukan Spring Modulith) untuk Famora MVP 1.

Included:
- PostgreSQL Flyway migration V1
- JPA entities
- Repository interfaces
- Security/JWT setup
- Auth service/controller
- User service/controller
- Family service/controller
- Family context via `X-Family-Id`
- AuditLog service
- Ping endpoint

Recommended run order:
1. Copy files into your `famora-service` repo.
2. Update package name if needed.
3. Put JWT secret in env/application config.
4. Run Flyway migration.
5. Test `/api/v1/ping`, `/api/v1/auth/register`, `/api/v1/auth/login`.

Important:
- Use `spring.jpa.hibernate.ddl-auto=validate`.
- PostgreSQL should not be exposed public.
- Vault secret must be encrypted before saving to DB.
