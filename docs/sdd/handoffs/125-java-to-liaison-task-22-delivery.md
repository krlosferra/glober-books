# Handoff 125: Task delivery, Task 22 (duplicate key and service exceptions)
- From: java
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY

## Resume point
- Done: production code, tests, progress.md entry, full build green, this delivery. Local commits only, nothing pushed.
- Next: nothing for the implementer. Orchestrator: add the Task 22 row to status.md (no row existed, so none was edited), Liaison audit, Security review.

## Task, branch, commits
Task 22, branch `feature/task-22-duplicates` (the task header names `feature/task-22-duplicate-key`; checked-out name kept). Commits: feat(service) production, test(service) tests, docs(progress). Delivery commit follows this file.

## Files
Created: `service/DuplicateKey.java`, `error/ApiException.java`, `error/DuplicateBookException.java`, `error/DuplicateErrorBody.java`, tests `service/DuplicateKeyTest.java`. Modified: `error/GlobalExceptionHandler.java`, `error/GlobalExceptionHandlerTest.java`, `docs/sdd/progress.md`. Nothing else (no pom, no config, `ErrorBodyFactory` and `BookValidationException` untouched; `DuplicateErrorBody.of` takes the factory envelope so the factory stays the only source of error, code and timestamp).

```
git diff --stat origin/main..HEAD (before this delivery commit)
 .../globant/globerbooks/error/ApiException.java    |  40 ++++++
 .../globerbooks/error/DuplicateBookException.java  |  34 +++++
 .../globerbooks/error/DuplicateErrorBody.java      |  35 +++++
 .../globerbooks/error/GlobalExceptionHandler.java  |  24 ++++
 .../globant/globerbooks/service/DuplicateKey.java  | 119 ++++++++++++++++
 .../error/GlobalExceptionHandlerTest.java          |  62 ++++++++
 .../globerbooks/service/DuplicateKeyTest.java      | 157 +++++++++++++++++++++
 .../124-orchestrator-to-java-task-22-brief.md      |  25 ++++
 docs/sdd/progress.md                               |   4 +
 docs/sdd/status.md                                 |   2 +-
 10 files changed, 501 insertions(+), 1 deletion(-)
```

```
git log origin/main..HEAD --format='%an <%ae>%n%B---' (before this delivery commit)
krlosferra <carlos.ferra@globant.com>
docs(progress): Task 22 entry [Task-22]
---
krlosferra <carlos.ferra@globant.com>
test(service): duplicate key and duplicate error body [Task-22]
---
krlosferra <carlos.ferra@globant.com>
feat(service): duplicate key and service exceptions [Task-22]
---
krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 22 brief, Task 21 done [Task-22]
---
```

## Criteria and spec rows to tests
| Criterion / spec row | Test |
|---|---|
| 1, US-004.5, BR-003: case, outer and repeated white space | DuplicateKeyTest.ignoresCaseSurroundingAndRepeatedWhiteSpace, collapsesEveryUnicodeWhiteSpaceRun... |
| 1, US-004.5: full-width (from code point U+FF23) via NFKC | DuplicateKeyTest.fullWidthFormMatchesThroughNfkc |
| 2, T-40, BR-003: Straße vs STRASSE no match | DuplicateKeyTest.sharpSDoesNotMatchDoubleS |
| 2, T-40: Clean Code / CLEAN CODE same author | DuplicateKeyTest.sameAuthorCaseVariantsOfOneTitleMatch; different title or author, swapped pair do not match |
| Locale independence (Locale.ROOT) | lowerCasingIsLocaleIndependent (tr-TR default), dottedCapitalIUsesTheRootMapping |
| 3 ASCII fast path = general path | asciiFastPathEqualsTheGeneralPathOnTenThousandGeneratedPairs (Random seed 22, 10,000 pairs, chars 0 to 127 with 25 percent spaces; 30,000 assertions), asciiBoundaryCharactersAreHandledLikeTheGeneralPath |
| Idempotence key(key(x)) = key(x) | keyIsIdempotentOnGeneratedAndUnicodeSamples (2,000 seeded ASCII plus 4 Unicode samples) |
| Keys never logged | toStringNeverShowsTheKeys |
| 4 ApiException to code and status through the factory | GlobalExceptionHandlerTest.apiExceptionBecomesItsCodeAndStatusWithoutDetails; duplicateBodyHasNoMemberOfTypeObject... (message is the code name, status 409) |
| 4, Appendix A, 5.2: 409 DUPLICATE_BOOK with one existing_book | GlobalExceptionHandlerTest.duplicateExceptionGives409WithExactlyOneExistingBookInTheSpecBody (exact string compare, no extra members) |
| SEC-F-109, SC-08 no Object | DuplicateErrorBody reflection check in the same test class |

Exact 409 body asserted (string equality, Jackson with SNAKE_CASE as in application.yml):
`{"error":"A book with the same title and author already exists.","code":"DUPLICATE_BOOK","timestamp":"2026-02-19T10:00:00.123Z","details":{"existing_book":{"id":"3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b","title":"Clean Code","author":"Robert Martin","status":"unread","rating":null,"date_added":"2026-02-19T10:00:00.000Z","date_completed":null}}}`

## Build evidence
`cd backend && ./mvnw clean verify`: Tests run: 167, Failures: 0, Errors: 0, Skipped: 1 (the skip pre-exists); "All coverage checks have been met."; BUILD SUCCESS. DuplicateKeyTest 16 tests, GlobalExceptionHandlerTest 16 tests (all pass).
Real line coverage (jacoco.csv): DuplicateKey 35/35, ApiException 5/5, DuplicateBookException 4/4, DuplicateErrorBody 2/2, GlobalExceptionHandler 45/46.
Sizes: GlobalExceptionHandler 215 lines, DuplicateKey 119, GlobalExceptionHandlerTest 269; all under 300.

## Design notes and deviations
- Fast path exists (`asciiKey`), used by `textKey` when every char is below 0x80; the general path is package-visible for the proof.
- White_Space is a literal code point list (UCD), not TextPolicy's private regex matcher, to leave TextPolicy untouched. Collapse runs after NFKC and lower-casing, as the brief orders.
- No JDK difference found: lower-casing of U+0130 with Locale.ROOT gives i + U+0307, as the default mapping states; no spec gap.
- Deviation: none from the plan. The duplicate handler returns `ResponseEntity<DuplicateErrorBody>`; the other handlers keep `ErrorBody`.
- The exception carries a `BookResponse` (dto) built in its constructor from the entity, so no entity leaves the service boundary.

## Risks and follow-ups
- Task 23a must pass the stored (trimmed, NFC) title and author to `DuplicateKey.of` and choose the earliest match (date_added, then lowest id) before throwing.
- Idempotence is tested on samples, not proved for all Unicode (NFKC after lower-casing is not guaranteed stable for every code point); the key is computed once per text, so this does not affect matching.
