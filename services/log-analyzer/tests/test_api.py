import logging

import pytest
from fastapi.testclient import TestClient

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


# --- 리뷰 회귀 테스트 (dc7467a REJECTED) ---

SYNTHETIC_SECRET = "SYNTHETIC-SECRET-7f3a9c"
SYNTHETIC_PATH = "/srv/synthetic-internal/settings_secret.py"
SYNTHETIC_LOG_LINE = "SYNTHETIC-LOG-LINE-41d2 token=SYNTHETIC-TOKEN-0b8e"


class LeakyAnalyzer:
    """메시지에 합성 secret, 내부 경로, 입력 로그 원문을 담은 예상하지 못한 예외를 낸다."""

    async def analyze(self, log: str) -> AnalyzerResult:
        raise RuntimeError(f"{SYNTHETIC_SECRET} at {SYNTHETIC_PATH}: {log}")


def test_unexpected_error_does_not_leak_into_response_or_logs(caplog):
    caplog.set_level(logging.DEBUG)
    log = f"collected 1 item\n{SYNTHETIC_LOG_LINE}\nFAILED tests/test_x.py::test_y"
    app = create_app(analyzer=LeakyAnalyzer(), settings=Settings(timeout_seconds=1.0))

    # raise_server_exceptions=True: 예외가 앱 밖(서버)으로 나가면 여기서 다시 발생해 테스트가 실패한다.
    # 서버는 앱 밖으로 나온 예외만 서버 로그에 남기므로, 이것으로 서버 로그 비노출을 확인한다.
    response = TestClient(app, raise_server_exceptions=True).post("/analyses", json={"log": log})

    assert_error(response, 500, "INTERNAL_ERROR", log=log)
    forbidden = [SYNTHETIC_SECRET, SYNTHETIC_PATH, "synthetic-internal", SYNTHETIC_LOG_LINE, "SYNTHETIC-TOKEN", "Traceback"]
    for value in forbidden:
        assert value not in response.text

    records = [record for record in caplog.records if record.name == "log_analyzer"]
    assert len(records) == 1
    record = records[0]
    assert record.levelno == logging.ERROR
    assert record.exc_info is None
    assert record.exc_text is None
    message = record.getMessage()
    assert "code=INTERNAL_ERROR" in message
    assert "exc_type=RuntimeError" in message
    assert "location=log_analyzer.service:analyze:" in message
    # caplog.text에는 pytest 형식기가 붙인 소스 파일 이름이 들어가므로, 경로 검사는 앱이 남긴 메시지로 한다.
    for value in forbidden:
        assert value not in caplog.text
    assert all(r.exc_info is None for r in caplog.records)
    assert ".py" not in message
    assert "/" not in message


def test_unexpected_error_location_without_traceback():
    from log_analyzer.errors import _failure_location

    assert _failure_location(None) == "unknown"


def test_blank_summary_cause_is_not_in_success_response(failure_log):
    line = line_number(failure_log, ASSERTION_LINE)
    result = AnalyzerResult(
        causes=[
            Cause(summary="", evidence=[Evidence(line=line, text=ASSERTION_LINE)]),
            Cause(summary="   ", evidence=[Evidence(line=line, text=ASSERTION_LINE)]),
            Cause(summary="합계 불일치", evidence=[Evidence(line=line, text="assert 90 == 100")]),
        ],
        next_checks=["할인 계산 확인"],
    )
    analyzer = FakeAnalyzer(result=result)
    response = make_client(analyzer).post("/analyses", json={"log": failure_log})

    assert response.status_code == 200
    body = response.json()
    assert body["causes"] == [{"summary": "합계 불일치", "evidence": [{"line": line, "text": "assert 90 == 100"}]}]
    assert all(cause["summary"].strip() for cause in body["causes"])
    assert body["excluded_evidence_count"] == 0
    assert analyzer.call_count == 1


@pytest.mark.parametrize("summary", ["", "   ", "\t\n"])
def test_only_blank_summary_cause_returns_empty_causes(failure_log, summary):
    line = line_number(failure_log, ASSERTION_LINE)
    result = AnalyzerResult(causes=[Cause(summary=summary, evidence=[Evidence(line=line, text=ASSERTION_LINE)])])
    analyzer = FakeAnalyzer(result=result)
    response = make_client(analyzer).post("/analyses", json={"log": failure_log})

    assert response.status_code == 200
    assert response.json() == {"causes": [], "next_checks": [], "excluded_evidence_count": 0}
    assert analyzer.call_count == 1


@pytest.mark.parametrize(
    "log",
    [
        pytest.param(" " * (MAX_LOG_CHARS + 1), id="spaces"),
        pytest.param("\n" * (MAX_LOG_CHARS + 1), id="newlines"),
        pytest.param(" \t\r\n" * (MAX_LOG_CHARS // 4 + 1), id="mixed-whitespace"),
    ],
)
def test_blank_log_over_limit_returns_413_without_calling_analyzer(log):
    assert len(log) > MAX_LOG_CHARS
    analyzer = FakeAnalyzer()
    response = make_client(analyzer).post("/analyses", json={"log": log})

    assert_error(response, 413, "LOG_TOO_LARGE")
    assert analyzer.call_count == 0


@pytest.mark.parametrize(
    "log",
    [
        pytest.param(" " * MAX_LOG_CHARS, id="spaces-at-limit"),
        pytest.param("\n" * MAX_LOG_CHARS, id="newlines-at-limit"),
    ],
)
def test_blank_log_within_limit_still_returns_422_without_calling_analyzer(log):
    assert len(log) == MAX_LOG_CHARS
    analyzer = FakeAnalyzer()
    response = make_client(analyzer).post("/analyses", json={"log": log})

    assert_error(response, 422, "INVALID_INPUT")
    assert response.json()["error"]["message"] == "log가 비어 있습니다."
    assert analyzer.call_count == 0
