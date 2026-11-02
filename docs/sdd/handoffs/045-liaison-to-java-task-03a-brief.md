# Handoff 045: Task brief, Task 03a SQLite data source, claim and clock
- From: liaison
- To: java (`sdd-java-architect`)
- Phase: 4   Round: 1
- Artifacts: `docs/sdd/tasks.md` v0.5.1 (Task 03a, Task 03b), `docs/sdd/plan.md` sections 2 and 7, `docs/sdd/plan-appendix.md` (AP-1, AP-6, AP-12), `docs/sdd/constitution.md` 1.2 Persistence, `docs/sdd/security.md` (SC-13, SC-32, SEC-F-069), handoffs 043 and 044
- Type: TASK_BRIEF
- Mode: E

Branch `feature/task-03a-sqlite-data-source` is checked out. Tasks 01 and 02 are merged. Do ONLY this task, then STOP and wait for approval.

## 1. Read first (and nothing else is validated context)
1. `docs/sdd/constitution.md`, Persistence Standards and Security Standards (mandatory properties, NEVER list).
2. `docs/sdd/plan.md` section 2 "SQLite settings and start order", and section 7 (X-10, N-2, N-3).
3. `docs/sdd/plan-appendix.md` AP-1 (rows `config.sqlite`, `ClockConfig`, test support), AP-6 (schema), AP-12.2 and AP-12.3.
4. `docs/sdd/security.md`: SC-13, SC-32 and SEC-F-069 (a, b, c).
Do not read ahead to later tasks.

## 2. Task text (verbatim, tasks.md v0.5.1)

### Task 03a: SQLite data source, claim and clock
- Source: B03 (first part) | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 01 | Branch: `feature/task-03a-sqlite-data-source`
- Description: Build the single-connection SQLite data source with the fixed settings (rollback journal never changed, `synchronous=FULL`, exclusive locking and the claim, immediate transactions), the creation of the directory (0700) and file (0600), the private native-library directory, `schema.sql` and the clock.
- Production: (1) `config/sqlite/SqliteDataSourceProvider.java`; (2) `config/ClockConfig.java`; (3) `schema.sql`. Tests: `SqliteStoreIntegrationTest`, `support/ClaimChildMain` (a small test main used as the second process).
- Acceptance criteria:
  1. A missing directory is created 0700 and a missing file 0600 before the first connection; after a write, every file the storage creates next to it is not wider (US-009.3, SC-13). After the claim the application only calls `stat`-type operations on the database path (a plain read would drop the lock, SEC-F-069 b); tests hash the file only when no instance holds it.
  2. Settings read back on the live connection equal the plan: default rollback journal (any other value stops the start), `synchronous` FULL, `locking_mode` EXCLUSIVE, `foreign_keys` on, `busy_timeout` 5000, `trusted_schema` off; loading an extension fails. Pool: size 1, `minimum-idle` 1, `idle-timeout` 0, `keepalive-time` 0, `max-lifetime` 0; the claim and the settings run at every connection initialization, and a test that forces the eviction of the connection reads them back (SC-32, N-3, SEC-F-069 c).
  3. The claim (`BEGIN EXCLUSIVE`, `COMMIT`, no change) makes a second process fail with "database is locked": the second process is a separate JVM (`support/ClaimChildMain`), not a second connection in the same JVM; the file hash is unchanged after both have stopped (US-009.7, N-2, SEC-F-069 a). If the claim cannot do this, the fallback (a no-op write transaction) is decided in this task with the PM, before Increment 2.
  4. `org.sqlite.tmpdir` is `<data dir>/tmp` (0700); if the driver cannot load from it the property is dropped and the risk goes to `security.md` through a handoff (X-10).
  5. `schema.sql` runs at every start; time comes only from the injected clock.
- Traces: US-009.3, US-009.7, M-3, SC-13, SC-32.

## 3. Scope and non-scope
In scope: the files named in "Production" and "Tests" above. `schema.sql` is the AP-6 statement exactly (Sponsor-approved; any change is ASK FIRST). It sits on the classpath (`backend/src/main/resources/schema.sql`) and runs at every start through the provider (not through `spring.sql.init`, which belongs to configuration in 03b). `ClockConfig` exposes a single `Clock` bean. A minimal wiring so the provider is a `DataSource` bean is allowed; keep it inside `config.sqlite`.

Out of scope (Task 03b or later; do not add, not even "temporarily"):
- `StartupPolicy` and any check of bind address or URL form (03b). The provider takes the file path from the existing `spring.datasource.url` value as given; if you need a default for the tests, pass the path to the provider directly from the test.
- `application.yml` and every property in it: Tomcat limits, `max-swallow-size`, Hikari `connection-timeout`, `ddl-auto`. Hikari values of this task are set in code in the provider/wiring, because `application.yml` is 03b's. If this forces a duplicate later, record it as a note in the delivery; do not create `application.yml`.
- `DataDirectoryGuard` (symlink, owner, group/other-bit refusal), header check, `StoredLibraryValidator`, `query_only` (Tasks 34a, 34b).
- Entities, repository, Hibernate `validate` (Task 04).

Files you may touch: the three production files, the two test files, `backend/config/**` only if a gate needs a justified entry (see section 6), and `docs/sdd/progress.md`. Anything else: STOP and ask.

## 4. Implementation constraints (from the plan; do not reinterpret)
- Settings table is plan section 2: rollback journal (never set `journal_mode`; any other value read back stops the start), `synchronous=FULL` explicit, `locking_mode=EXCLUSIVE`, `transaction_mode=IMMEDIATE`, `busy_timeout=5000`, `foreign_keys=ON`, `trusted_schema=OFF`, read-write without create (the file is pre-created 0600 by this task), extension loading off. Settings are NOT in the URL (the URL stays `jdbc:sqlite:<file>`); they are applied by the data source that Hikari wraps, for every physical connection, before any query on it, and the claim (`BEGIN EXCLUSIVE`, `COMMIT`) runs last in that initialization.
- Pool: `maximum-pool-size` 1, `minimum-idle` 1, `idle-timeout` 0, `keepalive-time` 0, `max-lifetime` 0.
- Native SQL (`PRAGMA`, `BEGIN EXCLUSIVE`, `COMMIT`) lives only in `config.sqlite` (AP-12.3). No `select *` anywhere (Checkstyle ban).
- After the claim: no stream, channel, `Files.read*`, hash or copy of the database path inside the application; only `exists`, `size`, attributes (SEC-F-069 b). Test hashing happens only when no instance holds the file (after the child JVM and the pool have stopped).
- Directory 0700 and file 0600 created before the first connection (POSIX permissions applied explicitly, not left to umask); `<data dir>/tmp` 0700 for `org.sqlite.tmpdir`, set before the driver loads. After a write, check the mode of the directory, the file and every sidecar present (for example `-journal`) is not wider than 0600. Sidecar presence is not guaranteed in rollback mode: assert on whatever exists, and say in the delivery which ones appeared.
- Time: the only wall-clock call in `src/main` is one `Clock.systemUTC()` in `ClockConfig`. Everything else uses the injected `Clock`.
- No new dependency and no new plugin (ASK FIRST). `sqlite-jdbc`, Hikari (through Boot), JPA and the dialect are already in the pom.
- Javadoc on public types and methods; no secrets; no logging of the path content beyond what is already fixed by the constitution; no `System.out`, no `printStackTrace`.

## 5. If a sqlite-jdbc key or pragma behaves differently from the plan
Plan items N-2 and N-3 came from knowledge, not from proof. Therefore:
1. Read back with pragmas on the live connection (`PRAGMA locking_mode`, `synchronous`, `foreign_keys`, `busy_timeout`, `trusted_schema`, `journal_mode`) and record the real output.
2. If any `SQLiteConfig` key, pragma, `transaction_mode`, extension-loading switch or the claim behaves otherwise (wrong value read back, ignored key, claim does not produce "database is locked" in the second JVM, eviction does not re-run initialization, native-library directory not loadable): STOP and report in the delivery with the command output. Do not choose a workaround on your own.
3. Fallbacks are decided, not applied: the no-op write transaction for the claim is decided in this task with the PM (tasks.md AC3); any other fallback (a different journal mode, a URL parameter, a `connection-init-sql` property, a new library, dropping `trusted_schema`) is ASK FIRST and needs Sponsor approval. The only fallback the plan pre-authorizes is dropping `org.sqlite.tmpdir` if the driver cannot load from it, with a handoff to Security for `security.md` (X-10).
4. A wrong key name is corrected in the task with evidence (AP-12.5), but the effective value must still equal the plan table.

## 6. Carried follow-ups from handoffs 043 and 044
1. `ClockConfig` suppression (043 item 10). Show with a grep that `ClockConfig.java` contains exactly one `Clock.systemUTC()` and no other wall-clock form. The suppression is file-scoped, and JaCoCo excludes `**/*Config.class`, so nothing else guards this. Consider tightening the suppression glob to the `config` package path; if you tighten it, show a seeded run in a different file named `ClockConfig.java` outside that package still failing. If you leave it as is, say why.
2. Time rule in tests (043 item 2). Now that the injected `Clock` exists, state in the delivery whether tests need a time rule without the test exemption (for example a `MutableClock` is not needed yet). This is a proposal to the PM; do not change `suppressions.xml` for the TIME-RULE test exemption without PM approval. Static-import `now()` (bare call) is not caught by the regex (044 note): avoid it and mention it.
3. Gate vacuity (043). This is the first code that Checkstyle, SpotBugs and JaCoCo really see. Show: (a) Checkstyle analysed file count greater than the 4 of Task 02; (b) SpotBugs "Total bugs: 0" with more than 1 analysed class (not the vacuous one-class run); (c) the JaCoCo report line for `SqliteDataSourceProvider` (the first non-excluded class) with a real covered/missed line ratio and the merged check `met`. If a gate flags your code, fix the code. A suppression or an `exclude.xml` entry needs a written justification, the PM's and Security's approval, and is listed as a deviation. `@SuppressFBWarnings` is not allowed (044 SEC-F-092 b).
4. Expect FSB patterns on the provider (path handling, `PATH_TRAVERSAL_IN`, `SQL_INJECTION_JDBC` for `Statement` with constants, predictable file permissions). Fix by design (constants, `Path` validation) before asking for an exclusion.

## 7. Test requirements (Mode G applies; the oracle is the spec, not the current behavior)
`SqliteStoreIntegrationTest` (Failsafe) and `ClaimChildMain` (test source, `main`, no JUnit):
- AC1: missing directory and file created with 0700 and 0600; sidecar modes after a write; pre-existing narrower or equal modes are not widened. Assert exact modes, not "not world-readable".
- AC2: each setting read back on the live connection equals the table; `journal_mode` reads back the default (`delete`); `PRAGMA` of the pool size via the pool bean (max 1, min-idle 1, timeouts 0); loading an extension fails (state which statement/API you used); then `softEvictConnections`, obtain the new connection, and read every setting back again, plus the second-process attempt (AC3) while the new connection holds the claim. If eviction does not produce a new physical connection, say so and prove it by another means (for example a different connection identity or a counter in the initialization).
- AC3: the child JVM (`ProcessBuilder` with the current `java`, built classpath, no shell string) holds the claim and signals readiness; a second JVM attempt fails with a message containing "database is locked" (assert on the exit code and the text); the file SHA-256 before and after both have stopped is equal. A same-JVM two-connection test is not proof and does not count. Include the negative control: with the holder stopped, the second process succeeds.
- AC4: the driver loads from `<data dir>/tmp` (0700) or the delivery follows section 5.
- AC5: `schema.sql` runs at every start (start twice on the same file: second run is a no-op and the stored table is unchanged); `ClockConfig` provides a `Clock` in UTC; no wall-clock call in the new main sources.
- Unhappy paths: unwritable or non-directory parent (fixed failure, no stack trace or path in the message beyond the file name), and a file already claimed by another process at start.
- Test names and AAA per the constitution; no `Thread.sleep` as synchronization (use readiness signals with a timeout); every child process is destroyed in `finally`.

## 8. Evidence table (real output, not claims)
Fill in the delivery. Paste the command and the tail of its output; a statement without output is not accepted.

| # | Evidence | Command (from `/home/user/glober-books/backend`) | Required content |
|---|---|---|---|
| E1 | Full build | `./mvnw clean verify` | `BUILD SUCCESS`, Surefire and Failsafe counts, Checkstyle file count, SpotBugs `Total bugs: 0` with the analysed class count, JaCoCo `All coverage checks have been met` |
| E2 | Coverage of the first real class | JaCoCo report (csv or html) of `SqliteDataSourceProvider` | covered and missed lines, ratio, merged line ratio of the project |
| E3 | Settings read-back (before and after eviction) | test log or a recorded test output | each pragma name and value, pool values |
| E4 | Claim from a second JVM | test output | child holds, second fails with "database is locked", negative control passes, SHA-256 equal before/after |
| E5 | Modes | `ls -la` or test assertion output for directory, file, `tmp`, sidecars | 0700, 0600, 0700 |
| E6 | Clock grep | `grep -rnE 'Clock\.systemUTC\|Instant\.now\|LocalDate(Time)?\.now\|new Date\(' src/main` | exactly one match, in `ClockConfig.java` |
| E7 | Native SQL inventory | `grep -rnE 'PRAGMA\|BEGIN EXCLUSIVE\|createNativeQuery\|nativeQuery' src/main` | matches only under `config/sqlite` |
| E8 | No new dependency | `git diff main..HEAD -- pom.xml config/` and `diff` of `config/dependency-tree.txt` against `./mvnw dependency:tree` | empty (or each change listed and approved) |
| E9 | Files touched | `git diff --stat main..HEAD` | only the files in section 3 plus `progress.md` |
| E10 | Hygiene | `git log main..HEAD --format='%an <%ae>%n%B---'` | human identity, format below, no AI attribution |

Also report: sqlite-jdbc version actually resolved, the real `SQLiteConfig` keys used, and anything that differed from the plan (section 5).

## 9. Git rules (`docs/sdd/git-workflow.md`)
- Stay on `feature/task-03a-sqlite-data-source`. Atomic commits, for example: (1) `schema.sql` and `ClockConfig`; (2) provider; (3) tests and `ClaimChildMain`; (4) `progress.md`.
- Message format: `type(scope): subject [Task-3a]`, for example `feat(sqlite): add single-connection data source with claim [Task-3a]`. Human identity from `git config`. 
- No AI attribution of any kind: no `Co-Authored-By`, no tool, model or vendor name, no "Generated with", no session or chat link, in messages, comments, test data, files or branch. Any automatic attribution line suggested by a tool is not added.
- Never `--no-verify`; keep `core.hooksPath .githooks`. No destructive git commands. No `git push`, no pull request: the orchestrator opens it after Sponsor approval.
- No `target/`, database files, seeds or temporary experiment files committed.

## 10. progress.md
Append one entry in the existing format for Task 03a: date, branch, what was built, the evidence summary (E1 to E10 results, not the full logs), deviations, open questions (sqlite-jdbc behavior, X-10 outcome, the clock-suppression decision), and the line `[Task-3a] SQLite data source, claim and clock - submitted for audit`. Do not mark it accepted; that follows the Liaison, Security and Sponsor verdicts. Do not edit `status.md`, `tasks.md`, `plan.md`, `security.md` or `constitution.md`: propose changes in the delivery or by handoff.

## 11. Deliverables
A `DELIVERY` handoff `docs/sdd/handoffs/046-java-to-liaison-task-03a-delivery.md` (or the next free number) with: the file list; one explanation per acceptance criterion (1 to 5) pointing to code and test; the evidence table; deviations and proposals (section 6 items 1 and 2); the outcome of section 5 if triggered. Then STOP and wait for the Liaison audit, the Security verdict and Sponsor approval. Do not start Task 03b or Task 04.
