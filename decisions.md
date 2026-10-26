# RetroFlow: Requirements and Decision Register

## Sources and status

This document combines [RetroFlow Project Brief v1.0](requirements/retroflow-brief.md) and [StackCraft Coding Standards v3.1](requirements/stackcraft-coding-standards.md). The brief defines the product scope; the standards add engineering and API requirements. Confirmed requirements below come from those documents, not new product decisions.

Open questions remain unresolved until agreed with the product owner or, for standards interpretation, the StackCraft Engineering Guild. Example code in the standards illustrates conventions; it does not automatically add business rules. Existing tests provide compatibility constraints and clues, not a complete specification.

## The domain: what RetroFlow is for

RetroFlow is a **shared record of team retrospectives and their follow-up work**. StackCraft's 12 teams currently scatter that information across sticky notes, Miro, and documents. The API should make it possible to answer: *What did this team discuss, what did they decide to do, and did those actions get completed?*

There is no frontend or authenticated user system in today's scope. Team members are represented by names, not accounts. Data lives in an in-memory H2 database and disappears when the application restarts.

### The four concepts

| Concept | Meaning | Relationships and rules |
|---|---|---|
| **Team** | A named group of people, such as "Platform," with members Alice and Bob. | Must have at least one member. Can have many retrospectives, but at most one open retrospective at a time. |
| **Retrospective** | A team's discussion session, such as "Sprint 12 Retro." It has a title, date, and status. | Belongs to one team. Its lifecycle is `OPEN → CLOSED`; reopening is forbidden. |
| **Feedback item** | Something a member contributes to that discussion. | Belongs to one retrospective. Has content, submitter name, and type: `WENT_WELL`, `NEEDS_IMPROVEMENT`, or `ACTION_ITEM`. New feedback is allowed only while the retrospective is open. Closed-retrospective feedback cannot be modified or deleted. |
| **Action item** | A feedback item representing follow-up work, such as "Automate deployment." | Has all feedback fields plus priority (`LOW`, `MEDIUM`, `HIGH`) and a completed flag. Completion is one-way: once completed, it cannot become incomplete again. |

**Action items are feedback, not an unrelated task list.** The implementation may use inheritance or composition, but it must preserve that domain relationship.

### A typical workflow

Alice creates a retrospective for her team. Members add positive feedback, improvement suggestions, and action items. Someone closes the retrospective, preventing further feedback contributions and edits. Action items record progress on the resulting work.

There is one important unresolved point in that workflow: **the brief does not say whether action items may be completed after their retrospective closes**. That matters because closing freezes feedback, but follow-up work normally happens afterward.

The business rules must live in the **service layer**, so they hold regardless of which controller invokes them. Controllers handle HTTP requests; repositories handle persistence. Neither should be the only place enforcing lifecycle rules.

## Confirmed engineering and API requirements

| Area | Confirmed requirement | Source |
|---|---|---|
| Runtime and persistence | Java 21, Spring Boot 3.x, Spring Data JPA, and in-memory H2. No persistence between restarts, frontend, or authentication/security layer is required. Maven or Gradle is permitted; this repository currently uses Maven. | Brief: technical constraints and scope |
| Business-rule enforcement | Enforce business rules in services, not just controllers. Validate all API input at the controller boundary before passing it to services; boundary validation does not replace service-layer business rules. | Brief: definition of done; standards 5.2 |
| Package structure | Feature-first packages are required for new projects, such as `team`, `retrospective`, and `feedback`. Existing projects are grandfathered; whether this inherited starter qualifies remains open below. | Standards 1.1 |
| Size limits | Classes must not exceed 200 lines. Methods must not exceed 20 lines excluding blank lines and comments; extract descriptive private helpers where necessary. | Standards 1.2-1.3 |
| Dependency injection | Use constructor injection for Spring-managed dependencies. Field injection is prohibited. The inherited tests' field injection requires reconciliation below. | Standards 1.4 |
| Service exceptions | Every exception thrown by a service must use `EntityType identifier: human-readable reason`, for example `Retrospective 42: cannot add feedback to a closed retrospective`. Do not swallow exceptions unless the reason is explicitly documented as permitted by the standards. | Standards 2.1, 2.3 |
| Error handling | A `@RestControllerAdvice` must handle all custom exceptions and return structured JSON with `error`, numeric `status`, and `timestamp`. Example: `{"error":"Retrospective 42: cannot add feedback to a closed retrospective","status":409,"timestamp":"2025-03-15T10:30:00Z"}`. The status must reflect the error category, not always be 400. | Standards 2.2, 4.1 |
| Safe error messages | Do not expose stack traces, internal class/package names, database errors, or JPA/Hibernate internals. Domain labels such as `Team` and `ActionItem` are explicitly used in the required message format. | Standards 2.1, 4.2 |
| HTTP status codes | Creation: `201`; successful read: `200`; update with body: `200`; update without body: `204`; deletion: `204`; missing resource: `404`; validation failure: `400`; business-rule violation: `409`; unexpected server error: `500`. | Standards 4.1 |
| List responses | Non-paginated list endpoints return JSON arrays, not wrapper objects. Wrappers are permitted when pagination metadata is included. | Standards 4.3 |
| Queries and secrets | Use parameterized SQL/JPQL or Spring Data derived queries, never query-string concatenation. Do not put secrets in source, configuration, or Git history; use environment variables or a secrets manager. These requirements apply despite authentication being out of scope. | Standards 5.1, 5.3 |
| Coverage | At least 80% line coverage and 70% branch coverage are required; the standards state CI enforces these thresholds. The brief also requires happy paths and key business-rule coverage, including closure, feedback modification, and irreversible completion. | Standards 3.1; brief: testing and definition of done |
| Test conventions | Use `methodName_condition_expectedBehaviour` names, explicit Arrange-Act-Assert comments, and named constants or clearly named local variables for test data. | Standards 3.2-3.4 |
| Service documentation | Every public service method needs JavaDoc describing its behavior, each parameter, each exception it can throw, and its return value when applicable. | Standards 6.1 |
| README | Include a 2-3 sentence project description, exact local startup commands, test commands, and either an API documentation link or a brief endpoint list. | Standards 6.2 |
| Delivery | By the Block 3 showcase at 15:15: implement entities and relationships, required REST operations, service-layer rules, boundary validation, required tests, and a starting, responsive application. | Brief: definition of done |

## Ambiguities and missing details

The following are decisions neither document fully settles—not additional requirements that should automatically be implemented. Questions resolved by the standards have been removed or narrowed.

### 1. Membership and who is allowed to act

"No authentication" removes identity verification, but it does not necessarily remove membership validation.

| Gap | Specific decision needed |
|---|---|
| Who is creating or closing a retrospective? | Both operations are restricted to team members, but no mechanism for supplying the acting member's name is defined. Is an actor required in the request, or is membership simply assumed? |
| Existing service contracts omit the actor | The supplied tests call `createRetrospective(teamId, title)` and `closeRetrospective(retroId)` without a member name. How should those contracts coexist with the membership requirement? |
| Who may submit feedback? | The submitter is described as a team member. Must the service reject a submitter whose name is absent from the retrospective's team? |
| Who may complete actions? | Any trusted caller, any member of the team, or only the original submitter? There is no action owner specified. |
| Who may edit or delete feedback? | If those operations exist, are they limited to the submitter, or available to any team member? |
| What makes two member names equal? | Are `Alice`, `alice`, and ` Alice ` the same member? Are names trimmed and compared case-insensitively? |
| Duplicate or ambiguous names | Can a team contain the same name twice? How are two different people both named "Alex" represented? Can the same name belong to several teams? |
| Membership changes | Are adding/removing members in scope? If so, can a former member's historical feedback remain, and which membership determines permission for later actions? |

### 2. Team behavior

| Gap | Specific decision needed |
|---|---|
| Team name validity | Must the name be nonblank? What is its maximum length? Must team names be unique, and is uniqueness case-sensitive? |
| Meaning of "at least one member" | Does `[null]` or `["   "]` count as invalid? Presumably yes, but the validation contract is not stated. |
| Team retrieval | Does "retrieved" mean retrieve by ID, list all teams, search by name, or all three? |
| Team updates and deletion | Neither is explicitly required. The README refers broadly to CRUD, which could incorrectly imply they are in scope. |
| Team deletion consequences | If deletion is supported, is it forbidden when retrospectives exist, or does it delete the entire history? |

### 3. Retrospective lifecycle and dates

| Gap | Specific decision needed |
|---|---|
| Initial status | Must every new retrospective start `OPEN`, or can clients create historical `CLOSED` retrospectives? The supplied tests imply an open default. |
| Meaning of "date" | Is it the meeting date, creation date, or scheduled date? Is it a date-only value or a timestamp? |
| Date source | Does the client supply the date, or does the server generate it? The supplied creation test provides no date. |
| Date restrictions | Are past and future dates allowed? Can two retrospectives for a team have the same date? Which timezone applies if time is involved? |
| Title validation | Are blank titles forbidden? What length limits apply? Must titles be unique within a team? |
| Repeated closure | Is closing an already closed retrospective an idempotent success or a business-rule violation (`409`)? Standards 3.3 shows a rejection example, but does not explicitly establish that as a product rule. Confirm whether it is intended as binding behavior. |
| Closure prerequisites | May an empty retrospective close? May it close while action items remain incomplete? |
| Other retrospective changes | Can title or date be edited? Can a retrospective be deleted or moved to another team? Does closure forbid those operations too? |
| Retrospective retrieval | No explicit retrieval operations are listed. Should clients get one by ID, list by team, and filter by status or date? Without some discovery mechanism, the central history is hard to use. |
| Concurrent creation | Two simultaneous requests could both observe "no open retrospective." The one-open rule must survive that race. Standards 4.1 settles the losing request's category as `409` if it violates the rule; transaction/locking strategy and any retry behavior still need an implementation decision. |
| Concurrent closure and feedback | If feedback submission races with closure, which ordering determines acceptance? The service needs a consistent transactional rule, not only a status check. |

### 4. Feedback creation and editing

| Gap | Specific decision needed |
|---|---|
| Are edit/delete operations required? | The brief forbids them after closure but never explicitly requires them while open. The definition of done nevertheless requires testing the modification rule. Is an actual update/delete API expected? |
| Editable fields | If editing is supported, may clients change only content, or also submitter, type, and retrospective? |
| Type conversion | Can ordinary feedback become an action item, or vice versa? What happens to priority and completion history when its type changes? |
| Content validation | Must content be nonblank? What is the maximum length? Are whitespace-only or duplicate submissions rejected? |
| Action creation through generic feedback | Can a generic feedback request use type `ACTION_ITEM`? If yes, it must also handle priority and completion correctly; otherwise it could create an incomplete action representation. |
| Fields inappropriate for the type | Should priority/completed fields on `WENT_WELL` feedback be rejected, ignored, or disallowed by the request schema? |
| Feedback retrieval shape | Does retrieving feedback for a retrospective include action items with their extra fields? Are type-specific filters needed? |
| Closed-history visibility | Closed feedback cannot change, but it presumably remains readable. The brief should explicitly confirm historical retrieval rather than leave closure behavior implicit. |

### 5. Action-item behavior—the biggest rule conflict

| Gap | Specific decision needed |
|---|---|
| Completion after closure | An action is a feedback item, and completion changes it. Does the closed-feedback freeze forbid completion, or is completion an explicit exception? This needs a product decision. |
| Other action edits after closure | If completion is allowed, are content, priority, and submitter still frozen? The exception must be narrow and explicit. |
| Initial completed state | Must actions always start incomplete? May clients create already-completed actions or supply the flag themselves? |
| Priority requirement | Must priority be supplied, or is there a default? |
| Repeated completion | Is completing an already completed action a successful no-op or an error? |
| Meaning of irreversible completion | Does it prohibit only `true → false`, or also deleting a completed action, changing its type, or editing its content? |
| Uncompletion API | The test calls `uncompleteActionItem` and expects rejection. Does the REST API need an explicit uncomplete endpoint, or should no such operation be exposed? |
| Ownership and follow-up metadata | The brief has a submitter, but no assignee, due date, completion timestamp, or completing member. Are those deliberately excluded? They should not be silently inferred from "track follow-up work." |

### 6. Filtering and collection behavior

| Gap | Specific decision needed |
|---|---|
| Filter scope | Are action items searched globally, per team, per retrospective, or through multiple endpoints? |
| Combining filters | Must `priority=HIGH&completed=false` work? If so, do the conditions combine with AND? |
| Omitted filters | Does omitting both return all actions? Does omitting `completed` include both states? |
| Multiple priorities | Can clients request several priorities at once, or only one? |
| Closed retrospectives | Do action queries include actions from closed retrospectives by default? |
| Invalid filters | Unrecognized priority or boolean values are validation failures and must return `400`. Should an empty parameter be rejected as invalid or treated as omitted? Define accepted boolean representations and enum casing. |
| Sorting | What ordering is guaranteed—creation order, retrospective date, priority, completion state? |
| Pagination | Are team, retrospective, feedback, and action lists paginated? If so, what are the default size, maximum size, and metadata schema? Standards 4.3 already requires arrays for non-paginated lists and permits wrappers only with pagination metadata. |
| Empty results | A successful non-paginated query with no matches returns `200` with `[]`; a missing parent resource is `404`. Does a nested listing require the parent to exist, rather than treating an unknown parent ID as simply no matches? |

### 7. REST contracts, errors, and consistency

| Gap | Specific decision needed |
|---|---|
| Endpoint contract | Routes, HTTP methods, request fields, response fields, and resource ID formats are unspecified. |
| Creation responses | `201` is mandatory. Must the response include the created resource, a `Location` header, or both? |
| ID validation | Missing resources must return `404`; malformed IDs are validation failures (`400`). What ID format and range are accepted, and are zero/negative numeric IDs invalid or merely nonexistent? |
| Error extensions and timestamp | The required fields are settled: `error`, `status`, and `timestamp`. Are stable machine-readable codes or field-error details needed in addition? Is UTC ISO-8601 with `Z`, as shown in the standards, mandatory, and what timestamp precision is expected? |
| Boundary-validation messages | Must malformed JSON, missing fields, type conversion failures, and other controller-level errors use the same JSON envelope? Standards 2.2 explicitly covers custom exceptions, while 2.1's entity/identifier message format explicitly covers service exceptions. Define boundary-error wording and whether to report one or all invalid fields. |
| Validation precedence | Boundary input validation must happen before invoking the service. If both boundary validation and resource lookup would fail, do not let service lookup bypass validation. Ordering among multiple boundary errors, and among multiple valid-input service failures, remains unspecified. |
| Enum parsing | Are type, status, and priority values strictly uppercase, or case-insensitive? |
| Unknown and server-owned fields | Are unknown JSON fields rejected? May requests supply IDs, status, or relationships that should be server-controlled? |
| Lost updates | If two members edit the same feedback, does the last write win, or should stale writes be rejected? |
| Duplicate retries | If a creation request is retried after a timeout, can it create duplicates, or is idempotency expected? This is separate from repeated close/complete operations. |

### 8. Delivery and acceptance gaps

| Gap | Specific decision needed |
|---|---|
| Existing tests versus required coverage | The three supplied tests cover adding feedback after closure, one open retrospective per team, and irreversible completion. They **do not cover editing/deleting closed feedback**, which the definition of done explicitly names. |
| Coverage scope and enforcement | Standards 3.1 settles the minimums at 80% lines and 70% branches. Which packages/classes count, are exclusions allowed, and what existing CI/report configuration enforces them? Required breadth for endpoint, persistence, missing-resource, and concurrency tests is still unspecified; thresholds do not replace the brief's named business-rule tests. |
| Showcase data | Must the demo start empty, or should there be seeded teams, retrospectives, and actions? H2 intentionally loses everything on restart. |
| API documentation | Standards 6.2 requires either an API documentation link or a brief endpoint list in the README. Is that sufficient for the showcase, or are executable requests/OpenAPI also expected? No particular documentation tool is mandated. |
| Source of truth | Use both the brief and the coding standards; the README's generic CRUD wording does not expand product scope. Define an approval process for genuine conflicts and exceptions. Stubs and example code should not silently decide business behavior. |

### 9. Standards applicability and reconciliation

| Gap or conflict | Specific decision needed |
|---|---|
| Feature-first migration | Standards 1.1 grandfathers existing projects. Does the inherited RetroFlow starter qualify, or must completion migrate its layer-first packages to feature-first packages? Existing tests import `com.stackcraft.retroflow.service.*`; a migration must explicitly address compatibility rather than silently break those imports. |
| Inherited test injection | Standards 1.4 prohibits field injection, but the supplied tests use `@Autowired` fields. Are inherited tests exempt, or may wiring be updated without changing their assertions and intent? Do not treat this mismatch as permission to skip or weaken tests. |
| Other inherited test conventions | Supplied test names do not follow `methodName_condition_expectedBehaviour`, and test-data naming needs review against 3.4. Are convention-only changes permitted for vendor tests, or should an explicit exception preserve them? |
| Identifier before persistence | Service exceptions must contain an entity identifier, but validation can fail before a new team, retrospective, or feedback item has an ID. What identifier token is approved for unsaved entities? |
| All service exceptions versus safe messages | Standards 2.1 applies to all exceptions thrown by services, including failures from repositories. Define how known persistence failures become formatted domain errors and how unexpected failures are translated/logged without leaking details or silently swallowing the cause. |
| Brief's broad 400 wording | The brief says invalid requests return `400`; standards 4.1 explicitly requires `409` for business-rule violations and `404` for missing resources. Read "invalid requests" as input-validation failures to satisfy both. If the product owner intended all rejected operations to return `400`, that is a direct conflict requiring agreement, not an implementation shortcut. |
| Illustrative uniqueness and lifecycle examples | Standards 4.2 uses duplicate team names as an error-message example, and 3.3 demonstrates rejecting repeated closure. Neither clearly states a new domain requirement. Team-name uniqueness and repeated-close behavior remain open until confirmed. |
| Boundary and service responsibility | Boundary validation is mandatory, as is service-layer business-rule enforcement. What contract should direct service callers receive for malformed inputs? Ensure required invariants hold for service calls such as the supplied tests, without assuming controller annotations alone enforce them. |

## What I would clarify first

Before implementing the endpoints, settle **completion after closure**, **whether feedback edit/delete is required**, **how member identity is supplied**, **date/default-state behavior**, and **action-query scope**. Also confirm **feature-first migration versus grandfathering** and **how inherited tests may be adapted to the coding standards**. These decisions directly change the service contracts, API design, and package layout.

HTTP status categories, the base error envelope, non-paginated list shape, coverage minimums, constructor injection, and documentation requirements are already specified by the standards; do not treat them as free design choices.

The irreversible transitions themselves are not ambiguous: **closed retrospectives cannot reopen, and completed actions cannot become incomplete**. The ambiguity is about the surrounding operations and exceptions—not whether those core rules should be enforced.