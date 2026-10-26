# RetroFlow

Backend API for StackCraft's internal retrospective tool. Replaces the mix of sticky notes,
Miro boards, and shared Google Docs the 12 dev teams currently use to run retros.

## Tech stack (as actually configured)

- Java 21
- Spring Boot 3.3.4 (web, data-jpa, validation)
- **Maven**: `pom.xml`. There is no Gradle build.
- **H2 in-memory database**: `jdbc:h2:mem:retroflow`. Data is lost on restart, which is intended
  (see the brief). There is no PostgreSQL and no `application.yml`: configuration lives in
  `src/main/resources/application.properties`.

## Getting started

Requires JDK 21 and Maven 3.9+. No database to install.

```
mvn spring-boot:run
```

- The API is available at http://localhost:8080.
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:retroflow`, user `sa`, no password)

## Running tests

```
mvn test
```

## Project structure

Layer-first packages under `com.stackcraft.retroflow`: `entity`, `repository`, `controller`,
`exception`. See `decisions.md` for why we keep that structure for now.

## Contact

The previous team lead has left the company. Ping #retroflow-support on Slack if you get stuck.
