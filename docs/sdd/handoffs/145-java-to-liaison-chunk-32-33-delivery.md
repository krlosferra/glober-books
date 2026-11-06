# Handoff 145: Delivery, review chunk 32+33 (Task 32 and Task 33 done; Task 33 criterion 3 per coordinator decision (a))
- From: java
- To: liaison (via orchestrator)
- Phase: 4   Round: 1
- Type: TASK_DELIVERY (partial) and STOP report

## Resume point
- DONE: Task 32 (commit 0fa7428, `[Task-32]`). Task 33 committed `test(backend): Task 33 durability across restart and kill [Task-33]` (files `support/AppProcess.java`, `DurabilityIntegrationTest.java`) after the coordinator decision (a): the test pre-creates the directory 0700 and the empty database file 0600, then asserts after every phase (start, operation, normal stop, kill, restart) that the directory is not wider than 0700, the database and companion files not wider than 0600, and owned by the test user. A marked comment says creation modes are proved in Task 34a (SC-13).
- Green run of the strict version: `cd backend && ./mvnw clean verify`: 900 unit, 269 integration (0 failures, 1 existing skip), `All coverage checks have been met`, `BUILD SUCCESS`, 4 m 49 s; `DurabilityIntegrationTest` 5 tests in 161.9 s (about 32 s each, none over 60 s). 20 kill rounds ran. Leftover processes after the run: 0.
- NEXT: Liaison audit and Security review of the chunk; the orchestrator pushes. Nothing pushed. No trailer or session link.

## Production gap found by Task 33 (resolved by decision (a); not fixed in this chunk)
`SqliteDataSourceProvider` (claim, 0700/0600 creation, `tmp` directory, locking) is used only by tests (`grep -rl SqliteDataSourceProvider backend/src` lists the provider, `SqliteStoreIntegrationTest` and `ClaimChildMain`). The packaged application builds its data source from Spring Boot auto-configuration (`spring.datasource.url`, Hikari pool of 1), so in the jar:
- a missing database file is created by SQLite with the process umask: `library.db` is `rw-r--r--` (0644), the data directory must already exist (the app does not create it: `SQLITE_CANTOPEN`), no `tmp` directory exists;
- SC-13 criterion 3 of Task 33 (file 0600) therefore fails: `library.db has mode rw-r--r--` in all 5 tests.
This matches the plan: the guard and the wiring are Task 34a/34b ("DataDirectoryGuard ... create directory 0700 and file 0600"; "SqliteDataSourceProvider (edit)"). It is a spec/plan ordering gap (Task 33 depends on 31a/31b, not on 34a), not a bug in the tests. A production change is outside this chunk, so I stopped.
Decision needed: (a) move criterion 3 (or the file-mode part of it) to Task 34a/34b and have Task 33 pre-create the directory (0700) and file (0600) and assert the modes stay narrower after each phase; (b) make Task 33 depend on 34a; (c) keep the strict test red. The test currently pre-creates only the directory (0700, as the E2E launcher does with `mkdtemp`).

## Experiment (not committed): everything else passes
With the mode check replaced by a print (temporary copy, then reverted to the strict version), `DurabilityIntegrationTest` ran 5 tests, 0 failures, 2 m 59 s in total (about 26 s to 36 s per test: each restart is a JVM start of about 5.3 s to 5.7 s). Observed modes at every phase (49 checks): `data: rwx------`, `library.db: rw-r--r--`; no `-journal` file was present at any kill point.
Kill rounds (operation, round, result, restart time until first answer): add 1 to 5 present after restart, 5735/5607/5390/5373/5593 ms; status 1 to 5 present, 5519/5313/5607/5421/5625 ms; rating 1 to 5 present, 5569/5377/5336/5283/5317 ms; delete 1 to 5 still deleted (404 `BOOK_NOT_FOUND`), 5409/5294/5188/5479/5468 ms. Equality is full-body: the `GET` after the restart equals the body of the acknowledged answer. Normal stop (SIGTERM, exit 0 or 143) and start: list identical after add, status change, rating change and delete (US-009.1). The console (captured to a file) contains `Started GloberBooksApplication` and never the marker `ZQXDATA` that is inside every title and author. Elapsed per test is under 60 s.

## Task 32 (committed 0fa7428)
Branch `test/task-32-33-concurrency-durability` (headers name `test/task-32-concurrency-faults` and `test/task-33-durability`; chunk branch kept).
Files: `ConcurrencyIntegrationTest` (120 lines), `ConcurrencyRaceIntegrationTest` (169), `FaultInjectionIntegrationTest` (167), `support/FaultInjection` (53), `support/ApiCalls` (101), `docs/sdd/progress.md` (entry). Deviation: the tasks list `ConcurrencyIntegrationTest` and `FaultInjection`; the split by theme (allowed by the brief) and the shared `ApiCalls` helper (HTTP client with barrier runner) are new. No production file, `pom.xml` or `backend/config/**` changed. Spec note: S-08 in `spec.md` says 200 iterations; the task and brief say 20; 20 were run.

Build: `cd backend && ./mvnw clean verify`: unit 900 tests, 0 failures; integration 264 tests, 0 failures, 1 skipped (existing assumption in `SqliteStoreIntegrationTest`), `All coverage checks have been met`, `BUILD SUCCESS`, 2 m 07 s.

| Criterion / spec row | Test | Result |
|---|---|---|
| C-06 | `ConcurrencyIntegrationTest.twoSimultaneousAddsAt4999BooksGiveOneCreatedAndOneLibraryFull` | `{201=1, 409 LIBRARY_FULL=1}`, 5000 books |
| C-07 | `twoSimultaneousIdenticalAddsGiveOneCreatedAndOneDuplicateAndOneBookStored` | `{201=1, 409 DUPLICATE_BOOK=1}`, 1 book |
| C-09 | `twentyAddsAt4990BooksGiveExactlyTenCreatedAndTenLibraryFull` | `{201=10, 409 LIBRARY_FULL=10}`, 5000 books |
| C-10 | `twentyIdenticalAddsGiveOneCreatedAndNineteenDuplicatesThenTwentyCreatedWithTheOverride` | `{201=1, 409 DUPLICATE_BOOK=19}`, then `{201=20}`, 21 books |
| S-08 (20 iterations) | `ConcurrencyRaceIntegrationTest.statusReadingAndRatingFourTogether...S08` | allowed: status call 200; rating call 200 or 400 `RATING_NOT_ALLOWED`; final always `reading`, rating null, date null. Observed: status 200 x20; rating 200 x11, 400 `RATING_NOT_ALLOWED` x9 |
| S-09 (20 iterations) | `statusCompletedRatingClearAndDeleteTogether...S09` | allowed: every answer 200 or 404 `BOOK_NOT_FOUND`; if the book exists INV-1 to INV-4 hold. Observed: clear 200 x7 / 404 x13; delete 200 x20; status 200 x6 / 404 x14; final ABSENT x20 |
| SEC-F-177 (60 iterations on one completed book, targets reading, unread, completed in turn) | `ratingAgainstEveryStatusChangeOnOneCompletedBookKeepsInvariantsSecF177` | allowed: status 200; rating 200 or 400 `RATING_NOT_ALLOWED`; INV-1 to INV-4; final status equals the target. Observed: to reading rating 200 x15 / 400 x5; to unread 200 x16 / 400 x4; to completed rating 200 x20; finals: completed rating=4 x20, reading null x20, unread null x20 |
| S-10, E-21 | `FaultInjectionIntegrationTest.failureAfterTheFirstChangeOfLeavingCompleted...S10E21`, `failureWhileEnteringCompleted...`, `repeatedForcedFailures...` | see below |
| Pool not wedged (SEC-F-167/168) | same class, 5 forced failures each followed by a timed `GET` under the 5 s pool timeout | pass |
| A-09 | `noProductionFileHoldsATestOnlyRouteSwitchOrTriggerA09` (scans `src/main`) and grep below | pass |

Forced failure (S-10, E-21): trigger `AFTER UPDATE OF status ON books ... RAISE(FAIL, 'forced-fault-secret-marker')`, created through the application's own pool (`support/FaultInjection`), so the row change has happened inside the open transaction when the statement fails; only the rollback leaves the book as before. Request `PUT /api/v1/books/3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b/status {"status":"reading"}` answered `500 {"error":"Unexpected error.","code":"INTERNAL_ERROR","timestamp":"2026-11-06T18:26:39.010Z"}` (no `details`; the test asserts no marker, trigger, SQL, class or table word). Before and after `GET` identical: `{"id":"3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b","title":"Done","author":"Author","status":"completed","rating":3,"date_added":"2026-02-01T08:00:00.000Z","date_completed":"2026-02-10T09:00:00.000Z"}`. After the trigger was dropped the same request gave `200` with status `unread`, rating null, date_completed null. Note for Security: the server log (not the client) prints the SQLite message with the trigger text on `WARN` (Hibernate `HHH000247`) and the exception class on `ERROR`; no book data in them.
A-09 grep: `grep -rniE "create trigger|raise\(|faultinjection|appprocess|x-probe|test-only|fault\." backend/src/main` returns nothing.

## Evidence for the chunk
- Leftover processes: `ps -eo pid,args | grep -E "globerbooks.jar|ClaimChildMain"` shows 0 after all runs (every process is killed in `@AfterEach`; a shutdown hook in `AppProcess` kills any left).
- Port: free loopback port chosen by the operating system for each start (never 8080 or 18080). Command: `java -Xmx512m -jar backend/target/globerbooks.jar --server.address=127.0.0.1 --server.port=<free> --spring.datasource.url=jdbc:sqlite:<temp>/data/library.db`, environment cleared except `PATH HOME JAVA_HOME LANG TZ`. Separate processes work in this sandbox.
- Elapsed of the new tests: Task 32 classes 1.6 s, 2.2 s, 0.5 s (the runs are dominated by the Spring context start, 20 to 25 s per class); Task 33 about 3 min in all for 5 tests, none over 60 s.

### git diff --stat origin/main..HEAD (at commit 0fa7428)
```
 .../globerbooks/ConcurrencyIntegrationTest.java    | 120 +++++++++++++++
 .../ConcurrencyRaceIntegrationTest.java            | 169 +++++++++++++++++++++
 .../globerbooks/FaultInjectionIntegrationTest.java | 167 ++++++++++++++++++++
 .../com/globant/globerbooks/support/ApiCalls.java  | 101 ++++++++++++
 .../globerbooks/support/FaultInjection.java        |  53 +++++++
 .../144-orchestrator-to-java-chunk-32-33-brief.md  |  25 +++
 docs/sdd/progress.md                               |   4 +
 docs/sdd/status.md                                 |   2 +-
 8 files changed, 640 insertions(+), 1 deletion(-)
```
### git log origin/main..HEAD --format='%an <%ae>%n%B---'
```
krlosferra <carlos.ferra@globant.com>
test(backend): Task 32 concurrency races and forced-failure rollback [Task-32]
---
krlosferra <carlos.ferra@globant.com>
docs(handoff): chunk 32+33 brief, Task 31b done [Task-32]
---
```

## Final state after decision (a)
Observed modes with the strict test: directory `rwx------`, `library.db` `rw-------` (pre-created), never widened by the application at any phase; no `-journal` file at the kill points. Final `git diff --stat origin/main..HEAD` and author log are below (replace the earlier snapshot).
```
 .../globerbooks/ConcurrencyIntegrationTest.java    | 120 ++++++++++++
 .../ConcurrencyRaceIntegrationTest.java            | 169 ++++++++++++++++
 .../globerbooks/DurabilityIntegrationTest.java     | 218 +++++++++++++++++++++
 .../globerbooks/FaultInjectionIntegrationTest.java | 167 ++++++++++++++++
 .../com/globant/globerbooks/support/ApiCalls.java  | 101 ++++++++++
 .../globant/globerbooks/support/AppProcess.java    | 178 +++++++++++++++++
 .../globerbooks/support/FaultInjection.java        |  53 +++++
 .../144-orchestrator-to-java-chunk-32-33-brief.md  |  25 +++
 .../145-java-to-liaison-chunk-32-33-delivery.md    |  73 +++++++
 docs/sdd/progress.md                               |   8 +
 docs/sdd/status.md                                 |   2 +-
 11 files changed, 1113 insertions(+), 1 deletion(-)
```
```
krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 33 delivery evidence and progress [Task-33]
---
krlosferra <carlos.ferra@globant.com>
test(backend): Task 33 durability across restart and kill [Task-33]
---
krlosferra <carlos.ferra@globant.com>
docs(handoff): chunk 32+33 partial delivery and stop report [Task-33]
---
krlosferra <carlos.ferra@globant.com>
test(backend): Task 32 concurrency races and forced-failure rollback [Task-32]
---
krlosferra <carlos.ferra@globant.com>
docs(handoff): chunk 32+33 brief, Task 31b done [Task-32]
---
```
