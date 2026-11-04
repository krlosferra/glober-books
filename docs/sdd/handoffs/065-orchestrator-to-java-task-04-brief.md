# Handoff 065: Task brief, Task 04 (domain and repository)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 04 of `docs/sdd/tasks.md` (v0.5.1): read its text, criteria 1 to 4 and traces. Branch `feature/task-04-domain-repository` is checked out; Tasks 01, 02, 03a, 03b, 05, 06, 10, 11 are merged. Read also: `spec.md` Section 4 (data model, formats, INV-1 to INV-4), 5.0 book representation, BR-010 (order); `plan.md` data model and D3; `plan-appendix.md` AP-1 (domain, repository, dto), AP-6 (schema), AP-12.1 and AP-12.3 (stored DDL equality, native SQL inventory); constitution Persistence and Code Style; `security.md` SC-07.

## Scope
`domain/Book` (no public setters; methods `changeStatus`, `assignRating`, `clearRating` may arrive in Task 30 but the entity must already forbid public mutation), `domain/BookStatus` (wire value lower case, `fromWire` returning `Optional`, nested converter), `repository/BookRepository` (Spring Data; the ordered query `date_added` descending then `id` ascending), `dto/BookResponse` (record with the seven members, null written), and `BookRepositoryTest`. The `DataSource` is Boot's for now (Task 34a/34b wire the provider); the test uses a SQLite file in `@TempDir` and `schema.sql`. Out of scope: service, controller, validation, duplicate key, `BookKeyRow` (Task 23).

## Binding constraints from the reviews
1. Criterion 1: Hibernate `validate` must pass on the STRICT table with its CHECKs, and a test must show that the DDL text in `sqlite_master` after the start equals the normalised `schema.sql` statement. **If validation does not pass, STOP and report with the real output: the fallback (`typeof` CHECKs, changing `schema.sql`) is ASK FIRST and is not applied on this task's authority.**
2. Native SQL stays in `config.sqlite`; the repository uses derived or JPQL queries only, no `select *`, no concatenation (SC-07; the Checkstyle ban is active).
3. Timestamps are millisecond UTC instants stored as the 24-character text of the schema; a converter nested in `Book` (AP-1). A saved book returns with identical timestamps.
4. Expected values come from the spec, not from the code under test. The CHECK-rejection tests insert through the database (JDBC or the repository) and assert the rejection for each of: bad id, title length 0 and 501, author length 0 and 201, status out of set, rating 0 and 6, rating on a non-completed book, `date_completed` present when not completed and absent when completed, malformed timestamps. Lengths are counted in code points (a title of 500 emoji passes, 501 fails).
5. No new dependency; no change under `backend/config/**` or `pom.xml`.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; the `ddl-auto: validate` result and the `sqlite_master` text compared with the normalised statement; one line per CHECK rejection; the ordered-query tie test; real line coverage of the new classes; `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'`.

## Rules
Commit after each finished step (`type(scope): subject [Task-4]`), local commits only, explicit paths, no AI attribution, never `--no-verify`. Update `docs/sdd/progress.md` in the same change. Write the delivery as `docs/sdd/handoffs/066-java-to-liaison-task-04-delivery.md` early and keep a "Resume point" section (done / next) so work is never lost if you are cut off.
