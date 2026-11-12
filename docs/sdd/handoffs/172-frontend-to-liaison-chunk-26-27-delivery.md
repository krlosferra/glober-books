# Handoff 172: Delivery, Tasks 26 and 27 (add form errors and in-flight guard; duplicate dialog)
- From: frontend | To: liaison | Type: TASK_DELIVERY
- Branch: `feature/task-24-text-failure-logic` (chunk branch), not pushed. Commits: bc3bbec (Task 26), 4958f10 (Task 27), 63ee788 (Task 27, Tab wrap); a fourth commit holds this handoff, status and progress.

## Resume point
Done: both tasks built, unit tests green, scripted exploration done, smoke run. Next: Liaison and Security review; Task 29 holds the E2E tests. Nothing pending.

## Files
Created: `frontend/js/ui/messages.js`, `frontend/js/ui/dialogs.js`, `frontend/js/logic/addCheck.js` and `addCheck.test.js`. Edited: `frontend/js/ui/addForm.js`, `frontend/js/logic/copy.js` and `copy.test.js`, `frontend/index.html`, `frontend/css/styles.css`, `frontend/js/api/http.js` and `http.test.js`. Nothing else.

## Task 26 criteria
| Criterion | Unit test | Exploration (scripted) |
|---|---|---|
| 1 inline message, linked, no request | `addCheck.test.js` (empty, whitespace, too long, per field) | Empty: 0 POSTs, "Enter a title." and "Enter an author.", `aria-describedby=title-error`, `aria-invalid=true`, focus Title; 501 chars: "The title can have at most 500 characters.", 0 POSTs |
| 2 in-flight guard | none (handler logic; Task 29) | Held response, click + Enter + forced click: 1 POST, button disabled; after release enabled, focus Title |
| 3 VALIDATION_ERROR, INVALID_STATUS | `http.test.js` (status entry without message kept), `failures.test.js` (Task 24) | Stub: title and author server messages beside fields, fixed Status text, focus Title (first), values kept, Messages empty; INVALID_STATUS: Status text, focus Status |
| 4 LIBRARY_FULL and other failures | `copy.test.js` exact strings | LIBRARY_FULL exact text; 500, network, non-JSON texts of 5.9; focus Title; values kept |
| 5 one update per failure | none | Source: one `textContent` write per failure (`messages.show`); clear is a separate call at submit start. Observer records per failure cycle: 2 (the clear plus the write) |

## Task 27 criteria
| Criterion | Unit test | Exploration (real duplicate, real jar) |
|---|---|---|
| 1 dialog content, Cancel focus, Escape | `copy.test.js` strings | Dialog "Possible duplicate", text, Dune, Frank Herbert, Unread, "Added: 2026-11-12"; focus on Cancel; Escape closes, focus Title, values kept, no extra POST |
| 2 Cancel | none | Focus Title, values kept; outside click leaves the dialog open (no other close path) |
| 3 Add anyway | none | Body has `"allow_duplicate":true`, one extra POST, form cleared, focus Title, 2 items; then LIBRARY_FULL / 500 / network (stubbed): dialog closed, focus Title, message shown; VALIDATION_ERROR: focus Author, field message |
| 4 inert and contained | none | Hostile text, 500-code-point word, 55 x (e + 8 marks): no injected elements, children and buttons inside the dialog box, no hscroll at 320 px; console shows only network status lines (404 favicon, stubbed 4xx/5xx), no CSP violation |

## Results
`node --test "frontend/**/*.test.js"`: 85 tests, 85 passed, 0 failed, exit 0. Smoke `npx playwright test --grep @smoke`: 3 tests, 3 passed, 0 failed, exit 0.

## Deviations
1. Exploration is a scripted run (browser tool server not exposed): real jar plus a throwaway static proxy serving the current `frontend/` with the jar's CSP (also rewriting Origin/Host to the jar, else POST is refused); pinned `@playwright/test`, `/opt/pw-browsers/chromium`; temporary directory mode 0700, ports 18931/18932; all processes stopped, scripts outside the repository.
2. `api/http.js` and its test edited (allowed by the brief): `parseFields` keeps a `status` detail without a string message (message becomes '').
3. New `logic/addCheck.js` (pure client check, unit-tested) is not in the task file lists; `copy.js` also got the dialog strings in the Task 26 commit.
4. Added a Tab/Shift+Tab wrap in `dialogs.js`: in the headless browser a bare native modal moved focus to the document after the last button, which CR-6 forbids.

## Findings and risks
- The smoke suite runs against the jar's baked-in frontend, so it did not exercise this code; Task 29 or a jar rebuild is needed for E2E evidence.
- Field errors (VALIDATION_ERROR, INVALID_STATUS, client checks) do not write to the "Messages" area (US-013.4: field messages stay beside the field); the brief's "every failure writes Messages once" is applied to page-level failures only. Confirm for Task 29's CR-5 observer.
- Field messages are not live regions; they are announced through `aria-describedby` when focus moves to the field.
- The button stays disabled while the duplicate dialog is open and until its outcome.
- Task 25 findings stand: focus after "Try again" and "Show all books" goes to the "Your books" heading; status region id is `#list-status`.
- SEC-F-095/132: server field messages render via `textContent` only; `sources.test.js` passes (SEC-F-094).
- A `::before` "Error: " prefix on field errors is CSS generated text (colour not the only signal); not in the DOM text.
