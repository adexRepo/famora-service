---
name: api-design
description: "Design, add, or modify Famora HTTP APIs, request/response DTOs, pagination, filtering, sorting, HTTP semantics, and external error contracts. Use whenever an endpoint or API-visible field or behavior changes."
---

# API design

1. Inspect neighboring controllers, DTOs, services, frontend-facing contracts, and exception handlers.
2. Preserve `/api/v1`, `ApiResponse<T>`, `PageResponse<T>`, and module route conventions unless an explicit migration is approved.
3. Define the authenticated caller, allowed role/ownership, family or business scope, and resource lookup predicate before implementing.
4. Use request/response DTOs and Jakarta Bean Validation. Prevent mass assignment by mapping only intentional fields.
5. Use correct HTTP method and status. Keep create, update, delete, workflow action, reveal, and download semantics distinct.
6. Paginate potentially large collections, cap page size, whitelist sort fields, and make optional filters composable with specifications where suitable.
7. Keep list payloads minimal for sensitive or large data; return full content only from authorized detail/reveal/download operations.
8. Preserve backward compatibility when practical. If a breaking change is required, identify every affected endpoint and client behavior.
9. Return stable, safe errors through the established exception handler. Do not leak internal exception, SQL, or storage details.
10. Test validation, authentication, authorization/isolation, response shape, status codes, pagination, and compatibility cases.

When handing off a contract, include route, headers, query/path parameters, request body, response body, error codes, defaults, and idempotency behavior.
