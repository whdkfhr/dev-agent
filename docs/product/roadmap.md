# Roadmap

## Phase 1 ✅

GitHub Webhook 수신

목표

- GitHub Issue Event 수신
- 이벤트 저장
- 로그 출력

## Phase 2 ✅

Planner Agent

목표

- Issue 분석
- Task 생성

## Phase 3 ✅

Architect Agent

목표

- 구현 설계 생성

## Phase 4 ✅

Implementer Agent

목표

- 코드 생성
- 코드 수정

## Phase 5 ✅

Reviewer Agent

목표

- 코드 리뷰
- 품질 점검

## Phase 6 ✅

Pull Request 자동 생성

## Phase 7 ✅

AgentRun 실행 추적 및 Tool Registry

목표

- Issue 단위 AgentRun 상태 추적
- Multi-step 실행 로그 저장
- Agent Tool capability 명시적 분리

## Phase 8

Spring AI 기반 Tool Calling 연동

목표

- Tool Registry를 ToolCallback 구조로 확장
- ChatClient 기반 agent execution loop 구성
- ToolCallingAdvisor 기반 tool use 관측성 강화

## Phase 9

테스트 로그 분석 서비스 (Python/FastAPI, ADR-007)

Phase 8과 독립적으로 진행한다.

목표

- 1단계: 가짜 분석기로 로그 분석 API와 정상·입력 오류·분석기 오류·타임아웃 테스트 완성
- 2단계: 실제 AI 모델 연결과 외부 호출의 오류·타임아웃·응답 결과 확인

범위 밖

- Spring 연동
- 배포
