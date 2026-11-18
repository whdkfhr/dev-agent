from log_analyzer.evidence import check_evidence, split_lines

LOG = "first line\nE       AssertionError: assert 90 == 100\nlast Line\r\nend"


def test_split_lines_removes_carriage_return_and_keeps_numbering():
    lines = split_lines(LOG)
    assert lines == ["first line", "E       AssertionError: assert 90 == 100", "last Line", "end"]


def test_exact_line_is_verified():
    result = check_evidence(split_lines(LOG), 1, "first line")
    assert result.verified is True
    assert result.value == "first line"


def test_substring_of_line_is_verified():
    result = check_evidence(split_lines(LOG), 2, "assert 90 == 100")
    assert result.verified is True
    assert result.value == "assert 90 == 100"


def test_substring_with_surrounding_spaces_returns_trimmed_value():
    result = check_evidence(split_lines(LOG), 2, "  assert 90 == 100 ")
    assert result.verified is True
    assert result.value == "assert 90 == 100"


def test_different_inner_spacing_is_not_verified():
    result = check_evidence(split_lines(LOG), 2, "E AssertionError: assert 90 == 100")
    assert result.verified is False
    assert result.value is None


def test_text_from_another_line_is_not_verified():
    result = check_evidence(split_lines(LOG), 1, "assert 90 == 100")
    assert result.verified is False


def test_line_out_of_range_is_not_verified():
    lines = split_lines(LOG)
    assert check_evidence(lines, 0, "first line").verified is False
    assert check_evidence(lines, len(lines) + 1, "end").verified is False


def test_empty_and_whitespace_only_text_is_not_verified():
    lines = split_lines(LOG)
    assert check_evidence(lines, 1, "").verified is False
    assert check_evidence(lines, 1, "   ").verified is False


def test_crlf_line_is_compared_without_carriage_return():
    result = check_evidence(split_lines(LOG), 3, "last Line")
    assert result.verified is True
    assert result.value == "last Line"


def test_comparison_is_case_sensitive():
    assert check_evidence(split_lines(LOG), 3, "last line").verified is False
