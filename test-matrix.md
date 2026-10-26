# RetroFlow test matrix

One row per rule × operation. Built only from `retroflow-brief.md` and `decisions.md`. Block 2 writes one test per row.

- **Exception**: `RetroflowException` (409), and its subclasses `NotFoundException` (404) and `InvalidInputException` (400). Bean validation failures (400) are raised by the web layer.
- **Message** follows standard 2.1: `[EntityType] [identifier]: [reason]`. Assert with `hasMessageContaining` on the reason part.
- **Source**: `B` = stated in the brief, `D-xx` = our decision in `decisions.md`.
- 🟢 = one of the three vendor tests in `RetroflowBusinessRulesTest` already covers this row.

## 1. The four core business rules (the ones from Block 1 Prompt 5)

| #  | Rule | Operation | Situation | Expected result | Exception | HTTP | Source |
|----|------|-----------|-----------|-----------------|-----------|------|--------|
| 1  | One OPEN retro per team | create retrospective | team already has an OPEN retro | rejected, nothing saved. Message `Team {id}: already has an open retrospective ({retroId})` | `RetroflowException` | 409 | B, D-R3 🟢 |
| 2  | One OPEN retro per team | create retrospective | all earlier retros of the team are CLOSED | created as OPEN | – | 201 | B |
| 3  | One OPEN retro per team | create retrospective | **another** team has an OPEN retro | created. The rule applies per team | – | 201 | B |
| 4  | Closed retro cannot be reopened | close retrospective | retro is already CLOSED | rejected, status stays CLOSED. Message `Retrospective {id}: already closed` | `RetroflowException` | 409 | B, D-R4 |
| 5  | Closed retro cannot be reopened | reopen (any route) | retro is CLOSED | impossible by design: no endpoint, no service method, no status setter. Test: `PUT /api/retrospectives/{id}/reopen` → 404, and status is still CLOSED | – | 404 | D-R5 |
| 6  | Feedback in CLOSED retro is frozen | add feedback item | retro is CLOSED | rejected, nothing saved. Message `Retrospective {id}: cannot add feedback to a closed retrospective` | `RetroflowException` | 409 | B 🟢 |
| 7  | Feedback in CLOSED retro is frozen | add action item | retro is CLOSED | rejected, nothing saved | `RetroflowException` | 409 | B |
| 8  | Feedback in CLOSED retro is frozen | modify feedback item | retro is CLOSED | rejected, content unchanged. Message `FeedbackItem {id}: cannot modify an item of a closed retrospective` | `RetroflowException` | 409 | B, D-F3 |
| 9  | Feedback in CLOSED retro is frozen | delete feedback item | retro is CLOSED | rejected, item still exists. Message `FeedbackItem {id}: cannot delete an item of a closed retrospective` | `RetroflowException` | 409 | B |
| 10 | Feedback in CLOSED retro is frozen | modify action item (content/priority) | retro is CLOSED | rejected, unchanged | `RetroflowException` | 409 | B, D-F3 |
| 11 | Feedback in CLOSED retro is frozen | delete action item | retro is CLOSED | rejected, item still exists | `RetroflowException` | 409 | B |
| 12 | Feedback in CLOSED retro is frozen | complete action item | retro is CLOSED, item not completed | **allowed**: `completed = true` | – | 200 | D-F6 ⚠️ |
| 13 | Feedback in CLOSED retro is frozen | modify feedback item | retro is OPEN | content updated | – | 200 | B |
| 14 | Feedback in CLOSED retro is frozen | delete feedback item | retro is OPEN | item removed | – | 204 | B |
| 15 | Completed action item cannot be uncompleted | uncomplete action item | item is completed | rejected, stays completed. Message `ActionItem {id}: already completed, cannot be undone` | `RetroflowException` | 409 | B, D-A2 🟢 |
| 16 | Completed action item cannot be uncompleted | complete action item | item is already completed | rejected. Message `ActionItem {id}: already completed` | `RetroflowException` | 409 | D-A1 |
| 17 | Completed action item cannot be uncompleted | uncomplete action item | item is not completed | no change, returns the item | – | 200 | D-A2 |
| 18 | Completed action item cannot be uncompleted | complete action item | item is not completed, retro OPEN | `completed = true` | – | 200 | B |

## 2. Other rules from the brief (validation and lookups)

| #  | Rule | Operation | Situation | Expected result | Exception | HTTP | Source |
|----|------|-----------|-----------|-----------------|-----------|------|--------|
| 19 | Team needs ≥ 1 member | create team | `members` empty or missing | rejected. Message `Team new: members must not be empty` | bean validation (web), `InvalidInputException` (service) | 400 | B |
| 20 | Team needs a name | create team | `name` blank or > 100 chars | rejected | bean validation / `InvalidInputException` | 400 | B, D-T2 |
| 21 | Member names valid | create team | a member name is blank | rejected | bean validation / `InvalidInputException` | 400 | D-T3 |
| 22 | Member names unique | create team | `["Alice", "alice"]` | rejected | `InvalidInputException` | 400 | D-T3 |
| 23 | Team names unique | create team | name exists already (any case) | rejected | `RetroflowException` | 409 | D-T1 |
| 24 | Retro needs a title | create retrospective | `title` blank or > 200 chars | rejected | bean validation | 400 | B, D-R2 |
| 25 | Retro date defaults | create retrospective | no `date` given | created with today's date | – | 201 | D-R1 |
| 26 | Retro belongs to a team | create / list retrospectives | team id does not exist | `Team {id}: not found` | `NotFoundException` | 404 | B |
| 27 | Feedback type is one of 3 | add feedback item | type is unknown (`"GREAT"`, `"went_well"`) | rejected, lists the allowed values | `InvalidInputException` | 400 | B, D-F4 |
| 28 | Action items need priority | add feedback item | type `ACTION_ITEM` via the feedback endpoint | rejected, points to the action-items endpoint | `InvalidInputException` | 400 | D-F5 |
| 29 | Priority is LOW/MEDIUM/HIGH | add action item | priority unknown or missing | rejected | `InvalidInputException` | 400 | B, D-A4 |
| 30 | Author is a team member | add feedback / action item | author not in the team | rejected. Message `Team {id}: 'Zoe' is not a member` | `InvalidInputException` | 400 | D-F1 |
| 31 | Feedback needs content | add feedback / action item | content blank or > 2000 chars | rejected | bean validation / `InvalidInputException` | 400 | B, D-F2 |
| 32 | Lookups | get team / retro / item, close, complete, modify, delete | id does not exist | `{Entity} {id}: not found` | `NotFoundException` | 404 | B |
| 33 | Retrieve feedback per retro | list feedback items | retro has items of all 3 types | all items, action items with `priority`/`completed` | – | 200 | B, D-F7 |
| 34 | Filter action items | list action items of a team | `priority=HIGH`, `completed=false`, both, or neither | only matching items, across all the team's retros | – | 200 | B, D-A3 |
| 35 | Filter action items | list action items of a team | `priority=URGENT` | rejected | bean/type validation | 400 | D-F4 |

## Count

- **Section 1:** 4 rules → **18 rows**: 1 → 3, 2 → 2, 3 → 9, 4 → 4. Of those, 11 rows are violations and 7 are "allowed" boundaries.
- **Section 2:** 17 rows.
- **Total: 35 rows.** The vendor tests cover 3 of them (rows 1, 6, 15).

## Flagged: rows that rest on a decision the brief does not make

- Row 5: what "cannot be reopened" means when no reopen operation exists. We test that the operation is absent.
- Row 12 ⚠️: completing an action item in a closed retro (D-F6). This is the most likely to be challenged.
- Rows 16 and 17: completing twice is a 409, and uncompleting an open item is a no-op (D-A1, D-A2).
- Rows 22, 23 and 30: uniqueness and membership checks are ours, not the brief's.
