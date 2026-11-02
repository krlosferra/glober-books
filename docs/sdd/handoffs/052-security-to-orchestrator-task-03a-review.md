# Handoff 052: Security review, Task 03a SQLite data source, claim and clock
- From: security
- To: orchestrator
- Phase: 4   Round: 1
- Artifacts: branch `feature/task-03a-sqlite-data-source` (commits main..HEAD), handoffs 045 and 047, `SqliteDataSourceProvider.java`, `ClockConfig.java`, `schema.sql`, `SqliteStoreIntegrationTest`, `ClaimChildMain` and helpers
- Type: AUDIT_REPORT
- Mode: E

## 1. Scope and method
Read: both handoffs, the three production files, the tests, `suppressions.xml`, `checkstyle.xml`, AP-6, SC-01/13/32, SEC-F-069. Ran (read-only, scratch copy of output only): `./mvnw -o clean verify` (clean exit; Failsafe `SqliteStoreIntegrationTest` 16 tests, 1 skipped, 0 failures; `BuildSplitProofIntegrationTest` 6; SpotBugs `total_classes=4 total_bugs=0`; JaCoCo `SqliteDataSourceProvider` 4 missed / 52 covered, nested class 2 / 21), a byte comparison of `schema.sql` against the AP-6 block of `plan-appendix.md` (equal), greps of `src/main`, and a probe program in the scratchpad (symlinked directory, symlinked file, directory-as-database, URL with query, extension load message). I did not run a seeded violation: the checks I needed (stat-only rule, suppression glob) are not covered by any gate, which is itself a finding (SEC-F-099, SEC-F-100). No git write, no source edit.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-098 | MEDIUM (conditional, not live in 03a) | Symlink / local actor | `SqliteDataSourceProvider.prepareStorage`, `createDirectory` | The provider follows symlinks for the data directory and the database file (`Files.isDirectory`, `Files.exists`). Nothing refuses them. Existing entries are accepted untouched (no mode check). | Probe: `link -> real` (0755) accepted, `tmp` created 0700 and `books.db` 0600 inside the 0755 target; `books.db -> target.db` accepted and opened. | Another local user (or a planted link) redirects the store to a location they control or read. Not reachable today because the provider is not wired into the application. | Out of scope for 03a by the brief (DataDirectoryGuard is Task 34a). Condition: Task 34a (or an equivalent refusal) must run before `create()` on every path and must be a precondition in the 03b/04 wiring brief; do not let the application start with the provider alone. Also test "narrower pre-existing mode not widened" (delivery admits only equal modes tested). | SC-13, SC-39 |
| SEC-F-099 | LOW | Verification gap | tests, `checkstyle.xml` | SEC-F-069 b (stat-only after the claim) is held only by code review: no test or rule fails if a later change adds `Files.read*`, a stream, hash or copy of the database path in `src/main`. Today the grep is clean (only `Files.exists` and pre-claim create/chmod in the provider; no `java.nio.file` use in any other main class). | `grep Files\.\|FileInputStream\|FileChannel src/main` outside `config/sqlite`: no hits. | A later task reads or hashes the file (for example the Task 34 validator) and silently drops the POSIX lock; a second instance then starts. | Add a source-scan test like `mainSourcesCallTheWallClockOnlyInClockConfig` that bans file-reading APIs in `src/main` except an allow-list (the stored-library validator, which must read before the claim or through the claimed connection). Proposal only; PM decides. | SEC-F-069 b, SC-32 |
| SEC-F-100 | LOW | Clock guard | `suppressions.xml:31`, `mainSourcesCallTheWallClockOnlyInClockConfig` | Suppression and test match the file name `ClockConfig.java` in any package under `src/main/java`. A second file with that name elsewhere could call `Clock.systemUTC()` unflagged; the test compares only `getFileName()`. Single `Clock.systemUTC()` itself VERIFIED (one match, `ClockConfig.java:21`). | Read of suppression regex `[\\/]src[\\/]main[\\/]java[\\/].*[\\/]ClockConfig\.java$`; test hit list uses file name. | Low realism (needs a review miss). | Anchor to `config[\\/]ClockConfig\.java$` and compare the relative path in the test (the delivery already proposes it). Also bare static-import `now()` remains uncaught (known, 044). | SC-27 |
| SEC-F-101 | LOW | Test strength | `failsToLoadAnExtension` | The test asserts only `SQLException`; `load_extension('nonexistent_extension')` fails also when loading is enabled (file not found). My probe shows the real cause is `(not authorized)`, so the control holds, but the test does not distinguish it. | Probe output: `[SQLITE_ERROR] SQL error or missing database (not authorized)`. | A regression that enables loading would still pass the test. | Assert the message contains `not authorized`. | SC-32 |
| SEC-F-102 | LOW | Permission window | `prepareStorage`, `createDirectory` | Directory and file are created with the umask mode and then `chmod`ed; for a pre-existing wider parent the empty 0644 file exists for an instant. No data is in it at that time. | Code read. | Negligible (empty file), but the brief says "applied explicitly, not left to umask". | Create with `PosixFilePermissions.asFileAttribute` (`Files.createDirectory(dir, attr)`, `Files.createFile(file, attr)`), keep the chmod as belt and braces. | SC-13 |
| SEC-F-103 | LOW | Error text | `create(String)`, `openPool`, `applySchema` | The wrapped `cause` is kept. Today the chain contains driver text without the path (probe: `[SQLITE_CANTOPEN] ... (unable to open database file)`), and the top message names only the file name; but for a URL with a query part the message shows `q.db?open_mode=1`, and a schema failure carries the SQL text in the cause. All is log or console only (startup), not an HTTP response. The URL form check is Task 03b. | Probe messages above. | Path or configuration text in startup logs only. | When 03b wires the failure into the start policy, make sure the failure is logged without cause detail beyond the fixed message, or accept as is. Add `?` and `#` to the URL-form rejection in 03b so the file name cannot smuggle driver parameters. | SC-11, SC-32 |
| SEC-F-104 | INFO | Pool/claim | `ClaimingDataSource.initialize` | The order is `trusted_schema=OFF`, `BEGIN EXCLUSIVE`, `COMMIT`, journal check; on any failure the connection is closed. A second instance can take the lock between an evicted connection's close and the replacement's claim (known, SEC-F-069 INFO). `org.sqlite.tmpdir` is a JVM-wide property set before the first driver use; if the driver was already loaded in the JVM the setting has no effect (tests mitigate with a fresh holder JVM). | Code read; delivery E3/E4; test output. | Availability only. | None. | SC-32 |

No CRITICAL or HIGH.

## 3. Controls checked
| Control | Status | Evidence |
|---|---|---|
| SC-01 (bind 127.0.0.1) | NOT_APPLICABLE to 03a | No server code or property in this task; StartupPolicy is 03b/17. Unchanged. |
| SC-13 (0700 / 0600) | VERIFIED for the provider (POSIX), with SEC-F-098 and SEC-F-102 | Exact-mode tests `createsMissingDirectory...WithExactModes` (0700/0600/0700) and sidecar test (`books.db-journal` rw-------) pass in my `verify`; my probe also showed 0700 `tmp`, 0600 file. Windows is a documented limit. Existing entries not tightened (by design, 34a). |
| SC-32 (SQLite hardening) | VERIFIED | Read-back `{journal_mode=delete, synchronous=2, locking_mode=exclusive, foreign_keys=1, busy_timeout=5000, trusted_schema=0}` before and after eviction (new physical connection proved by identity); WAL file refused; pool 1/1/0/0/0; extension load rejected `(not authorized)` (probe); `trusted_schema` set by `PRAGMA` per connection before the claim because `SQLiteConfig` has no key, equal to the plan's connection-init effect (accepted); extension loading `enableLoadExtension(false)`; `resetOpenMode(CREATE)` only. |
| SEC-F-069 a (child JVM proof) | VERIFIED | Separate JVM via `ProcessBuilder`, exit 2 with "database is locked", negative control exit 0, hash equal after both stop (E4, test code read: readiness line with timeout, `finally` destroy). |
| SEC-F-069 b (stat-only after claim) | VERIFIED by review, no automated guard | Provider: `Files.exists` (stat) before the claim, no read of the path after; schema comes from the classpath; grep of other main classes clean. Gap: SEC-F-099. |
| SEC-F-069 c (claim per connection, pool, eviction read-back) | VERIFIED | `ClaimingDataSource` overrides both `getConnection` forms; `poolConfig` sets max 1, min-idle 1, idle/keepalive/max-lifetime 0; eviction test and `secondJvmStillFails...AfterEviction`. |
| `org.sqlite.tmpdir` | VERIFIED | Set to `<data dir>/tmp` (0700) before the pool; delivery's holder-JVM test finds the extracted library there (test code read; not re-observed in my probe, which showed an empty `tmp`, the library being loaded earlier/elsewhere in a JVM already warmed). X-10 not triggered. |
| Native SQL confined and parameter-free | VERIFIED | `PRAGMA`/`BEGIN EXCLUSIVE`/`COMMIT` only in `config/sqlite`, four constant strings, no concatenation; SpotBugs 0 with no suppression. |
| Schema vs AP-6 | VERIFIED | Byte-identical to the AP-6 block (script comparison). STRICT, all CHECKs present; `CREATE TABLE IF NOT EXISTS` is idempotent (test). |
| Single `Clock.systemUTC()` | VERIFIED (SEC-F-100 LOW) | One match in `ClockConfig.java`; no other wall-clock form in `src/main` (test and grep). |
| Hygiene | VERIFIED | Commits by the human identity; no AI attribution in messages; no pom/config change in `git diff main --stat`. |

## 4. Not verified
- The extracted native library in `<data dir>/tmp` was not re-observed by me; I rely on the test code and the delivery output.
- The unwritable-parent test is skipped in this root sandbox (the 1 skip); not exercised by me or by the delivery. Needs a non-root CI run.
- Windows behavior, advisory databases for sqlite-jdbc 3.53.4.0 and HikariCP 7.0.2 (no advisory source queried in this task; dependencies are unchanged from Task 01/02).
- Provider is not wired into Spring, so no end-to-end start was tested.

## 5. Residual risks for the Sponsor
None new to accept. SEC-F-098 stays open as a design dependency on Task 34a.

## 6. Verdict: PASS WITH CONDITIONS
Conditions (no blocking finding):
1. SEC-F-098: record in the Task 03b/04 brief that the provider must not be the sole start path; `DataDirectoryGuard` (34a) runs before it. Add a test that a narrower pre-existing mode is not widened when feasible.
2. SEC-F-101: tighten the extension assertion to the `not authorized` message (one line) before merge.
3. SEC-F-100/099/102/103: LOW; may be fixed in 03a or scheduled (PM decision): anchored suppression, stat-only source-scan test, attribute-based creation, URL `?`/`#` rejection in 03b.

Next step: Java applies condition 2 (and optionally 3); Security re-verifies by reading the diff; then the Sponsor gate.

## Appendix: exact security.md text (to be applied by the owner; I did not edit the file)
Controls: SC-13 status `PARTIAL (Task 03a verified 2026-11-02, handoff 052: exact modes 0700/0600, tmp 0700, sidecar -journal 0600; symlink refusal pending Task 34a, SEC-F-098)`. SC-32 evidence: `Task 03a (052): read-back identical before and after forced eviction {journal_mode=delete, synchronous=2, locking_mode=exclusive, foreign_keys=1, busy_timeout=5000, trusted_schema=0}; pool 1/1/0/0/0; extension load refused (not authorized); trusted_schema by PRAGMA (no SQLiteConfig key); WAL refused; schema.sql byte-equal to AP-6`. SC-01: no change (NOT_APPLICABLE to 03a).

Findings log rows:
| SEC-F-098 | MEDIUM | Provider follows symlinks for data dir and database file; existing entries accepted untouched | OPEN, conditional: Task 34a guard must precede the provider; not live while unwired | 052 |
| SEC-F-099 | LOW | Stat-only rule (SEC-F-069 b) has no automated guard | OPEN, proposal: source-scan test | 052 |
| SEC-F-100 | LOW | ClockConfig suppression and test match file name in any package | OPEN, anchor to `config/ClockConfig.java` | 052 |
| SEC-F-101 | LOW | Extension-load test asserts only SQLException | OPEN, assert `not authorized` (condition 2) | 052 |
| SEC-F-102 | LOW | Directory and file created then chmod; use creation attributes | OPEN | 052 |
| SEC-F-103 | LOW | Startup failure keeps cause; URL query text can appear; reject `?` and `#` in 03b | OPEN, with 03b | 052 |
| SEC-F-104 | INFO | Lock gap on connection replacement; JVM-wide tmpdir property | ACCEPTED (availability) | 052 |

Changelog line: `0.22 (2026-11-02): Task 03a review (handoff 052): SC-13 and SC-32 evidence; SEC-F-098 to SEC-F-104 added (1 MEDIUM conditional, 5 LOW, 1 INFO); verdict PASS WITH CONDITIONS.`
