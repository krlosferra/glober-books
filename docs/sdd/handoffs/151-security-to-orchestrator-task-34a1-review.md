# Handoff 151: Security review, Task 34a-1 (data directory guard and file identity, not wired)
- From: security
- To: orchestrator
- Phase: 4   Round: 1
- Type: AUDIT_REPORT

## 1. Scope and method
Branch `feature/task-34a1-directory-guard`, HEAD 83c6866 against origin/main 1cecb43. Read: briefs/delivery 148 and 149, `DataDirectoryGuard`, the diff of `SqliteDataSourceProvider`, security.md and the status row. Ran: a throwaway harness (`Probe`, `Id`) compiled in the scratchpad from `git archive HEAD` (the repo was not touched), as root on POSIX, plus a sqlite-jdbc open for the race probe. I did NOT re-run `./mvnw clean verify` (the delivery reports it green; the orchestrator supplies the build check). I did not read the two test classes line by line; the harness replaces them as evidence.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-182 | LOW | Integrity | `inspect` | File with `nlink` 2 accepted | Probe `hard link nlink=2`: `OK`, tree unchanged | A second name for the database outside the 0700 directory (needs same user or root) | Read `unix:nlink`, refuse above 1 with a fixed reason, add a test | SC-13 |
| SEC-F-183 | LOW (document grade) | Race | guard + `create(Guarded)` | Identity check detects, does not prevent | After `check()` I swapped the file for a dangling symlink; sqlite-jdbc followed it, created the target as `rw-r--r--` and wrote a table; `requireSameFile()` then refused `IDENTITY_CHANGED`. Pool is closed but nothing created is removed. Also `setPosixFilePermissions` after creation follows links | Same user or root swaps the file inside the window | Keep as documented limit; Sponsor to sign R-25 (proposed in security.md) | SC-13, SC-39 |
| SEC-F-184 | LOW | Fail closed | `check`, `requireOwnerOnly`, `inspect` | (a) `CANNOT_CREATE` after partial creation: 300-byte name leaves `data` and `data/tmp` behind (inside the guarded directory only); (b) owner compared to the principal for `user.name`, not the uid; (c) suid/sgid and ACLs invisible (mode 6600 file accepted) | Probes `long name 300` (`unchanged=false`), `file mode 6600` | Local only; (b) needs control of JVM flags | (a) refuse names over 255 bytes before creating, or document; (b) compare `unix:uid`; (c) read `unix:mode` | SC-13 |
| SEC-F-185 | INFO | Notes | provider | Items for 34a-2, see section 6 | grep of `src/main`: only the two sqlite classes use the provider | | | SC-13 |

No CRITICAL, no HIGH, no MEDIUM.

## 3. Probe results (criteria asked)
- Creation: umask 022 and 000 in child JVMs: dir `rwx------`, file `rw-------`, `tmp` `rwx------`. Atomicity: `Files.createDirectory/createFile` receive the mode as a `PosixFilePermissions` attribute, so mkdir/open get 0700/0600 in the same syscall; the umask can only narrow it (a 0277 umask gives a narrower mode first, then the explicit set widens to 0700/0600 exactly). No instant at which the path is wider than the target. VERIFIED. The `chmod` that follows is by path (follows links), see SEC-F-183.
- Refused, fixed reason, tree (type, mode, owner, link target) identical before and after: symlink as data dir, as file, as `tmp`, below a symlinked parent (two forms), dangling file link (target not created); FIFO, socket, char device, directory in place of the file, regular file in place of `tmp` (all `WRONG_FILE_TYPE`, found by `readAttributes` without opening, so a FIFO cannot block the start); relative, `..`, `/library.db`, null (`LOCATION_NOT_ABSOLUTE`); 300-byte directory name (`CANNOT_CREATE`, nothing created).
- Symlink swapped after the check: refused after the open (see SEC-F-183). Swap by delete and recreate, copy, hard link of another inode, symlink to the same inode: all `IDENTITY_CHANGED`; in-place rewrite of the same inode is accepted (correct, it is the same file). The first re-link variant of my harness was flawed and proves nothing.
- Foreign owner and group/other bits: not re-probed by me; the delivery shows tests with `Files.setOwner` as root that ran, and I read `requireOwnerOnly` (owner and the six group/other bits, `NOFOLLOW_LINKS`). Accepted on reading, with SEC-F-184 (b).
- Not refused: `nlink` 2 (SEC-F-182), mode 0000 or sticky 1700 or 6600 (narrower or invisible, SEC-F-184), Unicode lookalike and case variants are distinct names on Linux and accepted (as new directories).
- Reasons: enum with eight fixed texts; `Refusal` has no cause (`getCause()` null), `toString()` is class plus the fixed text, `Guarded.toString()` is `Guarded`; no logger in the class (grep). No path, owner or mode in any text. VERIFIED. `Path.of` with a NUL byte throws `InvalidPathException` (with the input text) before the guard: 34a-2 must catch it.
- Identity: compares `fileKey` (device+inode) and regular-file flag with NOFOLLOW after the pool opens. It catches a swap that happened before the pool opened (other inode, symlink, deletion). It does not catch: a swap after the open that keeps the inode, the damage already done by the driver when a swap happened before the open (SEC-F-183), a platform without `fileKey` (null equals null; only the regular-file flag then counts). Grade: document grade, same-user or root attacker only; the Javadoc and delivery 149 state it honestly. The Sponsor must sign R-25.
- Old paths: `create(Path)` and `create(String)` are unchanged and public; they follow symlinks and accept lax modes. Today nothing in `src/main` calls the provider (grep), so no bypass is live. Once wired, any code path that reaches them is a full bypass.
- Unwired: `git diff` touches no `pom.xml`, `application.yml`, Spring configuration or `backend/config/**`; `application.yml` still holds the Boot data source. The packaged jar is unchanged: SEC-F-178 stays OPEN, SC-13 and SC-32 for the jar stay FAILED.

## 4. Controls checked
SC-13: VERIFIED at unit level for creation and refusal; FAILED for the jar. SC-39 (directory/file part): unit level only; jar NOT_STARTED. SC-11, SC-37: fixed reasons VERIFIED at unit level; the log/console rule on the jar is 34a-2. SC-32: unchanged, jar FAILED. SC-14: no logger in the guard, VERIFIED by grep.

## 5. Not verified
Full Maven build and coverage (not re-run); foreign-owner and group/other tests (read, not run by me); behaviour on macOS or Windows (no `fileKey` or POSIX there); case-insensitive file systems; ACLs; the real hot-journal mode inheritance (34a-2 on the jar); public advisory databases not needed (no dependency changed).

## 6. What 34a-2 must prove (SEC-F-178 and conditions)
1. Jar built from the branch, umask 022, nothing pre-created: `data` 0700, `library.db` 0600, `data/tmp` 0700 before the first connection; after a write and a forced journal, `-journal` is 0600.
2. Existing 0755 `data/` and 0644 file: the jar refuses at start with a clean message, exit non-zero, no path in the console, files unchanged.
3. Symlinked `data` or `library.db`: refused likewise.
4. Second instance on the same file: refused with a clean message (claim live in the jar); `foreign_keys`, `trusted_schema`, `busy_timeout` 5000 read back on the jar.
5. A test that fails if a data source is built any way other than `check` then `create(Guarded)`; the old overloads removed or unreachable from configuration.
6. `InvalidPathException` mapped to a fixed refusal; SEC-F-182 and SEC-F-184 (a)(b) fixed or accepted in writing; R-25 signed.

## 7. Residual risks for the Sponsor
R-25 (check-to-open window, same user or root only): recommend ACCEPT. Nothing else new.

## 8. Verdict
PASS WITH CONDITIONS.
- C-1: SEC-F-178 stays open and blocks any tag or release until 34a-2 is merged and I have re-probed the jar (section 6).
- C-2: the Sponsor signs R-25 at the gate, or Task 34a-2 treats the limit as unaccepted.
- C-3: SEC-F-182 (nlink) and SEC-F-184 (a)(b) go into 34a-2 or are accepted in writing; none blocks the merge of 34a-1.
Security files changed: `docs/sdd/security.md` (v0.51), `docs/sdd/status.md` (row 34a-1), this file. Nothing committed.
