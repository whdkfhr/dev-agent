"""근거 검증기 (DESIGN-015 근거 검증 규칙)."""

from dataclasses import dataclass


@dataclass(frozen=True)
class EvidenceCheck:
    verified: bool
    value: str | None


def split_lines(log: str) -> list[str]:
    """`\\n`으로 나누고 각 줄 끝의 `\\r`을 제거한다. 줄 번호는 인덱스 + 1이다."""
    return [line.removesuffix("\r") for line in log.split("\n")]


def check_evidence(lines: list[str], line: int, text: str) -> EvidenceCheck:
    """근거가 입력 로그의 해당 줄에 있는지 판단한다.

    앞뒤 공백을 뺀 값(정규화 값)으로 비교하고, 검증되면 그 값을 돌려준다.
    안쪽 공백은 바꾸지 않는다.
    """
    normalized = text.strip()
    if not normalized:
        return EvidenceCheck(verified=False, value=None)
    if line < 1 or line > len(lines):
        return EvidenceCheck(verified=False, value=None)
    if normalized not in lines[line - 1]:
        return EvidenceCheck(verified=False, value=None)
    return EvidenceCheck(verified=True, value=normalized)
