# Handoff 164: TASK_DELIVERY, Task 49+50 (raw-socket run, grammar corners, hidden-channel repeat, log hygiene)
- From: java | To: liaison | Phase: 4 | Round: 1 | Type: TASK_DELIVERY
- Branch: `test/task-49-50-raw-socket-release` (the task header names `test/task-49-50-raw-socket-and-grammar`; the brief's branch was used). Not pushed.
- Commits: step 1 (criteria 1 to 4) `9ee1e94`; step 2 (criteria 5 to 7, drip, NFC) is the commit after it on the branch.
- Production: none. No `pom.xml`, `application.yml`, `backend/config/**` or README change.

## Resume point
- Done: both steps committed locally (not pushed); all five new classes green inside the full run; status row IN_REVIEW.
- Next: the Sponsor decides on findings 1 and 2 and on the build time (section 5); then Liaison audit. A full `clean verify` inside 9 minutes was not possible (section 5).

## 1. Files created (tests only, all under 300 lines)
`PreApplicationIntegrationTest`, `GrammarCornersIntegrationTest`, `TextAndParserIntegrationTest`, `LogHygieneIntegrationTest`, `DripAndCostIntegrationTest` (fifth class: drip, NFC; the task names four), support: `FreshApp` (guarded temporary location, start, stop with leftover check), `RawRules` (no-disclosure rule, body class, next-request proof, evidence line), `DripClient` (one byte every 2 s, paced by a blocking read). `RawHttpClient`, `AppProcess`, `ApiCalls`, `UnicodeFixtures`, `CapacityFixtures` reused unchanged. One packaged-jar process per class; stopped in `@AfterAll` with an alive check.

## 2. Criterion and row to test
| Criterion / row | Test |
|---|---|
| 1: E-10 no Host 1.0, two Host; E-37 no Host 1.1, space in target, version 9.9 (505) | `PreApplicationIntegrationTest.criterion1NoHostTwoHostsAndMalformedRequestLinesAreRefusedBeforeTheApplication` |
| 1: E-37 two Content-Length, -1, Content-Length plus Transfer-Encoding | `criterion1ContentLengthFramingConflictsAreRefused` |
| 1: E-20, E-36 line and header over 8 KiB (also 20,000) | `criterion1LinesAndHeadersAboveEightKibAreRefused` |
| 2: E-28 both TRACE 405 | `criterion2BothTraceRequestsAreRefusedWith405BeforeTheApplication` |
| 2: E-28 FOO, get (with JSON content type) | `criterion2UnknownAndLowerCaseMethodsAreRefused` |
| 3: E-15, E-24, E-24b, E-25 rows | `criterion3ContainerRowsGiveTheEnvelopeOrAPreApplicationAnswerWithoutDisclosure` |
| 3: SEC-F-151/128/172 Tomcat-first pages | `criterion3TomcatFirstPagesAnswer400Or404WithoutDisclosure` |
| 4: E-31 websocket, h2c (and h2c with settings) | `criterion4UpgradeRequestsAreAnsweredWith200AndNeverSwitchProtocols` |
| 5: E-34 rows 2 to 7 (and row 1), E-35 all 11 | `GrammarCornersIntegrationTest.e34...`, `e35...` |
| 5: application rows of E-24b, E-25 (strict envelopes) | `e24bAndE25ApplicationRowsGiveTheStatedEnvelopes` |
| 6: T-23, T-24, T-24b, T-25, T-26 | `TextAndParserIntegrationTest.t23...`, `t24...`, `t24b...`, `t25...`, `t26...` |
| 7: T-33, SC-37 | `LogHygieneIntegrationTest.hostileIdsQueryValuesAndTitlesLeaveOneCleanLogLinePerEventAndNoTitleText` |
| SEC-F-147/R-24 drip | `DripAndCostIntegrationTest.dripVariantsAreCutAndManyHeldConnectionsDoNotStopTheService` |
| SEC-F-153 NFC | `sec153NormalisationAtTheBodyCapAnswersWithinAGenerousBound` |
| SEC-F-106 | `GrammarCornersIntegrationTest.sec106HostWithAnOutOfRangeOctetIsRefusedAsInvalidHost`, `sec106BindAddressWithAnOutOfRangeOctetIsRefusedByTheStartPolicy` |

## 3. Raw-socket results (body class: html = container page; headers listed are those present among Content-Type, CSP, X-Content-Type-Options, Cache-Control, Server, Allow; next served = the list on a new connection answered 200)
| Request | Status | Body | Headers | Next served |
|---|---|---|---|---|
| no Host, HTTP/1.1 | 400 | html | Content-Type | yes |
| no Host, HTTP/1.0 | 400 | json envelope (INVALID_HOST, application) | Content-Type, CSP, X-Content-Type-Options, Cache-Control | yes |
| two Host | 400 | html | Content-Type | yes |
| `GET / HTTP/9.9` | 505 | html | Content-Type | yes |
| target with a space | 400 | html | Content-Type | yes |
| two Content-Length; Content-Length -1 | 400 | html | Content-Type | yes |
| Content-Length and Transfer-Encoding | 400 | html | Content-Type, CSP, X-Content-Type-Options, Cache-Control | yes |
| line 9,000 / 20,000; header 9,000 | 400 | html | Content-Type | yes |
| TRACE with and without JSON type | 405 | html | Content-Type, Allow | yes |
| FOO, get (JSON type) | 405 | json envelope (application) | envelope set plus Allow | yes |
| `/%2e%2e/`, `/..;/`, `/%2f`, `/%00` | 400 | html | Content-Type | yes |
| `/WEB-INF/web.xml`, `/META-INF/x` | 404 | html | Content-Type | yes |
| `/%2e%2e/%2e%2e/etc/passwd`, `/..%2f..%2fetc/passwd` | 400 | html | Content-Type | yes |
| id plus `%2f`, `%2F`, `%5c`, literal `\x`; `/books/%zz`, `/books/%`; `/%00` | 400 | html | Content-Type | yes |
| `.%2E`, id plus `%3b` | 404 | json envelope NOT_FOUND | envelope set | yes |
| `?status=%zz`, `?status=%FF` | 400 | json envelope MALFORMED_REQUEST | envelope set | yes |
| Upgrade websocket, h2c, h2c with settings on `/` | 200 | html page, no Upgrade header | envelope set | yes |
No product, version, `Server` header or echo of the planted text appeared in any pre-application answer.

## 4. Drip and cost
| Variant (1 byte per 2 s) | Result in a 60 s window |
|---|---|
| request line (whole request dripped) | not cut in 30 s (15 bytes sent); an earlier 60 s run: not cut (30 bytes) |
| headers | not cut in 30 s; not cut in the 60 s run |
| body of a valid POST | first answer `400` after 10.0 s (12.0 s in the older SlowRequest test); connection still open at 30 s and at 60 s |
| 12 request-line drips held at once, then a normal GET at once and 10 s later | 200 and 200; none of the 12 was cut in 30 s (nor in 60 s) |
Reading: the slow request line and header clients are not cut within 30 to 60 s (SEC-F-073 accepted limit, R-24 accepted); 12 held connections (more than 8 workers) did not stop the service in this run; no connector value changed. NFC at the cap: 8,000 combining marks (descending class order, 16 KiB body) answered `400 VALIDATION_ERROR` in 170 to 205 ms (bound asserted: 5,000 ms).

## 5. Build summary
**Not completed.** `cd backend && ./mvnw clean verify` was run twice in the background and stopped by the 9-minute cap (`timeout` 530 s and 539 s, exit 124) before the end: the surefire phase passed (977 tests, 0 failures, 0 skipped); in the failsafe phase 41 integration classes had finished with 0 failures, among them my five (8, 5, 5, 1 and 2 tests, all passed), when the cap hit during `ColdStartBoundIntegrationTest`. No Maven summary line, no JaCoCo figure and no exit 0 were obtained, so none is claimed. The existing integration suite alone is near the cap (for example Durability 155 s, StoredLibraryRefusal 40 s, SlowRequest 37 s, CapacityMixedLoad 35 s); this task adds about 60 s (drip 35 s). Sponsor/orchestrator: run the full build with a longer cap, or accept the per-class evidence.

## 6. Findings (STOP rows, not weakened, left out of the committed tests)
1. **E-37 row 1, `GET  /  HTTP/1.1` (two spaces): observed `200` (page served), spec says pre-application `400`.** Request: `GET  /  HTTP/1.1`, `Host: localhost:<port>`, `Connection: close`. The container accepts repeated spaces as one separator. Not asserted; no production change made (Security/PM to decide: spec row or container setting, the latter is ASK FIRST).
2. **E-28 row `FOO` without a content type: observed application `415`** (content type judged first), spec says `405` (or a pre-application 400, 405, 501). With `Content-Type: application/json` (the prelude of the TRACE rows) FOO and `get` give `405`; those are asserted. The row text does not say whether the content type is sent; PM to confirm.
3. T-33: the application writes no log line per request for hostile ids, queries or titles (0 lines with a marker, 0 WARN/ERROR lines), so one-line-per-event holds with zero events; the markers were shown in the request first. No raw CR, U+2028, U+2029, U+0085 or ESC in the console, no title text.
4. Raw `[` in a query or id is answered by the container (known, E-26 row 5); the log test uses encoded `%5b` in the query.

## 7. Open items observed (no test required)
- SEC-F-105: `SqliteDataSourceConfig` builds the pool only through `SqliteDataSourceProvider.create(Guarded)` with a fixed `poolConfig`; the only Hikari key it reads is `spring.datasource.hikari.connection-timeout`; `jdbc-url`, `data-source-class-name`, `jndi-name` are not read.
- SEC-F-148: `RequestGateFilter` stage 9 judges id, query and status from the request line before any body byte; a declared length over the 16 KiB cap is refused with `PAYLOAD_TOO_LARGE` without reading the body (`BoundedBodyRequest.LIMIT`).

## 8. Deviations
Branch name differs from the task header (brief). Fifth test class and three support classes added (split by theme, size limit). None else.
