# ADR-003: Multi-Agent Architecture

- **Status**: Accepted
- **Date**: 2026-06-26
- **Deciders**: arok2

## Context

Issue로부터 PR까지 이어지는 개발 자동화를 구현하려면 요구사항 분석, 설계, 구현,
검증이라는 여러 책임이 필요하다. 이 모든 책임을 단일 Agent에게 맡길지, 역할별로
나눌지를 결정해야 한다.

## Decision

역할별로 분리된 **Multi-Agent Architecture**를 채택한다.
(Planner → Architect → Implementer → Reviewer)

근거:

- 하나의 Agent에게 모든 책임을 맡기면 요구사항 분석·설계·구현·검증이 혼재되어
  결과 품질이 일정하지 않을 수 있다.
- **사람의 개발 프로세스와 유사하게 역할을 분리**하면 각 단계의 책임이 명확해진다.
- 각 Agent의 출력물이 다음 Agent의 입력이 되므로 **추적 가능성(traceability)**이 높아진다.
- 향후 Agent를 교체하거나 추가하기 쉽다.

## Consequences

**긍정적**

- 단계별 책임 분리로 결과 품질의 일관성이 향상된다.
- 산출물이 단계마다 문서로 남아 추적성이 높다.
- 새 Agent 추가/교체가 용이하다(확장성).

**부정적 / 트레이드오프**

- Agent 간 전달(handoff)과 워크플로우 오케스트레이션 로직이 추가로 필요하다.
- 단계가 많아져 단일 Agent 대비 전체 실행 시간이 늘어날 수 있다.

## Related

- [[ADR-002-use-claude-code]]
- [[ADR-005-documentation-first-development]]
