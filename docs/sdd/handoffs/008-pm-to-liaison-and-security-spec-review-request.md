# Handoff 008: Review request for spec.md v0.1 and the requirements register
- From: pm
- To: liaison (and security reviewer, in parallel)
- Phase: 2   Round: 1 of 3
- Artifacts: `docs/sdd/spec.md` v0.1, `docs/sdd/requirements.md` v0.1, `docs/sdd/handoffs/007-pm-to-sponsor-discovery-and-decisions.md` (Discovery Brief, analysis, decisions, questions)
- Type: REVIEW_REQUEST

## Summary
First draft of the Phase 2 package. Sponsor decisions are requested separately (handoff 007, Q1 to Q5); the spec marks dependent text with the decision id (`[DEC-nnn pending]` or `proposed`). Please review for SDD doctrine, constitution compliance and testability (Liaison) and for abuse cases, limits and unsafe behavior (Security). Counts: 50 requirements, 14 stories (US-005 reserved), 13 rules (BR-005 reserved), 7 endpoints, 24 decisions (3 confirmed, 16 proposed, 5 pending).

## Content: what to check

### Liaison
1. Template conformity: the 7 sections are present, extended with Appendices A (error catalog), B (limits), C (traceability).
2. Constitution compliance: snake_case, timestamp format, UUID ids, `/api/v1/`, error envelope, reserved codes, status codes (no 204, no 422), Content-Type on body-less DELETE, no pagination with a bound, no technology leakage. In particular judge DEC-008: spec codes `INVALID_STATUS`, `INVALID_RATING`, `VALIDATION_ERROR` for value-domain errors, and `MALFORMED_REQUEST` for wrong JSON types (the constitution lists wrong type under `MALFORMED_REQUEST`).
3. Whether `RATING_NOT_ALLOWED` as 400 (BRD) rather than 409 is acceptable under "409 conflict (duplicate book, as spec.md defines)".
4. Testability: every acceptance criterion binary; every BR has a pass and a fail scenario; BR-012 order of evaluation unambiguous; US-013 and US-014 are verifiable by Playwright and axe. Flag the reference to P-04 in US-014.1 if it is an implementation detail.
5. Invariants INV-1 to INV-8 against every endpoint (can any sequence violate them?). Check the state matrix, BR-004, BR-008, same-state updates, create-as-completed.
6. Traceability: Appendix C and `requirements.md` section 2; gaps in a column are findings. FR4 has no downstream items by design (DEC-003).
7. Decision Log handling: PROPOSED and PENDING entries are marked as such; none is presented as confirmed. Check the reserved ids (US-005, BR-005, DEC-003) against the Phase 5 template.
8. Scope: gold plating (get by id, clear rating, GET list `total`, delete) and the open points of the agent definition section 7; anything missed.
9. Unambiguous HOW-free wording; consistency between stories, rules, contract and Appendix A (codes, statuses, `details`).
10. Whether the 3-round budget and the time-budget question (DEC-022) need escalation.

### Security (abuse cases and limits)
1. SC-16: text policy of BR-006 (trim with Unicode white space, NFC, code-point counting, rejected categories Cc, Cf except U+200D/U+FE0F, Zl, Zp, surrogates, private use, unassigned; Zs inside text allowed; tab/newline rejected). Is it complete? Are bidi controls (Cf) and variation selectors (U+FE0F kept, others?) handled as you intend? Are homoglyph and combining-mark flooding acceptable given the 500/200 limits?
2. SC-17 and R-02: maximum 5,000 books, `LIBRARY_FULL`, response bound derivation in Appendix B (worst case about 16 MB, typical under 2 MB). Is the bound acceptable or should it be lower (for example 2,000)? Is the unbounded `existing_book` payload fine (one book only)?
3. `details` shape and message content (DEC-017): no echo of input, no internal names; `field` values are API names only.
4. Strictness (SC-31, SC-08): unknown, duplicate, wrong-type members; `rating` literal rules (4.9, 5.0, 1e0, huge numbers, null, string); `allow_duplicate` typing; unknown and repeated query parameters; non-empty body on DELETE; client-supplied `id`, `date_added`, `rating` on POST rejected.
5. Identifier handling (SC-16, SC-40): malformed id is a 400 envelope, unknown id 404; uppercase UUIDs rejected; routes with trailing slash or suffix are 404.
6. Enumeration and error side channels: does `DUPLICATE_BOOK` returning the existing book leak anything beyond the user's own data (single user)? Is `BOOK_NOT_FOUND` vs `MALFORMED_REQUEST` distinction acceptable?
7. Concurrency and atomicity (R-06, BR-013): the concurrent duplicate scenario (US-004.8), last-write-wins, and the all-or-nothing rule.
8. Frontend abuse cases in US-014.6, US-013.5 and US-011/012: hostile text in every field and error path, no request to another origin, dialogs, message mapping by code (never server text), error table 5.9 for `INVALID_HOST` and `FORBIDDEN`.
9. Denial of service: 16 KiB body versus the largest valid request (about 9 KiB), double-submit protection, delete and re-add loops at capacity.
10. Durability statement (US-009.4): no promise on power loss; is that acceptable against data-integrity expectations?
11. Anything in the contract that creates an endpoint, method or member you want removed (get by id, `DELETE .../rating`).

## Open items / decisions needed
The Sponsor decisions Q1 to Q5 (handoff 007) are open; the review can proceed on the recommended text. Specific points where I want a ruling rather than a note: DEC-008 (code mapping), RATING_NOT_ALLOWED status, the 5,000 maximum, P-04 reference in US-014.1.

## Requested response
Liaison: REVIEW_REPORT with findings (BLOCKER / MAJOR / MINOR), questions and a verdict. Security: AUDIT_REPORT with findings and a verdict (PASS / PASS WITH CONDITIONS / FAIL). I will resolve every finding in round 2: fix, log a decision, or escalate.
