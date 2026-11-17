# DESIGN-{ID}

## Overview

이 설계가 해결하는 문제

---

## Architecture Overview

### System Structure

* Controller Layer
* Service Layer
* Domain Layer
* Repository Layer

---

## API Design

### Endpoint: POST /example

Request:

```json
{
}
```

Response:

```json
{
}
```

Error:

* 400
* 500

---

## Data Model

### Entity: Example

| Field | Type   | Description |
| ----- | ------ | ----------- |
| id    | Long   | PK          |
| name  | String | 이름          |

---

## Package Structure

```
com.project
 ├── controller
 ├── service
 ├── domain
 ├── repository
 ├── dto
```

---

## Key Design Decisions

* Decision 1
* Decision 2

---

## Trade-offs

| Option | Pros | Cons |
| ------ | ---- | ---- |
| A      | ...  | ...  |
| B      | ...  | ...  |

---

## Non-Functional Design

* Performance 고려
* Security 고려
* Scalability 고려

---

## Implementation Guide

* Step 1
* Step 2
* Step 3
