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

Spring 서비스는 위 레이어를 기준으로 적는다. Python 서비스(`services/<name>/`)는 이 DESIGN이 정한 구성 요소와 그 책임을 적는다.

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

외부 의존(다른 서비스, AI 모델 등)을 호출하는 엔드포인트는 타임아웃 값과, 의존성 오류와 타임아웃 각각의 상태 코드와 사유를 함께 적는다.

---

## Data Model

### Entity: Example

| Field | Type   | Description |
| ----- | ------ | ----------- |
| id    | Long   | PK          |
| name  | String | 이름          |

Relationships: 없음 (있으면 대상 Entity와 관계를 적는다)

영속 저장이 없는 서비스는 Entity 대신 Model(요청, 응답, 내부 값)을 적고 Persistence를 표기한다.

### Model: ExampleRequest

| Field | Type   | Constraints | Description |
| ----- | ------ | ----------- | ----------- |
| text  | string | 필수, 최대 길이 | 입력 값 |

Persistence: 없음

---

## Package Structure

Spring 서비스:

```
com.arok2.dev_agent
 ├── controller/
 ├── service/
 ├── domain/
 ├── repository/
 └── dto/
```

Python 서비스는 `services/<name>/`을 루트로, 이 DESIGN이 정한 모듈 구성을 적는다.

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

선택 이유: ...

검토한 대안을 표에 적고 선택 이유를 적는다. 실제로 검토할 대안이 없으면 표 대신 "대안 없음: 이유"를 적는다.

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
