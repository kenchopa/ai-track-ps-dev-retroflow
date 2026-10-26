# Retro answers: Block 1 and Block 2

Our answers to the questions in `block1.md` and `block2.md` (course repo), taken from what is in
this repo: the git history, `decisions.md`, `test-matrix.md` and `prompt-log.md`.

---

## Block 1

### Pair AB: domain model and API

| Question | Answer |
|---|---|
| **P1: ambiguities and decisions** | `decisions.md` covers every open point in the brief: 6 ground rules plus decisions on teams, retros, feedback, action items and the entity model, and the endpoint contract. The main ones: 409 for business rules (G1); no reopen operation at all (R5); a team name must be unique, ignoring case (T1); the author must be a team member (F1); an action item can still be completed after its retro is closed (F6, still open for the CTO). |
| **P2: proposal vs. decisions** | We used `SINGLE_TABLE` inheritance (`ActionItem extends FeedbackItem`). Feedback per retro is then one query with no joins. The cost is nullable `priority`/`completed` columns. `JOINED` gives a cleaner schema but needs a join on every read; `TABLE_PER_CLASS` needs a `UNION`. |
| **P2: N+1** | All relationships are one-way, lazy `@ManyToOne`, with no `@OneToMany` collections, so loading a collection by accident can't cause N+1. The one known N+1: members are an EAGER `@ElementCollection`, so `GET /api/teams` runs one extra query per team. That's acceptable for 12 teams. |
| **P3: where we differed from the prompt** | The prompt asked for setters on everything. We left out setters for `status` and `completed`; they change only through `close()` and `markCompleted()`. That way "cannot reopen" and "cannot uncomplete" can't be broken through a setter. `mvn compile` passed. |
| **P4: status codes** | 201 + `Location` on create, 200 on reads and close, 404 for an unknown id, 400 for validation, 409 for business rules. There is one `@RestControllerAdvice` with the body `{error, status, timestamp}`. |
| **P5: which rules were enforced?** | At the end of Block 1, **none** of the 4 rules were. The service bodies were stubs throwing `UnsupportedOperationException`, so all 3 vendor tests failed, as expected. |
| **P6: row count** | **35 rows.** The 4 core rules give 18 (one OPEN retro: 3, no reopen: 2, closed retro frozen: 9, no uncomplete: 4). Validation and lookups give 17 more. The vendor tests cover 3 of them (rows 1, 6, 15). |

### Pair CD: scaffolding and CI

| Question | Answer |
|---|---|
| **P1: what the README got wrong** | It said Gradle and PostgreSQL. The project is really Maven with in-memory H2. Other issues the audit found: no actuator, no coverage gate, no Maven wrapper, H2 console always on, `ddl-auto=update`, no error handler. |
| **P2: Dockerfile** | Multi-stage, with a slim JRE (`temurin:21-jre-alpine`) at runtime. Running as root would mean an exploit in the app gets root in the container. The app runs as user `retroflow`, and the app files stay owned by root, so the process can't rewrite its own code. Checked: `id` shows `retroflow`, `touch /app/x` is denied, and the container reports healthy. |
| **P3: does JaCoCo fail or only report?** | It fails. Proof: `lines covered ratio is 0.21, but expected minimum is 0.80` and then BUILD FAILURE. |
| **P4: the security concern** | (1) **Unpinned actions:** someone can move a tag like `@v4` to malicious code, so every action is now pinned to a full commit SHA (see the tj-actions compromise). (2) **Pwn requests:** with `pull_request_target`, code from a fork's PR runs with a write token and secrets. We use `pull_request`, which gives fork PRs a read-only token. (3) The third-party JaCoCo comment action got a write token, so we replaced it with `awk` plus `gh`. (4) Permissions are read-only by default and checkout uses `persist-credentials: false`. **Did the AI catch its own mistakes?** Yes: it flagged all of these in its own draft. It also found that the PR trigger pointed at `main` while the fork uses `master`. |
| **P5: README** | Every command was run before commit: docker compose, `./mvnw spring-boot:run`, the tests, and the dev container. |

### House rules

`.github/copilot-instructions.md` (`CLAUDE.md` imports it) has both required rules: never change a
test to make it pass, and never add a dependency without checking that the exact version exists.

---

## Block 2

**Warm-up:** 3 tests, 3 errors. All of them stop at `TeamService.createTeam`, which was a stub.
Nothing was missing: the signatures already matched, the methods just weren't implemented. The
agent really ran `mvn test`, and `git status` stayed clean.

### Pair AC: service layer

| Question | Answer |
|---|---|
| **Step 1: all 3 closed-retro operations in the plan?** | Yes: add, modify and delete, all in the service layer, and no test changes. Flagged instead of decided: F6 (row 12) and validating in both layers. |
| **Step 2: is `uncomplete` loud?** | Yes. It throws `RetroflowException` with `ActionItem 7: already completed, cannot be undone`, which becomes a 409. `RetroflowBusinessRulesTest` was not touched (`git diff -- src/test` was empty). It went green on the first run, so the agent never had to work around a failing test. |
| **Step 3: test count** | All 35 rows have a test, counted via `@DisplayName("Row N…")`. They're split over 4 classes because of the 200-line limit. To prove the tests can fail, we broke 3 rules by hand and the right rows failed each time. |
| **Step 4: mutation score** | **93% before → 100% after** (71/71 mutants killed; 88/88 with the STRONGER mutator set). **Bug the first tests missed:** an action item update that never actually set the content or priority. Every test still passed, because the matrix only listed rule *violations*, not the happy path. Would a code review catch a missing `setPriority`? Probably not. The delete path in a closed retro had no surviving mutant. |

### Pair BD: web layer

| Question | Answer |
|---|---|
| **Step 1: which status code for a business rule?** | 409. The agent cited StackCraft 4.1, and we confirmed it in `decisions.md` (G1). Flagged gap: there was no endpoint to change an action item's priority, so we decided to add `PUT /api/action-items/{id}`. |
| **Step 2: a test that could never fail?** | Yes. `getFeedbackItems_forUnknownRetro_returns404` was green even though the endpoint didn't exist, because an unknown route is also a 404. Fix: also check the body (`Retrospective 999: not found`). 24 of 42 tests failed, all for real reasons. |
| **Step 3: result** | 85/85 green, no test changed. No controller returns or accepts an entity. One error format everywhere. A `DataIntegrityViolationException` used to give a 500; now it gives a 409 without database details. |
| **Step 4: ArchUnit** | 4 rules: no repositories in controllers, no entities exposed (`List<Team>` included), services don't depend on the web layer, no field injection. We broke each one on purpose and only that rule failed. `archunit-junit5:1.5.1` was confirmed to exist on Maven Central. |

### Together

- **Merge:** no conflicts. `mvn verify` gives **89/89 green**: 3 vendor, 43 service, 42 contract,
  4 architecture. Coverage is 93.7% of lines and 86.0% of branches.
- **Best prompt:** AC step 4 (PIT). **Worst:** BD step 2, first version (it only checked the
  status code).
- **Something the agent did that we didn't expect:** in the smoke test, the author `alice` was
  accepted but stored as typed, not as the team's spelling `Alice`. No test caught it.

---

## Open points

- `decisions.md` still says *"proposal, drafted with AI… each decision still needs a ✅ from the
  team."* Confirm the decisions or be ready to defend them.
- The prompt log says Claude Code played both pairs on one machine. Be able to explain the answers
  above in your own words.
- No record yet of two Block 1 items: the "start a new chat and check that the house rules change
  the answer" test, and the optional "try a second tool" comparison.
