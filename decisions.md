# RetroFlow: team decisions

Our interpretation of everything `retroflow-brief.md` leaves open. When the brief and this file
disagree, the brief wins and this file gets fixed. When neither covers something, **flag it,
don't decide it silently**: add it here first.

Status: **proposal, drafted with AI in Block 1. Each decision still needs a ✅ from the team.**

## Ground rules

| #  | Decision | Why |
|----|----------|-----|
| G1 | A business rule violation returns **409 Conflict**. Validation failures return **400**, unknown ids return **404**. | StackCraft standard 4.1. The brief only says "appropriate". Confirmed by the team in Block 2 (BD step 1). |
| G2 | Business rules are enforced in the **service layer** and throw `RetroflowException` (or a subclass). The controller only validates the shape of the input. | Brief, "Definition of done". Standard 5.2. |
| G3 | Every error body is `{"error": "...", "status": 409, "timestamp": "..."}`, and every message is `[EntityType] [identifier]: [reason]`. A new resource that has no id yet uses `new` as its identifier (`Team new: members must not be empty`). An error that belongs to no entity uses `Request -:`. | Standards 2.1 and 2.2. The monitoring parses these messages. |
| G4 | We keep the starter's layer-first packages (`entity`, `service`, `controller`, ...). | The vendor tests import `...entity`, `...service` and `...exception`. Standard 1.1 grandfathers existing projects. The Block 3 audit will flag this. |
| G5 | The API never exposes entities. Requests and responses are Java records (DTOs). | This avoids lazy-loading and serialization surprises, and keeps the API stable when the entities change. |
| G6 | Authentication is out of scope (brief), so "any team member can create/close a retrospective" is **not enforced**. Requests don't say who is acting. | There is no trustworthy actor. A `createdBy` field would be honour-system only, and the vendor test `createRetrospective(teamId, title)` has no actor either. |

## Teams

| #  | Question the brief leaves open | Decision |
|----|--------------------------------|----------|
| T1 | Must team names be unique? | Yes, compared **case-insensitively** after trimming. A duplicate returns **409** (`Team new: a team named 'Alpha' already exists`). |
| T2 | Name length and blank names? | Required, not blank, max 100 characters. Otherwise **400**. |
| T3 | Can a member name be blank, or appear twice? | Not blank, max 100 characters. Duplicates within a team are compared case-insensitively ("Alice" = "alice") and return **400**. |
| T4 | Can members be changed after creation? | No. The brief only asks for create and retrieve. No update or delete endpoints for teams. |
| T5 | Can a person be a member of several teams? | Yes. Members are plain names, so there is no global identity. |
| T6 | Is there a "list all teams" endpoint? | Yes, `GET /api/teams` ("teams can be retrieved"). |

## Retrospectives

| #  | Question | Decision |
|----|----------|----------|
| R1 | Where does the date come from? | It is optional in the request. If it is missing, the server uses today's date. Past and future dates are both allowed. |
| R2 | Title rules? | Required, not blank, max 200 characters. Titles do not have to be unique. |
| R3 | What happens when a retro is created while the team already has an OPEN one? | **409** (`Team 3: already has an open retrospective (12)`). There is no auto-close. |
| R4 | What happens when an already CLOSED retro is closed again? | **409** (`Retrospective 12: already closed`). It is not a silent no-op. This matches the example in standard 3.3. |
| R5 | How is "cannot be reopened" enforced? | There is **no reopen operation at all**: no endpoint, no service method, and no `setStatus` on the entity (only `close()`). |
| R6 | Can retros or teams be deleted? | No. It isn't in the brief, and deleting would destroy the history the brief wants to keep. |
| R7 | Can a retro with open action items be closed? | Yes. Action items outlive their retro (see F6). |
| R8 | Concurrency: two requests creating an OPEN retro at the same moment? | The service checks for an open retro, then inserts. For today we lock the team row (`PESSIMISTIC_WRITE`) during creation. This is noted as a known limit, not a full guarantee. |
| R9 | Ordering of `GET /api/teams/{id}/retrospectives`? | Newest date first, then highest id. |

## Feedback items

| #  | Question | Decision |
|----|----------|----------|
| F1 | Must the author be a member of the retro's team? | **Yes.** An unknown author returns **400** (`Team 3: 'Zoe' is not a member`). Names are matched case-insensitively. |
| F2 | Content rules? | Required, not blank, max 2000 characters. |
| F3 | Which fields can be modified while the retro is OPEN? | `content` only. For action items, also `priority`. `type` and `author` are immutable. Changing the type would mean changing the entity class. |
| F4 | How are enum values accepted? | Exact uppercase names only (`WENT_WELL`). Anything else returns **400** and lists the allowed values. |
| F5 | Can an ACTION_ITEM be posted through the generic feedback endpoint? | No, that returns **400**. Action items have their own endpoint because they need a `priority`. |
| F6 | Is **completing** an action item a "modification" that a CLOSED retro forbids? | **No.** Completing is follow-up work that happens after the retro. Forbidding it would defeat the brief's goal ("whether action items were ever resolved"). Editing an action item's content or priority, or deleting it, **is** forbidden once the retro is closed. ⚠️ This is the most debatable decision: confirm it with the CTO. |
| F7 | `GET` feedback per retro: does it include action items? | Yes. It returns all items in creation order, and action items include `priority` and `completed`. |

## Action items

| #  | Question | Decision |
|----|----------|----------|
| A1 | What does completing an already-completed item do? | **409** (`ActionItem 15: already completed`). This is consistent with R4. |
| A2 | What does "uncomplete" do? | It exists as an endpoint so the rule is visible and testable. On a completed item it returns **409** (`ActionItem 15: already completed, cannot be undone`). On an item that is not completed it changes nothing and returns **200**. |
| A3 | What is the scope of "filtered by priority and completed status"? | Per team, across all of its retros: `GET /api/teams/{teamId}/action-items?priority=HIGH&completed=false`. Both filters are optional and can be combined. |
| A4 | Default priority? | None. `priority` is required when an action item is created. |

## Entity model

| #  | Decision | Why |
|----|----------|-----|
| E1 | `ActionItem extends FeedbackItem` with **`SINGLE_TABLE`** inheritance and a discriminator column `item_kind` (`FEEDBACK` / `ACTION`). | "Feedback items per retro" is a polymorphic query, and single table answers it with one query and no joins. Filtering action items stays a simple `WHERE`. The cost is that `priority` and `completed` must be nullable columns, so their NOT NULL is enforced by bean validation and Java instead of the database. `JOINED` would give a cleaner schema but needs a join on every read. `TABLE_PER_CLASS` makes polymorphic queries a `UNION`. |
| E2 | `type` stays a real column next to the discriminator. An `ActionItem` always has `type = ACTION_ITEM`, and a plain `FeedbackItem` never does. The constructors guarantee this. | The brief defines `type` as data. The discriminator alone can't express WENT_WELL vs NEEDS_IMPROVEMENT. |
| E3 | All relationships are **unidirectional `@ManyToOne(fetch = LAZY)`** (Retro → Team, Item → Retro). There are no `@OneToMany` collections. | Collections get queried through repositories. This avoids N+1 through accidental collection loading and avoids JSON cycles. |
| E4 | Team members are an `@ElementCollection` (`team_member` table), fetched **EAGER**, with an `@OrderColumn`. | Members are needed in almost every use case (author check, team response) and the list is small. The trade-off: `GET /api/teams` does one extra query per team. That is acceptable for 12 teams. |
| E5 | Status changes go through methods (`Retrospective.close()`, `ActionItem.markCompleted()`). There is no setter for `status` or `completed`. | The rules "cannot be reopened" and "cannot be uncompleted" can't be broken by accident through a setter. This deliberately differs from the "getters and setters for everything" in the Block 1 prompt. |
| E6 | Enums are stored as `STRING`. | Reordering an enum must not corrupt data. |

## Endpoint contract

Block 1 builds the first two groups (service bodies come in Block 2). The rest is the target for Block 2.

| Method | Path | Success | Errors |
|--------|------|---------|--------|
| POST | `/api/teams` | 201 + `Location` | 400, 409 (T1) |
| GET | `/api/teams` | 200 | |
| GET | `/api/teams/{id}` | 200 | 404 |
| POST | `/api/teams/{teamId}/retrospectives` | 201 + `Location` | 400, 404, 409 (R3) |
| GET | `/api/teams/{teamId}/retrospectives` | 200 | 404 |
| GET | `/api/retrospectives/{id}` | 200 | 404 |
| PUT | `/api/retrospectives/{id}/close` | 200 | 404, 409 (R4) |
| POST | `/api/retrospectives/{id}/feedback-items` | 201 | 400, 404, 409 (closed) |
| POST | `/api/retrospectives/{id}/action-items` | 201 | 400, 404, 409 (closed) |
| GET | `/api/retrospectives/{id}/feedback-items` | 200 | 404 |
| PUT | `/api/feedback-items/{id}` | 200 | 400, 404, 409 (closed) |
| PUT | `/api/action-items/{id}` (content + priority, F3, added in Block 2) | 200 | 400, 404, 409 (closed) |
| DELETE | `/api/feedback-items/{id}` | 204 | 404, 409 (closed) |
| PUT | `/api/action-items/{id}/complete` | 200 | 404, 409 (A1) |
| PUT | `/api/action-items/{id}/uncomplete` | 200 (no-op) | 404, 409 (A2) |
| GET | `/api/teams/{teamId}/action-items?priority=&completed=` | 200 | 400, 404 |

## Open questions (not decided, ask the CTO)

- F6: should completing an action item stay possible after the retro is closed? (We assume yes.)
- Do we need an audit trail of who closed a retro or completed an item, given that there is no auth?
- Should the H2 console stay enabled in the Docker image? (It is today, see README.)
