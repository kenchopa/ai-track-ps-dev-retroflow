# RetroFlow: Business-Rule Enforcement Review

## Scope and overall finding

This review describes the current source code, assuming the application is running that version. It distinguishes entity-level guards, service-layer enforcement, and actual HTTP behavior.

**None of the four rules is fully enforced end to end by an implemented service workflow.** The services were intentionally generated as stubs. Some entity mutations have defensive guards, but those are not equivalent to enforcing the rules for every persistence operation.

Valid HTTP requests reaching the real service stubs currently fail with `UnsupportedOperationException`. [GlobalExceptionHandler.handleUnexpected](src/main/java/com/stackcraft/retroflow/exception/GlobalExceptionHandler.java#L86) logs the exception and returns:

```json
{
  "error": "Request: an unexpected server error occurred",
  "status": 500,
  "timestamp": "<current UTC instant>"
}
```

That failure prevents the requested operation from happening, but it is **not business-rule enforcement**: an allowed request fails the same way.

## Summary

| Business rule | Currently enforced? | Existing enforcement or gap | What an HTTP client would see now |
|---|---|---|---|
| Only one `OPEN` retrospective per team | **No.** | No service lookup, lock, or database uniqueness mechanism implements it. | A valid creation request returns `500`, whether or not the team already has an open retrospective. No retrospective is created. |
| A closed retrospective cannot reopen | **Partially: entity guard only.** | `Retrospective.setStatus()` rejects `CLOSED -> OPEN`. No reopen endpoint exists; services do not implement lifecycle workflows. | An attempted `PUT /api/retrospectives/{id}/reopen` returns `404` because there is no route, not `409` for a checked transition. |
| Closed feedback cannot be modified or deleted | **Partially for edits; not for repository deletion.** | Content/submitter/priority setters check the parent status. Repository deletion has no corresponding status guard. | Feedback update/delete routes do not exist, so requests to proposed feedback paths return `404`. |
| Completed action items cannot be uncompleted | **Partially: entity guard only.** | `ActionItem.setCompleted(false)` rejects reversal on an already completed instance. The service method is still a stub. | There is no action-item update/uncomplete route; requests to those proposed paths return `404`. |

For an existing route with the wrong HTTP verb, Spring may instead return `405`. The `404` examples below use paths that are not registered at all.

## 1. A team can only have one OPEN retrospective at a time

### Is it enforced?

**No.**

Both [RetrospectiveService.createRetrospective overloads](src/main/java/com/stackcraft/retroflow/service/RetrospectiveService.java#L27) immediately throw `UnsupportedOperationException`; neither examines the team's existing retrospectives.

[RetrospectiveRepository](src/main/java/com/stackcraft/retroflow/repository/RetrospectiveRepository.java) only extends `JpaRepository`. There is no open-retrospective lookup or locking query.

The [team/status index](src/main/java/com/stackcraft/retroflow/entity/Retrospective.java#L18) is a normal, non-unique index. It improves potential lookup performance but imposes no business constraint. Creating multiple `Retrospective` objects for the same team is also not prevented by the entity constructors.

### Where should it be enforced?

In `RetrospectiveService.createRetrospective(...)`, inside a transaction:

1. Locate the team and reject an absent team with `ResourceNotFoundException` (`404`).
2. Serialize concurrent creations for that team, for example by acquiring a pessimistic write lock on the team row.
3. Check whether an `OPEN` retrospective already exists.
4. If one exists, throw `RetroflowException`, such as `Team 7: already has an open retrospective` (`409`).
5. Otherwise create and save the new open retrospective.

Every creation entry point, including the inherited two-argument overload, must use the same enforcement path. Closure must participate in a coherent transaction/locking strategy if it runs concurrently with creation.

A check followed by an insert without concurrency control is insufficient: two requests could both pass the check. `@Version` on an otherwise unchanged team row does not automatically protect child inserts.

Do **not** add a unique constraint on `(team_id, status)`: that would incorrectly forbid multiple closed retrospectives for the same team. A database-level backstop would require a design that specifically constrains open rows.

### What happens if a client violates it now?

Example:

```http
POST /api/teams/7/retrospectives
Content-Type: application/json

{
  "title": "Another open retrospective",
  "date": "2026-10-26"
}
```

After boundary validation, [RetrospectiveController.createRetrospective](src/main/java/com/stackcraft/retroflow/controller/RetrospectiveController.java#L24) calls the stub. The response is the sanitized `500` shown above; the code never checks whether team 7 exists or already has an open retrospective.

A caller using repositories directly can currently persist multiple valid open retrospective entities for the same team. The mapping alone does not stop it.

## 2. A closed retrospective cannot be reopened

### Is it enforced?

**Yes for a transition through the existing entity setter; no complete service workflow exists.**

[Retrospective.setStatus](src/main/java/com/stackcraft/retroflow/entity/Retrospective.java#L121) checks the current state and throws `RetroflowException` if an already closed instance is assigned `OPEN`:

```text
Retrospective 42: cannot reopen a closed retrospective
```

`Retrospective.close()` delegates to that setter with `CLOSED`. Repeatedly assigning `CLOSED` does not throw at the entity level.

There is no reopen endpoint or general retrospective-update endpoint. The create request DTO accepts only `title` and `date`, not `status`.

### Where should it be enforced?

The entity guard should remain as defense in depth. Services must load the existing persisted retrospective and validate lifecycle transitions before saving changes.

The current close service must implement loading, existence checking, closure, and persistence. If a future update/reopen operation is added, it must explicitly reject reopening with `RetroflowException` (`409`), not overwrite the state using a newly constructed entity carrying the same ID.

JPA uses field access in this model. Entity setters do not automatically inspect database history during merge, bulk updates, or native SQL. There is no database transition constraint or callback enforcing this rule independently.

### What happens if a client violates it now?

- `PUT /api/retrospectives/42/reopen` returns `404`: no such route exists.
- Sending an extra `status` field in the retrospective creation JSON returns `400` because unknown request fields are rejected. This is request-shape validation, not validation against a previously closed record.
- `PUT /api/retrospectives/42/close` cannot reopen anything. It currently reaches the unimplemented closure service and returns `500`. Its controller does not bind a status body; adding `{"status":"OPEN"}` does not turn it into a reopen operation.

A direct Java call to `closedRetrospective.setStatus(OPEN)` throws the domain exception before changing the state. If such an exception propagates through an HTTP controller, [handleBusinessRule](src/main/java/com/stackcraft/retroflow/exception/GlobalExceptionHandler.java#L30) maps it to `409`. No existing HTTP reopen route exercises that guard.

## 3. Feedback items in a closed retrospective cannot be modified or deleted

### Is it enforced?

**Partially for domain edits; not for deletion.**

Existing edit protections:

- [FeedbackItem.setContent](src/main/java/com/stackcraft/retroflow/entity/FeedbackItem.java#L101) and [setSubmittedBy](src/main/java/com/stackcraft/retroflow/entity/FeedbackItem.java#L110) call `requireEditable()`.
- [FeedbackItem.requireEditable](src/main/java/com/stackcraft/retroflow/entity/FeedbackItem.java#L115) rejects edits when the associated retrospective is closed.
- [ActionItem.setPriority](src/main/java/com/stackcraft/retroflow/entity/ActionItem.java#L43) uses the same edit guard.
- [Retrospective.addFeedbackItem](src/main/java/com/stackcraft/retroflow/entity/Retrospective.java#L148) additionally rejects new feedback after closure.

The edit exception is:

```text
FeedbackItem 42: cannot modify feedback in a closed retrospective
```

The parent collection getter is unmodifiable, and `setFeedbackItems()` refuses to remove existing children. Those protections do not prevent deleting a feedback row through its repository.

[FeedbackItemRepository](src/main/java/com/stackcraft/retroflow/repository/FeedbackItemRepository.java) inherits `delete`, `deleteById`, and batch deletion operations from `JpaRepository`, with no parent-status check. There is no deletion callback or database constraint protecting closed feedback.

There is also an unresolved interpretation: `ActionItem.setCompleted()` does not check retrospective status. Since actions are feedback, completion is technically a modification. Whether completion after closure is an allowed exception remains a product decision in `decisions.md`; the broad freeze cannot be claimed as fully implemented.

### Where should it be enforced?

Future feedback update and deletion service methods must:

1. Load the feedback and its parent retrospective.
2. Return `404` for a missing resource.
3. Check the persisted parent status and reject closed feedback with a formatted `RetroflowException` (`409`).
4. Apply the change or call repository deletion only if allowed.

The status check and mutation must be transactionally coordinated with retrospective closure, for example by locking the parent row in both workflows. Otherwise closure can race with a previously approved edit/delete.

Entity guards should remain, but deletion cannot rely on setter invocation. Direct repository, bulk, and native mutations must not become alternate application paths that bypass service rules.

### What happens if a client violates it now?

There are no feedback update or delete controllers. For example, `PUT /api/feedback-items/42` and `DELETE /api/feedback-items/42` return `404`; no feedback-status rule is evaluated.

A direct setter call on existing closed feedback rejects content, submitter, or priority changes. In contrast, a direct repository deletion of a feedback row is not rejected on the basis of its retrospective being closed.

The existing feedback-addition service is also only a stub. Calling it directly throws `UnsupportedOperationException`, not a closed-retrospective domain exception. It has no exposed HTTP route in this task.

## 4. Completed action items cannot be uncompleted

### Is it enforced?

**Yes on an existing entity's completion setter; no implemented service/API workflow exists.**

[ActionItem.setCompleted](src/main/java/com/stackcraft/retroflow/entity/ActionItem.java#L52) throws when the current flag is true and the requested value is false:

```text
ActionItem 42: already completed, cannot be undone
```

The assignment occurs only after the guard, so the instance stays completed. `complete()` delegates to `setCompleted(true)`.

[RetrospectiveService.uncompleteActionItem](src/main/java/com/stackcraft/retroflow/service/RetrospectiveService.java#L121) does not load an action or call the guarded setter. It immediately throws `UnsupportedOperationException`. The completion service is likewise unimplemented.

The feedback table's `@Check` ensures that action-specific fields are present for action rows; it does not compare old and new completion values. `@Version` detects conflicting row updates, not forbidden state transitions by itself.

### Where should it be enforced?

Any service path that can change completion must load the existing action and apply the transition against its persisted state. An attempted `true -> false` must throw `RetroflowException` and map to `409` if exposed through HTTP.

Retain the entity guard. Do not create a replacement entity with `completed = false` and merge it over an existing completed action: field-based merge is not a substitute for the guarded transition. Concurrent completion/update operations also need appropriate transactional/version handling.

Whether an uncomplete REST endpoint should exist at all is still unresolved. If none is required, keep it absent, but still prevent reversal through any future general action-update operation.

### What happens if a client violates it now?

No action update or uncompletion endpoint is registered. For example, `PUT /api/action-items/42/uncomplete` returns `404`.

A direct Java call to `completedAction.setCompleted(false)` throws the domain exception and preserves `true`. Calling `RetrospectiveService.uncompleteActionItem(42L)` instead throws the stub's `UnsupportedOperationException`; if that service were invoked from an HTTP route unchanged, the existing handler would return `500`, not `409`.

## Interpretation of existing tests

Controller tests that mock a service to throw `RetroflowException` establish that the HTTP layer maps that exception to `409`. They do not establish that the real service detects the forbidden situation.

Entity tests cover guarded setters and some persistence behavior, but they do not prove that every repository operation enforces the rules. The inherited business-rule tests still fail against intentionally unimplemented services.

**Bottom line:** distinguish "the route does not exist," "the service fails because it is a stub," and "the application evaluated the persisted state and rejected a business-rule violation." Only the last is end-to-end business-rule enforcement.
