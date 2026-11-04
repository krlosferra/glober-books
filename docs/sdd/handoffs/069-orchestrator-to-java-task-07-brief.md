# Handoff 069: Task brief, Task 07 (add and list API, thin)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 07 of `docs/sdd/tasks.md` (v0.5.1): read its text, criteria 1 to 5 and traces. Branch `feature/task-07-add-and-list-thin` is checked out; Tasks 01 to 06, 10 and 11 are merged. Read also: `spec.md` 5.0 (book representation, strict parsing limits for this task are basic only), 5.1 (list), 5.2 (add), US-001.1, US-001.2, US-001.9, US-002.1 to .3, .5, BR-010, X-07; `plan.md` D3 (transactions), D7 (lists streamed, service returns the list after its transaction ends); `plan-appendix.md` AP-1 and AP-3; the existing classes you must reuse: `error/*` (never build error bodies in controllers), `config/transport/*`, `domain/*`, `repository/BookRepository`, `dto/BookResponse`, `config/ClockConfig`.

## Scope
`service/BookService` (add: one `@Transactional` method; list: read-only; id from an injected `Supplier<UUID>` v4, time from the injected `Clock`, cut to milliseconds), `controller/BookController` (POST and GET on `/api/v1/books`, binds and maps only, holds no rule), `dto/CreateBookRequest` (record: `title`, `author`, `status` optional default `unread`, `allow_duplicate` accepted and ignored), `dto/BookListResponse` (`books`, `total`), tests `BookServiceTest` and `BookControllerTest`. Basic Bean Validation only (the full BR-006 text policy is Task 18-20): empty/blank and over-length (500/200 code points) give `400 VALIDATION_ERROR` with the field; an unknown member gives `400 MALFORMED_REQUEST` (enable the minimal unknown-property failure in Jackson config for this; strict parsing is Task 21). Out of scope: duplicate and capacity rules (23), route table, Accept, query rules (15, 16), status/rating endpoints.

## Carried conditions (must be done in this task)
1. SEC-F-118: set the SQLite dialect for the full application in `application.yml` (`spring.jpa.database-platform` or the Boot 4 key; verify it is recognised) and add a startup test that boots the context WITHOUT test-only properties and passes `ddl-auto: validate`.
2. Task 04 audit F1: a contract test proves the wire format: `date_added` and `date_completed` serialised as 24-character UTC text with milliseconds and `Z`, members named in snake_case (`date_added`, `date_completed`), null written (not omitted), the seven members exactly, lower-case status.
3. SEC-F-119: no entity type (`Book`) in any controller signature or return type; only DTOs.
4. SEC-F-071 / criterion 5: the list is serialised straight to the response stream, never to an intermediate `byte[]` or `String`; the service returns after its transaction ends (D7). Show it in the code and by a test that the controller returns a streaming body or writes through the output stream.
5. X-07: a read writes nothing (assert the database file hash or row count unchanged around a list; the driver ignores `setReadOnly`).
6. Text fields carry user input: the controller and service never log request bodies.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; for POST and GET the real status and body via the test client (valid add, empty list, three books newest first with a tie ordered by `id`, empty title, 501-code-point title, unknown member); the wire-format contract test output; the startup-without-test-properties result; real line coverage of the new classes; `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'` (paste both into the delivery).

## Rules
STOP and report on any Boot 4.1.2 or Jackson 3 behaviour that differs from the plan (key names, record binding, null handling) and on any spec gap. Commit after each finished step (`type(scope): subject [Task-7]`), local commits only, explicit paths, no AI attribution, never `--no-verify`. Update `docs/sdd/progress.md` in the same change. The task is at the 45-minute ceiling: if it will not fit, split at a clean boundary (POST first, GET second) and report. Write the delivery as `docs/sdd/handoffs/070-java-to-liaison-task-07-delivery.md` early with a "Resume point" section (done / next) kept updated so nothing is lost if you are cut off.
