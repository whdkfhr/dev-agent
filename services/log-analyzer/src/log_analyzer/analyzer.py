"""분석기 경계. 2단계에서 실제 AI 구현을 이 경계 뒤에 추가한다."""

from typing import Protocol

from log_analyzer.schemas import AnalyzerResult


class AnalyzerError(Exception):
    """분석기가 결과를 만들지 못했음을 알린다."""


class Analyzer(Protocol):
    async def analyze(self, log: str) -> AnalyzerResult:
        """로그를 받아 검증 전 분석 결과를 돌려준다.

        취소에 협조해야 한다. 이벤트 루프를 막는 동기 작업을 하면
        서비스 타임아웃이 제때 동작하지 않는다 (DESIGN-015).
        """
        ...
