# Handoff 175: Task brief, Task 28 (E2E library view, filter and states)
- From: orchestrator (direct dispatch, constitution 1.7)
- To: frontend
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 28 of `docs/sdd/tasks.md` (v0.6.6): the task block is the specification (criteria 1 to 4, traces). Branch `test/task-28-e2e-library-view` (checked out, from main after PR 47). Commits tagged `[Task-28]`. Node at `/opt/node24/bin`. The jar bakes the frontend at build time: rebuild the jar before running E2E.

## Read (by ID, not whole files)
`spec.md`: US-010.1 to US-010.10, Section 5.9 texts, T-12; `frontend-testing.md`; the existing `e2e/` files (`support/*.ts`, `tests/*.spec.ts`, `playwright.config.ts`) and `frontend/js/**`. No other documents.

## Open items assigned (`docs/sdd/active-conditions.md`)
- **SEC-F-197 (LOW):** `listState.loadFailed` must use the fixed generic failure text when `describeFailure` returns no string text (a list GET answered with `VALIDATION_ERROR` or `DUPLICATE_BOOK` currently shows an empty failure text). Fix in the production file with a unit test (small, listed as a deviation: Task 28 is otherwise test-only).
- **SEC-F-198 (LOW):** re-rendering 5,000 maximum-size entries blocks the page about 5 s. Measure it in E2E (seed through the API, record the time) and, if it is above 1 s, try `content-visibility: auto` with `contain-intrinsic-size` in `styles.css`; if that does not help, report and leave it to Task 48. Do not break the CSP.
- **Liaison F-1 to F-3 of handoff 173:** use the status region id `#list-status`; assert on the real page that a single request is in flight; test Tab and Shift+Tab after a click on the dialog text (this belongs to Task 29, note it); take every expected string from the spec text, never import it from `copy.js`; assert zero CSP violations in each flow.

## Scope
`e2e/support/stubs.ts` (new), `e2e/support/library.ts` (edit), `e2e/tests/us-010-library-view.spec.ts` (edit), plus the SEC-F-197 fix (`frontend/js/logic/listState.js` and its test) and, only if needed for SEC-F-198, `frontend/css/styles.css`. No backend, no `pom.xml`, no new dependency, no change of `playwright.config.ts` unless a spec gap forces it (STOP and report then).

## Binding constraints
1. Criterion 1: a fixed UTC value in a far time zone (use `timezoneId` such as `Pacific/Kiritimati` and `Pacific/Pago_Pago`) shows the local date.
2. Criterion 3 and 4: use held responses (route interception with a controllable promise); prove "no list item until a load succeeds", the add during a held load, and the last-request-wins result; the count and the status region are asserted too.
3. The page stays the real one served by the jar; stubs only for states the page cannot provoke (failure, held loads).
4. Hostile strings (`<img src=x onerror=...>`, `"><script>`, 500 code points) render as inert text; zero console CSP violations.
5. The tests must be deterministic: no fixed sleeps, use web-first assertions; each test resets the library through the API. Never port 8080; the harness uses 18080 through its launcher.
6. Every file at or under 300 lines.

## Delivery (compact, constitution 1.7)
Write `docs/sdd/handoffs/176-frontend-to-liaison-task-28-delivery.md` early with a "Resume point". It holds: task, branch, commit hashes; files created or edited; a table mapping each criterion to its test; the summary lines of `node --test "frontend/**/*.test.js"` and of the full E2E run (tests, passed, failed, exit code; flakiness: run the new spec 3 times); the SEC-F-198 measurement; deviations; risks. No raw terminal output, no `git diff --stat`, no author log. Commit after each step (`type(scope): subject [Task-28]`), local only, explicit paths, never `--no-verify`, nothing naming a tool, model or vendor as author or co-author, never session links or trailers. Update `docs/sdd/progress.md` in the same change.

## Rules
STOP and report on any spec gap or contradiction, any need for a dependency, any change outside the listed files, or any backend behaviour that differs from the contract. Do not push.
