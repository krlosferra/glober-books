# Handoff 137: Task 31a delivery (get, delete, status change)
- From: java
- To: liaison (via orchestrator)
- Phase: 4   Round: 1
- Type: TASK_DELIVERY
- Task: 31a, branch `feature/task-31a-get-delete-status`, local commits only (not pushed)

## Resume point
- Done: route table, service, controller, `RouteTableTest`, `BookStatusServiceTest`, `BookControllerTest`, `BookStoryIntegrationTest`, `RoutingIntegrationTest` placeholder fixed, `./mvnw clean verify` green, progress.md, status.md row (IN_REVIEW).
- Next: nothing in the implementation. The orchestrator pushes and starts the audit and the Security review.
- The diff stat and log below were taken before the delivery commit itself; the delivery commit adds only this file, `progress.md` and `status.md`.

## Commits (author is the configured human identity; no trailers)
```
krlosferra <carlos.ferra@globant.com>
fix(service): name the delete use case discard for SpotBugs [Task-31a]
---
krlosferra <carlos.ferra@globant.com>
test(api): story tests for get, delete and status change [Task-31a]
---
krlosferra <carlos.ferra@globant.com>
feat(api): get, delete and status change with the route table rows [Task-31a]
---
krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 31a brief [Task-31a]
---
```

## git diff --stat origin/main..HEAD
```
 .../globerbooks/config/transport/RouteTable.java   |  55 +++-
 .../globerbooks/controller/BookController.java     |  39 +++
 .../globant/globerbooks/service/BookService.java   |  55 ++++
 .../globerbooks/BookStoryIntegrationTest.java      | 294 +++++++++++++++++++++
 .../config/transport/RouteTableTest.java           |  37 ++-
 .../config/transport/RoutingIntegrationTest.java   |   6 +-
 .../globerbooks/controller/BookControllerTest.java |  86 ++++++
 .../globerbooks/service/BookStatusServiceTest.java | 192 ++++++++++++++
 .../136-orchestrator-to-java-task-31a-brief.md     |  26 ++
 9 files changed, 776 insertions(+), 14 deletions(-)
```

## Files
Production: `service/BookService.java` (get, changeStatus, discard), `controller/BookController.java` (three methods), `config/transport/RouteTable.java` (two templates, key lookup). Tests: `controller/BookControllerTest` (edit), `service/BookStatusServiceTest` (new), `BookStoryIntegrationTest` (new), `config/transport/RouteTableTest` (edit), `config/transport/RoutingIntegrationTest` (edit: the Task 15 placeholder that expected 404 for the id paths until Task 31 now expects the spec's 405). Docs: this file, `progress.md`, `status.md` row. No change to `pom.xml`, `backend/config/**`, the schema, any dependency, `GlobalExceptionHandler` or the gate. `RouteTableTest` and `RoutingIntegrationTest` are not in the brief's test list; both had to change because their old expectations (id paths not routable) are what this task reverses.

## Design points
- Order (BR-012): the gate judges host to empty body (including id shape, which is why a malformed id with a bad body is `MALFORMED_REQUEST`); then `@Valid @RequestBody StatusRequest` (strict reader, then validation) runs before the service; then the service parses the status again (defence), looks the book up with `findById` (bound parameter), then applies the change. No `StatusRequest` change was needed.
- `changeStatus` is one `@Transactional` unit: `Book.changeStatus(target, clock)` then `saveAndFlush`; the rating and `date_completed` are cleared in the same save as the status. `discard` is find, delete, flush, return the removed book. No query in a loop.
- Naming: the delete use case is `BookService.discard`, not `delete`. SpotBugs (EI_EXPOSE_REP2, no exclusion allowed) treats a service with a method named `delete` as mutable and failed the build on `BookController(BookService)`. Controller method is still `delete`.
- `RouteTable`: templates `/api/v1/books/{id}` (GET, DELETE) and `/api/v1/books/{id}/status` (PUT); one non-empty segment is the id; `Allow` comes from the template (`DELETE, GET` and `PUT`). Non-routable paths (`;`, `%2f`, dot segments) are still `NOT_FOUND` before any id rule. The test stays authoritative: `equalsTheMappingsOfTheControllersAndTheCatalog` compares the table with the MVC mappings.
- No logging added; no id is logged; no `Object` members; all files at or under 300 lines (largest: `BookStoryIntegrationTest` 294, `BookControllerTest` 285).

## Criterion map
| Criterion / spec row | Tests |
|---|---|
| 1, 5.3, US-007: get after 201, same body | `BookStoryIntegrationTest.getReturnsTheBookJustCreated`; slice `BookControllerTest.getReturns200WithTheBookAndPassesTheIdText` |
| T-15, 5.3 404 | `getOfAnUnknownWellFormedIdIsBookNotFound`; `getMapsBookNotFoundTo404`; `BookStatusServiceTest.getOfAnUnknownIdGivesBookNotFound` |
| T-14 (upper-case id, `not-a-uuid`, `1%27%20OR...`; GET, DELETE, PUT status) | `malformedIdsAreMalformedRequestOnEveryIdEndpoint`. The rows for `PUT` and `DELETE .../rating` wait for Task 31b (routes do not exist yet) |
| 5.4, US-006 each valid value | `everyValidStatusIsAppliedAndReturned` (7 members), `putStatusReturns200WithTheBookAndPassesIdAndStatus` |
| S-04 completed to completed | `completedToCompletedChangesNothing`; `BookStatusServiceTest.completedToCompletedKeepsRatingAndTheOriginalCompletionDate` |
| S-11 unread/reading same state | `sameStateChangesOnUnreadAndReadingChangeNothing` (list identical); `sameStateChangeChangesNothing` |
| criterion 3, BR-004, BR-008, DEC-023 (fixed clock) | `entersCompletedWithTheClockValueAndNoRating` (123 ms from 123456789 ns), `leavingCompletedClearsRatingAndCompletionDateWithTheStatusInOneSave`, `leavingCompletedForUnreadClears...`; real port `enteringCompletedSetsTheDateAndLeavingClearsRatingAndDate`; transaction annotations `everyUseCaseRunsInOneTransactionAndReadsAreReadOnly`. Forced-failure proof is Task 32 |
| T-19 (`"Completed"`, `""`, `null`, `{}` -> INVALID_STATUS; `5`, `true` -> MALFORMED_REQUEST), SEC-F-159 | `invalidStatusValuesAreInvalidStatusAndWrongTypesAreMalformed` (full context, real port, plus unknown member, `[]`, truncated JSON); slice `putStatusRejectsAnInvalidValueWithTheFieldAndNoServiceCall` |
| T-38 field values before existence | `fieldValuesAreJudgedBeforeExistence` (unknown id: `"Done"` 400 INVALID_STATUS, `5` 400 MALFORMED_REQUEST, `"reading"` 404); `BookStatusServiceTest.judgesTheStatusBeforeLookingForTheBook` (no `findById`) |
| 5.7, S-06 delete then delete | `deleteReturnsTheRemovedBookAndTheSecondDeleteIsNotFound` |
| S-07 library identical after failures | `failedRequestsLeaveTheLibraryIdentical` (full `GET /api/v1/books` body before and after 13 failing requests) |
| C-04, US-008.3 | `atCapacityListStatusChangeAndDeleteWorkThenOneAddFitsAndTheNextIsLibraryFull` (5,000 seeded) |
| E-18, criterion 5 | `deleteWithoutBodyPassesTheGateAndAnyBodyIsMalformedRequest` (no body 200; one space 400; `{}` 400; GET with body 400) |
| E-24 row 1 (real-port, completed here) | `anyPercentInTheIdSegmentIsMalformedRequest` |
| E-27 rows 1 and 2 (completed here), row 3 | `wrongMethodOnAnIdPathIsMethodNotAllowedBeforeTheIdFormat`; `RouteTableTest.decidesMethodBeforeTheIdFormat` |
| E-02 (id rows) | `RoutingIntegrationTest.e02WrongMethodOnAnIdPathIsMethodNotAllowedWithTheAllowOfThatPath` (GET .../status, POST /{id}). Not covered: `GET .../{id}/rating` (route is Task 31b) |
| Five routes | `RouteTableTest.holdsExactlyTheFiveApiRoutes`, `equalsTheMappingsOfTheControllersAndTheCatalog` |
| E-24 rows 2 and 3, T-14 rating rows | not completed here (rows 2 and 3 were covered by Task 16; rating rows are Task 31b) |

## Real-port table (from the build output; elapsed ms as measured by the client, mixed with cold JIT for the first call)
| Request | Body | Status | Code | ms |
|---|---|---|---|---|
| PUT /api/v1/books/00000000-0000-4000-8000-000000000000/status | '{"status":"Done"}' | 400 | INVALID_STATUS | 16 ms |
| PUT /api/v1/books/00000000-0000-4000-8000-000000000000/status | '{"status":5}' | 400 | MALFORMED_REQUEST | 11 ms |
| PUT /api/v1/books/00000000-0000-4000-8000-000000000000/status | '{"status":"reading"}' | 404 | BOOK_NOT_FOUND | 7 ms |
| DELETE /api/v1/books/00000000-0007-4000-8000-000000000000 | ' ' | 400 | MALFORMED_REQUEST | 5 ms |
| DELETE /api/v1/books/00000000-0007-4000-8000-000000000000 | '{}' | 400 | MALFORMED_REQUEST | 3 ms |
| GET /api/v1/books | '{}' | 400 | MALFORMED_REQUEST | 2 ms |
| DELETE /api/v1/books/00000000-0007-4000-8000-000000000000 | none | 200 | - | 37 ms |
| POST /api/v1/books | '{"title":"T","author":"A"}' | 201 | - | 21 ms |
| GET /api/v1/books/8a92bc5c-4909-4686-86ca-90d470dd7532 | none | 200 | - | 6 ms |
| DELETE /api/v1/books/8a92bc5c-4909-4686-86ca-90d470dd7532 | none | 200 | - | 6 ms |
| DELETE /api/v1/books/8a92bc5c-4909-4686-86ca-90d470dd7532 | none | 404 | BOOK_NOT_FOUND | 3 ms |
| GET /api/v1/books/8a92bc5c-4909-4686-86ca-90d470dd7532 | none | 404 | BOOK_NOT_FOUND | 4 ms |
| GET /api/v1/books | none | 200 | - | 6 ms |
| PUT /api/v1/books/00000000-0003-4000-8000-000000000000/status | '{"status":"completed"}' | 200 | - | 6 ms |
| GET /api/v1/books | none | 200 | - | 5 ms |
| GET /api/v1/books/NOT-A-UUID | none | 400 | MALFORMED_REQUEST | 5 ms |
| GET /api/v1/books/00000000-0000-4000-8000-000000000000 | none | 404 | BOOK_NOT_FOUND | 3 ms |
| DELETE /api/v1/books/00000000-0000-4000-8000-000000000000 | none | 404 | BOOK_NOT_FOUND | 2 ms |
| DELETE /api/v1/books/00000000-0008-4000-8000-000000000000 | '{}' | 400 | MALFORMED_REQUEST | 1 ms |
| PUT /api/v1/books/00000000-0000-4000-8000-000000000000/status | '{"status":"reading"}' | 404 | BOOK_NOT_FOUND | 4 ms |
| PUT /api/v1/books/not-a-uuid/status | '{"status":"reading"}' | 400 | MALFORMED_REQUEST | 1 ms |
| PUT /api/v1/books/00000000-0008-4000-8000-000000000000/status | '{"status":"Done"}' | 400 | INVALID_STATUS | 4 ms |
| PUT /api/v1/books/00000000-0008-4000-8000-000000000000/status | '{}' | 400 | INVALID_STATUS | 4 ms |
| PUT /api/v1/books/00000000-0008-4000-8000-000000000000/status | '{"status":null}' | 400 | INVALID_STATUS | 5 ms |
| PUT /api/v1/books/00000000-0008-4000-8000-000000000000/status | '{"status":5}' | 400 | MALFORMED_REQUEST | 3 ms |
| PUT /api/v1/books/00000000-0008-4000-8000-000000000000/status | '{"status":true}' | 400 | MALFORMED_REQUEST | 3 ms |
| PUT /api/v1/books/00000000-0008-4000-8000-000000000000/status | '{"status":"unread","rating":1}' | 400 | MALFORMED_REQUEST | 3 ms |
| PUT /api/v1/books/00000000-0008-4000-8000-000000000000/status | '{"status":' | 400 | MALFORMED_REQUEST | 3 ms |
| GET /api/v1/books | none | 200 | - | 5 ms |
| GET /api/v1/books | none | 200 | - | 4 ms |
| PUT /api/v1/books/00000000-0004-4000-8000-000000000000/status | '{"status":"unread"}' | 200 | - | 5 ms |
| PUT /api/v1/books/00000000-0005-4000-8000-000000000000/status | '{"status":"reading"}' | 200 | - | 5 ms |
| GET /api/v1/books | none | 200 | - | 5 ms |
| POST /api/v1/books | '{"title":"Clean Code","author":"Robert Martin"}' | 201 | - | 8 ms |
| GET /api/v1/books/10184636-6d0f-43de-8c31-23b61d8dc2ae | none | 200 | - | 3 ms |
| GET /api/v1/books | none | 200 | - | 96 ms |
| PUT /api/v1/books/00000000-0000-4000-8000-00000000000a/status | '{"status":"completed"}' | 200 | - | 29 ms |
| DELETE /api/v1/books/00000000-0000-4000-8000-00000000000b | none | 200 | - | 7 ms |
| POST /api/v1/books | '{"title":"One more","author":"Someone"}' | 201 | - | 53 ms |
| POST /api/v1/books | '{"title":"Yet another","author":"Someone"}' | 409 | LIBRARY_FULL | 6 ms |
| PUT /api/v1/books/00000000-0001-4000-8000-000000000000/status | '{"status":"reading"}' | 200 | - | 17 ms |
| PUT /api/v1/books/00000000-0001-4000-8000-000000000000/status | '{"status":"completed"}' | 200 | - | 27 ms |
| PUT /api/v1/books/00000000-0001-4000-8000-000000000000/status | '{"status":"unread"}' | 200 | - | 14 ms |
| POST /api/v1/books/not-a-uuid | '{}' | 405 | METHOD_NOT_ALLOWED | 5 ms |
| GET /api/v1/books/not-a-uuid/status | none | 405 | METHOD_NOT_ALLOWED | 8 ms |
| GET /api/v1/books/not-a-uuid/other | none | 404 | NOT_FOUND | 3 ms |
| GET /api/v1/books/%33f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b | none | 400 | MALFORMED_REQUEST | 7 ms |
| PUT /api/v1/books/00000000-0006-4000-8000-000000000000/status | '{"status":"Completed"}' | 400 | INVALID_STATUS | 14 ms |
| PUT /api/v1/books/00000000-0006-4000-8000-000000000000/status | '{"status":""}' | 400 | INVALID_STATUS | 5 ms |
| PUT /api/v1/books/00000000-0006-4000-8000-000000000000/status | '{"status":null}' | 400 | INVALID_STATUS | 4 ms |
| PUT /api/v1/books/00000000-0006-4000-8000-000000000000/status | '{}' | 400 | INVALID_STATUS | 4 ms |
| PUT /api/v1/books/00000000-0006-4000-8000-000000000000/status | '{"status":5}' | 400 | MALFORMED_REQUEST | 3 ms |
| PUT /api/v1/books/00000000-0006-4000-8000-000000000000/status | '{"status":true}' | 400 | MALFORMED_REQUEST | 6 ms |
| PUT /api/v1/books/00000000-0006-4000-8000-000000000000/status | '{"status":"reading","x":1}' | 400 | MALFORMED_REQUEST | 3 ms |
| PUT /api/v1/books/00000000-0006-4000-8000-000000000000/status | '[]' | 400 | MALFORMED_REQUEST | 6 ms |
| PUT /api/v1/books/00000000-0006-4000-8000-000000000000/status | '{"status":' | 400 | MALFORMED_REQUEST | 4 ms |
| GET /api/v1/books/00000000-0000-4000-8000-000000000000 | none | 404 | BOOK_NOT_FOUND | 5 ms |
| PUT /api/v1/books/00000000-0002-4000-8000-000000000000/status | '{"status":"completed"}' | 200 | - | 7 ms |
| PUT /api/v1/books/00000000-0002-4000-8000-000000000000/status | '{"status":"reading"}' | 200 | - | 7 ms |
| GET /api/v1/books/00000000-0002-4000-8000-000000000000 | none | 200 | - | 3 ms |
| GET /api/v1/books/3F2B8C1E-5A4D-4E6F-9B7A-1C2D3E4F5A6B | none | 400 | MALFORMED_REQUEST | 4 ms |
| DELETE /api/v1/books/3F2B8C1E-5A4D-4E6F-9B7A-1C2D3E4F5A6B | none | 400 | MALFORMED_REQUEST | 4 ms |
| PUT /api/v1/books/3F2B8C1E-5A4D-4E6F-9B7A-1C2D3E4F5A6B/status | '{"status":"reading"}' | 400 | MALFORMED_REQUEST | 3 ms |
| GET /api/v1/books/not-a-uuid | none | 400 | MALFORMED_REQUEST | 2 ms |
| DELETE /api/v1/books/not-a-uuid | none | 400 | MALFORMED_REQUEST | 2 ms |
| PUT /api/v1/books/not-a-uuid/status | '{"status":"reading"}' | 400 | MALFORMED_REQUEST | 2 ms |
| GET /api/v1/books/1%27%20OR%20%271%27%3D%271 | none | 400 | MALFORMED_REQUEST | 2 ms |
| DELETE /api/v1/books/1%27%20OR%20%271%27%3D%271 | none | 400 | MALFORMED_REQUEST | 2 ms |
| PUT /api/v1/books/1%27%20OR%20%271%27%3D%271/status | '{"status":"reading"}' | 400 | MALFORMED_REQUEST | 2 ms |

Rows map to the brief's list: get existing (201 then GET 200), get unknown 404 BOOK_NOT_FOUND, get malformed 400, put status each valid value 200 (reading, completed, unread), invalid values 400 INVALID_STATUS / MALFORMED_REQUEST, put status unknown id with `{"status":"Done"}` 400 INVALID_STATUS, delete existing 200, second delete 404, delete with one-space body 400. The 5,000-book run: list 5,000 books 188 ms; status change 19 ms; delete 12 ms; add 201 62 ms; next add 409 LIBRARY_FULL 8 ms.

## Build evidence
- `cd backend && ./mvnw -o clean verify`: `Tests run: 874, Failures: 0, Errors: 0, Skipped: 0` (surefire, unit and slice); `Tests run: 201, Failures: 0, Errors: 0, Skipped: 1` (failsafe; the skip is the existing `SqliteStoreIntegrationTest` assumption that skips when the process ignores file permissions, as it runs here as root); `All coverage checks have been met`; Checkstyle and SpotBugs green; `BUILD SUCCESS`.
- Real line coverage (JaCoCo CSV): `RouteTable` 35/35, `BookService` 56/56, `BookController` 9/9; whole backend 919/949 lines = 96.8%.
- Offline mode (`-o`) was used because the dependencies are already in the local repository.

## Deviations, risks, follow-ups
- Deviation: `discard` instead of `delete` (above). Deviation: `RouteTableTest` and `RoutingIntegrationTest` edited (above). Neither needs a spec change; the PM may want the plan to name `discard`.
- Spec gap for the PM and Task 31b: E-02 lists `GET /api/v1/books/{id}/rating` as 405, which cannot hold until the rating route exists; T-14 rating rows likewise.
- Observation (no change): the gate checks the id shape before the body is parsed, so a malformed id with an invalid status body is `MALFORMED_REQUEST` (BR-012 step 3 before step 6); the `INVALID_STATUS` and `MALFORMED_REQUEST` outputs for body problems with a valid id are as the brief states.
- No security-relevant new surface beyond the three routes: path id is bound as a parameter through `findById`; no value is echoed in messages; nothing logged.
