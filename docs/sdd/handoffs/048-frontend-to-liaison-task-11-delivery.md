# Handoff 048: TASK_DELIVERY, Task 11 (Frontend logic and API client, thin)

- From: frontend | To: liaison | Type: TASK_DELIVERY | Date: 2026-11-02
- Task: Task 11 | Branch: `feature/task-11-logic-and-client-thin` | Commit: see `git log` (subject ends with `[Task-11]`)

## Files
Created under `frontend/js/`: `logic/copy.js`, `logic/format.js`, `logic/ids.js`, `api/http.js`, `api/booksApi.js`, tests `logic/format.test.js`, `logic/ids.test.js`, `logic/copy.test.js`, `api/http.test.js`, `api/booksApi.test.js`, `sources.test.js`. Modified: `docs/sdd/progress.md` (Task 11 section). Created: this handoff. Nothing else changed.

## Evidence
- `node --version`: `v24.22.2`; `.nvmrc`: `24.22.2`. `find . -name package.json -not -path '*/node_modules/*'`: only `./e2e/package.json`.
- `node --test "frontend/**/*.test.js"`: tests 43, pass 43, fail 0, exit 0. No module-syntax warning; AC1 holds without a `package.json` (no constitution issue).
- No-match glob (`"nodir/**/*.test.js"`): `tests 0, pass 0, fail 0`, exit code 0 (observation for AP-10: an empty match does not fail).
- Negative run (scratch copy outside the repo, `setTimeout("abort()", 15000);` appended to `api/http.js`): `✖ production sources contain no forbidden sink`, `AssertionError ... api/http.js`, pass 5, fail 1. Scratch copy deleted; nothing in the repo.

## AC to tests
| AC | Tests |
|---|---|
| 1 | whole run |
| 2 | `statusLabel ...`, `countText ...`, `localDate Pacific/Auckland / America/Los_Angeles / UTC`, `ratingText ...`, `matchesFilter ...`, `isBookId ...`, `bookPath ...`, `copy holds the exact strings` |
| 3 | `api/booksApi.test.js` (all), `api/http.test.js` (status/code cases, `request options`, `details of the wrong shape`, `fetch rejecting is network`) |
| 4 | `timeout: no complete response after 15000 ms ...`, `a response before the timer clears it ...`, `a caller-supplied abort is network` |
| 5 | `sources.test.js` (checker self-tests, `production sources contain no forbidden sink`) |

## Notes and deviations
- `localDate` tests set `process.env.TZ` as the first statement of the file and again inside each test; one process switched zones correctly, so one test file is enough.
- Timer functions are injected (`setTimer`/`clearTimer`); the test asserts delay `15000` and a function argument. The timer covers the body read: a race against the abort signal ends a body that never completes.
- `sources.test.js` scans production files only (`*.test.js` excluded); patterns are also built from parts. The concatenated bracket-access check is a heuristic. Self-tests use in-memory strings.
- A `DUPLICATE_BOOK` without a usable `details.existing_book` is classified `unreadable` (my choice; the spec does not state it). `toBook` also rejects a status outside the three values or a non-integer rating as the wrong shape (types of the seven members are in spec 5.0).
- `existingBook`/`fields` only for api failures; the envelope `error` and `timestamp` are not copied (a test checks it).
- `matchesFilter` returns false for a book when the filter is an unknown value other than `all`.

## For sdd-security-reviewer
Forbidden-sink scan: clean over 5 production files. `fetch` only in `api/http.js`. Timeout: `AbortController` with `setTimeout(function () {...}, 15000)`, cleared in `finally`; no `AbortSignal.timeout`. Request options: `redirect: 'error'`, `cache: 'no-store'`, `credentials: 'omit'`; root-relative URLs only. No dependency, no `package.json`.
