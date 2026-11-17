# Code Validator

## Purpose

Validates that code produced by Implementer Agent meets quality, architecture, and completeness requirements before Reviewer Agent can start.

Code that fails this validator must be fixed by Implementer before GATE 3 passes.

---

## Trigger

Run this validator after Implementer completes source code and tests.

GATE 3 depends on this validator passing.

---

## Scope by Service

| Service | Categories applied |
| ------- | ------------------ |
| Spring service (`src/`) | Category 1, 2, 3, 4, 5 and Architecture Violation Detection |
| Python service (`services/<name>/`) | Category 1, 2 and Category 6 |

### GATE 3 "Source code compiles" for Python services

- Baseline: syntax check of all source and test files passes, and the automated tests pass.
- The exact scope of import and startup verification is defined in that service's DESIGN. This validator does not require importing every module unconditionally.

---

## Validation Categories

### Category 1 — Completeness

All items in TASK `## Requirements` → `### Functional` section must be implemented.

- [ ] Every functional requirement has corresponding code
- [ ] Every functional requirement has corresponding test
- [ ] All `## Acceptance Criteria` are addressed

### Category 2 — Architecture Compliance

Code must match DESIGN document exactly.

- [ ] Package structure matches `## Package Structure` in DESIGN
- [ ] API endpoints match `## API Design` in DESIGN (path, method, request, response)
- [ ] Entity or Model fields match `## Data Model` in DESIGN
- [ ] No additional endpoints added without DESIGN approval
- [ ] No layer violations as defined in DESIGN (Spring: Controller must not contain business logic)

### Category 3 — Coding Standards (Spring service)

- [ ] Constructor injection only (no `@Autowired` field injection)
- [ ] No business logic in Controller layer
- [ ] Service layer contains business logic
- [ ] Repository layer handles only persistence
- [ ] DTO used for API contracts (no domain objects in response)
- [ ] Global exception handler (`@RestControllerAdvice`) exists or referenced
- [ ] No silent exception catching without logging

### Category 4 — Test Requirements (Spring service)

- [ ] Unit tests exist for all Service methods with business logic
- [ ] Controller tests exist for all endpoints
- [ ] Tests use meaningful assertions (not just `assertNotNull`)
- [ ] External dependencies are mocked in unit tests
- [ ] Tests do not test framework behavior (Spring handles that)

### Category 5 — Java 17 Standards (Spring service)

- [ ] No deprecated APIs used
- [ ] Records used for immutable DTOs where appropriate
- [ ] No unnecessary null checks on framework-managed objects

---

### Category 6 — Python Common Standards (Python service)

Structure, module layout and the exception handling mechanism follow the service's DESIGN. This category does not require a specific structure.

Input validation
- [ ] External input is validated against a schema at the entry boundary
- [ ] Size and format limits defined in DESIGN are enforced
- [ ] Invalid input is rejected with the 4xx status code defined in DESIGN and a message that identifies the cause

Error handling
- [ ] No exception is silently swallowed
- [ ] Client input errors, dependency errors and timeouts are distinguished and returned as defined in DESIGN
- [ ] Error responses do not expose stack traces, secrets or internal paths

External dependency substitution
- [ ] External dependencies (AI models, other services) sit behind a replaceable boundary
- [ ] Automated tests replace external dependencies with fake implementations

Tests
- [ ] Tests use pytest
- [ ] Automated tests run without external services, real AI or secrets (FastAPI's local test client is allowed)
- [ ] Normal, invalid input, dependency failure and timeout cases are tested deterministically
- [ ] Checks against a real AI are separate tests that run only when explicitly invoked, and are excluded from the default automated test run
- [ ] Test input data such as logs is hand-made

Types and dependencies
- [ ] Public functions and models have type hints
- [ ] Dependency versions are pinned in a lock file
- [ ] No secret in code or in the repository

---

## FAIL Conditions (fix required)

| Violation                                       | Severity |
| ----------------------------------------------- | -------- |
| Missing tests for business logic                | CRITICAL |
| Layer violation (business logic in Controller)  | CRITICAL |
| API endpoint not matching DESIGN                | CRITICAL |
| Field injection used                            | MAJOR    |
| Domain object returned directly in API response | MAJOR    |
| Silent exception catch without logging          | MAJOR    |
| Missing test assertions (empty test body)       | MAJOR    |
| Unused imports or dead code                     | MINOR    |
| (Python) Secret in code or repository           | CRITICAL |
| (Python) Automated test depends on external service, real AI or secret | MAJOR |
| (Python) Missing dependency failure or timeout test | MAJOR |
| (Python) Exception silently swallowed           | MAJOR    |
| (Python) Input not validated at entry boundary  | MAJOR    |
| (Python) Real AI check included in default automated test run | MAJOR |
| (Python) Missing type hints on public functions | MINOR    |

CRITICAL or MAJOR violations → must fix before GATE 3 passes.

MINOR violations → fix recommended but not blocking.

---

## Architecture Violation Detection

Applies to the Spring service. For Python services, check against the structure defined in DESIGN.

Check each file category:

### Controller
- Must have `@RestController` or `@Controller`
- Must only call Service methods
- Must not contain `if/else` business logic
- Must not access Repository directly

### Service
- Must have `@Service`
- Contains business logic
- May call Repository
- Must not build HTTP responses

### Repository
- Must extend Spring Data interface or use `@Repository`
- Must not contain business logic

### DTO
- Used in Controller method signatures
- Not used in Repository queries
- Should be immutable (Record or final fields)

---

## Validation Checklist

Run through before marking GATE 3 passed.

**Completeness**
- [ ] All TASK items implemented
- [ ] All acceptance criteria addressed

**Architecture**
- [ ] Package structure matches DESIGN
- [ ] API matches DESIGN spec
- [ ] Data model matches DESIGN spec
- [ ] No unauthorized additions

**Standards**
- [ ] Constructor injection everywhere
- [ ] No business logic in Controller
- [ ] DTOs used for API contract
- [ ] Exception handling present

**Tests**
- [ ] Unit tests for all Service logic
- [ ] Controller tests for all endpoints
- [ ] Meaningful assertions

**Python service**
- [ ] Syntax check and automated tests pass
- [ ] Import and startup verification done as defined in DESIGN
- [ ] Category 6 has no CRITICAL or MAJOR violation

---

## TASK Status Update

After passing validation:

Update TASK file:

```
Status: IN_REVIEW
```

---

## On Failure

If validation fails:

1. List all violations by severity
2. Return to Implementer with specific locations and reasons
3. Implementer fixes violations
4. Re-run this validator

Do not proceed to Reviewer until GATE 3 passes.
