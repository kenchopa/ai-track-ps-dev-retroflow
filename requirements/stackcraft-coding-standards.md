# StackCraft Engineering: Coding Standards
**Version 3.1 · Effective from Q1 2025 · Applies to all backend Java projects**

---

## 1. Code Structure & Organisation

### 1.1 Package structure (CRITICAL)
All projects must follow a feature-first package structure, not a layer-first structure.

**Correct:**
```
com.stackcraft.retroflow.team
com.stackcraft.retroflow.retrospective
com.stackcraft.retroflow.feedback
```

**Incorrect:**
```
com.stackcraft.retroflow.controller
com.stackcraft.retroflow.service
com.stackcraft.retroflow.repository
```

Violation severity: **Major**. Existing projects are grandfathered but new projects must comply.

---

### 1.2 Class length
No class may exceed 200 lines. If a class approaches this limit, it is doing too much.

Violation severity: **Minor**

---

### 1.3 Method length
No method may exceed 20 lines (excluding blank lines and comments).
Methods that exceed this must be broken into private helper methods with descriptive names.

Violation severity: **Major**

---

### 1.4 Constructor injection only
Field injection (`@Autowired` on a field) is prohibited.
Constructor injection must be used for all Spring-managed dependencies.

```java
// PROHIBITED
@Autowired
private TeamRepository teamRepository;

// REQUIRED
private final TeamRepository teamRepository;

public TeamService(TeamRepository teamRepository) {
    this.teamRepository = teamRepository;
}
```

Violation severity: **Critical**. Field injection makes testing impossible and hides dependencies.

---

## 2. Exception Handling

### 2.1 Exception message format (CRITICAL)
All exceptions thrown by the service layer must follow this exact format:

```
[EntityType] [identifier]: [human-readable reason]
```

Examples:
- `"Retrospective 42: cannot add feedback to a closed retrospective"`
- `"Team 7: must have at least one member"`
- `"ActionItem 15: already completed, cannot be undone"`

This format is required because our observability platform parses exception messages
for alerting and dashboards. Deviating from this format breaks our monitoring.

Violation severity: **Critical**

---

### 2.2 Global exception handler required
Every project must have a `@RestControllerAdvice` class that handles all custom exceptions
and maps them to structured JSON error responses.

Required error response format:
```json
{
  "error": "EntityType identifier: reason",
  "status": 400,
  "timestamp": "2025-03-15T10:30:00Z"
}
```

Violation severity: **Critical**

---

### 2.3 Never swallow exceptions
Catching an exception and doing nothing (or only logging) is prohibited unless
explicitly documented with a comment explaining why.

```java
// PROHIBITED
try {
    doSomething();
} catch (Exception e) {
    log.error("Error", e);
}

// REQUIRED: either rethrow, wrap, or document why swallowing is intentional
```

Violation severity: **Major**

---

## 3. Testing Standards

### 3.1 Minimum coverage
Line coverage must be at least 80%. Branch coverage must be at least 70%.
The CI pipeline enforces this: builds will fail below these thresholds.

---

### 3.2 Test naming convention
Test method names must follow: `methodName_condition_expectedBehaviour`

```java
// REQUIRED
@Test
void createRetrospective_whenTeamAlreadyHasOpenRetro_throwsException()

// PROHIBITED
@Test
void testCreateRetro()
void shouldThrowException()
```

Violation severity: **Minor** (but enforced in code reviews)

---

### 3.3 Arrange-Act-Assert with comments
All tests must use the AAA pattern with explicit comments:

```java
@Test
void closeRetrospective_whenAlreadyClosed_throwsException() {
    // Arrange
    Retrospective retro = ...;
    service.closeRetrospective(retro.getId());

    // Act & Assert
    assertThatThrownBy(() -> service.closeRetrospective(retro.getId()))
        .isInstanceOf(RetroflowException.class)
        .hasMessageContaining("already closed");
}
```

Violation severity: **Minor**

---

### 3.4 No magic numbers in tests
Test data must use named constants or clearly named local variables.

```java
// PROHIBITED
team.setMembers(List.of("Alice", "Bob", "Carol")); // why 3 members?

// REQUIRED
var minimumViableTeam = List.of("Alice"); // 1 member is the minimum
team.setMembers(minimumViableTeam);
```

Violation severity: **Minor**

---

## 4. API Design

### 4.1 HTTP status codes (CRITICAL)
The following status codes are mandatory for these situations:

| Situation | Required status code |
|-----------|----------------------|
| Resource created | 201 Created |
| Successful read | 200 OK |
| Successful update with response body | 200 OK |
| Successful update with no response body | 204 No Content |
| Successful delete | 204 No Content |
| Resource not found | 404 Not Found |
| Validation failure | 400 Bad Request |
| Business rule violation | 409 Conflict |
| Unexpected server error | 500 Internal Server Error |

Using 200 for a creation, or 400 for a business rule violation, is a Critical violation.

Violation severity: **Critical**

---

### 4.2 No internal details in error messages
Error messages returned to API consumers must not contain:
- Stack traces
- Internal class names or package names
- Database error messages
- JPA/Hibernate internal messages

```json
// PROHIBITED
{
  "error": "org.hibernate.exception.ConstraintViolationException: could not execute statement"
}

// REQUIRED
{
  "error": "Team 7: a team with this name already exists"
}
```

Violation severity: **Critical**

---

### 4.3 Consistent response structure
All list endpoints must return an array, never a wrapped object, unless pagination metadata is included.

```json
// REQUIRED for simple lists
[
  { "id": 1, "title": "Sprint 1 Retro" },
  { "id": 2, "title": "Sprint 2 Retro" }
]

// PROHIBITED (wrapping without reason)
{
  "data": [...],
  "count": 2
}
```

Violation severity: **Minor**

---

## 5. Security (applies even to internal APIs)

### 5.1 No secrets in code or configuration files
API keys, passwords, and tokens must never appear in:
- Java source files
- `application.properties` or `application.yml`
- Git history

Use environment variables or a secrets manager.

Violation severity: **Critical**

---

### 5.2 Validate all input at the boundary
All data entering the system via API must be validated before it reaches the service layer.
The controller is the boundary. Do not assume the service will catch bad data.

Violation severity: **Major**

---

### 5.3 Parameterised queries only
String concatenation in JPQL or SQL queries is prohibited regardless of context.
Use named parameters or Spring Data method naming instead.

```java
// PROHIBITED
String jpql = "SELECT r FROM Retrospective r WHERE r.team.id = " + teamId;

// REQUIRED
@Query("SELECT r FROM Retrospective r WHERE r.team.id = :teamId")
List<Retrospective> findByTeamId(@Param("teamId") Long teamId);
```

Violation severity: **Critical**

---

## 6. Documentation

### 6.1 JavaDoc on all public service methods
Every public method in a service class must have a JavaDoc comment with:
- A description of what the method does
- `@param` for each parameter
- `@throws` for each exception that can be thrown
- `@return` if the method returns a value

Violation severity: **Minor** (rises to Major for methods implementing business rules)

---

### 6.2 README requirements
Every project repository must have a README that includes:
- What the project does (2-3 sentences)
- How to get it running locally (exact commands, not "follow standard setup")
- How to run the tests
- A link to the API documentation or a brief endpoint list

Violation severity: **Minor**

---

*For questions about these standards, contact the StackCraft Engineering Guild.*
*Last reviewed: March 2025*
