import asyncio

import pytest

from log_analyzer.analyzer import AnalyzerError
from log_analyzer.fake_analyzer import (
    DEFAULT_NEXT_CHECK,
    DEFAULT_SUMMARY,
    FakeAnalyzer,
    FakeMode,
)
from log_analyzer.schemas import AnalyzerResult, Cause, Evidence

from conftest import line_number


def test_success_mode_returns_given_result_and_counts_calls():
    given = AnalyzerResult(causes=[Cause(summary="s", evidence=[Evidence(line=1, text="x")])], next_checks=["c"])
    analyzer = FakeAnalyzer(result=given)
    assert asyncio.run(analyzer.analyze("x")) == given
    assert analyzer.call_count == 1


def test_error_mode_raises_analyzer_error():
    analyzer = FakeAnalyzer(mode=FakeMode.ERROR)
    with pytest.raises(AnalyzerError):
        asyncio.run(analyzer.analyze("x"))
    assert analyzer.call_count == 1


def test_hang_mode_ends_only_by_cancellation():
    analyzer = FakeAnalyzer(mode=FakeMode.HANG)

    async def run() -> None:
        await asyncio.wait_for(analyzer.analyze("x"), timeout=0.01)

    with pytest.raises(TimeoutError):
        asyncio.run(run())
    assert analyzer.cancelled is True


def test_default_mode_uses_first_three_keyword_lines_as_evidence(failure_log):
    result = asyncio.run(FakeAnalyzer().analyze(failure_log))
    expected_lines = [
        line_number(failure_log, "E       AssertionError: assert 90 == 100"),
        line_number(failure_log, "tests/test_order.py:12: AssertionError"),
        line_number(failure_log, "FAILED tests/test_order.py::test_total_price"),
    ]
    assert len(result.causes) == 1
    assert result.causes[0].summary == DEFAULT_SUMMARY
    assert [item.line for item in result.causes[0].evidence] == expected_lines
    lines = failure_log.split("\n")
    for item in result.causes[0].evidence:
        assert item.text == lines[item.line - 1].strip()
    assert result.next_checks == [DEFAULT_NEXT_CHECK]


def test_default_mode_returns_no_cause_for_passing_log(pass_log):
    result = asyncio.run(FakeAnalyzer().analyze(pass_log))
    assert result.causes == []
    assert result.next_checks == []
