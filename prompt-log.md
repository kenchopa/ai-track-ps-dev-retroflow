# Prompt log: Block 2

Every prompt we gave the agent, with a one-line verdict. In this run the "agent" is Claude Code,
working on one machine and playing both pairs one after the other:
- Pair AC worked on `block2-service`.
- Pair BD worked on `block2-web`.
- Both were merged into `block2` (our stand-in for `main`).

---

## Setup and warm-up (both pairs)

**Prompt (warm-up)**

```
Run `mvn test` yourself. Do not change, create or delete any file. In at most 10 lines, explain
why the build fails right now: for each failing test, what is missing.
```

**Result**
- Build compiles. 3 tests run, 3 errors, 0 failures.
- `closingARetrospective_preventsAddingFeedback`, `aTeam_cannotHaveTwoOpenRetrospectives` and
  `completedActionItem_cannotBeUncompleted` all stop on their first line, because
  `TeamService.createTeam` throws `UnsupportedOperationException` (a Block 1 stub).
- Behind that, every method the tests call next is also a stub: `createRetrospective`,
  `closeRetrospective`, `addFeedbackItem`, `addActionItem`, `completeActionItem` and
  `uncompleteActionItem`.
- The signatures all match the tests already. Block 1 made them that way on purpose. So nothing
  is *missing*: it's all unimplemented.

**Verdict:** worked. The agent ran the build itself (not a guess), and `git status` was clean afterwards.

---

## Pair AC (`block2-service`)

### Step 1: plan

**Prompt**

```
Read these files first, in this order: retroflow-brief.md, decisions.md, test-matrix.md,
src/test/.../RetroflowBusinessRulesTest.java, then everything under service/, entity/,
repository/ and exception/.
Do NOT change any file yet. Give me a plan in three separate parts:
1. What is needed to make the three tests in RetroflowBusinessRulesTest compile and pass:
   per test, which service methods are missing, stubbed, or have a different signature.
2. How every row of test-matrix.md gets enforced in the service layer, and which rows already
   have a test.
3. Per rule: the file, the class and method where it is enforced, and the exception it throws.
For every planned change, name the file and method. Rules go in the service layer, never in a
controller. Where the matrix and the brief disagree or a row needs a decision we haven't made,
flag it. Don't decide it. The plan must not change any test.
```

**Plan (agent output, summarised)**

*Part 1: vendor tests.* All 6 methods exist with the right signatures but throw. Implementing these makes all three tests pass:
- `TeamService.createTeam`
- `RetrospectiveService.createRetrospective`
- `closeRetrospective`
- `addFeedbackItem`
- `addActionItem`
- `completeActionItem`
- `uncompleteActionItem`

No test changes are needed.

*Part 2/3: where each rule lives.* The new services split for class size (max 200 lines):

| Matrix rows | Where (file → method) | Exception |
|---|---|---|
| 1-3 one OPEN retro per team | `RetrospectiveService.createRetrospective` → `TeamRepository.findByIdForUpdate` (pessimistic lock, D-R8) + `RetrospectiveRepository.findFirstByTeamIdAndStatus` | `RetroflowException` |
| 4 close twice | `RetrospectiveService.closeRetrospective` | `RetroflowException` |
| 5 no reopen | nothing to add: no method exists. Prove it with a reflection test | – |
| 6-7 add to closed retro | new `FeedbackItemService.addFeedbackItem` / `ActionItemService.addActionItem` → shared `requireOpen(retro)` | `RetroflowException` |
| 8, 10, 13 modify | new `FeedbackItemService.updateFeedbackItem(id, content)`, `ActionItemService.updateActionItem(id, content, priority)` | `RetroflowException` |
| 9, 11, 14 delete | new `FeedbackItemService.deleteFeedbackItem(id)` (covers action items too) | `RetroflowException` |
| 12, 15-18 complete/uncomplete | `ActionItemService.completeActionItem` / `uncompleteActionItem`. **No** open-retro check on complete (D-F6) | `RetroflowException` |
| 19-23 team input | `TeamService.createTeam` + shared `ServiceValidation` helpers | `InvalidInputException`, `RetroflowException` (dup name) |
| 24-26 retro input, lookups | `RetrospectiveService`, `ServiceValidation.requireText` | `InvalidInputException`, `NotFoundException` |
| 27-31 feedback input | `ServiceValidation.parseEnum`, `requireMember` | `InvalidInputException` |
| 32 not found | every `find…OrThrow` helper | `NotFoundException` |
| 33 list feedback | `FeedbackItemService.getFeedbackItems` | – |
| 34-35 filter | `ActionItemService.getActionItemsForTeam(teamId, priority, completed)` → `ActionItemRepository` JPQL with named parameters | `InvalidInputException` |

- `RetrospectiveService` keeps the four feedback and action item methods the vendor test calls, and delegates them to the new services.
- Already tested: rows 1, 6 and 15 (the vendor tests). No other row has a test.

*Flagged, not decided:*
- (a) Row 12 (complete in a closed retro) depends on D-F6, which is still open for the CTO.
- (b) The brief says nothing about validating in the service when the controller already does it. We validate in both (defence in depth), using the same messages.
- (c) Row 25: "today" comes from the server clock, so the test compares against `LocalDate.now()`.

**Verdict:** worked. All three "closed retro" operations (add, modify, delete) are in the plan, everything is in the service layer, and no test is touched. We accepted the split into `FeedbackItemService` and `ActionItemService`.

### Step 2: let the agent run the tests

**Prompt**

```
Implement the plan from step 1 (pasted above), in this order: repositories, then TeamService, then
RetrospectiveService, then the feedback and action item services. After each part, run
`mvn test -Dtest=RetroflowBusinessRulesTest` yourself, keep going until it passes, then stop and
list what you changed.
Hard limits:
- RetroflowBusinessRulesTest is never edited, skipped or disabled. If a test looks wrong, stop
  and tell me.
- No new dependencies.
- Don't remove anything without asking.
- Only touch service/, entity/ and repository/.
```

**Result**
- Repositories:
  - `TeamRepository`: `existsByNameIgnoreCase`, `findAllByOrderByNameAsc`, and `findByIdForUpdate` (`PESSIMISTIC_WRITE`)
  - `RetrospectiveRepository`: `findFirstByTeamIdAndStatus`, `findByTeamIdOrderByDateDescIdDesc`
  - `FeedbackItemRepository`: `findByRetrospectiveIdOrderByIdAsc`
  - New `ActionItemRepository.findForTeam`: JPQL with named parameters, and null means "any"
- Services: `TeamService` and `RetrospectiveService` are implemented. The new
  `FeedbackItemService` and `ActionItemService` hold the feedback rules.
- Shared code: `EntityLookup` (find or 404) and `ServiceValidation` (text, enum, member and
  open-retro checks, all with `[Entity] [id]: reason` messages).
- `RetrospectiveService` keeps the vendor-test methods and delegates them.
- `mvn test -Dtest=RetroflowBusinessRulesTest`: 3 run, 0 failures, 0 errors, BUILD SUCCESS.
  `git diff -- src/test` is empty.
- `uncompleteActionItem` on a completed item throws `RetroflowException`
  (`ActionItem 7: already completed, cannot be undone`). It is loud, not silent.
- Largest class: `RetrospectiveService`, 177 lines (limit 200).

**Verdict:** worked on the first run, so there was no failing test to work around. Because it was
green immediately, step 3 has to prove that the tests can fail.

### Step 3: generate the test suite

**Prompt**

```
Read test-matrix.md and .github/copilot-instructions.md. Write one test per matrix row in
src/test/.../service, next to the vendor test. Follow the house rules: name tests
method_condition_expectedBehaviour, use Arrange/Act/Assert comments and named constants, and
assert the exact exception type and message, not just "throws" or "not null". Put
@DisplayName("Row N: ...") on every test so we can count them. Run `mvn test` and report.
If a row can't be tested at the service level, flag it. Don't skip it silently.
Then prove the tests can fail: break three rules in the code one at a time, run the tests,
show which fail, and put the code back.
```

**Result**
- Four test classes instead of one: `RetrospectiveRulesTest`, `FeedbackItemRulesTest`,
  `ActionItemRulesTest` and `TeamRulesTest`, plus a shared `RetroflowFixtures`. One class would
  have been about 500 lines, and StackCraft 1.2 allows at most 200.
- `@Transactional`, so every test rolls back. Team names are random because the vendor tests
  commit their teams to the same H2 database.
- **35 of the 35 rows have a test** (counted with `grep -o '@DisplayName("Row N'`). There are 40
  test runs in total: row 27 is parameterized (2 runs), plus 1 happy-path test and the 3 vendor
  tests. All green.
- Assertions use `isExactlyInstanceOf`, so a 409 rule can't pass by accidentally throwing the
  400 or 404 subclass. They also check the full message.
- Flagged:
  - Row 5 ("reopen") is a reflection test at this level: there is no reopen method in the
    service and no status setter on the entity. The HTTP part (PUT /reopen → 404) belongs to
    Pair BD's contract tests.
  - Row 35's `400` for `priority=URGENT` is the service's `InvalidInputException`. Mapping it to
    HTTP is the web layer's job.
- Sabotage check. Each time the code was put back with `git checkout`, and `git status` was clean.

  | Sabotage | Tests that failed |
  |---|---|
  | `deleteFeedbackItem` ignores a closed retro | rows 9 and 11 |
  | `uncompleteActionItem` silently does nothing | row 15 and the vendor test |
  | open-retro check looks at `CLOSED` instead of `OPEN` | rows 1 and 2, row 34, and the vendor test |

**Verdict:** worked. The row count matches the matrix, and the sabotage run shows the tests notice broken rules.

### Step 4: attack your own tests (PIT)

**Prompt**

```
Add PIT mutation testing to pom.xml. Before you add them, check on Maven Central that
pitest-maven and pitest-junit5-plugin exist in the exact versions you choose, and tell me
which versions you checked. Limit targetClasses to com.stackcraft.retroflow.service.* and
targetTests to the service tests and RetroflowBusinessRulesTest. Don't bind it to the build.
Run it, report the mutation score, and list every surviving or uncovered mutant: file, line and
what was changed. Then write tests that kill them, without changing production code, and
report the score before and after. If PIT won't run after two tries, sabotage the code by hand
instead, one bug at a time.
```

**Result**
- Checked on Maven Central: `pitest-maven` 1.30.1 and `pitest-junit5-plugin` 1.2.3 (both latest
  releases). Run it with
  `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`. The report is in
  `target/pit-reports/`.
- **Before:** 71 mutants, 66 killed, 0 survived, 5 without coverage. **Score 93%.**

  | File:line | Mutation | What it means |
  |---|---|---|
  | `ActionItemService:81` | removed `item.setContent(text)` | an update that silently keeps the old text: never noticed |
  | `ActionItemService:82` | removed `item.setPriority(...)` | the same for priority |
  | `ActionItemService:83` | `return item` → `return null` | the API would return an empty body |
  | `RetrospectiveService:131` | `addFeedbackItem` returns null | the vendor-facing delegate is only tested on a *closed* retro |
  | `RetrospectiveService:174` | `uncompleteActionItem` returns null | only tested on a *completed* item |

- The root cause is the same in every case: the matrix lists the **rule violations**, and we
  tested those carefully, but the happy path of "modify an action item" (rows 10/13 cover
  feedback, not action items) was never run.
- New `ServiceHappyPathTest` (3 tests). Production code unchanged.
- **After:** 71 of 71 killed, **100%**. A cross-check with the stronger mutator set
  (`-Dmutators=STRONGER`) also killed 88 of 88.
- The delete path of a feedback item in a closed retrospective: no mutant survived there. Rows 9
  and 11 kill the "remove the check" mutant (we also saw that in the step 3 sabotage).

**Verdict:** worked. The lesson: the mutants that matter weren't on the rules we worried about,
they were on the "boring" update path nobody wrote a row for. Would we have spotted a missing
`setPriority` in a code review? Probably not.

---

## Pair BD (`block2-web`)

> Branched from `block2-service` instead of `block2`. The planned controllers for feedback and
> action items need `FeedbackItemService` and `ActionItemService`, which Pair AC created. We work
> one after the other on one machine, so this avoids mocking classes that don't exist yet.
> BD only touches `controller/`, `dto/` and `exception/`, plus tests.

### Step 1: plan before you act

**Prompt**

```
Read the controllers, the dto and exception packages, retroflow-brief.md and decisions.md
(G1, G3, G5 and the endpoint contract). Do NOT change any file yet. Plan in three parts, with
the file and class for every change:
1. Every endpoint that exists today: what it returns (status code and body) on success, invalid
   input, unknown id and a business-rule violation. Compare that with the brief and decisions.md.
2. Contract tests for every endpoint in the decisions.md contract: success, 400, 404 and 409,
   working with JSON in and JSON out, with the services mocked.
3. Restructuring: DTOs instead of entities, one @RestControllerAdvice with one error format,
   validation on the request objects, no logic in controllers.
Stay out of services, entities and repositories (Pair AC). Where the brief doesn't answer
something, flag it. Don't decide it.
```

**Plan (agent output, summarised)**

*Part 1: what exists.* Two controllers with 7 endpoints. They match the contract:
- `TeamController`: `POST /api/teams` returns 201 + `Location`, and `GET /api/teams` and
  `GET /api/teams/{id}` return 200.
- `RetrospectiveController`: create returns 201, list and get return 200, and close returns 200.
- They already use request and response records. One `GlobalExceptionHandler` maps:
  - 404 → `NotFoundException`
  - 400 → `InvalidInputException`, bean validation, unreadable JSON and type mismatches
  - 409 → `RetroflowException`
  - Spring's own 404/405 keep their status
  - everything else → 500 without internals

*Gaps against decisions.md:*
- (a) None of the 8 feedback and action item endpoints exist yet.
- (b) The contract has `PUT /api/feedback-items/{id}` for content, but decision F3 also lets an
  action item's **priority** change. There is no endpoint for that. **Flagged.** Proposal:
  `PUT /api/action-items/{id}` with `{content, priority}`. Needs a team decision.
- (c) A validation error on an *update* body would say `FeedbackItem new: …`. "new" is wrong
  there: the identifier should be the id from the path.
- (d) Two requests creating the same team name at the same moment pass the service check. The
  database's unique constraint then throws `DataIntegrityViolationException`, which becomes a
  **500** today. It should be a 409, without database details (StackCraft 4.2).

*Part 2: contract tests.* `@WebMvcTest` with all four services as `@MockBean`. One class per
controller: `TeamControllerTest`, `RetrospectiveControllerTest`, `FeedbackItemControllerTest` and
`ActionItemControllerTest`. The latter two target controllers that don't exist yet, so they'll
fail at first.

*Part 3: restructure.* New `FeedbackItemController` and `ActionItemController`. New request
records `CreateFeedbackItemRequest`, `CreateActionItemRequest`, `UpdateFeedbackItemRequest`
and `UpdateActionItemRequest`, and one `FeedbackItemResponse` (priority and completed only for
action items). Handler fixes for (c) and (d).

*Business-rule status code:* the agent picked **409**, citing StackCraft 4.1, and that is
already decision G1. The brief itself only says "appropriate", so we confirmed it in
`decisions.md`.

**Verdict:** worked. Nothing in services, entities or repositories is touched. Gap (b) was
flagged instead of decided, and we decided it ourselves (the extra `PUT /api/action-items/{id}`,
added to the contract).

### Step 2: write the contract tests first

**Prompt**

```
Following the plan, write a web-layer test class per controller in src/test/.../controller:
Team, Retrospective, FeedbackItem and ActionItem (the last two per the decisions.md contract).
Test only the web layer: @WebMvcTest with every service mocked. Send JSON in and assert on JSON
out (jsonPath), never on Java classes, so the tests survive the restructuring. Check the status
AND the body, including the error format {error, status, timestamp}. Cover success, 400, 404
and 409 where they apply. Run them and report which fail and why. Don't fix anything yet.
```

**Result:** 42 contract tests in 4 classes, sharing a `ControllerContractTest` base and `WebFixtures`.
- `TeamControllerTest` 9, `RetrospectiveControllerTest` 10, `FeedbackItemControllerTest` 11, `ActionItemControllerTest` 12.
- **24 fail, all for real reasons:**
  - 22 tests: the feedback and action item endpoints don't exist yet, so they get a 404 from
    Spring ("Request -: Not Found").
  - `createTeam_whenDatabaseRejectsDuplicate_returns409WithoutDatabaseDetails`: a
    `DataIntegrityViolationException` becomes a **500**. This is gap (d) from the plan. Code
    bug, not a test bug.
  - `getFeedbackItems_forUnknownRetro_returns404`. See below.
- **A test that could never fail.** The first version of `getFeedbackItems_forUnknownRetro_returns404`
  was **green**, even though the endpoint doesn't exist. It only checked for a 404, and an
  unknown route is also a 404. We made it assert the error message as well
  (`Retrospective 999: not found`), and now it fails for the right reason. The same trap exists
  for `reopenRetrospective_hasNoEndpoint_returns404`, but there a route-level 404 is exactly
  what we want (test-matrix.md row 5).

**Verdict:** needed one fix. Checking the status alone isn't enough for "not found" tests: always
assert the body too.

### Step 3: restructure while the tests watch

**Prompt**

```
Follow the plan to make the failing contract tests green, in this order:
(1) request and response records, (2) FeedbackItemController, (3) ActionItemController,
(4) GlobalExceptionHandler fixes. Run the controller tests after every part, then stop and list
what changed. Only touch controller/, dto/ and exception/. Services, entities and repositories
belong to Pair AC. The tests work with JSON, so they shouldn't need to change: if you think one
must change, ask first. Don't remove anything without asking.
```

**Result**
- New: `CreateFeedbackItemRequest`, `CreateActionItemRequest`, `UpdateFeedbackItemRequest`,
  `UpdateActionItemRequest`, `FeedbackItemResponse` (`priority`/`completed` only for action
  items), `FeedbackItemController` (4 endpoints) and `ActionItemController` (5 endpoints).
- `GlobalExceptionHandler`:
  - Validation errors on an update body now name the item from the path (`FeedbackItem 100: …`)
    instead of `… new: …`.
  - `DataIntegrityViolationException` → 409 `Request -: conflicts with existing data`, logged,
    and no database text reaches the client.
- **All 85 tests green**: 42 contract tests, 40 service tests and the 3 vendor tests.
- `git status`: no test file changed. Nothing in `service/`, `entity/` or `repository/` changed.
- "Show me every place where an entity is still returned or accepted by a controller": **none**.
  `TeamController` and `RetrospectiveController` still use the entity as a local variable, only
  to read the new id for the `Location` header. We accepted that.
- One error shape everywhere, including bean validation, unreadable JSON, type mismatches,
  Spring's own 404/405 and the 500 fallback.

**Verdict:** worked. The type and priority stay `String` in the requests on purpose: an enum
would make Jackson reject `"GREAT"` with a generic "malformed JSON" message, while the service
returns "must be one of WENT_WELL, NEEDS_IMPROVEMENT".

### Step 4: lock it in with an architecture test

**Prompt**

```
Add ArchUnit as a test dependency. First check on Maven Central that the artifact and the exact
version exist, and tell me what you checked. Then write one architecture test class with these
rules: controllers don't use repositories; controller methods don't return or accept entities
(also not inside a List); services don't depend on controllers or DTOs; no field injection.
Prove each rule can fail: break it on purpose, run the test, show the failure, and put the code
back. Afterwards git status must show no change in src/main.
```

**Result**
- `com.tngtech.archunit:archunit-junit5:1.5.1`, test scope. It is the latest release in
  maven-metadata.xml, and the `.pom` returns HTTP 200 on repo1.maven.org.
- `ArchitectureTest` has 4 rules, all green on the current code. The "no entities" rule uses
  `getAllInvolvedRawTypes()`, so `List<Team>` is caught too. A plain `notHaveRawReturnType`
  would have missed it.
- Proof. Each sabotage broke exactly its own rule and nothing else, and the code was restored
  with `git checkout`:

  | Sabotage | Rule violated |
  |---|---|
  | `TeamController` references `TeamRepository` | `controllersDoNotUseRepositories` |
  | `public List<Team> sabotage()` in `TeamController` | `controllersDoNotExposeEntities` ("exposes entity Team") |
  | `TeamService` references `TeamResponse` | `servicesDoNotDependOnWebLayer` |
  | `@Autowired` field in `TeamController` | `noFieldInjection` |

- `git status src/main` was empty afterwards.

**Verdict:** worked. We checked the `pom.xml` dependency ourselves too.

---

## Bring it together (both pairs)

**Prompt**

```
Merge block2-service and block2-web into block2 (our main). If there are conflicts, show me each
one and how you resolved it. Then run `mvn verify` and report the test count, how many are green,
and the coverage. Finally, start the app and walk through the main flow by hand with curl.
```

**Result**
- **Merge:** both branches went in without conflicts. block2-web was built on top of
  block2-service, and the two pairs touched different packages.
- **`mvn clean verify`:** BUILD SUCCESS. **89 tests, 89 green**:

  | Tests | Count |
  |---|---|
  | vendor tests | 3 |
  | service tests (matrix rows + PIT happy paths) | 43 |
  | contract tests | 42 |
  | architecture rules | 4 |

  Coverage is 93.7% of lines and 86.0% of branches (the gate is 80% / 70%).
- **Smoke test against the real app** (`docker compose up`): every row in the matrix that can
  be checked over HTTP behaves as written, including 409 on the second open retro, on modify,
  delete and add in a closed retro, and on uncomplete; complete still works after close; the
  filter works; `/reopen` gives 404.

**Best prompt:** AC step 4 (PIT). It found a real gap that 35 carefully written rule tests had
missed: an action item update that silently changes nothing would have passed every test.

**Worst prompt:** BD step 2, first version. "Check the status code" let a 404 test pass for an
endpoint that didn't exist. We had to add "and check the body" afterwards.

**What the agent did that we didn't expect:** the smoke test showed that the author `alice`
is accepted (membership is case-insensitive, D-F1) but **stored as typed**, not as the team's
spelling `Alice`. No test caught it, because no row in the matrix says how the name is stored.
**Open question for the team:** normalise to the member's spelling? We added it below instead
of letting the agent decide.

### Open points after Block 2
- Store the author with the team's spelling of the member name? (see above)
- D-F6 (complete after close) is still waiting for the CTO.

---
---

# Prompt log: Block 3

Same setup as Block 2: Claude Code plays both pairs one after the other.
- Pair AD works on `block3-audit`.
- Pair BC works on `block3-review`.
- Both get merged into `block3` (our stand-in for `main`).

Every StackCraft rule we cite in this block comes from the `ai-track-standards` MCP server
(`read_document("stackcraft-coding")`, version 3.1). The local `stackcraft-coding-standards.md`
is only used to check the quotes.

## Setting up (both pairs)

- `.mcp.json` in the root of the repo registers `ai-track-standards` for Claude Code
  (`https://ai-track-ps-production.up.railway.app/mcp`, HTTP, no login). Copilot users: use
  `.vscode/mcp.json` from `block3.md`. It isn't committed because `.vscode/` is in `.gitignore`.
- The tools list shows `mcp__ai-track-standards__list_documents` and `read_document`.
  `list_documents` returns `stackcraft-coding`, `stackcraft-qa` and `meridian-regulatory`.
- There is no local JDK on this machine. Maven runs in Docker:
  `docker run --rm -v "$PWD":/workspace -v ai-track-ps-dev-retroflow_m2:/root/.m2 -w /workspace maven:3.9-eclipse-temurin-21 mvn -B verify`.

## Step 1 (both pairs): feel the difference

The same question, asked three times with `claude -p` in an empty folder, so the repo's
`CLAUDE.md` (which already contains the answer) is not loaded.

**Question**

```
What exact format does StackCraft require for exception messages in the service layer,
and which HTTP status code do we have to use when a business rule is violated?
```

**Our extra sentence for the run with the connector**

```
Answer only from the stackcraft-coding document on the ai-track-standards MCP server: read it
with read_document first, quote the exact section number and wording you rely on, and say so if
the document does not answer part of the question.
```

| Run | Setup | What came back |
|---|---|---|
| A: "off" | MCP off, file tools still on | It **went looking on disk** and found `ai-course/ai-track-ps-dev/stackcraft-coding-standards.md`. The answer was correct (§2.1, 409), but grounded in a file we never pointed it to. "Switching off the connector" is not the same as "no context" when an agent can read files. |
| B: really off | MCP off, file and web tools off | **Honest:** "I don't recognize StackCraft… rather than guess, I'll say I don't know". Then it still gave a "usual practice" fallback: **422** (or 409) and an `ERROR_CODE: message` / RFC 9457 format. That is wrong for StackCraft (§4.1 says 409, and §2.1 is a different format). Anyone who reads only the bullets copies the wrong answer. |
| C: on | MCP on, plus our extra sentence | Called `read_document`, quoted **§2.1** `[EntityType] [identifier]: [human-readable reason]` with the 3 examples, and **§4.1** "Business rule violation → 409 Conflict" plus "Using … 400 for a business rule violation, is a Critical violation." It also listed what the document **doesn't** cover: which exception class to use, what an "identifier" is when there is no id, and where validation ends and a business rule begins. |

**Check against `stackcraft-coding-standards.md`:** §2.1 (lines 67-80) and §4.1 (lines 187-204)
exist and are quoted word for word. Run C also noticed that the §2.2 sample body says
`"status": 400`, which contradicts §4.1 if you copy it for a business rule. That one is easy to
miss.

**Our own service layer:** we already use the §2.1 format, e.g. `ActionItemService:117`
`"ActionItem " + id + ": already completed, cannot be undone"` (the standard's own example) and
`NotFoundException` → `"{Entity} {id}: not found"`. Our own convention for when there's no id
yet: `Team new: …`, `FeedbackItem new: …`, and `Request -: …` for errors that belong to no
entity (decision G3). The standard says nothing about this case, and run C flagged that gap too.

**Verdict:** worked. The difference: without the connector you get either an honest "I don't
know" followed by a plausible wrong default, or an agent that quietly grounds itself in whatever
file it finds. With the connector you get the real section numbers, quotes you can check, and a
list of what the document leaves open.

## Pair AD (`block3-audit`)

### Step 2: the audit

**Prompt**

```
Read the StackCraft coding standards through the ai-track-standards MCP server
(read_document "stackcraft-coding"). Don't use what you remember about Java standards: if a rule
isn't in that document, it isn't a finding. Then audit everything under src/ against every
section, one by one. For every finding give: the file and line, the exact quote of the section it
breaks, why it matters (use the document's own reason where it gives one), the correct
implementation as code, and the severity from the section's "Violation severity" line. Sort
Critical → Major → Minor. Use mechanical checks where you can (line counts, grep for @Autowired,
catch blocks and @Query) and say how you checked. Also list the sections you checked that came
out clean. Where a rule is ambiguous, or two of our rules conflict, flag it instead of deciding.
Write it to audit/audit-findings.md. Don't change any code.
```

**Result:** `audit/audit-findings.md`: 3 Critical, 3 Major and 4 Minor findings, 11 sections clean,
2 open questions for the Guild, and 1 security issue outside the standards.
- **C1 §2.1:** `FeedbackItem.java:79` throws `"Use ActionItem for feedback of type ACTION_ITEM"`,
  which leaves the service layer unparseable. It can't be reached through the API today.
- **C2 §1.4:** `@Autowired` fields in 6 of our test classes plus the vendor test. Production code is
  clean (ArchUnit checks it). ⚖️ Fixing it means editing existing tests, which step 3 forbids.
- **C3 §4.1 (disputed):** "author is not a member" returns 400. It depends on stored state, like our
  409s do. ⚖️ This is a team decision (F1), not the AI's.
- **Major:** enum values are validated in the service, not at the boundary (§5.2); the vendor-facing
  `createRetrospective(teamId, title)` has no JavaDoc tags (§6.1, Major because it's a business
  rule); layer-first packages (§1.1, grandfathered).

**Then check it**
- **Three quotes looked up** in `stackcraft-coding-standards.md` (§2.1, §1.4, §5.2, plus §6.1):
  all verbatim, none made up. One surprise: the document **contradicts itself** on §1.1. The
  heading says "(CRITICAL)", the severity line says "Major".
- **Two findings opened at the named line** (`FeedbackItem.java:79`, `ServiceValidation.java:49`):
  the violation is really there.
- **§1.1 found?** Yes, as Major and grandfathered. It's tricky because the vendor test imports
  `…entity`/`…service`/`…exception`, so going feature-first would break a file we may never edit.
- **False positive from our own tooling:** the method-length script reported a 49-line test. A
  `{` inside a JSON string literal threw off the brace counting, and the method is really 4 lines.
  A mechanical check needs checking too.

**Verdict:** worked. Grounding changed the audit: no generic "use Lombok"- or "add Swagger"-style
findings, every finding points to a section, and the ambiguous ones (C2, C3) are flagged instead
of "fixed".

### Step 3: fix the Critical findings, with the tests watching

**Prompt**

```
Fix the Critical findings in audit/audit-findings.md one at a time. For each: make the smallest
change in src/main, run `mvn test` yourself, and stop if anything goes red. Existing tests are
never edited: if a fix would need that, don't fix it, flag it. Findings marked ⚖️ need a team
decision: don't fix those, write down why. After each fix, update its entry in the audit (what was
wrong, which section) and commit it on its own. Finish with `mvn verify` and `git diff -- src/test`.
```

**Result**
- **Fix 1, C1 §2.1** (`684b6e8`): the `FeedbackItem` guard message is now
  `FeedbackItem new: type ACTION_ITEM must be created as an ActionItem`. `mvn test`: 89/89.
- **Fix 2, M2 §6.1** (`d2a6648`): full `@param`/`@return`/`@throws` on
  `createRetrospective(teamId, title)`. It's Major, not Critical, but it's a business-rule method
  and a three-line change. `mvn test`: 89/89.
- **C2 §1.4, not fixed** ⚖️: field injection lives only in test classes. Converting them means
  editing existing tests (forbidden in this step), and the vendor test can never comply. Question
  for the team: does §1.4 apply to `src/test`?
- **C3 §4.1, not fixed** ⚖️: changing "not a member" from 400 to 409 changes decision F1, matrix row
  30 and existing tests. That's the team's call, not ours.
- `mvn verify`: **89/89 green**, coverage 93.7% lines / 86.0% branches, BUILD SUCCESS.
  `git diff dbbe1e2 -- src/test` is **empty**: no test was touched.
- **Test for fix 1:** none yet, on purpose. Block 3 has us write it with the other pair's
  `stackcraft-tests` skill in the swap.

**Verdict:** worked, but only 1 of the 3 Criticals could be fixed inside the rules of this step.
The other two are real disagreements (a rule vs. a vendor file, and a rule the standard doesn't
define), and an agent "fixing" them would just be picking a side for us.
