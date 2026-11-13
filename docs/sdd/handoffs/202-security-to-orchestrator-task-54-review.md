# 202 Security to orchestrator: Task 54 review

From: security. Type: AUDIT_REPORT. Commit 4bac369 (branch feature/task-54-focus-after-rating-not-allowed). Phase 4, per task.

## Scope and method
Read the diff of 4bac369 (entryActions.js, entryOps.js, tests, e2e spec) and the callers of `restoreFocus`/`failed`/`send`; ran `node --test` on entryOps and sources tests (5 pass). Jar not started: the change is a client-side focus change with no new request, header or server input (not verified on the jar; the E2E in the delivery was not re-run by me).

## Findings
None new.

| Check | Result |
|---|---|
| New sink (innerHTML, outerHTML, insertAdjacentHTML, eval, inline handler, storage) | None in the diff; grep of both files clean. |
| Focus key origin | Only literals: `'status'`, `'delete'`, `'clear'`, `String(rating)` where rating comes from `parseRating` (1 to 5); `focusKeyAfterReload` returns the same key or `'status'`. No server string reaches it. |
| Selector construction | `controlOf` uses fixed class selectors; the only interpolation (`input[value="${key}"]`) is guarded by `/^[1-5]$/`. Status select is found by the fixed class `.entry-status-select`. The entry is found by id through `findEntryItem` (id already validated by `isBookId`). |
| Reload path | `reloadEntry` still calls `getBook` once (pre-existing); the added `restoreFocus` is DOM-only. On failure it falls to `failed(..., 'status')` as before. No extra request. |
| Hostile title | Title never enters a selector or key; it is rendered via textContent (SC rendering control unchanged). |
| Null safety | `used !== null` guard added; final `controlOf(item, target).focus()` is safe because the status select always exists (note: `clear` branch dereferences `clear.hidden` pre-existing; not a security matter). |

Controls: rendering/XSS (SC-XSS family) and focus handling VERIFIED at code and unit level; no change to headers, CSP, API or persistence.

## Not verified
Jar/E2E behavior; browser focus outcome (functional, Liaison scope).

## Residual risks
None added. SEC-F-094 (LOW, extend sources.test.js rules) stays open; unchanged by this task.

## Verdict
PASS. Next step: Liaison verdict, then Sponsor gate.
