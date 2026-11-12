# Handoff 179: Task brief, Task 29 (E2E add, validation and duplicate flows)
- From: orchestrator (direct dispatch, constitution 1.7)
- To: frontend
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 29 of `docs/sdd/tasks.md` (v0.6.6): the task block is the specification (criteria 1 to 4, traces). Branch `test/task-29-e2e-add-and-duplicate` (checked out), stacked on the Task 28 branch (PR 48 not yet merged). Commits tagged `[Task-29]`. Node at `/opt/node24/bin`. The jar bakes the frontend at build time: rebuild the jar before E2E. Reuse the support files of Task 28 (`e2e/support/stubs.ts`, `view.ts`, `library.ts`); put page helpers in `e2e/support/pages.ts`.

## Read (by ID, not whole files)
`spec.md`: US-011.1 to US-011.10, US-013.2, US-013.4, CR-1, CR-3, CR-5, CR-7, Section 5.9 and the dialog texts, T-03, T-07, T-08, T-40, M-1, smoke test 4; `frontend-testing.md`; the existing `e2e/` files and `frontend/js/**`. No other documents.

## Open items assigned (`docs/sdd/active-conditions.md`, Liaison F-1 to F-3 of handoff 173, remaining part)
- Count the updates of the "Messages" area: zero for a field-level failure (US-013.4, CR-5), one per other failure.
- Exactly one POST on repeated submit (double click and double Enter, with a held response), counted on the real page.
- Tab and Shift+Tab after a click on the dialog text stay inside the two buttons.
- A very short viewport: the dialog buttons stay reachable (the dialog has no `max-height`); assert and, if clipped, report (do not change production CSS without a STOP).
- Expected strings are literals from the spec, never imported from `copy.js`; hostile strings inert; zero CSP violations in every flow (the guard fixture).

## Scope
`e2e/support/pages.ts` (new), `e2e/tests/us-011-add-book.spec.ts` and `e2e/tests/us-011-duplicate.spec.ts` (new), the smoke test 4 (duplicate add) in its current smoke file as the task says, `docs/sdd/progress.md`. Split files at 300 lines. No backend, no `pom.xml`, no new dependency, no `playwright.config.ts` change (STOP and report if a spec gap forces it). Production code only if a test exposes a defect: STOP and report it, do not fix it silently.

## Binding constraints
1. Deterministic tests: web-first assertions, no fixed sleeps, held responses via route interception with a controllable promise; every test resets the library through the API; port 18080 only through the harness launcher, never 8080.
2. Criterion 4: 500 code points, decomposed "e acute" and a leading U+00A0 are accepted by the real jar and shown trimmed.
3. Duplicate flow on the real jar: dialog text with the existing book, initial focus on Cancel, Escape, Cancel keeps values and focuses Title, Add anyway adds a second book.
4. Stubbed paths keep the real security headers (use the Task 28 helper that fulfils with the real response).
5. Every file at or under 300 lines.

## Delivery (compact, constitution 1.7)
Write `docs/sdd/handoffs/180-frontend-to-liaison-task-29-delivery.md` early with a "Resume point". It holds: task, branch, commit hashes; files created or edited; a table mapping each criterion and each carried item to its test; summary lines of `node --test "frontend/**/*.test.js"` and of the full E2E run (tests, passed, failed, exit code; run the new specs 3 times for flakiness); deviations; risks. No raw terminal output, no `git diff --stat`, no author log. Commit after each step (`type(scope): subject [Task-29]`), local only, explicit paths, never `--no-verify`, nothing naming a tool, model or vendor as author or co-author, never session links or trailers.

## Rules
STOP and report on any spec gap or contradiction, any need for a dependency, any change outside the listed files, or any backend behaviour that differs from the contract. Do not push.
