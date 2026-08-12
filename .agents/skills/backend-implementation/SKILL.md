---
name: backend-implementation
description: "Implement or modify Famora backend behavior, including features, bug fixes, business rules, integrations, and focused refactoring. Use for Java or Spring Boot application changes that require tracing a request or event flow and preserving module contracts."
---

# Backend implementation

Follow the root `AGENTS.md` and the owning feature's current patterns.

1. Inspect the relevant controller, DTOs, service, entities, repositories, specifications, migrations, audit path, and tests.
2. Trace the complete request or event flow, including family/business context, current user, transaction boundary, external effects, and response mapping.
3. State the business invariant being changed and identify the smallest safe implementation.
4. Check authentication, role/ownership, family/business isolation, IDOR, sensitive output, and audit requirements.
5. Implement inside the owning feature package. Preserve public contracts unless the task explicitly changes them.
6. Add a forward Flyway migration only when persistence changes.
7. Add meaningful tests for the rule, authorization boundary, regression, and important failure case.
8. Run focused checks, then broader compilation/tests in proportion to impact.
9. Report affected files, contract changes, assumptions, migrations, and checks actually run.

Keep KISS, YAGNI, and pragmatic DRY. Avoid speculative abstractions, unrelated refactoring, new dependencies without need, giant services, and cross-module leakage.
