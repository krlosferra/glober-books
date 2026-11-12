# Handoff 168: Task 24 delivery (frontend text and failure logic)
- From: frontend | To: liaison | Type: TASK_DELIVERY | Phase 4, Round 1
- Task: 24. Branch: `feature/task-24-text-failure-logic` (the task header names `feature/task-24-text-and-failures-logic`; the checked-out name was kept). Commits: local, see `git log` (not pushed).

## Resume point
Done: `text.js`, `failures.js`, `copy.js` edit, three test files, progress entry. Next: Liaison review; decide the two findings below before Task 26 uses `failures.js`.

## Findings that need a decision (STOP items, work finished with the spec-conformant choice)
1. Brief vs spec: the brief and SEC-F-095/132 wording say the server's `fields[].message` is never rendered and `failures.js` returns only `copy.js` strings. Spec US-011.6 and 5.9 say the `title`/`author` message from the response is shown beside the field ("the text shown equals the `message` of the response"), and `active-conditions.md` asks for `textContent` rendering of `fields[].message`. The spec wins by authority: `describeFailure` returns, for a usable `VALIDATION_ERROR`, `fields: [{field, text}]` where `text` is the server message for title/author (data, textContent only) and the fixed status text for status. The server `error` text is never read or returned. If the Sponsor wants fixed texts only, a spec change is needed.
2. Source gate vs quoted string: the INVALID_HOST text contains `http://localhost`, which the `external URL` gate of `sources.test.js` forbids in production files. `copy.js` builds it with `${'http'}://` so the gate stays unchanged; the asserted string is identical to the spec.
3. Risk for Task 26 / `api/http.js`: `parseFields` drops entries without a string `message`, so a `status` entry with no message (usable per CR-7) never reaches `failures.js`; also `fields` only carries what http.js kept. Not changed (out of scope).
Note: the timeout is already mapped to `kind: 'network'` by `http.js`; `describeFailure` also accepts `kind: 'timeout'`.

## Files
Created: `frontend/js/logic/text.js`, `text.test.js`, `failures.js`, `failures.test.js`. Edited: `frontend/js/logic/copy.js`, `copy.test.js`, `docs/sdd/progress.md`. Nothing else.

## Criteria and strings to tests
| Item | Test (file) |
|---|---|
| C1 500 emoji, 500 decomposed e-acute, 300 x U+1D15E = 600 | `US-011.3 T-07` x2, `US-011.3 T-22` (text.test.js) |
| C1 empty/whitespace `empty`; 501 title, 201 author `length` | `US-011.3: empty and white space only...`, `...501 title and 201 author...` |
| BR-006 item 2 White_Space list (U+0085, A0, 2028, 2029, 3000 trimmed; U+FEFF not) | `BR-006 item 2: trims exactly the White_Space characters` |
| C2 every Appendix A code and non-envelope cases, unknown code | `US-013.1 5.9: ...` tests, `US-013.1 .3 ...`, `US-013.2 US-013.3 US-013.6 ...` (failures.test.js) |
| C3 VALIDATION_ERROR fallback, usable entries, `error` never shown | `US-011.6 CR-7 ...`, `CR-7: a status entry...`, `US-013.5 ...` |
| C4 strings: "Choose Unread, Reading or Completed.", "Choose a rating from 1 to 5.", "Only completed books can be rated.", library full text (US-011.9), "This book no longer exists.", request-defect text, host text, internal text, no-response text (US-013.2) | each in failures.test.js and `copy holds the failure texts...` (copy.test.js) |
| Hostile input, markup in `code` not echoed | `hostile input does not throw...` (both), `hostile code values are never echoed` |
Strings quoted in US-011/012 outside the failure table (dialog, "Book added: ", "Book deleted: ") belong to UI tasks; `Book added: ` already exists in copy.js. No quoted string appears twice with different wording.

## Checks
`node --test "frontend/**/*.test.js"`: tests 69, pass 69, fail 0, exit code 0 (includes `sources.test.js` source gates). No Playwright, no backend, no dependency.
