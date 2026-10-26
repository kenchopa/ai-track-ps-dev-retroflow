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
