---
name: code-review
description: "Review Famora code changes, pull requests, commits, or modules to identify defects, security risks, business-rule violations, regressions, performance issues, and missing tests. Use when the user asks for a review or risk assessment."
---

# Code review

Inspect the diff and enough surrounding code to trace affected execution paths. Review in this order:

1. Correctness and externally visible behavior.
2. Authentication, authorization, tenant isolation, and sensitive data.
3. Business rules and workflow state transitions.
4. Data integrity, transactions, idempotency, and concurrency.
5. Backward compatibility and regressions.
6. Query count, pagination, external calls, storage, cache, and performance.
7. Maintainability and module boundaries.
8. Missing or weak tests.

Lead with findings ordered by severity. Each meaningful finding must include severity, precise file/line location, problem, realistic impact, and recommended correction. Do not report style preferences unless they create a concrete maintenance or defect risk.

If no defects are found, say so clearly and list only material residual risks or verification gaps. Keep summaries secondary to findings.
