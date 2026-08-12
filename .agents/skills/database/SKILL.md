---
name: database
description: "Change or review Famora PostgreSQL schema, Flyway migrations, JPA entities, repositories, specifications, custom queries, indexes, transactions, concurrency, and database performance. Use for any persistence contract or query behavior change."
---

# Database work

1. Inspect the latest Flyway migrations and the historical definition of every affected table, constraint, trigger, and index.
2. Inspect entity inheritance, mappings, repositories/specifications, service transactions, and actual read/write predicates.
3. Design the smallest forward-compatible change for existing production data.
4. Add a new forward migration using the next repository version. Never modify a deployed migration or depend on manual production SQL.
5. Preserve `famora` schema qualification, referential integrity, audit/status columns, nullability, and application enum conventions.
6. Use database uniqueness or locking when an invariant can race. Explain the concurrency model; do not assume a pre-save existence check is atomic.
7. Add indexes only for demonstrated filters, joins, uniqueness, or ordering. Consider write cost and index selectivity.
8. Prefer derived queries/specifications for ordinary composable reads. Use JPQL/native SQL for justified aggregation, union, projection, or performance needs and bind every value.
9. Check N+1 behavior, lazy loading with Open Session in View disabled, pagination, query count, and unbounded data risks.
10. Verify compilation/tests and, when an accessible database is available, Flyway migration and repository behavior. Report when migration execution could not be tested.

Do not add database enum/check constraints merely to duplicate application enums unless a concrete integrity requirement warrants them.
