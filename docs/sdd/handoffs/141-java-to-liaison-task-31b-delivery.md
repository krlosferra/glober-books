# Handoff 141: Task 31b delivery (rating, clear rating and the rating error mapping)
- From: java
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY

## Resume point
- Done: production code (`BookService.rate/unrate`, `BookController` two endpoints, `RouteTable` seventh route, `BookValidationException` message constants, `GlobalExceptionHandler` allowlist), unit, slice and real-port tests, full `./mvnw -o clean verify` green, progress.md and status.md row (IN_REVIEW), this delivery.
- Next: nothing for the implementer. The orchestrator pushes, the Liaison audits and Security reviews (protected surface, alone). Nothing is pushed; all commits are local.

## 1. Task, branch, commits
Task 31b, branch `feature/task-31b-rating-endpoints`. Commits on top of the amendment and brief commits: `02a6061` feat (production and unit/slice tests), `117b274` real-port tests, then the docs commit (progress, status) and the final delivery commit.

## 2. Files
Production (4, as listed in the task): `service/BookService.java` (`rate`, `unrate`), `controller/BookController.java` (`PUT` and `DELETE /{id}/rating`), `config/transport/RouteTable.java` (`BOOK_RATING`, seven routes), `error/GlobalExceptionHandler.java` (allowlist adds the rating message). One further production edit: `service/BookValidationException.java` gets two public message constants (`STATUS_MESSAGE`, `RATING_MESSAGE`) and a Javadoc fix, to consolidate the texts the brief asked not to triplicate; no new exception type was needed because `BookValidationException` already carries a code and detail entries.
Tests: `service/BookRatingServiceTest` (new), `controller/BookRatingControllerTest` (new), `error/GlobalExceptionHandlerTest` (edit), `config/transport/RouteTableTest` (edit), `BookRatingIntegrationTest` (new, real port). Docs: `progress.md`, `status.md` (own row), this file. No change to `pom.xml`, `backend/config/**`, the schema, `BookControllerTest` or anything else.

## 3. Criteria to code to tests
| Criterion | Code | Tests |
|---|---|---|
| 1 marker before existence and for unread/reading; valid on unknown id is 404 | `BookService.rate` | `BookRatingServiceTest.judgesTheInvalidMarkerBeforeLookingForTheBook`, `...BeforeTheStatusOfAnUnreadBook`, `anUnknownIdWithAValidRatingIsBookNotFound`; IT `ordersFieldValueBeforeExistenceAndExistenceBeforeState`, `anInvalidValueOnAnUnreadBookIsInvalidRating...` |
| 2 status pre-check, no catch of `IllegalStateException` | `BookService.rate` | `refusesAValidRatingOnABookThatIsNotCompleted...`, `checksTheStatusBeforeCallingTheDomainMethod` (verify never `assignRating`), `doesNotCatchAnUnexpectedDomainRefusal` (same exception surfaces), `GlobalExceptionHandlerTest.anUnexpectedDomainRefusalIsNeverMappedTo400AndGives500WithoutItsMessage` |
| 3 one transaction, unchanged on failure, 1 to 5, replacement, clear twice | `@Transactional` on both | `ratesACompletedBookWithEachValueFromOneToFive`, `replacesAnExistingRating`, `clearsTheRating...`, `clearingAnUnratedBookIsIdempotent...`, `eachRatingUseCaseIsOneTransaction...`; IT `ratesACompletedBookWithEachValue...`, `replacesTheRatingAndClearingTwice...`, `ratingAnUnreadOrReadingBook...Unchanged`, `leavingCompletedClears...` (S-02, S-03). The forced-failure proof is Task 32 |
| 4 real port codes and ordering | whole path | IT `invalidNumbersAndNull...` (14 values), `wrongTypesAndInvalidLiterals...` (12), `missingRatingMember...`, `strictBodyRulesHoldOnTheRealPort` (8, SEC-F-159), `deeplyNested...` (T-31 nested), `ordersField...` |
| 5 5,000 books | seeded library | IT `atFiveThousandBooksTheRatingStepOfTheCapacityScenarioSucceeds` |
| 6 routing | `RouteTable` | `RouteTableTest.holdsExactlyTheSevenApiRoutes`, `findsPutAndDeleteOnTheRating...`, `answersMethodNotAllowedWithDeleteAndPutForTheOtherMethodsOnTheRating`, `equalsTheMappingsOfTheControllersAndTheCatalog`; IT `getOnTheRatingIsMethodNotAllowedWithAllowDeleteAndPut`, `malformedIdIsMalformedRequestOnBothRatingMethods`, `deleteRatingWithABodyIsMalformedRequestAndWithoutOneIsOk` |

Spec rows: 5.5 and 5.6 (slice and IT bodies, 200/400/404), S-01, S-02, S-03, S-05, T-14 rating rows, T-17, T-18, T-31 (three numbers, nested), T-32, T-36, T-37 (100 digits INVALID_RATING, 101 MALFORMED_REQUEST), C-03 rating step, E-02 rating row, E-29, SEC-F-170 (RATING_NOT_ALLOWED, fixed message, one transaction), E-18 (one-space body on DELETE rating is 400, empty body 200).

## 4. Test inventory
Unit: `BookRatingServiceTest` 17. Slice: `BookRatingControllerTest` 5. Advice: `GlobalExceptionHandlerTest` +2 (18 total). Routing: `RouteTableTest` 58 (+2 new, 1 renamed to seven routes). Real port: `BookRatingIntegrationTest` 52 (parameterized rows included). Weak-oracle care: expected codes come from the spec rows above, not from observed output.

## 5. Build output
`cd backend && ./mvnw -o clean verify`: BUILD SUCCESS. Unit/slice phase `Tests run: 900, Failures: 0, Errors: 0, Skipped: 0`; integration phase `Tests run: 253, Failures: 0, Errors: 0, Skipped: 1` (the skip is in `SqliteStoreIntegrationTest`, not touched). Checkstyle clean, SpotBugs `BugInstance size is 0` (method names `rate` and `unrate` do not trip EI_EXPOSE_REP2; no exclusion), JaCoCo merged check passed. Line coverage of the new and edited classes (JaCoCo csv): `BookService` 67/67, `BookController` 11/11, `RouteTable` 39/39, `GlobalExceptionHandler` 46/46, `BookValidationException` 11/11 (100 percent each).

### Real-port request and response table (elapsed ms from the test client; warm unless noted)
| Request | Status | Code | ms |
|---|---|---|---|
| PUT rating 1..5 on completed | 200 | - | 8 to 24 |
| PUT rating 4 then 2 (replacement) | 200, 200 | - | 24, 9 |
| DELETE rating, twice | 200, 200 (rating null) | - | 10, 6 |
| PUT 4 on unread / on reading | 400 | RATING_NOT_ALLOWED | 5, 5 |
| PUT 7 on unread, null on reading, then 4 on unread | 400, 400, 400 | INVALID_RATING, INVALID_RATING, RATING_NOT_ALLOWED | 5, 9, 6 |
| PUT `7`, `0`, `6`, `-1`, `-0`, `null`, `5.0`, `4.9`, `1e0`, `1E1`, 23 nines, `1e999999999`, `1e-999999999`, 0.0...1, `{}`, 100 nines | 400 | INVALID_RATING | 4 to 33 |
| PUT 101 nines, `01`, `+5`, `"5"`, `true`, `[5]`, `{"a":1}`, `NaN`, `Infinity`, `5.`, `.5`, `0x5`, `5e` | 400 | MALFORMED_REQUEST | 4 to 7 |
| PUT duplicate member, unknown member, status+rating, truncated, array, scalar, trailing doc | 400 | MALFORMED_REQUEST | 2 to 7 |
| PUT nested 8,000 `[` (cold first call 401 ms, reported only; warm) / 2,000 nested `{"x":` | 400 | MALFORMED_REQUEST | 401 cold, 9, 9 |
| PUT 7 and `null` on unknown id | 400 | INVALID_RATING | 5, 7 |
| PUT 4 on unknown id; DELETE rating on unknown id | 404 | BOOK_NOT_FOUND | 7, 6 |
| PUT/DELETE rating, id upper case / `not-a-uuid` / `1%27%20OR%20%271%27%3D%271` | 400 (each of six) | MALFORMED_REQUEST | 2 to 6 |
| GET, POST, PATCH `.../rating` | 405, `Allow: DELETE, PUT`, JSON | METHOD_NOT_ALLOWED | 8, 2, 2 |
| DELETE rating with a one-space body / without body | 400 / 200 | MALFORMED_REQUEST / - | 2 / 13 |
| 5,000 books: PUT status completed, PUT rating 4, DELETE rating | 200, 200, 200 | - | 70, 9, 8 |

The cold-start figure of the first nested request (401 ms) is above 300 ms because of JIT; T-31's bound is asserted after one warm-up call (the same pattern as earlier tasks). Raised for the Liaison as a risk, not hidden.

## 6. Deviations
1. The rating slice tests are in the new `BookRatingControllerTest`, not in `BookControllerTest`: that file is 287 lines and the additions would exceed 300 (Checkstyle FileLength). One file outside the brief's list.
2. `BookValidationException` (not in the brief's file list) gets two constants. It is the existing carrier of code plus details, so no new exception type and no `ApiException` change. The gate's own copy of the status message in `RequestGateFilter` is out of scope and still a literal.
3. Method names `rate` and `unrate` instead of `assignRating`/`clearRating` (note (a) of the task).

## 7. Risks and open questions
- T-17 lists `{}` among the rating values with `INVALID_RATING`, T-18 lists `{"a":1}` as `MALFORMED_REQUEST`. The Task 30 reader treats a rating value of `{}` (as a member value, `{"rating":{}}`) as a type mismatch, so `MALFORMED_REQUEST`; the empty body `{}` (missing member) is `INVALID_RATING`, which is what I test. If T-17 means `{"rating":{}}`, the spec rows disagree with the accepted Task 30 reader; I did not change either. Please confirm with the PM.
- `ErrorCode.INVALID_RATING` top-level text is "Rating must be an integer from 1 to 5." and the detail is "must be an integer from 1 to 5" as in 5.5; no value is echoed anywhere (asserted on the body).
- SEC-F-170: the status check and the write run in one `@Transactional` method; the forced-failure and concurrency proofs stay with Task 32.

## 8. Git evidence
```
$ git diff --stat origin/main..HEAD   (before the final commit of this file)
 .../globerbooks/config/transport/RouteTable.java   |  13 +-
 .../globerbooks/controller/BookController.java     |  25 ++
 .../globerbooks/error/GlobalExceptionHandler.java  |   3 +-
 .../globant/globerbooks/service/BookService.java   |  44 +++-
 .../service/BookValidationException.java           |   8 +-
 .../globerbooks/BookRatingIntegrationTest.java     | 292 +++++++++++++++++++++
 .../config/transport/RouteTableTest.java           |  26 +-
 .../controller/BookRatingControllerTest.java       | 121 +++++++++
 .../error/GlobalExceptionHandlerTest.java          |  31 +++
 .../globerbooks/service/BookRatingServiceTest.java | 185 +++++++++++++
 .../140-orchestrator-to-java-task-31b-brief.md     |  26 ++
 .../141-java-to-liaison-task-31b-delivery.md       |  67 +++++
 docs/sdd/plan.md                                   |   1 +
 docs/sdd/progress.md                               |   5 +
 docs/sdd/status.md                                 |   4 +-
 docs/sdd/tasks.md                                  |   7 +-
 16 files changed, 846 insertions(+), 12 deletions(-)

$ git log origin/main..HEAD --format='%an <%ae>%n%B---'
krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 31b delivery [Task-31b]
---
krlosferra <carlos.ferra@globant.com>
docs(progress): Task 31b status and progress entry [Task-31b]
---
krlosferra <carlos.ferra@globant.com>
test(api): real-port rating tests for Task 31b [Task-31b]
---
krlosferra <carlos.ferra@globant.com>
feat(api): rating and clear rating endpoints with INVALID_RATING mapping [Task-31b]
---
krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 31b brief, Task 31a done [Task-31b]
---
krlosferra <carlos.ferra@globant.com>
docs(tasks): amend Task 31b with E-02, E-29 and T-14 rows, plan note on discard [Task-31b]
---
```

Branch not pushed. No trailers or session links in any commit message.
