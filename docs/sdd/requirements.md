# GloberBooks Requirements Register

- Owner: PM (`sdd-product-manager`)
- Last updated: 2026-10-30
- Status: Draft v0.4 (Phase 2, round 3 of 3; final diff check). The Sponsor confirmed all decisions on 2026-10-30 (including the promotion of REQ-009 and REQ-012 to Must, DEC-030 final); `approved` below means confirmed by the Sponsor or traceable to a Sponsor-approved source, with the spec text still subject to the Phase 2 gate. REQ-053, REQ-054, REQ-056 and REQ-057 are `proposed` (they close with the Phase 2 gate).
- Companion documents: `spec.md` v0.4 (stories, rules, contract), `handoffs/007-pm-to-sponsor-discovery-and-decisions.md` (Discovery Brief and decisions), `handoffs/015-pm-to-liaison-and-security-final-diff-check-request.md`.

IDs (`REQ-nnn`) are stable and never reused. Source = BRD section (`exercise-brief.md`), constitution section, `security.md` control, or a decision id. Type: F functional, BR business rule, NFR non-functional, C constraint, S security. Priority: MoSCoW, or `Reserved`. Status: `proposed` (PM draft), `approved` (Sponsor confirmed or approved source), `reserved` (confirmed as not specified now), `won't` (confirmed out of scope for v1).

## 1. Register

| ID | Source | Statement | Type | Priority | Status | Trace (spec) |
|---|---|---|---|---|---|---|
| REQ-001 | BRD FR1 | A user can add a book with title, author and optionally status (default `unread`). | F | Must | approved | US-001, BR-006 |
| REQ-002 | BRD FR2 | A user can change a book's status among `unread`, `reading`, `completed`. | F | Must | approved | US-006, BR-001 |
| REQ-003 | BRD FR3 | A user can give a completed book a rating of 1 to 5. | F | Must | approved | US-003, BR-002, BR-007 |
| REQ-004 | BRD FR4, DEC-003 | FR4 Author Biography: reserved, not specified in this version. | F | Reserved | reserved (DEC-003, Sponsor 2026-10-30) | US-005 (reserved), BR-005 (reserved) |
| REQ-005 | BRD scope, brief US-002 | The user sees all books, newest added first, showing title, author, status and rating (if any). | F | Must | approved | US-002, BR-010 |
| REQ-006 | Brief US-002 | The list can be filtered by status. | F | Must | approved | US-002, BR-010 |
| REQ-007 | Brief US-002 | An empty library (or an empty filter result) shows an explanatory empty state. | F | Must | approved | US-002, US-010 |
| REQ-008 | Brief US-003, DEC-002 | A rating can be changed after it was first assigned. | F | Must | approved | US-003 |
| REQ-009 | Brief US-003 ("rating can be null"), DEC-009 | A rating can be removed (set back to none) while the book stays completed. | F | Must (promoted by DEC-030; the page's "Clear rating" needs it) | approved (DEC-009); promotion confirmed (DEC-030, Sponsor 2026-10-30) | US-003.7, US-012.4 |
| REQ-010 | BRD data integrity, brief US-004 | Adding a book whose title and author match an existing book (case-insensitive, trimmed) is flagged. | F/BR | Must | approved | US-004, BR-003 |
| REQ-011 | Brief US-004 | The user may override the duplicate flag and add the book anyway. | F | Must | approved (DEC-005) | US-004, US-011 |
| REQ-012 | BRD "CRUD", DEC-009 | A user can retrieve one book by id. | F | Must (promoted by DEC-030; US-001.9, US-008.1 and the page use it) | approved (DEC-009); promotion confirmed (DEC-030, Sponsor 2026-10-30) | US-007 |
| REQ-013 | BRD "CRUD", DEC-009 | A user can permanently delete a book. | F | Must | approved (DEC-009) | US-008 |
| REQ-014 | BRD "CRUD", DEC-010 | A user can edit title and author of an existing book. Decided out of scope for v1; a mistaken entry is deleted and re-added. | F | Won't (v1) | won't (DEC-010, Sponsor 2026-10-30) | Section 6, US-012.9 |
| REQ-015 | BRD scope "frontend" | A web page lists books and filters them by status. | F | Must | approved (DEC-021) | US-010 |
| REQ-016 | BRD scope, brief US-004 | The web page adds a book and presents the duplicate confirmation dialog. | F | Must | approved (DEC-021) | US-011 |
| REQ-017 | BRD FR2, FR3, DEC-007 | The web page changes status, rates, clears a rating and deletes a book, with confirmations where data is lost (rated book leaving completed; delete). | F | Must | approved (DEC-021, DEC-007) | US-012 |
| REQ-018 | Constitution Frontend Standards, Error Handling | The web page has defined loading, empty and error states and handles every error code. | F | Must | approved (DEC-021) | US-013 |
| REQ-019 | Constitution Frontend Standards | The web page meets WCAG 2.1 AA and renders all text safely. | NFR/S | Must | approved | US-014 |
| REQ-020 | BRD Rating Integrity | A rating can exist only while the book's status is `completed`; otherwise assigning one is rejected with `RATING_NOT_ALLOWED`. | BR | Must | approved | BR-007, INV-1 |
| REQ-021 | BRD Validation Range | A rating is an integer from 1 to 5 inclusive; anything else is rejected with `INVALID_RATING`. | BR | Must | approved | BR-002, INV-2 |
| REQ-022 | BRD, brief BR-001 | Status is always one of three exact lower-case values; anything else is rejected with `INVALID_STATUS`. | BR | Must | approved | BR-001, INV-3 |
| REQ-023 | Invariant 1, DEC-007 | When a completed book changes to `unread` or `reading`, its rating and completion date are cleared in the same operation; the page asks for confirmation first when a rating is shown. | BR | Must | approved (DEC-007, Sponsor 2026-10-30) | BR-004, INV-1, INV-4, US-012.3 |
| REQ-024 | Brief data model, DEC-023 | `date_completed` is set when a book becomes completed (including on creation), is unchanged by same-state updates, and is null otherwise. | BR | Must | approved (DEC-023) | BR-008, INV-4 |
| REQ-025 | Brief US-001 | Title is 1 to 500 and author 1 to 200 characters, counted in Unicode code points after trimming. | BR | Must | approved (constitution, SC-16) | BR-006 |
| REQ-026 | SC-16, constitution Security, DEC-029 | Text fields are trimmed with Unicode white space, stored in NFC, and reject control, format (except the joiners U+200C and U+200D under a positional rule), tag, line and paragraph separator, private-use, noncharacter and unassigned code points, text that renders blank, leading marks and any run of marks and joiners with more than 8 code points or more than 1 variation selector; unpaired surrogates and invalid UTF-8 reject the request. | BR/S | Must | approved (DEC-029, Sponsor 2026-10-30) | BR-006, Appendix D.1 |
| REQ-027 | SC-17, R-02, DEC-013 | The library holds at most 5,000 books; adding beyond it is rejected with `LIBRARY_FULL`, also under concurrent adds. | BR | Must | approved (DEC-013, Sponsor 2026-10-30) | BR-009, BR-013, Appendix D.2 |
| REQ-028 | Brief US-002, DEC-014 | The list is ordered by `date_added` descending, ties broken by id ascending. | BR | Must | approved (DEC-014) | BR-010 |
| REQ-029 | Constitution API/Security, DEC-015 | A path id that is not a lowercase UUID gets `400 MALFORMED_REQUEST`; a well-formed unknown id gets `404 BOOK_NOT_FOUND`. | BR | Must | approved (DEC-015) | BR-011 |
| REQ-030 | DEC-024 | When several checks fail, the response reports the first failing check in a fixed order. | BR | Must | approved (DEC-024) | BR-012 |
| REQ-031 | R-06, DEC-018 | Each write is atomic; the duplicate check and the insert cannot interleave with another add. | BR | Must | approved (R-06, DEC-018) | BR-013 |
| REQ-032 | BRD invariant 5, constitution Error Handling | Every error uses the standard envelope and the codes of the spec's error catalog; messages never echo input. | NFR | Must | approved | Appendix A, INV-6 |
| REQ-033 | BRD success metric, DEC-019 | An acknowledged write survives a restart of the application, including a forced kill of the process. | NFR | Must | approved (DEC-019) | US-009 |
| REQ-034 | BRD metric "add in under 10 seconds" | A user can add a book with the keyboard only: type the title, Tab, type the author, Enter. | NFR | Must | approved (DEC-020) | US-011 |
| REQ-035 | BRD metric "view library instantly", DEC-020 | The list API and page respond within the stated bounds (list API at 5,000 typical books, page at 1,000 books). | NFR | Must | approved (DEC-020) | US-002, US-010 |
| REQ-036 | SC-17, DEC-013 | The list response is bounded: at most 5,000 books and a stated maximum size (below 16,000,000 bytes). | NFR/S | Must | approved (DEC-013) | BR-009, Appendix B |
| REQ-037 | Constitution API Standards, Persistence | Paths under `/api/v1/`, snake_case members, fixed UTC timestamp format with milliseconds, lowercase UUID ids, lower-case enum values. | C | Must | approved | Section 4, Section 5 |
| REQ-038 | BRD out of scope | No authentication, multi-user, cloud database, third-party metadata, covers, social features, mobile app or advanced search. | C | Must | approved | Section 6 |
| REQ-039 | SC-09, SC-38 | Every string from the API is rendered as text only; stored script payloads never execute. | S | Must | approved | US-014 |
| REQ-040 | SC-06, SC-08, SC-31 | Bodies over 16 KiB are rejected; unknown fields, duplicate keys, trailing content and wrong types are rejected. | S | Must | approved | Section 5.0, BR-012, Appendix D.3 |
| REQ-041 | Constitution Testing | Every business rule has at least one passing and one failing test case; expected values come from the spec. | C | Must | approved | Section 3 (each BR has both scenarios) |
| REQ-042 | BRD success criteria | Constitution, spec, plan and tasks stay synchronized with the code. | C | Must | approved | Section 7, Appendix C |
| REQ-043 | Constitution Frontend Standards | All UI strings are English. | C | Must | approved | US-010 to US-014 |
| REQ-044 | Constitution Frontend Standards | Current evergreen browsers are supported; automated checks run on Chromium only (R-03). | C | Must | approved | US-014 |
| REQ-045 | Constitution Tech Stack, DEC-004 | Data is persisted in a local file database; the BRD "local/in-memory" wording is resolved by the constitution. | C | Must | approved | Section 4 |
| REQ-046 | Constitution API Standards | The list is not paginated in v1; adding paging is a spec change. | C | Must | approved | Section 5.1, Section 6 |
| REQ-047 | BRD exercise time (6 to 8 h), DEC-022 | The delivery plan treats the time budget as indicative and cuts in the order stated in `spec.md` Section 1 (Delivery priorities); overruns are reported, not hidden. | C | Should | approved (DEC-022, Sponsor 2026-10-30) | tasks.md (Phase 3) |
| REQ-048 | BRD invariant 3 | A same-state status update is accepted and changes nothing (including `date_completed`). | BR | Must | approved (DEC-006) | US-006 |
| REQ-049 | Constitution Security (CSRF layer) | Every non-GET request, including body-less deletes, carries `Content-Type: application/json`. | C/S | Must | approved | Section 5.0 |
| REQ-050 | SC-17, SC-16 | Maximum response and request sizes are documented with their derivation. | NFR/S | Must | approved | Appendix B |
| REQ-051 | DEC-027, SC-31, SC-29, SC-40 | Wire-level behavior is exact: path matching and id judged as received, content type, Accept and query grammar, HEAD, OPTIONS and other methods with `Allow`, static response headers, body rules on every method, parser failures are `MALFORMED_REQUEST`, number literal length, order of the transport checks. | BR/S | Must | approved (DEC-027, Sponsor 2026-10-30, as amended) | Section 5.0, BR-011, BR-012, Appendix D.3 |
| REQ-052 | DEC-028 | An unopenable or invalid database stops the application at start and leaves the file untouched; "valid" is defined by observable facts. | NFR | Must | approved (DEC-028, Sponsor 2026-10-30, as amended) | US-009.3, US-009.5 |
| REQ-053 | SC-16, SC-17, SC-29, SC-30, SC-31 | The security test cases have exact expected statuses and codes in the spec, with raw-socket cases marked. | C/S | Must | proposed | Appendix D |
| REQ-054 | SC-13, SC-39, SEC-F-050 | On a POSIX system the application refuses to start when the data directory or database file is a symbolic link, is owned by another user or is group or world accessible, and creates them with owner-only modes. | NFR/S | Must | proposed (Security condition, handoff 011) | US-009.6 |
| REQ-055 | DEC-032, SC-32 | A second instance on the same database file is not supported; its start fails and the first instance keeps serving. | NFR | Must | approved (DEC-032, Sponsor 2026-10-30) | US-009.7, BR-013 |
| REQ-056 | SC-09, SC-38, SEC-F-055 | The page takes no data from the address, `window.name`, referrer or `postMessage` and stores nothing in web storage, cookies or IndexedDB; a submission in progress cannot be repeated by any means. | S | Must | proposed (Security condition, handoff 011) | US-014.8, US-011.4 |
| REQ-057 | DEC-021, handoff 010 F-04 to F-06 | The page has a defined state model (when the list loads, how the count is kept, the load-failure and empty states, overlapping loads, the page-level message area) and exact wording and structure (names, headings, labels, dialogs) on which tests rely. | F | Must | proposed (DEC-021 as amended) | US-010 to US-013, Section 5.9 |

Totals: 57 requirements: Must 54, Should 1 (REQ-047), Won't 1 (REQ-014), Reserved 1 (REQ-004). Status: 52 approved or confirmed (REQ-014 won't among them), 1 reserved (REQ-004), 4 proposed (REQ-053, REQ-056, REQ-057, REQ-054). The promotion of REQ-009 and REQ-012 to Must is confirmed (DEC-030, Sponsor 2026-10-30). No requirement waits for a Sponsor answer.

Cut order if time runs out (DEC-030, Sponsor 2026-10-30; defined in `spec.md` Section 1, which controls): (1) time thresholds and recorded timings, and visual layout measurements, namely the 1 s list figures and the 300 ms single-book figure of M-2 and DEC-020, US-010.9, the recorded time of C-05, the 320-pixel clause of US-014.4 and the bounding-box measurement of US-014.6; (2) US-014.3 and US-014.5 (the contrast rules inside the automated check of US-014.1 are not cut). Only measurements are cut. Never cut: invariants, business rules, every security criterion and every other Appendix D case, in particular test E-22, the containment rules of the stylesheet (US-014.6), the 2-second add bound of M-2 and C-11 with its assertions that no response is `500` and that a failed request changed nothing, the 300 ms bound on parser failures (Section 5.0, T-31) and the 2-second response of E-33. A cut item is reported as open, never dropped silently.

## 2. Traceability FR to downstream (summary; the full matrix is in `spec.md` Appendix C)

| BRD item | Requirements | Stories | Rules | Endpoints |
|---|---|---|---|---|
| FR1 Add Book | REQ-001, 010, 011, 025, 026, 027, 051 | US-001, US-004, US-011 | BR-003, BR-006, BR-009 | POST /api/v1/books |
| FR2 Track Status | REQ-002, 022, 023, 024, 048 | US-006, US-012 | BR-001, BR-004, BR-008 | PUT /api/v1/books/{id}/status |
| FR3 Rate Books | REQ-003, 008, 009, 020, 021 | US-003, US-012 | BR-002, BR-007 | PUT, DELETE /api/v1/books/{id}/rating |
| FR4 Author Biography | REQ-004 | US-005 (reserved) | BR-005 (reserved) | none (reserved, DEC-003) |
| Rating Integrity rule | REQ-020, 023 | US-003, US-006 | BR-007, BR-004 | rating and status endpoints |
| Validation Range rule | REQ-021 | US-003 | BR-002 | PUT rating |
| Data Integrity (duplicates) rule | REQ-010, 011, 031 | US-004 | BR-003, BR-013 | POST books |
| Explicit error handling rule | REQ-032, 051, 053 | all | BR-012 | all |

## 3. Changelog
- v0.4 (2026-10-30): Round 3 of Phase 2. Header statuses corrected (REQ-055 is approved; REQ-053, 054, 056 and 057 are proposed). REQ-009 and REQ-012 promotion confirmed (DEC-030 final). REQ-026 reworded for the run rule of BR-006 6c. REQ-034 reworded to the keyboard sequence of M-1. REQ-047 now refers to the cut order of `spec.md` Section 1. REQ-057 extended to the completed page state model. Cut order paragraph replaced (measurements only; E-22, stylesheet containment, the 2-second and 300 ms bounds are never cut). Totals unchanged (57).
- v0.3 (2026-10-30): Round 2 of Phase 2. REQ-004 reduced to "reserved, not specified in this version" with priority Reserved. REQ-009 and REQ-012 promoted to Must (DEC-030). REQ-026 reworded for the text policy (DEC-029). REQ-051 and REQ-052 approved (DEC-027, DEC-028 confirmed as amended) and extended. REQ-054 to REQ-057 added (data location trust, single instance, page abuse criteria, UI state model and wording). Totals recomputed (57).
- v0.2 (2026-10-30): Applied the Sponsor's decisions of 2026-10-30: statuses set to approved where confirmed; REQ-004 reserved (DEC-003); REQ-014 won't (DEC-010); REQ-023, REQ-027, REQ-036, REQ-047 approved. REQ-026 updated (surrogates and invalid UTF-8 reject the request; U+FE0F is not a Cf character). REQ-051 to REQ-053 added. Totals recomputed.
- v0.1 (2026-10-30): First draft, Phase 2 round 1.
