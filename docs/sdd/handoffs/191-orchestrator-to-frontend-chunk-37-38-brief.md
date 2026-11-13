# Handoff 191: Task brief, Tasks 37 and 38 (rating group and clear rating; delete with confirmation)
- From: orchestrator (direct dispatch, constitution 1.7)
- To: frontend
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Tasks 37 then 38 of `docs/sdd/tasks.md` (v0.6.6): the two task blocks are the specification (criteria, traces US-012.4, .5, .7, .9, US-013.4, US-010.4, .6, .10, 5.9). One chunk branch `feature/task-37-rating-group` (checked out from main after PR 52; Tasks 35 and 36 are merged): build Task 37 first (commits tagged `[Task-37]`), then Task 38 (`[Task-38]`); both edit `ui/entryActions.js`, keep them as separate commits. One review and one pull request for the chunk (constitution 1.6). Node at `/opt/node24/bin`. Path rule: `docs/sdd/README.md` section 5.

## Read (by ID, not whole files)
`spec.md`: US-012.4 to .9, US-010.4, .6, .10, US-013.4, 5.5 to 5.7, 5.9, US-014 focus and accessibility rows; `plan.md` frontend section, plan-appendix AP-7.1 and AP-8 (focus table: rating group, clear, delete, dialogs); `frontend-testing.md` section 5; the existing `frontend/index.html`, `css/styles.css`, `frontend/js/ui/*.js` (`entryView.js`, `entryActions.js`, `dialogs.js`, `listView.js`, `messages.js`, `app.js`), `logic/*.js` (`statusChange.js`, `failures.js`, `copy.js`, `focusTrap.js`) and `api/booksApi.js` with their tests. No other documents.

## Open items assigned (`docs/sdd/active-conditions.md`)
- **Liaison F-35-1:** `setRating`, `clearRating` and `deleteBook` reject with a TypeError on a refused id or value: await each inside try/catch; a rejection shows the generic failure text and leaves the controls enabled.
- **F-35-2:** add a `setStatus`/rating `INVALID_STATUS` and `INVALID_RATING` case in `failures.test.js` when you touch `failures.js` (Task 37 criterion 2 edits it).
- **SEC-F-200 (INFO):** `entryActions.send` can leave a per-entry busy record for an id removed while a dialog was open; Task 38 removes entries, so make the delete path clear the record (one line and a test if the pure part is extractable).
- **SEC-F-094:** `frontend/js/sources.test.js` keeps passing (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `eval`, `new Function`, inline handlers, storage).
- Task 36 notes: the status control is a native `select` named "Status of <title>" (plan AP-8); the rating mount point is `.entry-rating-mount`; the status slot beside the entry carries failure text; focus is not stolen from another control; `RATING_NOT_ALLOWED` reloads the entry (this task).

## Scope
Task 37: `frontend/js/ui/entryView.js` and `entryActions.js` (edit), `logic/failures.js` (edit) and `failures.test.js`, `css/styles.css` (edit). Task 38: `ui/entryActions.js`, `ui/dialogs.js` (edit; keep the duplicate and status dialogs unchanged), `frontend/index.html` (edit). Pure decisions you extract get unit tests. No E2E spec (Task 39+40), no backend, no `pom.xml`, no new dependency.

## Binding constraints
1. **Texts from the spec:** every visible string comes from `copy.js` and equals the spec text exactly (exact-string tests): group "Rating of <title>", button "Clear rating of <title>" (exists only while a rating exists), "Choose a rating from 1 to 5.", "Only completed books can be rated.", "This book no longer exists.", "Delete <title>", dialog "Delete book" with "Delete <title>? This cannot be undone.", Delete and Cancel, "Book deleted: <title>". Titles only through `textContent` or `setAttribute` with a plain string, never markup.
2. **Task 37:** the group offers 1 to 5 by keyboard (native radios), shows the current value as text; selecting sends `PUT .../rating`, clearing sends `DELETE .../rating`; the rating group exists only on completed entries; a failed request returns the control to the server's last known value and shows the 5.9 text beside the entry; `INVALID_RATING` shows "Choose a rating from 1 to 5."; `RATING_NOT_ALLOWED` shows "Only completed books can be rated." and reloads the entry; `BOOK_NOT_FOUND` removes the entry, reloads the list and announces "This book no longer exists."; after a request that removes nothing, focus returns to the control used (plan assumption, not asserted by tests).
3. **Task 38:** "Delete <title>" opens the confirmation dialog, initial focus on Cancel, Escape and Cancel send nothing and focus the Delete button; on success the entry disappears, the count follows US-010.4, "Book deleted: <title>" is announced and focus moves to the "Your books" heading; the last delete shows the empty message (US-010.6); a failed delete shows its 5.9 text beside the entry and focus returns to the Delete button (focus after a 404 is the heading, plan assumption); no control edits a title or author (US-012.9). One request in flight per entry, as in Task 36.
4. **Safe rendering and CSP:** the template and `textContent` only; no HTML strings; ids validated with `logic/ids.js` before any request or DOM attribute; nothing from the server in markup, attributes (except the `aria-label` text), URLs or ids; no inline styles or handlers; no storage; no title or server text in the console.
5. Every file at or under 300 lines. Plain ES modules and CSS.
6. **Exploration evidence** (criterion of both tasks): try the Playwright browser tools once; if the tool server is not exposed (it has not been so far), say so and run the real page with the pinned `@playwright/test` and `/opt/pw-browsers/chromium` in a throwaway script outside the repository against a jar built from this tree (`mvn -o package -DskipTests` from `backend/`; the jar bakes the frontend at build time). Record per criterion what you observed (rating set and cleared, keyboard use, failure paths by stubbed responses incl. `INVALID_RATING`, `RATING_NOT_ALLOWED`, 404, 500, network; delete with Cancel, Escape, success, last delete, failed delete; hostile and 500-code-point titles). Mark it a scripted run (documented deviation). Own port, never 8080 or 18080, temp data dir mode 0700, stop every process you start. Run `npx playwright test --grep @smoke` from `e2e/` at the end of Task 38; it must stay green.

## Delivery (compact, constitution 1.7)
Write `docs/sdd/handoffs/192-frontend-to-liaison-chunk-37-38-delivery.md` early with a "Resume point": tasks, branch, commit hashes; files created or edited; one table per task mapping each criterion to its unit test and exploration observation; summary lines of `node --test "frontend/**/*.test.js"` and of the smoke run (tests, passed, failed, exit code; count unit tests and smoke separately); deviations; risks. No raw terminal output, no `git diff --stat`, no author log. Commit locally with explicit paths and `type(scope): subject [Task-37]` or `[Task-38]`, never `--no-verify`, nothing naming a tool, model or vendor, no trailers. Update `docs/sdd/progress.md`.

## Rules
STOP and report on any spec gap or contradiction, any need for a dependency, any change outside the listed files, or any backend behaviour that differs from the contract. Do not push.
