# TASK-003

## Summary
Architect Agent가 생성한 Design 문서를 서버에 저장하고 조회할 수 있는 Design 관리 API를 구현한다.

## Background
Phase 2에서 Task 관리 API가 구현됐다.
다음 단계로, Architect Agent가 Task를 분석하여 생성한 Design 문서를
서버가 저장하고 관리할 수 있어야 한다.
현재는 Design이 Git 파일로만 존재하며, API를 통한 저장/조회가 불가능하다.
이 기능이 있어야 이후 Implementer Agent가 Design 정보를 서버에서 읽어
코드 생성을 진행할 수 있다.

## Goals
- Architect Agent가 생성한 Design 문서를 서버에 저장한다
- 저장된 Design 목록을 조회한다
- 특정 Design을 ID로 조회한다
- Task ID로 연관된 Design을 조회한다

## Scope

### In Scope
- Design 저장 엔드포인트
- 전체 Design 목록 조회 엔드포인트
- 특정 Design 단건 조회 엔드포인트
- Task ID 기반 Design 조회 엔드포인트
- 메모리 저장 (ConcurrentHashMap, Phase 3에서 DB 교체 예정)

### Out of Scope
- Design 수정, 삭제
- Claude API 직접 호출
- GitHub Actions 트리거
- 데이터베이스 저장

## Requirements

### Functional
- Design 저장 시 designId, taskId, content를 입력받아 저장하고 생성 결과 반환
- 중복 designId로 저장 시 에러 반환
- Design 목록 조회 시 저장된 모든 Design 반환
- 존재하지 않는 designId 조회 시 에러 반환
- taskId로 조회 시 해당 Task에 연관된 Design 반환, 없으면 에러 반환

### Non-Functional
- 동시 요청에 안전하게 처리
- 에러 응답은 의미 있는 코드와 메시지 포함

## Acceptance Criteria
- [ ] Design 저장 호출 시 201 Created와 저장된 Design 반환
- [ ] 중복 designId 저장 시 409 Conflict 반환
- [ ] Design 목록 조회 시 저장된 전체 Design 반환
- [ ] 존재하지 않는 designId 조회 시 404 Not Found 반환
- [ ] taskId로 조회 시 연관 Design 반환, 없으면 404 반환
- [ ] 모든 비즈니스 로직에 단위 테스트 존재

## Dependencies
TASK-002 (완료)

## Test Requirements
- DesignService 단위 테스트 (저장, 중복, 조회, 404 케이스)
- DesignController 통합 테스트 (MockMvc)

## Status
IN_REVIEW
