# TASK-006

## Summary
AI Agent 파이프라인의 최종 산출물인 GitHub Pull Request 정보를 서버에 저장하고 조회할 수 있는 PullRequest 관리 API를 구현한다.

## Background
Phase 5에서 Review 관리 API가 구현됐다.
파이프라인의 마지막 단계로, Review가 APPROVED 되면 GitHub Actions가 PR을 생성하고
그 결과를 서버에 기록해야 한다.
현재는 PR이 생성되더라도 서버에 상태가 남지 않아
파이프라인 전체 흐름을 API로 추적하는 것이 불가능하다.
이 기능이 구현되면 GitHub Issue → Task → Design → Implementation → Review → PR 까지
전 단계의 산출물을 서버에서 일관되게 조회할 수 있다.

## Goals
- GitHub Actions가 PR 생성 후 결과를 서버에 저장한다
- 저장된 PullRequest 목록을 조회한다
- 특정 PullRequest를 ID로 조회한다
- Task ID로 연관된 PullRequest를 조회한다

## Scope

### In Scope
- PullRequest 저장 엔드포인트
- 전체 PullRequest 목록 조회 엔드포인트
- 특정 PullRequest 단건 조회 엔드포인트
- Task ID 기반 PullRequest 조회 엔드포인트
- 메모리 저장 (ConcurrentHashMap)

### Out of Scope
- GitHub API 직접 호출을 통한 PR 생성 (Spring Boot → GitHub)
- PullRequest 수정, 삭제
- PR 머지 자동화
- 데이터베이스 저장

## Requirements

### Functional
- PullRequest 저장 시 prId, taskId, implId, reviewId, title, body, prUrl, status를 입력받아 저장하고 생성 결과 반환
- 중복 prId로 저장 시 에러 반환
- PullRequest 목록 조회 시 저장된 모든 결과 반환
- 존재하지 않는 prId 조회 시 에러 반환
- taskId로 조회 시 해당 Task에 연관된 PullRequest 반환, 없으면 에러 반환

### Non-Functional
- 동시 요청에 안전하게 처리
- 에러 응답은 의미 있는 코드와 메시지 포함

## Acceptance Criteria
- [ ] PullRequest 저장 호출 시 201 Created와 저장된 결과 반환
- [ ] 중복 prId 저장 시 409 Conflict 반환
- [ ] PullRequest 목록 조회 시 저장된 전체 결과 반환
- [ ] 존재하지 않는 prId 조회 시 404 Not Found 반환
- [ ] taskId로 조회 시 연관 PullRequest 반환, 없으면 404 반환
- [ ] 모든 비즈니스 로직에 단위 테스트 존재

## Dependencies
TASK-005 (완료)

## Test Requirements
- PullRequestService 단위 테스트 (저장, 중복, 조회, 404 케이스)
- PullRequestController 통합 테스트 (MockMvc)

## Status
IN_REVIEW
