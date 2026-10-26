# RetroFlow team kit

Clone the repo, run one script, and your AI tool behaves like the rest of the team's.

## Get started (under 5 minutes)

```bash
git clone <this repo> && cd ai-track-ps-dev-retroflow
./scripts/setup-team-kit.sh      # git hooks, local MCP server, checks Java/Docker and the standards server
claude                           # or open the folder in VS Code with Copilot (agent mode)
```

On the first `claude` start, accept the **trust dialog** for this folder once. Until you do,
Claude Code keeps the project's MCP servers at "Pending approval": a cloned repo can't approve the
commands it runs on your machine. That's on purpose, and it's the only click.

## What's in here

| What | Where | Tool |
|---|---|---|
| House rules, read before every request | `.github/copilot-instructions.md` (`CLAUDE.md` imports it) | both |
| MCP servers: StackCraft standards (hosted) and `check_standards` + `decisions.md` (local) | `.mcp.json`, `.vscode/mcp.json` | Claude Code / VS Code |
| Skills: `stackcraft-audit`, `stackcraft-tests` | `.claude/skills/` | Claude Code |
| Agents: `reviewer` (reads and runs, **can't edit**) and `implementer` (edits, runs the build) | `.claude/agents/` | Claude Code |
| **Guardrail:** existing tests (`src/test`, `mcp-decisions/test`) can't be changed | `.githooks/pre-commit` (any tool), `.claude/hooks/protect-tests.sh` (blocks the edit itself), and the `test-guardrail` CI check on every PR | all / Claude Code / GitHub |
| Shared settings: MCP servers enabled, edit hook on | `.claude/settings.json` | Claude Code |
| Design system for the web UI (tokens, rules, blueprint) | `DESIGN.md`, `designs/` | all |

## How to use it

- **Ask for an audit:** the `stackcraft-audit` skill loads by itself. It reads the standards
  through MCP and runs `check_standards`.
- **Implement, then review:** "use the implementer to add X", then "use the reviewer on that
  diff". The reviewer has no edit tools, so it can't "fix" what it should report.
- **New tests are always fine.** Changing an existing test is blocked twice: by the Claude Code
  hook when the edit happens, and by git at commit time. If a test is really wrong, agree on that
  with the team, commit with `ALLOW_TEST_CHANGES="<reason>" git commit`, and add a line
  `ALLOW_TEST_CHANGES: <reason>` to the PR description.
- **AI summary work:** set `OPENAI_API_KEY` in your shell, never in a file. `./mvnw test -Peval`
  costs money (see `eval/README.md`).

## When it doesn't work

| Symptom | Fix |
|---|---|
| MCP servers "Pending approval" | Start `claude` in the repo and accept the trust dialog. Check with `claude mcp list` |
| `retroflow-decisions` fails to start | Node 20+ is needed. Rerun `./scripts/setup-team-kit.sh` |
| `ai-track-standards` unreachable | Network or VPN. `check_standards` still runs, with severity `unknown` |
| Commit refused: "changes or deletes existing tests" | That's the guardrail. Add a new test, or see above |
| Skill doesn't load | Name the task the way its description does ("audit…", "write tests for…") |
| No JDK | Use Docker: `docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9-eclipse-temurin-21 mvn -B verify` |

**Known limits:**
- `git commit --no-verify` skips the git hook, and the Claude hook doesn't see edits made with
  `sed` in Bash. The CI check catches both at PR time, but only for PRs: a direct push to a branch
  without branch protection still gets through. Turn on branch protection with `test-guardrail`
  as a required check.
- Skills and agents are Claude Code only. Copilot users get the house rules and the MCP servers.
- **Who keeps this up to date:** whoever changes a standard or a decision updates the kit in the same
  PR. Questions go to the team lead.
