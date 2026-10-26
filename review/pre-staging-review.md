# RetroFlow: pre-staging review

**Reviewed:** branch `block3-review` at `dbbe1e2` (Block 2 merged plus the Block 3 setup), on 2026-10-26.
**Reviewer:** Pair BC, with Claude Code as the agent.
**Sources of truth:** `retroflow-brief.md`, and StackCraft's *Coding Standards v3.1* (`stackcraft-coding`)
and *QA & Test Standards v2.0* (`stackcraft-qa`), both read through the `ai-track-standards` MCP server.
**Sign-off: CONDITIONAL**

## Summary

RetroFlow does what the brief asks for. We started the real application and checked every business
rule over HTTP, as separate operations, and all of them behave correctly: one open retro per team, no
reopen, no add/modify/delete in a closed retro, and no uncomplete. The build is green: 89 tests, coverage
above the gate. Two things have to be fixed before staging. The documented way to run the app
(`docker compose up`) exposes the H2 database console without a password, which lets anyone on the
network bypass every rule. And no automated test checks the rules through the HTTP API against the real
application, which StackCraft QA §1.1 requires.

**In plain language (QA §6.2):** the rules are right today, but anyone who can reach the server can edit
the database directly, and nothing would warn us if the API stopped enforcing a rule. Fix those two and
it can go to staging.

## Critical issues: must fix before staging

| # | Severity | Finding | Proof | Fix |
|---|---|---|---|---|
| C1 | High (security) | **The H2 console is open to the network, with an empty password.** `docker compose up` runs the app with `SPRING_H2_CONSOLE_SETTINGS_WEB_ALLOW_OTHERS: "true"`, and the console uses `sa` with no password. Anyone who reaches port 8080 gets full read and write access to the database. That includes `UPDATE retrospective SET status='OPEN'`, which reopens a closed retro: a business rule bypassed without the API knowing. | `docker-compose.yml:12` (`app`) and `:33` (`dev`): `SPRING_H2_CONSOLE_SETTINGS_WEB_ALLOW_OTHERS: "true"`. `application.properties:6-7,13` (`username=sa`, `password=` empty, `spring.h2.console.enabled=true`). **Reproduced:** the image run with that variable, `curl http://localhost:18081/h2-console/` → `<title>H2 Console</title>` and the `login.jsp` redirect. Without the variable it answers `Sorry, remote connections ('webAllowOthers') are disabled`. We did not log in. | Turn the console off by default (`spring.h2.console.enabled=false`) and only enable it in a local `dev` profile. Remove `WEB_ALLOW_OTHERS` from the `app` service. If a console is still needed, set a datasource password from an environment variable (coding standard §5.1). |
| C2 | High (test quality; **Critical** as a QA §1.1 standards violation) | **No business rule is tested against a running instance through the API.** QA §1.1: "Every business rule … must have at least one executable test that verifies it against a running instance of the application — not a unit test on an isolated method in a vacuum." The service tests call the services directly. The 42 contract tests run `@WebMvcTest` with **every service mocked**, so the path request → real service → database → 409 is never tested automatically. Only our manual curl run covered it. | `ControllerContractTest.java:24-34` (four `@MockBean` services). `grep -rn "RANDOM_PORT\|TestRestTemplate\|AutoConfigureMockMvc\|WebTestClient" src/test` → 0 hits. | Add one `@SpringBootTest(webEnvironment = RANDOM_PORT)` class with an HTTP test per brief rule: a second OPEN retro → 409, close twice → 409, add/modify/delete in a closed retro → 409 (three tests), uncomplete a completed item → 409. Assert the status **and** the `error` message (QA §2.2). |

## Other issues

### Should fix

| # | Severity | Finding | Proof | Fix |
|---|---|---|---|---|
| S1 | Medium (business rules) | **Race condition: adding a feedback item while the retro is being closed.** `requireOpenForNewItem` reads the status without a lock, and `closeRetrospective` doesn't lock either. Two concurrent requests can save an item into a retro that has just been closed. Creating a retro *does* lock the team row (decision R8), but closing doesn't. | `ServiceValidation.java:54-58` (no lock). `RetrospectiveService.java:108-114` (`lookup.retrospective`, not a locking lookup). **Not reproduced:** the window is milliseconds, and a single curl run can't hit it. Based on reading the code only. | Load the retrospective with `PESSIMISTIC_WRITE` (or add an `@Version` column) in both `closeRetrospective` and the add/modify/delete paths. |
| S2 | Medium (business rules / data) | **The author is stored as typed, not as the member's name.** Membership is checked ignoring case (F1), but the stored value is the client's spelling. That splits one person into "Alice" and "alice" in reports. | `POST /api/retrospectives/1/feedback-items {"author":"alice"}` on a team with member `Alice` → `201 {…"author":"alice"}`. `ServiceValidation.java:46-51` returns `name`, not the matching member. | Return the team's own spelling from `requireMember`. Add a matrix row and a test. The team still has to decide this (it's an open point from Block 2). |
| S3 | Low (test quality) | **The vendor tests accept any `RetroflowException` subclass.** `isInstanceOf(RetroflowException.class)` also passes on `InvalidInputException` (400) or `NotFoundException` (404), so a vendor test can pass for the wrong reason, for example an author rejected instead of a closed retro. | `RetroflowBusinessRulesTest.java:48,60,76`. `InvalidInputException extends RetroflowException` (`InvalidInputException.java:7`). | The vendor test must stay as it is. Our own matrix tests already close the gap with `isExactlyInstanceOf` (32 assertions). Note this in the README so nobody uses the vendor tests as proof on their own. |
| S4 | Low (test quality) | **The vendor tests commit fixed team names to the shared H2 database.** "Team Alpha/Beta/Gamma" aren't rolled back. Running the class twice in one context (with surefire's `rerunFailingTestsCount`, say) turns the second run into a 409 on the duplicate name: order-dependent behaviour (QA §3.1/§3.2). | `RetroflowBusinessRulesTest.java:27` (`@SpringBootTest`, no `@Transactional`), `:39` (`"Team Alpha"`). Today the team's own tests avoid it with random names (Block 2 log). | Never enable test reruns for this class. Keep using random names in our own fixtures. |

### Fix when time allows

| # | Severity | Finding | Proof | Fix |
|---|---|---|---|---|
| L1 | Low (API design) | **A bad date gives a vague 400.** The brief asks for "a `400` with a meaningful error message". The response doesn't say which field is wrong. | `POST /api/teams/1/retrospectives {"title":"t","date":"not-a-date"}` → `400 {"error":"Request -: malformed JSON or invalid field value"}` | In `handleUnreadableBody`, name the field from `InvalidFormatException.getPath()`, e.g. `Retrospective new: date must be an ISO date (2026-10-26)`. |
| L2 | Low (API design) | **Action items can only be deleted through `/feedback-items/{id}`,** while they're updated through `/action-items/{id}`. The design works but isn't consistent. | `FeedbackItemController.java:56` is the only `@DeleteMapping`. `DELETE /api/feedback-items/2` on an action item → `409 "ActionItem 2: cannot delete …"`, so it works. | Add `DELETE /api/action-items/{id}` (delegating), or document the choice in `decisions.md`. |
| L3 | Low (maintainability) | **Out-of-date Javadoc on `RetroflowException`.** It still says "nothing yet throws it with a real, specific message". | `RetroflowException.java:9-14` | Replace it with one line: business rule violation, mapped to 409. |
| L4 | Low (API design) | **Errors that don't belong to an entity use `Request -:`.** §2.1 asks for `[EntityType] [identifier]`. `Request -` is our own convention (G3), not the standard's. | `PUT /api/retrospectives/1/reopen` → `404 {"error":"Request -: Not Found"}` | Fine for now. Confirm with whoever owns the monitoring parser that it accepts `Request -`. |

## What's done well

1. **Every business rule holds over HTTP, checked one operation at a time against the running app:**
   - Second OPEN retro → `409 Team 1: already has an open retrospective (1)`.
   - Close twice → `409 Retrospective 1: already closed`.
   - `/reopen` → 404.
   - Closed retro: add feedback → 409, add action item → 409, modify feedback → 409, delete feedback → 409, modify action item → 409 (through both routes), delete action item → 409.
   - Complete twice → 409, uncomplete a completed item → `409 ActionItem 2: already completed, cannot be undone`.
   - Completing after close still works (decision F6).
2. **One error format everywhere, with no internal details** (coding §2.2, §4.2): unknown id, bad type,
   malformed JSON, 405, and a duplicate name all return `{error, status, timestamp}`. No class names or
   database messages reach the client, and a `DataIntegrityViolationException` becomes a clean 409.
3. **Status codes follow coding §4.1:** 201 + `Location` on create, 204 on delete, 400 for validation,
   404 for unknown ids, **409 for business rules**.
4. **Queries are parameterised** (coding §5.3). `ActionItemRepository.java:16-25` uses named parameters,
   and the other repositories use derived queries.
5. **A test suite with teeth:** 89/89 green on `mvn verify` (run in Docker), the JaCoCo gate met, PIT at
   100% in Block 2, 4 ArchUnit rules, and exact exception and message assertions in the matrix tests.

## False positives we threw out

- **"SQL/JPQL injection in `ActionItemRepository.findForTeam`."** It's a text block with
  `:teamId`/`:priority`/`:completed` named parameters and `@Param` (`ActionItemRepository.java:16-25`).
  Nothing is concatenated. A classic false positive: a reviewer sees `@Query` with a long string and
  assumes the worst.
- **"Missing authentication / anyone can close any retro."** Tempting, because the brief says "any team
  member can close", but it also says *"No authentication or security layer required"*. That's out of
  scope (decision G6), so it isn't listed. C1 is different: it isn't about API users, it's an open
  database console that goes around the API completely.
- **"Tautological `isNotNull()` in `ServiceHappyPathTest:65`."** It isn't on its own: the next three lines
  assert the content, type and author with `isEqualTo`. A null id would also really mean "not saved".
  Not a QA §2.1 violation.

## Scope: what we did NOT check (QA §6.1)

- Concurrency (S1) only by reading the code, not with a load test.
- The CI workflow and the Docker image beyond the H2 console. No dependency or CVE scan.
- Performance and large datasets: lists have no pagination, which is fine for 12 teams but untested beyond that.
- No authentication or authorization review (out of scope according to the brief).

## How we checked

- The app ran from the `runtime` image on port 18080. Every request and its response is in the
  BC section of `prompt-log.md`.
- `mvn -B verify` in `maven:3.9-eclipse-temurin-21`: `Tests run: 89, Failures: 0, Errors: 0`,
  `All coverage checks have been met`, `BUILD SUCCESS`.
- We reproduced two findings ourselves: C1 (curl on the console) and S2 (curl with `alice`). C2 is
  backed by the grep and the `@MockBean` lines.
