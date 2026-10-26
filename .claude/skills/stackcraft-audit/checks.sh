#!/usr/bin/env bash
# Mechanical StackCraft checks. Every hit is a lead, not a finding: open the line first.
set -u
src="${1:-src}"

echo "== §1.2 classes over 200 lines"
find "$src" -name '*.java' -exec wc -l {} + | awk '$2 != "total" && $1 > 200'

echo "== §1.4 @Autowired on a field (the next line is a field, not a constructor)"
grep -rn -A1 --include='*.java' '@Autowired' "$src" | grep -E '^\S+-[0-9]+-\s*(private|protected|public)?\s*[A-Z][A-Za-z0-9<>]*\s+\w+\s*;'

echo "== §2.3 catch blocks (check that each one rethrows, wraps or explains)"
grep -rn --include='*.java' 'catch (' "$src/main" 2>/dev/null

echo "== §5.3 string concatenation near a query"
grep -rn --include='*.java' -E '(@Query|createQuery|createNativeQuery)' "$src" | grep -E '"\s*\+|\+\s*"'

echo "== §5.1 secrets in config"
grep -rn -i -E '(password|secret|token|api[-_.]?key)\s*[=:]\s*\S' "$src/main/resources" 2>/dev/null
exit 0
