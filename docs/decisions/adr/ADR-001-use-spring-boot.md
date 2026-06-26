# ADR-001: Spring Boot 선택

- **Status**: Accepted
- **Date**: 2026-06-26
- **Deciders**: arok2

## Context

이 프로젝트의 핵심 학습 목표는 AI Agent 그 자체가 아니라, AI Workflow를 떠받치는
**Backend Architecture를 설계하고 학습하는 것**이다. 따라서 언어와 프레임워크는
새로운 학습 부담을 늘리기보다, 비즈니스 로직과 워크플로우 설계에 집중할 수 있게
해주는 선택이어야 한다.

또한 시스템 특성상 REST API, GitHub Webhook 수신, 비동기 처리, 외부 API 연동 등이
필수적으로 요구된다.

## Decision

백엔드 프레임워크로 **Java 17 + Spring Boot 3.x**를 선택한다.

근거:

- Java는 가장 익숙한 언어이므로 비즈니스 로직과 AI Workflow 설계에 집중할 수 있다.
- AI Agent 자체보다 **Backend Architecture를 학습하는 것**이 이번 프로젝트의 핵심이다.
- Spring Boot는 REST API, Webhook, 비동기 처리, GitHub 연동 등에 필요한 생태계가
  잘 갖춰져 있다.
- 실무에서도 널리 사용되는 프레임워크이므로 **포트폴리오 가치가 높다.**

## Consequences

**긍정적**

- 익숙한 환경에서 아키텍처 설계에 집중할 수 있다.
- 풍부한 생태계로 Webhook/비동기/외부 연동 구현이 빠르다.
- 실무 친화적이어서 포트폴리오로서의 설득력이 크다.

**부정적 / 트레이드오프**

- JVM 기반이라 경량 스크립트 대비 기동/메모리 비용이 있다.
- AI/LLM 생태계의 1차 언어(Python)가 아니므로, 일부 SDK·예제는 직접 포팅이 필요할 수 있다.

## Related

- [[ADR-002-use-claude-code]]
- [[ADR-003-multi-agent-architecture]]
