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


def parse_files(output: str) -> dict[str, str]:
    """
    Parses AI output for file blocks in the format:

    --- FILE: path/to/File.java ---
    content
    --- END FILE ---
    """
    pattern = r"--- FILE: (.+?) ---\n(.*?)--- END FILE ---"
    matches = re.findall(pattern, output, re.DOTALL)
    return {path.strip(): content.strip() for path, content in matches}


def main():
    issue_number = os.environ["ISSUE_NUMBER"]
    issue_title = os.environ["ISSUE_TITLE"]
    task_id = str(issue_number).zfill(3)

    task_path = f"docs/tasks/TASK-{task_id}.md"
    design_path = f"docs/design/DESIGN-{task_id}.md"

    task_content = read_file(task_path)
    design_content = read_file(design_path)

    if not task_content:
        print(f"[ERROR] GATE 1 FAILED: {task_path} not found.")
        sys.exit(1)
    if not design_content:
        print(f"[ERROR] GATE 2 FAILED: {design_path} not found. Implementer cannot start without DESIGN document.")
        sys.exit(1)

    claude_md = read_file("CLAUDE.md")
    implementer_md = read_file(".claude/agents/implementer.md")
    code_validator = read_file(".claude/validators/code-validator.md")

    system_prompt = f"""
{claude_md}

---

{implementer_md}

---

## Code Validation Rules

Your output will be validated against:

{code_validator}

## Output Format (STRICT)

You must output source files using EXACTLY this format:

--- FILE: src/main/java/com/arok2/dev_agent/path/to/ClassName.java ---
// Java source code here
--- END FILE ---

--- FILE: src/test/java/com/arok2/dev_agent/path/to/ClassNameTest.java ---
// Test code here
--- END FILE ---

Rules:
- Every business logic class MUST have a corresponding test file
- Use constructor injection only (no @Autowired field injection)
- No business logic in Controller layer
- Follow the package structure defined in DESIGN
- Output ONLY file blocks. No explanation text outside file blocks.
""".strip()

    user_content = f"""
Implement the following task.

Issue #{issue_number}: {issue_title}

TASK:
{task_content}

DESIGN:
{design_content}

Generate all required source files and test files.
""".strip()

    client = anthropic.Anthropic(api_key=os.environ["ANTHROPIC_API_KEY"])

    print(f"[Implementer] Calling API for TASK-{task_id}...")
    message = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=16000,
        system=system_prompt,
        messages=[{"role": "user", "content": user_content}],
    )

    raw_output = message.content[0].text
    files = parse_files(raw_output)

    if not files:
        print("[ERROR] No file blocks found in Implementer output. Check output format.")
        print(raw_output[:500])
        sys.exit(1)

    for file_path, content in files.items():
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"[Implementer] Written: {file_path}")

    # Update TASK status to IN_REVIEW
    task_updated = task_content.replace("## Status\nIN_PROGRESS", "## Status\nIN_REVIEW")
    if task_updated != task_content:
        with open(task_path, "w", encoding="utf-8") as f:
            f.write(task_updated)
        print("[Implementer] TASK status updated to IN_REVIEW")

    print(f"[Implementer] {len(files)} files generated.")
    print(f"[Implementer] Input tokens: {message.usage.input_tokens}, Output tokens: {message.usage.output_tokens}")


if __name__ == "__main__":
    main()
