# Handoff 163: Task brief, Task 49+50 (raw-socket run, grammar corners, hidden-channel repeat and log hygiene)
- From: orchestrator (direct dispatch, constitution 1.7)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 49+50 of `docs/sdd/tasks.md` (v0.6.6): the task block is the specification; read its description, criteria 1 to 7 and traces there. Branch `test/task-49-50-raw-socket-release` is checked out (the header names `test/task-49-50-raw-socket-and-grammar`; record the difference). Tasks 01 to 41+42 are merged. Tests only: **Production: none**. Build order: criteria 1 to 4 (Task 49) first, commit; then 5 to 7 (Task 50), commit. If it overruns, split back into 49 and 50 at that boundary and report. This is the last backend task before the release tag, so a failing row is a finding to report, not something to weaken.

## Read (by ID, not whole files)
`spec.md` Appendix D rows E-10, E-15, E-20, E-24, E-24b, E-25, E-28, E-31, E-34, E-35, E-36, E-37, tests T-23 to T-26, T-24b, T-33, DEC-033; `security.md` SC-18, SC-29, SC-37, R-13, R-23, R-24; `constitution.md` Testing Requirements; the existing tests `RequestGateFilterIntegrationTest`, `TransportSecurityIntegrationTest`, `TransportLimitsIntegrationTest`, `RoutingIntegrationTest`, `StrictBodyIntegrationTest`, `support/AppProcess`, `support/ApiCalls` and the raw-socket helpers already used there (reuse them; do not duplicate).

## Open items assigned to this task (from `docs/sdd/active-conditions.md`, section 2)
1. **SEC-F-151, SEC-F-128, SEC-F-172 (LOW, INFO):** the Tomcat-first pages (`/%2e%2e/`, `/..;/`, `%2f`, `%00`, `/WEB-INF/web.xml`, `/META-INF/x`, `TRACE`, an oversized request line) are answered by the container before the application, as HTML and without the security headers. Pin them on raw sockets: status 400 or 404 (405 for TRACE), no product, version or echo of the path, and the next request on a new connection is served. This is criteria 1 to 3; do not assert headers the container does not send, and do not add a connector setting (ASK FIRST).
2. **SEC-F-147 / R-24 (MEDIUM, accepted):** re-check the drip clients (one byte every 2 s on the request line, the headers and the body of a valid POST). Record the real cut time of each variant and whether the 8 worker threads can be held; report, do not change any connector value (ASK FIRST). If 8 drip connections stop the service, say so plainly in the delivery for the Sponsor.
3. **SEC-F-153 (INFO):** add one timing row for the NFC cost at the cap (8,000 combining marks in descending class order took 166 ms): the request must answer within a generous bound; record the figure.
4. **SEC-F-106 (LOW):** test whether the start policy accepts an IPv4 literal with an out-of-range octet (for example `127.0.0.256`) in the bind address or Host handling; if the test shows a defect, STOP that row and report it (a production change is outside this task); do not weaken the test.
5. **SEC-F-105 (LOW, Security) and SEC-F-148 (LOW, Liaison):** no test required from you; list in the delivery, in one line each, what you observed that bears on them (for example the Hikari `jdbc-url` keys reaching the application, and the number of bytes read before an id, query or route is judged for a body on GET).

## Binding constraints
- Raw sockets on a random free port (never 8080 or 18080), with the packaged jar through `support/AppProcess` or the full-context test pattern already used; one process per test class at most; every process stopped and a leftover check at the end; temporary data location created through the guard; no `Thread.sleep` for synchronisation (blocking reads with a deadline).
- Expected values come from the spec rows (E-xx, T-xx), not from what the application answers. If a spec row and the observed behaviour differ, STOP that row and report the exact request and response.
- Hygiene log tests (criterion 7): capture the real log output of the application for hostile ids, query values and titles; assert one line per event, no raw CR, LF, U+2028, U+2029, U+0085 or ESC, and no title text (T-33, SC-37). The marker you plant must be shown to be in the request first, so the absence is not vacuous.
- No production, `pom.xml`, `application.yml`, `backend/config/**` or README change. Every file at or under 300 lines (split the four test classes by theme if needed). No logging of titles or authors in the tests' own output.

## Delivery (compact, constitution 1.7)
Write `docs/sdd/handoffs/164-java-to-liaison-task-49-50-delivery.md` early with a "Resume point" (done / next) and keep it current. It holds: task, branch, commit hashes; the files created; one table mapping every criterion (1 to 7) and every E/T row to its test; one table for the raw-socket results (request, status, body class, headers present, next request served); the drip results; the NFC timing; the build summary line (tests, failures, skipped, coverage, exit code; run `cd backend && ./mvnw clean verify` in the background with a timeout under 9 minutes and read the reports); deviations; findings; no raw terminal output, no `git diff --stat`, no author log. Commit after each step (`type(scope): subject [Task-49-50]`), local only, explicit paths, never `--no-verify`, nothing naming a tool, model or vendor as author or co-author, never session links or trailers. Update `docs/sdd/progress.md` in the same change.

## Rules
STOP and report on any spec gap, any row whose observed behaviour differs from the spec, any need for a production change, or any ASK FIRST item (connector settings, SC-06 values). Do not push.
