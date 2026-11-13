# Handoff 199: Task brief, Task 54 (focus after RATING_NOT_ALLOWED)
- From: orchestrator (direct dispatch, constitution 1.7)
- To: frontend
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 54 of `docs/sdd/tasks.md` (v0.6.8): the task block is the specification (criteria 1 to 3, traces US-012.7 and plan assumption P-F2). Branch `feature/task-54-focus-after-rating-not-allowed` (checked out from main after PR 55). Commits tagged `[Task-54]`. Node at `/opt/node24/bin`. The jar bakes the frontend at build time: rebuild it (`mvn -o package -DskipTests` from `backend/`) before running E2E. Path rule: `docs/sdd/README.md` section 5.

## Read (by ID, not whole files)
Spec US-012.7 and plan-appendix AP-8 (focus table, assumption P-F2); `frontend/js/ui/entryActions.js` (`send`, `restoreFocus`, `reloadEntry`), `frontend/js/logic/entryOps.js` and its test, `frontend/js/ui/entryView.js` for the select locator; `e2e/tests/us-012-status-rating.spec.ts` (the `RATING_NOT_ALLOWED` test, where focus landing on `body` is recorded as an annotation) and `e2e/support/view.ts`. No other documents.

## Open items assigned (`docs/sdd/active-conditions.md`)
- **Liaison F-37-1:** after `RATING_NOT_ALLOWED` the entry reloads as Reading or Unread and the rating group is hidden, so the focus given to a rating radio falls to the page body. Focus must fall back to the entry's status select "Status of <title>" when the control used no longer exists, and stay unchanged when it still exists.
- SEC-F-094: `frontend/js/sources.test.js` keeps passing (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `eval`, `new Function`, inline handlers, storage).

## Scope
`frontend/js/ui/entryActions.js` (edit), `frontend/js/logic/entryOps.js` (edit, the pure focus decision) and its unit test (one case for the decision, plus the case where the control still exists); `e2e/tests/us-012-status-rating.spec.ts` (edit: replace the annotation by one assertion that focus is on the status select named "Status of <title>"; focus rules asserted elsewhere stay untouched). `docs/sdd/progress.md`. No other production code, no backend, no `pom.xml`, no new dependency, no `playwright.config.ts` change. The spec and plan line for the rule is a PM matter: do not edit them.

## Binding constraints
1. The decision is pure: input = whether the control used still exists in the reloaded entry; output = the target (the same control, or the status select). The view only applies it. Never steal focus from another control the user has since moved to (keep the existing "focus only if it was on the control or on the body" rule of Task 36).
2. Safe rendering and CSP unchanged: no new sinks; ids validated as before; no storage; nothing from the server in attributes or markup.
3. Playwright note: `selectOption` does not focus; Escape closes a dialog before its `close` event (wait for the restored value). Deterministic assertions only (web-first, no fixed sleeps).
4. Every file at or under 300 lines.

## Delivery (compact, constitution 1.7)
Write `docs/sdd/handoffs/200-frontend-to-liaison-task-54-delivery.md` early with a "Resume point": task, branch, commit hashes; files edited; a table mapping each criterion to its unit test and E2E assertion; summary lines of `node --test "frontend/**/*.test.js"` and of the full E2E run (tests, passed, failed, exit code; run `npx playwright test us-012 --repeat-each=3` once for flakiness; unit tests and E2E counted separately); deviations; risks. No raw terminal output, no `git diff --stat`, no author log. Commit locally with explicit paths and `type(scope): subject [Task-54]`, never `--no-verify`, nothing naming a tool, model or vendor, no trailers.

## Rules
STOP and report on any spec gap or contradiction, any need for a dependency, any change outside the listed files, or any backend behaviour that differs from the contract. Do not push.
