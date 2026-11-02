# Handoff 028: Java architect, backend plan revision (round 2 text)
- From: java
- To: orchestrator (copy to liaison, security, pm)
- Phase: 3   Round: 2 of 3
- Answers: handoff 025 (Liaison F-02, F-03, F-05, F-06, F-11, F-12 AP-5, F-14, F-22) and handoff 026 (SEC-F-066 to 074, 078, 079)
- Type: PLAN_REVISION (replacement text; `plan.md`, `plan-appendix.md` and all other files are untouched, the orchestrator applies it)

How to read: each block names the passage, then gives the text that replaces it. "Replace" means the whole passage named; "Add" means a new line or row. Task numbers are those of `tasks.md` 0.3. Items marked **TASKS** must also be mirrored in `tasks.md` by the PM (listed in section C). Items marked **SPONSOR** are listed in section D and are not decided here.

Page budget: the new text adds about 28 lines to `plan.md`. To stay within about 5 pages, the validator order, the pool rules and the native SQL inventory live in a new appendix section AP-12; `plan.md` keeps only the normative one-liners and the "Native SQL and JDBC" paragraph the Liaison asked for (F-11). Two cuts are proposed in section B-12 (about 8 lines). Net effect is plan.md at about 135 lines of the present density.

---

## A. plan.md

### A-1 Section 2, row D2 (SEC-F-066)
Replace the D2 row with:

| D2 Errors | One `ErrorBodyFactory` builds every body (`ErrorBody`, `DuplicateErrorBody`; no `details` of type `Object`). Users: `GlobalExceptionHandler`, the gate through `ErrorResponseWriter`, `EnvelopeResponseWrapper` (Task 17) for any `sendError`. `ErrorMvcAutoConfiguration` excluded. **From Task 06 on, unconditionally:** a Tomcat customizer sets the error report valve to `showReport=false` and `showServerInfo=false` so that no container page names the product or version, and the advice maps the MVC framework exceptions (no handler or resource, method not supported, media type not supported, not acceptable) to the envelope. Tomcat's pre-application answers (TRACE included) stay generic and carry no product or version | Constitution; controllers never build bodies; the container error page is never reachable in any tagged build |

### A-2 Section 2, row D5 (SEC-F-078, SEC-F-073)
Replace the D5 row with:

| D5 Limits | Tomcat `threads.max 8`, `max-connections 128`, `accept-count 128`, `connection-timeout 5s`, `max-swallow-size 16KiB` (explicit, next to the others); Hikari `connection-timeout 5000` | At most 8 requests run. Up to 128 connections are accepted and the ones beyond the 8 threads wait in Tomcat's executor queue; only beyond `max-connections` does `accept-count` (the OS backlog) apply. Of the running requests one holds the database connection; the others wait at most 5 s for it and then answer the generic `500 INTERNAL_ERROR` (pool timeout, no detail). `connection-timeout 5s` is a per-read timeout, not a deadline for a whole request; the drip test of Task 16 decides whether a deadline in `BoundedBodyRequest` is needed. Any change of threads, connection limits or the Hikari timeout is ASK FIRST and returns to Security |

### A-3 Section 2, row D4 (SEC-F-072)
Replace the last sentence group of D4 ("`StaticContentController` serves `StaticAssetCatalog`: ...") with:
`StaticContentController` serves `StaticAssetCatalog`, which receives its root resource through its constructor; the production wiring (`StaticContentConfig`) passes `classpath:/static/` and nothing else, and there is no property, profile or environment variable that changes the root. The catalog lists every `.html`, `.css` and `.js` under that root at startup (any other file there stops the start). The Task 09 test builds the catalog directly with a fixture directory.

### A-4 Section 2, paragraph after the table of settings (SEC-F-067, Liaison F-11)
Replace "Built in `SqliteDataSourceProvider` from the validated path; the configured URL stays `jdbc:sqlite:<file>` and is not overridable." with:

Built in `SqliteDataSourceProvider` from the path that `StartupPolicy` (Task 05) has validated. The URL form is fixed to `jdbc:sqlite:<file>` with no parameters; only `<file>` is configurable (`SPRING_DATASOURCE_URL` carries it, as the E2E launcher needs). Every per-connection setting is applied by the data source that the pool wraps, with the same keys as the sqlite-jdbc URL parameters and the same effect as `connection-init-sql` (constitution, Persistence); they are not in the URL so that the URL stays checkable.

### A-5 Section 2, settings table, rows "locking_mode" and "Pool" (SEC-F-069)
Replace both rows with:

| `locking_mode` | `EXCLUSIVE`. The claim is `BEGIN EXCLUSIVE` + `COMMIT` with no change, **run for every physical connection, before any query on it** (connection init, together with all other settings); a second instance fails with "database is locked" |
| Pool | size 1: `maximum-pool-size` = `minimum-idle` = 1, `idle-timeout 0`, `keepalive-time 0`, `max-lifetime 0`, so the pool never retires or adds a connection on its own. A replacement after a fatal error repeats the claim and the settings (proved by a forced eviction and a read-back, Task 03) |

Add a row:

| After the claim | The application only calls `stat`-type operations (exists, size, attributes) on the database path. Reads of the file by plain Java I/O happen before the claim (start step 2) or when no instance holds the file; closing any other descriptor of the file would release a POSIX lock (AP-12.2) |

### A-6 Section 2, "Start order" steps 1 to 5 and the paragraph after (SEC-F-067, 068, 074; Liaison F-14)
Replace the list and the paragraph "`StartupRefusalIntegrationTest` ..." with:

1. `StartupPolicy` (Task 05): bound address not loopback, or URL not of the form `jdbc:sqlite:<file>` with no parameters: stop. Then `DataDirectoryGuard` (Task 34): create directory 0700 and file 0600; refuse symlink, other owner, group or other bits.
2. A non-empty file: plain Java I/O reads the 100-byte header (magic string, page size, bytes 18 and 19 equal to 1). A WAL-marked or non-SQLite file is refused here, before SQLite touches it, so no journal is applied to it. The check "size equals page size times page count" is skipped when a non-empty `<file>-journal` exists (the header count is valid only after a commit).
3. The pooled connection opens and takes the claim. SQLite rolls back a hot `-journal` here and nowhere else.
4. `StoredLibraryValidator` on that connection with `query_only` ON, in this order: (a) `sqlite_master` and the stored DDL, (b) `quick_check`, (c) the rows, bounded. Detail and refusal reasons: AP-12.1. A zero-length file skips this step.
5. Only then `query_only` goes OFF, `schema.sql` runs (a no-op on a valid database), then Hibernate `validate`.

`StartupRefusalIntegrationTest` hashes the file before and after each invalid case that has no journal and requires an equal hash, a non-zero exit and a fixed reason without book data. The only earlier write is SQLite's rollback of a hot `-journal` at step 3. The outcome (accept, or refuse with a non-zero exit) is judged on the file after that rollback; a journal that is not a valid journal of that file is ignored by SQLite and changes nothing (closing clause proposed for CR-9; Task 34 AC3 has a case for each of: valid file with a genuine hot journal, invalid file with a genuine hot journal, planted garbage journal beside a valid and beside an invalid file).

### A-7 Section 2, after "Data model": new paragraph "Native SQL and JDBC" (Liaison F-11, constitution line 160)
Add:

**Native SQL and JDBC (justified, constitution NEVER list).** JPA and Spring Data are used for everything the API does. Raw JDBC and native statements exist in `config.sqlite` only, because they run before Hibernate and `schema.sql` or configure the connection, which JPA cannot do: (1) the pragmas and the claim (`journal_mode`, `synchronous`, `locking_mode`, `foreign_keys`, `busy_timeout`, `trusted_schema`, `query_only`, `BEGIN EXCLUSIVE`, `COMMIT`), run per connection by `SqliteDataSourceProvider`; (2) the read-only checks of `StoredLibraryValidator`: `sqlite_master` (`name`, `type`, `sql`), `quick_check`, and one row query with an explicit column list and `LIMIT 5001`. The validator cannot use entities: Hibernate maps only after validation and would load unchecked values. All statements are constants or take bound parameters, none contains `*` in a select list (`count(*)` is not a select list of columns and is avoided anyway). The duplicate scan is not native: it is a JPQL constructor projection with explicit fields (`BookKeyRow`). Inventory in AP-12.3.

### A-8 Section 2, "Data model" last sentences (Liaison F-11, SEC-F-068)
Replace "Fallback if the capacity measurement fails: a stored key and an index, which is a new Sponsor decision." with:
Fallback if the capacity measurement fails: a stored key and an index. That changes `schema.sql` and is ASK FIRST: the task stops and requests Sponsor approval, it does not apply the fallback. The stored DDL of an existing file must equal the normalised statement of `schema.sql` (AP-12.1), so a file with the same columns but no CHECK constraints is refused.

### A-9 Section 5 (Liaison F-12, AP-5)
Replace the sentence "Risk tiers (P0 always blocks, ...) are assigned per Appendix D row and per UI criterion in `tasks.md` section 7 and AP-5, AP-9." with:
Risk tiers (P0 always blocks, P1 verified once, P2 documented) are assigned per Appendix D row and per UI criterion in `tasks.md` section 7, which is the only tier register; AP-5 and AP-9.6 keep no tier of their own and point to it.

Add one row to the testing table, level "Backend integration", cell content: append "; the claim is proved in a child JVM (Task 03), never only with a second connection in one JVM; hashing of the database file happens only while no instance holds it".

### A-10 Section 7, risks (Liaison F-06, F-11; SEC-F-069, 071, 073, 078)
Replace rows X-04, X-06, X-10, N-2/N-3 and N-6 and add rows X-12 and N-10:

| X-03 | 50 adds plus 50 lists at 4,949 maximum-size books through one connection; the eighth running request may wait more than the Hikari 5 s and answer `500` | Measured in Task 41; failure goes to the Sponsor, never a weaker test, a 503 or a changed SC-06 value |
| X-04 | Duplicate scan at 4,999 maximum-size books without a stored key, estimate 0.3 to 1 s against the 2 s bound (never cut) | Smoke in Task 23, measured in Task 41; fallback is the key column: ASK FIRST, the task stops and asks the Sponsor |
| X-06 | `ddl-auto: validate` with STRICT and CHECK on the community dialect is untested | Spike in Task 04; fallback `typeof` CHECKs changes `schema.sql`: ASK FIRST, stop and request approval; the spike also proves that the stored DDL text equals the normalised statement (AP-12.1) |
| N-2, N-3 | Claim semantics, per-connection init and sqlite-jdbc keys come from knowledge | Task 03 reads effective values back with pragmas, proves the claim from a child JVM and after a forced eviction; fallback a no-op write transaction, decided in Task 03 |
| N-6 | Tasks at the 45-minute ceiling (01, 03, 06, 08, 14, 21, 23, 34) | A task that overruns is split, never extended; the first failed spike stops the task for a split. Proposed splits: 23a rules, 23b filter and C-11 (a)(b); 34a `DataDirectoryGuard` and `StartupFailureAnalyzer`, 34b validator, header check, hot journal, second instance |
| X-12 | Memory: a maximum list is about 31 MB of strings and entities per in-flight request, up to 8 in flight | Fixed `-Xmx` (proposed 512 MB, final value from Task 41) in the README command and in the C-11 (c) JVM; Task 41 AC4 passes when 8 slow readers end without `OutOfMemoryError` and the process then serves; failure goes to the Sponsor |
| N-10 | Planted or hot journal beside an invalid file (R-20 proposed) | Cases in Task 34 AC3 (A-6); accepted residual risk is a Sponsor matter |

(X-10 and the rest stay as they are. The `swap between check and open` of Task 34 AC1 has no test that respects A-09: it is documented as a limit of `DataDirectoryGuard`, see D-5.)

### A-11 Section 2, new line under D3 or a new D7 (SEC-F-071)
Add row:

| D7 Response writing | A list is serialised by the message converter straight into the response output stream; no `byte[]` or `String` of the whole body is built. The service returns the DTO list after its transaction ends, so the connection is not held while a slow client reads | Bounds memory to the DTOs of the in-flight lists (X-12) and keeps the single connection free |

### A-12 Section 4, E2E bullet (SEC-F-070 is a frontend matter; only the backend-visible part)
No backend text. The jar must start with the three values as command-line arguments: it already does (Spring command-line arguments are the highest source). Task 13 owns the rest.

### A-13 Section 3 / 4 (Liaison F-02, X-11)
Replace the X-11 row with:
| X-11 | The gate's route table duplicates the MVC mappings | `RouteTableTest` enumerates the controllers' mappings and fails on any difference. The table holds the routes that exist at that task: two in Task 15, seven after Task 31, which edits `RouteTable` and keeps the test green |

---

## B. plan-appendix.md

### B-1 Header line 7 (Liaison F-22 a)
Replace the "Task ids:" line with:
Task ids: every task is named by its number in `tasks.md` (Task 01 to Task 52). Frontend candidate ids of handoff 021 (F0.1 and so on) appear only in the `Source:` line of a task.

### B-2 AP-1 (Liaison F-02, F-05, F-22 c; SEC-F-067, 072; F-06)
Replace these rows:

| controller | `BookController` (Task 07, Task 23, Task 31) | Seven endpoints, no rules |
| | `StaticContentController` (Task 09) | Serves catalog entries |
| error | `GlobalExceptionHandler` (Task 06, Task 20, Task 22) | `@RestControllerAdvice`, explicit JSON type. Task 06: generic `500` and the MVC framework exceptions. Task 22: maps `ApiException` to its code and status and `DuplicateBookException` to `409` with `details.existing_book` through the factory |
| | `ApiException`, `DuplicateBookException`, `DuplicateErrorBody` (Task 22) | Service exceptions with code; duplicate carries the earliest match |
| config | `ClockConfig` (Task 03), `ValidatorConfig` (Task 20), `WebMvcConfig` (Task 21), `StaticContentConfig` (Task 09), `ErrorReportConfig` (Task 06), `BookText` (Task 20, annotation with nested validator) | Wiring (no EL interpolator); `StaticContentConfig` passes `classpath:/static/` to the catalog; `ErrorReportConfig` is the unconditional Tomcat customizer (`showReport=false`, `showServerInfo=false`) |
| config.transport | `RequestGateFilter` (Task 06, Task 15, Task 16), `HeaderPolicy` (Task 06), `BoundedBodyRequest` (Task 06, Task 16) | unchanged |
| | `RequestTarget`, `AcceptPolicy` (Task 15), `RouteTable` (Task 15, edited in Task 31), `QueryPolicy` (Task 16: query grammar and the `status` value of the list, `INVALID_STATUS`) | Raw-target parser, route and method table, Accept and query grammars |
| | `EnvelopeResponseWrapper` (Task 17), `StaticAssetCatalog` (Task 09: constructor takes the root resource) | `sendError` to envelope; fixed served paths |
| config.sqlite | `SqliteDataSourceProvider` (Task 03, Task 34), `StartupPolicy` (**Task 05**), `DataDirectoryGuard`, `StoredHeaderReader`, `StoredLibraryValidator`, `StartupFailureAnalyzer` (Task 34) | Data source with per-connection settings and claim; fail-closed start; directory trust; plain-I/O header check (step 2, before SQLite); stored-data validation; clean console reason |

`StartupPolicy` leaves Task 17 (Task 17 keeps `EnvelopeResponseWrapper` and the test that an error dispatch cannot bypass the headers). **TASKS** (section C).

### B-3 AP-2 (Liaison F-03; SEC-F-066, 073)
Row 8, replace with:
| 8 | Query grammar, then on the list the `status` value: exactly `unread`, `reading` or `completed`, else `INVALID_STATUS` (owner: `QueryPolicy`, Task 16; decoded value passed as request attribute) | gate | 400 `MALFORMED_REQUEST`, `INVALID_STATUS` |

Row 0, replace the "Failure" cell with: pre-application answer (400, 405, 414, 431, 501, 505), no product or version, no HTML body naming the container (error report valve configured in Task 06).

Replace the last sentence of the paragraph after the table ("E-22 is the connector's 5 s timeout, ...") with:
E-22: the connector's 5 s timeout is a per-read timeout. It is asserted by configuration in Task 08 and live in Task 16, where the live test also drips one byte every 2 s on the request line, on the headers and on the body of a valid POST and records whether the connection survives; if it does, `BoundedBodyRequest` gets a total read deadline or the limit is reported to the Sponsor as an accepted limit (never a silent pass). The gate registers for the request dispatch and a Task 17 test shows an error dispatch cannot bypass the security headers.

### B-4 AP-5 (Liaison F-12, AP-5 aligned with tasks.md section 7)
Replace the whole of AP-5 (title and table) with:

## AP-5 Appendix D rows by tier and task
`tasks.md` section 7.1 is the only register of tiers and of the task that proves each row. This section holds no tier of its own. The architect's first classification (handoff 022) differs in these places, all settled in `tasks.md` section 7.3 and confirmed by Security (handoff 026, section 5):
- E-36: only the third row (2,000 query pairs) is P0 (Task 16, D1); the first two rows are P1 pre-application answers (Task 49).
- S-09: the 20-iteration run is P0 (Task 32, D2); 200 iterations are P1 (Task 51).
- Release sweep: Increment 3, P0, Task 42 (D3), not Increment 4.
- E-34: row 1 is P0 with the body cap (Tasks 06, 08, 15, 16); rows 2 to 7 are P1 (Task 50). T-14b belongs to the strict-parsing family (P0).
- E-04 is an Origin check at gate stage 3 and is proved in Task 08, not after Task 15.
- The security headers and Host rows start in Task 06, the container error path rows in Task 08 and Task 17.

### B-5 AP-6 last paragraph (Liaison F-11)
Replace "Fallback if C-11 fails: stored key plus index, a new Sponsor decision. `ddl-auto: validate` on STRICT and CHECK is proved in Task 04 (fallback: `typeof` CHECKs)." with:
Fallback if C-11 fails: stored key plus index. It changes `schema.sql` and is ASK FIRST: the task stops and requests Sponsor approval. `ddl-auto: validate` on STRICT and CHECK is proved in Task 04; its fallback (`typeof` CHECKs) also changes `schema.sql` and is ASK FIRST in the same way. The existing-file check (AP-12.1) compares the stored DDL with this statement, so a change here changes that check.

### B-6 New section AP-12 (SEC-F-068, 069, 071; Liaison F-11, F-14)

## AP-12 SQLite start-up details

### AP-12.1 `StoredLibraryValidator` (read-only, `query_only` ON, `trusted_schema` OFF)
Order, each step failing closed with a fixed reason:
1. **Structure.** `SELECT name, type, sql FROM sqlite_master`: exactly one table `books`; the only other entry allowed is the primary key auto-index; no view, trigger, virtual table or other index. The stored `sql` of `books`, normalised (comments dropped, white space runs collapsed to one space, `IF NOT EXISTS` removed because SQLite stores `CREATE TABLE books`), equals the `schema.sql` statement normalised by the same function. Task 04 proves the round trip; if SQLite stores another text, the comparison becomes a check of each named constraint (`ck_books_*`) against the AP-6 text, decided in Task 04.
2. **`quick_check`** only after step 1, because it evaluates the CHECK expressions and indexes of the stored schema.
3. **Rows, bounded.** A first query selects explicit columns `id`, `typeof(title)`, `typeof(author)`, `typeof(status)`, `length(CAST(title AS BLOB))`, `length(CAST(author AS BLOB))` with `LIMIT 5001`; more than 5,000 rows, a non-text cell, or a cell over the byte bound (2,000 bytes for a title, 800 for an author, 36 for an id, 24 for a date) is refused before any text is fetched. Only then are the rows read one at a time (no list of all rows). Lengths are counted in code points in Java (SQLite `length()` stops at U+0000 and is not trusted for this); a cell containing U+0000 is refused. Formats, INV-1 to INV-4 are checked on each row.
4. **Reasons.** A fixed enum text per failure (for example "stored library: schema differs", "stored library: too many books", "stored library: invalid row"); never a cell value, id, path component beyond the configured file name, or SQLite message text. The same rule applies to the log.
Task 34 AC2 adds: table without CHECKs, an extra index, NUL inside a title, one oversized cell, `LIMIT` case with 5,001 rows. **TASKS**

### AP-12.2 Claim, pool and file descriptors
- The data source that Hikari wraps opens each physical connection, applies all settings and then the claim (`BEGIN EXCLUSIVE`, `COMMIT`). Hikari: `maximum-pool-size` 1, `minimum-idle` 1, `idle-timeout` 0, `keepalive-time` 0, `max-lifetime` 0.
- POSIX advisory locks are dropped when any descriptor of the file is closed in the same process. After the claim the application opens no stream on the database file; `stat`-type calls only. Tests that hash the file do so only while no instance holds it (after the child exits).
- Task 03 proves it with two processes: a test main (`ClaimProbeMain`, test sources) in a child JVM holds the claim; the test then tries a claim from another process and expects "database is locked"; the test also forces an eviction (`softEvictConnections`), reads every setting back and repeats the second-process attempt. One connection pair in one JVM is not accepted as proof. If the claim does not behave, the no-op write transaction is decided in Task 03, before Increment 2 depends on it. The full-application proof stays C-12 in Task 34.

### AP-12.3 Native statements (inventory for the constitution NEVER list)
| Where | Statement | Why not JPA |
|---|---|---|
| `SqliteDataSourceProvider` | `PRAGMA` settings, `BEGIN EXCLUSIVE`, `COMMIT` | connection configuration before any entity manager |
| `StoredLibraryValidator` | `SELECT name, type, sql FROM sqlite_master`; `PRAGMA quick_check`; the bounded row query of AP-12.1; `PRAGMA query_only` | must run before `schema.sql` and Hibernate, on untrusted data |
| `BookRepository` | none native; `BookKeyRow` is a JPQL constructor projection | duplicate scan |
A grep gate in Task 42 asserts that no other `@Query(nativeQuery = true)`, `createNativeQuery` or JDBC `Statement` exists under `backend/src/main` outside `config.sqlite`.

### AP-12.4 Memory
The README run command is `java -Xmx512m -jar ...` and the C-11 (c) JVM uses the same value; the final number is taken from the Task 41 measurement and recorded in `plan.md`. Lists are written to the response stream by the converter (plan D7).

### B-7 AP-9.2 (stale "task F0.2"; backend-visible part only; frontend owns the wording of the rest)
Replace "**Open point to verify in task F0.2:**" with "**Open point to verify in Task 13:**" and, in the same paragraph, delete "The Liaison should decide whether the launcher counts as "global-setup" for SC-33 (Q-F7)." (handled by the amendment of SC-33, SPONSOR item S-1). Add `harness.spec.ts` to the AP-9.1 `tests/` list (frontend developer).

### B-8 AP-9.6 (Liaison F-12)
Replace the heading and first row cells: title "AP-9.6 Tiers of the UI criteria: see `tasks.md` section 7.2 (the register)". Delete the table, or keep it only as a pointer; in any case replace "US-011.1 to .11 (all; .2 is M-1)" with "US-011.1 to .10 (.2 is M-1)". (Frontend developer applies the same correction if the table is kept.)

### B-9 AP-10 (stale ids, Liaison F-22 a; Node pin; axe, F-09)
In the first column "Task": F0.1 becomes Task 10 (Node, `@playwright/test`, Tests as TypeScript, `@playwright/mcp`); F0.3 becomes Task 11; F0.4 becomes Task 10 (MCP pin) and Task 11 (tool names used by the client); F3.1 becomes Task 43. Row Node: replace "created by whichever of Task 1 or F0.1 comes first; the other task only checks it" with "created by Task 10; Task 01 and Task 14 only check it". Row `@axe-core/playwright`: replace "(an isolated test context ... this needs a Sponsor and Security decision)" with "(if the scan needs `bypassCSP`, it is set only on a dedicated context created inside `us-014-accessibility.spec.ts`, never in `playwright.config.ts`, never for a test that uses the `guard` fixture for CSP assertions; the application CSP is never relaxed, Sponsor decision 2026-11-02; first try `page.evaluate` without any bypass)". The frontend developer owns the final wording.

### B-10 AP-11 (stale ids)
Replace "A1, A2, F4.4, F5, F6" by the task numbers the frontend developer assigns (A1 = Task 09 catalog; A2 = Maven resources entry, Task 01 or Task 09 by `tasks.md`; F4.4 = Tasks 29 and 44; F5 and F6 = Task 10 and the notes of Task 12). The frontend developer confirms.

### B-11 AP-1 intro sentence (Liaison F-22 f)
Add under the title: "This appendix is part of the plan: the normative rules are repeated in `plan.md` and win on any difference. Tier tables are not repeated here (AP-5)."

### B-12 Proposed cuts in plan.md to pay for the additions
(a) Section 7 closing "Cost:" paragraph shortens to one sentence: "About 33.5 h for Increments 0 to 3 plus Tasks 49 and 50 (about 40.5 h with rework), accepted by the Sponsor on 2026-11-02 against the 6 to 8 h of DEC-022." This also fixes Liaison F-08 (plan section 7 says "about 32 h"). (b) Section 6 row "3": "after Tasks 49 and 50" stays; delete the sentence "Backend and frontend tasks run in parallel ..." (it repeats `tasks.md`).

---

## C. Mirror in tasks.md (PM; named so the plans do not drift)
- Task 05: add `config/sqlite/StartupPolicy.java` and `StartupPolicyTest` (bound address, URL form with no parameters, path configurable); remove it from Task 17 and keep `EnvelopeResponseWrapper` there. Estimate of Task 05 goes up (about 35 minutes proposed); Task 17 down.
- Task 06: add `config/ErrorReportConfig.java` (unconditional); advice mapping the MVC framework exceptions; Task 17 line "(3) a Tomcat customizer only if the error valve needs it" is deleted.
- Task 08: AC1 names the 404 and 500 triggers that exist at `v0.1`; new AC: raw-socket check that an invalid request line, `FOO` and an oversize header answer in {400, 405, 414, 431, 501} with no `Tomcat`, no version and no HTML in the body; list `application.yml` (edit) for the wrong-key correction (F-20).
- Task 03: AC3 becomes the child-JVM proof of AP-12.2 plus the forced-eviction read-back; the pool settings of A-5; add `ClaimProbeMain` to test files; note "first failed spike stops the task for a split".
- Task 09: replace the test "fixture directory chosen by property" with "catalog built with a fixture directory through its constructor"; no property in `application.yml`; add `StaticContentConfig`.
- Task 15: AC4 says `RouteTableTest` fails on any difference between the table and the mappings that exist (two routes); Task 31: add `config/transport/RouteTable.java` (edit) to production, `Depends on` 15 and 16, AC "RouteTableTest still passes with seven routes".
- Task 16: AC2 gains the `INVALID_STATUS` rule for the list (exactly `unread`, `reading`, `completed`); AC4 gains the drip variants (A-2, B-3); Task 23 AC3 keeps filtering and order only.
- Task 22: add `error/GlobalExceptionHandler.java` (edit) and an AC that `ApiException` becomes its code and status and the duplicate carries `details.existing_book` through the factory.
- Task 34: AC1 drops "a swap between check and open is detected" or records it as a documented limit (SPONSOR D-5); AC2 adds the AP-12.1 cases; AC3 gets the four journal cases of A-6; split proposal 34a/34b (N-6).
- Task 41: AC4 gets the pass criterion of X-12 (`-Xmx`, no `OutOfMemoryError`, serves afterwards).
- Task 42: grep gate of AP-12.3; SC-20 evidence for `e2e/` (Security, section 5).
- Task 49: asserts only what the spec allows for a container answer; it does not assert the `Allow` list (SEC-F-079); the configuration producing "no product, no version" is Task 06, tested at `v0.1` in Task 08.
- Task 17: test that an error dispatch cannot bypass the security headers.
- README (Task 14, frontend): the `-Xmx` run command and the sentence that `v0.1` and `v0.2` databases are to be deleted before Increment 1 (SEC-F-077).

## D. Sponsor matters (listed, not decided)
- S-1 Amendment of the SC-33 sentence in `constitution.md` and `security.md` for the launcher (Liaison F-10; Security SEC-F-070 ruling). Needed before Task 13.
- S-2 Whether `SqliteDataSourceProvider` setting the per-connection keys through the wrapped data source (not literally "in the JDBC URL or `connection-init-sql`", constitution line 119) is accepted as the same mechanism, or whether an amendment is wanted. The architect's reading: same keys, same effect, and necessary because the claim cannot be one `connection-init-sql` statement.
- S-3 Approval at the time, never before, of the two `schema.sql` fallbacks (typeof CHECKs, stored key and index). The plan now says "stop and request approval".
- S-4 Residual risk R-20 (hot journal planted next to an invalid file): accept or not; and the CR-9 closing clause proposed by the Liaison (F-14).
- S-5 `-Xmx512m` as a run requirement in the README (a deployment rule, value to be fixed by the Task 41 measurement), and what happens if Task 41 cannot pass (X-12).
- S-6 If the Task 16 drip test shows a connection surviving, whether to add a total read deadline or accept the limit (SEC-F-073).
- S-7 Whether `max-swallow-size 16KiB` needs approval as a limit next to SC-06 (it is not one of the listed SC-06 values; Security proposed it).
- S-8 Task 34 "swap between check and open": drop or document as limit (A-09 forbids a test-only switch).
- S-9 Rejecting a stored cell with U+0000 is a validator rule that US-009.5 does not state; PM and Security confirm it is covered by "invalid format".

## E. Assumptions to prove (not facts)
Tomcat 11 error valve properties via a `TomcatContextCustomizer`/`ErrorReportValve` setters; the 404 trigger for an unmapped path with the resource handler off (Task 08 states which one works); the stored DDL text of `STRICT` tables after `CREATE TABLE IF NOT EXISTS`; `softEvictConnections` re-running the connection init; `max-swallow-size` property name in Boot 4. Each is proved in the named task and a wrong key is corrected there, not dropped.
