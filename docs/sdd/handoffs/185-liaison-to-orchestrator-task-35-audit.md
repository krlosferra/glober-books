# Handoff 185: Audit, Task 35 (client endpoints)
- From: liaison | To: orchestrator | Type: AUDIT_REPORT
- Mode F. Commit 190decb, branch feature/task-35-client-endpoints. Inputs: tasks.md Task 35, handoffs 183 and 184, spec 5.3 to 5.7, E-07, BR-011, SEC-F-097, SEC-F-094.

## Verdict: ACCEPT Task 35 (Liaison). Security verdict still required (Security: per task).
Compliance: Layer 1 and 1b pass. Tests: Layer 2 pass. Proceed to Task 36 after Security and Sponsor.

## Re-run (by me)
`node --test "frontend/**/*.test.js"`: 99 tests, 99 pass, 0 fail, exit 0 (matches the delivery). `sources.test.js` (SEC-F-094 rules) is part of that run and passes.

## Criteria to evidence
| Item | Result |
|---|---|
| AC1 path via `bookPath` only; DELETE has Content-Type, no body | Pass. `bookRequest` builds every path with `bookPath`; the DELETE test asserts `'body' in init === false`, header `application/json`, paths `${BASE}/rating` and `BASE`. `requestJson` sets the header for every non-GET (E-07 met by construction) and a body only when defined. |
| Spec literals (5.4 to 5.7) | Pass. Bodies are asserted as exact strings `{"status":"completed"}` and `{"rating":5}`; methods PUT, PUT, DELETE, DELETE, GET; paths `/status`, `/rating`, none. |
| AC2 mapping and failures | Pass. camelCase map asserted; 404, INVALID_RATING with field message, RATING_NOT_ALLOWED (no details), 500, non-JSON 502, wrong-shape 200, network are asserted; server `error` text is shown absent. Gap (MINOR, not blocking): no `setStatus` INVALID_STATUS case (details with `field: status`, no message). `http.js` handles it and it is covered in Task 11 tests; Tasks 36/37 do not depend on it. |
| AC3 / BR-011 hostile ids | Pass, not vacuous. 15 hostile values (`../x`, `..`, `%2e%2e`, `%2E`, upper case, empty, 5000 chars, `id/status`, `id?x=1`, leading space, null, undefined, number, object, array) times all five functions, each `assert.rejects(TypeError)`, then `calls.length === 0` on a shared recorder. The recorder does record calls, so the zero is meaningful. Invalid status and rating values (NaN, Infinity, 2.5, '3', 0, 6) also give zero calls. |
| SEC-F-097 | Pass for this task: one test iterates GET, PUT x2, DELETE x2 and POST and asserts the header presence equals `method !== 'GET'`. Closing the row is Security's. |
| Files and limits | Pass. Only `booksApi.js` (140 lines), `booksApi.test.js` (219), `progress.md` and the handoff changed; both under 300. JSDoc on every export. No new dependency, no secrets, no AI attribution; commit message is Conventional with `[Task-35]`. Functions are under 50 lines. |

## Ruling on the implementer's decision
A refused id, status or rating rejects with a TypeError instead of a result. Acceptable, no spec or tasks line needed:
- It matches `bookPath` (Task 8/11 pattern) and the brief's closed set of failure kinds (`api`, `network`, `unreadable`); a fourth kind would change the Task 11 result contract (spec-level, out of scope).
- BR-011 treats a bad id as a programming error: Tasks 36 and 37 pass only `book.id` taken from a server book (already validated by `toBook`) and radio values from fixed controls, so the branch is unreachable in normal use.
- Residual risk is a UI handler that does not catch: controls stay disabled. Recorded as the active condition Liaison F-35-1 (target Tasks 36, 37), not a spec change. Note `toBook` checks only that `id` is a string, so a malformed server id would reach the TypeError path; the try/catch condition covers it.

## Findings
| ID | Severity | Finding | Resolution |
|---|---|---|---|
| F-35-1 | MINOR | TypeError contract needs a caller rule | Added to `active-conditions.md` for Tasks 36 and 37 |
| F-35-2 | MINOR | No INVALID_STATUS case in the new failure test | Optional; add when Task 36 touches failures |

## Process notes
No build or server started. Layer 3: `getBook`, `setStatus` etc. are `async` wrappers only to turn the synchronous throw into a rejection; intentional and consistent. Remind: update `progress.md` (already edited by the implementer), merge by pull request only after Security and Sponsor approval. SEC-F-094 stays open; SEC-F-097 is for Security to close.
