# Handoff 118: Liaison audit of Task 20 (validation wiring and field-error mapping)
- From: liaison | To: orchestrator | Phase: 4 | Round: 1 | Type: REVIEW_REPORT
- Mode: F (with G for oracles). Artifacts: handoffs 116 and 117, `git diff d23be69..HEAD`, `git log d23be69..HEAD`, spec BR-001, BR-006, BR-012, US-001.4 to .6 and .10, T-09, T-13, T-34, T-35, tasks.md Task 20.
- Own check: `./mvnw -o clean verify` re-run, exit clean; surefire plus failsafe XML report 765 tests, 0 failures (delivery states 611 + 154).

## Verdict: ACCEPT WITH FOLLOW-UPS
Condition: fix F-1 (one-line test change) in a further commit before the pull request is opened. Everything else may be carried. Security verdict is separate and not replaced by this one.

## Acceptance criteria
| # | Result | Evidence |
|---|---|---|
| 1 title/author failures, one entry per field, order title, author, status; `{}` gives title and author | PASS | `GlobalExceptionHandler.handleInvalidBody` keeps first message per field (`putIfAbsent`), sorts by `FIELD_ORDER`; record components in the same order; tests in 117 table exist and map to T-09, US-001.10 |
| 2 only status gives INVALID_STATUS; status plus other gives VALIDATION_ERROR; absent is unread; explicit null is INVALID_STATUS | PASS | handler `onlyStatus` rule matches BR-012 step 6 and BR-001 exception; `StatusDeserializer` (absent stays null, null becomes ""); `@Pattern` rejects "". Integration case covers both. T-34, T-35 covered |
| 3 T-13 | PASS in substance, see F-1 | 501-code-point title with `${7*7}` rejected; interpolator returns template unchanged; message set comes from a fixed list; unit, MockMvc and real-port cases exist; log lines checked |
| 4 thin checks removed, same exception type | PASS | `BookService` now calls `TextPolicy.check/normalize` and throws `BookValidationException`; no `outOfRange` left |

## Constraints
- No expression language, no new dependency, no pom or resource-folder change: PASS (diff touches only `src` and docs).
- Messages never built from user input: PASS. `messageFor` uses only the limit constant. Handler maps any other message to "is invalid".
- No logging of request text in new code: PASS (no logger added).
- Files at or under 300 lines: PASS (largest changed file is the 157-line integration test).
- Check order required, empty, length, characters: delegated to `TextPolicy.check`; PASS. Carry items (item 6 after NFC, Persian word from code points): PASS (`BookTextTest` rows 105 and 114).
- Commits: five commits plus the delivery commit; Conventional Commits with `[Task-20]`; author is the human identity; no AI attribution in messages, trailers, diff content (grep for tool, vendor, co-author, session text over log and diff: no hit). Atomic enough. PASS (Layer 1b).

## Fixed-message list versus spec
The handler list (`is required`, `must not be empty`, length 500 and 200, `contains characters that are not allowed`, status message) matches the spec only partly in text:
- Status text: exact in spec lines 423 and 438. Length text: exact in spec line 436 (title); the author variant follows the same pattern. Characters text: exact in BR-006.
- "is required" and "must not be empty" are NOT written in the spec. BR-006 names only the categories "required (null or missing), empty, length, characters". The wording comes from the brief and the implementer. The category selection is spec-backed; the exact words are not. See F-2.

## The five deviations
1. `BookValidationException.messageFor`: ACCEPTED. It removes duplication between constraint and service (the brief asks for one rule list). Minor layering note: the `config` validator imports a `service` exception for a message table; acceptable now, a neutral home could be chosen later.
2. Nested `CreateBookRequest.StatusDeserializer`: ACCEPTED. It is the only way found to tell absent from null (BR-001 exception) and the attempt with `@JsonSetter` is reported. It is a file-list deviation inside an already listed file. Task 21 must keep the behaviour when it replaces the deserializer (see F-3). It is also an unrecorded plan gap: PM or plan owner should add it to plan amendments.
3. Corrected Task 07 oracles, verified against the spec text:
   - `""` title now "must not be empty" (was the length message): BR-006 line 278 orders empty before length, and "including an empty result" is a violation of item 5; correct.
   - `null` title "is required": BR-006 "required (null or missing)": category correct (wording see F-2). `"   "` is empty after trim, so "must not be empty": correct (item 2 then 5).
   - `{}` order title then author: US-001.10 and BR-012 step 6 ("order of entries: title, author, status"); the old author-then-title oracle contradicted the spec. Correct, and the added `details.length()==2` assertion is right.
   - The `BookApiIntegrationTest` edit follows the same rule. All corrected oracles are spec-derived, not code-derived.
4. Branch name `feature/task-20-validation` versus header `...-validation-wiring`: ACCEPTED as a recorded difference; PM should align the header (same class as follow-up F-6 of Task 17).
5. Diff base d23be69: ACCEPTED; the stat in 117 matches my own `git diff --stat` (my count is 16 files because the delivery file itself is added).

## Findings
| ID | Severity | Location | Finding | Resolution |
|---|---|---|---|---|
| F-1 | MAJOR (flaky oracle) | `BookValidationIntegrationTest.hostileTitleIsRejected...` | Asserts `response.body()` `.doesNotContain("49")`, but the body includes the real-clock `timestamp` (minute, second or millisecond digits can contain "49"): roughly 5 percent of runs fail at random. The spec rule is "no message contains 49", not "no body" | Assert on the `details[].message` values (or the code and details only), keep the `${7*7}` check on the whole body. Same tests in `BookTextTest` and handler test are fine (fixed clock 10:00:00.123, message-only) |
| F-2 | MINOR | spec BR-006 / Appendix | Exact words "is required" and "must not be empty" are not in the spec; tests now pin them | PM: add the four text messages to Section 5.0 or BR-006 and a Decision Log line; until then they are brief-defined |
| F-3 | MINOR | Task 21 | Absent versus null `status` and the `StatusDeserializer` must survive the strict reader; add a Task 21 acceptance line/test for `"status": null` and absent | PM to carry into Task 21 |
| F-4 | MINOR | `GlobalExceptionHandler.FIXED_MESSAGES` | Duplicates the message list of `messageFor` and the status pattern text in three places (record, service constant, handler) | Single constants holder when Task 30 adds the rating message; not blocking |
| F-5 | MINOR | delivery 117 | The `status.md` row for Task 20 is absent; the diff also edits `status.md` only for the 18+19 DONE flip | Orchestrator adds the Task 20 row (IN_REVIEW, audit handoff 118) |
| F-6 | INFO | `isWhiteSpace` regex | Optional replacement still open (SEC-F-154) | carried |

## Next step
Orchestrator: route F-1 fix to the implementer (one commit), send to the Security reviewer, then Sponsor gate and pull request. Reminder: update `progress.md` and commit `[Task-20] ... - ACCEPTED` after the Sponsor's approval. I did not commit anything.
