# Handoff 182: Security review of Task 29 (E2E add, validation, duplicate) and the F-29-1 fix
- From: security   To: orchestrator   Type: AUDIT_REPORT
- Scope: branch `test/task-29-e2e-add-and-duplicate`, `git log origin/main..HEAD` (6 commits): `e2e/support/pages.ts`, three `us-011-*` specs, smoke 4, and the Task 27 follow-up `frontend/js/logic/focusTrap.js` (+ test), `frontend/js/ui/dialogs.js`. Phase 4, per-task review.
- Method: read the full diff and the guard fixture; grep for sinks, ports, secrets, storage; `node --test "frontend/**/*.test.js"` (91 of 91 pass). No jar probe: the dialog is client-side only and no server code changed (see Not verified).

## Findings
None new. No SEC-F number opened.

## Checks requested
| Check | Result | Evidence |
|---|---|---|
| Focus trap keeps focus in the modal, no new sink | OK | `nextTabTarget` is a pure function of two inputs (`shift`, `'first'/'last'/'other'`); `trapTab` only calls `preventDefault` and `.focus()` on the two fixed buttons. No `innerHTML`, `outerHTML`, `insertAdjacent*`, `eval`, `new Function` in `frontend/js` (grep). Nothing server-controlled enters the function. Native `showModal` still makes the rest of the page inert. |
| Dialog cannot close without an outcome | OK | `dialogs.js` unchanged here: closes only through Confirm (`'confirm'`), Cancel (`'cancel'`) or the native Escape (returnValue stays `''`); the promise resolves true only for `'confirm'`. Tests: Escape and Cancel keep values, add nothing (one POST, total 1). |
| Stubbed POSTs: real headers, no book | OK | `fulfillLikeReal` does `route.fetch` with method GET (no body) and fulfills with that response's headers; a GET creates nothing. Every test asserts `library.list().total` unchanged. Guard fixture (`headerProblems`, CSP) still runs on the fulfilled responses. `failAdds(skip=1)` lets only the first duplicate-producing POST reach the server. |
| Hostile strings, one POST, zero CSP violations | Real | XSS: `SCRIPT_TITLE`, `<b>bold</b>`, `<img onerror>` as title, author and server message; asserts no `dialog script, dialog b` nodes and zero alert dialogs. One POST counted on the page (`page.on('request')`, `holdAdd` for double click/Enter). Auto `guard` fixture asserts zero `securitypolicyviolation`, no CSP console problems, no page errors, no foreign origin, headers of every asset. |
| No secrets, port 8080, outside paths | OK | Specs use `BASE_ORIGIN` 127.0.0.1:18080 only via the existing fixture; no 8080, no env, no file paths, no tokens in the diff. |
| "Add anyway" body carries nothing from the server | OK | `booksApi.addBook` builds the body from the form input and adds only `allow_duplicate: true`; the test pins `[undefined, true]` and `toMatchObject` of the typed values. The existing-book data from the 409 is only used for the dialog text (`setText`). |

## Controls
SC-19/SC-20 family (DOM output through `textContent`, CSP): VERIFIED at the E2E level on the real page against the jar launcher (Task 29 run: 69 of 69 reported by the delivery; I did not rerun the browser suite). Dialog and add form: unchanged status.

## Open items kept
- SEC-F-094 (LOW): kept; the E2E XSS tests now cover title, author, dialog and server messages.
- SEC-F-138 (LOW): kept and widened: no assertion that local storage, session storage and cookies stay empty after the add flows (checked once in handoff 174; the guard sees no `Set-Cookie`/`Location`).
- Residual, INFO: the guard's violation mark is a `console.log` string, so page text printing it could forge a violation entry (fails the test, never hides one). No action.

## Not verified
Browser E2E suite not rerun by me (Playwright with the launcher uses 18080, reserved); jar not started, since only frontend files and tests changed. Delivery's counts (91 unit, 69 E2E) are the Frontend's; I reran the 91 unit tests only.

## Residual risks
None for the Sponsor. No LOW/INFO accepted by Security in this review (0 of 5 this phase for this task).

## Verdict: PASS
Next: Liaison verdict is already on record (handoff 181); the task may go to the Sponsor gate. Files edited by me: this handoff, `security-log.md` (0.59), `active-conditions.md` (SEC-F-138 row, closed line), `status.md` (Security column). Nothing committed.
