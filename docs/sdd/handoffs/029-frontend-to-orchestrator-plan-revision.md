# Handoff 029: Frontend plan revision, Phase 3 round 2 (replacement text)
- From: frontend
- To: orchestrator (copy to liaison, security, pm)
- Phase: 3   Round: 2 of 3
- Responds to: handoff 025 (F-09, F-10, F-13, F-15(4), F-16, F-19, F-22, F-24) and handoff 026 (SEC-F-070, SEC-F-076, SEC-F-077)
- Type: PLAN_REVISION (documents only; nothing was edited outside this file)

How to use: each block names the file and passage, quotes the anchor to replace, and gives the exact replacement. The orchestrator applies them to `plan.md` / `plan-appendix.md`. Items that need a Sponsor, PM or other-owner decision are in section 5; I decide none of them. Test-title ids and Task numbers are those of `tasks.md` 0.3.

## 1. Accessibility checker (F-09, SEC-F-076)

### 1.1 `plan-appendix.md` AP-10, row `@axe-core/playwright` (replace the whole row)
| `@axe-core/playwright` | Approved by the constitution; the pin is chosen at Task 43 (SC-21). Before any pin: publisher Deque; the package and `axe-core` exist on the registry; no install scripts; a peer range that accepts Playwright 1.64.1; lockfile `resolved` hosts on `https://registry.npmjs.org/`; `npm audit --audit-level=high` clean; then the pin is written in the constitution table. The checker runs in test code only; the application CSP and its response headers are never relaxed (Sponsor decision 2026-11-02). **Allowed test-side mechanism:** (1) first attempt with no bypass at all: the scan injects through the test driver (`page.evaluate` is not subject to the page CSP); (2) only if that fails, `bypassCSP: true` is set on a dedicated context created inside `us-014-accessibility.spec.ts` (`browser.newContext({ bypassCSP: true })`); never in `playwright.config.ts` `use`, never in a project, never in a shared fixture; (3) the scan context never serves a test that uses the `guard` fixture for CSP assertions, and the Task 45 CSP and Trusted Types tests run in a context without bypass; (4) the scan context still blocks cross-origin requests and records them. The rule set is fixed to the WCAG 2.1 A and AA tags with the impact filter "serious or critical" (US-014.1). `frontend-testing.md` still lists the checker as ASK FIRST against constitution line 45: aligned in Task 43 if the file is added to its list (open matter M-5), otherwise in Task 48. | 43 |

### 1.2 `plan-appendix.md` AP-9.3 (add after the `guard` bullet)
- The accessibility scan (Task 43) uses its own context (AP-10). That context is not a fixture of other tests, does not use `guard`, and a test file that creates it asserts at its end that `bypassCSP` was never set on the default context (`browser.contexts()` created by the `page` fixture are untouched).

### 1.3 `plan.md` risk table, row `F-axe` (replace)
| F-axe | The accessibility checker may be blocked by the CSP or Trusted Types | Runs in test code only; first try with no bypass; if one is needed it is a `bypassCSP` on a dedicated context inside `us-014-accessibility.spec.ts` (never global, never for the guard tests); the application CSP and headers are never relaxed (Sponsor decision 2026-11-02; SEC-F-076) |

### 1.4 Proposed `tasks.md` Task 43 wording (PM owns the file)
- `Depends on: 40, 39` (delete "decision of the Sponsor and Security on the checker and the content-security policy").
- AC3 replacement: "The scan first runs without any bypass. If the checker cannot inject under the CSP, a dedicated context with `bypassCSP: true` is created inside `us-014-accessibility.spec.ts` only; the configuration, the `guard` tests and the Task 45 tests are unchanged; the server headers are never changed; the scan context still blocks cross-origin requests (SEC-F-076 (b) to (d))."
- AC1 adds the SC-21 checks of 1.1 and the constitution pin. If the scan cannot run without relaxing the application itself, the task stops and the Sponsor decides (US-014.1 stays P0).

## 2. Launcher, isolation, SC-33 (F-10, SEC-F-070)

### 2.1 `plan-appendix.md` AP-9.2, first two bullets (replace)
- `global-setup.ts` asserts: the `baseURL` host is exactly `127.0.0.1`; the port is 18080; the application jar exists. It never reads `baseURL` or the port from an environment variable.
- **Start order.** In Playwright 1.64.1 the `webServer` starts before `globalSetup` (to be recorded as evidence by Task 13 AC3). A directory made in `global-setup` therefore cannot be passed to the server, and creating it at config load would run once per worker. The launcher `support/start-server.mjs` (plain Node, no dependency) is the `webServer.command` and creates the directory. For SC-33 the launcher is the component that creates the temporary directory, and `global-setup` keeps the loopback, port and jar assertions (wording proposed in M-1 below).
- **Launcher rules (SEC-F-070).**
  1. It creates the directory with `fs.mkdtempSync(path.join(fs.realpathSync(os.tmpdir()), 'globerbooks-e2e-'))`, mode 0700, and asserts that the real path is a direct child of the real `os.tmpdir()`, has the prefix, is not a symlink, and that the database path is inside it.
  2. It passes the three settings as **command-line arguments** of `java -jar`, the highest-precedence source: `--server.address=127.0.0.1 --server.port=18080 --spring.datasource.url=jdbc:sqlite:<dir>/library.db`. The launcher never reads these values from its own environment.
  3. It starts the child with an **allowlisted environment** built from nothing: `PATH`, `HOME`, `JAVA_HOME`, `LANG`, `TZ` only, copied when present. No `SPRING_*`, no `JAVA_TOOL_OPTIONS`, no `JDK_JAVA_OPTIONS`, no `_JAVA_OPTIONS`, no `JAVA_OPTS`, no `SERVER_*`. (A `-Xmx` flag, if the plan fixes one, is an explicit argument of the launcher, not an inherited variable.)
  4. It forwards termination (`SIGTERM`, `SIGINT`) and exit codes, and deletes the directory only after the child has exited, only the path returned by its own `mkdtemp`, re-checking rules 1 above before `rmSync`. It never opens the database file (it is locked).
  5. Playwright fails when the port is busy (`reuseExistingServer: false`).
- **Hostile-environment run (Task 13 AC).** One test run (harness spec, tagged `@isolation`, executed by hand with the variables set in the shell, not part of the default run) with `SPRING_DATASOURCE_URL` pointing at a decoy file, `SPRING_APPLICATION_JSON` setting another URL and bind, `JAVA_TOOL_OPTIONS` setting `-Dspring.datasource.url=...`, `SERVER_ADDRESS=0.0.0.0` and `SERVER_PORT` set to another port. Pass: the decoy file is byte-identical (SHA-256 before and after, hashed only when no instance holds it) and not locked, the application answers on `127.0.0.1:18080`, nothing listens on the other port or interface, and the data lives in the launcher's directory. The run does not read the decoy through the application.
- Shared database, one worker: (keep the existing last bullet unchanged).

### 2.2 `plan.md` section 3 bullet (line 78, replace)
- E2E runs the jar `backend/target/globerbooks.jar` through the launcher `e2e/support/start-server.mjs` (the `webServer.command`): bind, port and datasource URL are command-line arguments (`--server.address=127.0.0.1 --server.port=18080 --spring.datasource.url=jdbc:sqlite:<file>` inside a fresh 0700 `mkdtemp` directory), the child gets an allowlisted environment (`PATH`, `HOME`, `JAVA_HOME`, `LANG`, `TZ`), readiness is `GET /api/v1/books` returning 200, budget 90 s. The database is exclusively locked while the application runs, so the E2E code never opens it. A run with hostile ambient variables proves a decoy file is untouched (Task 13).

### 2.3 Proposed `tasks.md` Task 13 wording (PM)
- Production list gains nothing; `Tests: e2e/tests/harness.spec.ts` (placeholder plus `@isolation`). Branch `chore/task-13-e2e-harness` (F-20: not `test/`).
- AC2 replacement: "The launcher creates the directory with `mkdtemp` under the real `os.tmpdir()` (0700, direct child, prefix, not a symlink), asserts the database path inside it, passes `--server.address`, `--server.port` and `--spring.datasource.url` as command-line arguments, starts the child with an environment limited to `PATH`, `HOME`, `JAVA_HOME`, `LANG`, `TZ`, forwards termination, deletes only its own directory after the child exited, and never opens the database file."
- New AC5: "With `SPRING_DATASOURCE_URL` (decoy file), `SPRING_APPLICATION_JSON`, `JAVA_TOOL_OPTIONS`, `SERVER_ADDRESS=0.0.0.0` and `SERVER_PORT` set in the shell, the run still serves on `127.0.0.1:18080` from the launcher's directory and the decoy file is unchanged (SEC-F-070)."
- AC3 stays: the observed start order is recorded.

## 3. Wording and rules in AP-7, AP-8 (F-13, F-15(4), F-16, F-19, F-22, F-24)

### 3.1 AP-8 "Live regions" row (F-13; replace)
| Live regions | `announce(text)` replaces the text of the live region with one `textContent` assignment. **Window and oracle for US-013.4 (as CR-5 is worded in handoff 025 F-13, once approved):** a failure causes exactly one replacement between the arrival of its outcome (the response, the network failure or the 15 s abandonment) and the end of its handling. The clearing of the page-level region when the user starts a new action is not part of any failure's update: it happens in the handler of the next user action, before its request is sent, never between a failure's arrival and its text. The mutation-observer test (Task 44 AC4) observes from the arrival of the failure (response fulfilled by the stub, or the clock tick for a timeout) to the end of its handling (the entry or field message and the re-enabled control are present) and counts writes to the live region's text. | US-013.4 |

### 3.2 AP-7.1 new rows after `bookRemoved(id)` (F-15(4); add)
| Event on an entry that a newer load already cleared | `bookReplaced(book)` and `bookRemoved(id)` whose `id` is not in `entries` (an in-flight status, rating or delete when the filter changed and `loadStarted` cleared the entries, or a response after a failed load) change nothing in the list or the count. The announcement of the outcome is still made (page-level region, same text as for a live entry). No focus move happens, and no entry message is written. `bookAdded` is unchanged (ignored unless `phase=loaded`). The `404` path while `phase=loading` does not start a second reload: the load in progress already covers it. |

Test (Task 25, logic): `bookReplaced` and `bookRemoved` on an empty `entries` during `loading` return a state equal to the input, with the announcement flag set; a stub E2E case in Task 29 or 36 (held status response, filter change, then release) shows no entry, the announcement text, and focus unchanged. These cases need the PM to name the owning task (open matter M-4).

### 3.3 AP-8 focus table (F-16, F-19; replace the table and the paragraph under it)
| Dialog result | Focus goes to |
|---|---|
| Duplicate: Cancel or Escape (US-011.8) | Title field (not the opener) |
| Duplicate: Add anyway then 201 (US-011.5) | Title field |
| Duplicate: Add anyway then `LIBRARY_FULL` (US-011.9), network failure or any other non-field failure (US-011.10) | Title field (US-014.2 as changed by CR-1) |
| Duplicate: Add anyway then `400 VALIDATION_ERROR` with entries or `INVALID_STATUS` | the first field in error (US-011.6), which can be Author or Status; US-011.6 prevails over the Title rule |
| Status confirmation: Cancel or Escape | the entry's status select (opener), previous choice restored, no request |
| Status confirmation: Continue | the status select after the request ends (same element, updated in place) |
| Delete: Cancel or Escape | the "Delete <title>" button (opener) |
| Delete: success (US-012.5) | the "Your books" heading |
| Delete: failure other than `404` | the Delete button of the entry (plan rule below) |
| `404 BOOK_NOT_FOUND` on delete, status or rating | **Plan assumption P-F1, not asserted by any test:** the "Your books" heading, because the entry is gone and the list reloads (US-012.7 names no target) |
| Any entry request that ends without removing the entry (success or failure of status or rating) | **Plan assumption P-F2, asserted only as "focus is not lost to the document body" for the status select (US-014.2 names dialogs only):** focus returns to the control used, or to the nearest control of the same entry if that control was replaced |
Because disabling a focused control drops focus, every entry operation remembers the focused control and restores it when the request ends, except where the entry is removed or was already cleared by a newer load (AP-7.1: no focus move). Radio groups and selects fire `change` on arrow keys, so a keyboard user can send several requests in a row; in-flight disabling serialises them (accepted behaviour, risk F6 of handoff 021).

Test consequence (for the PM): Task 37 AC4 cites US-014.2, which speaks of dialogs only. Reword as "Focus returns to the control used after a request that does not remove the entry (plan assumption P-F2; not a spec criterion)". Tests for P-F1 and P-F2 are marked in their title with `[plan]` and are tier P1 unless a change request makes them spec rules (open matter M-3).

### 3.4 Static asset names (F-19(c))
- `plan.md` and AP-8: replace any statement that the page loads `/css/styles.css` or `/js/**` as an asserted fact with: "the page's own stylesheet and scripts are discovered at run time from the document (`link[rel=stylesheet]`, `script[src]`, module imports are not followed); asset names are not hard-coded in tests (spec 5.8)."
- Proposed Task 12 AC4 and Task 14 AC2 wording (PM): "every request the page makes is same-origin, answered 200 from the static catalog with the Content-Type and headers of the constitution, and the set of requests contains no host other than the base origin; asset names are discovered from the loaded document, not hard-coded."
- AP-9.3 `guard`: add "checks the `Content-Type` and security headers of every same-origin response the page received (collected through `page.on('response')`), without a list of expected paths".
- Note: `styles.css` is read by name only by `tokens.test.js` (a logic test that reads the source file from the repository, not an E2E assertion about served names); that stays.

### 3.5 Stale ids and layout (F-22)
- `plan-appendix.md` line 7, replace the whole Task-ids sentence with: "Task ids: all tasks are the numbered tasks of `tasks.md`. The ids of handoffs 021 and 022 (Bnn, Fn.n) survive only in each task's `Source:` line."
- Replace every Fn.n or A/F reference in the file:
| Location | Now | Replace with |
|---|---|---|
| AP-7.3 Concurrency | "(F2.1)" | "(Task 25)" |
| AP-8 last paragraph of the focus rules | "noted in F6" | "risk F6 of handoff 021, accepted" |
| AP-9.1 tree | "pins in F5" | "pins in AP-10" |
| AP-9.1 Tags | "(the measurements of the cut list, F4.5)" | "(the measurements of the cut list, Task 51)" |
| AP-9.2 | "task F0.2" | "Task 13" (the whole open-point bullet is replaced in 2.1) |
| AP-9.3 | "(F4.4)" | "(Task 14 and AP-9.4)" |
| AP-10 Node | "created by whichever of Task 1 or F0.1 comes first; the other task only checks it" | "created by Task 10; Task 11 and Task 14 check it" |
| AP-10 rows | "F0.1", "F0.3", "F0.4", "F3.1" | "Task 10", "Task 11", "Task 10 (MCP pin; confirm against tasks.md)", "Task 43" |
| AP-11.1 item 1 | "as in A2 ... F1.1 paths (A1)" | "as in plan section 3 (resources entry) ... the static paths of Task 09" |
| AP-11.2 US-014.1 row | "(F5) ... F3.1" | "(AP-10) ... Task 43" |
The task numbers for Node, `node --test` and the MCP pin must be confirmed against `tasks.md` by the PM before the replacement is applied; the table gives the numbers the Liaison cited (Task 10 creates `.nvmrc`, Task 43 the checker, Task 13 the harness) and marks the MCP number as unconfirmed. I did not find a task number for "Task 51" in my reading beyond handoff 025's own phrase "Tasks 42, 51" and tasks.md's range 01 to 53: confirm the `@cutlist` task (open matter M-6).
- AP-9.1 tree, `tests/` line: add `harness.spec.ts` (Task 13) first: `harness.spec.ts  us-010-library-view.spec.ts ...`. Under `support/` add nothing.
- Header-reader class (F-22(c), Java side, proposal only): AP-1 `config.sqlite` row gains "`SqliteHeaderReader` (Task 34): plain Java I/O reads the 100-byte header (magic string, page size, file change counter, bytes 18 and 19 journal-mode check, page count) before SQLite opens the file; only `stat`-type and this read touch the file after the claim" and the SEC-F-069 (b) rule. Name and task are the Java architect's to confirm (open matter M-7).

### 3.6 Tiers, AP-9.6 (F-12 side, F-24, US-011.11 typo)
Replace the P0 and P1 rows (the PM regenerates them from `tasks.md` section 7 once D1 to D4 are confirmed; this is the frontend content):
| **P0** | US-010.1, .3 to .8, .10 and the CR-2 clause of US-010.2 (no entry and no count until the load ends); US-011.1 to .10 (.2 is M-1; there is no US-011.11); US-012.1 to .7, .9; US-013.1 to .6; US-014.1, .2, .4 (lang, title, h1, main), .6 (inert text and no CSP violation), .7, .8; SC-38 | (rows unchanged) |
| **P1** | the loading-text part of US-010.2; US-012.8; US-014.3, .5; US-014.6 bounding box (cut list); US-014.4 320 px clause (cut list); US-010.9 1,000 entries (cut list); the two `[plan]` focus assumptions of 3.3 | (rows unchanged) |
Note: `tasks.md` section 7 stays the tier register (F-12); this table is generated from it.

### 3.7 CR-2 follow-through for the page (F-15(1), (2), (3))
No frontend wording change beyond AP-7.1: `bookAdded` keeps its guard (`phase=loaded`). For the spec/PM the frontend states the dependence: the E2E test for US-010.7 (add during a held load) asserts that after the add the book appears with the first list load requested after the add completed, not with the load that was already in flight (needs the PM to adopt F-15(1) in the spec).

## 4. Task 14 README and SEC-F-077
Proposed addition to Task 14 AC3 (PM): "The README states in a visible section that tags `v0.1` and `v0.2` are checkpoints for the Sponsor and not for real data, and that the database file created by these builds is to be deleted before Increment 1 (SEC-F-077, SC-16)." The README is already in Task 14's production list. Its wording is checked by a source test in Task 14 (the README contains the sentence).

## 5. Open matters (not decided here)
| ID | Owner | Matter |
|---|---|---|
| M-1 | Sponsor (Amendment Procedure) | Amend SC-33 in `constitution.md` and the matching text in `security.md`. Proposed wording (handoff 025 F-10, with the launcher rules of 2.1): "a fresh `mkdtemp` directory (mode 0700) created by `global-setup` or, when the web server starts before global setup, by the launcher that is the `webServer.command`; whichever creates it asserts that the datasource path lies inside it, passes bind, port and datasource URL as command-line arguments, starts the application with an allowlisted environment, deletes only that directory after the process exits, and the suite fails when the port is busy; `global-setup` asserts the loopback `baseURL`, the port and the jar." Must be approved before Task 13 starts. Security stated it will reword SC-33 as "`global-setup` or the launcher it delegates to"; the constitution text is the Sponsor's. |
| M-2 | PM and Security | Whether the spec states (US-013.4 / CR-5) the window of 3.1 (Liaison's proposed sentence) or AP-8 drops the separate clearing. I wrote the plan to match the Liaison's proposed text; if the PM words CR-5 differently, 3.1 changes. |
| M-3 | PM / Sponsor | Whether the focus targets after `404` and after a request that removes nothing (3.3, P-F1 and P-F2) stay plan assumptions (no test asserts the exact target) or become a change request (F-19 offered both). |
| M-4 | PM | Owner task and tier for the events-on-cleared-entries tests of 3.2 (suggest Task 25 for the logic, Task 29 for the stub E2E). |
| M-5 | PM / Sponsor | Whether `frontend-testing.md` is updated in Task 43 (add it to the task's file list) or in Task 48; it still lists the checker as ASK FIRST against constitution line 45. |
| M-6 | PM | Confirm the task numbers used in 3.5 (cut-list measurements, MCP pin, `node --test` check). |
| M-7 | Java architect | Name and task of the header-reader class (3.5). |
| M-8 | Sponsor / Security | Whether Task 43 needs the `bypassCSP` context at all is decided by running the scan without it first (1.1); if the scan cannot run without relaxing the application, the Sponsor decides (F-09). |
| M-9 | PM | The sentences in 1.4, 2.3, 3.3, 3.4, 4 are proposals for `tasks.md`; I did not edit it. |

## 6. Not done and limits
Documents only; no browser, no code. I could not verify Playwright 1.64.1 behaviours asserted above (webServer before globalSetup, `page.evaluate` ignoring the page CSP, `bypassCSP` as a per-context option): each is stated as something Task 13 or Task 43 records. I did not edit `plan.md`, `plan-appendix.md`, `tasks.md`, `spec.md`, the constitution or `status.md`. Handoff 025 F-22 also assigned traceability, risk N-6 and AP-1 prose items to other owners; I left those.
