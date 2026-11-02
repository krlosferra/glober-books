# Handoff 047: Task delivery, Task 03a SQLite data source, claim and clock
- From: java
- To: liaison
- Phase: 4   Round: 1
- Artifacts: handoff 045 (brief), branch `feature/task-03a-sqlite-data-source`
- Type: TASK_DELIVERY
- Mode: E

(Numbered 047: 046 is the Task 11 brief. `status.md` is owned by the orchestrator and was not edited.)

## 1. Files
Created: `backend/src/main/java/com/globant/globerbooks/config/sqlite/SqliteDataSourceProvider.java`, `backend/src/main/java/com/globant/globerbooks/config/ClockConfig.java`, `backend/src/main/resources/schema.sql` (AP-6 verbatim, extracted by script from `plan-appendix.md`), `backend/src/test/java/com/globant/globerbooks/config/sqlite/SqliteStoreIntegrationTest.java`, `backend/src/test/java/com/globant/globerbooks/support/ClaimChildMain.java`, plus three test helpers split out of the test class because of the 300-line Checkstyle limit: `support/ChildJvm.java`, `support/FileFacts.java`, `support/PoolFacts.java`. Modified: `docs/sdd/progress.md`, this handoff. Nothing else (no pom, no `backend/config/**`, no `application.yml`, no Spring `DataSource` bean wiring: the provider is a static factory, 03b/04 wire it).

Resolved: sqlite-jdbc 3.53.4.0, HikariCP 7.0.2. Real `SQLiteConfig` keys: `setSynchronous(FULL)`, `setLockingMode(EXCLUSIVE)`, `setTransactionMode(IMMEDIATE)`, `setBusyTimeout(5000)`, `enforceForeignKeys(true)`, `enableLoadExtension(false)`, `resetOpenMode(CREATE)`. URL stays `jdbc:sqlite:<file>`. Then, in the wrapped data source, per physical connection and in this order: `PRAGMA trusted_schema = OFF`, `BEGIN EXCLUSIVE`, `COMMIT` (claim), read of `PRAGMA journal_mode` (must be `delete`, else the connection is closed and the start fails).

## 2. Acceptance criteria
1. Modes: `SqliteDataSourceProvider.prepareStorage/createDirectory` (explicit `setPosixFilePermissions`, only on what it creates; existing entries untouched). Tests `createsMissingDirectoryAndFileAndNativeDirectoryWithExactModes`, `keepsEveryFileNextToTheDatabaseNotWiderThan0600AfterAWrite`, `doesNotChangeModesOfAnExistingDirectoryAndFile`. After the claim the provider only runs the pragmas and `schema.sql`; it never opens the file (no `Files.read*`).
2. Settings and pool: `sqliteConfig()`, `ClaimingDataSource.initialize`, `poolConfig()`. Tests `readsBackEverySettingOnTheLiveConnectionAndAfterEviction` (compares the raw `SQLiteConnection` identity before and after `softEvictConnections`: a new physical connection appears, so the init re-ran), `failsToLoadAnExtension` (statement `SELECT load_extension('nonexistent_extension')` throws `SQLException`), `refusesToStartOnAFileThatIsNotInRollbackJournalMode`.
3. Claim: tests `secondJvmFailsWithDatabaseIsLockedWhileChildHoldsAndHashIsUnchanged` (separate JVM through `ProcessBuilder`, no shell, readiness line `READY` with a 60 s timeout, `finally` destroys the process, negative control), `secondJvmStillFailsWhileTheReplacementConnectionAfterEvictionHoldsTheClaim`, `failsStartWithFixedMessageWhenAnotherProcessHoldsTheFile`. The claim works as planned: no fallback needed.
4. tmp dir: `prepareStorage` creates `<data dir>/tmp` (0700) and sets `org.sqlite.tmpdir` before the pool (driver) loads; the holder-JVM test lists `tmp` and finds the extracted `*sqlitejdbc*` library there (a fresh JVM, so the load really happened from it). X-10: the driver loads from it, nothing dropped, no `security.md` handoff needed.
5. Schema and clock: `applySchema` (Spring `ResourceDatabasePopulator`, classpath `schema.sql`, every start); tests `appliesSchemaAtEveryStartWithoutChangingTheStoredTable`, `providesAClockInUtc`, `mainSourcesCallTheWallClockOnlyInClockConfig`. Unhappy paths: `failsWithFixedMessageNamingOnlyTheFileWhenParentIsNotADirectory` (message `The storage of books.db could not be prepared`, no cause, no path), `failsWithFixedMessageWhenParentIsNotWritable` (skipped when running as root; this sandbox is root, so it is the 1 Failsafe skip), plus URL tests.

## 3. Test specification (from the spec)
Modes exact (0700/0600/0700); sidecars <= 0600; six pragmas equal the plan table before and after eviction; pool 1/1/0/0/0; extension load fails; WAL file refused; child holds -> second JVM exit 2 with "database is locked"; hash equal after both stop; holder stopped -> second JVM exit 0; claimed file at start -> fixed failure; bad parent -> fixed failure; two starts -> same stored DDL; clock UTC; one wall-clock call. Inventory: Surefire 7 (earlier tasks), Failsafe 22 (16 new in `SqliteStoreIntegrationTest`, 6 earlier).

## 4. Evidence
**E1** `./mvnw clean verify`:
```
[INFO] You have 0 Checkstyle violations.            (result file lists 11 files: 3 main + 8 test; Task 02 had 4)
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0          (Surefire)
[WARNING] Tests run: 16, Failures: 0, Errors: 0, Skipped: 1 ... SqliteStoreIntegrationTest
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 1          (Failsafe)
[INFO] BugInstance size is 0      (spotbugsXml: total_classes='4' total_bugs='0'; 5 class files incl. nested ClaimingDataSource)
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```
SpotBugs first flagged `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` (`getParent()`) and `SQL_INJECTION_JDBC` (script string executed by a `Statement`); both fixed by design (`Objects.requireNonNull`; `ResourceDatabasePopulator`). No suppression, no `exclude.xml` entry, no `@SuppressFBWarnings`.

**E2** JaCoCo csv (`target/site/jacoco/jacoco.csv`, LINE_MISSED,LINE_COVERED): `SqliteDataSourceProvider` 4/52, `SqliteDataSourceProvider.ClaimingDataSource` 2/21. Project: 6 missed, 73 covered = 92.4%, merged check met. (Config classes and the application class are excluded.)

**E3** Test output (`SETTINGS_BEFORE` and `SETTINGS_AFTER`, identical):
`{journal_mode=delete, synchronous=2, locking_mode=exclusive, foreign_keys=1, busy_timeout=5000, trusted_schema=0}` and `POOL max=1 minIdle=1 idle=0 keepalive=0 maxLifetime=0`. (`synchronous=2` is FULL.)

**E4** Failsafe output (the test prints `JAVA_TOOL_OPTIONS` noise from the environment first):
```
FAILED The database books.db could not be opened and claimed | Failed to initialize pool: [SQLITE_BUSY] The database file is locked (database is locked) | [SQLITE_BUSY] The database file is locked (database is locked) |   exit=2
SHA256 before=392ad2986081a60cec0060b14e1a826da3b5b33e89df1755f3f3d744d80c3e22 after=392ad2986081a60cec0060b14e1a826da3b5b33e89df1755f3f3d744d80c3e22
NEGATIVE_CONTROL exit=0   (CLAIMED)
SECOND_JVM_AFTER_EVICTION exit=2   (same message)
```
Each refused attempt takes the 5 s `busy_timeout`, so the class runs about 24 s.

**E5** Exact-mode assertions pass: directory `rwx------`, file `rw-------`, `tmp` `rwx------`. Sidecar that appeared: only `books.db-journal` (`rw-------`), seen mid-write and again after commit in the output `SIDECARS_SEEN=[books.db-journal:rw-------, books.db-journal:rw-------]`; no `-wal`, `-shm`.

**E6** grep: one match, `src/main/java/com/globant/globerbooks/config/ClockConfig.java:21: return Clock.systemUTC();`

**E7** grep: matches only in `config/sqlite/SqliteDataSourceProvider.java` (Javadoc and the four SQL constants).

**E8** `git diff main..HEAD -- pom.xml config/` is empty; `dependency:tree` equals `config/dependency-tree.txt` (diff empty, "TREE_SAME").

**E9** and **E10**: see the commands at the end of this file (run after the commits).

## 5. Section 5 outcome (plan assumptions N-2, N-3, AP-12.5)
No behavior differed from the plan: claim, per-connection settings, eviction re-running the init, and tmpdir all proven with real output above. One difference of method: sqlite-jdbc has no `SQLiteConfig` key for `trusted_schema`, so it is set with `PRAGMA trusted_schema = OFF` in the wrapped data source, before the claim. That is the plan's "same effect as connection-init-sql" done in code; it is not a dropped setting, but Security/Liaison may want to confirm.

## 6. Deviations, proposals and follow-ups
- Deviation: no Hikari `connection-timeout` set here (D5 puts it in `application.yml`, 03b); no Spring `DataSource` bean (allowed, not needed). Three extra test helper files exist only for the 300-line limit.
- ClockConfig suppression (043 item 10): left file-scoped as is. Reason: tightening needs a change to `backend/config/**` and a seeded run; the grep E6 and the test `mainSourcesCallTheWallClockOnlyInClockConfig` already guard it. Proposal if wanted: change the glob to `[\\/]config[\\/]ClockConfig\.java$`.
- Time rule in tests (043 item 2): proposal to the PM. No test needs a time rule yet; tests use no clock at all (`MutableClock` not needed yet). The test exemption can stay until `BookService` tests need a `MutableClock`. Static-import `now()` (bare call) is not caught by the regex; I avoided it.
- Unwritable-parent test skips as root; run as non-root in CI to exercise it. "Narrower pre-existing mode" is not tested (a 0400 file is environment dependent as root); only equal modes are asserted unchanged.
- Test output carries the sandbox `JAVA_TOOL_OPTIONS` banner; harmless.
