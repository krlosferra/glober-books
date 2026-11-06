# Handoff 131: Security review, chunk 23 (Task 23a add rules, Task 23b list filter and add bound)
- From: security
- To: orchestrator
- Phase: 4   Round: 1
- Type: AUDIT_REPORT
- Brief: 128; delivery 129

## 1. Scope and method
Branch `feature/task-23-add-rules`, base 74c0bfa, head 72b5a5f (pull request 34, constitution 1.6, one review for the chunk). Read: the production diff (`BookService`, `BookRepository`, `BookKeyRow`, `BookController`, `DuplicateKey`), `QueryPolicy`, `RequestGateFilter`, `SqliteDataSourceProvider`, `application.yml`, `schema.sql`, the error handler list. Probed in a scratch copy (jar built from `git archive HEAD`, JDK 21, `-Xmx512m`, port 18099, temporary database pre-filled with 4,999 books of 500-code-point Cyrillic title and 200-code-point author): duplicate and capacity paths, injection title, filter values, 50 parallel POSTs in three shapes. I did not rerun the delivery's test suite (the orchestrator build check owns it).

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-166 | LOW | DoS margin | `BookService.findEarliestMatch` | Cold duplicate at 4,999 maximum-size books took 1.64 s here (0.93 s in the delivery); bound is 2 s | curl `time_total` 1.636 s, first request after start; warm 0.06 to 0.18 s | Slow machine, cold JVM: bound could be exceeded | Task 41 records the cold figure on the reference machine with the README heap; if above 2 s, STOP to the Sponsor (no weaker test, no schema change without ASK FIRST) | SC-17 |
| SEC-F-167 | INFO | Availability | single connection, `connection-timeout: 5000` | 50 parallel adds at 4,940 books queue for up to 3.9 s; no error here | 50x 201, max 3.9 s, 0 ERROR lines | Slower host passes 5 s and the connection request fails | Task owning C-11 c proves the failure maps to a generic envelope with no partial write | SC-17, SC-11 |
| SEC-F-168 | INFO | Concurrency | pool size 1, `locking_mode=EXCLUSIVE`, `BEGIN IMMEDIATE` | No TOCTOU on count+scan+insert inside the process; no unique index backs the rule, so it depends on pool size 1 | probes below | n/a | Any change of pool or locking needs a new review | SC-17 |
Closed: SEC-F-120 (cap in the add transaction), SEC-F-163 (stored-form key, earliest match). No CRITICAL, HIGH or MEDIUM.

## 3. Controls checked
- SQL/JPQL injection (SC-07, SC-16, T-12): VERIFIED. No `@Query`, no native query, no `JdbcTemplate`; `findAllBy()` is a Spring Data class-based projection onto `BookKeyRow`, `findByStatusOrderBy...` binds an enum parsed by `BookStatus.fromWire`; the key comparison runs in Java. Probe: title `x'; DROP TABLE books;--` stored as data (at 4,999 books it reached LIBRARY_FULL, in the 100-book probes the table stayed intact).
- Transaction and order (BR-009, INV-5, INV-7): VERIFIED by code read: `@Transactional create`: validation, `count() >= 5000` then `LIBRARY_FULL`, then scan only when `allow_duplicate` is not true, then insert. One scan query, no loop (plus one `findById` for the matched book). Probe at 5,000 books: new add, add with override and duplicate with override all 409 `LIBRARY_FULL` with no `details`.
- Duplicate rule: VERIFIED. Probe: duplicate of the first stored book gives 409 `DUPLICATE_BOOK` with one `existing_book`; earliest match by `date_added` then id (comparator in code, tests in delivery).
- Concurrency (probes): 4,999 books and 50 parallel distinct adds: one 201, 49 409, total 5,000. 50 parallel overrides at 4,999: 50 409. 30 identical parallel adds on a 100-book library: one 201, 29 409, total 101. No 500. Cause: one pooled connection, exclusive locking, IMMEDIATE transactions. C-11 c itself is out of chunk.
- Status filter (US-002.4, BR-010): VERIFIED. The controller reads `@RequestAttribute(QueryPolicy.STATUS_ATTRIBUTE)`; only the gate sets it, after decoding and a three-value check; a client cannot set a request attribute. Probes: `?status=reading` 200 `{"books":[],"total":0}`; repeated `status` 400 MALFORMED_REQUEST; `status=' OR 1=1` 400 INVALID_STATUS with a fixed message and no echo. The service re-checks the value (defence in depth).
- DoS cost (SC-17): see SEC-F-166. List of 5,000 maximum-size books: 7.75 MB, 0.13 to 0.45 s; heap 512 MB, no OutOfMemoryError in any probe (single-run scale; the 8-reader memory criterion stays with Task 41).
- Disclosure (SEC-F-164): unchanged. 409 shows one full book, as Appendix A states; `LIBRARY_FULL` carries no details; no keys or counts.
- Logging (SC-14): VERIFIED. No logger in service, repository or `BookKeyRow`; the probe log (35 lines) holds no title, author or key; `BookKeyRow.toString` hides members.
- Error bodies (SC-11): VERIFIED by probes above (generic messages, fixed `code`).
- Scope hygiene: no change to `pom.xml`, `backend/config/**`, schema or dependencies (diff stat). `DuplicateKey.isWhiteSpace` made package-private only.

## 4. Not verified
Full `./mvnw clean verify` and the delivery tests (not rerun by me); the 8 slow readers memory criterion (Task 41); C-11 c (out of chunk); behaviour on the reference machine; the Javadoc of `BookKeyRow` still says "JPQL constructor projection" while the code is a Spring Data projection (wording, Liaison). Public advisory databases not queried in this review (no dependency change).

## 5. Residual risks
SEC-F-162 and SEC-F-165 stay spec-accepted (R-18 family). No new risk for the Sponsor register; SEC-F-166 is a watch item.

## 6. Verdict
PASS WITH CONDITIONS. C-1 (LOW, SEC-F-166): Task 41 records the cold 2-second figures on the reference machine; the evidence is in the Task 41 delivery. Open INFO items SEC-F-167 and SEC-F-168 go to the C-11 c task. Next: Liaison verdict, then the Sponsor gate. Updated `security.md` (v0.46) and `status.md` (rows 23a, 23b, IN_REVIEW, pull request 34). Nothing committed.
