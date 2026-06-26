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
    issue_body = os.environ.get("ISSUE_BODY") or "(no description provided)"

    claude_md = read_file("CLAUDE.md")
    planner_md = read_file(".claude/agents/planner.md")
    task_template = read_file(".claude/templates/TASK.md")
    task_validator = read_file(".claude/validators/task-validator.md")

    if not planner_md:
        print("[ERROR] .claude/agents/planner.md not found. Aborting.")
        sys.exit(1)

    task_id = str(issue_number).zfill(3)

    system_prompt = f"""
{claude_md}

---

{planner_md}

---

## TASK Template

Use this exact template structure:

{task_template}

---

## Validation Rules

Your output will be validated against:

{task_validator}

## Critical Output Rules

- Output ONLY the markdown content of the TASK file
- Do NOT include any explanation or commentary outside the markdown
- The file header must be exactly: # TASK-{task_id}
- Status must be: TODO
- Do NOT include API design, class names, or implementation details
""".strip()

    user_content = f"""
GitHub Issue #{issue_number}: {issue_title}

{issue_body}

Generate TASK-{task_id}.md for this GitHub Issue.
""".strip()

    client = anthropic.Anthropic(api_key=os.environ["ANTHROPIC_API_KEY"])

    print(f"[Planner] Calling API for Issue #{issue_number}...")
    message = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=4096,
        system=system_prompt,
        messages=[{"role": "user", "content": user_content}],
    )

    task_content = message.content[0].text
    output_path = f"docs/tasks/TASK-{task_id}.md"

    os.makedirs("docs/tasks", exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(task_content)

    print(f"[Planner] TASK written to {output_path}")
    print(f"[Planner] Input tokens: {message.usage.input_tokens}, Output tokens: {message.usage.output_tokens}")


if __name__ == "__main__":
    main()
