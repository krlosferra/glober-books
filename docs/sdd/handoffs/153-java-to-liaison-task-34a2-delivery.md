# Handoff 153: Task delivery, Task 34a-2 (wire the packaged application to the guard and prove it on the jar)
- From: java
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY

## Resume point
- Done: wiring, analyzer, guard follow-ups, tests, README, progress.md, status.md row, full `./mvnw clean verify` green (949 unit, 289 integration, 1 skipped that exists on main: root ignores permissions), smoke E2E 3/3, this file.
- Next: nothing for the implementer. Orchestrator: push, Liaison audit, Security re-probe of the jar (SEC-F-178), Sponsor gate. Local commits only, not pushed.

## 1. Task, branch, commits
Task 34a-2, branch `feature/task-34a2-wire-guard`, commits listed in section 10 (all authored by the configured human identity, no trailers).

## 2. Files
Production (`backend/src/main/java/com/globant/globerbooks/config/sqlite/`): created `SqliteDataSourceConfig` (the data source bean), `StartupFailureAnalyzer`, `OwnerOnlyCheck` (owner/mode check moved out of the guard, plus the hard-link count), `NativeDirectoryModes` (see 6); edited `DataDirectoryGuard` (283 lines), `SqliteDataSourceProvider` (242). Edited `src/main/resources/META-INF/spring.factories` (one line registering the analyzer). No `pom.xml`, no `backend/config/**`, no `application.yml` change.
Tests: created `SqliteWiringIntegrationTest`, `JarDataLocationIntegrationTest`, `StartupRefusalIntegrationTest`, `SqliteDataSourceConfigTest`, `DataDirectoryGuardCleanupTest`, `support/ConsoleRules`; edited `support/AppProcess` (231 lines: launch with a URL, a umask via `sh -c 'umask "$1" && shift && exec "$@"'`, a working directory, and `runToExit`), `support/ChildJvm`, moved `support/ClaimChildMain` to `config/sqlite/ClaimChildMain` (it needs the now package-private `create(Path)`), `SqliteStoreIntegrationTest` (import), `DataDirectoryGuardTest` (one expectation, see 6), `StartupPolicyIntegrationTest` (fixture: the directory is created 0700 as the guard now demands).
Docs: `README.md` (Run section: the application creates the directory; links refused; pass the real path; relative default), `docs/sdd/progress.md`, `docs/sdd/status.md` (own row), this file. Nothing else changed.

## 3. Design
- `SqliteDataSourceConfig.dataSource(Environment)` is the only `DataSource` bean (Boot's `DataSourceAutoConfiguration` backs off on a missing-bean condition). It reads `spring.datasource.url` (the property README, `AppProcess` and the E2E launcher already pass; unchanged), runs after `StartupPolicy` (environment post-processor, before any bean), takes `Path.of(...).toAbsolutePath().normalize()` (never `toRealPath()`), calls `DataDirectoryGuard.check` then `SqliteDataSourceProvider.create(Guarded)`, and re-applies `spring.datasource.hikari.connection-timeout` (5000, existing `MandatoryPropertiesIntegrationTest` still holds) at runtime. `InvalidPathException` (NUL) and a missing or foreign URL become `Refusal(LOCATION_NOT_ABSOLUTE)`.
- `create(Path)` and `create(String)` are package-private; `create(Guarded)` is the only public creator. A failure to open or claim (second instance: `SQLITE_BUSY`) or to apply the schema is now `Refusal(DATABASE_UNAVAILABLE)` with a fixed text and no cause on the guarded path. The unguarded overloads keep their old messages for the store tests.
- `StartupFailureAnalyzer` (registered in `spring.factories`) walks the cause chain; a `Refusal` or one of the three `StartupPolicy` reasons gives a fixed description plus one fixed action and a null cause, so Boot prints only that block.
- Guard follow-ups: SEC-F-182 `MULTIPLE_LINKS` (`unix:nlink` > 1 on the database file, skipped where the platform has no such attribute); SEC-F-184 a: `check` removes, newest first, only what that call created, then refuses with `CANNOT_CREATE`; SEC-F-184 b: the owner is still compared by name (the JDK has no portable way to read the running process uid; stated in `OwnerOnlyCheck` Javadoc); L-1: Javadoc no longer says a refusal "creates nothing" in general. The directory is named `tmp` in the Javadoc.

## 4. Criteria to code to tests (all expected values from tasks.md, spec US-009 and security.md)
| Criterion / trace | Code | Tests |
|---|---|---|
| 1 wiring, SC-32, US-009.3, SEC-F-185 item 5 | `SqliteDataSourceConfig`, `create(Guarded)` | `SqliteWiringIntegrationTest.theOnlyDataSourceIsTheGuardedPoolAndHibernateUsesIt` (one DataSource bean from the config class, same instance in Hibernate's factory bean), `.noDataSourceIsBuiltAnyOtherWayInProductionCode` (one public `create`, it takes `Guarded`; `new HikariDataSource`/`new SQLiteDataSource`/`DriverManager`/`DataSourceBuilder` only in the provider; the provider is called only from the config, as `create(DataDirectoryGuard.check(`), existing `MandatoryPropertiesIntegrationTest` |
| 1 pragmas, SC-32 | provider | `SqliteWiringIntegrationTest.pragmasReadBackFromTheRunningDataSource`: see 5 |
| 2 modes, SC-13, US-009.6, SEC-F-181 | guard, `NativeDirectoryModes` | `JarDataLocationIntegrationTest.createsThePrivateLocationUnderUmask022` and `...Umask000` (jar, empty location), `SqliteWiringIntegrationTest.theLocationWasCreatedPrivateAndAJournalOfAnOpenWriteIsNoWiderThan0600` (journal held open) |
| 2 default relative `data/` (constraint 2) | config | `JarDataLocationIntegrationTest.theShippedDefaultRelativeLocationWorksWhenStartedFromTheWorkingDirectory` (no URL argument, jar started in a temporary working directory) |
| 3 refusals, SC-13, SC-39, SEC-F-098, SEC-F-140, SEC-F-182, SEC-F-184 | guard, `OwnerOnlyCheck` | `StartupRefusalIntegrationTest`: 0755 directory, 0644 file, symlinked directory, symlinked file, two hard links, file of another uid, directory of another uid (both with root chown, uid 12345), a file where the directory should be, a file where the parent should be, a missing parent, a 300-byte name; unit: `DataDirectoryGuardCleanupTest`, `SqliteDataSourceConfigTest.nulByteInTheLocationBecomesAFixedRefusal`, `.pathThroughALinkIsRefused` |
| 4 reporting, SC-11, SC-37 | `StartupFailureAnalyzer` | every `StartupRefusalIntegrationTest` case asserts "APPLICATION FAILED TO START", the fixed text, and via `support/ConsoleRules` no temp path, no `library.db`, no `Caused by`, no `.java:`, no stack-frame pattern, no book marker; `SqliteDataSourceConfigTest` analyzer tests; `refusesAUrlWithParametersThroughTheSameAnalyzer` |
| 5 second instance, US-009.7, C-12, SEC-F-178 | provider claim | `StartupRefusalIntegrationTest.secondInstanceExitsNonZeroAndTheFirstKeepsServing` |
Traces DEC-028 and DEC-032 are covered by the same claim and pragma tests; US-009.3 by wiring plus pragma tests.

## 5. Evidence
Pragmas read back from the application's own `DataSource` (in process with the real configuration, because reading connection pragmas from the jar needs a test-only switch, A-09): `foreign_keys=1`, `trusted_schema=0`, `busy_timeout=5000`, `journal_mode=delete`, `locking_mode=exclusive`, `synchronous=2`, `SELECT load_extension(...)` raises `SQLException`, maximum pool size 1, minimum idle 1, connection timeout 5000. **Limit:** the same values are not read from the running jar; the jar proves wiring only through its behaviour (modes, second instance claim, refusals). I did not add a switch.

Modes on the jar (empty location, `sh -c "umask N && exec java -jar ..."`): directory `rwx------`, `tmp` `rwx------`, `library.db` `rw-------` under umask 022 and 000, asserted as soon as the application answers; the guard sets the modes before the first connection (unit proof of 34a-1, order in `check`), and a jar cannot observe the moment before the first connection from outside. During 60 adds a watcher thread polling the directory saw, under both umasks: `{library.db-journal=rw-------, library.db=rw-------}`, so the `-journal` file was observed (SEC-F-181) at 0600; after a normal stop every regular file in `data` is no wider than 0600. The held-open-write in-process test also observes the journal at `rw-------`.

Refusals (jar, umask 022, exit code 1 each, "tree before == after: true" from `TreeSnapshot` hashes/modes/owners/link targets): accessible to group or others (0755 directory; 0644 file), parent missing or not a directory (missing parent; file as parent), symbolic link (directory; file), another user (file; directory), not of the expected type (file where the directory should be), more than one hard link, could not be created (300-byte name: nothing left, the `data` directory the guard made is removed again), URL parameters (`Start refused: the datasource URL must not carry parameters or a fragment.`). Console excerpt (one line of the block, the same shape for each):
```
ERROR [main] o.s.b.d.LoggingFailureAnalysisReporter : __***************************_APPLICATION FAILED TO START_***************************__Description:__The database file has more than one hard link.__Action:__Fix the data location or the settings named by the reason, then start again._
```
Spring's own WARN line "Exception encountered during context initialization ... BeanCreationException: Error creating bean with name 'entityManagerFactory' ..." also appears; it carries the fixed refusal text and class and bean names, no path and no stack frames. Real stack traces do not appear.

Second instance: the first instance (umask 022) answered an add (201); the second ran against the same file, ended with exit 1 after the busy timeout (about 6 s) with `The database could not be opened or is in use by another process.`; the first still answered with the identical list body and the tree snapshot (names, modes, hashes) was unchanged.

Another owner: the run is root, so `chown` to uid 12345 worked and both owner cases ran and passed. If the suite runs as a normal user these two tests are skipped by `assumeTrue` and the unit proof of 34a-1 stands.

Build: `cd backend && ./mvnw clean verify` green: unit 949 run, 0 failures; integration 289 run, 0 failures, 1 skipped (`SqliteStoreIntegrationTest.failsWithFixedMessageWhenParentIsNotWritable`, "running as a user that ignores permissions", also skipped before this task); "All coverage checks have been met"; line coverage about 96%; 6 min. Elapsed of the new tests: `StartupRefusalIntegrationTest` 43.7 s (13 tests, one jar start each), `JarDataLocationIntegrationTest` 19.3 s, `SqliteWiringIntegrationTest` 1.2 s, unit classes under 0.2 s. `DurabilityIntegrationTest` (Task 33) takes 169 s and now runs on the wired jar. Smoke E2E (`PATH=/opt/node24/bin npx playwright test --grep @smoke`): 3 passed in 20.9 s, so the launcher (temporary 0700 directory, `--spring.datasource.url=jdbc:sqlite:<dir>/library.db`) works with the wired source. Leftover processes after the suite: none (`pgrep -f globerbooks.jar` empty).

## 6. Deviations, decisions and findings for the Liaison
1. **Extracted native files (new finding, decided by me, please confirm).** With the guard wired, the sqlite-jdbc driver extracts its library and `.lck` file into `<data>/tmp` with the process umask (observed `rwxr--r--` and `rw-r--r--`), which made the Task 33 `DurabilityIntegrationTest.assertPrivate` fail. The directory is 0700, so no one else can reach them, but to keep the rule "every file under the data directory is no wider than 0600" I added `NativeDirectoryModes.narrow`, called after the pool opens (the library is loaded by then), which sets 0600 on regular files in `tmp`, ignoring failures. Asserted on the jar in `JarDataLocationIntegrationTest`. Task 33's test needed no change. A kill between extraction and narrowing leaves a 0644 file in a 0700 directory until the next start.
2. `DataDirectoryGuardTest.refusesWhenTheFileCannotBeCreated...` expected `data` to remain after the refusal; with SEC-F-184 a it is removed, so the expectation is now "root is empty" (new behaviour, spec text for 34a-1 said nothing about leftovers). `StartupPolicyIntegrationTest.startsWithAnotherDatabasePath` created its directory with the umask default 0755; it now uses 0700 because the guard refuses wider directories (fixture, not an oracle change).
3. SEC-F-184 b is not done by numeric uid (no portable JDK way to read the process uid); the name check stays (Javadoc says so).
4. `ClaimChildMain` moved to the `config.sqlite` test package to keep `create(Path)` package-private instead of widening it.
5. The README sentence "from any other directory the start fails with 'unable to open database file'" is replaced: now `data/` is created in the working directory when its parent exists.
6. A start refusal leaves exit code 1 and the Boot banner and INFO lines before the failure block; nothing in them holds a path of the database.

## 7. Risks and follow-ups (not implemented)
- Pragmas are proved in process, not on the jar (no non-switch way); Security may accept this or ask for a plan-approved read path.
- The `Description` ends with a full stop added by the analyzer; the guard `Reason` texts have none.
- R-25 (check-to-open race) unchanged and accepted by the Sponsor.
- The second-instance test costs about 6 s because of the 5 s busy timeout.

## 8. Self-audit
Files at or under 300 lines (largest: guard 283); Javadoc on all public types and methods; no `Thread.sleep` (polling with deadlines; the watcher spins until stopped); no new dependency; no test-only route or switch in `src/main` (source scan test stays green); no `pom.xml` or `backend/config/**` change; no session link or trailer in any commit.

## 9. `git diff --stat origin/main..HEAD` (taken before the final amend of this file, which only changes its own line count)
```
 README.md                                          |  10 +-
 .../config/sqlite/DataDirectoryGuard.java          |  55 +++---
 .../config/sqlite/NativeDirectoryModes.java        |  41 +++++
 .../globerbooks/config/sqlite/OwnerOnlyCheck.java  |  65 +++++++
 .../config/sqlite/SqliteDataSourceConfig.java      |  56 ++++++
 .../config/sqlite/SqliteDataSourceProvider.java    |  29 +++-
 .../config/sqlite/StartupFailureAnalyzer.java      |  36 ++++
 .../src/main/resources/META-INF/spring.factories   |   1 +
 .../{support => config/sqlite}/ClaimChildMain.java |   3 +-
 .../sqlite/DataDirectoryGuardCleanupTest.java      |  55 ++++++
 .../config/sqlite/DataDirectoryGuardTest.java      |   2 +-
 .../sqlite/JarDataLocationIntegrationTest.java     | 112 ++++++++++++
 .../config/sqlite/SqliteDataSourceConfigTest.java  |  79 +++++++++
 .../config/sqlite/SqliteStoreIntegrationTest.java  |   1 -
 .../config/sqlite/SqliteWiringIntegrationTest.java | 140 +++++++++++++++
 .../sqlite/StartupPolicyIntegrationTest.java       |   4 +-
 .../sqlite/StartupRefusalIntegrationTest.java      | 192 +++++++++++++++++++++
 .../globant/globerbooks/support/AppProcess.java    |  81 +++++++--
 .../com/globant/globerbooks/support/ChildJvm.java  |   5 +-
 .../globant/globerbooks/support/ConsoleRules.java  |  29 ++++
 .../152-orchestrator-to-java-task-34a2-brief.md    |  26 +++
 .../153-java-to-liaison-task-34a2-delivery.md      |  75 ++++++++
 docs/sdd/progress.md                               |   5 +
 docs/sdd/status.md                                 |   4 +-
 24 files changed, 1041 insertions(+), 65 deletions(-)
```

## 10. `git log origin/main..HEAD --format='%an <%ae>%n%B---'` (hash added for reading; the final amend changes the hash of the last commit)
```
8658a47 krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 34a-2 delivery, status and progress [Task-34a-2]
---
ae2aaf2 krlosferra <carlos.ferra@globant.com>
test(storage): native files are asserted on the jar only [Task-34a-2]
---
d35648b krlosferra <carlos.ferra@globant.com>
feat(storage): narrow the extracted native files and adapt fixtures to the guard [Task-34a-2]
---
93dedb7 krlosferra <carlos.ferra@globant.com>
docs(readme): data directory is created by the application, links refused [Task-34a-2]
---
168c902 krlosferra <carlos.ferra@globant.com>
feat(storage): wire the guarded data source and the start failure analyzer [Task-34a-2]
---
bdaba49 krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 34a-2 brief, Task 34a-1 done [Task-34a-2]
---
```
