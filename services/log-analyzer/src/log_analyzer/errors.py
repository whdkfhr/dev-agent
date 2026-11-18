"""전역 오류 처리: 모든 오류를 공통 형식 {"error": {"code", "message"}}로 바꾼다."""

import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

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


async def _handle_unexpected(request: Request, exc: Exception) -> JSONResponse:
    logger.exception("analysis failed code=%s", INTERNAL_ERROR)
    return error_response(500, INTERNAL_ERROR, "내부 오류가 발생했습니다.")


def register_error_handlers(app: FastAPI) -> None:
    app.add_exception_handler(RequestValidationError, _handle_validation)
    app.add_exception_handler(LogTooLargeError, _handle_too_large)
    app.add_exception_handler(AnalyzerError, _handle_analyzer_error)
    app.add_exception_handler(AnalysisTimeoutError, _handle_timeout)
    app.add_exception_handler(Exception, _handle_unexpected)
