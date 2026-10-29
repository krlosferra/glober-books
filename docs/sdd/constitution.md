# Project Constitution: GloberBooks

> Status: DRAFT v0.1. It must pass the Phase 1 validation prompt before it is treated as final.
> This document is the highest authority in the repository. If `spec.md`, `plan.md`, `tasks.md` or code conflict with it, the constitution wins until it is amended by the human.

## Tech Stack
- **Language/Runtime:** Java 21 (LTS)
- **Backend framework:** Spring Boot 3.x (web, validation, data-jpa)
- **Build tool:** Maven (single module, Maven Wrapper committed)
- **Database:** SQLite (file-based, path set by configuration) accessed through Spring Data JPA / Hibernate
- **Frontend:** Plain HTML + CSS + vanilla JavaScript, served as static resources by the Spring Boot application. No frontend build step and no frontend framework.
- **Testing:** JUnit 5, Mockito, AssertJ, Spring Boot Test; JaCoCo for coverage

### Approved dependencies
Only the following are approved. Anything else falls under ASK FIRST.
- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-data-jpa`
- `org.xerial:sqlite-jdbc`
- `org.hibernate.orm:hibernate-community-dialects` (SQLite dialect)
- `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ)
- `jacoco-maven-plugin`

## Architecture Principles
- Layered architecture: Controllers → Services → Repositories (Data Access). Dependencies point downward only.
- Controllers handle HTTP concerns only (parsing, status codes, DTO mapping). They contain no business rules.
- Services own all business rules (status validation, rating integrity, duplicate detection). They never touch HTTP types.
- Repositories own persistence only. They contain no business rules.
- Constructor-based dependency injection only (no field injection).
- API request/response objects (DTOs) are separate from persistence entities. Entities are never serialized directly.
- Single Responsibility Principle for all classes.
- Business logic is synchronous and blocking. No reactive stack and no ad-hoc threading unless a spec requirement demands it and the human approves.

## Boundaries

### ✅ ALWAYS DO:
- Write unit tests for all business logic before finalizing a task
- Follow RESTful API naming conventions (use `/api/v1/` prefix)
- Use explicit error handling and return standard HTTP status codes
- Add Javadoc comments for all public classes and public methods
- Validate all user inputs at the API boundary (Bean Validation) and re-check business rules in the service layer
- Use parameterized queries or JPA-managed queries only
- Keep each change limited to the files listed in the current task
- Update `progress.md` and commit after each accepted task

### ⚠️ ASK FIRST:
- Before adding any new third-party dependencies/libraries (anything not in "Approved dependencies")
- Before modifying the database schema
- Before adding new entities/tables
- Before changing architectural patterns
- Before adding or changing an API endpoint, request field or response field not defined in `spec.md`

### 🚫 NEVER DO:
- Never hardcode secrets, API keys, or sensitive data in code
- Never skip writing tests for business logic
- Never use `SELECT *` in hand-written queries (list explicit columns or use explicit projections)
- Never store passwords in plain text
- Never add features not in `spec.md`
- Never expose stack traces, SQL or internal class names in API responses
- Never serialize JPA entities directly in API responses
- Never modify files outside the scope of the current task
- Never commit code that fails the test suite or the coverage gate

## Code Style
- Naming: camelCase for variables, fields and methods; PascalCase for classes, interfaces, enums and records; UPPER_SNAKE_CASE for constants
- Packages: lowercase, rooted at `com.globant.globerbooks`
- Max function length: 50 lines
- Max file length: 300 lines
- Descriptive variable names (minimum 3 characters). The only exception is the identifier `id`.
- One public top-level type per file
- Prefer immutable types (records for DTOs) and `final` fields where practical
- No wildcard imports; no unused imports or dead code

## Configuration and Secrets
- All environment-specific values (database file path, server port) come from `application.properties`/`application.yml` or environment variables, with safe defaults for local development.
- No secret, credential or token may exist in source control. If one becomes necessary, the human must be asked first.
- The SQLite database file and any local config overrides must be listed in `.gitignore`.

## Security Standards
- Apply Bean Validation constraints to every request DTO, including maximum lengths (title 500, author 200, bio 500 once specified).
- Reject unknown or malformed JSON bodies with `400`.
- Use only parameterized or JPA-managed queries to prevent SQL injection.
- The frontend must render user-supplied text using `textContent` (or equivalent escaping), never `innerHTML` with unescaped input.
- Error responses must not leak internals (see Error Handling Standards).
- No authentication or multi-user features exist in this project (out of scope), so none may be added.
- Logs must not contain user-supplied values beyond identifiers needed for debugging.

## Error Handling Standards
All errors must return:
```json
{
  "error": "Human-readable message",
  "code": "ERROR_CODE",
  "timestamp": "ISO-8601"
}
```
- `timestamp` is UTC, formatted ISO-8601 (e.g. `2024-02-19T10:00:00Z`).
- `code` is a stable UPPER_SNAKE_CASE value. Codes defined by `spec.md` (for example `INVALID_STATUS`, `INVALID_RATING`, `RATING_NOT_ALLOWED`) must be used exactly as written there.
- For validation errors, `error` must identify the offending field(s).
- A single global exception handler produces all error responses. Controllers must not build error bodies by hand.

Status codes:
- 200: Success with data
- 201: Resource created
- 400: Validation errors (return field-specific messages)
- 404: Resource not found
- 409: Conflict (e.g. duplicate book, as defined in `spec.md`)
- 500: Unexpected server errors (generic message only)

## Testing Requirements
- Minimum 80% line coverage, enforced by a JaCoCo check that fails the Maven build below the threshold.
- Unit tests: one test class per service and per validation component; mock the repository and any other collaborator.
- Integration tests: exercise the HTTP layer against a real SQLite database (a temporary file or in-memory instance), never against mocks.
- Test class naming: `[ClassName]Test` for unit tests and `[ClassName]IntegrationTest` for integration tests.
- Use AAA pattern: Arrange, Act, Assert (separated by blank lines or comments).
- Test method names must describe behavior (e.g. `rejectsRatingWhenBookNotCompleted`).
- Every business rule in `spec.md` (BR-xxx) must have at least one passing test and one failing-case test.
- Tests must be deterministic: no dependence on wall-clock time, ordering, or shared state between tests.
- A task is not complete until `mvn test` (including the coverage check) passes.

## API Standards
- All endpoints prefixed with `/api/v1/`
- RESTful conventions (GET/POST/PUT/DELETE); resource names are plural nouns
- JSON field names use snake_case, matching the API contract in `spec.md` (e.g. `date_added`); Java code maps to them via a global Jackson naming strategy
- All timestamps are UTC, ISO-8601
- Consistent response format: success bodies follow `spec.md` exactly; error bodies follow Error Handling Standards
- The API contract in `spec.md` is the single source of truth for paths, fields and status codes

## Workflow Rules (SDD)
- Governance order of authority: `constitution.md` → `spec.md` → `plan.md` → `tasks.md` → code.
- No code is written for behavior that is not in `spec.md`. A new idea means update `spec.md` first, then `plan.md`, then `tasks.md`, then implement.
- Work proceeds one task at a time from `tasks.md`. The agent stops after each task and waits for human approval.
- One commit per accepted task, message format: `[Task-N] Description - ACCEPTED`.
- Documentation (`spec.md`, `plan.md`, `tasks.md`, `progress.md`) must be updated in the same change as any code that affects it.
