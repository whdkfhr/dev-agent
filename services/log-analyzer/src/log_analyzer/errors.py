"""전역 오류 처리: 모든 오류를 공통 형식 {"error": {"code", "message"}}로 바꾼다."""

import logging
from types import TracebackType

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.types import ASGIApp, Message, Receive, Scope, Send

from log_analyzer.analyzer import AnalyzerError
from log_analyzer.schemas import ErrorBody, ErrorResponse
from log_analyzer.service import AnalysisTimeoutError

logger = logging.getLogger("log_analyzer")

INVALID_INPUT = "INVALID_INPUT"
LOG_TOO_LARGE = "LOG_TOO_LARGE"
ANALYZER_ERROR = "ANALYZER_ERROR"
ANALYZER_TIMEOUT = "ANALYZER_TIMEOUT"
INTERNAL_ERROR = "INTERNAL_ERROR"


class LogTooLargeError(Exception):
    """log가 크기 제한을 넘었다."""


def error_response(status_code: int, code: str, message: str) -> JSONResponse:
    body = ErrorResponse(error=ErrorBody(code=code, message=message))
    return JSONResponse(status_code=status_code, content=body.model_dump())


def _invalid_input_message(exc: RequestValidationError) -> str:
    """입력 값을 다시 노출하지 않도록 오류 종류만으로 메시지를 만든다."""
    for error in exc.errors():
        error_type = error.get("type", "")
        location = tuple(error.get("loc", ()))
        if error_type == "json_invalid":
            return "요청 본문이 올바른 JSON이 아닙니다."
        if location == ("body",):
            return "요청 본문은 JSON 객체여야 합니다."
        if error_type == "missing":
            return "log 필드가 필요합니다."
        if error_type == "string_type":
            return "log는 문자열이어야 합니다."
        if error_type == "value_error":
            return "log가 비어 있습니다."
    return "요청 본문이 올바르지 않습니다."


async def _handle_validation(request: Request, exc: RequestValidationError) -> JSONResponse:
    logger.warning("analysis rejected code=%s", INVALID_INPUT)
    return error_response(422, INVALID_INPUT, _invalid_input_message(exc))


async def _handle_too_large(request: Request, exc: LogTooLargeError) -> JSONResponse:
    logger.warning("analysis rejected code=%s", LOG_TOO_LARGE)
    return error_response(413, LOG_TOO_LARGE, "log는 100,000자를 넘을 수 없습니다.")


async def _handle_analyzer_error(request: Request, exc: AnalyzerError) -> JSONResponse:
    logger.warning("analysis failed code=%s", ANALYZER_ERROR)
    return error_response(502, ANALYZER_ERROR, "분석기가 결과를 만들지 못했습니다.")


async def _handle_timeout(request: Request, exc: AnalysisTimeoutError) -> JSONResponse:
    logger.warning("analysis failed code=%s", ANALYZER_TIMEOUT)
    return error_response(504, ANALYZER_TIMEOUT, "분석이 제한 시간 안에 끝나지 않았습니다.")


def _failure_location(tb: TracebackType | None) -> str:
    """예외가 난 위치를 모듈 이름:함수:줄 번호로 돌려준다. 파일 경로는 쓰지 않는다.

    이 패키지 안의 가장 안쪽 프레임을 우선하고, 없으면 가장 안쪽 프레임을 쓴다.
    """
    innermost = None
    own = None
    while tb is not None:
        frame = tb.tb_frame
        location = f"{frame.f_globals.get('__name__', '?')}:{frame.f_code.co_name}:{tb.tb_lineno}"
        innermost = location
        if location.startswith("log_analyzer"):
            own = location
        tb = tb.tb_next
    return own or innermost or "unknown"


def log_unexpected_error(exc: BaseException) -> None:
    """예상하지 못한 예외를 안전한 정보(예외 종류, 발생 위치)만으로 남긴다.

    예외 메시지와 스택 트레이스는 입력 로그 원문, secret, 내부 경로를 담을 수 있으므로 남기지 않는다.
    """
    logger.error(
        "analysis failed code=%s exc_type=%s location=%s",
        INTERNAL_ERROR,
        type(exc).__qualname__,
        _failure_location(exc.__traceback__),
    )


class UnexpectedErrorMiddleware:
    """처리되지 않은 예외를 500 공통 형식으로 바꾸는 ASGI 미들웨어.

    Starlette의 기본 500 처리기는 응답 후 예외를 서버로 다시 던져 서버 로그에 예외 메시지와
    스택 트레이스가 남는다. 이 미들웨어는 예외를 안전하게 기록한 뒤 서버로 넘기지 않는다.
    """

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        response_started = False

        async def send_wrapper(message: Message) -> None:
            nonlocal response_started
            if message["type"] == "http.response.start":
                response_started = True
            await send(message)

        try:
            await self.app(scope, receive, send_wrapper)
        except Exception as exc:
            log_unexpected_error(exc)
            if response_started:
                # 응답을 이미 보내기 시작했으면 새 응답을 보낼 수 없다. 기록만 하고 연결 종료는 서버에 맡긴다.
                return
            response = error_response(500, INTERNAL_ERROR, "내부 오류가 발생했습니다.")
            await response(scope, receive, send)


def register_error_handlers(app: FastAPI) -> None:
    app.add_exception_handler(RequestValidationError, _handle_validation)
    app.add_exception_handler(LogTooLargeError, _handle_too_large)
    app.add_exception_handler(AnalyzerError, _handle_analyzer_error)
    app.add_exception_handler(AnalysisTimeoutError, _handle_timeout)
    app.add_middleware(UnexpectedErrorMiddleware)
