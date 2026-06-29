# DESIGN-004

## Overview
Implementation 관리 API를 구현한다.
Implementation은 메모리(ConcurrentHashMap)에 저장되고, REST 엔드포인트를 통해 저장/조회된다.
각 Implementation은 연관된 Task ID, Design ID를 가지며 Task ID로도 조회 가능하다.

## Architecture Overview

```
Client
  │ POST /implementations
  │ GET  /implementations
  │ GET  /implementations/{implId}
  │ GET  /implementations/by-task/{taskId}
  ▼
ImplementationController
  │ implId, taskId, designId, status, generatedFiles
  ▼
ImplementationService
  │ Implementation 생성 → ConcurrentHashMap 저장
  │ 중복 검사 → ImplementationAlreadyExistsException
  │ 조회 → ImplementationNotFoundException
  ▼
ImplementationResponse / ErrorResponse
```

레이어:
- Controller: HTTP 요청 수신, 경로 변수/바디 추출
- Service: Implementation 저장, 중복 검사, 조회 로직
- Domain: Implementation (불변 Record)
- DTO: ImplementationCreateRequest, ImplementationResponse

## API Design

### POST /implementations

Request:
```json
{
  "implId": "IMPL-001",
  "taskId": "TASK-001",
  "designId": "DESIGN-001",
  "status": "GENERATED",
  "generatedFiles": [
    "src/main/java/com/arok2/dev_agent/webhook/controller/WebhookController.java",
    "src/main/java/com/arok2/dev_agent/webhook/service/WebhookService.java"
  ]
}
```

Response 201:
```json
{
  "implId": "IMPL-001",
  "taskId": "TASK-001",
  "designId": "DESIGN-001",
  "status": "GENERATED",
  "generatedFiles": [
    "src/main/java/com/arok2/dev_agent/webhook/controller/WebhookController.java",
    "src/main/java/com/arok2/dev_agent/webhook/service/WebhookService.java"
  ],
  "createdAt": "2026-06-29T10:00:00Z"
}
```

Error 409 (중복 implId):
```json
{
  "code": "DUPLICATE_IMPLEMENTATION",
  "message": "Implementation already exists: IMPL-001"
}
```

### GET /implementations

Response 200:
```json
[
  {
    "implId": "IMPL-001",
    "taskId": "TASK-001",
    "designId": "DESIGN-001",
    "status": "GENERATED",
    "generatedFiles": ["..."],
    "createdAt": "2026-06-29T09:00:00Z"
  }
]
```

### GET /implementations/{implId}

Response 200: (POST와 동일한 구조)

Error 404:
```json
{
  "code": "IMPLEMENTATION_NOT_FOUND",
  "message": "Implementation not found: IMPL-999"
}
```

### GET /implementations/by-task/{taskId}

Response 200: (POST와 동일한 구조)

Error 404:
```json
{
  "code": "IMPLEMENTATION_NOT_FOUND",
  "message": "Implementation not found for task: TASK-999"
}
```

## Data Model

### Implementation (Domain Record)

| Field          | Type         | Description                        |
|----------------|--------------|------------------------------------|
| implId         | String       | Implementation 식별자 (예: IMPL-001) |
| taskId         | String       | 연관 Task 식별자                    |
| designId       | String       | 연관 Design 식별자                  |
| status         | String       | GENERATED / FAILED                 |
| generatedFiles | List<String> | 생성된 파일 경로 목록               |
| createdAt      | Instant      | 저장 시각                           |

### ImplementationCreateRequest (DTO Record)

| Field          | Type         | Description            |
|----------------|--------------|------------------------|
| implId         | String       | Implementation 식별자   |
| taskId         | String       | 연관 Task 식별자        |
| designId       | String       | 연관 Design 식별자      |
| status         | String       | 초기 상태               |
| generatedFiles | List<String> | 생성된 파일 경로 목록   |

### ImplementationResponse (DTO Record)

| Field          | Type         | Description            |
|----------------|--------------|------------------------|
| implId         | String       | Implementation 식별자   |
| taskId         | String       | 연관 Task 식별자        |
| designId       | String       | 연관 Design 식별자      |
| status         | String       | 현재 상태               |
| generatedFiles | List<String> | 생성된 파일 경로 목록   |
| createdAt      | Instant      | 저장 시각               |

## Package Structure

```
com.arok2.dev_agent
├── implementation/
│   ├── controller/
│   │   └── ImplementationController.java
│   ├── service/
│   │   └── ImplementationService.java
│   ├── domain/
│   │   └── Implementation.java
│   └── dto/
│       ├── ImplementationCreateRequest.java
│       └── ImplementationResponse.java
└── common/
    └── exception/
        ├── ImplementationAlreadyExistsException.java
        └── ImplementationNotFoundException.java
```

## Key Design Decisions
- Implementation, ImplementationCreateRequest, ImplementationResponse를 Java Record로 정의
- generatedFiles는 List<String>으로 저장 (파일 경로만 관리, 실제 파일 내용은 저장하지 않음)
- 저장소는 두 개의 ConcurrentHashMap 사용
  - `implId → Implementation` (implId 기반 조회용)
  - `taskId → Implementation` (taskId 기반 조회용)

## Trade-offs

| Option | Pros | Cons | Selected |
|--------|------|------|----------|
| 파일 경로만 저장 | 단순, 메모리 절약 | 실제 코드 조회 불가 | ✅ (Phase 4) |
| 파일 내용 함께 저장 | 코드 직접 조회 가능 | 메모리 과다 사용 | 미채택 |

## Implementation Guide

구현 순서:
1. `Implementation.java` — Domain Record 생성
2. `ImplementationCreateRequest.java`, `ImplementationResponse.java` — DTO Record 생성
3. `ImplementationAlreadyExistsException.java`, `ImplementationNotFoundException.java` — 커스텀 예외 생성
4. `GlobalExceptionHandler.java` — 새 예외 핸들러 추가 (409, 404)
5. `ImplementationService.java` — 저장, 중복 검사, 조회 로직 구현
6. `ImplementationController.java` — 엔드포인트 구현
7. `ImplementationServiceTest.java` — Service 단위 테스트
8. `ImplementationControllerTest.java` — Controller MockMvc 테스트
