import anthropic
import os
import re
import sys


def read_file(path: str) -> str:
    try:
        with open(path, "r", encoding="utf-8") as f:
            return f.read()
    except FileNotFoundError:
        print(f"[WARN] File not found: {path}")
        return ""


def extract_issue_number_from_pr_body(pr_body: str) -> str | None:
    match = re.search(r"TASK-(\d+)", pr_body or "")
    return match.group(1).zfill(3) if match else None


def main():
    pr_number = os.environ["PR_NUMBER"]
    pr_title = os.environ["PR_TITLE"]
    pr_body = os.environ.get("PR_BODY") or ""

    task_id = extract_issue_number_from_pr_body(pr_body)
    if not task_id:
        print("[WARN] Could not extract TASK ID from PR body. Proceeding without task context.")

    pr_diff = read_file("/tmp/pr_diff.txt")
    task_content = read_file(f"docs/tasks/TASK-{task_id}.md") if task_id else ""
    design_content = read_file(f"docs/design/DESIGN-{task_id}.md") if task_id else ""

    claude_md = read_file("CLAUDE.md")
    reviewer_md = read_file(".claude/agents/reviewer.md")
    review_validator = read_file(".claude/validators/review-validator.md")

    system_prompt = f"""
{claude_md}

---

{reviewer_md}

---

## Review Validation Rules

Your review must satisfy:

{review_validator}

## Output Format (STRICT)

You must output a review report in EXACTLY this format:

## Review Result
APPROVED | REJECTED

## Summary
[2-5 sentences]

## Issues Found

### Critical
- [file:line] [description] [impact]

### Major
- [file:line] [description] [impact]

### Minor
- [file:line] [description] [suggestion]

## Architecture Compliance
PASS | FAIL
[explanation]

## Test Coverage
PASS | FAIL
[explanation]

## Acceptance Criteria
[List each criterion from TASK and mark MET or NOT MET]

Rules:
- Do NOT write or rewrite any code
- If APPROVED: no Critical or Major issues may exist
- If REJECTED: at least one Critical or Major issue must be documented
""".strip()

    task_section = f"\n\nTASK Document:\n{task_content}" if task_content else ""
    design_section = f"\n\nDESIGN Document:\n{design_content}" if design_content else ""

    user_content = f"""
Review Pull Request #{pr_number}: {pr_title}
{task_section}
{design_section}

PR Diff:
{pr_diff[:12000]}
""".strip()

    client = anthropic.Anthropic(api_key=os.environ["ANTHROPIC_API_KEY"])

    print(f"[Reviewer] Calling API for PR #{pr_number}...")
    message = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=4096,
        system=system_prompt,
        messages=[{"role": "user", "content": user_content}],
    )

    review_result = message.content[0].text

    with open("/tmp/review_result.md", "w", encoding="utf-8") as f:
        f.write(f"## Reviewer Agent Report\n\n")
        f.write(review_result)

    print(f"[Reviewer] Review written to /tmp/review_result.md")

    result_line = next(
        (line for line in review_result.splitlines() if "APPROVED" in line or "REJECTED" in line), ""
    )
    print(f"[Reviewer] Result: {result_line.strip()}")
    print(f"[Reviewer] Input tokens: {message.usage.input_tokens}, Output tokens: {message.usage.output_tokens}")


if __name__ == "__main__":
    main()
