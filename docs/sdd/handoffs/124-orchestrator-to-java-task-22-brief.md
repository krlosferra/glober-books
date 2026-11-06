# Handoff 124: Task brief, Task 22 (duplicate key and service exceptions)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 22 of `docs/sdd/tasks.md` (v0.6.1): read its text, criteria 1 to 4 and traces. Branch `feature/task-22-duplicates` is checked out; Tasks 01 to 21 are merged. The task header names the branch `feature/task-22-duplicate-key`; keep the checked-out name and record the difference. Read also: `spec.md` BR-003 (duplicate key definition, line "Definition: the duplicate key of a (title, author) pair..."), US-004.5, T-40, Appendix A and 5.2 (the 409 `DUPLICATE_BOOK` body with `details.existing_book`), Appendix D rows for 409; `plan.md` D2 and `plan-appendix.md` (DuplicateKey, ApiException, DuplicateBookException, DuplicateErrorBody, ErrorBodyFactory, "no `details` of type `Object`"); `constitution.md` Error Handling Standards and Code Style (300 lines); `security.md` SC-02, SC-16 and the existing findings on error bodies; the existing `error/*` (ErrorBodyFactory, ErrorBody, GlobalExceptionHandler, BookValidationException), `service/TextPolicy` and `service/BookService`.

## Scope
`service/DuplicateKey.java`, `error/ApiException.java`, `error/DuplicateBookException.java`, `error/DuplicateErrorBody.java`, edit to `error/GlobalExceptionHandler.java`, tests `DuplicateKeyTest` and a case in `GlobalExceptionHandlerTest`. Out of scope: the add rules and capacity check themselves (Task 23a), the database unique index, the controller, any new dependency, any change to `pom.xml` or `backend/config/**`.

## Binding constraints
1. Key: from the already trimmed NFC text, normalize to NFKC, `toLowerCase(Locale.ROOT)` (not case folding, not locale-sensitive), collapse every run of Unicode White_Space inside to one space, trim. Pair key = (title key, author key). "Straße" and "STRASSE" must NOT match; "Clean Code" and "CLEAN CODE" match; " clean   code " and the full-width "Ｃlean Code" match "Clean Code" (the full-width case must come out of NFKC, so the test input is built from code points).
2. The ASCII fast path (if you add one for speed) must produce the same result as the general path; prove it on a generated set (fixed seed, at least 10,000 pairs of ASCII strings including spaces, tabs excluded by BR-006) and on boundary cases; if there is no fast path, say so and still test the general path property (idempotence: key(key(x)) = key(x)).
3. Keys are computed in application logic, never part of the API, never logged, never in an error body.
4. `ApiException` is the base for service exceptions carrying code and HTTP status; the handler turns it into the envelope through `ErrorBodyFactory` (single source of bodies). `DuplicateBookException` produces `409 DUPLICATE_BOOK` with `details.existing_book` containing exactly one book in the representation of Appendix A (no extra members, no list, no key, nothing else), built by `DuplicateErrorBody` (typed record, no `Object`). Reuse `BookValidationException` as is (do not break Task 20).
5. No user text in messages, logs or exception messages (SC-16): the exception message is the code name; the existing book fields only appear in the body as the spec states.
6. Expected values come from the spec examples, not from the code. All files at or under 300 lines.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; a table of criteria and spec rows (BR-003, US-004.5, T-40, Appendix A, 5.2) to tests; the generated-set result; the handler test showing the exact JSON of the 409 body; real line coverage of the new classes; `git diff --stat origin/main..HEAD` and `git log origin/main..HEAD --format='%an <%ae>%n%B---'` pasted inside the delivery file.

## Rules
STOP and report on any spec gap or JDK behaviour that differs from the plan (for example a Unicode case where `toLowerCase(Locale.ROOT)` differs from the spec statement). Commit after each finished step (`type(scope): subject [Task-22]`), local commits only, explicit paths, never `--no-verify`, nothing naming a tool, model or vendor as author or co-author, never session links or trailers. Update `docs/sdd/progress.md` in the same change. Write the delivery as `docs/sdd/handoffs/125-java-to-liaison-task-22-delivery.md` early with a "Resume point" section (done / next) kept updated so nothing is lost if you are cut off.
