import asyncio

import pytest
from pydantic import ValidationError

from log_analyzer.analyzer import AnalyzerError
from log_analyzer.fake_analyzer import FakeAnalyzer, FakeMode
from log_analyzer.schemas import AnalyzerResult, Cause, Evidence
from log_analyzer.service import AnalysisService, AnalysisTimeoutError, verify_result

LOG = "line one\nE   AssertionError: assert 90 == 100\nline three"


def test_unverified_evidence_is_excluded_and_counted():
    result = AnalyzerResult(
        causes=[
            Cause(
                summary="합계 불일치",
                evidence=[
                    Evidence(line=2, text=" assert 90 == 100 "),
                    Evidence(line=2, text="not in log"),
                    Evidence(line=9, text="line one"),
                ],
            )
        ],
        next_checks=["할인 계산 확인"],
    )
    response = verify_result(LOG, result)
    assert response.excluded_evidence_count == 2
    assert response.causes == [Cause(summary="합계 불일치", evidence=[Evidence(line=2, text="assert 90 == 100")])]
    assert response.next_checks == ["할인 계산 확인"]


def test_cause_without_verified_evidence_is_removed():
    result = AnalyzerResult(
        causes=[
            Cause(summary="남는 원인", evidence=[Evidence(line=1, text="line one")]),
            Cause(summary="빠지는 원인", evidence=[Evidence(line=3, text="nope"), Evidence(line=1, text="three")]),
            Cause(summary="근거 없는 원인", evidence=[]),
        ]
    )
    response = verify_result(LOG, result)
    assert [cause.summary for cause in response.causes] == ["남는 원인"]
    assert response.excluded_evidence_count == 2


def test_cause_summary_must_not_be_empty():
    """회귀: Cause.summary는 1자 이상이다 (DESIGN-015 Data Model)."""
    with pytest.raises(ValidationError):
        Cause(summary="", evidence=[Evidence(line=1, text="line one")])


def test_empty_summary_from_unvalidated_result_is_not_returned():
    """회귀: 검증을 거치지 않은 빈 summary가 들어와도 성공 응답을 만들지 않는다."""
    cause = Cause.model_construct(summary="", evidence=[Evidence(line=1, text="line one")])
    result = AnalyzerResult.model_construct(causes=[cause], next_checks=[])
    with pytest.raises(ValidationError):
        verify_result(LOG, result)


def test_blank_next_checks_are_removed():
    result = AnalyzerResult(causes=[], next_checks=["확인 1", "", "   ", "확인 2"])
    response = verify_result(LOG, result)
    assert response.next_checks == ["확인 1", "확인 2"]
    assert response.causes == []
    assert response.excluded_evidence_count == 0


def test_analyzer_error_is_propagated():
    service = AnalysisService(FakeAnalyzer(mode=FakeMode.ERROR), timeout_seconds=1.0)
    with pytest.raises(AnalyzerError):
        asyncio.run(service.analyze(LOG))


def test_timeout_raises_timeout_error_and_cancels_analyzer():
    analyzer = FakeAnalyzer(mode=FakeMode.HANG)
    service = AnalysisService(analyzer, timeout_seconds=0.02)
    with pytest.raises(AnalysisTimeoutError):
        asyncio.run(service.analyze(LOG))
    assert analyzer.cancelled is True
