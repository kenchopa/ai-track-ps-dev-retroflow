---
name: stackcraft-audit
description: Audit a Java/Spring Boot codebase against StackCraft's coding standards and write the findings with file:line, the quoted section, severity and the correct code. Use when asked to audit, check compliance, or find violations of StackCraft (or "our") coding standards, or before a fix round that must be based on those standards.
---
# StackCraft audit

**Source of truth:** read the standards through the `ai-track-standards` MCP server with
`read_document("stackcraft-coding")` before anything else. Never audit from memory: a rule that
isn't in that document is not a finding. Note the version you read. Server unreachable → stop and say so.

## Steps
1. Read the whole document. Then read `decisions.md` and `CLAUDE.md`: our own rules and agreed
   exceptions (e.g. grandfathered packages, vendor tests that may never be edited).
2. Run `bash .claude/skills/stackcraft-audit/checks.sh` for the mechanical part (class length,
   `@Autowired` fields, catch blocks, query strings). Open every hit before you report it: the script
   has false positives (e.g. a `{` inside a string literal).
3. Go through the document **section by section**, in order. For each section: finding(s), or "clean"
   plus how you checked it.
4. Open the file at the line before you write a finding. No line, no finding.

## Each finding has
- `file:line` and the offending code
- the exact quote of the section and its number, plus the severity from its "Violation severity" line
  (if the heading and that line disagree, use the line and mention it)
- why it matters, using the document's own reason when it gives one
- the correct implementation, as code
- a status: open, fixed, or ⚖️ needs a team decision

## Output
Write `audit/audit-findings.md` (if it exists: keep its IDs, refresh line numbers, mark new items
NEW, and write a dated copy instead of overwriting it): a summary table, then findings sorted Critical → Major → Minor,
then "Checked and clean", "Open questions" (where the standard is ambiguous), and a table of the
quotes you checked word for word against the document.

## Never
- Change code during the audit.
- "Fix" an ambiguity yourself: when a rule is unclear, or it conflicts with `decisions.md` or a vendor
  file, flag it with both readings.
- Report generic best practice (Lombok, Swagger, …) as a StackCraft violation.
