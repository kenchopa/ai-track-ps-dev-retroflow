# RetroFlow: Complete Entity Classes and Accessor Decisions

## Scope

Implement `Team`, `Retrospective`, `FeedbackItem`, and `ActionItem` as explicit Java classes using Java 21, Spring Boot 3, Jakarta Persistence, and Jakarta Bean Validation. No Lombok, placeholders, or TODOs are used.

The inheritance and validation placeholders in the request are resolved using the proposal in [prompt-2.md](prompt-2.md). That document records the original design; this document supersedes its implementation notes about constructor visibility and the absence of setters.

## Implemented classes

| Class | Source | Role |
|---|---|---|
| `Team` | [Team.java](src/main/java/com/stackcraft/retroflow/entity/Team.java) | Team identity, name, ordered member names, and retrospective relationship. |
| `Retrospective` | [Retrospective.java](src/main/java/com/stackcraft/retroflow/entity/Retrospective.java) | Team-owned session, title, meeting date, status, and feedback relationship. |
| `FeedbackItem` | [FeedbackItem.java](src/main/java/com/stackcraft/retroflow/entity/FeedbackItem.java) | Abstract entity containing the common feedback fields and inheritance mapping. |
| `ActionItem` | [ActionItem.java](src/main/java/com/stackcraft/retroflow/entity/ActionItem.java) | Concrete feedback subtype with priority and irreversible completion. |
| `WentWellFeedback` | [WentWellFeedback.java](src/main/java/com/stackcraft/retroflow/entity/WentWellFeedback.java) | Concrete `WENT_WELL` feedback. |
| `NeedsImprovementFeedback` | [NeedsImprovementFeedback.java](src/main/java/com/stackcraft/retroflow/entity/NeedsImprovementFeedback.java) | Concrete `NEEDS_IMPROVEMENT` feedback. |

The existing `RetrospectiveStatus`, `FeedbackType`, and `Priority` enums remain part of the model. Full implementations are in the linked source files, not partial examples in this document.

## Inheritance

Use `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` on the abstract `FeedbackItem` root. The `feedback_type` discriminator contains `WENT_WELL`, `NEEDS_IMPROVEMENT`, or `ACTION_ITEM`.

All feedback shares one identity space and one table, so retrieving feedback for a retrospective is a polymorphic query without subtype joins. `ActionItem` inherits its ID, version, retrospective, content, and submitter; it does not declare a second ID.

The two ordinary-feedback subclasses keep the discriminator and Java subtype consistent. `getType()` derives its value from the subtype. There is deliberately no `setType()` or type constructor argument: type is not an independently writable field, and changing it would contradict the runtime class.

Action-specific columns are nullable in the shared table because ordinary feedback has no priority or completion state. A Hibernate `@Check` on the root requires those columns for action rows and forbids them for ordinary feedback rows. `@NotNull` on `ActionItem.priority` validates action instances without incorrectly making that column mandatory for all rows.

`SINGLE_TABLE` avoids the joins of `JOINED` and the polymorphic unions/column duplication of `TABLE_PER_CLASS`. Hibernate's `@Check` is a deliberate provider-specific addition; all persistence and validation API annotations use `jakarta`, not `javax`.

## Constructors and accessors

Every concrete entity has a public no-argument constructor, a creation constructor, and an all-arguments constructor. The abstract root also has a public no-argument and all-arguments constructor for subclass use. A no-argument entity is temporarily incomplete and must be populated and validated before persistence.

All-arguments constructors include generated identity and version values as explicitly requested. For subclasses they include the inherited fields as well:

```text
Team(id, version, name, members, retrospectives)
Retrospective(id, version, team, title, date, status, feedbackItems)
FeedbackItem(id, version, retrospective, content, submittedBy)
ActionItem(id, version, retrospective, content, submittedBy, priority, completed)
WentWellFeedback(id, version, retrospective, content, submittedBy)
NeedsImprovementFeedback(id, version, retrospective, content, submittedBy)
```

For normal creation, prefer the shorter constructors with no generated ID or version arguments. JPA generates IDs and manages versions. The public `setId()` and `setVersion()` methods satisfy the explicit accessor request but must not be exposed as client-controlled request fields or used to overwrite metadata on managed entities.

All persistent fields have explicit getters and setters, with inherited accessors reused by subclasses. Primitive completion uses the JavaBeans getter `isCompleted()`. The only getter without a corresponding setter is the derived, nonpersistent `getType()`.

JPA uses field access because `@Id` is on the field, so loading historical records does not invoke domain setters or creation constructors.

## Validation

| Field | Constraints | Decision |
|---|---|---|
| Team name | `@NotBlank`, `@Size(max = 200)`, non-null column | Reject null/blank names and bound storage. No uniqueness requirement is invented. |
| Team members | `@NotEmpty` | A team must have at least one member. |
| Each member name | `@NotBlank`, `@Size(max = 200)`, non-null column | An entry must represent a nonblank name, not just make the collection nonempty. |
| Retrospective team | `@NotNull`, required many-to-one, non-null FK | Every retrospective belongs to exactly one team. |
| Retrospective title | `@NotBlank`, `@Size(max = 200)`, non-null column | Reject empty discussion titles. |
| Retrospective date | `@NotNull`, non-null column | Store an explicit `LocalDate`; do not invent a timezone or prohibit future meetings. |
| Retrospective status | `@NotNull`, string enum, non-null column | Limit state to `OPEN` or `CLOSED`. |
| Feedback retrospective | `@NotNull`, required many-to-one, non-null FK | Every feedback item belongs to exactly one retrospective. |
| Feedback content | `@NotBlank`, `@Size(max = 10_000)`, non-null column | Reject empty contributions and bound text storage. |
| Feedback submitter | `@NotBlank`, `@Size(max = 200)`, non-null column | Record a nonblank member name. Membership validation still belongs in the service. |
| Action priority | `@NotNull`, string enum | A priority must be supplied; no default is silently selected. |
| Action completed | Primitive `boolean`, initially `false` | No Java null state; completion is initially incomplete for the creation constructor. |

The 200/10,000 character limits remain proposed implementation limits, not newly discovered product requirements. Matching request-DTO validation is still required at the controller boundary. Bean Validation annotations are evaluated when a validator or JPA validation runs, not automatically on every setter call.

## Relationships and collections

- Team to retrospectives and retrospective to feedback are lazy, bidirectional one-to-many relationships. The child many-to-one fields own the foreign keys.
- Both many-to-one mappings explicitly use `FetchType.LAZY`, `optional = false`, and non-null join columns. `updatable = false` reflects the existing no-reparenting proposal.
- Member names use a lazy `@ElementCollection` and an ordered `List<String>`. Order and duplicate names are preserved; normalization and uniqueness remain unresolved.
- Entity collections use `List`, with no stored position or implied API ordering. Queries must specify any required sorting.
- Collection getters return unmodifiable views. Setters defensively copy their input before mutating the internal collection, including when passed their own getter's view.
- Collection setters retain the managed collection instance, validate that children already belong to the correct parent, reject duplicates, and reject removing existing children. Removing a required relationship cannot be represented consistently by only editing the inverse collection.
- To attach a child, use its creation constructor or initialize its parent through the parent setter. Parent setters synchronize the inverse collection and reject reassignment to another parent. Adding the same child twice does not duplicate the relationship.
- No cascade remove, orphan removal, or automatic persistence cascade is enabled. Deletion must be an explicit service operation with business-rule checks, not a side effect of a collection setter.
- Parent/collection setters enforce object-graph consistency, not detached-graph merge semantics. Reconcile incoming IDs to managed entities in a service rather than constructing an arbitrary detached graph.

## Guarded mutation

| Operation | Entity behavior |
|---|---|
| Reopen a closed retrospective | `setStatus(OPEN)` rejects `CLOSED -> OPEN` with `RetroflowException`. `close()` delegates to the setter. |
| Reverse completion | `setCompleted(false)` rejects `true -> false` with `RetroflowException`. `complete()` delegates to the setter. |
| Change feedback content or submitter after closure | Reject through `requireEditable()`. |
| Change action priority after closure | Reject through the same feedback edit guard. |
| Add feedback to a closed retrospective | Reject before registering the child in the parent's collection. |
| Move a retrospective or feedback item to another parent | Reject rather than leave Java state inconsistent with the non-updatable foreign key. |
| Change feedback type | No setter; the subtype fixes the type. |

These are defensive entity checks, not a replacement for the brief's mandatory service-layer rules. Domain exceptions use `EntityType identifier: reason`. Unsaved entities currently render their null ID as `null`; the register's question about an approved pre-persistence identifier remains unresolved. Invalid structural arguments such as a null parent/collection or inconsistent team collection fail explicitly rather than being silently ignored.

Repeated close/complete assignments remain harmless at the entity level; a future service must decide whether repeated API requests are successful no-ops or conflicts. The entity does not decide whether completion after closure is permitted: `setCompleted()` enforces only irreversible completion. Title/date editing and membership changes likewise do not establish new REST operations or permission policies.

## Concurrency and remaining scope

All entity roots have `@Version`. It protects updates to the same row but does not enforce the one-open-retrospective rule across inserts or synchronize closure with changes to child rows.

Services must still enforce membership, one-open-per-team, transactional locking, deletion restrictions, and the agreed completion-after-closure policy. Controllers, request/response DTOs, and global HTTP error mapping are not added by this entity-only task. Entities must not be bound directly to API request bodies.

Keep the existing `com.stackcraft.retroflow.entity` package for compatibility with repository and inherited test imports. This is not a decision to ignore the feature-first standard; the existing-project grandfathering/migration question in `decisions.md` remains open.
