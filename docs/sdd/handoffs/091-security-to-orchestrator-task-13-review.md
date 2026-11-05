# Handoff 091: Security review of Task 13 (E2E harness)
- From: security | To: orchestrator | Type: AUDIT_REPORT | Branch: `chore/task-13-e2e-harness`

## Scope and method
Read: handoff 089, `e2e/support/start-server.mjs`, `e2e/playwright.config.ts`, `e2e/global-setup.ts`, `e2e/tests/harness.spec.ts`, constitution 1.3 SC-33 (line 266), security.md SC-33, SEC-F-070, SEC-F-080. Re-ran `cd e2e && npx playwright test` (Node 24 PATH): `1 passed (7.3s)`; afterwards no `/tmp/globerbooks-e2e-*` directory remained. The hostile-environment run was NOT re-run; I rely on the delivery evidence.

## Findings
| ID | Severity | Location | Finding | Remediation |
|---|---|---|---|---|
| SEC-F-133 | LOW | start-server.mjs `assertOwnDirectory` | The 0700 mode is set by `chmodSync` but never asserted by `lstat`, and the owner uid is not checked. `mkdtemp` already creates 0700 and `/tmp` is sticky, so the practical risk is low. | Add `(st.mode & 0o777) === 0o700` and `st.uid === process.getuid()` to the check. |
| SEC-F-134 | LOW | start-server.mjs `main` | If `assertOwnDirectory` throws after `mkdtemp`, the directory is not removed. It is left empty in the tmpdir and nothing is deleted. | Fail closed is acceptable. Optionally `rmdirSync` the empty dir when it is our own path. |
| SEC-F-135 | LOW | start-server.mjs signals | Only SIGTERM and SIGINT are forwarded. SIGHUP, or SIGKILL of the launcher after Playwright's 10 s graceful timeout, would orphan java and the temp dir (which holds only test data). The java process is a direct child with no shell, so SIGTERM does reach it. | Also forward SIGHUP. Accept the SIGKILL case as residual. |
| SEC-F-136 | INFO | playwright.config.ts | The config-load `--check-port` runs for every config load, including `--list`. It uses `execFileSync` with `process.execPath` and constant arguments (no shell, no injection) and its only side effect is one TCP connect to 127.0.0.1:18080. | None. |
| SEC-F-137 | INFO | evidence | The decoy was not read through the application (declared in 089). The hostile set did not include `SPRING_CONFIG_*`, `SPRING_PROFILES_ACTIVE` or `JDK_JAVA_OPTIONS`, and there was no decoy `application.properties` in the parent cwd. The allowlist and the cwd cover all of these by construction. | Optional: add them to the next hostile run. |

No CRITICAL, HIGH or MEDIUM findings.

## Controls
- SC-33: VERIFIED.
  - The launcher creates the temp dir (`mkdtemp` under `realpath(os.tmpdir())`) with prefix `globerbooks-e2e-`.
  - Checks: lstat not a symlink, real path equals the path, direct child of the real tmpdir, prefix. The mode check is SEC-F-133.
  - The datasource path is asserted inside the dir.
  - Args are `--server.address=127.0.0.1 --server.port=18080 --spring.datasource.url=...` (command line wins over SPRING_APPLICATION_JSON, JAVA_TOOL_OPTIONS and env).
  - Env is an allowlist of PATH, HOME, JAVA_HOME, LANG, TZ. The child showed `HOME JAVA_HOME PATH`.
  - cwd is the temp dir, which holds only `library.db`, so there is no `config/` or `application.*` file (SEC-F-080).
  - `spawn('java', args)` has no `shell`, so there is no injection surface (the jar path is derived from `__dirname`, nothing comes from the environment).
  - Config and global-setup use literals (baseURL host and port asserted). `reuseExistingServer:false`. Port 18080 on loopback only (lsof showed java only on 127.0.0.1:18080).
  - Delete logic: `cleanup` runs once, after the child `exit` or `error`, re-runs `assertOwnDirectory(dir)`, and deletes only its own `dir` constant (never empty, never a variable from outside). `rmSync` does not follow symlinks. No other delete exists.
  - Raw TCP busy check: the timeout counts as busy, an error counts as free, and there is only a tiny check-then-start window.
- SEC-F-070 and SEC-F-080: implementation matches the closed design. Stay CLOSED, now verified in code.
- Evidence: the decoy SHA-256 was identical three times, decoy2.db and decoy3.db were never created, and the child env and cwd were listed. With the code review this supports the claims. Minor gap: no `lsof` on the decoy during the run, which is covered by the code (the decoy URL is never passed to the child).

## Not verified
Hostile run not repeated by me. Orphan behavior under SIGKILL not exercised. Advisory databases not consulted (no dependency change in this task other than the existing e2e lockfile, `npm audit` not run here).

## Residual risks
None needing Sponsor acceptance; SEC-F-135 SIGKILL orphan leaves only test data in a 0700 temp dir.

## Verdict
PASS (with the optional hardening of SEC-F-133 to SEC-F-135 as LOW backlog). Next step: Liaison ACCEPT, then Sponsor gate.

## Appendix: exact text for security.md
- SC-33 status: `VERIFIED (Task 13, handoff 091): launcher mkdtemp under real tmpdir, prefix and lstat checks, args --server.address/--server.port/--spring.datasource.url, allowlisted env (PATH, HOME, JAVA_HOME, LANG, TZ), cwd in the temp dir, spawn without shell, delete of own dir only after child exit with re-check; decoy hash unchanged in hostile run (089).`
- Findings log rows:
  - `| SEC-F-133 | LOW | Launcher does not assert mode 0700 or owner uid of its temp dir (mkdtemp already creates 0700) | OPEN (LOW backlog) | 091 |`
  - `| SEC-F-134 | LOW | Launcher leaves an empty temp dir if its own directory check fails after mkdtemp | OPEN (LOW backlog) | 091 |`
  - `| SEC-F-135 | LOW | SIGHUP not forwarded; SIGKILL of the launcher orphans java and the test temp dir | OPEN (LOW backlog) | 091 |`
  - `| SEC-F-136 | INFO | Config-load --check-port uses execFileSync, constant args, one loopback TCP connect; no injection | NOTED | 091 |`
  - `| SEC-F-137 | INFO | Hostile run did not include SPRING_CONFIG_*, SPRING_PROFILES_ACTIVE, JDK_JAVA_OPTIONS or a decoy application.properties; covered by allowlist and cwd | NOTED | 091 |`
- Changelog: `- 0.19 (date): Task 13 review (handoff 091): SC-33 VERIFIED; SEC-F-133 to SEC-F-137 added (3 LOW, 2 INFO); verdict PASS.`
