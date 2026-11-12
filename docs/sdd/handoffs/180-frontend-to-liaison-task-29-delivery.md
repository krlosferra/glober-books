# Handoff 180: Task 29 delivery (E2E add, validation and duplicate flows)
- From: frontend   To: liaison   Type: TASK_DELIVERY
- Branch: `test/task-29-e2e-add-and-duplicate` (local, not pushed). Commits: see `git log` (tagged `[Task-29]`).

## Resume point
Done: support file, three new specs, smoke 4, progress row, runs below. Next: Liaison and Security review; decision on finding F-29-1 (production focus trap, outside this task).

## Files
Created: `e2e/support/pages.ts`, `e2e/tests/us-011-add-book.spec.ts`, `e2e/tests/us-011-add-failures.spec.ts` (split of the add spec at 300 lines), `e2e/tests/us-011-duplicate.spec.ts`. Edited: `e2e/tests/us-010-library-view.spec.ts` (smoke 4), `docs/sdd/progress.md`. Nothing else; no production code, config or dependency. Jar rebuilt before the run (`mvn -o package -DskipTests`).

## Criteria and carried items to tests
| Item | Test (file, title start) |
|---|---|
| AC1 defaults, focus | add-book: `US-011.1 AC-1` |
| AC1 keyboard add | existing `US-011.2 and M-1` (Task 28 file, @smoke) |
| AC1 client checks, no request | add-book: `US-011.3 AC-1` (5 cases + both fields) |
| AC1 double click, double Enter, one POST (held, counted on the page) | add-book: `US-011.4 AC-1` x2 |
| AC1 confirmation, focus | add-book: `US-011.5 AC-1`, `AC-2` |
| AC2 forbidden character, server message | add-book: `US-011.6 AC-1` x2 |
| AC2 stubbed status entry, INVALID_STATUS, LIBRARY_FULL, 500 (plus network, MALFORMED_REQUEST) | add-failures: `US-011.6 AC-2/AC-3`, `US-011.9`, `US-011.10` |
| AC3 dialog text, focus on Cancel, Escape, Cancel, Add anyway | duplicate: `US-011.7 AC-1` x2, `US-011.8 AC-1`, `AC-2 and smoke 4` |
| AC3 Add anyway ending in LIBRARY_FULL, 500, network, VALIDATION_ERROR | duplicate: `US-011.8 AC-3` x4 |
| Smoke test 4 | library-view: `US-011.7 and .8, smoke 4 ... @smoke` |
| AC4 500 code points, decomposed e acute, leading U+00A0 (T-07, T-08), T-40 | add-book: `T-07` x2, `T-08`, `T-40 via the page` |
| F: Messages updates: 0 field-level, 1 per other failure, 2 with an earlier message (clear + one) | `countMessageUpdates` in add-book, add-failures, duplicate (also 0 for the dialog) |
| F: exactly one POST on double click / double Enter | as above |
| F: Tab stays in the two buttons after a text click | duplicate: `US-014.2 CR-6 ... repeated Tab` (green) and `... Shift+Tab` (finding, below) |
| F: very short viewport | duplicate: `US-014.2: a very short viewport` (320x90) |
| F: literals only, hostile inert, CSP clean | literals in each file; hostile: add-book, add-failures, duplicate; the guard fixture runs in every test |

## Results
- `node --test "frontend/**/*.test.js"`: 86 tests, 86 pass, 0 fail.
- Full E2E run (`npx playwright test`): 69 tests, 69 passed, 0 failed, exit 0 (includes the expected-failure test below).
- New specs run 3 times: 41 passed each time, no flakiness.
- Smoke (`--grep @smoke`): 4 passed.

## Findings and deviations
- F-29-1 (production, not fixed): after a click on non-interactive text inside the dialog, Shift+Tab moves focus to a non-button (active element id empty), because `dialogs.js` wraps only when a button is already focused. Tab from the same start stays inside. Pinned as `test.fail()` in the Shift+Tab test (strict assertion kept; it turns red once fixed, then remove `test.fail()`). Needs a decision: fix in a Task 27 follow-up.
- F-29-2 (observation): at 320x90 the buttons stay visible (focus scrolls the clipped, `overflow: hidden` dialog) but the heading and question text are scrolled out of view, because the UA `max-height` applies. Buttons reachable asserted; no CSS changed.
- The error slot has a visual "Error: " prefix, so the field's accessible description is "Error: <message>"; tests match the message at the end.
- Smoke 4 placed in `us-010-library-view.spec.ts` (current smoke file). Add spec split into `us-011-add-failures.spec.ts` to stay at 300 lines or fewer (all files <= 234).
- Stubbed POSTs take their headers from a real GET of the same URL, so no book is created and the security headers stay real.

## Risks
`countMessageUpdates` counts mutation records of `#messages`; a future change to how the area is written (for example adding an element) changes the count.

## F-29-1 fix
- Commit: `fix(frontend): keep dialog focus on the two buttons [Task-27]` (hash in `git log` and progress.md).
- Change: new pure `frontend/js/logic/focusTrap.js` (`nextTabTarget`) with `focusTrap.test.js` (5 tests); `trapTab` in `frontend/js/ui/dialogs.js` uses it, so with focus on neither button Shift+Tab goes to the last button and Tab to the first (preventDefault). `test.fail()` removed from the Shift+Tab test in `e2e/tests/us-011-duplicate.spec.ts`.
- Results: `node --test "frontend/**/*.test.js"`: 91 tests, 91 pass, 0 fail (sources.test.js green). Jar rebuilt (`mvn -o package -DskipTests` in `backend/`, no errors). Full E2E: 69 passed, 0 failed (the Shift+Tab test now passes normally).
