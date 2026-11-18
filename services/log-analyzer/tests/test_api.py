import pytest

from log_analyzer.app import create_app
from log_analyzer.config import TIMEOUT_ENV, Settings
from log_analyzer.fake_analyzer import DEFAULT_NEXT_CHECK, DEFAULT_SUMMARY, FakeAnalyzer, FakeMode
from log_analyzer.schemas import MAX_LOG_CHARS, AnalyzerResult, Cause, Evidence

from conftest import line_number, make_client

ASSERTION_LINE = "E       AssertionError: assert 90 == 100"
FAILED_LINE = "FAILED tests/test_order.py::test_total_price - AssertionError: assert 90 == 100"


def assert_evidence_in_log(log: str, body: dict) -> None:
    lines = log.split("\n")
    for cause in body["causes"]:
        for item in cause["evidence"]:
            assert 1 <= item["line"] <= len(lines)
            assert item["text"] in lines[item["line"] - 1]


def assert_error(response, status_code: int, code: str, log: str | None = None) -> None:
    assert response.status_code == status_code
    body = response.json()
    assert list(body.keys()) == ["error"]
    assert set(body["error"].keys()) == {"code", "message"}
    assert body["error"]["code"] == code
    message = body["error"]["message"]
    assert message
    assert "Traceback" not in message
    assert "/" not in message and "\\" not in message
    assert ".py" not in message
    if log is not None and log.strip():
        assert log.strip() not in message


def test_failure_log_returns_causes_evidence_and_next_checks(failure_log):
    result = AnalyzerResult(
        causes=[
            Cause(
                summary="할인이 적용된 합계가 기대값과 다르다",
                evidence=[
                    Evidence(line=line_number(failure_log, ASSERTION_LINE), text=ASSERTION_LINE),
                    Evidence(line=line_number(failure_log, FAILED_LINE), text=FAILED_LINE),
                ],
            )
        ],
        next_checks=["Order.total()의 할인 계산을 확인한다"],
    )
    analyzer = FakeAnalyzer(result=result)
    response = make_client(analyzer).post("/analyses", json={"log": failure_log})

    assert response.status_code == 200
    body = response.json()
    assert body == {
        "causes": [
            {
                "summary": "할인이 적용된 합계가 기대값과 다르다",
                "evidence": [
                    {"line": line_number(failure_log, ASSERTION_LINE), "text": ASSERTION_LINE},
                    {"line": line_number(failure_log, FAILED_LINE), "text": FAILED_LINE},
                ],
            }
        ],
        "next_checks": ["Order.total()의 할인 계산을 확인한다"],
        "excluded_evidence_count": 0,
    }
    assert_evidence_in_log(failure_log, body)
    assert analyzer.call_count == 1


def test_substring_evidence_with_surrounding_spaces_is_trimmed(failure_log):
    line = line_number(failure_log, ASSERTION_LINE)
    result = AnalyzerResult(causes=[Cause(summary="합계 불일치", evidence=[Evidence(line=line, text="  assert 90 == 100 ")])])
    analyzer = FakeAnalyzer(result=result)
    response = make_client(analyzer).post("/analyses", json={"log": failure_log})

    assert response.status_code == 200
    body = response.json()
    assert body["causes"][0]["evidence"] == [{"line": line, "text": "assert 90 == 100"}]
    assert_evidence_in_log(failure_log, body)
    assert body["excluded_evidence_count"] == 0
    assert analyzer.call_count == 1


def test_evidence_not_in_log_is_excluded(failure_log):
    line = line_number(failure_log, ASSERTION_LINE)
    result = AnalyzerResult(
        causes=[
            Cause(
                summary="합계 불일치",
                evidence=[
                    Evidence(line=line, text=ASSERTION_LINE),
                    Evidence(line=line, text="KeyError: 'price'"),
                    Evidence(line=10_000, text="assert 90 == 100"),
                ],
            )
        ],
        next_checks=["할인 계산 확인"],
    )
    analyzer = FakeAnalyzer(result=result)
    response = make_client(analyzer).post("/analyses", json={"log": failure_log})

    assert response.status_code == 200
    body = response.json()
    assert body["causes"] == [{"summary": "합계 불일치", "evidence": [{"line": line, "text": ASSERTION_LINE}]}]
    assert body["next_checks"] == ["할인 계산 확인"]
    assert body["excluded_evidence_count"] == 2
    assert_evidence_in_log(failure_log, body)
    assert analyzer.call_count == 1


def test_all_evidence_unverified_returns_empty_causes(failure_log):
    result = AnalyzerResult(
        causes=[Cause(summary="없는 원인", evidence=[Evidence(line=1, text="NullPointerException")])],
        next_checks=["설정 확인"],
    )
    analyzer = FakeAnalyzer(result=result)
    response = make_client(analyzer).post("/analyses", json={"log": failure_log})

    assert response.status_code == 200
    body = response.json()
    assert body["causes"] == []
    assert body["excluded_evidence_count"] == 1
    assert analyzer.call_count == 1


def test_default_app_uses_keyword_rule(failure_log, monkeypatch):
    monkeypatch.delenv(TIMEOUT_ENV, raising=False)
    from fastapi.testclient import TestClient

    response = TestClient(create_app()).post("/analyses", json={"log": failure_log})

    assert response.status_code == 200
    body = response.json()
    assert len(body["causes"]) == 1
    assert body["causes"][0]["summary"] == DEFAULT_SUMMARY
    assert len(body["causes"][0]["evidence"]) == 3
    assert body["next_checks"] == [DEFAULT_NEXT_CHECK]
    assert body["excluded_evidence_count"] == 0
    assert_evidence_in_log(failure_log, body)


def test_pass_log_returns_empty_result(pass_log):
    analyzer = FakeAnalyzer()
    response = make_client(analyzer).post("/analyses", json={"log": pass_log})

    assert response.status_code == 200
    assert response.json() == {"causes": [], "next_checks": [], "excluded_evidence_count": 0}
    assert analyzer.call_count == 1


@pytest.mark.parametrize(
    "payload",
    [
        pytest.param({}, id="missing"),
        pytest.param({"log": ""}, id="empty"),
        pytest.param({"log": "  \n\t "}, id="blank"),
        pytest.param({"log": 123}, id="number"),
        pytest.param({"log": None}, id="null"),
        pytest.param({"log": ["a"]}, id="list"),
        pytest.param(["log"], id="not-object"),
    ],
)
def test_invalid_input_is_rejected_without_calling_analyzer(payload):
    analyzer = FakeAnalyzer()
    response = make_client(analyzer).post("/analyses", json=payload)

    assert_error(response, 422, "INVALID_INPUT")
    assert analyzer.call_count == 0


def test_body_that_is_not_json_is_rejected():
    analyzer = FakeAnalyzer()
    response = make_client(analyzer).post(
        "/analyses", content=b"FAILED tests/test_x.py", headers={"Content-Type": "application/json"}
    )

    assert_error(response, 422, "INVALID_INPUT", log="FAILED tests/test_x.py")
    assert analyzer.call_count == 0


def test_log_over_limit_is_rejected_without_calling_analyzer():
    analyzer = FakeAnalyzer()
    log = "a" * (MAX_LOG_CHARS + 1)
    response = make_client(analyzer).post("/analyses", json={"log": log})

    assert_error(response, 413, "LOG_TOO_LARGE", log=log)
    assert analyzer.call_count == 0


def test_log_at_limit_is_accepted():
    analyzer = FakeAnalyzer()
    response = make_client(analyzer).post("/analyses", json={"log": "a" * MAX_LOG_CHARS})

    assert response.status_code == 200
    assert response.json() == {"causes": [], "next_checks": [], "excluded_evidence_count": 0}
    assert analyzer.call_count == 1


def test_analyzer_error_returns_502(failure_log):
    analyzer = FakeAnalyzer(mode=FakeMode.ERROR)
    response = make_client(analyzer).post("/analyses", json={"log": failure_log})

    assert_error(response, 502, "ANALYZER_ERROR", log=failure_log)
    assert analyzer.call_count == 1


def test_analyzer_timeout_returns_504(failure_log):
    analyzer = FakeAnalyzer(mode=FakeMode.HANG)
    response = make_client(analyzer, timeout_seconds=0.05).post("/analyses", json={"log": failure_log})

    assert_error(response, 504, "ANALYZER_TIMEOUT", log=failure_log)
    assert analyzer.call_count == 1
    assert analyzer.cancelled is True


class BrokenAnalyzer:
    """분석기 경계 밖의 예상하지 못한 예외를 재현한다."""

    async def analyze(self, log: str) -> AnalyzerResult:
        raise RuntimeError("/internal/path/secret.py exploded")


def test_unexpected_error_returns_500_without_details(failure_log):
    response = make_client(BrokenAnalyzer()).post("/analyses", json={"log": failure_log})

    assert_error(response, 500, "INTERNAL_ERROR", log=failure_log)
    assert "exploded" not in response.text
    assert "RuntimeError" not in response.text


def test_error_codes_are_distinct(failure_log):
    codes = {
        make_client(FakeAnalyzer()).post("/analyses", json={}).json()["error"]["code"],
        make_client(FakeAnalyzer()).post("/analyses", json={"log": "a" * (MAX_LOG_CHARS + 1)}).json()["error"]["code"],
        make_client(FakeAnalyzer(mode=FakeMode.ERROR)).post("/analyses", json={"log": failure_log}).json()["error"]["code"],
        make_client(FakeAnalyzer(mode=FakeMode.HANG), 0.05).post("/analyses", json={"log": failure_log}).json()["error"]["code"],
    }
    assert codes == {"INVALID_INPUT", "LOG_TOO_LARGE", "ANALYZER_ERROR", "ANALYZER_TIMEOUT"}


@pytest.mark.parametrize("raw", ["0", "-1", "abc", "nan", "inf"])
def test_invalid_timeout_env_fails_app_creation(raw, monkeypatch):
    monkeypatch.setenv(TIMEOUT_ENV, raw)
    with pytest.raises(ValueError):
        create_app()


def test_timeout_env_is_applied(monkeypatch):
    monkeypatch.setenv(TIMEOUT_ENV, "2.5")
    assert Settings.from_env().timeout_seconds == 2.5
