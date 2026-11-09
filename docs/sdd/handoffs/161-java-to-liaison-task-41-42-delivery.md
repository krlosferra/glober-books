# Handoff 161: Delivery, Task 41+42 (capacity and performance at maximum size, and backend release sweep)
- From: java
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY
- Branch: `test/task-41-42-capacity-performance` (checked out by the orchestrator; the tasks.md header names `chore/task-41-42-capacity-and-release-sweep`; no README, configuration or production file changed, so a `test/` branch holds). Not pushed.
- Commits: b387316 (criteria 1 to 4 and SEC-F-166), 57e8999 (sweep results in progress.md), plus the commit of this file.

## Resume point
- Done: everything. Capacity tests, sweep, full `./mvnw clean verify` green, progress.md updated.
- Next: Liaison audit, Security review, orchestrator push. Nothing is left on my side.
- Not done on purpose: no `status.md` row exists for 41+42 on the board (orchestrator owns it), no README edit (see criterion 4), SEC-F-126 untouched.

## No stop condition was hit
No bound failed, the dependency tree is identical, no ASK FIRST change and no production change was needed. Three observations are listed under "Observations" for the Liaison and Security.

## Files
Created: `CapacityPerformanceIntegrationTest`, `CapacityMixedLoadIntegrationTest`, `ColdStartBoundIntegrationTest` (all under `backend/src/test/java/com/globant/globerbooks/`), `support/CapacityFixtures`, `support/SlowReader`. Modified (test support only): `support/AppProcess` (new `start(..., jvmOptions)` overload and `pid()`; the fixed `-Xmx512m` stays), `support/ApiCalls` (`port()`). Also `docs/sdd/progress.md` and this file. Largest new file 258 lines (AppProcess, existing); every file at or under 300. Nothing else changed: no `src/main`, `pom.xml`, `application.yml`, README, `config/**`, `e2e/`.

## Test specification (from the spec, C-05, C-08, C-11, M-2, SEC-F-166)
| Case | Expected (spec) | Test |
|---|---|---|
| C-05: 5,000 books, 500-code-point titles and 200-code-point authors outside the basic plane (4-byte UTF-8, the largest body) | 200, `total` 5000, 5000 entries, body < 16,000,000 bytes, time recorded not gated | `CapacityPerformanceIntegrationTest.listOfFiveThousandMaximumSizeBooksIsOkBelowTheBodyBoundAndTheTimeIsRecorded` |
| C-08: 20 add and delete pairs at 4,999 | only 201 and 200; count 5,000 after each add, 4,999 after each delete | `...twentyAddAndDeletePairsAt4999BooksGiveOnlyCreatedAndOkAndNeverMoreThan5000Books` |
| C-11 (c): 4,949 books, 50 adds with 50 lists together (barrier start), jar in a separate JVM, `-Xmx512m`, port from the OS, guarded temp data location | adds all 201, lists all 200 with body < 16,000,000, 4,999 after, no 500, no refused connection; duplicate add 409 DUPLICATE_BOOK and invalid add 400 change nothing (still 4,999); process alive, no OutOfMemoryError in the console | `CapacityMixedLoadIntegrationTest.fiftyAddsWithFiftyListsAt4949BooksAllSucceedAndLeave4999WithTheProcessServing` |
| Criterion 4: eight slow list readers (raw sockets, 16 KB per 20 ms) on 5,000 books | no OutOfMemoryError, process alive, list afterwards 200 with total 5000 | `...eightSlowListReadersCauseNoOutOfMemoryAndTheProcessServesAfterwards` |
| SEC-F-166 / M-2 / C-11 (a), (b) cold: 4,999 books, shared 200-code-point author (worst case of the duplicate scan), fresh JVM | first add 201 < 2 s; duplicate of first, middle, last 409 DUPLICATE_BOOK < 2 s (the add is deleted first because at 5,000 LIBRARY_FULL comes before the duplicate check) | `ColdStartBoundIntegrationTest.firstAddOnAFreshJvmThenDuplicates...` and `...eachFirstRequestOfAFreshJvmBeingADuplicate...` (one fresh JVM per book, the duplicate is the first mutating request) |

Synchronisation uses a CyclicBarrier (`ApiCalls.together`) and a latch; no `Thread.sleep`. The 20 ms pause of `SlowReader` is a timed latch wait that throttles the read; it is not synchronisation. The test harness readiness check is one list request, so the add and duplicate code paths are cold but the list path is warm.

## Measured numbers (machine: Linux amd64, 4 CPUs, 16 GB, Java 21.0.12.1; real output lines)
```
EVIDENCE C-05 | status=200 body_bytes=14641684 elapsed_ms=668 (recorded, no gate)      (first run 764 ms)
EVIDENCE C-08 | pairs=20 statuses=201/200 only, max count observed=5000 longest_add_ms=316
EVIDENCE C-11c | adds={201=50} lists={200=50} longest_request_ms=7076 pool_timeout_ms=5000 requests=100
EVIDENCE C-11c heap | gc_peak=221M of 269M rss_peak=VmHWM: 537088 kB
EVIDENCE slow reader | HTTP/1.1 200 bytes=466944 ended=deadline   (x8, 458752 to 483328 bytes each, 12 s deadline)
EVIDENCE slow readers heap | gc_peak=220M of 248M rss_peak=VmHWM: 519808 kB xmx=512m
EVIDENCE cold | fresh JVM, first request is the duplicate of book 0 | 409 DUPLICATE_BOOK | 444 ms
EVIDENCE cold | fresh JVM, first request is the duplicate of book 2499 | 409 DUPLICATE_BOOK | 428 ms
EVIDENCE cold | fresh JVM, first request is the duplicate of book 4998 | 409 DUPLICATE_BOOK | 488 ms
EVIDENCE cold | fresh JVM, first add | 201 | 392 ms
EVIDENCE cold | duplicate of book 0 | 409 DUPLICATE_BOOK | 129 ms
EVIDENCE cold | duplicate of book 2499 | 409 DUPLICATE_BOOK | 110 ms
EVIDENCE cold | duplicate of book 4998 | 409 DUPLICATE_BOOK | 108 ms
```
(The first run of the same tests, before one test-code fix, gave C-05 764 ms, mixed load longest 7050 ms, gc_peak 205M and 229M, cold first add 387 ms, duplicates 376 and 425 ms; all within the same ranges.)

Memory bound (SEC-F-081): the peak heap before a collection is 205 to 229 MB of the 512 MB maximum (GC log of the child, `-Xlog:gc`), the peak resident set 520 to 560 MB. **512 MB is confirmed; no other value is needed.** The README already carries `-Xmx512m` in both commands (line 33 `spring-boot:run` form, line 37 jar form), so criterion 4's README edit is a no-op; README diff: none.

## Observations (no stop condition, for Liaison and Security)
1. **Longest request 7.05 to 7.08 s against the 5 s pool timeout (SEC-F-167).** All 100 requests succeeded (50 times 201, 50 times 200), so no request waited more than 5 s for the one pooled connection. The figure is the whole client-side exchange of the last queued request (100 requests, 8 Tomcat threads, one connection, each list 14 MB), not the pool wait. The spec gives no latency bound for C-11 (c); the figure is recorded, not gated.
2. **Slow readers were not cut by the server within 12 s** (first trial with a 40 s deadline: the same, "ended=deadline"). The 5 s `connection-timeout` governs the request read (E-22), not a stalled response write, and the readers are loopback only. No memory growth (peak heap below 230 MB) and the process served afterwards, so criterion 4 passes. If Security wants a write timeout it is a production change (ASK FIRST).
3. **SEC-F-126 (`static-locations` key, "Task 42 decides whether to remove it")**: not decided here; removing it is a production edit outside this task's file list. Left as is; the PM or Sponsor decides.

## Sweep (criteria 5 to 9)
Criterion 5, SpotBugs with Find Security Bugs, from the full build: `BugInstance size is 0`. Exclusions: `backend/config/spotbugs/exclude.xml` holds none (an empty `<FindBugsFilter>` with the justification template in a comment); nothing was added.

Criterion 6, grep gates in `backend/src/main` (run from that directory; exit 1 means no match):
```
$ grep -rniE 'select\s+([a-z_]+\.)?\*' .                                  [exit 1, empty]
$ grep -rnE 'RestTemplate|RestClient|WebClient|HttpClient|HttpURLConnection|URLConnection|openConnection|openStream|java\.net\.http' --include=*.java .   [exit 1, empty]
$ grep -rnE 'System\.(out|err)' --include=*.java .                         [exit 1, empty]
$ grep -rn 'printStackTrace' .                                              [exit 1, empty]
$ grep -rnE 'System\.currentTimeMillis|LocalDateTime\.now|Instant\.now|LocalDate\.now|ZonedDateTime\.now|OffsetDateTime\.now|new Date\(|Clock\.system|\.now\(' --include=*.java .
./java/com/globant/globerbooks/config/ClockConfig.java:21:        return Clock.systemUTC();      <- the clock bean itself, allowed
$ grep -rnE 'CrossOrigin|getSession|sendRedirect|WebSocket|websocket|ServerEndpoint' .   [exit 1, empty]
$ grep -rnE 'createNativeQuery|nativeQuery|@NamedNativeQuery|JdbcTemplate|createStatement|prepareStatement|DriverManager|java\.sql\.' --include=*.java .
 hits only in config/sqlite/SqliteDataSourceProvider.java and config/sqlite/StoredLibraryValidator.java (JDBC Connection and Statement; allowed by AP-12.3)
$ grep -rnE 'nativeQuery' --include=*.java .                                [exit 1, empty]
```
Checkstyle (SC-07, SC-14, SC-27 patterns) also ran green in the build.

Criterion 7: `./mvnw -o -q dependency:tree -DoutputFile=<scratch>/tree.txt` then `diff tree.txt config/dependency-tree.txt`: no output, exit 0 (115 lines each). Coverage: merged JaCoCo line coverage 95.55 percent (1,225 covered, 57 missed); "All coverage checks have been met."

Criterion 8 (`export PATH=/opt/node24/bin:$PATH`, Node v24.22.2, in `e2e/`): `npm audit --audit-level=high` printed `found 0 vulnerabilities`, exit 0. Lockfile: 3 `resolved` entries, all on `registry.npmjs.org`; none elsewhere. No `npm install` was run; `git status` shows the lockfile untouched.

Criterion 9: README line 42 states "The database file is locked exclusively while the application runs: no other tool (no `sqlite3`, no viewer) can open it. Stop the application first." Present; no edit.

## Criterion and trace map
| Criterion or trace | Evidence |
|---|---|
| 1, C-05 | `CapacityPerformanceIntegrationTest` list test; 14,641,684 bytes, 668 ms |
| 2, C-08 | same class, loop test |
| 3, C-11 (c), SEC-F-167 | `CapacityMixedLoadIntegrationTest` mixed test; observation 1 |
| 4, SEC-F-071, SEC-F-081 | same class, slow readers; heap figures; 512 MB confirmed |
| 5 SEC-F-166, M-2 | `ColdStartBoundIntegrationTest`; figures above (maximum 488 ms) |
| 5 SC-06 values | not tuned; no change |
| 5 SC-07, SC-14, SC-18, SC-27, SC-41 | grep gates and Checkstyle (criterion 6); SpotBugs 0 |
| SC-17, SC-19 | dependency tree diff empty (criterion 7) |
| SC-20, SC-36 | npm audit and lockfile hosts (criterion 8) |
| X-03 | recorded as above; no production change |
| N-9 | README line 42 |
| SEC-F-168 | the mixed load and the duplicate rules ran against the one-connection pool (`maximum-pool-size: 1`) and gave only 201, 200 and 409 |
| SEC-F-174, SEC-F-191 | cold figures on the jar (200 to 490 ms); the 5,000-book libraries opened through the stored-library validator in every child JVM here |

## Full build
`cd backend && ./mvnw clean verify` (background, 17:22:14 to 17:29:56, 7 min 42 s): unit tests `Tests run: 977, Failures: 0, Errors: 0, Skipped: 0`; integration tests `Tests run: 330, Failures: 0, Errors: 0, Skipped: 1` (the skip is in `SqliteStoreIntegrationTest`, existing); SpotBugs `BugInstance size is 0`; `All coverage checks have been met.`; `BUILD SUCCESS`.
New test times: `CapacityPerformanceIntegrationTest` 3.9 s, `CapacityMixedLoadIntegrationTest` 30.7 s, `ColdStartBoundIntegrationTest` 22.2 s. None over 60 s. (Existing test over 60 s: `DurabilityIntegrationTest` 136.7 s.)
Leftover check: `pgrep -af globerbooks.jar` empty after the run; every child is stopped in a `finally`, and `AppProcess` has a shutdown hook.

## Deviations
- Branch name as noted above; the 75-minute estimate was not exceeded in a way that needs the split back into 41 and 42, so none was made (two commits mark the boundary).
- Titles and authors of the capacity tests use code points outside the basic plane only (the spec C-05 wording), not the mixed `LibrarySeeder` filler; this is the larger body.
- A stray empty-variable mistake of mine created the file `/run1.log` at the filesystem root (an unset `TMPDIR` in a first build command); the removal was blocked by the safety check. It holds only an old Checkstyle error line and is not part of the repository; please delete it.

## Risks and follow-ups (not implemented)
Observations 1 to 3 above. A fixed-heap run at a lower value (for example 256 MB) was not tried: the brief asked to confirm or revise 512 MB, and the measured peak (230 MB of heap, 540 MB resident) supports 512 MB without a margin to cut.

## git diff --stat origin/main..HEAD (before the commit of this file)
```
 .../CapacityMixedLoadIntegrationTest.java          | 146 +++++++++++++++++
 .../CapacityPerformanceIntegrationTest.java        | 116 ++++++++++++++
 .../globerbooks/ColdStartBoundIntegrationTest.java |  92 +++++++++++
 .../com/globant/globerbooks/support/ApiCalls.java  |  11 ++
 .../globant/globerbooks/support/AppProcess.java    |  39 ++++-
 .../globerbooks/support/CapacityFixtures.java      | 172 +++++++++++++++++++++
 .../globant/globerbooks/support/SlowReader.java    |  72 +++++++++
 .../160-orchestrator-to-java-task-41-42-brief.md   |  26 ++++
 docs/sdd/progress.md                               |   5 +
 docs/sdd/status.md                                 |   2 +-
 10 files changed, 674 insertions(+), 7 deletions(-)
```
(`docs/sdd/status.md` and the brief 160 come from the orchestrator's commit 8365393, not from me.)

## git log origin/main..HEAD --format='%an <%ae>%n%B---'
```
57e8999 krlosferra <carlos.ferra@globant.com>
docs(progress): Task 41+42 capacity figures and release sweep results [Task-41-42]
---
b387316 krlosferra <carlos.ferra@globant.com>
test(capacity): maximum-size list, add loop, mixed load, slow readers and cold bounds [Task-41-42]
---
8365393 krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 41+42 brief, Task 34b done [Task-41-42]
---
```
