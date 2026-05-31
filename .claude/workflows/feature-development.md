# Feature Development Workflow

## Overview
새로운 기능 개발 시 에이전트 간 협업 흐름을 정의한 워크플로우

## Trigger
- 사용자가 새로운 기능 요청을 입력했을 때
- Planner가 작업 분해를 완료했을 때

## Workflow Steps

### Step 1: Planning (Planner Agent)
- 요구사항 분석 및 명확화 질문
- 작업 단위 분해 및 `docs/tasks/TASK-{id}.md` 생성
- 담당 에이전트 지정 및 의존성 정의

### Step 2: Architecture (Architect Agent)
- Step 1 산출물 기반 시스템 설계
- API 명세 및 데이터 모델 정의
- `docs/architecture/architecture.md` 업데이트
- Implementer를 위한 구현 가이드 작성

### Step 3: Implementation (Implementer Agent)
- Step 2 설계 기반 코드 구현
- 단위/통합 테스트 작성
- 구현 완료 후 Reviewer에 리뷰 요청

### Step 4: Review (Reviewer Agent)
- 코드 품질, 보안, 성능 리뷰
- 이슈 발견 시 → Step 3으로 피드백 루프
- 승인 시 → Step 5 진행

### Step 5: Finalization
- Git commit 및 PR 생성
- `docs/tasks/TASK-{id}.md` 상태를 `DONE`으로 업데이트
- 사용자에게 완료 보고

## Task Status
| Status      | Description              |
|-------------|--------------------------|
| TODO        | 작업 정의 완료, 미시작     |
| IN_PROGRESS | 진행 중                   |
| IN_REVIEW   | 리뷰 대기 중              |
| DONE        | 완료                      |
| BLOCKED     | 의존성 또는 이슈로 블로킹  |

## Escalation Rules
- Implementer가 설계 변경이 필요하다고 판단 → Architect에 에스컬레이션
- Reviewer가 요구사항 불명확 발견 → Planner에 에스컬레이션
- 3회 이상 리뷰 반려 → 사용자에게 보고 및 재설계 검토
