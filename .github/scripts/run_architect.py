import anthropic
import os
import sys


def read_file(path: str) -> str:
    try:
        with open(path, "r", encoding="utf-8") as f:
            return f.read()
    except FileNotFoundError:
        print(f"[WARN] File not found: {path}")
        return ""


def main():
    issue_number = os.environ["ISSUE_NUMBER"]
    issue_title = os.environ["ISSUE_TITLE"]
    task_id = str(issue_number).zfill(3)
    task_path = f"docs/tasks/TASK-{task_id}.md"

    task_content = read_file(task_path)
    if not task_content:
        print(f"[ERROR] GATE 1 FAILED: {task_path} not found. Architect cannot start without TASK document.")
        sys.exit(1)

    claude_md = read_file("CLAUDE.md")
    architect_md = read_file(".claude/agents/architect.md")
    design_template = read_file(".claude/templates/DESIGN.md")
    design_validator = read_file(".claude/validators/design-validator.md")
    architecture_md = read_file("docs/architecture/architecture.md")

    if not architect_md:
        print("[ERROR] .claude/agents/architect.md not found. Aborting.")
        sys.exit(1)

    system_prompt = f"""
{claude_md}

---

{architect_md}

---

## Current System Architecture

{architecture_md}

---

## DESIGN Template

Use this exact template structure:

{design_template}

---

## Validation Rules

Your output will be validated against:

{design_validator}

## Critical Output Rules

- Output ONLY the markdown content of the DESIGN file
- Do NOT include any explanation or commentary outside the markdown
- The file header must be exactly: # DESIGN-{task_id}
- Do NOT write Java code — design only (structure, API spec, data model)
- API endpoints must include: method, path, request schema, response schema, error cases
- Data model must include: entity name, fields with types
""".strip()

    user_content = f"""
Design the implementation for the following TASK.

Issue #{issue_number}: {issue_title}

TASK Document:
{task_content}

Generate DESIGN-{task_id}.md for this task.
""".strip()

    client = anthropic.Anthropic(api_key=os.environ["ANTHROPIC_API_KEY"])

    print(f"[Architect] Calling API for TASK-{task_id}...")
    message = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=8096,
        system=system_prompt,
        messages=[{"role": "user", "content": user_content}],
    )

    design_content = message.content[0].text
    output_path = f"docs/design/DESIGN-{task_id}.md"

    os.makedirs("docs/design", exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(design_content)

    # Update TASK status to IN_PROGRESS
    task_updated = task_content.replace("## Status\nTODO", "## Status\nIN_PROGRESS")
    if task_updated != task_content:
        with open(task_path, "w", encoding="utf-8") as f:
            f.write(task_updated)
        print(f"[Architect] TASK status updated to IN_PROGRESS")

    print(f"[Architect] DESIGN written to {output_path}")
    print(f"[Architect] Input tokens: {message.usage.input_tokens}, Output tokens: {message.usage.output_tokens}")


if __name__ == "__main__":
    main()
