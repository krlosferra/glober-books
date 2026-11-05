# Handoff 105: Task delivery, Task 16 (query, id and empty-body rules; transport limits)
- From: java
- To: liaison
- Type: TASK_DELIVERY
- Branch: feature/task-16-query-id-limits (local commits, not pushed)

## Resume point
Done: all steps (QueryPolicy, id rule, query rule, no-body rule, early rejection, slow-request tests, evidence, delivery, progress.md, status row IN_REVIEW).
Next: Liaison audit; Sponsor decisions on F-1 and F-2 below.

## Findings that need a decision (STOP items)
- F-1 (Tomcat differs from spec): `GET /api/v1/books?status[]=reading` never reaches the application. Tomcat 11.0.27 rejects a raw `[` or `]` in the request target ("Invalid character found in the request target") and answers its own HTML 400 page. Spec E-26 row 5 expects `400 MALFORMED_REQUEST`. The encoded form `?status%5B%5D=reading` gives the envelope. The test asserts status 400 only for the raw row. Fix options: accept as a container-rejected row (Appendix D note), or set `relaxedQueryChars` (SC-06 value, ASK FIRST, not done). The HTML body also bypasses the envelope (Task 17 and 06 territory).
- F-2 (SEC-F-073 drip): one byte every 2 s for 20 s on the request line and on the headers: connection still open (accepted limit; the gate runs after the head is parsed, so `BoundedBodyRequest` cannot cover it; a fix is Tomcat configuration, ASK FIRST). Body of a valid POST: before the change it survived too; I added a total read deadline (10 s, `BoundedBodyRequest.DEADLINE`, an assumption: the spec gives no value) and the client now gets `400 MALFORMED_REQUEST` with `Connection: close` at about 12 s. But Tomcat keeps swallowing the dripped remainder after the response (connection not closed within 20 s more; maxSwallowSize counts bytes, not time), so the worker thread is still held. Recommend the Sponsor records the drip on all three as an accepted limit (8 threads, loopback only) or approves a connector change.

## Files
Created: `config/transport/QueryPolicy.java`; tests `QueryPolicyTest`, `RequestGateRulesTest`, `BoundedBodyRequestTest`, `TransportLimitsIntegrationTest`, `SlowRequestIntegrationTest`.
Modified: `RequestGateFilter` (stage 9), `BoundedBodyRequest` (deadline), `ErrorResponseWriter` (overload with field problems, needed for the `details` entry of `INVALID_STATUS`, spec BR-001; not in the task file list), `docs/sdd/progress.md`, `docs/sdd/status.md` (own row).
Deviations: the id function lives in `QueryPolicy` (only new production file allowed); two extra test classes (`RequestGateRulesTest`, `SlowRequestIntegrationTest`, `BoundedBodyRequestTest`) because `RequestGateFilterTest` is at the 300-line limit and the slow tests take 45 s. Optional `HeaderPolicyTest` carry-over not done. No change under `backend/config/**` or `pom.xml`.

## Criteria
1. Id: `QueryPolicy.idWellFormed` (QueryPolicyTest: upper case, wrong length, `%`, null); gate (`RequestGateRulesTest.badIdGives400Malformed`, `routeAndMethodAreJudgedBeforeTheId`, mocked test-only route table). Real-port rows E-19, E-24 row 1, E-27 rows 1 and 2, T-14 complete in Task 31 (no id route exists, they answer 404 now).
2. Query: `QueryPolicy.check` (QueryPolicyTest, 51 cases), `TransportLimitsIntegrationTest.e26QueryGrammarOnTheList`, `t20StatusValueAndShapeRows`, `malformedEncodingInvalidUtf8AndOtherEndpointsAreMalformed`, `e36TwoThousandQueryPairsOnTheList`, `e38StaticContentTakesNoQueryAndNoBody`. Decoded value goes to attribute `globerbooks.status`; no filtering added.
3. No body: `e18AndT39BodyOnGetIsMalformedAndEmptyBodiesPass`, `e32ChunkedBodyOverTheCapIs413OnAnyRoute`.
4. `e31ExpectContinueWithOversizedLengthGets413AndNoContinue`, `e33EarlyRejectedLargeDeclaredBodyAnswersWithinTwoSecondsWhileOthersAreServed`.
5. `SlowRequestIntegrationTest.e22IdleConnectionIsClosedWithinTheTimeoutWhileOthersAreServed`, `dripVariantsAreRunAndRecorded` (F-2).

## Real-port evidence (status and code as observed)
| Row | Request | Status, code |
|---|---|---|
| E-26 | `?` / `?&` / `?status=reading&` / `?status=reading;x=1` / `?%73tatus=reading` / `?status=%72eading` / `?status=reading+` | 200 / 400 MALFORMED_REQUEST / 400 MALFORMED_REQUEST / 400 INVALID_STATUS / 200 / 200 / 400 INVALID_STATUS |
| E-26 row 5 | `?status[]=reading` | 400 Tomcat HTML page (F-1); `?status%5B%5D=reading` 400 MALFORMED_REQUEST |
| T-20 | `?status=READING`, `?status=`, `?status` | 400 INVALID_STATUS with details field status |
| T-20 | `?status=a&status=b`, `?foo=1`, `?Status=reading` | 400 MALFORMED_REQUEST |
| E-36 row 3 | 2,000 pairs `a=b&` | 400 MALFORMED_REQUEST |
| E-38 | `/?x=1`; `/index.html?`; `/` with 1-byte body; `/css/app.css?x=1` | 400; 200; 400; 400 (MALFORMED_REQUEST) |
| E-18, T-39 | GET list with body " " and with `{}` | 400 MALFORMED_REQUEST; Content-Length 0, none, empty chunked: 200; chunked " ": 400 |
| E-32 | chunked 16,385 to unknown path, GET list, DELETE id path | 413 PAYLOAD_TOO_LARGE (each) |
| E-31 Expect | POST, length 16,385, `Expect: 100-continue` | 413 PAYLOAD_TOO_LARGE, raw answer holds no `100 Continue` |
| E-33 | 415-rejected POST declaring 10,485,760 (1 MiB sent), other GET meanwhile | 415 in 5 ms (limit 2 s); other GET 200 |
| E-22 | idle half request | closed by the server after 4.2 s (timer started after the socket was open); other GET 200 |
| Drip | request line, headers, body (one byte per 2 s, 20 s window) | open at 20 s; open at 20 s; 400 at 12.1 s, then connection still open (F-2) |

## Verify
`cd backend && ./mvnw -o clean verify`: Checkstyle 0 violations; unit 458 tests, 0 failures; integration 144 tests, 0 failures, 1 skipped (pre-existing, `SqliteStoreIntegrationTest`); SpotBugs green after one fix; "All coverage checks have been met"; BUILD SUCCESS. Line coverage of new or edited classes (JaCoCo csv): QueryPolicy 44 of 44, RequestGateFilter 81 of 83, BoundedBodyRequest 21 of 24. Full log kept at /home/user/verify-t16.log.

## git diff --stat main..HEAD (before this final commit)
```
 .../config/transport/BoundedBodyRequest.java       |  56 +++++-
 .../globerbooks/config/transport/QueryPolicy.java  | 136 +++++++++++++++
 .../config/transport/RequestGateFilter.java        |  47 ++++-
 .../globerbooks/error/ErrorResponseWriter.java     |  14 ++
 .../config/transport/BoundedBodyRequestTest.java   |  48 ++++++
 .../config/transport/QueryPolicyTest.java          |  95 ++++++++++
 .../config/transport/RequestGateRulesTest.java     | 183 ++++++++++++++++++++
 .../transport/SlowRequestIntegrationTest.java      | 149 ++++++++++++++++
 .../transport/TransportLimitsIntegrationTest.java  | 192 +++++++++++++++++++++
 104-orchestrator-to-java-task-16-brief.md          |  26 +++
 105-java-to-liaison-task-16-delivery.md            |   9 +
 docs/sdd/status.md                                 |   3 +-
 12 files changed, 951 insertions(+), 7 deletions(-)
```

## git log main..HEAD (author and subject, before this final commit)
All commits authored by `krlosferra <carlos.ferra@globant.com>`, no trailers:
```
fix(transport): keep gate writer lambdas for SpotBugs [Task-16]
feat(transport): slow-request tests and total body read deadline [Task-16]
test(transport): real-port query, no-body and early rejection rows [Task-16]
feat(transport): gate applies id, query and no-body rules [Task-16]
feat(transport): add QueryPolicy query grammar and id rule [Task-16]
docs(sdd): start the Task 16 delivery with a resume point [Task-16]
docs(sdd): add the Task 16 brief and update the status board [Task-16]
```

## Risks and follow-ups
The 10 s deadline value is mine (spec silent). Query decoding treats `+` as a space and decodes names before the unknown and repeat checks (required by E-26 row 6). The body is read up to the cap before the route is decided (existing order), so a body on GET is read, not just one byte.
