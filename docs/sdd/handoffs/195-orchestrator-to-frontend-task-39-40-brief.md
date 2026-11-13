# Handoff 195: Task brief, Task 39+40 (E2E status, rating and delete)
- From: orchestrator (direct dispatch, constitution 1.7)
- To: frontend
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 39+40 of `docs/sdd/tasks.md` (v0.6.6): the task block is the specification (criteria 1 to 8, traces US-012, US-010.6, S-01 to S-06, S-11). Build order: criteria 1 to 4 (status and rating) first, then 5 to 8 (delete); separate commits tagged `[Task-39]` and `[Task-40]`. Branch `test/task-39-40-e2e-status-rating-delete` (checked out), stacked on the chunk 37+38 branch (PR 53 not yet merged; the orchestrator rebases it onto main after the merge). Node at `/opt/node24/bin`. The jar bakes the frontend at build time: rebuild it (`mvn -o package -DskipTests` from `backend/`) before running E2E. Path rule: `docs/sdd/README.md` section 5.

## Read (by ID, not whole files)
`spec.md`: US-012.1 to .9, US-010.4, .6, .10, S-01 to S-06, S-11, 5.4 to 5.7 and 5.9; `frontend-testing.md`; the existing `e2e/` files (`support/*.ts`, `tests/*.spec.ts`, `playwright.config.ts`) and `frontend/js/ui/entryView.js`, `entryActions.js`, `dialogs.js` for locators only. No other documents.

## Open items assigned (`docs/sdd/active-conditions.md`)
- **F-37-1:** after `RATING_NOT_ALLOWED` the entry reloads as Reading and the rating group is hidden, so the focus given to a radio falls to the page body. The plan assumes focus returns to the control used; no story names it. Observe and record where focus lands; assert only what the spec requires (the plan assumption is not asserted by tests) and report it as an observation.
- **F-37-2 (INFO):** the UI wiring has no unit test: assert in E2E the focus rules (Cancel and Escape focus the Delete button, success focuses the "Your books" heading, a failed delete returns to the Delete button), the 404 path and the failure redraw.
- **Task 36 notes for locators:** the status control is a native `select` (role `combobox`) named "Status of <title>"; Playwright's `selectOption` does not focus the select, so focus it first; Escape closes the dialog before the `close` event, so wait for the restored value before asserting it.
- **Liaison F-1 to F-3 of handoff 173 and Task 29 conventions:** expected strings are literals from the spec, never imported from `copy.js` or `frontend/`; zero CSP violations in every flow (the guard fixture); stubbed responses keep the real security headers (the Task 28 helper that fulfils with the real response); count requests on the real page where the spec says one request.
- **SEC-F-138 (LOW, partial):** in the add and delete flows also assert that `localStorage`, `sessionStorage` and cookies are empty at the end of each flow (a small helper in `support/`, reused).

## Scope
`e2e/tests/us-012-status-rating.spec.ts` and `us-012-delete.spec.ts` (new), `e2e/support/pages.ts` (edit) and other support helpers only as needed, smoke tests 5 and 6 (tag `@smoke`, in the current smoke file as in Task 29), `docs/sdd/progress.md`. No production code (a defect found: STOP and report, do not fix), no backend, no `pom.xml`, no new dependency, no `playwright.config.ts` change. Split files at 300 lines.

## Binding constraints
1. Deterministic tests: web-first assertions, no fixed sleeps, held responses through route interception with a controllable promise; each test resets the library through the API; the stale-state cases (criteria 4 and 7) change or delete the book through the API behind the page, then act on the page. Port 18080 only through the harness launcher, never 8080.
2. The status changes and the rating are checked against the real jar (the returned values, including `date_completed`, are shown); stubs only for states the page cannot provoke.
3. Hostile title and 500-code-point title appear in at least the delete and the confirmation flows and stay inert.
4. Every file at or under 300 lines.

## Delivery (compact, constitution 1.7)
Write `docs/sdd/handoffs/196-frontend-to-liaison-task-39-40-delivery.md` early with a "Resume point": tasks, branch, commit hashes; files created or edited; a table mapping each criterion (1 to 8) and each carried item to its test; summary lines of `node --test "frontend/**/*.test.js"` and of the full E2E run (tests, passed, failed, exit code; run the new specs 3 times for flakiness; unit tests and E2E counted separately); observations (F-37-1 focus landing); deviations; risks. No raw terminal output, no `git diff --stat`, no author log. Commit locally with explicit paths and `type(scope): subject [Task-39]` or `[Task-40]`, never `--no-verify`, nothing naming a tool, model or vendor, no trailers.

## Rules
STOP and report on any spec gap or contradiction, any need for a dependency, any change outside the listed files, any production defect, or any backend behaviour that differs from the contract. Do not push.
