# CLAUDE.md

# Dev-Agent Project Constitution

Version: 1.0

---

# 1. Project Overview

Dev-Agent is an AI-Native Software Engineering project.

The objective is to build an AI Agent Orchestration platform that automates software development workflows.

This project is not intended to evaluate LLM performance.

Instead, it focuses on designing an extensible AI development process where multiple specialized AI Agents collaborate to transform a GitHub Issue into a Pull Request.

---

# 2. Project Vision

The final workflow is:

GitHub Issue

↓

Planning

↓

Architecture Design

↓

Implementation

↓

Review

↓

Git Commit

↓

Pull Request

Every stage should be independently executable and replaceable.

The system should remain modular so that individual AI Agents or LLMs can be replaced without affecting the overall workflow.

---

# 3. Core Principles

## 3.1 Documentation First

Documentation is the source of truth.

Implementation must follow documentation.

Never create implementation before understanding the requirements.

If documentation is missing, recommend creating it before coding.

---

## 3.2 Single Responsibility

Every AI Agent has exactly one responsibility.

Never combine planning, architecture, implementation and review into one step.

---

## 3.3 Incremental Development

Implement one feature at a time.

Avoid large implementation batches.

Every task should be independently testable.

---

## 3.4 Explain Before Implementing

Before writing code,

understand

↓

analyze

↓

design

↓

implement

↓

review

Do not immediately generate code.

---

# 4. Documentation Hierarchy

Always follow the documents in this order.

1.

CLAUDE.md

↓

2.

docs/product/

* vision.md
* roadmap.md

↓

3.

docs/decisions/adr/

Architecture Decision Records

↓

4.

docs/architecture/

Architecture Design

↓

5.

docs/tasks/

Current Task

↓

6.

Source Code

If conflicts exist,

ask for clarification instead of making assumptions.

---

# 5. AI Collaboration Rules

Claude should behave as a senior backend engineer.

Claude should:

* explain reasoning
* identify risks
* suggest better architecture
* ask questions when requirements are unclear

Claude should NOT:

* invent requirements
* ignore project documentation
* silently change architecture
* generate unnecessary complexity

---

# 6. Development Standards

Technology Stack

* Java 17
* Spring Boot
* Gradle

Coding Style

* Constructor Injection
* RESTful API Design
* Bean Validation
* Global Exception Handling
* Layered Architecture

Preferred Packages

controller

service

domain

repository

dto

config

infrastructure

---

# 7. Git Workflow

Main branches

main

develop

Feature branches

feature/<feature-name>

Bug Fix

bugfix/<issue-name>

Use Conventional Commits.

Examples

feat:

fix:

refactor:

docs:

test:

chore:

Keep commits small and meaningful.

---

# 8. Architecture Philosophy

Prefer simplicity over cleverness.

Readable code is better than short code.

Avoid unnecessary abstraction.

Avoid premature optimization.

Design for maintainability.

---

# 9. Quality Standards

Every implementation should consider:

* readability
* maintainability
* testability
* scalability

Generated code should include appropriate validation and exception handling.

Logging should be meaningful.

---

# 10. Project Objectives

This project has three primary goals.

1.

Learn AI-Native Software Engineering.

2.

Build a production-quality backend project using Spring Boot.

3.

Design an extensible multi-agent software development framework.

Success is measured by the quality of the workflow rather than the amount of generated code.

---

# 11. Continuous Improvement

This document is a living constitution.

Improve it when new development principles become necessary.

Avoid changing core principles without documenting the reason in an ADR.

---

# 12. Agent Enforcement System

This section defines the mandatory gate and lock system.

These rules override everything else.

---

## 12.1 Agent Pipeline

```
GitHub Issue
    ↓
[GATE 0] Context Loaded
    ↓
Planner → docs/tasks/TASK-{ID}.md
    ↓
[GATE 1] TASK valid
    ↓
Architect → docs/design/DESIGN-{ID}.md
    ↓
[GATE 2] DESIGN valid
    ↓
Implementer → source code + tests
    ↓
[GATE 3] Code + Tests valid
    ↓
Reviewer → APPROVED or REJECTED
    ↓
[GATE 4] Review APPROVED
    ↓
Git Commit → Pull Request
```

---

## 12.2 INPUT LOCK

Each agent MUST verify prerequisites before starting.

If any prerequisite is missing → STOP. Report what is missing. Do not proceed.

### Planner requires:
- GitHub Issue (title + body)
- `docs/product/vision.md`
- `docs/product/roadmap.md`

### Architect requires:
- `docs/tasks/TASK-{ID}.md` with Status: TODO

### Implementer requires:
- `docs/tasks/TASK-{ID}.md`
- `docs/design/DESIGN-{ID}.md`

### Reviewer requires:
- `docs/tasks/TASK-{ID}.md`
- `docs/design/DESIGN-{ID}.md`
- Source code present
- Tests present

---

## 12.3 OUTPUT LOCK

Each agent MUST produce output matching its template.

Free-form text is not a valid deliverable.

| Agent       | Required Output                       | Template                       |
| ----------- | ------------------------------------- | ------------------------------ |
| Planner     | `docs/tasks/TASK-{ID}.md`            | `.claude/templates/TASK.md`    |
| Architect   | `docs/design/DESIGN-{ID}.md`         | `.claude/templates/DESIGN.md`  |
| Implementer | Source code + Test code               | coding standards in agent file |
| Reviewer    | Structured review report              | reviewer agent format          |

Validator references:
- `.claude/validators/task-validator.md`
- `.claude/validators/design-validator.md`
- `.claude/validators/code-validator.md`
- `.claude/validators/review-validator.md`

---

## 12.4 FILE STATE MACHINE

TASK status is the single source of truth for pipeline state.

```
TODO → IN_PROGRESS → IN_REVIEW → DONE
                               ↘ BLOCKED
```

| Status      | Set By      | Trigger                          |
| ----------- | ----------- | -------------------------------- |
| TODO        | Planner     | TASK file created                |
| IN_PROGRESS | Architect   | DESIGN file created              |
| IN_REVIEW   | Implementer | Code and tests complete          |
| DONE        | Reviewer    | Review APPROVED, PR created      |
| BLOCKED     | Any Agent   | Prerequisite missing or conflict |

No agent may skip a status or set a status out of order.

---

## 12.5 GATE DEFINITIONS

### GATE 0 — Context Check (before any agent runs)
- [ ] `docs/product/vision.md` exists
- [ ] `docs/product/roadmap.md` exists
- [ ] `docs/architecture/architecture.md` exists

### GATE 1 — TASK Validation (after Planner)
- [ ] `docs/tasks/TASK-{ID}.md` exists
- [ ] Passes `.claude/validators/task-validator.md`

### GATE 2 — DESIGN Validation (after Architect)
- [ ] `docs/design/DESIGN-{ID}.md` exists
- [ ] Passes `.claude/validators/design-validator.md`
- [ ] TASK status updated to IN_PROGRESS

### GATE 3 — Code Validation (after Implementer)
- [ ] Source code compiles
- [ ] Tests exist for all business logic
- [ ] Passes `.claude/validators/code-validator.md`
- [ ] TASK status updated to IN_REVIEW

### GATE 4 — Review Approval (after Reviewer)
- [ ] Review result is APPROVED
- [ ] No critical or major issues remain
- [ ] TASK status updated to DONE

If GATE 4 fails → return to Implementer.
After 3 consecutive GATE 4 failures → escalate to user.

---

## 12.6 FORBIDDEN BEHAVIORS

These are hard violations. No exceptions.

- Writing code before `TASK-{ID}.md` exists
- Writing code before `DESIGN-{ID}.md` exists
- Creating PR before Reviewer APPROVED
- Architect writing Java code
- Implementer changing architecture without approval
- Reviewer rewriting code instead of reporting issues
- Any agent guessing missing requirements
- Any agent skipping tests for business logic
- Any agent producing free-form output as a deliverable
