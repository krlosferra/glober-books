# Handoff 057: Task brief, Task 05 (error foundation)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 05 of `docs/sdd/tasks.md` (v0.5.1): read its text, criteria 1 to 4 and traces. Branch `feature/task-05-error-foundation` is checked out; Tasks 01, 02, 03a, 03b, 10, 11 are merged. Read also: `spec.md` Appendix A (codes, statuses, `details` shapes, messages), section 5.0 error envelope, INV-6; `plan.md` D2 (one factory; `ErrorMvcAutoConfiguration` excluded; two body records `ErrorBody` and `DuplicateErrorBody`, no `details` of type `Object`); `plan-appendix.md` AP-1 (error package), AP-3; constitution error-handling standards; `security.md` SC-08, SC-11, SC-29.

## Scope
`error/ErrorCode.java`, `error/ErrorBody.java` (nested field problem), `error/ErrorBodyFactory.java`, `error/ErrorResponseWriter.java` and `ErrorBodyFactoryTest`. Out of scope: `DuplicateErrorBody` and `ApiException` (Task 22), `GlobalExceptionHandler` (Task 06), any filter or controller. The factory takes the injected `Clock` (from `ClockConfig`); the timestamp format is the one of the spec (UTC, milliseconds, `Z`). Messages are fixed strings from Appendix A; none contains user input, class names, SQL or paths.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; a test per code asserting status and exact body fields (expected values copied from Appendix A, not from the code); the writer sets `application/json; charset=UTF-8` and writes only the envelope; a reflection or compile-level check that no `details` member is of type `Object`; the real coverage ratio of the `error` package; `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'`.

## Rules
STOP and report if Appendix A and the plan disagree. Commit after each finished step (`type(scope): subject [Task-5]`), local commits only, explicit paths, no AI attribution, never `--no-verify`. Update `docs/sdd/progress.md` in the same change. Carry-over cleanups from the 03b audit that fit here: remove the stray `System.out` in `StartupPolicyIntegrationTest` and make the file-API scan test independent of the working directory. Write the delivery as `docs/sdd/handoffs/058-java-to-liaison-task-05-delivery.md` and keep a "Resume point" section at its end (done / next) so work is never lost if you are cut off.
