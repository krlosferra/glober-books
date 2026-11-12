# Handoff 165: AUDIT_REPORT, Task 49+50 (raw-socket run, grammar corners, hidden-channel repeat, log hygiene)
- From: liaison | To: orchestrator | Phase: 4 | Round: 1 | Type: AUDIT_REPORT
- Mode: F (with G for the oracles). Reviewed: brief 163, delivery 164, `tasks.md` Task 49+50 (criteria 1 to 7), spec rows E-10, E-15, E-20, E-24, E-24b, E-25, E-28, E-31, E-34 to E-37, T-23 to T-26, T-24b, T-33, DEC-033, and the five new test classes and three support classes on `test/task-49-50-raw-socket-release` (9c99aa7..8be14c3).

## Verdict: ACCEPT WITH FOLLOW-UPS
No blocking defect. The two STOP rows are correctly reported, not weakened. Two spec rows need PM wording and one is a Sponsor decision (below). Security still owes its verdict (Task 49+50 is `Security: per task`, and it carries the 41+42 close-out).

## What I ran, and what I took
- Ran offline, background, 520 s cap: `./mvnw -o verify` limited to the five new integration classes (`-Dit.test=...`, unit phase skipped by a non-matching `-Dtest`). Result: 21 tests (8, 5, 5, 1, 2), 0 failures, 0 errors, 0 skipped, BUILD SUCCESS, exit 0. No leftover java process afterwards.
- Taken as given from the orchestrator: full `./mvnw -o clean verify`, BUILD SUCCESS, exit 0, 9 min 22 s, 1,328 tests, 0 failures, 0 errors, 1 skipped. I did not run it. It now exceeds the 9-minute tool cap: note for the plan (ledger section 6, "the 6-minute full verify time").
- Read from the code: everything else.

## Criteria
| # | Criterion | Result | Note |
|---|---|---|---|
| 1 | No Host 1.0 and 1.1, two Host, request line, 9.9, CL duplicate or -1, CL+TE, over 8 KiB; no disclosure; next served | MET except E-37 row 1 | Expected sets come from the rows (400/505, 400/414/431). `RawRules.assertNoDisclosure` checks product, version regex, Server header, echo; every row proves the next request. The two-space row is absent (finding F-1). No Host on 1.0 got an application `INVALID_HOST` 400: inside the accepted set. |
| 2 | TRACE 405 pre-application (with and without type); FOO and get | MET for TRACE; FOO partly | TRACE: 405 and not an envelope (DEC-033 satisfied). FOO and get asserted only with a JSON content type (finding F-2). No `Allow` assertion (SEC-F-079 respected). |
| 3 | Encoded `/` `\`, `%00`, literal `\`, dot segments: envelope or pre-application answer | MET | 16 rows, each marked "app" (strict envelope) or "pre" (envelope or 400) exactly as the spec text allows it; `/%2e%2e`, `.%2E`, `%3b`, `%33...`, `%62ooks` are strict. Plus the SEC-F-151/128/172 pages (400 or 404, no disclosure). |
| 4 | Upgrade websocket and h2c give 200, never 101 | MET | Status 200 and no `Upgrade` header, three variants. |
| 5 | E-34 rows 2 to 7, all E-35, application rows of E-24b, E-25 | MET | Statuses copied from the rows (201/201/415 x5 ... and the eleven E-35 values). Row 1 is also checked. |
| 6 | T-23 to T-26, T-24b on the real port | MET | Built from numeric code points; expected 201/400 per the rows, and `VALIDATION_ERROR` with field `title`. |
| 7 | Hostile ids, query values, titles: one line per event, no raw separators, no title text | MET, but vacuous for "one line per event" (finding F-3) | |
| | SEC-F-106 | MET | Start policy refuses `127.0.0.256`, `127.0.0.300`, `999.0.0.1`; Host with a bad octet gives `INVALID_HOST`. No defect. |
| | SEC-F-153 | MET | 8,000 marks, 16 KiB body: 400 in 170 to 205 ms, bound 5,000 ms. |
| | SEC-F-147 / R-24 | MET as recording; see F-4 | |

## Brief constraints
- Tests only: `git diff --name-only` outside `src/test` and `docs/sdd` is empty (no `src/main`, `pom.xml`, `application.yml`, `backend/config`, README). Met.
- Files at or under 300 lines (largest 175). Random port through `AppProcess`; one process per class; stop plus alive check in `@AfterAll`. Met.
- Reuse: `RawHttpClient`, `AppProcess`, `ApiCalls`, `UnicodeFixtures` reused unchanged. Met.
- `Thread.sleep`: none. One timed wait, `new CountDownLatch(1).await(10, SECONDS)` in the drip test, is a pause in disguise (the hold period, not synchronisation). Acceptable, but name it as such (F-5).
- Attribution: three commits, author krlosferra, no tool, model or session text. Met.
- Process leftovers: none after my run.

## Findings and the key judgments
**F-1 (E-37 row 1, `GET  /  HTTP/1.1`, two spaces, returns 200): both the row and the behaviour need a decision.** The row lists a request line that is not valid under RFC 9112 (a single SP separates the parts; a recipient MAY parse leniently, and Tomcat does). The spec promises a pre-application 400 for it, the container serves the page. The behaviour carries no disclosure or bypass (the target is normalised to `/`; the same page a clean request gets), so this is not a security defect, but the spec and the behaviour disagree and the row was rightly left out.
- Recommendation (Sponsor decision, ASK FIRST either way): accept the lenient parse, because refusing it needs a connector or filter change that buys nothing. PM wording for E-37: remove `GET  /  HTTP/1.1` (two spaces) from the first cell and add a line: "`GET  /  HTTP/1.1` (repeated spaces): the server reads repeated spaces as one separator and serves the request; no refusal is required (accepted, X-nn)". The alternative (keep the row and make it fail) would be a production change and is not recommended.
- Until decided, this is an open row: list it in the release notes as "not enforced, accepted".

**F-2 (E-28, `FOO` without a content type gets 415).** The row presupposes nothing about the content type for `FOO` and `get`: only the two TRACE rows say "the content type is not evaluated". DEC-033 removed the content-type-first order for TRACE only, so for `FOO` and `get` the application's content-type check (E-34, 415 before the method check) is the spec'd order for a request that reaches the application. Behaviour is consistent with the spec as a whole; the row is ambiguous. The test asserts the rows with a JSON content type, which is a faithful reading but it hides the 415 case.
- PM wording for E-28, appended to the `FOO` clause: "`FOO` and `get` are sent with the JSON content type; without a content type the application answers 415 `UNSUPPORTED_MEDIA_TYPE` first (the content type is judged before the method for any request that reaches the application, E-34)". The developer then adds one assertion for that 415 (small follow-up, same file, after the PM text lands).

**F-3 (T-33 / criterion 7 is satisfied with zero events, and the test cannot show neutralisation).** Checked: the application logs only `LOG.error` for unexpected failures with a class name and top frame, never ids, queries or titles (`RequestGateFilter` lines 60 and 176, `GlobalExceptionHandler` line 201); the packaged run showed 0 lines with a marker and 0 WARN or ERROR. So the spec text "one line per event, no raw separator, no title text" holds, and the test is a correct no-leak proof: the markers and hostile characters did reach the server (the 400 `INVALID_STATUS` and 400 `VALIDATION_ERROR` answers and the 201 prove the requests were processed, and the console shows a startup line, so it was captured). It is vacuous for the neutralisation pattern itself: nothing hostile ever lands in a log line, so a broken `%replace` would not fail it. The `get()` helper's "carries the planted text" assertion is a tautology on the constant, not proof of receipt (the status asserts are the real proof).
- Verdict on the spec: satisfied; on the test: meaningful as a no-leak test, not as a pattern test. Accept; follow-up (LOW): a positive control that drives one real log line with hostile text through the console pattern (the pattern is configured in `application.yml`; earlier tests mention SC-37 in the start refusal and in MandatoryProperties, but none feeds CR, U+2028, U+2029, U+0085, ESC to the pattern). Ledger SEC-F-123 (pattern strips only six characters) stays an optional hardening item. Do not add production logging of ids to make the test non-vacuous.

**F-4 (drip evidence, R-24) is well founded but slightly over-stated in the table.** Facts from the code: the committed test runs a 30 s window; the "60 s" figures come from an earlier uncommitted run, and the delivery's table header says "60 s window". Request line and headers: not cut in 30 s (the client had sent only 15 of about 50 bytes in that time, so the cut at the 5 s timer, SC-06, does not apply to byte-by-byte slow senders: consistent with the accepted R-24 and SEC-F-073). Body: first answer 400 about 10 s, connection open at 30 s; the 60 s remark is from the earlier run. 12 held connections (more than the 8 workers) did not stop the service: a normal GET answered 200 at once and 10 s later. That last point is a good figure but one run; Tomcat's NIO connector keeps idle (not yet parsed) connections off worker threads, so a stopped service would need a drip that has begun a request body being read, a case not tried (R-24 stays accepted; say "one run, 12 request-line drips" in the release note). The test name `dripVariantsAreCut...` is misleading: only the body variant is asserted; line and header are recorded. Rename or reword (LOW).

**F-5 (test hygiene, LOW).** (a) `new CountDownLatch(1).await(10, SECONDS)` is a sleep; use a named constant and a comment. (b) The "next served" proof in `RawRules.record` is called inside `raw()` and asserted; fine. (c) The evidence tables of the delivery are not reproduced by the tests (they print `RAW |` lines), good for repeat runs.

**Fifth class and support classes:** justified (split by theme and the 300-line limit; named and listed). Not a defect. Branch name: record in `tasks.md` (PM debt, add to the existing 41+42 branch-name line). The build time: the full verify is now 9 min 22 s, over the 9-minute tool cap; the next run of the whole suite must be a background run of more than 9 minutes, or a plan note on splitting the integration suite.

**Observations for the ledger:** SEC-F-105 (Hikari keys): the delivery reports only the fixed `poolConfig` and one key read (`connection-timeout`); that is a code reading, not a test, so Security should decide whether it is enough. SEC-F-148 (bounded read): delivery says the gate judges id, query and status from the request line before any body byte and refuses a declared length over the 16 KiB cap without reading the body; as Liaison I confirm this reading matches the intended bounded read for the cap, but "the number of bytes read before an id or route is judged for a body on GET" was not measured; I close SEC-F-148 as intended behaviour, with the measurement left out.

## Follow-ups
1. Sponsor: decide E-37 row 1 (F-1; recommendation: accept the lenient parse and reword the row) and state it in the release notes.
2. PM: reword E-28 (F-2) and E-37 (F-1); list the branch name of 49+50 in `tasks.md`; add the full-verify time (9 min 22 s) to the plan amendments.
3. Java (after the PM text): add the one assertion for `FOO` without a content type (415); rename the drip test; name the 10-second pause; a log-pattern positive control (F-3).
4. Security: verdict on 49+50 and the 41+42 close-out (ledger line "Task 41+42 Security close-out"); SEC-F-105 decision; decide whether the slow-reader S-6 and the drip limit go to the release notes.
5. Ledger: close SEC-F-106 (no defect), SEC-F-153 (figure 170 to 205 ms), SEC-F-148 (Liaison), the Tomcat-first pages part of SEC-F-151/128/172 (pinned); SEC-F-147 stays accepted with the new figures; keep the "slow-reader test" LOW item.
6. Orchestrator: update `progress.md` and commit with the audit; Sponsor gate, then merge only on the Sponsor's order.
