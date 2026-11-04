# Handoff 066: Task 04 delivery (domain and repository)
- From: java
- To: liaison
- Type: TASK_DELIVERY
- Branch: feature/task-04-domain-repository (local commits only, not pushed)

## Resume point
- Done: everything. BookStatus + test, Book, BookResponse, BookRepository, BookRepositoryTest, progress.md, status.md row (IN_REVIEW), this delivery.
- Next: Liaison audit and Security review; orchestrator pushes.

## Files
Created: `domain/BookStatus.java`, `domain/Book.java`, `dto/BookResponse.java`, `repository/BookRepository.java`; tests `domain/BookStatusTest.java`, `repository/BookRepositoryTest.java`. Edited: `docs/sdd/progress.md`, `docs/sdd/status.md` (row 04), this file. Nothing under `backend/config/**`, `pom.xml`, `schema.sql`, `application.yml`.

## Criteria
1. Validate and DDL: `BookRepositoryTest.hibernateValidatePassesAndStoredDdlEqualsNormalisedSchema`. The context starts with `ddl-auto=validate` on the STRICT table with the community SQLite dialect and the CHECKs, with no error and no fallback. The `sqlite_master` text, normalised (comments none, white space collapsed, `IF NOT EXISTS` removed, trailing `;` dropped), equals the same normalisation of `schema.sql`: SQLite stores the statement text as written, minus `IF NOT EXISTS`. AP-12.1 whole-statement comparison therefore stands.
2. CHECKs, inserted through JDBC, each asserts `DataAccessException` and zero rows (`databaseRejectsRowsThatBreakAnInvariant`, 28 cases, one line each): id 35 chars; id 37 chars; id upper case; id non-hex; id hyphen misplaced; id without hyphens; empty id; title length 0; title 501; author 0; author 201; status upper case; status outside set; rating 0; rating 6; rating non-numeric text; rating on unread; rating on reading; date_completed on unread; on reading; completed without date_completed; date_added without milliseconds; with space separator; lower-case z; with offset; letters in digits; empty; date_completed malformed; date_completed 25 chars. Accepted boundaries (`acceptsBoundaryRowsThatTheSpecAllows`, 6): title 1/500, author 200, rating 1 and 5, completed without rating. Code points: `titleOfFiveHundredEmoji...` (500 passes, 501 fails), `authorOfTwoHundredEmoji...` (200 passes, 201 fails).
3. `savedBookReturnsWithSameMillisecondTimestampsAndLowerCaseStatus` (raw columns are `completed`, `2026-01-02T03:04:05.678Z`, `2026-02-03T04:05:06.007Z`); `unreadBookKeepsNullRating...`; tie test `orderedQuerySortsByDateAddedDescendingThenIdAscendingIncludingTies` (two equal `date_added`: ids b then c, then the older a); `statusFilterReturnsOnlyThatStatus...`. Generated SQL ends `order by b1_0.date_added desc,b1_0.id`.
4. `bookHasNoPublicSetters`, `repositoryDeclaresNoNativeOrHandWrittenQueries` (no `@Query`), `bookResponseHasExactlyTheSevenMembers...`; derived queries only; Checkstyle ban active and green.

## Evidence
`cd backend && ./mvnw -o clean verify`: BUILD SUCCESS. Surefire 211 tests, 0 failures (BookRepositoryTest 45, BookStatusTest 11); failsafe 37 tests, 0 failures, 1 skipped (pre-existing SqliteStoreIntegrationTest assumption). Checkstyle, SpotBugs and the JaCoCo gate passed. Line coverage of the new classes (merged report): BookStatus 12/12, BookStatus.WireConverter 5/5, Book 18/18, Book.InstantConverter 4/5, BookResponse 4/4.
One first run failed on my own test: `rating '3'` as text was accepted because STRICT converts numeric text to an integer losslessly; the case was changed to non-numeric text `abc` (spec oracle: no text in `rating`), not weakened.

## Deviations and risks
- None from the plan. The test sets `spring.jpa.database-platform=...SQLiteDialect` by property; `application.yml` has no dialect, so a later task that starts the full application must set it (not changed here).
- `Book` has a public all-argument constructor and getters only; `changeStatus`, `assignRating`, `clearRating` come in Task 30. `BookResponse` keeps `Instant` members; the 24-character wire format is a Jackson concern for Task 07.
- Test uses Boot's DataSource on a temp file (pool size 1 from `application.yml`), as the brief allows.
