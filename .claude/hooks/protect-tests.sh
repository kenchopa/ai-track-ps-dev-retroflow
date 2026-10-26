#!/usr/bin/env bash
# Claude Code PreToolUse hook (TEAM-KIT.md): blocks Edit/Write on a test file that already exists in git.
# New test files are fine. Exit code 2 = the edit is refused and Claude sees the message below.
set -uo pipefail

file=$(python3 -I -c 'import json,sys; print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))' 2>/dev/null)
[ -z "$file" ] && exit 0
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
relative=$(python3 -I -c 'import os,sys; print(os.path.relpath(os.path.realpath(sys.argv[1]), os.path.realpath(".")))' "$file")

case "$relative" in
  src/test/*|mcp-decisions/test/*)
    if git cat-file -e "HEAD:$relative" 2>/dev/null; then
      echo "Blocked by the team guardrail: $relative is an existing test. Existing tests are never changed to make a fix pass. Write a new test, or stop and ask the user if this test is really wrong." >&2
      exit 2
    fi
    ;;
esac
exit 0
