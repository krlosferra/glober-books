# Handoff 117: Task 20 delivery (validation wiring and field-error mapping)
- From: java | To: liaison | Phase: 4 | Round: 1 | Type: TASK_DELIVERY
- Branch: `feature/task-20-validation` (task header says `feature/task-20-validation-wiring`; checked-out name kept). Base: d23be69 (Task 18+19 merged). Local commits only, not pushed.

## Resume point
- Done: production code, tests, `./mvnw clean verify` green, progress.md entry, this delivery.
- Next: nothing for the implementer; the orchestrator pushes and routes to the Liaison and Security. The row in status.md is left to the orchestrator (no Task 20 row exists on the board).

## Files
Created: `config/BookText.java`, `config/ValidatorConfig.java`, tests `config/BookTextTest`, `BookValidationIntegrationTest`.
Edited: `dto/CreateBookRequest`, `error/GlobalExceptionHandler`, `service/BookService`, `service/BookValidationException`, tests `GlobalExceptionHandlerTest`, `BookServiceTest`, `BookControllerTest`, `BookApiIntegrationTest`, `docs/sdd/progress.md`.

## Deviations (for the Liaison)
1. Extra production edit: `BookValidationException.messageFor(Problem, max)` holds the fixed message texts so `BookText` and the service share one list (no duplicated rules). Not in the task file list.
2. Extra nested class `CreateBookRequest.StatusDeserializer` (extends Jackson 3 `StringDeserializer`): absent `status` stays null (= unread), explicit null becomes "" (= INVALID_STATUS). The plan does not say how absent and null are told apart. `@JsonSetter(nulls=AS_EMPTY)` was tried and also turned a missing member into "" (broke the add test), so the deserializer was needed. Task 21 may move it.
3. Extra test file `BookValidationIntegrationTest` (real port, full context, required by constraint 5). Existing tests whose oracle came from the Task 07 thin checks were corrected to the spec (empty title message "must not be empty", `{}` order title then author).
4. Status is checked by `@Pattern("unread|reading|completed")` with the fixed message; the service repeats it with `BookStatus.fromWire`.
5. The handler passes only messages from a fixed list (else "is invalid"), so no constraint text can leak.
6. Local `main` is behind origin, so `git diff main..HEAD` shows earlier merged tasks; the stat below is against d23be69, the Task 18+19 merge.

## Criteria to tests
| Criterion / spec row | Tests |
|---|---|
| 1, US-001.4, .10, T-09 (`{}`, order, one entry per field) | GlobalExceptionHandlerTest.emptyObjectGivesValidationErrorWithTitleThenAuthor, titleAuthorAndStatusFailuresAreOrderedTitleAuthorStatus; BookControllerTest.addRejectsMissingTitleAndAuthorWithBothFields; BookValidationIntegrationTest.emptyObjectGivesTitleAndAuthorEntries |
| US-001.4 boundaries 500/501, 200/201, required/empty | BookTextTest (acceptsTitleOf500AndAuthorOf200CodePoints, reportsNullAsRequiredAndBlankAsEmpty, reportsLengthAt501AndAt201); BookValidationIntegrationTest.titleOf501CodePointsIsRejectedAndOf500Accepted |
| BR-006 check order, characters | BookTextTest.firstFailingCheckWinsLengthBeforeCharacters, reportsForbiddenCharactersWithTheFixedMessage; Integration.authorWithZeroWidthSpace... |
| 2, T-35 (status only) | GlobalExceptionHandlerTest.onlyStatusFailingGivesInvalidStatus; Integration.onlyInvalidStatusGivesInvalidStatus; BookServiceTest.reportsInvalidStatusAloneAsInvalidStatus |
| 2, T-34 (title then status) | GlobalExceptionHandlerTest.statusWithAnotherFailing...; Integration.invalidStatusWithEmptyTitle...; BookServiceTest.reportsInvalidStatusWithAnotherField... |
| 2, null vs absent | GlobalExceptionHandlerTest.explicitNullStatus...; Integration.explicitNullStatusIsInvalidStatusAndAbsentStatusIsUnread; BookServiceTest.treatsTheEmptyStatus... |
| 3, T-13 | BookTextTest.messageNeverEchoesExpressionTextOrItsResult, literalInterpolatorEvaluatesNothing; GlobalExceptionHandlerTest.hostileTitleOf501...; Integration.hostileTitleIsRejectedAndNothingEchoes... (body and log lines) |
| 4 thin checks removed, same exception type | BookServiceTest.repeatsTheCharacterRuleAndNamesTheField..., storesTheNormalizedTextTrimmedAndInNfc, rejectsEmptyBlankAndNullTitleWithField |
| Carry: item 6 after NFC; Persian from code points | BookTextTest.itemSixIsJudgedAfterNfc..., acceptsThePersianWordBuiltFromCodePoints (U+200C asserted) |

## Real-port requests (BookValidationIntegrationTest, from the verify log)
| Request | Status | Code | details |
|---|---|---|---|
| `{}` | 400 | VALIDATION_ERROR | title "is required", author "is required" |
| title 501 chars | 400 | VALIDATION_ERROR | title "must be between 1 and 500 characters" |
| author with U+200B | 400 | VALIDATION_ERROR | author "contains characters that are not allowed" |
| status "Done" only | 400 | INVALID_STATUS | status "must be one of: unread, reading, completed" |
| title "" + status "Done" | 400 | VALIDATION_ERROR | title "must not be empty", status (in that order) |
| `"status": null` | 400 | INVALID_STATUS | status |
| absent status | 201 | | status "unread" |
| T-13 `${7*7}` + 495 chars (501) | 400 | VALIDATION_ERROR | title length message; body and log lines contain neither `${7*7}` nor the text |

## Build and coverage
`cd backend && ./mvnw clean verify`: BUILD SUCCESS, unit/slice Tests run 611, failures 0; integration Tests run 154, failures 0, skipped 1 (pre-existing); Checkstyle and JaCoCo gate passed; overall line coverage 96.6 percent (717 of 742). New classes: BookText.Validator 9/9 lines, ValidatorConfig.LiteralMessageInterpolator 3/3, CreateBookRequest.StatusDeserializer 3/3, BookValidationException 11/11, BookService 28/28, GlobalExceptionHandler 40/42. Every file at or under 300 lines. No new dependency, no pom or resource-folder change, no logging of request text.

## git diff --stat d23be69..HEAD (before this file)
```
 .../com/globant/globerbooks/config/BookText.java   |  73 ++++++++++
 .../globerbooks/config/ValidatorConfig.java        |  41 ++++++
 .../globant/globerbooks/dto/CreateBookRequest.java |  43 +++++-
 .../globerbooks/error/GlobalExceptionHandler.java  |  39 +++--
 .../globant/globerbooks/service/BookService.java   |  33 ++---
 .../service/BookValidationException.java           |  17 +++
 .../globerbooks/BookApiIntegrationTest.java        |   2 +-
 .../globerbooks/BookValidationIntegrationTest.java | 157 +++++++++++++++++++++
 .../globant/globerbooks/config/BookTextTest.java   | 139 ++++++++++++++++++
 .../globerbooks/controller/BookControllerTest.java |   9 +-
 .../error/GlobalExceptionHandlerTest.java          |  82 +++++++++++
 .../globerbooks/service/BookServiceTest.java       |  37 ++++-
 .../116-orchestrator-to-java-task-20-brief.md      |  26 ++++
 docs/sdd/progress.md                               |   4 +
 docs/sdd/status.md                                 |   2 +-
 15 files changed, 657 insertions(+), 47 deletions(-)
```

## git log d23be69..HEAD (author and body)
```
1e1dec3 krlosferra <carlos.ferra@globant.com>
docs(progress): record Task 20 [Task-20]
---
0327eb3 krlosferra <carlos.ferra@globant.com>
test(validation): align add-api oracle and log check with the spec [Task-20]
---
1983d42 krlosferra <carlos.ferra@globant.com>
test(validation): cover wiring, field-error mapping and T-13 [Task-20]
---
38519e9 krlosferra <carlos.ferra@globant.com>
feat(validation): wire text policy into Bean Validation and field errors [Task-20]
---
7ecf9da krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 20 brief, Task 18+19 done [Task-20]
---
```

## Risks
- The `isWhiteSpace` regex matcher was left as is (optional, out of scope).
- `FIXED_MESSAGES` in the advice repeats the spec list; Task 30 will add the rating message.
