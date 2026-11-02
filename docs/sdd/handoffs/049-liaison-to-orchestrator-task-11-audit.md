# Handoff 049: REVIEW_REPORT, Task 11 audit (Mode F)

- From: liaison | To: orchestrator (for Sponsor and PM) | Date: 2026-11-02
- Task: Task 11, branch `feature/task-11-logic-and-client-thin`, commit 94cfa93 plus the brief commit
- Artifacts reviewed: handoffs 046 and 048; `frontend/js/logic/{copy,format,ids}.js`, `frontend/js/api/{http,booksApi}.js`, `frontend/js/sources.test.js`, `api/http.test.js`, and the test names of `booksApi`, `format`, `ids` and `copy`; spec US-010.3, US-010.4, US-013.3, US-013.6, DEC-017, Appendix A; plan-appendix AP-7.2 and AP-7.3.

## Verdict: ACCEPT WITH FOLLOW-UPS

Evidence limit: I did not re-run the tests. I rely on the orchestrator's run (`node --test "frontend/**/*.test.js"`, Node 24.22.2, 43 pass) and on the pasted output in 048. Security verdict from `sdd-security-reviewer` is still required before the Sponsor gate.

## Layer 1 and 2: acceptance criteria

| AC | Result | Evidence |
|---|---|---|
| 1 Runner | Met | 43 pass, no `package.json` added, no module warning reported; the no-match glob observation (0 tests, exit 0) is recorded for AP-10 |
| 2 Format and ids | Met | `statusLabel`, `countText` (1, 0, 2, 1000), `ratingText` (rated, Not rated, empty), `localDate` uses local getters; three TZ tests for the `2026-02-19T23:30:00.000Z` value give 2026-02-20, 2026-02-19, 2026-02-19 (US-010.3, US-010.4); `isBookId` is a lowercase UUID regex and `bookPath` throws for an invalid id (BR-011) |
| 3 Client | Met | Only `/api/v1/books` literals and `?status=` for the three values; `Accept` always, `Content-Type` on non-GET only; `redirect: 'error'`, `cache: 'no-store'`, `credentials: 'omit'`; `allow_duplicate` only when `=== true`; closed set of 16 codes; `error` and `timestamp` not copied; result type never throws for HTTP outcomes. Tests cover 201, 200, 400 x2, 409 x2, 404, 500, 502 HTML, `{}`, unknown code, fetch rejection, malformed `details` |
| 4 Timeout | Met | `setTimer(function () { controller.abort(); }, 15000)`; timer cleared in `finally`; test asserts delay 15000, function argument, aborted signal, clear on early response, caller abort. Body read is covered because the race with the abort promise includes `response.text()`; the test uses a body that never completes |
| 5 Sources | Met | The checker has every required sink; self-tests accept the function-argument timers and flag the string, quote and template forms; `fetch(` is allowed only in `api/http.js`; the bracket-concatenation heuristic is added and labelled as a heuristic |

Scope: only the files of brief section 4 are created, plus the progress entry and handoff 048. No `index.html`, CSS, UI, `package.json` or dependency. Constitution NEVER list: no secrets, no vendor-dir edits, no removed tests, and no forbidden sink in the production sources (scan clean over 5 files). No scope creep found. Layer 1b (git hygiene) not re-audited here; the orchestrator's `hygiene` run and the hooks cover it.

## Rulings on the delivery's choices

1. DUPLICATE_BOOK with a missing or malformed `existing_book` classified `unreadable`: **accepted, not a CHANGE_REQUEST.** DEC-017 fixes `details` as `{existing_book}` for this code and spec 5.0 fixes the book shape, so a 409 without a readable book is not a valid error envelope for the client. Treating it as a broken contract follows the intent of US-013.3 (page maps `unreadable` to `INTERNAL_ERROR`) and avoids opening a dialog with nothing to show. The gap is only that US-013.3 lists three triggers (not JSON, missing `code`, unknown `code`) and not this one. Follow-up F-1: the PM may add one clarifying clause to US-013.3 as a MINOR editorial change (no behavior change); not a gate for Task 11. Task 12 or the later task that builds the duplicate dialog must treat `unreadable` here as `INTERNAL_ERROR`.
2. Stricter `toBook` (status in the three values, integer rating, string types): **accepted.** Spec 5.0 fixes the member types and the status enumeration. Residual: rating range 1 to 5 is not checked; that is the server's invariant, and a stricter client check would be a new rule. No action. Note for the future: if a status value is ever added to the spec, `toBook` must change in the same task.
3. Timeout covering the body read: **accepted and required** by CR-4 ("no complete response") and by brief AC4. The abandoned `work` promise cannot cause an unhandled rejection because its errors are caught inside.
4. Single TZ test file: **accepted.** The brief allowed it if one process switches zones reliably; 048 reports it does (`TZ` set as first statement and in each test) and the three expected dates pass, so the oracle is not weak. Residual: this relies on Node re-reading `TZ` at runtime on Linux; if the Task 14 hygiene or CI run moves to another OS and one test fails, split per zone then (F-2, watch item).
5. `sources.test.js` heuristic: **accepted.** Regex gate plus an explicit heuristic for concatenated bracket access; production files only, `*.test.js` excluded and stated; patterns are built from parts so the file does not trip itself; self-tests use in-memory strings. It is a grep and cannot prove absence of evasion (constitution line 241 keeps review as the backstop). Note `eval` is matched only with a following `(` and `import` only with `(`; this fits the brief's "dynamic `import(`" and is adequate.

## copy.js and format.js against the plan

- `copy.js`: holds exactly the status labels and the prefixes "Added: ", "Completed: ", "Rating: " and "Not rated", each quotable from US-010.3. Plan.md line 66 describes `copy.js` as "every exact UI string"; the brief deliberately limited it to this task's strings, and the other strings (5.9 error texts, dialogs, announcements) are owned by later tasks. No invented text. Follow-up F-3: when `failures.js` and the UI tasks add strings, they must go through `copy.js` as plan.md says; Task 12 brief should state that.
- `format.js`: matches AP-7.2 (`statusLabel`, `localDate`, `countText`, `ratingText`, `matchesFilter`). `matchesFilter` is pure: `all` matches everything, otherwise status equality. An unknown filter matches nothing, which is the safe result. `"Completed: YYYY-MM-DD"` is built from `COMPLETED_PREFIX` plus `localDate` by the caller; no function yet composes it. That is consistent with the brief (no composing function listed) and with Task 12 AC2.
- Minor (non-blocking, Layer 3): `localDate` returns an empty string for an invalid input and `statusLabel` returns an empty string for an unknown status; both are silent fallbacks, acceptable here because `toBook` validates input upstream.

## Numbered follow-ups

- F-1 (PM, MINOR, optional): add a clause to US-013.3 that a `DUPLICATE_BOOK` response without a readable `existing_book` is also treated as `INTERNAL_ERROR`. Not a CHANGE_REQUEST for the gate; PM decides whether to batch with other editorial changes.
- F-2 (watch): TZ tests in one file depend on runtime `TZ` switching; split per zone if any other environment fails.
- F-3 (Task 12 brief): all new UI strings come via `copy.js`; `failures.js` owns 5.9 texts.
- F-4 (process): `sdd-security-reviewer` verdict on Task 11 is pending (note in 048 gives the facts: single `fetch`, library timer, request options, no dependency).
- F-5 (process): the tasks.md Task 11 line says `Depends on: 10`; Task 10 is merged, so no dependency issue. After approval, the orchestrator updates `status.md` and `progress.md` audit notes; commit message already ends with `[Task-11]`; no PR before the Sponsor approves at the gate.

## Next step

Security verdict, then Sponsor approval of Task 11. Then brief Task 12 (depends on 11 and 09).
