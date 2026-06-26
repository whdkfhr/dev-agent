# ADR-004: GitHub 선택

- **Status**: Accepted
- **Date**: 2026-06-26
- **Deciders**: arok2

## Context

이 프로젝트의 최종 목표는 **Issue → PR 자동화**다. 이를 구현하려면 Issue 관리,
이벤트 수신(Webhook), Pull Request 생성, 그리고 이후 자동 검증/배포까지 지원하는
플랫폼이 필요하다.

## Decision

대상 플랫폼으로 **GitHub**를 선택한다.

근거:

- GitHub는 업계 표준에 가까운 Git 플랫폼이다.
- Issue, Pull Request, Webhook, Actions 등 **자동화 기능이 풍부하다.**
- 이번 프로젝트의 목표인 **Issue → PR 자동화**를 구현하기에 가장 적합하다.
- 향후 **GitHub Actions와 연계한 자동 검증 및 배포**까지 확장하기 쉽다.

## Consequences

**긍정적**

- 자동화 파이프라인 구현에 필요한 기능(Issue/PR/Webhook/Actions)이 한 플랫폼에 모여 있다.
- 표준 플랫폼이라 포트폴리오 전달력과 재현성이 높다.
- CI/CD(Actions) 확장 경로가 자연스럽다.

**부정적 / 트레이드오프**

- 현재 설계는 GitHub API/Webhook 모델에 결합된다 → 향후 다른 플랫폼(GitLab 등)
  전환 시 외부 연동부를 **인터페이스로 추상화**해 둘 필요가 있다.

## Related

- [[ADR-003-multi-agent-architecture]]
