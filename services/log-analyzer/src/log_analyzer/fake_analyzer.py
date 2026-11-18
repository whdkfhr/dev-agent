"""1단계용 가짜 분석기. 정상 결과, 오류, 응답 없음을 결정적으로 재현한다."""

import asyncio
from enum import Enum

from log_analyzer.analyzer import AnalyzerError
from log_analyzer.evidence import split_lines
from log_analyzer.schemas import AnalyzerResult, Cause, Evidence

DEFAULT_KEYWORDS = ("FAILED", "ERROR", "Error", "Exception")
DEFAULT_MAX_EVIDENCE = 3
DEFAULT_SUMMARY = "로그에 실패 또는 오류를 나타내는 줄이 있다"
DEFAULT_NEXT_CHECK = "근거로 표시된 줄의 테스트와 오류 메시지를 확인한다"


class FakeMode(Enum):
    SUCCESS = "success"
    ERROR = "error"
    HANG = "hang"


class FakeAnalyzer:
    def __init__(self, mode: FakeMode = FakeMode.SUCCESS, result: AnalyzerResult | None = None) -> None:
        self.mode = mode
        self.result = result
        self.call_count = 0
        self.cancelled = False

    async def analyze(self, log: str) -> AnalyzerResult:
        self.call_count += 1
        if self.mode is FakeMode.ERROR:
            raise AnalyzerError("fake analyzer failure")
        if self.mode is FakeMode.HANG:
            try:
                await asyncio.Event().wait()
            except asyncio.CancelledError:
                self.cancelled = True
                raise
        if self.result is not None:
            return self.result
        return keyword_result(log)


def keyword_result(log: str) -> AnalyzerResult:
    """로컬 실행용 기본 동작: 키워드가 들어간 처음 3줄을 근거로 원인 후보 1개를 만든다."""
    evidence = [
        Evidence(line=number, text=line.strip())
        for number, line in enumerate(split_lines(log), start=1)
        if line.strip() and any(keyword in line for keyword in DEFAULT_KEYWORDS)
    ][:DEFAULT_MAX_EVIDENCE]
    if not evidence:
        return AnalyzerResult(causes=[], next_checks=[])
    return AnalyzerResult(
        causes=[Cause(summary=DEFAULT_SUMMARY, evidence=evidence)],
        next_checks=[DEFAULT_NEXT_CHECK],
    )
