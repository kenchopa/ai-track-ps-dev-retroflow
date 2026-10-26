#!/usr/bin/env bash
# One-time setup after cloning (TEAM-KIT.md). Safe to run again.
set -uo pipefail
cd "$(git rev-parse --show-toplevel)" || exit 1
ok=0

step() { printf '%-48s' "$1"; }
pass() { echo "ok"; }
fail() { echo "MISSING: $1"; ok=1; }

step "1. git hooks (guardrail on existing tests)"
git config core.hooksPath .githooks && chmod +x .githooks/* .claude/hooks/*.sh && pass

step "2. local MCP server (mcp-decisions)"
if command -v node >/dev/null && command -v npm >/dev/null; then
  (cd mcp-decisions && npm ci --silent >/dev/null 2>&1) && pass || fail "npm ci failed in mcp-decisions/"
else
  fail "Node 20+ (https://nodejs.org)"
fi

step "3. Java 21 or Docker (to build and test)"
if command -v java >/dev/null && java -version 2>&1 | grep -q '"21'; then pass
elif docker info >/dev/null 2>&1; then echo "ok (Docker; see README for the mvn command)"
else fail "a JDK 21 or a running Docker"; fi

step "4. OPENAI_API_KEY (only for the summary + eval)"
[ -n "${OPENAI_API_KEY:-}" ] && pass || echo "not set (fine unless you work on the AI summary)"

step "5. hosted standards server reachable"
curl -s -o /dev/null -m 10 -X POST -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"ping"}' https://ai-track-ps-production.up.railway.app/mcp && pass || fail "network access to ai-track-ps-production.up.railway.app"

echo
[ $ok -eq 0 ] && echo "Team kit ready. Start 'claude' (or VS Code) in the repo root; see TEAM-KIT.md." || echo "Fix the MISSING lines above, then run this again."
exit $ok
