# Handoff 139: Security review, Task 31a (get, delete, status change)
- From: security
- To: orchestrator
- Phase: 4   Round: 1
- Type: AUDIT_REPORT
- Task: 31a, branch `feature/task-31a-get-delete-status` (HEAD 88aba37), pull request 37

## 1. Scope and method
Scope: `git diff origin/main..HEAD` for `RouteTable`, `BookController`, `BookService`, their tests, briefs 136 and delivery 137; the existing gate (`RequestGateFilter`, `RequestTarget`, `QueryPolicy`, `HeaderPolicy`).
Method: code read; grep for `@Query`, `nativeQuery`, `JdbcTemplate` in `backend/src/main` (none); a scratch copy of HEAD (`git archive`) built with `./mvnw -o package` and run on port 18131 with a temporary database, about 120 curl probes (`--path-as-is`); the delivery's `BookStatusServiceTest` (18), `RouteTableTest` (56) and `BookStoryIntegrationTest` (15, includes the 5,000-book run) re-run green in the scratch copy.

## 2. Findings
No CRITICAL, HIGH or MEDIUM finding.

| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-172 | INFO | Protocol surface | container | New routes inherit container-first pages: `TRACE` (HTML 405, `Allow: HEAD, DELETE, POST, GET, OPTIONS, PUT`, no security headers), request line over about 8 KB (HTML 400), `%00` and `%2f` in the id (HTML 400) | probes `trace-ct`, `long9000`, `ID%00`, `ID%2f` | None: fixed pages, no echo, no state change | None now; Task 49 raw-socket pin (SEC-F-151) | SC-18, SC-29, R-13 |
| SEC-F-173 | INFO | Info disclosure | `BookService.get/changeStatus/discard` | Existence is observable (404 vs 200), as the spec requires; field values are judged before existence so the ordering adds no oracle | probes: unknown id + `Done` gives 400 INVALID_STATUS, + `reading` gives 404 | Single user, loopback, random v4 UUIDs: none | None | SC-11 |

## 3. Controls checked
- SC-16 (id rule) VERIFIED. Gate checks `[0-9a-f]{8}-...` on segment 3 before the chain. Probes: `NOPE` 400, upper-case 400, `%33...` 400, `ID;x=1` 404, `books;a/ID` 404, `books/../books/ID` 404, `%2e%2e` 404, `ID%00` and `ID%2f` Tomcat 400, 5,000-character id 400 envelope, unicode `%EF%BC%BA` 400, CRLF `%0d%0a` 400. Never a 500. Trailing slash, `//ID`, `/status/` give 404. No SQL is built from the id: `findById` only, no `@Query`.
- SC-18 VERIFIED for the new routes. `RouteTable.keyFor`: exact path, or `/api/v1/books/{non-empty}` (4 segments), or `.../{id}/status` (5 segments, literal `status`). `/Status`, `/%73tatus`, `/status/x`, `/other` are 404 before any handler or static file (the static catalog still needs an exact path). Method decided before id format: `POST /not-a-uuid` is 405. `Allow` is correct (`DELETE, GET` on `/{id}`; `PUT` on `/status`); HEAD 405, OPTIONS 405 envelope (with Content-Type, as for every non-GET method), PUT/PATCH/POST on `/{id}` 405, DELETE/GET on `/status` 405. TRACE: see SEC-F-172. DELETE without `Content-Type` gives 415 as E-07 requires.
- SC-31 / SEC-F-159 (status body) VERIFIED through the real resolver on a real port: `5`, `true`, `["reading"]`, `{"a":1}`, duplicate `status`, unknown `x`, `rating`, `[]`, `null`, `"reading"`, ` `, trailing `x`, truncated JSON all 400 MALFORMED_REQUEST; `null`, `{}`, `Reading`, `reading ` and `Done` 400 INVALID_STATUS; escaped value `reading` and escaped member `status` read as the plain text (valid, 200, matches JSON semantics); body of 17,000 and 20,000 characters 413. The delivery's slice tests are not relied on for strictness.
- Ordering (BR-012, T-38) VERIFIED: unknown id + invalid body gives INVALID_STATUS or MALFORMED_REQUEST, + valid body 404 BOOK_NOT_FOUND.
- Atomicity VERIFIED: `changeStatus` is one `@Transactional`, `Book.changeStatus` sets status, clears rating and `date_completed` together, same-state is a no-op (probe: second `completed` keeps the original `date_completed`; `reading` clears it to null; re-completing sets a new one). `discard` is find, delete, flush, returns the snapshot taken before the delete; second delete 404. Rating-clearing cannot be probed on a real port until 31b; covered by `BookStatusServiceTest` (re-run green) and the code read.
- S-07 VERIFIED: full `GET /api/v1/books` body byte-identical before and after failing requests (bad status, 5, `{}`, delete of an unknown id, malformed id) in my probe, and in the delivery test.
- SC-02, SC-11 VERIFIED on the new error paths: 13 security headers on 200/400/404/405/413/415 envelopes, no `Server`, no echo (bad Host gives INVALID_HOST, foreign Origin FORBIDDEN).
- SC-14 VERIFIED: no logger in the new code; scratch app log (36 lines) has no WARN, ERROR, id or status text.
- C-04 (capacity at 5,000): `BookStoryIntegrationTest.atCapacity...` passes in my re-run (list, status change, delete, one add 201, next add 409 LIBRARY_FULL); single-connection assumption of SEC-F-168 unchanged (no config or pool diff).
- Supply chain: no `pom.xml`, `backend/config` or dependency change (diff stat).

## 4. Not verified
- Full `./mvnw clean verify` (874 + 201 tests) was not re-run; I re-ran the three new test classes only. Coverage figures are the delivery's.
- Rating clearing through the API (needs 31b); forced-failure rollback (Task 32).
- Concurrency of parallel PUT/DELETE (Task 32).
- Public advisory databases were not consulted (no dependency change).
- Container-first pages were probed only for the new routes, not raw-socket.

## 5. Residual risks
None new. SEC-F-172 is covered by R-13 already accepted; recommend no new Sponsor decision.

## 6. Verdict
**PASS.** Carried to Task 31b: SEC-F-170 (rating on a non-completed book must give RATING_NOT_ALLOWED, `RatingValue` marker to INVALID_RATING, rating and status in one transaction) and SEC-F-159 (strictness proved in full context; this task honoured it). Next step: Sponsor gate for Task 31a together with the Liaison audit (handoff 138).

I edited `security.md` (v0.48) and the Security cell of Task 31a in `status.md`; nothing committed. Scratch files are under the session scratchpad.
