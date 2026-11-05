# Handoff 109: Task delivery, Task 17 (container error wrapper)
- From: java
- To: liaison
- Type: TASK_DELIVERY
- Branch: feature/task-17-container-errors (local commits, not pushed)

## Resume point
Done: EnvelopeResponseWrapper, RequestGateFilter (SEC-F-113), unit and real-port tests, `./mvnw clean verify` green, SEC-F-128 probes on port 18080 (app stopped), progress.md, status row IN_REVIEW.
Next: Liaison audit and Security review.

## Files
Created: `config/transport/EnvelopeResponseWrapper.java`; tests `EnvelopeResponseWrapperTest`, `RequestGateFailureTest`, `support/ErrorProbeConfig` (test-only filter).
Modified: `RequestGateFilter` (private `HeaderResponse` removed; the whole gate body inside one catch), `TransportSecurityIntegrationTest` (2 new cases), `docs/sdd/progress.md`, `docs/sdd/status.md` (own row).
Deviations: two extra test files (the case for SEC-F-113 and the probe filter) because `RequestGateFilterTest` and `TransportSecurityIntegrationTest` were at the 300-line Checkstyle limit. `EnvelopeResponseWrapper` takes a nested `Rejecter` (the `ErrorResponseWriter::write` method reference), not the writer: SpotBugs EI_EXPOSE_REP2 flags a stored writer and `config/**` may not change; the filter already used this pattern. Mapping of `sendError` status: 400 MALFORMED_REQUEST, 403 FORBIDDEN, 404 NOT_FOUND, 405, 406, 413, 415 to their codes, every other status to INTERNAL_ERROR (500). `sendError` on a committed response is a no-op (the servlet API says IllegalStateException).

## Criteria
1. `EnvelopeResponseWrapper.sendError` -> `ErrorResponseWriter`; `EnvelopeResponseWrapperTest.sendErrorLeavesAsTheEnvelopeOfTheMappedCode` (11 statuses), `sendErrorNeverEchoesTheMessage`; real port `TransportSecurityIntegrationTest.sendErrorInsideTheApplicationLeavesAsTheEnvelopeWithTheHeaders` (500, 418, reset then 404; full header set, `application/json`, no "secret", no HTML).
2. `TransportSecurityIntegrationTest.errorPathAnswersNotFoundInTheEnvelopeWithTheHeaders` (GET and POST `/error`; the gate's route table answers it).
3. SEC-F-114: same two real-port tests assert all 8 headers; `securityHeadersAreSetAtCreationAndSurviveReset`, `sendErrorAfterResetKeepsTheHeadersAndWritesTheEnvelope`. The wrapper writes the envelope itself, so no container error dispatch happens for an application `sendError`.
SEC-F-113: `RequestGateFailureTest.unexpectedFailureBeforeTheControllerBecomesTheEnvelopeWithTheHeaders` (a mocked `RouteTable` throws; 500 INTERNAL_ERROR, headers, no text echoed, chain not reached). Note: an IOException in body buffering now also gives 500 (was propagated).

## Real-port table (jar on 127.0.0.1:18080, raw sockets, `Host: localhost:18080`)
Headers column: all 8 security headers present (Y) or absent (n). No `Server` header in any row.
| Request | Status | Answered by | Body | Headers |
|---|---|---|---|---|
| GET /error | 404 | application | envelope NOT_FOUND | 8 of 8 |
| GET /api/v1/books | 200 | application | list | 8 of 8 |
| GET /api/v1/books/%2e%2e | 404 | application | envelope NOT_FOUND | 8 of 8 |
| GET /%2e%2e/ | 400 | Tomcat | HTML "HTTP Status 400 – Bad Request", no report, no version, no path | 0 of 8 |
| GET /..;/ | 400 | Tomcat | same HTML 400 | 0 of 8 |
| GET /api/v1%2fbooks | 400 | Tomcat | same HTML 400 | 0 of 8 |
| GET /a%00b | 400 | Tomcat | same HTML 400 | 0 of 8 |
| GET /WEB-INF/web.xml | 404 | Tomcat | HTML "HTTP Status 404 – Not Found" (431 bytes), no report, no path | 0 of 8 |
| GET /META-INF/x | 404 | Tomcat | same HTML 404 | 0 of 8 |
Forced `sendError` rows (500, 418, reset+404) run in the test, which prints `EVIDENCE` lines: status 500/500/404, `Content-Type: application/json;charset=UTF-8`, 12 header lines, envelope bodies, `Server: null`.

## SEC-F-128 finding
Tomcat answers the five paths itself before the application: the `ErrorReportValve` (report and server info off, Task 06) renders a minimal HTML page with no version, no path reflection, no state change, but without the security headers (`Content-Type: text/html;charset=utf-8`, `Content-Language: en`). No connector setting was added (ASK FIRST). The finding is covered by R-13 and R-23 under constitution 1.5. Rows that still need a Task 49 raw-socket test: `/%2e%2e/`, `/..;/`, `%2f`, `%00`, `/WEB-INF/web.xml` (assert status in 400/404, no product, version or echo, next request served). Open question for the Sponsor: whether the missing security headers on these HTML pages need a decision beyond 1.5 (they carry no active content, but there is no CSP or nosniff on them).

## Evidence
`cd backend && ./mvnw clean verify`: BUILD SUCCESS; unit/slice 474 tests, 0 failures; integration 146 tests, 0 failures, 1 skipped (pre-existing); Checkstyle and SpotBugs clean; "All coverage checks have been met"; `EnvelopeResponseWrapper` 30 of 30 lines, 12 of 12 branches covered; whole project 619 lines covered, 23 missed.
`git diff --stat main..HEAD` also lists changes already on `origin/main` (local `main` is behind); the Task 17 files are the six under `backend/`. Commit identity `krlosferra <carlos.ferra@globant.com>`, no tool names or trailers:
- feat(transport): turn sendError into the envelope and wrap the whole gate in the catch [Task-17]
- test(transport): envelope wrapper, gate failure and sendError real-port cases [Task-17]
