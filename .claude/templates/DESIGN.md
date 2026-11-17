# DESIGN-{ID}

## Overview

이 설계가 해결하는 문제와 범위

---

## Architecture Overview

레이어 구조 및 컴포넌트 다이어그램 (텍스트)

* Controller Layer
* Service Layer
* Domain Layer
* Repository Layer

---

## API Design

### POST /example

Request:

```json
{
}
```

Response 201:

```json
{
}
```

Error:

* 400: 이유
* 500: 이유

엔드포인트마다 HTTP Method, Path, Request body(해당 시), 성공 상태 코드가 붙은 Response body, 상태 코드별 Error 사유를 모두 적는다.

---

## Data Model

### Entity: Example

| Field | Type   | Description |
| ----- | ------ | ----------- |
| id    | Long   | PK          |
| name  | String | 이름          |

Relationships: 없음 (있으면 대상 Entity와 관계를 적는다)

---

## Package Structure

```
com.arok2.dev_agent
 ├── controller/
 ├── service/
 ├── domain/
 ├── repository/
 └── dto/
```

---

## Key Design Decisions

* Decision 1: 이유
* Decision 2: 이유

---

## Trade-offs

| Option | Pros | Cons | Selected |
| ------ | ---- | ---- | -------- |
| A      | ...  | ...  | ✅        |
| B      | ...  | ...  |          |

---

## Non-Functional Design

* 성능: ...
* 보안: ...
* 확장성: ...

---

## Implementation Guide

Implementer를 위한 구현 순서 및 주의사항

1. Step 1
2. Step 2
3. Step 3
