# Handoff 016: Spec final diff check (spec.md v0.4, requirements.md v0.4)
- From: liaison
- To: pm
- Phase: 2   Round: 3 of 3
- Artifacts: `docs/sdd/spec.md` v0.4, `docs/sdd/requirements.md` v0.4, read against handoffs 013 (my round 2 report, file `013-liaison-to-pm-spec-review-round2.md`, P-1 to P-12) and 015 (your request)
- Type: REVIEW_REPORT

Mode: B (diff check) with D (traceability) and G (oracle quality of the changed rows). Scope frozen as agreed: only real defects that would make an implementer or test author guess.

## Verdict: APPROVED

No BLOCKER, no MAJOR, no real defect. Three wording observations below are optional and non-blocking (change requests for the build or one-line edits the PM may make while closing the gate; none needs a new review). The Liaison part of the Phase 2 gate is open. The gate still needs the Security verdict and Sponsor approval.

## 1. N-01 to N-12 (P-1 to P-12)

| Finding | Result | Evidence |
|---|---|---|
| N-01 / P-1 | Applied as specified | 5.0 Routing carries the dot-segment definition (with the `%2e%2e%2e` clarification); US-007.2 and BR-011 carry the not-routable exception; E-24b exists, is in the raw-socket list and in the Appendix C view row. Re-derived: `.%2E` 404; `<id>%2f`, `%2F`, `%5c`, `%3b` 404; `<id>\x` 404 or accepted pre-application 400; `%33f2b8...` 400; `%2e%2e` 404; `%2e%2e%2e` in the id position 400. One answer each, no contradiction between US-007.2, BR-011, 5.0, E-24, E-24b, E-25 |
| N-02 / P-2 | Applied verbatim | 5.8 first paragraph; E-38 says "one of the page's asset paths"; E-15 (`/js/`, `/css/`) still holds because directory paths are 404. No conflict with the constitution (finite set of ES-module files, no build step) |
| N-03 / P-3 | Applied (US-010.6 rephrased, same behavior) | US-010.4 count shown with the list only; US-010.6 keyed on the US-010.4 count, so the empty state after deleting the last entry is defined; US-010.7 no insertion before a successful load or after a failed one; US-010.10 last request wins; US-011.5 conditioned on a successful load. The PM's added sentence in US-010.6 ("Neither message is shown in the load-failure state") is consistent with US-010.7 and creates no new behavior |
| N-04 / P-4 | Applied, deliberately different on the 2 s bound; ruling in section 2 | Section 1, DEC-030, `requirements.md` cut paragraph agree word for word on every enumerated item. E-22 uncut as the Sponsor decided |
| N-05 / P-5 | Applied, see observation O-1 | T-30 row 2 and T-27 carry the explanation in words; the escape text itself is not printed |
| N-06 / P-6 | Applied, merged with SEC-F-064 (c) | 5.0 Accept grammar. E-35 re-derived row by row: 11 inputs, 11 expected values (406; 200; 200; 406; 406; 406; 200; 406; 406; 200; 200), all consistent with the grammar including `Q=0` (ignored, so nothing left, 406) and `application/json;q=0, */*` (200, deviation stated) |
| N-07 / P-7 | Applied with the Security run definition (superseding my 6c, sound) | 6c defines a run including U+200C and U+200D, at most 8 code points and one variation selector; 6d is my wording; the "adjacent" sentence is present. Re-derived: T-24 (x + 9 U+0301 fail, x + 8 pass, two selectors fail, one passes); T-24b rows 1 to 4 fail (runs of 10, 450, 3 with two selectors, and `a` + U+0301 composes to U+00E1 then U+FE0F U+0301 U+FE0F with two selectors) and rows 5 and 6 pass (run of 8 with one joiner; U+2764 U+FE0F U+200D U+1F525); T-25 five failing rows each match one clause of 6d; BR-006 passing and failing examples match 6c and 6d |
| N-08 / P-8 | Applied | Appendix A note; pre-application list now includes 405, TRACE and method tokens; envelope check split into error part and all-responses part; raw-socket list is the union of my P-8 and SEC-F-059 with the general sentence |
| N-09 / P-9 | Applied | Appendix C: REQ-015, 016, 017, 029, 030 in rows, T-14b in the view row, constraints row for REQ-037, 038, 041 to 047, gap sentence for US-009.3, .5, .6 |
| N-10 / P-10 | Applied, one deliberate difference, accepted | Header, REQ-034, REQ-047 as specified. The totals sentence now reads "No requirement waits for a Sponsor answer" because the Sponsor confirmed REQ-009 and REQ-012; correct. Totals recomputed: 57 = 54 + 1 + 1 + 1; status 52 + 1 + 4 = 57 |
| N-11 / P-11 | Applied | Section 6 `/?utm_source=x` line; US-009.3 permission sentence |
| N-12 / P-12 | Applied | US-011.8 (Title field); US-014.2 exception naming US-011.5, US-011.8, US-012.5 (all three targets are stated there: Title, Title, the "Your books" heading); US-013.4 defines the "Messages" area; US-012.7 and 5.9 refer to it |

Counts recomputed and confirmed: 102 acceptance criteria (10, 8, 9, 9, 9, 2, 5, 7, 10, 10, 9, 6, 8); 12 active rules; 32 decisions; Appendix D 106 rows (42 T, 13 C, 40 E, 11 S); 57 requirements.

## 2. Ruling on the cut list conflict (reconciliation 3)

Question: is the 2-second bound of M-2 and C-11 a measurement or a security bound under the Sponsor's wording "the cut list removes only measurements; security items are never cut"?

Ruling: it is a security bound, and I accept the PM's position. I withdraw my round 2 reading (handoff 013, S-2 and P-4) that it is a timing item.

Reasoning:
1. The two halves of the Sponsor's sentence are not symmetrical. "Only measurements are cut" says what may be cut at most. "Security items are never cut" is the stronger, absolute clause. An item that is both a timing figure and a security control falls under the absolute clause. The deciding question is therefore the purpose of the figure, not its unit.
2. The 1 s and 300 ms figures (M-2, DEC-020) and the recorded time of C-05 measure a quality attribute ("the library feels instant"); DEC-020 and SEC-F-054 say they are measured once and recorded. Dropping them loses a quality report, not a control. The 2 s bound of C-11 is different: it is a pass/fail oracle for the cost of the add path at 4,999 maximum-size books (SEC-F-054, SEC-F-058). It is the only observable that shows the duplicate check and capacity check cannot be used to exhaust the process. Cutting it removes a security test case from Appendix D, which the Section 1 sentence "every other Appendix D case [is] never cut" already forbids.
3. The text is now executable: Section 1 names which M-2 figures are cut and which one is not, so there is no whole-item cut that drops the "never `500`" assertion, and `requirements.md` repeats the same list.
4. Consistency with the Security veto: Security holds the veto and reads it as a security bound. We agree; no Sponsor decision is needed on this point beyond noting it in the gate package.

Residual note (no change requested): DEC-020 says timing targets are "measured once to avoid flaky tests", while the 2 s bound is asserted on every run. The Phase 3 plan should give the C-11 timing test a margin and run it on the reference environment; if it proves flaky, that is a change request (raise the bound by Sponsor decision), not a reason to cut it.

## 3. Other reconciliations

- Reconciliations 1 (BR-006 6c), 2 (one dot-segment definition), 4 (raw-socket union), 6 (Accept), 7 (T-27, T-30), 8 (totals), 9 (pre-application reference): accepted as stated.
- Reconciliation 5 (the added literal `\` in the pre-application list, E-15, E-24b): accepted. A container refuses a literal backslash in a request target as an invalid request line; allowing the 400 as an alternative to 404 is correct, and the spec still gives the application answer (404) when the container passes it on. Security is the owner of the final confirmation.
- Pre-application statuses (405, 414, 431, 501, 505) without the optional constitution sentence (S-6): accepted. The spec wording is confined to container answers (Appendix A note, Appendix D introduction), so there is no conflict with the constitution's closed status list; the constitution sentence stays a hygiene suggestion for its next revision.

## 4. Hostile re-read fixes by the PM

- US-010.6 "Neither message is shown in the load-failure state": consistent with US-010.7 and US-010.4. No contradiction.
- T-25 pair of U+200D: required so that 6d's "the same kind twice included" has a case; I accept it as the completion of N-05 logic, not as a new behavior. Count of failing rows (five) matches the clauses.
- Literal backslash: see section 3.
- BR-006 examples: re-derived against 6c and 6d; every passing example passes and every failing example fails (including "x" + 8 U+0301 + U+200D + U+0301 = run of 10, "a" + U+FE0F + U+200D + U+FE0F = two selectors, "x" + 7 U+0301 + U+200D + "y" = run of 8).
- M-2 naming which figures are cut: correct and sufficient.

No new contradiction found.

## 5. Appendix C and `requirements.md`

Consistent: REQ-015, 016, 017, 029, 030, 037, 038, 041 to 047 now appear in Appendix C; T-14b and T-24b and E-24b are in the ranges; REQ-034 and REQ-047 are updated; header lists REQ-053, 054, 056, 057 as proposed (matches the 4 proposed in the totals; the totals sentence lists them in a different order, harmless); the cut paragraph of `requirements.md` equals Section 1. REQ-026 and DEC-029 agree with 6c.

## 6. The two items not added

- A test row for the `charset` white-space rule (SEC-F-064 d): not a defect. The grammar in 5.0 is exact (white space around `;` and after the value, not around `=`) and E-34 already tests the around-`;` case (`application/json ; charset=utf-8` is 201). The trailing-space-after-value case can be derived by the test author from one sentence. Change request during the build if a row is wanted.
- An add that arrives while a filter load is in flight: not a defect for the spec. US-010.10 (last request wins) and US-011.5 define the page after each response; the interleaving of one add response with one list response is a race on the timing of two responses and is not reproducible without a stub. Recommended build-time rule if a test is wanted: the displayed list is exactly the last list response plus adds that completed after that response arrived. Raise as a change request only if a test exposes it.

## 7. Optional wording observations (non-blocking; do not reopen the round)

- O-1 (T-27, T-30): the rows explain the escaped form in words but do not print the escape text; in T-30 the two first rows look identical on screen and T-27 shows the glyph. A test author can build the input from the sentence, so this is not a guess. If the PM edits anyway: T-30 row 2 `{"title":"b"...}` form, and T-27 `😀` printed as text.
- O-2 (Section 1 and `requirements.md` cut paragraph): the sentence "Only measurements are cut" is followed by step (2), which cuts US-014.3 (contrast) and US-014.5 (reduced motion), which are not measurements. This is the Sponsor's own decision 6 and the list is enumerated "in this order and in no other", so the enumeration controls and no implementer can misread it. If edited: replace "Only measurements are cut." with "Only the items above are cut, and no security item."
- O-3 (5.0 Accept): a `q` parameter without `=` (`application/json;q`) is not covered by "must have a value of the form". It is rejected by the same reading ("any other value makes the header unparseable"); add a row only if wanted.

## 8. Template acceptance criteria (last check)

| Criterion | Result |
|---|---|
| 7 sections | Present (Overview, User Stories, Business Rules, Data Model, API Contract, Out of Scope, Decision Log, plus Appendices A to D) |
| Stories with criteria | 13 active stories, 102 binary criteria; US-005 reserved |
| At least 3 Given-When-Then rules | 12 active rules |
| API examples | Present for list, add (success, validation, duplicate), status, rating |
| Decision log | 32 decisions, dated, with status and rationale |
| Zero ambiguity | Met at the level of this review; O-1 to O-3 are wording, not guesses |
| No constitution conflicts | Met: N-02 closed; pre-application statuses confined to container answers (R-13) |
| Traceability | Complete for FR, BRD rules, stories, rules, endpoints; Appendix C test column present; start-up criteria gap stated |

## 9. Sponsor-only decisions and gate package

No new decision is needed. For the gate package:
- S-4 (show the Sponsor): `409` is used for `DUPLICATE_BOOK` and `LIBRARY_FULL`; `RATING_NOT_ALLOWED` stays `400` (BRD, DEC-008, Appendix A). The constitution's "as `spec.md` defines" covers both; no amendment.
- Also show: the 2 s add bound is recorded as a security bound that is never cut (section 2), the pre-application statuses outside the constitution list are a stated container residual (R-13), and the optional constitution sentence (S-6) was not applied by Sponsor decision.
- The Phase 2 gate needs, besides this verdict: the Security verdict on handoff 015 section 7, and Sponsor approval. The `status.md` Liaison column and Spec coverage may be updated by the orchestrator.

## 10. Next step

Orchestrator: collect the Security verdict, then present the gate package to the Sponsor. After approval Phase 3 (plan and tasks) starts; the spec is frozen and later findings are change requests. Phase 3 items already recorded in handoff 015 section 5 (start-up fixtures, memory bound for C-11 (c), raw-target filter, fixture validation on the pinned JDK) stay as stated; add to them: a margin for the C-11 timing test, and the optional rows of section 6.
