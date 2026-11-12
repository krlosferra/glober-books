# Handoff 181: Task 29 audit (E2E add, validation and duplicate flows, plus F-29-1 fix)
- From: liaison   To: orchestrator   Type: AUDIT_REPORT
- Mode: F (with G). Branch `test/task-29-e2e-add-and-duplicate`, commits f5b2bee..34e393a.

## Verdict: ACCEPT Task 29 and the Task 27 follow-up (F-29-1). Security review still required (Security: per task, and the focusTrap change touches production code).

## What I re-ran / took from the delivery
- Re-ran: `node --test "frontend/**/*.test.js"`: 91 tests, 91 pass, 0 fail.
- Taken from the delivery 180, not re-run: full E2E 69/69, three runs of the new specs (41 each), jar rebuild. I read all test code; no E2E re-run judged necessary (no vacuity found that a run would reveal).

## Layer 1, compliance (blocking): pass
- Files: only the task's test files, `e2e/support/pages.ts`, smoke 4 in the current smoke file, progress, and the declared production fix. No config, dependency, backend change. The add spec split into `us-011-add-failures.spec.ts` is a justified 300-line deviation. All files <= 229 lines.
- Expected strings: no import of `copy.js` or `frontend/js` anywhere in `e2e/` (grep); texts match spec literals (US-011.9, US-013.2, 5.9 table checked).
- Hostile strings inert (title, author, field messages, dialog); guard fixture gives CSP/console checks in every test.

## Criteria
1. Pass. Defaults/focus, 5 client checks plus both fields (aria-invalid, description, focus, zero POSTs, library unchanged), double click and double Enter with a held response: one POST counted both on the page (`trackPosts`) and at the route (`held.count`), one book in the library. Confirmation and focus (AC-1, AC-2 filtered). Keyboard add is the Task 28 test, referenced.
2. Pass. Server message taken from the real 400 body and matched beside the field (not copied from code); stubbed status entry, INVALID_STATUS, LIBRARY_FULL, 500, network, MALFORMED_REQUEST keep values and focus Title. Stub keeps real headers.
3. Pass. Dialog text, existing book, focus on Cancel, Escape, Cancel (values kept, Title focused, one POST, one book), Add anyway (body `allow_duplicate` undefined then true, two books), plus the four failure endings. Smoke 4 present and tagged.
4. Pass. 500 code points, 500 decomposed e acute (1,000 typed, stored 500), U+00A0 trimmed: asserted on the page and in the stored record, not only on the response.
- Messages-update counts: 0 for field-level (also for the dialog), 1 per page-level failure, 2 with an earlier message (clear plus one). Meets F-1 of handoff 173.
- Oracle quality: assertions state what the spec says (counts, literals, focus), not mirrored code. Determinism: web-first assertions, `expect.poll`, held promise, no fixed sleeps, library reset per test.

## F-29-1 fix and test
- `nextTabTarget` is a small pure function; `trapTab` now sends focus to the first or last button when focus is on neither. Matches US-014.2 wording (never leaves the dialog). Five unit tests cover all four edge cases plus the "other" case. The `test.fail()` pin is gone (grep: none in the spec), and the strict Shift+Tab test now passes. Edit is inside the dialogs module, 14-line new file; acceptable as a Task 27 follow-up commit tagged `[Task-27]`.

## Findings
| ID | Sev | Finding | Resolution |
|---|---|---|---|
| F-29-2 | LOW | 320x90: heading and question are clipped (UA `max-height` plus `overflow: hidden`); buttons reachable. | Acceptable for this task: 90 px high is not a supported viewport in the spec, and US-014.2/.4 name only reachability and no horizontal scroll at 320 wide. No spec line is needed for acceptance. Recorded as a PM backlog question in active-conditions (is a minimum height such as 320x256 required; if so one extra assertion, and CSS changes need a PM line because of the US-014.6 containment rules). |
| F-29-3 | MINOR | Tab/Shift+Tab loops: iterations 2 to 5 poll an id that already matches, so a slow Tab could pass before it is processed; the start state (focus on neither button) is not asserted. The first iteration after a click is meaningful and the pre-fix run failed on it, so the test is not vacuous. | Optional: assert the active id is empty before the loop. Non-blocking. |
| F-29-4 | INFO | `countMessageUpdates` depends on how `#messages` is written (declared risk). | Keep; revisit if the area gets child elements. |

## Housekeeping
- active-conditions: F-1 to F-3 of handoff 173 closed; F-29-2 added (PM, LOW).
- Remind: update `progress.md` audit note and commit after the Security verdict and Sponsor approval. Branch is stacked on Task 28 (PR 48): merge order matters.
- Layer 1b: commit subjects follow Conventional Commits with `[Task-N]`; I saw no AI attribution in messages (author is the human identity). Not a full hook re-check.
