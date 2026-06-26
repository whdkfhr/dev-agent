# ADR-002: Claude Code 선택

- **Status**: Accepted
- **Date**: 2026-06-26
- **Deciders**: arok2

## Context

AI Agent를 구동할 도구(모델/툴)를 선택해야 한다. 선택지로는 Claude Code, OpenAI
Codex, Gemini 등이 있다. 다만 이 프로젝트의 목적은 **여러 LLM의 성능을 비교 실험하는
것이 아니라, AI 개발 프로세스(Workflow)를 구축하는 것**이다.

## Decision

AI Agent 실행 도구로 **Claude Code (Anthropic)**를 선택한다.

근거:

- Claude Code를 이미 지속적으로 사용해왔기 때문에 작업 방식이 익숙하다.
- 프로젝트의 목적은 LLM 비교 실험이 아니라 **AI 개발 프로세스 구축**이다.
- 따라서 모델을 비교하기보다 **안정적인 개발 환경을 유지하는 것**을 우선한다.
- Agent Workflow 설계가 핵심이므로, 특정 모델에 종속되지 않도록 추후 다른
  모델(OpenAI Codex, Gemini 등)로 **교체 가능한 구조**를 목표로 한다.

## Consequences

**긍정적**

- 익숙한 도구로 진입 비용 없이 워크플로우 설계에 집중할 수 있다.
- 안정적인 개발 환경으로 결과의 일관성을 확보한다.

**부정적 / 트레이드오프**

- 현 시점에서는 단일 도구에 의존한다 → 이를 완화하기 위해 외부 모델/도구를
  **인터페이스로 추상화**하여 교체 가능성을 유지한다.
- 모델 간 성능 비교 데이터는 본 프로젝트 범위에서 다루지 않는다.

## Related

- [[ADR-003-multi-agent-architecture]]
- [[ADR-005-documentation-first-development]]
