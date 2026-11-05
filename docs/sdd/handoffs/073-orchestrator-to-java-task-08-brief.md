# Handoff 073: Task brief, Task 08 (real-port baseline and mandatory-property proofs, closes 0a)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 08 of `docs/sdd/tasks.md` (v0.5.1): read its text, criteria 1 to 6 and traces. Branch `chore/task-08-real-port-baseline` is checked out; Tasks 01 to 07, 10 and 11 are merged. Read also: `spec.md` Appendix D rows E-04, E-07 to E-14, E-21, E-23 and the 405/404/406 rules of 5.0; `plan-appendix.md` AP-2 and AP-9.2; constitution mandatory properties, logging pattern and Security Standards; `security.md` SC-10, SC-28, SC-30, SC-31, SC-37, SEC-F-077, SEC-F-112 (R-23 accepted), SEC-F-121; the existing real-port tests (`RequestGateFilterIntegrationTest`, `TomcatErrorReportIntegrationTest`, `BookApiIntegrationTest`) which you must reuse or extend, not duplicate.

## Scope
Tests `RawHttpClient` (under `support/`), `TransportSecurityIntegrationTest`, `MandatoryPropertiesIntegrationTest`; edit `application.yml` only to correct a wrong key (report it); `docs/sdd/progress.md` (the 0a note). No production code otherwise.

## Carried conditions
1. SEC-F-121: include real-port rows that hit `/api/v1/books` itself (POST and GET) with a forged Host, a bad Origin, `Sec-Fetch-Site: cross-site` and a wrong or missing `Content-Type`.
2. R-23 is accepted: container-generated pages (duplicate or malformed Host, invalid request line, `FOO`, header over 8 KiB) may be a bare HTML 400 without the 8 headers. Criterion 3 says "no HTML": assert no product, no version, no report text and no `Server` header; if the container's bare body does contain HTML tags, report that as a deviation to the Liaison (do not weaken the other assertions).
3. The 500 is forced by a trigger created in the test database (no seam in production code).
4. Mandatory-property proofs: one test per mandatory property of the constitution, showing it effective at runtime (read back from the live context or observed in behaviour), including Jackson 3 behaviour for naming (snake_case) and unknown fields; the log pattern must strip CR, LF, U+2028, U+2029, U+0085 and ESC (assert on real log output of a request carrying such characters); checkpoint logs contain no book data.
5. The 0a note in `progress.md`: the exact commands to try 0a (build the jar or `./mvnw spring-boot:run`, `curl` add and list on `127.0.0.1:8080`, stop), a statement that `v0.1` is a checkpoint, not for real data, and that its database is deleted before Increment 1 (SEC-F-077). Say that the database file is exclusively locked while the app runs (no other tool can open it). Use only loopback; never port 18080 in the tests (random port) and never 8080 in tests.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; a table of every request with real status, relevant headers and body for criteria 1 to 4 (including the raw-socket cases); the property-by-property proof table; the log-sanitisation output; `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'` (paste both).

## Rules
STOP and report on any wrong key or behaviour differing from the plan, and on any spec gap. Commit after each finished step (`type(scope): subject [Task-8]`), local commits only, explicit paths, no AI attribution, never `--no-verify`. If the task will not fit, split at a clean boundary (security class first, properties class second) and report. Write the delivery as `docs/sdd/handoffs/074-java-to-liaison-task-08-delivery.md` early with a "Resume point" section (done / next) kept updated so nothing is lost if you are cut off.
