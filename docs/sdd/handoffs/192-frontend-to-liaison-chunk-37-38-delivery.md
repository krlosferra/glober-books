# Handoff 192: Delivery, Tasks 37 and 38 (chunk)
- From: frontend | To: liaison | Type: TASK_DELIVERY | Brief: 191

## Resume point
Done: Tasks 37 and 38 built, committed locally on `feature/task-37-rating-group` (not pushed): `0c31bdd` [Task-37], `d9e8aaf` [Task-37] fix, `4c63de3` [Task-38]; plus the commit holding this handoff and `progress.md`. Next: Liaison audit, Security review, Sponsor gate, then Task 39+40 (E2E). `status.md` rows left for the orchestrator.

## Files
Edited: `frontend/index.html`, `css/styles.css`, `js/ui/entryView.js`, `js/ui/entryActions.js` (210 lines), `js/ui/dialogs.js`, `js/logic/copy.js`, `copy.test.js`, `failures.test.js`, `docs/sdd/progress.md`. Created: `js/logic/entryOps.js` and `entryOps.test.js` (pure names, `parseRating`, `isRatingChange`, `hasClearButton`). `failures.js` needed no edit: its `INVALID_RATING`, `RATING_NOT_ALLOWED` and `BOOK_NOT_FOUND` decisions already matched. Nothing else changed.

## Task 37
| Criterion | Unit test | Exploration (scripted run) |
|---|---|---|
| 1 group "Rating of <title>", 1 to 5 by keyboard, value as text, clear only while rated, PUT/DELETE | `entryOps.test.js` (names, parseRating, isRatingChange, hasClearButton), `copy.test.js` | rating 4 set, text "Rating: 4 of 5"; ArrowRight moved to 5 and sent PUT; Tab reached "Clear rating of Alpha"; clear gave "Not rated", button gone; no group on a reading entry |
| 2 failure returns to last known value, 5.9 text beside entry; `INVALID_RATING` text | `failures.test.js` (F-35-2 case added) | INVALID_RATING, 500, non-JSON 500, network: text beside entry, radios reverted (none checked), controls enabled, Messages area untouched |
| 3 `RATING_NOT_ALLOWED` text and entry reload; `BOOK_NOT_FOUND` removal, list reload, announce | `failures.test.js` | stubbed: text shown, entry reloaded as Reading, group gone; 404: one list reload, "This book no longer exists." in Messages, focus on "Your books" |
| 4 focus returns to the control used | not asserted | focus stays on a radio after set, keyboard change, clear (first radio when the clear button is gone) and failures |

## Task 38
| Criterion | Unit test | Exploration |
|---|---|---|
| 1 dialog "Delete book", text, Delete/Cancel, focus on Cancel; Escape and Cancel send nothing, focus Delete button | `copy.test.js` (`deleteConfirmText`) | initial focus Cancel; Escape and Cancel: no request, count unchanged, focus on "Delete Epsilon" |
| 2 success: entry gone, count, "Book deleted: <title>", focus heading, last delete empty message | `entryOps.test.js` (`deletedText`) | 7 to 6 books, announced, focus "Your books"; deleting all showed the empty message and "0 books" |
| 3 failed delete: 5.9 text, focus on Delete button; 404 heading | `failures.test.js` | 500 and network: text beside entry, focus Delete button, enabled; 404: announce, reload, focus heading |
| 4 no control edits title or author | none | none exists; hostile title `<img ...>` inert in group name, dialog and entry; 500-code-point title fits the dialog; scrollWidth 320 at 320 px; no console CSP violations |

## Tests
`node --test "frontend/**/*.test.js"`: 109 tests, 109 passed, 0 failed, exit 0. `npx playwright test --grep @smoke` from `e2e/`: 4 tests, 4 passed, 0 failed, exit 0 (own run on 18080, no process left). Exploration: Playwright browser tools not exposed, so a scripted run (documented deviation) with the pinned `@playwright/test` and `/opt/pw-browsers/chromium` outside the repository, on port 18931 against a jar built from this tree, 0700 temp dir, stopped afterwards.

## Open items
F-35-1: every request is awaited in try/catch; a rejection shows the generic text and leaves controls enabled. F-35-2: case added. SEC-F-200: `setUi` no longer creates a busy record for an entry that is gone, and the delete path clears it. SEC-F-094: `sources.test.js` passes.

## Deviations and risks
- New files and `copy.js` edits beyond the Task 37 list, as binding constraint 1 and "pure decisions" require.
- Found by exploration and fixed: refreshing during a request unchecked the chosen radio; now the choice stays checked while busy and the outcome sets the server value (separate commit).
- Task 38 branch in `tasks.md` is `feature/task-38-delete-entry`; chunk branch used as briefed.
- While busy the group is disabled and focus is restored to the radio used after the request; arrow keys send one request per step, serialised (accepted, plan AP-8).
