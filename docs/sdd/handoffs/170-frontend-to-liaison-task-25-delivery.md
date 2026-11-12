# Handoff 170: Task 25 delivery (list state, filter and list states)
- From: frontend | To: liaison | Type: TASK_DELIVERY
- Branch: `feature/task-24-text-failure-logic` (chunk branch); commits 0082ac5 (model, copy, tests), 0d93b5c (UI); a third commit holds this handoff, status and progress. Not pushed.

## Resume point
Done: all five criteria implemented and explored. Next: Liaison and Security review; Task 26 (note below); Task 28 writes the E2E tests.

## Files
Created: `frontend/js/logic/listState.js`, `listState.test.js`. Edited: `logic/copy.js` (list texts), `ui/app.js`, `ui/listView.js`, `index.html`, `css/styles.css`, `docs/sdd/status.md` (row 25), `docs/sdd/progress.md`. Nothing else.

## Criteria
| # | Unit tests (listState.test.js) | Exploration (scripted) |
|---|---|---|
| 1 | initial All; loadStarted records filter; copy texts exact | group "Filter by status", All checked; ArrowRight on All sent `?status=unread` only |
| 2 | new load clears entries; only the latest response counts (success and failure) | "Loading your books…" only, count hidden, 0 items; a slow Unread response was discarded after Reading |
| 3 | empty vs filterEmpty decided by count, incl. after last delete | empty library message; stubbed empty Reading body gave "No books with this status." + "Show all books", which checks All and loads (focus moved to "Your books" heading) |
| 4 | failure: text, no entries, no count; add ignored before success, during load, after failure; replaced/removed on cleared entry: state equal, announce true, no reload during loading | backend stopped: 5.9 no-response text, Try again, 0 items, no count; add not inserted; Try again repeated `?status=unread` |
| 5 | count arithmetic, non-matching add, noLongerMatches still counts | 3 books on All = "3 books"; non-matching add under Unread unchanged; matching add 2 books, new entry first |

## Tests
`node --test "frontend/**/*.test.js"`: 79 tests, 79 passed, 0 failed, exit 0 (includes the source gates of sources.test.js, SEC-F-094). Playwright smoke not run (no E2E change; Task 28 tests this task).

## Deviations and notes
- Exploration is a scripted run, not the Playwright MCP (tool server not exposed to this run). Real packaged jar on port 18931 (temporary data directory, mode 0700) behind a throwaway static proxy on 18932 serving `frontend/` with the jar's CSP, because the jar serves the older frontend baked at build time and ignored a static-location override; pinned `@playwright/test` with `/opt/pw-browsers/chromium`. Delays, one stubbed empty body used `page.route`. Processes stopped, data removed. 0 CSP violations, no horizontal scroll at 320 px.
- Focus: Try again and Show all books move focus to the "Your books" heading because the button is hidden afterwards (spec silent on this; flag for review).
- Empty and filter-empty states also show the count "0 books" (US-010.4: shown whenever the list is shown; "0 books" is specified).
- The list status region is `#list-status` (was `#loading`); it carries loading, empty, filter-empty and failure texts, role status.
- bookReplaced and bookRemoved return `{state, announce, applied}` (remove adds `reload`) for Tasks 26 and later; not wired yet (no entry operations exist).

## Risks
- Task 26: `parseFields` in `api/http.js` drops a `status` entry without a string `message`, which CR-7 treats as usable (not fixed here).
- Re-rendering replaces all entries on each state change (up to 5,000); acceptable now, revisit if Task 28 measures a delay.
