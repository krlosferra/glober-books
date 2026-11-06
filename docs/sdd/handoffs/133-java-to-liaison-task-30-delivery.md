# Handoff 133: Task delivery, Task 30 (book transitions and value types)
- From: java
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY
- Branch: `feature/task-30-rating` (the task header names `feature/task-30-book-transitions`); local commits only, not pushed

## Resume point
- Done: domain transitions and `BookTest`; `RatingValue`, `RatingRequest`, `StatusRequest`, `RatingValueDeserializerTest`, `RatingBodyFullContextTest`; progress.md note; `./mvnw clean verify` green; this delivery.
- Next: nothing for the implementer. Orchestrator: Liaison audit and Security review (protected surface, reviewed alone), then push and pull request after the gate.

## Files
Created: `dto/RatingValue.java`, `dto/RatingRequest.java`, `dto/StatusRequest.java`; tests `domain/BookTest`, `dto/RatingValueDeserializerTest`, `dto/RatingBodyFullContextTest`. Modified: `domain/Book.java`, `docs/sdd/progress.md`. Nothing else (no pom, no resources, no schema, no `status.md` change by me; its diff in the stat below comes from the brief commit). All files under 300 lines (Book 211).

## Criteria to tests
| Criterion / spec row | Code | Tests |
|---|---|---|
| 1. BR-008, US-006.3: entering completed sets the date from the clock, rating null | `Book.changeStatus` | `BookTest.enteringCompletedSetsTheCompletionDateFromTheClockAndLeavesRatingNull` (unread and reading; the clock carries nanoseconds, expected value is the millisecond one), `returningToCompletedGivesTheNewTimeAndNoRating` |
| 1. BR-004, US-006.4: leaving clears rating and date together | same | `leavingCompletedClearsRatingAndCompletionDateTogether`, `leavingCompletedWithoutRatingClearsTheCompletionDate` (clock that throws if read) |
| 1. BR-004 second case, BR-008, US-006.5: same-state change changes nothing | same | `completedToCompletedChangesNothing`, `unreadAndReadingSwitchKeepRatingAndDateNull` |
| 2. BR-007, INV-1, INV-2: rating refused on a non-completed book, book unchanged | `Book.assignRating` (`IllegalStateException`) | `bookThatIsNotCompletedRefusesARatingAndStaysUnchanged`, `completedBookTakesEachRatingAndReplacesIt`, `ratingOutsideOneToFiveIsRefusedAndTheOldOneStays`, `clearingTheRatingKeepsStatusAndCompletionDate` |
| 2. schema CHECKs repeat the rule | `schema.sql` (exists, unchanged): `ck_books_rating`, `ck_books_completed` | existing `BookRepositoryTest` rows "rating on unread", "rating on reading", "rating 0", "rating 6", "date_completed on unread/reading", "completed without date_completed" fail on a real SQLite file. Verified, not changed. |
| 3. T-17, T-18, T-31, T-32, T-37, BR-002 | `RatingValue.Reader`, `RatingRequest` | `RatingValueDeserializerTest` (bare strict mapper) and `RatingBodyFullContextTest` (SEC-F-159: the application's own mapper and validator, and the resolver chosen from the real handler adapter, which comes before the framework processor), same literal lists |
| StatusRequest, T-19, US-006.6, BR-001 | `StatusRequest` (reuses `CreateBookRequest.StatusDeserializer`) | `RatingBodyFullContextTest`: `Completed`, empty, `null`, `{}` (missing member), padded values fail the pattern on field `status` (the advice gives INVALID_STATUS); `5`, `true`, `[]`, `{}` as value, unknown member, duplicate member, lone surrogate give MalformedRequestException |
| T-36, T-38 (order of checks), BR-004 end to end | not in this task | Task 31 (endpoints, advice) |

## Rating literal table (full-context, real resolver; expected from the spec, observed identical)
| Input as written | Expected | Observed |
|---|---|---|
| 1, 2, 3, 4, 5 | valid | valid |
| 0, 6, -1, -3, -0, 10, 55, 4.9, 5.0, 1e0, 1E1, 1.0E0, null, body `{}`, 100 nines, minus and 99 nines, 0.0...01 (100 characters), 1e999999999, 1e-999999999, 0.0000000000000000000000000000000000000001 | INVALID_RATING | invalid marker |
| "5", "", true, false, [5], [], {} as value, {"a":1}, 01, +5, NaN, Infinity, -Infinity, 5., .5, 0x5, 5e, 101 digits, minus and 101 digits | MALFORMED_REQUEST | MalformedRequestException |
| minus and 100 nines (101 characters), 0.00..01 over 100 characters, -0.0..01 (101 characters) | MALFORMED_REQUEST (BR-002: literal as written over 100 characters) | MalformedRequestException, through my own length check (finding 1) |
| duplicate rating, rating plus status in either order, trailing text, `[5]`, `5`, empty body | MALFORMED_REQUEST | MalformedRequestException |

Elapsed for `1e999999999`: 0.3 to 0.4 ms warm (probe, two runs; the first parse of any number in a cold JVM took 92 ms), asserted under 300 ms in both test classes. No BigDecimal is built: the text is judged without conversion.

## Findings (decisions needed)
1. Jackson's number-length constraint (`MAX_NUMBER` 100) counts digits, not the sign: minus and 100 digits passed it and gave INVALID_RATING, where BR-002 says a literal longer than 100 characters as written is MALFORMED_REQUEST. I added a text-length check in `RatingValue.Reader` (no change to `JacksonConfig`). Security should confirm; the digit-only count applies to any later number field.
2. SEC-F-161: `MAX_TOKENS` 64 and `MAX_DEPTH` 16 are enough for every rating and status body tested (largest legal body: 5 tokens). Not changed.
3. The plan names no domain exception for a rating on a non-completed book. I used `IllegalStateException` (the domain must not depend on the error package); the Task 31 service pre-checks state (BR-012 step 8) and maps it to `RATING_NOT_ALLOWED`. Plan gap for the PM and Liaison.
4. Carried to Task 31: `GlobalExceptionHandler` gives a single-field code only for `status`; the rating marker needs INVALID_RATING with the `details` entry `rating` ("must be an integer from 1 to 5"). The advice is not in Task 30's file list, so it is untouched. `RatingValue.INVALID` and `isValid()` are the hooks.
5. Branch name differs from the task header (recorded above).
6. `getAbsentValue` was dropped from the reader as dead code (the record's compact constructor handles a missing member). Two throw lines of `RatingValue.Reader` show as uncovered in JaCoCo.

## Build evidence
`cd backend && ./mvnw clean verify`: BUILD SUCCESS (Checkstyle, 844 unit and slice tests, 0 failures; 186 integration tests, 0 failures, 1 skipped (pre-existing, SqliteStoreIntegrationTest); JaCoCo gate). New tests: BookTest 23, RatingValueDeserializerTest 55, RatingBodyFullContextTest 73.
Line coverage (merged): project 890/920 = 96.7 percent; Book 34/34, RatingValue 4/4, RatingValue.Reader 12/14, RatingRequest 4/4, StatusRequest 4/4.
Self-audit: no logging, no user text in messages (fixed strings), no Object members, no new dependency, no System.out.

## git diff --stat origin/main..HEAD (before this delivery commit)
```
 .../java/com/globant/globerbooks/domain/Book.java  |  58 ++++++-
 .../com/globant/globerbooks/dto/RatingRequest.java |  18 +++
 .../com/globant/globerbooks/dto/RatingValue.java   |  81 ++++++++++
 .../com/globant/globerbooks/dto/StatusRequest.java |  25 +++
 .../com/globant/globerbooks/domain/BookTest.java   | 168 +++++++++++++++++++++
 .../globerbooks/dto/RatingBodyFullContextTest.java | 167 ++++++++++++++++++++
 .../dto/RatingValueDeserializerTest.java           | 119 +++++++++++++++
 .../132-orchestrator-to-java-task-30-brief.md      |  24 +++
 docs/sdd/status.md                                 |   4 +-
 9 files changed, 660 insertions(+), 4 deletions(-)
```

## git log origin/main..HEAD --format='%an <%ae>%n%B---' (before this delivery commit)
```
krlosferra <carlos.ferra@globant.com>
feat(dto): rating literal, rating and status requests [Task-30]
---
krlosferra <carlos.ferra@globant.com>
feat(domain): status and rating transitions on Book [Task-30]
---
krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 30 brief, chunk 23 done [Task-30]
---
```
