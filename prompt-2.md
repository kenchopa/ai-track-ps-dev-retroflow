# RetroFlow: Proposed JPA Entity Model

I recommend **`SINGLE_TABLE` inheritance for feedback**, lazy entity relationships, and member names stored as an element collection. This keeps the model small while allowing feedback queries to return ordinary feedback and action items together.

This is a **proposed model**, not approval of the open product decisions in `decisions.md`.

## 1. Proposed assumptions

The model needs several choices the brief has not settled:

| Area | Proposed choice |
|---|---|
| Retrospective date | `LocalDate` representing the meeting date. The service supplies it explicitly; any "today" default belongs in the service using an injected `Clock`. |
| Initial states | Retrospectives start `OPEN`; action items start incomplete. |
| Text limits | Team name, member name, submitter name, and title: 200 characters. Feedback content: 10,000 characters. These are proposed limits, not existing requirements. |
| Member names | Preserve input order and allow duplicate names for now. Name normalization and uniqueness need a separate product decision. |
| Name/title uniqueness | No uniqueness constraint until the requirement is confirmed. |
| Parent assignment | A retrospective cannot move between teams, and feedback cannot move between retrospectives. |
| Feedback type | Fixed at creation. Converting between types is not supported by this proposal. |
| Deletion | No automatic cascading deletion of retrospective history or feedback. |
| Action completion after closure | Remains unresolved. The persistence mapping supports either policy; the service must enforce the agreed one. |

## 2. Relationships

```text
Team
  +-- member names: List<String>
  +-- 0..* Retrospective
              +-- 0..* FeedbackItem
                          +-- WentWellFeedback
                          +-- NeedsImprovementFeedback
                          +-- ActionItem
```

Each retrospective belongs to **exactly one team**. Each feedback item, including an action item, belongs to **exactly one retrospective**.

I propose feature-first packages:

```text
com.stackcraft.retroflow.team
com.stackcraft.retroflow.retrospective
com.stackcraft.retroflow.feedback
```

Whether the existing starter must migrate remains a standards-applicability decision.

## 3. Entity mappings

The following snippets show **all persistent fields and their mappings**, plus constructors and type-discrimination methods. Routine getters, creation factories, relationship helpers, and lifecycle methods are omitted to keep the proposal focused; these are not complete drop-in application classes.

Use `jakarta.persistence.*` and `jakarta.validation.constraints.*` imports with Spring Boot 3.

### Enums

Each public enum belongs in its own file.

```java
public enum RetrospectiveStatus {
    OPEN,
    CLOSED
}
```

```java
public enum FeedbackType {
    WENT_WELL,
    NEEDS_IMPROVEMENT,
    ACTION_ITEM
}
```

```java
public enum Priority {
    LOW,
    MEDIUM,
    HIGH
}
```

### Team

```java
package com.stackcraft.retroflow.team;

import com.stackcraft.retroflow.retrospective.Retrospective;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @NotEmpty
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "team_members",
        joinColumns = @JoinColumn(name = "team_id", nullable = false)
    )
    @OrderColumn(name = "member_position")
    @Column(name = "member_name", nullable = false, length = 200)
    private List<@NotBlank @Size(max = 200) String> members =
        new ArrayList<>();

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private List<Retrospective> retrospectives = new ArrayList<>();

    protected Team() {
    }
}
```

**Why these choices:**

- **`Long` ID:** distinguishes an unsaved entity (`null`) from one with a generated identifier. `IDENTITY` is simple and supported by H2; sequence-based IDs would be worth considering if high-volume batched inserts became important.
- **`@ElementCollection`:** member names are values owned by a team, not independently identifiable users. A `Member` entity would introduce identity and lifecycle requirements the brief does not have.
- **`List<String>` with `@OrderColumn`:** preserves member order and does not silently impose uniqueness. A `Set` would be appropriate only after agreeing what duplicate names mean.
- **`@NotEmpty` and element constraints:** require at least one member and reject null, blank, or overlong member names.
- **No unique team-name constraint:** uniqueness is still an open question.
- **Lazy retrospective collection:** retrieving a team should not load its entire history.

The element collection's rows are lifecycle-dependent on the team. That is different from automatically deleting its retrospectives.

### Retrospective

```java
package com.stackcraft.retroflow.retrospective;

import com.stackcraft.retroflow.feedback.FeedbackItem;
import com.stackcraft.retroflow.team.Team;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "retrospectives",
    indexes = {
        @Index(
            name = "idx_retrospectives_team_status",
            columnList = "team_id,status"
        )
    }
)
public class Retrospective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false, updatable = false)
    private Team team;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotNull
    @Column(name = "meeting_date", nullable = false)
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private RetrospectiveStatus status = RetrospectiveStatus.OPEN;

    @OneToMany(mappedBy = "retrospective", fetch = FetchType.LAZY)
    private List<FeedbackItem> feedbackItems = new ArrayList<>();

    protected Retrospective() {
    }
}
```

**Why these choices:**

- **`LocalDate`:** suitable for a meeting date without inventing a time or timezone. If "date" actually means a creation instant, use `Instant` instead.
- **No `@PastOrPresent`:** future retrospective dates have not been prohibited.
- **String enum persistence:** stores `OPEN` and `CLOSED`, rather than fragile ordinal numbers that change meaning when enum declarations are reordered.
- **Explicit lazy `@ManyToOne`:** JPA defaults this relationship to eager fetching. Explicit laziness avoids loading a team whenever a retrospective is retrieved.
- **`optional = false` and `nullable = false`:** express the required relationship at both the ORM and database-schema levels.
- **`updatable = false`:** prevents changing the team foreign key through ordinary ORM updates. The entity API should also omit a reparenting setter.
- **Composite index on team and status:** supports finding a team's open retrospective. **It does not enforce the one-open rule.**

### FeedbackItem: abstract inheritance root

```java
package com.stackcraft.retroflow.feedback;

import com.stackcraft.retroflow.retrospective.Retrospective;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(
    name = "feedback_items",
    indexes = {
        @Index(
            name = "idx_feedback_retrospective",
            columnList = "retrospective_id"
        ),
        @Index(
            name = "idx_feedback_type_priority_completed",
            columnList = "feedback_type,priority,completed"
        )
    }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name = "feedback_type",
    discriminatorType = DiscriminatorType.STRING,
    length = 20
)
public abstract class FeedbackItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "retrospective_id",
        nullable = false,
        updatable = false
    )
    private Retrospective retrospective;

    @NotBlank
    @Size(max = 10_000)
    @Column(nullable = false, length = 10_000)
    private String content;

    @NotBlank
    @Size(max = 200)
    @Column(name = "submitted_by", nullable = false, length = 200)
    private String submittedBy;

    protected FeedbackItem() {
    }

    @Transient
    public abstract FeedbackType getType();
}
```

### Ordinary feedback subclasses

```java
package com.stackcraft.retroflow.feedback;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("WENT_WELL")
public class WentWellFeedback extends FeedbackItem {

    protected WentWellFeedback() {
    }

    @Override
    public FeedbackType getType() {
        return FeedbackType.WENT_WELL;
    }
}
```

```java
package com.stackcraft.retroflow.feedback;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("NEEDS_IMPROVEMENT")
public class NeedsImprovementFeedback extends FeedbackItem {

    protected NeedsImprovementFeedback() {
    }

    @Override
    public FeedbackType getType() {
        return FeedbackType.NEEDS_IMPROVEMENT;
    }
}
```

### ActionItem

```java
package com.stackcraft.retroflow.feedback;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@DiscriminatorValue("ACTION_ITEM")
public class ActionItem extends FeedbackItem {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 6)
    private Priority priority;

    @Column
    private boolean completed = false;

    protected ActionItem() {
    }

    @Override
    public FeedbackType getType() {
        return FeedbackType.ACTION_ITEM;
    }
}
```

**Important:** the action-specific columns intentionally do **not** have `nullable = false`. With single-table inheritance, those columns must permit nulls for ordinary-feedback rows. `@NotNull` still requires a priority when validating an `ActionItem`.

For database-level protection, add a conditional table constraint through schema DDL or Hibernate's `@Check`, conceptually:

```sql
CHECK (
    (
        feedback_type = 'ACTION_ITEM'
        AND priority IS NOT NULL
        AND completed IS NOT NULL
    )
    OR
    (
        feedback_type IN ('WENT_WELL', 'NEEDS_IMPROVEMENT')
        AND priority IS NULL
        AND completed IS NULL
    )
)
```

Do not put a global `completed DEFAULT FALSE` on the shared table: that would populate an action-only field for ordinary feedback too.

## 4. Why this inheritance design?

### Choose SINGLE_TABLE

All feedback types live in `feedback_items`. A discriminator identifies the concrete subtype.

| Alternative | Trade-off |
|---|---|
| **`SINGLE_TABLE` - recommended** | Efficient polymorphic retrieval without subtype joins. Only two action-specific columns are nullable for ordinary feedback. |
| `JOINED` | Gives action fields their own table and allows ordinary non-null constraints there, but action retrieval joins tables and inserts span multiple tables. |
| `TABLE_PER_CLASS` | Repeats common columns and typically needs unions when retrieving all feedback. Little benefit here. |
| Composition | Possible, but requires maintaining a one-to-one action-details relationship and keeping its presence consistent with feedback type. It also does not directly satisfy the requested inheritance design. |

For this small hierarchy and the requirement to retrieve mixed feedback per retrospective, `SINGLE_TABLE` is the simplest fit.

### Why introduce two ordinary-feedback subclasses?

It adds two small classes but gives **one authoritative representation of type**:

```text
Java subtype <-> database discriminator <-> FeedbackType returned by getType()
```

There is no writable enum field that could say `WENT_WELL` on an `ActionItem`.

A concrete base class plus an independently writable `type` field looks smaller, but introduces contradictory states and validation rules to prevent them. Mapping the discriminator again as a read-only field is possible, but that field may not be populated on a newly constructed instance until it is reloaded. Deriving type from the subtype avoids that issue.

The discriminator is the persisted type; `getType()` is its domain-facing representation, not a second database mapping.

## 5. Collection, ownership, and fetch decisions

| Decision | Reason |
|---|---|
| Child owns the relationship | `Retrospective.team` and `FeedbackItem.retrospective` hold the foreign keys. Their parent collections use `mappedBy` to avoid extra join tables. |
| `List` for entity collections | Avoids requiring entity equality/hash-code semantics just to manage a `Set`. Generated IDs make naive hash-based equality problematic before persistence. |
| No `@OrderColumn` for entity collections | No user-controlled retrospective/feedback ordering is required. Adding it would create persisted positional state and extra updates. |
| Explicit ordering in repository queries | A plain entity `List` does not guarantee database order. Define sorting in the relevant query once the API ordering is agreed. |
| All associations lazy | Load only what each operation needs. Fetch joins, entity graphs, or DTO projections should supply endpoint-specific data and avoid N+1 queries. |
| No `CascadeType.REMOVE` or `orphanRemoval` | Removing something from a collection must not silently delete history or bypass closed-retrospective rules. |
| No persistence cascade initially | Services explicitly save each resource. Creating a team need not implicitly persist an entire retrospective graph. |

Creation helpers should keep both sides of bidirectional relationships synchronized. For example, attaching feedback must set its `retrospective` and add it to the retrospective's collection. Expose read-only collection views rather than unrestricted collection setters.

Do not serialize JPA entities directly as API responses. DTOs avoid recursive parent/child JSON, accidental lazy loads, and exposing writable persistence state.

## 6. Rules that entity annotations cannot enforce

**This mapping alone does not enforce the core lifecycle rules.** They require service logic and transactions.

| Rule | Required enforcement |
|---|---|
| At least one team member | Entity validation plus request validation. An ordinary foreign key cannot enforce a minimum child-row count. |
| Submitter belongs to the team | Service membership check; submitter names are not foreign keys to authenticated users. |
| At most one open retrospective per team | Lock the team row in the creation transaction, then check for an existing open retrospective and insert. Every creation path must use that protocol. |
| Closed retrospective cannot reopen | No unrestricted status setter; service rejects forbidden transitions. |
| Closed feedback cannot be added, edited, or deleted | Service checks parent status under a concurrency protocol shared with closure. |
| Completed action cannot be uncompleted | No unrestricted completed setter; service exposes guarded completion behavior and rejects reversal. |
| Completion after closure | Implement only after the unresolved policy is agreed. |

Do **not** use a unique constraint on `(team_id, status)`: that would incorrectly allow only one closed retrospective per team.

### Why include @Version?

Optimistic versioning detects concurrent updates to the **same entity**, reducing lost updates.

It is not sufficient by itself for cross-row rules. Inserting a retrospective does not necessarily update its team's version, and editing feedback does not necessarily update its retrospective's version. Explicit locking or another coordinated transaction strategy is still needed for one-open enforcement and closure-versus-feedback races.

Finally, entity validation is a persistence safeguard—not a substitute for the coding standards' required boundary validation. Request DTOs must also validate incoming data before service invocation.

## Implementation notes

- The implementation retains `com.stackcraft.retroflow.entity` rather than migrating packages, preserving existing repository and vendor-test imports while the grandfathering question remains open.
- All nine proposed entity/enum classes are implemented. Constructors accept explicit values and synchronize parent collections; collection getters are read-only, and generated IDs/versions have no setters.
- The shared feedback table includes the conditional Hibernate `@Check` described above.
- `close()` and `complete()` perform one-way state assignments only. Whether repeated requests should succeed or fail, and whether completion after closure is allowed, remains service policy rather than being decided by these methods.
- Services, HTTP endpoints, request DTOs, and cross-entity concurrency rules are outside this entity-model implementation. The inherited business-rule test still references services that do not exist yet.
