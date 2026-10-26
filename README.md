# RetroFlow

RetroFlow is the backend REST API for StackCraft's internal retrospective tool. Teams record their
retrospectives, the feedback given in them and the action items that come out of them, so there
is finally one place that shows whether those action items were ever resolved.

> **Status (end of Block 1):** the entity model, the endpoints for teams and retrospectives, the
> validation and the error handling are in place. The service methods are still stubs, so a
> *valid* request to a team or retrospective endpoint returns `500` until Block 2 implements the
> business rules. Invalid requests already return `400`.

## Tech stack

- Java 21, Spring Boot 3.3.4 (web, data-jpa, validation, actuator)
- Maven, via the included wrapper (`./mvnw`). You don't need to install Maven.
- H2 in-memory database. Data is gone after a restart, which is intended (see the brief).
- JaCoCo coverage gate: `verify` fails below 80% line or 70% branch coverage

## Getting started (under 5 minutes)

Pick **one** of the options below.

**A. Docker only.** No JDK needed.

```bash
git clone <your-fork-url> retroflow && cd retroflow
docker compose up --build        # first build ~2 min, later builds are cached
```

**B. Local JDK 21.** Check it with `java -version`.

```bash
git clone <your-fork-url> retroflow && cd retroflow
./mvnw spring-boot:run           # Windows: mvnw.cmd spring-boot:run
```

**C. Dev container / Codespaces.** Open the repo, choose *Reopen in Container*, then run
`./mvnw spring-boot:run`.

Check that it runs:

```bash
curl http://localhost:8080/actuator/health      # {"status":"UP"}
```

| What | Where |
| --- | --- |
| API | http://localhost:8080/api |
| Health | http://localhost:8080/actuator/health |
| H2 console | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:retroflow`, user `sa`, no password) |

## Running the tests

| Command | What it does |
| --- | --- |
| `./mvnw test` | Unit and integration tests |
| `./mvnw verify` | Tests and the JaCoCo coverage gate. The HTML report is in `target/site/jacoco/index.html` |
| `docker compose run --rm test` | The same tests, inside Docker (no JDK needed) |

The three tests in `RetroflowBusinessRulesTest` come from the previous vendor. They fail until
Block 2 implements the service layer. **Do not change them to make them pass.**

CI (`.github/workflows/ci.yml`) runs `mvn verify` on every push and on pull requests. On a pull
request it also comments the coverage.

## Development loop

- **IDE:** import as a Maven project. The run configuration is `RetroflowApplication`.
- **Docker, with live source:** `docker compose run --rm --service-ports dev` starts the app from
  your working copy and shares a Maven cache volume. Stop it with `Ctrl+C` and run the command
  again to pick up changes.
- **Stop Docker:** `docker compose down`

## API

Every error has the same shape (StackCraft standard 2.2):

```json
{ "error": "Retrospective 12: already closed", "status": 409, "timestamp": "2026-10-26T10:30:00Z" }
```

`400` = invalid input, `404` = unknown id, `409` = business rule violation.
The full contract, including the endpoints for feedback and action items that are planned for
Block 2, is in [`decisions.md`](decisions.md#endpoint-contract).

| Method | Path | Success |
| --- | --- | --- |
| POST | `/api/teams` | 201 |
| GET | `/api/teams` | 200 |
| GET | `/api/teams/{id}` | 200 |
| POST | `/api/teams/{teamId}/retrospectives` | 201 |
| GET | `/api/teams/{teamId}/retrospectives` | 200 |
| GET | `/api/retrospectives/{id}` | 200 |
| PUT | `/api/retrospectives/{id}/close` | 200 |

Try it by hand:

```bash
# Create a team
curl -i -X POST localhost:8080/api/teams \
  -H 'Content-Type: application/json' \
  -d '{"name":"Team Alpha","members":["Alice","Bob"]}'

# Invalid input returns 400 with the reason
curl -i -X POST localhost:8080/api/teams \
  -H 'Content-Type: application/json' \
  -d '{"name":"","members":[]}'

# Create a retrospective for team 1 (date is optional, it defaults to today)
curl -i -X POST localhost:8080/api/teams/1/retrospectives \
  -H 'Content-Type: application/json' \
  -d '{"title":"Sprint 1 Retro","date":"2026-10-26"}'

# Close it
curl -i -X PUT localhost:8080/api/retrospectives/1/close
```

## Project files

| File | What it is for |
| --- | --- |
| `decisions.md` | Our interpretation of everything the brief leaves open |
| `test-matrix.md` | One row per business rule and operation. Block 2 writes a test per row |

The code uses layer-first packages under `com.stackcraft.retroflow` (`entity`, `repository`,
`service`, `controller`, `dto`, `exception`). `decisions.md` (G4) explains why.

## Contact

The previous team lead has left the company. Ping #retroflow-support on Slack if you get stuck.
