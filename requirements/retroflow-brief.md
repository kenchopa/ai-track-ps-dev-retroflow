# RetroFlow: Project Brief

**Client:** StackCraft (internal tooling team)
**Issued to:** Development team
**Version:** 1.0

---

## Background

StackCraft is a mid-sized software consultancy with 12 agile development teams. They currently run retrospectives using a mix of sticky notes, Miro boards, and shared Google Docs. There is no central record of what was discussed, what action items were created, or whether those action items were ever resolved.

A previous vendor was contracted to build RetroFlow, a lightweight internal tool to solve this. They delivered a partial Spring Boot backend before the contract ended. The codebase compiles. That is about all that can be said for it.

Your team has been brought in to finish the job. The CTO will do a brief demo review at the end of the day.

---

## What RetroFlow needs to do

RetroFlow is a backend REST API. There is no frontend in scope for today. The API must support the following:

### Teams

- A **team** has a name and a list of members (stored as names, no authentication required)
- Teams can be created and retrieved
- A team must have at least one member

### Retrospectives

- A **retrospective** belongs to a team
- A retrospective has a title, a date, and a status: `OPEN` or `CLOSED`
- Any team member can create a retrospective for their team
- A retrospective can be closed by any team member
- **Once a retrospective is closed, it cannot be reopened**
- A team can have multiple retrospectives, but only one can be `OPEN` at a time

### Feedback items

- A **feedback item** belongs to a retrospective
- A feedback item has content (text), a type (`WENT_WELL`, `NEEDS_IMPROVEMENT`, `ACTION_ITEM`), and the name of the team member who submitted it
- Feedback items can be added to an `OPEN` retrospective
- **Feedback items in a `CLOSED` retrospective cannot be modified or deleted**
- Feedback items can be retrieved per retrospective

### Action items

- An **action item** is a special kind of feedback item (type `ACTION_ITEM`) that additionally tracks a `priority` (`LOW`, `MEDIUM`, `HIGH`) and a boolean `completed` flag
- Action items can be marked as completed
- **Completed action items cannot be uncompleted**
- Action items must be retrievable filtered by priority and by completed status

---

## Technical constraints

- Java 21, Spring Boot 3.x
- Spring Data JPA with an in-memory H2 database (no persistence required between restarts)
- Maven or Gradle, your choice
- No authentication or security layer required (assume all users are trusted internal users)
- All endpoints must return appropriate HTTP status codes
- Input validation is required: invalid requests must return a `400` with a meaningful error message
- A reasonable level of test coverage is expected: aim for the happy path plus the key business rules

---

## What the previous vendor left behind

The starter repository contains:

- A working Spring Boot application shell
- Empty entity stubs for `Team`, `Retrospective`, `FeedbackItem`, and `ActionItem`
- Empty repository interfaces
- A placeholder `RetroflowController` with no implemented methods
- A broken test file with three failing tests (they reference methods that don't exist yet)
- A `README.md` that is mostly wrong

Your job is to make this work. Use AI to get there faster, but you are responsible for what ships.

---

## The things the brief does not tell you

Good software development means asking questions. If something is ambiguous, surface it. Some ambiguities here are intentional. How you handle them matters.

---

## Definition of done for today

By the showcase at the end of Block 3 (15:15), your team should have:

- [ ] All entities implemented with correct relationships
- [ ] REST endpoints for all operations described above
- [ ] Business rules enforced at the service layer (not just the controller)
- [ ] Input validation on all endpoints
- [ ] At least the following tested: retrospective closure rule, feedback item modification rule, action item completion rule
- [ ] The application starts and the endpoints respond correctly

---

*StackCraft CTO quote from the kickoff call:*
> "I don't care how you build it. I care that the rules are right. We've had too many tools where the happy path works and everything else is a mess."
