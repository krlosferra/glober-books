# Handoff 116: Task brief, Task 20 (validation wiring and field-error mapping)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 20 of `docs/sdd/tasks.md` (v0.6.1): read its text, criteria 1 to 4 and traces. Branch `feature/task-20-validation` is checked out; Tasks 01 to 19 are merged (`TextPolicy` with `check` and `normalize` exists). The task header names the branch `feature/task-20-validation-wiring`; the checked-out name stays, record the difference. Read also: `spec.md` BR-001, BR-006 (the message "contains characters that are not allowed" and the check order required, empty, length, characters), BR-012 steps 5 and 6, US-001.4 to .6 and .10, Appendix D rows for validation, T-09, T-13, T-34, T-35; `plan.md` and `plan-appendix.md` (BookText, ValidatorConfig, GlobalExceptionHandler takes `RouteTable`); `constitution.md` Security Standards (no expression language, no user text in messages or logs), Error Handling Standards, Code Style (300 lines); `security.md` SC-16, SC-02; the existing `service/TextPolicy`, `error/*`, `dto/CreateBookRequest`, the Task 07 thin checks.

## Scope
`config/BookText.java` (a Bean Validation constraint plus validator that calls `TextPolicy.check`), `config/ValidatorConfig.java` (validator factory without expression language: a message interpolator that never evaluates `${...}` or `{...}` from user input), edits to `error/GlobalExceptionHandler.java` and `dto/CreateBookRequest.java`, removal of the thin checks of Task 07 in the service (the service throws the same exception type), tests `BookTextTest`, `GlobalExceptionHandlerTest` (edit). Out of scope: strict body reading and lone surrogates (Task 21), the rating field and its codes beyond what BR-012 step 6 needs now, duplicates, any new dependency, any change under `backend/config/**` (the resource folder, not the Java package) or `pom.xml`.

## Binding constraints
1. Messages are fixed strings from the spec ("is required", "must not be empty", the length message, "contains characters that are not allowed"); never built from user input; no constraint message template contains user text. The Bean Validation interpolator must not use EL (no `jakarta.el` dependency, no `${}` evaluation).
2. One `details` entry per failing field, ordered title, author, status; `field` is `title`, `author` or `status`; the first failing check per field wins in the order required, empty, length, characters.
3. BR-012 step 6 selection: only `status` failing gives `INVALID_STATUS`; `status` plus any other failing field gives `VALIDATION_ERROR`; absent status is `unread`; explicit `null` status is `INVALID_STATUS`; `{}` gives title and author entries.
4. The stored text is `TextPolicy.normalize` output; the service repeats `check` and throws the same exception type as before (defence in depth, no duplication of rules: call `TextPolicy`).
5. T-13: a 501-character title containing `${7*7}` is rejected; assert no response body, log line or exception message contains `${7*7}` or `49` as an echo. Add a real-port or MockMvc-with-full-context case, not only a unit test.
6. Carry from Task 18+19 follow-ups: add a test row showing that item 6 is judged after NFC; build the Persian word of the tests from code points (U+200C asserted) where touched. Use `TextPolicy` as-is; replacing the regex matcher in `isWhiteSpace` is optional and out of scope unless trivial.
7. No logging of request text anywhere in the new code. Every file at or under 300 lines.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; a table of criteria and spec rows (T-09, T-13, T-34, T-35, US-001.4 to .6, .10) to tests; a real-port request/response table for: `{}`, title 501, author with U+200B, `status` invalid only, `status` invalid plus empty title, `"status": null`, the T-13 payload; real line coverage of the new classes; `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'` (paste both in the delivery file itself).

## Rules
STOP and report on any spec gap, any Boot 4.1.2 or Hibernate Validator behaviour that differs from the plan, and any need for a new dependency. Commit after each finished step (`type(scope): subject [Task-20]`), local commits only, explicit paths, never `--no-verify`, nothing naming a tool, model or vendor as author or co-author, never session links or trailers. Update `docs/sdd/progress.md` in the same change. Write the delivery as `docs/sdd/handoffs/117-java-to-liaison-task-20-delivery.md` early with a "Resume point" section (done / next) kept updated so nothing is lost if you are cut off.
