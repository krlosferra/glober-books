# Handoff 015: Final diff-check request (spec.md v0.4, requirements.md v0.4)
- From: pm
- To: liaison (copy to security)
- Phase: 2   Round: 3 of 3
- Artifacts: `docs/sdd/spec.md` v0.4, `docs/sdd/requirements.md` v0.4; answers handoffs 013 (Liaison) and 014 (Security)
- Type: REVIEW_REQUEST

This is a short diff check, not a new review. Scope was frozen: only Liaison P-1 to P-12, Security conditions 1 to 3, the small edits SEC-F-059, 060, 061, 064, and the recording of SEC-F-063 and SEC-F-065. No feature, rule or surface was added. The spec is frozen after this round; later findings become change requests during the build (`delivery-strategy.md`).

## 1. Counts

- Acceptance criteria: 102 in 13 active stories (unchanged; edits append sentences to existing criteria).
- Business rules: 12 active (BR-005 reserved). Decisions: 32 (DEC-027, 028, 029, 030, 031 and 021 amended; none added).
- Appendix D: 106 rows (T-01 to T-40 with T-14b and T-24b = 42; C-01 to C-13 = 13; E-01 to E-39 with E-24b = 40; S-01 to S-11 = 11). New rows: T-24b, E-24b.
- Requirements: 57 (Must 54, Should 1, Won't 1, Reserved 1); status 52 approved, 1 reserved, 4 proposed (REQ-053, 054, 056, 057).

## 2. Sponsor decisions applied (2026-10-30, "accept all recommendations")

| Decision | Applied at |
|---|---|
| E-22 removed from cut step 1; stays a required security test | Spec Section 1 (never-cut list), DEC-030; `requirements.md` cut paragraph |
| Cut list removes only measurements; stylesheet containment rules never cut (checked by reading the stylesheet) | Spec Section 1, US-014.6 (new last sentence), DEC-030 |
| REQ-009 and REQ-012 promoted to Must: confirmed, DEC-030 final | Spec header Status, DEC-030, DEC-009 unchanged; `requirements.md` REQ-009, REQ-012, totals |
| R-18 (about 250 bytes of hidden data per field) accepted and recorded | Spec Section 6 (hidden channels line), DEC-031 |
| R-19 not accepted as written; resolved by the cut-list change | DEC-030 (decision column), Section 1 |
| Usability limits of SEC-F-065 recorded | Spec Section 6 (new bullet) |
| `/?utm=x` gets a JSON 400: accepted | Spec Section 6 (new bullet), DEC-031 |
| Format characters other than U+200C and U+200D stay rejected (LRM, RLM, ALM, soft hyphen, word joiner): accepted limitation | Spec Section 6 (new bullet), DEC-031 |
| Optional constitution sentence on pre-application statuses: NOT applied | Spec wording is confined to container answers (Appendix A note, Appendix D introduction); no constitution change requested |
| Delivery strategy accepted; spec frozen after this round | Spec header Status, changelog v0.4 |

## 3. Finding-by-finding table

### 3.1 Liaison (handoff 013)

| Finding | Resolution | Spec location |
|---|---|---|
| N-01 (MAJOR) | P-1 applied. Dot segment defined (with Security's `%2e%2e%2e` clarification); US-007.2 and BR-011 carry the not-routable exception; E-24b added; E-24b added to the raw-socket list and to the Appendix C view row | 5.0 Routing; US-007.2; BR-011; Appendix D E-24b; Appendix D introduction; Appendix C |
| N-02 (MAJOR) | P-2 applied verbatim; E-38 reworded to "one of the page's asset paths" | 5.8 (first paragraph); E-38 |
| N-03 (MAJOR) | P-3 applied: count shown only with the list; empty messages keyed on the US-010.4 count; no insertion before a successful load or after a failed one; last request wins | US-010.4, .6, .7, .10; US-011.5 |
| N-04 (MAJOR) | P-4 applied as amended by the Sponsor decisions and the reconciliation in 4.3 below; also applied to `requirements.md` | Section 1 Delivery priorities; DEC-030; `requirements.md` cut paragraph |
| N-05 (MINOR) | P-5 applied: T-30 second row is the escape form of `title` (`title`); T-27 row 1 shows the escaped pair `😀` as 12 bytes (merged with SEC-F-060) | T-27, T-30 |
| N-06 (MINOR) | P-6 applied, merged with SEC-F-064 (c); E-35 gains `q=1.5`, `Q=0`, `text/html,,application/json` and Security's `application/json;q=0, */*` | 5.0 Accept; E-35 |
| N-07 (MINOR) | P-7 applied for 6d and the "adjacent" sentence; 6c superseded by the Security run definition (4.1 below); T-25 fixture note added | BR-006 6c, 6d, closing sentence; T-25 |
| N-08 (MINOR) | P-8 applied: Appendix A note, pre-application list (405, TRACE and method tokens), envelope check split into error and all-response parts, raw-socket list completed (union with SEC-F-059) | Appendix A note; Appendix D introduction |
| N-09 (MINOR) | P-9 applied: REQ-016, 015, 029, 017, 030 added to rows, T-14b in view row, constraints row, start-up gap sentence | Appendix C |
| N-10 (MINOR) | P-10 applied to `requirements.md` (header, REQ-034, REQ-047); the totals sentence follows the Sponsor confirmation instead (4.5 below) | `requirements.md` header, REQ-034, REQ-047, totals |
| N-11 (MINOR) | P-11 applied: Section 6 line for `/?utm_source=x`; US-009.3 permission sentence | Section 6; US-009.3 |
| N-12 (MINOR) | P-12 applied: Title field; focus exception in US-014.2; the page-level message area "Messages" defined; references in US-012.7 and 5.9 | US-011.8; US-014.2; US-013.4; US-012.7; 5.9 (BOOK_NOT_FOUND row) |

Verification of round 1 residuals (F-01 to F-21) needed no change beyond the above. Liaison sponsor items: S-1 confirmed (DEC-030), S-2 decided (E-22 uncut), S-3 recorded (Section 6), S-4 for the gate package (no spec change), S-5 recorded in Section 6, S-6 not applied (Sponsor decision).

### 3.2 Security (handoff 014)

| Finding | Resolution | Spec location |
|---|---|---|
| SEC-F-057 (MEDIUM, condition 1) | 6c replaced with the run definition (marks plus U+200C and U+200D, at most 8 code points, at most one variation selector); T-24b added with the six rows; Appendix B marks row, DEC-029 and `requirements.md` REQ-026 updated; BR-006 examples extended | BR-006 6c; T-24b; Appendix B; DEC-029 |
| SEC-F-058 (condition 2) | M-2, Appendix B last row and C-11 restated at 4,999 books with the three-part case (a), (b), (c) | M-2; Appendix B; C-11 |
| SEC-F-059 | Raw-socket list and pre-application list extended; accepted-pre-application sentence appended to E-15, E-24 and E-24b | Appendix D introduction; E-15; E-24; E-24b |
| SEC-F-060 | T-27 escape pair, T-30 second row (escaped name), S-09 initial state `reading` | T-27; T-30; S-09 |
| SEC-F-061 | US-009.5 (no views, triggers or virtual tables; formats and lengths), US-009.7 (claim held for the whole run); test list and plan notes in section 5 below | US-009.5; US-009.7 |
| SEC-F-062 (condition 3) | Cut list rewritten (see 4.3); US-014.6 sentence on the stylesheet; effect of step 1 stated | Section 1; US-014.6; `requirements.md` |
| SEC-F-063 | Recorded as R-18 (accepted by the Sponsor); optional rule 6e not applied | Section 6; DEC-031 |
| SEC-F-064 | (a) and (b) via P-1; (c) via P-6 plus the `*/*` deviation sentence; (d) `charset` white space | 5.0 Routing, Accept, Content type; BR-011; US-007.2 |
| SEC-F-065 | Recorded as accepted limits, including U+1D159 | Section 6 |

## 4. Reconciliations (overlap between the two reviews, stated)

1. **BR-006 6c.** Liaison P-7 (runs of Mn, Mc, Me; two adjacent variation selectors) and Security 4.1 (runs that include the joiners; at most one variation selector per run) overlap. Security's definition is stricter and closes the bypass of SEC-F-057, and it contains the Liaison point that a variation selector counts toward the 8 (it is category Mn). Applied: Security's 6c, plus the Liaison sentence that "consecutive" and "next to" mean adjacent code points. 6d is the Liaison wording (either kind of joiner, the same kind twice included), which Security's ruling 1 accepts.
2. **Dot segment (N-01 and SEC-F-064 b).** One definition: equal to `.` or `..` once `%2e` or `%2E` is decoded, escape digits case-insensitive; `%2e%2e%2e` is not a dot segment and is 400 in the id position.
3. **Cut list (N-04 against SEC-F-062).** The Liaison cuts the 2-second figure of C-11 as a timing item; Security says the 2-second bound of C-11 is not cut. Applied: Security's position, because the Security reviewer holds the veto and the bound protects the costly add path (SEC-F-054, SEC-F-058). Cut: the 1 s list figures, the 300 ms single-book figure, US-010.9, the recorded time of C-05, the 320-pixel clause, the bounding-box measurement. Not cut: E-22, stylesheet containment, the 2-second bound of M-2 and C-11, the C-11 assertions (no `500`, a failed request changed nothing), the 300 ms parser bound, the 2-second response of E-33. This is the one place where the Sponsor decision ("the cut list removes only measurements") needs the reading that the 2-second add bound is a security bound, not a measurement; flagged in section 7.
4. **Raw-socket list.** Union of P-8 and SEC-F-059: T-33, T-39, E-15 (rows with `..`, `%` or `;`), E-18, E-24, E-24b, E-25, E-26, E-34 (two `Content-Type` headers), E-38 (body on GET), plus the general sentence for any other row with a dot segment, an invalid escape or a character not allowed in a URL.
5. **Pre-application answers.** Liaison P-8 (400, 405, 414, 431, 501, 505; TRACE and unknown or lower-case method tokens) and Security 4.3 (encoded `/` or `\`, dot segment leaving the root) are combined in the Appendix D introduction. The accepted-pre-application sentence is also attached to E-24b (same container behavior as E-24), and I added "a literal `\`" to the list and to E-15, E-24b because a container refuses it as an invalid request line. This is the only wording I added to either reviewer's text; strike it if Security objects.
6. **Accept grammar.** P-6 and SEC-F-064 (c) agree on the `q` form. Merged: empty elements ignored, unparseable ranges, `Q` name case-insensitive, `application/json;q=0, */*` acceptable (stated deviation from RFC 9110 specificity).
7. **T-27 and T-30.** Same defect named by both reviewers; one fix each (the escaped form written out).
8. **Cut-list "totals" sentence (P-10).** P-10 said no requirement waits for a Sponsor answer "except the promotion of REQ-009 and REQ-012". The Sponsor has since confirmed it, so the original sentence holds again and the register says the promotion is confirmed.
9. **Pre-application reference.** The older texts pointed to "D.3 header"; the paragraph is the Appendix D introduction. Corrected in the Conventions line and in the Appendix A note.

## 5. Items recorded for Phase 3 (not spec text)

- Phase 3 test list for start-up (SEC-F-061 e, N-09 c): symlinked directory, symlinked file, file mode 0644, directory mode 0770, foreign owner (skipped where a test cannot change ownership, stated), missing path creates 0700/0600, each failure leaves an existing file's bytes unchanged (hash before and after), a view or trigger present, a row with a 600-character title; test databases are created with mode 0600 in a 0700 directory.
- Plan notes: open without following links and check the opened descriptor, not the path; the claim of US-009.7 is `locking_mode=EXCLUSIVE` or an equivalent file lock; immediate write lock, CHECK constraints, capacity trigger as already recorded; bound the heap or stream the list so that 50 concurrent responses of up to 15.5 MB do not exhaust memory (C-11 c); if the plan cannot meet C-11 (c), escalate to the Sponsor, do not weaken the test and do not introduce 503.
- The filter judges the raw request target: `//`, `.`, `..` and `;parameters` are normalized or stripped by the container before mapping, so E-01 (`;x=1`), E-15 (`/index.html;jsessionid=1`), E-24, E-24b and E-27 are tests of that choice.
- Fixtures (marks, joiners, T-24b, T-25) are validated on the pinned JDK in Task 1; `1e999999999` must never be converted to an integer.

## 6. Hostile re-read by the PM (contradictions introduced by my own edits, fixed)

- US-010.6 and the new US-010.4 sentence: the empty messages could have appeared in the load-failure state; added "Neither message is shown in the load-failure state (US-010.7)" (clarification of P-3, no new behavior).
- T-25: rule 6d now names the same kind twice (U+200D next to U+200D); a rule clause without a case would repeat N-05, so one input was added to T-25 (the expected count of failing rows is now five). Strike it if you consider it a new case.
- BR-006 examples extended with the joiner-split run and the same-kind joiner pair so that the failing and passing examples match 6c and 6d.
- Section 1 cut list against M-2: M-2 contains both a cut figure and an uncut one; the text now names which figures are cut.
- BR-006, BR-011, BR-012, 5.0, 5.8, Appendix A to D and `requirements.md` were re-read together; no remaining conflict found. Not changed, deliberately: no new test row for the `charset` white-space rule (SEC-F-064 d) and none for an add while a filter load is in flight; both can be derived at build time or raised as a change request.

## 7. Locations each reviewer must re-check (diff check only)

### Liaison

1. Section 1 Delivery priorities, DEC-030, `requirements.md` cut paragraph (N-04): confirm the reconciliation 3 (2-second bound uncut).
2. 5.0 Routing, US-007.2, BR-011, E-24, E-24b (N-01).
3. 5.8 first paragraph and E-38 (N-02).
4. US-010.4, .6, .7, .10, US-011.5 (N-03) and the added sentence in US-010.6.
5. T-27, T-30 (N-05); 5.0 Accept and E-35 (N-06); BR-006 6c, 6d, closing sentence, examples, T-24b, T-25 (N-07).
6. Appendix A note, Appendix D introduction (N-08); Appendix C rows and the gap sentence (N-09); `requirements.md` header, REQ-034, REQ-047, totals (N-10).
7. Section 6, US-009.3 (N-11); US-011.8, US-014.2, US-013.4, US-012.7, 5.9 (N-12).
8. Counts in section 1 of this handoff.

### Security

1. BR-006 6c with 6d, T-24b, Appendix B marks row, DEC-029 (SEC-F-057).
2. M-2, Appendix B last row, C-11 (SEC-F-058).
3. Section 1 cut list, US-014.6 last sentence, `requirements.md` cut paragraph, DEC-030 (SEC-F-062; R-19).
4. Appendix D introduction (raw-socket list, pre-application list, envelope check), E-15, E-24, E-24b, including my addition of a literal `\` (SEC-F-059; Liaison P-8 c asked for your confirmation).
5. T-27, T-30, S-09 (SEC-F-060); US-009.3, US-009.5, US-009.7 (SEC-F-061).
6. 5.0 Routing, Accept, Content type; BR-011; US-007.2; E-35 (SEC-F-064).
7. Section 6 and DEC-031 (R-18, SEC-F-065, the Cf limitation, the `/?utm_source=x` line).

## 8. Points still needing a ruling

- Reconciliation 3: whether the 2-second add bound counts as a security bound (kept uncut here) or as a measurement (the Liaison's reading). Security and Liaison should both state which; the Sponsor decides if they differ.
- Reconciliation 5: the added "literal `\`" wording.
- Liaison S-4 (gate package): show the Sponsor that `409` is used for `DUPLICATE_BOOK` and `LIBRARY_FULL` and that `RATING_NOT_ALLOWED` stays `400`.
- The `status.md` Security column and Spec coverage are for the orchestrator to update after the verdicts.
