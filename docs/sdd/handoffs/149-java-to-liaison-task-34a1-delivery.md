# Handoff 149: Task delivery, Task 34a-1 (complete directory guard and file identity, not wired)
- From: java
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY

## Resume point
- Done: guard, provider edit, both test classes, full build green, progress.md and status.md row, this file.
- Next: nothing for the implementer. Orchestrator: push, Liaison audit, Security review (protected surface, alone), Sponsor gate. Task 34a-2 wires the guard.
- Branch `feature/task-34a1-directory-guard`, local commits only, not pushed.

## 1. Task, branch, commits
Task 34a-1, branch `feature/task-34a1-directory-guard`. Code commits: `feat(storage): data directory guard and file identity, not wired [Task-34a-1]` and `fix(storage): reword guard Javadoc for the A-09 marker scan [Task-34a-1]` (the first build failed on the existing A-09 scan `FaultInjectionIntegrationTest.noProductionFileHoldsATestOnlyRouteSwitchOrTriggerA09`, which forbids the words "test-only" in `src/main`; the Javadoc was reworded, no test changed). A third commit holds this file, `progress.md` and my `status.md` row.

## 2. Files
Created: `config/sqlite/DataDirectoryGuard.java` (282 lines), tests `DataDirectoryGuardTest` (297), `SqliteFileIdentityTest` (123), and two small test helpers in `src/test/.../support/`: `TreeSnapshot` (tree listing with mode, owner, link target, SHA-256) and `GuardChildMain` (child process for the umask test). The helpers exist only because the test files are limited to 300 lines and the umask must be set in a separate process. Modified: `SqliteDataSourceProvider.java` (new `create(DataDirectoryGuard.Guarded)`, 231 lines), `docs/sdd/progress.md`, my row of `docs/sdd/status.md`. Nothing else: no `pom.xml`, `application.yml`, Spring configuration or `backend/config/**`. The PM files (tasks.md, spec.md) were committed earlier by the orchestrator.

## 3. Design
- `DataDirectoryGuard.check(Path)` returns `Guarded(databaseFile, nativeDirectory, FileIdentity)`. Layout `<data dir>/<file>` plus the native directory `<data dir>/tmp` (0700) that the provider already used.
- Order: absolute and normalized path, no symbolic link in the path (every component, also of `tmp`), parent of the data directory exists and is a directory, then inspection of every existing part (type, owner, group/other bits, all with `NOFOLLOW_LINKS`, attributes only), and only then creation of what is missing (directory, `tmp`, file) with 0700/0600 given at creation and set again explicitly. So a refusal creates, changes and deletes nothing. The data directory is created one level deep only (a missing parent is a refusal, criterion 3).
- Owner: the owner of each part must equal the principal looked up for `user.name`; a failed lookup fails closed (`CANNOT_CREATE`).
- Reasons are the enum `Reason` with one constant text each; `Refusal` (an `IllegalStateException`) carries only that text and no cause; `Guarded.toString()` is fixed and holds no path.
- Identity: `FileIdentity(fileKey, regularFile)` read with `NOFOLLOW_LINKS`. `SqliteDataSourceProvider.create(Guarded)` opens the pool, calls `guarded.requireSameFile()` (attributes only, so no descriptor of the file is closed and the POSIX lock stays), applies the schema, and on a mismatch closes the pool and rethrows the refusal.
- `create(Path)` and `create(String)` are unchanged, because the existing tests (for example `doesNotChangeModesOfAnExistingDirectoryAndFile` with `rwxr-x---`) legitimately use directories the guard would refuse. 34a-2 decides what the wired path does with them.

## 4. Criteria to code to tests
| Criterion / row | Code | Tests (all in `DataDirectoryGuardTest` unless noted) |
|---|---|---|
| 1, US-009.3, US-009.6, SC-13 creation | `createDirectory`, `createFile` | `createsDirectoryWith0700AndFileWith0600UnderAnyUmask` (umask 022, 000, 277, 077, in a child process via `sh -c "umask N && exec java"`), `createsOnlyTheMissingPartsAndLeavesNoOtherFile`, `acceptsExistingOwnerOnlyDirectoryAndFileAndChangesNothing`, `acceptsAFileNarrowerThan0600` |
| 2, US-009.6, SC-13, SC-39 refusal | `inspect`, `requireOwnerOnly`, `rejectSymbolicLinks` | `refusesASymbolicLinkAsTheDataDirectory`, `...AsTheDatabaseFile`, `refusesADanglingSymbolicLink...`, `refusesASymbolicLinkInTheGuardedPath`, `...AsTheNativeDirectory`, `refusesAnyGroupOrOtherPermissionBitOnTheDirectory` (6 modes), `...OnTheFile` (6 modes), `refusesANativeDirectoryWithAGroupBit...`, `refusesADirectoryOfAnotherOwner`, `refusesAFileOfAnotherOwner`, `refusesADirectoryInPlaceOfTheFile...`. Each uses `assertRefused`: tree snapshot (type, mode, owner, link target, SHA-256) equal before and after |
| 3, SEC-F-098, SEC-F-140 | `requireAbsolute`, `requireDirectory`, `CANNOT_CREATE` | `refusesARelativeANonNormalizedAndANullLocation`, `refusesAMissingParentOfTheDataDirectoryWithoutCreatingAnything`, `refusesAParentThatIsNotADirectory`, `refusesWhenTheFileCannotBeCreated...` (file name of 300 bytes, listing of the parent shows only `data`) |
| 4, SC-11, SC-37 fixed reasons | `Reason`, `Refusal` | `usesOneFixedTextPerCategoryWithNoPathOwnerOrModeInIt`: two different bad locations give the same message, no path, owner, mode in any text, no cause, `Guarded.toString()` holds no path. No logging is done in the guard |
| 5 identity | `FileIdentity`, `Guarded.requireSameFile`, provider `create(Guarded)` | `SqliteFileIdentityTest`: `acceptsTheSameFile`, `refusesAFileSwappedForAnotherFile`, `refusesAFileReplacedByASymbolicLinkToTheOriginal`, `refusesAFileThatIsGone`, `identitiesOfTwoDistinctFilesDiffer`, `providerStartsOnAGuardedFileAndAppliesTheSchema`, `providerRefusesAndReleasesTheClaimWhenTheFileWasSwappedAfterTheCheck` |
| 6 existing behaviour | provider unchanged except the new method | `SqliteStoreIntegrationTest` (16 run, 1 skipped as before: the not-writable test, because the sandbox is root), `StartupPolicy*`, claim child |
SC-11 and SC-37 hold at unit level only (fixed reasons); console and log rule on the jar is Task 34a-2.

## 5. Evidence
- `cd backend && ./mvnw -o clean verify`: BUILD SUCCESS. Checkstyle 0 violations; surefire 940 tests, 0 failures, 0 skipped; failsafe 269 tests, 0 failures, 1 skipped (the root-only not-writable test, same as before); SpotBugs done; "All coverage checks have been met". (Offline mode because the sandbox has a proxy; no dependency changed.) A first run failed once on the A-09 scan, see section 1.
- Observed modes under each umask (child JVM, output of `GUARDED dir= file= tmp=`): umask 022: dir=rwx------ file=rw------- tmp=rwx------; umask 000: same; umask 277: same; umask 077: same.
- Refusal cases observed (reason category, tree snapshot with SHA-256 and modes equal before and after, `unchanged=true` printed by the test): symlink as directory SYMBOLIC_LINK; symlink as file SYMBOLIC_LINK (target hash unchanged); symlink in the path SYMBOLIC_LINK; dangling link SYMBOLIC_LINK (the target is not created); foreign owner of directory and of file FOREIGN_OWNER (done with `Files.setOwner` to `nobody` as root; the tests ran, they did not skip; if run as a non-root user they abort with an explicit message, not silently); group or other bit (12 modes) PERMISSIONS_TOO_WIDE; directory where the file should be WRONG_FILE_TYPE; relative, non-normalized, null path LOCATION_NOT_ABSOLUTE; missing or file parent PARENT_UNUSABLE; 300-byte name CANNOT_CREATE.
- Line coverage (JaCoCo, from `target/site/jacoco/jacoco.csv`): `DataDirectoryGuard` with its nested types 90 of 94 lines (95.7%; the uncovered lines are the fail-closed branches of an IOException on attributes that cannot be provoked without a test-only switch); `SqliteDataSourceProvider` 63 of 70 lines (90%) plus `ClaimingDataSource` 21 of 23. Overall gate (80%) met.

## 6. Deviations, limits, unwired state
- No deviation from the plan. Two test helper files were added under `support/` (section 2).
- Documented limits (also in the Javadoc of the guard): (1) race between check and open: a swap in that window cannot be tested without a switch that exists only for tests (A-09); the identity comparison narrows it and the 0700 directory owned by the user removes every attacker except the same user or root. The Sponsor is asked to agree (handoff 027). The swap before the open IS tested. (2) On a file system without POSIX attributes the owner and mode checks are not made (US-009.6); symlink, type and identity checks still are. (3) Every component of the path is checked for a symbolic link, so a data location below a link (for example a link in `/var` on some systems) is refused by design.
- The guard is NOT wired: the packaged application still uses Boot's auto-configured data source (jar creates `library.db` 0644, does not create the directory, a second instance starts). No Spring configuration, `application.yml` or `pom.xml` was touched; nothing needed wiring to prove the criteria. SEC-F-178 stays open and SC-13 for the jar stays FAILED until Task 34a-2. The companion-file clause of Task 33 was exercised vacuously (no `-journal` at the kill points; SEC-F-181). The guard prepares no companion file itself; SQLite creates `-journal` with the mode of the database file, which 34a-2 proves on the jar.
- Follow-up for 34a-2: decide whether `create(Path)` and `create(String)` stay (tests use lax modes) or are replaced by the guarded path.

## 7. Git evidence (taken before the last docs commit)
`git diff --stat origin/main..HEAD`:
```
 .../config/sqlite/DataDirectoryGuard.java          | 282 +++++++++++++++++++
 .../config/sqlite/SqliteDataSourceProvider.java    |  28 ++
 .../config/sqlite/DataDirectoryGuardTest.java      | 297 +++++++++++++++++++++
 .../config/sqlite/SqliteFileIdentityTest.java      | 123 +++++++++
 .../globerbooks/support/GuardChildMain.java        |  16 ++
 .../globant/globerbooks/support/TreeSnapshot.java  |  48 ++++
 .../148-orchestrator-to-java-task-34a1-brief.md    |  25 ++
 docs/sdd/spec.md                                   |   8 +-
 docs/sdd/status.md                                 |   7 +-
 docs/sdd/tasks.md                                  |  76 +++---
 10 files changed, 874 insertions(+), 36 deletions(-)
```
`git log origin/main..HEAD --format='%an <%ae>%n%B---'`:
```
krlosferra <carlos.ferra@globant.com>
fix(storage): reword guard Javadoc for the A-09 marker scan [Task-34a-1]
---
krlosferra <carlos.ferra@globant.com>
feat(storage): data directory guard and file identity, not wired [Task-34a-1]
---
krlosferra <carlos.ferra@globant.com>
docs(tasks): split Task 34a into 34a-1 and 34a-2, DEC-037, brief 34a-1 [Task-34a-1]
---
```
Note: the first listed commit is the PM and brief commit by the orchestrator; the last docs commit of this task was added after this listing.
