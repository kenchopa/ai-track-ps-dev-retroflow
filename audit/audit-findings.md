# RetroFlow audit against the StackCraft coding standards

> **Second pass:** `audit-findings-2026-10-26.md` is the same audit, rerun by a fresh session with
> the `stackcraft-audit` skill (swap, step 4). It keeps these IDs and adds 15 Minor items we
> missed, the `@MockBean` question under C2, and 7 open questions.

- **Source of truth:** `stackcraft-coding` v3.1 (March 2025), read through the `ai-track-standards`
  MCP server with `read_document`. Every quote below comes from that call. Section numbers were
  checked against the local copy `stackcraft-coding-standards.md` in the course repo.
- **Code audited:** branch `block3-audit`, starting from `dbbe1e2` (block 2 merged, 89/89 tests
  green, coverage gate met).
- **Severity:** the "Violation severity" line of each section. Findings are sorted Critical →
  Major → Minor.
- **Status:** ✅ fixed in step 3 · ⚖️ needs a team decision (flagged, not decided) · ➖ accepted /
  not fixed, with the reason given.

## Summary

| # | Severity | Section | Where | Status |
|---|---|---|---|---|
| C1 | Critical | §2.1 Exception message format | `entity/FeedbackItem.java:79` | ✅ |
| C2 | Critical | §1.4 Constructor injection only | `ControllerContractTest.java:21` (+ the vendor test) | ⚖️ |
| C3 | Critical (disputed) | §4.1 HTTP status codes | `service/ServiceValidation.java:49` | ⚖️ |
| M1 | Major | §5.2 Validate all input at the boundary | 3 request records + `ActionItemController.java:62` | ➖ |
| M2 | Major | §6.1 JavaDoc (business-rule method) | `service/RetrospectiveService.java:45-53` | ✅ |
| M3 | Major | §1.1 Package structure | every package | ➖ grandfathered |
| m1 | Minor | §6.1 JavaDoc on public service methods | 9 lookup methods | ➖ |
| m2 | Minor | §3.2 Test naming convention | 3 of our tests, 3 vendor tests | ➖ |
| m3 | Minor | §3.3 Arrange-Act-Assert with comments | 9 tests | ➖ |
| m4 | Minor | §3.4 No magic numbers in tests | 3 places | ➖ |

The fixes for the Minor findings and for M1 would mean editing existing tests or changing the
API contract. Step 3 only covers Critical findings, so these are listed for the backlog.

---

## Critical

### C1 · §2.1: an exception from the service layer that doesn't follow the message format ✅

- **Where:** `src/main/java/com/stackcraft/retroflow/entity/FeedbackItem.java:79`
  ```java
  throw new IllegalArgumentException("Use ActionItem for feedback of type ACTION_ITEM");
  ```
  The constructor is called from `FeedbackItemService.addFeedbackItem` (`FeedbackItemService.java:60`),
  so if it ever fires, the exception leaves the service layer.
- **Standard (§2.1, Critical):** *"All exceptions thrown by the service layer must follow this exact
  format: `[EntityType] [identifier]: [human-readable reason]`"*
- **Why it matters:** *"our observability platform parses exception messages for alerting and
  dashboards. Deviating from this format breaks our monitoring."* This guard catches a programming
  error, and a programming error is exactly the alert you want to see, but this message can't be
  parsed. Today it can't be reached through the API, because `FeedbackItemService.parseType`
  rejects `ACTION_ITEM` first. So the risk is low, but it's a letter-of-the-standard Critical.
- **Correct implementation:** keep the guard, and word it like every other message:
  ```java
  throw new IllegalArgumentException("FeedbackItem new: type ACTION_ITEM must be created as an ActionItem");
  ```
- **Fixed (step 3, fix 1):** what was wrong: a free-text message that the §2.1 parser can't read.
  Based on §2.1. `mvn test`: 89/89 green, no test changed. The new test that pins the message is
  written with the `stackcraft-tests` skill in the swap.

### C2 · §1.4: field injection in test classes ⚖️

- **Where:** `@Autowired` on a **field** in
  - `controller/ControllerContractTest.java:21-22`: `protected MockMvc mvc;`, the base class of all
    4 contract test classes
  - the vendor's `RetroflowBusinessRulesTest.java:30-34` (`teamService`, `retrospectiveService`)

  Both are under `src/test`. Production code is clean: `ArchitectureTest.noFieldInjection`
  enforces it there, with `DoNotIncludeTests`.
- **Correction (step 4):** the first version of this finding also listed `ActionItemRulesTest:36`,
  `RetrospectiveRulesTest:34`, `TeamRulesTest:34`, `ServiceHappyPathTest:35` and
  `FeedbackItemRulesTest:38`. Those are `@Autowired` **constructors**, which is constructor injection
  and compliant. The audit had trusted a bare `grep "@Autowired"` without opening the lines. The
  skill's `checks.sh` looks at the line after the annotation and caught the mistake.
- **Standard (§1.4, Critical):** *"Field injection (`@Autowired` on a field) is prohibited.
  Constructor injection must be used for all Spring-managed dependencies."*
- **Why it matters:** the standard gives the reason: *"Field injection makes testing impossible and
  hides dependencies."* That reason is about production beans, and these are the tests themselves.
  Read literally, the rule still applies. Our own service tests already show the compliant way.
- **Correct implementation:** constructor injection, as in `TeamRulesTest`:
  ```java
  abstract class ControllerContractTest {
      protected final MockMvc mvc;
      protected ControllerContractTest(MockMvc mvc) { this.mvc = mvc; }   // subclasses: @Autowired constructor
  }
  ```
- **Why not fixed in step 3:** two mandates conflict. Step 3 says *"existing tests stay
  untouched"*, and the house rules say `RetroflowBusinessRulesTest` is never edited. The vendor test
  could never comply anyway. **For the team:** does §1.4 apply to `src/test`? If yes, convert
  `ControllerContractTest` (and its 4 subclasses' constructors) in a separate change, extend the
  ArchUnit rule to the tests, and exclude the vendor test.

### C3 · §4.1: "author is not a member" returns 400; is that a business rule? ⚖️

- **Where:** `src/main/java/com/stackcraft/retroflow/service/ServiceValidation.java:49`
  ```java
  throw new InvalidInputException("Team " + team.getId() + ": '" + name + "' is not a member");
  ```
  `InvalidInputException` maps to **400** (`GlobalExceptionHandler.java:38-41`). This is decision
  F1, test-matrix row 30.
- **Standard (§4.1, Critical):** *"Validation failure → 400 Bad Request. Business rule violation →
  409 Conflict. … Using … 400 for a business rule violation, is a Critical violation."*
- **Why it matters:** the standard doesn't define where validation ends and a business rule
  begins. The MCP run in step 1 flagged that gap too. Every other 400 in the code checks the
  request on its own: blank, too long, unknown enum value, duplicate inside the list. This one
  depends on **stored state** (the team's members), and that is how the other 409s work (an open
  retro exists, a name is taken). If membership counts as a business rule, the current 400 is a
  Critical violation.
- **Correct implementation, if the team agrees it is a business rule:** throw `RetroflowException`
  (409), and update decision F1, matrix row 30 and the matching service and contract tests.
- **Why not fixed:** it changes the API contract and our own decision F1, and it means editing
  existing tests. The house rules say *"flag it and ask"*.

---

## Major

### M1 · §5.2: enum values are validated in the service, not at the controller ➖

- **Where:**
  - `dto/CreateFeedbackItemRequest.java:14` (`@NotBlank String type`)
  - `dto/CreateActionItemRequest.java:14` and `dto/UpdateActionItemRequest.java:12`
    (`@NotBlank String priority`)
  - `controller/ActionItemController.java:62` (`@RequestParam(required = false) String priority`,
    no check at all)

  Whether the value is one of the allowed constants is only checked in
  `ServiceValidation.parseEnum` (`ServiceValidation.java:37-43`).
- **Standard (§5.2, Major):** *"All data entering the system via API must be validated before it
  reaches the service layer. The controller is the boundary. Do not assume the service will catch
  bad data."*
- **Why it matters:** `"GREAT"` or `?priority=URGENT` reaches the service. The service does catch
  it today, but that is exactly the assumption §5.2 forbids. Any new caller of the service has to
  remember the check.
- **Correct implementation:** keep `String` (so the error can list the allowed values) and add a
  boundary check: `@Pattern(regexp = "LOW|MEDIUM|HIGH", message = "must be one of LOW, MEDIUM, HIGH")`
  on the records, and `@Validated` plus the same `@Pattern` on the `priority` request parameter. The
  service check stays as defence in depth.
- **Why not fixed:** Major, not Critical. It also changes the error text of existing contract tests
  (`FeedbackItem new: type must be one of …` would come from the handler instead of the service).
  This was a deliberate Block 2 choice (see the BD step 3 verdict in `prompt-log.md`), and it
  belongs on the backlog.

### M2 · §6.1: the vendor-facing `createRetrospective(teamId, title)` has no `@param`/`@return`/`@throws` ✅

- **Where:** `src/main/java/com/stackcraft/retroflow/service/RetrospectiveService.java:45-53`. The
  JavaDoc only has a description and an `@see`.
- **Standard (§6.1):** *"Every public method in a service class must have a JavaDoc comment with: …
  `@param` for each parameter, `@throws` for each exception that can be thrown, `@return` if the
  method returns a value."* Severity: *"Minor (rises to Major for methods implementing business
  rules)"*. This method enforces "one OPEN retro per team", so it is Major.
- **Why it matters:** it is the entry point the vendor test calls. Someone reading only this method
  can't tell that it throws 409 and 404.
- **Correct implementation:** the full tags, like the three-argument overload has.
- **Fixed (step 3, fix 2):** what was wrong: no `@param`/`@return`/`@throws` on a business-rule
  method. Based on §6.1. `mvn test`: 89/89 green, no test changed. Class is now 183 lines (§1.2: ≤ 200).

### M3 · §1.1: layer-first packages ➖ grandfathered

- **Where:** `com.stackcraft.retroflow.{controller, dto, entity, exception, repository, service}`.
- **Standard (§1.1):** *"All projects must follow a feature-first package structure, not a
  layer-first structure."* The **Incorrect** example is literally
  `com.stackcraft.retroflow.controller / .service / .repository`. Severity: *"Major. Existing
  projects are grandfathered but new projects must comply."*
- **Why it's tricky:** the starter was layer-first on purpose, and the vendor's
  `RetroflowBusinessRulesTest` imports `…entity`, `…service` and `…exception`. Moving to
  `…team` / `…retrospective` / `…feedback` would break a test we may never edit. RetroFlow is an
  existing, half-built project, so it falls under "grandfathered" (our decision G4).
- **Note on the document:** the heading says **"(CRITICAL)"**, but the severity line says
  **"Major"**. We sorted by the severity line. That inconsistency is worth reporting to the Guild.
- **Correct implementation, if it ever has to move:** one package per feature, each with its own
  controller, service, repository and entity, and the shared error handling in a `common` package.

---

## Minor

### m1 · §6.1: lookup methods without a description sentence ➖

These have `@param`/`@return`/`@throws`, but no sentence saying what the method does:
- `TeamService.getTeam` (`:60`) and `getAllTeams` (`:67`)
- `RetrospectiveService.getRetrospective` (`:91`) and `getRetrospectivesForTeam` (`:100`) *(line numbers
  refreshed: fix 2 moved them from `:85`/`:94`)*
- `FeedbackItemService.getFeedbackItems` (`:68`)
- the four `EntityLookup` methods (`:41`, `:61`, `:70`, `:79`)

§6.1 asks for *"A description of what the method does"*. **Fix:** add one line per method.

### m2 · §3.2: test names without a condition part ➖

- `TeamControllerTest.getAllTeams_returns200WithArray` (`:95`)
- `RetrospectiveControllerTest.getRetrospectivesForTeam_returns200WithArray` (`:96`)
- `FeedbackItemControllerTest.getFeedbackItems_returns200WithActionItemFields` (`:77`)

§3.2 requires *"`methodName_condition_expectedBehaviour`"*. **Fix:** for example,
`getAllTeams_whenTeamsExist_returns200WithArray`. The 3 vendor tests (`closingARetrospective_preventsAddingFeedback`
etc.) don't follow it either, but they can't be renamed.

### m3 · §3.3: tests with only `// Act & Assert` and no `// Arrange` ➖

- `RetrospectiveControllerTest:50`, `:142`
- `TeamControllerTest:129`
- `FeedbackItemControllerTest:54`, `:114`, `:134`
- `ActionItemControllerTest:170`
- `RetrospectiveRulesTest:145`
- `TeamRulesTest:111`

§3.3: *"All tests must use the AAA pattern with explicit comments"*. **Fix:** move the input into a
named local under `// Arrange`, even when it is one line.

### m4 · §3.4: magic numbers in tests ➖

- `RetrospectiveControllerTest.java:85`: `"Team 1: already has an open retrospective (10)"`. Build it
  from `TEAM_ID` and a named `OPEN_RETRO_ID`.
- `FeedbackItemControllerTest.java:84`: `.value(2)`. Use `items.size()`.
- `FeedbackItemRulesTest.java:187`: `items.get(2)`. Use `items.getLast()` or a named index.

§3.4: *"Test data must use named constants or clearly named local variables."*

---

## Checked and clean

| Section | How we checked | Result |
|---|---|---|
| §1.2 Class length ≤ 200 | `wc -l` on every class | Largest: `ActionItemRulesTest` 199, `RetrospectiveService` 177 |
| §1.3 Method length ≤ 20 | Script that counts body lines without blanks or comments | Longest real method: 13 lines (`TeamService.requireMembers`, a filter test). The script flagged `TeamControllerTest:62` as 49 lines: a **false positive**, caused by the `{` inside the JSON string `"{\"name\": "` |
| §2.2 Global exception handler | `GlobalExceptionHandler` | One `@RestControllerAdvice`, body `{error, status, timestamp}` for every error |
| §2.3 Never swallow exceptions | `grep "catch ("` in `src/main` | No catch blocks. The handler logs **and** responds, so nothing is swallowed |
| §3.1 Coverage | `mvn verify` | 93.7% lines / 86.0% branches; the gate is 80/70 and fails the build |
| §4.1 Status codes per endpoint | Controllers + contract tests | 201 create, 200 read/update with body, 204 delete, 404, 400, 409 (C3 aside) |
| §4.2 No internal details | Every handler method | Fixed messages. `DataIntegrityViolationException` → `Request -: conflicts with existing data`; Spring errors → reason phrase only |
| §4.3 Lists are arrays | List endpoints | All return `List<…>`, no wrapper |
| §5.1 No secrets | `application.properties`, full `git log -p` | `spring.datasource.password=` is **empty**, and history only has `${{ github.token }}` references |
| §5.3 Parameterised queries | Every `@Query` | `:teamId`, `:priority`, `:completed`, `:id`: named parameters only |
| §6.2 README | `README.md` headings | What it is, getting started, running the tests, endpoint list |

## Open questions for the Guild, not findings

- **§2.1 identifier when there is no id.** We use `Team new: …`, `FeedbackItem new: …`, and
  `Request -: …` for errors that belong to no entity (decision G3). All the standard's examples use
  a numeric id. Will the monitoring parser accept `new` and `-`?
- **§2.2 sample shows `"status": 400`.** Copied for a business rule, it would contradict §4.1. Our
  handler sets the status per exception type.

## Outside the standards (for the pre-staging review)

- `docker-compose.yml` sets `SPRING_H2_CONSOLE_SETTINGS_WEB_ALLOW_OTHERS: "true"` and
  `application.properties` enables `/h2-console` with user `sa` and an empty password. Anyone who
  can reach port 8080 gets a SQL console on the database. No StackCraft section covers this, but it
  is the first thing we'd raise for staging.

## Quotes checked against the local document (step 2 "then check it")

| Quote | Local `stackcraft-coding-standards.md` | Verbatim? |
|---|---|---|
| §2.1 "All exceptions thrown by the service layer must follow this exact format" | line 67-80 | ✅ |
| §1.4 "Field injection (`@Autowired` on a field) is prohibited." | line 44-61 | ✅ |
| §5.2 "The controller is the boundary. Do not assume the service will catch bad data." | line 266-270 | ✅ |
| §6.1 "Minor (rises to Major for methods implementing business rules)" | line 293-300 | ✅ |
| §1.1 heading "(CRITICAL)" vs. "Violation severity: **Major**" | line 8 vs. 25 | ✅ the inconsistency is in the document itself |

None of the quotes were made up. The two "is the violation really there?" spot checks,
`FeedbackItem.java:79` and `ServiceValidation.java:49`, show exactly the quoted line.
