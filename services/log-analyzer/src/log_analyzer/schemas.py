"""요청, 응답, 오류, 분석기 결과 모델 (DESIGN-015 Data Model)."""

from pydantic import BaseModel, field_validator

MAX_LOG_CHARS = 100_000


class AnalysisRequest(BaseModel):
    """사람이 입력한 테스트 실행 로그. 크기 제한(413)은 API 계층에서 따로 검사한다."""

    log: str

    @field_validator("log")
    @classmethod
    def log_must_not_be_blank(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("log must not be blank")
        return value


class Evidence(BaseModel):
    line: int
    text: str


class Cause(BaseModel):
    summary: str
    evidence: list[Evidence]


class AnalyzerResult(BaseModel):
    """분석기가 돌려준 검증 전 결과."""

    causes: list[Cause] = []
    next_checks: list[str] = []


class AnalysisResponse(BaseModel):
    """검증된 근거만 담은 성공 응답."""

    causes: list[Cause]
    next_checks: list[str]
    excluded_evidence_count: int


class ErrorBody(BaseModel):
    code: str
    message: str


class ErrorResponse(BaseModel):
    error: ErrorBody
