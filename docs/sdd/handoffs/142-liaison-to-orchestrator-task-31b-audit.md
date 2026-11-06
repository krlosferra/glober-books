# Handoff 142: Liaison audit of Task 31b (rating, clear rating, rating error mapping)
- From: liaison
- To: orchestrator
- Phase: 4   Round: 1
- Type: REVIEW_REPORT
- Mode: F (with G for oracles)
- Artifacts: branch `feature/task-31b-rating-endpoints` (7 commits over 8bb973f), brief 140, delivery 141, tasks.md 0.6.3 Task 31b, spec 5.5/5.6, BR-002/007/012/013, US-003, T-14/17/18/31/36/37, C-03, E-02, E-29, the code and tests.

## Verdict: ACCEPT Task 31b (Layers 1, 1b, 2 pass; Security verdict still required)
Follow-ups F-1 to F-3 below are non-blocking. Security (protected surface, reviewed alone) is not replaced by this audit.

## Judgments
(a) Ordering and IllegalStateException: `BookService.rate` judges `rating.isValid()` first, then `find(id)` (404), then `status != COMPLETED` gives `RATING_NOT_ALLOWED`, and only then `assignRating`. This is BR-012 steps 6 to 8. No catch of `IllegalStateException` in service or advice. Pinned twice: `BookRatingServiceTest.doesNotCatchAnUnexpectedDomainRefusal` (same exception surfaces, no save) and `GlobalExceptionHandlerTest...Gives500WithoutItsMessage`. The pin is at two layers (service, advice); there is no single end-to-end stub test, which is acceptable. `unrate` has no status check, which matches BR-013 (clear is idempotent and valid for any book).

(b) T-17 vs T-18: a misreading, not a code defect, but T-17 is ambiguous. US-003.5 says "An empty object is rejected with INVALID_RATING" (the whole body `{}`, a missing member). US-003.6 says a rating of type object is MALFORMED_REQUEST, and T-18 `{"a":1}` is that case. The Task 30 reader (`{"rating":{}}` is MALFORMED_REQUEST, body `{}` is INVALID_RATING) agrees with both US rows. T-17's `{}` therefore means the body `{}`. T-19 uses the same shorthand (`{}` for status, US-003/US-006.6 "an empty object"). The tests assert exactly this and cover `{"a":1}` as a value. No code change.
Proposal for the PM (spec change, MINOR): in T-17 write "the body `{}`" instead of bare `{}`, and add a row or a sentence for `{"rating":{}}` under T-18 so the shorthand cannot be read as a rating value. Same wording fix for T-19.

(c) Deviations: all three accepted.
1. `BookRatingControllerTest` as a new file: justified (BookControllerTest is 287 lines, Checkstyle limit 300; new file is 121). Brief said "edit"; record it. Tasks.md lists `BookControllerTest (edit)`: the PM should list the new file.
2. Two public constants in `BookValidationException`: justified by brief constraint 5 (do not triplicate texts); no new exception type; no behaviour change. Outside the file list, so record it as a plan/tasks note. Residual: `RequestGateFilter` still holds its own literal of the status message (out of scope; follow-up F-3).
3. `rate`/`unrate` names: allowed by note (a); SpotBugs reports 0 bugs per the delivery and my re-run showed no failure.

(d) Cold 401 ms vs T-31 "each answered within 300 ms": spec T-31 has no warm-up carve-out, and the test asserts the bound only after one warm-up call (and reports the cold figure, which is honest). Under a literal reading the first call after start-up misses the bound once. Not a blocker for this task: the cause is JIT on a 16 KiB nested body in a local single-user app, no 500, correct 400, and the earlier tasks used the same pattern. But it is a spec/test mismatch and must be recorded, not left implicit.
Recommendation: (1) PM adds "measured after one warm-up request of the same shape" to T-31 (or the Sponsor accepts the cold exception in a Decision Log entry); (2) Task 51 (final gate pass) pins a cold-start run for T-31 so the figure is a known, bounded number; (3) Security is informed (the first nested request is the largest parse cost, still bounded by the 16 KiB limit and the strict parser's depth handling). A Sponsor note is needed only if the PM prefers to keep T-31 literal.

(e) Oracles: expected values trace to the spec. `INVALID_RATING` set (7, 0, 6, -1, -0, null, 5.0, 4.9, 1e0, 1E1, huge, 1e999999999, 1e-999999999, 0.0...01, body `{}`, 100 digits) matches US-003.2/5, T-17, T-31, T-37; `MALFORMED_REQUEST` set (`"5"`, true, `[5]`, `{"a":1}`, 01, +5, NaN, Infinity, 5., .5, 0x5, 5e, 101 digits) matches US-003.6, T-18, T-37. One fixed message, `details` size 1, field `rating`, no echo asserted. 405 test asserts `Allow` equals "DELETE, PUT" and the JSON content type for GET, POST and PATCH (E-02, E-29). T-14 rating rows: six requests (PUT and DELETE times three ids) give `400 MALFORMED_REQUEST`. DELETE with a one-space body is 400, empty body 200 (E-18). C-03 rating step passes at 5,000 books. T-36 sequence (7 then 4 on an unread book) is tested. Weak spot: the `INVALID_RATING` rows assert `< 300 ms` on warm requests only, consistent with (d).

(f) Repository hygiene (Layer 1b): all 7 commits are by the Sponsor's identity, Conventional Commits with `[Task-31b]`, atomic by concern (feat, test, docs). No trailers, no tool/model/vendor names, no session links in messages (checked with `git log --format=%B`). Note: the system reminder for this session asks for a session trailer on commits I create; I created none, and the repository rule (CLAUDE.md, git-workflow.md) forbids it, so the orchestrator must not add one when committing this file.

## Layer results
- Layer 1: all six criteria satisfied (see tests in delivery section 3). Files touched: the four production files, `BookValidationException` (deviation 2), tests as listed plus the new slice file; no change to pom.xml, resources or schema (diff stat confirms). All files under 300 lines (largest new test 292). No logging added. `@Transactional` on both use cases; the failure-injection proof is Task 32 as the brief says.
- Layer 2: delivery reports 900 unit/slice + 253 integration tests green, coverage 100 percent on the edited classes, Checkstyle and SpotBugs clean. I re-ran `./mvnw -o -q clean verify`: the tail showed only INFO log lines and no BUILD FAILURE or test error. I did not capture the exit code, so treat the re-run as consistent with, not independent proof of, the delivery numbers.
- Layer 3: `rate` could return early with less duplication of `find`; none needed. Minor: Javadoc for `unrate` is fine.

## Follow-ups
- F-1 (PM, MINOR): disambiguate `{}` in T-17 and T-19 as the body `{}`; add the `{"rating":{}}` value case to T-18.
- F-2 (PM + Sponsor note or Task 51): resolve the T-31 cold-start wording as in (d).
- F-3 (PM, MINOR): tasks.md Task 31b test list gains `BookRatingControllerTest` and `BookValidationException` constants; plan note on the shared message constants and the remaining literal in `RequestGateFilter`.
- Security gate: `sdd-security-reviewer` verdict is required before the Sponsor gate (SEC-F-159, SEC-F-169, SEC-F-170). Then the Sponsor approves, the orchestrator pushes and opens the pull request (squash merge, `[Task-31b]`), and `progress.md` records the audit.
