"""POST /analyses 라우팅. 근거 검증과 타임아웃은 분석 서비스가 맡는다."""

import logging
import time

from fastapi import APIRouter, Request

from log_analyzer.errors import LogTooLargeError
from log_analyzer.schemas import MAX_LOG_CHARS, AnalysisRequest, AnalysisResponse
from log_analyzer.service import AnalysisService

logger = logging.getLogger("log_analyzer")

router = APIRouter()


@router.post("/analyses", response_model=AnalysisResponse)
async def analyze(body: AnalysisRequest, request: Request) -> AnalysisResponse:
    if len(body.log) > MAX_LOG_CHARS:
        raise LogTooLargeError()
    service: AnalysisService = request.app.state.analysis_service
    started = time.monotonic()
    response = await service.analyze(body.log)
    logger.info(
        "analysis completed chars=%d causes=%d excluded=%d elapsed_ms=%d",
        len(body.log),
        len(response.causes),
        response.excluded_evidence_count,
        int((time.monotonic() - started) * 1000),
    )
    return response
