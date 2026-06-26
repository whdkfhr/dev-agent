# DESIGN-{ID}

## 1. Overview

이 설계가 해결하는 문제

---

## 2. Architecture Overview

### System Structure

* Controller Layer
* Service Layer
* Domain Layer
* Repository Layer

---

## 3. API Design

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

## 4. Data Model

### Entity: Example

| Field | Type   | Description |
| ----- | ------ | ----------- |
| id    | Long   | PK          |
| name  | String | 이름          |

---

## 5. Package Structure

```
com.project
 ├── controller
 ├── service
 ├── domain
 ├── repository
 ├── dto
```

---

## 6. Key Design Decisions

* Decision 1
* Decision 2

---

## 7. Trade-offs

| Option | Pros | Cons |
| ------ | ---- | ---- |
| A      | ...  | ...  |
| B      | ...  | ...  |

---

## 8. Non-Functional Design

* Performance 고려
* Security 고려
* Scalability 고려

---

## 9. Implementation Guide (for Implementer)

* Step 1
* Step 2
* Step 3
