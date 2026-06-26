# Feature Development Workflow

Version: 1.0

---

# Overview

This workflow defines the standard process for implementing a new feature within the Dev-Agent project.

The workflow ensures that every feature follows a consistent lifecycle from requirement analysis to Pull Request creation.

Each AI Agent has a single responsibility and produces artifacts that become the input for the next stage.

---

# Objectives

* Ensure consistent feature development
* Improve implementation quality
* Reduce ambiguity between AI Agents
* Keep documentation synchronized with source code
* Produce reviewable and testable software increments

---

# Trigger

This workflow starts when one of the following events occurs.

* User requests a new feature
* GitHub Issue is created
* GitHub Issue is updated
* GitHub Issue is reopened
* Manual workflow execution

---

# Step 0. Context Collection

## Responsible

System

## Purpose

Collect project context before planning.

## Required References

* CLAUDE.md
* docs/product/vision.md
* docs/product/roadmap.md
* docs/decisions/adr/
* docs/architecture/
* Existing TASK documents
* Relevant source code

## Output

Shared project context for downstream agents.

---

# Step 1. Planning

## Responsible

Planner Agent

## Input

* Feature Request
* GitHub Issue
* Context Collection

## Responsibilities

* Analyze requirements
* Ask clarification questions if necessary
* Identify dependencies
* Break work into small tasks
* Define acceptance criteria
* Assign implementation priority

## Output

docs/tasks/TASK-{id}.md

The task document must include:

* Goal
* Scope
* Out of Scope
* Task List
* Acceptance Criteria
* Dependencies
* Status

---

# Step 2. Architecture

## Responsible

Architect Agent

## Input

* TASK document
* Existing architecture
* ADR documents

## Responsibilities

* Design the implementation
* Update architecture documentation
* Define APIs
* Define package structure
* Design DTOs
* Design database changes
* Identify technical risks

## Output

Architecture documentation

Implementation Guide

API Specification (if needed)

Updated architecture documents

---

# Step 3. Implementation

## Responsible

Implementer Agent

## Input

* Architecture Design
* TASK document

## Responsibilities

* Implement production-ready code
* Follow project coding standards
* Write unit tests
* Write integration tests where applicable
* Update implementation status

Implementer should NOT modify architecture without approval.

## Output

* Java source code
* Test code
* Updated task status

---

# Step 4. Review

## Responsible

Reviewer Agent

## Input

* Source Code
* Architecture Design
* TASK document

## Responsibilities

Review:

* Correctness
* Readability
* Maintainability
* Security
* Performance
* Spring Boot Best Practices
* Test Coverage
* Error Handling

## Result

PASS

or

FAIL

If FAIL

Return review comments to Implementer.

Implementation returns to Step 3.

Repeat until PASS.

---

# Step 5. Approval

## Responsible

System + User (when required)

## Responsibilities

* Verify acceptance criteria
* Confirm review approval
* Confirm documentation updates

If approved

Proceed to Finalization.

---

# Step 6. Finalization

## Responsibilities

* Create Git Commit
* Push feature branch
* Create Pull Request
* Update TASK status
* Notify completion

## Output

* Commit SHA
* Pull Request URL
* Updated TASK document

---

# Task Status

| Status      | Description                             |
| ----------- | --------------------------------------- |
| TODO        | Task defined but not started            |
| IN_PROGRESS | Currently being implemented             |
| IN_REVIEW   | Waiting for review                      |
| BLOCKED     | Waiting for dependency or clarification |
| DONE        | Successfully completed                  |

---

# Escalation Rules

Planner → User

* Requirements are ambiguous
* Business rules are missing

Implementer → Architect

* Design changes are required
* Unexpected technical limitations are discovered

Reviewer → Implementer

* Code quality issues
* Missing tests
* Security concerns
* Architecture violations

Reviewer → Planner

* Acceptance criteria are unclear
* Requirements conflict with implementation

System → User

* More than three review failures
* Major architecture redesign required

---

# Deliverables

Each completed workflow should produce:

Planning

* TASK document

Architecture

* Updated architecture documents

Implementation

* Source code
* Test code

Review

* Review report

Finalization

* Git Commit
* Pull Request
* Updated documentation

---

# Success Criteria

A feature is considered complete only when:

* Documentation is updated
* Architecture matches implementation
* Acceptance criteria are satisfied
* Tests pass successfully
* Review status is PASS
* Pull Request is created
* TASK status is DONE

Completion is defined by software quality, not by the amount of generated code.
