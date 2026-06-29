# TASK-001

## Summary
GitHub에서 발생한 Issue 이벤트를 Webhook으로 수신하고, 저장 후 로그를 출력한다.

## Background
GitHub Actions 파이프라인의 시작점은 GitHub Issue Event다.
서버가 Webhook을 수신할 수 있어야 이후 Planner Agent 호출 등 자동화가 가능하다.

## Goals
- GitHub Webhook POST 요청을 수신하는 엔드포인트를 구현한다
- 수신한 이벤트를 메모리에 저장한다
- 이벤트 수신 로그를 출력한다

## Scope

### In Scope
- `POST /webhook/github` 엔드포인트 구현
- `X-GitHub-Event` 헤더로 이벤트 타입 식별
- 이벤트 payload를 메모리(ConcurrentHashMap)에 저장
- 이벤트 수신 로그 출력 (eventId, eventType, action)

### Out of Scope
- GitHub Webhook Signature 검증 (X-Hub-Signature-256)
- 데이터베이스 저장 (Phase 3)
- Planner Agent 호출
- 이벤트 타입별 분기 처리

## Requirements

### Functional
- `POST /webhook/github` 호출 시 200 OK 반환
- `X-GitHub-Event` 헤더가 없으면 400 Bad Request 반환
- payload에서 `action` 필드 추출
- 이벤트마다 고유한 UUID 부여

### Non-Functional
- 로그는 eventId, eventType, action을 포함해야 한다
- 잘못된 요청은 의미 있는 에러 메시지를 반환해야 한다

## Acceptance Criteria
- [ ] POST /webhook/github 호출 시 200 OK와 eventId 반환
- [ ] X-GitHub-Event 헤더 없으면 400 반환
- [ ] 수신 시 로그에 eventId, eventType, action 출력
- [ ] 이벤트가 메모리에 저장되고 조회 가능

## Dependencies
없음

## Test Requirements
- WebhookController 통합 테스트 (MockMvc)
- WebhookService 단위 테스트

## Status
IN_REVIEW
