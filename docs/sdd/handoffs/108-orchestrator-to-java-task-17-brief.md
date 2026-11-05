# Handoff 108: Task brief, Task 17 (container error wrapper)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 17 of `docs/sdd/tasks.md` (v0.6.1): read its text, criteria 1 to 3 and traces. Branch `feature/task-17-container-errors` is checked out; Tasks 01 to 16 are merged. Read also: `plan.md` D2; `plan-appendix.md` AP-1 (EnvelopeResponseWrapper row), AP-2 (stage 1 re-applies headers on `reset` and `sendError`); `constitution.md` Error Handling Standards (including the container-first rule of 1.5) and Security Standards; `security.md` SC-02, SC-11, SC-29, R-13, R-23, SEC-F-113, SEC-F-114, SEC-F-128; the existing `config/transport/*` (RequestGateFilter, HeaderPolicy, TomcatErrorReportCustomizer) and the error factory and writer.

## Scope
`config/transport/EnvelopeResponseWrapper` (new), edits to `RequestGateFilter` (SEC-F-113), tests `EnvelopeResponseWrapperTest` and a case in `TransportSecurityIntegrationTest`. Out of scope: the Task 49 raw-socket rows, any new error code, any change under `backend/config/**` or `pom.xml`, any new dependency.

## Binding constraints
1. Criterion 1: a `sendError(status[, message])` raised inside the application (filter, controller, static handler) leaves as the envelope (status mapped to the existing codes; unknown statuses to the generic INTERNAL_ERROR/NOT_FOUND mapping already in the factory) with the full security headers and `application/json`, never as a container page. The message passed to `sendError` is never echoed.
2. Criterion 2: `GET /error` (and the error dispatch path in general) answers `404 NOT_FOUND` in the envelope with the headers.
3. Criterion 3 (SEC-F-114): a test shows that an ERROR dispatch cannot bypass the headers of the gate: assert the full header set on a response produced through `sendError` and on the `/error` path, and that a response that was reset keeps the headers.
4. SEC-F-113: the whole body of `RequestGateFilter.doFilterInternal`, header checks and body buffering included, runs inside the catch that yields `500 INTERNAL_ERROR` in the envelope; add a test that makes a collaborator throw an unexpected runtime exception before the controller and observes the envelope with the headers (test-only seam, not a property).
5. SEC-F-128: container-first 400/404 pages (`/%2e%2e/`, `/..;/`, `%2f`, `%00`, `/WEB-INF/web.xml`) are answered by Tomcat before the application. Probe each on the real port and record the real status, body and headers in the delivery. Where a page reaches the application as a `sendError`, it must now be the envelope. Where Tomcat answers it itself, do NOT add a connector setting (ASK FIRST); record it as a finding covered by R-13 and R-23 under constitution 1.5 (no version, no path reflection, no state change) and state which rows still need a Task 49 raw-socket test.
6. Reuse `ErrorBodyFactory` and `ErrorResponseWriter`; no duplicated body building; `HeaderPolicy` stays the single source of header values.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; a table of real-port requests (`/error`, a forced `sendError` route if any test-only route is needed, the SEC-F-128 paths) with status, body code and the presence of each security header; real line coverage of the new class; `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'` (paste both).

## Rules
STOP and report on any Boot 4.1.2 or Tomcat behaviour that differs from the plan and any spec gap. Commit after each finished step (`type(scope): subject [Task-17]`), local commits only, explicit paths, never `--no-verify`, nothing naming a tool, model or vendor as author or co-author. Update `docs/sdd/progress.md` in the same change. Use port 18080 only if E2E is needed; stop everything you start. Write the delivery as `docs/sdd/handoffs/109-java-to-liaison-task-17-delivery.md` early with a "Resume point" section (done / next) kept updated so nothing is lost if you are cut off.
