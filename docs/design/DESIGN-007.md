# DESIGN-007

## Overview
AgentRun은 하나의 GitHub Issue 처리 실행을 나타내는 도메인이다.
이 실행은 Agent pipeline 상태와 Tool Use 기반 step log를 함께 보관한다.

이번 설계는 실제 LLM 호출이나 GitHub PR 생성까지 수행하지 않고,
AI Agent orchestration의 핵심 추적 모델과 API 표면을 Spring Boot 코드로 명확히 표현한다.

## Architecture Overview

GitHub Issue
     |
     v
POST /agent-runs
     |
     v
AgentRunService
     |
     +-- AgentRun status: PLANNING -> DESIGNING -> IMPLEMENTING -> TESTING -> REVIEWING -> PR_READY
     |
     +-- stepLogs[]: input, output, toolName, status, retry

ToolRegistryController
     |
     v
ToolRegistryService
     |
     v
AgentTool definitions

## API Design

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
  "createdAt": "2026-08-05T00:00:00Z",
  "updatedAt": "2026-08-05T00:00:00Z"
}
```

### GET /agent-runs

Response 200:
```json
[
  {
    "runId": "RUN-001",
    "issueId": "ISSUE-123",
    "issueTitle": "Add health check endpoint",
    "status": "PLANNING",
    "stepLogs": [],
    "createdAt": "2026-08-05T00:00:00Z",
    "updatedAt": "2026-08-05T00:00:00Z"
  }
]
```

### GET /agent-runs/{runId}

Response 200: AgentRunResponse

### POST /agent-runs/{runId}/advance

Request:
```json
{
  "nextStatus": "DESIGNING"
}
```

Response 200: AgentRunResponse

### POST /agent-runs/{runId}/steps

Request:
```json
{
  "stepName": "Create task document",
  "input": "GitHub Issue body",
  "output": "docs/tasks/TASK-007.md",
  "toolName": "TaskDocumentTool",
  "status": "SUCCESS",
  "retry": false
}
```

Response 201: AgentRunResponse

### GET /tools

Response 200:
```json
[
  {
    "name": "RepositoryContextTool",
    "description": "README, build.gradle, package structure를 조회한다.",
    "inputSchema": "repositoryRoot",
    "outputSchema": "repositoryContext"
  }
]
```

### GET /tools/{toolName}

Response 200: ToolDefinitionResponse

## Data Model

### AgentRun

| Field      | Type               | Description |
|------------|--------------------|-------------|
| runId      | String             | 실행 ID |
| issueId    | String             | GitHub Issue ID 또는 번호 |
| issueTitle | String             | GitHub Issue 제목 |
| status     | AgentRunStatus     | 현재 pipeline 상태 |
| stepLogs   | List<AgentStepLog> | 실행 step 로그 |
| createdAt  | Instant            | 생성 시각 |
| updatedAt  | Instant            | 마지막 변경 시각 |

### AgentRunStatus

| Status         | Description |
|----------------|-------------|
| PLANNING       | Issue 분석 및 TASK 생성 |
| DESIGNING      | DESIGN 문서 생성 |
| IMPLEMENTING   | 코드 패치 생성 및 적용 |
| TESTING        | 테스트 실행 |
| REVIEWING      | diff와 테스트 결과 기반 리뷰 |
| PR_READY       | PR 제목/본문 생성 완료 |

### AgentStepLog

| Field     | Type          | Description |
|-----------|---------------|-------------|
| stepName  | String        | step 이름 |
| input     | String        | Agent 또는 tool 입력 |
| output    | String        | Agent 또는 tool 출력 |
| toolName  | String        | 사용 tool 이름 |
| status    | AgentStepStatus | SUCCESS 또는 FAILED |
| retry     | boolean       | 재시도 여부 |
| createdAt | Instant       | 로그 생성 시각 |

### AgentTool

| Field        | Type   | Description |
|--------------|--------|-------------|
| name         | String | tool 이름 |
| description  | String | tool 책임 |
| inputSchema  | String | 입력 설명 |
| outputSchema | String | 출력 설명 |

## Package Structure

com.arok2.dev_agent
└── agentrun/
    ├── controller/
    │   ├── AgentRunController.java
    │   └── ToolRegistryController.java
    ├── domain/
    │   ├── AgentRun.java
    │   ├── AgentRunStatus.java
    │   ├── AgentStepLog.java
    │   ├── AgentStepStatus.java
    │   └── AgentTool.java
    ├── dto/
    │   ├── AgentRunCreateRequest.java
    │   ├── AgentRunResponse.java
    │   ├── AgentRunStatusUpdateRequest.java
    │   ├── AgentStepLogCreateRequest.java
    │   ├── AgentStepLogResponse.java
    │   └── ToolDefinitionResponse.java
    └── service/
        ├── AgentRunService.java
        └── ToolRegistryService.java

## Key Design Decisions
- AgentRun은 인메모리 저장소를 사용해 기존 서비스들과 구현 스타일을 맞춘다.
- 상태 전이 순서는 AgentRunStatus enum에 캡슐화한다.
- Tool Registry는 실제 tool 실행기가 아니라 Agent가 호출 가능한 capability catalog로 시작한다.
- step log 추가 시 ToolRegistryService로 tool 존재 여부를 검증한다.
- Spring AI 적용은 다음 단계로 미룬다. 공식 Spring AI 문서의 ChatClient, ToolCallback, ToolCallingAdvisor 구조는 현재 Tool Registry를 ToolCallback bean 목록으로 치환하는 방식으로 연결할 수 있다.

## Implementation Guide

1. AgentRunStatus, AgentStepStatus enum 생성
2. AgentRun, AgentStepLog, AgentTool record 생성
3. AgentRun 요청/응답 DTO 생성
4. Tool Registry 응답 DTO 생성
5. AgentRunNotFoundException, AgentRunAlreadyExistsException, InvalidAgentRunStatusTransitionException, ToolNotFoundException 생성
6. GlobalExceptionHandler에 AgentRun/Tool 예외 매핑 추가
7. ToolRegistryService에 7개 tool 정의 등록
8. AgentRunService에 생성, 조회, 상태 전이, step log 추가 구현
9. AgentRunController와 ToolRegistryController 구현
10. 서비스/컨트롤러 테스트 추가
11. README에 프로젝트 고도화 설명 및 Spring AI 확장 포인트 추가
