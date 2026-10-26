# RetroFlow house rules for AI assistants

- Stack: Java 21, Spring Boot 3.3, Maven (`./mvnw`), Spring Data JPA on in-memory H2, Jakarta (never `javax`). Follow the StackCraft coding standards (`stackcraft-coding-standards.md` in the course repo); our spec decisions are in `decisions.md`, the rules to test in `test-matrix.md`.
- Constructor injection only, no `@Autowired` fields. No Lombok: write constructors, getters and setters explicitly.
- Business rules live in the service layer and throw `RetroflowException` (409), `NotFoundException` (404) or `InvalidInputException` (400). Messages are `[EntityType] [id]: [reason]`, e.g. `Retrospective 42: already closed`.
- Controllers validate input with bean validation on request records, contain no logic, and never return entities. Use the status codes from standard 4.1 (201 create, 204 delete, 409 business rule).
- Methods at most 20 lines, classes at most 200. JavaDoc with `@param`/`@return`/`@throws` on every public service method.
- Tests: name them `method_condition_expectedBehaviour`, use `// Arrange`, `// Act`, `// Assert` comments, named constants instead of magic values, and assert real values and messages, not just "not null".
- When the brief or `decisions.md` doesn't answer a question, flag it and ask. Don't pick a default silently.
- When you have a choice between approaches, explain briefly why you chose yours.
- Before you say you're done: run `./mvnw test` (or `./mvnw verify` for coverage) and report the real result.
- **Never change, skip or disable a test just to make it pass.** If a test looks wrong, stop and ask us first. `RetroflowBusinessRulesTest` is never edited.
- **Never add a dependency or plugin without first checking that the exact version exists on Maven Central.** Say which version you checked, and ask before adding it.
- Never put secrets in code or config, never concatenate strings into JPQL/SQL, and never return stack traces or class names to API clients. The OpenAI key only comes from the `OPENAI_API_KEY` environment variable.
- Tests tagged `eval` call a paid model (`./mvnw test -Peval`): only run them when asked. Normal tests never call a real model; stub `SummaryModel` instead.
- Tools in this repo: StackCraft standards via the `ai-track-standards` MCP server, mechanical checks via `check_standards` (`retroflow-decisions` server), skills in `.claude/skills/`, agents `reviewer` (read and run only) and `implementer`. See `TEAM-KIT.md`.
