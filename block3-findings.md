# Block 3 in plain English

A short version of what we did in Block 3, what we found, and what is still open, so we can line up
with the coach. The details and the proof are in `audit/audit-findings.md`,
`review/pre-staging-review.md` and `prompt-log.md`.

## What Block 3 was about

We connected the AI to StackCraft's real coding standards through an **MCP server**: a small service
the AI can call to read a document. Then we used it to check RetroFlow against those standards in
two ways:
- **Pair AD** did an **audit**: does the code follow the rules?
- **Pair BC** did a **review**: is the app ready for staging?

Finally, each pair turned its way of working into a **skill**: a set of instructions the AI only
loads when the request matches.

## 1. With and without the connector

We asked the same question three times: *what message format does StackCraft want for errors, and
which status code for a broken business rule?*

- **Connector off, AI allowed to read files:** it went looking on the disk by itself, found a copy
  of the standards and answered correctly. Switching the connector off doesn't mean the AI knows
  nothing.
- **Connector off, no file access:** it honestly said it didn't know StackCraft. Then it suggested
  "the usual" anyway: status 422 and a generic message format. **Both are wrong** for StackCraft.
  Someone skimming the answer would have copied the mistake.
- **Connector on:** it read the real document and quoted the two rules word for word: the format
  `Entity id: reason`, and **409** for a business rule. It also told us what the document *doesn't*
  answer.

**In one sentence:** without the source, the AI guesses something that sounds plausible. With the
source, it quotes the rule and you can check it.

## 2. The audit (Pair AD): does the code follow StackCraft's rules?

Mostly, yes. The service code is clean, and 11 of the rule sections had nothing wrong at all.

**Fixed:**
- One error message was worded in a way StackCraft's monitoring can't read. It's a safety check
  deep in the code that users can't reach today, but the rule is strict. Reworded.
- One important method was missing part of its documentation. Added.

Both fixes were made one at a time, with all tests passing after each, and no existing test changed.

**Not fixed, because the team has to decide:**
- **Test classes and "constructor injection".** The rule says Spring objects must be passed in
  through the constructor. A few of our *test* classes don't do that, and neither does the vendor's
  test file, which we may never edit. Does the rule apply to tests at all? The document doesn't
  say.
- **"Author is not a team member": 400 or 409?** We answer 400 ("bad input"). StackCraft says a
  broken *business rule* must be 409, but it never defines where bad input ends and a business rule
  begins. Since this check depends on data already in the database, it could be either. It's our
  decision (F1) to revisit.

**Backlog (not urgent):**
- Some input is checked later than the rules want (the allowed values for "type" and "priority").
- A few tests miss part of the naming pattern, an "Arrange" comment, or use hard-coded numbers.
- The project is organised by layer instead of by feature. The rules allow that for existing
  projects.

**Surprises:**
- **We made a mistake ourselves.** We first reported 6 test classes breaking the injection rule.
  Our own check script later showed that 5 of them were fine: we had searched for a keyword and
  never opened the lines. We corrected it. Lesson: check the AI's findings, and check your own too.
- **The standards document contradicts itself** on the package rule: the heading says "critical",
  the severity line says "major". Worth reporting to whoever owns the document.

## 3. The review (Pair BC): can it go to staging?

**Verdict: CONDITIONAL.** The app does what the brief asks. We started it and tried every business
rule for real (second open retro, closing twice, editing or deleting in a closed retro, undoing a
completed action item). All of them behave correctly.

**Must fix before staging:**
1. **The database console is open to anyone on the network, with no password.** Through it,
   someone could change data directly, for example reopen a closed retro, without going through
   any of our rules. We reproduced it.
2. **No automated test checks the rules through the real API.** Our tests check the services and
   the web layer separately, with fakes in between. Only our manual check proved the whole chain
   works. StackCraft's QA standard asks for at least one real end-to-end test per rule.

**Should fix:**
- If someone adds feedback at the exact moment a retro gets closed, the item could still slip in.
  Rare, and we only saw it in the code.
- An author typed as "alice" is accepted for the member "Alice", but stored as "alice". One person
  could show up as two in reports.

**What the AI got wrong (false positive):** it is tempting to flag "SQL injection" on our database
queries. That isn't real: the queries use parameters and never paste user text into the query.

**Also out of scope:** the brief says no login is needed, so "missing authentication" is not a
finding.

## 4. The skills and the swap

- Pair AD wrote `stackcraft-audit` and Pair BC wrote `stackcraft-tests`. Both tell the AI to read
  the standards through the MCP server instead of relying on memory.
- We tested each other's skill in a brand-new chat **without saying a skill existed**. Both times,
  the AI picked the right skill on its own from its description alone.
- The tests skill wrote 4 tests for our fix, and showed that they fail when the fix is undone.
  It also refused to copy a bad pattern from the other pair's example file.
- The audit skill found 15 small things our own audit missed. It also found 7 places where the
  standard itself is unclear.
- **Comparing the two:** the audit finds rule breaches, line by line. The review finds what
  actually breaks in production (the open database console, the missing end-to-end tests), because
  it runs the app. You need both.

## Where we ended up

- **93 tests, all green.** Coverage is 94% of lines and 88% of branches (the minimum is 80/70).
- Pull request #4 (`block3` → `block2`), with CI green.

## Questions to settle with the coach and the team

1. Do the injection rules apply to test code too?
2. "Author is not a member": 400 or 409?
3. Is it OK to go to staging once the database console is closed and the end-to-end tests are
   added? That's what "CONDITIONAL" means.
4. Completing an action item after its retro is closed (F6) is still waiting for the CTO.
5. `decisions.md` is still marked "proposal". Every decision needs the team's ✅.
