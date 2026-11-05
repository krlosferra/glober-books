# Handoff 111: Security review, Task 17 (container error wrapper)
- From: security
- To: orchestrator
- Type: AUDIT_REPORT
- Branch: feature/task-17-container-errors (9999f2a, e1addcf, afa1f60)

## 1. Scope and method
Read: briefs/delivery 108 and 109, `EnvelopeResponseWrapper`, `RequestGateFilter`, `ErrorResponseWriter`, test `ErrorProbeConfig`, constitution 1.5 (Security and Error Handling Standards), security.md SC-02, SC-11, SC-29, R-13, R-23, SEC-F-113, 114, 128. Ran (read-only, offline): grep for the probe in `src/main`, `pom.xml` and build output; `EnvelopeResponseWrapperTest` and `RequestGateFailureTest` (pass). I did not start the application or re-probe the real port; the table of delivery 109 is taken as reported.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-151 | LOW | Container-first pages | Tomcat, 5 paths + `/META-INF/x` | HTML 400/404 without the 8 headers, `text/html`, no version, no path, no state change | Delivery 109 table (not re-probed) | None practical: static page, no reflected data, rejected before routing | Accept under 1.5 once Task 49 pins status (400/404), no product/version/echo, next request served. No connector change (ASK FIRST) | SC-29, R-13 |
| SEC-F-152 | INFO | Error handling | `RequestGateFilter.doFilterInternal` catch | `reject` inside the catch may throw `IOException` (client gone) to the container; client aborts logged at ERROR (class and top frame only) | Code read | None | Optional tidy-up later | SC-11 |
Closed: SEC-F-113 (whole gate in the catch; test makes `RouteTable` throw), SEC-F-114 (headers set on construction, `reset`, `sendError`; real-port tests). SEC-F-128 partly closed (application side done; container-first rows go to SEC-F-151).

## 3. Controls checked
- Headers re-applied after reset/sendError: VERIFIED (code: `reset()` and `envelope()` call `applyHeaders()`; writer uses `resetBuffer`, not `reset`, so headers stay; tests `securityHeadersAreSetAtCreationAndSurviveReset`, `sendErrorAfterResetKeepsTheHeadersAndWritesTheEnvelope`).
- No echo of the sendError message: VERIFIED (both overloads ignore it; probe text "secret probe text" asserted absent).
- Whole gate in the 500 catch: VERIFIED (`gate()` covers checks, buffering, route, rules, chain; catches IOException, ServletException, RuntimeException, Error; fixed INTERNAL_ERROR; committed response is left alone). Wrapper construction sits outside the try but only sets static headers.
- ErrorProbeConfig cannot reach production: VERIFIED. It lives in `src/test`, is `@TestConfiguration` (not component-scanned, only via explicit `@Import` in `TransportSecurityIntegrationTest`), no reference in `src/main`, `pom.xml` or `src/main/resources`, no `spring.factories` or imports file. It sets no profile or property.
- SC-02, SC-11, SC-29: VERIFIED for application paths (statuses mapped to existing codes; unknown to INTERNAL_ERROR; `/error` is 404 envelope).
- Tomcat-answered pages vs constitution 1.5: they fit the container-first rule (answered before any application code, generic body, no version/path/user data, no state change). The one missing element is the test pinning the status; the delivery only probed manually, so I do not yet record the acceptance. Not covered: nothing leaks data or changes state.

## 4. Not verified
Real-port probes (I did not run the app; relied on delivery 109); full `./mvnw clean verify`, coverage numbers and commit identity (Liaison scope); public advisory databases not queried (no dependency change in this task).

## 5. Residual risks
SEC-F-151: recommend Security acceptance under 1.5 after the Task 49 pin (would be 1 of 5 for this phase). No Sponsor decision needed now; answer to the open question in 109: the pages carry no active content, so no decision beyond 1.5.

## 6. Verdict
PASS WITH CONDITIONS. Condition (not blocking merge): Task 49 must include the raw-socket rows for `/%2e%2e/`, `/..;/`, `%2f`, `%00`, `/WEB-INF/web.xml` (status in 400/404, no product/version/echo, next request served). No CRITICAL or HIGH open. Next step: Liaison audit, then Sponsor gate. security.md bumped to 0.40; status.md Security cell set. Nothing committed.
