# Handoff 071: Liaison audit, Task 07 (add and list API, thin)
- From: liaison
- To: orchestrator
- Type: REVIEW_REPORT
- Mode: F (with G on oracles)
- Artifacts reviewed: 069, 070, BookService, BookController, CreateBookRequest, BookResponse, IdConfig, GlobalExceptionHandler (grep of the new handlers only), application.yml. Tests were skimmed through the delivery inventory, not re-read line by line. The `./mvnw clean verify` green result and the git facts (5 commits, human identity, no attribution) are taken from the orchestrator. `tasks.md` and the spec sections were not re-read in this pass; criteria are checked against the brief's restatement in 069.

## Verdict: ACCEPT WITH FOLLOW-UPS (Layer 1, Layer 2 and 1b pass; no blocking item). Security verdict is still required from sdd-security-reviewer.

## Criteria 1 to 5 (against the brief)
1. POST 201 with seven members: code and evidence consistent (defaults to unread, id from the supplier, time from the Clock cut to ms, `date_completed` = `date_added` when created as completed). PASS.
2. List newest first, ties by id, empty gives `{"books":[],"total":0}`: repository method `findAllByOrderByDateAddedDescIdAsc`; evidence lists Newest, Tie A, Tie B, Oldest. PASS.
3. Validation and unknown member: empty/blank, 501 and 201 code points give 400 `VALIDATION_ERROR` with the field; unknown member gives `MALFORMED_REQUEST` with no details. Lengths are measured in code points. PASS.
4. One transaction for create, `readOnly` for list, controller holds no rule. Annotation test is a weak oracle by itself, but the integration test (file hash and row count unchanged) covers the real behaviour. PASS.
5. Streaming: see item 4 below. PASS WITH FOLLOW-UP.

## Carried conditions
1. SEC-F-118: `spring.jpa.database-platform` set in yml and a startup test without test properties (overrides only the DB file and port) on `ddl-auto: validate`. PASS.
2. Wire format: contract test on the real port, snake_case, null written, 24-character dates. PASS.
3. SEC-F-119: reflection test, controller uses only DTOs (confirmed in code). PASS.
4. SEC-F-071: PASS WITH FOLLOW-UP (item 4).
5. X-07: DB hash and row count asserted. PASS.
6. No logging: no log statements in the service or controller. PASS.

## Rulings on the deviations
1. `add` renamed to `create` (SpotBugs EI_EXPOSE_REP2): ACCEPT. Renaming is the correct fix; an exclusion would weaken a gate. Naming is internal and not part of the contract. Note for later tasks: avoid service method names starting with `add`/`set`-style prefixes, or the same finding returns.
2. `@JsonFormat` on `BookResponse` vs global config: ACCEPT the per-DTO annotation now. It is explicit, local and proved on the real port. However, the pattern is duplicated on each date field and the constant is package-private. Follow-up (item 1): when Task 09 or later adds a second DTO with instants (for example the update-status response), do not copy the pattern again. Put the format in one place, either a shared annotation-free Jackson `spring.jackson.*` / `JsonMapper` customizer for `Instant`, or reuse `BookResponse.STAMP`. A global customizer is the better end state because it makes a new date field correct by default and it keeps the error-body `timestamp` consistent (check that the error body's `timestamp` already has 3 digits; the evidence shows it does, from its own factory). Record it as a plan note, not a Task 07 defect.
3. Extra files (`BookValidationException`, `IdConfig`, two integration test classes): ACCEPT. Each is required by the brief (supplier injection, startup test, wire-format contract test, X-07 test) or by layering (the service must not depend on web exception types). Follow-up (item 2): the PM or orchestrator must add them to the Task 07 file list in `tasks.md` or `plan.md` so that traceability stays true ("only task-listed files modified").
4. Edits to the Task 06 `GlobalExceptionHandler`: ACCEPT. Adding mappings is additive and necessary, and it is the one place error bodies are built (the controller builds none). Follow-up (item 3): confirm that `GlobalExceptionHandlerTest` was not weakened, and that Security checks that `MethodArgumentNotValidException` details echo only the field name and fixed messages, never the rejected value (the evidence shows fixed messages, so likely fine).
5. Streaming evidence type-level only (SEC-F-071): ACCEPT for Task 07, with a follow-up. The code is genuinely free of `byte[]`/`String` buffering in the controller: it returns a DTO and the converter writes to the stream. The reflection test is an acceptable proxy at this size. But note two honest limits. (a) The service materializes a `List<BookResponse>` before serialization, which is the plan's D7 design (list returned after the transaction), so the "stream" is the HTTP write, not a lazy cursor. (b) The test would not catch a later `ResponseBodyAdvice` or filter that buffers. Follow-up (item 4): when Task 15/16 or the capacity task bounds list size, add one observation-level test (for example a `ContentCachingResponseWrapper` absence check on the filter chain, or a large-list response that is read incrementally) and let Security decide whether SEC-F-071 is closed or stays open at the per-gate level.
6. Spec gap F4 (blank title plus bad status reports only the title; details order): not a Task 07 defect, since the brief said basic validation only. Two real consequences: (a) a mixed failure will report fewer problems than BR-012 step 6 requires; (b) the details order must be title, author. Follow-up (item 5): PM to record F4 in the Decision Log or as a note in Task 20 (and Task 18-19) so that the acceptance criteria say: all invalid fields reported in one response, order title, author, then status; the Bean Validation boundary must not short-circuit the service check. Recommendation: in Task 20 drop `@NotBlank` from the DTO and let the service own all text rules, which removes the two-source ordering problem.
7. Slice tests need `Host: localhost:80`: ACCEPT. It is correct that the slice includes the gate filter. Follow-up (item 6): put the header in a shared test helper so later slice tests do not repeat it, and check the same applies to the contract tests of Tasks 08-09.

## Additional observations
- Strip uses `String.strip()` (not the BR-006 policy) and is already flagged for Task 18. The comment in `BookService` says so. Fine.
- Two sources of the same limits: `@NotBlank` on the DTO and `check` in the service. Risk of drift is low now and is resolved by item 5.
- `allow_duplicate` is a `Boolean` accepted and ignored, as scoped. A non-boolean value (for example `"yes"`) gives a Jackson error mapped to `MALFORMED_REQUEST`: acceptable, confirm in Task 21.
- Delivery file 070 still lists "Remaining: Liaison audit"; the orchestrator should update the resume point and `progress.md` on acceptance.
- Layer 1b: 5 commits, human identity, `[Task-7]` suffix, no attribution (as reported by the orchestrator). Not independently re-run. Layer 3: no concerns.

## Numbered follow-ups
1. Single source for the instant format (global Jackson customizer preferred) before the next DTO with dates.
2. Add the four extra files to the Task 07 file list in the backlog.
3. Security to confirm the new handlers never echo rejected values; confirm `GlobalExceptionHandlerTest` unchanged or only extended.
4. Observation-level streaming test when list bounds arrive; Security to rule on SEC-F-071.
5. PM to record F4 and the details order (title, author, status) in Tasks 18-20.
6. Shared `Host: localhost:80` helper for slice tests.

Next step: Security verdict, then Sponsor approval, then push and pull request by the orchestrator. Task 08 is unlocked after both.
