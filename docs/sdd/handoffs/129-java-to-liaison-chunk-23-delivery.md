# Handoff 129: Delivery, review chunk 23 (Task 23a add rules, Task 23b list filter and add bound)
- From: java
- To: liaison (and Security reviewer)
- Phase: 4   Round: 1
- Type: TASK_DELIVERY
- Brief: handoff 128

## Resume point
- Done: Task 23a (commit 9d7092b), Task 23b (commit after it, see log), progress.md updated, both builds green, 2-second bound proved on the first run (no schema change, no stop).
- Next: nothing for the implementer. This handoff file is committed in a third commit (docs). Not pushed; the orchestrator pushes. Status.md rows for 23a and 23b are not in the board table, so no row was edited (orchestrator to add).
- Branch: `feature/task-23-add-rules` (headers name `feature/task-23a-add-rules` and `feature/task-23b-list-filter-add-bound`; chunk branch kept, as the brief says).

## git diff --stat origin/main..HEAD (before this handoff file)
```
 .../globerbooks/controller/BookController.java     |  12 +-
 .../globant/globerbooks/repository/BookKeyRow.java |  26 +++
 .../globerbooks/repository/BookRepository.java     |   9 +
 .../globant/globerbooks/service/BookService.java   |  53 +++++-
 .../globant/globerbooks/service/DuplicateKey.java  |   9 +-
 .../globerbooks/AddBoundIntegrationTest.java       | 118 +++++++++++++
 .../globerbooks/AddRulesIntegrationTest.java       | 188 +++++++++++++++++++++
 .../globerbooks/BookListIntegrationTest.java       | 169 ++++++++++++++++++
 .../RequestGateFilterIntegrationTest.java          |   2 +-
 .../globerbooks/controller/BookControllerTest.java |  19 ++-
 .../globerbooks/repository/BookRepositoryTest.java |  15 ++
 .../service/BookServiceAddRulesTest.java           | 157 +++++++++++++++++
 .../globerbooks/service/BookServiceTest.java       |  24 ++-
 .../globerbooks/service/DuplicateKeyTest.java      |  15 ++
 .../globant/globerbooks/support/LibrarySeeder.java | 123 ++++++++++++++
 .../128-orchestrator-to-java-chunk-23-brief.md     |  26 +++
 docs/sdd/progress.md                               |   8 +
 docs/sdd/status.md                                 |   2 +-
 18 files changed, 955 insertions(+), 20 deletions(-)
```

## Author log (origin/main..HEAD)
```
2af73e8 krlosferra <carlos.ferra@globant.com>
feat(api): Task 23b list status filter and the 2-second add bound [Task-23b]
---
9d7092b krlosferra <carlos.ferra@globant.com>
feat(service): Task 23a add rules, capacity and duplicate detection [Task-23a]
---
6e5c9e8 krlosferra <carlos.ferra@globant.com>
docs(handoff): chunk 23 brief, Task 22 done [Task-23a]
---
```

## Files
Production: `service/BookService.java` (edit), `repository/BookRepository.java` (edit: `findAllBy()`), `repository/BookKeyRow.java` (new), `controller/BookController.java` (edit), `service/DuplicateKey.java` (only `isWhiteSpace` made package-private for the F-1 test). Tests: `BookServiceAddRulesTest` (new), `BookServiceTest`, `BookControllerTest`, `DuplicateKeyTest`, `BookRepositoryTest`, `RequestGateFilterIntegrationTest` (one call updated to `list(null)`), `AddRulesIntegrationTest`, `BookListIntegrationTest`, `AddBoundIntegrationTest`, `support/LibrarySeeder`. Docs: `progress.md`. No change to `pom.xml`, `backend/config/**`, the schema or any dependency.

## Criteria to tests
| Task | Criterion / spec row | Code | Tests |
|---|---|---|---|
| 23a | 1 duplicate 409 earliest match, nothing stored (US-004.1, .2, BR-003) | `BookService.create`, `findEarliestMatch` | AddRulesIntegrationTest.duplicateGives409WithTheExistingBookAndStoresNothing, reportsTheEarliestMatchAndTheLowestIdOnATie; BookServiceAddRulesTest.reportsTheEarliestOfTwoMatchesByDateAdded, breaksATieOnTheMillisecondByTheLowestIdTextWhateverTheRowOrder, rejectsAMatchByKeyWithTheExistingBookAndStoresNothing |
| 23a | 1 override true stores, false/absent does not (US-004.3, .4) | same | overrideTrueStoresASeparateBookAndFalseOrAbsentDoesNot; BookServiceAddRulesTest.overrideSkipsOnlyTheScanAndStoresASeparateBook, absentAndFalseOverrideRunTheScanOnceWithoutALoop |
| 23a | 1 status, rating, dates ignored (US-004.6, .7) | scan reads key columns only | statusRatingAndDatesDoNotAffectDetection |
| 23a | 2 LIBRARY_FULL at 5,000 before the duplicate check, with and without override, count stays (C-01, C-02, US-001.7, US-004.9) | count first | atCapacityEveryAddGives409LibraryFullBeforeTheDuplicateCheckAndTheCountStays (body has no details); the5000thBookIsAcceptedAndThenTheLibraryIsFull; BookServiceAddRulesTest.rejectsWithLibraryFullAt5000BeforeAnyDuplicateCheckWithAndWithoutOverride, addsTheBookAt4999AndCountsBeforeScanningBeforeInserting (InOrder) |
| 23a | 3 one transaction, no query in a loop (BR-009, INV-5, INV-7) | `@Transactional` create, one `findAllBy()` | BookServiceTest.addIsTransactionalAndListIsReadOnlyTransactional, BookServiceAddRulesTest.absentAndFalseOverrideRunTheScanOnceWithoutALoop; BookRepositoryTest.keyRowsCarryTheFourScanColumnsOfEveryBookInOneQuery |
| 23a | 4 injection payloads unchanged, +1 each (T-12); Straße/STRASSE both store, Clean Code/CLEAN CODE 409 (T-40) | | injectionPayloadsAreStoredUnchangedAndTheCountGrowsByOneEachTime; sharpSAndDoubleSBothStoreAndCaseVariantsOfOneAuthorDuplicate; BookServiceAddRulesTest.sharpSAndDoubleSDoNotMatchSoBothStore |
| 23a | Brief 1: white space equals \\p{IsWhite_Space} over all code points (F-1) | `DuplicateKey.isWhiteSpace` | DuplicateKeyTest.whiteSpaceEqualsTheUnicodeWhiteSpacePropertyOverAllCodePoints |
| 23b | 1 filter, order, filtered total, empty, at most 5,000, no pagination members (US-002.4, .5, .8, BR-010) | `BookService.list(String)`, `BookController.list` (request attribute) | BookListIntegrationTest (6 tests); BookControllerTest.listPassesTheStatusAttributeOfTheGateAndNullWhenThereIsNone; BookServiceTest.listWithAStatusUsesTheFilteredQueryInTheSameOrder, listRejectsAStatusThatIsNotOneOfTheThreeValues |
| 23b | 2 bound: 201 within 2 s, duplicate 409 within 2 s, never 500 (C-11 a, b, M-2) | | AddBoundIntegrationTest (4 tests) |
| 23b | 3 stop if the bound fails | not triggered | n/a |

## Test specification
Cases came from the spec rows named above: happy, duplicate (case, white space, full-width, sharp S), earliest match and tie, override true/false/absent, 4,999 / 5,000 boundary with each override value, empty library and empty filter, invalid status still 400, injection strings of T-12, 5,000-entry list, bound at 4,999 maximum-size books with duplicates at the first, middle and last stored book. Test levels: unit (BookServiceAddRulesTest 8, BookServiceTest +2, DuplicateKeyTest +1), slice (BookControllerTest +1, BookRepositoryTest +1), real-port integration (AddRulesIntegrationTest 8, BookListIntegrationTest 6, AddBoundIntegrationTest 4).

## Build evidence
`cd backend && ./mvnw clean verify` after 23a: BUILD SUCCESS (unit 690, integration 175, 0 failures; JaCoCo gate met). After 23b: BUILD SUCCESS, unit 693 and integration 185 run, 0 failures, 1 skipped (the pre-existing skip in SqliteStoreIntegrationTest; the two FAILED lines printed in that run are that test's expected claim-failure output). Real line coverage from `target/site/jacoco/jacoco.xml`: BookService 44/44, BookController 6/6, BookKeyRow 2/2, DuplicateKey 35/35; whole project 850/878 (96.8%).

## Real-port requests (excerpts from the test output)
- Duplicate: POST `{"title":" clean   code ","author":"ROBERT MARTIN"}` -> 409 `{"error":"A book with the same title and author already exists.","code":"DUPLICATE_BOOK","timestamp":"...","details":{"existing_book":{"id"...`
- Override: POST `...,"allow_duplicate":true` -> 201; `"allow_duplicate":false` and absent -> 409.
- Full library: four adds (new, duplicate with override, duplicate without, new with override) -> 409 `{"error":"The library is full.","code":"LIBRARY_FULL","timestamp":"2026-11-06T13:27:04.819Z"}` (no details); the 5,000th book -> 201, the next -> 409 LIBRARY_FULL.
- Filter: GET `?status=reading` -> 200 books only reading, `?status=%72eading` -> 200 same; `?status=completed` on a library without one -> 200 `{"books":[],"total":0}`; empty library -> 200 `{"books":[],"total":0}`; `?status=Reading` -> 400 INVALID_STATUS.
- T-12 payloads and T-40 pairs: 201, 201, 201, then 409 as the spec states.

## Timings of the 2-second tests (System.nanoTime around the whole HTTP exchange, cold, first request after seeding)
Machine: 4 CPUs, Linux amd64, Java 21.0.12.1, max heap 3,420 MB. Non-ASCII maximum-size books (Cyrillic, precomposed capital, full-width letter, supplementary symbol), so the general key path runs.
| Request | Status | Elapsed |
|---|---|---|
| duplicate of the first stored book (cold, first in the run) | 409 | 928 ms |
| duplicate of the middle book | 409 | 112 ms |
| duplicate of the last book | 409 | 143 ms |
| non-matching add | 201 | 143 ms |
| non-matching add (C-11 steps) | 201 | 234 ms |
| duplicate after removal (C-11 step b) | 409 | 81 ms |
The cold 928 ms is the worst figure seen, a margin of about 2.2 times against the 2 s bound. The test would catch a regression; it is not weakened.

## Deviations and notes
1. The 300-line rule: the new add-rule unit tests are in `BookServiceAddRulesTest` (not in `BookServiceTest`, which would pass 300 lines). Not in the brief's list.
2. `BookRepository.findAllBy()` is a Spring Data class-based projection onto `BookKeyRow`, not an `@Query` constructor expression: `BookRepositoryTest.repositoryDeclaresNoNativeOrHandWrittenQueries` (Task 04) forbids any `@Query`, and the projection gives the same four-column selection without a hand-written query. Plan text says "JPQL constructor projection"; the plan appendix could say "derived projection".
3. `BookService.list()` became `list(String status)` (null = every book) and the service refuses an unknown value (defence in depth); three existing test call sites were updated.
4. The scan compares `DuplicateKey.textKey` of the scanned author, then of the title, with the incoming key, instead of building a full `DuplicateKey.of` per row (same equality; the title key is computed only when the author key matches, which saves time on the costly path). The incoming pair uses `DuplicateKey.of` on the stored (normalized) text.
5. `DuplicateKey.isWhiteSpace` is now package-private (needed by the F-1 test).
6. `HttpClient` console output prints `?` for non-ASCII characters (console encoding only; the data is stored and compared as Unicode).

## Risks and follow-ups
- Concurrency (C-11 c, 50 parallel adds) is not in this chunk; the count and scan share one transaction but SQLite single-writer behaviour under load is for the task that owns C-11 c.
- The cold figure of 928 ms depends on the machine; Task 41 records the figures on the reference machine.
- A status row for 23a/23b in `status.md` is for the orchestrator.

## Follow-up round (handoffs 130 and 131)
- F-4: in this delivery "cold" means the first request in the JVM of that test run (the first duplicate in `AddBoundIntegrationTest`, 928 ms).
- F-1 (condition) done: `support/LibrarySeeder.seedMaxSizeSharedAuthor` and `AddBoundWorstCaseIntegrationTest`: 4,999 books with the same 200-code-point author and distinct 500-code-point titles, so the scan computes a title key for every row. Real port, `System.nanoTime`, 4 CPUs, Linux amd64, Java 21.0.12.1. The class ran after `AddBoundIntegrationTest` in the same JVM, so it is not the first request of the JVM (the JIT was warm from the earlier class); the numbers are the first and second round of this class.
  | Round | Non-matching add | Duplicate of first / middle / last |
  |---|---|---|
  | 1 | 201 in 442 ms | 409 in 237 / 218 / 239 ms |
  | 2 | 201 in 150 ms | 409 in 151 / 156 / 152 ms |
  All under 2 s; the bound held, no schema change. Security's 1.64 s cold figure on its machine (first request in a fresh JVM) is the case to record on the reference machine in Task 41.
- F-2/F-3 and the Security nit: `BookKeyRow` Javadoc now says Spring Data class-based projection; the plan wording needs the same amendment (noted in progress.md).
- SEC-F-166, 167, 168 and the BR-009 validation-before-capacity PM question are carried as notes in progress.md; no code.
- Build: `./mvnw clean verify` BUILD SUCCESS; 693 unit and 186 integration tests, 0 failures, 1 pre-existing skip.
