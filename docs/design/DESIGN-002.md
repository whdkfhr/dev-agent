# DESIGN-002

## Overview
Task 관리 API를 구현한다.
Task는 메모리(ConcurrentHashMap)에 저장되고, REST 엔드포인트를 통해 저장/조회된다.

## Architecture Overview

```
Client
  │ POST /tasks
  │ GET  /tasks
  │ GET  /tasks/{taskId}
  ▼
TaskController
  │ taskId, title, content, status
  ▼
TaskService
  │ Task 생성 → ConcurrentHashMap 저장
  │ 중복 검사 → TaskAlreadyExistsException
  │ 조회 → TaskNotFoundException
  ▼
TaskResponse / ErrorResponse
```

레이어:
- Controller: HTTP 요청 수신, 경로 변수/바디 추출
- Service: Task 저장, 중복 검사, 조회 로직
- Domain: Task (불변 Record)
- DTO: TaskCreateRequest, TaskResponse

## API Design

### POST /tasks

Request:
```json
{
  "taskId": "TASK-002",
  "title": "Task 관리 API 구현",
  "content": "# TASK-002\n...",
  "status": "TODO"
}
```

Response 201:
```json
{
  "taskId": "TASK-002",
  "title": "Task 관리 API 구현",
  "status": "TODO",
  "createdAt": "2026-06-29T10:00:00Z"
}
```

Error 409 (중복 taskId):
```json
{
  "code": "DUPLICATE_TASK",
  "message": "Task already exists: TASK-002"
}
```

### GET /tasks

Response 200:
```json
[
  {
    "taskId": "TASK-001",
    "title": "GitHub Webhook 수신 구현",
    "status": "IN_REVIEW",
    "createdAt": "2026-06-29T09:00:00Z"
  }
]
```

### GET /tasks/{taskId}

Response 200:
```json
{
  "taskId": "TASK-001",
  "title": "GitHub Webhook 수신 구현",
  "status": "IN_REVIEW",
  "createdAt": "2026-06-29T09:00:00Z"
}
```

Error 404 (존재하지 않는 taskId):
```json
{
  "code": "TASK_NOT_FOUND",
  "message": "Task not found: TASK-999"
}
```

## Data Model

### Task (Domain Record)

| Field     | Type    | Description              |
|-----------|---------|--------------------------|
| taskId    | String  | Task 식별자 (예: TASK-001) |
| title     | String  | Task 제목                 |
| content   | String  | TASK 문서 전체 내용        |
| status    | String  | TODO / IN_PROGRESS / IN_REVIEW / DONE |
| createdAt | Instant | 저장 시각                 |

### TaskCreateRequest (DTO Record)

| Field   | Type   | Description     |
|---------|--------|-----------------|
| taskId  | String | Task 식별자      |
| title   | String | Task 제목        |
| content | String | TASK 문서 내용   |
| status  | String | 초기 상태        |

### TaskResponse (DTO Record)

| Field     | Type    | Description   |
|-----------|---------|---------------|
| taskId    | String  | Task 식별자    |
| title     | String  | Task 제목      |
| status    | String  | 현재 상태      |
| createdAt | Instant | 저장 시각      |

## Package Structure

```
com.arok2.dev_agent
├── task/
│   ├── controller/
│   │   └── TaskController.java
│   ├── service/
│   │   └── TaskService.java
│   ├── domain/
│   │   └── Task.java
│   └── dto/
│       ├── TaskCreateRequest.java
│       └── TaskResponse.java
└── common/
    └── exception/
        ├── GlobalExceptionHandler.java  (기존, 예외 추가)
        └── ErrorResponse.java           (기존)
```

## Key Design Decisions
- Task, TaskCreateRequest, TaskResponse를 Java Record로 정의 (불변성 보장)
- 저장소는 ConcurrentHashMap (Phase 3에서 PostgreSQL로 교체 예정)
- 중복/미존재 케이스는 커스텀 예외로 처리 → GlobalExceptionHandler에서 일원화
- content 필드에 TASK 문서 전체를 저장해 Agent가 내용을 직접 읽을 수 있도록 함

## Trade-offs

| Option | Pros | Cons | Selected |
|--------|------|------|----------|
| ConcurrentHashMap | 단순, 의존성 없음 | 재시작 시 소멸 | ✅ (Phase 2) |
| H2 인메모리 DB | 영속성, 쿼리 가능 | 설정 복잡 | Phase 3 |

## Non-Functional Design
- 성능: ConcurrentHashMap으로 동시 요청 안전 처리
- 확장성: TaskService는 인터페이스로 추상화하지 않음 (단순함 우선)

## Implementation Guide

구현 순서:
1. `Task.java` — Domain Record 생성
2. `TaskCreateRequest.java`, `TaskResponse.java` — DTO Record 생성
3. `TaskAlreadyExistsException.java`, `TaskNotFoundException.java` — 커스텀 예외 생성
4. `GlobalExceptionHandler.java` — 새 예외 핸들러 추가 (409, 404)
5. `TaskService.java` — 저장, 중복 검사, 조회 로직 구현
6. `TaskController.java` — POST /tasks, GET /tasks, GET /tasks/{taskId} 구현
7. `TaskServiceTest.java` — Service 단위 테스트
8. `TaskControllerTest.java` — Controller MockMvc 테스트
