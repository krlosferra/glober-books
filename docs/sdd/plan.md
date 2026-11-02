# GloberBooks Technical Plan

- Owner: Orchestrator (assembled from the Java architect's section, handoff 022, and the frontend developer's section, handoff 021; revised from handoffs 027, 028, 029); the PM owns `tasks.md`
- Version: 0.2 (Phase 3 draft, review round 2 of 3)
- Last updated: 2026-11-02
- Changelog: 0.2 (2026-11-02) applies round 1 findings (handoffs 025, 026) as written in handoffs 027 to 029: task ids follow `tasks.md` 0.5 (03a/03b, 23a/23b, 34a/34b); error report, limits, start order, claim and pool, validator order, native SQL paragraph, launcher, risks X-12 and N-10; details moved to AP-12; open Sponsor items listed at the end. 0.1: first draft.
- Inputs: `constitution.md` 1.1, `spec.md` 0.4.3 (change requests CR-1 to CR-9 pending), `security.md`, `delivery-strategy.md` 1.0, `tasks.md` 0.5
- Detail that does not fit three to five pages is in `plan-appendix.md` (class table, request pipeline, code sources, tiers by task, frontend logic tests, E2E layout, tooling checks, SQLite start-up details, open assumptions). The appendix is part of the plan; the plan never overrides the constitution or the spec.

## 1. Shape of the solution

One process: Spring Boot (JDK 21) bound to `127.0.0.1`, one embedded SQLite file in rollback-journal mode, and a static page served by the same process. The page is plain HTML, CSS and vanilla ES modules with no build step. Layers follow the constitution: `controller`, `service`, `repository`, `domain`, `dto`, `error`, `config`. Backend and frontend share only the API contract of `spec.md` (5.0 to 5.9, Appendix A).

## 2. Backend decisions

| # | Decision | Rationale |
|---|---|---|
| D1 Pipeline | One `RequestGateFilter` at highest precedence: security headers, Host, Origin, Sec-Fetch-Site, Content-Type, body cap (16,384 bytes, buffered), route and method, Accept, id, query, bytes on GET and DELETE. Then strict JSON, field validation, service. Order: AP-2 | Tomcat normalises `//`, `;` and dot segments before Spring routes; only the raw request target tells `/api/v1/books/%2e%2e` from a valid path |
| D2 Errors | One `ErrorBodyFactory` builds every body (`ErrorBody`, `DuplicateErrorBody`; no `details` of type `Object`). Users: `GlobalExceptionHandler`, the gate through `ErrorResponseWriter`, `EnvelopeResponseWrapper` (Task 17) for any `sendError`. `ErrorMvcAutoConfiguration` excluded. **From Task 06 on, unconditionally:** a Tomcat customizer sets the error report valve to `showReport=false` and `showServerInfo=false` so that no container page names the product or version, and the advice maps the MVC framework exceptions (no handler or resource, method not supported, media type not supported, not acceptable) to the envelope. Tomcat's pre-application answers (TRACE included) stay generic and carry no product or version | Constitution; controllers never build bodies; the container error page is never reachable in any tagged build |
| D3 Transactions | Pool of one connection held for a whole `@Transactional` service method, each started `BEGIN IMMEDIATE`. Add = count, duplicate scan, insert in one transaction. Status and rating: load, call a `Book` method, commit; last write wins. CHECK constraints repeat INV-1 to INV-4. `open-in-view` off. No capacity trigger | Atomic by construction |
| D4 Static | Spring's resource handler off. `StaticContentController` serves `StaticAssetCatalog`, which receives its root resource through its constructor; the production wiring (`StaticContentConfig`) passes `classpath:/static/` and nothing else, and no property, profile or environment variable changes the root. The catalog lists every `.html`, `.css` and `.js` under that root at startup (any other file there stops the start). The Task 09 test builds the catalog directly with a fixture directory. `/` equals `/index.html`; all else 404. No `ETag`, `Last-Modified`, `Accept-Ranges`; `Range` and conditionals ignored | The default handler adds them and answers 206 and 304 with no switch (5.8, E-30) |
| D5 Limits | Tomcat `threads.max 8`, `max-connections 128`, `accept-count 128`, `connection-timeout 5s`, `max-swallow-size 16KiB` (explicit, next to the others); Hikari `connection-timeout 5000` | At most 8 requests run. Up to 128 connections are accepted and those beyond the 8 threads wait in Tomcat's executor queue; only beyond `max-connections` does `accept-count` (the OS backlog) apply. Of the running requests one holds the database connection; the others wait at most 5 s for it and then answer the generic `500 INTERNAL_ERROR` (pool timeout, no detail). `connection-timeout 5s` is a per-read timeout, not a deadline for a whole request; the drip test of Task 16 decides whether a deadline in `BoundedBodyRequest` is needed. Any change of threads, connection limits or the Hikari timeout is ASK FIRST and returns to Security |
| D6 Time, ids | One `Clock` (only in `ClockConfig`), instants cut to milliseconds; UUID v4 from an injected `Supplier<UUID>` | Testable |
| D7 Response writing | A list is serialised by the message converter straight into the response output stream; no `byte[]` or `String` of the whole body is built. The service returns the DTO list after its transaction ends, so the connection is not held while a slow client reads | Bounds memory to the DTOs of the in-flight lists (X-12) and keeps the single connection free |

`config` has three sub-packages (constitution 1.1): `config.transport`, `config.json`, `config.sqlite`. Classes ending in `Config` are pure wiring and excluded from JaCoCo with `GloberBooksApplication`.

### SQLite settings and start order

Built in `SqliteDataSourceProvider` from the path that `StartupPolicy` (Task 03b) has validated. The URL form is fixed to `jdbc:sqlite:<file>` with no parameters; only `<file>` is configurable (`SPRING_DATASOURCE_URL` carries it, as the E2E launcher needs). Every per-connection setting is applied by the data source that the pool wraps, with the same keys as the sqlite-jdbc URL parameters and the same effect as `connection-init-sql` (constitution, Persistence); they are not in the URL so that the URL stays checkable.

| Setting | Value |
|---|---|
| `journal_mode` | default rollback journal, never changed; any other value stops the start |
| `synchronous` | `FULL`, explicit |
| `locking_mode` | `EXCLUSIVE`. The claim is `BEGIN EXCLUSIVE` + `COMMIT` with no change, **run for every physical connection, before any query on it** (connection init, together with all other settings); a second instance fails with "database is locked" |
| Others | `transaction_mode=IMMEDIATE`, `busy_timeout` 5000, `foreign_keys` ON, `trusted_schema` OFF, read-write without create, extension loading off |
| Pool | size 1: `maximum-pool-size` = `minimum-idle` = 1, `idle-timeout 0`, `keepalive-time 0`, `max-lifetime 0`, so the pool never retires or adds a connection on its own. A replacement after a fatal error repeats the claim and the settings (proved by a forced eviction and a read-back, Task 03a) |
| After the claim | The application only calls `stat`-type operations (exists, size, attributes) on the database path. Reads of the file by plain Java I/O happen before the claim (start step 2) or when no instance holds the file; closing any other descriptor of the file would release a POSIX lock (AP-12.2) |
| Temp dir | `org.sqlite.tmpdir` = `<data dir>/tmp` (0700); dropped and recorded as a risk if the driver cannot load from it |

Start order, before the first request (US-009.5 "validate before write" without WAL):
1. `StartupPolicy` (Task 03b): bound address not loopback, or URL not of the form `jdbc:sqlite:<file>` with no parameters: stop. Then `DataDirectoryGuard` (Task 34a): create directory 0700 and file 0600; refuse symlink, other owner, group or other bits.
2. A non-empty file: plain Java I/O reads the 100-byte header (magic string, page size, bytes 18 and 19 equal to 1; code in `SqliteDataSourceProvider`, Task 34b). A WAL-marked or non-SQLite file is refused here, before SQLite touches it, so no journal is applied to it. The check "size equals page size times page count" is skipped when a non-empty `<file>-journal` exists (the header count is valid only after a commit).
3. The pooled connection opens and takes the claim. SQLite rolls back a hot `-journal` here and nowhere else.
4. `StoredLibraryValidator` on that connection with `query_only` ON, in this order: (a) `sqlite_master` and the stored DDL, (b) `quick_check`, (c) the rows, bounded. Detail and refusal reasons: AP-12.1. A zero-length file skips this step.
5. Only then `query_only` goes OFF, `schema.sql` runs (a no-op on a valid database), then Hibernate `validate`.

`StartupRefusalIntegrationTest` hashes the file before and after each invalid case that has no journal and requires an equal hash, a non-zero exit and a fixed reason without book data. The only earlier write is SQLite's rollback of a hot `-journal` at step 3. The outcome (accept, or refuse with a non-zero exit) is judged on the file after that rollback; a journal that is not a valid journal of that file is ignored by SQLite and changes nothing (CR-9 closing clause; Task 34b AC3 has a case for each of: valid file with a genuine hot journal, invalid file with a genuine hot journal, planted garbage journal beside a valid and beside an invalid file).

### Data model (`schema.sql`, one `STRICT` table, executed at every start)

Table `books`: `id` (36-character lowercase UUID), `title` (1 to 500 code points), `author` (1 to 200), `status` (`unread`, `reading`, `completed`), `rating` (null or 1 to 5, only when completed), `date_added`, `date_completed` (24-character UTC text with milliseconds; `date_completed` present exactly when completed). Every rule is a named CHECK; the full DDL is in `plan-appendix.md` (AP-6). No index but the primary key: with at most 5,000 rows the duplicate scan reads all anyway. The duplicate key is computed in Java from one query ordered by `date_added`, `id` (earliest match) and never stored. Fallback if the capacity measurement fails: a stored key and an index. That changes `schema.sql` and is ASK FIRST: the task stops and requests Sponsor approval, it does not apply the fallback. The stored DDL of an existing file must equal the normalised statement of `schema.sql` (AP-12.1), so a file with the same columns but no CHECK constraints is refused.

**Native SQL and JDBC (justified, constitution NEVER list).** JPA and Spring Data are used for everything the API does. Raw JDBC and native statements exist in `config.sqlite` only, because they run before Hibernate and `schema.sql` or configure the connection, which JPA cannot do: (1) the pragmas and the claim (`journal_mode`, `synchronous`, `locking_mode`, `foreign_keys`, `busy_timeout`, `trusted_schema`, `query_only`, `BEGIN EXCLUSIVE`, `COMMIT`), run per connection by `SqliteDataSourceProvider`; (2) the read-only checks of `StoredLibraryValidator`: `sqlite_master` (`name`, `type`, `sql`), `quick_check`, and one row query with an explicit column list and `LIMIT 5001`. The validator cannot use entities: Hibernate maps only after validation and would load unchecked values. All statements are constants or take bound parameters, none contains `*` in a select list (`count(*)` is not a select list of columns and is avoided anyway). The duplicate scan is not native: it is a JPQL constructor projection with explicit fields (`BookKeyRow`). Inventory in AP-12.3.

### API implementation

`BookController` has seven methods that bind, trigger validation, choose the status and map DTOs. Request records: `CreateBookRequest`, `StatusRequest`, `RatingRequest` (with `RatingValue`: only the integer token `1` to `5` is valid, every other number or `null` is invalid, no numeric conversion). Strict JSON: reporting UTF-8 decoder first, then a mapper that fails on unknown or duplicate members, trailing tokens and coercion; read constraints depth 16, string 16,384, name 64, number 100, document 16,384; every failure is `MALFORMED_REQUEST` within 300 ms. Which component produces which code: AP-3.

## 3. Frontend decisions

Files under `frontend/` (about 16 sources, 9 test files, each file under 300 lines, each function under 50):
- `index.html`, `css/styles.css`, `js/main.js` (the only script element).
- `js/api/` (`http.js` is the only `fetch`; `booksApi.js` maps snake_case to camelCase once).
- `js/logic/` (pure, DOM-free, unit tested with `node:test`): `copy.js` (every exact UI string), `ids.js`, `text.js`, `format.js`, `failures.js` (5.9 table), `listState.js`.
- `js/ui/` (DOM only, no business rules): `dom.js`, `messages.js`, `dialogs.js`, `listView.js`, `entryView.js`, `entryActions.js`, `addForm.js`, `app.js`.

| Topic | Decision |
|---|---|
| Rendering | Clone one `<template>`; text only through `textContent`; no `innerHTML`, no inline style or handler, classes and `hidden` only (CSP `script-src 'self'; style-src 'self'`, Trusted Types) |
| State | Pure transitions in `listState.js`: filter, phase (`loading`, `loaded`, `failed`), entries, count, request sequence; stale list responses are ignored; the page never sorts |
| API client | Root-relative URLs through `bookPath(id)`; `Content-Type: application/json` on every non-GET including DELETE; `redirect: 'error'`, `cache: 'no-store'`; never throws for an HTTP outcome (`ok` or failure of kind `api`, `network`, `unreadable`); branches on `code` only; 15 second timeout counted as `network` (CR-4) |
| Controls | Native: `select` for status, radio groups for filter and rating, `<dialog>` for duplicate, status confirmation and delete |
| Accessibility | One `h1`, `lang`, named landmarks, a live region per page and per entry, focus rules per dialog result (AP-8), colour tokens at 4.5:1 text and 3:1 borders and focus ring, text with every status and error, containment rules for 500-character words and mark runs |
| Not built | Edit, sorting, search, pagination, undo, favicon, any local storage |

## 4. Interface agreements between the two sides

- Served set is exactly the `.html`, `.css` and `.js` files under `static/`; directories, favicon and everything else are 404 in the envelope; a query on an asset or on `/` is 400 (5.8, E-38).
- Maven copies `frontend/` to `static/` with one `<resources>` entry: no filtering, `**/*.test.js`, `**/*.map` and dotfiles excluded.
- Content types: `text/html;charset=UTF-8`, `text/css;charset=UTF-8`, `text/javascript;charset=UTF-8`. The page's own stylesheet and scripts are discovered at run time from the document (`link[rel=stylesheet]`, `script[src]`; module imports are not followed); asset names are not hard-coded in tests (spec 5.8).
- E2E runs the jar `backend/target/globerbooks.jar` through the launcher `e2e/support/start-server.mjs` (the `webServer.command`): bind, port and datasource URL are command-line arguments (`--server.address=127.0.0.1 --server.port=18080 --spring.datasource.url=jdbc:sqlite:<file>` inside a fresh 0700 `mkdtemp` directory), the child gets an allowlisted environment (`PATH`, `HOME`, `JAVA_HOME`, `LANG`, `TZ`), readiness is `GET /api/v1/books` returning 200, budget 90 s. The database is exclusively locked while the application runs, so the E2E code never opens it. A run with hostile ambient variables proves a decoy file is untouched (Task 13). The jar already accepts the three values as command-line arguments (the highest-precedence source).
- The `DELETE` sent by the browser has the JSON `Content-Type` and no body (accepted by the gate); a browser `Origin` of `http://127.0.0.1:18080` or `http://localhost:18080`, `Sec-Fetch-Site: same-origin` and `none` pass; `Origin: null` is 403. The E2E base URL uses the literal `127.0.0.1`.

## 5. Testing strategy

| Level | Runner | Content |
|---|---|---|
| Backend unit | Surefire | Services (repository mocked), policies, validators, error mapping |
| Backend slice | Surefire | `@WebMvcTest`; `@DataJpaTest` with `replace = NONE` and a SQLite file in `@TempDir` |
| Backend integration | Failsafe | `@SpringBootTest(RANDOM_PORT)`, temporary file per class, no mocks of own code; restart, kill and second-instance tests use a separate JVM; the claim is proved in a child JVM (Task 03a), never only with a second connection in one JVM; hashing of the database file happens only while no instance holds it |
| Real port (SC-30) | Failsafe | `RawHttpClient` writes bytes verbatim on a `Socket` for every Appendix D row a client library would alter |
| Frontend logic | `node --test "frontend/**/*.test.js"` | Pure modules with expected values copied from the spec; source greps for the constitution's NEVER list; colour-token contrast |
| E2E | `@playwright/test` 1.64.1, desktop and mobile Chromium | Real backend for contract flows, `page.route` only for what the page cannot provoke; `guard` fixture fails any CSP violation, console error naming CSP or Trusted Types, page error or cross-origin request |

Rules: expected values come from the spec; no sleeps; fixed clock and id supplier where time or ties matter; concurrency tests assert the allowed set of outcomes; Unicode fixtures are built from code points after a fixture check on the pinned JDK (AP-4). JaCoCo: bundle line coverage at least 80% from merged Surefire and Failsafe data, checked at `verify`. Playwright exploration through the MCP is allowed by the frontend agent's allowlist only and never replaces committed tests.

Risk tiers (P0 always blocks, P1 verified once, P2 documented) are assigned per Appendix D row and per UI criterion in `tasks.md` section 7, which is the only tier register; AP-5 and AP-9.6 keep no tier of their own and point to it.

## 6. Increments and order

The task list, estimates and dependencies are in `tasks.md`; this plan fixes only the order of capabilities.

| Increment | Delivers | Tag |
|---|---|---|
| 0a (Tasks 01 to 08) | Maven scaffold and quality gates, SQLite store and start policy, domain, error foundation, thin gate, add and list API, real-port baseline | `v0.1` |
| 0b (Tasks 09 to 14) | Static serving, E2E toolchain and harness, thin page, three smoke tests, README run instructions | `v0.2` |
| 1 | Routing, query and id rules, text policy, strict JSON, duplicates, capacity, filter, add errors, duplicate dialog | Sponsor names |
| 2 | Status, rating, delete, durability, directory trust, stored-data validation, entry UI | Sponsor names |
| 3 | Capacity and performance at maximum size, release sweep, accessibility check, error matrix, safe rendering, styles, keyboard and focus, release documentation | after Tasks 49 and 50 |
| 4 | P1 hardening and long runs, measurements | time permitting |

## 7. Risks and costly points

| ID | Point | Handling |
|---|---|---|
| X-03 | 50 adds plus 50 lists at 4,949 maximum-size books through one connection; the eighth running request may wait more than the Hikari 5 s and answer `500` | Measured in Task 41; failure goes to the Sponsor, never a weaker test, a 503 or a changed SC-06 value |
| X-04 | Duplicate scan at 4,999 maximum-size books without a stored key, estimate 0.3 to 1 s against the 2 s bound (never cut) | Bound proved in Task 23b, measured in Task 41; fallback is the key column: ASK FIRST, the task stops and asks the Sponsor |
| X-06 | `ddl-auto: validate` with STRICT and CHECK on the community dialect is untested | Spike in Task 04; fallback `typeof` CHECKs changes `schema.sql`: ASK FIRST, stop and request approval; the spike also proves that the stored DDL text equals the normalised statement (AP-12.1) |
| X-08 | Jackson 3 key names and strictness switches come from knowledge, not tests | Proved in Task 03b and Task 08 (naming, unknown fields), Task 21 (rest) |
| X-10 | `org.sqlite.tmpdir` under the data directory | Task 03a; if the driver cannot load from it the property is dropped and the risk recorded in `security.md` |
| X-11 | The gate's route table duplicates the MVC mappings | `RouteTableTest` enumerates the controllers' mappings and fails on any difference. The table holds the routes that exist at that task: two in Task 15, seven after Task 31, which edits `RouteTable` and keeps the test green |
| X-12 | Memory: a maximum list is about 31 MB of strings and entities per in-flight request, up to 8 in flight | Fixed `-Xmx` (proposed 512 MB, final value from Task 41) in the README command and in the C-11 (c) JVM; Task 41 AC4 passes when 8 slow readers end without `OutOfMemoryError` and the process then serves; failure goes to the Sponsor |
| N-1 | Hot journal rollback at first open is a write before validation | Spec change CR-9 states it is not a violation |
| N-2, N-3 | Claim semantics, per-connection init and sqlite-jdbc keys come from knowledge | Task 03a reads effective values back with pragmas, proves the claim from a child JVM and after a forced eviction; fallback a no-op write transaction, decided in Task 03a |
| N-6 | Tasks at the 45-minute ceiling (01, 03a, 06, 08, 14, 16, 21, 31, 34b) | A task that overruns is split, never extended; the first failed spike stops the task for a split (done so far: 03a/03b, 23a/23b, 34a/34b) |
| N-9 | Exclusive lock: the database cannot be inspected with another tool while the application runs | README states it |
| N-10 | Planted or hot journal beside an invalid file (R-20 proposed) | Cases in Task 34b AC3; accepted residual risk is a Sponsor matter |
| F-axe | The accessibility checker may be blocked by the CSP or Trusted Types | Runs in test code only; first try with no bypass; if one is needed it is a `bypassCSP` on a dedicated context inside `us-014-accessibility.spec.ts` (never global, never for the guard tests); the application CSP and headers are never relaxed (Sponsor decision 2026-11-02; SEC-F-076) |

Cost: about 35.75 h of task time for the release scope (Increments 0 to 3 plus Tasks 49 and 50, 53 tasks; backend 20.7 h), about 45 h with the rework allowance; the Sponsor accepted about 32 h on 2026-11-02 against the 6 to 8 h of DEC-022 and is asked to confirm the new figure.

## 8. Sponsor decisions

Decided on 2026-11-02: items 1, 2, 4, 5, 6, 8, 10 and 11 are approved or accepted as recommended (item 1 as constitution 1.2 and the matching `security.md` text; item 4 at 35.75 h, backend 20.7 h). Items 3, 7 and 9 stay deferred: the task asks the Sponsor only if the case occurs. The list below keeps the original wording for traceability.

1. Amendment of the SC-33 sentence in `constitution.md` and the matching text in `security.md` for the launcher; needed before Task 13.
2. Whether `SqliteDataSourceProvider` setting the per-connection keys through the wrapped data source counts as the constitution's "JDBC URL or `connection-init-sql`" mechanism, or an amendment is wanted; approval of the native SQL paragraph of section 2.
3. Approval, at the time and never before, of the two `schema.sql` fallbacks (`typeof` CHECKs; stored key and index).
4. Confirmation of the release-scope figure (35.75 h, backend 20.7 h) against the accepted figure of about 32 h.
5. The documented limit for the race between the directory check and the pool opening (Task 34a); residual risk R-20 (hot journal beside an invalid file) and the CR-9 closing clause.
6. `-Xmx512m` as a README run requirement and the course of action if Task 41 cannot pass (X-12); `max-swallow-size 16KiB` as a limit next to the SC-06 values.
7. If the Task 16 drip test shows a connection surviving: a total read deadline or an accepted limit.
8. Approval of CR-1 to CR-9 and DEC-034 at the Phase 3 gate, before Task 11 is briefed.
9. Task 43: if the accessibility scan cannot run without relaxing the application, the Sponsor decides; a `bypassCSP` context is reported before it is merged.
10. Whether the focus targets after `404` and after a request that removes nothing (AP-8, P-F1 and P-F2) stay plan assumptions or become a change request.
11. Rejection of a stored cell containing U+0000 (AP-12.1), which US-009.5 does not state: confirm it is covered by "invalid format".
