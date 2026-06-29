# TASK-002

## Summary
Planner Agent가 생성한 Task를 서버에 저장하고 조회할 수 있는 Task 관리 API를 구현한다.

## Background
Phase 1에서 GitHub Webhook 이벤트 수신 기능이 구현됐다.
다음 단계로, Planner Agent가 Issue를 분석하여 생성한 Task를
서버가 저장하고 관리할 수 있어야 한다.
현재는 Task가 Git 파일로만 존재하며, API를 통한 저장/조회가 불가능하다.
이 기능이 있어야 이후 Agent들이 Task 정보를 서버에서 읽어 다음 단계를 진행할 수 있다.

## Goals
- Planner Agent가 생성한 Task를 서버에 저장한다
- 저장된 Task 목록을 조회한다
- 특정 Task를 ID로 조회한다

## Scope

### In Scope
- Task 저장 엔드포인트
- 전체 Task 목록 조회 엔드포인트
- 특정 Task 단건 조회 엔드포인트
- 메모리 저장 (ConcurrentHashMap, Phase 3에서 DB 교체 예정)

### Out of Scope
- Task 수정, 삭제
- Claude API 직접 호출
- GitHub Actions 트리거
- 데이터베이스 저장

## Requirements

### Functional
- Task 저장 시 taskId, title, content, status를 입력받아 저장하고 생성 결과 반환
- 중복 taskId로 저장 시 에러 반환
- Task 목록 조회 시 저장된 모든 Task 반환
- 존재하지 않는 taskId 조회 시 에러 반환

### Non-Functional
- 동시 요청에 안전하게 처리
- 에러 응답은 의미 있는 코드와 메시지 포함

## Acceptance Criteria
- [ ] Task 저장 호출 시 201 Created와 저장된 Task 반환
- [ ] 중복 taskId 저장 시 409 Conflict 반환
- [ ] Task 목록 조회 시 저장된 전체 Task 반환
- [ ] 존재하지 않는 taskId 조회 시 404 Not Found 반환
- [ ] 모든 비즈니스 로직에 단위 테스트 존재

## Dependencies
TASK-001 (완료)

## Test Requirements
- TaskService 단위 테스트 (저장, 중복, 조회, 404 케이스)
- TaskController 통합 테스트 (MockMvc)

## Status
IN_REVIEW
