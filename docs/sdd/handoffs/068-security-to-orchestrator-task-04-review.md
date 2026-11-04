# Handoff 068: Security review of Task 04 (domain and repository)
- From: security
- To: orchestrator
- Type: AUDIT_REPORT
- Branch: feature/task-04-domain-repository

## 1. Scope and method
Read handoff 066, Book, BookStatus, BookResponse, BookRepository, BookRepositoryTest (first 150 lines), schema.sql, application.yml; `git diff main --stat` for pom, config and resources (no change; sqlite-jdbc and hibernate-community-dialects were already in pom.xml). I did not re-run the build or the 211+37 tests (relied on the delivery report), and ran no seeded check.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-116 | LOW | Availability, integrity | Book.InstantConverter, ck_books_added/done | The CHECK GLOB accepts digit shapes that are not dates (for example `9999-99-99T99:99:99.999Z`). On read, `Instant.from(parse)` throws, so one bad row can break the whole list query. | schema.sql GLOB only; converter has no guard | Needs write access to the 0700 database file (R-08), or a future code path bypassing the service | Service layer must only write converter output (only Instants are accepted); Task 34 stored-library validation should parse every date. Optional test: a malformed-date row fails closed. | SC-32 |
| SEC-F-117 | LOW | Input validation | schema.sql ck_books_id/title/author | `length()` stops at the first NUL (SEC-F-068). A title or author with an embedded NUL after position 1 can pass the CHECK. The CHECKs also do not enforce the control-character policy or blank text. | SQLite semantics; the tests cover code points but no NUL case | A client sends `"a\u0000..."` with 10 000 chars, and the DB CHECK does not catch it | Task 30 / Task 15 must check length in Java (code points), reject NUL and control characters before persistence, and add a test with a NUL title and author. | SC-32, SC-07 |
| SEC-F-118 | INFO | Config | application.yml, BookRepositoryTest | The dialect is set only by test property. A later full-application task must set `spring.jpa.database-platform` (the Java agent notes this). | handoff 066 | Without it, `validate` on STRICT fails closed at startup (not a security hole) | Condition on the first task that starts the full app (Task 03 or Task 07), with a startup test. | SC-32 |
| SEC-F-119 | INFO | Entity | Book public constructor | The constructor is public and does not validate. Only the service layer may build entities. | Book.java | A later controller binds Book directly (mass assignment) | The Task 07 and Task 15 reviews check that no Book or entity type is used in a controller signature. | SC-08 |

No CRITICAL or HIGH findings.

## 3. Controls checked
- SC-07: VERIFIED for Task 04. Two derived queries with an enum parameter, no `@Query`, no native SQL, no `select *`, no string-built SQL. `repositoryDeclaresNoNativeOrHandWrittenQueries` guards this and the Checkstyle regex is active (handoff 044). Remains partly open for later repositories.
- SC-32/SC-37 interplay: VERIFIED by test as reported. `ddl-auto=validate` on a STRICT table with the community dialect; the stored `sqlite_master` text, normalised, equals the normalised schema.sql. Statement-text equality is a stronger proof than a column check. Not re-run by me.
- INV-1 to INV-4 CHECKs: VERIFIED by review of schema.sql and the 28 reject and 6 accept cases listed in 066. Status set; rating only on completed and 1 to 5 (INTEGER STRICT rejects non-numeric text); completion date present exactly when completed; fixed 24-character timestamp shape. Residual: SEC-F-116, SEC-F-117.
- Id format: VERIFIED. 36 characters, hyphens at positions 9, 14, 19 and 24, 32 non-hyphen characters, `NOT GLOB '*[^0-9a-f-]*'` (lowercase hex only, so no path, quote or injection characters). The hyphen-count check plus the position checks are redundant but harmless. `updatable=false` on id and date_added.
- Code-point lengths: VERIFIED for emoji (500 and 501, 200 and 201 cases). The NUL caveat is raised as SEC-F-117.
- BookResponse: VERIFIED. A record with exactly seven components (test asserts it), built through `from()`, no entity reference, lowercase status via `wireValue`, null for absent members.
- Book: VERIFIED. No setters (test `bookHasNoPublicSetters`); `Optional` getters; the protected no-argument constructor is for JPA.
- Timestamps: VERIFIED. The formatter is fixed UTC with milliseconds and a literal `Z`; no default time zone is involved; the round trip is asserted on the raw column. Years above 9999 produce a 25-character value that the CHECK rejects (fails closed).
- Status converter: fails closed with a generic `IllegalStateException("Unknown stored status")`, which leaks no stored value.
- Dependencies: VERIFIED, no new dependency (no pom.xml change).

## 4. Not verified
I did not run the build or the tests myself, and ran no seeded check. I did not read test lines 150 to 283. The Task 34 stored-library validation is out of scope here.

## 5. Residual risks
None new for the Sponsor. SEC-F-116 and SEC-F-117 depend on R-08 (same-user write access).

## 6. Verdict
PASS WITH CONDITIONS. Conditions, to be closed in later tasks, not blocking the merge of Task 04: SEC-F-117 (Java length and NUL tests at Task 15 / Task 30), SEC-F-118 (dialect property at the first full-app task).

## Appendix: exact security.md text (orchestrator to apply; I did not edit security.md)
Findings log rows:
- `| SEC-F-116 | LOW | CHECK GLOB accepts digit-shaped invalid dates (9999-99-99T99:99:99.999Z); the converter throws on read, so a tampered row breaks list queries | OPEN, covered by Task 34 stored-library validation | 068 |`
- `| SEC-F-117 | LOW | SQLite length() stops at NUL and CHECKs do not enforce control-character policy; Java must check code-point length and NUL (SEC-F-068) | OPEN, condition on Task 15 / Task 30 with a NUL test | 068 |`
- `| SEC-F-118 | INFO | Dialect set only by test property; the first full-app task must set spring.jpa.database-platform and test the startup | OPEN, note for Task 03 / Task 07 | 068 |`
- `| SEC-F-119 | INFO | Book has a public non-validating constructor; no entity type may appear in controller signatures | OPEN, check at Task 07 / Task 15 | 068 |`

Control cell updates:
- SC-07: append `2026-11-04 Task 04 (068): VERIFIED for BookRepository (two derived queries, no @Query, guard test, Checkstyle ban active); status stays IN_PROGRESS until later repositories.`
- SC-32: append `2026-11-04 Task 04 (068): Hibernate validate on STRICT with the community dialect; the sqlite_master text equals schema.sql after normalisation; CHECKs for INV-1 to INV-4 and the id format proven by 28 reject and 6 accept cases; NUL caveat SEC-F-117.`
