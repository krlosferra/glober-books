# Handoff 176: Task 28 delivery (E2E library view, filter and states)
- From: frontend   To: liaison   Type: TASK_DELIVERY
- Task: 28. Branch: `test/task-28-e2e-library-view`. Commits: `8951848` (SEC-F-197 fix) and the commit that adds this file (`test(e2e): ... [Task-28]`, see `git log`). Local only, not pushed.

## Resume point
Done: everything in the brief. Next: Liaison review; Task 29 (note below); Task 48 for SEC-F-198.

## Files
- Created: `e2e/support/stubs.ts`, `e2e/support/view.ts`, `e2e/tests/us-010-library-states.spec.ts`.
- Edited: `e2e/support/library.ts` (setStatus, rate, createMaxSize), `e2e/tests/us-010-library-view.spec.ts`, `frontend/js/logic/listState.js` and `listState.test.js` (SEC-F-197), `docs/sdd/progress.md`.
- Nothing else changed (`styles.css` was tried and reverted).

## Criteria to tests
| Criterion | Test (title prefix) |
|---|---|
| 1 titles, one h1, list name | `US-010.1 AC-1` (existing smoke) |
| 1 loading text in a status region | `US-010.2 and .3 AC-1` (held first load) |
| 1 local date, far zones | `US-010.3 AC-1` for Pacific/Kiritimati and Pacific/Pago_Pago (stubbed fixed UTC values) |
| 1 entry content, rating texts | `US-010.2 and .3 AC-1`, `US-010.3 AC-3` (real API) |
| 1 hostile strings, 500 code points | `US-010.8 T-12` (plus smoke `US-014.6 T-12`) |
| 2 order, count texts | `US-010.4 AC-1`; "1000 books" in `US-010.9 SEC-F-198` |
| 2 filter options and parameter | `US-010.5 AC-1` |
| 2 empty, filter-empty, Show all books | `US-010.6 AC-1`, `US-010.6 AC-2` |
| 3 failure texts and Try again, no list item | `US-010.7 AC-1` x7 (500, unreadable, no response, FORBIDDEN, MALFORMED_REQUEST, VALIDATION_ERROR without details, DUPLICATE_BOOK on a list), `US-010.7 AC-2` (active filter) |
| 3 add during held load / after failure | `US-010.7 AC-3` x2 |
| 4 overlapping loads | `US-010.10 AC-2` (both release orders); `US-010.10 AC-1` (single request per action, add does not reload) |

Liaison F-1 to F-3: status region `#list-status`; request counts asserted on the real page (held-load controller records every list GET); expected strings are literals from the spec; the guard fixture (auto, every test) asserts zero CSP violations, console problems, page errors, foreign origins and headers of every asset. Stubbed answers keep the real headers. The Tab and Shift+Tab test after a click on the dialog text is NOT here: it belongs to Task 29.

## Results
- `node --test "frontend/**/*.test.js"`: 86 tests, 86 pass, 0 fail.
- Full E2E (`npx playwright test`, jar rebuilt): 27 passed, 0 failed, exit 0 (3.1 min, includes the 5,000-entry test).
- Flakiness: the new specs (25 tests without the long one) run 3 times: 25/25, 25/25, 25/25.

## SEC-F-198 measurement (reference machine of this session, seeding through the API)
5,000 maximum-size entries (title 500, author 200): first load 2.0 s; re-render after a filter change 1.7 s (above 1 s); 1,000 entries all present 0.57 s after the load. Tried `content-visibility: auto` with `contain-intrinsic-size: auto 10rem` on `.entry`: no help (measured 3.4 s first load, 3.4 s re-render, 2.2 s for 1,000, in a noisier run) so it was reverted. Left to Task 48. Times come from Playwright polling and include its overhead.

## Deviations
1. Production fix in `listState.js` for SEC-F-197 (as the brief allows). It falls back to `SERVER_FAILED_TEXT` when the failure text is not a non-empty string.
2. Two extra files outside the brief's list (`e2e/support/view.ts`, `e2e/tests/us-010-library-states.spec.ts`) because one spec would exceed 300 lines.
3. The 5,000-entry test is part of the default suite (about 3 minutes); consider tagging it if that is too slow.

## Risks
- The perf figures are single measurements on a shared machine and vary (2.9 to 5.2 min for the same test).
- `#book-list > li` and `#count` ids are used in a few assertions where no accessible locator exists (hidden elements, bulk count).
