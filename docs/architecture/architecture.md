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

## Key Design Decisions
- 에이전트는 독립적인 Spring Bean으로 정의
- 워크플로우는 상태 머신(State Machine) 패턴으로 구현
- 작업(Task)은 불변 도메인 객체로 설계
- 외부 시스템(GitHub, Claude API)은 인터페이스로 추상화

## Non-Functional Requirements
| 항목     | 목표                              |
|----------|-----------------------------------|
| 응답성   | 에이전트 실행 상태 실시간 스트리밍 |
| 확장성   | 새 에이전트 추가 시 기존 코드 무변경 |
| 추적성   | 모든 에이전트 액션 로깅           |
| 보안     | API Key 환경변수 관리, 입력값 검증 |
