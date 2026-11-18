"""전역 오류 처리: 모든 오류를 공통 형식 {"error": {"code", "message"}}로 바꾼다."""

import logging

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


class UnexpectedErrorMiddleware:
    """예상하지 못한 예외를 500 공통 형식으로 바꾼다.

    예외 메시지와 스택 트레이스에는 입력 로그 원문, secret, 내부 경로가 들어갈 수 있으므로
    로그에는 결과 코드와 예외 종류 이름만 남긴다 (DESIGN-015 보안).
    Starlette의 `Exception` 핸들러는 응답 후 예외를 다시 던져 서버(uvicorn)가 스택 트레이스를
    기록하므로 쓰지 않고, 이 미들웨어가 예외를 받아 응답을 끝낸다.
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
            logger.error(
                "analysis failed code=%s error_type=%s response_started=%s",
                INTERNAL_ERROR,
                type(exc).__name__,
                response_started,
            )
            if response_started:
                # 응답을 이미 보내기 시작해 500으로 바꿀 수 없다. 예외를 삼키지 않고 연결을 끊게 한다.
                # 다시 던진 예외의 스택 트레이스는 서버가 기록할 수 있다.
                raise
            response = error_response(500, INTERNAL_ERROR, "내부 오류가 발생했습니다.")
            await response(scope, receive, send)


def register_error_handlers(app: FastAPI) -> None:
    app.add_exception_handler(RequestValidationError, _handle_validation)
    app.add_exception_handler(LogTooLargeError, _handle_too_large)
    app.add_exception_handler(AnalyzerError, _handle_analyzer_error)
    app.add_exception_handler(AnalysisTimeoutError, _handle_timeout)
    app.add_middleware(UnexpectedErrorMiddleware)
