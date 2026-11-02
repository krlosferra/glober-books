# 050 Security to Orchestrator: Task 11 review

From: security. Type: AUDIT_REPORT. Branch: feature/task-11-logic-and-client-thin (worktree /home/user/gb-task11). Phase 4.

## 1. Scope and method
Read frontend/js/api/http.js, api/booksApi.js, logic/ids.js, copy.js, format.js, their tests and sources.test.js (delivery handoff 048 is named 048-frontend-to-liaison-task-11-delivery.md). Ran `node --test` (Node 24.22.2) on the 43 tests: 43 pass, 0 fail (running the bare directory `node --test frontend/js` reports 1 failure because Node treats the directory as a module; use the file globs). Seeded violations on a scratch copy outside the repo (the repository is unchanged).

## 2. Findings

| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-094 | LOW | Static gate strength | frontend/js/sources.test.js | The regex scan is a text heuristic. Seeded on a scratch copy: `el['inner'+'HTML']` caught; `globalThis['fe'+'tch']` caught; NOT caught: template-literal key ``o[`outer${'HTML'}`]``, `window[['ev','al'].join('')]`, `Reflect.get(globalThis,'local'+'Storage')`, `window.open(x)` (also `location`, `createContextualFragment`, `DOMParser`, `setAttribute` of on*/href/src are not in RULES although listed in SC-09) | Seed run, 4 of 5 lines passed the gate | Only a deliberate or careless author could evade it; no sink exists today (manual read: none) | Add rules for `createContextualFragment`, `DOMParser`, `window.open`, `\blocation\b`, `setAttribute\(\s*["'`](on|href|src|formaction)`, `Reflect\.`, `globalThis\[`/`window\[` any bracket; keep review plus the E2E XSS test (Task 13+) as the real control. Not blocking | SC-09 |
| SEC-F-095 | LOW | Server text | http.js parseFields | `fields[].message` (server text) is passed through to the caller unmodified. Safe now (nothing renders it), but Task 12 must render it with textContent only. Unknown `code` values and non-JSON bodies collapse to `unreadable`, so server `error` text is never carried at all (good) | http.js lines 24-30, 54-58 | None until rendered | Carry as a condition on Task 12: textContent only, E2E payload in details | SC-09 |
| SEC-F-096 | INFO | Timeout | http.js | Timeout and caller abort both resolve NETWORK via Promise.race even when the body read stalls after headers; the timer is cleared in finally; listener removed. No defect | code read, http.test.js lines 126-161 | | | SC-10 |
| SEC-F-097 | INFO | Not exercised | booksApi.js | Only listBooks and addBook exist; per-book calls (bookPath, DELETE without body) are not yet wired. DELETE Content-Type is satisfied by the `method !== 'GET'` branch in requestJson. Re-verify when the PATCH/DELETE wrappers land | code read | | Re-check with a test for DELETE headers at that task | SC-10 |

No CRITICAL or HIGH findings.

## 3. Controls checked
- SC-09 (Task 11 scope): VERIFIED by manual grep and read of all production files: no innerHTML family, eval, new Function, string timers, dynamic import, inline handlers, storage, location reads, external URLs, setAttribute. Gate is a heuristic (SEC-F-094). Rendering and the E2E payload test remain for later tasks, so SC-09 stays IN PROGRESS.
- SC-10 client side: VERIFIED. Paths are root-relative constants (`/api/v1/books` plus a fixed `?status=` from an allowlist Set); `Accept: application/json`; `Content-Type: application/json` on every non-GET; `redirect: 'error'`, `cache: 'no-store'`, `credentials: 'omit'`; the only fetch call is in api/http.js (gate enforced and tested).
- BR-011: VERIFIED. `isBookId` is a strict lowercase hyphenated UUID regex with a string type check, `bookPath` throws before building the URL and also applies encodeURIComponent. Filter is allowlisted so no query injection.
- Mass assignment: addBook copies only title, author, status and `allow_duplicate === true`.
- Response handling: bodies are parsed defensively (toBook type-checks all seven members; DUPLICATE_BOOK with a bad existing book becomes unreadable); frozen NETWORK/UNREADABLE results.

## 4. Not verified
No browser or rendering run (no UI yet); no server probe (frontend only); no `npm audit` (no dependency changes reviewed; none seen in this branch scope).

## 5. Residual risks
None needing Sponsor acceptance. SEC-F-094 is a hardening item.

## 6. Verdict
PASS WITH CONDITIONS (non-blocking, none gate the merge):
1. Strengthen the sources.test.js rules per SEC-F-094 (this task or the next hardening change).
2. Task 12 must render `fields[].message` and all book strings with textContent only (SEC-F-095) and add the E2E payload check.
3. Re-verify DELETE/PATCH wrappers when added (SEC-F-097).

## Appendix: exact text for security.md (not applied)
Findings log rows:
- `| SEC-F-094 | LOW | sources.test.js regex gate misses template-literal keys, array join, Reflect.get, window.open, location, DOMParser, createContextualFragment, setAttribute | OPEN: extend rules; E2E XSS test is the real control | 050 |`
- `| SEC-F-095 | LOW | fields[].message passed through by the client; Task 12 must use textContent only | OPEN: condition on Task 12 | 050 |`
- `| SEC-F-096 | INFO | Timeout and abort verified (race, timer cleared, listener removed) | CLOSED | 050 |`
- `| SEC-F-097 | INFO | DELETE/PATCH wrappers absent; Content-Type rule covers non-GET; re-verify when added | OPEN | 050 |`
Control rows: SC-10 client half VERIFIED at Task 11 (relative URLs, headers, redirect error, no-store, omit credentials); SC-09 gate present, status IN_PROGRESS pending rendering tasks.
Changelog: `0.22 (2026-11-02): Task 11 review (handoff 050): SEC-F-094 to SEC-F-097. Verdict PASS WITH CONDITIONS.`
