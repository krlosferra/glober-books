# Handoff 138: Liaison audit, Task 31a (get, delete, status change)
- From: liaison
- To: orchestrator (for the PM, Security and the Sponsor gate)
- Phase: 4   Round: 1
- Type: REVIEW_REPORT
- Mode: F (with G for the oracles)
- Artifacts reviewed: brief 136, delivery 137, `tasks.md` v0.6.2 Task 31a/31b, `spec.md` 5.3, 5.4, 5.7, US-007.2, US-003.8, T-14, T-14b, T-38, S-07, E-02, E-18, E-24, E-27, `git diff origin/main..HEAD` (main and test code), `git log origin/main..HEAD`.

## Verdict: ACCEPT WITH FOLLOW-UPS (Task 31a)
Compliance: criteria 1 to 5 met; brief constraints 1 to 7 met. Tests: I re-ran `./mvnw -o clean verify` in `backend`: BUILD SUCCESS, "All coverage checks have been met", failsafe `Tests run: 201, Failures: 0, Errors: 0, Skipped: 1` (the skip is the existing root-permission assumption in `SqliteStoreIntegrationTest`, as in the delivery). Security verdict from `sdd-security-reviewer` is still required and I cannot replace it. Protected surface, reviewed alone: the Sponsor gate follows.

## Layer results
- Layer 1 (spec compliance): PASS. Order of checks follows BR-012: gate (route, id shape, query, empty body), then strict reader plus `@Valid StatusRequest`, then `BOOK_NOT_FOUND`, then the domain change. Service re-parses the status before `findById` (defence, T-38). Lookup by bound parameter through `findById`. No logging added, no value echo, no new dependency, no `pom.xml`, `backend/config/**`, schema, gate or handler change. Only the task-listed files plus two test files (see b) were touched. No file above 300 lines found by `wc`.
- Layer 2 (testing): PASS. Oracles checked against the spec, not the code (details below).
- Layer 1b (hygiene): PASS. The six local commits all carry the configured human identity; a grep of the commit bodies for tool, vendor, session, "Generated" and co-author text found nothing; Conventional Commit subjects with `[Task-31a]`; branch name matches the task header. (h) closed.
- Layer 3 (quality, non-blocking): see N-1.

## Key judgments
(a) `discard`: sound. The claim is plausible (SpotBugs' EI_EXPOSE_REP2 mutability heuristic keys on mutator-style method names) and the constitution forbids an exclusion without approval, so renaming is the least-cost route and keeps the controller method `delete`. No spec text names the service method. `plan-appendix.md` line 18 only says "all use cases, one `@Transactional` method each", so no plan contradiction, but the name should be recorded: follow-up F-1 (a one-line plan note, so Task 31b and later tasks do not reintroduce `delete`/`remove`/`set*` named methods on `BookService`; 31b will add `assignRating`/`clearRating` style names, which should be checked against the same detector).

(b) The two extra test edits are justified and the new oracle is right. The Task 15 placeholder asserted 404 for `GET .../status` and `POST /{id}` "until Task 31" with the comment "spec 405"; E-02 and E-27 rows 1 and 2 say 405 with `Allow` of that path (E-29). The new expected values (`PUT` for `.../status`, `DELETE, GET` for `/{id}`) are the methods the spec defines for those paths, not read from the code. In `RouteTableTest` the only change to the non-routable list is `/api/v1/books/x` replaced by `/api/v1/books/x/y`, which is correct (a single non-empty segment is now an id); `/api/v1/books/` and `/api/v1/books/<id>/` stay non-routable, as US-007.2 requires. Brief constraint 6 expected exactly this table update. Not a scope breach.

(c) RouteTable rule: consistent with E-27 rows 1 and 2 (`POST /books/not-a-uuid` gives 405; `GET /books/not-a-uuid/status` gives 405), row 3 (`/other` gives 404, covered by segment-5-not-`status`), US-007.2 (route before id) and SC-18 (`Allow` lists exactly the defined methods, computed from the template). Non-routable segments (`;`, `%2f`, dot segments) still give 404 before the id rule (E-24b), and any `%` in an otherwise routable segment reaches the gate and gives 400 (E-24 row 1, tested at the real port). The id shape stays in the gate only; the service does not re-implement it. Accepted.

(d) Malformed id plus invalid status body gives `MALFORMED_REQUEST`: correct. BR-012 puts the id shape (step 3) before field values (step 6), and US-003.8 says a malformed id returns 400 `MALFORMED_REQUEST`. The T-38 rule (field values before existence) concerns a well-formed id only. Observation O-1: no test pins this combination for `PUT status` (the S-07 test calls `/not-a-uuid/status` with a valid body only). Optional follow-up F-4: add one real-port row. The spec does not need a change, but the PM may add one sentence to US-006 so the ordering is not left to inference.

(e) E-02 `GET .../{id}/rating` = 405 and the T-14 rating rows: confirmed NOT covered by 31b's text. Task 31b's criteria and traces mention T-31, T-36, T-37, C-03, BR-012 steps 6 to 8 and "extend the route table with the two rating routes (seven in all)", but the Traces line omits T-14, T-14b-style routing rows, E-02 (rating row), E-24 and E-29. Until 31b, `GET/PUT/DELETE .../rating` answer 404 `NOT_FOUND` instead of 405 / 400, which is a known interim state recorded in progress.md. F-2 (PM, `tasks.md` Task 31b): add a criterion "On the real port, `GET .../{id}/rating` gives 405 with `Allow: DELETE, PUT`, and T-14 rows for `PUT` and `DELETE .../rating` give 400 `MALFORMED_REQUEST`; `RouteTableTest` holds the seven routes", and add T-14, E-02, E-29 to the traces. Do not let Task 31b pass without it, otherwise these rows never get a test.

(f) Order and atomicity:
- Field values before existence: `fieldValuesAreJudgedBeforeExistence` (unknown id: `"Done"` gives 400 INVALID_STATUS, `5` gives 400 MALFORMED_REQUEST, `"reading"` gives 404) matches T-38 and T-19; the service unit test asserts `findById` is never called for invalid statuses. Good oracle (expected values come from the spec).
- T-19 is proven at the real port (SEC-F-159 met), plus unknown member, `[]` and truncated JSON, all `MALFORMED_REQUEST`.
- Transaction atomicity: `Book.changeStatus` mutates status, rating and date together, then one `saveAndFlush` in one `@Transactional` method; the fixed clock test checks 123 ms from 123456789 ns (truncation to ms per DEC-023) and that leaving completed clears both fields. The annotation-reflection test is a weak proxy for "one transaction" but the brief and criterion 3 defer the forced-failure proof to Task 32 (S-10). Accepted; Task 32 must not drop it.
- S-07 snapshot: reasonably strong. It seeds a completed book with a rating and a date, snapshots the full `GET /books` body, runs 13 failing requests (bad id shape, unknown id on GET/DELETE/PUT, `DELETE` with a body, five bad status bodies, an extra member, truncated JSON) against the rated book, and compares the entire body. It would catch a partial write on the rated book. Weakness (minor, N-2): only two books and the failing PUTs are all body-level failures; no failing request exercises the service after the book is loaded, which is Task 32's forced-failure case.

(g) 5,000-book run: delivery numbers are plausible and consistent with the build output (list 96 ms for 5,000, status change 29 ms, delete 7 ms, add 53 ms, next add 409 `LIBRARY_FULL` 6 ms; count remains 5,000). The test asserts `total` 5000, 200, 200, 201, 409 and `repository.count()` 5000. Minor N-3: it does not assert the changed book's new status in the response body, nor that the deleted id is gone; neither blocks C-04. The cold-JIT caveat is stated.

## Criteria check
| Criterion | Result | Evidence |
|---|---|---|
| 1 (5.3, 5.4, 5.7, 201 then GET, S-06, S-04, S-11, five routes) | Met | story tests, `holdsExactlyTheFiveApiRoutes`, `equalsTheMappingsOfTheControllersAndTheCatalog` |
| 2 (T-38, T-19, S-07) | Met | see (f) |
| 3 (fixed clock, same transaction) | Met | `BookStatusServiceTest`; forced failure is Task 32 |
| 4 (C-04, US-008.3) | Met | (g) |
| 5 (E-18 on real endpoints) | Met | no body 200, one space 400, `{}` 400, GET with body 400 (spec E-18 row uses `{}`, criterion uses one space; both are tested) |
| Brief 1 to 7 | Met | 6: completed E-24 row 1, E-27 rows 1 and 2, T-14 (GET/DELETE/PUT status); stated honestly in the delivery |

## Follow-ups (none blocks the 31a gate)
- F-1 (PM/plan owner): record in `plan-appendix.md` (BookService row) that the delete use case is `discard`, with the SpotBugs reason, and that method names on `BookService` must avoid mutator-style prefixes flagged by EI_EXPOSE_REP2. Ask the Java architect to check `assignRating` names in 31b against the same detector early.
- F-2 (PM, `tasks.md` Task 31b): add the E-02 rating row, T-14 rating rows, E-29 and the seven-route `RouteTableTest` to the criteria and traces (see e).
- F-3 (Security review): ask `sdd-security-reviewer` to confirm SC-18 (`Allow` for the two new templates), SC-14 (no id logged) and that the "any non-empty segment is an id" rule cannot make a non-routable target routable (E-24b rows are covered by Task 16 and unchanged by this diff).
- F-4 (optional, Java architect in 31b or PM): add a real-port row `PUT /books/not-a-uuid/status` with `{"status":"Done"}` giving `MALFORMED_REQUEST`, to pin (d); optionally one sentence in US-006.
- N-1 (style): `RouteTable.ID_SEGMENT_COUNT`/`SUBRESOURCE_SEGMENT_COUNT` read oddly (`ID_SEGMENT_COUNT - 1` as an index); a clearer constant naming would help when 31b adds `/rating`. Non-blocking.
- N-2, N-3: see (f) and (g).

## Actions for the orchestrator
1. Obtain the Security verdict on this task; reviewed alone (constitution 1.6).
2. After the Sponsor approves at the gate: update `status.md` (audit column "ACCEPT WITH FOLLOW-UPS (handoff 138)", state to DONE once merged), push and open the pull request (the approval is the request), squash merge. I did not edit `status.md` or `progress.md`, and I made no commit.
3. Route F-1 and F-2 to the PM before Task 31b is briefed.
