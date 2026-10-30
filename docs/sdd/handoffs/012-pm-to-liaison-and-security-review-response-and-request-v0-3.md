# Handoff 012: Resolution of reviews 010 and 011, and review request for spec.md v0.3
- From: pm
- To: liaison (and security reviewer, in parallel)
- Phase: 2   Round: 2 of 3
- Artifacts: `docs/sdd/spec.md` v0.3, `docs/sdd/requirements.md` v0.3; answers to `handoffs/010` (Liaison, REJECTED) and `handoffs/011` (Security, PASS WITH CONDITIONS)
- Type: REVIEW_REQUEST
- Supersedes: the request in handoff 009. Review v0.3, not v0.2.

## 1. Summary

Every finding is resolved in the spec (none declined). Scope was frozen: the changes are those the findings and the Sponsor decisions of 2026-10-30 require, plus the test rows and numbering that follow from them. Where the two reviews overlap or disagree I took one text and say so in section 4; five of those choices need a ruling (section 6).

Counts of v0.3: 14 story ids (13 active, US-005 reserved) with 102 acceptance criteria; 13 rule ids (12 active, BR-005 reserved); 7 endpoints plus the page; 32 decisions (DEC-001 to DEC-032); invariants INV-1 to INV-8; 57 requirements (Must 54, Should 1, Won't 1, Reserved 1); Appendix D has 104 rows (T-01 to T-40 with T-14b, C-01 to C-13, E-01 to E-39, S-01 to S-11). After editing I re-read the whole spec once as a hostile reviewer, with special attention to BR-006, BR-011, BR-012, Section 5.0 and Appendix D; the checks and what they changed are in section 5.

## 2. Sponsor decisions applied (2026-10-30)

| # | Decision | Applied in |
|---|---|---|
| 1 | U+200C and U+200D allowed under the positional rule; tag characters rejected, loss of subdivision flag emoji accepted; marks capped (8 in a row, one variation selector in a row, no leading mark) | BR-006 items 6, 6b, 6c, 6d; DEC-029; Section 6 accepted limitations; T-03, T-23 to T-26 |
| 2 | R-16 accepted and stated in the Overview | Section 1 (Target Users); DEC-031; Section 6 |
| 3 | R-17 accepted | Section 6; DEC-031 |
| 4 | Second instance not supported, second start fails | US-009.7; BR-013; Section 6; DEC-032; C-12 |
| 5 | SEC-F-056 limits accepted and recorded | Section 6 (confusables, lower-case not case folding, Unicode 15.0); BR-003 definition; DEC-001 amended; DEC-031 |
| 6 | Cut order: M-2 timing and visual checks, then US-014.3 and US-014.5, never invariant or security items | Section 1 "Delivery priorities"; DEC-030; `requirements.md` |
| 7 | `LIBRARY_FULL` 409, `RATING_NOT_ALLOWED` 400 | Appendix A, BR-007, BR-009 unchanged; F-21 note in section 3 |
| 8 | DEC-025 to DEC-028 confirmed as amended | Decision log (status CONFIRMED, amendments listed in the Decision column) |

New decisions: DEC-029 (text policy), DEC-030 (cut order; the promotion of REQ-009 and REQ-012 is PROPOSED, it is the Liaison's recommendation F-10 and not part of decision 6, so I ask the Sponsor to confirm it at the gate), DEC-031 (accepted risks and limits), DEC-032 (single instance). Amended: DEC-001, 004, 009, 011, 012, 021, 022, 025 to 028.

## 3. Finding-by-finding resolution

### Liaison (handoff 010)

| Finding | Resolution | Location in spec v0.3 |
|---|---|---|
| F-01 BLOCKER | One rule, stated once: paths matched as received; encoded variants of a fixed segment, encoded `/`, `\`, `;`, a literal `\` or `;` in the path, and dot segments (literal or encoded) are `404 NOT_FOUND`; the id is judged as received, never decoded, any `%` in it is `400 MALFORMED_REQUEST`; percent-encoding validity matters in the path only through these two rules; malformed percent-encoding or decoded invalid UTF-8 in the query is `MALFORMED_REQUEST`; the `status` value is decoded once, `+` is a space. Contradiction removed from BR-011 and T-14 | 5.0 Routing and Query strings; BR-011; BR-012 steps 2 to 4; US-002.6; US-007.2; T-14; E-24, E-25, E-26 |
| F-02 BLOCKER | `{id}` matches any single non-empty segment; route and method before the id; T-14 rewritten to the defined combinations; 405 row added | BR-012 step 2; BR-011 last sentence; T-14, T-14b; E-27 |
| F-03 MAJOR | Every Phase 5 hint removed. FR4 text is "reserved, not specified in this version"; US-005 body "reserved"; BR-005 "reserved"; Section 4, Section 6, Appendix A, US-010.1 negative clause, DEC-003, v0.2 changelog line, header status, Appendix C gap sentence, `requirements.md` REQ-004 and the FR4 summary row cleaned. Grep for "Phase 5", "drift", "ASK FIRST", "Scenario B", "BIO_TOO_LONG", "author concept", "Author entity" returns nothing in either file | Section 1; US-005; BR-005; Sections 4, 6; DEC-003; Appendix A, C; `requirements.md` REQ-004 |
| F-04 MAJOR | State model: P-4 applied (US-010.10 new; count formula in US-010.4; US-010.7 "zero list items"; US-011.5 and US-012.5 follow US-010.4; US-012.7 message to the page-level polite area) | US-010.4, .7, .10; US-011.5; US-012.5, .7; 5.9 |
| F-05 MAJOR | P-5 applied: list named "Books", heading "Your books" (level 2), date labels, status labels, filter as radio group "Filter by status", "Show all books", Status control, dialog names and texts, per-entry accessible names, focus target after delete, scoping by list item | US-010.1, .3, .5, .6; US-011.1, .7, .8; US-012.1, .3, .4, .5 |
| F-06 MAJOR | Conventions say API texts are informative and UI quoted texts are exact; Appendix A last paragraph reworded; 5.9 `INVALID_STATUS` and the `status` entry on the add form show "Choose Unread, Reading or Completed."; the unknown-field clause deleted (no reachable source); the two status paths are marked as covered by a stubbed-response page test | Header Conventions; Appendix A; US-011.6; 5.9 |
| F-07 MAJOR | P-7 applied: absent or zero-length file is created; valid database defined by observable facts (kind, Book structure, no other tables allowed, rows satisfy INV-1 to INV-4 and 5,000); every other case exits non-zero, no request served, reason without book data, bytes of the file unchanged (files the storage creates beside it excluded); the mechanism is left to the plan | US-009.3, US-009.5 |
| F-08 MAJOR | Grammar: Content-Type (case-insensitive type, only unquoted `charset=utf-8`, spaces around `;`, other parameter, list or repeated header is 415); Accept (empty or absent accepts all, ranges, q=0 ignored, unparseable 406); query (empty `?` is no query, empty pair, malformed encoding, unknown or repeated name); zero-byte, BOM, `null`, array, string, number bodies are `MALFORMED_REQUEST`; names compared after unescaping; static page: non-empty query and any body byte are 400 (my choice, see section 4) | 5.0; BR-012 steps 1(c), 2, 4, 5; E-34, E-35, E-38; T-29, T-30, T-39 |
| F-09 MAJOR | DEC-025 tests added: T-34 (bad status plus bad title), T-35 (bad status alone on POST), T-36 (invalid then valid rating on an unread book), T-37 (100 and 101 digit literals), T-38 (invalid status on unknown id), S-11 (same-state unread and reading); sentences added to BR-001 and BR-002; US-003.3 states the order. The capacity-edge row is covered by C-03 plus C-04 (now also asserts the next add is 409) | BR-001; BR-002; US-003.3; T-34 to T-38; S-11; C-04 |
| F-10 MAJOR | REQ-009 and REQ-012 promoted to Must (DEC-030); US-007 is Must; the cut list is published in Section 1 and in `requirements.md` (step 1: M-2 and US-010.9 timing, E-22, the 320-pixel clause of US-014.4 and the containment clause of US-014.6; step 2: US-014.3, US-014.5). Musts now number 54 of 57, so the list is the control, not the label | Section 1 Delivery priorities; DEC-030; US-007; `requirements.md` |
| F-11 MINOR | Key definition: trim again after collapsing; Unicode default lower-case mapping, not case folding; "Straße" and "STRASSE" are not duplicates; test T-40 | BR-003; DEC-001; US-004.5; T-40 |
| F-12 MINOR | Appendix B body row corrected (combined with SEC-F-053, see reconciliation R-7) | Appendix B |
| F-13 MINOR | INV-5 reworded as a transition invariant | Section 1 |
| F-14 MINOR | M-1 and US-011.2: type the title, Tab, type the author, Enter, keyboard only, no other control | M-1; US-011.2; DEC-020 |
| F-15 MINOR | Observables stated: `role="status"` or `aria-live="polite"`; text content equals the stored value; 1,000 entries present 1,000 ms after `load`; one live-region update per failure; data state for US-014.1; 3:1 focus indicator; language `en`; unbroken word of 500 code points | US-010.2, .8, .9; US-013.4; US-014.1, .2, .4, .6 |
| F-16 MINOR | 431 and the other container statuses are named once, in the D.3 header and in the Appendix A note, as the only statuses outside Appendix A and only for pre-application answers; "container" and "R-13" wording replaced by "pre-application answer" with one footnote to `security.md`; E-20 and E-21 reworded without the plan's hook | Appendix A note; Appendix D header; E-20, E-21 |
| F-17 MINOR | 5.8 lists the roles of the served paths (page at two paths, one stylesheet, one script, names left to the plan); `/favicon.ico` is 404 and excluded from the console criterion; `/index.html/` added to E-15; E-39 | 5.8; US-014.6; E-15, E-39 |
| F-18 MINOR | Appendix C has an "Appendix D cases" column; the gap sentence says the functional test specification is produced in Phase 3 from the acceptance criteria | Appendix C |
| F-19 MINOR | DEC-011 extended (no `message` in any success response; `PUT` status and rating return the book; deviation from the BRD example recorded); dialog body text "This book may already exist. Add anyway?" carried into US-011.7 | DEC-011; US-011.7 |
| F-20 MINOR | DEC-004 rationale reworded as the Liaison proposed | DEC-004 |
| F-21 MINOR | No spec change. Gate package point: `409` is used for `DUPLICATE_BOOK` and `LIBRARY_FULL`; `RATING_NOT_ALLOWED` stays `400` per the BRD; both are Appendix A entries. Sponsor decision 7 confirms them | Appendix A (unchanged) |

### Security (handoff 011)

| Finding | Resolution | Location in spec v0.3 |
|---|---|---|
| SEC-F-043 MEDIUM | Rule 6b applied as given | BR-006 6b; T-23; US-001.6 |
| SEC-F-044 MEDIUM | Rules 6c applied (8 marks, one variation selector, no leading mark; raising to 16 not needed); US-014.6 containment criterion with a bounding-box check (worst case: base characters each followed by 8 marks) | BR-006 6b, 6c; Appendix B; US-014.6; T-24 |
| SEC-F-045 LOW | Sponsor decision 1: joiners allowed under rule 6d | BR-006 6, 6d; DEC-029; T-25 |
| SEC-F-046 LOW | Closed by Security; no spec change. Plan note below (section 7) | none |
| SEC-F-047 MEDIUM | 5.0 parser rule applied (every parser, limit or decoder failure is `MALFORMED_REQUEST`, never `INTERNAL_ERROR`, within 300 ms; decoder never replaces; invalid constructs listed); T-22 to T-33 added (with corrections, see R-5 and R-6) | 5.0 Strict parsing; BR-012 step 5; T-22 to T-33 |
| SEC-F-048 LOW | (a) to (f) closed by the routing, query and content-type rules (see R-1 to R-3) | 5.0; BR-011; E-24 to E-27, E-34 |
| SEC-F-049 LOW | Methods, TRACE, lower-case names; `Allow` on every 405 with the exact lists; static responses without `ETag`, `Last-Modified`, `Accept-Ranges`; `Range`, conditional and `Upgrade` headers ignored; no 101, 206, 304, 416; `Expect: 100-continue` | 5.0 Methods, Request bodies; 5.8; E-28 to E-31 |
| SEC-F-050 MEDIUM | (a) R-16 stated in the Overview (Sponsor decision 2); (b) US-009.6 added as given | Section 1; US-009.6; DEC-031 |
| SEC-F-051 MEDIUM | S-08 to S-10, C-09, C-10, C-13 added; BR-013 gains the race sentence and the "other writer" sentence; INV-1 and INV-4 hold in every final state. DB-level backstops are plan items (section 7) | BR-013; S-08 to S-10; C-09, C-10, C-13 |
| SEC-F-052 MEDIUM | D header: raw-socket list, envelope check extended with the constitution's security headers (E-23), pre-application answers defined; E-10 split so that the one-header comma case is exactly `INVALID_HOST`; E-20 allows 400, 414 or 431; E-21 reworded; E-24 to E-37 added | Appendix D header; E-10, E-20, E-21, E-23; E-24 to E-37 |
| SEC-F-053 LOW | Appendix B body row corrected | Appendix B |
| SEC-F-054 LOW | M-2 qualified (300 ms for typical data; add at 5,000 books of maximum size within 2 s, never 500); C-11 added; C-05 records its time; 5,000 typical books on the page is a measured limit without threshold | M-2; Appendix B; US-010.9; C-05, C-11 |
| SEC-F-055 LOW | US-011.4 in-flight guard; US-014.8; containment in US-014.6; query on `/` is 400 (E-38) | US-011.4; US-014.6, .8; E-38 |
| SEC-F-056 INFO | Three limits recorded (Sponsor decision 5) | Section 6; DEC-031; BR-003 |

Security rulings of section 4 of handoff 011: 1 (joiners) applied; 2 (tag characters) applied with the limitation line; 3 (marks) applied at 8; 4 agreed, T-26 to T-28; 5 (16 MB) accepted with C-05 time recorded; 6 (HEAD and OPTIONS 405) with `Allow` and the DEC-027 note; 7 (sub-order) with the cross-site navigation consequence in Section 6; 8 (pre-application sets) applied; 9 (invalid body for unknown id) unchanged.

## 4. Reconciliations (places where the two reviews or texts overlap or conflict)

| # | Topic | Liaison text | Security text | What v0.3 does and why |
|---|---|---|---|---|
| R-1 | Encoded dot segment as an id (`/books/%2e%2e`) | P-1: an encoded `/`, `\` or dot segment is not routed (404) | E-24: `%2e%2e` in the id position is `400` (any `%` in the id) | 404. The constitution says traversal and encoded variants give 404, and a dot segment is a path-structure fault, not an id. The rule "any `%` in the id is 400" holds for every other id. `%33f2b8c1e-...` stays 400 as both agree |
| R-2 | Malformed percent-encoding in the path | P-1: in the path it is 404 | E-25: `/books/%zz` and `/books/%` are 400 | In the id position it is a `%` in the id: 400. Elsewhere the segment cannot equal a fixed segment: 404. The query rule (400) is common to both. Summarized in 5.0 Routing |
| R-3 | Query on the static page | F-08: ignore or reject, state one | Item 4: reject (400) | Reject: 400 for any non-empty query and any body byte on a static GET; one rule for the whole application. Empty `?` is no query |
| R-4 | Content-Type `charset` | Quoted or unquoted accepted | Quoted is 415 | Quoted is 415 (smaller surface; Security veto weight). Both agree on case-insensitive and spaces around `;` |
| R-5 | Accept and `q=0` | q-values ignored; `text/html, application/json;q=0` is 200 | `q=0` excludes; `*/*;q=0` is 406 | `q=0` excludes (ranges with q equal to 0 are ignored, the rest decide); unparseable is 406. Liaison's example row becomes 406 (E-35). I chose it because it is what the header means and it is exact |
| R-6 | Test numbering | New rows T-22 to T-26 and S-08 | New rows T-22 to T-33, S-08 to S-10 | Security's numbers kept; the Liaison's cases are T-34 to T-38 and S-11. The Liaison's grammar rows (P-8) are merged into E-26, E-34, E-35, T-29, T-30, T-39, E-38 |
| R-7 | Appendix B body row | "valid content needs at most about 13 KiB" | "text in decomposed form can exceed 16 KiB and gets 413" | Both are true and are stated: typical under 9 KiB; Hangul written as escapes about 12.7 KB; U+1F82 as four escapes 700 times is 16,800 bytes and is 413, a stated limit; padding to 16,384 is accepted |
| R-8 | Statuses outside the constitution list (F-16) | 431 only | 400, 414, 431, 501, 505 | The five statuses are listed once for pre-application answers only. Ruling requested (section 6) |
| R-9 | Parser limits and long strings | Within 16 KiB no parser limit may change a code | Every limit trip is `MALFORMED_REQUEST` | Both: a string up to the cap never trips a limit (T-11 stays `VALIDATION_ERROR`); depth, number length and other limits are `MALFORMED_REQUEST` |
| R-10 | "Plan names the hook", "container", "R-13" (F-16, SEC-F-052) | Reword to observable behavior | Hook not reachable in production, a test asserts 404 | E-21 states both without HOW: the way of provoking is defined in the plan; it does not exist in the shipped application and a request to it gets 404 |

Corrections to reviewer test text (the intent is kept):
- T-24: "e" plus 9 times U+0301 would be composed by NFC into one precomposed letter plus 8 marks and so would pass; the base is now "x", which has no precomposed form with U+0301.
- T-31: 8,000 nested objects `{"x":` take 48,000 bytes and do not fit in 16 KiB; the object case uses 2,000 levels (12,000 bytes); the array case (8,000 levels, 16,000 bytes) fits.
- T-31: the three number rows are `INVALID_RATING` (as Security wrote), the two nested rows `MALFORMED_REQUEST`.
- Confirmation texts "Added: <title>" and "Deleted: <title>" became "Book added: <title>" and "Book deleted: <title>", because P-5 makes "Added: YYYY-MM-DD" a date label and a text locator would match both.

## 5. Hostile re-read of v0.3: contradictions found and fixed in my own edits

1. BR-012 step 1(c) still said "charset other than UTF-8 counts as wrong"; now it points to the grammar of 5.0 (which also covers quoted values and repeated headers).
2. 5.0 first said any body on GET is `MALFORMED_REQUEST` but BR-012 listed only "GET or DELETE"; both now say "any body byte on GET or DELETE", and the static page's GET is stated in 5.0 and 5.8.
3. BR-011 said the route precedes the id, 5.0 said paths are matched as received, and E-24 had a `%2e%2e` row from Security; reconciled (R-1) and E-24 changed.
4. Section 1 (priorities) cut E-22 and US-014.6 containment; D header and US-014.6 now agree on which clause is cut; the inertness and console-CSP clauses of US-014.6 are not in the cut list.
5. DEC-009 and US-007 still said Should after the promotion; both point to DEC-030.
6. INV-6 said "every error response"; it now excepts pre-application answers, in line with Appendix A and D.
7. US-003.3 now states that an invalid rating on a non-completed book is `INVALID_RATING` (BR-002, T-36), removing the only place where BR-007 could be read the other way.
8. The old T-03 listed the ZWNJ as failing; moved to T-25 as the accepted row. BR-006 examples updated in the same way.
9. Appendix A `NOT_FOUND`, `MALFORMED_REQUEST` and `UNSUPPORTED_MEDIA_TYPE` rows widened to match 5.0 (encoded paths, query on static paths, grammar).
10. M-1 text and US-011.2 and DEC-020 now agree (DEC-020 no longer says "three actions").

I checked these pairs and found them determined: bad `Accept` plus unknown path (404); bad id plus bad query (400 MALFORMED either way, step 3 first); static page with query plus body (400, same code); POST `/` without content type (415) versus with it (405); HEAD on a static path (405, `Allow: GET`); empty body on PUT (400) versus on DELETE (accepted).

## 6. Points on which I ask for a ruling

1. R-8: statuses 414, 501 and 505 are outside the constitution's list (it names 400 or 431 in R-13). Confirm that listing them for pre-application answers is acceptable, or propose a constitution amendment (the PM cannot edit it).
2. R-4 and R-5 (quoted `charset` is 415; `q=0` excludes): confirm, or choose the Liaison's variants; both change only E-34 and E-35.
3. R-3 (query and body on the static page are 400): confirm, noting that a link to `/?utm=x` gets a JSON 400.
4. DEC-030: the promotion of REQ-009 and REQ-012 to Must is my application of Liaison F-10, which the Sponsor's decision 6 did not cover; the Sponsor should confirm it at the gate.
5. The cut list treats the containment clause of US-014.6 as a "visual check" (Sponsor decision 6). Security: confirm that this is acceptable given SEC-F-044, since the inertness, console and no-address-data clauses stay uncut.

## 7. Plan notes for Phase 3 (kept out of the spec as HOW)

- SEC-F-046: the plan names the trimming method (a code-point loop over the Unicode White_Space property, not the platform's strip helper), and must not use the platform whitespace helper for BR-006 (it differs for no-break space and U+001C to U+001F).
- SEC-F-051: write transactions use an immediate write lock; database CHECK constraints for status values, rating 1 to 5, rating only with `completed`, and `date_completed` iff `completed`; a capacity trigger as backstop; fail the second start on a held lock (US-009.7); `trusted_schema` off.
- Security ruling 7: set the swallow limit of rejected bodies low (suggested 64 KiB) so E-33 holds.
- The 15.0 choice follows the pinned JDK; the reasoning belongs to the plan. Validate the fixtures of T-03, T-23 to T-26 on the pinned JDK in Task 1.
- Phase 3 specifies the functional tests for criteria not in Appendix D (Appendix C note) and the page and E2E tests for US-010 to US-014.

## 8. What each reviewer must re-check

### Liaison
1. F-01 and F-02: read 5.0 Routing, BR-011, BR-012 steps 2 to 4 and T-14, T-14b, E-24 to E-27 together; confirm one answer per request.
2. F-03: confirm no Phase 5 hint remains anywhere in the spec or the register (also Overview, Section 4, Section 6, Appendix C).
3. F-04 to F-06: confirm the state model of US-010.4 and US-010.10, the wording of US-010 to US-013 and Section 5.9 are enough for an E2E author who uses only the spec.
4. F-07, F-08: confirm the predicate of US-009.5 and the grammar sentences are exact; rule on section 6 points 1 to 3.
5. F-09, F-10: confirm T-34 to T-38, S-11 and the cut list; rule on section 6 point 4.
6. Reconciliations R-1 to R-10 and the four corrections to reviewer text (section 4).
7. Constitution 1.0 compliance of the new text (Allow header, statuses, security headers in the envelope check).

### Security
1. SEC-F-043 to SEC-F-045: BR-006 items 6 to 6d against rulings 1 to 3; T-23 to T-26 (note the base "x" in T-24).
2. SEC-F-047: 5.0 Strict parsing and T-22 to T-33 (note the corrected nesting sizes in T-31).
3. SEC-F-048, SEC-F-049: reconciliations R-1 to R-5 (especially the 404 for `%2e%2e`, static-page query 400, quoted charset 415, `q=0`); E-24 to E-31, E-34, E-35, E-38.
4. SEC-F-050, SEC-F-051: US-009.6, US-009.7, BR-013, S-08 to S-10, C-09 to C-13.
5. SEC-F-052: the D header (raw-socket list, pre-application definition, envelope check with security headers), E-10, E-20, E-21, E-23, E-32 to E-37.
6. SEC-F-053 to SEC-F-056, and section 6 point 5. Set SC-16, SC-17, SC-29, SC-30 at spec level if satisfied.

## 9. Next step
Liaison and Security review v0.3 (round 2 of 3). If either returns findings, round 3 is the last before the Sponsor gate; the three-round budget is not exceeded by this response. The Phase 2 gate stays closed until Liaison APPROVED, Security PASS (or conditions closed) and Sponsor approval.
