# Handoff 062: Task 06 delivery (thin gate, exception advice, container error report)
- From: java
- To: liaison
- Type: TASK_DELIVERY
- Branch: feature/task-06-thin-gate (local commits only, not pushed)

## Summary
Files created: `config/transport/{RequestGateFilter,HeaderPolicy,BoundedBodyRequest,TomcatErrorReportCustomizer}.java`, `error/GlobalExceptionHandler.java`; tests `HeaderPolicyTest` (52), `RequestGateFilterTest` (20), `GlobalExceptionHandlerTest` (6), plus two extra real-port classes `RequestGateFilterIntegrationTest` (5) and `TomcatErrorReportIntegrationTest` (1). Modified: `application.yml` (`spring.servlet.encoding.enabled: false`), `progress.md`, `status.md` row, this file. Nothing else.

## Criteria to tests
1. Headers on every response, re-applied: `RequestGateFilterTest.securityHeadersSurviveResetAndSendError`, `passesAnAllowedGetWithSecurityHeaders`, `HeaderPolicyTest.securityHeadersMatchTheConstitution`; real port: `RequestGateFilterIntegrationTest`.
2. Host/Origin/Sec-Fetch-Site/Content-Type: `HeaderPolicyTest` grammar cases, `RequestGateFilterTest` order and statuses, integration table below.
3. Body cap: `declaredOverLimitBodyGives413WithoutReadingAnyByte` (zero `getInputStream` calls), `chunkedBodyOverLimitGives413AtByte16385`, at-limit cases pass; real port with `Expect: 100-continue` and chunked.
4. 500: `GlobalExceptionHandlerTest` (also `Accept: text/xml`), `RequestGateFilterTest.unexpectedExceptionGives500...`, `failureAfterWriterWasOpenedStillAnswersJson`, `committedResponseIsLeftAloneWhenTheChainFails` (SEC-F-110).
5. Routing exceptions 404/405/406/415 in `GlobalExceptionHandlerTest`; container page: `TomcatErrorReportIntegrationTest`.

## Real output
`cd backend && ./mvnw -o clean verify`: BUILD SUCCESS; unit 155 tests, 0 failures; integration 37 tests, 0 failures, 1 skipped (pre-existing in `SqliteStoreIntegrationTest`); SpotBugs 0 bugs; "All coverage checks have been met". Line coverage of new classes: RequestGateFilter 48/50, HeaderResponse 13/16, BoundedBodyRequest 9/13 (+ stream 5/8, exception 2/2), HeaderPolicy 18/18, TomcatErrorReportCustomizer 13/13, GlobalExceptionHandler 16/16.

Real-port evidence (security headers omitted here for length; every response carried all 8 and none carried `Server`, `Set-Cookie`, `Location`, `Access-Control-*`):
| Request | Status | Code |
|---|---|---|
| Host `evil.example`, `localhost`, `localhost:1`, `[::1]:<port>` | 400 | INVALID_HOST |
| Host `LOCALHOST:<port>`, `127.0.0.1:<port>` (unknown path) | 404 | NOT_FOUND (gate passed) |
| `Origin: http://evil.example` | 403 | FORBIDDEN |
| `Sec-Fetch-Site: cross-site` | 403 | FORBIDDEN |
| POST/PUT/DELETE no Content-Type, and `text/plain` | 415 | UNSUPPORTED_MEDIA_TYPE |
| POST Content-Length 16385 + `Expect: 100-continue`, no body sent | 413, `Connection: close`, no `100` line | PAYLOAD_TOO_LARGE |
| chunked 16385 bytes | 413, `Connection: close` | PAYLOAD_TOO_LARGE |
| chunked 16384 bytes | 200 (`len=16384`) | |
| forced exception, `Accept: text/xml` | 500, `Content-Type: application/json;charset=UTF-8`, body `{"error":"Unexpected error.","code":"INTERNAL_ERROR","timestamp":"2026-11-02T21:05:23.153Z"}` | INTERNAL_ERROR |
| unknown path (resource handler route) | 404 JSON, headers present | NOT_FOUND |

Container page (invalid request line `GET / HTTP/1.1 junk junk` on a raw socket): `HTTP/1.1 400` with a bare page whose title and h1 read "HTTP Status 400 - Bad Request"; no product, version, report or `Server` header.
No body byte read for a declared over-limit body: the unit test counts `getInputStream` calls (0); on the real socket no body was sent and the answer was 413 with no `100`.

## Deviations and risks
- Boot 4.1.2: the encoding key is `spring.servlet.encoding.enabled` (plan says `server.servlet...`); applied in `application.yml`.
- Tomcat's default `continueResponseTiming=immediately` sent `100 Continue` before the gate ran (observed); `TomcatErrorReportCustomizer` also sets `onRead`. Not an SC-06 value change; Liaison to confirm.
- Two integration test classes beyond the three listed; Task 08 may absorb them.
- `RequestGateFilter` is `final` and keeps a lambda over the writer to satisfy SpotBugs without an exclusion.
- The error valve is added from a context customizer; verified by the container page output; no negative control run.
- Not done (out of scope): `Allow` header on 405 (Task 15), error-dispatch bypass test (Task 17), E-22 slow body.

## Resume point
All steps done and committed locally (not pushed). Nothing left except the orchestrator's push and audit.
