# Code Validator

## Purpose

Validates that code produced by Implementer Agent meets quality, architecture, and completeness requirements before Reviewer Agent can start.

Code that fails this validator must be fixed by Implementer before GATE 3 passes.

---

## Trigger

Run this validator after Implementer completes source code and tests.

GATE 3 depends on this validator passing.

---

## Validation Categories

### Category 1 — Completeness

All items in TASK `## Tasks` section must be implemented.

- [ ] Every task item has corresponding code
- [ ] Every task item has corresponding test
- [ ] All `## Acceptance Criteria` are addressed

### Category 2 — Architecture Compliance

Code must match DESIGN document exactly.

- [ ] Package structure matches `## Package Structure` in DESIGN
- [ ] API endpoints match `## API Design` in DESIGN (path, method, request, response)
- [ ] Entity fields match `## Data Model` in DESIGN
- [ ] No additional endpoints added without DESIGN approval
- [ ] No layer violations (Controller must not contain business logic)

### Category 3 — Coding Standards

- [ ] Constructor injection only (no `@Autowired` field injection)
- [ ] No business logic in Controller layer
- [ ] Service layer contains business logic
- [ ] Repository layer handles only persistence
- [ ] DTO used for API contracts (no domain objects in response)
- [ ] Global exception handler (`@RestControllerAdvice`) exists or referenced
- [ ] No silent exception catching without logging

### Category 4 — Test Requirements

- [ ] Unit tests exist for all Service methods with business logic
- [ ] Controller tests exist for all endpoints
- [ ] Tests use meaningful assertions (not just `assertNotNull`)
- [ ] External dependencies are mocked in unit tests
- [ ] Tests do not test framework behavior (Spring handles that)

### Category 5 — Java 17 Standards

- [ ] No deprecated APIs used
- [ ] Records used for immutable DTOs where appropriate
- [ ] No unnecessary null checks on framework-managed objects

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

CRITICAL or MAJOR violations → must fix before GATE 3 passes.

MINOR violations → fix recommended but not blocking.

---

## Architecture Violation Detection

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
