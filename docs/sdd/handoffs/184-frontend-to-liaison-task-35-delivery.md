# Handoff 184: Task delivery, Task 35 (client endpoints)
- From: frontend | To: liaison | Type: TASK_DELIVERY

## Resume point
Done: Task 35 implemented and unit-tested. Branch `feature/task-35-client-endpoints`, one local commit `[Task-35]` (not pushed). Next: Liaison audit and Security review.

## Files edited
`frontend/js/api/booksApi.js` (140 lines), `frontend/js/api/booksApi.test.js`, `docs/sdd/progress.md`. Nothing else.

## Criteria to tests
| Item | Test (booksApi.test.js) |
|---|---|
| AC1 path via id builder, DELETE with Content-Type and no body | getBook; setStatus; setRating; SEC-F-097 clearRating and deleteBook |
| AC2 camelCase book; api, network, unreadable with fields | getBook (mapped); failures test (404, INVALID_RATING fields, RATING_NOT_ALLOWED, 500, non-JSON, wrong shape, network, server `error` text absent) |
| AC3 / BR-011 refused before any request | BR-011 hostile id test (`../`, `%2e`, upper case, empty, 5000 chars, null, number, object, array, suffix and query injection) for all five functions; zero fetch calls |
| Constraint 2 validated bodies | invalid status and rating test (zero fetch calls); exact bodies in setStatus and setRating |
| SEC-F-097 | "every non-GET wrapper carries Content-Type, GET does not" plus the DELETE test |
| SEC-F-094 | `sources.test.js` still passes |

Command `node --test "frontend/**/*.test.js"`: 99 tests, 99 passed, 0 failed, exit 0.

## Deviations and risks
- Decision for the Liaison: a refused id, status or rating is a rejected promise (TypeError, as `bookPath` already throws), not a failure result, because the brief allows only the kinds `api`, `network`, `unreadable`. The UI tasks (36, 37) must only pass ids taken from server books. Say so if a result-shaped refusal is preferred.
- Exports added: `getBook`, `setStatus`, `setRating`, `clearRating`, `deleteBook`.
