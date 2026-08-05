# TASK-007

## Summary
AI Agent 파이프라인의 실행 상태, Tool Use, multi-step 실행 로그를 추적할 수 있는 AgentRun 도메인을 추가한다.

## Background
현재 dev-agent는 Task, Design, Implementation, Review, PullRequest API를 통해 단계별 산출물을 관리하지만,
하나의 GitHub Issue가 어떤 Agent 단계와 Tool 호출을 거쳐 처리되는지 추적하는 실행 단위가 없다.

채용 우대사항에 맞는 AI Agent 사이드 프로젝트로 고도화하려면
Agent 파이프라인 상태 전이, 명시적 Tool Registry, step 단위 입력/출력 로그를 코드와 문서에서 확인할 수 있어야 한다.

## Goals
- AgentRun 도메인 추가
- PLANNING, DESIGNING, IMPLEMENTING, TESTING, REVIEWING, PR_READY 상태 추적
- Agent가 호출 가능한 Tool 목록을 Tool Registry로 분리
- step별 입력, 출력, 사용 tool, 성공/실패, 재시도 여부 저장
- README에 이력서용 프로젝트 설명 추가

## Scope

### In Scope
- AgentRun 생성 및 조회 API
- AgentRun 상태 전이 API
- AgentRun step log 추가 API
- Tool Registry 조회 API
- RepositoryContextTool, TaskDocumentTool, DesignDocumentTool, CodePatchTool, TestRunnerTool, ReviewTool, PullRequestTool 등록
- AgentRun 및 Tool Registry 서비스 테스트
- AgentRun 컨트롤러 테스트
- README 고도화 설명 추가

### Out of Scope
- 실제 LLM API 호출
- 실제 파일 패치 적용
- 실제 `./gradlew test` 프로세스 실행
- 실제 GitHub Pull Request 생성
- DB 영속화
- Spring AI 의존성 추가 및 Provider 설정

## Requirements

### Functional
- AgentRun은 runId, issueId, issueTitle, status, stepLogs, createdAt, updatedAt을 가진다.
- AgentRun 생성 시 초기 상태는 PLANNING이다.
- 상태는 PLANNING → DESIGNING → IMPLEMENTING → TESTING → REVIEWING → PR_READY 순서로만 전이된다.
- 잘못된 상태 전이는 실패해야 한다.
- step log는 stepName, input, output, toolName, status, retry, createdAt을 기록한다.
- Tool Registry는 등록된 tool 이름, 설명, input schema, output schema를 반환한다.
- 알 수 없는 tool 조회는 실패해야 한다.

### Non-Functional
- 기존 인메모리 저장 방식과 레이어드 아키텍처를 유지한다.
- 비즈니스 로직은 서비스 계층에서 검증한다.
- 테스트는 상태 전이, 로그 저장, Tool Registry 조회를 포함한다.
- Spring AI는 추후 전환 가능한 확장 포인트로 문서화한다.

## Acceptance Criteria
- [ ] POST /agent-runs 호출 시 AgentRun이 PLANNING 상태로 생성된다.
- [ ] POST /agent-runs/{runId}/advance 호출 시 다음 상태로 전이된다.
- [ ] 상태 전이를 건너뛰면 400 Bad Request가 반환된다.
- [ ] POST /agent-runs/{runId}/steps 호출 시 step log가 추가된다.
- [ ] GET /agent-runs/{runId} 호출 시 stepLogs가 함께 조회된다.
- [ ] GET /tools 호출 시 7개 tool 정의가 반환된다.
- [ ] GET /tools/{toolName} 호출 시 단일 tool 정의가 반환된다.
- [ ] README에 Multi-step 개발 자동화 워크플로우 설명이 추가된다.

## Dependencies
- 기존 Spring Boot Web 의존성
- 기존 JUnit/Spring Boot Test 의존성

## Test Requirements
- AgentRunService 단위 테스트
- ToolRegistryService 단위 테스트
- AgentRunController MockMvc 테스트
- ToolRegistryController MockMvc 테스트

## Status
IN_REVIEW
