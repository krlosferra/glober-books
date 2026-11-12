# Handoff 167: Task brief, Task 24 (frontend text and failure logic)
- From: orchestrator (direct dispatch, constitution 1.7)
- To: frontend
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 24 of `docs/sdd/tasks.md` (v0.6.6): the task block is the specification (description, criteria 1 to 4, traces). Branch `feature/task-24-text-failure-logic` is checked out (the header names `feature/task-24-text-and-failures-logic`; record the difference). Tasks 01 to 50 are merged; the page shell, the thin client and the E2E harness exist (Tasks 10 to 14). This is the first task of Increment 1: pure, DOM-free logic modules, so there is no Playwright test in this task; the unit tests run with the Node on `/opt/node24/bin` (`export PATH=/opt/node24/bin:$PATH`) and `node --test "frontend/**/*.test.js"` from the repository root.

## Read (by ID, not whole files)
`spec.md`: Section 5.9 (the table of failures with the action and the exact text for each case; every quoted string is a test oracle), US-011.3, US-011.6, US-013.1, US-013.3, US-013.5, CR-7 (usable `details` entry), Appendix A (every error `code` and its status), BR-006 items 2 to 5 (trim by Unicode White_Space, NFC, code-point length, the 500 and 200 limits), T-07 and T-22; `plan.md` frontend section (module layout under `frontend/js/logic`); the existing `frontend/js/logic/copy.js` and its test, `frontend/js/api/*` (how the client turns a response into a failure), `frontend/js/sources.test.js` (the source gates your files must pass). No other documents.

## Open items assigned to this task (`docs/sdd/active-conditions.md`)
None assigned to Task 24 itself. Keep in mind for the whole Increment 1: SEC-F-095 and SEC-F-132 (`fields[].message` from the server is never rendered; it is data, and Task 26 will render only the exact texts of this table with `textContent`); SEC-F-094 (the source gates of `sources.test.js` must keep passing: no `innerHTML`, `eval` and similar in your files).

## Scope
Create `frontend/js/logic/text.js` and `frontend/js/logic/failures.js`, edit `frontend/js/logic/copy.js` (the exact texts live there), with a unit test next to each module. Nothing else: no DOM, no network call, no page edit, no CSS, no backend, no `pom.xml`, no new dependency, no E2E change.

## Binding constraints
1. **Length (criterion 1):** trim with Unicode White_Space (not a hand-written list; `String.prototype.trim` is not identical, check U+0085, U+00A0, U+2028, U+2029, U+3000 and U+FEFF against the spec's list in BR-006 item 2), normalize to NFC, count code points. 500 emoji count 500; 500 decomposed "é" count 500; 300 times U+1D15E count 600; empty or whitespace gives `empty`; 501 for the title and 201 for the author give `length`. The client check is a courtesy: the server stays the authority. Use `Intl.Segmenter`? No: count code points, not graphemes.
2. **Failure table (criteria 2 and 3):** every `code` of Appendix A and the three non-envelope cases (network failure, a non-JSON body or non-envelope body, a timeout) map to the action and the exact text of 5.9; an unknown `code` maps to the `INTERNAL_ERROR` entry. `VALIDATION_ERROR` with `details` missing, not an array, or with no usable entry maps to the `MALFORMED_REQUEST` text; an entry is usable when `field` is `title`, `author` or `status` and, for `title` and `author`, `message` is a non-empty string; the server's `error` text is never returned, displayed or interpolated (US-013.5): the function returns only strings from `copy.js`.
3. **Exact strings (criterion 4):** every quoted string of 5.9 and of US-011 to US-013 is asserted exactly, with expected values copied from the spec text, not from the code you write. If a quoted string appears twice in the spec with different wording, STOP and report.
4. **Hostile input:** the functions must not throw on `null`, `undefined`, numbers, objects, arrays, lone surrogates, 16 KiB strings; they return a defined result. No regular expression with nested quantifiers over user text. A `message` or `code` that contains markup or script text is never echoed.
5. **Source gates:** `frontend/js/sources.test.js` must stay green (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `eval`, `new Function`, `document.write`, inline handlers, `Function` constructor, `localStorage`); modules are ES modules in plain JavaScript; every file at or under 300 lines.
6. **No Playwright in this task.** Evidence is the unit tests. Do not try to use the browser tool.

## Delivery (compact, constitution 1.7)
Write `docs/sdd/handoffs/168-frontend-to-liaison-task-24-delivery.md` early with a "Resume point" (done / next). It holds: task, branch, commit hashes; files created or edited; one table mapping every criterion and every quoted string of 5.9 and US-011 to US-013 to its test name; the summary line of `node --test "frontend/**/*.test.js"` (tests, passed, failed, exit code) and of the backend-independent checks you ran; deviations; risks. No raw terminal output, no `git diff --stat`, no author log. Commit after each step (`type(scope): subject [Task-24]`), local only, explicit paths, never `--no-verify`, nothing naming a tool, model or vendor as author or co-author, never session links or trailers. Update `docs/sdd/progress.md` in the same change.

## Rules
STOP and report on any spec gap or contradiction, any need for a dependency, any change outside the three files and their tests. Do not push.
