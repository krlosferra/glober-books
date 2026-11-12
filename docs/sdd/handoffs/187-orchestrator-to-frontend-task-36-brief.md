# Handoff 187: Task brief, Task 36 (entry view, status control and confirmation)
- From: orchestrator (direct dispatch, constitution 1.7)
- To: frontend
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 36 of `docs/sdd/tasks.md` (v0.6.6): the task block is the specification (criteria 1 to 4, traces US-012.1 to .3, .6, .8). Branch `feature/task-36-entry-status` (checked out, from main after PR 51; Task 35 client functions `setStatus` and friends are in `frontend/js/api/booksApi.js`). Commits tagged `[Task-36]`. Node at `/opt/node24/bin`. Path rule: `docs/sdd/README.md` section 5; list a directory before opening a path you did not create or read.

## Read (by ID, not whole files)
`spec.md`: US-012.1 to US-012.3, US-012.6, US-012.8, US-010.1 to .3, US-014 focus and accessibility rows for the entry and dialogs, 5.4 (status), 5.9, DEC-007; `plan.md` frontend section and appendix AP-7.1 and AP-8 (state model, focus table); `frontend-testing.md` section 5; the existing `frontend/index.html`, `css/styles.css`, `frontend/js/ui/*.js` (especially `listView.js`, `dialogs.js`, `messages.js`), `logic/*.js` and `api/booksApi.js` with their tests. No other documents.

## Open items assigned (`docs/sdd/active-conditions.md`)
- **Liaison F-35-1:** `setStatus` rejects with a TypeError on a refused id or status (not a failure result): every call is awaited inside try/catch, ids come only from server books, and a rejection leaves the controls enabled and shows the generic failure text.
- **F-35-2:** add a `setStatus` `INVALID_STATUS` failure test when you touch `failures.js` (only if you do).
- SEC-F-094: `frontend/js/sources.test.js` keeps passing (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `eval`, `new Function`, inline handlers, storage).
- Carried rules from Tasks 24 to 29: the status region is `#list-status`; focus after list-level actions follows the existing rules; the dialog focus trap `logic/focusTrap.js` is reusable for the confirmation dialog (extend `ui/dialogs.js`; keep the duplicate dialog behaviour unchanged).

## Scope
`frontend/index.html`, `css/styles.css` (edit); new `frontend/js/ui/entryView.js` and `frontend/js/ui/entryActions.js`; `ui/listView.js`, `ui/app.js` and `ui/dialogs.js` only as needed to mount the entry and open the second dialog. Unit tests for any pure decision you extract (for example, whether a change needs confirmation, the "No longer matches this filter" rule); no E2E spec (Task 39+40). No backend, no `pom.xml`, no new dependency.

## Binding constraints
1. **Texts from the spec:** the confirmation dialog is named "Confirm status change", with the text "Moving <title> back to <Unread|Reading> removes its rating and completion date.", Continue and Cancel, initial focus on Cancel, Cancel restores the previous choice without a request (US-012.3, DEC-007). A completed entry without a rating changes without a dialog. "No longer matches this filter" per US-012.8. Add every string to `copy.js` with exact-string tests; the title appears only through `textContent`.
2. **Control:** a single-choice control (radio group) named "Status of <title>" with Unread, Reading, Completed; choosing another value sends `PUT .../status`; the entry shows the values returned by the server (status, rating, completion date) and its controls are disabled while the request runs; one request in flight per entry. The rating control exists only on completed entries (the group itself is Task 37; leave a clearly marked mount point and do not build it).
3. **Safe rendering and CSP:** built from the `<template>` with `textContent`; no HTML strings; ids validated with `logic/ids.js` before any request or DOM attribute; nothing from the server in markup, attributes, URLs or ids; no inline styles or handlers; no storage. Every failure shows its 5.9 text beside the entry via `logic/failures.js`; never the server `error` text.
4. Every file at or under 300 lines. Plain ES modules and CSS.
5. **Exploration evidence** (criterion 4): try the Playwright browser tools once; if the tool server is not exposed to your run (it has not been so far), say so and run the real page with the pinned `@playwright/test` and `/opt/pw-browsers/chromium` in a throwaway script outside the repository against a jar built from this tree (`mvn -o package -DskipTests` from `backend/`; the jar bakes the frontend at build time). Record per criterion what you observed (status change, dialog focus and Cancel, rating-bearing entry, filter mismatch note, failure path with a stubbed response, hostile and 500-code-point titles). Mark it as a scripted run (documented deviation). Own port, never 8080 or 18080, temp data dir mode 0700, stop every process you start. Run `npx playwright test --grep @smoke` from `e2e/` at the end; it must stay green.

## Delivery (compact, constitution 1.7)
Write `docs/sdd/handoffs/188-frontend-to-liaison-task-36-delivery.md` early with a "Resume point": task, branch, commit hashes; files created or edited; a table mapping each criterion to its unit test and exploration observation; summary lines of `node --test "frontend/**/*.test.js"` and of the smoke run (tests, passed, failed, exit code); deviations; risks. No raw terminal output, no `git diff --stat`, no author log. Commit locally with explicit paths and `type(scope): subject [Task-36]`, never `--no-verify`, nothing naming a tool, model or vendor, no trailers. Update `docs/sdd/progress.md`.

## Rules
STOP and report on any spec gap or contradiction, any need for a dependency, any change outside the listed files, or any backend behaviour that differs from the contract. Do not push.
