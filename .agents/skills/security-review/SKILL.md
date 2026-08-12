---
name: security-review
description: "Review Famora authentication, authorization, tenant isolation, sensitive data, public APIs, file handling, WebSocket security, security configuration, and abuse resistance. Use for explicit security audits or security-sensitive code changes."
---

# Security review

Trace a realistic caller-controlled path from transport input to data or side effect. Report only findings with a plausible execution path.

Review:

1. Authentication bypass, including the current filter-chain `permitAll` behavior and code paths that do not require `CurrentUserProvider`.
2. Broken access control, IDOR, family/business isolation, creator/owner semantics, role escalation, and default-context switching.
3. Mass assignment, unsafe enum/JSON handling, injection, unsafe deserialization, and client-controlled ownership/audit fields.
4. Vault secrets, tokens, personal data, documents, financial payloads, logs, errors, WebSocket payloads, and cache exposure.
5. Upload size, signature/MIME validation, filename and object-key safety, path traversal, archive risks, checksums, download authorization, and temp cleanup.
6. SSRF and trust boundaries for external URLs or object-storage configuration.
7. Brute force, rate abuse, replay, duplicate submission, idempotency, race conditions, and database-enforced invariants.

Order findings by severity. For each finding include location, precondition, concrete attack/failure sequence, impact, and smallest effective correction. Separate confirmed defects from residual risks and missing tests. Avoid generic hardening advice that is not tied to reachable code.
