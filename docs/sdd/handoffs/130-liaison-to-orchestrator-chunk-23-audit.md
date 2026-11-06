# Handoff 130: Audit report, review chunk 23 (Task 23a, Task 23b)
- From: liaison
- To: orchestrator (Sponsor gate, with the Security verdict)
- Phase: 4   Round: 1
- Type: AUDIT_REPORT
- Brief: handoff 128; delivery: handoff 129
- Branch: `feature/task-23-add-rules`, commits 9d7092b (23a), 2af73e8 (23b), 72b5a5f (delivery); base 74c0bfa

Mode: F (with G for the oracles)
Artifacts reviewed: tasks.md 23a/23b, handoffs 128/129, the diff 74c0bfa..HEAD (BookService, BookRepository, BookKeyRow, BookController, DuplicateKey, LibrarySeeder, BookServiceAddRulesTest, AddRulesIntegrationTest, BookListIntegrationTest, AddBoundIntegrationTest). Spec rows were checked through the criteria text and the tests' expected values, not re-read in full.

## Verdict
- Task 23a: ACCEPT. Compliance: criteria 1 to 4 met. Tests: green.
- Task 23b: ACCEPT WITH ONE CONDITION (F-1 below): the bound test does not exercise the worst case of the scan optimization the implementer introduced. The condition is one extra test, no production change expected. If the new test fails the 2 s bound, STOP: that is the ASK FIRST schema change for the Sponsor (criterion 3).
- Security verdict is separate and is not replaced by this report.

## Evidence I produced
- `./mvnw -o clean verify` re-run by me: BUILD SUCCESS, integration 185 run, 0 failures, 1 skipped; `git status` clean afterwards (the build wrote only ignored output). Matches the delivery.
- My timings on the same machine class (4 CPUs, Linux amd64, Java 21.0.12.1): duplicate first 809 ms (cold), 118, 103; non-matching add 132; C-11 steps 84 and 94 ms. Same order of magnitude as the delivery (928 / 112 / 143 / 143 / 234 / 81). Bound 2000 ms.
- No file over 300 lines among the test and main files checked (awk over `*.java` in the touched folders showed only the total line).

## Brief constraints 1 to 7
| # | Constraint | Result | Evidence |
|---|---|---|---|
| 1 | Stored-text keys, earliest match, tie, white-space test | Met | `findEarliestMatch` uses `DuplicateKey.textKey` on stored rows and `DuplicateKey.of` on the already normalized incoming pair. `EARLIEST = dateAdded, then id` (String compare of a UUID text, ASCII, so lexical). Tests: `reportsTheEarliestOfTwoMatchesByDateAdded`, `breaksATieOnTheMillisecondByTheLowestIdTextWhateverTheRowOrder` (both row orders), integration `reportsTheEarliestMatchAndTheLowestIdOnATie`, `DuplicateKeyTest.whiteSpaceEqualsTheUnicodeWhiteSpacePropertyOverAllCodePoints` |
| 2 | Order count, scan, insert; LIBRARY_FULL first and not skipped by override | Met | `create`: validate, `count() >= 5000` throws, then `if !TRUE.equals(allowDuplicate)` scan, then save. `InOrder` unit test; 3 override values at 5,000 with `never().findAllBy()`; integration 4,999 -> 5,000th stored -> next 409 |
| 3 | Status/rating/dates ignored; Straße/STRASSE; T-12; seeding via repository | Met | Scan reads only the four key columns; `LibrarySeeder` uses `saveAll`; no test-only endpoint |
| 4 | 409 bodies | Met | `DuplicateBookException` / `ApiException(LIBRARY_FULL)`; the integration test asserts no `details` on LIBRARY_FULL |
| 5 | Filter from the gate attribute | Met | `@RequestAttribute(QueryPolicy.STATUS_ATTRIBUTE)` set at `RequestGateFilter:146`; no `getParameter*` in the controller. `?status=%72eading` test proves the decoded value is used |
| 6 | 2 s bound methodology and numbers | Partly met, see F-1 | 4,999 max-size books, monotonic clock, real port, machine recorded, numbers real and reproduced. Gap: seed uses a distinct author per book |
| 7 | Oracles from the spec; 300 lines; no logging | Met | Expected values are literal spec values (409 codes, ids, counts, `[]`/0). `BookKeyRow.toString()` hides members; no logger added |

Order note: validation (400) runs before the count (409 at capacity), so an invalid body at 5,000 books gives 400. That is consistent with the brief's order ("inside the transaction: count first") only if the spec says validation precedes capacity; I found no contradiction in the tasks text but did not re-verify BR-009 wording. Orchestrator/PM: confirm in one line, no action expected.

## Judgement of the five deviations
1. `BookServiceAddRulesTest` split: ACCEPT. The 300-line rule (constitution Code Style) forced it; the file is cohesive and named after the rule. Record it in progress.md (done by the implementer) and in tasks.md "Tests" lines at the next PM edit (cosmetic).
2. Class-based projection `findAllBy()` instead of `@Query`: ACCEPT. Task 04's `repositoryDeclaresNoNativeOrHandWrittenQueries` is an existing constitutional guard (parameterised-only, no hand-written queries), and the derived projection still selects four columns with no entity load. Required follow-ups: (a) the `BookKeyRow` Javadoc says "It is a JPQL constructor projection", which is now inaccurate; fix wording ("class-based projection"); (b) PM/coding agent to change the plan wording "JPQL constructor projection" to "derived class-based projection" in `plan.md`/`plan-appendix.md` (plan is amended by the coding agent through Plan Amendments, not by me). Neither changes behavior.
3. `list(String)` signature: ACCEPT. Needed for 23b criterion 1, keeps the controller thin, and the service re-checks the value (defence in depth, same pattern as `create`). Test call sites updated; `BookServiceTest` covers the invalid value.
4. Author-key-first scan: ACCEPT on equivalence. `authorKey(row).equals(a) && titleKey(row).equals(t)` is logically identical to comparing full `DuplicateKey` values, and the short circuit cannot change the result. The cost is that it moves the worst case to the "same author, different titles" input, which is not tested (F-1). Equivalence itself is covered indirectly by the earliest-match and case-variant tests; a one-line unit assertion that a same-author/different-title row does not match would make it explicit (included in F-1).
5. `DuplicateKey.isWhiteSpace` package-private: ACCEPT. Minimal widening, needed by the F-1(Task 22) oracle, with Javadoc added. No public API change.

## Findings
| ID | Severity | Location | Finding | Required action |
|---|---|---|---|---|
| F-1 | MAJOR (condition for 23b) | `AddBoundIntegrationTest`, `LibrarySeeder.maxAuthor` | Every seeded book has a distinct author ("A<i> ..."), so with the author-first short circuit the 500-code-point title key is computed only for the matching row. The non-matching add (132 to 234 ms) therefore measures the cheap path. The worst case, which is the one M-2 must bound, is 4,999 books with the same 200-cp author and different 500-cp titles (both keys computed for every row, about 2.5 times more normalization work), and also an incoming key whose author matches all rows. Not tested. | Add a test with a seed variant (same maximum-size author, distinct maximum-size titles): a non-matching add gives 201 in 2 s and a duplicate of the last stored book gives 409 in 2 s. Record the numbers in progress.md. If it fails: STOP, ASK FIRST schema change to the Sponsor. Do not weaken the test. Expected to pass with margin from the numbers seen, but it is unproven. |
| F-2 | MINOR | `BookKeyRow` Javadoc | Says "JPQL constructor projection"; the implementation is a class-based derived projection. | Reword. |
| F-3 | MINOR | `plan.md` / `plan-appendix.md` | Wording "JPQL constructor projection" now stale. | Plan Amendment by the coding agent, PM tracks. |
| F-4 | MINOR | `AddBoundIntegrationTest` | The comment/delivery calls the first request "cold", but the first test run in the JVM is whichever method JUnit picks; only the first of the class is cold. Numbers are still real (809/928 ms for the first). | Wording only; for Task 41 take the reference-machine figures in a fixed order. |
| F-5 | OBSERVATION | `create` | `findById` after the scan is a second query for the matched book, not in a loop. Acceptable (BR-009 forbids per-row queries). Count and scan share one transaction but SQLite single-writer races (C-11 c) are not proven here; correctly left to its owning task. | None now. Track C-11 c. |
| F-6 | OBSERVATION | status.md | No rows for 23a/23b in the board (implementer noted). | Orchestrator adds them. |

## The "database is locked" FAILED lines
They are real output of `SqliteStoreIntegrationTest` (lines `FAILED The database books.db could not be opened and claimed | ... database is locked`), printed twice in my run. The test at lines ~160 and ~186 deliberately starts a second JVM against a claimed database and asserts `contains("database is locked")`; the class result is "Tests run: 16, Failures: 0, Errors: 0, Skipped: 1". They are the expected output of the startup-claim refusal, not a failure of this chunk. The one skip is the pre-existing permission `Assumptions` test (running as root). Suggest the Sponsor is told once so the lines are not read as errors; no action.

## Did chunking work?
Yes, with a nuance. The chunk produced no defect that a per-task review would have caught earlier: the one real gap (F-1) lies in the bound test of 23b, which depends on a design choice (deviation 4) made inside the chunk; reviewing 23a alone would not have seen the 500-cp titles at all, and a review after 23b alone would have found the same thing. The stale Javadoc (F-2) is also a 23a artifact but cosmetic. Cost saved: one audit and one Security pass for two dependent tasks. Risk to watch: the deviation 4 optimization crossed the task boundary (introduced in 23a code, justified by the 23b bound). Recommendation: keep chunking for dependent pairs where the second task only verifies the first; require, in future briefs, that a performance optimization names the worst-case input in the bound test.

## Follow-ups
1. Implementer: F-1 test (condition), F-2 Javadoc. Progress.md note with the numbers.
2. Coding agent / PM: F-3 plan wording.
3. Orchestrator: status rows (F-6); remind commit `[Task-23b] ... - ACCEPTED` and the progress update after Sponsor decision; PR only after Sponsor approval (CLAUDE.md).
4. Security reviewer verdict is still required for the chunk before the Sponsor gate.

Next step: implementer closes F-1 and F-2 (one small commit tagged `[Task-23b]`), Security verdict, then Sponsor gate.
