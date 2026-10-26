pu# Block 1 Prompt Ladders
Pair AB and Pair CD work in parallel, so choose what part each pair in your team works on.

These are structured prompts, not free-form suggestions. Work through them in order.
The goal of this block is not to finish as much as possible: it is to build prompting habits
that will make the rest of the day faster and safer.

After each prompt: read the output critically before using it. If something feels off, it probably is.

> Work in your IDE with the repo open. Copilot can see your project, so you don't have to paste code into the chat. Point it at a file instead (`#file:pom.xml` in VS Code, or attach the file with the `+` button in other IDEs). The more precisely you say what it should look at, the better the answer.

**Which tool?** The exercises are written for GitHub Copilot Chat in your IDE. No strong opinion? Use that. Want to use Claude Code, another model or another tool? Go ahead, as long as AI does the work. The cheat sheets in `ai-cheat-sheets/` show you how the main tools work.

**Two repos.** You use two repos today:

* **Course files** (these exercises, the brief, the StackCraft standards and the cheat sheets): https://github.com/switchfully/ai-track-ps-dev. You read from this one.
* **The code you work on**, the RetroFlow starter: https://github.com/switchfully/ai-track-ps-dev-retroflow. Your group works in a fork of this one. Everything you create today goes in that fork, and so does every commit.

"The root of the repo" in these exercises always means the root of your group's fork.

**How to read an exercise.** Every prompt below starts with the same three lines, so you always know where you stand:

* **Read:** the files the prompt works with. These already exist, you don't make them.
* **Create:** the file you make or change, and where it lives. If it says *nothing*, no file changes in this step.
* **Done when:** what has to be true before you move on to the next prompt.

Not sure what to do now? Look at these lines first. Still not sure after that? Ask your coach. It means the exercise isn't clear enough, not that you're doing it wrong.

**The files of the day.** Over the day your repo collects a few files that you make yourselves. You'll meet them in this order:

| File | Made by | In | What it is for |
|---|---|---|---|
| `decisions.md` | Pair AB | Block 1 | Your team's answers to everything the brief leaves open |
| `test-matrix.md` | Pair AB | Block 1 | One row for every business rule and operation. Block 2 starts from it |
| `README.md`, `Dockerfile`, `.github/workflows/ci.yml` | Pair CD | Block 1 | The project scaffolding |
| `.github/copilot-instructions.md` (or `CLAUDE.md`) | Both pairs | Block 1 | Your house rules. The AI reads this before every request |
| `prompt-log.md` | Every pair | Block 2 | Your prompts, with a one-line verdict each |
| `audit/audit-findings.md` | Pair AD | Block 3 | The audit of RetroFlow against StackCraft's standards |
| `review/pre-staging-review.md` | Pair BC | Block 3 | The review with a sign-off status |
| `.github/skills/<name>/SKILL.md` | Both pairs | Block 3 | The skill you build and swap |

Anything that is not in this table already exists in the starter repo.

---

## Pair AB: Domain model & REST API skeleton

You are building the entity model and the basic CRUD endpoints for RetroFlow.

---

### Prompt 1: Understand before you build

* **Read:** the RetroFlow brief (`retroflow-brief.md`, in the Github repo)
* **Create:** `decisions.md` in the root of the repo (new file)
* **Done when:** every ambiguity you found has a decision in `decisions.md` that you made yourselves

Open Copilot Chat. Paste the full contents of the RetroFlow brief and send this prompt:

```
I'm joining a project to finish a half-built Spring Boot backend called RetroFlow.
Here is the project brief:

[paste brief here]

Explain the domain to me as if I'm a new developer joining the team.
Then identify every ambiguity or missing detail you find in the requirements.
Be specific: don't just say "the requirements could be clearer."
```

**What to do with the output:**
- Go through every ambiguity Copilot identified. Are they real? Did it miss any?
- Discuss with your pair: how would you resolve each one?
- Write down your decisions in a file called `decisions.md` in the root of the repo.
  These are your team's interpretation of the spec. You'll refer back to them.

> **Watch for:** Copilot may confidently resolve ambiguities for you without flagging them.
> That's a risk. Make sure the decisions are yours, not the AI's defaults.

---

### Prompt 2: Entity model proposal

* **Read:** the brief and your `decisions.md`
* **Create:** nothing. This is a proposal, no file changes yet
* **Done when:** you know where the proposal differs from your decisions, and why

```
Based on this domain brief, propose a complete JPA entity model for RetroFlow in Java 21
with Spring Boot 3.x. Include:
- All fields with appropriate types and JPA annotations
- Relationships between entities with correct cardinality and fetch strategies
- Validation annotations where appropriate
- The FeedbackItem/ActionItem inheritance using an appropriate JPA strategy

For each design decision, explain why you made it: especially where you had a choice
between alternatives (e.g. inheritance strategy, fetch type, collection type).
```

**What to do with the output:**
- Compare the proposed entity model to your `decisions.md`: does it match your decisions?
- Check the inheritance strategy Copilot chose. Do you agree? What are the trade-offs of
  the alternative?
- Check the fetch strategies. Are lazy relationships going to cause N+1 problems anywhere?
- Do not copy-paste blindly. Adapt the output to match your team's decisions.

---

### Prompt 3: Implement the entities

* **Read:** `decisions.md` and the empty entity stubs in the `entity` package
* **Create:** the four entity classes. They replace the stubs that are already there
* **Done when:** `mvn compile` passes

Once you've agreed on the model:

```
Implement the following entities for RetroFlow as complete Java classes:
Team, Retrospective, FeedbackItem, ActionItem.

Requirements:
- Java 21 (you can use records for DTOs when appropriate, but entities should be classes)
- Spring Boot 3 / Jakarta EE annotations (not javax)
- Include all-args constructor, no-args constructor, getters and setters
- Do not use Lombok, write explicit code
- Follow this inheritance strategy: [insert your chosen strategy]
- Apply these validation constraints: [summarise your decisions]

Provide each class in full. No placeholders or TODOs.
```

**What to do with the output:**
- Replace the stub entities in the repo with the generated code
- Make sure the package declarations are correct (`com.stackcraft.retroflow.entity`)
- Verify the application still compiles: `mvn compile`

---

### Prompt 4: REST endpoints for Teams and Retrospectives

* **Read:** your entities and the placeholder `RetroflowController`
* **Create:** controllers and service classes for Teams and Retrospectives (new classes in the `controller` and `service` packages)
* **Done when:** the project compiles, and you can say for every endpoint which status codes it returns

```
Implement Spring Boot REST controllers for the following operations in RetroFlow.
Use a service layer: do not put business logic in the controller.

Operations:
- POST /api/teams: create a team (name + list of member names)
- GET /api/teams/{id}: get a team by id
- POST /api/teams/{teamId}/retrospectives: create a retrospective for a team
- GET /api/teams/{teamId}/retrospectives: list all retrospectives for a team
- PUT /api/retrospectives/{id}/close: close a retrospective

For each endpoint:
- Return the correct HTTP status code for success and for common error cases
- Validate all input: reject invalid requests with 400 and a meaningful message
- Throw an appropriate exception for business rule violations (do not return nulls)

Also generate the corresponding service class stubs with method signatures but no
implementation yet: we will implement the business logic separately.
```

**What to do with the output:**
- Check the HTTP status codes. Are they correct? (201 for creation, 404 for not found, etc.)
- Check the error handling: is it using @RestControllerAdvice or something else?
- Are the service method signatures sensible?

---

### Prompt 5: Interrogate the output

* **Read:** the whole codebase as it is now, and the brief
* **Create:** nothing. Write the list of rules that are not enforced yet in your notes
* **Done when:** for every business rule you know if it is enforced, where, and which ones still need a test

This is the most important prompt of the block. Do not skip it.

```
Review the REST API you just generated for RetroFlow against the following business rules
from the brief. For each rule, tell me:
1. Whether it is currently enforced in the code
2. Where exactly it is (or should be) enforced
3. What would happen if a client tried to violate it right now

Business rules to check:
- A team can only have one OPEN retrospective at a time
- A closed retrospective cannot be reopened
- Feedback items in a closed retrospective cannot be modified or deleted
- Completed action items cannot be uncompleted
```

**What to do with the output:**
- This is your quality gate. Anything missing needs to be added before the block ends.
- Note down which rules Copilot says are enforced, but you cannot verify in the code, those
  need a test before you trust them.

---

### Prompt 6: turn the rules into a test matrix

* **Read:** the brief and `decisions.md`
* **Create:** `test-matrix.md` in the root of the repo (new file)
* **Done when:** you counted the rows yourselves and the number matches what the brief asks for

Don't write any tests yet. First make sure you know exactly what has to be tested. You'll need this table in Block 2.

```
Based on the RetroFlow brief and our decisions.md, make a table of every business rule.
One row for every rule and for every operation it covers. For example, "feedback items in
a closed retrospective cannot be modified or deleted" is two rows (modify and delete), and
adding to a closed retrospective is a third.

Columns: rule, operation, situation, expected result, exception, HTTP status.
Use only the brief and decisions.md. If a row needs a decision we haven't written down,
flag it instead of choosing.
```

**What to do with the output:**
- Count the rows yourself. Four rules in the brief: how many rows should there be? Is anything missing?
- Check the HTTP status column against `decisions.md`. If the AI picked a status that you never decided, discuss it with your pair and write down your choice.
- Save the table as `test-matrix.md` in the root of the repo. Pair AC starts from it in Block 2.

---

## Pair CD: Project scaffolding & CI pipeline

You are setting up the project infrastructure: a clean build, a working Dockerfile,
and a CI pipeline that keeps the team honest.

---

### Prompt 1: Audit the existing setup

* **Read:** `pom.xml`, `application.properties` and the existing `README.md`
* **Create:** a corrected `README.md`. It replaces the one that is there
* **Done when:** the README says what is really configured

```
I have a Spring Boot 3.x Maven project. Look at #file:pom.xml and
#file:src/main/resources/application.properties.

The README says this project uses Gradle and PostgreSQL. The actual files suggest otherwise.
Audit this setup and tell me:
1. What is actually configured?
2. What the README got wrong?
3. Anything missing or misconfigured for a production-ready REST API?
4. What would you add or change?
```

**What to do with the output:**
- Fix the README first: it should reflect reality
- Note the improvement suggestions. Which ones are worth doing today?

---

### Prompt 2: Dockerfile

* **Read:** `pom.xml`
* **Create:** `Dockerfile` in the root of the repo (new file)
* **Done when:** the image builds, and you checked how the non-root user is set up

```
Write a production-quality Dockerfile for this Spring Boot Maven application.

Requirements:
- Multi-stage build (build stage + runtime stage)
- Minimize final image size: use a JRE, not a JDK, in the runtime stage
- Do not run the application as root
- The build stage should run `mvn package -DskipTests`
- The final image should expose port 8080
- Include a HEALTHCHECK instruction

After the Dockerfile, explain each instruction and why you made each choice.
Specifically address: why multi-stage? what are the security implications of running as root?
```

**What to do with the output:**
- Read the explanation, not just the Dockerfile
- Check: does the non-root user setup actually work? (Common mistake: creating the user but
  not assigning file ownership correctly)
- Save the Dockerfile to the repo root

---

### Prompt 3: GitHub Actions CI pipeline

* **Read:** `pom.xml`
* **Create:** `.github/workflows/ci.yml` (new file) and the JaCoCo configuration in `pom.xml` (an edit of the existing file)
* **Done when:** both exist, and you checked that the build really fails under 80% coverage

```
Write a GitHub Actions workflow for this Spring Boot Maven project.

The workflow should:
- Trigger on push to any branch and on pull requests to main
- Run on ubuntu-latest with Java 21 (use actions/setup-java with temurin distribution)
- Cache Maven dependencies to speed up builds
- Run `mvn verify` (compile + test)
- Fail the build if test coverage drops below 80% (use JaCoCo Maven plugin)
- On pull requests, post a comment with the test coverage percentage

Also add the JaCoCo plugin configuration to the pom.xml so coverage reporting works.

After generating the workflow, identify the top 3 security risks in CI/CD pipelines
and tell me whether this workflow has any of them.
```

**What to do with the output:**
- This is a good moment to actually think about the security question, not just copy the answer
- Check the JaCoCo config: does it actually fail the build on low coverage, or just report?
  (These are different configurations: Copilot often generates reporting without enforcement)
- Save the workflow to `.github/workflows/ci.yml`

---

### Prompt 4: Interrogate the pipeline

* **Read:** your `.github/workflows/ci.yml`
* **Create:** fixes in the same `ci.yml`. No new file
* **Done when:** the critical issues are fixed, and you can explain the security concern in your own words

```
Review the GitHub Actions workflow in #file:.github/workflows/ci.yml for common
misconfigurations.

Specifically check for:
1. Pinned vs unpinned action versions (security implications)
2. Whether secrets are handled correctly
3. Whether the workflow could be manipulated by a malicious pull request
   (look up "GitHub Actions pwn requests" if you're not familiar)
4. Any permissions that are broader than necessary

For each issue you find, suggest a fix.
```

**What to do with the output:**
- This prompt deliberately asks Copilot to audit something Copilot just generated
- See if it catches its own mistakes; it often does, which is a useful thing to demonstrate
- Apply at least the critical fixes before the block ends

---

### Prompt 5: Developer experience

* **Read:** the repo as it is now
* **Create:** a new `README.md`. It replaces the corrected one from Prompt 1
* **Done when:** every command in the README works when you run it

```
What else should this repository have for a developer joining the project for the first time?
Think about: local development setup, IDE configuration, how to run tests, how to test the API
manually, any tooling that would make the development loop faster.

Generate a proper README.md that replaces the existing (incorrect) one.
Include a "Getting Started" section that gets someone from a fresh clone to a running API
in under 5 minutes.
```

**What to do with the output:**
- A good README is a real deliverable, don't treat this as a throwaway task
- Check that the commands in the README actually work before committing it

---

## The last 15 minutes, with your whole team: write your house rules

* **Read:** the prompts of this morning that worked best
* **Create:** `.github/copilot-instructions.md` (or `CLAUDE.md` for Claude Code), in the shared repo
* **Done when:** the file is committed, it has the two required rules, and you saw it change an answer

By now you've probably typed "constructor injection", "no Lombok" or "explain your choice" more than once. That's annoying, and it's easy to forget one of them. So let's write them down once, in a place where the AI reads them before every request.

* Together with the other pair, write a short instructions file for this project: `.github/copilot-instructions.md` (10 to 20 lines). Using Claude Code? Call it `CLAUDE.md`. Use the lines from your prompts that made the biggest difference. You can let the AI draft it, but do edit it yourselves afterwards:

```
Based on the prompts we used this morning [paste 2 or 3 of your best prompts], write a
short set of standing instructions for an AI assistant working on this codebase. Cover:
the stack and versions, how we write code (injection, naming, error handling), how we
write tests, what it must always do before it says it's done (run `mvn test`), and what
it must never do. Keep it under 20 lines.
```

* Make sure it contains at least these two rules, in your own words: the AI never changes a test just to make it pass (it asks first), and it never adds a dependency without checking that it exists.
* Commit it to the shared repo. In Block 2 the agent reads this file automatically.
* Try it out. Start a new chat, and run one of your earlier prompts again, but this time without the extra rules. Does the answer still follow them?

---

## Finished early? Try a second tool

Take one of your best prompts and run it in a different tool or with a different model. Claude Code, another model in your IDE, whatever you have. Compare the two answers. What did one do that the other didn't? Write down your conclusion in two lines, you can share it in Retro 1.

---

## End of Block 1 checklist

This is also what we look at in Retro 1. Your coach asks one pair to show their result on the shared screen. Your team should be able to answer yes to:

**Pair AB:**
- [ ] `decisions.md` exists with your spec interpretation documented
- [ ] Entity stubs replaced with complete implementations
- [ ] Application compiles (`mvn compile` passes)
- [ ] Controller and service stubs exist for Teams and Retrospectives
- [ ] You've run Prompt 5 and know which business rules are not yet enforced
- [ ] `test-matrix.md` exists with a row for every rule and operation

**Pair CD:**
- [ ] README.md reflects reality
- [ ] Dockerfile exists and builds
- [ ] `.github/workflows/ci.yml` exists with JaCoCo coverage gate
- [ ] You can articulate the security concern from Prompt 4

**Both pairs together:**
- [ ] `test-matrix.md` and your instructions file are in the shared repo
- [ ] Code from both pairs is committed and merged (deal with any conflicts now)
- [ ] `mvn verify` runs without build errors (tests will fail, that's expected)
