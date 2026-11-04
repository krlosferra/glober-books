# Handoff 070: Task 07 delivery (add and list API, thin)
- From: java
- To: liaison
- Type: TASK_DELIVERY
- Branch: `feature/task-07-add-and-list-thin` (local commits, not pushed)

## Resume point
- Done: everything in the brief; `./mvnw verify` green (BUILD SUCCESS, all coverage checks met). Remaining: Liaison audit, Security review, push by the orchestrator.

## Files
Created: `dto/CreateBookRequest`, `dto/BookListResponse`, `service/BookService`, `service/BookValidationException` (extra), `config/IdConfig` (extra, the `Supplier<UUID>` bean), `controller/BookController`; tests `BookServiceTest`, `BookControllerTest`, `BookApiIntegrationTest` (extra), `ApplicationStartupIntegrationTest` (extra).
Modified: `error/GlobalExceptionHandler` (mappings for `BookValidationException`, `MethodArgumentNotValidException`, `HttpMessageNotReadableException`), `dto/BookResponse` (`@JsonFormat` on both dates), `application.yml` (`spring.jpa.database-platform`), `status.md`, `progress.md`.

## Criteria
1. 201 and seven members: `BookServiceTest.addsUnreadBookWithIdAndMillisecondDate`, `BookApiIntegrationTest.addGivesCreatedWithTheSevenMembersInWireFormat`, `completedBookWritesBothDatesAs24CharacterUtcText`.
2. List order, empty: `listIsNewestFirstWithTiesByIdAndNothingIsWritten`, `emptyLibraryListsEmptyArrayAndZero`.
3. Validation and unknown member: `rejectsEmptyBlankAndNullTitleWithField`, `rejectsTitleOf501AndAuthorOf201CodePoints`, `acceptsBoundaryLengthsMeasuredInCodePoints`, `BookControllerTest.addRejectsUnknownMemberAsMalformed`, `BookApiIntegrationTest.unknownMemberIsMalformedRequestWithoutDetails`.
4. One transaction / read-only: `BookServiceTest.addIsTransactionalAndListIsReadOnlyTransactional` (annotations). A read writes nothing: database file SHA-256 and row count unchanged around the list (integration test above). The controller holds no rule.
5. Streaming: the controller returns `ResponseEntity<BookListResponse>` (a DTO, never `byte[]` or `String`), so the message converter writes to the response stream; the service returns after its transaction. `BookControllerTest.endpointsExposeOnlyDtosAndNeverBufferTheBody` checks return and parameter types by reflection. Limit: a type-level proof, not an observation of the output stream.
Carried: 1 dialect and startup (`ApplicationStartupIntegrationTest`, overrides only the database file and port 0); 2 wire format (`zeroMillisecondInstantsStillCarryThreeFractionDigits` and the 201 pattern tests); 3 no entity in controller signatures (same reflection test); 6 no logging in controller or service (none present).

## Findings to report (Jackson 3 and tooling)
- F1 (Task 04 audit F1 confirmed real): Jackson 3 writes an `Instant` of 2026-02-19T10:00:00.000Z as `2026-02-19T10:00:00Z` (zero fraction dropped). Fixed in `BookResponse` with `@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="UTC")` (the `com.fasterxml.jackson.annotation` package is still valid in Jackson 3). Proved on the real port.
- F2: Hibernate already auto-detects `SQLiteDialect` here; the explicit `spring.jpa.database-platform` key is recognised in Boot 4.1.2 and asserted by the startup test (SEC-F-118).
- F3: SpotBugs EI_EXPOSE_REP2 fired on the controller because `BookService` had a method named `add` (SpotBugs treats `add*` classes as mutable). Renamed the service method to `create`; no exclusion added.
- F4 (small spec gap): the service reports an invalid `status` alone as `INVALID_STATUS` and mixed failures as `VALIDATION_ERROR`; Bean Validation (`@NotBlank`) runs first at the boundary, so a blank title plus a bad status reports only `title` in Task 07 (Task 20 aligns with BR-012 step 6). Strip uses `String.strip()`, not the BR-006 rule (Task 18). Bean Validation details for `{}` are ordered alphabetically (author, title); Task 20 must order title, author.
- Unknown member: `FAIL_ON_UNKNOWN_PROPERTIES` is on (also set in yml), giving `HttpMessageNotReadableException` mapped to `MALFORMED_REQUEST`; no difference from the plan found.
- The `@WebMvcTest` slice includes the gate filter, so slice requests need `Host: localhost:80`.

## Test inventory
Unit: BookServiceTest (13). Slice: BookControllerTest (10). Integration (real port, temp SQLite file): BookApiIntegrationTest (11), ApplicationStartupIntegrationTest (1).

## Real output
`cd backend && ./mvnw verify`: unit phase "Tests run: 234, Failures: 0, Errors: 0, Skipped: 0"; failsafe phase "Tests run: 49, Failures: 0, Errors: 0, Skipped: 1" (the skip is in the earlier SqliteStoreIntegrationTest); "All coverage checks have been met."; BUILD SUCCESS. Line coverage of new classes (missed/covered): BookService 0/29, BookController 0/6, CreateBookRequest 0/1, BookListResponse 0/4, BookValidationException 0/6, GlobalExceptionHandler 0/29, BookResponse 0/4.

Real status and body from the test client (long cases shortened by the test):
```
EVIDENCE POST {"title":"Clean Code","author":"Robert Martin"} -> 201 {"id":"71d92907-50fb-47f4-9654-2a8f0f901dd0","title":"Clean Code","author":"Robert Martin","status":"unread","rating":null,"date_added":"2026-11-04T14:24:04.018Z","date_completed":null}
EVIDENCE GET  -> 200 {"books":[],"total":0}
EVIDENCE POST {"title":"T","author":"A","isbn":"1"} -> 400 {"error":"The request is malformed.","code":"MALFORMED_REQUEST","timestamp":"2026-11-04T14:24:05.004Z"}
EVIDENCE POST {"title":"","author":"A"} -> 400 {"error":"The request contains invalid values.","code":"VALIDATION_ERROR","timestamp":"2026-11-04T14:24:05.054Z","details":[{"field":"title","message":"must be between 1 and 500 characters"}]}
EVIDENCE POST {"title":"T","author":"A","status":"done"} -> 400 {"error":"Status must be unread, reading or completed.","code":"INVALID_STATUS","timestamp":"2026-11-04T14:24:05.087Z","details":[{"field":"status","message":"must be one of: unread, reading, completed"}]}
EVIDENCE POST {"title":"One","author":"A"} -> 201 {"id":"10f966a4-67f0-48fd-ac43-064f6fd7f353","title":"One","author":"A","status":"unread","rating":null,"date_added":"2026-11-04T14:24:05.102Z","date_completed":null}
EVIDENCE GET  -> 200 {"books":[{"id":"10f966a4-67f0-48fd-ac43-064f6fd7f353","title":"One","author":"A","status":"unread","rating":null,"date_added":"2026-11-04T14:24:05.102Z","date_completed":null}],"total":1}
EVIDENCE POST {"title":"???????????????????????????????????... -> 201 {"id":"a0e3419f-4d87-4c32-b71b-7f0d50daa8a5","title":"????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
EVIDENCE POST {"title":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa... -> 400 {"error":"The request contains invalid values.","code":"VALIDATION_ERROR","timestamp":"2026-11-04T14:24:05.161Z","details":[{"field":"title","message":"must be between 1 and 500 characters"}]}
EVIDENCE POST {"title":"T","author":"A","status":"completed","allow_duplicate":true} -> 201 {"id":"a8aa7b90-3a48-4ce0-843d-1c95c4750117","title":"T","author":"A","status":"completed","rating":null,"date_added":"2026-11-04T14:24:05.178Z","date_completed":"2026-11-04T14:24:05.178Z"}
EVIDENCE GET  -> 200 {"books":[{"id":"00000000-0000-4000-8000-00000000000c","title":"Newest","author":"A","status":"unread","rating":null,"date_added":"2026-02-20T10:00:00.000Z","date_completed":null},{"id":"00000000-0000-4000-8000-00000000000a","title":"Tie A","author":"A","status":"unread","rating":null,"date_added":"2026-02-19T10:00:00.000Z","date_completed":null},{"id":"00000000-0000-4000-8000
EVIDENCE POST {"title":"T","author":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb... -> 400 {"error":"The request contains invalid values.","code":"VALIDATION_ERROR","timestamp":"2026-11-04T14:24:05.262Z","details":[{"field":"author","message":"must be between 1 and 200 characters"}]}
EVIDENCE GET  -> 200 {"books":[{"id":"00000000-0000-4000-8000-00000000000a","title":"Zero","author":"A","status":"unread","rating":null,"date_added":"2026-02-19T10:00:00.000Z","date_completed":null}],"total":1}
```
Three books newest first with a tie ordered by id: titles Newest, Tie A, Tie B, Oldest, total 4 (asserted).

## git diff --stat main..HEAD (before this delivery commit)
```
 .../com/globant/globerbooks/config/IdConfig.java   |  21 +++
 .../globerbooks/controller/BookController.java     |  59 +++++++
 .../globant/globerbooks/dto/BookListResponse.java  |  29 ++++
 .../com/globant/globerbooks/dto/BookResponse.java  |   7 +-
 .../globant/globerbooks/dto/CreateBookRequest.java |  16 ++
 .../globerbooks/error/GlobalExceptionHandler.java  |  55 ++++++
 .../globant/globerbooks/service/BookService.java   | 102 +++++++++++
 .../service/BookValidationException.java           |  47 +++++
 backend/src/main/resources/application.yml         |   1 +
 .../ApplicationStartupIntegrationTest.java         |  40 +++++
 .../globerbooks/BookApiIntegrationTest.java        | 191 +++++++++++++++++++++
 .../globerbooks/controller/BookControllerTest.java | 176 +++++++++++++++++++
 .../globerbooks/service/BookServiceTest.java       | 169 ++++++++++++++++++
 .../069-orchestrator-to-java-task-07-brief.md      |  25 +++
 .../070-java-to-liaison-task-07-delivery.md        |   8 +
 docs/sdd/status.md                                 |   5 +-
 16 files changed, 948 insertions(+), 3 deletions(-)
```

## git log main..HEAD (author, subject; commits carry no trailers)
```
33693f1 krlosferra <carlos.ferra@globant.com> feat(api): SQLite dialect, startup and contract integration tests [Task-7]
2c33db7 krlosferra <carlos.ferra@globant.com> feat(controller): add and list endpoints with error mapping [Task-7]
f4c0717 krlosferra <carlos.ferra@globant.com> feat(service): add and list use cases with thin checks [Task-7]
4714c16 krlosferra <carlos.ferra@globant.com> feat(dto): add create request and list response [Task-7]
5bd760a krlosferra <carlos.ferra@globant.com> docs(sdd): add the Task 07 brief and update the resume point [Task-7]
```

## Deviations / risks
Extra files beyond the task list (BookValidationException, IdConfig, ApplicationStartupIntegrationTest, BookApiIntegrationTest); service method named `create`; Task 06 advice edited. Not pushed.
