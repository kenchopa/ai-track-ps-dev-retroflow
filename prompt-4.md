# RetroFlow: REST Controllers and Service Contracts

## Scope and implementation status

Implement the five requested HTTP operations using thin controllers, validated request DTOs, response DTOs, and shared exception handling. Keep business logic out of controllers.

`TeamService` and `RetrospectiveService` are deliberately **stubs**, as requested. Each method throws `UnsupportedOperationException` instead of returning null, an empty result, a fabricated entity, or an apparent success. No persistence, membership checks, lifecycle decisions, or locking is implemented in these services yet.

Consequently, valid requests that reach a real service stub currently return a sanitized `500`, not the future success response. Validation failures are already handled at the HTTP boundary. Resource-not-found and business-rule mappings are wired for the exceptions future implementations will throw. Controller contract tests use mocked services; they do not demonstrate implemented business logic.

## Endpoint contracts

| Operation | Request | Success contract | Common error contracts |
|---|---|---|---|
| `POST /api/teams` | JSON `name` and `members` | `201`, team response, `Location: /api/teams/{id}` | `400` invalid input; `409` if a confirmed business rule is violated |
| `GET /api/teams/{id}` | Positive numeric team ID | `200`, team response | `400` invalid ID; `404` missing team |
| `POST /api/teams/{teamId}/retrospectives` | Positive team ID; JSON `title` and `date` | `201`, retrospective response, `Location: /api/retrospectives/{id}` | `400` invalid input; `404` missing team; `409` team already has an open retrospective |
| `GET /api/teams/{teamId}/retrospectives` | Positive team ID | `200`, JSON array; `[]` for an existing team with no history | `400` invalid ID; `404` missing team |
| `PUT /api/retrospectives/{id}/close` | Positive retrospective ID; no body required | `204`, no response body | `400` invalid ID; `404` missing retrospective; `409` a service-reported closure rule violation |

`500` is reserved for unexpected/unimplemented server behavior, not input failures or domain conflicts. Framework HTTP errors such as `405` and `415` retain their status and receive the same JSON error envelope.

The retrospective creation `Location` identifies the resource, but this task does not add a `GET /api/retrospectives/{id}` endpoint.

### Request examples

Create a team:

```http
POST /api/teams
Content-Type: application/json

{
  "name": "Alpha",
  "members": ["Alice", "Bob"]
}
```

Create a retrospective:

```http
POST /api/teams/7/retrospectives
Content-Type: application/json

{
  "title": "Sprint retro",
  "date": "2026-10-26"
}
```

Close a retrospective:

```http
PUT /api/retrospectives/42/close
```

### Response examples

Team:

```json
{
  "id": 7,
  "name": "Alpha",
  "members": ["Alice", "Bob"]
}
```

Retrospective:

```json
{
  "id": 42,
  "teamId": 7,
  "title": "Sprint retro",
  "date": "2026-10-26",
  "status": "OPEN"
}
```

Business-rule error:

```json
{
  "error": "Team 7: already has an open retrospective",
  "status": 409,
  "timestamp": "2026-10-26T10:30:00Z"
}
```

## Boundary validation decisions

- IDs are Java `Long` values and must be greater than zero. Malformed, nonpositive, and overflowing IDs return `400` before service invocation.
- Team names and retrospective titles must be nonblank and no longer than 200 characters.
- Members must be a nonempty list; every name must be nonblank and no longer than 200 characters.
- The retrospective date is required and represented by an ISO local-date string, not a timestamp or numeric date array. Invalid calendar dates return `400`. Past/future dates are not prohibited.
- Requiring a client-supplied date follows the earlier entity proposal. It is an explicit API choice, not a newly discovered requirement. The inherited two-argument service contract remains stubbed and does not decide a default date.
- Missing bodies, malformed JSON, unknown fields, and trailing JSON documents return `400`. Numeric/boolean values are not silently coerced into names or titles.
- Field validation messages identify the field and failed constraint. Parsing failures use a safe message explaining malformed JSON, unknown fields, or invalid field types/dates without exposing internal types.
- No trimming, case folding, team-name uniqueness, member deduplication, authentication, or actor parameter is invented. Membership and acting-member policy remain open service requirements.
- No class-level `@Validated` is needed on controllers: Spring MVC's built-in method validation handles constrained path parameters, and `@Valid` validates body records.

## Structure and responsibilities

| Component | Responsibility |
|---|---|
| `TeamController` | Team creation/retrieval routing, DTO conversion, creation location and status. |
| `RetrospectiveController` | Team retrospective creation/listing and retrospective closure routing. |
| Request records | Jakarta Bean Validation constraints at the controller boundary. |
| Response records | Explicit, shallow API representations; no recursive JPA graph, generated version, or writable entity exposure. |
| `TeamService` | Signatures for creating and retrieving teams; implementation deferred. |
| `RetrospectiveService` | Signatures for dated retrospective creation, listing, and closure; implementation deferred. |
| `ResourceNotFoundException` | Domain resource lookup failure, formatted as `EntityType identifier: not found`, mapped to `404`. |
| `RetroflowException` | Business-rule violation, mapped to `409`. The more specific not-found subtype maps to `404`. |
| `GlobalExceptionHandler` | Structured errors, safe validation feedback, framework HTTP error preservation, and sanitized unexpected errors. |
| `ApiJsonConfiguration` | Reject unknown/trailing input and unwanted scalar-to-string coercion. |
| `IsoLocalDateDeserializer` | Accept only a valid ISO local-date string; explicitly reject Jackson's otherwise supported numeric date arrays. |

Controllers only delegate, map DTOs, and construct HTTP responses. They do not query repositories, choose status transitions, check membership, or catch domain exceptions locally.

Constructor injection is used throughout. Existing layer-first package names are retained consistently with the previous tasks; the feature-first migration/grandfathering question remains recorded in `decisions.md`.

## Service signatures

```java
Team createTeam(String name, List<String> members);
Team getTeam(Long id);
Retrospective createRetrospective(Long teamId, String title, LocalDate date);
List<Retrospective> listRetrospectives(Long teamId);
void closeRetrospective(Long id);
```

The inherited tests also reference the following signatures, so explicit stubs preserve compilation without changing assertions or implementing unrelated operations:

```java
Retrospective createRetrospective(Long teamId, String title);
FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String submittedBy);
ActionItem addActionItem(Long retrospectiveId, String content, String priority, String submittedBy);
void completeActionItem(Long id);
void uncompleteActionItem(Long id);
```

These compatibility methods add no additional REST endpoints. Public service methods include JavaDoc for parameters, returns where applicable, and domain/not-implemented exceptions.

For temporary creation-stub messages, `new` is used instead of an absent numeric identifier. This does not resolve the register's organization-wide question about an approved pre-persistence identifier.

## Requirements for future service implementations

Implement resource existence checks explicitly: an absent team is `404`, while an existing team's empty history is a successful empty list. Never use null to represent an expected lookup failure.

Enforce the single-open-retrospective rule transactionally, including simultaneous requests. Do not rely on the controller, a race-prone existence check alone, or a unique `(team_id, status)` constraint that would also restrict closed history.

Return teams with member names initialized before leaving the service transaction, so response mapping does not depend on Open Session in View. Retrospective responses only need the owning team's ID, not the entire team or feedback graph.

Choose repeated-closure behavior separately. This task maps a reported business violation to `409` but does not decide that a repeated close must be a violation rather than an idempotent success. Other unresolved date defaults, membership checks, and action-item policies remain in `decisions.md`.

Do not expose JPA IDs, versions, status setters, or entity relationships as directly bindable request fields. Requests describe allowed operations, not arbitrary entity mutation.

## Running the contract checks

On the specified JDK 21:

```powershell
mvn '-Dtest=TeamControllerTest,RetrospectiveControllerTest,ApiErrorHandlingTest' test
```

On this workstation's installed JDK 25, the existing Mockito/Byte Buddy version needs its compatibility flag:

```powershell
mvn '-DargLine=-Dnet.bytebuddy.experimental=true' '-Dtest=TeamControllerTest,RetrospectiveControllerTest,ApiErrorHandlingTest' test
```

The inherited business-rule tests are intentionally unchanged and will fail against service stubs until business logic is implemented. They now compile against the explicit service contracts.
