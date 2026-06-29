# DESIGN-003

## Overview
Design 관리 API를 구현한다.
Design은 메모리(ConcurrentHashMap)에 저장되고, REST 엔드포인트를 통해 저장/조회된다.
각 Design은 연관된 Task ID를 가지며, Task ID로도 조회 가능하다.

## Architecture Overview

```
Client
  │ POST /designs
  │ GET  /designs
  │ GET  /designs/{designId}
  │ GET  /designs/by-task/{taskId}
  ▼
DesignController
  │ designId, taskId, content
  ▼
DesignService
  │ Design 생성 → ConcurrentHashMap 저장
  │ 중복 검사 → DesignAlreadyExistsException
  │ 조회 → DesignNotFoundException
  ▼
DesignResponse / ErrorResponse
```

레이어:
- Controller: HTTP 요청 수신, 경로 변수/바디 추출
- Service: Design 저장, 중복 검사, 조회 로직
- Domain: Design (불변 Record)
- DTO: DesignCreateRequest, DesignResponse

## API Design

### POST /designs

Request:
```json
{
  "designId": "DESIGN-002",
  "taskId": "TASK-002",
  "content": "# DESIGN-002\n..."
}
```

Response 201:
```json
{
  "designId": "DESIGN-002",
  "taskId": "TASK-002",
  "createdAt": "2026-06-29T10:00:00Z"
}
```

Error 409 (중복 designId):
```json
{
  "code": "DUPLICATE_DESIGN",
  "message": "Design already exists: DESIGN-002"
}
```

### GET /designs

Response 200:
```json
[
  {
    "designId": "DESIGN-001",
    "taskId": "TASK-001",
    "createdAt": "2026-06-29T09:00:00Z"
  }
]
```

### GET /designs/{designId}

Response 200:
```json
{
  "designId": "DESIGN-001",
  "taskId": "TASK-001",
  "createdAt": "2026-06-29T09:00:00Z"
}
```

Error 404:
```json
{
  "code": "DESIGN_NOT_FOUND",
  "message": "Design not found: DESIGN-999"
}
```

### GET /designs/by-task/{taskId}

Response 200:
```json
{
  "designId": "DESIGN-001",
  "taskId": "TASK-001",
  "createdAt": "2026-06-29T09:00:00Z"
}
```

Error 404:
```json
{
  "code": "DESIGN_NOT_FOUND",
  "message": "Design not found for task: TASK-999"
}
```

## Data Model

### Design (Domain Record)

| Field     | Type    | Description                        |
|-----------|---------|------------------------------------|
| designId  | String  | Design 식별자 (예: DESIGN-001)      |
| taskId    | String  | 연관 Task 식별자 (예: TASK-001)     |
| content   | String  | DESIGN 문서 전체 내용               |
| createdAt | Instant | 저장 시각                           |

### DesignCreateRequest (DTO Record)

| Field    | Type   | Description        |
|----------|--------|--------------------|
| designId | String | Design 식별자       |
| taskId   | String | 연관 Task 식별자    |
| content  | String | DESIGN 문서 내용   |

### DesignResponse (DTO Record)

| Field     | Type    | Description     |
|-----------|---------|-----------------|
| designId  | String  | Design 식별자    |
| taskId    | String  | 연관 Task 식별자 |
| createdAt | Instant | 저장 시각        |

## Package Structure

```
com.arok2.dev_agent
├── design/
│   ├── controller/
│   │   └── DesignController.java
│   ├── service/
│   │   └── DesignService.java
│   ├── domain/
│   │   └── Design.java
│   └── dto/
│       ├── DesignCreateRequest.java
│       └── DesignResponse.java
└── common/
    └── exception/
        ├── GlobalExceptionHandler.java  (기존, 예외 추가)
        ├── DesignAlreadyExistsException.java
        └── DesignNotFoundException.java
```

## Key Design Decisions
- Design, DesignCreateRequest, DesignResponse를 Java Record로 정의 (불변성 보장)
- 저장소는 두 개의 ConcurrentHashMap 사용
  - `designId → Design` (designId 기반 조회용)
  - `taskId → Design` (taskId 기반 조회용)
- 중복/미존재 케이스는 커스텀 예외로 처리 → GlobalExceptionHandler에서 일원화
- content 필드에 DESIGN 문서 전체를 저장해 Agent가 내용을 직접 읽을 수 있도록 함

## Trade-offs

| Option | Pros | Cons | Selected |
|--------|------|------|----------|
| ConcurrentHashMap x2 | taskId 조회 O(1) | 메모리 2배 | ✅ (Phase 3) |
| ConcurrentHashMap x1 + 선형 탐색 | 메모리 절약 | taskId 조회 O(n) | - |

## Implementation Guide

구현 순서:
1. `Design.java` — Domain Record 생성
2. `DesignCreateRequest.java`, `DesignResponse.java` — DTO Record 생성
3. `DesignAlreadyExistsException.java`, `DesignNotFoundException.java` — 커스텀 예외 생성
4. `GlobalExceptionHandler.java` — 새 예외 핸들러 추가 (409, 404)
5. `DesignService.java` — 저장, 중복 검사, 조회 로직 구현
6. `DesignController.java` — 엔드포인트 구현
7. `DesignServiceTest.java` — Service 단위 테스트
8. `DesignControllerTest.java` — Controller MockMvc 테스트
