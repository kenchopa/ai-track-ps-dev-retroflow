---
name: stackcraft-tests
description: Write or review Java/Spring Boot tests the StackCraft way (naming, Arrange-Act-Assert, exact exception and message asserts, one HTTP test per business rule against the running app). Use when asked to add tests for a fix, a business rule or an endpoint, to review test quality, or to check a test suite against StackCraft's QA standards.
---
# StackCraft tests

**Source of truth:** before writing anything, read both documents through the `ai-track-standards` MCP
server: `read_document("stackcraft-coding")` (§3 Testing) and `read_document("stackcraft-qa")`.
Never quote a rule from memory. Cite the section number you read. If the server is unreachable, stop and say so.

## Writing tests
1. Read what you test: the changed code, `test-matrix.md` (one row = one test) and `decisions.md`.
   If a row needs a decision that isn't written down, flag it and ask. Never pick the expected result yourself.
2. Name it `method_condition_expectedBehaviour` (coding §3.2). Use `// Arrange`, `// Act`, `// Assert`
   comments (§3.3) and named constants, no magic values (§3.4).
3. Assert behaviour, not just existence (QA §2.1, §2.2):
   - exceptions: `isExactlyInstanceOf(...)` plus the full `[EntityType] [id]: [reason]` message (coding §2.1)
   - HTTP: status **and** the `$.error` body; data: the changed field's new value
4. Every "cannot"/"must not" rule gets a test that tries the forbidden action (QA §2.3), and at least one
   test hits it through HTTP on the real app: `@SpringBootTest(webEnvironment = RANDOM_PORT)` (QA §1.1).
   Use `template/BusinessRuleHttpTest.java` for the layout.
5. Each test sets up its own data, with unique names, so order doesn't matter (QA §3.1, §3.2).
6. Run the tests yourself (`./mvnw test`, or Maven in Docker when there's no JDK) and report the real
   counts. Then prove a new test can fail: break the rule in the code, run it, show the failure, restore
   the code, and check that `git status` is clean (QA §5.1).

## Reviewing tests
Report per test file:line: tautological asserts (a lone `isNotNull()`), status-only HTTP checks,
`isInstanceOf` on an exception hierarchy, shared or order-dependent data, rules without a negative test.
Open each test before claiming it covers a rule (QA §5.2). Say what you did NOT check (QA §6.1).

## Never
- Change, skip or disable an existing test to make it pass. `RetroflowBusinessRulesTest` is never edited.
- Add a test dependency without first checking that the exact version exists on Maven Central.
- Report a test as covering a rule without having run it.
