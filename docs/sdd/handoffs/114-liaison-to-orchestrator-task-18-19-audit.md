# Handoff 114: Audit, Task 18+19 (text policy and Unicode fixture check)
- From: liaison
- To: orchestrator
- Type: REVIEW_REPORT
- Mode: F (with G for oracles)
- Artifacts reviewed: brief 112, delivery 113, `tasks.md` Task 18+19, `spec.md` BR-006, `TextPolicy.java`, `TextPolicyTest`, `TextPolicyMarksTest`, `UnicodeFixtureCheckTest`, `support/UnicodeFixtures`, `git diff/log origin/main..HEAD`
- Security gate: separate (handoff 115 exists from `sdd-security-reviewer`); not replaced here.

## Verdict: ACCEPT WITH FOLLOW-UPS
ACCEPT Task 18+19. Compliance: criteria 1 to 8 met, constraints 1 to 8 met, no blocking finding. Tests: re-run offline `./mvnw -o -q clean verify` completed with no failure output; surefire: UnicodeFixtureCheckTest 6, TextPolicyTest 74, TextPolicyMarksTest 36, all 0 failures/errors/skips (matches delivery). Proceed to Task 20 after the Sponsor gate and the Security verdict.

## Layer 1, specification compliance
| Item | Result |
|---|---|
| Criterion 1 fixture check | Met. JDK feature 21 and update >= 12; U+1E030 defined, U+2FFC not; 33 named code points with expected categories written as spec values (not read from `TextPolicy`); NFC of T-24b, T-25 and the 300 x U+1D15E (600) cases. Independent of `TextPolicy`. |
| Criterion 2 trim/tab/LF/NFC | Met. Own trim loop on `\p{IsWhite_Space}`; 16 white-space code points incl. U+0085, U+00A0, U+2028, U+2029, U+3000; U+001C and U+00A0 prove it is not `String.strip`. Tab/LF inside rejected, at ends trimmed. Stored form asserted as "Café  AU  Lait" (case and inner spacing kept). |
| Criterion 3 length | Met. 500/501 title, 200/201 author, 500 x (e+U+0301) accepted, 300 x U+1D15E rejected TOO_LONG, 16000 chars gives TOO_LONG, length before characters. Check order required, empty, length, characters matches BR-006. |
| Criterion 4 characters | Met. 30 forbidden code points (Cc, Cf except ZWNJ/ZWJ, Zl, Zp, PUA planes, unassigned, tags, FFFE/FFFF/FDD0/FDEF/plane ends); U+FFFD accepted; Zs inside accepted. Noncharacter test `(p & 0xFFFE) == 0xFFFE` is correct. |
| Criterion 5 | Met. Blank fillers (all five), mark-first (Mn, Mc, Me, VS16), "ab"+U+3164+"cd" accepted. |
| Criterion 6 | Met. 8 accepted, 9 rejected, 7+ZWJ+"y" accepted, 8+ZWJ+mark rejected, U+E0100/E0101 and FE0F+ZWJ+FE0F rejected, selectors in separate runs accepted. Code: run counter and selector counter reset on a non-run code point; selector counts toward 8. Correct. |
| Criterion 7 | Met. Family emoji, Persian word accepted; 12 misplaced-joiner rows (first, last, ZWNJ/ZWJ pairs in all four combinations, space, NBSP, U+3000). |
| Criterion 8 | Met. Single `int[]` pass, no regex over the text; T-24b 50-repetition input (452 code points) rejected, 2 s bound, no `Thread.sleep`; plus an 8000-mark run. |
| Constraint 5 reject-only | Met. `normalize` only trims and NFCs; invalid text never repaired. |
| Constraint 6/8 logging | Met. No logger, no text in the enum, trivial test that the result carries no text. |
| Scope | Met. Only the five Java files plus handoffs, `progress.md`, one `status.md` line (Task 17 DONE, the board). Nothing under `backend/config/**` or `pom.xml`; no new dependency. |
| Javadoc on public API | Present on class, constants, enum, `normalize`, `check`. |

## BR-006 examples to tests (expected values from the spec text)
Passing: "Les Misérables"/"Victor Hugo", 500 emoji, 500 decomposed é, family emoji, Persian word, "ab"+U+3164+"cd", "x"+8 U+0301, "x"+7 U+0301+ZWJ+"y", U+FFFD between letters: all present. Failing: "", "   ", U+00A0, 501, NUL, LF, tab, U+200B, U+202E, U+E000, U+0378, U+E0041, U+FFFE, U+FDD0, only U+3164/U+2800/U+0301, leading U+0301, 9 marks, 8+ZWJ+mark, E0100+E0101, FE0F+ZWJ+FE0F, joiner first/last, ZWNJ next to ZWJ, ZWJ next to ZWJ, joiner next to space: all present with the expected Problem asserted (EMPTY vs FORBIDDEN_CHARACTERS vs TOO_LONG, not just "is present"). `"\uD800"` MALFORMED_REQUEST correctly left to Task 21; the `TextPolicy` defence-in-depth rejection is tested.

## Layer 2, testing
Green (own run). Oracles are spec-derived and assert the specific Problem, so no weak-oracle blocker. Coverage 98.6% line for `TextPolicy` is the delivery's figure; the build's JaCoCo check passed (not independently read).

## Layer 1b, repository hygiene
- Commits `f3919c1`, `5f81d71` (and docs `b23924b`): author krlosferra; Conventional Commits with `[Task-18-19]`; atomic by step (1 to 4, then 5 to 8); no trailers, no tool/model/vendor names, no session lines (grep clean on messages and changed files).
- Branch name `feature/task-18-19-text-policy` per tasks.md. No secrets, DB or build output tracked.
- Line limit: 193 / 170 / 145 / 86 / 67, all under 300.
- Working tree also shows uncommitted `docs/sdd/security.md`, `status.md` and handoff 115 (not part of the two commits; orchestrator/Security files).

## Deviation: `requirePinnedData()` in `@BeforeAll` instead of a class orderer
Accepted. It achieves the intent of constraint 2 (a data mismatch fails the rule classes before any rule is judged) with fewer moving parts and no new resource file. Limits, recorded as follow-ups: the guard checks only JDK version and the two Unicode 15.0 sentinels, not the category or NFC assertions; those fail in `UnicodeFixtureCheckTest`, which fails the build but does not stop the rule classes in the same run. Acceptable for this task; any later class that depends on the Unicode data must also call the guard.

## Findings
| ID | Sev | Location | Finding | Resolution |
|---|---|---|---|---|
| F-1 | MINOR | Delivery 113 | The `git diff --stat` and author log promised "pasted below" are not in the file; evidence was required by the brief. | Audited directly with git instead; orchestrator to add or note it. Not blocking. |
| F-2 | MINOR | `TextPolicy.isWhiteSpace` | A `Matcher` plus a new `String` is created per examined end code point. Linear and bounded (only leading/trailing white space), but it is a regular expression, against the spirit of "no regex" (constraint/criterion 8 targets text-wide backtracking, which is absent). | Optional: replace with an explicit White_Space code point set or `Character`-based test (still no hand-copied tables beyond the 25 White_Space code points; weigh against constraint 4). Not required. |
| F-3 | MINOR | Test sources | The Persian word is a source literal containing an invisible U+200C; the header claims fixtures are built from code points. Nothing asserts that the literal still holds U+200C (an editor could strip it and the "accepts" tests would still pass; the NFC test would too). I confirmed one U+200C is present now. | Add `assertThat(PERSIAN).contains(of(ZWNJ))` or build it from code points. |
| F-4 | MINOR | `TextPolicyMarksTest` timing tests | `assertThatCode(() -> title(input)).doesNotThrowAnyException()` is redundant; `doesNotReturnTheTextInAnyResult` is trivially true for an enum. Harmless. | Optional cleanup. |
| F-5 | MINOR | Coverage of NFC-then-judge | No test shows items 6 to 6d are judged after NFC (for example a decomposed form that composes to a letter and removes a leading mark or shortens a run). | Add one row at Task 20 or later; spec sentence "judged on the text after items 2 and 3". |
| F-6 | INFO | Risk | JDK pin is "21.0.12 or later", not exact build; the Eclipse Temurin pin stays unverified (progress.md). | Already recorded. |

## Follow-ups for the orchestrator
1. Set the audit column of Task 18+19 in `status.md` to `ACCEPT WITH FOLLOW-UPS (handoff 114)`, with F-1 to F-5 carried; combine with the Security verdict (handoff 115) at the Sponsor gate.
2. Task 20: map `Problem` to the BR-006 messages (required, empty, length, characters); keep `check` as the single decision point; add the F-5 row.
3. Task 21 owns the `"\uD800"` and invalid UTF-8 MALFORMED_REQUEST rows.
4. Remind: update `progress.md` is done by java; after the Sponsor approval open the pull request (squash) with message `[Task-18-19] ... - ACCEPTED`, no attribution lines of any tool in the commit or in the pull request, per CLAUDE.md git rules.
