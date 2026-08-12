---
name: object-storage
description: "Implement or review Famora MinIO, local/MFT storage, file assets, document uploads, daily-report photos, downloads, previews, backup sessions, resumable chunks, checksums, metadata, and cleanup. Use for any file or object-storage workflow."
---

# Object storage

1. Trace controller limits, service validation, file-asset persistence, storage operation, authorization, audit, download, and cleanup.
2. Preserve family/business scope in both database lookups and object keys. Never use possession of a file ID, bucket, key, or path as authorization.
3. Enforce servlet and domain-specific size limits. Reject empty files and inspect magic bytes; do not trust client MIME type or extension alone.
4. Sanitize original names, generate server-side stored names, normalize paths, reject traversal, and ensure local targets remain under the configured root.
5. Preserve original/resolved MIME, extension, size, SHA-256, file type, storage type, bucket/key/path, and ownership metadata without exposing internal paths publicly.
6. Keep storage/network calls outside long database transactions where practical. Design cleanup or reconciliation for storage success followed by database failure and vice versa.
7. For resumable backup, isolate temp directories by family/session/item, validate chunk number/size/checksum, make retries idempotent, assemble in deterministic order, verify final size/hash, and clean temp data after completion or cancellation.
8. Authorize preview/download/delete separately and audit sensitive view/download actions without logging file contents or secrets.
9. Test path traversal, spoofed MIME, oversized payloads, cross-family access, duplicate chunks, checksum mismatch, concurrent completion/cancel, partial failure, and cleanup.

Do not add a second storage abstraction or provider unless the requirement needs it. Follow `StorageService`, `FileService`, and backup session conventions already present.
