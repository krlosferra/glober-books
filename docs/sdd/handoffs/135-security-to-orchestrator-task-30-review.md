# Handoff 135: Security review, Task 30 (book transitions, rating literal, status request)
- From: security
- To: orchestrator
- Phase: 4   Round: 1
- Type: AUDIT_REPORT
- Brief: 132; delivery 133

## 1. Scope and method
Branch `feature/task-30-rating`, head 94cfd1c, `git diff origin/main..HEAD` (pull request 35, protected surface, reviewed alone). Read the production diff (`Book`, `RatingValue`, `RatingRequest`, `StatusRequest`), `JacksonConfig`, `StrictJsonBodyResolver`, `schema.sql`, and the delivery. In a scratch copy of HEAD (`git archive`): ran `BookTest`, `RatingValueDeserializerTest`, `RatingBodyFullContextTest` (151 tests, 0 failures; the full-context test needs the `frontend` folder for static content, so the copy took the whole tree), and a throwaway probe test (scratch only, not in the repository) that builds the mapper from the real `JacksonConfig.strictFactory` and `strictMapper`. I did not run the whole `verify` build (the orchestrator build check owns it).

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-169 | INFO | Parser limits | `RatingValue.Reader` | Jackson `maxNumberLength` counts digits only; the reader adds a text-length check so BR-002 (literal as written, 100 characters) holds. Sound for the rating. | Probe: minus plus 100 digits and a 101-character float give MismatchedInputException; 100 digits gives the invalid marker; 101 digits gives StreamConstraintsException. All are JacksonException, mapped to MALFORMED_REQUEST by the resolver | None for the rating | Any future numeric body field must repeat the text check or share a helper. There is none today | SC-31 |
| SEC-F-170 | LOW | Error mapping | `Book.assignRating`, Task 31 | The domain throws `IllegalStateException` (plan names no type). If the Task 31 service skips the BR-012 step 8 pre-check, the caller gets 500 INTERNAL_ERROR instead of RATING_NOT_ALLOWED. Fails closed, nothing written, nothing leaked | Code read; message is a fixed string | Wrong status code only | Task 31 review checks: unread and reading give RATING_NOT_ALLOWED; the marker gives INVALID_RATING with a fixed message and no value echo; status change and rating run in one transaction. PM records the exception type in the plan | SC-11, SC-29 |
| SEC-F-171 | INFO | Integrity | `Book`, `schema.sql` | Domain and schema agree (details below). No action | See section 3 | None | None | SC-08 |
| SEC-F-161 | INFO | Parser limits | `JacksonConfig` | CLOSED. MAX_TOKENS 64 and MAX_DEPTH 16 fit every rating and status body (5 tokens, depth 1); no collection body exists | Probes below | None | Revisit only if an array body is ever added | SC-31 |

No CRITICAL, HIGH or MEDIUM finding.

## 3. Controls checked
- SC-31 (rating literal): VERIFIED. The reader calls `parser.getString()` on a `VALUE_NUMBER_INT` or `VALUE_NUMBER_FLOAT` token; no `getIntValue`, `getDecimalValue`, `getDoubleValue` anywhere. Valid only if the text is one character 1 to 5; any other integer text, any float token and `null` (via `getNullValue`) give the marker; every other token goes to `handleUnexpectedToken` (type mismatch, MALFORMED_REQUEST). Probe results (scratch, real config), time per input:
  | Input | Result | Time |
  |---|---|---|
  | `1e999999999` | invalid marker | 63 ms cold, 0 ms warm (delivery: 0.3 ms warm; limit 300 ms) |
  | `-1e999999999`, `1E+999999999999999999999` | invalid marker | 0 ms |
  | 100 nines | invalid marker | 0 ms |
  | 101 nines | StreamConstraintsException | 4 ms |
  | minus and 100 nines (101 chars) | MismatchedInputException (own check) | 1 ms |
  | `1.` plus 97 zeros plus `e5` (101 chars) | MismatchedInputException (own check) | 0 ms |
  | 17 nested `[`, 100,000 nested `[` | MismatchedInputException at the first token, no recursion | 0 ms |
  | 100 array items, 70-char member name, 20,000-char string | fail as JacksonException | 0 ms |
  | whitespace around 5, escaped member name `rating` | 5 | 0 ms |
  The resolver catches `JacksonException` only, and every constraint failure in Jackson 3 is one; the 16 KiB body cap and the gate come earlier.
- SEC-F-159 proof: VERIFIED. `RatingBodyFullContextTest` picks the resolver from the application's real `RequestMappingHandlerAdapter` (asserts it is `StrictJsonBodyResolver`, first in line), with the application's mapper and validator; it reuses the same literal lists as the unit test. The finding stays open for Task 31 slices.
- SC-08: VERIFIED. New types are records of `Integer` or `String`; no `Object`, `Map`, `JsonNode`, interface or `@JsonTypeInfo`; unknown members rejected by the existing mapper switch.
- SC-16: no new text field; `StatusRequest` reuses `CreateBookRequest.StatusDeserializer` and an exact-match `@Pattern` (no trim, no case folding). Missing and `null` status become the empty text and fail the pattern on field `status` (INVALID_STATUS); non-strings are MALFORMED_REQUEST. Same semantics as Task 20/21.
- SC-11 (no echo): messages are fixed strings ("rating needs a completed book", "rating out of range", "number too long"); no logger, no `System.out` in the diff (grep). The resolver replaces any Jackson message by a fixed `MalformedRequestException`.
- Domain invariants: VERIFIED by code and tests. `changeStatus`: same state returns without touching anything; entering completed sets `dateCompleted` from the passed `Clock`, truncated to milliseconds (same as the 24-character stored form), rating untouched (null); leaving completed sets rating and date to null together; the clock is not read on those paths (test uses a clock that throws). `assignRating` refuses a non-completed book first, then values outside 1 to 5, and changes nothing on refusal.
- Schema: `ck_books_rating` (rating null, or 1 to 5 and status completed) and `ck_books_completed` ((status = completed) = (date_completed not null)) are present in `schema.sql`, unchanged by this branch; the existing repository rows exercise direct violations on a real SQLite file.
- Dependencies, pom, resources, schema: no change in the diff (stat lists only Java sources, tests and docs).

## 4. Not verified
- Full `./mvnw clean verify` and JaCoCo figures (reported by the delivery; orchestrator build check).
- End-to-end behaviour of the endpoints: they belong to Task 31. Whether the advice yields INVALID_RATING with details `rating` for the marker is untested here (nothing wires it yet).
- A real-port probe: not possible, no endpoint exists in this task.
- Public advisory databases: not queried (no dependency change).

## 5. Residual risks
None new for the Sponsor. SEC-F-170 is a Task 31 carry-over, not a residual risk.

## 6. Verdict
PASS WITH CONDITIONS. Task 30 may merge. Condition C-1 (LOW, SEC-F-170) is checked at the Task 31 security review: RATING_NOT_ALLOWED for a non-completed book, INVALID_RATING for the marker with a fixed message and no echo, one transaction. SEC-F-161 closed; SEC-F-159 stays open for slices of Task 31. Next step: Liaison audit, then Sponsor gate and merge of pull request 35.

Files updated: `docs/sdd/security.md` (v0.47), `docs/sdd/status.md` (Task 30 row added).
