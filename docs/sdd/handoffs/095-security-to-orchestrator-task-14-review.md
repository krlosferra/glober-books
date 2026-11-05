# Handoff 095: Security review of Task 14
- From: security
- To: orchestrator
- Phase: 4   Round: 1
- Type: AUDIT_REPORT
- Task: 14, branch `chore/task-14-smoke-guard-readme`

## 1. Scope and method
Read: handoff 093, `e2e/support/fixtures.ts`, `builders.ts`, `library.ts`, `e2e/tests/us-010-library-view.spec.ts`, `README.md`, the Task 14 part of `progress.md`, `playwright.config.ts`, `application.yml`, and security.md SC-09, SC-10, SC-23, SC-38, SEC-F-077, 081, 098, 130. The smoke run was NOT re-run (read-only review; the delivery shows a negative control per guard channel). Public advisory databases not consulted (no dependency change in this task).

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Remediation | Control |
|---|---|---|---|---|---|---|---|
| SEC-F-138 | LOW | Test guard | `fixtures.ts` `watch()` | Foreign-origin check looks only at `http:`/`https:` requests; `ws:`/`wss:` and `data:`/`blob:` navigations are not flagged. CSP `connect-src 'self'` would still raise a violation event, so the gap is covered by the violation channel. No seeded wrong-header control was run, and `Set-Cookie`/`Location` absence (security.md section 5 error rules) is not asserted. | Code read; handoff 093 "Not done" | Add `Set-Cookie` absent to `headerProblems`; seed one wrong-header control when the guard is next touched (Task 15 or later) | SC-10 |
| SEC-F-139 | INFO | SEC-F-130 filter | `SEC_F_130_WARNING` | Verified tight: one constant, compared with `text.trim() !==` (full-string equality, no regex, no substring), applied only inside the `GUARDED_CONSOLE` branch for `error`/`warning` console messages. Any other Permissions-Policy, CSP or Trusted Types message still fails. Header check still demands `bluetooth=()` on every asset, so the filter does not mask a header change. Negative control in 093 confirms the exact text. Residual: a hostile page could emit this exact string through `console.error`; only the guard is affected and the violation and page-error channels remain. | Code read; 093 control | None. Remove the filter when SEC-F-130 closes | SC-10 |
| SEC-F-140 | LOW | README / data path | README "Run" | `mkdir -p backend/data` creates the directory with the user's umask (typically 0755), not the 0700 of SC-13; the database file mode is whatever the provider sets until Task 34a. The path `./data/globerbooks.db` is relative: from `backend/` it works; from another directory without `data/` it fails closed ("unable to open database file", verified in 093); from a directory that already holds a `data/` folder it would create or open a database there (the SEC-F-098 class: unexpected location, existing entries accepted). Not exploitable remotely; local, single user. | `application.yml` line 3; README | Keep "run from `backend/`" as written. Carry to Task 34a: resolve the path to an absolute, guarded location (SEC-F-098) and create `data/` as 0700. Optionally state in the README that `chmod 700 backend/data` is advised for v0.2 | SC-13, SEC-F-098 |
| SEC-F-141 | INFO | Task 1 proof table | `progress.md` | Claims match the evidence cited, and the table is honest about unverified pins (`@playwright/mcp` 0.0.83 no page exploration; Temurin not verified). F-4 (no gate enforces the committed dependency tree) is open and correctly stated. The unverified MCP exploration keeps SC-23 at IN_PROGRESS; it must not be marked VERIFIED. | Table read | Sponsor to be told at the v0.2 gate that MCP exploration is outstanding; carry to the next task that can expose the tool | SC-23 |

## 3. Controls checked
- SC-09 (page side): VERIFIED for the smoke. The hostile title test registers the dialog handler before navigation, asserts the exact text is visible as text, zero `script` elements in `main` and in the entry, no dialog, and the guard asserts no violation, console error or page error. Strong enough for T-12. Note: it relies on no stored-sink grep; the grep evidence is in earlier reviews.
- SC-10: VERIFIED in the test layer. Exact CSP string equals the constitution value; the other 6 headers are compared exactly, Permissions-Policy by feature, Content-Type per resource type for every same-origin asset (favicon skipped). `Cache-Control: no-store` is checked.
- SC-38: NOT_STARTED, unchanged (no assertion that `innerHTML = string` throws; the smoke only shows no violation). Do not mark it covered.
- SC-23: IN_PROGRESS (see SEC-F-141).
- Other checks: no secrets, tokens or hostnames from the environment (origin and port are constants `127.0.0.1:18080`, matching `baseURL`). `library.ts` sends `Content-Type: application/json` on POST and DELETE; GET carries none. `uniqueTitle` uses `randomUUID` only for test labels. README: loopback only (`127.0.0.1:8080`), `-Xmx512m` in both run commands (SEC-F-081 CLOSED), `v0.1`/`v0.2` not for real data (SEC-F-077), the locked-database note, the mkdir note, and the `--grep @isolation` command matches `playwright.config.ts` (`grepInvert` unless the argument is present). Nothing exposes the app beyond loopback. `npm ci --ignore-scripts` is used.

## 4. Not verified
- I did not re-run the smoke or the jar. Delivery evidence is the only proof for the run results.
- `@playwright/mcp` page exploration (snapshot, console and network, 360 px resize): not performed in this or any run; the tool is not exposed.
- Public advisories: not queried (no dependency change).

## 5. Residual risks for the Sponsor
- SEC-F-130 (OPEN, Sponsor decision on the Permissions-Policy header list): the guard tolerates exactly one message until then. Recommend deciding with the next constitution change request.
- SEC-F-140: local file mode and path weakness until Task 34a. Recommend accept until then, for v0.2 with non-real data.

## 6. Verdict
PASS WITH CONDITIONS (no CRITICAL or HIGH open).
Conditions, not blocking the merge: (1) the v0.2 gate note states that the MCP exploration and SC-38 are outstanding; (2) SEC-F-140 is carried into the Task 34a brief.
Next step: Liaison ACCEPT, then Sponsor gate.

## Appendix: exact text for security.md (not edited by me)
Findings log:
- `| SEC-F-138 | LOW | Guard: foreign-origin check sees only http(s) requests, no Set-Cookie or Location absence assertion, no seeded wrong-header control | OPEN: covered by the CSP violation channel; hardening in a later e2e task | 095 |`
- `| SEC-F-139 | INFO | SEC-F-130 filter is one exact-equality constant, applied only to console error and warning; header check still requires bluetooth=() | VERIFIED; remove when SEC-F-130 closes | 095 |`
- `| SEC-F-140 | LOW | README mkdir creates backend/data with the umask mode, and the relative database path can open or create a database in an unexpected directory (SEC-F-098 class) | OPEN, carried into the Task 34a brief (absolute guarded path, 0700) | 095 |`
- `| SEC-F-141 | INFO | Task 1 proof table is accurate; MCP page exploration and Temurin pin remain unverified | SC-23 stays IN_PROGRESS | 095 |`
Controls: SC-10 evidence "Task 14: guard asserts exact CSP, 6 headers, Permissions-Policy features and Content-Type on every same-origin asset (handoff 095)"; SC-09 evidence "Task 14: T-12 smoke, no dialog, no script element, no violation (handoff 095)"; SC-38 stays NOT_STARTED; SC-23 stays IN_PROGRESS.
Changelog: `- 0.33 (2026-11-05): Task 14 review (handoff 095): SEC-F-138 to SEC-F-141 added. Verdict PASS WITH CONDITIONS.`
