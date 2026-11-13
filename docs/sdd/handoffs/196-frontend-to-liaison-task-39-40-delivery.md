# Handoff 196: Task delivery, Task 39+40 (E2E status, rating and delete)
- From: frontend
- To: liaison
- Type: TASK_DELIVERY

## Resume point
Done: criteria 1 to 8, carried items, smoke 5 and 6. Next: Liaison and Security review, Sponsor gate; the orchestrator rebases onto main after PR 53 merges (not pushed).
Branch `test/task-39-40-e2e-status-rating-delete`. Commits: see `git log` (one tagged `[Task-39]`, one `[Task-40]`, one for progress).

## Files
Created: `e2e/tests/us-012-status-rating.spec.ts`, `e2e/tests/us-012-delete.spec.ts`, `e2e/support/requests.ts` (request tracking, failures with real headers), `e2e/support/storage.ts` (SEC-F-138 helper).
Edited: `e2e/support/view.ts` (entry controls, count, local date, shown status), `e2e/support/library.ts` (`find`, `date_completed`), `e2e/support/pages.ts` (add-form Status locator), `e2e/tests/us-010-library-view.spec.ts` (smoke 5 and 6, storage check, locator repair), `e2e/tests/us-011-add-book.spec.ts` (storage check), `docs/sdd/progress.md`. Nothing else; no production code, no dependency, no config change.

## Criteria to tests
| Item | Tests (title starts with) |
|---|---|
| 1 | `US-012.1, .2, .4, smoke 5` (status spec, full flow with hostile title; smoke file has the compact @smoke version); `US-012.2 AC-1`; `US-012.4` keyboard |
| 2 | `US-012.3 AC-1, S-02` (Unread and Reading, Continue, values from the real API); `US-012.3 AC-2, S-03` (Cancel and Escape, no PUT); `US-012.3, S-03` new date and no rating; no dialog when unrated |
| 3 | `US-012.3 and S-04, S-11` (no request, API unchanged); `US-012.8` (mismatch text, count, no list request) |
| 4 | `US-012.7, S-01` (RATING_NOT_ALLOWED, entry reloaded, one GET of the book); `US-012.7, S-05, S-06` x3 (status, rating, clear on a deleted book: message, entry removed, 2 list GETs) |
| 5 | `US-012.5 AC-1, smoke 6` x3 (plain, hostile, 500 code points); smoke file `US-012.5, smoke 6` |
| 6 | `US-012.5 AC-2` Cancel and Escape |
| 7 | `US-012.7, S-06` (stale delete: message, entry removed, 2 list GETs, heading focused) |
| 8 | `US-010.6 AC-1` last book (empty message, "0 books") |
| F-37-1 | observation only, see below |
| F-37-2 | focus rules asserted in the tests of criteria 5 to 7, status Continue/Cancel/Escape focus on the select; failed delete 500 and no response: entry kept, message beside it, focus on Delete |
| SEC-F-138 | `expectNoBrowserStorage` in delete flows (all), smoke 6, keyboard add smoke, US-011.5 add test |
| F-1 to F-3 | literals only; guard fixture on every test; stubs through `fulfillLikeReal` with real headers; requests counted on the real page |

## Results
- `node --test "frontend/**/*.test.js"`: 109 tests, 109 passed, 0 failed.
- Full E2E (`cd e2e && npx playwright test`): 95 tests, 95 passed, 0 failed, exit 0. Smoke (`--grep @smoke`): 6 passed, exit 0.
- New and smoke specs (us-012 and us-010-library-view, 39 tests) run 3 times in a row: 39 passed each time.

## Observations
- F-37-1: after `RATING_NOT_ALLOWED` focus lands on `body` (the reloaded entry is Reading and the rating group is hidden). Not asserted; written to the test annotation. Fallback to the status select is a frontend change if wanted.
- The 3rd run of an earlier loop showed 3 failures in a 55 s run while another job shared the machine; not reproduced in the 3 clean runs nor the full run.

## Deviations
- Two pre-existing Task 28/29 locators broke on this branch because Task 36 added per-entry status selects and a "Confirm status change" dialog: `getByLabel('Status')` in `support/pages.ts` is now `getByRole('combobox', { name: 'Status', exact: true })`, and two `getByText('Reading'/'Unread')` in the library view spec use `shownStatus`. Test-only repair, no production change.
- Storage assertions touch two existing add-flow tests (outside the listed spec files) as SEC-F-138 asked.
- Full-flow tests in the new specs carry no `@smoke` tag; the smoke versions live in the smoke file as in Task 29.

## Risks
- Same-state "no request" is checked with request counts on a Playwright `selectOption`, which dispatches change events; the guard is `isChange` in production.
- Escape-restored value is awaited before assertion, as the Task 36 note says.
