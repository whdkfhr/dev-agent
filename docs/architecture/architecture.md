# System Architecture

## Overview
AI Agent 기반 개발자 작업 자동화 시스템의 전체 아키텍처 문서

## Tech Stack
| Layer       | Technology                     |
|-------------|-------------------------------|
| Language    | Java 17                        |
| Framework   | Spring Boot 3.x                |
| Build Tool  | Gradle                         |
| AI          | Claude Code (Anthropic)        |
| VCS         | Git / GitHub                   |
| Database    | PostgreSQL (Phase 3, 예정)     |
| Cache       | Redis (Phase 3, 예정)          |
| Messaging   | GitHub Webhook (Phase 4, 예정) |

## Architecture Style
**Layered Architecture** (향후 Hexagonal 전환 고려)

```
┌─────────────────────────────────────┐
│           Presentation Layer        │  REST API (Controller)
├─────────────────────────────────────┤
│           Application Layer         │  UseCase / Service
├─────────────────────────────────────┤
│             Domain Layer            │  Entity, Domain Logic
├─────────────────────────────────────┤
│         Infrastructure Layer        │  Repository, External API
└─────────────────────────────────────┘
```

## Package Structure
```
dev-agent/
└── src/main/java/com/devagent/
    ├── agent/          # 에이전트 정의 및 실행
    │   ├── planner/
    │   ├── architect/
    │   ├── implementer/
    │   └── reviewer/
    ├── workflow/       # 워크플로우 엔진
    ├── task/           # 작업 관리
    ├── github/         # GitHub 연동 (Phase 4)
    └── common/         # 공통 유틸, 예외, 응답 형식
```

## Agent Interaction Flow
```
User Request
     │
     ▼
┌─────────┐    Task Doc    ┌───────────┐
│ Planner │ ─────────────► │ Architect │
└─────────┘                └───────────┘
                                │ Design Guide
                                ▼
                         ┌─────────────┐
                         │ Implementer │
                         └─────────────┘
                                │ Code
                                ▼
                          ┌──────────┐
                          │ Reviewer │
                          └──────────┘
                                │ Approve / Feedback
                                ▼
                           Git Commit / PR
```

## GitHub Actions Execution Architecture

GitHub Actions가 에이전트의 실행 환경입니다.

```
GitHub Issue (opened)
        │
        ▼
┌───────────────────┐
│  planner.yml      │  on: issues.opened
│  run_planner.py   │  → Anthropic API 호출
└───────────────────┘
        │ docs/tasks/TASK-{id}.md 생성
        │ label: "design" 추가
        ▼
┌───────────────────┐
│  architect.yml    │  on: issues.labeled (design)
│  run_architect.py │  → Anthropic API 호출
└───────────────────┘
        │ docs/design/DESIGN-{id}.md 생성
        │ label: "implement" 추가
        ▼
┌────────────────────┐
│  implementer.yml   │  on: issues.labeled (implement)
│  run_implementer.py│  → Anthropic API 호출
└────────────────────┘
        │ src/ 코드 생성
        │ PR 생성
        ▼
┌───────────────────┐
│  reviewer.yml     │  on: pull_request.opened
│  run_reviewer.py  │  → Anthropic API 호출
└───────────────────┘
        │ PR 리뷰 코멘트 작성
        ▼
   APPROVED / REJECTED
```

## Label State Machine

GitHub Label이 파이프라인 상태를 나타냅니다.

| Label       | 의미                | 다음 트리거         |
|-------------|---------------------|---------------------|
| `plan`      | Planner 실행 요청   | planner.yml         |
| `design`    | Architect 실행 요청 | architect.yml       |
| `implement` | Implementer 실행    | implementer.yml     |
| `review`    | PR 리뷰 대기        | reviewer.yml (자동) |
| `done`      | 완료                | -                   |

GitHub = 상태 저장소, Label = 상태, MD 파일 = 계약, PR = 실행 결과

## GitHub Actions File Structure

```
.github/
├── workflows/
│   ├── planner.yml       # Issue opened → TASK 생성
│   ├── architect.yml     # label:design → DESIGN 생성
│   ├── implementer.yml   # label:implement → 코드 생성 + PR
│   └── reviewer.yml      # PR opened → 리뷰 코멘트
├── scripts/
│   ├── run_planner.py    # Planner AI 호출 스크립트
│   ├── run_architect.py  # Architect AI 호출 스크립트
│   ├── run_implementer.py# Implementer AI 호출 스크립트
│   └── run_reviewer.py   # Reviewer AI 호출 스크립트
└── ISSUE_TEMPLATE/
    └── feature-request.yml  # 구조화된 이슈 입력 포맷
```

## Required GitHub Secrets

| Secret              | 용도                     |
|---------------------|--------------------------|
| `ANTHROPIC_API_KEY` | Anthropic API 호출       |
| `GITHUB_TOKEN`      | Issue 라벨, PR 생성 (자동 제공) |

## Key Design Decisions
- 에이전트는 GitHub Actions Job으로 실행 (stateless)
- 상태는 GitHub Issue Label과 MD 파일로 관리 (GitHub = Database)
- AI 호출은 Python 스크립트로 분리 (YML과 로직 분리)
- 산출물은 반드시 Git commit으로 영속화
- `[skip ci]` 태그로 자동 커밋의 무한 루프 방지

## Non-Functional Requirements
| 항목     | 목표                              |
|----------|-----------------------------------|
| 응답성   | 에이전트 실행 상태 GitHub Actions 로그로 추적 |
| 확장성   | 새 에이전트 = 새 YML + 새 스크립트 추가 |
| 추적성   | 모든 AI 호출 토큰 사용량 로깅     |
| 보안     | ANTHROPIC_API_KEY GitHub Secret 관리 |
