# Handoff 188: Task delivery, Task 36 (entry view, status control and confirmation)
- From: frontend
- To: liaison
- Type: TASK_DELIVERY
- Task: 36; branch `feature/task-36-entry-status`; commit: see the Resume point (one commit tagged `[Task-36]`, local, not pushed)

## Resume point
Done: all four criteria implemented and explored. Next: Liaison and Security review; Task 37 mounts the rating group in `.entry-rating-mount` (empty, hidden unless completed).

## Files
Created: `frontend/js/ui/entryView.js`, `frontend/js/ui/entryActions.js`, `frontend/js/logic/statusChange.js`, `frontend/js/logic/statusChange.test.js`.
Edited: `frontend/index.html` (template: select, mismatch note, rating mount, per-entry status slot; `status-dialog`), `frontend/css/styles.css`, `frontend/js/logic/copy.js` (strings and `confirmStatusText`), `frontend/js/ui/dialogs.js` (shared `runDialog`, `confirmStatusChange`; duplicate dialog behaviour unchanged), `frontend/js/ui/listView.js` (entry rendering moved to `entryView.js`, `render(state, uiOf)`), `frontend/js/ui/app.js` (mount), `docs/sdd/progress.md`. `failures.js` untouched, so F-35-2 not triggered. Nothing else changed.

## Criteria
| Criterion | Unit test | Exploration (scripted run) |
|---|---|---|
| 1 control "Status of <title>", Unread/Reading/Completed, PUT, returned values, disabled in flight | `statusChange.test.js` (isChange, name, strings) | Names present; options `Unread, Reading, Completed`; change sent `PUT .../status {"status":"reading"}`; entry showed Reading; select disabled during a delayed response; completed entry showed "Completed: <date>" and "Not rated" from the server; focus stayed on the control |
| 2 confirmation, exact text, Cancel focus, Cancel without request | `statusChange.test.js` (needsConfirmation, exact text) | Rated entry to Unread: dialog "Confirm status change", text exact, buttons Cancel, Continue, focus on Cancel, Tab wraps Cancel/Continue; Cancel and Escape: no PUT, previous value restored, focus back on the select; Continue: one PUT, rating and completion date gone; unrated completed entry changed with no dialog |
| 3 rating control only on completed | (mount point logic is in the view) | Mount visible only on the two completed entries; group itself is Task 37 |
| 4 "No longer matches this filter" | `matchesFilter`/`bookReplaced` tests exist from Task 25 | Filter Reading, entry changed to Completed: stayed, note shown, count unchanged ("2 books"); note gone after switching filter |
| F-35-1 | rejection path in `send` | Stubbed 500: "Something went wrong on our side. Try again." beside the entry, value restored, control enabled; network abort: "Cannot reach the server..." beside the entry; 404 stub: entry removed, "This book no longer exists." in Messages, focus on "Your books" heading, list reloaded |
| Hostile and long titles | | `<img src=x onerror=alert(1)>` shown literally in names and dialog text (0 img elements); 500-character title: no horizontal scroll at 320 px; console showed only resource errors from stubbed responses, no CSP violation |

## Results
- `node --test "frontend/**/*.test.js"`: tests 103, pass 103, fail 0 (exit 0); the 4 smoke tests pass separately (107 was the sum).
- `npx playwright test --grep @smoke` from `e2e/`: 4 tests, 4 passed, 0 failed, exit 0.

## Deviations
1. **Control is a `<select>`, not a radio group.** The brief says radio group; plan AP-8 and its focus table say `select` with `aria-label`, and spec US-012.1 only says "single-choice". The plan outranks the brief, so I followed the plan. Switching to radios later is a view-only change; no E2E exists yet (Tasks 39 and 40 should use role combobox).
2. Exploration was a scripted run (documented deviation): the Playwright browser tools are not exposed to my run. Real page, pinned `@playwright/test`, `/opt/pw-browsers/chromium`, jar built from this tree, port 18911, temp data dir mode 0700, all processes stopped; script kept outside the repository.
3. Success of a status change is not announced (no spec text exists); 404 on status is handled here (removal, reload, announcement) because the same request can return it; `RATING_NOT_ALLOWED` reload of the entry is not built (unreachable for status; Task 37).
4. Focus after a request returns to the select only if focus was on it or lost to the body (it never steals focus from another control); Playwright's `selectOption` does not focus, so a test must focus the select first.
5. `entryActions.js` is split into module functions with a context object to keep functions under 50 lines.

## Risks
- A list re-render during a request (an add while a status change is in flight) keeps the disabled state through the `uiOf` registry, but the select element is then a new one; focus restore finds it by id.
- Escape closes the dialog before the `close` event: a test that reads the restored value must wait for it.
