---
name: testing
description: "Create, modify, review, or plan Famora backend tests for business rules, authorization, regressions, transactions, edge cases, repositories, APIs, and integration boundaries. Use whenever verification or test coverage is part of the task."
---

# Backend testing

1. Identify the externally meaningful rule or regression and its failure modes.
2. Choose the narrowest useful level: JUnit unit test for pure logic, repository test for mappings/queries/constraints, MVC test for transport/security, or Spring integration test for transaction and module behavior.
3. Cover the happy path plus meaningful invalid state, authorization/tenant isolation, boundary values, and regression case.
4. Verify family and business resources cannot be read or mutated with another tenant's IDs.
5. Test transaction behavior where partial writes, after-commit publishing, async audit, or concurrency affects correctness.
6. Stub external HTTP/storage boundaries deterministically; do not call live external services from normal tests.
7. Assert public behavior and persisted state rather than private methods or incidental call order.
8. Reuse builders/fixtures only when they remove meaningful repetition without hiding the scenario.
9. Run the focused test, then `mvn test` when the change can affect shared behavior.

Avoid excessive mocking, tests for trivial accessors, sleeps for asynchronous behavior, and coverage-only assertions. State any database, MinIO, or environment limitation that prevented a test from running.
