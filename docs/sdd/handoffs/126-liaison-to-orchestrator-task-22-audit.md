# Handoff 126: Liaison audit, Task 22 (duplicate key and service exceptions)
- From: liaison
- To: orchestrator
- Phase: 4   Round: 1
- Type: REVIEW_REPORT

Mode: F (with G for the oracles)
Artifacts reviewed: brief 124, delivery 125, tasks.md Task 22, spec BR-003, US-004.5, T-40, Appendix A, 5.2; `DuplicateKey`, `ApiException`, `DuplicateBookException`, `DuplicateErrorBody`, `GlobalExceptionHandler`, `DuplicateKeyTest`, `GlobalExceptionHandlerTest`, progress.md, status.md; commits 4deaa81, ddfe141, 085bb97, 811b60f (base 0d31888).

## Verdict: ACCEPT WITH FOLLOW-UPS

ACCEPT Task 22. Compliance: criteria 1 to 4 and constraints 1 to 6 met. Tests: `./mvnw -o clean verify` re-run here, build green; surefire reports show DuplicateKeyTest 16/16 and GlobalExceptionHandlerTest 16/16, 0 failures. Security verdict from `sdd-security-reviewer` is still needed (I cannot replace it). Proceed to Task 23a after it and after the Sponsor gate.

## Layer 1 (specification compliance)
| Item | Result | Evidence |
|---|---|---|
| C1 NFKC, `toLowerCase(Locale.ROOT)`, collapse, trim | Pass | `DuplicateKey.generalKey`: NFKC, then lower case, then White_Space run collapse and trim. Order matches BR-003 Definition. |
| Full-width "Ｃlean Code" | Pass | Test input built from U+FF23 and asserted unequal to "Clean Code" first, so the match must come from NFKC. |
| C2 Straße vs STRASSE no match; Clean Code / CLEAN CODE match | Pass | `sharpSDoesNotMatchDoubleS`, `sameAuthorCaseVariantsOfOneTitleMatch`. Values are those of T-40 and US-004.5, not derived from the code. |
| C3 ASCII fast path = general path | Pass | Fast path exists (`asciiKey`). Test: seed 22, 10,000 pairs, 25 percent spaces, chars 0 to 127, plus boundary samples (`@ [ ` {`, 0x7F, 0x00, 0x1C to 0x1F, 0x09, 0x0D). Controls 0x1C to 0x1F are not collapsed in either path, correct for White_Space. Weakness: the `pairs` counter assertion is trivial (see F-4). |
| C4 ApiException through the factory; 409 body | Pass | `handleApi` uses `respond(code)`; `handleDuplicate` builds from `factory.create(code)`. Most specific handler wins for `DuplicateBookException`. `BookValidationException` and `ErrorBodyFactory` untouched. |
| Exact 409 JSON | Pass | String-equality test; compared with spec 5.2 example: same members, same order, `rating:null`, `date_completed:null`, `existing_book` with exactly the 7 Appendix A members, no list, no key. |
| No `Object` in details (SEC-F-109) | Pass | `DuplicateErrorBody.Details` has one typed `BookResponse` component; reflection test on both records. Weakness: it checks `!= Object.class` on the body and `== BookResponse` on Details, adequate. |
| Keys not logged, not in body | Pass | grep: no logger in the new classes; `toString` hidden (tested). Exception message is the code name (SC-16). `DuplicateBookException` stores a `BookResponse`, not an entity. |
| Locale independence | Pass | Default locale set to tr-TR, "TITLE" (contains I) still gives "title" on both paths; U+0130 gives i + U+0307, the default mapping. |
| Scope / files | Pass | Only the files listed in the task and brief; no pom, no config, no new dependency. Files at or under 300 lines. |
| Layer 1b hygiene | Pass | Author `krlosferra`, Conventional Commits with `[Task-22]`, atomic commits, no trailers, no tool, model or vendor name, no session link in any message (checked with `git log`). |

White_Space list: I compared the literal list in `DuplicateKey.isWhiteSpace` with `\p{IsWhite_Space}` (the matcher TextPolicy uses) over every code point 0 to 0x10FFFF using the JDK here: 0 differences. The list (09 to 0D, 20, 85, A0, 1680, 2000 to 200A, 2028, 2029, 202F, 205F, 3000) is exactly the UCD set for this JDK.

## Findings
| ID | Severity | Location | Finding | Resolution |
|---|---|---|---|---|
| F-1 | MINOR | `DuplicateKey.isWhiteSpace` vs `TextPolicy.isWhiteSpace` | Two separate definitions of White_Space. Acceptable now (identical, and it follows the earlier Liaison F-2 / SEC-F-154 advice to leave the regex matcher). Risk is drift if one is edited. | Follow-up: add a test (any task that touches either, ideally 23a) that checks `DuplicateKey` against `\p{IsWhite_Space}` for all code points, or have TextPolicy expose one shared predicate (a plan-level choice, not required now). Not blocking. |
| F-2 | MINOR | Delivery "Risks" | Idempotence is sampled, not proved; not a requirement of the spec. Stated honestly by the implementer. | None. |
| F-3 | MINOR | Handoff 125 | The delivery says "10 files, 501 insertions" before its own commit; the task diff here is 11 files (the delivery file). Expected. Also status.md had no Task 22 row; the delivery diff edits only the Task 21 row to DONE. | Orchestrator adds the Task 22 row (IN_REVIEW, audit column: ACCEPT WITH FOLLOW-UPS, handoff 126). |
| F-4 | MINOR | `asciiFastPathEqualsTheGeneralPathOnTenThousandGeneratedPairs` | `pairs++` and `isEqualTo(10_000)` assert nothing about behavior; the real checks are the three asserts inside the loop, which are sound. | Cosmetic; drop on next touch. |
| F-5 | MINOR | Task header vs branch | Branch `feature/task-22-duplicates` differs from `feature/task-22-duplicate-key` in tasks.md (same pattern as Tasks 17, 21). | PM aligns the name in tasks.md. |

No BLOCKER or MAJOR findings.

## Note for Task 23a (checked against spec)
- Stored text to `DuplicateKey.of`: correct per BR-003 ("already trimmed and NFC-normalized text"). 23a must pass the stored/normalized values (`TextPolicy.normalize` output) for both the incoming pair and the rows scanned, never the raw request text.
- Earliest match: spec US-004.2 and DEC-005 say earliest `date_added`, ties lowest `id`. The delivery wording "earliest match (date_added, then lowest id)" is consistent. Remind 23a that `id` is a UUID string, so "lowest id" is a plain lexical comparison of the stored text (state this in the 23a brief), and that `date_added` has millisecond precision so ties are possible.
- The 409 must be `LIBRARY_FULL` before `DUPLICATE_BOOK` (US-004.9, BR-012); the exception classes here carry no ordering logic, correct for this task.
- `allow_duplicate: true` skips the duplicate check but not the capacity check.

## Follow-ups
1. Orchestrator: add the Task 22 row to status.md (IN_REVIEW, Liaison ACCEPT WITH FOLLOW-UPS handoff 126, Security pending) and merge only after the Security verdict and Sponsor approval (pull request then squash, per CLAUDE.md).
2. Security reviewer: confirm SEC-F-109 (typed body), SC-16 (no text in messages), SC-02 (no keys exposed).
3. 23a brief: carry the three points above, and include F-1 test if cheap.
4. PM: align branch name in tasks.md (F-5).
5. Reminder: progress.md already has the Task 22 entry; update its status to DONE at merge, commit `[Task-22]`.

Next step: Security review of Task 22, then Sponsor gate.
