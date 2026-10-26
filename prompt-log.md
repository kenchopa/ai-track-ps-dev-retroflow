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
