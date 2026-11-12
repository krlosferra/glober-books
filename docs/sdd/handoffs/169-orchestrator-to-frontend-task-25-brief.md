# Handoff 169: Task brief, Task 25 (list state, filter and list states)
- From: orchestrator (direct dispatch, constitution 1.7)
- To: frontend
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 25 of `docs/sdd/tasks.md` (v0.6.6): the task block is the specification (criteria 1 to 5, traces). Work continues on the branch `feature/task-24-text-failure-logic` (checked out): Increment 1 chunk, one commit series per task tagged `[Task-25]`; the same branch will carry Tasks 24 to 27 as one review chunk and one pull request. Task 24 is committed (0830f72). Node is at `/opt/node24/bin` (`export PATH=/opt/node24/bin:$PATH`).

**Correction to brief 167 (orchestrator's error):** that brief said the server's field message is never rendered. The spec (US-011.6 and 5.9) shows the `title` and `author` message of a `VALIDATION_ERROR` detail beside the field, and the security condition (SEC-F-095, SEC-F-132) is that it is rendered with `textContent` only. Task 24 followed the spec correctly (`describeFailure` returns `fields: [{field, text}]`). The spec governs; keep that rule: the server's `error` text is never read; a field `message` is shown only through `textContent`, never `innerHTML`.

## Read (by ID, not whole files)
`spec.md`: US-010.2, US-010.4 to US-010.7, US-010.10, US-011.5, CR-2, Section 5.9 (the failure texts; reuse `frontend/js/logic/failures.js` and `copy.js` of Task 24), the list endpoint 5.1 (`status` filter, `total`, order); `plan.md` frontend section and plan-appendix AP-7.1 (state model, stale responses, focus rules for events on cleared entries); `frontend-testing.md` section 5 (exploration evidence); the existing `frontend/index.html`, `frontend/css/styles.css`, `frontend/js/ui/*.js`, `frontend/js/api/*.js`, `frontend/js/logic/*.js` and their tests. No other documents.

## Open items assigned (`docs/sdd/active-conditions.md`)
SEC-F-094: the source gates of `frontend/js/sources.test.js` keep passing (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `eval`, `new Function`, `document.write`, inline handlers, storage). Task 24 risk for Task 26: `parseFields` in `api/http.js` drops a `status` entry without a string `message`, which CR-7 treats as usable; note it, do not fix it here (Task 26).

## Scope
Create `frontend/js/logic/listState.js` (pure page-state model, DOM-free) with `listState.test.js`; edit `frontend/js/ui/app.js` and `listView.js`, `frontend/index.html` and `frontend/css/styles.css`. Nothing else: no backend, no `pom.xml`, no new dependency, no E2E spec (Task 28 tests this task), no add-form change beyond what criterion 4 requires.

## Binding constraints
1. **State model first:** all decisions of criteria 2 to 5 (loading, empty, filter-empty, failed, count arithmetic, stale-response discarding by a request token, ignoring a replaced or removed event for an entry already cleared by a new load while still announcing it and moving no focus) live in the pure model and are unit-tested with expected values from the spec text. The view only renders the model.
2. **Safe rendering:** the list is built from the `<template>` with `textContent`; no HTML string building; ids are checked with `logic/ids.js` before they are used in a request or a DOM attribute; nothing from the server is interpolated into markup, attributes, URLs or `id` values.
3. **Filter:** radio group "Filter by status" with All, Unread, Reading, Completed, All checked at load; choosing loads with the matching `status` query value (`unread`, `reading`, `completed`; none for All); keyboard operation is the native radio behaviour; accessible name and `aria-live` announcements as 5.9 and the accessibility rows require.
4. **States and texts:** every visible string comes from `copy.js` and equals the spec text exactly (assert in tests). "Loading your books…" replaces the list and the count; the failure text and "Try again" repeat the active filter; the count is hidden after a failure.
5. **Exploration evidence (criterion 5):** try the Playwright browser tools once; if the tool server is not exposed to your run (it has not been in Tasks 12 and 14), say so, do not substitute silently: run the real page against the packaged jar with the pinned `@playwright/test` in a throwaway script outside the repository (or the E2E harness with a temporary spec you do not commit), record for each criterion what you observed (filter choices, loading state, empty and filter-empty messages, failure and retry with the backend stopped, stale response), and mark it as a scripted run (a documented deviation). Browser binary: `/opt/pw-browsers/chromium` if `npx playwright install` is not possible. Start the jar on a free port (never 8080 or 18080 unless it is the harness's own port 18080 through the launcher), with a temporary data directory (mode 0700); stop every process you start.
6. Every file at or under 300 lines. Plain JavaScript ES modules, plain CSS. No dependency.

## Delivery (compact, constitution 1.7)
Write `docs/sdd/handoffs/170-frontend-to-liaison-task-25-delivery.md` early with a "Resume point". It holds: task, branch, commit hashes; files created or edited; one table mapping every criterion to unit tests and to the exploration observation; the summary line of `node --test "frontend/**/*.test.js"` (tests, passed, failed, exit code) and of `npx playwright test --grep @smoke` from `e2e/` if you ran it (the smoke must stay green); deviations (including the scripted exploration); risks. No raw terminal output, no `git diff --stat`, no author log. Commit after each step (`type(scope): subject [Task-25]`), local only, explicit paths, never `--no-verify`, nothing naming a tool, model or vendor as author or co-author, never session links or trailers. Update `docs/sdd/progress.md` in the same change.

## Rules
STOP and report on any spec gap or contradiction, any need for a dependency or a change outside the listed files, and any backend behaviour that differs from the contract. Do not push.
