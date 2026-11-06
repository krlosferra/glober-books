# Handoff 147: Security review, chunk 32+33 (Task 32 concurrency and fault injection, Task 33 durability)
- From: security
- To: orchestrator
- Phase: 4   Round: 1
- Type: AUDIT_REPORT

## Verdict: PASS WITH CONDITIONS
No CRITICAL or HIGH finding. The chunk is tests only and its tests are sound. The review found a real gap in the product the tests ran against (SEC-F-178, MEDIUM) that earlier Security evidence did not make visible. It does not block this pull request. It blocks any tag or release.

Conditions:
- C-1 (SEC-F-178, MEDIUM): no tag and no release before Task 34a-1 is merged and Security has re-probed the packaged jar (see section 3).
- C-2 (SEC-F-180, LOW): standing rule, keep `org.hibernate` at INFO; no code change.

## 1. Scope and method
Scope: `git diff origin/main..HEAD` (base e8be07a, HEAD c447039): 7 test files under `backend/src/test`, handoffs 144/145, `progress.md`, `status.md`, `tasks.md` 0.6.4. No `src/main`, `pom.xml` or `backend/config/**` change.
Method: read all seven test files and the provider, `application.yml`, `StartupPolicy`; built the jar from `git archive HEAD` (backend and frontend) in the scratchpad; started it on loopback ports 18551 to 18554 with temp data directories (never 8080 or 18080) and probed it. I did not run the full `./mvnw verify` (see section 5). Processes started by me were killed; `pgrep -f globerbooks.jar` is empty at the end. No git write.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-178 | MEDIUM | Configuration, persistence | `application.yml` data source; `SqliteDataSourceProvider` used only by tests | The jar uses Boot's auto-configured data source, not the provider | umask 022, existing 0755 `data/`: `library.db` is 0644, no `tmp`; missing directory: exit 1, SQLITE_CANTOPEN; second instance on the same file started (port 18553) and its POST returned 201; a Python process took `BEGIN IMMEDIATE` while both ran; `/proc/<pid>/fd` shows 1 descriptor on the file per instance (pool of 1 is live); `journal_mode` = delete | Another local user reads the reading list file directly (R-16 already accepts API access on a shared machine, the file read is extra); two instances on one file break the count and duplicate rules across processes; hot-journal recovery order (R-20) not implemented | Task 34a-1: wire `DataDirectoryGuard` and the provider as the application data source; then Security re-probes the jar | SC-13, SC-32, SC-06, SEC-F-098, SEC-F-140, SEC-F-168, US-009.7 |
| SEC-F-179 | INFO | A-09 | `src/main` | No test-only code in production | grep for create trigger, raise(, faultinjection, appprocess, x-probe, test-only, fault. in `backend/src/main`: empty; `git diff --name-only` shows only tests and docs | none | none | A-09 |
| SEC-F-180 | LOW | Log hygiene | server log after a forced SQL failure | WARN prints the SQLite message with the trigger text, ERROR prints the exception class | Reproduced on the jar: `WARN org.hibernate.orm.jdbc.error : HHH000247: ErrorCode: 19` and `[SQLITE_CONSTRAINT_TRIGGER] ... (forced-fault-secret-marker)`; `ERROR GlobalExceptionHandler : Unexpected error: org.springframework.orm.jpa.JpaSystemException ...`. No SQL, bind value, title, author or id | None for a client (response is the generic envelope). Engine text could name a constraint, never a value | Keep `org.hibernate` at INFO | SC-14, SC-37 |
| SEC-F-181 | INFO | Test limits | `DurabilityIntegrationTest` | SIGKILL proves process-crash durability only; companion-file clause vacuous (no `-journal` at kill points); creation modes not proved | code read | none | State limits in Task 34b brief and release notes | SC-13, R-20 |
| SEC-F-167, SEC-F-177 | INFO | Concurrency | production | Closed by evidence in section 4 | section 4 | | | |

## 3. Question 1: the packaged application versus the provider
Reproduced, with my own numbers (scratch copy of HEAD, `-Xmx512m -jar target/globerbooks.jar --server.address=127.0.0.1 --server.port=<port> --spring.datasource.url=jdbc:sqlite:<dir>/library.db`):

| Check | Result in the jar | Designed in the provider |
|---|---|---|
| Missing data directory | start fails, exit 1, `SQLiteException` CANTOPEN, nothing created | created 0700 |
| File mode (umask 022) | `library.db` 0644, directory as the caller made it (0755) | 0600 / 0700 |
| `tmp` directory | absent | created 0700 |
| Pool | 1 connection (yml; one fd on the file) | 1 |
| journal_mode | delete | delete, checked |
| Claim / `locking_mode=EXCLUSIVE` | absent: second instance started and wrote; third process got a write lock | `BEGIN EXCLUSIVE` claim, second JVM fails |
| `foreign_keys`, `trusted_schema=OFF`, `BEGIN IMMEDIATE`, `busy_timeout` 5000, extension loading off | not set (driver defaults) | set per connection |
| Rollback on failure, generic 500 | works (section 5) | n/a |

Evidence that rested on the provider and is overstated for the jar (security.md annotated in place):
- SC-13 (Task 03a, handoff 052): modes verified for the provider only. Now FAILED for the jar.
- SC-32 (Task 03a read-back of the pragmas, "second JVM fails"): provider only. Journal mode and pool of 1 hold in the jar; the rest does not.
- SC-06: the Hikari 5 s timeout and pool of 1 are live; the SQLite `busy_timeout` is not.
- SEC-F-168 (handoff 131): "PRAGMA locking_mode=EXCLUSIVE, BEGIN IMMEDIATE ... a second process is excluded by the claim" is wrong for the jar. The in-process serialisation conclusion stands (pool of 1 plus one transaction).
- SEC-F-098, SEC-F-140: already conditional on 34a; confirmed not live. R-20 and US-009.7 exclusion: not implemented in the jar yet.

Severity: MEDIUM. Loopback only, single user (R-16 accepted), and the plan always put the wiring in Tasks 34a/34b, so there is no remote path and no data loss. But SC-13 and US-009 are accepted controls that earlier reviews recorded as verified, so it is a release blocker, not an INFO.

What must be true before the release tag (re-scoped Task 34a-1), checked by me on the jar:
1. Starting on an absent location creates the directory 0700 and the file 0600 before the first connection, whatever the umask (try 022 and 000); `-journal` after a write is 0600.
2. A pre-existing wider directory or file, a symlink, or a foreign owner is refused with a clean message (SEC-F-098), or at minimum 34a-2 follows before the tag.
3. A second instance on the same file fails at start with a clean message; a third process cannot take a write lock while the first runs.
4. Pool 1, `locking_mode=EXCLUSIVE`, `BEGIN IMMEDIATE`, `trusted_schema=OFF`, `foreign_keys=ON`, `busy_timeout` set, read back on the running jar (a test through the application's data source, not only through the provider class).
5. Recommend one jar-level test (AppProcess) for items 1 and 3, so the gap cannot reappear. I agree with the Liaison (handoff 146) that a wired guard that creates but does not yet refuse symlinks and wide modes is a regression window; if 34a is split, 34a-1 must not reach `main` alone unless nothing is tagged between them.

## 4. Questions 2 to 4: tests
- Test-only code (A-09): see SEC-F-179. The trigger is created by `FaultInjection` through the test application's own pool; `AppProcess` and `ApiCalls` are in `src/test`.
- Concurrency tests: expected counts equal the spec rows; allowed-set oracles; barrier, no `Thread.sleep` (grep of the seven files: none).
- My own probes on the jar (Python threads released by a barrier, scratch database):
  - 50 parallel adds with 4,999 books stored: `{201: 1, 409 LIBRARY_FULL: 49}`; 5,000 books after; a second burst of 50: `{409 LIBRARY_FULL: 50}`.
  - 300 iterations of 4 parallel calls on one completed book (status to a random target, PUT rating 4, PUT rating 2, DELETE rating): 1,008 x 200, 192 x 400 `RATING_NOT_ALLOWED`, 0 violations of INV-1 to INV-4.
  - 10 parallel deletes of one id: 1 x 200, 9 x 404.
  - No production race found; the pool of 1 serialises transactions inside the process. Cross-process races exist only because of SEC-F-178.

## 5. Question 3 and 5
- Forced failure on the jar (I created the trigger in the scratch database with Python): `PUT .../status {"status":"reading"}` gave `500 {"error":"Unexpected error.","code":"INTERNAL_ERROR","timestamp":"2026-11-06T18:47:34.303Z"}`; the book before and after is byte-identical; the next request is served at once. Log lines: SEC-F-180 (LOW, log hygiene, no book data).
- `AppProcess`: environment cleared then `PATH HOME JAVA_HOME LANG TZ` only; `--server.address=127.0.0.1`; free loopback port chosen by the OS; data and console files under JUnit `@TempDir`; kill in `@AfterEach` plus a shutdown hook; no secrets in the command line; the test asserts the console never holds the marker. `System.out.println("EVIDENCE ...")` carries no book text. Leftover processes after my runs: none.
- The FaultInjection trigger text constant is a fixed marker; no real data.

## 6. Controls checked
SC-06 VERIFIED (timeout, pool) with the busy-timeout correction; SC-11 VERIFIED (jar); SC-13 FAILED for the jar (tests prove non-widening only); SC-14 VERIFIED with SEC-F-180 note; SC-32 downgraded for the jar (not set to VERIFIED); A-09 VERIFIED; SC-33/E2E not touched.

## 7. Not verified
- I did not run the full `./mvnw clean verify` (about 5 minutes, beyond a safe tool window); the green result (900 unit, 269 integration) is the developer's claim and the Liaison also did not obtain one. The orchestrator build check must supply it before the pull request is opened. I built the jar with tests skipped.
- I did not re-run `DurabilityIntegrationTest` (about 3 minutes, 20 JVM starts). I reproduced its premises by hand instead.
- Actual SQLite pragma values (`foreign_keys` etc.) on the jar were not read back, because that needs code or a test through the application's data source; the conclusion rests on the configuration being absent from `application.yml` and on the behaviour probes (no lock exclusion).
- Power-loss durability.
- Public advisory databases were not consulted: no dependency change in this chunk.

## 8. Residual risks for the Sponsor
- R-16 stays as accepted. With SEC-F-178 open, the 0644 file extends it to direct file reads on a shared machine. Recommendation: accept only until Task 34a-1; do not tag before.
- R-20 (hot journal) accepted earlier; not yet implemented in the jar.

## 9. security.md changes
Version 0.50; SC-13 set to FAILED (jar) with the probe; SC-06, SC-11, SC-14, SC-32 evidence notes; SEC-F-098, SEC-F-140 annotated; SEC-F-168 corrected; SEC-F-167 and SEC-F-177 closed; SEC-F-178 to SEC-F-181 added; R-20 note; changelog. `status.md`: rows 32 and 33 added (IN_REVIEW, pull request 39, Security PASS WITH CONDITIONS). Transparency: eight single-line replacements in `security.md` were made with a read-replace-write script before I applied the Edit-only instruction; `git diff` shows exactly those eight lines changed, nothing else. The remaining edits used Edit.

## 10. Next step
Orchestrator: supply the green `./mvnw clean verify`, then open pull request 39 after the Sponsor gate. PM/Liaison: carry C-1 into the 34a-1 brief (acceptance by jar probe, items 1 to 5 above). Security re-verifies at the 34a-1 review. Nothing was committed.
