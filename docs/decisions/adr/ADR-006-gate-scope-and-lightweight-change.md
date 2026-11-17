# ADR-006: 게이트 적용 범위와 경량 변경 절차

## 1. Status

Accepted

* 날짜: 2026-11-17

Accepted로 바뀌는 시점과 조건은 3.6에 적는다.

---

## 2. Context

- 헌법 §12는 "모든 것에 우선"하고 §12.6은 "예외 없음"이며, 적용 범위에 제한이 없다. 문서와 CI 변경도 문언상 게이트 대상이다.
- 게이트의 통과 기준은 기능 개발을 전제로 한다. `design-validator.md`는 API 엔드포인트와 Entity를 필수로 요구하고, `code-validator.md`는 Spring 계층 규칙만 본다. 문서·CI 변경은 이 게이트를 정직하게 통과할 수 없다.
- 실제로 PR #9, #10(README)과 TASK-007은 Issue 없이 진행되었다. 규칙에 적힌 예외는 아니었다.
- 규칙을 고치는 작업도 같은 게이트에 막혀 있어, 현재 규칙만으로는 시작할 수 없다.
- 헌법이 `CLAUDE.md`(추적됨)와 `AGENTS.md`(로컬 전용, 존재하지 않는 `.Codex/` 경로 참조)로 나뉘어 있다. 도구 이름과 경로를 제외한 본문은 같다.
- TASK 형식의 기준이 세 개이고 서로 다르다. `.claude/templates/TASK.md`(§12.3 OUTPUT LOCK), `.claude/validators/task-validator.md`(GATE 1), `.claude/agents/planner.md`의 STRICT FORMAT이다. 기존 TASK-001~007은 모두 `planner.md`의 형식을 따른다.
- PR 생성과 DONE의 순서가 규칙 안에서 충돌한다. §12.4는 DONE의 조건을 "Review APPROVED, PR created"로 적는데, §12.5의 GATE 4는 PR 전에 DONE을 요구하고, `review-validator.md`는 "APPROVED 후 DONE, 그다음 PR 생성"으로 적는다.
- `reviewer.md`는 입력을 "PR diff"로 적으면서 "APPROVED 없이는 PR이 생성될 수 없다"고도 적는다.

---

## 3. Decision

### 3.1 게이트의 적용 범위를 줄인다

이것은 규칙의 변경이다. 지금까지 문언상 모든 변경에 적용되던 §12가, 앞으로는 3.2에 정한 변경에만 적용된다. 그 밖의 변경은 TASK, DESIGN, 상태 머신, Reviewer 게이트 없이 사람의 PR 검토로 대체된다.

"모든 것에 우선"과 §12.6의 "예외 없음"은 범위 안에서만 유효하다는 뜻으로 바뀐다. 범위 안의 변경에 대한 잠금, 금지 행위, GATE 0~3의 정의는 문언을 바꾸지 않는다. GATE 4와 상태 머신은 3.9에 따라 바뀐다.

### 3.2 전체 게이트가 적용되는 변경

아래 중 하나라도 해당하면 Issue, TASK, DESIGN, 구현, 리뷰를 모두 거친다.

- `src/` 아래의 모든 변경과 테스트 코드
- API 계약과 데이터 모델
- 새 서비스나 새 실행 단위의 추가
- 빌드, 런타임, 배포 구성
- 배포되는 제품의 일부로 실행되거나 제품 로직을 담은 스크립트
- 위 변경과 그 밖의 변경이 한 변경 묶음에 섞인 경우

### 3.3 절차의 관계와 판단 기준

절차는 두 갈래다. 제품을 바꾸면 전체 게이트, 아니면 경량 절차다. 둘 중 하나만 적용된다.

ADR은 세 번째 절차가 아니라 추가 요건이다. 어느 갈래에든 붙을 수 있다.

| 제품 변경 | 규칙 변경 | 적용 |
| --------- | --------- | ---- |
| 아니요 | 아니요 | 경량 절차 |
| 아니요 | 예 | 경량 절차 + ADR (같은 PR에 ADR 포함, 소유자 승인) |
| 예 | 아니요 | 전체 게이트 |
| 예 | 예 | ADR을 먼저 경량 절차로 승인받고, 그다음 전체 게이트 |

판단 질문은 세 개다.

1. 제품의 실행물이 바뀌는가. 3.2의 변경이 해당한다. "예"면 전체 게이트다.
2. 헌법, ADR, `docs/architecture/architecture.md`에 적힌 규칙이나 동작이 바뀌는가. 게이트 요구 사항, 승인 주체, 단계와 라벨 전이, 에이전트의 의무와 산출물 형식, validator의 필수 항목이 해당하며, 강화하는 방향도 포함한다. "예"면 ADR이 필요하다.
3. 자동화의 능력이 늘어나는가. 권한, 트리거, 쓰기 대상, secret, 외부 호출이 해당한다. "예"면 ADR이 필요하다.

충돌 해결 규칙은 다음과 같다.

- 질문이 아래 표보다 우선한다. 표는 예시다.
- 능력을 줄이거나 보호 장치를 더하는 변경은, 문서에 적힌 규칙을 바꾸지 않고 문서대로 맞추는 것일 때만 경량 절차다. 문서에 적힌 동작이 달라지면 줄이는 방향이어도 ADR이 필요하다.
- 판단이 서지 않으면 무거운 쪽을 적용하고, 분류는 소유자가 확정한다.

| 변경 종류 | 경량 절차로 충분 | ADR 추가 필요 | 전체 게이트 |
| --------- | ---------------- | ------------- | ----------- |
| 문서(README, 제품, 아키텍처) | 사실 정정, 현황 반영 | 기술 스택이나 아키텍처 양식 변경 | 해당 없음 |
| 실행 스크립트 | 호출되지 않는 스크립트의 삭제, 주석 | 워크플로가 호출하게 되거나 저장소 쓰기·외부 호출을 추가 | 제품 로직을 담거나 배포 환경에서 실행 |
| 워크플로 권한과 동작 | 문서에 적힌 동작에 맞추는 권한 축소와 보호 검사 추가, 산출물이 같은 버그 수정 | 권한 확대, 트리거나 실행 조건 변경, 쓰기 대상 추가, 새 secret이나 외부 호출, 단계 순서나 라벨 전이 변경 | `src/` 변경을 동반 |
| 에이전트 프롬프트 | 의무가 달라지지 않는 오탈자와 표현 정리 | MUST/MUST NOT, 입력과 출력 형식, 실패 조건, 역할 경계의 변경 | 해당 없음 |
| validator | 검사 항목이 그대로인 표현 정리 | 필수 항목이나 FAIL 조건의 추가, 삭제, 완화 | 해당 없음 |
| 헌법 | 없음 | 모든 원칙 변경 | 해당 없음 |

### 3.4 경량 절차

1. `plan`, `design`, `implement` 라벨이 없는 Issue를 만든다. 목적, 변경 목록, 3.3의 세 질문에 대한 답, 검증 방법을 적는다.
2. develop에서 브랜치를 만들고, PR 하나에 목적 하나만 담는다.
3. PR 본문에 `Closes #N`, 변경 목록, 검증 결과를 적는다.
4. 소유자가 직접 검토하고 머지한다. 에이전트와 워크플로는 머지하지 않는다.
5. TASK와 DESIGN은 만들지 않으며, 상태 머신(§12.4)도 적용하지 않는다.

### 3.5 부트스트랩

이 ADR을 담은 PR은 허용 규칙이 없는 상태에서 나가므로 1회 예외로 기록한다.

- 소유자의 승인 문장이 있기 전에는 Issue, 파일, PR을 만들지 않는다.
- 부트스트랩 승인은 PR 작성 허가이며, 변경 내용의 최종 승인이나 머지 승인이 아니다.
- 승인 범위 밖의 변경은 이 PR에 넣지 않는다.
- 이 예외는 다시 쓸 수 없다. 이후의 변경은 모두 3.3과 3.4를 따른다.

승인 기록은 8. Notes에 적는다.

### 3.6 Proposed에서 Accepted로 바뀌는 시점

1. PR은 Status가 Proposed인 채로 올라간다. 이때의 마지막 커밋을 X라고 한다.
2. 소유자가 산출물을 검토하고 PR에 내용 승인을 남긴다. 승인에 커밋 X를 적는다. 이것은 머지 승인이 아니다.
3. 커밋 Y를 추가한다. Y는 이 파일의 Status, 날짜, Notes의 승인 기록만 바꾼다.
4. 소유자가 확인한다. PR의 마지막 커밋이 Y인지, X와 Y 사이의 변경이 그 세 곳뿐인지, 그 밖의 커밋이 없는지를 본다.
5. 소유자가 직접 머지한다. 이 머지가 최종 승인이고, 규칙은 develop에 머지된 시점부터 효력을 가진다.

X 이후에 Y가 아닌 변경이 생기면 내용 승인은 무효가 되고 2단계부터 다시 한다.

### 3.7 헌법 단일화

`AGENTS.md`를 헌법의 원본으로 추적하고, `CLAUDE.md`는 `@AGENTS.md` 한 줄로 둔다.

| 항목 | 통합 전 | 통합 후 |
| ---- | ------- | ------- |
| §12 적용 범위 | 제한 없음 | 3.2의 변경으로 한정 (변경) |
| §12.6 "예외 없음" | 모든 변경 | 범위 안의 변경 (의미 변경) |
| GATE 0~3, 잠금, 금지 행위 | 두 파일에 같은 문언 | 같은 문언, 한 곳 |
| GATE 4, 상태 머신의 DONE, Reviewer 입력 | PR 전 DONE | 3.9의 순서 (변경) |
| 템플릿과 validator 경로 | `.claude/`와 존재하지 않는 `.Codex/` | `.claude/` |
| §4 문서 계층 1순위 | 각자 자기 파일명 | `AGENTS.md` (변경) |
| §5 주어 | Claude / Codex | Agents (변경) |
| 버전 표기 | 1.0 | 1.1 (변경) |
| 추적 여부 | `AGENTS.md`는 로컬에서 제외 | 둘 다 추적 (변경) |

### 3.8 TASK와 DESIGN의 형식 기준

형식의 기준은 실제로 쓰이는 에이전트 프롬프트(`planner.md`, `architect.md`)의 STRICT FORMAT으로 한다. 기존 TASK-001~007과 DESIGN-001~007이 이 형식이다.

- `.claude/templates/TASK.md`를 `planner.md`의 형식으로 바꾼다.
- `.claude/templates/DESIGN.md`의 섹션 번호와 `(for Implementer)` 접미어를 없앤다.
  - 같은 파일의 예시를 `architect.md` STRICT FORMAT과 `design-validator.md`의 필수 내용에 맞춘다. 성공 응답 상태 코드(`Response 201:` 등), 상태 코드별 Error 사유, Entity 관계 표기, 패키지 루트 `com.arok2.dev_agent`, 사유가 붙은 Key Design Decisions, `Selected` 열이 있는 Trade-offs 표다.
- `.claude/validators/task-validator.md`의 필수 섹션을 같은 형식으로 바꾼다.
- `.claude/validators/code-validator.md`의 `## Tasks` 참조를 `## Requirements`의 `### Functional`로 바꾼다.

이것은 GATE 1 기준의 변경이다.

| 검사 | 변경 전 | 변경 후 |
| ---- | ------- | ------- |
| 목표 문장 | `## Goal`이 한 문장, 2문장 초과 시 FAIL | `## Summary`가 정확히 한 문장, 두 문장 이상이면 FAIL (강화) |
| 작업 항목 | `## Tasks`에 2개 이상 | 삭제. `## Goals` 1개 이상과 `### Functional` 1개 이상으로 대체 |
| 배경 | 없음 | `## Background` 필수 (추가) |
| 비기능 요구사항 | 없음 | `### Non-Functional` 필수, "None" 허용 (추가) |
| 범위 | `## Scope`, `## Out of Scope` | `### In Scope`, `### Out of Scope` |
| 구현 상세 금지 | `## Tasks` 대상 | `## Goals`, `## Requirements` 대상 |
| Acceptance Criteria, Dependencies, Test Requirements, Status | 그대로 | 그대로 |

### 3.9 PR 생성과 DONE의 순서

- 기능 작업의 Reviewer는 PR 생성 전에 기준 브랜치와 feature 브랜치의 diff를 검토하고, 두 기준 커밋(기준 브랜치 커밋과 검토한 feature 브랜치 커밋)을 리포트에 기록한다.
- PR은 Reviewer APPROVED 후에 생성한다.
- GATE 4는 TASK를 DONE으로 바꾸지 않는다. GATE 4를 통과한 TASK는 IN_REVIEW로 남는다.
- PR이 실제로 생성된 뒤에, PR 생성 후 작업을 관리하는 담당자가 TASK를 DONE으로 바꾼다.
- DONE은 PR 생성 완료를 뜻한다. 머지 완료를 뜻하지 않는다.
- 사용자는 리뷰된 커밋과 PR 최종 HEAD의 차이를 확인한다. 해당 TASK 상태 외의 변경이 있거나, 기준 브랜치의 변경이 리뷰 결과에 영향을 줄 수 있으면 기존 APPROVED에 의존하지 않고 다시 검토한다.

반영 위치는 `AGENTS.md` §12.1, §12.2, §12.4, §12.5와 `.claude/validators/review-validator.md`, `.claude/agents/reviewer.md`다. §12.6의 "Creating PR before Reviewer APPROVED" 금지는 이미 이 순서와 일치하므로 바꾸지 않는다.

---

## 4. Alternatives Considered

### 전체 게이트 강행

* 장점: 규칙을 바꾸지 않는다.
* 단점: DESIGN의 필수 항목을 "해당 없음"으로 채우게 되어 임의 통과와 같다.

### 1회 면제만 기록

* 장점: 변경이 가장 작다.
* 단점: 다음 문서 작업에서 같은 문제가 반복된다.

### 경로만으로 구분

* 장점: 판단이 쉽다.
* 단점: 워크플로나 프롬프트처럼 경로는 문서 쪽이지만 영향이 큰 변경을 걸러내지 못한다.

### 경량 TASK 도입

* 장점: 문서 작업에도 계획 문서가 남는다.
* 단점: 상태 머신에서 IN_PROGRESS를 설정할 주체가 없어 규칙이 더 늘어난다.

### `AGENTS.md`를 포인터로 두고 `CLAUDE.md`를 원본으로 유지

* 장점: 파일 이동이 없다.
* 단점: `AGENTS.md`를 읽는 도구가 링크를 따라 읽어야만 규칙이 유지된다.

### validator를 기준으로 삼고 프롬프트와 기존 TASK를 고침

* 장점: validator 문언이 그대로다.
* 단점: 기존 TASK 7개와 Planner 프롬프트를 모두 바꿔야 한다.

---

## 5. Rationale

게이트의 목적은 요구사항과 설계 없이 제품 코드가 생기는 것을 막는 데 있다. 그 위험이 없는 변경에는 사람의 검토로 대체하되, 게이트 자체를 바꾸는 변경은 ADR로 남긴다.

기준 문서가 서로 다르면 어떤 산출물도 모든 기준을 동시에 만족할 수 없다. 실제로 쓰이는 형식 하나로 맞추는 것이 가장 작은 변경이다.

리뷰는 PR 전에 끝나야 §12.6과 일치한다. DONE을 PR 생성 뒤로 옮기면 "PR created"라는 기존 조건과도 맞는다.

---

## 6. Consequences

### Positive

* 문서와 CI 변경의 암묵적 우회가 명시적 절차가 된다.
* 헌법이 한 곳에 모이고 도구에 중립적인 이름을 가진다.
* TASK와 DESIGN의 템플릿, validator, 프롬프트가 같은 섹션을 요구한다.
* PR 생성과 DONE의 순서가 하나로 정해진다.

### Negative

* 게이트의 적용 범위가 실제로 줄어든다. 3.3의 분류가 잘못되면 검토 없이 영향 큰 변경이 들어갈 수 있다. 분류와 검토의 부담은 소유자에게 간다.
* 경량 PR은 자동 리뷰를 받지 않는다.
* GATE 1에서 `## Tasks` 2개 이상이라는 검사가 없어진다.
* DONE 커밋이 리뷰된 커밋 뒤에 추가되므로, PR마다 리뷰된 커밋과 최종 HEAD의 차이를 확인해야 한다.

### 이 ADR이 해결하지 않는 불일치

* `.github/workflows/implementer.yml`은 리뷰 전에 PR을 만들고, `.github/workflows/reviewer.yml`은 PR이 열린 뒤 PR diff를 검토한다. 변경 전에도 후에도 §12.6과 어긋난다.
* `reviewer.yml`은 `reviewer.md`를 프롬프트로 쓰면서 기준 커밋을 넘기지 않는다. 워크플로로 실행하면 Review Basis를 채울 수 없다.
* `.claude/workflows/feature-development.md`는 TASK의 섹션을 또 다른 목록으로 적고 있다.
* 기존 ADR-001~005의 머리말 형식은 `.claude/templates/ADR.md`와 다르다. 이 ADR은 템플릿을 따른다.
* TASK-007의 Status는 `IN_REVIEW`로 남아 있다.
* `@AGENTS.md` 참조가 각 도구에서 전문을 읽히는지는 PR의 검증 결과에 적는다.

---

## 7. Related Decisions

* ADR-002
* ADR-003
* ADR-005

---

## 8. Notes

### 부트스트랩 승인 기록

* 일시: 2026-11-17
* 승인자: arok2 (저장소 소유자)
* Issue: #11
* 허용 범위: 라벨 없는 Issue 1개, 최신 develop 기준 feature 브랜치 1개, develop 대상 PR 1개
* 변경 경로:
  1. `docs/decisions/adr/ADR-006-gate-scope-and-lightweight-change.md`
  2. `AGENTS.md`
  3. `CLAUDE.md`
  4. `README.md`
  5. `.claude/templates/TASK.md`
  6. `.claude/templates/DESIGN.md`
  7. `.claude/validators/task-validator.md`
  8. `.claude/validators/code-validator.md`
  9. `.claude/validators/review-validator.md`
  10. `.claude/agents/reviewer.md`
* 로컬 설정 변경: 소유자 로컬 저장소의 `.git/info/exclude`에서 `AGENTS.md` 항목만 제거
* 면제: 이 PR에 한해 TASK, DESIGN, Reviewer 승인 없이 PR을 만드는 것(§12.6)
* 제외: 위 목록 밖의 파일, main, 저장소 설정과 워크플로 활성 상태 변경, 에이전트에 의한 머지
* 성격: PR 작성 허가. 변경 내용의 최종 승인이나 머지 승인이 아님

### 최종 승인 기록

* 일시: 2026-11-17 10:29 UTC
* 승인자: arok2 (저장소 소유자)
* 대상 커밋(X): `0d6377c7cb1abd2d289fe861f7226e2171527f1e`
* 승인 위치: 프로젝트 작업 스레드의 소유자 메시지 (PR 댓글이 아님)
* 성격: ADR-006의 내용 승인. 머지 승인이 아니며, 3.6의 5단계에 따라 소유자의 머지가 최종 승인이다.
