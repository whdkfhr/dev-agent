# DESIGN-021

## Overview
TASK-021에 대한 설계다. DESIGN-007의 AgentRun API 표면과 데이터 모델은 그대로 두고, 다음 두 가지를 고친다.

1. 같은 runId에 대한 생성, 상태 전이, step log 추가가 "조회 → 새 record 생성 → 저장"으로 나뉘어 있어, 동시 요청에서 중복 생성 성공, step log 유실, 상태 되돌림, 같은 전이의 중복 성공이 일어난다.
2. 필수 입력 검증이 없어 runId 누락 시 500이 나고, Issue 식별자·step 이름·step 상태가 없는 값이 저장된다.

범위는 단일 애플리케이션 인스턴스 안의 동시 요청이다. DESIGN-007은 참조 문서로 유지하며, 이 문서는 DESIGN-007과 다른 부분만 정한다. 여기서 정하지 않은 API, 상태 순서, Tool Registry 동작은 DESIGN-007을 따른다.

이 설계는 네트워크 재시도 중복 제거 기능을 추가하지 않는다. 테스트의 "정확히 한 번"은 테스트가 보낸 각 요청에 대한 저장 결과를 뜻한다. 같은 요청을 클라이언트가 다시 보내면 생성은 409를 받고, step log는 별개의 로그로 한 번 더 저장된다.

## Architecture Overview

```
AgentRunController            (변경 없음: 요청을 서비스에 위임)
     |
     v
AgentRunService
     |
     |  생성
     |    1. 입력 검증 (runId, issueId) ------------- 실패: InvalidAgentRunRequestException (400)
     |    2. 원자적 "없을 때만 넣기" ---------------- 이미 있음: AgentRunAlreadyExistsException (409, 기존)
     |
     |  상태 전이 (기존 우선순위 유지)
     |    1. run 존재 사전 확인 --------------------- 없음: AgentRunNotFoundException (404, 기존)
     |    2. nextStatus null 확인 ------------------- null: InvalidAgentRunStatusTransitionException (400, 기존)
     |    3. 원자적 "있을 때만 갱신"
     |         갱신 시점 run 부재 ------------------- AgentRunNotFoundException (404)
     |         갱신 안에서 현재 상태 기준 전이 검사 -- 실패: InvalidAgentRunStatusTransitionException (400, 기존)
     |
     |  step 추가
     |    1. 입력 검증 (stepName, toolName, status) -- 실패: InvalidAgentRunRequestException (400)
     |    2. run 존재 사전 확인 --------------------- 없음: AgentRunNotFoundException (404, 기존 우선순위)
     |    3. tool 등록 확인 ------------------------- 없음: ToolNotFoundException (404, 기존)
     |    4. 원자적 "있을 때만 갱신"
     |         갱신 시점 run 부재 ------------------- AgentRunNotFoundException (404)
     |         갱신 안에서 현재 값에 로그 1건 추가
     v
ConcurrentHashMap<String, AgentRun>   (인메모리, 변경 없음)

GlobalExceptionHandler        (InvalidAgentRunRequestException 매핑 1개만 추가)
```

- 레이어 구조와 인메모리 저장 방식은 그대로다.
- 원자성은 저장소 맵이 제공하는 키 단위 원자적 연산으로 얻는다. 새 락 객체나 새 저장소 계층을 만들지 않는다.
- AgentRun과 AgentStepLog는 지금처럼 불변 record로 두고, 갱신은 "현재 값으로부터 새 값을 만드는 함수"를 키 단위 원자적 연산에 넘기는 방식으로 한다.

## API Design

아래 세 엔드포인트의 성공 응답 형식은 DESIGN-007과 같다. 바뀌는 것은 입력 검증 오류와 동시 요청에서의 결과 보장이다. 조회 API(`GET /agent-runs`, `GET /agent-runs/{runId}`)와 Tool Registry API(`GET /tools`, `GET /tools/{toolName}`)는 바뀌지 않는다.

"빈 값"은 필드 없음, `null`, 빈 문자열, 공백 문자만으로 된 문자열을 모두 뜻한다.

### POST /agent-runs

Request:
```json
{
  "runId": "RUN-001",
  "issueId": "ISSUE-123",
  "issueTitle": "Add health check endpoint"
}
```

Response 201:
```json
{
  "runId": "RUN-001",
  "issueId": "ISSUE-123",
  "issueTitle": "Add health check endpoint",
  "status": "PLANNING",
  "stepLogs": [],
  "createdAt": "2026-11-22T00:00:00Z",
  "updatedAt": "2026-11-22T00:00:00Z"
}
```

Error:
- 400 `INVALID_AGENT_RUN_REQUEST`: `runId` 또는 `issueId`가 빈 값. 저장하지 않는다. (변경: `runId` 없음은 기존 500, 공백 `runId`와 빈 `issueId`는 기존 201)
- 409 `DUPLICATE_AGENT_RUN`: 같은 `runId`가 이미 있음. 동시에 같은 `runId`로 요청하면 정확히 한 요청만 201을 받고 나머지는 409를 받는다. 저장된 값은 201을 받은 요청의 값이다. (기존 코드 유지, 동시성 보장 추가)
- 500 `INTERNAL_ERROR`: 깨진 JSON 등 요청 본문 변환 실패 (기존 동작, 이번 범위 밖)

`issueTitle`은 선택 값이다. 없거나 빈 값이어도 201이다(기존 유지).

### POST /agent-runs/{runId}/advance

Request:
```json
{
  "nextStatus": "DESIGNING"
}
```

Response 200: AgentRunResponse (DESIGN-007과 같음)

Error (검사 순서대로, 기존 우선순위와 같음):
- 404 `AGENT_RUN_NOT_FOUND`: `runId`가 없음. `nextStatus`가 없거나 `null`이거나 건너뛰는 전이여도 run이 없으면 404가 먼저다. (기존 우선순위 유지. 2026-11-22 develop `ff28b04`에서 존재하지 않는 run에 `nextStatus` 없음, `null`, `TESTING`을 보내면 모두 404임을 확인)
- 400 `INVALID_AGENT_RUN_STATUS_TRANSITION`: `nextStatus`가 없거나 `null`(기존), 또는 갱신 시점의 현재 상태에서 `nextStatus`로 갈 수 없음. 같은 `nextStatus`로 동시에 여러 요청이 오면 정확히 한 요청만 200을 받고, 나머지는 먼저 성공한 전이 뒤의 상태를 기준으로 검사되어 이 400을 받는다. (기존 코드 유지, 409를 새로 쓰지 않는다)
- 404 `AGENT_RUN_NOT_FOUND`: 사전 확인 뒤 갱신 시점에 run이 없음. 현재는 삭제 API가 없어 일어나지 않지만, 갱신 결과가 비어 있으면 404로 처리한다.
- 500 `INTERNAL_ERROR`: `nextStatus`에 빈 문자열이나 정의되지 않은 값 등 요청 본문 변환 실패. 서비스에 도달하기 전에 실패하므로 run 존재와 상관없이 500이다. (기존 동작, 이번 범위 밖)

상태 전이에는 새 입력 검증을 추가하지 않는다. 우선순위는 기존과 같이 "run 존재 → nextStatus null → 전이 가능 여부"이다.

### POST /agent-runs/{runId}/steps

Request:
```json
{
  "stepName": "Create task document",
  "input": "GitHub Issue body",
  "output": "docs/tasks/TASK-021.md",
  "toolName": "TaskDocumentTool",
  "status": "SUCCESS",
  "retry": false
}
```

Response 201: AgentRunResponse (DESIGN-007과 같음). 응답의 `stepLogs`는 이 요청의 로그가 추가된 직후의 값이다. 동시에 들어온 다른 요청의 로그가 포함될 수도 있다.

Error (검사 순서대로):
- 400 `INVALID_AGENT_RUN_REQUEST`: `stepName` 또는 `toolName`이 빈 값이거나, `status`가 없거나 `null`. 저장하지 않는다. (변경: 기존에는 `stepName`·`status` 누락이 201, `toolName` 누락·공백이 404)
- 404 `AGENT_RUN_NOT_FOUND`: `runId`가 없음. tool이 등록되지 않았어도 run이 없으면 이 404가 먼저다. (기존 우선순위 유지)
- 404 `TOOL_NOT_FOUND`: run은 있고 `toolName`이 빈 값이 아니지만 Tool Registry에 없음 (기존 유지)
- 404 `AGENT_RUN_NOT_FOUND`: 사전 확인 뒤 갱신 시점에 run이 없음. 현재는 삭제 API가 없어 일어나지 않지만, 갱신 결과가 비어 있으면 404로 처리한다.
- 500 `INTERNAL_ERROR`: `status`에 빈 문자열·공백·정의되지 않은 값, `retry`에 불리언이 아닌 값 등 요청 본문 변환 실패. 서비스에 도달하기 전에 실패하므로 run 존재와 상관없이 500이며 저장되지 않는다. (기존 동작, 이번 범위 밖)

동시에 들어온 유효한 step log 요청은 각각 201을 받고, 각 요청의 로그는 정확히 한 번씩 저장된다. 상태 전이와 겹쳐도 로그와 전이 결과가 모두 남는다.

검사 순서는 "입력 → run 존재 → tool → 원자적 갱신"이다. 기존 순서 "run → tool"은 그대로 유지되고, 그 앞에 새 입력 검증만 추가된다. 존재하지 않는 run에 등록되지 않은 tool로 요청하면 기존과 같이 `AGENT_RUN_NOT_FOUND`이다.

새 입력 검증이 run 확인보다 앞에 오므로, 존재하지 않는 run에 필수 값이 빠진 요청은 기존 404(2026-11-22 develop `ff28b04`에서 `toolName` 없음, `status` 없음 모두 404 확인) 대신 400을 받는다. 이 조합은 기존에 입력 검증이 없어서 404가 나던 경우이며, 입력 오류를 먼저 알려 준다는 TASK-021의 요구에 따른 의도된 변경이다.

run 존재 사전 확인은 오류 우선순위를 위한 것이고, 원자성은 4단계의 갱신이 보장한다. 사전 확인과 갱신 사이에 run이 사라지는 경우도 갱신 결과로 다시 판단해 404로 처리한다.

## Data Model

### Model: AgentRun

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| runId | String | 필수, 빈 값 불가, 저장소 키, 중복 불가 | 실행 ID |
| issueId | String | 필수, 빈 값 불가 (변경) | GitHub Issue ID 또는 번호 |
| issueTitle | String | 선택 | GitHub Issue 제목 |
| status | AgentRunStatus | 필수 | 현재 상태. DESIGN-007의 순서로만 전이 |
| stepLogs | List<AgentStepLog> | 불변 리스트, 추가만 가능 | 성공한 step log를 추가 순서대로 보관 |
| createdAt | Instant | 필수 | 생성 시각 |
| updatedAt | Instant | 필수 | 마지막으로 원자적 갱신이 반영된 시각 |

Persistence: 없음 (인메모리, 재시작 시 사라짐)

### Model: AgentStepLog

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| stepName | String | 필수, 빈 값 불가 (변경) | step 이름 |
| input | String | 선택 | 입력 |
| output | String | 선택 | 출력 |
| toolName | String | 필수, 빈 값 불가, Tool Registry에 등록된 이름 | 사용 tool |
| status | AgentStepStatus | 필수, SUCCESS 또는 FAILED (변경: null 불가) | step 결과 |
| retry | boolean | 기본값 false | 재시도 여부 |
| createdAt | Instant | 필수 | 서버가 기록한 시각 |

Persistence: 없음

### Model: AgentRunCreateRequest

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| runId | String | 빈 값이면 400 | 실행 ID |
| issueId | String | 빈 값이면 400 (API 계약 변경) | Issue 식별자 |
| issueTitle | String | 선택 | Issue 제목 |

Persistence: 없음

### Model: AgentStepLogCreateRequest

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| stepName | String | 빈 값이면 400 | step 이름 |
| input | String | 선택 | 입력 |
| output | String | 선택 | 출력 |
| toolName | String | 빈 값이면 400, 미등록이면 404 | tool 이름 |
| status | AgentStepStatus | 없거나 null이면 400. 정의되지 않은 문자열은 본문 변환 단계에서 실패(범위 밖) | step 결과 |
| retry | boolean | 없으면 false | 재시도 여부 |

Persistence: 없음

### Model: ErrorResponse (기존)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| code | String | 필수 | 오류 코드. 새 코드 `INVALID_AGENT_RUN_REQUEST` 추가 |
| message | String | 필수 | 어느 필드가 왜 잘못됐는지. 예: "issueId must not be blank". 요청 값 자체는 넣지 않는다 |

Persistence: 없음

## Package Structure

```
com.arok2.dev_agent
├── agentrun/
│   ├── controller/
│   │   ├── AgentRunController.java          (변경 없음)
│   │   └── ToolRegistryController.java      (변경 없음)
│   ├── domain/                              (변경 없음)
│   ├── dto/                                 (변경 없음)
│   └── service/
│       ├── AgentRunService.java             (변경: 입력 검증, 원자적 갱신)
│       └── ToolRegistryService.java         (변경 없음)
└── common/
    └── exception/
        ├── InvalidAgentRunRequestException.java   (신규)
        └── GlobalExceptionHandler.java            (변경: 위 예외의 400 매핑 1개 추가)

src/test/java/com/arok2/dev_agent/agentrun/
├── service/
│   ├── AgentRunServiceTest.java                   (변경: 입력 검증 테스트 추가)
│   └── AgentRunServiceConcurrencyTest.java        (신규)
└── controller/
    └── AgentRunControllerTest.java                (변경: 입력 오류 테스트 추가)
```

## Key Design Decisions
- runId 단위 원자성은 저장소 맵(ConcurrentHashMap)의 키 단위 원자적 연산으로 얻는다. 생성은 "없을 때만 넣기", 상태 전이와 step 추가는 "있을 때만 현재 값으로부터 새 값 계산"을 쓴다. 이유: 같은 키에 대한 갱신이 맵 안에서 원자적으로 처리되고, 별도 락 객체의 생성·정리가 필요 없다. 다른 runId의 요청은 일반적으로 서로 기다리지 않지만, 맵의 같은 해시 구간에 속하면 갱신이 짧게 대기할 수 있다. 이 대기는 성능에만 영향을 주며 결과의 정확성에는 영향을 주지 않는다.
- 상태 전이 검사는 원자적 갱신 안에서 그 시점의 현재 상태를 기준으로 한다. 이유: 갱신 밖에서 검사하면 검사와 저장 사이에 다른 갱신이 끼어 상태가 되돌아가거나 같은 전이가 두 번 성공한다. 갱신 안에서 예외를 던지면 맵은 기존 값을 유지한다.
- 원자적 갱신 함수 안에서는 현재 값으로 새 record를 만드는 일만 한다. Tool Registry 조회, 입력 검증, 다른 키 접근, 대기, 외부 호출은 하지 않는다. 이유: 이 연산은 키가 속한 구간을 잠근 채 실행되므로, 함수가 길거나 다른 갱신을 기다리면 처리량이 떨어지거나 교착할 수 있다.
- 입력 검증, run 존재 사전 확인, tool 등록 확인은 원자적 갱신 전에 서비스 계층에서 한다. 이유: 비즈니스 규칙은 서비스에서 검증한다는 기존 원칙(TASK-007 Non-Functional)을 따르고, 서비스 단위 테스트로 검증할 수 있다. Tool Registry는 기동 후 바뀌지 않으므로 갱신 밖에서 확인해도 결과가 같다.
- 오류 우선순위는 기존 API의 순서를 유지한다. 상태 전이는 "run 존재 → nextStatus null → 전이 가능 여부", step 추가는 기존 "run → tool" 앞에 새 입력 검증만 둔다. 이유: 불필요한 오류 코드 변경을 피한다. run 존재는 사전 확인으로 우선순위를 맞추고, 갱신 시점의 부재도 갱신 결과로 다시 처리해 원자성과 우선순위를 분리한다.
- Bean Validation(spring-boot-starter-validation)은 이번에 도입하지 않는다. 이유: TASK-021 Non-Functional이 운영 의존성 추가를 금지한다. 프로젝트의 다른 API도 Bean Validation을 쓰지 않는다.
- 입력 오류는 새 예외 하나와 새 오류 코드 `INVALID_AGENT_RUN_REQUEST`(400)로 표현하고, GlobalExceptionHandler에는 이 예외의 매핑 하나만 추가한다. 이유: 기존 `Exception` catch-all과 다른 API의 처리를 건드리지 않아, 앱 전체의 본문 변환 실패 처리(범위 밖)와 겹치지 않는다.
- 같은 전이의 동시 요청에서 진 요청은 기존 `INVALID_AGENT_RUN_STATUS_TRANSITION`(400)을 받는다. 이유: 갱신 안에서 현재 상태를 다시 검사하면 진 요청은 "DESIGNING → DESIGNING" 같은 잘못된 전이가 되어 기존 계약에 그대로 해당한다. 새 409 코드를 만들지 않는다.
- step log 리스트는 계속 불변 리스트로 두고 추가할 때마다 복사한다. 이유: 응답과 저장소가 같은 리스트를 공유해도 안전하다. 추가 비용은 로그 수에 비례하지만, 실행당 step 수가 작은 현재 용도에서는 문제가 되지 않는다(Non-Functional Design 참조).
- 네트워크 재시도 중복 제거(요청 ID 기반 멱등성)는 추가하지 않는다. 이유: TASK-021 범위 밖이며, 같은 step log를 다시 보내면 별개의 로그로 저장되는 것이 현재 계약이다.
- 동시성 테스트는 운영 코드에 테스트용 주입 지점을 두지 않고, 시작 배리어와 불변식 검증으로 작성한다. 이유와 대안 비교는 Trade-offs의 두 번째 표에 적는다.

## Trade-offs

### 1. runId 단위 원자적 처리 방식

| Option | Pros | Cons | Selected |
|--------|------|------|----------|
| A. 맵의 키 단위 원자적 연산 (없을 때만 넣기, 있을 때만 계산해 갱신) | 새 객체·락 관리 없음. 같은 runId의 갱신은 원자적. 다른 runId는 대체로 서로 기다리지 않음. 갱신 중 예외 시 기존 값 유지. 기존 불변 record와 맞음 | 갱신 함수 안에서 오래 걸리는 일이나 다른 키 갱신을 하면 안 된다는 제약을 지켜야 함. 다른 runId라도 같은 해시 구간이면 갱신이 짧게 대기할 수 있음 | ✅ |
| B. runId별 락 객체(별도 맵에 보관) | 임계 구역 안에서 자유롭게 여러 단계를 처리 | 락 객체 생성·정리 관리 필요. 락 맵과 데이터 맵 두 개를 일관되게 유지해야 함. 코드가 늘어남 | |
| C. 서비스 메서드 전체 동기화 | 가장 단순 | 모든 runId 요청이 하나씩 처리됨. 조회까지 막을지 별도로 정해야 함 | |
| D. 낙관적 갱신(이전 값과 같을 때만 교체, 실패 시 재시도) | 락 없음 | 재시도 반복 처리와 재시도 한도 설계가 필요하고, 경합이 많으면 반복이 늘어남 | |
| E. AgentRun을 변경 가능한 객체로 바꾸고 내부에서 동기화 | 로그 추가가 복사 없이 가능 | 불변 record라는 기존 도메인 설계를 바꿈. 응답 생성 시 일관된 스냅샷을 따로 만들어야 함 | |

선택 이유: A가 기존 저장 구조와 불변 record를 그대로 둔 채 TASK-021의 네 가지 동시성 요구(중복 생성, 로그 유실, 전이·로그 겹침, 같은 전이 중복)를 모두 같은 방식으로 해결하고, 추가 구조가 가장 적다. A의 제약(갱신 함수 안에서 대기·외부 호출·다른 키 접근 금지)은 Implementation Guide에 명시해 지킨다.

### 2. 동시성 검증 방법

| Option | Pros | Cons | Selected |
|--------|------|------|----------|
| A. 원자적 갱신 함수 안의 테스트용 주입 지점 | 정확한 실행 순서를 강제 | 키 구간을 잠근 채 테스트가 기다리므로, 두 번째 요청의 진행을 기다리는 테스트는 교착함. 운영 코드에 테스트 전용 경로가 생김 | |
| B. 원자적 갱신 밖(갱신 직전)의 주입 지점 | 교착 없음 | 수정 후 구조에서는 충돌 구간이 갱신 안에 있어 "읽은 뒤 쓰기 전"을 강제할 수 없음. 동시 출발만 보장하므로 C와 효과가 같고 운영 코드만 복잡해짐 | |
| C. 시작 배리어 + 불변식 검증 + 반복 (운영 코드 변경 없음) | 운영 코드가 단순. 원자적 구현이면 수정 후 결과가 실행마다 같음. 수정 전 코드에도 같은 테스트를 그대로 돌려 비교 가능 | 수정 전 코드에서의 실패는 스케줄링에 달려 있어 매번 재현된다고 보장하지 않음 | ✅ |
| D. 동시성 검증 도구(Lincheck, jcstress) | 가능한 실행 순서를 체계적으로 탐색. 수정 전 실패를 더 확실히 찾음 | 테스트 의존성 추가와 빌드 구성 변경. jcstress는 별도 실행 구조가 필요. 학습·유지 비용 | |

선택 이유: C는 운영 코드에 테스트 경로를 만들지 않고, A의 교착 위험과 B의 무의미한 복잡도를 피한다. 수정 후 원자적 구현에서는 불변식이 항상 성립하므로 회귀 테스트로 안정적이다. C의 한계(수정 전 실패의 확률성)는 수정 전 비교 실행에서 시도·실패 횟수를 기록하는 방식으로 드러낸다. D는 이번에는 채택하지 않으며, C로 결함을 놓친 사례가 생기면 별도 작업으로 검토한다.

## Non-Functional Design
- 성능: 같은 runId에 대한 갱신은 원자적으로 하나씩 반영된다. 다른 runId의 갱신은 일반적으로 서로 기다리지 않지만, 맵의 같은 해시 구간에 속하면 짧게 대기할 수 있어 항상 병렬로 처리된다고 보장하지 않는다. 이 대기는 결과의 정확성에 영향을 주지 않으며, 갱신 함수를 짧게 유지해 대기 시간을 줄인다. 조회는 갱신을 기다리지 않는다. step 추가는 기존 로그를 복사하므로 실행당 로그 수 n에 대해 추가 1건이 O(n)이다. 실행당 step이 수십 건 수준인 현재 용도에서는 허용한다. 수천 건 이상이 필요해지면 저장 구조를 다시 설계한다.
- 동시성: 단일 인스턴스 안에서 같은 runId에 대한 생성·전이·step 추가가 원자적이다. 다중 인스턴스는 범위 밖이며, 인메모리 저장소라 인스턴스 간에는 데이터를 공유하지 않는다.
- 보안: 입력 오류 메시지에는 필드 이름과 사유만 넣고 요청 값은 넣지 않는다. 인증은 기존과 같이 없다.
- 확장성: 영속 저장소로 바꿀 때는 저장소 계층의 원자적 연산(DB 제약, 낙관적 잠금 등)으로 같은 보장을 다시 설계해야 한다. 이 설계의 보장은 인메모리 맵에 한정된다.
- 호환성: 성공 응답 형식은 바뀌지 않는다. 오류 응답 변화는 API Design에 적은 항목뿐이며, `issueId` 필수화는 기존에 201이던 요청을 400으로 바꾸는 계약 변경이다. README의 AgentRun API 설명에 이를 적는다.
- 기존 테스트: 현재 AgentRun 테스트의 요청은 모두 `runId`, `issueId`, `stepName`, `toolName`, `status`를 넣고 있어 계약 변경과 충돌하는 기존 테스트는 없다(`nextStatus` 누락 400 테스트는 그대로 유지된다).

## Implementation Guide

### 1. 예외와 오류 매핑
1. `common/exception`에 `InvalidAgentRunRequestException`을 추가한다. 메시지는 "<필드> must not be blank" 또는 "status must not be null" 형식이다.
2. `GlobalExceptionHandler`에 이 예외를 400, 코드 `INVALID_AGENT_RUN_REQUEST`로 매핑하는 handler 하나만 추가한다. 기존 handler와 `Exception` catch-all은 바꾸지 않는다.

### 2. 입력 검증 (AgentRunService)
1. 생성: `runId`, `issueId`가 null이거나 공백 제거 후 비어 있으면 예외. `issueTitle`은 검사하지 않는다.
2. step 추가: `stepName`, `toolName`이 null이거나 공백 제거 후 비어 있으면 예외. `status`가 null이면 예외.
3. 검증은 run 존재 확인, tool 확인, 원자적 갱신보다 먼저 한다. 저장 값은 요청 값을 그대로 쓴다(공백을 잘라 저장하지 않는다).
4. 상태 전이에는 새 입력 검증을 추가하지 않는다.

### 3. 원자적 갱신 (AgentRunService)
1. 생성: 새 AgentRun을 만든 뒤 "없을 때만 넣기"로 저장한다. 기존 값이 돌아오면 `AgentRunAlreadyExistsException`. 먼저 존재 여부를 확인하는 코드는 지운다.
2. 상태 전이: 먼저 run 존재를 확인해 없으면 `AgentRunNotFoundException`(기존 우선순위). 그다음 `nextStatus`가 null이면 기존처럼 `InvalidAgentRunStatusTransitionException`. 그다음 "있을 때만 갱신"으로, 갱신 함수 안에서 현재 값의 상태로 `canTransitionTo`를 검사하고, 실패하면 같은 예외를 던지고, 성공하면 상태와 `updatedAt`만 바꾼 새 AgentRun을 돌려준다. 결과가 없으면(키 없음) `AgentRunNotFoundException`.
3. step 추가: 입력 검증 → run 존재 확인(없으면 `AgentRunNotFoundException`) → Tool Registry 확인 → AgentStepLog 생성 → "있을 때만 갱신"으로, 갱신 함수 안에서 현재 값의 로그 리스트를 복사해 1건을 추가한 불변 리스트와 `updatedAt`으로 새 AgentRun을 돌려준다. 결과가 없으면 `AgentRunNotFoundException`.
4. 응답은 원자적 연산이 돌려준 값으로 만든다. 갱신 후 저장소를 다시 조회하지 않는다.
5. 갱신 함수 안에서는 Tool Registry 조회, 로그 출력을 포함한 I/O, 다른 runId 접근, 대기를 하지 않는다.

### 4. 입력 오류 테스트
1. 서비스 테스트: 생성의 `runId`·`issueId`, step의 `stepName`·`toolName`에 대해 null, 빈 문자열, 공백 문자열, step의 `status` null 각각 예외가 나고 저장 상태가 바뀌지 않는지 검증한다.
2. 컨트롤러 테스트(MockMvc): 위 필드에 대해 필드 없음, `null`, `""`, `"   "` 요청이 400과 `INVALID_AGENT_RUN_REQUEST`를 반환하고, 이후 조회 결과가 바뀌지 않았는지 확인한다. `status`는 필드 없음과 `null`만 400을 검증한다.
3. 유지 확인: `issueTitle` 없는 생성 201, 등록되지 않은 tool 404 `TOOL_NOT_FOUND`.
4. 범위 밖 확인: `status`에 `""`와 `"DONE"`을 보낸 뒤 step log 수가 바뀌지 않았는지만 확인한다. 응답 코드는 검증하지 않는다.

### 4-1. 오류 우선순위 테스트
서비스 테스트와 컨트롤러 테스트(MockMvc) 양쪽에서 확인한다. 컨트롤러 테스트는 상태 코드와 `code`를 함께 검증한다.

| 대상 | 요청 조건 | 기대 결과 | 근거 |
|------|-----------|-----------|------|
| 상태 전이 | 없는 run + `nextStatus` 없음 | 404 `AGENT_RUN_NOT_FOUND` | 기존 우선순위 유지 |
| 상태 전이 | 없는 run + `nextStatus: null` | 404 `AGENT_RUN_NOT_FOUND` | 기존 우선순위 유지 |
| 상태 전이 | 없는 run + 건너뛰는 전이 | 404 `AGENT_RUN_NOT_FOUND` | 기존 우선순위 유지 |
| 상태 전이 | 있는 run + `nextStatus` 없음 | 400 `INVALID_AGENT_RUN_STATUS_TRANSITION` | 기존 테스트 유지 |
| step 추가 | 없는 run + 등록되지 않은 tool(나머지 유효) | 404 `AGENT_RUN_NOT_FOUND` | 기존 우선순위 유지 |
| step 추가 | 있는 run + 등록되지 않은 tool | 404 `TOOL_NOT_FOUND` | 기존 유지 |
| step 추가 | 없는 run + `toolName` 없음 | 400 `INVALID_AGENT_RUN_REQUEST` | 의도된 변경(입력 검증 우선) |
| step 추가 | 없는 run + `status` 없음 | 400 `INVALID_AGENT_RUN_REQUEST` | 의도된 변경(입력 검증 우선) |
| step 추가 | 있는 run + `stepName` 공백 + 등록되지 않은 tool | 400 `INVALID_AGENT_RUN_REQUEST` | 입력 검증이 tool 확인보다 먼저 |
| 생성 | 이미 있는 runId + `issueId` 공백 | 400 `INVALID_AGENT_RUN_REQUEST`, 기존 실행 값 변경 없음 | 입력 검증이 중복 확인보다 먼저 |

갱신 시점의 run 부재(사전 확인 뒤 사라지는 경우)는 삭제 API가 없어 공개 API로 재현할 수 없다. 이 경로는 코드 리뷰에서 "갱신 결과가 비어 있으면 404"로 처리하는지 확인하고, 테스트로 강제하기 위한 주입 지점은 두지 않는다.

### 5. 동시성 회귀 테스트 (AgentRunServiceConcurrencyTest)
공통 규칙
- 테스트마다 새 AgentRunService와 ToolRegistryService를 직접 만든다(Spring 컨텍스트 불필요).
- 스레드 풀로 요청을 제출하고, 모든 작업이 시작 배리어에서 함께 출발하게 한다.
- 각 작업의 결과(성공 응답 또는 예외 종류)를 모아 검증한다. 대기에는 모두 시간 제한(예: 10초)을 두고, 넘으면 실패로 처리한다.
- 한 시나리오를 새 서비스로 여러 번(예: 50회) 반복하고, 한 번이라도 불변식이 깨지면 실패로 본다. 반복 횟수와 스레드 수는 상수로 두고 전체 실행 시간이 몇 초 안에 끝나게 정한다.
- 이 테스트는 기존 공개 API와 기존 예외만 사용해, 수정 전 코드에도 그대로 컴파일·실행되게 한다(아래 6).

시나리오와 불변식
1. 중복 생성: 같은 runId, 서로 다른 `issueId`(요청별 고유 표식)로 K건 동시 생성. 성공은 정확히 1건, 나머지는 모두 `AgentRunAlreadyExistsException`, 저장된 `issueId`는 성공한 요청의 표식과 같다.
2. step log 동시 추가: 실행 1개에 서로 다른 `stepName`(예: "step-0" ~ "step-(N-1)")으로 유효한 요청 N건 동시 추가. N건 모두 성공하고, 저장된 step log의 `stepName` 목록이 요청 표식 집합과 정확히 같으며(누락 없음, 중복 없음, 그 밖의 로그 없음), 개수가 N이다. 개수만 비교하지 않는다.
3. 전이와 로그 겹침: PLANNING 실행에 DESIGNING 전이 1건과 서로 다른 step log M건을 동시에 실행. 전이와 M건이 모두 성공하고, 최종 상태가 DESIGNING이며, 저장된 step log가 M건의 표식과 정확히 일치한다.
4. 같은 전이 중복: PLANNING 실행에 DESIGNING 전이 K건 동시 요청. 성공은 정확히 1건, 나머지는 모두 `InvalidAgentRunStatusTransitionException`, 최종 상태는 DESIGNING.

### 6. 수정 전 코드 비교 실행 (커밋하지 않음)
1. 격리된 작업 디렉터리에 PR의 base 커밋(develop)을 꺼내고, 5의 테스트 파일만 복사해 실행한다.
2. 시나리오별로 반복 횟수 중 불변식이 깨진 횟수를 기록한다. 실행 환경(JDK 버전, CPU 수)도 함께 적는다.
3. 같은 테스트를 수정 후 코드에서 실행한 결과(전 시나리오 통과)와 나란히 PR 본문에 기록한다.
4. 기록에 다음 한계를 함께 적는다.
   - 수정 전 코드의 실패는 스레드 스케줄링에 달려 있어 매번 재현된다고 보장하지 않는다. 수정 전 비교에서 어떤 시나리오가 통과해도 결함이 없다는 뜻이 아니다.
   - 수정 후 통과는 테스트한 시나리오와 반복 범위에서의 결과이며, 가능한 모든 실행 순서를 증명하지 않는다. 보장의 근거는 키 단위 원자적 연산이라는 구조에 있다.
   - 서비스 계층 직접 호출 기준이며, HTTP 부하나 다중 인스턴스는 다루지 않는다.

### 7. 문서
1. README의 API Endpoints 절에서 AgentRun API의 새 400 오류(`INVALID_AGENT_RUN_REQUEST`)와 `issueId` 필수화를 적는다. Tool Registry와 수동 기록 방식 설명은 바꾸지 않는다(별도 경량 작업).
2. TASK-021 Status를 구현과 테스트 완료 후 `IN_REVIEW`로 바꾼다. TASK-007과 DESIGN-007은 바꾸지 않는다.

### 주의사항
- 원자적 갱신 함수 안에서 예외를 던지면 맵은 기존 값을 유지한다. 이 동작에 의존하므로 갱신 함수 안에서 부분적으로 상태를 바꾸는 코드를 쓰지 않는다.
- 다른 서비스(Task, Design 등)의 같은 패턴은 고치지 않는다(TASK-021 범위 밖).
- `GlobalExceptionHandler`의 기존 handler 순서와 catch-all을 바꾸지 않는다.
