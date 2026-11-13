# Handoff 193: Audit, review chunk Tasks 37 and 38
- From: liaison | To: orchestrator | Type: AUDIT_REPORT | Brief: 191 | Delivery: 192
- Branch `feature/task-37-rating-group`, commits 0c31bdd, d9e8aaf, 4c63de3, 9031665

## Verdicts
| Scope | Verdict |
|---|---|
| Task 37 | ACCEPT WITH FOLLOW-UPS (F-37-1, MINOR) |
| Task 38 | ACCEPT |
| Chunk | ACCEPT WITH FOLLOW-UPS. Security verdict is still required before the Sponsor gate (`Security: per task`). |

## Evidence
- Re-ran `node --test "frontend/**/*.test.js"`: 109 tests, 109 pass, 0 fail (unit tests only). Not re-run: Playwright smoke (4 of 4) and the scripted exploration; both taken from the delivery.
- Read: `entryActions.js` (210 lines), `entryOps.js`, `entryView.js`, `dialogs.js`, `copy.js`, `index.html` diffs, the new tests. All files are under 300 lines. `sources.test.js` is inside the 109 and passes (SEC-F-094 kept, not closed). No `innerHTML`-type sinks added.

## Task 37
| Criterion | Result |
|---|---|
| 1 group, 1 to 5, text value, clear only while rated, PUT/DELETE | Met. Native radios in `role="group"` with `aria-label` "Rating of <title>"; clear visibility from `hasClearButton`; `isRatingChange` and `parseRating` tested incl. bad values. |
| 2 failure returns to last known value, 5.9 text beside entry, INVALID_RATING text | Met. Radios stay on the choice only while busy; `setUi(busy:false)` redraws the server value. Text from `failures.js` literals; F-35-2 case asserts server text and fields are ignored. |
| 3 RATING_NOT_ALLOWED reload; BOOK_NOT_FOUND removal | Met. `reloadEntry` uses `getBook` in try/catch and shows the text after reload; 404 goes to `removeGone` (list reload, "This book no longer exists.", heading focus). Decisions asserted in `failures.test.js`; the UI wiring is shown only by the scripted run. |
| 4 focus to control used | Met, not asserted by design. Exception: F-37-1. |

## Task 38
| Criterion | Result |
|---|---|
| 1 dialog, texts, Cancel focus, Escape/Cancel send nothing and focus Delete | Met. `deleteConfirmText` exact-string tested; `autofocus` on Cancel; the cancel path returns before `send` and calls `restoreFocus('delete')`. |
| 2 success: removal, count, announce, heading focus, empty message | Met. Title is read before removal; `bookRemoved` does count-1 (tested 8 to 7); last delete gives `viewOf` = empty (tested); no list reload on success. |
| 3 failed delete: text, focus Delete; 404 heading | Met. Failure path calls `restoreFocus(key=delete)`; 404 via `removeGone`. |
| 4 no control edits title or author; exploration recorded | Met. None exists; hostile-title and 320 px checks recorded. |

## Cross-checks
- Texts are literals in `copy.js` with exact-string tests; names built by tested pure functions.
- One request per entry: `entryOfEvent` ignores events while `busy`; the group and buttons are disabled while busy; keyboard steps are serialised.
- F-35-1: met. `send` awaits the request inside one try/catch, and `reloadEntry` has its own; a rejection gives the generic text and re-enables controls. Closed in the ledger.
- F-35-2: met, test added. Closed.
- SEC-F-200: fixed (`setUi` deletes the busy record for a gone entry; `removeGone` clears it). It was only a NOTED INFO row, no ledger row.
- Radio fix d9e8aaf: sound. It keeps the choice checked while busy and the outcome sets the server value. This was a real defect found by exploration, not by a unit test.
- Deviation, `entryOps.js` and its tests, plus `copy.js` edits beyond the Task 37 list: accepted. It follows the "pure decisions, logic-tested" rule and was written down in the delivery. `failures.js` needed no edit and none was made.
- Deviation, scripted Playwright run instead of MCP exploration (MCP not exposed): accepted for this chunk; the E2E task 39+40 gives the permanent coverage.
- Chunk branch used instead of `feature/task-38-delete-entry`: accepted as briefed; the squash merge covers both.

## Findings
| ID | Sev | Finding | Resolution |
|---|---|---|---|
| F-37-1 | MINOR | After `RATING_NOT_ALLOWED` the entry reloads as Reading and the rating group is hidden, so the focus given to a radio falls to the page body. This is a plan assumption and not asserted; the delivery's table does not state where focus lands. | Ledger row added (39+40 or a small fix: fall back to the status select). |
| F-37-2 | INFO | The UI wiring (the focus rules, the 404 path, the failure redraw) has no unit test; the proof is the scripted run and the coming E2E 39+40. Same as plan AP-8. | None; E2E must assert criteria 5 and 6 of Task 39+40. |

## Did grouping let a defect through?
- Task 37: no. The radio defect was found by exploration, which a per-task review would not have found earlier either.
- Task 38: no. Task 38 reuses `send`, `removeGone` and `restoreFocus` from Task 37 and shares the same files; separate reviews would have repeated the same read.

## Next
Security review of the chunk (veto), then the Sponsor gate; the orchestrator updates the `status.md` rows. Remind: update `progress.md` (already done in 9031665) and squash-merge with `[Task-37]`/`[Task-38]` subjects. I did not edit code, commit or push.
