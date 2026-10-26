# Eval: the AI retrospective summary

"It looked fine when I tried it" isn't a test. This folder measures whether
`POST /api/retrospectives/{id}/summary` still works after a change to the prompt, the model or the
code around it.

## Run it

```bash
export OPENAI_API_KEY=...                          # never in a file
./mvnw test -Peval -Deval.label=my-change          # or with Docker, see below
./mvnw test -Peval -Dtest=JudgeEvalTest            # check the judge itself
```

With Docker (no local JDK):
`docker run --rm -e OPENAI_API_KEY -v "$PWD":/workspace -w /workspace maven:3.9-eclipse-temurin-21 mvn -B test -Peval -Deval.label=my-change`.
The key is passed by name, so it never appears in your shell history.

Options:
- `-Dretroflow.summary.model=gpt-4.1-nano`
- `-Dretroflow.summary.temperature=0`
- `-Deval.judge.model=gpt-4.1`

It prints one score and the cost, and writes `eval/results/<label>.json` with every check, the
judge's reasons and the summaries themselves.

Tests tagged `eval` are excluded from `mvn verify` and CI (see `pom.xml`). They cost money and they
are noisy.

## What it checks

`summary-cases.json` has 13 cases. 7 are tricky on purpose:
- an empty retro
- prompt injection, plain and as fake JSON
- an item of almost 2000 characters
- Dutch feedback
- names of colleagues
- quotes, backslashes, emoji and markdown

| Check | Done by | Why |
|---|---|---|
| valid answer, open action items exact | **code** (`SummaryValidator`, in the app itself) | structure is decidable |
| section mentions a key word | **code** (`EvalChecks`) | "the CI problem is in needsImprovement" is a substring question |
| never says a forbidden word (names, "no problems") | **code** | same |
| at most N points (duplicates merged) | **code** | a count |
| written in Dutch | **code**, crude stop-word check | good enough to catch an English answer |
| faithful: every point backed by the feedback | **model** (`Judge`, rubric in `judge-prompt.txt`) | "adds a cause that isn't there" can't be done with a regex |

The judge only judges faithfulness: no invented facts, numbers, causes, people or flipped
sentiment. Coverage, style and language are left to the code checks. `JudgeEvalTest` checks the
judge itself on 8 planted summaries with a known verdict (`judge-cases.json`).

## Results (2026-10-26)

Score = passed checks / all checks, over 50 checks.

| Run | One change | Score | Cost per run |
|---|---|---|---|
| baseline (3 runs) | none | 94.0 / 96.0 / 94.0 | $0.0183 (EUR 0.0163) |
| `3-prompt-v2`, `4-prompt-v2-again` | **prompt**: "never name people" with an example, "same topic = one point" | **98.0 / 98.0** | $0.0185 |
| `5-mini-judge`, `6-mini-judge-again` | **judge model** gpt-4.1 → gpt-4.1-mini (cost) | 98.0 / 96.0 | **$0.0069 (EUR 0.0061), −63%** |

- **Noise:** about ±2 points between unchanged runs. The duplicates case flips by itself. A change
  smaller than that is no improvement.
- **The prompt change is real:** two runs at 98 against three at 94–96, and the same two failures
  (names, duplicates) were fixed in both.
- **Where the money goes:** 13 summaries cost about 7.3k input and 0.7k output tokens on
  gpt-4.1-mini ($0.004). The judge was 79% of the cost until it moved to gpt-4.1-mini.
- **Prices:** OpenAI pricing page on 2026-10-26, gpt-4.1 $2.00/$8.00, gpt-4.1-mini $0.40/$1.60 per
  1M tokens in/out. EUR at the ECB rate of 2026-10-23 (1 EUR = 1.1193 USD).

## Do we trust the judge?

Partly, and that's why `JudgeEvalTest` exists.
- **Both judges get 8/8 on the planted cases** (changed number, invented cause, flipped sentiment,
  invented person, invented action, and three faithful rephrasings).
- **gpt-4.1 rejected a fair rephrase on a real case.** It turned down "Standups were effective"
  for "Standups were great", but accepted the same kind of rephrase in isolation. Context changes
  its verdict.
- **gpt-4.1-mini broke its own rubric once.** It failed the prompt-injection case for *leaving
  out* the injected item, although the rubric says not to judge coverage. In another case it
  correctly caught "causing disruption", which the gpt-4.1 judge had let through.
- **So:** the judge adds roughly the same noise as the model it judges. Read its reasons on every
  failure before acting on a score. Rerun `JudgeEvalTest` whenever the judge model or the rubric
  changes.

## In CI?

- **Every push:** only the free, deterministic tests (`mvn verify`: stub model, mock HTTP
  server, contract tests).
- **The eval:** `.github/workflows/eval.yml` runs **only by hand** (`workflow_dispatch`): before a
  release, and after any change to the prompt, the model or the summary code. It needs the repository
  secret `OPENAI_API_KEY`. Never on pull requests from forks, where secrets aren't available anyway.
