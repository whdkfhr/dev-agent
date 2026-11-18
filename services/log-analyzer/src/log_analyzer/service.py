"""분석 서비스: 타임아웃 적용, 분석기 호출, 근거 검증, 응답 조립. HTTP를 알지 않는다."""

import asyncio

from log_analyzer.analyzer import Analyzer
from log_analyzer.evidence import check_evidence, split_lines
from log_analyzer.schemas import AnalysisResponse, AnalyzerResult, Cause, Evidence


class AnalysisTimeoutError(Exception):
    """분석기가 타임아웃 안에 결과를 돌려주지 않았다."""


class AnalysisService:
    def __init__(self, analyzer: Analyzer, timeout_seconds: float) -> None:
        self._analyzer = analyzer
        self._timeout_seconds = timeout_seconds

    async def analyze(self, log: str) -> AnalysisResponse:
        try:
            result = await asyncio.wait_for(self._analyzer.analyze(log), timeout=self._timeout_seconds)
        except TimeoutError as exc:
            raise AnalysisTimeoutError("analyzer timed out") from exc
        return verify_result(log, result)


def verify_result(log: str, result: AnalyzerResult) -> AnalysisResponse:
    """검증되지 않은 근거를 제외하고 그 수를 센다.

    근거가 남지 않은 원인 후보와 summary가 빈 문자열인 원인 후보는 뺀다(Cause.summary는 1자 이상).
    summary 때문에 뺀 원인 후보의 근거는 검증된 것이므로 excluded_evidence_count에 세지 않는다.
    """
    lines = split_lines(log)
    excluded = 0
    causes: list[Cause] = []
    for cause in result.causes:
        verified: list[Evidence] = []
        for item in cause.evidence:
            check = check_evidence(lines, item.line, item.text)
            if check.verified and check.value is not None:
                verified.append(Evidence(line=item.line, text=check.value))
            else:
                excluded += 1
        if verified and cause.summary:
            causes.append(Cause(summary=cause.summary, evidence=verified))
    next_checks = [item for item in result.next_checks if item.strip()]
    return AnalysisResponse(causes=causes, next_checks=next_checks, excluded_evidence_count=excluded)
