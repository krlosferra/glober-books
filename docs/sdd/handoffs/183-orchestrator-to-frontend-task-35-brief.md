# Handoff 183: Task brief, Task 35 (client endpoints for status, rating, clear and delete)
- From: orchestrator (direct dispatch, constitution 1.7)
- To: frontend
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 35 of `docs/sdd/tasks.md` (v0.6.6): the task block is the specification (criteria 1 to 3, traces 5.3 to 5.7, BR-011). Branch `feature/task-35-client-endpoints` (checked out, from main after PR 49). Commits tagged `[Task-35]`. Node at `/opt/node24/bin`. Path rule: `docs/sdd/README.md` section 5; list a directory before opening a path you did not create or read.

## Read (by ID, not whole files)
`spec.md`: 5.3 to 5.7 (get, status, rating, clear, delete), E-07, BR-011, 5.9 failure shapes; `plan.md` frontend section (API layer); the existing `frontend/js/api/*.js`, `frontend/js/logic/ids.js` and their tests. No other documents.

## Open items assigned (`docs/sdd/active-conditions.md`)
- SEC-F-097 (INFO): re-verify the non-GET `Content-Type` rule when the DELETE and PUT wrappers are added: every non-GET carries `Content-Type: application/json`, DELETE with no body; assert it in tests.
- SEC-F-094: `frontend/js/sources.test.js` keeps passing.

## Scope
`frontend/js/api/booksApi.js` (edit) and `booksApi.test.js` (edit); a new file only if the 300-line limit forces a split. No UI, no backend, no `pom.xml`, no new dependency, no E2E spec.

## Binding constraints
1. Paths are built only with the validated id builder (`logic/ids.js`); an id that is not a lower-case UUID is refused before any request (no fetch call), unit-tested with hostile ids (`../`, `%2e`, upper case, empty, very long, non-string).
2. `PUT /status` and `PUT /rating` send JSON bodies built from validated values only (status among the three literals, rating an integer 1 to 5); `DELETE` (clear rating and delete book) sends `Content-Type: application/json` and no body.
3. Responses map to the camelCase book; failures map to the existing `api`, `network` and `unreadable` kinds with field messages and the existing book where present; nothing from the server `error` text is read.
4. No `innerHTML`, `eval`, storage or inline anything; plain ES module; file at or under 300 lines.
5. Unit tests use a stub `fetch`; expected bodies, methods and headers are spec literals.

## Delivery (compact, constitution 1.7)
Write `docs/sdd/handoffs/184-frontend-to-liaison-task-35-delivery.md` early with a "Resume point": task, branch, commit hashes; files edited; a table mapping each criterion and SEC-F-097 to its unit test; the summary line of `node --test "frontend/**/*.test.js"` (tests, passed, failed, exit code); deviations; risks. No raw terminal output, no `git diff --stat`, no author log. Commit locally with explicit paths and `type(scope): subject [Task-35]`, never `--no-verify`, nothing naming a tool, model or vendor, no trailers. Update `docs/sdd/progress.md`.

## Rules
STOP and report on any spec gap or contradiction, any need for a dependency, any change outside the listed files, or any backend behaviour that differs from the contract. Do not push.
