# Handoff 058: Task 05 delivery (error foundation)
- From: java | To: liaison | Type: TASK_DELIVERY | Branch: `feature/task-05-error-foundation` (local commits, not pushed)

## Files
Created: `backend/src/main/java/com/globant/globerbooks/error/{ErrorCode,ErrorBody,ErrorBodyFactory,ErrorResponseWriter}.java`, `backend/src/test/java/com/globant/globerbooks/error/ErrorBodyFactoryTest.java`, this handoff. Modified: `StartupPolicyIntegrationTest.java` (System.out removed), `StartupPolicyTest.java` (scan independent of working directory), `docs/sdd/progress.md`, `docs/sdd/status.md` (row 05). Nothing else.

## Criteria -> test
1. 16 codes, statuses, exact members, details only as `{field,message}` list: `catalogHasExactlyTheSixteenCodesOfAppendixA`, `everyCodeHasTheStatusOfAppendixA` (16 rows), `bodyWithoutDetailsHasExactlyErrorCodeAndTimestamp`, `bodyWithDetailsCarriesFieldAndMessageEntries`, `rejectsMissingOrEmptyDetails`. Expected values are typed from Appendix A.
2. Timestamp from the injected clock, UTC, milliseconds, `Z` (fixed clock on offset +5 gives `2026-02-19T10:00:00.000Z`): `timestampComesFromTheInjectedClockInTheFixedFormat`.
3. No input/class/SQL/path in texts: `noMessageContainsClassNamesSqlOrPaths`, `codesWithTextInAppendixAUseThatExactText` (8 texts from Appendix A); no `Object` member: `noDetailsMemberIsOfTypeObject` (reflection).
4. Writer: `writerSetsStatusExplicitContentTypeAndWritesOnlyTheEnvelope` (status 413, header `application/json; charset=UTF-8`, exact body, leftover buffer cleared), `writerWritesDetailsOfAPreparedBodyWithTheStatusOfItsCode`.

## Evidence (`cd backend && ./mvnw clean verify`, real)
BUILD SUCCESS (Checkstyle, SpotBugs, JaCoCo check green). Unit: 77 run, 0 failures (ErrorBodyFactoryTest 33). Integration: 31 run, 0 failures, 1 skipped (pre-existing in SqliteStoreIntegrationTest). Coverage of package `error` (jacoco.csv): 0 missed lines, 0 missed branches, 0 missed instructions in all 5 classes = 100%.
`git diff --stat main..HEAD` and `git log main..HEAD --format='%an <%ae>%n%B---'`: all commits authored by `krlosferra <carlos.ferra@globant.com>`, subjects end `[Task-5]`, no trailers or attribution.

## Deviations / notes
- Appendix A gives text for 8 codes only. The other 8 texts are new fixed strings, no input: MALFORMED_REQUEST "The request is malformed.", INVALID_HOST "The host is not allowed.", FORBIDDEN "The request is not allowed.", NOT_FOUND "Resource not found.", METHOD_NOT_ALLOWED "Method not allowed.", NOT_ACCEPTABLE "The response format is not acceptable.", PAYLOAD_TOO_LARGE "The request body is too large.", UNSUPPORTED_MEDIA_TYPE "The content type is not supported." Appendix A calls texts informative; Liaison to confirm. Appendix A and plan D2 do not disagree.
- Writer owns a private `JsonMapper` (an injected mapper tripped SpotBugs EI_EXPOSE_REP2); also keeps the envelope independent of global Jackson settings.
- Writer calls `getOutputStream()`; a caller that already used `getWriter()` gets IllegalStateException (Task 06 filters must not).
- `ErrorBodyFactory`/`ErrorResponseWriter` are `@Component`s; no Spring-context test yet (Task 06).

## Resume point
- Done: all steps, delivery, progress.md, status row set to IN_REVIEW (commit hash to be read from `git log`).
- Next: orchestrator build check, Liaison audit, Security review, push and PR by the orchestrator.
