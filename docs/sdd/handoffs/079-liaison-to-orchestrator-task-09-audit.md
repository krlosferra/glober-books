# Handoff 079: Liaison audit of Task 09 (static content)
- From: liaison
- To: orchestrator
- Phase: 4   Round: 1
- Type: REVIEW_REPORT
- Brief: 077; delivery: 078

Mode: F. Artifacts reviewed: 077, 078, tasks.md Task 09, spec E-15/E-29/E-30/E-39 and line 511, StaticAssetCatalog, StaticContentController, StaticContentConfig, MalformedRequestException, GlobalExceptionHandler (handler lines), the test list in 078. I did not re-run the build (orchestrator ran `clean verify`: green). I did not read StaticContentIntegrationTest line by line, so the test names in 078 are taken as reported.

## Verdict: ACCEPT WITH FOLLOW-UPS (Liaison verdict only; the Security verdict and Sponsor approval are still needed)

## Criteria
1. ACCEPT. `/` maps to `/index.html`; the three content types are exact; the gate sets the headers; real-port table agrees.
2. ACCEPT. The controller reads no Range or conditional header and sets no ETag, Last-Modified or Accept-Ranges; real-port row shows 200 with the full body.
3. ACCEPT. The catalog matches the exact raw request URI, so directories, trailing slash, double slashes, case and `;jsessionid` forms miss and fall to the 404 envelope. `/error`, `/.git/config` and the db path are not in the catalog. Tomcat's own 400 for `%2e%2e` and `..;` is allowed by E-15 as a pre-application answer.
4. ACCEPT. The catalog fails at construction on any non-`.html/.css/.js` file, `*.test.js` and dotfiles, with a fixed message and no path. The location is a constructor literal in `StaticContentConfig`. The SEC-F-072 test (system property, environment-style and static-locations keys) satisfies brief constraint 1.
Constraints 2 to 5: met. No dependency, `pom.xml` or `backend/config/**` change. Layer 1b: commits are by the human identity with no attribution, in the `type(scope): subject [Task-9]` form.

## Rulings on the reported gaps
1. HEAD and OPTIONS answer 200 on static paths (spec 5.8, line 511 and E-29 say 405 with `Allow: GET`). FOLLOW-UP, belongs to Task 15 (route table, methods). Not a Task 09 criterion; criteria 1 to 4 do not mention it. Task 15 must include HEAD and OPTIONS on `/` and on an asset, and the `Allow` header. Because Spring maps HEAD onto the `@GetMapping("/**")`, Task 15 has to block it there; note this in the Task 15 brief.
2. 415 before 405 for DELETE, PUT and PATCH without Content-Type. This is Task 08 gate behaviour, not Task 09. It stays open under the E-10 amendment; spec line 576 limits 415 to "methods other than GET and HEAD" and does not settle the order against 405. This is a PM question (spec amendment) before Task 15. Not a Task 09 defect.
3. `/WEB-INF/web.xml` answers a Tomcat HTML 404, and the Tomcat 400 pages lack the security headers. FOLLOW-UP, belongs to Task 17 (container error wrapper) and Task 49 (raw-socket run). Excluding it from the Task 09 test is acceptable only because Task 17 records it; add `/WEB-INF/web.xml` and the 400 pages' headers to Task 17's test list.
4. `MalformedRequestException` and its advice handler: ACCEPT. The controller must not build bodies, so a new exception is the minimal route. It adds no new error code (`MALFORMED_REQUEST` exists) and no dependency, and it is not an ASK FIRST item. Note for Task 16: it may reuse the exception for the query grammar; do not add a second mechanism.
5. `static-locations` kept in `application.yml`: ACCEPT. With `add-mappings: false` it is inert, and the SEC-F-072 test proves it does not reach the catalog. Recommended follow-up (MINOR): Task 42 (backend release sweep) should decide whether to remove the key, since a leftover key that looks like control is a trust smell. The `StartupPolicyIntegrationTest` edit (null to `false`) is correct, because it follows the new intent.
6. Carried item F3 from Task 04 (handoff 067: "Task 09 tests cover a JSON string rating"). That was my mis-assignment; a static-content task cannot test it. It belongs to Task 30, criterion 3 (strings give `MALFORMED_REQUEST` per the Task 30 text), with Task 21 (strict body reading) as the transport side. The spec line cited in 067 (INVALID_RATING for a string) conflicts with the Task 30 wording (a string gives MALFORMED_REQUEST). The oracle is the spec: the PM must confirm which is right before Task 30 is briefed. Close F3 against Task 09 and re-register it against Task 30.

## Minor observations (non-blocking, Layer 3)
- `catalog.find` runs before the query and body checks, so a query on an unknown path stays 404 (the test `queryOnAnUnknownPathIsStillNotFound` pins it). AP-2 stage 8 says a non-empty query on an asset or on `/` is 400, so this order is consistent.
- The `NoResourceFoundException` is thrown as a control-flow signal with a fixed reason string; acceptable, and the advice maps it to the envelope.
- StaticAssetCatalog covers 29/32 lines; the missed lines are the IOException branch. Above the 80% gate.

## Reminders
Security review of Task 09 is pending (SC-12, SEC-F-072, header coverage on the Tomcat 400 pages). On acceptance: update `progress.md` and `status.md`, squash-merge via pull request after the Sponsor's approval (`[Task-9] ... - ACCEPTED`).
