# ADR-005: Documentation First Development

- **Status**: Accepted
- **Date**: 2026-06-26
- **Deciders**: arok2

## Context

AI Agent는 사람처럼 프로젝트의 전체 맥락을 지속적으로 기억하지 못한다. 세션이
바뀌거나 Agent가 교체되면 맥락이 사라지기 쉽다. 따라서 프로젝트의 맥락을 어디에
보존하고, Agent가 무엇을 근거로 작업하게 할지를 결정해야 한다.

## Decision

**문서 우선 개발(Documentation First Development)**을 개발 원칙으로 채택한다.
즉, **문서가 먼저 존재하고, 코드는 문서를 구현한 결과물**이다.

근거:

- AI Agent는 사람처럼 프로젝트의 전체 맥락을 기억하지 못한다.
- Vision, Roadmap, Task, Architecture 문서를 프로젝트의 **"공유 기억(shared memory)"**으로 사용한다.
- Agent는 문서를 기반으로 계획, 설계, 구현을 수행한다.
- 문서가 최신 상태로 유지될수록 AI의 결과 품질과 일관성이 향상된다.

## 개발 원칙 (Workflow)

```
GitHub Issue
      ↓
Planner
      ↓
Task 문서 생성
      ↓
Architect
      ↓
Architecture 문서 생성
      ↓
Implementer
      ↓
Code 작성
      ↓
Reviewer
      ↓
Review 결과 문서
      ↓
Pull Request
```

즉, **문서가 먼저 존재하고, 코드는 문서를 구현한 결과물이다.**

## Consequences

**긍정적**

- 세션/Agent가 바뀌어도 문서를 통해 맥락이 유지된다(공유 기억).
- 각 단계 산출물이 문서로 남아 추적성과 일관성이 높아진다.
- 새로운 Agent가 합류해도 문서만으로 온보딩이 가능하다.

**부정적 / 트레이드오프**

- 문서와 코드의 동기화 비용이 발생한다 → 문서가 낡으면 오히려 품질이 저하되므로,
  문서 갱신을 워크플로우의 필수 단계로 포함해야 한다.

## Related

- [[ADR-002-use-claude-code]]
- [[ADR-003-multi-agent-architecture]]
