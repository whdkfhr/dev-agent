# Dev Agent

AI Agent 기반 개발자 작업 자동화 시스템

GitHub Issue를 입력으로 받아 AI Agent가 작업을 분석하고, 코드를 생성하며, 최종적으로 Pull Request를 자동 생성하는 플랫폼입니다.

---

## Overview

```
GitHub Issue
     ↓
  Planner       요구사항 분석 → TASK 문서 생성
     ↓
 Architect      시스템 설계 → DESIGN 문서 생성
     ↓
Implementer     코드 구현 → 소스 코드 + 테스트 생성
     ↓
  Reviewer      코드 리뷰 → APPROVED / REJECTED
     ↓
 Pull Request   자동 생성
```

각 단계는 독립적으로 실행 가능하며, 에이전트는 이전 단계의 산출물 없이 다음 단계로 진행할 수 없습니다.

---

## Tech Stack

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Build | Gradle |
| AI | Claude Code (Anthropic) |
| VCS | Git / GitHub |
| Database | PostgreSQL (예정) |
| Cache | Redis (예정) |

---

## Agent System

### Agents

| Agent | 역할 | 입력 | 출력 |
|-------|------|------|------|
| **Planner** | GitHub Issue를 실행 가능한 작업 단위로 분해 | GitHub Issue | `docs/tasks/TASK-{ID}.md` |
| **Architect** | TASK 기반 시스템 설계 및 API/데이터 모델 정의 | TASK 문서 | `docs/design/DESIGN-{ID}.md` |
| **Implementer** | 설계 문서 기반 Spring Boot 코드 구현 | DESIGN 문서 | 소스 코드 + 테스트 |
| **Reviewer** | 코드 품질 / 아키텍처 준수 / 보안 검토 | 코드 + TASK + DESIGN | 리뷰 리포트 |

### Gate System

각 에이전트 사이에 검증 게이트가 존재합니다. 게이트를 통과하지 못하면 이전 에이전트로 리턴됩니다.

```
[GATE 0] Context 확인     vision.md, roadmap.md, architecture.md 존재 여부
[GATE 1] TASK 검증        task-validator — 필수 섹션, 포맷, 금지 내용 확인
[GATE 2] DESIGN 검증      design-validator — API 명세, 데이터 모델, 구현 가이드 확인
[GATE 3] Code 검증        code-validator — 아키텍처 준수, 테스트 존재, 레이어 분리 확인
[GATE 4] Review 승인      review-validator — APPROVED 판정, 심각도별 이슈 분류 확인
```

### Task State Machine

```
TODO → IN_PROGRESS → IN_REVIEW → DONE
                               ↘ BLOCKED
```

| 상태 | 전환 주체 | 의미 |
|------|-----------|------|
| TODO | Planner | 작업 정의 완료 |
| IN_PROGRESS | Architect | 설계 완료, 구현 진행 중 |
| IN_REVIEW | Implementer | 구현 완료, 리뷰 대기 |
| DONE | Reviewer | 승인 완료, PR 생성 |
| BLOCKED | 모든 Agent | 진행 불가 |

---

## Project Structure

```
dev-agent/
├── CLAUDE.md                          # 시스템 헌법 — 모든 에이전트가 따르는 규칙
│
├── .claude/
│   ├── agents/                        # 에이전트 정의
│   │   ├── planner.md
│   │   ├── architect.md
│   │   ├── implementer.md
│   │   └── reviewer.md
│   ├── workflows/
│   │   └── feature-development.md    # Step 0~6 전체 협업 흐름
│   ├── templates/                     # 산출물 강제 포맷
│   │   ├── TASK.md
│   │   ├── DESIGN.md
│   │   └── ADR.md
│   └── validators/                    # Gate 검증 규칙 (강제력 엔진)
│       ├── task-validator.md
│       ├── design-validator.md
│       ├── code-validator.md
│       └── review-validator.md
│
├── docs/
│   ├── product/
│   │   ├── vision.md                  # 제품 목표
│   │   └── roadmap.md                 # Phase별 개발 계획
│   ├── architecture/
│   │   └── architecture.md            # 시스템 아키텍처
│   ├── decisions/adr/                 # 아키텍처 결정 기록
│   ├── tasks/                         # TASK-{ID}.md (Planner 산출물)
│   └── design/                        # DESIGN-{ID}.md (Architect 산출물)
│
└── src/
    └── main/java/com/devagent/
        ├── agent/                     # 에이전트 실행 엔진
        ├── workflow/                  # 워크플로우 상태 머신
        ├── task/                      # Task 도메인
        ├── github/                    # GitHub 연동
        └── common/                    # 공통 유틸, 예외, 응답 형식
```

---

## Roadmap

| Phase | 목표 | 상태 |
|-------|------|------|
| Phase 1 | GitHub Webhook 수신 — Issue Event 수신, 저장, 로그 출력 | 진행 예정 |
| Phase 2 | Planner Agent — Issue 분석, Task 생성 | 예정 |
| Phase 3 | Architect Agent — 구현 설계 생성 | 예정 |
| Phase 4 | Implementer Agent — 코드 생성 및 수정 | 예정 |
| Phase 5 | Reviewer Agent — 코드 리뷰, 품질 점검 | 예정 |
| Phase 6 | Pull Request 자동 생성 | 예정 |

---

## Core Principles

**Documentation First**
문서가 진실의 원천입니다. 문서 없이 코드를 작성하지 않습니다.

**Input Lock**
각 에이전트는 이전 단계 산출물이 없으면 실행되지 않습니다.

**Output Lock**
각 에이전트는 템플릿 기반의 구조화된 산출물만 생성합니다.

**Single Responsibility**
각 에이전트는 정확히 하나의 책임만 가집니다.

---

## Architecture

Layered Architecture 기반으로 설계되었습니다.

```
┌──────────────────────────┐
│    Presentation Layer    │  Controller
├──────────────────────────┤
│    Application Layer     │  Service
├──────────────────────────┤
│      Domain Layer        │  Entity, Domain Logic
├──────────────────────────┤
│   Infrastructure Layer   │  Repository, External API
└──────────────────────────┘
```

---

## Git Workflow

```
main        — 릴리즈 브랜치
develop     — 통합 브랜치
feature/*   — 기능 개발 브랜치
bugfix/*    — 버그 수정 브랜치
```

Conventional Commits 규칙을 따릅니다.

```
feat:     새로운 기능
fix:      버그 수정
refactor: 리팩토링
docs:     문서 수정
test:     테스트 추가/수정
chore:    빌드, 설정 변경
```
