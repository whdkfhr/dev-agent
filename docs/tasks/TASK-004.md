# TASK-004

## Summary
서버 상태를 확인할 수 있는 Health Check 엔드포인트를 추가한다.

## Background
배포 후 서버가 정상적으로 실행 중인지 확인할 수 있는 API가 없어서
운영 모니터링이 불가능하다.

## Goals
- GET /health 엔드포인트 구현
- 서버 상태 응답 반환

## Scope

### In Scope
- GET /health (200 OK, {"status": "UP"})

### Out of Scope
- DB 연결 상태 확인
- 외부 시스템 상태 확인

## Requirements

### Functional
- GET /health 호출 시 200 OK 반환
- 응답 바디에 status 필드 포함

### Non-Functional
- 인증 불필요 (공개 엔드포인트)

## Acceptance Criteria
- [ ] GET /health 200 OK 반환
- [ ] 응답 바디: {"status": "UP"}

## Dependencies
없음

## Test Requirements
- HealthController MockMvc 통합 테스트

## Status
TODO
