# ADR-007: Python/FastAPI 테스트 로그 분석 서비스 도입

## 1. Status

Proposed

* 날짜: 2026-11-17

Accepted로 바뀌는 시점과 조건은 3.10에 적는다.

---

## 2. Context

- 테스트 실행 로그를 사람이 직접 읽고 원인을 좁히는 데 시간이 든다. 로그를 입력받아 원인 후보와 그 근거, 다음 확인 사항을 돌려주는 API를 만들고자 한다.
- 이 기능은 AI 모델 호출이 핵심이며, AI/LLM 생태계의 1차 언어는 Python이다. ADR-001도 이 점을 부정적 결과로 적었다.
- 지금의 규칙은 Java/Spring 하나만 전제한다.
  - `AGENTS.md` §6의 기술 스택은 Java 17, Spring Boot, Gradle뿐이다.
  - `design-validator.md`는 영속 Entity와 `com.arok2.dev_agent` 패키지를 전제로 하고, Java 코드만 금지한다.
  - `implementer.md`와 `code-validator.md`는 Spring 계층 규칙과 Java 17 기준만 검사한다.
- 헌법 §12.0의 경로 기준은 `src/`뿐이다. 새 서비스를 다른 경로에 두면, 서비스 생성 이후의 코드 변경이 게이트 대상인지 해석이 갈린다.
- 기존 Failure Analyzer는 Reviewer의 REJECTED 리포트와 TASK, DESIGN, PR diff를 교차 분석해 Fix Plan을 만든다. 이름과 목적이 비슷해 보이므로 책임 경계를 정해 둘 필요가 있다.
- DESIGN-006과 DESIGN-007에는 `## Trade-offs` 섹션이 없다. architect.md와 DESIGN 템플릿에는 있으나 design-validator에는 필수 항목이 아니다.

---

## 3. Decision

### 3.1 서비스와 위치

같은 저장소 안에 독립 실행되는 Python/FastAPI 서비스를 둔다.

* 위치: `services/log-analyzer/`
* Spring 애플리케이션(`src/`)과 코드, 빌드, 실행 단위를 공유하지 않는다.
* ADR-001을 대체하지 않는다. Spring 서비스의 기술 선택은 그대로이며, 이 ADR은 Python 서비스를 허용하는 범위를 더한다.

### 3.2 기능

* 입력: 사람이 수동으로 넣는 테스트 실행 로그
* 출력: 원인 후보, 로그에 실제 존재하는 근거, 다음 확인 사항
* 근거는 입력 로그에 실제로 있는 내용이어야 한다. 검증 방법은 해당 DESIGN에서 정한다.

### 3.3 Failure Analyzer와의 책임 경계

| 항목 | Failure Analyzer | 로그 분석 서비스 |
| ---- | ---------------- | ---------------- |
| 입력 | Reviewer 리포트, TASK, DESIGN, PR diff | 사람이 넣은 테스트 실행 로그 |
| 출력 | `FIX-ANALYSIS-{ID}-retry{N}.md` (Fix Plan) | API 응답 (원인 후보, 근거, 다음 확인 사항) |
| 실행 | GitHub Actions 워크플로 | 독립 HTTP 서비스 |
| 목적 | Fix Agent의 최소 수정 계획 | 사람의 원인 파악 보조 |

두 기능은 서로 호출하지 않는다. 연동이 필요해지면 별도 결정으로 다룬다.

### 3.4 기술 스택

* Python 3.12
* FastAPI, Pydantic v2
* pytest, FastAPI의 로컬 테스트 클라이언트
* uv (Python 버전과 의존성 관리)

라이브러리 버전은 1단계 구현의 빌드 구성(pyproject와 lock 파일)에서 고정한다. 빌드 구성은 §12.0에 따라 전체 게이트 대상이다. lint 도구 사용 여부는 1단계 DESIGN에서 정한다.

실행 환경 확인 결과는 8. Notes에 적는다.

### 3.5 단계

**1단계: 가짜 분석기로 API와 테스트 완성**

* API와 정상, 잘못된 입력, 분석기 오류, 타임아웃을 검증하는 테스트를 완성한다.
* 분석기 실패와 타임아웃은 가짜 분석기로 재현한다.
* 직접 만든 테스트 로그를 사용한다.
* 실제 AI 연동은 구현 범위에서 제외한다.

**2단계: 실제 AI 모델 연결**

* 실제 AI 모델을 연결하고, 외부 호출의 오류와 타임아웃, 응답 결과를 명시적으로 실행하는 별도 테스트로 확인한다.
* 직접 만든 테스트 로그를 사용한다.
* AI 제공자로 전송되는 지시문과 로그의 내용, 제공자의 데이터 처리 정책을 2단계 설계에서 확인하고 DESIGN에 기록한다.

두 단계는 각각 Issue, TASK, DESIGN, 구현, 리뷰를 거친다(§12.0의 전체 게이트).

### 3.6 두 단계에 공통으로 적용하는 원칙

* 분석기는 교체 가능한 경계 뒤에 둔다. 경계의 구체적 형태는 DESIGN에서 정한다.
* 자동화 테스트는 외부 서비스, 실제 AI, secret 없이 실행한다. FastAPI의 로컬 테스트 클라이언트 사용은 허용한다.
* 실제 AI를 호출하는 확인은 명시적으로 실행하는 별도 테스트로 구분하며, 자동화 테스트의 기본 실행에 포함하지 않는다.
* secret을 코드와 저장소에 두지 않는다.

### 3.7 2단계의 추가 ADR

2단계 Issue를 작성할 때, 이 ADR의 내용과 2단계의 변경 범위를 보고 ADR-006 3.3의 질문으로 판단한다. 이 ADR에서 미리 선행 조건으로 정하지 않는다. 분류는 소유자가 확정한다.

### 3.8 범위 밖

* Spring 서비스와의 연동
* 배포
* 이 서비스를 실행하는 GitHub Actions 워크플로

### 3.9 규칙 변경

| 파일 | 변경 |
| ---- | ---- |
| `AGENTS.md` | §6을 서비스별 스택으로 나눔. §12.0에 `services/` 추가. §12.6 "Architect writing Java code"를 "implementation code"로. Version 1.2 |
| `docs/architecture/architecture.md` | Tech Stack 행, 로그 분석 서비스 절 추가 |
| `docs/product/roadmap.md` | Phase 9 추가 |
| `.claude/templates/DESIGN.md`, `.claude/agents/architect.md`, `.claude/validators/design-validator.md` | Model과 Persistence 표기, 서비스별 Package Structure, 구현 코드 금지, 외부 의존의 타임아웃과 오류, Trade-offs 기준 |
| `.claude/agents/implementer.md`, `.claude/validators/code-validator.md` | Java 기준을 `src/` 대상으로 한정, Python 공통 기준 추가 |
| `.claude/agents/reviewer.md` | Python 공통 기준 위반의 Severity 예시 추가 |

GATE 0~4의 정의(§12.5)와 상태 머신(§12.4)은 바꾸지 않는다. GATE 3의 "Source code compiles"를 Python에 적용하는 방법은 `code-validator.md`에 적는다.

새 DESIGN 기준(Model 표기, Trade-offs 기준 등)은 이 ADR이 develop에 머지된 이후 새로 작성하거나 GATE 2를 다시 받는 DESIGN에 적용한다. 그 전에 작성되어 다시 검증받지 않는 DESIGN은 수정하지 않는다.

### 3.10 Proposed에서 Accepted로 바뀌는 시점

이 순서는 이 ADR에만 적용한다.

1. PR은 Status가 Proposed인 채로 올라간다.
2. 소유자가 PR 댓글로 내용 승인을 남기고, 검토한 커밋 ID를 적는다. 이것은 머지 승인이 아니다.
3. 이 파일의 Status, 날짜, Notes의 승인 기록만 바꾸는 상태 커밋을 추가한다.
4. 소유자가 최종 커밋을 확인하고 직접 머지한다. 이 머지가 최종 승인이고, 규칙은 develop에 머지된 시점부터 효력을 가진다.

승인한 커밋 이후에 상태 커밋이 아닌 변경이 생기면 내용 승인은 무효가 되고 2단계부터 다시 한다.

---

## 4. Alternatives Considered

### Spring 애플리케이션 안에 구현

* 장점: 언어와 규칙을 늘리지 않는다.
* 단점: AI SDK와 예제를 직접 포팅해야 하고, 실험적 기능이 기존 애플리케이션의 빌드와 테스트에 섞인다.

### 별도 저장소

* 장점: 규칙과 빌드가 완전히 분리된다.
* 단점: 이 프로젝트의 문서 우선 절차와 에이전트 파이프라인을 함께 쓸 수 없다.

### 1단계부터 실제 AI 연동

* 장점: 실제 응답을 빨리 볼 수 있다.
* 단점: API 계약과 오류 처리를 외부 응답에 맞춰 흔들리게 만들고, 실패와 타임아웃을 결정적으로 재현하기 어렵다.

### Python 3.13

* 장점: 지원 기간이 더 길다.
* 단점: 3.12보다 실익이 크지 않고, 확인된 환경에서 3.12로 충분하다.

---

## 5. Rationale

AI 연동이 핵심인 기능은 Python에서 만드는 비용이 가장 낮다. 같은 저장소에 두면 기존 절차(Issue, TASK, DESIGN, 리뷰)를 그대로 쓸 수 있고, 독립 실행 단위로 두면 Spring 애플리케이션에 영향을 주지 않는다.

가짜 분석기로 API와 테스트를 먼저 완성하면 외부 응답과 무관하게 계약과 오류 처리를 확정할 수 있다. 실제 AI 연동은 그 계약 위에서 분석기만 바꾸는 작업이 된다.

규칙은 언어별 세부 구조를 강제하지 않고 공통 원칙만 정한다. 구조는 실제 기능의 DESIGN에서 정하는 것이 문서 우선 원칙과 맞다.

---

## 6. Consequences

### Positive

* AI 연동 기능을 Spring 애플리케이션과 분리해 실험할 수 있다.
* 자동화 테스트가 외부 서비스에 의존하지 않아 결정적이다.
* `services/` 아래 변경이 게이트 범위에 명시된다.
* stateless 서비스도 DESIGN 기준을 정직하게 통과할 수 있다.

### Negative

* 저장소에 두 번째 언어와 빌드 도구가 생긴다.
* Python 서비스의 검증은 GitHub Actions 워크플로 없이 시작한다. 실행과 확인이 사람에게 맡겨진다.
* 실행 환경마다 Python 3.12 설치 가능 여부가 다르다(8. Notes).
* 기존 DESIGN-006, 007은 새 Trade-offs 기준을 만족하지 않는 채로 남는다.

### 이 ADR이 해결하지 않는 것

* 이 서비스의 에이전트 작업을 워크플로로 실행할 때의 문제. `implementer.yml`은 리뷰 전에 PR을 만들고(ADR-006 6장), `run_implementer.py`의 출력 형식은 `src/main/java` 경로를 전제로 한다. 이 서비스의 단계 작업은 에이전트를 수동으로 실행해 진행한다.
* `architecture.md`의 기존 Java 패키지 표기(`com.devagent`)와 실제 패키지(`com.arok2.dev_agent`)의 차이

---

## 7. Related Decisions

* ADR-001
* ADR-003
* ADR-005
* ADR-006

---

## 8. Notes

* Issue: #13

### 실행 환경 확인 (2026-11-17)

| 환경 | Python 3.12 | uv | 결과 |
| ---- | ----------- | -- | ---- |
| 클라우드 작업 컨테이너 (Linux x86_64) | uv로 3.12.13 설치 성공 | 0.11.32 | FastAPI 0.163.1, Pydantic 2.14.3, pytest 9.2.0, httpx 0.28.1 설치 후 로컬 테스트 클라이언트로 정상 요청과 422 응답 테스트 통과 |
| 소유자 데스크톱 앱의 Linux VM (Ubuntu 22.04, aarch64) | 시스템 Python 3.10.12, 3.12 설치 실패 | 0.13.4 | 네트워크 정책으로 Python 배포본과 PyPI에 접근 불가 |
| 소유자 Mac 본체 | 미확인 | 미확인 | 이 작업에서 접근할 수 없었음 |

* FastAPI 0.163.1은 Python 3.11 이상을 요구한다(PyPI 확인). 3.10에서는 쓸 수 없다.
* 1단계 구현 전에 소유자 Mac에서 `uv python install 3.12`와 의존성 설치를 확인한다.
* 대안: Mac에서 uv로 설치할 수 없으면 Homebrew의 `python@3.12`를 쓰고 uv는 의존성 관리에만 쓴다. 3.12를 쓸 수 없으면 FastAPI의 최소 요구인 3.11로 낮추고 이 ADR을 고친다.

### 승인 기록

(3.10의 3단계에서 적는다)
