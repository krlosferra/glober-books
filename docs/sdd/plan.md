# GloberBooks Technical Plan

- Owner: Orchestrator (assembled from the Java architect's section, handoff 022, and the frontend developer's section, handoff 021); the PM owns `tasks.md`
- Version: 0.1 (Phase 3 draft, review round 1 of 3)
- Last updated: 2026-11-02
- Inputs: `constitution.md` 1.1, `spec.md` 0.4.1 (change requests CR-1 to CR-9 pending), `security.md`, `delivery-strategy.md` 1.0, `tasks.md` 0.3
- Detail that does not fit three to five pages is in `plan-appendix.md` (class table, request pipeline, code sources, tiers by task, frontend logic tests, E2E layout, tooling checks, open assumptions). The plan never overrides the constitution or the spec.

## 1. Shape of the solution

One process: Spring Boot (JDK 21) bound to `127.0.0.1`, one embedded SQLite file in rollback-journal mode, and a static page served by the same process. The page is plain HTML, CSS and vanilla ES modules with no build step. Layers follow the constitution: `controller`, `service`, `repository`, `domain`, `dto`, `error`, `config`. Backend and frontend share only the API contract of `spec.md` (5.0 to 5.9, Appendix A).

## 2. Backend decisions

| # | Decision | Rationale |
|---|---|---|
| D1 Pipeline | One `RequestGateFilter` at highest precedence: security headers, Host, Origin, Sec-Fetch-Site, Content-Type, body cap (16,384 bytes, buffered), route and method, Accept, id, query, bytes on GET and DELETE. Then strict JSON, field validation, service. Order: AP-2 | Tomcat normalises `//`, `;` and dot segments before Spring routes; only the raw request target tells `/api/v1/books/%2e%2e` from a valid path |
| D2 Errors | One `ErrorBodyFactory` builds every body (`ErrorBody`, `DuplicateErrorBody`; no `details` of type `Object`). Users: `GlobalExceptionHandler`, the gate through `ErrorResponseWriter`, `EnvelopeResponseWrapper` for any `sendError`. `ErrorMvcAutoConfiguration` excluded. Tomcat's pre-application answers (TRACE included) stay generic | Constitution; controllers never build bodies |
| D3 Transactions | Pool of one connection held for a whole `@Transactional` service method, each started `BEGIN IMMEDIATE`. Add = count, duplicate scan, insert in one transaction. Status and rating: load, call a `Book` method, commit; last write wins. CHECK constraints repeat INV-1 to INV-4. `open-in-view` off. No capacity trigger | Atomic by construction |
| D4 Static | Spring's resource handler off. `StaticContentController` serves `StaticAssetCatalog`: every `.html`, `.css`, `.js` under `classpath:/static/` found at startup (any other file there stops the start). `/` equals `/index.html`; all else 404. No `ETag`, `Last-Modified`, `Accept-Ranges`; `Range` and conditionals ignored | The default handler adds them and answers 206 and 304 with no switch (5.8, E-30) |
| D5 Limits | Tomcat `threads.max 8`, `max-connections 128`, `accept-count 128`, `connection-timeout 5s`; Hikari `connection-timeout 5000` | At most 8 requests wait for the connection; the rest queue in the OS |
| D6 Time, ids | One `Clock` (only in `ClockConfig`), instants cut to milliseconds; UUID v4 from an injected `Supplier<UUID>` | Testable |

`config` has three sub-packages (constitution 1.1): `config.transport`, `config.json`, `config.sqlite`. Classes ending in `Config` are pure wiring and excluded from JaCoCo with `GloberBooksApplication`.

### SQLite settings and start order

Built in `SqliteDataSourceProvider` from the validated path; the configured URL stays `jdbc:sqlite:<file>` and is not overridable.

| Setting | Value |
|---|---|
| `journal_mode` | default rollback journal, never changed; any other value stops the start |
| `synchronous` | `FULL`, explicit |
| `locking_mode` | `EXCLUSIVE`; the claim is `BEGIN EXCLUSIVE` + `COMMIT` with no change, before any query; a second instance fails with "database is locked" |
| Others | `transaction_mode=IMMEDIATE`, `busy_timeout` 5000, `foreign_keys` ON, `trusted_schema` OFF, read-write without create, extension loading off |
| Pool | size 1, `max-lifetime 0` (a recycled connection would drop the claim) |
| Temp dir | `org.sqlite.tmpdir` = `<data dir>/tmp` (0700); dropped and recorded as a risk if the driver cannot load from it |

Start order, before the first request (US-009.5 "validate before write" without WAL):
1. `StartupPolicy` (non-loopback address or another URL form: stop), then `DataDirectoryGuard` (create directory 0700 and file 0600; refuse symlink, other owner, group or other bits).
2. A non-empty file: plain Java I/O reads the 100-byte header (magic string, page size, format bytes 18 and 19 equal to 1, size equals page size times page count). A WAL-marked or non-SQLite file is refused before SQLite touches it.
3. The pooled connection opens and takes the claim.
4. `StoredLibraryValidator` on that connection with `PRAGMA query_only = ON`: `quick_check`, exactly one table `books`, no view, trigger or virtual table, STRICT columns, every row against formats, lengths, INV-1 to INV-4 and the 5,000 limit. A zero-length file skips this step.
5. Only then `query_only` goes OFF, `schema.sql` runs (a no-op on a valid database), then Hibernate `validate`.

`StartupRefusalIntegrationTest` hashes the file before and after each invalid case and requires an equal hash, a non-zero exit and a reason without book data. The one earlier write possible is SQLite's own rollback of a hot `-journal` left by a killed instance (spec change CR-8).

### Data model (`schema.sql`, one `STRICT` table, executed at every start)

Table `books`: `id` (36-character lowercase UUID), `title` (1 to 500 code points), `author` (1 to 200), `status` (`unread`, `reading`, `completed`), `rating` (null or 1 to 5, only when completed), `date_added`, `date_completed` (24-character UTC text with milliseconds; `date_completed` present exactly when completed). Every rule is a named CHECK; the full DDL is in `plan-appendix.md` (AP-6). No index but the primary key: with at most 5,000 rows the duplicate scan reads all anyway. The duplicate key is computed in Java from one query ordered by `date_added`, `id` (earliest match) and never stored. Fallback if the capacity measurement fails: a stored key and an index, which is a new Sponsor decision.

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
- Content types: `text/html;charset=UTF-8`, `text/css;charset=UTF-8`, `text/javascript;charset=UTF-8`.
- E2E runs the jar `backend/target/globerbooks.jar` with `SERVER_ADDRESS=127.0.0.1`, `SERVER_PORT=18080` and an explicit `SPRING_DATASOURCE_URL` inside a fresh 0700 temporary directory; readiness is `GET /api/v1/books` returning 200; budget 90 s. The database is exclusively locked while the application runs, so the E2E code never opens it.
- The `DELETE` sent by the browser has the JSON `Content-Type` and no body (accepted by the gate); a browser `Origin` of `http://127.0.0.1:18080` or `http://localhost:18080`, `Sec-Fetch-Site: same-origin` and `none` pass; `Origin: null` is 403. The E2E base URL uses the literal `127.0.0.1`.

## 5. Testing strategy

| Level | Runner | Content |
|---|---|---|
| Backend unit | Surefire | Services (repository mocked), policies, validators, error mapping |
| Backend slice | Surefire | `@WebMvcTest`; `@DataJpaTest` with `replace = NONE` and a SQLite file in `@TempDir` |
| Backend integration | Failsafe | `@SpringBootTest(RANDOM_PORT)`, temporary file per class, no mocks of own code; restart, kill and second-instance tests use a separate JVM |
| Real port (SC-30) | Failsafe | `RawHttpClient` writes bytes verbatim on a `Socket` for every Appendix D row a client library would alter |
| Frontend logic | `node --test "frontend/**/*.test.js"` | Pure modules with expected values copied from the spec; source greps for the constitution's NEVER list; colour-token contrast |
| E2E | `@playwright/test` 1.64.1, desktop and mobile Chromium | Real backend for contract flows, `page.route` only for what the page cannot provoke; `guard` fixture fails any CSP violation, console error naming CSP or Trusted Types, page error or cross-origin request |

Rules: expected values come from the spec; no sleeps; fixed clock and id supplier where time or ties matter; concurrency tests assert the allowed set of outcomes; Unicode fixtures are built from code points after a fixture check on the pinned JDK (AP-4). JaCoCo: bundle line coverage at least 80% from merged Surefire and Failsafe data, checked at `verify`. Playwright exploration through the MCP is allowed by the frontend agent's allowlist only and never replaces committed tests.

Risk tiers (P0 always blocks, P1 verified once, P2 documented) are assigned per Appendix D row and per UI criterion in `tasks.md` section 7 and AP-5, AP-9.

## 6. Increments and order

The task list, estimates and dependencies are in `tasks.md`; this plan fixes only the order of capabilities.

| Increment | Delivers | Tag |
|---|---|---|
| 0a (Tasks 01 to 08) | Maven scaffold and quality gates, SQLite store, domain, error foundation, thin gate, add and list API, real-port baseline | `v0.1` |
| 0b (Tasks 09 to 14) | Static serving, E2E toolchain and harness, thin page, three smoke tests, README run instructions | `v0.2` |
| 1 | Routing, query and id rules, text policy, strict JSON, duplicates, capacity, filter, add errors, duplicate dialog | Sponsor names |
| 2 | Status, rating, delete, durability, directory trust, stored-data validation, entry UI | Sponsor names |
| 3 | Capacity and performance at maximum size, release sweep, accessibility check, error matrix, safe rendering, styles, keyboard and focus, release documentation | after Tasks 49 and 50 |
| 4 | P1 hardening and long runs, measurements | time permitting |

Backend and frontend tasks run in parallel after the contract is final; a frontend E2E task starts only when the backend tasks it needs are merged (`tasks.md`).

## 7. Risks and costly points

| ID | Point | Handling |
|---|---|---|
| X-03 | 50 adds plus 50 lists at 4,949 maximum-size books through one connection must stay inside the OS backlog | Measured in Task 41; failure goes to the Sponsor, never a weaker test or a 503 |
| X-04 | Duplicate scan at 4,999 maximum-size books without a stored key, estimate 0.3 to 1 s against the 2 s bound (never cut) | Smoke in Task 23, measured in Task 41; fallback is the key column (new decision) |
| X-06 | `ddl-auto: validate` with STRICT and CHECK on the community dialect is untested | Spike in Task 04; fallback `typeof` CHECKs |
| X-08 | Jackson 3 key names and strictness switches come from knowledge, not tests | Proved in Task 03 and Task 08 (naming, unknown fields), Task 21 (rest) |
| X-10 | `org.sqlite.tmpdir` under the data directory | Task 03; if the driver cannot load from it the property is dropped and the risk recorded in `security.md` |
| X-11 | The gate's route table duplicates the MVC mappings | `RouteTableTest` enumerates the controllers' mappings and fails on any difference |
| N-1 | Hot journal rollback at first open is a write before validation | Spec change CR-8 states it is not a violation |
| N-2, N-3 | Claim semantics and sqlite-jdbc URL keys from knowledge | Task 03 reads effective values back with pragmas; fallback a no-op write transaction |
| N-6 | Tasks at the 45-minute ceiling (01, 06, 08, 21, 34) | A task that overruns is split, never extended |
| N-9 | Exclusive lock: the database cannot be inspected with another tool while the application runs | README states it |
| F-axe | The accessibility checker may be blocked by the CSP or Trusted Types | Runs in test code only; the application CSP is never relaxed (Sponsor decision 2026-11-02) |

Cost: the P0 scope is about 32 h of task time for Increments 0 to 3 (about 39 h with rework), accepted by the Sponsor on 2026-11-02 against the indicative 6 to 8 h of DEC-022.
