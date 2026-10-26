# RetroFlow re-audit against the StackCraft coding standards (2026-10-26)

- **Source of truth:** `stackcraft-coding` **v3.1** ("Effective from Q1 2025", "Last reviewed: March
  2025"), read in full through the `ai-track-standards` MCP server (`read_document`) at the start of
  this audit. Every quote below is copied from that call.
- **Also read:** `decisions.md`, `CLAUDE.md` → `.github/copilot-instructions.md`.
- **Code audited:** `HEAD` = `3e0ddff` (after the Block 3 audit and its Critical fixes). Clean tree.
- **Relation to `audit-findings.md`:** this is an independent second pass, not a copy. The finding
  IDs C2, C3, M1, M3 and m1–m4 are reused so the references in `prompt-log.md` and `review/` stay
  valid. New items are marked **NEW**, and corrected line numbers are marked **line fix**.
- **Not run in this pass** (the permission prompt blocked them): `./mvnw verify` and a `git log -p`
  scan for secrets. §3.1 and the history part of §5.1 are therefore *not re-measured*. See "Checked
  and clean".
- **Status:** open · ⚖️ needs a team decision · ✅ fixed (verified at the line) · ➖ accepted
  exception (reason given).

## Summary

| # | Severity | Section | Where | Status |
|---|---|---|---|---|
| C2 | Critical | §1.4 Constructor injection only | `ControllerContractTest.java:21-22`, vendor `RetroflowBusinessRulesTest.java:30-34` | ⚖️ |
| C3 | Critical (disputed) | §4.1 HTTP status codes | `service/ServiceValidation.java:49` | ⚖️ |
| M1 | Major | §5.2 Validate all input at the boundary | 3 request records + `ActionItemController.java:62` | open |
| M3 | Major | §1.1 Package structure | every package | ➖ grandfathered (G4) |
| m1 | Minor | §6.1 JavaDoc description sentence | 9 public methods (**line fix**) | open |
| m2 | Minor | §3.2 Test naming | 4 own tests (**1 NEW**) + 3 vendor tests | open |
| m3 | Minor | §3.3 AAA comments | 10 tests (**1 NEW**) | open |
| m4 | Minor | §3.4 No magic values in tests | 15 places (**12 NEW**) | open |
| C1 | Critical | §2.1 Message format | `entity/FeedbackItem.java:79` | ✅ verified |
| M2 | Major | §6.1 JavaDoc on a business-rule method | `service/RetrospectiveService.java:45-59` | ✅ verified |

**Production code:** no open violation that the team hasn't already accepted or flagged. The two
Criticals are a test-code question (C2) and a definition question (C3). Neither one is a clear
breach that we could just fix. Most of the open work is test hygiene (m2–m4).

---

## Critical

### C2 · §1.4: `@Autowired` on fields in test classes ⚖️

- **Where:**
  - `src/test/java/com/stackcraft/retroflow/controller/ControllerContractTest.java:21-22`
    ```java
    @Autowired
    protected MockMvc mvc;
    ```
    This is the base class of all 4 controller test classes.
  - `src/test/java/com/stackcraft/retroflow/RetroflowBusinessRulesTest.java:30-34` (vendor file,
    never edited): `@Autowired private TeamService teamService;` and `… retrospectiveService;`
  - **NEW, related:** `ControllerContractTest.java:24-34` has four `@MockBean protected …Service`
    fields. They aren't `@Autowired`, but Spring still injects them into fields. See open question Q1.
- **Not a violation:** `@Autowired` on the *constructors* of `TeamRulesTest:34`,
  `RetrospectiveRulesTest:34`, `FeedbackItemRulesTest:38`, `ActionItemRulesTest:36` and
  `ServiceHappyPathTest:35`. That is constructor injection.
- **Production code is clean.** `ArchitectureTest.java:50-51` enforces
  `NO_CLASSES_SHOULD_USE_FIELD_INJECTION`, but with `ImportOption.DoNotIncludeTests` (`:26`), so the
  guard doesn't cover `src/test`.
- **Standard:** §1.4 *"Field injection (`@Autowired` on a field) is prohibited. Constructor injection
  must be used for all Spring-managed dependencies."* Violation severity: **Critical**.
- **Why it matters:** the document's reason is *"Field injection makes testing impossible and hides
  dependencies."* That reason is written for production beans. Read literally, the rule has no
  `src/main` limit.
- **Correct implementation** (as `TeamRulesTest` already does):
  ```java
  abstract class ControllerContractTest {
      protected final MockMvc mvc;

      protected ControllerContractTest(MockMvc mvc) {
          this.mvc = mvc;
      }
  }

  @WebMvcTest
  class TeamControllerTest extends ControllerContractTest {
      @Autowired
      TeamControllerTest(MockMvc mvc) {
          super(mvc);
      }
  }
  ```
- **Why ⚖️:** the vendor test can never comply, because the house rules say it is never edited. The
  team has to decide whether §1.4 applies to `src/test`. If it does: convert `ControllerContractTest`
  and its 4 subclasses, and run the ArchUnit rule on tests too, with the vendor class excluded.

### C3 · §4.1: "author is not a team member" returns 400. Is that a business rule? ⚖️

- **Where:** `src/main/java/com/stackcraft/retroflow/service/ServiceValidation.java:49`
  ```java
  throw new InvalidInputException("Team " + team.getId() + ": '" + name + "' is not a member");
  ```
  `InvalidInputException` maps to **400** (`GlobalExceptionHandler.java:38-41`). This follows
  decision F1.
- **Standard:** §4.1 *"Validation failure | 400 Bad Request"* / *"Business rule violation | 409
  Conflict"* … *"Using 200 for a creation, or 400 for a business rule violation, is a Critical
  violation."* Violation severity: **Critical**.
- **Why it matters:** every other 400 in the code can be decided from the request alone (blank, too
  long, unknown enum value, duplicate in the list). This one depends on **stored state** (the team's
  members), and every other state-dependent check in the code is a 409 (open retro exists, name
  taken, retro closed). If membership is a business rule, this is a Critical violation. The standard
  doesn't define where validation stops and a business rule starts.
- **Correct implementation, if the team says it is a business rule:**
  ```java
  throw new RetroflowException("Team " + team.getId() + ": '" + name + "' is not a member");
  ```
  Then update F1, test-matrix row 30, `FeedbackItemRulesTest.java:149-158` (expects
  `InvalidInputException`) and the controller Javadoc at `FeedbackItemController.java:31` and
  `ActionItemController.java:31`.
- **Why ⚖️:** it changes the API contract and an agreed decision, and it means editing existing
  tests. The house rules say *flag it and ask*.

---

## Major

### M1 · §5.2: enum values are only checked in the service, not at the controller boundary (open)

- **Where:**
  - `dto/CreateFeedbackItemRequest.java:14`: `@NotBlank String type,`
  - `dto/CreateActionItemRequest.java:14`: `@NotBlank String priority,`
  - `dto/UpdateActionItemRequest.java:12`: `@NotBlank String priority) {`
  - `controller/ActionItemController.java:62`: `@RequestParam(required = false) String priority,`
    with no check at all

  Whether the value is an allowed constant is only checked in `ServiceValidation.parseEnum`
  (`ServiceValidation.java:37-43`). The DTOs say so on purpose (`CreateActionItemRequest.java:10`:
  *"checked by the service so the error lists the allowed values"*).
- **Standard:** §5.2 *"All data entering the system via API must be validated before it reaches the
  service layer. The controller is the boundary. Do not assume the service will catch bad data."*
  Violation severity: **Major**.
- **Why it matters:** `"type": "GREAT"` or `?priority=URGENT` reaches the service. It is caught there
  today, but §5.2 forbids relying on that.
- **Correct implementation:** keep `String` (decision F4: exact uppercase names) and add a boundary
  check whose message still lists the allowed values:
  ```java
  public record CreateActionItemRequest(
          @NotBlank @Size(max = FeedbackItem.MAX_CONTENT_LENGTH) String content,
          @NotBlank @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "must be one of LOW, MEDIUM, HIGH") String priority,
          @NotBlank @Size(max = FeedbackItem.MAX_AUTHOR_LENGTH) String author) {
  }
  ```
  For the query parameter: put `@Validated` on `ActionItemController` and
  `@Pattern(regexp = "LOW|MEDIUM|HIGH") String priority` on the parameter. Note that
  `GlobalExceptionHandler.handleInvalidParameter` (`:69-72`) then answers with the generic
  `Request -: invalid request parameter`. To keep the allowed values in the error, that handler needs
  to pass the constraint message through. The service check stays as defence in depth.
- **Side effect:** the 400 texts in the existing contract tests change, for example
  `ActionItemControllerTest.java:160`. That means editing tests, so ask first.

### M3 · §1.1: layer-first packages ➖ grandfathered

- **Where:** `com.stackcraft.retroflow.{controller, dto, entity, exception, repository, service}`.
- **Standard:** §1.1 *"All projects must follow a feature-first package structure, not a
  layer-first structure."* Its **Incorrect** example is exactly `…controller / …service /
  …repository`. Violation severity: **Major**. *"Existing projects are grandfathered but new
  projects must comply."*
- **Why accepted:** RetroFlow is an existing project. The vendor test imports `…entity`, `…service`
  and `…exception`, so moving packages would break a file we may never edit (decision G4).
  ⚠️ G4 is still marked "proposal, needs a ✅" in `decisions.md`.
- **Correct implementation, if it ever has to move:** `…retroflow.team`, `…retrospective` and
  `…feedback`, each holding its own controller, service, repository and entity, plus a `…common`
  package for the error handling.
- **Note on the document:** the heading says "(CRITICAL)", but the severity line says **Major**. We
  used the line.

---

## Minor

§6.1's severity is *"Minor (rises to Major for methods implementing business rules)"*. None of the
methods below enforces a business rule, so they stay Minor.

### m1 · §6.1: public service methods without a description sentence (open, **line fix**)

These have `@param`/`@return`/`@throws` but no sentence that says what the method does. §6.1 requires
*"A description of what the method does"*.

| Method | Line now | Line in `audit-findings.md` |
|---|---|---|
| `TeamService.getTeam` | `TeamService.java:60` | 60 |
| `TeamService.getAllTeams` | `TeamService.java:67` | 67 |
| `RetrospectiveService.getRetrospective` | `RetrospectiveService.java:91` | 85 (stale since fix 2) |
| `RetrospectiveService.getRetrospectivesForTeam` | `RetrospectiveService.java:100` | 94 (stale since fix 2) |
| `FeedbackItemService.getFeedbackItems` | `FeedbackItemService.java:68` | 68 |
| `EntityLookup.team` / `retrospective` / `feedbackItem` / `actionItem` | `EntityLookup.java:41, 61, 70, 79` | same |

`EntityLookup` is a `@Component`, not a `@Service`. It is counted here because it lives in the
service package and services call it. See Q3.

**Fix:** one line per method, for example:
```java
/**
 * Finds a team by id.
 *
 * @param teamId id of the team
 * @return the team
 * @throws NotFoundException if no team has this id
 */
```

### m2 · §3.2: test names that don't follow `methodName_condition_expectedBehaviour` (open)

| Test | Problem |
|---|---|
| `TeamControllerTest.java:95` `getAllTeams_returns200WithArray` | no condition |
| `RetrospectiveControllerTest.java:96` `getRetrospectivesForTeam_returns200WithArray` | no condition |
| `FeedbackItemControllerTest.java:77` `getFeedbackItems_returns200WithActionItemFields` | no condition |
| **NEW** `TeamRulesTest.java:111` `lookups_withUnknownId_throwNotFound` | `lookups` is not a method. It tests 6 methods in one test. |
| vendor `RetroflowBusinessRulesTest.java:37, 52, 64` | none of the 3 fits. Can't be renamed. |

Standard: *"Test method names must follow: `methodName_condition_expectedBehaviour`"*, Violation
severity **Minor (but enforced in code reviews)**.
**Fix:** `getAllTeams_whenTeamsExist_returns200WithArray`, and so on. Split
`lookups_withUnknownId_throwNotFound` into `getTeam_withUnknownId_throwsNotFound`,
`getRetrospective_withUnknownId_throwsNotFound`, and so on.
Borderline, not counted: `reopen_byDesign_…` (`RetrospectiveRulesTest.java:100`) and
`reopenRetrospective_hasNoEndpoint_…` (`RetrospectiveControllerTest.java:142`). Both test that a
method does **not** exist, so there is no real method name to use.

### m3 · §3.3: tests with only `// Act & Assert` and no `// Arrange` (open)

- `RetrospectiveControllerTest.java:50`, `:142`
- `TeamControllerTest.java:62` (**NEW**: the first audit missed it), `:129`
- `FeedbackItemControllerTest.java:54`, `:114`, `:134`
- `ActionItemControllerTest.java:170`
- `RetrospectiveRulesTest.java:145`
- `TeamRulesTest.java:111`

Standard: §3.3 *"All tests must use the AAA pattern with explicit comments"*, **Minor**.
**Fix:** move the input into a named local under `// Arrange`, for example:
```java
// Arrange
String blankTitleBody = "{\"title\": \"\"}";

// Act & Assert
mvc.perform(post(CREATE_URL).contentType(MediaType.APPLICATION_JSON).content(blankTitleBody))
```
(Whether a test with no setup still needs `// Arrange` is Q4.)

### m4 · §3.4: hard-coded ids and counts in tests (open, **12 NEW**)

Most of these repeat a fixture id as a literal inside an expected message, so the test breaks
silently if `WebFixtures` changes (`TEAM_ID = 1`, `RETRO_ID = 10`, `FEEDBACK_ID = 100`,
`ACTION_ID = 200`, `WebFixtures.java:18-21`).

| Where | Literal | Use instead |
|---|---|---|
| `RetrospectiveControllerTest.java:85` | `"Team 1: … (10)"` | `"Team " + TEAM_ID + ": … (" + RETRO_ID + ")"` |
| **NEW** `RetrospectiveControllerTest.java:132` | `"Retrospective 10: …"` | `"Retrospective " + RETRO_ID + ": …"` |
| **NEW** `RetrospectiveControllerTest.java:45` | `"2026-10-26"` | `RETRO_DATE.toString()` |
| **NEW** `FeedbackItemControllerTest.java:66` | `"Retrospective 10: …"` | `RETRO_ID` |
| `FeedbackItemControllerTest.java:84` | `.value(2)` | `items.size()` from a named list |
| **NEW** `FeedbackItemControllerTest.java:124`, `:143` | `"FeedbackItem 100: …"` | `FEEDBACK_ID` |
| **NEW** `ActionItemControllerTest.java:68` | `"Retrospective 10: …"` | `RETRO_ID` |
| **NEW** `ActionItemControllerTest.java:91`, `:114`, `:126` | `"ActionItem 200: …"` | `ACTION_ID` |
| **NEW** `ActionItemControllerTest.java:160` | `"Team 1: …"` | `TEAM_ID` |
| **NEW** `RetrospectiveRulesTest.java:66` | `hasSize(2)` | `containsExactly(second, first)` or a named count |
| **NEW** `FeedbackItemRulesTest.java:157` | `'Zoe'` | `"'" + outsider + "'"` (the local on `:152`) |
| `FeedbackItemRulesTest.java:187` | `items.get(2)` | `items.getLast()` |

Standard: §3.4 *"Test data must use named constants or clearly named local variables."* **Minor**.

---

## Verified fixed since the first audit

| # | Line now | What's there |
|---|---|---|
| C1 §2.1 | `entity/FeedbackItem.java:79` | `"FeedbackItem new: type ACTION_ITEM must be created as an ActionItem"`, which is in the format |
| M2 §6.1 | `service/RetrospectiveService.java:45-55` | Description, `@param` ×2, `@return`, `@throws` ×3 |

## Checked and clean

| Section | How checked | Result |
|---|---|---|
| §1.2 Class ≤ 200 lines | `wc -l` on all 49 classes; `checks.sh` | Clean. ⚠️ `ActionItemRulesTest` is **199** lines, so one more test breaks §1.2. Next largest: `RetrospectiveService` 183. |
| §1.3 Method ≤ 20 lines | Counted by hand while reading every file (the script was blocked) | Clean. Longest: `ActionItemRulesTest.getActionItemsForTeam_withFilters_…` ~14 code lines; production: `TeamService.requireMembers` 13 |
| §1.4 in `src/main` | `checks.sh` + reading every constructor | Constructor injection everywhere; ArchUnit rule `ArchitectureTest.java:50-51` |
| §2.1 Message format | Every `throw` in `service/`, `entity/`, `exception/NotFoundException.java:9` | All `[Type] [id\|new]: reason` |
| §2.2 Global handler | `GlobalExceptionHandler.java` | One `@RestControllerAdvice`, `ApiError(error, status, timestamp)` for all 9 handlers |
| §2.3 No swallowed exceptions | `checks.sh` (no `catch (` in `src/main`) | Clean. The handlers that log (`:65`, `:95`) also answer the client, so nothing is swallowed |
| §3.1 Coverage | `pom.xml:22-24, 99-114` | Gate configured at 80% line / 70% branch, fails `verify`. **Not re-measured** (verify blocked). Last measured 93.7% / 86.0% at `dbbe1e2` |
| §4.1 Status codes (apart from C3) | All 16 handler methods + exception mapping | 201 on both creates (`ResponseEntity.created` / `@ResponseStatus(CREATED)`), 200 reads/updates with body, 204 delete, 404/400/409/500 per exception type |
| §4.2 No internal details | Every handler method | Fixed texts; DB error → `Request -: conflicts with existing data` (pinned by `TeamControllerTest.java:82-92`); Spring errors → reason phrase only; type mismatch echoes only the parameter name |
| §4.3 Lists are arrays | 4 list endpoints | All return `List<…Response>` |
| §5.1 No secrets | Grep of the working tree; `application.properties:7` | `spring.datasource.password=` is empty; CI uses `${{ github.token }}` only. **History not re-scanned** (blocked); the first audit reported it clean |
| §5.3 Parameterised queries | `checks.sh`; both `@Query` (`ActionItemRepository.java:16-25`, `TeamRepository.java:24-25`) | Named parameters only; the rest are derived queries |
| §6.1 (apart from m1) | Every public method in the 4 services | `@param`/`@return`/`@throws` complete |
| §6.2 README | `README.md` | What it is, 3 ways to run it with exact commands, test commands, endpoint list |

## Open questions (the standard is ambiguous or conflicts with our rules)

- **Q1 · §1.4 and tests.** Does "all Spring-managed dependencies" cover test classes? If it does, are
  `@MockBean` fields (`ControllerContractTest.java:24-34`) also "field injection"? There is no
  constructor alternative for `@MockBean` in Boot 3.3. Reading 1: production beans only (fits the
  stated reason). Reading 2: every class (fits the literal text).
- **Q2 · §4.1 validation vs. business rule.** This decides C3. Reading 1: membership is input
  validation, so 400 (current F1). Reading 2: it depends on stored state, so it is a business rule
  and must be 409.
- **Q3 · §6.1 "service class".** Is `EntityLookup` (`@Component` in `service/`) a service class?
- **Q4 · §3.3 with no setup.** Does a test with nothing to arrange still need `// Arrange`? The
  standard's own example has Arrange plus "Act & Assert", but it doesn't cover the empty case.
- **Q5 · §2.1 identifiers.** `new` and `-` (`Team new: …`, `Request -: …`, decision G3). All the
  standard's examples use numeric ids. Will the monitoring parser accept these?
- **Q6 · §2.1 exceptions that only pass through.** A `DataIntegrityViolationException` from a
  repository leaves the service with Hibernate's message, which isn't in the format. Does "thrown by
  the service layer" include exceptions that only propagate through it? (The client never sees it,
  see §4.2, but `GlobalExceptionHandler.java:65` logs it.)
- **Q7 · §3.1 has no "Violation severity" line**, unlike every other section. What severity does a
  coverage drop have?
- **For the Guild:** §1.1's heading "(CRITICAL)" disagrees with its severity line "Major".

## Outside the standards (not findings)

- The H2 console is enabled (`application.properties:13`) with user `sa` and an empty password, and
  `docker-compose.yml` lets anyone reach it. Already raised in `review/`.
- `README.md:58`: a blank line inside the "Running the tests" table means the PIT row (`:59`) no
  longer renders as part of it.

## Quotes checked word for word against the MCP document (v3.1)

| Quote | Section | Verbatim? |
|---|---|---|
| "Field injection (`@Autowired` on a field) is prohibited. Constructor injection must be used for all Spring-managed dependencies." | §1.4 | ✅ |
| "Field injection makes testing impossible and hides dependencies." | §1.4 | ✅ |
| "Using 200 for a creation, or 400 for a business rule violation, is a Critical violation." | §4.1 | ✅ |
| "All data entering the system via API must be validated before it reaches the service layer. The controller is the boundary. Do not assume the service will catch bad data." | §5.2 | ✅ |
| "All projects must follow a feature-first package structure, not a layer-first structure." / "Existing projects are grandfathered but new projects must comply." | §1.1 | ✅ |
| "A description of what the method does" / "Minor (rises to Major for methods implementing business rules)" | §6.1 | ✅ |
| "Test method names must follow: `methodName_condition_expectedBehaviour`" / "Minor (but enforced in code reviews)" | §3.2 | ✅ |
| "All tests must use the AAA pattern with explicit comments" | §3.3 | ✅ |
| "Test data must use named constants or clearly named local variables." | §3.4 | ✅ |
| "All exceptions thrown by the service layer must follow this exact format" | §2.1 | ✅ |
