# DESIGN-006

## Overview
PullRequest 관리 API를 구현한다.
PullRequest는 메모리(ConcurrentHashMap)에 저장되고, REST 엔드포인트를 통해 저장/조회된다.
GitHub Actions가 PR 생성 완료 후 이 API를 호출해 결과를 기록하며,
Task ID로도 조회 가능하다.

## Architecture Overview

```
GitHub Actions (PR 생성 완료 후 호출)
  │ POST /pull-requests
  │ GET  /pull-requests
  │ GET  /pull-requests/{prId}
  │ GET  /pull-requests/by-task/{taskId}
  ▼
PullRequestController
  │ prId, taskId, implId, reviewId, title, body, prUrl, status
  ▼
PullRequestService
  │ PullRequest 생성 → ConcurrentHashMap 저장
  │ 중복 검사 → PullRequestAlreadyExistsException
  │ 조회 → PullRequestNotFoundException
  ▼
PullRequestResponse / ErrorResponse
```

레이어:
- Controller: HTTP 요청 수신, 경로 변수/바디 추출
- Service: PullRequest 저장, 중복 검사, 조회 로직
- Domain: PullRequest (불변 Record)
- DTO: PullRequestCreateRequest, PullRequestResponse

## API Design

### POST /pull-requests

Request:
```json
{
  "prId": "PR-001",
  "taskId": "TASK-001",
  "implId": "IMPL-001",
  "reviewId": "REVIEW-001",
  "title": "feat: TASK-001 Webhook 수신 구현",
  "body": "## Summary\n- Webhook 수신 엔드포인트 구현\n- 이벤트 저장 로직 추가",
  "prUrl": "https://github.com/whdkfhr/dev-agent/pull/1",
  "status": "OPEN"
}
```

Response 201:
```json
{
  "prId": "PR-001",
  "taskId": "TASK-001",
  "implId": "IMPL-001",
  "reviewId": "REVIEW-001",
  "title": "feat: TASK-001 Webhook 수신 구현",
  "body": "## Summary\n- Webhook 수신 엔드포인트 구현\n- 이벤트 저장 로직 추가",
  "prUrl": "https://github.com/whdkfhr/dev-agent/pull/1",
  "status": "OPEN",
  "createdAt": "2026-06-29T10:00:00Z"
}
```

Error 409 (중복 prId):
```json
{
  "code": "DUPLICATE_PULL_REQUEST",
  "message": "PullRequest already exists: PR-001"
}
```

### GET /pull-requests

Response 200:
```json
[
  {
    "prId": "PR-001",
    "taskId": "TASK-001",
    "implId": "IMPL-001",
    "reviewId": "REVIEW-001",
    "title": "feat: TASK-001 Webhook 수신 구현",
    "body": "...",
    "prUrl": "https://github.com/whdkfhr/dev-agent/pull/1",
    "status": "OPEN",
    "createdAt": "2026-06-29T09:00:00Z"
  }
]
```

### GET /pull-requests/{prId}

Response 200: (POST와 동일한 구조)

Error 404:
```json
{
  "code": "PULL_REQUEST_NOT_FOUND",
  "message": "PullRequest not found: PR-999"
}
```

### GET /pull-requests/by-task/{taskId}

Response 200: (POST와 동일한 구조)

Error 404:
```json
{
  "code": "PULL_REQUEST_NOT_FOUND",
  "message": "PullRequest not found for task: TASK-999"
}
```

## Data Model

### PullRequest (Domain Record)

| Field     | Type    | Description                        |
|-----------|---------|------------------------------------|
| prId      | String  | PullRequest 식별자 (예: PR-001)     |
| taskId    | String  | 연관 Task 식별자                    |
| implId    | String  | 연관 Implementation 식별자          |
| reviewId  | String  | 연관 Review 식별자                  |
| title     | String  | PR 제목                            |
| body      | String  | PR 본문                            |
| prUrl     | String  | GitHub PR URL                      |
| status    | String  | OPEN / MERGED / CLOSED             |
| createdAt | Instant | 저장 시각                           |

### PullRequestCreateRequest (DTO Record)

| Field    | Type   | Description                |
|----------|--------|----------------------------|
| prId     | String | PullRequest 식별자          |
| taskId   | String | 연관 Task 식별자            |
| implId   | String | 연관 Implementation 식별자  |
| reviewId | String | 연관 Review 식별자          |
| title    | String | PR 제목                    |
| body     | String | PR 본문                    |
| prUrl    | String | GitHub PR URL              |
| status   | String | 초기 상태                   |

### PullRequestResponse (DTO Record)

| Field     | Type    | Description                |
|-----------|---------|----------------------------|
| prId      | String  | PullRequest 식별자          |
| taskId    | String  | 연관 Task 식별자            |
| implId    | String  | 연관 Implementation 식별자  |
| reviewId  | String  | 연관 Review 식별자          |
| title     | String  | PR 제목                    |
| body      | String  | PR 본문                    |
| prUrl     | String  | GitHub PR URL              |
| status    | String  | 현재 상태                   |
| createdAt | Instant | 저장 시각                   |

## Package Structure

```
com.arok2.dev_agent
├── pullrequest/
│   ├── controller/
│   │   └── PullRequestController.java
│   ├── service/
│   │   └── PullRequestService.java
│   ├── domain/
│   │   └── PullRequest.java
│   └── dto/
│       ├── PullRequestCreateRequest.java
│       └── PullRequestResponse.java
└── common/
    └── exception/
        ├── PullRequestAlreadyExistsException.java
        └── PullRequestNotFoundException.java
```

## Key Design Decisions
- PullRequest, PullRequestCreateRequest, PullRequestResponse를 Java Record로 정의
- prUrl은 GitHub Actions가 PR 생성 후 전달하는 실제 URL
- status는 OPEN / MERGED / CLOSED 세 가지
- 저장소는 두 개의 ConcurrentHashMap 사용
  - `prId → PullRequest` (prId 기반 조회용)
  - `taskId → PullRequest` (taskId 기반 조회용)

## Trade-offs

| Option | Pros | Cons | Selected |
|--------|------|------|----------|
| Spring Boot → GitHub API 직접 호출 | 서버가 PR 생성 주체 | 외부 의존성, 토큰 관리 복잡 | 미채택 (Phase 6) |
| GitHub Actions → Spring Boot API 호출 | 기존 워크플로우와 일관성 유지, 외부 의존성 없음 | PR 생성 주체가 분리됨 | ✅ (Phase 6) |

## Implementation Guide

구현 순서:
1. `PullRequest.java` — Domain Record 생성
2. `PullRequestCreateRequest.java`, `PullRequestResponse.java` — DTO Record 생성
3. `PullRequestAlreadyExistsException.java`, `PullRequestNotFoundException.java` — 커스텀 예외 생성
4. `GlobalExceptionHandler.java` — 새 예외 핸들러 추가 (409, 404)
5. `PullRequestService.java` — 저장, 중복 검사, 조회 로직 구현
6. `PullRequestController.java` — 엔드포인트 구현
7. `PullRequestServiceTest.java` — Service 단위 테스트
8. `PullRequestControllerTest.java` — Controller MockMvc 테스트
