# Handoff 197: Audit, Task 39+40 (E2E status, rating and delete)
- From: liaison
- To: orchestrator
- Type: AUDIT_REPORT

## Resume point
Done: full audit of commits e25f212 and 14a97e1 against the Task 39+40 block, brief 195 and delivery 196. Verdict ACCEPT with one MINOR fix (F-39-1) before the commit is squashed. Next: frontend fixes the one line, orchestrator runs the E2E once (see "Evidence"), Security verdict, Sponsor gate.

## Verdict
ACCEPT Task 39+40 (merged task, one review). Layer 1 (spec compliance), Layer 1b (hygiene) and Layer 2 (tests): met, with F-39-1 below. Layer 3: nothing to report. Security: the reduced check is the orchestrator's (handoff 198); I note only the items in "Security notes".

## Criteria (each asserted, none vacuous)
| # | Result | Evidence in the code |
|---|---|---|
| 1 | Met | Status spec test 1: options list, no rating group on unread or reading, `completed` then date equal to the API's `date_completed` shown through `localDay`, "Not rated", rate 4, replace 2 (the 4 radio unchecked), clear; API read after each step; PUT and DELETE counts. The rating control is asserted absent on the other entry and after moving away (`US-012.2 AC-1`). Smoke 5 is in the smoke file with `@smoke`. |
| 2 | Met | Unread and Reading loops: dialog text literal, Cancel focused, zero PUT before Continue, then rating and date gone in the page and `null` in the API, focus on the select. Cancel and Escape: zero PUT, value restored (awaited), rating still checked, API unchanged. New date and no rating after back and forth. |
| 3 | Met | Same-state test on three entries (rated completed, reading, unread): zero PUT, no dialog, empty message area, list identical to the API list before. US-012.8: text on the changed entry only, count stays 2, zero list GET, the text is gone after the next load. |
| 4 | Met | S-01: the book is moved to reading through the API after the page loaded, then the page rates: message, entry shows Reading, group gone, one PUT, one `GET /books/{id}`, API unchanged. S-05 and S-06 for status, rating and clear: the book is deleted through the API first, then the page acts; message in the page area, entry removed, count 1, exactly 2 list GETs. |
| 5 | Met | Three title kinds: dialog text literal, Cancel focused, no DELETE before confirm, then entry gone, count `1 book`, "Book deleted: <title>", heading focused, one DELETE, one list GET (US-010.10), API 404, no native dialog, no `<script>` in `main`, no browser storage. |
| 6 | Met | Cancel and Escape: dialog hidden, Delete button focused, entry present, no message, zero DELETE, book still in the API. |
| 7 | Met | Book removed through the API, then Delete on the page: message, entry gone, count, heading focused, 1 DELETE, 2 list GETs. Also failed delete (500 and no response): entry kept, message beside the entry, Delete enabled and focused, API still has the book. |
| 8 | Met | Last book: "Book deleted: <title>", "0 books", the empty message literal, heading focused, API total 0. |

Other checks you asked for:
- Literals: no import from `copy.js` or `frontend/` in the two new specs, `requests.ts` or `storage.ts` (grep). Texts match spec US-012.3, .5, .7, .8, US-010.6 and Section 5.9 (compared by reading). The `#status-text` and `#delete-text` ids are production ids (an implementation coupling, accepted: the dialog name and text are the spec part).
- Determinism: no `sleep` or `waitForTimeout` in the new files; web-first assertions throughout; the Escape-restored value is awaited. Request counts are read after a visible state change; the "no request" counts (same-state, Cancel, Escape) are read right after instant-pass assertions, so they depend on Playwright delivering the request event before the next API round trip (it does, and each such test also compares the API state). Stubs go through `fulfillLikeReal` (real headers), the guard fixture runs on every test.
- Stale cases act on the page after changing or deleting through the API: yes, the order is `goto`, API change, page action.
- Locator repairs: `pages.ts` status is now `combobox` named exactly "Status" (the per-entry selects are "Status of ..."), still bound to the add form control; the two `getByText` calls became `shownStatus` (a paragraph whose whole text is the label), which still proves the label is shown, as text and not as the option. Neither weakens the earlier tests; the old text locator was in fact ambiguous after Task 36.
- `expectNoBrowserStorage` on two add-flow tests: a pure addition (local, session, cookie string and context cookies).
- Limits: largest file 271 lines (limit 300); no production code, dependency or config changed (`git diff 071d923..HEAD` shows only `e2e/`, `docs/sdd/handoffs/196` and `progress.md`). Smoke 5 and 6 exist and carry `@smoke`.
- Hygiene: two commits with the `[Task-N]` tags and Conventional Commits; no AI text seen in the diff or the log.

## Findings
| ID | Severity | Location | Finding | Required fix |
|---|---|---|---|---|
| F-39-1 | MINOR | `us-012-delete.spec.ts` line 17 (`longTitle`) | The title is `uniqueTitle('Long')` (13 characters) plus a space plus 480 `L`, so 494 code points, not 500 (checked with node). The test name and the criterion say 500. The slice never cuts. The wrap and focus behaviour is not the boundary of 500, and BR-006 accepts 500. | Build to exactly 500: `const prefix = \`${uniqueTitle('Long')} \`; return prefix + 'L'.repeat(500 - [...prefix].length);` and add `expect([...title]).toHaveLength(500)` in the test. Test-only. |
| F-39-2 | INFO | status spec, tests of the same-state, Cancel and Escape | "No request" is proved by a count read right after assertions that pass at once; a late request would be missed only if it were slower than the API read that follows. | None. Accept. |

## F-37-1 (focus after RATING_NOT_ALLOWED)
Recorded as a test annotation plus a console line, not asserted: correct, because no story names the target and a failing assertion would pin an accident. The annotation lives only in the test report, which is not read by anyone later, so it does not meet "record" on its own. Judgement: the item stays a ledger row, retargeted to the PM. Options for the PM and Sponsor: (a) accept that focus lands on the page body after a stale rating (a keyboard user loses their place; the message is announced in the entry region); (b) one small frontend task (focus falls back to the status select of the reloaded entry) plus one E2E assertion. Recommendation: (b), a few lines in `entryActions.js`, not before the release tag if the Sponsor prefers to accept the debt. I did not make it a task row; that is the PM's. I updated the row in `active-conditions.md`.

## Conditions
- Closed: F-37-2 (INFO, handoff 193; focus rules, the 404 path and the failure redraw are asserted in E2E; it was not a ledger row).
- Already closed: F-35-1 (handoff 193).
- SEC-F-138: Security's item; I did not edit it. Delivered: storage, session storage and cookies asserted empty after the delete flows, smoke 6, the keyboard add smoke and the US-011.5 add test. Still open (partial): `Set-Cookie` and `Location` absence assertions in the guard, the foreign-origin check beyond http(s).
- New: F-39-1 (above, fix on this branch).

## Evidence
- Re-ran here: `node --test "frontend/**/*.test.js"`: 109 tests, 109 passed, 0 failed (matches the delivery).
- Not re-run here: E2E (the full run starts the jar and takes minutes). Taken from the delivery: full run 95 of 95, smoke 6 of 6, new and smoke specs 3 times in a row, 39 of 39 each. The delivery was written before the rebase, but the branch already contained 7a93ecb (Task 37 and 38), so the rebase brought no code change.
- Is an E2E re-run necessary? Yes, once, after F-39-1 is fixed, because that edit changes a test: run the full suite and the two new specs once more (`npx playwright test e2e/tests/us-012*.spec.ts --repeat-each=3` is enough for the repeat). Flakiness judgement: the one 3-failure run (55 s, shared machine) is not reproducible in 4 clean runs and no fixed waits exist; the held-response and counting patterns are sound. I rate it a load artefact, not a test defect, but I keep it as a watch item: if it appears again, the first suspect is the request-count reads in F-39-2. Do not run the suite alongside another job.
- Remaining Layer-2 gate: no coverage rule for E2E; the unit coverage is unchanged (no production change).

## Security notes
No new finding. The stub path keeps the real headers; hostile title and `main script` absence asserted; no secrets in the diff. The rest is the orchestrator's reduced check (handoff 198).

## Next
1. Frontend: fix F-39-1, push nothing until the gate. 2. Orchestrator: E2E run as above, then the Sponsor gate. 3. PM: decide F-37-1 (task or acceptance). On approval: `progress.md` entry and commit `[Task-39] [Task-40] ... - ACCEPTED` as per the usual flow.
