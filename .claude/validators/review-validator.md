# Review Validator

## Purpose

Validates that a review produced by Reviewer Agent is complete and structured before GATE 4 can pass.

This validator ensures Reviewer produces a real review, not a superficial one.

---

## Trigger

Run this validator after Reviewer Agent produces its review report.

GATE 4 depends on this validator passing AND the review result being APPROVED.

---

## Required Review Output

Reviewer MUST produce a structured report with all of the following.

### Required Fields

| Field                    | Required Content                                     |
| ------------------------ | ---------------------------------------------------- |
| Review Result            | Exactly `APPROVED` or `REJECTED`                     |
| Review Basis             | Base commit and reviewed head commit of the diff     |
| Summary                  | 2–5 sentence evaluation of the overall implementation|
| Issues Found             | Categorized list (Critical / Major / Minor)          |
| Architecture Compliance  | `PASS` or `FAIL` with explanation                    |
| Test Coverage            | `PASS` or `FAIL` with explanation                    |
| Acceptance Criteria      | Each criterion marked MET or NOT MET                 |

---

## Validation Rules

### FAIL conditions (review must be redone)

- Review Result is missing
- Review Result is neither `APPROVED` nor `REJECTED`
- Review Basis is missing or does not name both commits
- Issues Found section is empty when result is REJECTED
- Architecture Compliance verdict is missing
- Test Coverage verdict is missing
- Acceptance Criteria not checked against TASK document
- Review is only positive feedback with no critical analysis
- Review rewrites or modifies code (Reviewer cannot write code)

### PASS conditions

- All required fields present
- Review Result is clearly stated
- Every critical or major issue includes location (file, method) and reason
- Architecture Compliance verdict is justified
- Test Coverage verdict is justified
- Each acceptance criterion evaluated

---

## APPROVED Criteria

A review may only be APPROVED when ALL of the following are true:

- [ ] All acceptance criteria from TASK are MET
- [ ] No CRITICAL issues found
- [ ] No MAJOR issues found
- [ ] Architecture Compliance: PASS
- [ ] Test Coverage: PASS

If any of the above is not satisfied → result must be REJECTED.

---

## REJECTED Criteria

A review must be REJECTED when ANY of the following is true:

- Any CRITICAL issue exists
- Any MAJOR issue exists
- Architecture Compliance: FAIL
- Test Coverage: FAIL
- Any acceptance criterion is NOT MET

---

## Issue Severity Definitions

Reviewer must categorize every issue found.

### Critical
Must fix before approval. No exceptions.
- Security vulnerability
- Broken business logic
- Architecture violation (business logic in Controller, etc.)
- Data corruption risk

### Major
Must fix before approval.
- Missing tests for business logic
- Incorrect implementation of acceptance criterion
- Unhandled exception in critical path
- High cyclomatic complexity in business logic

### Minor
Improvement recommended. Not blocking.
- Naming inconsistency
- Minor readability issue
- Small refactor suggestion
- Unused import

---

## Review Report Format

Reviewer MUST produce output in this format.

```
## Review Result
APPROVED | REJECTED

## Review Basis
- Base commit: [commit of the base branch the diff was taken against]
- Reviewed head commit: [commit of the feature branch that was reviewed]

## Summary
[2-5 sentences]

## Issues Found

### Critical
- [location] [description] [why it matters]

### Major
- [location] [description] [why it matters]

### Minor
- [location] [description] [suggestion]

## Architecture Compliance
PASS | FAIL
[explanation]

## Test Coverage
PASS | FAIL
[explanation]

## Acceptance Criteria

- [ TASK criterion 1 ] → MET | NOT MET
- [ TASK criterion 2 ] → MET | NOT MET
```

---

## Forbidden Reviewer Behaviors

Reviewer MUST NOT:

- Write or rewrite any code
- Propose full reimplementation
- Change architecture directly
- Mark APPROVED when any critical or major issue exists
- Produce a review with no issues found if result is REJECTED (must explain why)

---

## Validation Checklist

- [ ] Review Result is present (`APPROVED` or `REJECTED`)
- [ ] Review Basis names the base commit and the reviewed head commit
- [ ] Summary is substantive (not empty)
- [ ] Issues Found section categorized (Critical / Major / Minor)
- [ ] Architecture Compliance verdict present with explanation
- [ ] Test Coverage verdict present with explanation
- [ ] All TASK acceptance criteria evaluated
- [ ] If REJECTED: at least one Critical or Major issue documented
- [ ] If APPROVED: no Critical or Major issues present
- [ ] No code written by Reviewer

---

## On REJECTED

1. Return structured review report to Implementer
2. Implementer addresses all Critical and Major issues
3. Implementer updates TASK status back to IN_REVIEW
4. Re-run code-validator (GATE 3)
5. Reviewer re-reviews

After 3 consecutive REJECTED results → escalate to user.

---

## Review Input

The review is performed before the Pull Request is created.

Reviewer reviews the diff between the base branch and the feature branch and records both commits in `## Review Basis`.

---

## After APPROVED

1. TASK status remains `IN_REVIEW`. Reviewer does not set `DONE`.
2. The Pull Request is created. It is never created before APPROVED.
3. After the Pull Request has actually been created, the work owner after PR creation updates the TASK file:

```
Status: DONE
```

`DONE` means that the Pull Request has been created. It does not mean that it has been merged.

---

## After the Pull Request Is Created

The user compares the reviewed head commit with the final HEAD of the Pull Request.

The earlier APPROVED is not relied on, and the change is reviewed again, when either of the following is true:

- the difference contains anything other than the status change of that TASK
- changes on the base branch could affect the review result
