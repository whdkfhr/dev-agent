from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from log_analyzer.app import create_app
from log_analyzer.config import Settings
from log_analyzer.fake_analyzer import FakeAnalyzer

LOGS_DIR = Path(__file__).parent / "fixtures" / "logs"


def read_log(name: str) -> str:
    return (LOGS_DIR / name).read_text(encoding="utf-8")


def line_number(log: str, needle: str) -> int:
    """needle이 처음 나오는 줄 번호(1부터)를 돌려준다. 테스트 데이터의 줄 번호를 하드코딩하지 않기 위함."""
    for number, line in enumerate(log.split("\n"), start=1):
        if needle in line:
            return number
    raise AssertionError(f"{needle!r} not found in log")


def make_client(analyzer: FakeAnalyzer, timeout_seconds: float = 1.0) -> TestClient:
    app = create_app(analyzer=analyzer, settings=Settings(timeout_seconds=timeout_seconds))
    return TestClient(app, raise_server_exceptions=False)


@pytest.fixture
def failure_log() -> str:
    return read_log("pytest_failure.log")


@pytest.fixture
def pass_log() -> str:
    return read_log("pytest_pass.log")
