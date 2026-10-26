# RetroFlow Business-Rule Test Matrix

## Sources and interpretation

Only these documents define this matrix:

- [RetroFlow Project Brief v1.0](requirements/retroflow-brief.md): Teams, Retrospectives, Feedback items, Action items, technical constraints, and definition of done.
- [decisions.md](decisions.md): domain explanation, confirmed engineering/API requirements, and unresolved questions in sections 1-9.

This is a requirements matrix, not a description of current implementation behavior. No decisions are imported from source code, previous prompt documents, or proposed implementations.

**DECISION REQUIRED** means the allowed sources do not settle that detail. It is not permission to pick an arbitrary expected result when writing the test.

Conventions:

- Each row covers one operation and situation. Separate rows distinguish adding, modifying, deleting, completing, and uncompleting.
- Unless a row states otherwise, all other inputs, resources, membership conditions, and business prerequisites are valid. An allowed operation is not unconditional permission to ignore another rule.
- `None` means no exception is expected. `Validation exception`, `Business-rule exception`, and `Not-found exception` are categories, **not selected Java class names**. The allowed documents do not establish concrete exception types.
- All service exceptions must follow `EntityType identifier: reason`. Exact wording and the identifier for unsaved entities remain undecided.
- Confirmed HTTP categories are `201` creation, `200` read, `200` update with body, `204` update without body/deletion, `400` validation, `404` missing resource, `409` business conflict, and `500` unexpected failure.
- `200/204 - DECISION REQUIRED` means the status depends on the unresolved update response-body contract, not that either status may be returned arbitrarily.
- An operation marked as having undecided scope is not a requirement to add an endpoint. Its domain restriction still applies if that operation is supported.
- For enum-set rows, run the accepted-value test for each listed value. Run the closure restrictions for ordinary feedback and action feedback where applicable, except for explicitly unresolved action-completion behavior.

## 1. Teams and membership

Sources: brief "Teams"; decisions domain table and ambiguity sections 1-2.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
|---|---|---|---|---|---|
| Teams can be created | Create team | A name and at least one valid member name are supplied | Create a team containing the supplied information | None | 201 |
| A team must have at least one member | Create team | Member list is empty | Reject; do not create the team | Validation exception; Java type unspecified | 400 |
| A team must have at least one member | Create team | Member list is missing or null, with no member information | Reject a team with no members; exact request schema/error wording needs definition | Validation exception; Java type unspecified | 400 |
| Members are stored as names | Create team | List contains null or whitespace-only entries, such as `[null]` or `[" "]` | **DECISION REQUIRED:** what constitutes a valid member name is explicitly unresolved | **DECISION REQUIRED:** validation contract | **DECISION REQUIRED:** 400 if these entries are invalid |
| A team has a name | Create team | Name is missing, null, blank, or over a proposed maximum | **DECISION REQUIRED:** request-requiredness, blank handling, normalization, and maximum length are not specified | **DECISION REQUIRED:** validation contract | **DECISION REQUIRED:** 400 for cases declared invalid |
| Team-name uniqueness is not specified | Create team | Another team has the same name, including a case-only variant | **DECISION REQUIRED:** uniqueness and case sensitivity | **DECISION REQUIRED** | **DECISION REQUIRED:** 201 if allowed; 409 if defined as a business conflict |
| Member identity is name-based | Create team | Names repeat or differ only by case/whitespace | **DECISION REQUIRED:** duplicate policy and name equivalence | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Member identity is name-based | Add member, if supported | Same person/name is already in this or another team | **DECISION REQUIRED:** membership-update scope, duplicates, and cross-team membership | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Teams can be retrieved | Retrieve team | The requested team exists | Return that team; exact retrieval identifier/route is not specified by these sources | None | 200 |
| Missing resources are not found | Retrieve team | A valid lookup identifies no team | Reject lookup as not found | Not-found exception; Java type unspecified | 404 |
| Retrieval scope is not specified | List/search teams | Client requests all teams or searches by name | **DECISION REQUIRED:** which retrieval operations are required | **DECISION REQUIRED** | 200 if supported and successful; otherwise contract undecided |
| Team updates are not specified | Update team | Client changes the team name | **DECISION REQUIRED:** operation scope, validation, and uniqueness on update | **DECISION REQUIRED** | **DECISION REQUIRED** |
| A team must have at least one member | Remove member, if supported | Removal would leave zero members | Must not leave a memberless team; **DECISION REQUIRED:** whether the operation exists and rejection classification | **DECISION REQUIRED:** validation versus business-rule exception | **DECISION REQUIRED:** 400 versus 409 |
| Membership history is not specified | Remove member, if supported | Member has previously submitted feedback | **DECISION REQUIRED:** whether removal is supported and how historical attribution/permissions behave | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Team deletion is not specified | Delete team | Team has retrospective history | **DECISION REQUIRED:** reject, cascade, or another history-preservation policy; operation scope also undecided | **DECISION REQUIRED** | **DECISION REQUIRED:** 204 if deletion is allowed; rejection category undecided |
| Authentication is out of scope | Invoke a supported operation | Otherwise valid caller supplies no authentication credentials | Do not require authentication solely for this API; this does not settle membership validation | None solely for absent credentials | Normal operation status; not an authentication rejection |

## 2. Retrospective creation, ownership, and retrieval

Sources: brief "Retrospectives"; decisions domain table and ambiguity sections 1, 3, and 7.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
|---|---|---|---|---|---|
| A retrospective belongs to a team | Create retrospective | A valid existing team and required retrospective data are supplied | Associate the new retrospective with that team | None | 201 |
| Missing resources are not found | Create retrospective | The referenced team does not exist | Reject; do not create an orphan retrospective | Not-found exception; Java type unspecified | 404 |
| A retrospective must belong to a team | Create retrospective | No team association can be determined | Reject creation without a team; **DECISION REQUIRED:** how the request identifies the team | Validation exception once request shape is defined | 400 for missing required association input |
| Any team member may create a retrospective for their team | Create retrospective | Actor is a member of the owning team | Membership must not block creation; **DECISION REQUIRED:** how the actor is supplied/represented | None for membership itself | 201 when all creation conditions are met |
| Creation is restricted to the team's members | Create retrospective | Actor is known not to belong to the team | Do not grant the member-only operation; **DECISION REQUIRED:** actor contract and rejection classification | **DECISION REQUIRED:** invalid-input versus business-rule category | **DECISION REQUIRED:** 400 versus 409; no authentication-layer status is specified |
| Actor identification is unresolved | Create retrospective | No actor identity is supplied | **DECISION REQUIRED:** require a name or assume membership; inherited service-contract mismatch is unresolved | **DECISION REQUIRED** | **DECISION REQUIRED** |
| A retrospective has a title and date | Create retrospective | Title or date is missing/null | **DECISION REQUIRED:** required client fields versus server defaults, especially date generation | **DECISION REQUIRED:** validation contract | **DECISION REQUIRED:** 400 if required input is missing |
| Title validation is not specified | Create retrospective | Title is blank, very long, or duplicates another title for the team | **DECISION REQUIRED:** blank policy, length limit, and title uniqueness | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Date semantics are not specified | Create retrospective | Supplied date is in the past/future or repeats another retrospective's date | **DECISION REQUIRED:** date meaning, allowed range, duplicates, format, and timezone if applicable | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Status is OPEN or CLOSED | Create retrospective | A status outside the allowed set is supplied as a status value | Reject an unsupported status if the request accepts status input; input exposure is undecided | Validation exception; Java type unspecified | 400 if parsed as an invalid status |
| Initial status is unresolved | Create retrospective | Client omits status or asks to create a CLOSED retrospective | **DECISION REQUIRED:** always start OPEN versus allow historical CLOSED creation; who controls status | **DECISION REQUIRED** | **DECISION REQUIRED:** 201 if permitted |
| At most one retrospective per team can be OPEN | Create OPEN retrospective | Team has no OPEN retrospective | Permit creation, subject to the other creation rules | None | 201 |
| At most one retrospective per team can be OPEN | Create OPEN retrospective | Team already has an OPEN retrospective | Reject; existing OPEN retrospective remains unchanged | Business-rule exception; Java type unspecified | 409 |
| A team can have multiple retrospectives | Create another retrospective | Team has previous CLOSED retrospectives but none OPEN | Closed history alone must not prevent creation | None | 201 |
| The OPEN limit is per team | Create OPEN retrospective | A different team already has an OPEN retrospective; target team has none | Other team's OPEN retrospective must not prevent creation | None | 201 |
| At most one retrospective per team can be OPEN | Create OPEN retrospective concurrently | Two creations target a team with no OPEN retrospective | Persist at most one OPEN retrospective; reject the competing creation that violates the rule; **DECISION REQUIRED:** serialization/retry protocol | Business-rule exception for the conflicting creation; Java type unspecified | 201 for accepted creation; 409 for a rejected business conflict |
| Ownership changes are not specified | Move retrospective, if supported | Client changes the owning team | **DECISION REQUIRED:** whether movement is allowed and how both teams' OPEN limits apply | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Retrospective retrieval scope is not specified | Retrieve one retrospective | Requested retrospective exists | **DECISION REQUIRED:** whether a single-resource read is required and its response shape | None if supported and successful | 200 if supported |
| Retrospective retrieval scope is not specified | List team retrospectives | Team exists, with or without history | **DECISION REQUIRED:** whether listing is required and its filters; a successful non-paginated list is an array | None if supported and successful | 200 if supported |
| Missing-parent listing semantics are unresolved | List team retrospectives | Referenced team does not exist | **DECISION REQUIRED:** require parent existence and return not-found versus treat as no matches | Not-found exception if parent existence is required; otherwise none | **DECISION REQUIRED:** 404 versus 200 with an empty list |

## 3. Retrospective lifecycle

Sources: brief "Retrospectives"; decisions domain table and ambiguity sections 1, 3, and 7.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
|---|---|---|---|---|---|
| A retrospective can be closed by any team member | Close retrospective | Retrospective is OPEN and actor belongs to the team; any agreed prerequisites hold | Set status to CLOSED; **DECISION REQUIRED:** actor representation and response body | None | 200/204 - **DECISION REQUIRED:** response body |
| Closure is restricted to the team's members | Close retrospective | Actor is known not to belong to the team | Do not grant the member-only operation; **DECISION REQUIRED:** identity/rejection contract | **DECISION REQUIRED:** validation versus business-rule category | **DECISION REQUIRED:** 400 versus 409 |
| Actor identification is unresolved | Close retrospective | No actor identity is supplied | **DECISION REQUIRED:** require a name or assume membership | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Missing resources are not found | Close retrospective | Retrospective does not exist | Reject closure as not found | Not-found exception; Java type unspecified | 404 |
| Once CLOSED, a retrospective cannot reopen | Reopen retrospective | Persisted status is CLOSED and requested status is OPEN | Reject; retain CLOSED; whether a dedicated reopen endpoint exists is undecided | Business-rule exception; Java type unspecified | 409 if exposed |
| Once CLOSED, a retrospective cannot reopen | Update retrospective status through a general update, if supported | General update attempts CLOSED to OPEN | Reject exactly as for reopening; a different operation must not evade the invariant | Business-rule exception; Java type unspecified | 409 if exposed |
| Status is OPEN or CLOSED | Update retrospective status, if supported | Supplied status is outside the allowed set | Reject unsupported status; general-update scope remains undecided | Validation exception; Java type unspecified | 400 if exposed |
| Repeated closure behavior is unresolved | Close retrospective | Status is already CLOSED | **DECISION REQUIRED:** successful no-op or rejection | None if idempotent; business-rule exception if rejected | **DECISION REQUIRED:** 200/204 versus 409 |
| Closure prerequisites are unresolved | Close retrospective | OPEN retrospective has no feedback | **DECISION REQUIRED:** whether empty retrospectives may close | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Closure prerequisites are unresolved | Close retrospective | OPEN retrospective has incomplete action items | **DECISION REQUIRED:** whether all actions must be complete before closure | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Non-status retrospective edits are unresolved | Modify retrospective | Client edits title/date, including after closure | **DECISION REQUIRED:** operation scope, editable fields, and closure effect | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Retrospective deletion is unresolved | Delete retrospective | Retrospective is OPEN or CLOSED and may have feedback | **DECISION REQUIRED:** operation scope and history/dependent-item policy | **DECISION REQUIRED** | **DECISION REQUIRED:** 204 if allowed |

## 4. Feedback creation and retrieval

Sources: brief "Feedback items"; decisions domain table and ambiguity sections 1, 4, and 6.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
|---|---|---|---|---|---|
| Feedback belongs to a retrospective | Add feedback | Valid parent and complete, valid feedback data are supplied to an OPEN retrospective | Create feedback associated with that retrospective | None | 201 |
| Missing resources are not found | Add feedback | Referenced retrospective does not exist | Reject; do not create orphan feedback | Not-found exception; Java type unspecified | 404 |
| Feedback must belong to a retrospective | Add feedback | No retrospective association can be determined | Reject creation without a parent; request shape remains undecided | Validation exception once association input is defined | 400 for missing required association input |
| Feedback can be added only while OPEN | Add ordinary feedback | Parent retrospective is CLOSED | Reject; add no feedback | Business-rule exception; Java type unspecified | 409 |
| Action items are feedback | Add action item | Parent retrospective is CLOSED | Reject action creation under the same no-new-feedback rule | Business-rule exception; Java type unspecified | 409 |
| Feedback type has a defined value set | Add feedback | Type is WENT_WELL or NEEDS_IMPROVEMENT | Preserve the selected valid type | None | 201 |
| Feedback type has a defined value set | Add feedback | Type is outside WENT_WELL, NEEDS_IMPROVEMENT, ACTION_ITEM | Reject unsupported type | Validation exception; Java type unspecified | 400 |
| Feedback has content, type, and submitter | Add feedback | One of these fields is absent/null | **DECISION REQUIRED:** exact required-input/default contract; a persisted feedback item must have the specified domain data | **DECISION REQUIRED:** request validation details | **DECISION REQUIRED:** 400 for missing input declared mandatory |
| Feedback text validation is unresolved | Add feedback | Content is blank, very long, or duplicates another contribution | **DECISION REQUIRED:** blank handling, maximum length, and duplicate policy | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Submitter is described as a team member | Add feedback | Supplied submitter name is not in the owning team's member list | **DECISION REQUIRED:** whether/how to reject nonmember submitters and compare names | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Member-name normalization is unresolved | Add feedback | Submitter differs from a stored member only by case/whitespace | **DECISION REQUIRED:** normalization and identity comparison | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Feedback can be retrieved per retrospective | Retrieve feedback list | Parent is OPEN and feedback exists | Return feedback for that retrospective, not another one's feedback | None | 200 |
| Feedback can be retrieved per retrospective | Retrieve feedback list | Existing parent has no matching feedback | Return an empty result; non-paginated result is `[]` | None | 200 |
| Closed-history visibility is explicitly unresolved | Retrieve feedback list | Parent retrospective is CLOSED | **DECISION REQUIRED:** explicitly confirm closed-history readability and response behavior | **DECISION REQUIRED** | **DECISION REQUIRED:** 200 if readable |
| Missing-parent listing semantics are unresolved | Retrieve feedback list | Referenced retrospective does not exist | **DECISION REQUIRED:** require parent existence versus empty matches | Not-found exception if parent is required; otherwise none | **DECISION REQUIRED:** 404 versus 200 |
| Action items are a special kind of feedback | Retrieve feedback list | Retrospective contains both ordinary feedback and actions | **DECISION REQUIRED:** whether actions appear in this list and how subtype fields are represented | **DECISION REQUIRED:** response contract | 200 if the listing succeeds; inclusion/shape unresolved |
| Type-filtered feedback retrieval is unresolved | Retrieve feedback list | Client supplies a feedback-type filter | **DECISION REQUIRED:** whether this filter is supported | **DECISION REQUIRED** | **DECISION REQUIRED:** 200 if supported |

## 5. Feedback modification and deletion

Sources: brief "Feedback items"; decisions ambiguity sections 1, 3-5, and 7.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
|---|---|---|---|---|---|
| CLOSED feedback cannot be modified | Modify ordinary feedback | Owning retrospective is CLOSED | Reject; retain existing feedback; edit-endpoint scope remains undecided | Business-rule exception; Java type unspecified | 409 if exposed |
| CLOSED feedback cannot be deleted | Delete ordinary feedback | Owning retrospective is CLOSED | Reject; retain the feedback; delete-endpoint scope remains undecided | Business-rule exception; Java type unspecified | 409 if exposed |
| Action items are feedback; CLOSED feedback cannot be modified | Modify action content | Owning retrospective is CLOSED | Reject a content edit; completion policy does not authorize ordinary edits | Business-rule exception; Java type unspecified | 409 if exposed |
| Action items are feedback; CLOSED feedback cannot be modified | Modify action submitter | Owning retrospective is CLOSED | Reject a submitter edit; completion policy does not authorize ordinary edits | Business-rule exception; Java type unspecified | 409 if exposed |
| Action items are feedback; CLOSED feedback cannot be modified | Modify action priority | Owning retrospective is CLOSED | Reject a priority edit; completion policy does not authorize ordinary edits | Business-rule exception; Java type unspecified | 409 if exposed |
| Action items are feedback; CLOSED feedback cannot be deleted | Delete action item | Owning retrospective is CLOSED | Reject; retain the action regardless of whether it is completed | Business-rule exception; Java type unspecified | 409 if exposed |
| Open-feedback editing is not explicitly required | Modify feedback | Parent is OPEN | **DECISION REQUIRED:** whether modification is supported, editable fields, and actor policy | **DECISION REQUIRED** | **DECISION REQUIRED:** 200/204 if supported and permitted |
| Open-feedback deletion is not explicitly required | Delete feedback | Parent is OPEN | **DECISION REQUIRED:** whether deletion is supported and who may delete | **DECISION REQUIRED** | **DECISION REQUIRED:** 204 if permitted |
| Missing resources are not found | Modify feedback, if supported | Feedback item does not exist | Reject as not found | Not-found exception; Java type unspecified | 404 |
| Missing resources are not found | Delete feedback, if supported | Feedback item does not exist | Reject as not found | Not-found exception; Java type unspecified | 404 |
| Type conversion is unresolved | Change feedback type | Client converts ordinary feedback into an action item or vice versa | **DECISION REQUIRED:** whether conversion is allowed and treatment of priority/completion history | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Feedback type has a defined value set | Change feedback type, if supported | Supplied type is outside WENT_WELL, NEEDS_IMPROVEMENT, ACTION_ITEM | Reject unsupported type; conversion scope remains undecided | Validation exception; Java type unspecified | 400 if exposed |
| Editable feedback relationships are unresolved | Move feedback | Client changes the retrospective association | **DECISION REQUIRED:** whether moving feedback is allowed and effects of source/target closure | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Actor permissions for edits are unresolved | Modify feedback | Actor is a different team member from the submitter | **DECISION REQUIRED:** submitter-only versus any-member editing | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Actor permissions for deletions are unresolved | Delete feedback | Actor is a different team member from the submitter | **DECISION REQUIRED:** submitter-only versus any-member deletion | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Closure and feedback changes must remain consistent | Add feedback concurrently with closure | Add and close race | No successful addition logically after closure; **DECISION REQUIRED:** transaction ordering and which request wins | Business-rule exception if addition is ordered after closure | 201 if ordered before closure; 409 if rejected after closure; ordering unresolved |
| Closure and feedback changes must remain consistent | Modify feedback concurrently with closure | Edit and close race | Must not permit an edit logically after closure; **DECISION REQUIRED:** edit scope and transaction ordering | Business-rule exception if a supported edit is rejected after closure | 200/204 if permitted before closure; 409 if rejected after closure; ordering/body unresolved |
| Closure and feedback changes must remain consistent | Delete feedback concurrently with closure | Delete and close race | Must not permit deletion logically after closure; **DECISION REQUIRED:** delete scope and transaction ordering | Business-rule exception if a supported delete is rejected after closure | 204 if permitted before closure; 409 if rejected after closure; ordering unresolved |

## 6. Action-item fields and lifecycle

Sources: brief "Action items"; decisions domain table and ambiguity sections 1, 4-5.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
|---|---|---|---|---|---|
| An action is feedback of type ACTION_ITEM | Add action item | Valid feedback data, permitted parent, and valid priority/completion initialization are provided | Create action feedback with content, submitter, parent, ACTION_ITEM type, priority, and boolean completion state | None | 201 |
| Action priority is LOW, MEDIUM, or HIGH | Add action item | Priority is one of the allowed values | Preserve the requested priority | None | 201 |
| Action priority is LOW, MEDIUM, or HIGH | Add action item | Priority is an unrecognized value | Reject unsupported priority | Validation exception; Java type unspecified | 400 |
| Action priority is LOW, MEDIUM, or HIGH | Modify action priority, if supported | Priority is an unrecognized value | Reject unsupported priority; editing scope and other applicable rules still need resolution | Validation exception; Java type unspecified | 400 if exposed |
| Priority requiredness/default is unresolved | Add action item | Priority is absent/null | **DECISION REQUIRED:** reject or apply an agreed default; no default priority is specified | **DECISION REQUIRED** | **DECISION REQUIRED:** 400 if mandatory; 201 if defaulted |
| Completion is a boolean | Add action item | A supplied completion value is not a valid boolean | Reject an invalid boolean representation; accepted coercions are not specified | Validation exception for an invalid value | 400 |
| Completion is a boolean | Update action completion through a general update, if supported | Supplied completion value is not a valid boolean | Reject invalid input; update scope and accepted coercions remain unresolved | Validation exception; Java type unspecified | 400 if exposed |
| Initial completion state is unresolved | Add action item | Completion is absent or explicitly true | **DECISION REQUIRED:** always start incomplete versus permit completed creation; who controls the flag | **DECISION REQUIRED** | **DECISION REQUIRED:** 201 if accepted |
| Generic ACTION_ITEM creation is unresolved | Add feedback through a generic creation operation | Type is ACTION_ITEM | **DECISION REQUIRED:** whether this operation accepts action creation and how extra fields are supplied | **DECISION REQUIRED** | **DECISION REQUIRED:** 201 if accepted |
| Action-only fields on ordinary feedback are unresolved | Add ordinary feedback | Request includes priority/completed fields | **DECISION REQUIRED:** reject, ignore, or exclude these fields from the request schema | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Action items can be completed | Complete action item | Action is incomplete, parent is OPEN, and the actor is permitted | Change completion to true; **DECISION REQUIRED:** response body and actor policy | None when permitted | 200/204 - **DECISION REQUIRED:** response body |
| Missing resources are not found | Complete action item | Requested action does not exist | Reject as not found | Not-found exception; Java type unspecified | 404 |
| Completion after closure conflicts with the feedback freeze | Complete action item | Action is incomplete and parent is CLOSED | **DECISION REQUIRED:** allow a completion-only exception or reject all modification | None if allowed; business-rule exception if forbidden | **DECISION REQUIRED:** 200/204 if allowed; 409 if forbidden |
| Completion is irreversible | Uncomplete action item | Action is already completed | Reject; keep completed true; explicit uncomplete-endpoint scope is undecided | Business-rule exception; Java type unspecified | 409 if exposed |
| Completion is irreversible regardless of mutation route | Update action completion through a general update, if supported | Stored completed=true; request sets completed=false | Reject; keep completed true | Business-rule exception; Java type unspecified | 409 if exposed |
| Completion is irreversible | Uncomplete action item | Action is completed and parent is CLOSED | Reject reversal; unresolved permission to complete after closure does not permit uncompletion | Business-rule exception; Java type unspecified | 409 if exposed |
| Repeated completion is unresolved | Complete action item | Action is already completed | **DECISION REQUIRED:** idempotent success versus rejection | None if idempotent; business-rule exception if rejected | **DECISION REQUIRED:** 200/204 versus 409 |
| Uncompletion API/no-op semantics are unresolved | Uncomplete action item | Action is already incomplete | **DECISION REQUIRED:** operation existence and whether false-to-false is accepted or rejected | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Missing resources are not found | Uncomplete action item, if exposed | Requested action does not exist | Reject as not found | Not-found exception; Java type unspecified | 404 |
| Completed-action edit policy is unresolved | Modify completed action | Parent is OPEN and client edits content or priority | **DECISION REQUIRED:** whether completion freezes more than the completed flag | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Completed-action deletion policy is unresolved | Delete completed action | Parent is OPEN | **DECISION REQUIRED:** whether completed actions can be deleted; general delete scope also unresolved | **DECISION REQUIRED** | **DECISION REQUIRED:** 204 if allowed |
| Completed-action type conversion is unresolved | Change completed action's type | Parent is OPEN and conversion would remove completion metadata | **DECISION REQUIRED:** conversion scope and preservation of irreversible completion history | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Completion actor policy is unresolved | Complete action item | Caller is not the submitter or is not a team member | **DECISION REQUIRED:** any trusted user versus team member versus submitter | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Follow-up metadata is not specified | Add action item | Client supplies assignee, due date, completion time, or completing member | **DECISION REQUIRED:** whether these fields are in scope and whether unknown fields are rejected | **DECISION REQUIRED** | **DECISION REQUIRED** |

## 7. Action-item filtering and collections

Sources: brief "Action items"; decisions ambiguity sections 4, 6, and 7.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
|---|---|---|---|---|---|
| Actions must be filterable by priority | Retrieve actions filtered by priority | A valid priority LOW, MEDIUM, or HIGH is supplied | Return only actions of that priority within the selected scope; **DECISION REQUIRED:** global/team/retrospective scope | None for a supported valid query | 200 |
| Actions must be filterable by completed status | Retrieve actions filtered by completed status | Filter is true | Return only completed actions within the selected scope; scope unresolved | None for a supported valid query | 200 |
| Actions must be filterable by completed status | Retrieve actions filtered by completed status | Filter is false | Return only incomplete actions within the selected scope; scope unresolved | None for a supported valid query | 200 |
| Invalid priority filters are validation failures | Retrieve actions filtered by priority | Priority is an unrecognized value | Reject invalid filter, rather than silently return no matches | Validation exception; Java type unspecified | 400 |
| Invalid completion filters are validation failures | Retrieve actions filtered by completed status | Value is not a recognized boolean, such as `maybe` | Reject invalid filter | Validation exception; Java type unspecified | 400 |
| Combined-filter semantics are unresolved | Retrieve actions with both filters | Priority and completed status are supplied together | **DECISION REQUIRED:** whether combined filtering is supported and whether conditions use AND | **DECISION REQUIRED** | **DECISION REQUIRED:** 200 if supported |
| Omitted-filter semantics are unresolved | Retrieve actions without filters | Neither priority nor completed is supplied | **DECISION REQUIRED:** return all actions, require a filter, or another contract | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Omitted-filter semantics are unresolved | Retrieve actions with only one filter | Other filter is absent | Enforce the supplied filter; **DECISION REQUIRED:** whether omitted filter has a default restriction | None if the query is accepted | 200 if accepted; default semantics unresolved |
| Empty-filter semantics are unresolved | Retrieve filtered actions | A filter value is an empty string | **DECISION REQUIRED:** reject versus treat as omitted | Validation exception if rejected; otherwise none | **DECISION REQUIRED:** 400 versus 200 |
| Multiple-priority semantics are unresolved | Retrieve actions filtered by several priorities | Client supplies more than one priority | **DECISION REQUIRED:** supported multi-value filter versus rejection | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Case/coercion rules are unresolved | Retrieve filtered actions | Priority has different casing, or boolean uses an alternate representation | **DECISION REQUIRED:** accepted spelling/casing/representations | Validation exception if rejected; otherwise none | **DECISION REQUIRED:** 400 versus 200 |
| Closed-parent inclusion is unresolved | Retrieve filtered actions | Matching actions belong to CLOSED retrospectives | **DECISION REQUIRED:** include or exclude closed-retrospective actions by default | **DECISION REQUIRED:** query policy | 200 for an accepted query; returned membership unresolved |
| Successful empty collections are not missing resources | Retrieve filtered actions | Valid query in a valid scope has no matches | Return an empty result; non-paginated result is `[]` | None | 200 |
| Nested missing-parent semantics are unresolved | Retrieve actions within a team/retrospective scope | Selected parent does not exist | **DECISION REQUIRED:** whether this scope exists and requires parent lookup | Not-found exception if parent is required; otherwise none | **DECISION REQUIRED:** 404 versus 200 with an empty result |
| List shape is defined only for non-paginated results | Retrieve a supported list | No pagination metadata is included | Return a JSON array, not an arbitrary wrapper object | None | 200 |
| Pagination is unresolved | Retrieve a supported list | Client requests a page/size | **DECISION REQUIRED:** support, defaults, limits, and metadata shape | **DECISION REQUIRED** | **DECISION REQUIRED:** 200 if valid and supported; 400 for invalid values once defined |
| Ordering is unresolved | Retrieve a supported list | More than one item matches | **DECISION REQUIRED:** sort field, direction, and tie-breaking; do not assert an invented order | None if query succeeds | 200 |

## 8. Cross-cutting input, errors, and mutation consistency

Sources: brief technical constraints and definition of done; decisions confirmed requirements and ambiguity sections 3, 7, and 9.

These supplement the operation-specific rows. They do not introduce new domain operations.

| Rule | Operation | Situation | Expected result | Exception | HTTP status |
|---|---|---|---|---|---|
| Invalid input must be rejected at the boundary | Create team | Request violates an agreed input constraint | Reject before service invocation; meaningful validation message | Validation exception; Java type unspecified | 400 |
| Invalid input must be rejected at the boundary | Create retrospective | Request violates an agreed input constraint | Reject before service invocation | Validation exception; Java type unspecified | 400 |
| Invalid input must be rejected at the boundary | Close retrospective | Request identifier violates an agreed input constraint | Reject before service invocation | Validation exception; Java type unspecified | 400 |
| Invalid input must be rejected at the boundary | Add feedback | Request violates an agreed input constraint | Reject before service invocation | Validation exception; Java type unspecified | 400 |
| Invalid input must be rejected at the boundary | Add action item | Request violates an agreed input constraint | Reject before service invocation | Validation exception; Java type unspecified | 400 |
| Invalid input must be rejected at the boundary | Modify feedback, if supported | Request violates an agreed input constraint | Reject before service invocation | Validation exception; Java type unspecified | 400 |
| Invalid input must be rejected at the boundary | Delete feedback, if supported | Request identifier violates an agreed input constraint | Reject before service invocation | Validation exception; Java type unspecified | 400 |
| Invalid input must be rejected at the boundary | Complete action | Request identifier/input violates an agreed input constraint | Reject before service invocation | Validation exception; Java type unspecified | 400 |
| Invalid input must be rejected at the boundary | Uncomplete action, if supported | Request identifier/input violates an agreed input constraint | Reject before service invocation | Validation exception; Java type unspecified | 400 |
| Identifier formats/ranges are unresolved | Any resource-targeting operation | Identifier is zero, negative, or of an unspecified format | **DECISION REQUIRED:** accepted identifier format/range; do not assume positive numeric IDs | Validation exception if invalid; not-found exception if valid but absent | **DECISION REQUIRED:** 400 versus 404 |
| Unknown/server-owned fields are unresolved | Create or update a resource | Client supplies an unknown field, ID, status, version, or relationship field | **DECISION REQUIRED:** request schema and reject/ignore/server-control policy | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Boundary-error wording is unresolved | Any operation with malformed input | JSON/type conversion fails before a custom service exception exists | Reject invalid input; **DECISION REQUIRED:** exact message and boundary-error envelope treatment | Boundary/parsing exception; Java type unspecified | 400 |
| Service business rules must not depend on controllers | Any business-rule mutation listed above | Operation is invoked directly through a service rather than through HTTP | Apply the same agreed domain invariant; **DECISION REQUIRED:** direct-service malformed-input contract | Corresponding domain exception on a forbidden transition; Java type unspecified | Not applicable to direct service calls; mapped category applies if exposed over HTTP |
| Custom errors require structured safe responses | Any operation throwing a custom exception | A domain validation, not-found, or business failure occurs | Return `error`, numeric `status`, and `timestamp`; service reason uses `EntityType identifier: reason`; no internal details | Corresponding custom category; Java types unspecified | 400, 404, or 409 according to the category |
| Unexpected failures are not business conflicts | Any operation | Unexpected server failure occurs | Return a safe server error, not success or an invented business rejection | Unexpected exception; Java type unspecified | 500 |
| Boundary validation precedes service lookup | Any operation | Input fails boundary validation and referenced resource is also missing | Reject boundary input before invoking the lookup | Validation exception; Java type unspecified | 400 |
| Other error precedence is unresolved | Any operation | Multiple boundary constraints or service business rules fail | **DECISION REQUIRED:** which error(s) to report and in what order | **DECISION REQUIRED** | Known category once selected; precedence unresolved |
| Lost-update behavior is unresolved | Modify feedback concurrently, if supported | Two clients edit the same feedback | **DECISION REQUIRED:** last-write-wins versus stale-write rejection | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Retry/idempotency behavior is unresolved | Create team | Client retries after a timeout | **DECISION REQUIRED:** duplicate creation versus idempotency mechanism | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Retry/idempotency behavior is unresolved | Create retrospective | Client retries after a timeout | Preserve the one-OPEN invariant; **DECISION REQUIRED:** recognizing/replaying the original creation versus treating retry as a conflict | Business-rule exception for a rejected new OPEN creation; replay policy unresolved | **DECISION REQUIRED:** replay contract versus 409 for a conflicting new creation |
| Retry/idempotency behavior is unresolved | Add feedback | Client retries after a timeout | **DECISION REQUIRED:** duplicate prevention or intentional duplicate contributions | **DECISION REQUIRED** | **DECISION REQUIRED** |
| Retry/idempotency behavior is unresolved | Add action item | Client retries after a timeout | **DECISION REQUIRED:** duplicate prevention or intentional duplicate actions | **DECISION REQUIRED** | **DECISION REQUIRED** |

## Decisions needed before turning flagged rows into assertions

The sources leave actor representation, membership comparisons, input requiredness/defaults, validation limits, initial states, repeated closure/completion, and completion after closure unresolved. They also leave optional edit/delete/retrieval scope, filtering combinations, historical visibility, and request/response details unresolved.

In particular, do not import the later implementation's date-requiredness, numeric-ID policy, constructor defaults, field limits, concrete exception classes, or current stub failures into these expected results. Those are not decisions recorded in the two allowed sources.

Engineering rules such as package layout, class length, injection style, query parameterization, and coverage thresholds remain important but are not business-operation cases in this matrix.
