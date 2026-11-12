# Handoff 178: Security review of Task 28 (E2E library view, filter and states)
- From: security   To: orchestrator   Type: AUDIT_REPORT
- Scope: branch `test/task-28-e2e-library-view`, commits `8951848` (SEC-F-197 fix) and `9efd95d` (E2E tests). Phase 4, per-task review.
- Method: read the diff (`listState.js`, `copy.js`, `failures.js`, `e2e/support/stubs.ts`, `view.ts`, `library.ts`, both specs, `fixtures.ts` unchanged), `node --test "frontend/**/*.test.js"` (86 of 86 pass), greps for ports, paths, secrets and DOM sinks.

## Findings
None new. No SEC-F number opened.

## Items in scope
| Item | Result | Evidence |
|---|---|---|
| SEC-F-197 | CLOSED | `loadFailed` uses `describeFailure(failure).text` only if it is a non-empty string, else the constant `SERVER_FAILED_TEXT` ("Something went wrong on our side. Try again."), a literal in `copy.js` pinned by `copy.test.js`. No text from the response body reaches the state. Only production change in the task. E2E rows for 400 `VALIDATION_ERROR` (no details) and 409 `DUPLICATE_BOOK` (with an `existing_book` object) assert the exact fixed text. The failure text is rendered by the existing `textContent` path; no `innerHTML` or `insertAdjacent*` in `frontend/js`. |
| SEC-F-198 | ACCEPTED as LOW (R-27) | 5,000 maximum-size entries: first load 2.0 s, re-render 1.7 s; 1,000 entries 0.57 s (down from the 5 s estimate; the delivery measures with Playwright polling, so the figure includes overhead). `content-visibility` tried and reverted (no gain). Single user, local data, no confidentiality or integrity effect. Accepted by Security under constitution 1.5; first LOW acceptance of Phase 4; Sponsor reviews at the gate. Task 48 may still optimize. |
| SEC-F-094 | KEPT | `sources.test.js` rules not extended here. The E2E hostile-string tests are now real controls (below). |
| SEC-F-138 | KEPT | The guard (`fixtures.ts`) is unchanged: no `Set-Cookie` or `Location` assertion, foreign-origin check sees http(s) only. |

## Checks requested
- **Stubs keep the real headers.** `fulfillLikeReal` calls `route.fetch()` and `route.fulfill({ response: real, ... })`, so the CSP, nosniff, COOP, CORP, Permissions-Policy and cache headers come from the real server. The guard fixture's `response` listener records every response of the origin, stubbed or not, and `headerProblems` compares CSP and the others by exact equality; the fixture is untouched in the diff, so nothing weakens. `holdLists` fulfils with the real snapshot; `route.abort` produces no response (nothing to check). Residual: a stubbed answer overrides `contentType` to `application/json`, which is what the real server sends.
- **Hostile strings are real.** `US-010.8 T-12` creates `<img onerror>` and `"><script>` strings in title and author through the real API, asserts the exact text is visible, `img, script` count is zero in each entry and in `main`, no dialog fired, and relies on the auto guard for zero CSP violations, console problems and page errors. An inline handler or script that did run would also raise a `securitypolicyviolation`. 500 code points and 200-character authors are included.
- **Secrets.** `grep` for token, password, secret, `process.env` in `e2e/`: no match (only the launcher's existing `TEST_WORKER_INDEX`). Fixed test ids and timestamps are synthetic.
- **Boundaries.** Only port `18080` on `127.0.0.1` appears (`fixtures.ts`, config, global setup, which refuses any other port); `8080` is not referenced; no `/tmp`, `..` or absolute path in the new files; stubs match only `/api/v1/books` GET and let other methods continue. Tests write through the API to the E2E temp database; `library.reset()` runs first and the 5,000-entry test ends with a reset.
- `git status`: clean; nothing created by me outside this handoff and the ledger documents. No probe server was started (test-only change, one frontend logic file covered by the unit and E2E evidence the delivery reports); not re-run: the full E2E suite (delivery reports 27 of 27 and 3 stable runs; I did not repeat the 3-minute run).

## Controls
SC-10 (headers on every asset, stubbed answers included): VERIFIED at E2E code level (reading, not a re-run). SC-39/XSS inert text: VERIFIED by test design (delivery run, not mine). Error text fixed, no server text on the page: VERIFIED at unit level (my run, 86 of 86) and by the E2E rows (delivery run).

## Not verified
The E2E suite was not re-run by Security; the 2.0 s and 1.7 s timings are the delivery's single measurements on a shared machine (it reports 2.9 to 5.2 min total variance).

## Residual risks
R-27 (SEC-F-198), accepted by Security as LOW; the Sponsor may revoke at the phase gate.

## Verdict: PASS
Carried in the ledger (not blocking): SEC-F-094, SEC-F-138. Note for process: I added row R-27 to `security.md` with a one-line scripted insert (diff is exactly 1 line, checked); no other scripted edit.

Next step: Liaison audit, then the Sponsor gate for Task 28; Task 29 next.
