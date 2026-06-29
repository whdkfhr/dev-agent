# TASK-005

## Summary
Reviewer Agent가 생성한 코드 리뷰 결과를 서버에 저장하고 조회할 수 있는 Review 관리 API를 구현한다.

## Background
Phase 4에서 Implementation 관리 API가 구현됐다.
다음 단계로, Reviewer Agent가 Implementation을 기반으로 코드를 리뷰한 결과를
서버가 저장하고 관리할 수 있어야 한다.
현재는 리뷰 결과가 존재하지 않으며, API를 통한 저장/조회가 불가능하다.
이 기능이 있어야 이후 Pipeline이 리뷰 상태를 확인하고
APPROVED 시 PR 생성, REJECTED 시 재구현 요청 등 후속 처리를 할 수 있다.

## Goals
- Reviewer Agent가 생성한 코드 리뷰 결과를 서버에 저장한다
- 저장된 Review 목록을 조회한다
- 특정 Review를 ID로 조회한다
- Implementation ID로 연관된 Review를 조회한다

## Scope

### In Scope
- Review 저장 엔드포인트
- 전체 Review 목록 조회 엔드포인트
- 특정 Review 단건 조회 엔드포인트
- Implementation ID 기반 Review 조회 엔드포인트
- 메모리 저장 (ConcurrentHashMap)

### Out of Scope
- Review 수정, 삭제
- Claude API 직접 호출을 통한 자동 리뷰 생성
- GitHub PR 코멘트 자동 등록
- 데이터베이스 저장

## Requirements

### Functional
- Review 저장 시 reviewId, implId, taskId, status, comments를 입력받아 저장하고 생성 결과 반환
- 중복 reviewId로 저장 시 에러 반환
- Review 목록 조회 시 저장된 모든 결과 반환
- 존재하지 않는 reviewId 조회 시 에러 반환
- implId로 조회 시 해당 Implementation에 연관된 Review 반환, 없으면 에러 반환

### Non-Functional
- 동시 요청에 안전하게 처리
- 에러 응답은 의미 있는 코드와 메시지 포함

## Acceptance Criteria
- [ ] Review 저장 호출 시 201 Created와 저장된 결과 반환
- [ ] 중복 reviewId 저장 시 409 Conflict 반환
- [ ] Review 목록 조회 시 저장된 전체 결과 반환
- [ ] 존재하지 않는 reviewId 조회 시 404 Not Found 반환
- [ ] implId로 조회 시 연관 Review 반환, 없으면 404 반환
- [ ] 모든 비즈니스 로직에 단위 테스트 존재

## Dependencies
TASK-004 (완료)

## Test Requirements
- ReviewService 단위 테스트 (저장, 중복, 조회, 404 케이스)
- ReviewController 통합 테스트 (MockMvc)

## Status
IN_REVIEW
