# Handoff 007: Discovery Brief, analysis of open points and decisions for the Sponsor
- From: pm
- To: sponsor
- Phase: 2   Round: 1 of 3
- Artifacts: `docs/sdd/spec.md` v0.1, `docs/sdd/requirements.md` v0.1, `docs/sdd/constitution.md` v1.0, `docs/sdd/security.md` v0.10
- Type: DECISION_REQUEST

## Summary
The Phase 2 draft is ready for review. It contains 50 requirements, 14 user stories (13 active, US-005 reserved for FR4), 13 business rules (12 active, BR-005 reserved), 7 API endpoints plus the static page, 24 logged decisions (3 confirmed by the brief or constitution, 16 proposed, 5 pending), and 5 Sponsor questions. Everything I could derive from the brief, the constitution or good practice I decided and logged as PROPOSED, so you can confirm in one pass (Section 4). Five questions need your judgment (Section 5). The Liaison and the Security reviewer review the same draft in parallel (handoff 008). Nothing is final until both verdicts and your approval.

## 1. Discovery Brief

### 1.1 Problem statement
Readers who keep a personal reading list in notes, memory or spreadsheets lose track of what they own, what they are reading and what they enjoyed, and add the same book twice. GloberBooks gives one reader one local, fast, reliable list with three statuses, a 1 to 5 rating for finished books and a warning for duplicates. It is also the pilot of an AI-assisted, Human-in-the-Loop SDD workflow: the artifacts must stay synchronized with the code and survive a drift exercise.

### 1.2 Personas and jobs-to-be-done
| Persona | Description | Jobs to be done |
|---|---|---|
| Rita, the casual reader (primary) | Dozens of books, uses a laptop browser, wants no setup or account | When I hear about a book, I want to note it in seconds. When I start or finish a book, I want to update it in one click. When I finish, I want to record how good it was. When I add something I already have, I want to be told. |
| Sam, the avid collector (secondary) | Hundreds to thousands of books, many editions and rereads | I want to filter by status and see my newest additions first. I want to knowingly add a second edition. I want to remove mistakes. I want everything to be there tomorrow. |
| Sponsor and reviewers (stakeholders, not users) | Run the SDD pilot | I want a spec that implementers can build from without guessing, and artifacts that match the code at the end. |
| Implementing agents (consumers of the spec) | Backend and frontend developers | I want exact paths, payloads, codes, limits and states so I never invent behavior. |

### 1.3 Goals and measurable success metrics
| Goal | Metric | Target | Verified by |
|---|---|---|---|
| Fast capture | Time to add a book | under 10 s; three keyboard actions (title, author, Enter) | E2E test (US-011.2) |
| Instant library | List visible | 1,000 books in under 1 s; list API at 5,000 books under 1 s; single operation under 300 ms | One recorded measurement (DEC-020) |
| No data loss | Acknowledged writes surviving a forced restart | 100% | Integration test (US-009) |
| Rating integrity | Ratings accepted on non-completed books | 0; 100% rejection | Tests for BR-007, INV-1 |
| Safe from duplicates | Same title+author added without a warning | 0 (including two simultaneous requests) | Tests BR-003, BR-013 |
| Governance | Acceptance criteria with an automated test; artifacts equal to code | 100%; yes at Phase 6 | Traceability matrix, audits |
| Quality | Coverage; accessibility | at least 80%; zero serious/critical axe violations | `verify`, E2E |

### 1.4 Scope with MoSCoW
| Capability | Priority | Notes |
|---|---|---|
| Add book (FR1), list newest first, filter by status | Must | |
| Track status (FR2) | Must | |
| Rate completed book, change rating (FR3) | Must | |
| Duplicate warning with override | Must | |
| Delete book | Must | BRD "CRUD"; not in the brief template |
| Frontend page: list, add, filter, status, rate, delete, duplicate dialog, empty/loading/error states | Must | |
| Accessibility AA, safe rendering, durability, limits, error envelope | Must | Constitution and security |
| Get one book by id | Should | Cheap, used by tests and row refresh |
| Clear a rating | Should | |
| Author bio (FR4) | Should, deferred | Phase 5 via change control unless you decide otherwise (Q1) |
| Edit title/author | Won't (v1) | Q2 |
| Pagination, search, sorting options, import/export, undo, authentication, cloud, metadata services, covers, social, mobile app | Won't | BRD out of scope or no demand |

### 1.5 Constraints from the constitution
Java 21 / Spring Boot / SQLite file / plain HTML-CSS-JS; `/api/v1/` prefix; snake_case JSON; ids UUID v4 lowercase; timestamps `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`; error envelope with optional `details`, reserved codes (`VALIDATION_ERROR`, `MALFORMED_REQUEST`, `INVALID_HOST`, `FORBIDDEN`, `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `NOT_ACCEPTABLE`, `PAYLOAD_TOO_LARGE`, `UNSUPPORTED_MEDIA_TYPE`, `INTERNAL_ERROR`); status codes limited to 200, 201, 400, 403, 404, 405, 406, 409, 413, 415, 500 (so no 204, no 422); lengths in code points after trimming; body cap 16 KiB; Content-Type JSON on every non-GET including body-less; strict JSON (no coercion, no unknown or duplicate members); no pagination but a stated maximum and bound; one new entity or schema change is ASK FIRST; no feature outside the spec; UI strings in English, WCAG 2.1 AA, native dialog, `textContent`, no local storage of data in the browser; loopback only.

### 1.6 Assumptions (each to validate)
| ID | Assumption | Validate with |
|---|---|---|
| A-1 | One user, one browser tab at a time is the normal case | Sponsor (Q-none: stated in BRD) |
| A-2 | 5,000 books is more than any reader needs | Sponsor, Q3 |
| A-3 | Title + author identifies "the same book"; editions are a user choice | Sponsor confirmation of DEC-001, DEC-005 |
| A-4 | Titles up to 500 and authors up to 200 characters cover real data | Brief (given) |
| A-5 | English-only UI | Brief/constitution |
| A-6 | Power-failure durability is not required, process-kill durability is | DEC-019 |
| A-7 | Rating loss when leaving completed is acceptable with a confirmation | Q4 |
| A-8 | The 6 to 8 hour budget is indicative | Q5 |
| A-9 | Local time zone display of dates is acceptable (storage is UTC) | DEC-021 |

### 1.7 Risks and dependencies
| ID | Risk / dependency | Mitigation |
|---|---|---|
| RK-1 | Time budget (9.5 hours of exercise phases against 6 to 8; extra security and test work) | Q5; MoSCoW cut line; task estimates in Phase 3 |
| RK-2 | Spec complexity from strict constitutional validation (type vs value codes) | Single order of evaluation (BR-012), tables, examples |
| RK-3 | FR4 timing undermines or supports the drift exercise | Q1 |
| RK-4 | Worst-case list response about 16 MB | Capacity 5,000 and Security review (SC-17); typical under 2 MB |
| RK-5 | Duplicate race | BR-013, R-06 test |
| RK-6 | Accidental data loss (delete, regress) | Confirmation dialogs; Q4 |
| RK-7 | Behavior unverifiable on non-Chromium browsers | R-03 accepted |
| DP-1 | Constitution v1.0 approved | done |
| DP-2 | Plan and tasks (Phase 3) depend on this spec and on the answers below | gate |

### 1.8 Glossary
Book: one entry of the library. Library: all books. Status: `unread`, `reading` or `completed`. Rating: whole number 1 to 5, only for completed books. Duplicate key: normalized title and author used to detect duplicates (BR-003). Override: the user's confirmation to add a duplicate. Envelope: the standard error body. Code point: a Unicode character as counted by the limits. Invariant: a rule that holds after every request. Sponsor: the human decision maker. Gate: a phase approval point. Drift: code diverging from the spec.

### 1.9 Challenge to the brief (what is vague, contradictory or missing)
Vague: "view library instantly"; "flagged or handled"; "brief biography". Contradictory: storage "local/in-memory" against the SQLite mandate; one 400 against two specific error codes; "400 for book not completed" against the specific code. Missing: update-status, get, delete, clear-rating operations; the duplicate confirmation mechanism; what happens to rating and completion date on regression; empty/loading/error behavior; limits and character policy; collection maximum; sorting ties; the shape of `details`; the author entity.

## 2. Analysis of the open points
"Const." means what the constitution or security controls already decide. My choice is the Recommendation; the decision id is in the spec's log.

| # | Point | Options and consequences | Const. already decides | Recommendation |
|---|---|---|---|---|
| 1 | Storage | A) memory (BRD): loses data, fails the restart metric. B) SQLite file: durable | SQLite file | B (DEC-004, confirmed) |
| 2 | Duplicate mechanism | A) flag `allow_duplicate` on POST: one endpoint, stateless. B) two-step token: more state, more surface. C) separate "force" endpoint: second path to same behavior | 409 for duplicates; 409 payload carries the existing book | A (DEC-005) |
| 3 | Missing operations | Add update status (Must), get by id (Should), delete (Must), clear rating (Should); update title/author (Won't, Q2) | "No endpoints beyond spec"; no 204 | DEC-006, DEC-009; delete returns 200 with the body |
| 4 | Regression of status | A) clear rating and completion date automatically. B) block until the user clears the rating (extra step, hostile). C) keep rating hidden (violates invariant 1) | Invariant 1 | A, with client-side confirmation (Q4, DEC-007) |
| 5 | Error codes vs HTTP | A) three specific codes all 400 (BRD). B) `RATING_NOT_ALLOWED` as 409. C) 422 (not allowed by constitution). Wrong type: constitution says `MALFORMED_REQUEST` | Reserved codes; "a spec code with the same meaning takes precedence" | A; value-in-domain errors use the specific code, wrong JSON type uses `MALFORMED_REQUEST` (DEC-008) |
| 6 | Naming | Brief uses `date_added`, `date_completed`; snake_case global; endpoint `/author-bio` later; flag `allow_duplicate` | snake_case, plural nouns | keep brief names (DEC-005, DEC-008) |
| 7 | Frontend | Single page; stories US-010 to US-014 with criteria for list, add, filter, rate, status, delete, duplicate dialog, empty, loading, error, accessibility, safe rendering | Frontend Standards | DEC-021 |
| 8 | Author bio | A) specify now: new Author entity (ASK FIRST), matching rule, shared bio. B) defer to Phase 5 as the BRD says. C) bio stored on each book (no sharing: contradicts the brief) | New entity is ASK FIRST; no feature outside spec | B (Q1). Pre-design for Phase 5 in Section 6 |
| 9 | Author matching (if FR4) | A) same normalized author key as the duplicate rule. B) exact text. C) separate alias table (too much) | Comparison keys computed in Java, a stored key column is a schema change | A, in Phase 5 |
| 10 | Maximum collection and response bound | 2,000 / 5,000 / 10,000 books; worst-case list sizes about 6 / 16 / 31 MB; typical 0.5 / 1.2 / 2.5 MB | Spec must define both (R-02 not accepted) | 5,000 with `409 LIBRARY_FULL` (Q3) |
| 11 | Lengths and character policy | Lengths from the brief; policy from SC-16: trim Unicode white space, NFC, reject Cc, Cf (except U+200D, U+FE0F), Zl, Zp, surrogates, private use, unassigned. Consequence: tab and newline inside a title are rejected | SC-16, Persistence (NFKC key) | BR-006 (DEC-012 confirmed) |
| 12 | Whitespace-only, code-point counting | Whitespace-only becomes empty and fails; counting after trimming and NFC in code points | SC-16 | BR-006 |
| 13 | Unicode normalization | Store NFC; compare NFKC + lower case; refinement: collapse internal white space in the key | Constitution allows spec refinement | DEC-001 |
| 14 | Sort tie-break | A) `id` ascending. B) insertion order (needs a stored sequence, schema). C) none (flaky tests) | "Newest first" in brief | A (DEC-014) |
| 15 | Malformed id | A) 400 `MALFORMED_REQUEST`. B) 404. Security accepts either, never 500 | Reserved code | A (DEC-015) |
| 16 | Invalid status filter | A) 400 `INVALID_STATUS`. B) ignore filter (silent wrong data). C) empty list | Case-sensitive enums | A (DEC-016) |
| 17 | Enum case on input | Case-sensitive only; `Completed` rejected | Constitution | DEC-016 |
| 18 | `details` shape | Array `{field,message}` for validation family; `{existing_book}` for 409; none elsewhere | Constitution leaves shape to spec | DEC-017 |
| 19 | Idempotency and concurrency | POST not idempotent (second add of same pair is 409 without override); PUT status/rating idempotent; DELETE book second call is 404; duplicate check and insert atomic; last write wins | Pool size 1; R-06 accepted with a test | DEC-018, BR-013 |
| 20 | What the UI shows per error | Table in spec 5.9: text chosen by code, never server text | Frontend Standards | US-013 |
| 21 | Durability on restart | A) process-kill safe. B) power-failure safe (cost, not needed). | SQLite file | A (DEC-019, US-009) |
| 22 | Timing | Numbers in M-2 | none | DEC-020 |
| 23 | Rating at creation | A) not allowed (FR1 lists title, author, status). B) allowed when completed (fewer calls, more rules) | n/a | A (DEC-011) |
| 24 | POST response shape | Template `{id, message}` vs full book | Consistent response format | Full book, no `message` (DEC-011) |
| 25 | Delete semantics | Hard delete with client-side confirmation; soft delete/undo is out of scope | no 204 | DEC-009 |
| 26 | Same-status update | Successful no-op; keeps `date_completed` | none | DEC-006 |
| 27 | Order of checks | Fixed order BR-012 | Reserved codes | DEC-024 |
| 28 | Unknown query parameters | Rejected (`MALFORMED_REQUEST`) as strict as the JSON | Strictness | DEC-016 |
| 29 | Body-less requests | DELETE needs `Content-Type: application/json` even without a body: the page's client must send it | CSRF layer | spec 5.0 |
| 30 | Time budget | See Q5 | none | DEC-022 |
| 31 | Stale tabs | Last write wins; no live sync | none | Out of Scope |
| 32 | Date display | Store UTC; show local calendar date | none | DEC-021 |

## 3. Conflicts found between the BRD (and its template) and the constitution
1. Storage: BRD "local or in-memory" against mandated SQLite file. Resolved by the constitution (DEC-004).
2. Errors: the template's single 400 and codes `INVALID_RATING` / `RATING_NOT_ALLOWED` versus the constitution's reserved codes and strict-type rule. Resolved with DEC-008 (specific codes for domain errors, `MALFORMED_REQUEST` for wrong types).
3. Template responses `{id, message}` / `{message}` versus a consistent response format; replaced by the full book (DEC-011).
4. Template timestamps (`...Z` without milliseconds) versus the fixed millisecond format: constitution wins.
5. Template has no 409 handling, no delete, no get, no status update: added as DEC-005, DEC-006, DEC-009.
6. FR4 "added via Drift Management" versus a Phase 2 spec: tension, not a contradiction (Q1). Also "shared across authors" implies a new entity, which the constitution makes ASK FIRST.
7. Length "chars" in the template (UTF-16 in many stacks) versus the constitution's code points: constitution wins.
8. Exercise time (6 to 8 hours, phases sum to 9.5 hours): internal to the brief (Q5).

## 4. PROPOSED decisions (confirm in one pass: "confirm all", or list the ids to change)
DEC-001 duplicate key with whitespace collapsing. DEC-005 `allow_duplicate` flag and `details.existing_book`. DEC-006 `PUT /status`, any-to-any, same-state no-op. DEC-008 error code mapping and precedence of specific codes. DEC-009 get by id, delete (200 with body), clear rating by `DELETE .../rating`. DEC-011 POST returns the full book, no rating at creation. DEC-014 sort tie-break by id. DEC-015 malformed id 400, unknown id 404. DEC-016 strict enums, invalid filter 400, unknown or repeated query parameters 400. DEC-017 `details` shapes. DEC-018 atomic duplicate check, last write wins. DEC-019 durability across process termination. DEC-020 timing targets. DEC-021 frontend scope and behaviors. DEC-023 `date_completed` semantics. DEC-024 order of evaluation. (16 items.) Confirmed by source, no action: DEC-002, DEC-004, DEC-012.

## 5. Sponsor questions (5)

Q1: Should Author Bio (FR4) be specified and built now, or held for the Phase 5 drift exercise as the BRD describes?
Why it matters: The BRD says FR4 is "added via Drift Management". Specifying it now removes the drift demonstration and needs a new Author entity (ASK FIRST). Holding it keeps Phase 2 smaller but FR4 has no downstream items until Phase 5.
Example scenario: A reader wants the same bio on both "Clean Code" and "Clean Architecture" by Robert Martin; that requires an author concept shared by books.
Options: A) Defer to Phase 5 as a change request (US-005 and BR-005 reserved). B) Specify now, with the Author entity approval. C) Specify now as a field on each book (not shared; contradicts the brief).
Recommendation: A. Preserves the demonstration of self-healing, which is a stated success criterion, and avoids an unrequested entity. Section 6 holds the ready design.
Needed by: Phase 2 gate. (DEC-003)

Q2: Can a reader edit the title or author of an existing book?
Why it matters: BRD says CRUD; FR1 to FR4 do not list editing. Editing needs a new endpoint, repeated duplicate checks and more UI.
Example scenario: A reader mistypes "Rober Martin". Without edit, they delete the book and add it again (losing status and rating).
Options: A) Not in v1 (delete and re-add). B) Edit title/author with duplicate check, as a Should.
Recommendation: A. Not required by any FR; keeps v1 small; can arrive by change request later.
Needed by: Phase 2 gate. (DEC-010)

Q3: What is the maximum number of books, and is 5,000 acceptable?
Why it matters: Security requires a stated maximum and response bound (risk R-02 was not accepted). It sets the error `LIBRARY_FULL` and the largest list response (worst case about 16 MB at 5,000, typical under 2 MB).
Example scenario: A collector with 6,000 records tries to add the 5,001st book and sees "Your library is full".
Options: A) 2,000 (worst case about 6 MB). B) 5,000. C) 10,000 (about 31 MB worst case).
Recommendation: B. Covers "thousands of books" with margin while keeping the response small enough to load instantly.
Needed by: Phase 2 gate. (DEC-013)

Q4: When a completed, rated book is moved back to Unread or Reading, how should the rating be handled?
Why it matters: Invariant 1 forces the rating to go (a rating exists only for completed books). The choice is between silent removal, a warning, or a blocking step.
Example scenario: A reader misclicks Reading on a 5-star book. Without a warning the 5 stars and the completion date vanish.
Options: A) The API clears rating and completion date; the page asks for confirmation first ("The rating and completion date will be removed"). B) Clear silently. C) Refuse the change until the rating is cleared by the user.
Recommendation: A. Protects against accidental loss with no extra API call; C is hostile, B loses data silently.
Needed by: Phase 2 gate. (DEC-007)

Q5: Is the 6 to 8 hour exercise budget a hard limit or an indicative figure?
Why it matters: The exercise phases sum to 9.5 hours and our security and test requirements add work; the PM must either cut scope or report the overrun.
Example scenario: Phase 3 yields 20 tasks of 30 minutes (10 hours of implementation alone).
Options: A) Indicative: deliver the full Must scope; Should items go last and can be cut. B) Hard limit: cut scope now to the minimum (drop get-by-id, clear rating, frontend polish).
Recommendation: A, with the MoSCoW cut line (Should, then Could) as the release valve and an explicit overrun report at Phase 3.
Needed by: Phase 3 start. (DEC-022)

## 6. Pre-design for FR4 (for Phase 5, no effect on v0.1)
If Q1 is answered A, the Phase 5 change request would add: US-005 (store and show an author's bio, shared across books by the same author, optional, plain text, up to 500 code points); BR-005 (`BIO_TOO_LONG` 400 above 500 code points, same character policy as BR-006); an Author concept matched by the author key of BR-003; `PUT /api/v1/books/{id}/author-bio` with `{"bio": "..."}` returning the book including `author_bio`; the Author entity approved by the Sponsor (ASK FIRST); additive response member `author_bio` within `/api/v1/`. Open then: bio clearing (null or empty), what happens to the bio when the last book of an author is deleted (recommend: bio removed with it), and display in the page.

## 7. Self-check (agent definition section 6)
- [x] Every requirement traces to the brief or a logged decision (register column Source; DEC ids).
- [x] Acceptance criteria are binary and use numbers and exact codes. Words such as "visible" and "announced" are tied to named behaviors (live region, focus, text).
- [x] Invariants INV-1 to INV-8 stated; BR-004, BR-007, BR-013 and the API protect them (no endpoint sets a rating on a non-completed book or leaves one after leaving completed).
- [x] Unhappy paths, defaults, nullability, ordering and filters specified (BR-010, BR-012, US-002).
- [x] No known conflict with the constitution: snake_case, fixed timestamps, UUIDs, reserved codes, no 204/422, `Content-Type` on DELETE, no pagination with a bound. One deliberate refinement to ask the Liaison to confirm: value-domain errors use the spec codes, wrong JSON types use `MALFORMED_REQUEST` (DEC-008).
- [x] Every UI behavior has an API mechanism (duplicate dialog: `allow_duplicate`; confirmations are client-side before a single request, stated in US-012.3 and US-012.5).
- [x] Out of scope explicit and respected; items beyond the brief (get, delete, clear rating) are logged as PROPOSED and marked.
- [x] Open questions listed (Q1 to Q5); nothing silently assumed; assumptions A-1 to A-9 listed.
- [x] No technology or implementation detail in the spec beyond constitutional terms (UUID, NFC/NFKC, code points). The approved accessibility tool and a native dialog are not named in the criteria except for the P-04 reference in US-014.1: I ask the Liaison to judge whether that reference should be dropped.

## Requested response
Sponsor: answer Q1 to Q5 and reply "confirm all proposed" or list the DEC ids to change (Section 4). Then I apply the answers in round 2 together with the Liaison and Security findings.
