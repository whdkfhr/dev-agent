# Dev Agent

AI Agent 기반 개발자 작업 자동화 시스템

GitHub Issue를 입력으로 받아 AI Agent가 작업을 분석하고, 코드를 생성하며, 최종적으로 Pull Request를 자동 생성하는 오케스트레이션 플랫폼입니다.

---

## Overview

```
GitHub Issue 생성
      ↓
  Planner       요구사항 분석 → TASK 문서 생성 → POST /tasks
      ↓
 Architect      시스템 설계 → DESIGN 문서 생성 → POST /designs
      ↓
Implementer     코드 구현 → 소스 코드 + 테스트 → POST /implementations
      ↓
  Reviewer      코드 리뷰 → APPROVED / REJECTED → POST /reviews
      ↓
 Pull Request   자동 생성 → POST /pull-requests
```

각 단계는 GitHub Actions로 자동 실행되며, 산출물은 Railway에 배포된 Spring Boot 서버에 저장됩니다.

---

## Tech Stack

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Build | Gradle |
| AI | Claude API (claude-sonnet-4-6) |
| CI/CD | GitHub Actions |
| 배포 | Railway |
| 저장소 | ConcurrentHashMap (인메모리) |

---

## API Endpoints

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/webhook` | GitHub Webhook 이벤트 수신 |
| POST | `/tasks` | Task 저장 |
| GET | `/tasks` | Task 목록 조회 |
| GET | `/tasks/{taskId}` | Task 단건 조회 |
| POST | `/designs` | Design 저장 |
| GET | `/designs` | Design 목록 조회 |
| GET | `/designs/{designId}` | Design 단건 조회 |
| GET | `/designs/by-task/{taskId}` | Task 기반 Design 조회 |
| POST | `/implementations` | Implementation 저장 |
| GET | `/implementations` | Implementation 목록 조회 |
| GET | `/implementations/{implId}` | Implementation 단건 조회 |
| GET | `/implementations/by-task/{taskId}` | Task 기반 Implementation 조회 |
| POST | `/reviews` | Review 저장 |
| GET | `/reviews` | Review 목록 조회 |
| GET | `/reviews/{reviewId}` | Review 단건 조회 |
| GET | `/reviews/by-impl/{implId}` | Implementation 기반 Review 조회 |
| POST | `/pull-requests` | PullRequest 저장 |
| GET | `/pull-requests` | PullRequest 목록 조회 |
| GET | `/pull-requests/{prId}` | PullRequest 단건 조회 |
| GET | `/pull-requests/by-task/{taskId}` | Task 기반 PullRequest 조회 |

---

## Agent System

### Agents

| Agent | 역할 | 트리거 | 산출물 |
|-------|------|--------|--------|
| **Planner** | GitHub Issue 분석 → Task 정의 | Issue 생성 | `docs/tasks/TASK-{ID}.md` |
| **Architect** | Task 기반 설계 → API/데이터 모델 정의 | `design` 라벨 | `docs/design/DESIGN-{ID}.md` |
| **Implementer** | 설계 기반 코드 구현 | `implement` 라벨 | 소스 코드 + 테스트 + PR |
| **Reviewer** | 코드 품질 / 아키텍처 준수 / 보안 검토 | PR 생성 | 리뷰 코멘트 |

### Gate System

```
[GATE 0] Context 확인     vision.md, roadmap.md, architecture.md 존재 여부
[GATE 1] TASK 검증        TASK 문서 필수 섹션 / 포맷 확인
[GATE 2] DESIGN 검증      API 명세 / 데이터 모델 / 구현 가이드 확인
[GATE 3] Code 검증        아키텍처 준수 / 테스트 존재 / 레이어 분리 확인
[GATE 4] Review 승인      APPROVED 판정 확인
```

### Task State Machine

```
TODO → IN_PROGRESS → IN_REVIEW → DONE
                               ↘ BLOCKED
```

---

## Project Structure

```
dev-agent/
├── CLAUDE.md                          # 시스템 헌법 — 모든 에이전트가 따르는 규칙
│
├── .github/workflows/
│   ├── planner.yml                    # Issue → TASK 생성
│   ├── architect.yml                  # TASK → DESIGN 생성
│   ├── implementer.yml                # DESIGN → 코드 생성 + PR
│   ├── reviewer.yml                   # PR → 코드 리뷰
│   └── e2e-mock.yml                   # API key 없이 전체 파이프라인 검증
│
├── .claude/
│   ├── agents/                        # 에이전트 프롬프트 정의
│   │   ├── planner.md
│   │   ├── architect.md
│   │   ├── implementer.md
│   │   └── reviewer.md
│   ├── templates/                     # 산출물 포맷 템플릿
│   └── validators/                    # Gate 검증 규칙
│
├── docs/
│   ├── product/
│   │   ├── vision.md
│   │   └── roadmap.md
│   ├── tasks/                         # TASK-{ID}.md
│   └── design/                        # DESIGN-{ID}.md
│
└── src/main/java/com/arok2/dev_agent/
    ├── webhook/                       # GitHub Webhook 수신
    ├── task/                          # Task 관리 API
    ├── design/                        # Design 관리 API
    ├── implementation/                # Implementation 관리 API
    ├── review/                        # Review 관리 API
    ├── pullrequest/                   # PullRequest 관리 API
    └── common/exception/              # 전역 예외 처리
```

---

## Deployment

Railway에 배포되어 있습니다.

```
https://dev-agent-production-1459.up.railway.app
```

### 동작 확인

```bash
curl https://dev-agent-production-1459.up.railway.app/tasks
```

---

## GitHub Actions 설정

워크플로우 실행에 필요한 GitHub Secrets:

| Secret | 설명 |
|--------|------|
| `ANTHROPIC_API_KEY` | Claude API key (AI 에이전트 실행용) |
| `SERVER_URL` | `https://dev-agent-production-1459.up.railway.app` |

---

## 다른 프로젝트에 적용하기

dev-agent는 오케스트레이션 서버로, 다른 프로젝트의 GitHub Issue → 코드 PR 자동화에 활용할 수 있습니다.

### 적용 방법 (워크플로우 복사)

1. 타겟 프로젝트에 워크플로우 파일 복사
```
.github/workflows/planner.yml
.github/workflows/architect.yml
.github/workflows/implementer.yml
.github/workflows/reviewer.yml
```

2. 타겟 프로젝트 GitHub Secrets 추가
```
ANTHROPIC_API_KEY=<Claude API key>
SERVER_URL=https://dev-agent-production-1459.up.railway.app
```

3. GitHub Issue 생성 → 파이프라인 자동 실행

---

## Roadmap

| Phase | 내용 | 상태 |
|-------|------|------|
| Phase 1 | GitHub Webhook 수신 | ✅ 완료 |
| Phase 2 | Planner Agent — Task 관리 API | ✅ 완료 |
| Phase 3 | Architect Agent — Design 관리 API | ✅ 완료 |
| Phase 4 | Implementer Agent — Implementation 관리 API | ✅ 완료 |
| Phase 5 | Reviewer Agent — Review 관리 API | ✅ 완료 |
| Phase 6 | Pull Request 관리 API + Railway 배포 | ✅ 완료 |

---

## Core Principles

**Documentation First** — 문서가 진실의 원천. 문서 없이 코드를 작성하지 않습니다.

**Input Lock** — 각 에이전트는 이전 단계 산출물이 없으면 실행되지 않습니다.

**Output Lock** — 각 에이전트는 템플릿 기반의 구조화된 산출물만 생성합니다.

**Single Responsibility** — 각 에이전트는 정확히 하나의 책임만 가집니다.

---

## Git Workflow

```
main        — 릴리즈 브랜치
develop     — 통합 브랜치
feature/*   — 기능 개발 브랜치
```

Conventional Commits 규칙을 따릅니다. 커밋 메시지는 한국어로 작성합니다.

```
feat:     새로운 기능
fix:      버그 수정
refactor: 리팩토링
docs:     문서 수정
test:     테스트 추가/수정
chore:    빌드, 설정 변경
```
