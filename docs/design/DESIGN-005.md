# DESIGN-005

## Overview
Review 관리 API를 구현한다.
Review는 메모리(ConcurrentHashMap)에 저장되고, REST 엔드포인트를 통해 저장/조회된다.
각 Review는 연관된 Implementation ID, Task ID를 가지며 Implementation ID로도 조회 가능하다.

## Architecture Overview

```
Client
  │ POST /reviews
  │ GET  /reviews
  │ GET  /reviews/{reviewId}
  │ GET  /reviews/by-impl/{implId}
  ▼
ReviewController
  │ reviewId, implId, taskId, status, comments
  ▼
ReviewService
  │ Review 생성 → ConcurrentHashMap 저장
  │ 중복 검사 → ReviewAlreadyExistsException
  │ 조회 → ReviewNotFoundException
  ▼
ReviewResponse / ErrorResponse
```

레이어:
- Controller: HTTP 요청 수신, 경로 변수/바디 추출
- Service: Review 저장, 중복 검사, 조회 로직
- Domain: Review (불변 Record)
- DTO: ReviewCreateRequest, ReviewResponse

## API Design

### POST /reviews

Request:
```json
{
  "reviewId": "REVIEW-001",
  "implId": "IMPL-001",
  "taskId": "TASK-001",
  "status": "APPROVED",
  "comments": [
    "코드 구조가 명확합니다.",
    "예외 처리가 잘 되어 있습니다."
  ]
}
```

Response 201:
```json
{
  "reviewId": "REVIEW-001",
  "implId": "IMPL-001",
  "taskId": "TASK-001",
  "status": "APPROVED",
  "comments": [
    "코드 구조가 명확합니다.",
    "예외 처리가 잘 되어 있습니다."
  ],
  "createdAt": "2026-06-29T10:00:00Z"
}
```

Error 409 (중복 reviewId):
```json
{
  "code": "DUPLICATE_REVIEW",
  "message": "Review already exists: REVIEW-001"
}
```

### GET /reviews

Response 200:
```json
[
  {
    "reviewId": "REVIEW-001",
    "implId": "IMPL-001",
    "taskId": "TASK-001",
    "status": "APPROVED",
    "comments": ["..."],
    "createdAt": "2026-06-29T09:00:00Z"
  }
]
```

### GET /reviews/{reviewId}

Response 200: (POST와 동일한 구조)

Error 404:
```json
{
  "code": "REVIEW_NOT_FOUND",
  "message": "Review not found: REVIEW-999"
}
```

### GET /reviews/by-impl/{implId}

Response 200: (POST와 동일한 구조)

Error 404:
```json
{
  "code": "REVIEW_NOT_FOUND",
  "message": "Review not found for implementation: IMPL-999"
}
```

## Data Model

### Review (Domain Record)

| Field     | Type         | Description                          |
|-----------|--------------|--------------------------------------|
| reviewId  | String       | Review 식별자 (예: REVIEW-001)        |
| implId    | String       | 연관 Implementation 식별자            |
| taskId    | String       | 연관 Task 식별자                      |
| status    | String       | APPROVED / REJECTED / NEEDS_REVISION |
| comments  | List<String> | 리뷰 코멘트 목록                       |
| createdAt | Instant      | 저장 시각                             |

### ReviewCreateRequest (DTO Record)

| Field    | Type         | Description                 |
|----------|--------------|-----------------------------|
| reviewId | String       | Review 식별자                |
| implId   | String       | 연관 Implementation 식별자   |
| taskId   | String       | 연관 Task 식별자             |
| status   | String       | 초기 상태                    |
| comments | List<String> | 리뷰 코멘트 목록              |

### ReviewResponse (DTO Record)

| Field     | Type         | Description                 |
|-----------|--------------|-----------------------------|
| reviewId  | String       | Review 식별자                |
| implId    | String       | 연관 Implementation 식별자   |
| taskId    | String       | 연관 Task 식별자             |
| status    | String       | 현재 상태                    |
| comments  | List<String> | 리뷰 코멘트 목록              |
| createdAt | Instant      | 저장 시각                    |

## Package Structure

```
com.arok2.dev_agent
├── review/
│   ├── controller/
│   │   └── ReviewController.java
│   ├── service/
│   │   └── ReviewService.java
│   ├── domain/
│   │   └── Review.java
│   └── dto/
│       ├── ReviewCreateRequest.java
│       └── ReviewResponse.java
└── common/
    └── exception/
        ├── ReviewAlreadyExistsException.java
        └── ReviewNotFoundException.java
```

## Key Design Decisions
- Review, ReviewCreateRequest, ReviewResponse를 Java Record로 정의
- comments는 List<String>으로 저장 (개별 코멘트 문자열 목록)
- status는 APPROVED / REJECTED / NEEDS_REVISION 세 가지
- 저장소는 두 개의 ConcurrentHashMap 사용
  - `reviewId → Review` (reviewId 기반 조회용)
  - `implId → Review` (implId 기반 조회용)

## Trade-offs

| Option | Pros | Cons | Selected |
|--------|------|------|----------|
| comments를 List<String>으로 저장 | 단순, 개별 코멘트 분리 가능 | 구조화된 코멘트 불가 | ✅ (Phase 5) |
| comments를 단일 String으로 저장 | 더 단순 | 파싱 불가, 구조 없음 | 미채택 |

## Implementation Guide

구현 순서:
1. `Review.java` — Domain Record 생성
2. `ReviewCreateRequest.java`, `ReviewResponse.java` — DTO Record 생성
3. `ReviewAlreadyExistsException.java`, `ReviewNotFoundException.java` — 커스텀 예외 생성
4. `GlobalExceptionHandler.java` — 새 예외 핸들러 추가 (409, 404)
5. `ReviewService.java` — 저장, 중복 검사, 조회 로직 구현
6. `ReviewController.java` — 엔드포인트 구현
7. `ReviewServiceTest.java` — Service 단위 테스트
8. `ReviewControllerTest.java` — Controller MockMvc 테스트
