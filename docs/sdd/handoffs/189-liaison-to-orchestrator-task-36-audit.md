# Handoff 189: Audit of Task 36 (entry view, status control and confirmation)
- From: liaison | To: orchestrator | Type: AUDIT_REPORT | Commit: 3bb4b0c, branch `feature/task-36-entry-status`
- Mode: F. Security verdict is separate (Security: per task).

## Verdict: ACCEPT (Layer 1, 1b, 2 pass), one MINOR claim correction (F-36-1)

## Evidence I produced
Re-ran `node --test "frontend/**/*.test.js"`: tests 103, pass 103, fail 0. The delivery and `progress.md` say 107; 107 = 103 + the 4 smoke tests. Not re-run: Playwright smoke and the scripted exploration (taken from the delivery; the code was read to confirm each claim is reachable). `wc -l`: every touched file under 300 lines (largest `entryActions.js`, 120).

## Criteria
| # | Result | Basis |
|---|---|---|
| 1 control, PUT, returned values, disabled in flight | Met | `onChange` ignores a busy entry or a non-change; `send` sets busy, then `succeeded` stores the server book via `bookReplaced`; `updateEntry` sets `select.disabled = ui.busy`. Busy lives in a per-id map, so it survives a list re-render. |
| 2 confirmation | Met | Exact text through `confirmStatusText`, heading, Continue and Cancel from `copy.js`, exact-string tests (including a hostile title); Cancel autofocus, Cancel or Escape: `refresh` restores the value, no `send`, focus back to the select. `needsConfirmation` false for a completed entry without a rating. |
| 3 rating control only when completed | Met | `.entry-rating-mount` visible only if `status === 'completed'`; the group is Task 37. No unit test (view logic); covered by exploration, acceptable for a view. |
| 4 "No longer matches this filter" | Met | `entry-mismatch` filled from `entry.noLongerMatches`; the count rule is the existing Task 25 logic. |
| Evidence recorded | Met with deviation | Scripted run instead of MCP tools: accepted, already a recorded deviation; the table gives a result per criterion. |

## Checks requested
- Texts: all new literals in `copy.js`, matching US-012.1, .3, .8; exact-string tests present. The Status option labels are in the template (the plan places them there), matching US-012.1.
- One request per entry: busy check before dialog and send. Gap considered and rejected as a defect: while the confirmation is open the page is modal, so no second change can arrive.
- F-35-1: met. `await setStatus` is inside try/catch; a rejection becomes `unreadable`, shows the generic text, restores the value, re-enables the control. F-35-2 not triggered (`failures.js` untouched). F-35-1 stays open for Task 37 only.
- BOOK_NOT_FOUND: `describeFailure.removeEntry` leads to `removeGone`: entry removed, state updated, text announced, heading focused, list reloaded (P-F1). Text and reload follow Task 37 AC3 wording; the stub run showed it.
- Focus: Cancel/Escape to the select (AP-8 table), Continue to the select after the request, P-F2 never steals focus from another control.
- Safe rendering: all values via `setText` (textContent), `aria-label` as attribute; grep finds no `innerHTML`, `eval` or inline `style=` in non-test source. Sources gate untouched; SEC-F-094 stays open.
- Layer 1b: one commit, `feat(frontend): ... [Task-36]`, human author, no attribution in the subject; I did not scan the trailer beyond the log lines shown.
- Tasks 36 says "Tests: none"; the added `statusChange.test.js` is additive and welcome. Not a scope breach (pure logic, same task).

## Rulings
1. **Select vs radio group:** the plan governs. `plan.md` (Controls table) and `plan-appendix.md` AP-8 ("Status control: `select` with `aria-label`") settle it, and US-012.1 says only "single-choice"; the `select` satisfies it. Brief 187 was wrong; the delivery is right. No line needed in `spec.md` or `tasks.md`: neither names a widget. One note for the PM when Tasks 39+40 are briefed: E2E uses role `combobox` for status and the radio role only for filter and rating, and `selectOption` does not focus (test must focus first).
2. **No announcement on success:** accepted. US-012 gives no success text and the per-entry `role="status"` slot carries only failures (5.9); the visible value changing is the confirmation. Do not invent a text.
3. **Focus-return rule:** accepted as implemented. It is P-F2 (plan assumption, asserted only as "not lost to the body"); the guard that it never steals focus from another control is stricter-safe. Cancel/Escape and Continue match the AP-8 table.
4. **RATING_NOT_ALLOWED reload:** belongs to Task 37 (its AC3). The status endpoint cannot return it. Accepted as not built.
5. Handling `BOOK_NOT_FOUND` in Task 36 is correct: status can return it, and Task 37 AC3 then only needs the rating path.

## Findings
| ID | Sev | Finding | Fix |
|---|---|---|---|
| F-36-1 | MINOR | Test count claimed 107 vs 103 re-run (107 includes the 4 smoke tests). | Correct `progress.md` Task 36 entry ("103 unit tests and the 4 smoke tests"); added to `active-conditions.md`. |
| Note | INFO | The Risks in 188 (re-render during a request, Escape timing) are real but handled (`uiOf` registry, id lookup). | Task 39+40 to wait for `close`. |

Layer 3: `entryActions.js` is readable; `ctx` object is fine. No change requested.

## Next
Orchestrator: obtain the Security verdict; correct the count in `progress.md`; Sponsor gate; then open the PR on approval. Task 37 next (F-35-1 still open there).
