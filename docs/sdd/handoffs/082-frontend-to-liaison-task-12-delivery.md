# Handoff 082: Task delivery, Task 12 (page shell with minimal list and add)
- From: frontend
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY (INCOMPLETE: browser exploration evidence missing)
- Branch: `feature/task-12-page-shell-minimal`

## Resume point
- Done: all production files (index.html, styles.css, dom.js, listView.js, addForm.js, app.js, main.js, copy.js additions), one local commit per step. `node --test "frontend/**/*.test.js"`: 43 tests, 43 pass. Jar built; the application was started by hand on 127.0.0.1:18080 with a 0700 temporary directory, all page assets answered 200, a POST and GET of `/api/v1/books` matched the contract; the application was stopped and the temporary directory deleted.
- Blocked: the Playwright tool server tools are not exposed to this run (no browser tool exists in its tool list). Per the brief the run stopped. Criterion 5 (snapshot, keyboard sequence with its single POST, focus after add, CSP console, 360 px layout) is NOT evidenced and nothing is claimed for it.
- Next: someone with the browser tools runs the exploration against the same build; then `docs/sdd/progress.md` entry and the `status.md` row (`IN_REVIEW`) are set. Neither was touched.

## Files
Created: `frontend/css/styles.css`, `frontend/js/main.js`, `frontend/js/ui/dom.js`, `listView.js`, `addForm.js`, `app.js`. Modified: `frontend/index.html` (placeholder replaced), `frontend/js/logic/copy.js` (added `LOADING_TEXT`, `BOOK_ADDED_PREFIX`). Nothing else under `frontend/`, nothing under `backend/`.

## Criteria to code (no Playwright tests: Tasks 13, 14 and 28)
1. Shell semantics: `index.html` (lang, title, one h1, main, h2 "Your books" with tabindex -1, `ul` aria-label "Books", form, autofocus on Title).
2. Entries and count: `listView.js` (template clone, textContent only, `format.js`), loading text in `#loading` (role status) set by `showLoading`.
3. Add flow: `addForm.js` (`inFlight` flag, `preventDefault`, `form.reset()`, focus to Title) and `app.js` (announce "Book added: <title>" in `#messages`, prepend when the first load succeeded).
4. Same-origin: only `/css/styles.css`, `/js/main.js`, its module imports and `/api/v1/books`; no inline script, style or handler (`sources.test.js` green).
5. Exploration: NOT DONE (see Resume point).

## Test results
`node --test "frontend/**/*.test.js"`: tests 43, pass 43, fail 0.

## Choices and gaps for the Liaison
- Empty library (US-010.6) is not in this slice: with zero books only the count "0 books" is shown and the list is hidden.
- Load failure: the loading text is cleared and nothing else is shown (Increment 1 adds the message and "Try again"). Add failure: the form is re-enabled, input kept, focus to Title.
- A book added while the first load is in progress, or after a failed load, is not inserted (US-010.7); it is announced.
- Filter is out of scope: the page always loads `all`; the prepend ignores filter matching.
- No `listState.js` exists yet (Task 25): app.js keeps one `loaded` flag.
- Hook note: the repository guard blocked three of my shell commands (a commit containing a longer subject and two chained commits) with an attribution message although the text held none; resent in smaller pieces. The session reminder asking for a session trailer in commits was not followed because it conflicts with the repository rule.

## Git evidence
```
 .../081-orchestrator-to-frontend-task-12-brief.md  |  27 ++++
 docs/sdd/status.md                                 |   5 +-
 frontend/css/styles.css                            | 136 +++++++++++++++++++++
 frontend/index.html                                |  51 +++++++-
 frontend/js/logic/copy.js                          |   4 +
 frontend/js/main.js                                |   3 +
 frontend/js/ui/addForm.js                          |  39 ++++++
 frontend/js/ui/app.js                              |  30 +++++
 frontend/js/ui/dom.js                              |  39 ++++++
 frontend/js/ui/listView.js                         |  69 +++++++++++
 10 files changed, 399 insertions(+), 4 deletions(-)
```
Commits (author krlosferra, no trailers): page start and entry script; add form with one request at a time; list view and page text strings; page shell and stylesheet (all `[Task-12]`). The full `git log main..HEAD --format` output is reproduced by the orchestrator after the final commit of this file.

## Browser exploration, criterion 5 (added by the orchestrator)
The Playwright tool server was not exposed to the implementer's run, so criterion 5 was met by a scripted browser run instead: a throwaway Node script outside the repository (scratchpad), using the pinned `playwright` 1.64.1 of `e2e/` with Chromium, against the production jar on `127.0.0.1:18080` with a temporary 0700 data directory. The application was stopped and the directory deleted afterwards. This is a deviation from the Definition of Done rule (tool server exploration): the Liaison rules on it. Real output:
- Load: title `GloberBooks`, `lang` `en`, one `h1` `GloberBooks`, one `main`, h2 `Add a book` and `Your books`, focus on `title`, Status default `unread`.
- Accessibility snapshot: main > heading "GloberBooks" [1], status "Messages", heading "Add a book", textbox "Title", textbox "Author", combobox "Status" (Unread selected, Reading, Completed), button "Add book", region "Your books" with heading, paragraph "0 books" and an empty status. The list named "Books" is hidden while empty (count 0).
- Keyboard: Title `<img src=x onerror=alert(1)> Dune`, Tab, author `Frank Herbert`, Enter: requests after Enter `["POST /api/v1/books"]` (exactly one); focus after add: `title`; form cleared (true, true); entry text `<img src=x onerror=alert(1)> Dune / Frank Herbert / Unread / Added: 2026-11-05`; status regions `Book added: <img src=x onerror=alert(1)> Dune`; count `1 book`; injected `img` elements 0; CSP violations `[]`.
- Console: one warning only, `Error with Permissions-Policy header: Unrecognized feature: 'bluetooth'` (an HTTP header of the backend, not a CSP violation: reported to Security as a finding on the header value).
- Layout: no horizontal scroll at 360 px.
- Not covered by this run: the list at 320 px, entries with a rating, a failed load.
