# Dev Agent

AI Agent 산출물을 문서, 상태, 테스트, 리뷰 게이트로 제한하고 검증하는 개발 자동화 학습 프로젝트

Dev Agent는 GitHub Issue 기반 개발 자동화 흐름을 실험하기 위해 만든 Spring Boot 백엔드입니다.
같은 저장소에 테스트 실행 로그를 분석하는 독립 Python/FastAPI 서비스([Log Analyzer](#log-analyzer-pythonfastapi))도 함께 두고 있습니다.

핵심은 AI가 만든 결과를 그대로 신뢰하는 것이 아니라, Planner, Architect, Implementer, Reviewer, Pull Request 단계의 산출물을 문서와 상태로 남기고 다음 단계로 넘기기 전에 테스트와 리뷰 게이트로 검증하는 구조를 설계하는 데 있습니다.

---

## Overview

```
GitHub Issue 생성
      ↓
  Planner       요구사항 분석 → TASK 문서 저장 → POST /tasks
      ↓
 Architect      시스템 설계 → DESIGN 문서 저장 → POST /designs
      ↓
Implementer     구현 산출물 기록 → 소스 코드 + 테스트 → POST /implementations
      ↓
  Reviewer      테스트 / diff / 리뷰 결과 검증 → APPROVED / REJECTED → POST /reviews
      ↓
Pull Request   검증된 변경분을 PR로 추적 → POST /pull-requests
```

각 단계는 GitHub Actions와 저장소의 문서 산출물(`docs/tasks/`, `docs/design/` 등)을 중심으로 연결됩니다. 위 그림의 `POST` 호출은 연결 가능한 Spring 서버가 `SERVER_URL`로 설정된 경우에만 산출물 메타데이터를 서버의 인메모리 저장소에 등록하며, 등록에 실패해도 워크플로는 경고만 남기고 계속됩니다([GitHub Actions 설정](#github-actions-설정)). 현재 운영 중인 서버는 없으므로, API 흐름과 상태 전이는 로컬에서 Spring 서비스를 실행해 확인합니다([Deployment](#deployment)).

AgentRun 도메인은 하나의 Issue 처리 과정을 `PLANNING → DESIGNING → IMPLEMENTING → TESTING → REVIEWING → PR_READY` 상태로 추적합니다.
상태 전이와 각 step의 입력, 출력, 사용 tool, 성공/실패, 재시도 여부는 API 호출자가 직접 기록합니다. 서버가 Agent나 tool을 실행하면서 자동으로 남기는 기록이 아닙니다. 현재 GitHub Actions 워크플로는 AgentRun API를 호출하지 않습니다. AgentRun 데이터는 인메모리에 저장되며 서버를 재시작하면 사라집니다.

---

## Current Scope

- GitHub Issue 기반 개발 자동화 흐름을 실험하기 위한 학습 프로젝트
- Planner, Architect, Implementer, Reviewer, Pull Request 단계의 산출물 저장과 상태 추적 API 구현
- AgentRun 상태 전이와 step log를 API 호출자가 직접 기록해 실행 흐름, tool 입력/출력, 성공/실패 여부 추적 (현재 워크플로는 AgentRun API를 호출하지 않음)
- Tool Registry는 tool 정의 목록만 제공하며 실제 tool을 실행하지 않음
- 현재 저장소는 `ConcurrentHashMap` 기반 인메모리 저장소 사용
- Spring AI `ToolCallback` 연동은 다음 단계 확장 대상으로 분리

---

## Limitations

- 프로덕션 수준의 권한 관리, 영속 DB, 대규모 큐 처리, 멀티 테넌시 기능은 아직 구현하지 않았습니다.
- AI 결과는 항상 신뢰 가능한 것이 아니므로, 이 프로젝트는 테스트, 리뷰, Gate로 AI 산출물을 제한하는 방향의 실험 프로젝트입니다.
- 실제 조직에 적용하려면 권한 모델, 감사 로그, 비용 제어, 실패 복구 정책, 보안 검토 기준이 추가로 필요합니다.
- 현재 인메모리 저장 방식은 학습과 검증 편의성을 위한 선택이며, 서버 재시작 시 데이터가 유지되지 않습니다.

---

## AI 도구 사용 범위

- 이 프로젝트 자체가 AI Agent 워크플로우를 다루지만, 목적은 AI 결과를 무조건 신뢰하는 것이 아니라 제어 가능한 자동화 흐름을 설계하는 것입니다.
- 프롬프트, 상태 전이, Gate 조건, 실패 처리 기준은 직접 정의했습니다.
- AI 산출물은 TASK, DESIGN, diff, 빌드, 테스트, 리뷰 결과로 검증한 뒤 다음 단계로 넘기는 구조를 지향합니다.
- 각 Agent는 단일 책임만 가지며, 이전 단계 산출물이 없거나 형식이 맞지 않으면 다음 단계로 진행하지 않는 Input Lock / Output Lock 규칙을 둡니다.

---

## 문제 해결 사례

### 1. 빈 PR 생성 차단

Implementer가 구현 산출물을 만들었지만 PR 생성 단계에서 `No commits between branches`가 발생하는 문제가 있었습니다. 로그상으로는 큰 오류가 드러나지 않아, 파이프라인은 성공한 것처럼 보이지만 실제 산출물은 없는 상태가 될 수 있었습니다.

원인은 커밋 단계의 `git add src/ docs/tasks/ generated/` pathspec이었습니다. 정상 경로에서는 `generated/` 디렉터리가 없을 수 있는데, 존재하지 않는 경로가 pathspec에 포함되면 `git add`가 전체 스테이징을 실패시킬 수 있었습니다. 여기에 에러를 무시하는 패턴이 겹치면 스테이징 0건, 커밋 스킵, 빈 브랜치 push, PR 생성 실패로 이어졌습니다.

해결은 존재 여부가 불확실한 경로를 나열하지 않고 `git add -A`로 변경분 전체를 스테이징한 뒤, `git diff --staged --quiet`로 실제 변경분을 명시적으로 검증하는 방식으로 바꾼 것입니다. 변경분이 없으면 조용히 넘어가지 않고 실패시켜, 산출물 없는 PR 생성을 차단했습니다.

### 2. Reviewer 반려 후 Self-Healing 흐름

Reviewer가 변경을 반려하면 사람이 다시 원인을 분석하고 수정해야 하는 병목이 있었습니다. 이를 줄이기 위해 리뷰 실패도 파이프라인 상태로 다루도록 라벨 기반 Self-Healing 흐름을 추가했습니다.

```
review_failed
      ↓
Failure Analyzer
      ↓
Fix Agent
      ↓
retry_N
      ↓
Reviewer 재실행
```

Failure Analyzer는 리뷰 결과와 diff를 분석해 `FIX-ANALYSIS-{ID}-retry{N}.md` 문서를 만들고, Fix Agent는 해당 분석 문서를 기준으로 최소 수정만 수행합니다. 재시도는 `retry_1`, `retry_2`, `retry_3` 라벨로 추적하고, 최대 3회를 초과하면 `fix_failed`로 전환해 사람의 개입을 요구합니다. 이 구조로 무한 루프와 API 비용 폭주를 막으면서 실패 복구 과정을 추적 가능하게 만들었습니다.

Failure Analyzer는 파이프라인 안에서 리뷰 반려를 분석합니다. 사람이 넣은 테스트 실행 로그를 분석하는 기능은 별도 서비스인 [Log Analyzer](#log-analyzer-pythonfastapi)입니다.

---

## Log Analyzer (Python/FastAPI)

사람이 붙여 넣은 테스트 실행 로그를 받아 원인 후보, 로그에 실제로 있는 근거, 다음 확인 사항을 돌려주는 독립 서비스입니다.
`services/log-analyzer/`에 있으며 Spring 서비스와 코드, 빌드, 실행 단위를 공유하지 않습니다.
결정과 설계는 [ADR-007](docs/decisions/adr/ADR-007-python-log-analysis-service.md), [TASK-015](docs/tasks/TASK-015.md), [DESIGN-015](docs/design/DESIGN-015.md)에 있습니다.

### Failure Analyzer와의 차이

| 항목 | Failure Analyzer | Log Analyzer |
|------|------------------|--------------|
| 입력 | Reviewer 리포트, TASK, DESIGN, PR diff | 사람이 넣은 테스트 실행 로그 |
| 출력 | `FIX-ANALYSIS-{ID}-retry{N}.md` (Fix Plan) | API 응답 (원인 후보, 근거, 다음 확인 사항) |
| 실행 | GitHub Actions 워크플로 | 독립 HTTP 서비스 |
| 목적 | Fix Agent의 최소 수정 계획 | 사람의 원인 파악 보조 |

두 기능은 서로 호출하지 않습니다.

### 현재 범위: 1단계 (가짜 분석기)

- 분석기는 교체 가능한 경계(`Analyzer`) 뒤에 있고, 지금은 가짜 분석기(`FakeAnalyzer`)만 씁니다. 실제 AI는 호출하지 않습니다.
- 가짜 분석기의 기본 동작은 `FAILED`, `ERROR`, `Error`, `Exception`이 들어간 처음 3줄을 근거로 원인 후보 1개를 만드는 키워드 매칭입니다. 테스트에서는 분석기 오류와 응답 없음(타임아웃)도 재현합니다.
- 서비스는 분석기가 돌려준 근거가 입력 로그의 해당 줄에 실제로 있는지 다시 검증합니다. 검증되지 않은 근거는 응답에서 빼고, 그 수를 `excluded_evidence_count`로 알립니다.
- 목적은 실제 AI를 붙이기 전에 요청·응답 계약, 근거 검증, 오류와 타임아웃 처리를 결정적인 테스트로 먼저 확정하는 것입니다.

**2단계 계획 (예정):** 같은 경계 뒤에 실제 AI 분석기를 연결합니다. 기존 API 계약과 근거 검증 구조는 최대한 유지하는 방향으로 설계할 예정입니다.
외부 호출의 오류, 타임아웃, 응답 결과는 명시적으로 실행하는 별도 테스트로 확인하고, 기본 자동화 테스트에는 포함하지 않습니다.
AI 제공자로 보내는 내용과 제공자의 데이터 처리 정책은 2단계 설계에서 정합니다. 2단계도 Issue, TASK, DESIGN, 구현, 리뷰를 모두 거칩니다.

### 실행 방법

Python 3.12와 [uv](https://docs.astral.sh/uv/)가 필요합니다. 명령은 모두 `services/log-analyzer/`에서 실행합니다.

```bash
cd services/log-analyzer
uv sync                                   # 의존성 설치 (uv.lock 기준)
uv run uvicorn log_analyzer.app:app       # http://127.0.0.1:8000
```

- `uv sync`는 Python 배포본과 PyPI 패키지를 내려받으므로 네트워크가 필요합니다.
- 분석기 타임아웃은 기본 10초이며 환경 변수 `LOG_ANALYZER_TIMEOUT_SECONDS`(0보다 큰 수)로 바꿀 수 있습니다.

### 요청·응답 예시

엔드포인트는 `POST /analyses` 하나입니다. 요청과 결과를 저장하지 않으므로 성공 상태 코드는 200입니다. `log`는 100,000자까지 받습니다.

```bash
curl -s -X POST http://127.0.0.1:8000/analyses \
  -H 'Content-Type: application/json' \
  -d '{"log": "tests/test_order.py::test_total FAILED\nE   AssertionError: assert 90 == 100"}'
```

응답 (가짜 분석기, 보기 좋게 줄바꿈):

```json
{
  "causes": [
    {
      "summary": "로그에 실패 또는 오류를 나타내는 줄이 있다",
      "evidence": [
        { "line": 1, "text": "tests/test_order.py::test_total FAILED" },
        { "line": 2, "text": "E   AssertionError: assert 90 == 100" }
      ]
    }
  ],
  "next_checks": ["근거로 표시된 줄의 테스트와 오류 메시지를 확인한다"],
  "excluded_evidence_count": 0
}
```

- `causes`: 원인 후보 목록. 각 후보의 `evidence`는 입력 로그의 줄 번호(1부터)와 그 줄에 실제로 있는 텍스트입니다. 원인 후보를 찾지 못하면 빈 배열이며 이때도 200입니다.
- `next_checks`: 다음에 확인할 사항
- `excluded_evidence_count`: 검사한 근거 중 검증에 실패해 제외한 수

본문이 허용되는 오류 응답은 모두 같은 JSON 형식입니다.

```json
{ "error": { "code": "INVALID_INPUT", "message": "log가 비어 있습니다." } }
```

| 상태 | `code` | 상황 |
|------|--------|------|
| 413 | `LOG_TOO_LARGE` | `log`가 100,000자를 넘음 (분석기 호출 안 함) |
| 422 | `INVALID_INPUT` | 본문이 JSON 객체가 아님, `log`가 없음·문자열이 아님·비어 있거나 공백만 있음 (분석기 호출 안 함) |
| 502 | `ANALYZER_ERROR` | 분석기가 오류를 알림 |
| 504 | `ANALYZER_TIMEOUT` | 분석기가 제한 시간 안에 끝나지 않음 |
| 500 | `INTERNAL_ERROR` | 예상하지 못한 내부 오류 |
| 404 | `NOT_FOUND` | 정의되지 않은 경로 |
| 405 | `METHOD_NOT_ALLOWED` | `/analyses`에 POST가 아닌 메서드 (`Allow: POST` 헤더 포함) |
| 그 밖의 HTTP 오류 | `HTTP_ERROR` | 위 외에 라우팅 단계에서 난 HTTP 오류 |

예외: HEAD 요청과, 본문이 허용되지 않는 상태 코드(1xx, 204, 304)에는 본문 없이 상태 코드와 헤더만 보냅니다. 예를 들어 `HEAD /analyses`는 본문 없는 405와 `Allow: POST` 헤더를 받습니다.
오류 메시지에는 스택 트레이스, 내부 경로, 입력 로그 원문을 넣지 않습니다. 전체 계약은 [DESIGN-015](docs/design/DESIGN-015.md)의 API Design을 봅니다.

### 테스트와 검증 결과

```bash
uv run python -m compileall -q src tests   # 문법 검사
uv run pytest                              # 자동화 테스트
```

- 의존성 설치(`uv sync`)에는 네트워크가 필요하지만, 설치 후 자동화 테스트는 네트워크, 외부 서비스, 실제 AI, secret 없이 실행됩니다. API 테스트는 FastAPI의 로컬 테스트 클라이언트를 씁니다.
- 타임아웃 테스트는 0.05초 타임아웃을 주입해 실제로 오래 기다리지 않습니다.
- 대상: 근거 검증기, 가짜 분석기, 분석 서비스, 스키마, API (5개 파일)
- 결과 (develop `db81c7b` 기준): **74 passed**. Starlette의 httpx 테스트 클라이언트 관련 deprecation 경고 1건이 있으며 테스트 결과에는 영향이 없습니다.
- 이 서비스를 실행하는 GitHub Actions 워크플로는 없습니다. 검증은 로컬에서 사람이 실행합니다.

### 현재 제한 사항

- 로컬 개발 전용입니다. **배포, 인증, Spring 서비스와의 연동은 범위 밖**입니다.
- 분석 결과는 가짜 분석기의 키워드 매칭이며 실제 원인 분석이 아닙니다.
- 요청과 결과를 저장하지 않습니다.
- 외부에 배포하려면 요청 본문 전체 크기 제한, 인증, 개발용 문서 경로(`/docs`, `/redoc`, `/openapi.json`)의 공개 여부, 접근 로그 정책을 먼저 검토해야 합니다.

---

## Tech Stack

| 구분 | 기술 |
|------|------|
| Language | Java 17 (Spring 서비스) |
| Framework | Spring Boot 3.5 |
| Build | Gradle |
| AI | Claude API |
| CI/CD | GitHub Actions |
| 배포 | 현재 운영 배포 없음 (과거 Railway 배포 이력은 [Deployment](#deployment) 참조) |
| 저장소 | ConcurrentHashMap (인메모리) |
| 로그 분석 서비스 | Python 3.12, FastAPI, Pydantic v2, uv, pytest ([Log Analyzer](#log-analyzer-pythonfastapi)) |

---

## API Endpoints

Spring 서비스의 API입니다. 현재 운영 배포가 없으므로 로컬에서 실행해 호출합니다([Deployment](#deployment)). 로그 분석 서비스의 `POST /analyses`는 [Log Analyzer](#log-analyzer-pythonfastapi) 절에 정리했습니다.

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/webhook` | GitHub Webhook 이벤트 수신 |
| POST | `/tasks` | Task 저장 |
| GET | `/tasks` | Task 목록 조회 |
| GET | `/tasks/{taskId}` | Task 단건 조회 |
| POST | `/designs` | Design 저장 |
| GET | `/designs` | Design 목록 조회 |
| GET | `/designs/{designId}` | Design 단건 조회 |
| GET | `/designs/by-task/{taskId}` | Task 기반 Design 조회 |
| POST | `/implementations` | Implementation 저장 |
| GET | `/implementations` | Implementation 목록 조회 |
| GET | `/implementations/{implId}` | Implementation 단건 조회 |
| GET | `/implementations/by-task/{taskId}` | Task 기반 Implementation 조회 |
| POST | `/reviews` | Review 저장 |
| GET | `/reviews` | Review 목록 조회 |
| GET | `/reviews/{reviewId}` | Review 단건 조회 |
| GET | `/reviews/by-impl/{implId}` | Implementation 기반 Review 조회 |
| POST | `/pull-requests` | PullRequest 저장 |
| GET | `/pull-requests` | PullRequest 목록 조회 |
| GET | `/pull-requests/{prId}` | PullRequest 단건 조회 |
| GET | `/pull-requests/by-task/{taskId}` | Task 기반 PullRequest 조회 |
| POST | `/agent-runs` | AgentRun 생성 |
| GET | `/agent-runs` | AgentRun 목록 조회 |
| GET | `/agent-runs/{runId}` | AgentRun 단건 조회 |
| POST | `/agent-runs/{runId}/advance` | AgentRun 다음 상태 전이 |
| POST | `/agent-runs/{runId}/steps` | AgentRun step log 추가 |
| GET | `/tools` | Agent Tool Registry 조회 |
| GET | `/tools/{toolName}` | Agent Tool 단건 조회 |

AgentRun API 입력 규칙
- `POST /agent-runs`: `runId`와 `issueId`는 필수입니다. 없거나 `null`, 빈 문자열, 공백뿐이면 400 `INVALID_AGENT_RUN_REQUEST`를 반환하고 저장하지 않습니다. `issueTitle`은 선택입니다.
- `POST /agent-runs/{runId}/steps`: `stepName`과 `toolName`은 같은 규칙으로 필수이고, `status`가 없거나 `null`이면 400 `INVALID_AGENT_RUN_REQUEST`입니다. 입력 검증이 run 존재 확인보다 먼저이므로, 존재하지 않는 run에 필수 값이 빠진 요청도 400입니다.
- 같은 `runId`에 대한 생성, 상태 전이, step log 추가는 단일 인스턴스 안에서 원자적으로 처리됩니다.

---

## Agent System

### Agents

| Agent | 역할 | 트리거 | 산출물 |
|-------|------|--------|--------|
| **Planner** | GitHub Issue 분석 → Task 정의 | Issue 생성 | `docs/tasks/TASK-{ID}.md` |
| **Architect** | Task 기반 설계 → API/데이터 모델 정의 | `design` 라벨 | `docs/design/DESIGN-{ID}.md` |
| **Implementer** | 설계 기반 변경분 작성 | `implement` 라벨 | 소스 코드 + 테스트 |
| **Reviewer** | 코드 품질 / 아키텍처 준수 / 테스트 결과 검토 | PR 생성 | 리뷰 코멘트 |
| **Failure Analyzer** | 반려 사유와 diff 분석 | `review_failed` 라벨 | `docs/fixes/FIX-ANALYSIS-{ID}-retry{N}.md` |
| **Fix Agent** | 분석 문서 기준 최소 수정 | `fixing` 라벨 | 패치 커밋 |

### AgentRun State Machine

```
PLANNING → DESIGNING → IMPLEMENTING → TESTING → REVIEWING → PR_READY
```

AgentRun은 Issue 하나를 처리하는 전체 실행 단위입니다.
API 호출자가 상태 전이와 각 단계의 step log(단계 이름, 사용한 tool 이름, 입력과 출력, 성공/실패, 재시도 여부)를 직접 기록합니다.
서버는 이 기록을 보관하고 상태 순서와 입력 값을 검증할 뿐, Agent나 tool을 실행하지 않습니다.
현재 GitHub Actions 워크플로는 AgentRun API를 호출하지 않으며, 데이터는 인메모리에 저장되어 서버를 재시작하면 사라집니다.

### Tool Registry

Tool Registry는 tool의 이름, 설명, 입력·출력 설명을 제공하는 정의 목록입니다(`GET /tools`).
서버는 등록된 tool을 실행하지 않습니다. 아래 표는 각 tool이 맡을 역할을 적은 정의이며, step log를 추가할 때 `toolName`이 이 목록에 있는지만 확인합니다.

| Tool | 정의된 역할 |
|------|------|
| RepositoryContextTool | README, build.gradle, 패키지 구조 조회 |
| TaskDocumentTool | TASK 문서 생성 및 검증 |
| DesignDocumentTool | DESIGN 문서 생성 및 검증 |
| CodePatchTool | 생성된 파일 블록 파싱 및 패치 계획 생성 |
| TestRunnerTool | `./gradlew test` 실행 결과 수집 |
| ReviewTool | diff와 테스트 결과 기반 리뷰 생성 |
| PullRequestTool | PR 제목/본문 생성 |

현재는 커스텀 Tool Registry로 tool 정의를 명시적으로 분리했습니다.
실제 tool 실행과 LLM 연동은 구현되어 있지 않습니다. Spring AI 적용 시에는 각 tool을 `ToolCallback` bean으로 노출하고, `ChatClient`와 `ToolCallingAdvisor`를 통해 LLM의 tool calling loop와 연결할 수 있도록 확장할 계획입니다.

### Gate System

```
[GATE 0] Context 확인     vision.md, roadmap.md, architecture.md 존재 여부
[GATE 1] TASK 검증        TASK 문서 필수 섹션 / 포맷 확인
[GATE 2] DESIGN 검증      API 명세 / 데이터 모델 / 구현 가이드 확인
[GATE 3] Code 검증        아키텍처 준수 / 테스트 존재 / 레이어 분리 확인
[GATE 4] Review 승인      APPROVED 판정 확인
```

Gate는 AI 산출물을 다음 단계로 넘기기 전에 최소 조건을 확인하는 장치입니다.
목표는 결과를 빠르게 만드는 것이 아니라, 실패가 어디서 발생했는지 추적하고 잘못된 산출물이 뒤 단계로 전파되지 않게 막는 것입니다.

### Task State Machine

```
TODO → IN_PROGRESS → IN_REVIEW → DONE
                               ↘ BLOCKED
```

TASK 상태는 파이프라인의 단일 진실 원천입니다.
단계 스킵이나 역방향 전이를 허용하지 않고, 전제 조건이 없거나 충돌이 있으면 `BLOCKED`로 전환합니다.

---

## 검증 범위

Spring 서비스 기준입니다.

- 서비스 계층 테스트로 산출물 저장, 중복 방지, 조회 실패, 상태 전이를 검증합니다.
- 컨트롤러 테스트로 API 요청/응답 계약과 예외 처리를 확인합니다.
- E2E 테스트로 Task, Design, Implementation, Review, PullRequest 흐름을 한 번에 검증합니다.
- 현재 테스트 파일은 19개이며, AgentRun 상태 전이, 입력 검증, 동시성 회귀 테스트와 Tool Registry 조회 테스트를 포함합니다.

로그 분석 서비스는 pytest로 별도 검증합니다(74 passed). [테스트와 검증 결과](#테스트와-검증-결과)를 봅니다.

---

## Project Structure

```
dev-agent/
├── AGENTS.md                          # 프로젝트 헌법과 Agent 실행 규칙 (원본)
├── CLAUDE.md                          # AGENTS.md를 불러오는 참조 (@AGENTS.md)
│
├── .github/workflows/
│   ├── planner.yml                    # Issue → TASK
│   ├── architect.yml                  # TASK → DESIGN
│   ├── implementer.yml                # DESIGN → 변경분 + 테스트
│   ├── reviewer.yml                   # PR diff → 리뷰
│   ├── failure-analyzer.yml           # review_failed → 실패 분석
│   ├── fix.yml                        # FIX-ANALYSIS → 패치
│   └── e2e-mock.yml                   # API key 없이 파이프라인 검증
│
├── .claude/
│   ├── agents/                        # 에이전트 프롬프트 정의
│   │   ├── planner.md
│   │   ├── architect.md
│   │   ├── implementer.md
│   │   └── reviewer.md
│   ├── templates/                     # 산출물 포맷 템플릿
│   └── validators/                    # Gate 검증 규칙
│
├── docs/
│   ├── product/
│   │   ├── vision.md
│   │   └── roadmap.md
│   ├── decisions/adr/                 # Architecture Decision Records
│   ├── architecture/
│   ├── tasks/                         # TASK-{ID}.md
│   └── design/                        # DESIGN-{ID}.md
│
├── services/
│   └── log-analyzer/                  # 테스트 로그 분석 서비스 (Python/FastAPI, ADR-007)
│       ├── src/log_analyzer/          # API, 분석 서비스, 근거 검증, 가짜 분석기
│       └── tests/                     # pytest 테스트 + fixture 로그
│
└── src/main/java/com/arok2/dev_agent/
    ├── webhook/                       # GitHub Webhook 수신
    ├── task/                          # Task 관리 API
    ├── design/                        # Design 관리 API
    ├── implementation/                # Implementation 관리 API
    ├── review/                        # Review 관리 API
    ├── pullrequest/                   # PullRequest 관리 API
    ├── agentrun/                      # AgentRun 상태 추적 + Tool Registry
    └── common/exception/              # 전역 예외 처리
```

---

## Deployment

현재 운영 중인 배포는 없습니다.

Phase 6에서 Spring 서비스를 Railway에 배포해 API 흐름을 확인한 적이 있으나, 비용이 발생하기 전에 Railway 프로젝트를 종료했습니다. 당시 사용한 Railway 주소는 더 이상 접근할 수 없습니다. 로그 분석 서비스는 처음부터 배포 대상이 아니었습니다.

### 로컬 실행과 동작 확인

```bash
./gradlew bootRun                    # http://localhost:8080
curl http://localhost:8080/tasks     # 다른 터미널에서 실행
```

데이터는 인메모리에만 있으므로 서버를 재시작하면 사라집니다.

---

## GitHub Actions 설정

워크플로우 실행에 쓰는 GitHub Secrets:

| Secret | 필수 여부 | 설명 |
|--------|-----------|------|
| `ANTHROPIC_API_KEY` | 필수 | Claude API key (AI 에이전트 실행용) |
| `SERVER_URL` | 선택 | 워크플로가 실제로 연결할 수 있는 Spring 서버가 있을 때만 그 서버의 주소를 설정합니다. |

`SERVER_URL`은 산출물 메타데이터를 Spring 서버에 등록하는 호출(`POST /tasks`, `/designs`, `/implementations`, `/reviews`, `/pull-requests`)에만 쓰입니다. 등록 호출이 실패하면 워크플로는 `WARN: Failed to register ... (non-blocking)` 경고를 남긴 뒤 다음 단계로 계속 진행합니다. 산출물의 기준은 저장소의 문서, 브랜치, Pull Request입니다.

- `SERVER_URL`을 설정하지 않으면 요청을 보낼 서버가 없으므로 등록은 이루어지지 않습니다.
- 연결 오류, 타임아웃, 응답 유실처럼 서버의 응답을 확인하지 못한 경우에는 서버가 요청을 처리했는지 워크플로만으로 단정할 수 없습니다. 경고가 남았더라도 서버에 등록되었을 수 있습니다.
- 한 번의 실행에서 여러 등록 호출이 이어지면(예: Implementer의 `/implementations`와 `/pull-requests`) 일부만 성공할 수 있습니다.

따라서 서버의 등록 상태가 저장소의 산출물과 일치한다고 가정하지 말고, 필요하면 서버의 조회 API로 직접 확인합니다.

---

## 다른 프로젝트에 적용하기

dev-agent의 워크플로와 에이전트 프롬프트는 다른 프로젝트의 GitHub Issue → PR 검증 흐름 실험에 활용할 수 있습니다. 산출물을 서버에서 조회하려면 Spring 서비스를 직접 실행하고, 워크플로가 연결할 수 있는 주소를 `SERVER_URL`로 설정해야 합니다.

### 적용 방법 (워크플로우 복사)

1. 타겟 프로젝트에 워크플로우 파일 복사
```
.github/workflows/planner.yml
.github/workflows/architect.yml
.github/workflows/implementer.yml
.github/workflows/reviewer.yml
.github/workflows/failure-analyzer.yml
.github/workflows/fix.yml
```

2. 타겟 프로젝트 GitHub Secrets 추가
```
ANTHROPIC_API_KEY=<Claude API key>
SERVER_URL=<연결 가능한 Spring 서버 주소>   # 선택. 없으면 서버 등록 호출은 경고 후 건너뜀
```

3. GitHub Issue 생성 → 라벨 기반 파이프라인 실행과 Gate 결과 확인

---

## Roadmap

| Phase | 내용 | 상태 |
|-------|------|------|
| Phase 1 | GitHub Webhook 수신 | ✅ 완료 |
| Phase 2 | Planner Agent — Task 관리 API | ✅ 완료 |
| Phase 3 | Architect Agent — Design 관리 API | ✅ 완료 |
| Phase 4 | Implementer Agent — Implementation 관리 API | ✅ 완료 |
| Phase 5 | Reviewer Agent — Review 관리 API | ✅ 완료 |
| Phase 6 | Pull Request 관리 API + Railway 배포 (이후 비용 발생 전 종료) | ✅ 완료 |
| Phase 7 | AgentRun 상태 추적 + Tool Registry + step log | ✅ 완료 |
| Phase 8 | Spring AI ToolCallback 기반 실제 Tool Calling 연동 | 예정 |
| Phase 9 | 테스트 로그 분석 서비스 1단계 — 가짜 분석기로 API와 테스트 완성 | ✅ 완료 |
| Phase 9 | 테스트 로그 분석 서비스 2단계 — 실제 AI 모델 연결 | 예정 |

---

## Core Principles

**Documentation First** — 문서가 진실의 원천. 문서 없이 코드를 작성하지 않습니다.

**Input Lock** — 각 에이전트는 이전 단계 산출물이 없으면 실행되지 않습니다.

**Output Lock** — 각 에이전트는 템플릿 기반의 구조화된 산출물만 생성합니다.

**Single Responsibility** — 각 에이전트는 정확히 하나의 책임만 가집니다.

---

## Git Workflow

```
main        — 릴리즈 브랜치
develop     — 통합 브랜치
feature/*   — 기능 개발 브랜치
```

Conventional Commits 규칙을 따릅니다. 커밋 메시지는 한국어로 작성합니다.

```
feat:     새로운 기능
fix:      버그 수정
refactor: 리팩토링
docs:     문서 수정
test:     테스트 추가/수정
chore:    빌드, 설정 변경
```
