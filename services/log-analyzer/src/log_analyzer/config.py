"""설정: 분석기 타임아웃 (DESIGN-015)."""

import math
import os
from dataclasses import dataclass

TIMEOUT_ENV = "LOG_ANALYZER_TIMEOUT_SECONDS"
DEFAULT_TIMEOUT_SECONDS = 10.0


@dataclass(frozen=True)
class Settings:
    timeout_seconds: float = DEFAULT_TIMEOUT_SECONDS

    def __post_init__(self) -> None:
        if not math.isfinite(self.timeout_seconds) or self.timeout_seconds <= 0:
            raise ValueError(f"{TIMEOUT_ENV} must be a number greater than 0")

    @classmethod
    def from_env(cls) -> "Settings":
        raw = os.environ.get(TIMEOUT_ENV)
        if raw is None:
            return cls()
        try:
            value = float(raw)
        except ValueError as exc:
            raise ValueError(f"{TIMEOUT_ENV} must be a number greater than 0") from exc
        return cls(timeout_seconds=value)
