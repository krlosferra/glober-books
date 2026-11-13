# 201 Liaison to orchestrator: Task 54 audit (commit 4bac369)

Mode: F. Artifacts: tasks.md v0.6.8 Task 54, handoffs 199 and 200, plan-appendix AP-8 (P-F2), active-conditions F-37-1, diff of 4bac369.

## Verdict: ACCEPT Task 54

Compliance: all 3 criteria met. Layer 1b: single commit `fix(frontend): ... [Task-54]`, no AI text in it. Security per task: verdict from `sdd-security-reviewer` still required (handoff 202); not replaced here.

| Criterion | Evidence | Result |
|---|---|---|
| 1 Focus on "Status of <title>" after RATING_NOT_ALLOWED | E2E in `us-012-status-rating.spec.ts` now ends with `await expect(page.getByLabel('Status of <title>', {exact:true})).toBeFocused()`. It is a web-first assertion with an exact accessible name, so it cannot pass on the body. The F-37-1 `evaluate`, annotation and `console.log` are removed (the unused `testInfo` parameter too). `reloadEntry` now calls `restoreFocus` after the reload. | met |
| 2 Control still exists: focus unchanged | `focusKeyAfterReload(key, exists)` is pure; its test covers both cases ('3'/false and 'clear'/false give 'status'; '3'/true and 'status'/true give the same key). The Task 36/37 focus tests are untouched. | met |
| 3 Earlier tests unchanged and green | Only added lines in `entryOps.test.js` (import plus one test). I re-ran `node --test "frontend/**/*.test.js"`: 110 pass, 0 fail (includes `sources.test.js` gates). | met |

## Checks requested
- "Still exists" is `controlOf(...) !== null && closest('[hidden]') === null`. This matches the view: the rating radios sit in `.entry-rating-mount`, which `setVisible` hides when not completed, so a radio inside a hidden ancestor counts as gone. The hidden clear button already falls back to the first radio in `controlOf`; if the mount is hidden, that radio is also judged gone and focus goes to status. `status` and `delete` are never hidden, so they stay put.
- Focus theft: the Task 36 guard (`active` is null, the body, or inside the entry item) is unchanged and still wraps the whole decision. The new code only chooses the target inside that guard, and only within the same entry. A control elsewhere keeps focus.
- Scope: 6 files, all listed or the usual progress.md and handoff. Line counts: `entryActions.js` 213, `entryOps.js` 67, E2E spec 266, all under 300.
- Sources gates: green in the unit run above.

## E2E re-run
Not re-run by me. The change is a one-line focus decision with the assertion tied to a real browser; the delivery reports full E2E 95 passed and `us-012 --repeat-each=3` 72 passed (taken from the delivery, not verified). Judgment: no separate re-run needed for this audit; the Increment 2 gate run covers it.

## Ledger
- F-37-1 closed (satisfied).
- PM debt row stays open: the focus rule (P-F2) still has no spec line and no plan line; plan.md section 147 item 10 remains a change request question.

## Non-blocking
- Layer 3: `restoreFocus` runs twice on this path (in `failed` and after the reload); harmless and correct, since the second call is what lands on the status select.

Reminder: update progress.md and the board after the Security verdict and Sponsor approval.
