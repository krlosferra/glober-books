# Handoff 157: Task delivery, Task 34b (stored-library validation, hot journal and second instance)
- From: java
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY

## Resume point
- Done: everything in the brief. Production, unit tests, jar-level tests, README sentence, progress.md, status.md row (IN_REVIEW), full `cd backend && ./mvnw clean verify` green (6 min 24 s), evidence below.
- Next: nothing for the implementer. Orchestrator: Liaison audit, Security review (protected surface, SC-32, SC-39), Sponsor gate. Local commits only, not pushed.

## 1. Task, branch, commits
Task 34b, branch `feature/task-34b-stored-library-validation`. Commits are in section 11 (human identity, no trailers, no session links).

## 2. Files
Production (`backend/src/main/java/com/globant/globerbooks/config/sqlite/`): created `StoredLibraryValidator`, `StoredLibraryHeader`; edited `SqliteDataSourceProvider`, `StartupFailureAnalyzer`. Not touched: `SqliteDataSourceConfig` (no change was needed), `pom.xml`, `application.yml`, `backend/config/**`, the schema.
Tests: created `StoredLibraryValidatorTest`, `StoredLibraryRowCheckTest`, `StoredLibraryRefusalIntegrationTest`, `HotJournalIntegrationTest`, `StoredLibraryAcceptIntegrationTest`, `SecondInstanceIntegrationTest`, `NativeFilesOnRefusalIntegrationTest`, `JournalWriterMain`, `RefusedStartMain`, `support/StoredLibraryFixtures`, `support/HotJournalWriter`. `StartupRefusalIntegrationTest` is NOT edited (new classes instead, it stays at 192 lines and the 34a-2 second-instance test stays as it is).
Docs: `README.md` (one sentence), `docs/sdd/progress.md`, `docs/sdd/status.md` (own row), this file. Nothing else.

## 3. Design
- `SqliteDataSourceProvider.create(Guarded)` order: (1) `StoredLibraryHeader.check` by plain I/O before the pool: zero length returns "empty" (validator skipped); otherwise a shared whole-file lock is tried (held by another process means "in use": the existing fixed `DATABASE_UNAVAILABLE` reason, never "damaged"), then the 100 bytes: magic, bytes 18 and 19 (2 is WAL-marked, anything but 1 is not of this kind), page size, and the size test (file size equals page size times page count when the count is valid), which is skipped when a non-empty `-journal` exists; (2) the pool opens and claims (`BEGIN EXCLUSIVE`): this is where SQLite rolls back a hot journal, nowhere else; (3) `requireSameFile`; (4) `StoredLibraryValidator.validate` on the claimed connection with `query_only` ON, reset to OFF in a `finally`; (5) `schema.sql`; Hibernate follows.
- `StoredLibraryValidator.validate`: (a) `SELECT name, type, tbl_name, sql FROM sqlite_master` (view, trigger or `CREATE VIRTUAL` anywhere is `FORBIDDEN_OBJECT`; no table is `NO_TABLES`; exactly one table `books`, no other object but `sqlite_autoindex_books_1`, and the stored DDL normalised equals `schema.sql` normalised, otherwise `SCHEMA_DIFFERS`); (b) `PRAGMA quick_check` must be the single row `ok`, any SQL error anywhere is `DAMAGED`; (c) pre-check query with explicit columns, `typeof()` and `length(CAST(.. AS BLOB))` only, `ORDER BY rowid LIMIT 5001` (more than 5,000 is `TOO_MANY_BOOKS`; a non-text cell is `INVALID_ROW`; a cell over 36/2000/800/9/24 bytes is `OVERSIZED_CELL`), no text is fetched; (d) only then the rows one at a time, judged by `requireRow`: id lowercase UUID shape, title 1 to 500 and author 1 to 200 code points counted in Java, NUL refused, status in the set, rating null or 1 to 5 only when completed (INV-1, INV-2), `date_completed` present iff completed (INV-4), both timestamps in the fixed form and a real date (strict resolver). Failure is `INVALID_ROW`.
- Nine fixed reasons, each its own text, no digit, path or cell value: `NOT_A_DATABASE`, `WAL_MARKED`, `DAMAGED`, `NO_TABLES`, `SCHEMA_DIFFERS`, `FORBIDDEN_OBJECT`, `TOO_MANY_BOOKS`, `OVERSIZED_CELL`, `INVALID_ROW`. `StartupFailureAnalyzer` recognizes `StoredLibraryValidator.Refusal` and prints `text + "."` with the one fixed action; no cause is kept.
- SEC-F-186: `NativeDirectoryModes.narrow` now runs in a `finally` around the pool opening. Stale extracted files are NOT removed at start, on purpose: a second instance cannot tell a stale file from the library a running first instance is using, and deleting a live instance's files is worse than the LOW finding. `NativeFilesOnRefusalIntegrationTest` shows the files of a refused start are 0600 (read before the JVM removes them). Note the `finally` covers pool-open failures, which the shared-lock check now makes rare; the test exercises a refused start after the pool opened.
- Why a separate `StoredLibraryHeader`: the validator alone passed 300 lines; the brief placed the header check in the provider, the provider only calls it.

## 4. Criteria and traces to tests (expected values from tasks.md, spec US-009 and security.md)
| Criterion / trace | Code | Tests |
|---|---|---|
| 1 order: header, `sqlite_master`, `quick_check`, rows with `LIMIT 5001`, pre-check before text, code points in Java (SEC-F-068, SC-32) | `StoredLibraryValidator.validate`, `StoredLibraryHeader.check`, provider order | `StoredLibraryValidatorTest` (19), `StoredLibraryRowCheckTest` (4: code point boundaries 1/500/501 and 1/200/201, supplementary characters, NUL, rating 0/1/5/6, INV-1 to INV-4, id and date shapes), jar: `StoredLibraryRefusalIntegrationTest` |
| 2 each listed case: non-zero exit, no request served, fixed reason, bytes unchanged (US-009.5, DEC-028, SC-39) | same + analyzer | `StoredLibraryRefusalIntegrationTest` (17): non-database, WAL-marked, truncated, garbage page, no tables, extra table, books without CHECKs, extra index, view, trigger, virtual table, INV-1, INV-4, bad timestamp, NUL title, oversized cell, 5,001 books; each asserts exit non-zero, the fixed text of its category and no other category text, the whole tree and SHA-256 equal before and after |
| 3 SEC-F-181 / Liaison F-1: marker in the invalid row never on console | `ConsoleRules` (marker `ZQXDATA`) | the same class: for 11 cases the marker is first shown to be in the file bytes, then absent from the console (the console is the only log sink, `application.yml` has no log file) |
| 3(i) valid file + genuine hot journal recovered and accepted (CR-9, R-20, SEC-F-074) | header size skip, claim rollback | `HotJournalIntegrationTest.validFileWithAGenuineHotJournalIsRolledBackAndAccepted` |
| 3(ii) invalid file + genuine hot journal: refused, hash after refusal = hash after reference rollback | same | `...invalidFileWithAGenuineHotJournalIsRefusedAndNothingIsWrittenAfterTheRollback` |
| 3(iii) garbage journal beside valid, beside invalid, beside a non-database | same | `...garbageJournalBesideAValidFile...`, `...BesideAnInvalidFile...`, `...BesideANonDatabase...` |
| 4 absent / empty created, valid opens, no book altered (US-009.3) | provider (empty decided before the claim) | `StoredLibraryAcceptIntegrationTest` (4: absent, zero-length, 40 valid books with rows hash and file hash, exactly 5,000 books) |
| 5 second instance, no false "damaged" (US-009.7, DEC-032, C-12) | `StoredLibraryHeader` shared lock, claim | `SecondInstanceIntegrationTest` (2), plus 34a-2's own test unchanged; unit `headerReportsInUseNotDamagedWhenAnotherProcessHoldsTheClaim` |
| SEC-F-186 | `finally narrow` | `NativeFilesOnRefusalIntegrationTest` |
| SEC-F-074 skip rule | `StoredLibraryHeader.require` | `headerSkipsTheSizeTestWhenANonEmptyJournalExistsButNotForAnEmptyOne` |
| SC-32, SC-39 | wiring unchanged from 34a-2, validator added | all of the above; `SqliteWiringIntegrationTest` stays green |

## 5. Test specification (cases derived from the spec, not from the code)
Header: zero length accepted (and reported empty); text file and a 16-byte file refused as not a database; WAL marker (bytes 18, 19 = 2, created with `PRAGMA journal_mode=WAL`); one page cut off refused as damaged; the same with an empty journal still refused, with a non-empty journal accepted; impossible page size; unknown format version; held by another JVM: "in use". Structure: no tables; extra table; replaced table; books with no CHECKs; books missing exactly `ck_books_rating`; extra index; view; trigger; FTS5 virtual table. Damage: a garbage page behind a valid header. Rows: boundaries as listed in section 4; 5,000 accepted and 5,001 refused; NUL in a title that the SQL CHECK lets through (`length()` stops at NUL); an author of 5,000 bytes after a NUL (oversized, passes the CHECK); `2026-13-45T25:61:61.000Z` (passes the GLOB CHECK); `2026-02-30...` refused by the strict date. Jar level: the cases of section 4 plus the hot-journal, absent/empty/valid and second-instance runs.

Inventory: unit 23 new (surefire total 972, 0 failures); jar-level 29 new (failsafe total 318, 0 failures, 1 skipped that already exists on main: root ignores permission bits).

## 6. Output of `cd backend && ./mvnw clean verify`
```
[INFO] Tests run: 972, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 318, Failures: 0, Errors: 0, Skipped: 1
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
[INFO] Total time:  06:24 min
```
Checkstyle, SpotBugs and the source scan (no test-only switch in `src/main`) ran inside this build and passed. Line coverage (merged, JaCoCo CSV): total 95.5%; `StoredLibraryValidator` 94.7% (+ `Reason` 100%, `Refusal` 100%), `StoredLibraryHeader` 97.1%, `SqliteDataSourceProvider` 82.9% (+ `ClaimingDataSource` 91.3%), `StartupFailureAnalyzer` 83.3%. Elapsed time of the new tests: `StoredLibraryRefusalIntegrationTest` 36.3 s (17 jar starts), `StoredLibraryAcceptIntegrationTest` 18.5 s, `SecondInstanceIntegrationTest` 16.4 s, `HotJournalIntegrationTest` 15.1 s, `NativeFilesOnRefusalIntegrationTest` 0.4 s, unit 0.9 s and 0.01 s. None over 60 s. Leftover processes after the run: `pgrep` for the jar, the writer and the claim holder found none.

## 7. Refusal table (jar, umask 022, one run per row; reason category fixed text; marker = `ZQXDATA` planted in the invalid row)
| Case | Exit | Reason (fixed text, category) | Marker in file / on console | SHA-256 main file before = after (first 8 hex) |
|---|---|---|---|---|
| non-database (text with marker) | 1 | the file is not a database of the kind used by the application (NOT_A_DATABASE) | yes / no | 40c3bcc7 = 40c3bcc7 |
| WAL-marked | 1 | the file is marked as using write-ahead logging (WAL_MARKED) | yes / no | 01ed4554 = 01ed4554 |
| damaged: one page cut off | 1 | the database file is damaged (DAMAGED, header size test) | n/a / no | f12f4e8a = f12f4e8a |
| damaged: garbage page, valid header | 1 | DAMAGED (quick_check or schema read) | n/a / no | 48dd7b33 = 48dd7b33 |
| no tables | 1 | the database holds no table (NO_TABLES) | n/a / no | 43c21347 = 43c21347 |
| other structure: extra table | 1 | the structure differs from the Book structure (SCHEMA_DIFFERS) | yes / no | 9b1f9467 = 9b1f9467 |
| books without its CHECKs (row with marker) | 1 | SCHEMA_DIFFERS | yes / no | ce68e62a = ce68e62a |
| extra index | 1 | SCHEMA_DIFFERS | n/a / no | 22bb6a53 = 22bb6a53 |
| view | 1 | a view, trigger or virtual table exists (FORBIDDEN_OBJECT) | n/a / no | 3f2de68b = 3f2de68b |
| trigger | 1 | FORBIDDEN_OBJECT | n/a / no | 7f88801d = 7f88801d |
| virtual table | 1 | FORBIDDEN_OBJECT | n/a / no | 6cb8ed40 = 6cb8ed40 |
| INV-1 rating on an unread book | 1 | DAMAGED (quick_check evaluates the CHECK first) | yes / no | fc81eb90 = fc81eb90 |
| INV-4 completed without date | 1 | DAMAGED (same) | yes / no | df6fb2fb = df6fb2fb |
| timestamp matching the GLOB but not a date | 1 | a book violates a format, a limit or an invariant (INVALID_ROW) | yes / no | 59508151 = 59508151 |
| NUL inside a title | 1 | INVALID_ROW | yes / no | fffe79db = fffe79db |
| one oversized cell (author 5,000 bytes) | 1 | a stored value is larger than allowed (OVERSIZED_CELL) | yes / no | 58bf0dbf = 58bf0dbf |
| 5,001 books | 1 | there are too many books (TOO_MANY_BOOKS) | yes / no | b3a73a31 = b3a73a31 |

Console excerpt of one run (the others differ only in the reason; one line in the log, the block printed by the analyzer): `APPLICATION FAILED TO START` / `Description: Stored library: the structure differs from the Book structure.` / `Action: Fix the data location or the settings named by the reason, then start again.` No path, no stack frame, no `Caused by`, no SQL, no book data; the tests also assert that no other category text appears. The whole tree (names, modes, owners, SHA-256 of every file) is equal before and after in every row, and the journal does not exist in these rows.

## 8. Hot journal, how it was produced, and hashes
Producer: `HotJournalWriter.leaveHotJournal` starts `JournalWriterMain` in its own JVM on the file; it opens the file with the default rollback journal, `synchronous=FULL`, `cache_size=5`, `autocommit` off, inserts 3,000 books of about 400 bytes in one uncommitted transaction (the tiny cache forces dirty pages into the main file after SQLite has synced its journal), prints `READY`, and the test kills it with SIGKILL. The journal is SQLite's own; the test asserts that the file grew and the journal is non-empty. The reference rollback is computed on copies in another directory by a plain connection that reads `sqlite_master` (SQLite rolls back there); the originals are untouched.
| Case | Setup | Result | Hashes |
|---|---|---|---|
| (i) valid + genuine journal | committed 20,480 bytes, 1,695,744 after the kill, journal 17,920 bytes | accepted, 50 books served, none of the writer's rows, journal consumed, hash after the run equals the reference | before start e9d1f751..; reference 94cd939b..; after run 94cd939b.. |
| (ii) invalid (extra table) + genuine journal | committed 24,576, 1,699,840 after the kill, journal 17,920 | exit 1, `SCHEMA_DIFFERS`, marker not on console, journal consumed by SQLite's rollback, no write after | before start 07a5d324..; reference c23dea53..; after refusal c23dea53.. |
| (iii-a) garbage journal (700 bytes) beside valid | | accepted, 50 books | before 94cd939b.. = after 94cd939b..; journal removed by SQLite |
| (iii-b) garbage journal beside invalid | | exit 1, `SCHEMA_DIFFERS` | before c23dea53.. = after c23dea53..; journal removed by SQLite |
| (iii-c) garbage journal beside a non-database | | exit 1, `NOT_A_DATABASE`, SQLite never opens the file | whole tree equal, journal untouched |

## 9. Absent, empty and valid; second instance
- Absent file: created, mode `rw-------`, directory `rwx------`, stored DDL equals `schema.sql` normalised, 0 books served (size 12,288). Zero-length file (0600): same. Valid 40-book library: rows SHA-256 `128325540bb77b8b..` before and after; whole file SHA-256 `fb91b437..` before and after (the start writes nothing). Exactly 5,000 books: opens, rows hash unchanged.
- Second instance, populated library of 25 books: exit 1, `The database could not be opened or is in use by another process.`, none of the nine validator texts on its console (so no false "damaged"), the first instance alive, list body byte-equal and SHA-256 `e9aac2af..` before and after. Second variant: the first instance is adding books in a loop while two second instances start: both exit 1 with the same reason; afterwards the first serves exactly 5 plus the number of acknowledged adds.

## 10. Deviations, findings and risks
- Deviation: `StoredLibraryHeader` is a separate class (line limit) and `StartupRefusalIntegrationTest` is not edited (new classes). `SqliteDataSourceConfig` needed no change.
- The stored DDL is compared for equality only (normalised); the "or each named CHECK is present" alternative is not implemented because equality is stricter and the Task 04 proof (`BookRepositoryTest`) shows SQLite stores the same text. A database whose DDL differs only by an equivalent rewrite is refused.
- Observation 1: `quick_check` evaluates CHECK constraints, so a row that breaks INV-1, INV-4 or any CHECK is reported as `DAMAGED`, not as `INVALID_ROW`. The row judgment in Java is still the guard for what the CHECKs cannot see (NUL, real dates, byte size). The Java INV logic is proved by `StoredLibraryRowCheckTest`. The spec does not fix the reason category, so I did not treat this as a gap.
- Observation 2 (brief said main file and journal unchanged): SQLite itself deletes a garbage journal when it opens the file (cases iii-a, iii-b); the main file is unchanged, which is what US-009.5 and CR-9 require ("a journal that is not a valid journal is ignored ... changes nothing" of the database). The journal is untouched only when SQLite never opens the file (iii-c). The Liaison should confirm that this reading of CR-9 is what the brief meant.
- Observation 3: the shared-lock check in `StoredLibraryHeader` uses fcntl locks. It must run before any connection of the same process exists (documented in the Javadoc); the claim is then the second line of defense. On a file system without locks the check reports "in use" (fixed reason), fail closed.
- Observation 4: the zero-length decision is taken once, before the pool opens (the driver can write to a zero-length file during connection setup, which made the size after the claim unreliable; the first version failed the absent and empty runs for that reason, fixed and covered).
- R-25 (check-to-open race) unchanged. R-20 evidence now exists for the jar. Pragmas are still proved in process (SEC-F-188 unchanged). Not tested: a non-POSIX file system, a hot journal that is a symbolic link (the guard does not vet the journal).
- Follow-ups (not implemented): README mention is one sentence; stale extracted native files are not cleaned (reason above).

## 11. `git diff --stat origin/main..HEAD` and `git log origin/main..HEAD --format='%an <%ae>%n%B---'`
```
 README.md                                          |   2 +-
 .../config/sqlite/SqliteDataSourceProvider.java    |  23 +-
 .../config/sqlite/StartupFailureAnalyzer.java      |  12 +-
 .../config/sqlite/StoredLibraryHeader.java         |  89 +++++++
 .../config/sqlite/StoredLibraryValidator.java      | 276 +++++++++++++++++++++
 .../config/sqlite/HotJournalIntegrationTest.java   | 166 +++++++++++++
 .../config/sqlite/JournalWriterMain.java           |  52 ++++
 .../NativeFilesOnRefusalIntegrationTest.java       |  43 ++++
 .../config/sqlite/RefusedStartMain.java            |  39 +++
 .../sqlite/SecondInstanceIntegrationTest.java      | 119 +++++++++
 .../sqlite/StoredLibraryAcceptIntegrationTest.java | 109 ++++++++
 .../StoredLibraryRefusalIntegrationTest.java       | 229 +++++++++++++++++
 .../config/sqlite/StoredLibraryRowCheckTest.java   |  94 +++++++
 .../config/sqlite/StoredLibraryValidatorTest.java  | 273 ++++++++++++++++++++
 .../globerbooks/support/HotJournalWriter.java      | 101 ++++++++
 .../globerbooks/support/StoredLibraryFixtures.java | 172 +++++++++++++
 .../156-orchestrator-to-java-task-34b-brief.md     |  26 ++
 .../157-java-to-liaison-task-34b-delivery.md       | 103 ++++++++
 docs/sdd/progress.md                               |   3 +
 docs/sdd/status.md                                 |   4 +-
 20 files changed, 1926 insertions(+), 9 deletions(-)
```
(taken before the final amend, which only changes the line count of this file)

```
krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 34b delivery [Task-34b]
---
krlosferra <carlos.ferra@globant.com>
docs(storage): README, progress and status for Task 34b [Task-34b]
---
krlosferra <carlos.ferra@globant.com>
test(storage): extracted native files are 0600 after a refused start [Task-34b]
---
krlosferra <carlos.ferra@globant.com>
feat(storage): decide the empty file before the claim; absent, empty, valid and second-instance tests [Task-34b]
---
krlosferra <carlos.ferra@globant.com>
test(storage): genuine hot journal and garbage journal on the jar [Task-34b]
---
krlosferra <carlos.ferra@globant.com>
test(storage): stored-library refusals on the packaged jar [Task-34b]
---
krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 34b delivery, resume point [Task-34b]
---
krlosferra <carlos.ferra@globant.com>
test(storage): validator and header unit tests, fixtures and hot journal writer [Task-34b]
---
krlosferra <carlos.ferra@globant.com>
feat(storage): validate the stored library before anything writes to it [Task-34b]
---
krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 34b brief, Task 34a-2 done [Task-34b]
---
```
