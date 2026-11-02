# Handoff 022: Backend plan, round 2 (Phase 3)
- From: java
- To: liaison
- Phase: 3   Round: 2 of 3
- Artifacts: `docs/sdd/constitution.md` v1.1, `docs/sdd/spec.md` v0.4.1, `docs/sdd/security.md`, `docs/sdd/delivery-strategy.md` v1.0, handoffs 020 (my round 1) and 021 (frontend plan); targets: the backend section of `docs/sdd/plan.md` (Part A), `docs/sdd/plan-appendix.md` (the APPENDIX at the end of Part A), the frontend answers (Part B), the input of `tasks.md` (Part C) and the risk list (Part D)
- Type: REVIEW_REQUEST

## Summary
Round 1 is replaced, not patched. The Sponsor's seven decisions of 2026-10-30 are applied: spec frozen; rollback-journal SQLite without WAL; the proposed schema approved; own static controller; Tomcat limits approved; `config` sub-packages; TRACE answered by the container. What changed in size: the plan section is about 2.5 pages (round 1: about 9); the class table, the long pipeline table and the test-tier table moved to the appendix; the increment map has **27 tasks** (round 1: 45): Increment 0 has 8, Increments 1 to 3 have 16, Increment 4 has 3. Honest effort: Increments 0 to 3 are about **17 hours** of task time (21 hours with expected rework, range 17 to 24); Increment 4 is about 2.3 hours (3 with rework). That is more than the indicative 6 to 8 hours of DEC-022 by a factor of about three, and it cannot be reduced by planning, only by a spec change (see D, question S-1). Increment 0 alone (a running API with the Task 1 proofs) is about 5.8 hours (7 with rework). No application code was written; only this file was edited. No dependency or plugin outside the constitution is needed.

**Counting rule used for "files"** (please confirm, D question L-1): a task lists at most 4 production and resource files it creates or edits (`pom.xml` and the wrapper files count once as "build files"); the task's own test classes are listed after a plus sign (at most 3) and are part of the task. Counting tests too would need about 11 tasks in Increment 0.

---

# PART A: Backend plan (paste into `plan.md`, about 2.5 pages)

## A.1 Architecture and decisions

Layers and packages follow the constitution. The `config` package has three sub-packages (constitution 1.1): `config.transport` (request gate, policies, container error path, static catalog), `config.json` (strict body reading), `config.sqlite` (data source, startup checks). Only pure wiring classes end in `Config` and are excluded from JaCoCo (with `GloberBooksApplication`). Class list: AP-1.

| # | Decision | Rationale |
|---|---|---|
| D1 Pipeline | One `RequestGateFilter` at highest precedence: security headers, Host, Origin, Sec-Fetch-Site, Content-Type, body cap 16,384 (buffered), route and method, Accept, id, query, body on GET and DELETE. Then strict JSON, field validation, service (BR-012 steps 5 to 8). Table: AP-2 | Tomcat normalises `//`, `;` and dot segments before Spring routes; only the raw request target tells `/api/v1/books/%2e%2e` from a valid path (5.0, E-24) |
| D2 Error factory | One `ErrorBodyFactory` builds every body. Users: `GlobalExceptionHandler` (controller, validation, framework; the catch-all logs one stack trace, answers `INTERNAL_ERROR`), the gate through `ErrorResponseWriter`, `EnvelopeResponseWrapper` (any `sendError` inside the application). `ErrorMvcAutoConfiguration` excluded. Two body records (`ErrorBody`, `DuplicateErrorBody`), no `details` of type `Object` (SC-08). Tomcat's pre-application answers (TRACE included) stay generic (R-13) | Constitution; controllers never build bodies |
| D3 Transactions | Pool of one connection held for a whole `@Transactional` service method, each started `BEGIN IMMEDIATE`. Add = count, duplicate scan, insert in one transaction (BR-009, BR-013, INV-5, INV-7). Status and rating: load, call a `Book` method, commit; concurrent updates serialise, last write wins (DEC-018); CHECKs repeat INV-1 to INV-4. `open-in-view` off. E-21 and S-10 use a trigger the test creates in its own database: no seam in the shipped application | Atomic by construction; no capacity trigger |
| D4 Static | Spring's resource handler off (`add-mappings: false`). `StaticContentController` serves `StaticAssetCatalog`: every file under `classpath:/static/` at startup, extensions `.html`, `.css`, `.js` only (anything else, `*.test.js`, `*.map` or a dotfile fails the start). `/` equals `/index.html`; all else 404. No `ETag`, `Last-Modified`, `Accept-Ranges`; `Range` and conditionals ignored | The default handler adds them and answers 206 and 304, with no switch (5.8, E-30) |
| D5 Limits | Tomcat `threads.max 8`, `min-spare 2`, `max-connections 128`, `accept-count 128`, `max-swallow-size 64KB`, `connection-timeout 5s`; Hikari `connection-timeout 5000` | At most 8 requests wait for the connection; the rest queue in the OS, not in Hikari |
| D6 Time, ids | One `Clock` (only in `ClockConfig`), instants cut to ms; UUID v4 from an injected `Supplier<UUID>` in the service | Constitution; testable |

**SQLite settings** (built in `SqliteDataSourceProvider` from the validated path; configured URL stays `jdbc:sqlite:<file>`; not overridable)

| Setting | Value | Reason |
|---|---|---|
| `journal_mode` | default rollback journal, never changed; any other value stops the start | Commit is on disk before the response (M-3, US-009.2) |
| `synchronous` | `FULL` (explicit) | Protects the commit; one flush per commit |
| `locking_mode` | `EXCLUSIVE`; claim `BEGIN EXCLUSIVE` + `COMMIT` with no change, before any query | US-009.7; second instance fails "database is locked". Side effect: no other tool opens the file while running |
| Other | `transaction_mode=IMMEDIATE`, `busy_timeout` 5000, `foreign_keys` ON (URL); `trusted_schema` OFF (one `connection-init-sql`); open read-write without create; extension loading off (test) | SC-32, constitution |
| Pool | size 1, `max-lifetime 0` | A recycled connection would drop the claim |
| Temp dir | `org.sqlite.tmpdir` = `<data dir>/tmp` (0700) before first driver use | SC-32; dropped and recorded as a risk if the driver cannot load from it (B03) |

**US-009.5 without WAL: how "validate before write" holds.** Order at startup, before the first request:
1. `StartupPolicy` (non-loopback address or a URL other than `jdbc:sqlite:<file>`: stop; SC-39), then `DataDirectoryGuard` (create missing directory 0700 and file 0600; refuse symlink, other owner, group/other bits; POSIX).
2. A non-empty file: plain Java I/O reads the 100-byte header (magic string, valid page size, format bytes 18 and 19 equal to 1, size equals page size times page count). A WAL-marked or non-SQLite file is refused here; SQLite never touches it.
3. The pooled connection opens and takes the claim (`BEGIN EXCLUSIVE`, `COMMIT`: no page changes).
4. `StoredLibraryValidator` runs on that connection with `PRAGMA query_only = ON` (SQLite itself refuses writes): `quick_check`, exactly one table `books` (and its key autoindex), no view, trigger or virtual table, expected STRICT columns, every row against formats, code-point lengths, INV-1 to INV-4 and the 5,000 limit. A zero-length file skips this step.
5. Only then `query_only` goes OFF and `schema.sql` runs (a no-op on a valid database), then Hibernate `validate`. The data source bean is not published before step 4 passes, so `spring.sql.init` and Hibernate cannot run earlier.

Proof: `StartupRefusalIntegrationTest` hashes the file before and after each invalid case and requires an equal hash, a non-zero exit and a reason without book data. The one possible earlier write is SQLite's rollback of a hot `-journal` left by a killed instance (PM wording check, D: P-1).

## A.2 Technology and A.3 folders
No new dependency or plugin: starters, SQLite driver and dialect, test starters, JaCoCo 0.8.16, Checkstyle 14.3.0 with plugin 3.6.0 are approved. SpotBugs with Find Security Bugs: pin verified in B02 (publisher, Java 21). No enforcer plugin: the ban list is the committed `dependency:tree` plus `RuntimeClasspathBanTest`. Tests use JDK `HttpClient`, `Socket`, `ProcessBuilder`. `-Xlint:all -Werror`, `--release 21`, explicit Failsafe executions, jar `globerbooks.jar`, one resources entry `frontend/` to `static/` (no filtering; `*.test.js`, `*.map`, dotfiles excluded). Layout as the constitution map plus `config/checkstyle/`, `config/spotbugs/exclude.xml` (a justification per entry) and `src/test/.../support/`; no test resource configures the application (properties are set in code).

## A.4 Data model: `schema.sql` (approved by the Sponsor; executed at every start)

```sql
CREATE TABLE IF NOT EXISTS books (
    id             TEXT    NOT NULL PRIMARY KEY,
    title          TEXT    NOT NULL,
    author         TEXT    NOT NULL,
    status         TEXT    NOT NULL,
    rating         INTEGER,
    date_added     TEXT    NOT NULL,
    date_completed TEXT,
    CONSTRAINT ck_books_id CHECK (
        length(id) = 36 AND length(replace(id, '-', '')) = 32
        AND substr(id, 9, 1) = '-' AND substr(id, 14, 1) = '-'
        AND substr(id, 19, 1) = '-' AND substr(id, 24, 1) = '-'
        AND id NOT GLOB '*[^0-9a-f-]*'),
    CONSTRAINT ck_books_title CHECK (length(title) BETWEEN 1 AND 500),
    CONSTRAINT ck_books_author CHECK (length(author) BETWEEN 1 AND 200),
    CONSTRAINT ck_books_status CHECK (status IN ('unread', 'reading', 'completed')),
    CONSTRAINT ck_books_rating CHECK (rating IS NULL OR (rating BETWEEN 1 AND 5 AND status = 'completed')),
    CONSTRAINT ck_books_completed CHECK ((status = 'completed') = (date_completed IS NOT NULL)),
    CONSTRAINT ck_books_added CHECK (length(date_added) = 24 AND date_added GLOB
        '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]Z'),
    CONSTRAINT ck_books_done CHECK (date_completed IS NULL OR (length(date_completed) = 24 AND date_completed GLOB
        '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9].[0-9][0-9][0-9]Z'))
) STRICT;
```

STRICT keeps text out of `rating`; `length()` counts code points (BR-006); NFC and trimming stay in the service. No index but the primary key (at most 5,000 rows; the duplicate scan reads all anyway). The duplicate key is computed in Java from one query ordered by `date_added`, `id` (earliest match, DEC-005), never stored. No capacity trigger (US-009.5 would call our own database invalid). Fallback if C-11 fails: stored key plus index, a new Sponsor decision. `ddl-auto: validate` on STRICT and CHECK is proved in B04 (fallback: `typeof` CHECKs).

## A.5 API implementation plan
`BookController` has seven methods that only bind, trigger validation, pick the status and map DTOs; responses carry the seven book members (null written) as `application/json; charset=UTF-8`. Request records: `CreateBookRequest`, `StatusRequest`, `RatingRequest` (with `RatingValue`, which reads token and text only: integer `1` to `5` valid, every other number or `null` invalid, no numeric conversion). Code sources: the gate (transport codes, path and query); `StrictJsonBodyResolver` (body); Bean Validation and the advice (field codes; the service re-checks and throws the same exceptions); `BookService` (existence, state rules); advice catch-all (`INTERNAL_ERROR`). Detail: AP-3.

## A.6 Testing strategy
| Level | Runner | Content |
|---|---|---|
| Unit | Surefire | Services (repository mocked), policies, validators, error mapping |
| Slice | Surefire | `@WebMvcTest` (service mocked); `@DataJpaTest`, `replace = NONE`, SQLite file in `@TempDir` |
| Integration | Failsafe | `@SpringBootTest(RANDOM_PORT)`, temporary file database per class, no mocks of own code; restart, kill and second-instance tests start a separate JVM (`ProcessBuilder`) |
| Real port (SC-30) | Failsafe | `RawHttpClient` writes bytes verbatim on a `Socket` for every Appendix D row a client library would alter; `HttpClient` otherwise |

Rules: expected values from the spec; no sleeps (blocking reads with timeouts); `MutableClock` and a fixed id supplier where time or ties matter; concurrency tests assert the allowed set of outcomes; Unicode fixtures are built from code points, after a fixture check on the pinned JDK (AP-4). JaCoCo: bundle line coverage at least 80 % from merged Surefire and Failsafe data, check at `verify`; process-based tests are not counted. Tiers: AP-5; Task 1 proofs by task: Part C.

---

# APPENDIX (place in `docs/sdd/plan-appendix.md`; not part of the 2.5 pages)

## AP-1 Class by class (task that creates it in brackets)

| Package | Class | Responsibility |
|---|---|---|
| (root) | `GloberBooksApplication` (B01) | Entry point; excludes `ErrorMvcAutoConfiguration` |
| controller | `BookController` (B07, B18, B20) | Seven endpoints, no rules |
| | `StaticContentController` (B09) | Serves catalog entries |
| service | `BookService` (B07, B18, B20) | All use cases, one `@Transactional` method each |
| | `TextPolicy` (B13, B14) | BR-006: trim (Unicode White_Space), NFC, code-point length, categories, mark runs, joiners; nested problem enum, no framework types |
| | `DuplicateKey` (B17) | BR-003 key: NFKC, `toLowerCase(Locale.ROOT)`, collapse white space, trim; ASCII fast path |
| repository | `BookRepository` (B04, B18), `BookKeyRow` (B18) | Spring Data; key-row projection for the scan |
| domain | `Book` (B04, B19), `BookStatus` (B04) | Entity without public setters (`changeStatus`, `assignRating`, `clearRating`); enum with wire value, `fromWire` returning `Optional`, nested converter; instant converter nested in `Book` |
| dto | `CreateBookRequest` (B07, B15), `BookListResponse` (B07), `BookResponse` (B04), `StatusRequest`, `RatingRequest`, `RatingValue` (B19, deserializer nested) | Records |
| error | `ErrorCode`, `ErrorBody` (with nested `FieldProblem`), `ErrorBodyFactory`, `ErrorResponseWriter` (B05) | 16 codes with status; the one factory; writer for filter and wrapper |
| | `GlobalExceptionHandler` (B06, B15, B17) | `@RestControllerAdvice`, explicit JSON type |
| | `ApiException`, `DuplicateBookException`, `DuplicateErrorBody` (B17) | Service exceptions with code; duplicate carries the earliest match |
| config | `ClockConfig` (B03), `ValidatorConfig` (B15), `WebMvcConfig` (B16), `BookText` (B15, annotation with nested validator) | Wiring (no EL interpolator) and the BR-006 constraint |
| config.transport | `RequestGateFilter` (B06, B10, B11), `HeaderPolicy` (B06: Host, Origin, Sec-Fetch-Site, Content-Type grammar), `BoundedBodyRequest` (B06, B11) | Filter and capped buffered body; security headers set inside the filter |
| | `RequestTarget`, `RouteTable`, `AcceptPolicy` (B10), `QueryPolicy` (B11) | Raw-target parser, route and method table, Accept and query grammars |
| | `EnvelopeResponseWrapper` (B12), `StaticAssetCatalog` (B09) | `sendError` to envelope; fixed served paths |
| config.json | `JacksonConfig`, `StrictJsonBodyResolver`, `StrictStringDeserializer` (B16) | Strict UTF-8 decode and parse, duplicate and surrogate checks, read constraints |
| config.sqlite | `SqliteDataSourceProvider` (B03, B23), `StartupPolicy` (B12), `DataDirectoryGuard`, `StoredLibraryValidator`, `StartupFailureAnalyzer` (B23) | Data source with settings and claim; fail-closed start; directory trust; stored-data validation; clean console reason |

Test support under `src/test/.../support/`: `RawHttpClient` (B08), `LibrarySeeder` (B18), `UnicodeFixtures` (B13), `FaultInjection` (B21), `AppProcess` (B22), `MutableClock` (when first needed).

## AP-2 Request pipeline (BR-012); a failure ends the request through the factory

| # | Stage | Where | Failure |
|---|---|---|---|
| 0 | Request line and header parsing, 8 KiB header, duplicate Host, bad Content-Length, TRACE and unknown methods | Tomcat | pre-application answer (400, 405, 414, 431, 501, 505), no product |
| 1 | Security headers set (re-applied on `reset` and `sendError`) | gate | none |
| 2 | Host: raw header lower-cased equals `localhost:<port>` or `127.0.0.1:<port>` (`<port>` = local port); `[::1]:<port>` only when bound to `::1`; absent or repeated fails | gate | 400 `INVALID_HOST` |
| 3 | `Origin`, then `Sec-Fetch-Site` | gate | 403 `FORBIDDEN` |
| 4 | Content-Type grammar for every method except GET and HEAD, body or not | gate | 415 |
| 5 | Body cap 16,384: declared length checked before any read (no `100 Continue`); chunked and unread bodies read into a buffer and stopped at byte 16,385; `Connection: close` on early rejection | gate | 413 |
| 6 | Route and method from the raw target, then `Accept` on `/api/v1/` | gate | 404, 405 + `Allow`, 406 |
| 7 | Id: raw segment, lowercase 36-character UUID | gate | 400 `MALFORMED_REQUEST` |
| 8 | Query grammar, then on the list the `status` value (decoded value passed as request attribute) | gate | 400 `MALFORMED_REQUEST`, `INVALID_STATUS` |
| 9 | Body bytes on GET or DELETE | gate | 400 `MALFORMED_REQUEST` |
| 10 | Strict UTF-8 decode and parse; unknown, duplicate, wrong-type, surrogate, limits | `StrictJsonBodyResolver` | 400 `MALFORMED_REQUEST` |
| 11 | Field values (Bean Validation, code points) | MVC | 400 `VALIDATION_ERROR`, `INVALID_STATUS`, `INVALID_RATING` |
| 12 | Existence, `RATING_NOT_ALLOWED`, `LIBRARY_FULL`, `DUPLICATE_BOOK` (service re-checks stage 11 first) | `BookService` | 404, 400, 409 |

Spring's `CharacterEncodingFilter` is disabled so the gate is first. The gate never calls `getParameter*`. A `catch Throwable` around the chain answers `INTERNAL_ERROR`. Tomcat answers E-10 (two), E-20, E-28 (TRACE: 405 before the application), E-36 (two), E-37 and the container-rejected rows of E-15, E-24, E-24b, E-25; tests on raw sockets assert only what the spec allows (status in 400, 405, 414, 431, 501, 505; no product, version or echo; the application serves the next request). E-22 is the connector's 5 s timeout, asserted by configuration in B08 and live in B11.

## AP-3 How the codes are produced; strict JSON
- `MALFORMED_REQUEST`: gate stages 7 to 9; `StrictJsonBodyResolver` (decode and parse failures, limits, unknown or duplicate member, wrong type, surrogate); the advice for any remaining Jackson or not-readable exception.
- `VALIDATION_ERROR`, `INVALID_STATUS`, `INVALID_RATING`: the advice sorts entries `title`, `author`, `status` and uses the single-field codes only when exactly that field failed (BR-012 step 6); the service repeats the checks and throws the same exception type.
- `DuplicateBookException` carries the earliest match for the `details.existing_book` of the 409.
- Rating literal: an integer token whose text is exactly `1` to `5` is valid; any other integer token (`0`, `-0`, 100 digits), any float token (`4.9`, `5.0`, `1e0`, `1e999999999`) and `null` become the invalid marker with no numeric conversion (T-17, T-31); other token types are mismatches (MALFORMED); a literal over 100 characters hits the read constraint; `01`, `+5`, `NaN`, `5.`, `.5`, `5e` are refused by the parser (T-32).
- Strict JSON: reporting UTF-8 decoder first (BOM, overlong forms, encoded surrogates cannot be auto-detected), then the mapper with unknown properties failing, trailing tokens failing, duplicate names failing after unescaping, no scalar coercion, no float-to-integer, no numeric enums, no default typing; read constraints: depth 16, string 16,384, name 64, number 100, document 16,384; every failure is `MALFORMED_REQUEST` within 300 ms (T-31, never cut). Boot 4 and Jackson 3 key names are proved by tests in B16 (SC-31); a key the framework does not recognise is corrected there, never dropped.

## AP-4 Unicode fixture check
`UnicodeFixtureCheckTest` (B13) runs on the pinned JDK before any rule test and asserts, independently of `TextPolicy`: JDK 21.0.12 or later; Unicode 15.0 tables (U+1E030 assigned, U+2FFC unassigned); the category of every code point the spec names; the NFC result of each T-24b and T-25 input ("e" plus U+0301 composes, "x" plus U+0301 does not, which is why the spec uses base "x"); run lengths counted by hand. A mismatch stops the task and is reported to the PM.

## AP-5 Appendix D rows by tier and task

Changes against round 1: rows the spec's never-cut sentence names (E-22, E-33, T-31, C-11 with its assertions, the 2 s add bound) and rows that are the only proof of a Must acceptance criterion (C-12 for US-009.7, C-13 for US-009.2, C-08 for INV-7) are now P0 and scheduled in Increments 1 to 3. Real-port repeats of rules whose logic is already proved by unit tests stay P1.

| Family | Rows | Tier | Task |
|---|---|---|---|
| Envelope and headers on every response class, 500 without detail | E-23, E-21 | P0 | B08, B21 |
| Host, Origin, Fetch-Metadata | E-04, E-10 (first five), E-11 to E-14 | P0 | B08 (E-04 after B10) |
| Content type, body cap, early rejection | E-05, E-07, E-08, E-09, E-32, E-31 (`Expect`), **E-33**, **E-22 live** | P0 | B08, B10, B11 |
| Routing, methods, Accept, query, id | E-01 to E-03, E-06, E-17 to E-19, E-24, E-26, E-27, E-29, E-38 | P0 | B10, B11 |
| Static content | E-15 (application rows), E-16, E-30, E-39 | P0 | B09 |
| Strict parsing and value domains | T-04, T-05, T-14 to T-21, T-27 to T-30, T-32, T-34 to T-40, **T-31** | P0 | B15, B16, B19, B20 |
| Text policy and injection payloads | T-01 to T-03, T-06 to T-13, T-22, **T-23 to T-26, T-24b** (BR-006 is a business rule: unit level P0) | P0 | B13, B14 |
| Capacity, duplicates, concurrency | C-01 to C-04, C-06, C-07, C-09, C-10, **C-05, C-08, C-11 (a) to (c)** | P0 | B18, B21, B24 |
| Invariant sequences | S-01 to S-07, S-10, S-11, S-08 (20 iterations) | P0 | B20, B21 |
| Durability and second instance | C-13 (5 iterations), **C-12** | P0 | B22, B23 |
| Real-port repeat of hidden-channel text rules | T-23 to T-26, T-24b | P1 | B26 |
| Grammar corners | E-34 (rows 2 to 7), E-35, E-24b, E-25 (application rows) | P1 | B25 |
| Pre-application answers, TRACE, Upgrade | E-10 (last two), E-20, E-25 and E-15 container rows, E-28, E-31 (Upgrade rows), E-36, E-37 | P1 | B25 |
| Log injection | T-33 (the `%replace` pattern is asserted by configuration in B08) | P1 | B26 |
| Long runs | C-13 (50 iterations), S-08 (200), S-09 | P1 | B27 |
| Accepted limits | spec Section 6 | P2 | documented |

---

# PART B: Answers to the frontend plan (handoff 021, F6.4 and F1.2)

| Question | Answer |
|---|---|
| A1, F6.4-1 Served-path rule | The served set is exactly the files found under classpath `static/` at startup: `/index.html` and `/` (same bytes) and every `.css` and `.js` file at its relative path (`/css/styles.css`, `/js/main.js`, `/js/api/http.js`, and so on). Directories (`/js/`, `/css/`), the favicon and everything else are 404 in the envelope. Modules may be added freely: there is no name list to maintain. The extensions allowed are `.html`, `.css`, `.js`; any other file under `static/` stops the start, so add an extension to the plan first |
| A2, F6.4-3 Maven mapping | Confirmed: one `<resources>` entry copies `frontend/` to `static/`, filtering off, `**/*.test.js`, `**/*.map` and dotfiles excluded. That covers `css/tokens.test.js` and `js/sources.test.js`. The catalog also refuses such names if they ever reach `static/`. No `package.json` under `frontend/` is needed |
| A3 Content types | `.html` `text/html;charset=UTF-8`, `.css` `text/css;charset=UTF-8`, `.js` `text/javascript;charset=UTF-8` (the JavaScript module type browsers require). Asserted per asset in B09 |
| A4, A5 Static headers | No `ETag`, `Last-Modified` or `Accept-Ranges`; `Range`, `If-None-Match`, `If-Modified-Since` ignored (200, full body); all constitution security headers incl. `Cache-Control: no-store`. A non-empty query on any asset or `/` is 400 `MALFORMED_REQUEST` (`/index.html?` with an empty query is 200); a body byte on a static GET is 400. Use no cache-busting query and no `<link rel=icon>` (`/favicon.ico` is 404) |
| A7, F6.4-2 Run | Jar `backend/target/globerbooks.jar` (built by `cd backend && ./mvnw -DskipTests package`), started with `java -jar` and the three environment variables `SERVER_ADDRESS=127.0.0.1`, `SERVER_PORT=18080`, `SPRING_DATASOURCE_URL=jdbc:sqlite:<absolute path inside the temporary directory>`. The launcher must create the directory 0700 (the application also creates a missing one). The database file is locked exclusively while the application runs: the E2E code must not open it. Readiness: `GET http://127.0.0.1:18080/api/v1/books` returns 200 (exists from B07; `GET /` works from B09). Startup budget: 90 s as proposed; the design target is under 15 s with 5,000 books |
| F6.4-4 Contract final before each increment | The spec text (5.0 to 5.9, Appendix A) is frozen at 0.4.1, so the clauses are already final; what the frontend waits for is merged backend code. Table below |
| F6.4-5 `DELETE` with JSON content type, no body | Accepted. The gate requires the `Content-Type` header on DELETE (E-07) and then reads at most one byte: no `Content-Length`, `Content-Length: 0` and a zero-length chunked body all pass; any byte (a space too) is 400 (E-18). Proved in B20 with the JDK client and with raw requests |
| F6.4-6 Origin, Sec-Fetch-Site, host name | A browser `Origin` of `http://127.0.0.1:18080` or `http://localhost:18080` on POST, PUT and DELETE passes, as does `Sec-Fetch-Site: same-origin` and `none`; `Origin: null` (a page from `file://`) and any other origin or port is 403 `FORBIDDEN` (E-12 to E-14). `localhost:18080` behaves exactly like `127.0.0.1:18080` (E-11). Use the literal `127.0.0.1` in the E2E `baseURL`: `localhost` may resolve to `::1`, where nothing listens |

**Earliest merged backend task per frontend task**

| Frontend task | Needs merged | Reason |
|---|---|---|
| F0.2 launcher and config | B08 | Jar, variables, readiness URL work |
| F0.4 API client tests | spec only | Injected fetch; BE-0 contract is 5.0 to 5.2 and Appendix A |
| F0.5, F0.6 page and smoke 1 to 3 | B09 (and B08 for add and list) | Static serving, content types, headers |
| F1.3, F1.6 filter and list states | B18 | List filter and order |
| F1.4, F1.5, F1.7 add errors, duplicate dialog | B15, B16, B18 | Validation shapes, strict body, 409 bodies |
| F2.x status, rating, delete | B20 | Endpoints 5.3 to 5.7 |
| F3.x, F4.x | B24 | Capacity and measurements |

---

# PART C: Revised increment map (input for `tasks.md`)

Counting rule: see Summary. Minutes are implementation time with `./mvnw verify` runs. Every task also runs the Checkstyle and coverage gates.

**Increment 0: thin walking skeleton (8 tasks, about 5.8 hours). Result: the application starts, adds and lists books through the API into SQLite with headers and error handling, one real-port security class passes, Maven and quality gates are green.**

| ID | Title | Files (production and resource) + tests | Min | Depends | Spec / control ids |
|---|---|---|---|---|---|
| B01 | Maven scaffold and build proofs: wrapper checksum, strict checksums, Surefire/Failsafe split, merged JaCoCo gate (with a recorded deliberately failing run), `-Xlint -Werror`, committed dependency tree | build files, `GloberBooksApplication`, committed tree file + `BuildSplitProofIntegrationTest`, `RuntimeClasspathBanTest` | 45 | none | Task 1 proofs, SC-19, SC-36 |
| B02 | Quality gates: Checkstyle rules and regex bans (wall clock, `System.out`, `printStackTrace`, `select *`), SpotBugs and Find Security Bugs pins and exclusion file | `checkstyle.xml`, `suppressions.xml`, `exclude.xml`, build files | 45 | B01 | SC-07, SC-14, SC-19, SC-21, SC-27 |
| B03 | SQLite store: data source with pragmas, claim, tmpdir, 0700/0600 creation, `schema.sql`, `application.yml` with every mandatory property and the Tomcat/Hikari limits, `ClockConfig` | `SqliteDataSourceProvider`, `ClockConfig`, `schema.sql`, `application.yml` + `SqliteStoreIntegrationTest` | 45 | B01 | US-009.3 (create), US-009.7 (claim), M-3, SC-01, SC-13, SC-32, SC-37 pattern |
| B04 | Domain and repository: entity, status enum, repository, book response; `validate` on STRICT and CHECK (spike), CHECK rejections | `Book`, `BookStatus`, `BookRepository`, `BookResponse` + `BookRepositoryTest` | 40 | B03 | Section 4, INV-1 to INV-4, 5.0 book representation |
| B05 | Error foundation: codes, bodies, factory, writer | `ErrorCode`, `ErrorBody`, `ErrorBodyFactory`, `ErrorResponseWriter` + `ErrorBodyFactoryTest` | 35 | B03 | Appendix A, INV-6, SC-29 |
| B06 | Thin gate and advice: headers, Host, Origin, Sec-Fetch-Site, Content-Type, body cap (declared and chunked), exception advice with catch-all | `RequestGateFilter`, `HeaderPolicy`, `BoundedBodyRequest`, `GlobalExceptionHandler` + `HeaderPolicyTest`, `RequestGateFilterTest`, `GlobalExceptionHandlerTest` | 45 | B05 | BR-012 step 1, SC-03 to SC-06, SC-10, SC-11, SC-28 |
| B07 | Add and list API (thin): service, controller, request and list records; basic Bean Validation only (replaced in B15); `allow_duplicate` accepted, no effect until B18 | `BookService`, `BookController`, `CreateBookRequest`, `BookListResponse` + `BookServiceTest`, `BookControllerTest` | 45 | B04, B06 | US-001.1, US-001.9, US-002.1, 5.1, 5.2 success |
| B08 | Real-port baseline and mandatory-property proofs: Host, Origin, Fetch-Metadata, Content-Type, body cap (E-09 rows), headers on every response class, 500 by trigger, POST then GET round trip, checkpoint logs; every mandatory property proved effective | `RawHttpClient`, `TransportSecurityIntegrationTest`, `MandatoryPropertiesIntegrationTest` (tests only) | 45 | B07 | SC-30, E-04, E-07 to E-14, E-21, E-23, integration checkpoints |

Task 1 proofs by task: build split, JaCoCo, `-Xlint`, wrapper, dependency tree: B01; Checkstyle and SpotBugs pins: B02; mandatory properties and Jackson 3 key names for naming and unknown fields: B03 (set) and B08 (proved; strictness switches in B16); real-port class with Host, CSRF, header, body-cap and error-path cases: B08 (container error path: B12); integration checkpoints with logs: B08; Node and Playwright installs: frontend F0.1 and F0.3.

**Increment 1: core library (10 tasks, about 6.9 hours), all P0**

| ID | Title | Files + tests | Min | Depends | Spec / control ids |
|---|---|---|---|---|---|
| B09 | Static content: catalog, controller, add-mappings off, content types, no cache validators | `StaticContentController`, `StaticAssetCatalog`, `application.yml` + `StaticContentIntegrationTest` (fixture directory by property) | 40 | B08 | 5.8, SC-12, E-15 (application rows), E-16, E-30, E-39 |
| B10 | Route table, raw target, methods with `Allow`, Accept | `RequestTarget`, `RouteTable`, `AcceptPolicy`, `RequestGateFilter` + `RouteTableTest`, `AcceptPolicyTest` | 45 | B09 | 5.0 routing, methods, Accept, BR-012 step 2, E-01 to E-03, E-05, E-06, E-17, E-24, E-27, E-29, X-11 |
| B11 | Query, id and empty-body rules; transport limits | `QueryPolicy`, `RequestGateFilter`, `BoundedBodyRequest` + `QueryPolicyTest`, `TransportLimitsIntegrationTest` | 45 | B10 | BR-011, BR-012 steps 3 to 5 (GET, DELETE), E-18, E-19, E-22 live, E-26, E-31 (`Expect`), E-32, E-33, E-38, T-20, T-39 |
| B12 | Container error path and fail-closed start | `EnvelopeResponseWrapper`, `StartupPolicy` (a Tomcat customizer only if the error valve needs it) + `EnvelopeResponseWrapperTest`, `StartupPolicyTest` | 35 | B06 | SC-11, SC-29, SC-39, INV-6 |
| B13 | Text policy part 1 and the Unicode fixture check | `TextPolicy` + `TextPolicyTest`, `UnicodeFixtureCheckTest`, `UnicodeFixtures` | 45 | B01 | BR-006 items 1 to 6, T-01 to T-03, T-06 to T-13, T-22, T-26, SC-16 |
| B14 | Text policy part 2: visible content, mark runs, joiners | `TextPolicy` + `TextPolicyMarksTest`, `UnicodeFixtures` | 45 | B13 | BR-006 items 6b to 6d, T-23 to T-25, T-24b |
| B15 | Validation wiring and field-error mapping | `BookText`, `ValidatorConfig`, `GlobalExceptionHandler`, `CreateBookRequest` + `BookTextTest`, `GlobalExceptionHandlerTest` | 40 | B14, B06 | US-001.4 to .6, .10, BR-001, BR-012 step 6, T-09, T-13, T-34, T-35 |
| B16 | Strict body reading and parser limits | `JacksonConfig`, `StrictJsonBodyResolver`, `StrictStringDeserializer`, `WebMvcConfig` + `JacksonStrictnessTest`, `StrictJsonBodyResolverTest` | 45 | B07 | 5.0 strict parsing, BR-012 step 5, SC-31, SC-08, T-04, T-05, T-16, T-21, T-27 to T-31, T-39 |
| B17 | Duplicate key and service exceptions | `DuplicateKey`, `ApiException`, `DuplicateBookException`, `DuplicateErrorBody` + `DuplicateKeyTest` | 30 | B13 | BR-003, US-004.5, T-40, Appendix A |
| B18 | Add rules and list: capacity, duplicate scan, `allow_duplicate`, 409 bodies; list filter and order; end-to-end add stories | `BookService`, `BookRepository`, `BookKeyRow`, `BookController` + `BookServiceTest`, `DuplicateAndCapacityIntegrationTest`, `LibrarySeeder` | 45 | B15, B16, B17 | US-001, US-002, US-004, BR-003, BR-009, BR-010, INV-5, INV-7, C-01 to C-04, T-12, T-40 |

**Increment 2: progress, rating, durability (5 tasks, about 3.4 hours), all P0**

| ID | Title | Files + tests | Min | Depends | Spec / control ids |
|---|---|---|---|---|---|
| B19 | Book transitions and value types: leaving `completed`, rating only when completed, completion date, rating literal | `Book`, `RatingValue`, `RatingRequest`, `StatusRequest` + `BookTest`, `RatingValueDeserializerTest` | 35 | B16 | BR-002, BR-004, BR-007, BR-008, INV-1 to INV-4, T-17, T-18, T-32, T-37 |
| B20 | Get, delete, status, rating, clear rating: service, controller, story contract tests | `BookService`, `BookController` + `BookControllerTest`, `BookStoryIntegrationTest` | 45 | B18, B19 | US-003, US-006 to US-008, 5.3 to 5.7, BR-012 steps 7 and 8, S-01 to S-07, S-11, T-14, T-15, T-19, T-36, T-38 |
| B21 | Concurrency and fault injection | tests only: `ConcurrencyIntegrationTest`, `FaultInjection` | 40 | B20 | BR-013, C-06, C-07, C-09, C-10, S-08 (20), S-10, E-21 |
| B22 | Durability across restart and kill | tests only: `AppProcess`, `DurabilityIntegrationTest` | 40 | B20 | US-009.1, .2, M-3, C-13 (5 iterations) |
| B23 | Directory trust, stored-library validation, second instance, clean failure | `StoredLibraryValidator`, `DataDirectoryGuard`, `StartupFailureAnalyzer`, `SqliteDataSourceProvider` + `StoredLibraryValidatorTest`, `DataDirectoryGuardTest`, `StartupRefusalIntegrationTest` | 45 | B22 | US-009.3, .5, .6, .7, SC-13, SC-39, C-12 |

**Increment 3: interface quality, backend side (1 task, about 0.7 hours), P0**

| ID | Title | Files + tests | Min | Depends | Spec / control ids |
|---|---|---|---|---|---|
| B24 | Capacity and performance at maximum size: C-11 (a) to (c), C-05, C-08, 2 s add bound, no 500, heap check; tune limits (yml) if needed | `application.yml` (if tuning) + `CapacityPerformanceIntegrationTest` | 40 | B20 | M-2, SC-17, C-05, C-08, C-11, DEC-020, X-03, X-04 |

The re-check of headers and content types against the real frontend assets needs no task: B09's test discovers assets from the served page and runs unchanged whenever the frontend merges.

**Increment 4: hardening and wrap-up (3 tasks, about 2.3 hours), P1, time permitting**

| ID | Title | Files + tests | Min | Depends | Spec / control ids |
|---|---|---|---|---|---|
| B25 | Raw-socket run: pre-application answers, TRACE, Upgrade, header grammar corners, Accept and Content-Type corner rows at the real port | tests only: `PreApplicationIntegrationTest`, `GrammarCornersIntegrationTest` | 45 | B11 | E-10 (tail), E-20, E-24b, E-25, E-28, E-31 (Upgrade), E-34, E-35, E-36, E-37 |
| B26 | Real-port repeat of hidden-channel text rules and log hygiene | tests only: `TextAndParserIntegrationTest`, `LogHygieneIntegrationTest` | 45 | B14 | T-23 to T-26, T-24b, T-33, SC-37 |
| B27 | Long runs and backend sign-off: 50-iteration kill loop, S-08 at 200, S-09, SpotBugs triage, grep gates, tree against baseline, coverage review | exclusion file, notes + `DurabilityIntegrationTest`, `ConcurrencyIntegrationTest` edits | 45 | B22, B21 | C-13, S-08, S-09, SC-07, SC-14, SC-27, SC-36 |

**Rows that stay P0 although the tier table would have called them P1** (they touch the spec's never-cut sentence or are the only proof of a Must criterion): E-22 live and E-33 (B11), T-31 (B16), C-11 (a) to (c) with the no-500 and nothing-changed assertions and the 2 s add bound (B24), C-08 (B24, INV-7), C-05 (B24, size assertion; the time is recorded only), C-12 (B23, US-009.7), C-13 with 5 iterations (B22, US-009.2), E-30 (B09, the reason for the static controller), BR-006 items 6b to 6d at unit level (B13, B14; business rule). The security-header baseline and body-cap rows are P0 in B08.

**Effort**

| Scope | Tasks | Task time | With expected rework | Notes |
|---|---|---|---|---|
| Increment 0 | 8 | 5.75 h | about 7 h | Highest uncertainty: B01 (Maven 3.9.16 and plugin pins unverified), B02 (SpotBugs pins), B03 (driver URL keys, claim, tmpdir), B04 (STRICT with `validate`) |
| Increment 1 | 10 | 6.9 h | about 8 h | B16 (Jackson 3 key names) and B18 (scan cost) |
| Increment 2 | 5 | 3.4 h | about 4.5 h | B23 is the densest task; B20 the widest contract test |
| Increment 3 | 1 | 0.7 h | about 1 h | Failure of C-11 (c) escalates to the Sponsor, not to a weaker test |
| **Increments 0 to 3** | **24** | **16.75 h** | **about 21 h (range 17 to 24)** | Parallel frontend work shortens calendar time, not backend time |
| Increment 4 | 3 | 2.25 h | about 3 h | Nothing P0 depends on it |
| Total | 27 | 19 h | about 24 h | Round 1: 45 tasks, 18 to 22 h |

---

# PART D: Remaining risks and questions

## Risks and infeasible or costly points (updated)

| ID | Point | Handling |
|---|---|---|
| X-01, X-02, X-05 | TRACE row of E-28, Spring static handler, WAL ordering | **Closed** by Sponsor decisions 7, 4 and 2 (the PM edits E-28) |
| X-03 | C-11 (c): 50 adds plus 50 lists at 4,949 maximum-size books through one connection (a list reads about 14 MB; an add normalises up to 4,999 pairs). Passes only if waiting stays in the OS backlog and Hikari's 5 s is never reached. `synchronous=FULL` adds a flush per commit (milliseconds) | Measured in B24; if it fails, escalate to the Sponsor (no weakened test, no 503) |
| X-04 | Duplicate scan at 4,999 maximum-size books, no stored key: about 3.5 million code points per add; estimate 0.3 to 1 s warm against the 2 s bound | Smoke in B18, measured in B24; fallback is the key-column schema change (new decision) |
| X-06 | `ddl-auto: validate` with STRICT and CHECK on the community dialect is untested | B04; fallback `typeof` CHECKs |
| X-07 | `readOnly` transactions: the driver refuses `setReadOnly`; Spring ignores it | B07 asserts a read writes nothing |
| X-08 | Jackson 3 specifics (property keys, trailing-token and duplicate switches, exact 100-character boundary, record null handling) come from knowledge, not tests | B03/B08 for naming and unknown fields, B16 for the rest |
| X-10 | `org.sqlite.tmpdir` under the data directory (SC-32) | B03; if the driver cannot load from it the property is dropped and the risk recorded in `security.md` |
| X-11 | The gate's route table duplicates the MVC mappings | `RouteTableTest` (B10) enumerates the controllers' mappings and fails on any difference |
| N-1 | **Hot journal.** A crash in the middle of a commit leaves a `-journal` that SQLite rolls back on the first open; that is a write before validation | Restores the last committed state of a file that already passed the header check; wording check by the PM (P-1) |
| N-2 | **Claim semantics.** `BEGIN EXCLUSIVE` plus `COMMIT` with no change must keep the lock in `locking_mode=EXCLUSIVE` and write nothing | B03 proves it (second connection gets "database is locked"; file hash unchanged); fallback is a no-op write transaction on a pragma-free table, decided then |
| N-3 | **sqlite-jdbc URL keys** (`open_mode`, `transaction_mode`, `locking_mode`, `busy_timeout`) are from knowledge | B03 reads the effective values back with pragmas |
| N-4 | **Task 1 proof split.** The container error path (the wrapper) lands in B12, after the real-port class of B08; the rest of Task 1 is complete at B08 | Security to accept; otherwise B12 moves into Increment 0 and Increment 0 has 9 tasks |
| N-5 | **Page not served by Increment 0.** The delivery strategy's "done when" for Increment 0 includes the page; B09 is the first Increment 1 task | Tag `v0.1` after B09 merges (S-2) |
| N-6 | **Density.** B01, B06, B08, B16 and B23 are at the 45-minute ceiling; B20 has the widest contract test | If one overruns, split it; the map then has 28 or 29 tasks |
| N-7 | **Static catalog with no frontend yet.** Before `frontend/` merges, `static/` is empty and `/` is 404; B09 tests use a fixture directory set by property | No requirement on `index.html` at startup, so backend builds stay independent |
| N-8 | **Tomcat 8 threads** and slow clients: a slow body read holds a thread up to 5 s | E-22 live in B11 serves other requests meanwhile; limits tunable in B24 |
| N-9 | **Exclusive lock side effect**: the database cannot be inspected with another tool while the application runs; the README must say so | Documentation item for the wrap-up |
| N-10 | **Tier moves** (above) turn six P1 rows into P0 | Security and PM confirm (P-2, Sc-1) |

## Questions

| ID | To | Question |
|---|---|---|
| S-1 | **Sponsor** | The P0 backend is about 17 h of task time (21 with rework) against the indicative 6 to 8 h of DEC-022, and Increment 0 alone is about 6 to 7 h. Planning cannot close that gap; only a spec change could (for example dropping BR-006 items 6b to 6d or the Unicode-hardening rows, which are Must). Do you accept the figure and keep the spec frozen, or do you want a change-request round for scope? |
| S-2 | **Sponsor** | Is the tag `v0.1` placed after B09 (page served) as proposed, or after B08 (API only)? |
| P-1 | PM | US-009.5 says the bytes stay unchanged. Please add that SQLite's own rollback of a hot `-journal` left by a killed instance is not a violation, and that a file marked WAL counts as "not of the kind used by the application" (refused). Also confirm the edit of E-28 (TRACE: 405 before the application for both rows) |
| P-2 | PM, Security | Confirm the tier moves of AP-5 and the Increment 4 list (P1 rows scheduled after the release must still be reported open, as the spec's never-cut sentence requires) |
| Sc-1 | Security | Review: pipeline order, startup order (steps 1 to 5 above), `BEGIN EXCLUSIVE` claim, `query_only` validation, tier moves, Tomcat limits, wrapper placement (N-4) |
| L-1 | Liaison | Confirm the file-counting rule (production and resource files at most 4, own tests extra) and the sub-package names `config.transport`, `config.json`, `config.sqlite` |

## Requested response
Liaison: verdict on Part A and the appendix as the backend section of `plan.md` and `plan-appendix.md`, and on Parts C and D as input for `tasks.md`; findings by severity; round 3 only if a blocker remains. Security (through the orchestrator): design review as listed under Sc-1.
