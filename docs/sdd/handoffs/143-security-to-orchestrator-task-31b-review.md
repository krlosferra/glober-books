# Handoff 143: Security review, Task 31b (rating and clear rating)
- From: security
- To: orchestrator
- Phase: 4   Round: 1
- Type: AUDIT_REPORT
- Task: 31b, branch `feature/task-31b-rating-endpoints` (HEAD fba4e53), pull request 38

## 1. Scope and method
Scope: `git diff origin/main..HEAD` for `RouteTable`, `BookController`, `BookService`, `BookValidationException`, `GlobalExceptionHandler`, their tests, brief 140 and delivery 141; `RatingValue`, `RatingRequest`, `Book.assignRating` read as context.
Method: code read; grep of every `new BookValidationException` / `new FieldProblem` / `getMessage` in `backend/src/main`; scratch copy of HEAD (`git archive`) built with `./mvnw -o -q -DskipTests package` and run on ports 18141 to 18145 with temporary databases; about 150 curl probes (`--path-as-is`); five fresh JVMs for cold-start timing. The Java test suite was not re-run (see section 4).

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-174 | INFO | DoS / timing | first request of a JVM | The 401 ms cold figure is not specific to nesting: the first request of any kind on a fresh JVM costs 200 to 306 ms (plain `{"rating":7}` 232 ms; 8,000 nested `[` 218, 306, 200 ms on three fresh JVMs); afterwards 5 to 9 ms, including the nested body and 2,000 nested `{"x":` | curl `%{time_total}` on ports 18142 to 18145 | None: one-time class loading and JIT, no amplification, parser stops at depth 16 | Optional: PM words T-31 as "after the first request of the JVM". No Sponsor action | SC-31 |
| SEC-F-175 | INFO | closure | rate/unrate | SEC-F-170 conditions met (section 3) | probes below | n/a | none | SC-11, SC-16 |
| SEC-F-176 | INFO | spec | T-17 / T-18 | `{}` is listed under INVALID_RATING (T-17) while `{"a":1}` is MALFORMED_REQUEST (T-18). `{"rating":{}}` gives MALFORMED_REQUEST, the empty body `{}` gives INVALID_RATING. Both fixed-message 400s | probes | none | PM to clarify the wording of T-17 | SC-31 |
| SEC-F-177 | INFO | integrity | Task 32 | Rollback on forced failure and the race of `rate` against `changeStatus` are not proved here; the one-connection pool serializes transactions today | code read (`SqliteDataSourceProvider`) | none reachable | Task 32 must prove it | SC-31 |

No CRITICAL, HIGH, MEDIUM or LOW finding.

## 3. Evidence by question
- **RATING_NOT_ALLOWED:** PUT `{"rating":4}` on an unread and on a reading book gives 400 `RATING_NOT_ALLOWED`, "Only completed books can be rated.", no details. Completed book accepts it (200).
- **INVALID_RATING:** 7, null, `{}` (empty body) on unread, reading and unknown ids give 400 `INVALID_RATING`, top text "Rating must be an integer from 1 to 5.", one detail `rating` / "must be an integer from 1 to 5". No received value appears in any body. The scratch log (36 lines) holds no rating text, no id and no value; its single ERROR line (from my own client-aborted chunked request) prints class and top frame only (`RequestGateFilter`).
- **Order:** marker (7) on an unknown id gives INVALID_RATING; valid 3 on an unknown id gives 404 `BOOK_NOT_FOUND`; valid on unread gives RATING_NOT_ALLOWED. Existence is observable only after the value is valid, as in the spec (SEC-F-173).
- **IllegalStateException:** `BookService.rate` pre-checks the status and calls `assignRating` only for COMPLETED; no catch of `IllegalStateException` in service or advice (only the catch-all `Exception` handler, which logs class and top frame and answers `INTERNAL_ERROR`). `GlobalExceptionHandlerTest.anUnexpectedDomainRefusalIsNeverMappedTo400AndGives500WithoutItsMessage` pins 500 with no message. Not reproducible at a real port (the pre-check makes the state unreachable), which is the intended result.
- **Atomicity:** `rate` and `unrate` are single `@Transactional` methods with `saveAndFlush`; the pool has one connection. Rating 5 then PUT status reading gave `rating: null`, `date_completed: null` (same transaction in 31a's `changeStatus`). Forced-failure rollback: Task 32.
- **Rating grammar at the real port** (completed book):
  - INVALID_RATING: `1e999999999`, `1e-999999999`, `-0`, `0`, `6`, `5.0`, `4.9`, `1E1`, `null`, missing member, 100 nines.
  - MALFORMED_REQUEST: 101 nines, `-` + 100 nines, `+` + 100 nines, `01`, `+5`, `NaN`, `Infinity`, `5.`, `.5`, `"5"`, `"abc"`, `true`, `false`, `[5]`, `[]`, `{"a":1}`, `{"rating":{}}`, `0x5`, `5e`, duplicate member, unknown member, `status` plus `rating`, trailing token, truncated body, array root, scalar root, empty body.
  - Timings 2 to 30 ms warm. 20 KB body: 413. 3 MB body declared or chunked: 413 in under 6 ms. Nested 8,000 `[` and 2,000 `{"x":`: 400 MALFORMED_REQUEST in 5 to 9 ms warm.
- **DELETE .../rating:** 200 twice with `rating: null`; on unread 200; unknown id 404; body `{}` or one space 400; `?x=1` 400; text/plain with body 415. Without any Content-Type the gate answers 415 on every non-GET method (spec 5.0 rule 1c, existing behaviour, same for `DELETE /{id}`).
- **Route and method:** GET, HEAD, POST, PATCH, OPTIONS give 405 `METHOD_NOT_ALLOWED` with `Allow: DELETE, PUT` exactly (HEAD has no body; POST, PATCH, OPTIONS with a JSON Content-Type). TRACE is the container page (SEC-F-079, R-13). `/rating/` is 404.
- **Ids:** upper-case id, `not-a-uuid`, `1%27%20OR%20%271%27%3D%271` give 400 MALFORMED_REQUEST on PUT and DELETE. `%2f` and `%00` give the known Tomcat HTML 400 (SEC-F-128, accepted family).
- **Headers/envelope:** the four checked headers (no-store, nosniff, CSP, X-Frame-Options) present on 200, 400 and 405 of the new path (counted 4 of 4); 404 and 413 envelopes JSON. Host forgery 400 `INVALID_HOST`; foreign Origin 403 `FORBIDDEN`.
- **Constants and allowlist:** `STATUS_MESSAGE` and `RATING_MESSAGE` are `public static final String` (immutable). `FIXED_MESSAGES` is applied only to Bean Validation default messages in `handleInvalidBody`; `RatingRequest` has no constraints. `handleValidation` forwards problems built in the service; all eight construction sites use a constant or `messageFor`, none takes request text. `getMessage` is not called anywhere in `src/main`. No route to echo user text.
- **No new logging:** the diff adds no logger or log call.

## 4. Controls checked
SC-08 VERIFIED (typed records, no new Object/Map/JsonNode). SC-11 VERIFIED (no stack, class or SQL in any rating error). SC-14 VERIFIED (no value or id in the scratch log). SC-16 VERIFIED (fixed messages, no echo). SC-18 VERIFIED (seventh route, `Allow` exact, no extra method). SC-29 VERIFIED for the new paths. SC-31 VERIFIED for the rating body through the real strict resolver and the real port.

## 5. Not verified
- Full `./mvnw clean verify` and the delivery's 900 plus 253 tests were not re-run; coverage and SpotBugs figures are the delivery's.
- Rollback on a forced failure and concurrent PUT rating against PUT status (Task 32).
- The IllegalStateException 500 path was verified by the delivery's tests and code read, not at a real port (unreachable by design).
- `-D` public advisory databases were not consulted: no dependency changed in this diff.
- The `Transfer-Encoding: chunked` empty-body DELETE probe was a curl artifact (client stalled, container 408 page); not a finding.

## 6. Residual risks
None requiring the Sponsor. Informational: SEC-F-174 (cold start), SEC-F-176 (T-17 wording for the PM), SEC-F-177 (Task 32).

## 7. Verdict
**PASS.** SEC-F-170 CLOSED; SEC-F-159 CLOSED for the body rules (real-port proof by the delivery and by my probes). Next step: Liaison audit and Sponsor gate, then the pull request. I edited `security.md` (v0.49) and the Security cell of Task 31b in `status.md`; nothing committed. Scratch files are in the session scratchpad; all test applications were stopped.
