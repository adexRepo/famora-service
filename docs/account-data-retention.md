# Account deletion data-retention matrix

| Module/data | Private data | Shared family/business data | Deletion behavior |
|---|---|---|---|
| Vault | `PRIVATE` items created by the user | Family-visible items | Private rows are hard-deleted; shared rows remain with an anonymized creator. |
| Notes | `PRIVATE` notes created by the user | Family-visible notes | Private rows are hard-deleted; shared rows remain with an anonymized creator. |
| Documents/files | User-owned documents and `PRIVATE`/`OWNER_ONLY` files | Family-visible files | Private rows are hard-deleted. Physical objects are queued in `storage_deletion_outbox`. |
| Backup | `PRIVATE`/`OWNER_ONLY` sessions created by the user, items, and chunks | Family-visible completed backups | Private rows are hard-deleted. Final objects and temporary chunks are queued for deletion. |
| Tracker | `PRIVATE` trackers owned by the user, logs, and notifications | Family/business trackers | Private graph is hard-deleted; shared trackers remain with the deleted user identity anonymized. |
| Finance | Transactions, debts, and payments created by the user | None distinguishable in the current schema | Creator-owned finance rows are hard-deleted to favor privacy. Related debt/payment rows are removed first. |
| Audit | IP address, user agent, and metadata | Minimal event identity | PII is removed while a minimal event and anonymized user reference are retained. |
| Storage cleanup | MinIO objects, local files, temporary chunks | Shared objects | Transactional outbox with row claiming and retry; completed tasks remain as operational evidence. |
