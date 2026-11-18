import pytest
from pydantic import ValidationError

from log_analyzer.schemas import MAX_LOG_CHARS, AnalysisRequest


def test_blank_log_within_limit_is_rejected_by_schema():
    with pytest.raises(ValidationError):
        AnalysisRequest(log=" " * MAX_LOG_CHARS)


def test_blank_log_over_limit_is_left_for_size_check():
    request = AnalysisRequest(log=" " * (MAX_LOG_CHARS + 1))
    assert len(request.log) == MAX_LOG_CHARS + 1
