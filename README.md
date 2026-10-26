# RetroFlow

Backend API for StackCraft's internal retrospective tool. Replaces the mix of sticky notes,
Miro boards, and shared Google Docs the 12 dev teams currently use to run retros.

## Tech stack

- Java 21
- Spring Boot 3
- Maven
- In-memory H2 database

## Getting started

1. Install JDK 21 or newer and Maven 3.9 or newer.
2. From the project root, run the app:

   ```
   mvn spring-boot:run
   ```

   The API will be available at http://localhost:8080. The application uses an in-memory H2
   database, so no separate database server is required.

## Running tests

```
mvn test
```

## Project structure

Standard Spring Boot layered structure — controllers, services, repositories, entities.

## Current status

Core domain model (Team, Retrospective, FeedbackItem, ActionItem) and CRUD endpoints are in
progress. The entity model and five controller contracts are implemented; services are
explicitly unimplemented stubs, so valid requests reaching them currently return `500`.
See [the API contracts and decisions](prompt-4.md) for request/response examples and
targeted controller-test commands. The inherited business-rule tests remain pending
service implementation.

| Method | Endpoint | Intended success status |
|---|---|---|
| POST | `/api/teams` | 201 |
| GET | `/api/teams/{id}` | 200 |
| POST | `/api/teams/{teamId}/retrospectives` | 201 |
| GET | `/api/teams/{teamId}/retrospectives` | 200 |
| PUT | `/api/retrospectives/{id}/close` | 204 |

## Contact

The previous team lead has left the company, so there is no forwarding contact. Ping #retroflow-support on
Slack if you get stuck, though response times have been slow lately.
