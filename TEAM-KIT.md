# RetroFlow team kit

Clone the repo, run one script, and your AI tool behaves like the rest of the team's.

## Get started (under 5 minutes)

```bash
git clone <this repo> && cd ai-track-ps-dev-retroflow
./scripts/setup-team-kit.sh      # git hooks, local MCP server, checks Java/Docker and the standards server
claude                           # or open the folder in VS Code with Copilot (agent mode)
```

## What's in here

| What | Where | Tool |
|---|---|---|
| House rules, read before every request | `.github/copilot-instructions.md` (`CLAUDE.md` imports it) | both |
| MCP servers: StackCraft standards (hosted) and `check_standards` + `decisions.md` (local) | `.mcp.json`, `.vscode/mcp.json` | Claude Code / VS Code |
| Skills: `stackcraft-audit`, `stackcraft-tests` | `.claude/skills/` | Claude Code |
| Agents: `reviewer` (reads and runs, **can't edit**) and `implementer` (edits, runs the build) | `.claude/agents/` | Claude Code |
| **Guardrail:** existing tests can't be changed | `.githooks/pre-commit` (any tool) and `.claude/hooks/protect-tests.sh` (blocks the edit itself) | all / Claude Code |
| Shared settings: MCP servers enabled, edit hook on | `.claude/settings.json` | Claude Code |

## How to use it

- **Ask for an audit:** the `stackcraft-audit` skill loads by itself. It reads the standards
  through MCP and runs `check_standards`.
- **Implement, then review:** "use the implementer to add X", then "use the reviewer on that
  diff". The reviewer has no edit tools, so it can't "fix" what it should report.
- **New tests are always fine.** Changing an existing test is blocked twice: by the Claude Code
  hook when the edit happens, and by git at commit time. If a test is really wrong, agree on that
  with the team and commit with `ALLOW_TEST_CHANGES="<reason>" git commit`.
- **AI summary work:** set `OPENAI_API_KEY` in your shell, never in a file. `./mvnw test -Peval`
  costs money (see `eval/README.md`).

## When it doesn't work

| Symptom | Fix |
|---|---|
| Claude asks to approve MCP servers, or they're missing | `.claude/settings.json` enables both. Check `claude mcp list`, and run `npm ci` in `mcp-decisions/` |
| `retroflow-decisions` fails to start | Node 20+ is needed. Rerun `./scripts/setup-team-kit.sh` |
| `ai-track-standards` unreachable | Network or VPN. `check_standards` still runs, with severity `unknown` |
| Commit refused: "changes or deletes existing tests" | That's the guardrail. Add a new test, or see above |
| Skill doesn't load | Name the task the way its description does ("audit…", "write tests for…") |
| No JDK | Use Docker: `docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9-eclipse-temurin-21 mvn -B verify` |

**Known limits:**
- `git commit --no-verify` skips the git hook, and the Claude hook doesn't see edits made with
  `sed` in Bash. Both guardrails are a seatbelt, not a vault. Code review is the last line.
- Skills and agents are Claude Code only. Copilot users get the house rules and the MCP servers.
- **Who keeps this up to date:** whoever changes a standard or a decision updates the kit in the same
  PR. Questions go to the team lead.
