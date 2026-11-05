# Handoff 113: Task delivery, Task 18+19 (text policy and Unicode fixture check)
- From: java
- To: liaison
- Type: TASK_DELIVERY
- Branch: `feature/task-18-19-text-policy`; commits: f3919c1 (criteria 1 to 4), then the step 2 commit (criteria 5 to 8, progress, this file). Local only, not pushed.

## Resume point
- Done: step 1 (criteria 1 to 4) and step 2 (criteria 5 to 8). `./mvnw clean verify` green. No split into 18 and 19 was needed.
- Next: Liaison audit and Security review. Nothing pending for java.

## Files
Created: `backend/src/main/java/com/globant/globerbooks/service/TextPolicy.java` (193 lines), tests `service/TextPolicyTest.java` (170), `service/TextPolicyMarksTest.java` (145), `service/UnicodeFixtureCheckTest.java` (86), `support/UnicodeFixtures.java` (67). Modified: `docs/sdd/progress.md`; this handoff. Nothing under `backend/config/**` or `pom.xml`.

## Design
- `TextPolicy.check(String raw, int max)` returns `Optional<Problem>` (`REQUIRED`, `EMPTY`, `TOO_LONG`, `FORBIDDEN_CHARACTERS`), the first failing check in the BR-006 order; `normalize(String)` returns the stored form (trimmed NFC). `TITLE_MAX` 500, `AUTHOR_MAX` 200. Pure, no framework types, no logging; the enum carries no text.
- Trim: own loop over code points using `\p{IsWhite_Space}` of the JDK (not `String.strip`: strip keeps U+00A0 and U+202F and trims U+001C to U+001F, which are not White_Space; tested with U+00A0 and U+001C).
- Characters: one linear pass over the code point array (forbidden categories, noncharacters, tags, begins-with-mark, joiner placement, visible content, mark run length and variation selector count). No regular expression runs over the text.
- Lone surrogates in a Java string are reported as FORBIDDEN_CHARACTERS (defence in depth; the strict reader of Task 21 rejects them earlier as MALFORMED_REQUEST).
- Ordering mechanism: `UnicodeFixtureCheckTest` plus `UnicodeFixtures.requirePinnedData()` called in `@BeforeAll` of `TextPolicyTest` and `TextPolicyMarksTest`, so a data mismatch fails every rule class before any rule is judged (no class-order property needed, no new resource file).

## Criteria to tests
| Criterion | Tests |
|---|---|
| 1 fixture check | `UnicodeFixtureCheckTest`: runsOnJdk21Update12OrLater, carriesUnicode15Data, reportsTheCategoryOfEveryCodePointTheSpecNames, composesEWithAcuteButNotXWithAcute, decomposesU1D15EIntoTwoCodePoints, leavesTheT24bAndT25InputsUnchangedByNfc |
| 2 trim, tab/LF, NFC (T-02, T-08) | trimsEveryUnicodeWhiteSpaceCodePointAtBothEnds (16 code points incl. U+0085, U+00A0, U+2028, U+2029, U+3000), trimsWhatStringStripWouldKeep..., rejectsTabAndLineFeedInsideButTrimsThemAtTheEnds, storesTheNfcFormKeepingCaseAndInternalSpacing, reportsEmptyWhenOnlyWhiteSpaceRemains (T-09 "", "   ", U+00A0), reportsRequiredForNull |
| 3 length (T-06, T-07, T-11, T-22) | acceptsTitleLimits..., acceptsAuthorLimits..., countsCodePointsNotUtf16Units, countsDecomposedEAfterNfc, rejectsThreeHundredU1D15E..., reportsLengthBeforeCharacters, reportsASixteenThousandCharacterTitle... |
| 4 characters (T-01, T-03, T-26) | rejectsForbiddenCodePointInsideText (30 code points: NUL, Cc, U+200B, U+202E, soft hyphen, LRM, WJ, ALM, BOM, Zl, Zp, private use planes, unassigned U+0378 and U+2FFC, tags, FFFE, FFFF, FDD0, FDEF, plane-end noncharacters), rejectsAnUnpairedSurrogate..., acceptsJoinersVariationSelectorAndFffdBetweenLetters, acceptsSpaceSeparatorsInside |
| 5 visible/mark first (T-23) | `TextPolicyMarksTest`: rejectsTextThatRendersBlankOrBeginsWithAMark (U+3164, U+2800, U+0301, U+0301+"abc", U+FE0F, U+115F, U+1160, U+FFA0, all five fillers, Me, Mc first), acceptsABlankFillerBetweenVisibleCharacters, acceptsNumbersPunctuationAndSymbolsAsVisibleContent |
| 6 runs (T-24, T-24b) | acceptsARunOfEightAndRejectsARunOfNine, acceptsARunOfEightWithAJoinerInside, rejectsARunOfTenMarksAndAJoiner, acceptsOneVariationSelectorAndRejectsTwoInOneRun, acceptsTwoVariationSelectorsInSeparateRuns, acceptsTheHeartFireEmojiSequence |
| 7 joiners (T-10, T-25) | acceptsAFamilyEmojiJoinedWithJoiners, acceptsThePersianWordWithANonJoiner, rejectsAMisplacedJoiner (12 rows: first, last, ZWNJ+ZWJ, ZWJ+ZWNJ, ZWJ+ZWJ, ZWNJ+ZWNJ, next to space, NBSP, ideographic space) |
| 8 no unbounded work | finishesTheFiftyRepetitionInputWellInsideATimeBound (2 s bound, no sleep; measured 0.002 s), finishesALongMarkRunAtTheSixteenKibCapInLinearTime (0.007 s) |

BR-006 examples: every passing and failing example of the spec is a row above (title of 500 emoji, decomposed e x 500, family emoji, Persian word, "ab" U+3164 "cd", "x"+8 and +7 variants, U+FFFD; "", "   ", U+00A0, 501, NUL, LF, tab, U+200B, U+202E, U+E000, U+0378, U+E0041, U+FFFE, U+FDD0, only U+3164/U+2800/U+0301, leading U+0301, 9 and 8+joiner+mark runs, two variation selectors in both forms, joiner first/last/adjacent/space). The `"\uD800"` MALFORMED_REQUEST example belongs to Task 21 (strict reader).

## Evidence
- `java -version`: OpenJDK 21.0.12.1 (2026-08-18). Fixture check: 6 tests, 0 failures (`Tests run: 6 ... UnicodeFixtureCheckTest`).
- `cd backend && ./mvnw clean verify`: surefire `Tests run: 590, Failures: 0, Errors: 0, Skipped: 0`; failsafe `Tests run: 146, Failures: 0, Errors: 0, Skipped: 1` (the skip is existing, in SqliteStoreIntegrationTest); `All coverage checks have been met.`; `BUILD SUCCESS`. New classes: UnicodeFixtureCheckTest 6, TextPolicyMarksTest 36, TextPolicyTest 74.
- JaCoCo `TextPolicy`: 69 of 70 lines covered (98.6%; the missed line is the private constructor); branches 60 of 62 covered.
- T-24b 50-repetition test: 0.002 s (surefire report).
- `git diff --stat origin/main..HEAD` and the author log are pasted below this line by the final step.

## Deviations, risks
- Deviations from the plan: none. The mechanism for "runs first" is the `@BeforeAll` guard instead of a class orderer (reason above).
- Risk: `\p{IsWhite_Space}` and `Character.getType` follow the JDK tables; the fixture test pins JDK 21 update 12 or later, not an exact build.
- Follow-up for Task 20: map `Problem` to the BR-006 messages (required, empty, length, characters) and call `check` again in the service.
