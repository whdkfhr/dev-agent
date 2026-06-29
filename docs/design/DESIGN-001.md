# DESIGN-001

## Overview
GitHub Webhook 이벤트를 수신하는 REST 엔드포인트를 구현한다.
이벤트는 메모리에 저장되고 로그로 출력된다.

## Architecture Overview

```
GitHub
  │ POST /webhook/github
  │ Header: X-GitHub-Event
  ▼
WebhookController
  │ eventType, rawPayload
  ▼
WebhookService
  │ GitHubEvent 생성 → ConcurrentHashMap 저장 → 로그 출력
  ▼
WebhookResponse (eventId, eventType, status)
```

레이어:
- Controller: HTTP 요청 수신, 헤더 추출
- Service: 이벤트 처리, 저장, 로그
- Domain: GitHubEvent (불변 Record)
- DTO: WebhookResponse (불변 Record)

## API Design

### POST /webhook/github

Request:
```
Header: X-GitHub-Event: issues
Body: (GitHub webhook raw JSON payload)
```

Response 200:
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "issues",
  "status": "RECEIVED"
}
```

Error 400 (X-GitHub-Event 헤더 없음):
```json
{
  "code": "MISSING_HEADER",
  "message": "Required request header 'X-GitHub-Event' is not present"
}
```

Error 500:
```json
{
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred"
}
```

## Data Model

### GitHubEvent (Domain Record)

| Field       | Type    | Description               |
|-------------|---------|---------------------------|
| id          | String  | UUID (PK)                 |
| eventType   | String  | X-GitHub-Event 헤더 값    |
| action      | String  | payload.action 필드       |
| rawPayload  | String  | 원본 JSON payload          |
| receivedAt  | Instant | 수신 시각                  |

### WebhookResponse (DTO Record)

| Field     | Type   | Description      |
|-----------|--------|------------------|
| eventId   | String | 생성된 이벤트 UUID |
| eventType | String | 이벤트 타입       |
| status    | String | 항상 "RECEIVED"  |

### ErrorResponse (DTO Record)

| Field   | Type   | Description   |
|---------|--------|---------------|
| code    | String | 에러 코드     |
| message | String | 에러 메시지   |

## Package Structure

```
com.arok2.dev_agent
├── webhook/
│   ├── controller/
│   │   └── WebhookController.java
│   ├── service/
│   │   └── WebhookService.java
│   ├── domain/
│   │   └── GitHubEvent.java
│   └── dto/
│       └── WebhookResponse.java
└── common/
    └── exception/
        ├── GlobalExceptionHandler.java
        └── ErrorResponse.java
```

## Key Design Decisions
- GitHubEvent, WebhookResponse, ErrorResponse를 Java Record로 정의 (불변성 보장)
- 저장소는 ConcurrentHashMap (Phase 3에서 PostgreSQL로 교체 예정)
- ObjectMapper를 통해 payload에서 action 필드만 추출
- GlobalExceptionHandler로 에러 응답 일원화

## Trade-offs

| Option | Pros | Cons | Selected |
|--------|------|------|----------|
| ConcurrentHashMap | 단순, 의존성 없음 | 재시작 시 소멸 | ✅ (Phase 1) |
| H2 인메모리 DB | 영속성, 쿼리 가능 | 설정 복잡 | Phase 3 |

## Non-Functional Design
- 성능: ConcurrentHashMap으로 동시 요청 안전 처리
- 보안: Signature 검증은 Out of Scope (Phase 2에서 추가)
- 확장성: WebhookService는 인터페이스로 추상화하지 않음 (단순함 우선)

## Implementation Guide

구현 순서:
1. `GitHubEvent.java` — Domain Record 생성
2. `WebhookResponse.java`, `ErrorResponse.java` — DTO Record 생성
3. `WebhookService.java` — 이벤트 처리 로직 구현
4. `WebhookController.java` — 엔드포인트 구현
5. `GlobalExceptionHandler.java` — 전역 예외 처리
6. `WebhookServiceTest.java` — Service 단위 테스트
7. `WebhookControllerTest.java` — Controller MockMvc 테스트
