# Handoff 051: Audit of Task 03a, SQLite data source, claim and clock
- From: liaison
- To: orchestrator (for PM, Security, Sponsor)
- Phase: 4   Round: 1
- Artifacts: handoffs 045 and 047, `tasks.md` Task 03a, `SqliteDataSourceProvider.java`, `ClockConfig.java`, `schema.sql`, `SqliteStoreIntegrationTest.java`, `backend/config/checkstyle/suppressions.xml`, `progress.md`
- Type: REVIEW_REPORT
- Mode: F (with G for oracles)

## Verdict: ACCEPT WITH FOLLOW-UPS
Compliance and Testing layers pass on the evidence in 047 and on the code I read. Two conditions before the Sponsor merge gate: item C1 (E9/E10 evidence) and the Security verdict (not replaced by this audit). None of the follow-ups needs a rework of Task 03a before 03b starts, except F1 if the Sponsor wants it closed in this branch.

I had no shell in this audit: the green `clean verify`, E9 and E10 are taken from the orchestrator's run and from 047, not reproduced by me.

## Acceptance criteria
| AC | Result | Basis |
|---|---|---|
| 1 Modes, stat-only after claim | PASS | `prepareStorage/createDirectory` set 0700/0600 explicitly before the pool; `tmp` 0700; exact-mode tests; sidecar `-journal` 0600 asserted. Provider never opens the file after the claim; the hash helper runs only after the pool and the child are stopped (test lines 148-166). |
| 2 Settings, pool, eviction | PASS | Six pragmas read back before and after `softEvictConnections` with a different raw connection identity; pool 1/1/0/0/0; `load_extension` fails; non-delete journal refused (WAL test). The `ClaimingDataSource` runs on every physical connection. |
| 3 Claim from a second JVM | PASS | Separate JVM, exit 2 plus "database is locked", hash equal, negative control exit 0, repeated after eviction, start refused with fixed message while another process holds the file. No fallback needed. |
| 4 tmpdir | PASS | Property set before the pool; child JVM test finds `*sqlitejdbc*` in `<data dir>/tmp`. X-10 closed, no `security.md` handoff needed. |
| 5 Schema, clock | PASS | `schema.sql` is the AP-6 statement (I compared it with the delivery claim; text is consistent with a STRICT table with the nine CHECKs); two starts give identical stored DDL; one `Clock.systemUTC()`. |

Plan checks: rollback journal never set and verified; `synchronous` FULL; EXCLUSIVE; IMMEDIATE; `busy_timeout` 5000; `foreign_keys`; extension loading off; native SQL only in `config.sqlite`; no `application.yml`, no `StartupPolicy`, no guard, no entity: scope against 03b/34a/04 is clean. No new dependency (E8 empty).

## Rulings on the points you asked
1. **`trusted_schema` by PRAGMA in the wrapped data source: ACCEPTED.** Brief 045 section 4 already says the wrapped data source applies the settings per physical connection; only a dropped setting or a `connection-init-sql`/URL parameter was ASK FIRST. The value is read back as 0 before and after eviction. Note for Security: the PRAGMA runs after the connection opens and before the claim, not as an open flag; Security should confirm it accepts that ordering. Also record the missing `SQLiteConfig` key as an AP-12.5 correction in the plan (PM).
2. **SpotBugs fixes: ACCEPTED.** `Objects.requireNonNull(getParent())` is inside the `try` that catches `RuntimeException`, so an NPE ends in the fixed message, not a stack trace. `ResourceDatabasePopulator` reads the classpath resource, not the database path, and keeps native SQL in `config.sqlite`. No suppression, no `exclude.xml`, no `@SuppressFBWarnings`, as 044 required. Gate vacuity answered: 11 Checkstyle files (was 4), SpotBugs 0 bugs on 4 classes (was 1), JaCoCo provider 52/56 and nested class 21/23 lines, merged 92.4%.
3. **Three extra test helpers (`ChildJvm`, `FileFacts`, `PoolFacts`): ACCEPTED as a justified deviation.** Forced by the 300-line limit (the test class is 287 lines), test-only, no gate weakened. Brief listed only two test files; PM should add the three files to the Task 03a "Tests" line in `tasks.md` for traceability (item F5).
4. **ClockConfig suppression left file-scoped: ACCEPTED for this task, tighten later.** The glob `[\\/]src[\\/]main[\\/]java[\\/].*[\\/]ClockConfig\.java$` matches any file named `ClockConfig.java` under `src/main/java`; the test `mainSourcesCallTheWallClockOnlyInClockConfig` compares file names only, so a second `ClockConfig.java` in another package would pass both guards. Risk is low, the reason given (a change to `backend/config/**` needs approval and a seeded run) is correct. See F2.
5. **Skipped unwritable-parent test: ACCEPTED, not evidenced.** The skip is honest (`assumeFalse(isWritable)`), and the code path (`createDirectory` throws, `IOException` becomes the fixed message) is the same one the passing non-directory-parent test covers. But the unhappy path is unproven in this environment. See F3.
6. **Untested narrower pre-existing mode: ACCEPTED as a gap, but the related test has a weak oracle.** `doesNotChangeModesOfAnExistingDirectoryAndFile` creates the entries at exactly 0700/0600, which are the values the provider would itself set, so it passes even if the provider re-applied permissions to existing entries. It cannot fail for the behavior it names. See F4.
7. **Delivery E9/E10 note missing: BLOCKING for the gate, not for the code.** 047 section 4 says "see the commands at the end of this file" and the file has none. Layer 1b is blocking in Mode F. See C1.

## Condition before merge
- **C1.** Orchestrator pastes the output of `git diff --stat main..HEAD` (only the files in 047 section 1 plus `progress.md` and the handoffs expected) and `git log main..HEAD --format='%an <%ae>%n%B---'` (human identity, `type(scope): subject [Task-3a]`, no AI attribution, no session link, atomic commits) and runs the repo hooks. I could not verify either. If any commit carries a trailer or link, REJECT on Layer 1b and the Sponsor decides the history repair; I do not run git.

## Follow-ups (numbered, none blocks 03b)
- **F1 (MINOR, security hygiene).** `Files.createFile` / `Files.createDirectory` followed by `setPosixFilePermissions` leaves a short window at the umask default (for example 0644/0755), and `Files.exists` then `createFile` is a check-then-act race. Create with the `PosixFilePermissions.asFileAttribute` attribute so the mode is set at creation (`FileFacts.fileAttr/dirAttr` already exist in tests). Cheap to do in this branch; otherwise fold into Task 34a (`DataDirectoryGuard`) and note it in `progress.md`. Security to rule on whether it is required.
- **F2 (MINOR).** Tighten the ClockConfig suppression to the package path (047 proposal `[\\/]config[\\/]ClockConfig\.java$`), show a seeded run with a `ClockConfig.java` outside `config/` still failing, and make the test compare the relative path, not the file name. Needs PM and Sponsor approval for `backend/config/**`; schedule with the next task that touches `backend/config` (Task 03b is the natural one).
- **F3 (MINOR).** Run `failsWithFixedMessageWhenParentIsNotWritable` once as a non-root user (or in CI) and paste the pass. Until then record it in `progress.md` as "skipped, unverified".
- **F4 (MINOR, weak oracle, Module 4).** Replace the oracle of `doesNotChangeModesOfAnExistingDirectoryAndFile` with one that can fail: a pre-existing directory at 0750 (and file at 0640) must come out at 0750/0640 (the provider does not touch what it did not create; refusal belongs to 34a). A narrower-than-0600 existing file stays untestable for a non-root run (the pool needs read-write), so state that limit in the test Javadoc instead of in the delivery only.
- **F5 (MINOR, PM).** Update `tasks.md` Task 03a "Tests" to list the three helpers, and note the Hikari `connection-timeout` is deferred to 03b (already in 047 deviations).
- **F6 (MINOR, for 03b).** The provider builds the URL as `jdbc:sqlite:` + path; a path containing `?` would be read by the driver as parameters. 03b `StartupPolicy` must reject it (URL form without parameters); add that case to its tests. `create(String)` accepts any text after the prefix by design today.
- **F7 (MINOR).** The wall-clock test regex does not match `Clock.systemDefaultZone`, bare `now()` or `System.nanoTime`-style reads; the Checkstyle rule does match `Clock.system(UTC|DefaultZone)(` and `.now(`. Acceptable; no action unless the PM wants the two aligned.
- **F8 (note for the time-rule proposal in 047).** Agreed that no test needs a time rule yet; PM to decide when `BookService` tests arrive.

## Next step
Orchestrator supplies C1, requests the Security verdict, then the Sponsor decides. After acceptance: update `progress.md` and `status.md` (`[Task-3a] ... ACCEPTED`), open the pull request only after Sponsor approval, then Task 03b may start. Do not start Task 04 before 03b is done.
