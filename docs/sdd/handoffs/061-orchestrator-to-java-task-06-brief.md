# Handoff 061: Task brief, Task 06 (thin gate, exception advice, container error report)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 06 of `docs/sdd/tasks.md` (v0.5.1): read its text, criteria 1 to 5 and traces. Branch `feature/task-06-thin-gate` is checked out; Tasks 01, 02, 03a, 03b, 05, 10, 11 are merged. Read also: `spec.md` BR-012 step 1 (sub-order Host, Origin then Sec-Fetch-Site, Content-Type), 5.0 transport rules, Appendix A, Appendix D rows E-04, E-07 to E-14, E-21, E-23 (expected statuses and codes); `plan.md` D1, D2, D5, `plan-appendix.md` AP-1 and AP-2 (stages 1 to 5); constitution Security Standards (headers, Host allowlist, CSRF layers, body cap 16 KiB streaming); `security.md` SC-02 to SC-06, SC-10, SC-11, SC-28, SC-29, SEC-F-066, SEC-F-110.

## Scope
`config/transport/RequestGateFilter`, `HeaderPolicy`, `BoundedBodyRequest`, `TomcatErrorReportCustomizer`, `error/GlobalExceptionHandler` and tests `HeaderPolicyTest`, `RequestGateFilterTest`, `GlobalExceptionHandlerTest`. Out of scope: route table, Accept, id and query rules (Tasks 15, 16), strict JSON (21), the container wrapper (17), the real-port class (Task 08), any controller.

## Binding constraints from the reviews
1. Use the existing `ErrorBodyFactory` and `ErrorResponseWriter` only. The writer uses `getOutputStream()`: never call `getWriter()` before it (SEC-F-110); add a test for an already committed response. Do not add a second error mapper or `ObjectMapper`.
2. Highest precedence for the filter, Spring's `CharacterEncodingFilter` disabled (AP-2). The filter never calls `getParameter*`.
3. Host check against the raw header: only `localhost:<port>` and `127.0.0.1:<port>` (case-insensitive), absent or repeated fails (`400 INVALID_HOST`); the port is the local port.
4. Body cap: a declared length over 16,384 gives `413` before any read and no `100 Continue`; chunked and unread bodies are buffered and stopped at byte 16,385; `Connection: close` on early rejection.
5. The error valve: `showReport=false`, `showServerInfo=false`, unconditional (a customizer registered with the web server factory).
6. Errors in the advice: the catch-all logs exactly one stack-trace line with no user text and answers `INTERNAL_ERROR`; the response is JSON even for `Accept: text/xml`.
7. Express nothing beyond the tasks.md list; no new dependency; STOP and report where Boot 4.1.2 differs from the plan (APIs for the error valve or filter registration may have changed), and do not change SC-06 values.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; a table of requests with the real status and body for: forged Hosts, allowed Hosts, bad Origin, bad Sec-Fetch-Site, missing or wrong Content-Type on POST/PUT/DELETE, declared over-limit body (and proof that no body byte was read), chunked over-limit body, a forced unexpected exception, `Accept: text/xml`; headers present on a normal response, after `sendError`, and after `reset`; a container-level error page (for example an invalid request line sent on a raw socket) showing no product, version or report; real coverage of the new classes; `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'`.

## Rules
Commit after each finished step (`type(scope): subject [Task-6]`), local commits only, explicit paths, no AI attribution, never `--no-verify`. Update `docs/sdd/progress.md` in the same change. This task is dense: if you estimate it will not fit, split it at a clean boundary (for example advice and error valve first, filter second) and report the split instead of overrunning. Write the delivery as `docs/sdd/handoffs/062-java-to-liaison-task-06-delivery.md` early and keep a "Resume point" section (done / next) at its end so work is never lost if you are cut off.
