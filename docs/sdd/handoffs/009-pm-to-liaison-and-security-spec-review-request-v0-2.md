# Handoff 009: Review request for spec.md v0.2 and the requirements register v0.2
- From: pm
- To: liaison (and security reviewer, in parallel)
- Phase: 2   Round: 1 of 3 (after the Sponsor's decisions)
- Artifacts: `docs/sdd/spec.md` v0.2, `docs/sdd/requirements.md` v0.2, `docs/sdd/handoffs/007-pm-to-sponsor-discovery-and-decisions.md` (Discovery Brief; its Section 6 holds the Phase 5 pre-design for FR4, which is deliberately not in the spec)
- Type: REVIEW_REQUEST
- Supersedes: the request in handoff 008 (which stays unchanged as the record). Review this version, not v0.1.

## Summary
The Sponsor answered handoff 007 on 2026-10-30 with "accept all recommendations". Applied: every proposed decision is CONFIRMED (DEC-001 to DEC-024, each marked "Sponsor 2026-10-30"); FR4 Author Bio is RESERVED (DEC-003: US-005 and BR-005 stay reserved, no author concept anywhere in the spec, an explicit out-of-scope-for-now note, Phase 5 pre-design stays in handoff 007 only); title and author are not editable (DEC-010, plus US-012.9); 5,000 books with `LIBRARY_FULL` (DEC-013); the page confirms before a rated completed book leaves `completed`, while the API clears rating and completion date on its own (DEC-007, US-012.3); the 6 to 8 hour budget is indicative and MoSCoW is the cut line (DEC-022). All PENDING and PROPOSED markers were removed from the text. Then I re-read the whole spec as a hostile reviewer and tightened it (list below).

Counts: 53 requirements (Must 48, Should 4, Won't 1); 14 story ids (13 active, US-005 reserved); 13 rule ids (12 active, BR-005 reserved); 7 API endpoints plus the page at `/`; 28 decisions (24 confirmed or reserved by the Sponsor, 4 new PM proposals DEC-025 to DEC-028 for confirmation at the gate); invariants INV-1 to INV-8; Appendices A (errors), B (limits), C (traceability), D (security test cases with exact expected results).

## Changes beyond applying the Sponsor's answers
1. Exact wire behavior (DEC-027, BR-012, Section 5.0): sub-order of the transport checks (Host, Origin/Sec-Fetch-Site, content type, body size); route, method and `Accept` order; query shape before query value; list of body faults that are all `MALFORMED_REQUEST`; unpaired surrogate and invalid UTF-8 are `MALFORMED_REQUEST` (not `VALIDATION_ERROR`); `charset` other than UTF-8 is 415; body on GET or DELETE and a query on endpoints that take none are `MALFORMED_REQUEST`; a number literal over 100 characters is `MALFORMED_REQUEST` while a long string (up to 16 KiB) is `VALIDATION_ERROR` by length; HEAD and OPTIONS are never defined (405 after the transport checks); no redirects, no cookies, no `Access-Control-*`.
2. Text policy precision (BR-006): trimming uses the Unicode White_Space property; U+FE0F is category Mn, not Cf, so it is allowed as a combining mark (the v0.1 wording listed it as a Cf exception, which was inaccurate); unassigned means unassigned in Unicode 15.0 (the version of the pinned JDK; tests need a fixed version); order of detail messages per field; examples now written as escapes.
3. Capacity under concurrency (BR-009, BR-013, C-06): count check and insert are one step. `LIBRARY_FULL` is evaluated before `DUPLICATE_BOOK` (US-004.9).
4. Database failure at start (US-009.5, DEC-028): refuse to start, leave the file untouched. This is a gap that the v0.1 draft left to the implementer.
5. Static content (5.8, E-15 to E-17): which paths exist, everything else 404 in the envelope.
6. UI wording made testable: exact dialog text and accessible names (US-012.1, .3, .4), the "No longer matches this filter" marker (US-012.8), count text (US-010.4), the rating control present exactly on completed entries (US-012.2), client checks limited to emptiness and length (US-011.3).
7. `details` shape fixed per code (Appendix A): at most 3 entries for `VALIDATION_ERROR`, in the order title, author, status; exactly one for `INVALID_STATUS` and `INVALID_RATING`; the fixed message list; `{}` body on POST gives two entries (US-001.10).
8. Response bound recomputed (Appendix B): below 16,000,000 bytes, valid only if text is written as UTF-8 and not as escape sequences (stated as a contract rule in 5.0); M-2 timing is for typical data.
9. New Appendix D with exact expected status and code for SC-16 (T-01 to T-21), SC-17 (C-01 to C-08), SC-29 and SC-30 (E-01 to E-23), and invariant sequences (S-01 to S-07). New requirements REQ-051 to REQ-053.

## Points settled as proposals (rationale; ruling requested)
- DEC-025 (DEC-008 refinement): `INVALID_STATUS` and `INVALID_RATING` are used when a single status or rating value is of the right JSON type but not allowed; `VALIDATION_ERROR` for title and author and whenever two or more fields fail; `MALFORMED_REQUEST` for structure and JSON types; `RATING_NOT_ALLOWED` stays 400 as in the BRD. Rationale: the constitution says a spec code with the same meaning takes precedence over the reserved code, the BRD names the rating codes, and the 400 class is kept (no 422, no 409 for a rating rule). Cost: one extra rule in BR-012 step 6 (a lone bad `status` gives `INVALID_STATUS`, a bad `status` together with a bad `title` gives `VALIDATION_ERROR`).
- DEC-026 (US-014.1 and tools): the criterion names the WCAG 2.1 A and AA rules and the impact levels, not the tool; the tool (axe) stays in the constitution and `frontend-testing.md`. Rationale: a tool name is a HOW detail.

## What each reviewer must check

### Liaison
1. Template conformity and traceability: seven sections plus Appendices A to D; Appendix C and `requirements.md` section 2 have no gap except the reserved FR4 row (by design).
2. Reserved ids: US-005, BR-005, DEC-003 are consistent with the Phase 5 template; confirm the spec contains no Phase 5 behavior or hint (Out of Scope note, Overview, Section 4 only say "reserved").
3. Constitution 1.0 compliance: error envelope and `details` shape; reserved and spec codes; status codes (no 204, no 422); timestamp format; snake_case; UUID ids; code-point counting after trimming; Content-Type rules including body-less DELETE; no CORS; strict JSON; no technology in the spec (check "UTF-8", "Unicode 15.0", "100 characters" for HOW leakage versus necessary contract).
4. Rulings on DEC-025 to DEC-028 (confirm, change or reject each before the Sponsor gate).
5. Binary testability of every acceptance criterion, especially US-010 to US-014 and the E2E verifiability of the UI wording; BR-012 unambiguity (try to find a request with two faults whose result is not determined).
6. Invariants INV-1 to INV-8 against all endpoints and the sequences of D.4; state matrix; same-state updates; create as completed.
7. Whether the three-round budget and DEC-022 need escalation, and whether any open point of the agent definition section 7 is still uncovered.

### Security
1. SC-16: BR-006 completeness. Please rule on: zero-width non-joiner U+200C is rejected (category Cf) although legitimate in some scripts (for example Persian); tag characters (U+E0020 to U+E007F, Cf) are rejected, which breaks flag-subdivision emoji; combining marks are allowed up to the 500 and 200 limits (no cap on marks per base character); unpaired surrogate and invalid UTF-8 give `MALFORMED_REQUEST` (T-04, T-05).
2. SC-17 and R-02: 5,000 books and a response below 16,000,000 bytes; `existing_book` is one book; error `details` at most 3 entries; check Appendix B derivation and that the 16 MB response is acceptable for a single local user.
3. SC-29: Appendix D.3 E-01 to E-23, including the OPTIONS and HEAD behavior, the sub-order of the transport checks (Host before Origin before content type before size), 404 for unknown static paths, container-level cases under R-13 (E-10, E-20), and the leak-free `INTERNAL_ERROR` case (E-21; the plan must name a test hook). Anything missing for a real-port test (SC-30)?
4. Strictness (SC-31, SC-08): T-16 to T-21; number literal rule (101 characters); `allow_duplicate` typing including `null`; member names case-sensitive; non-object bodies.
5. Identifiers and routing (SC-40): T-14, T-15, E-01, E-15.
6. Side channels: `DUPLICATE_BOOK` returns the existing book (single user, acceptable?); `BOOK_NOT_FOUND` versus `MALFORMED_REQUEST` distinction; evaluation order puts field validation before existence (an invalid body for an unknown id reports the body error).
7. Concurrency and atomicity (BR-013, C-06 to C-08); last write wins.
8. Frontend abuse cases (US-014.6, US-013.5, 5.9): messages chosen by code, never by server text; payload display; no cross-origin request.
9. DoS: 16 KiB body versus the largest valid body (under 9 KiB), double-submit protection, add and delete loops at capacity; durability statement (US-009.4) and the fail-closed database start (US-009.5).
10. Anything in the contract you want removed (get by id, `DELETE .../rating`).

## Requested response
Liaison: REVIEW_REPORT with findings (BLOCKER / MAJOR / MINOR), questions and a verdict. Security: AUDIT_REPORT with findings and a verdict (PASS / PASS WITH CONDITIONS / FAIL). I will resolve every finding in round 2: fix, log a decision, or escalate. No Sponsor question is open at this moment; DEC-025 to DEC-028 go to the Sponsor with the gate package.
