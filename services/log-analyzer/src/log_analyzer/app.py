"""앱 팩토리와 로컬 실행용 앱."""

from fastapi import FastAPI

from log_analyzer.analyzer import Analyzer
from log_analyzer.api import router
from log_analyzer.config import Settings
from log_analyzer.errors import register_error_handlers
from log_analyzer.fake_analyzer import FakeAnalyzer
from log_analyzer.service import AnalysisService


def create_app(analyzer: Analyzer | None = None, settings: Settings | None = None) -> FastAPI:
    """분석기와 설정을 주입해 앱을 만든다. 생략하면 기본 가짜 분석기와 환경 변수 설정을 쓴다."""
    resolved_settings = settings if settings is not None else Settings.from_env()
    resolved_analyzer = analyzer if analyzer is not None else FakeAnalyzer()
    app = FastAPI(title="log-analyzer", version="0.1.0")
    app.state.analysis_service = AnalysisService(resolved_analyzer, resolved_settings.timeout_seconds)
    register_error_handlers(app)
    app.include_router(router)
    return app


app = create_app()
