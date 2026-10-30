# GloberBooks Task Backlog

- Owner: PM (`sdd-product-manager`)
- Version: 0.1
- Last updated: 2026-10-30
- Status: Draft (Phase 3, review round 1 of 3). Awaiting the Liaison and Security reviews and the Sponsor gate. Definitions only: task state lives in `status.md`.
- Inputs: `delivery-strategy.md` v1.0, `spec.md` v0.4.2 (draft change requests CR-1 to CR-8 on top of the approved 0.4.1), `constitution.md` 1.1, `security.md`, handoffs 020 (backend plan candidates B-nn) and 021 (frontend plan candidates F-n.n). Handoff 022 (the leaner backend plan) was not in the repository when this version was written; the first review round re-baselines against it.
- Sponsor decisions applied (2026-10-30): spec frozen except change requests; exercise release = Increments 0 to 3 with every P0 row; Increment 4 = P1 hardening, later and time permitting; Increment 0 is a thin walking skeleton; SQLite uses the default rollback journal with exclusive locking (no WAL) and the schema of handoff 020 part 4; static content served by our own controller; `TRACE` answered by the container with a pre-application `405`.

## 0. How to read this backlog

**Numbering.** Tasks are numbered 01 to 58 in build order (branch `<type>/task-NN-<slug>`, commit `type(scope): subject [Task-N]` per `git-workflow.md`). The constitution's "Task 1" is Increment 0 as a whole (Tasks 01 to 15); section 8 maps each Task 1 proof to the task that produces it.

**Files convention.** "Files" lists at most 4 entries. An entry is one hand-written file or one group of closely coupled files (a set of small records, a class with its sibling, a generated wrapper set); a test file counts as an entry. The Maven wrapper files are generated and count as one entry. Where a task is dense, the estimate sits at the upper bound and the review should check the size.

**Estimates** are agent working minutes for build plus own tests, excluding the Liaison and Security reviews and the Sponsor gate. **Tier** is P0, P1 or P2 from `delivery-strategy.md` section 4; the per-row classification is in section 7.

**Parallelism.** The contract in `spec.md` is final for every clause the frontend uses, so backend and frontend tasks may run in parallel; a frontend E2E task needs the merged backend tasks it lists. Backend and frontend tasks never edit the same files (`git-workflow.md`).

**Definition of Ready.** Spec clause approved (the CR-n marked text counts only after approval), acceptance criteria testable, every dependency DONE, the Liaison task brief written.

**Definition of Done** (per task): the constitution's list: `cd backend && ./mvnw verify` green (backend), `cd e2e && npx playwright test` green and the frontend logic tests green (frontend), real output attached; coverage at least 80% (backend); every acceptance criterion mapped to a test; no new dependency, schema change or file outside the scope; `progress.md` and affected documents updated in the same branch; `hygiene` workflow green; Liaison ACCEPT, Security PASS (or PASS WITH CONDITIONS with every condition closed), PM acceptance, Sponsor approval. The per-increment gates below add to it.

**Increment gates** (all must be green before the Sponsor tries the increment and the tag `v0.N` is set): the P0 rows listed for the increment, `./mvnw verify`, `node --test "frontend/**/*.test.js"`, `npx playwright test` (smoke first), `hygiene`, and an updated `progress.md`.

## 1. Totals and honest estimates

| Increment | Tasks | Backend | Frontend | Integration | Effort (sum of estimates) | Elapsed with two agents in parallel (no review waits) |
|---|---|---|---|---|---|---|
| 0 Walking skeleton | 15 (01 to 15) | 9 | 5 | 1 | 615 min, about 10.3 h | about 7.5 h |
| 1 Core library | 17 (16 to 32) | 11 | 6 | 0 | 735 min, about 12.3 h | about 8.8 h |
| 2 Progress and rating | 11 (33 to 43) | 5 | 6 | 0 | 425 min, about 7.1 h | about 4 h |
| 3 Interface quality and release gate | 10 (44 to 53) | 4 | 6 | 0 | 405 min, about 6.8 h | about 4.2 h |
| **Exercise release (0 to 3)** | **53** | 29 | 23 | 1 | **2,180 min, about 36 h** | **about 24 to 25 h** |
| 4 P1 hardening | 5 (54 to 58) | 3 | 2 | 0 | 210 min, about 3.5 h | about 2.5 h |
| All | 58 | 32 | 25 | 1 | about 40 h | |

Findings the Sponsor should know:
1. **The indicative budget of 6 to 8 hours (DEC-022) is not reachable** with the spec as frozen: the 36 hours of Increments 0 to 3 are only the build time of P0 work. The reviews and gates per task come on top. Fewer or larger tasks reduce the number of reviews, not the hours of building. The only levers on hours are scope (the cut list of `spec.md` section 1, tier rules) or accepting the elapsed time.
2. **Increment 0 has 15 tasks, not about 8.** The constitution's Task 1 proofs (build split, quality gates, mandatory properties, ban list, real-port security class, toolchains, E2E isolation, run instructions) are 15 tasks of 30 to 45 minutes when each task keeps the INVEST limits (45 minutes, at most 4 file entries). Features are already as thin as the contract allows: no validation beyond empty and length, no duplicate or capacity rule, no routing table, no filter. Options: (A) accept 15 tasks, about 10 h effort and 7.5 h elapsed (recommended); (B) merge pairs into about 9 tasks of 60 to 90 minutes (same hours, fewer reviews, INVEST limit waived); section 9 lists the pairs.
3. **The total is 58 tasks, not about 40.** Reaching about 45 needs the merges of section 9, which break the 45-minute limit.
4. The architects' 73 candidate tasks were merged to 58: B-01..B-18 became 15 (the thin skeleton), B-20..B-31 became 11, B-40..B-47 became 5 plus 2, B-50 folded into the Task 14 guard, B-51 and B-64 kept; the frontend 28 became 25.

## 2. Increment 0: walking skeleton (Tasks 01 to 15)

Goal: a running application that adds and lists books through the API and the page, with every quality gate green. Gate rows (P0): E-16, E-23 on every response class that exists, E-10 (first five rows), E-11, E-07, E-09 (declared length), E-15 and E-39 (application rows), US-010.1, US-011.1, US-011.2, US-014.6 (smoke), mandatory properties effective, hygiene green. Deliberately deferred to Increment 1: Origin and Fetch-Metadata, chunked body cap, route table, `Accept`, query and id checks, TextPolicy, duplicates, capacity, filter, the data-directory refusals.

### Task 01: Maven scaffold and build proofs
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: none | Branch: `chore/task-01-maven-scaffold`
- Description: Create the Maven project with the pinned wrapper, parent and plugins, the Surefire and Failsafe split, the merged JaCoCo coverage check, `-Xlint:all -Werror`, the mapping of `frontend/` into the classpath `static/`, the committed dependency tree and the classpath ban test.
- Files: (1) `backend/pom.xml` and the wrapper set (`backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/**`); (2) `GloberBooksApplication.java`; (3) `BuildSplitProofTest.java` and `BuildSplitProofIntegrationTest.java`; (4) `RuntimeClasspathBanTest.java` and `backend/config/dependency-tree.txt`.
- Acceptance criteria:
  1. `cd backend && ./mvnw verify` is green on the pinned JDK; the wrapper is Maven 3.9.16 with `distributionSha256Sum`, `.mvn/maven.config` holds `--strict-checksums`, and a run with a wrong checksum fails (recorded) (SC-19, SC-36).
  2. A `*Test` class runs in Surefire and a `*IntegrationTest` class in Failsafe; JaCoCo merges both and `check` at line coverage 80% is bound to `verify`; a recorded run with an impossible threshold fails `verify`.
  3. A deliberately introduced compiler warning fails the build (`-Xlint:all -Werror`, recorded).
  4. The committed dependency tree equals `dependency:tree`; no banned coordinate is present; every transitive exception is named; `RuntimeClasspathBanTest` passes (SC-36).
  5. The built jar is named `globerbooks.jar`, contains `frontend/` as `static/` without `*.test.js`, `*.map` or dotfiles, and contains no test class.
- Traces: constitution Task 1 proofs, SC-19, SC-36, SC-27 (no HTTP client in `main`).

### Task 02: Quality gates (Checkstyle, SpotBugs, Find Security Bugs)
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 01 | Branch: `chore/task-02-quality-gates`
- Description: Add Checkstyle 14.3.0 through `maven-checkstyle-plugin` 3.6.0 and SpotBugs with Find Security Bugs to `verify`, verifying the unverified pins (publisher, Java 21 and class-file 65 support) or asking the Sponsor to re-approve other pins, with a justified exclusion file.
- Files: (1) `backend/pom.xml`; (2) `backend/config/checkstyle/checkstyle.xml` and `suppressions.xml`; (3) `backend/config/spotbugs/exclude.xml`; (4) the pin evidence in the delivery handoff (the constitution pin table is changed only through its Amendment Procedure).
- Acceptance criteria:
  1. Checkstyle runs in `verify` with the constitution rules (naming with the 3-character and `id` exception, 300-line and 50-line limits, imports, Javadoc, line length) and regex bans for wall-clock calls, `System.out`, `printStackTrace` and `select *`; one seeded violation of each ban fails the build (recorded).
  2. Checkstyle 14.3.0 with plugin 3.6.0 is shown to work together, or a re-approval request with the working pins is in the delivery (SC-19).
  3. SpotBugs and Find Security Bugs pins are chosen after checking the publishers and Java 21 support, bound to `verify`, and every exclusion has a written justification (SC-21).
  4. `verify` is green on the skeleton with the gates active.
- Traces: SC-07, SC-14, SC-19, SC-21, SC-27, constitution Task 1 proofs.

### Task 03: SQLite foundation and fail-closed start
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 01 | Branch: `feature/task-03-sqlite-foundation`
- Description: Add the start-up policy that refuses a non-loopback bind address and any datasource URL that is not `jdbc:sqlite:<file>`, and the single-connection SQLite data source with the fixed settings (default rollback journal, exclusive locking, immediate transactions) and creation of the missing directory (mode 0700) and file (mode 0600) before the first connection.
- Files: (1) `config/StartupPolicy.java`; (2) `config/SqliteDataSourceConfig.java`; (3) `StartupPolicyTest.java`; (4) `SqliteFoundationIntegrationTest.java`.
- Acceptance criteria:
  1. Start with `SERVER_ADDRESS=0.0.0.0`, with `:memory:` and with a URL carrying parameters or another scheme is refused with a fixed reason that contains no book data (SC-39, SC-01).
  2. A missing data directory is created with mode 0700 and a missing database file with mode 0600 before the first connection; after a write, the modes of every file the storage creates next to it are checked on POSIX (US-009.3, SC-13).
  3. Reading the settings back on the live connection gives the values recorded in the plan: rollback journal (not WAL), exclusive locking, `foreign_keys` on, `busy_timeout` 5000, `trusted_schema` off, pool size 1; loading an extension fails (SC-32, constitution Persistence Standards).
  4. A row written through the data source is present after the context is restarted on the same file (US-009.1 smoke).
- Traces: US-009.1, US-009.3, SC-01, SC-13, SC-32, SC-39.

### Task 04: Book persistence (schema, entity, repository)
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 03 | Branch: `feature/task-04-book-persistence`
- Description: Add the approved `schema.sql` (STRICT table with CHECK constraints, no secondary index, no trigger), the `Book` entity with its status and timestamp converters, and `BookRepository`, and prove that Hibernate `validate` accepts the STRICT table.
- Files: (1) `src/main/resources/schema.sql`; (2) `domain/Book.java`, `BookStatus.java`, `InstantTextConverter.java`; (3) `repository/BookRepository.java` and `BookKeyRow.java`; (4) `SchemaConstraintsTest.java`.
- Acceptance criteria:
  1. `schema.sql` runs at every start without error and `ddl-auto: validate` passes with the STRICT table and CHECK constraints (spike result recorded; if it fails the plan's fallback is applied and reported).
  2. The database itself rejects a row with a bad id pattern, a title or author length outside 1 to 500 and 1 to 200, a status outside the three values, a rating outside 1 to 5, a rating on a non-completed book, `date_completed` not equal to completed status, or a malformed timestamp (INV-1 to INV-4).
  3. A saved book returns with the same millisecond timestamps and lower-case status text; the ordered list query returns `date_added` descending then `id` ascending, including ties (US-002.2, BR-010).
  4. `Book` has no public setters; no query concatenates SQL and none uses `select *` (SC-07).
- Traces: Section 4, BR-010, INV-1 to INV-4, SC-07.

### Task 05: Configuration, clock, ids, Jackson basics
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 04 | Branch: `feature/task-05-configuration`
- Description: Write `application.yml` with every mandatory property, the Tomcat and Hikari limits and the log pattern that neutralizes control characters, add the clock, id generator and Jackson configuration, and prove each mandatory property effective with the Spring Boot 4 and Jackson 3 key names.
- Files: (1) `src/main/resources/application.yml`; (2) `config/ClockConfig.java`, `config/JacksonConfig.java`, `service/IdGenerator.java` and the exclusion in `GloberBooksApplication.java`; (3) `MandatoryPropertiesIntegrationTest.java`; (4) `LogPatternTest.java`.
- Acceptance criteria:
  1. For every mandatory property of the constitution a test shows its effect; a key that Boot 4 does not recognize is corrected and reported, never dropped silently (static locations stay set and the default resource handler is off).
  2. `ErrorMvcAutoConfiguration` is excluded, whitelabel is off, no Actuator or dev tool is on the runtime classpath, the bind address defaults to `127.0.0.1` (SC-02, SC-01).
  3. The log pattern removes CR, LF, U+2028, U+2029, U+0085 and ESC from message and exception, and the pinned levels hold (configuration-level assertion, SC-37).
  4. Timestamps come only from the injected clock, truncated to milliseconds, in the fixed format; ids are lower-case UUID v4 (constitution Persistence Standards).
  5. Unknown members in a request DTO fail and member names map to snake_case (SC-31 partial; the strict resolver is Task 19).
- Traces: constitution mandatory properties, SC-02, SC-06 (limits), SC-31, SC-37.

### Task 06: Error envelope and exception advice
- Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 01 | Branch: `feature/task-06-error-envelope`
- Description: Add the error codes of Appendix A, the envelope records, the single error-body factory, the response writer for filters and the global exception advice with the generic 500.
- Files: (1) `error/ErrorCode.java`, `ErrorBody.java`, `DuplicateErrorBody.java`, `FieldProblem.java`, `ApiException.java`; (2) `error/ErrorBodyFactory.java` and `ErrorResponseWriter.java`; (3) `error/GlobalExceptionHandler.java`; (4) `ErrorBodyFactoryTest.java` and `GlobalExceptionHandlerTest.java`.
- Acceptance criteria:
  1. `ErrorCode` holds the 16 codes with the HTTP status of Appendix A; every body has exactly `error`, `code`, `timestamp` plus `details` only in the three shapes of Appendix A (INV-6).
  2. The advice answers with `Content-Type: application/json; charset=UTF-8` also when the request has `Accept: text/xml` (SC-29).
  3. An unexpected exception gives `500 INTERNAL_ERROR` with the text "Unexpected error.", no detail, and one log line holding the stack trace and no user text (E-21 at unit level, SC-11).
  4. No message contains user input, class names, SQL or paths (Appendix A).
- Traces: Appendix A, INV-6, SC-11, SC-29.

### Task 07: Transport gate (thin) and real-port baseline
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 05, 06 | Branch: `feature/task-07-transport-gate`
- Description: Add the request filter that runs first and sets the security headers on every response, applies the Host allowlist, the `Content-Type` grammar for every method other than GET and HEAD and the declared-length body cap, and add the raw-socket test client and the baseline real-port security test class.
- Files: (1) `config/RequestGateFilter.java`; (2) `config/ContentTypePolicy.java` and `config/SecurityHeaders.java`; (3) `support/RawHttpClient.java`; (4) `TransportSecurityIntegrationTest.java` (the `ContentTypePolicyTest` cases live in the same class).
- Acceptance criteria:
  1. On the real port, every response class that exists (200, 400, 404, 413, 415) carries the constitution security headers and no `Server`, `Set-Cookie`, `Location` or `Access-Control-*` (E-23, SC-10).
  2. The five forged Host forms give `400 INVALID_HOST`; `LOCALHOST:<port>`, `127.0.0.1:<port>` and `localhost:<port>` pass (E-10 first five rows, E-11, SC-03).
  3. A non-GET request without a valid `Content-Type` gives `415` (E-07 rows, E-34 row 1 passes the gate; SC-05).
  4. A declared body of 16,385 bytes gives `413 PAYLOAD_TOO_LARGE` on any path; 16,384 bytes passes the gate (E-09 declared rows, SC-06).
  5. A rejection answers in the envelope, shows no stack trace and the application serves the next request.
- Traces: BR-012 steps 1a, 1c, 1d, SC-03, SC-05, SC-06, SC-10, SC-30 baseline.

### Task 08: Add and list books (thin API)
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 04, 05, 06, 07 | Branch: `feature/task-08-add-and-list-thin`
- Description: Implement `POST /api/v1/books` and `GET /api/v1/books` with a deliberately thin rule set (status default, empty and length checks, server id and date), newest first, so the page can add and list; the full rules replace the thin ones in Increment 1.
- Files: (1) `service/BookService.java`; (2) `dto/CreateBookRequest.java`, `BookResponse.java`, `BookListResponse.java`; (3) `controller/BookController.java`; (4) `BookServiceTest.java` and `BookControllerTest.java`.
- Acceptance criteria:
  1. A valid title and author give `201` with exactly the seven members, `status` `unread`, `rating` and `date_completed` null, a lower-case UUID `id` and a millisecond `date_added` (US-001.1, US-001.2, 5.2).
  2. The list returns `200` with `books` and `total`, newest first with ties by `id` ascending; an empty library returns `[]` and 0 (US-002.1, US-002.2, US-002.3, US-002.5).
  3. An empty title or author, or one over 500 or 200 code points, gives `400 VALIDATION_ERROR` with the field in `details`; an unknown member gives `400 MALFORMED_REQUEST` (US-001.4 and US-001.2 subsets).
  4. Add is one transactional service method and list a read-only one; controllers hold no business rule (constitution).
- Traces: US-001, US-002, 5.1, 5.2.

### Task 09: Static content from our own controller
- Layer: backend | Tier: P0 | Estimate: 35 | Depends on: 05, 07 | Branch: `feature/task-09-static-content`
- Description: Serve the page and its assets from a fixed catalog built at start-up from the configured static location, with the framework's resource handler off, so no conditional, range or caching header appears and every other path is `404`.
- Files: (1) `config/StaticAssetCatalog.java`; (2) `controller/StaticContentController.java`; (3) `StaticContentIntegrationTest.java`; (4) `src/test/resources/test-static/**` (fixture files).
- Acceptance criteria:
  1. `GET /` and `GET /index.html` return `200` with `text/html;charset=UTF-8`; `.js` files return `text/javascript` and `.css` files `text/css`; the constitution headers are present (E-16, 5.8).
  2. No response has `ETag`, `Last-Modified` or `Accept-Ranges`; `Range`, `If-None-Match` and `If-Modified-Since` are ignored and the full body is returned with `200` (E-30).
  3. Directory paths, `/favicon.ico`, a `*.test.js` path, `/error`, `/.git/config`, `/data/globerbooks.db` and traversal forms return `404 NOT_FOUND` in the envelope (E-15 application rows, E-39, SC-12).
  4. The catalog is built from `spring.web.resources.static-locations` once at start; files added later are not served.
- Traces: 5.8, SC-12, E-15, E-16, E-30, E-39.

### Task 10: E2E toolchain and pins
- Layer: frontend | Tier: P0 | Estimate: 30 | Depends on: none | Branch: `chore/task-10-e2e-toolchain`
- Description: Create the E2E package with the exact pins, install rules and lockfile, check the Node pin and verify the `@playwright/mcp` pin in one run.
- Files: (1) `e2e/package.json` and `e2e/package-lock.json`; (2) `e2e/.npmrc`; (3) `.nvmrc`; (4) the verification evidence in the delivery.
- Acceptance criteria:
  1. `.nvmrc` and `engines.node` hold the pinned Node and `node --version` equals it.
  2. `npm ci` with `.npmrc` (`ignore-scripts=true`, `save-exact=true`, `engine-strict=true`) installs `@playwright/test` 1.64.1; `npx playwright --version` and `npx playwright install chromium` succeed; no `typescript` package and no `tsconfig` exist (SC-19).
  3. `npm audit --audit-level=high` is clean and every lockfile `resolved` URL is on `https://registry.npmjs.org/` (SC-20, SC-36, SC-21).
  4. `@playwright/mcp` 0.0.83 runs once with the explicit flags; the browser disk use and the tool names are recorded (SC-23).
- Traces: constitution pins, SC-19 to SC-21, SC-23, SC-36.

### Task 11: Frontend logic and API client (thin)
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: none | Branch: `feature/task-11-logic-and-client-thin`
- Description: Add the DOM-free modules for exact copy, formatting and id checks and the single network module with list and add, each with `node:test` tests, proving the unit test command on the pinned Node.
- Files: (1) `frontend/js/logic/copy.js`, `format.js`, `ids.js` and their tests; (2) `frontend/js/api/http.js` and its test; (3) `frontend/js/api/booksApi.js` and its test; (4) `frontend/js/sources.test.js`.
- Acceptance criteria:
  1. `node --test "frontend/**/*.test.js"` runs the ES-module tests on the pinned Node without a `package.json`; if it cannot, the delivery reports a constitution issue.
  2. Formatting: status labels, "n books" with "1 book" and "0 books", `Added: YYYY-MM-DD` from a UTC value in the local time zone (test sets `TZ`), UUID check and encoded path builder (US-010.3, US-010.4, BR-011).
  3. The client sends only root-relative `/api/v1/books` URLs, `Accept` and, on non-GET, `Content-Type: application/json`, `allow_duplicate` only when true, maps snake_case to camelCase, never throws for an HTTP outcome and returns `ok` or a failure of kind `api`, `network` or `unreadable` (US-013.1, US-013.3).
  4. A request with no complete response after 15 seconds counts as `network` (US-013.6 as changed by CR-4).
  5. The served sources contain none of the forbidden sinks of the constitution (SC-09).
- Traces: US-010.3, US-010.4, US-013, SC-09, BR-011.

### Task 12: Page shell with minimal list and add
- Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 11 | Branch: `feature/task-12-page-shell`
- Description: Build the single page with its semantics, stylesheet, the list rendering from a `<template>` and the add form, so a reader can add and see books; filter, errors and dialogs follow in Increment 1.
- Files: (1) `frontend/index.html` and `frontend/css/styles.css`; (2) `frontend/js/ui/dom.js` and `ui/listView.js`; (3) `frontend/js/ui/addForm.js`; (4) `frontend/js/ui/app.js` and `frontend/js/main.js`.
- Acceptance criteria:
  1. The page has `lang="en"`, the title and the single `h1` "GloberBooks", a main landmark, the heading "Your books", a list named "Books", the form with Title, Author, Status (default Unread) and "Add book", and keyboard focus on Title (US-010.1, US-011.1, US-014.4 without the 320 clause).
  2. Each entry shows title, author, status label, "Added: YYYY-MM-DD" and the rating text rules, only through `textContent`; the count "n books" follows the API order; "Loading your books…" is shown in a status element during the load (US-010.2, US-010.3, US-010.4, US-010.8).
  3. Typing a title, Tab, an author and Enter sends exactly one `POST`, puts the returned book at the top, clears the form, announces "Book added: <title>" and returns focus to Title (US-011.2, US-011.4, US-011.5).
  4. The page loads only `/css/styles.css` and `/js/**`, with no inline script, style or handler and no request to another origin (US-014.7, SC-10).
- Traces: US-010.1 to .4, US-010.8, US-011.1, US-011.2, US-011.5, US-014.4, US-014.7.

### Task 13: E2E harness (config, launcher, isolation)
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 10, 09 | Branch: `test/task-13-e2e-harness`
- Description: Add the Playwright configuration, the launcher that starts the application jar on a temporary database directory and the global setup that asserts the isolation rules, with one placeholder test of the served page.
- Files: (1) `e2e/playwright.config.ts`; (2) `e2e/global-setup.ts`; (3) `e2e/support/start-server.mjs`; (4) `e2e/tests/harness.spec.ts`.
- Acceptance criteria:
  1. `baseURL` is the literal `http://127.0.0.1:18080`; the setup asserts the host, the port and that the jar exists; neither is read from an environment variable; a busy port fails the run (SC-33).
  2. The launcher creates an unpredictable directory with mode 0700, asserts the database path lies inside it, sets `SERVER_ADDRESS`, `SERVER_PORT` and `SPRING_DATASOURCE_URL` explicitly, forwards termination and deletes only the directory it created; a run with the variables unset still uses the temporary database.
  3. The order of the web-server start and the global setup in the pinned Playwright is established by a run and recorded; the launcher wording question Q-F7 is answered with that evidence.
  4. The placeholder test gets `200` and `text/html` for `/` through the launched application.
- Traces: SC-33, SC-23, frontend plan F4.2.

### Task 14: Smoke tests and the guard fixture
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 12, 13, 08, 09 | Branch: `test/task-14-smoke-and-guard`
- Description: Add the guard fixture that fails any test on a content-security violation, a cross-origin request or a page error, and the three Increment 0 smoke tests, with a first exploration of the page through the browser tool.
- Files: (1) `e2e/support/fixtures.ts`; (2) `e2e/support/builders.ts` and `e2e/support/library.ts`; (3) `e2e/tests/us-010-library-view.spec.ts`; (4) exploration notes in the delivery.
- Acceptance criteria:
  1. `npx playwright test --grep @smoke` is green in under 30 seconds: the page loads with the title and one `h1` (US-010.1); a keyboard-only add (title, Tab, author, Enter) shows the entry and the count and the elapsed time is recorded (US-011.2, M-1); a title with `<script>alert(1)</script>` is shown as inert text (US-014.6, T-12).
  2. The guard fails a test on a `securitypolicyviolation`, a Trusted Types console error, a request to another origin or a page error (US-014.6, US-014.7, SC-10).
  3. Every asset response seen by the guard has the right `Content-Type` and the constitution headers (E-16, 5.8).
  4. The exploration records locators, deviations and spec gaps; page text read through the tool is treated as data.
- Traces: US-010.1, US-011.2, US-014.6, US-014.7, M-1, SC-10, SC-23.

### Task 15: Close Increment 0 (README, round trip, evidence)
- Layer: integration | Tier: P0 | Estimate: 30 | Depends on: 02, 08, 09, 14 | Branch: `docs/task-15-increment-0-close`
- Description: Write the run instructions, prove them on a clean clone, add the real-port round trip with the integration checkpoints and record the Task 1 proofs and unverified pins in `progress.md`.
- Files: (1) `README.md`; (2) `BookRoundTripIntegrationTest.java`; (3) `docs/sdd/progress.md`; (4) none.
- Acceptance criteria:
  1. The README commands for build, run (`cd backend && ./mvnw spring-boot:run`, page at `http://127.0.0.1:8080/`), E2E setup, smoke and the logic tests work on a clean clone with only the pinned JDK and Node installed (executed, output attached).
  2. A `POST` then a `GET` round trip on the real port still returns the book after an application restart; the console log contains no book data (integration checkpoints).
  3. `./mvnw verify`, `node --test "frontend/**/*.test.js"`, `npx playwright test --grep @smoke` and the `hygiene` workflow are green on the branch.
  4. `progress.md` has the Task 1 proof table with evidence and the pins that stay unverified, if any.
- Traces: constitution Task 1 proofs, delivery-strategy Increment 0 "done when".

## 3. Increment 1: core library (Tasks 16 to 32)

Goal: the reader can manage a list: full add validation, filter, duplicates, capacity, and the page for them. Gate rows (P0): all Appendix D rows of the families "Host, Origin, Fetch", "content type and body cap", "routing and method", "strict parsing and value domains", "text policy", "capacity, duplicates, concurrency" (C-01 to C-04, C-06, C-07, C-09, C-10, C-11 bounds), E-22, E-33, US-010.1 to .8, .10, US-011.1 to .10.

### Task 16: Unicode fixture check and duplicate key
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 01 | Branch: `feature/task-16-duplicate-key-and-fixtures`
- Description: Prove on the pinned JDK that the Unicode data the spec assumes is true, build the code-point fixtures, and implement the duplicate key of BR-003.
- Files: (1) `service/DuplicateKey.java`; (2) `support/UnicodeFixtures.java`; (3) `UnicodeFixtureCheckTest.java`; (4) `DuplicateKeyTest.java`.
- Acceptance criteria:
  1. `UnicodeFixtureCheckTest` asserts the JDK is at least 21.0.12, that U+1E030 is assigned and U+2FFC is not (Unicode 15.0), the category of every code point the spec names, and the NFC result of the T-24b and T-25 inputs; a mismatch stops the task and is reported to the PM.
  2. The key applies NFKC, `toLowerCase(Locale.ROOT)`, collapsing of white-space runs and trimming: "Clean Code" matches " clean   code " and the full-width "Ｃlean Code" (US-004.5, BR-003).
  3. "Straße" and "STRASSE" do not match and "Clean Code" by "b" matches "CLEAN CODE" by "b" (T-40).
  4. The ASCII fast path gives the same key as the general path on a generated set; test sources contain only ASCII and build non-ASCII fixtures from code points.
- Traces: BR-003, US-004.5, T-40, SC-16 precondition.

### Task 17: Text policy, part 1 (trim, NFC, length, forbidden categories)
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 16 | Branch: `feature/task-17-text-policy-1`
- Description: Implement items 1 to 6 of BR-006 returning a problem kind without framework types.
- Files: (1) `service/TextPolicy.java` and `TextProblem.java`; (2) `support/UnicodeFixtures.java` (edit); (3) `TextPolicyTest.java`; (4) none.
- Acceptance criteria:
  1. Trimming uses the Unicode White_Space property (a leading U+00A0 is trimmed; a tab or line feed at an end is trimmed, inside is rejected) and the stored text is NFC (T-08, T-02, US-001.5).
  2. Length is counted in code points after NFC: 500 accepted and 501 rejected for a title, 200 and 201 for an author; 500 decomposed "é" is accepted; 300 times U+1D15E is rejected (T-06, T-07, T-22, T-11).
  3. NUL, other Cc, Cf except U+200C and U+200D, Zl, Zp, private use, noncharacters, unassigned (15.0) and tag code points are rejected; U+FFFD between letters is accepted (T-01, T-03, T-09, T-26).
  4. The reported problem follows the order required, empty, length, characters (BR-006).
- Traces: BR-006 items 1 to 6, REQ-025, REQ-026, SC-16.

### Task 18: Text policy, part 2 (visible content, mark runs, joiners)
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 17 | Branch: `feature/task-18-text-policy-2`
- Description: Implement items 6b to 6d of BR-006: visible content, mark runs and joiner adjacency.
- Files: (1) `service/TextPolicy.java` (edit); (2) `TextPolicyMarksTest.java`; (3) `support/UnicodeFixtures.java` (edit); (4) none.
- Acceptance criteria:
  1. Text that renders blank (only U+3164, U+2800, U+0301 or U+FE0F) and text that begins with a mark are rejected; "ab", U+3164, "cd" is accepted (T-23).
  2. A mark run of 9 code points, or with two variation selectors, is rejected; a run of 8 and one variation selector are accepted (T-24, T-24b).
  3. Joiners: the Persian word with U+200C is accepted; a joiner first, last, next to another joiner or next to a space is rejected; a family emoji sequence is accepted (T-10, T-25).
  4. The 8-mark limit counts joiners and marks together, including the 50-repetition input of T-24b, without a timeout or an unbounded regular expression.
- Traces: BR-006 items 6b to 6d, DEC-029, SC-16.

### Task 19: Strict JSON body resolver
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 05, 06, 07 | Branch: `feature/task-19-strict-json`
- Description: Decode the buffered body with a strict UTF-8 decoder and parse it strictly into the request DTO, mapping every failure to `MALFORMED_REQUEST` within 300 ms, with the low read constraints.
- Files: (1) `config/StrictJsonBodyResolver.java` and `config/StrictStringDeserializer.java`; (2) `config/WebMvcConfig.java`; (3) `StrictJsonBodyResolverTest.java`; (4) `TextAndParserIntegrationTest.java`.
- Acceptance criteria:
  1. Invalid UTF-8, a byte order mark, comments, a trailing comma, single quotes, unquoted names, a raw control character in a string, trailing content, a zero-byte body and a non-object value each give `400 MALFORMED_REQUEST` (T-05, T-28, T-29, T-39).
  2. An unpaired surrogate, an unknown member, a duplicate member (also after unescaping) and a wrong JSON type give `400 MALFORMED_REQUEST`; `allow_duplicate` as `"true"`, `1` or `null` is rejected (T-04, T-16, T-21, T-27, T-30).
  3. A string up to the 16 KiB cap is a length error, not a parse error (T-11); deeply nested bodies give `MALFORMED_REQUEST` in under 300 ms and never `500` (T-31 nesting rows).
  4. Each Jackson 3 switch is proven effective by a test (SC-31): trailing tokens, duplicates, no coercion, no default typing, read constraints.
- Traces: 5.0 strict parsing, BR-012 step 5, SC-31, T-04, T-05, T-16, T-21, T-27 to T-30, T-39.

### Task 20: Field validation wiring
- Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 18, 19 | Branch: `feature/task-20-field-validation`
- Description: Wire the text policy into Bean Validation without expression language, choose between `VALIDATION_ERROR`, `INVALID_STATUS` and `INVALID_RATING` by BR-012 step 6, and let the service repeat the checks.
- Files: (1) `config/BookText.java` and `config/BookTextValidator.java`; (2) `config/ValidatorConfig.java`; (3) `error/GlobalExceptionHandler.java` (edit); (4) `BookValidationTest.java`.
- Acceptance criteria:
  1. Title and author failures give `400 VALIDATION_ERROR` with one `details` entry per failing field in the order title, author, status; `{}` gives entries for title and author (US-001.4, US-001.10, T-09).
  2. Only `status` failing gives `INVALID_STATUS`; `status` with another field failing gives `VALIDATION_ERROR` (T-34, T-35); an absent status means `unread` and an explicit `null` is `INVALID_STATUS` (BR-001).
  3. A 501-character title containing `${7*7}` is rejected and no message contains `${7*7}` or `49` (T-13); messages never echo input.
  4. The service repeats the checks and throws the same exception type; the thin checks of Task 08 are removed.
- Traces: US-001.4 to .6, US-001.10, BR-001, BR-012 step 6, T-34, T-35.

### Task 21: Add rules (capacity and duplicates)
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 16, 20 | Branch: `feature/task-21-add-rules`
- Description: Implement capacity and duplicate detection inside the one add transaction, the `allow_duplicate` override and the exact 409 bodies, and prove the 2-second bound with 4,999 maximum-size books.
- Files: (1) `service/BookService.java`; (2) `error/DuplicateBookException.java` and `repository/BookRepository.java` (edit); (3) `support/LibrarySeeder.java`; (4) `BookServiceTest.java` and `AddRulesIntegrationTest.java`.
- Acceptance criteria:
  1. A matching key gives `409 DUPLICATE_BOOK` with `details.existing_book` (earliest `date_added`, then lowest `id`) and stores nothing; `allow_duplicate: true` stores a separate book; `false` behaves as absent; status, rating and dates do not take part (US-004.1 to .4, .6, .7).
  2. At 5,000 books every add is `409 LIBRARY_FULL`, before the duplicate check, with or without the override (US-001.7, US-004.9, BR-012 step 8).
  3. Count, duplicate scan and insert are one transaction with no query in a loop (BR-009, INV-5, INV-7, constitution).
  4. With 4,999 books of maximum size, an add of a non-matching book completes in at most 2 seconds with `201`, and an add that duplicates a stored book is answered `409 DUPLICATE_BOOK` in at most 2 seconds, never `500` (C-11 a and b, M-2). If the bound fails the task stops and the key-column schema change goes to the Sponsor.
- Traces: US-001.7, US-004, BR-003, BR-009, C-11 (a, b), INV-5, INV-7.

### Task 22: Transport completion (Origin, chunked cap, early rejection, slow request)
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 07 | Branch: `feature/task-22-transport-completion`
- Description: Add the Origin and Fetch-Metadata checks and the streaming body cap with early rejection and the `Expect` rule to the gate, and test the slow-request protection on the real port.
- Files: (1) `config/ClientOriginPolicy.java`; (2) `config/BoundedBodyRequest.java` and `config/RequestGateFilter.java` (edit); (3) `ClientOriginPolicyTest.java`; (4) `TransportSecurityIntegrationTest.java` (edit).
- Acceptance criteria:
  1. A present `Origin` that is not an allowed origin, or `Sec-Fetch-Site` other than `same-origin` or `none`, gives `403 FORBIDDEN` on every method, a cross-origin preflight included, with no `Access-Control-*` header; the allowed forms pass (E-04, E-12, E-13, E-14, SC-04, SC-28).
  2. A chunked body crossing 16,384 bytes gives `413` on any route and method, including an unknown path and GET or DELETE; a 16,385-byte body to an unknown path is `413` (E-09 chunked rows, E-32).
  3. `Expect: 100-continue` with an oversized length gets `413` and no `100 Continue`; a request rejected early that declares 10 MiB answers within 2 seconds and other requests are served meanwhile (E-31, E-33).
  4. A connection that sends no complete request within 5 seconds is closed while other requests are served (E-22, live run).
- Traces: BR-012 steps 1b, 1d, SC-04, SC-06, SC-28, E-04, E-09, E-12 to E-14, E-22, E-31 to E-33.

### Task 23: Routing, methods and container error path
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 22, 08 | Branch: `feature/task-23-routing`
- Description: Decide route and method from the raw request target in the gate, answer `404` and `405` with the exact `Allow` header, and turn any container `sendError` into the envelope.
- Files: (1) `config/RouteTable.java` and `config/RequestTarget.java`; (2) `config/EnvelopeResponseWrapper.java`; (3) `config/RequestGateFilter.java` (edit); (4) `RouteTableTest.java`.
- Acceptance criteria:
  1. Unknown paths, a trailing slash, a suffix, a `;` or encoded variants of fixed segments and encoded dot segments give `404 NOT_FOUND`; HEAD, OPTIONS and other methods on a defined path give `405` (E-01, E-02, E-03, E-17, E-24 rows 2 and 3, E-27).
  2. Every `405` carries `Allow` with exactly the methods defined for the path and never HEAD or OPTIONS (E-29, E-05).
  3. `RouteTableTest` enumerates the controllers' mappings and fails on any difference with the route table (SC-18, SC-40).
  4. A `sendError` raised inside the application leaves as the envelope with the security headers, never as a container page (SC-02, SC-11).
- Traces: 5.0 routing and methods, BR-012 step 2, SC-18, SC-40.

### Task 24: Accept, query, id and body rules in the gate
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 23 | Branch: `feature/task-24-accept-query-id`
- Description: Add the `Accept` grammar, the query grammar (including the page and asset paths), the id format judged as received and the no-body rule for GET and DELETE.
- Files: (1) `config/AcceptPolicy.java` and `config/QueryPolicy.java`; (2) `config/RequestGateFilter.java` (edit); (3) `AcceptPolicyTest.java` and `QueryPolicyTest.java`; (4) `TransportSecurityIntegrationTest.java` (edit).
- Acceptance criteria:
  1. `Accept: text/xml` gives `406`; absent, empty, `*/*` and `application/json` give `200`; an error body stays JSON (E-06).
  2. A malformed id, one containing `%`, or upper case gives `400 MALFORMED_REQUEST` after the route and method are known (T-14, T-14b, E-24 row 1, E-27).
  3. A non-empty query on any endpoint except the list, an unknown or repeated parameter, an empty pair, a malformed percent-encoding and an invalid UTF-8 sequence give `400 MALFORMED_REQUEST`; `?`, `?status=%72eading` pass; 2,000 query pairs give `400` (E-19, E-26, E-36 third row, E-38, T-20).
  4. Any body byte on GET or DELETE gives `400 MALFORMED_REQUEST` (E-18, E-38, T-39).
- Traces: BR-011, BR-012 steps 2 to 5, US-002.6, US-002.7, E-06, E-18, E-19, E-26, E-36, E-38, T-14, T-20, T-39.

### Task 25: List filter, order and capacity sequences
- Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 21, 24 | Branch: `feature/task-25-list-filter-capacity`
- Description: Add the `status` filter to the list (value judged after query decoding) and prove the ordering, the empty states and the capacity sequences with a seeded library.
- Files: (1) `service/BookService.java` and `controller/BookController.java` (edit); (2) `BookListIntegrationTest.java`; (3) `CapacityIntegrationTest.java`; (4) `support/LibrarySeeder.java` (edit).
- Acceptance criteria:
  1. `?status=reading` returns only that status in the same order with `total` of the filtered result; an empty library or filter returns `200` with `[]` and 0 (US-002.4, US-002.5, BR-010).
  2. `?status=READING`, `?status=`, `?status` and `?status=reading+` give `400 INVALID_STATUS`; repeated or unknown parameters give `400 MALFORMED_REQUEST` first (US-002.6, US-002.7, T-20).
  3. With 5,000 books a new add and a duplicate add (with or without override) give `409 LIBRARY_FULL`; list, status change, rating and delete still work; after one delete one add succeeds and the next gives `LIBRARY_FULL` (C-01 to C-04, US-008.3).
  4. The list never returns more than 5,000 entries and has no pagination members (US-002.8).
- Traces: US-002, BR-009, BR-010, C-01 to C-04.

### Task 26: Add stories end to end and concurrent adds
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 25 | Branch: `test/task-26-add-stories-concurrency`
- Description: Test the add stories through the real port and the concurrency guarantees of BR-013 for adds, asserting the set of allowed outcomes and never an interleaving.
- Files: (1) `AddStoriesIntegrationTest.java`; (2) `ConcurrencyIntegrationTest.java`; (3) `support/LibrarySeeder.java` (edit); (4) none.
- Acceptance criteria:
  1. Injection payloads (`'; DROP TABLE books;--`, `<script>alert(1)</script>`, `${7*7}`, `#{7*7}`, `%s%n`) are stored unchanged and the count grows by exactly one each time (T-12).
  2. "Straße" and "STRASSE" both store; "Clean Code" and "CLEAN CODE" by one author give `201` then `409` (T-40).
  3. Two simultaneous different adds at 4,999 books give one `201` and one `409 LIBRARY_FULL`; 20 simultaneous adds at 4,990 give exactly 10 and 10 (C-06, C-09).
  4. Two and 20 simultaneous identical adds give one `201` and the rest `409 DUPLICATE_BOOK`; the same with `allow_duplicate: true` gives all `201` (US-004.8, C-07, C-10).
- Traces: BR-013, US-004.8, C-06, C-07, C-09, C-10, T-12, T-40, INV-5, INV-7.

### Task 27: Frontend text and failure logic
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 11 | Branch: `feature/task-27-text-and-failures-logic`
- Description: Add the code-point length check and the table that maps every failure to the action and exact text of Section 5.9.
- Files: (1) `frontend/js/logic/text.js` and its test; (2) `frontend/js/logic/failures.js` and its test; (3) `frontend/js/logic/copy.js` (edit); (4) none.
- Acceptance criteria:
  1. Length is counted after trimming and NFC: 500 emoji count 500, 500 decomposed "é" count 500, 300 times U+1D15E count 600; empty or whitespace only is `empty`; 501 and 201 are `length` (US-011.3, T-07, T-22).
  2. Every `code` of Appendix A and the three non-envelope cases (no response, not JSON, missing or unknown `code`) map to the action and exact text of Section 5.9; an unknown code is treated as `INTERNAL_ERROR` (US-013.1, US-013.3).
  3. A `VALIDATION_ERROR` without usable `details` maps to the `MALFORMED_REQUEST` text (CR-7); the server's `error` text is never used for display (US-013.5).
  4. The tests assert every quoted string of Section 5.9 and US-011 to US-013 exactly.
- Traces: US-011.3, US-011.6, US-013.1, US-013.3, US-013.5, 5.9.

### Task 28: List state, filter and list states
- Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 27 | Branch: `feature/task-28-list-state-filter`
- Description: Add the pure page-state model and use it for the filter radios, the loading, empty, filter-empty and failed states, the count and the discarding of stale responses.
- Files: (1) `frontend/js/logic/listState.js` and its test; (2) `frontend/js/ui/app.js` and `ui/listView.js` (edit); (3) `frontend/index.html`; (4) `frontend/css/styles.css`.
- Acceptance criteria:
  1. The filter is a radio group named "Filter by status" with All, Unread, Reading, Completed, All checked at load; choosing an option loads the list with the matching `status` ("All" sends none) (US-010.5).
  2. A new load removes the entries and the count and shows only "Loading your books…"; a response to an earlier request is discarded (US-010.2 as changed by CR-2, US-010.10).
  3. Empty library shows "Your reading list is empty. Add your first book above."; an empty filter result shows "No books with this status." and "Show all books", decided by the count of US-010.4 (US-010.6).
  4. A load failure shows the Section 5.9 text and "Try again", repeating the request with the active filter, with zero list items and no count; an add before the first success, during a load or after a failure is not inserted (US-010.7 as changed by CR-2).
  5. The count follows the `total` plus matching adds minus deletes, unchanged by a non-matching add (US-010.4).
- Traces: US-010.2, .4 to .7, .10, CR-2.

### Task 29: Add form errors and in-flight guard
- Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 27, 28 | Branch: `feature/task-29-add-form-errors`
- Description: Add the client checks, the field messages, the single-request guard, the capacity and generic failure messages and the page-level message area.
- Files: (1) `frontend/js/ui/addForm.js`; (2) `frontend/js/ui/messages.js`; (3) `frontend/index.html`; (4) `frontend/css/styles.css`.
- Acceptance criteria:
  1. An empty or too long value shows an inline message linked to its field (informative wording, CR-3) and sends no request (US-011.3).
  2. While a request is in progress the button is disabled and a second click or Enter sends no request, checked by the handler and not only by the attribute (US-011.4).
  3. `VALIDATION_ERROR` shows each `details` message beside its field, the status text beside Status and focus moves to the first field in error; `INVALID_STATUS` shows the same status text (US-011.6).
  4. `LIBRARY_FULL` shows "Your library is full (5,000 books). Delete a book to add another." and keeps the typed values; any other failure shows its Section 5.9 text and keeps them (US-011.9, US-011.10, US-013.2).
  5. Each failure writes its text to the "Messages" status area once, as one replacement of the text (US-013.4 as defined by CR-5).
- Traces: US-011.3, .4, .6, .9, .10, US-013.2, US-013.4.

### Task 30: Duplicate dialog
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 29 | Branch: `feature/task-30-duplicate-dialog`
- Description: Add the modal "Possible duplicate" dialog with the existing book, the Cancel and Add anyway flows and the focus rules.
- Files: (1) `frontend/js/ui/dialogs.js`; (2) `frontend/js/ui/addForm.js` (edit); (3) `frontend/index.html`; (4) `frontend/css/styles.css`.
- Acceptance criteria:
  1. `409 DUPLICATE_BOOK` opens a modal dialog named "Possible duplicate" with "This book may already exist. Add anyway?" and the existing book's title, author, status label and "Added: YYYY-MM-DD", all set as text; initial focus is on "Cancel" and Escape cancels (US-011.7).
  2. "Cancel" closes the dialog, keeps the typed values and focuses Title (US-011.8).
  3. "Add anyway" closes the dialog, resends the same values with `allow_duplicate: true` and then behaves as a normal add; a failure of that request leaves focus on Title (US-011.8, US-011.5, CR-1).
  4. The dialog text of a 500-code-point title and of the hostile texts is shown as text (US-014.6 inert part).
- Traces: US-011.7, US-011.8, CR-1, US-014.6.

### Task 31: E2E library view, filter and states
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 28, 25 | Branch: `test/task-31-e2e-library-view`
- Description: Test the list stories against the real backend and with stubbed responses for the states the page cannot provoke.
- Files: (1) `e2e/tests/us-010-library-view.spec.ts` (edit); (2) `e2e/support/stubs.ts`; (3) `e2e/support/library.ts` (edit); (4) none.
- Acceptance criteria:
  1. Title and document title, one `h1`, "Your books", the list named "Books", the loading status text, the entry content with the local date for a fixed UTC value in a far time zone, and the rating texts (US-010.1 to .3, US-010.8).
  2. Order, count texts "1 book", "0 books" and "1000 books", filter options, empty and filter-empty messages with "Show all books" (US-010.4 to .6).
  3. A stubbed failure shows the Section 5.9 text and "Try again"; Try again with a filter repeats it; no list item exists until a load succeeds (US-010.7).
  4. Overlapping loads (held responses) show the result of the request sent last (US-010.10).
- Traces: US-010, T-12 (display).

### Task 32: E2E add, validation and duplicate flows
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 30, 26 | Branch: `test/task-32-e2e-add-and-duplicate`
- Description: Test the add stories end to end, the stubbed validation and capacity paths and the duplicate flow, and add the fourth smoke test.
- Files: (1) `e2e/tests/us-011-add-book.spec.ts`; (2) `e2e/tests/us-011-duplicate.spec.ts`; (3) `e2e/support/pages.ts`; (4) none.
- Acceptance criteria:
  1. Form defaults and focus, keyboard add, client checks without a request, double click and double Enter send one request (held response), confirmation text and focus after success (US-011.1 to .5).
  2. A forbidden character shows the server's `message` beside the field; stubbed `status` entry and `INVALID_STATUS` show "Choose Unread, Reading or Completed."; stubbed `LIBRARY_FULL` and a 500 keep the typed values (US-011.6, .9, .10).
  3. Duplicate: dialog text, focus on Cancel, Escape, Cancel keeps values, Add anyway adds a second book with the same title; this is smoke test 4 tagged `@smoke` (US-011.7, .8).
  4. Boundary texts (500 code points, decomposed "é", leading U+00A0) are accepted and shown trimmed (T-07, T-08, T-40 through the page).
- Traces: US-011, T-03, T-07, T-08, T-40, M-1.

## 4. Increment 2: progress and rating (Tasks 33 to 43)

Goal: the reader can track status, rate, clear a rating and delete, with the invariants intact. Gate rows (P0): S-01 to S-07, S-08 (20 iterations), S-09 (20 iterations), S-10, S-11, T-15, T-17 to T-19, T-31 numeric rows, T-32, T-36 to T-38, C-03, C-13 (5 iterations), US-009.1, US-009.2, US-012.1 to .7, .9, US-013 (page parts built so far).

### Task 33: Rating value and `INVALID_RATING`
- Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 19 | Branch: `feature/task-33-rating-value`
- Description: Judge the rating literal from the token and its text without numeric conversion and map the outcomes to `INVALID_RATING` or `MALFORMED_REQUEST`.
- Files: (1) `dto/RatingValue.java` and `dto/RatingRequest.java`; (2) `config/RatingValueDeserializer.java`; (3) `error/GlobalExceptionHandler.java` (edit); (4) `RatingValueDeserializerTest.java`.
- Acceptance criteria:
  1. The integers 1 to 5 are valid; 0, 6, -1, -0, 4.9, 5.0, 1e0, 1E1, `null`, `{}`, a missing member and a 100-digit integer give `400 INVALID_RATING` (T-17, T-32 `1E1`, T-37).
  2. A string, `true`, an array, an object, `01`, `+5`, `NaN`, `Infinity`, `5.`, `.5`, `0x5`, `5e` and a 101-digit literal give `400 MALFORMED_REQUEST` (T-18, T-32, T-37).
  3. `1e999999999`, `1e-999999999` and a 40-digit decimal fraction give `INVALID_RATING` within 300 ms and never `500` (T-31 numeric rows).
  4. Extra members, duplicate `rating` members and a `status` member in a rating body give `MALFORMED_REQUEST` (T-32).
- Traces: BR-002, US-003.2, .5, .6, T-17, T-18, T-31, T-32, T-37.

### Task 34: Status and rating rules in the domain and service
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 33, 21 | Branch: `feature/task-34-status-rating-rules`
- Description: Add the transitions that keep INV-1 to INV-4 and the service operations get, delete, change status, set rating and clear rating, in the order of BR-012 steps 7 and 8.
- Files: (1) `domain/Book.java` (edit) and `dto/StatusRequest.java`; (2) `service/BookService.java` (edit); (3) `BookTest.java`; (4) `BookServiceTest.java` (edit).
- Acceptance criteria:
  1. Every one of the nine status transitions is accepted; entering completed sets `date_completed` from the clock, leaving it clears rating and `date_completed` together, and a same-state change changes nothing (US-006.2 to .5, BR-004, BR-008, S-11).
  2. A valid rating on an unread or reading book is `RATING_NOT_ALLOWED` and the book is unchanged; an invalid value is `INVALID_RATING` first; a rating can be replaced and cleared, and clearing is idempotent (US-003.3, .4, .7, T-36).
  3. Field values are judged before existence: an invalid body on an unknown well-formed id reports the body error (T-38, US-003.8, US-006.8).
  4. Delete returns the removed book, a second delete is `BOOK_NOT_FOUND`, and `title`, `author`, `id` and `date_added` never change (US-008.1, .2, US-006.9).
- Traces: BR-004, BR-007, BR-008, BR-012 steps 7 and 8, INV-1 to INV-4.

### Task 35: Remaining endpoints and story contract tests
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 34, 24 | Branch: `feature/task-35-remaining-endpoints`
- Description: Expose get, delete, status, rating and clear-rating endpoints and test the stories and the invariant sequences through the real port.
- Files: (1) `controller/BookController.java` (edit); (2) `BookControllerTest.java` (edit); (3) `BookStoryIntegrationTest.java`; (4) `InvariantSequenceIntegrationTest.java`.
- Acceptance criteria:
  1. `GET`, `DELETE`, `PUT status`, `PUT rating` and `DELETE rating` answer as in 5.3 to 5.7, and a `201` is followed by a `GET` of the same book (US-001.9, US-007, US-008.1).
  2. The sequences S-01 to S-06 and S-11 give the stated results, and after any failed request of Appendix D D.1 to D.3 the library content is identical (S-07).
  3. A well-formed unknown id gives `404 BOOK_NOT_FOUND`, a malformed one `400 MALFORMED_REQUEST`, `PUT status` with `"Completed"`, `""`, `null`, `{}` gives `INVALID_STATUS` and with `5` or `true` gives `MALFORMED_REQUEST` (T-15, T-19, US-006.6 to .8).
  4. At 5,000 books, list, status change, rating and delete succeed (C-03).
- Traces: 5.3 to 5.7, US-003, US-006, US-007, US-008, S-01 to S-07, S-11, T-15, T-19, C-03.

### Task 36: Races and fault injection
- Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 35 | Branch: `test/task-36-races-and-faults`
- Description: Test concurrent updates and a forced failure in the middle of a status change, using a database trigger on the test database and no production hook.
- Files: (1) `RaceIntegrationTest.java`; (2) `support/FaultInjection.java`; (3) none; (4) none.
- Acceptance criteria:
  1. Status `reading` and rating 4 run concurrently on a rated completed book, 20 iterations: the final state is always `reading`, rating null, `date_completed` null, and the rating call is `200` or `RATING_NOT_ALLOWED` (S-08).
  2. `PUT status completed`, `DELETE rating` and `DELETE` of the book run concurrently, 20 iterations: every answer is `200` or `BOOK_NOT_FOUND` and INV-1 and INV-4 hold (S-09).
  3. A failure forced after the first of the two changes of BR-004 gives `500 INTERNAL_ERROR` and the book is exactly as before; the application keeps serving (S-10, E-21, BR-013).
  4. No production class contains a test-only route or switch (A-09).
- Traces: BR-013, S-08, S-09, S-10, E-21, INV-8.

### Task 37: Durability across restart and kill
- Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 35 | Branch: `test/task-37-durability`
- Description: Start the packaged application as a separate process and prove that acknowledged writes survive a normal stop and a forced kill.
- Files: (1) `support/AppProcess.java`; (2) `DurabilityIntegrationTest.java`; (3) none; (4) none.
- Acceptance criteria:
  1. After an acknowledged add, status change, rating change and delete, a normal stop and start show the same data (US-009.1).
  2. After a forced kill right after each of those four operations returned `2xx`, 5 iterations each, the restart shows the acknowledged change and the database opens normally under the rollback journal (US-009.2, C-13, M-3).
  3. The modes of the directory, the file and every file the storage creates next to it stay 0700, 0600 and not wider (SC-13).
  4. The console output of the child process contains no book data.
- Traces: US-009.1, US-009.2, M-3, C-13 (5 iterations), SC-13.

### Task 38: Client endpoints for status, rating, clear and delete
- Layer: frontend | Tier: P0 | Estimate: 25 | Depends on: 11 | Branch: `feature/task-38-client-endpoints`
- Description: Add the client functions for get, set status, set rating, clear rating and delete.
- Files: (1) `frontend/js/api/booksApi.js` (edit); (2) `frontend/js/api/booksApi.test.js` (edit); (3) none; (4) none.
- Acceptance criteria:
  1. Each function builds its path only through the validated id builder, with the right method; `DELETE` carries `Content-Type: application/json` and no body, and `GET` no body (E-07, 5.6, 5.7).
  2. Responses map to the camelCase book; failures map to the kinds `api`, `network` and `unreadable` with the field messages and the existing book where present.
  3. An id that is not a lower-case UUID is refused before any request (BR-011).
- Traces: 5.3 to 5.7, BR-011, US-013.1.

### Task 39: Entry view, status control and confirmation
- Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 38, 28 | Branch: `feature/task-39-entry-status`
- Description: Build one entry from the template with a status control and the confirmation dialog for a status change that removes a rating, using the server's returned values.
- Files: (1) `frontend/index.html`; (2) `frontend/js/ui/entryView.js`; (3) `frontend/js/ui/entryActions.js`; (4) `frontend/css/styles.css`.
- Acceptance criteria:
  1. Each entry has a single-choice status control named "Status of <title>" with Unread, Reading, Completed; choosing another value sends `PUT .../status` and the entry shows the returned values; its controls are disabled while the request runs (US-012.1, US-012.6).
  2. When a rating is shown and the user chooses unread or reading, a dialog named "Confirm status change" says "Moving <title> back to <Unread|Reading> removes its rating and completion date." with Continue and Cancel, focus on Cancel; Cancel restores the choice without a request; a completed entry without a rating changes without a dialog (US-012.3).
  3. The rating control exists on completed entries only: it appears after a change to completed and disappears after a change away from it (US-012.2).
  4. With a filter active, an entry that no longer matches stays and shows "No longer matches this filter" (US-012.8).
- Traces: US-012.1 to .3, .6, .8, US-014.6 (dialog text inert).

### Task 40: Rating group and clear rating
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 39 | Branch: `feature/task-40-rating-group`
- Description: Add the labelled rating group, the clear-rating button and the failure paths for status and rating changes.
- Files: (1) `frontend/js/ui/entryView.js` (edit); (2) `frontend/js/ui/entryActions.js` (edit); (3) `frontend/js/logic/failures.js` (edit); (4) `frontend/css/styles.css`.
- Acceptance criteria:
  1. A group named "Rating of <title>" offers 1 to 5 by keyboard and shows the current value as text; a button "Clear rating of <title>" exists only while a rating exists; selecting sends `PUT .../rating` and clearing sends `DELETE .../rating` (US-012.4).
  2. A failed request returns the control to the server's last known value and shows the Section 5.9 text beside the entry; `INVALID_RATING` shows "Choose a rating from 1 to 5." (US-012.7, 5.9).
  3. `RATING_NOT_ALLOWED` shows "Only completed books can be rated." and reloads the entry with `GET`; `BOOK_NOT_FOUND` removes the entry, reloads the list and announces "This book no longer exists." in the message area (US-012.7, US-010.10).
  4. Focus returns to the control used after a request that does not remove the entry (US-014.2).
- Traces: US-012.4, .7, US-013.4, 5.9.

### Task 41: Delete with confirmation
- Layer: frontend | Tier: P0 | Estimate: 35 | Depends on: 39 | Branch: `feature/task-41-delete-entry`
- Description: Add the delete button and the confirmation dialog with the focus rules.
- Files: (1) `frontend/js/ui/entryActions.js` (edit); (2) `frontend/js/ui/dialogs.js` (edit); (3) `frontend/index.html`; (4) none.
- Acceptance criteria:
  1. Each entry has a button named "Delete <title>" that opens a dialog named "Delete book" with "Delete <title>? This cannot be undone." and Delete and Cancel; initial focus on Cancel; Escape and Cancel send nothing and return focus to the Delete button (US-012.5).
  2. On success the entry disappears, the count follows US-010.4, "Book deleted: <title>" is announced and focus moves to the "Your books" heading; after the last entry the empty message of US-010.6 shows.
  3. A failed delete shows its Section 5.9 text beside the entry and focus returns to the Delete button.
  4. No control edits a title or author (US-012.9).
- Traces: US-012.5, .9, US-010.4, US-010.6, US-014.2.

### Task 42: E2E status and rating
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 40, 35 | Branch: `test/task-42-e2e-status-rating`
- Description: Test status changes, the clearing of rating and date, rating set, replace and clear, and the stale-state failure paths, and add the fifth smoke test.
- Files: (1) `e2e/tests/us-012-status-rating.spec.ts`; (2) none; (3) none; (4) none.
- Acceptance criteria:
  1. Change to completed, rate, replace, clear (smoke test 5), with the rating control present only on completed entries (US-012.1, .2, .4).
  2. Moving a rated book back shows the confirmation; Continue clears rating and completion date as the server returns, Cancel restores without a request (US-012.3, S-02, S-03).
  3. A same-state choice leaves the entry unchanged (S-04, S-11); with a filter active the "No longer matches this filter" text shows (US-012.8).
  4. After changing the book through the API behind the open page, rating it shows "Only completed books can be rated." and the entry is reloaded; deleting it behind the page then acting shows "This book no longer exists." (US-012.7, S-01, S-05).
- Traces: US-012, S-01 to S-05, S-11.

### Task 43: E2E delete
- Layer: frontend | Tier: P0 | Estimate: 30 | Depends on: 41, 35 | Branch: `test/task-43-e2e-delete`
- Description: Test the delete flows and the stale 404, and add the sixth smoke test.
- Files: (1) `e2e/tests/us-012-delete.spec.ts`; (2) none; (3) none; (4) none.
- Acceptance criteria:
  1. Delete with confirmation removes the entry, updates the count, announces "Book deleted: <title>" and focuses the "Your books" heading (smoke test 6, US-012.5).
  2. Cancel and Escape send no request and focus the Delete button (US-012.5).
  3. Deleting a book already deleted through the API shows "This book no longer exists.", removes the entry and reloads the list (US-012.7, S-06).
  4. Deleting the last book shows the empty message (US-010.6).
- Traces: US-012.5, .7, US-010.6, S-06.

## 5. Increment 3: interface quality and release gate (Tasks 44 to 53)

Goal: usable, accessible and safe to start and stop; the release candidate. Gate rows (P0): US-009.5 to .7, C-05 size bound, C-11 (c) assertions, US-013.1 to .6, US-014.1, .2, .4 (without 320), .6 (inert, no CSP violation, stylesheet containment), .7, .8, SC-38, the release checks of Task 47, and every earlier P0 row. After this increment the release criteria of `delivery-strategy.md` section 6 are checked: Security pre-release verdict, documents synchronized, README on a clean clone.

### Task 44: Data-directory guard, claim and second instance
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 37 | Branch: `feature/task-44-startup-guard-claim`
- Description: Refuse an untrustworthy data directory or file, hold the exclusive claim on the database from before the first query, and make a second instance fail while the first keeps serving.
- Files: (1) `config/DataDirectoryGuard.java`; (2) `config/SqliteDataSourceConfig.java` (edit); (3) `DataDirectoryGuardTest.java`; (4) `StartupRefusalIntegrationTest.java`.
- Acceptance criteria:
  1. On POSIX, a symbolic link as directory or file, another owner, or any group or other permission bit makes the start fail with a non-zero status and a fixed reason without book data, and the database file bytes are unchanged (US-009.6, SC-13).
  2. A swap of the file between the check and the pool opening is detected by comparing the file identity.
  3. A second instance on the database of a running instance exits non-zero with the same reporting while the first keeps serving and its data are unchanged (US-009.7, C-12).
  4. The native library directory is a private subdirectory of the data directory; if the driver cannot load from it the property is dropped and the residual risk is recorded in `security.md` through a handoff (SC-32).
- Traces: US-009.6, US-009.7, DEC-032, SC-13, SC-32, C-12.

### Task 45: Stored-library validation and clean start failure
- Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 44 | Branch: `feature/task-45-stored-library-validation`
- Description: Validate an existing database file read-only before anything writes to it and report any refusal on the console without book data.
- Files: (1) `config/StoredLibraryValidator.java`; (2) `config/StartupFailureAnalyzer.java`; (3) `StoredLibraryValidatorTest.java`; (4) `StartupRefusalIntegrationTest.java` (edit).
- Acceptance criteria:
  1. A non-database, a damaged file, other structure, no tables, a view, a trigger or a virtual table, a row that violates a format, a limit or INV-1 to INV-4, and more than 5,000 rows each make the start fail with a non-zero status, no request served, a reason without book data and database bytes unchanged (compared by hash) (US-009.5).
  2. An absent or zero-length file is created; a valid database is opened and no book is altered (US-009.3).
  3. A database left with an unfinished rollback journal by a forced kill is recovered normally and then judged; the chosen handling is recorded for the Security review.
  4. Hostile content in a stored row never appears in the console output.
- Traces: US-009.3, US-009.5, DEC-028, SC-39.

### Task 46: Capacity under load
- Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 26, 21 | Branch: `test/task-46-capacity-under-load`
- Description: Prove the response bound and the mixed load at maximum size and tune the thread and connection limits.
- Files: (1) `CapacityPerformanceIntegrationTest.java`; (2) `src/main/resources/application.yml` (edit if the limits change); (3) none; (4) none.
- Acceptance criteria:
  1. With 5,000 books of maximum size the list returns `200`, `total` 5000 and a body below 16,000,000 bytes; the elapsed time is recorded without a gate (C-05).
  2. With 4,949 books of maximum size, 50 concurrent adds of different books together with 50 concurrent lists give all adds `201`, all lists `200`, 4,999 books after, no `500`, no refused connection and no undocumented status (C-11 c).
  3. A failed request in that run changed nothing and the process still serves normally afterwards (INV-8).
  4. Peak heap is recorded; if the mix cannot pass, the task stops and the Sponsor decides (no weakened test).
- Traces: C-05, C-11 (c), SC-17, M-2.

### Task 47: Backend release sweep
- Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 45, 46 | Branch: `chore/task-47-backend-release-sweep`
- Description: Run the repository-wide checks that the pre-release Security review needs, and fix or report each finding.
- Files: (1) `backend/config/spotbugs/exclude.xml` (edit); (2) `backend/config/dependency-tree.txt` (edit if approved); (3) `docs/sdd/progress.md`; (4) none.
- Acceptance criteria:
  1. SpotBugs and Find Security Bugs report nothing unexplained and every exclusion keeps its justification.
  2. The grep gates are clean for `backend/src/main`: `select *`, outbound HTTP clients, `System.out`, `printStackTrace`, wall-clock calls, `CrossOrigin`, `getSession`, `sendRedirect`, websocket classes (SC-07, SC-14, SC-18, SC-27, SC-41).
  3. `dependency:tree` equals the committed baseline or the difference is approved; coverage is at least 80% and read next to the oracle rule.
  4. `RouteTableTest` and `RuntimeClasspathBanTest` pass on the final tree.
- Traces: SC-07, SC-14, SC-18, SC-19, SC-27, SC-36, SC-41.

### Task 48: Accessibility check (axe) and its pin
- Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 43, 42; Sponsor and Security decision on the axe and content-security interaction | Branch: `test/task-48-accessibility-check`
- Description: Verify and pin the accessibility checker, then test the page against the WCAG 2.1 A and AA rules in the data state of US-014.1.
- Files: (1) `e2e/package.json` and `e2e/package-lock.json`; (2) `e2e/tests/us-014-accessibility.spec.ts`; (3) none; (4) none.
- Acceptance criteria:
  1. The package publisher, the registry entry, the absence of install scripts, the peer range for the pinned Playwright and a clean audit are recorded and the exact pin is proposed for the constitution table (SC-21).
  2. The scan with the WCAG 2.1 A and AA rule tags reports zero violations of impact serious or critical on the page with at least three books (one completed and rated), two field errors on the add form, and each dialog open (US-014.1).
  3. The content security policy of the guard tests is not weakened; if the checker needs a relaxed context, that decision is taken by the Sponsor and Security before the task starts and the context is used for this scan only.
  4. Every control has a programmatic label (US-014.2).
- Traces: US-014.1, US-014.2, DEC-026, SC-21.

### Task 49: Error matrix
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 42, 29 | Branch: `test/task-49-error-matrix`
- Description: Test every error code and the non-envelope cases with stubbed responses, the timeout and the live-region update count.
- Files: (1) `e2e/tests/us-013-errors.spec.ts`; (2) none; (3) none; (4) none.
- Acceptance criteria:
  1. Each code of Appendix A shows the action and exact text of Section 5.9; not JSON, missing code and unknown code show the `INTERNAL_ERROR` text; no response shows the US-013.2 text and keeps the input (US-013.1 to .3).
  2. No raw `error` text, status code, stack, URL or id is displayed except the book's own data (US-013.5).
  3. A held request abandoned by the browser clock at 15 seconds re-enables its controls and shows the US-013.2 text (US-013.6 as changed by CR-4).
  4. Exactly one replacement of the live region text occurs per failure, observed with a mutation observer (US-013.4 as defined by CR-5).
- Traces: US-013, 5.9, E-21 (page text).

### Task 50: Safe rendering
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 42 | Branch: `test/task-50-safe-rendering`
- Description: Test that hostile and worst-case texts are inert in the list, every dialog and every error path, and that the page reads and stores nothing it should not.
- Files: (1) `e2e/tests/us-014-safe-rendering.spec.ts`; (2) `frontend/js/sources.test.js` (edit); (3) none; (4) none.
- Acceptance criteria:
  1. `<script>alert(1)</script>`, `<img src=x onerror=alert(1)>`, quotes, ampersands, a 500-code-point word and a title of base characters each followed by 8 combining marks are shown as inert text in the list and in each dialog with no content-security violation (US-014.6, SC-09).
  2. An assignment of a string to `innerHTML` throws under Trusted Types (SC-38).
  3. The forbidden-sink grep over the served sources passes (SC-09).
  4. The page reads nothing from the address, `window.name`, referrer or messages, stores nothing in web storage, cookies or IndexedDB and requests no other origin (US-014.7, US-014.8).
- Traces: US-014.6 to .8, SC-09, SC-38.

### Task 51: Styles polish
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 50 | Branch: `feature/task-51-styles-polish`
- Description: Finish the stylesheet: colour tokens with the required contrast, the visible focus ring, containment rules for long text and reduced motion, with a test that reads the stylesheet.
- Files: (1) `frontend/css/styles.css`; (2) `frontend/css/tokens.test.js`; (3) none; (4) none.
- Acceptance criteria:
  1. The colour token pairs reach 4.5:1 for text and 3:1 for control boundaries and the focus ring, computed by the WCAG formula (US-014.2, US-014.3).
  2. The stylesheet wraps or clips overflowing text in the list item and in each dialog, and the test that reads it fails if the rules are removed (US-014.6 containment; never cut).
  3. No animation or transition is defined, and a reduced-motion block removes any remaining motion (US-014.5).
  4. Status and errors always carry text as well as colour (US-014.3).
- Traces: US-014.2, .3, .5, .6.

### Task 52: Keyboard and focus rules
- Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 43, 30, 47 | Branch: `test/task-52-keyboard-focus`
- Description: Test keyboard-only operation, the focus return table of the stories and the testable reading of focus trapping for every dialog.
- Files: (1) `e2e/tests/us-011-add-book.spec.ts` (edit); (2) `e2e/tests/us-012-status-rating.spec.ts` (edit); (3) `e2e/tests/us-014-accessibility.spec.ts` (edit); (4) none.
- Acceptance criteria:
  1. Every operation (add, filter, status, rating, clear, delete, each dialog) is completed with the keyboard alone and the add sequence of M-1 works (US-014.2, M-1).
  2. Focus lands where the stories say: Title after Cancel, after Add anyway success and failure, the "Your books" heading after a delete, the opener after the confirmation dialogs (US-011.5, US-011.8, US-012.5, US-014.2 with CR-1).
  3. With each dialog open, repeated Tab and Shift+Tab presses never put focus on a page element outside the dialog (US-014.2 as defined by CR-6).
  4. The focused control has a computed outline that is neither none nor zero width (US-014.2).
- Traces: US-011.5, .8, US-012.5, US-014.2, M-1.

### Task 53: Release documentation
- Layer: frontend | Tier: P0 | Estimate: 30 | Depends on: 52, 47 | Branch: `docs/task-53-release-docs`
- Description: Align the testing documents with the delivered code and add the story-to-test table so that the documents match the final code before the release.
- Files: (1) `docs/sdd/frontend-testing.md`; (2) `README.md` (frontend notes); (3) `docs/sdd/progress.md`; (4) none.
- Acceptance criteria:
  1. The layout and the accessibility row of `frontend-testing.md` match the delivered tree and the constitution (no screenshots folder unless used).
  2. A table maps every P0 acceptance criterion of US-010 to US-014 to a test title, and every cut-list or P1 item to its state (done or reported open).
  3. The README frontend run and test commands are executed on a clean clone.
  4. The Liaison confirms that constitution, spec, plan, tasks and the status board describe the final code (release criterion 5); the status board update is made by the orchestrator.
- Traces: M-5, delivery-strategy section 6.

## 6. Increment 4: P1 hardening (Tasks 54 to 58), later and time permitting

Every task here is P1 or P2 and never blocks the release; rows that touch the never-cut list were moved into Increments 1 to 3 as P0. **Open point for the Sponsor:** `spec.md` section 1 allows only measurements to be cut, while P1 rows in Task 54 and Task 55 are not measurements. Either run Task 54 and Task 55 (about 85 minutes) before the release tag, or record in the delivery report, as the spec requires, that these rows are open (recommendation: run them before the tag).

### Task 54: P1 raw-socket run (pre-application answers)
- Layer: backend | Tier: P1 | Estimate: 45 | Depends on: 23, 24 | Branch: `test/task-54-pre-application-run`
- Description: Run the container-level cases once on raw sockets and record the result.
- Files: (1) `PreApplicationIntegrationTest.java`; (2) none; (3) none; (4) none.
- Acceptance criteria:
  1. HTTP/1.0 or 1.1 without Host, two Host headers, an invalid request line, a version `9.9`, two `Content-Length` headers, `Content-Length: -1`, both `Content-Length` and `Transfer-Encoding`, a line or header over 8 KiB: status in {400, 414, 431, 505}, no product or version, no echo, the next request is served (E-10 last two, E-20, E-36 first two, E-37).
  2. Both `TRACE` requests are answered `405` before the application; `FOO` and `get` give `405` or 400, 501 (E-28, DEC-033).
  3. The container rows with an encoded `/` or `\`, `%00`, a literal `\` or a dot segment above the root accept either the envelope or a pre-application answer without disclosure (E-15, E-24, E-24b, E-25 container rows).
  4. `Upgrade: websocket` and `Upgrade: h2c` give `200` and no `101` (E-31 upgrade rows).
- Traces: E-10, E-15, E-20, E-24, E-24b, E-25, E-28, E-31, E-36, E-37, SC-18, SC-29.

### Task 55: P1 grammar corners and log hygiene
- Layer: backend | Tier: P1 | Estimate: 40 | Depends on: 24 | Branch: `test/task-55-grammar-and-logs`
- Description: Test the remaining header grammar rows and the log neutralization with hostile input.
- Files: (1) `LogHygieneIntegrationTest.java`; (2) `TransportSecurityIntegrationTest.java` (edit); (3) none; (4) none.
- Acceptance criteria:
  1. `Content-Type` rows 2 to 7 of E-34 and all rows of E-35 (`Accept`) give the stated statuses.
  2. The application rows of E-24b and E-25 give the stated envelopes.
  3. Hostile path ids, query values and titles (`%0d%0a`, `%e2%80%a8`, `%1b[2J`) leave a log with one line per event, no raw CR, LF, U+2028, U+2029, U+0085 or ESC, and no title text (T-33, SC-37).
- Traces: E-24b, E-25, E-34, E-35, T-33, SC-37.

### Task 56: P1 long runs and timing figures
- Layer: backend | Tier: P1 | Estimate: 45 | Depends on: 36, 37, 46 | Branch: `test/task-56-long-runs-and-figures`
- Description: Run the long loops once and record the measured figures of M-2.
- Files: (1) `DurabilityIntegrationTest.java` (edit); (2) `RaceIntegrationTest.java` (edit); (3) `CapacityPerformanceIntegrationTest.java` (edit); (4) the recorded figures in `docs/sdd/progress.md`.
- Acceptance criteria:
  1. The kill loop of C-13 runs 50 iterations and passes; S-08 and S-09 pass at 200 iterations; C-08 (20 add and delete pairs at 4,999 books) never exceeds 5,000 books.
  2. The figures are recorded once on the reference machine: single-book operation, list at 5,000 typical books, add at 4,999 maximum-size books, list of C-05 (DEC-020).
  3. A figure over its threshold is reported open; the P0 bounds already passed in Tasks 21 and 46.
- Traces: C-08, C-13, S-08, S-09, M-2, DEC-020.

### Task 57: Measurements and responsive layout
- Layer: frontend | Tier: P1 | Estimate: 45 | Depends on: 52 | Branch: `test/task-57-measurements-responsive`
- Description: Add the cut-list measurements, the mobile project and the layout checks.
- Files: (1) `e2e/tests/measurements.spec.ts`; (2) `e2e/playwright.config.ts` (edit); (3) `e2e/tests/us-014-accessibility.spec.ts` (edit); (4) none.
- Acceptance criteria:
  1. With 1,000 books all entries are present 1,000 ms after the `load` event (recorded once); with 5,000 books of typical size the time is recorded with no threshold (US-010.9).
  2. At 320 CSS pixels there is no horizontal scrolling of the page, and at 360 pixels the worst-case texts of US-014.6 stay inside the bounding box of their list item or dialog and the buttons stay visible and operable (US-014.4, US-014.6).
  3. An item that is not written is reported open in the delivery, never skipped silently.
- Traces: US-010.9, US-014.4, US-014.6, M-2.

### Task 58: Final pass
- Layer: frontend | Tier: P1 | Estimate: 35 | Depends on: 57 | Branch: `docs/task-58-final-pass`
- Description: Explore the finished page at 320 and 360 pixels and close the traceability and cut-list report.
- Files: (1) `docs/sdd/progress.md`; (2) none; (3) none; (4) none.
- Acceptance criteria:
  1. An exploration through the browser tool at 320 and 360 pixels is recorded with deviations and spec gaps.
  2. The story-to-test table is complete and every open item is listed with its tier.
  3. The cut-list report states which measurement items were done and which are open.
- Traces: M-5, spec section 1.

## 7. Test and control tiers (classification for Security confirmation)

Rule (delivery-strategy section 4): P0 stops a realistic local attack or protects a core rule and blocks an increment; P1 is verified once and recorded; P2 is documented. Per the Sponsor: every item on the never-cut list of `spec.md` section 1 is P0. "Differs" marks a row where this classification differs from handoff 020 (backend, "B") or 021 (frontend, "F"). The cadence "5 iterations" or "20 iterations" is the P0 loop; the long run is P1 (Task 56).

### 7.1 Appendix D families

| Family | Rows | Tier | Differs | Built and run in |
|---|---|---|---|---|
| Envelope and headers on every response class, 500 without detail | E-21, E-23 | P0 | no | 06, 07, 36 |
| Host, Origin, Fetch-Metadata | E-04, E-10 (first five rows), E-11, E-12, E-13, E-14 | P0 | no | 07, 22 |
| Content type, body cap, early rejection, Expect | E-05, E-07, E-08, E-09, E-31 (`Expect` row), E-32, E-34 row 1 | P0 | no (E-34 row 1 made explicit) | 07, 22 |
| E-33 early rejection of a large declared body (2 s) | E-33 | P0 | **yes, B has P1**: the spec lists the 2-second response as never cut | 22 |
| Slow request | E-22 (configuration and live 5 s run) | P0 | **yes, B has live run P1**: the spec lists E-22 as never cut; the run takes about 6 s | 22 |
| Routing and method determinism | E-01, E-02, E-03, E-17, E-18, E-19, E-24 (rows 1 to 3 as answered by the application), E-26, E-27, E-29, E-36 (third row), E-38 | P0 | E-36 third row was unclassified in B | 23, 24 |
| Static content, traversal, favicon | E-15 (application rows), E-16, E-39 | P0 | no | 09, 14 |
| Accept grammar | E-06 | P0 | no | 24 |
| Strict parsing and value domains | T-04, T-05, T-14, T-14b, T-15, T-16 to T-21, T-27 to T-30, T-32, T-34 to T-40 | P0 | no | 19, 20, 24, 33, 35 |
| Parser limits never 500, under 300 ms | T-31 | P0 | no (never cut) | 19, 33 |
| Text policy core and injection payloads | T-01, T-02, T-03, T-06 to T-13, T-22, T-26 | P0 | T-26 is P1 in B (noncharacters and unassigned are item 6 of BR-006) | 17, 26 |
| Text policy hidden channels: blank, marks, joiners | T-23, T-24, T-24b, T-25 | P0 at unit level (Tasks 17 and 18 cannot be accepted without them); P1 for the real-port copy | **yes (label)**: B says P1 although it automates them as unit tests | 18 |
| Capacity, duplicates, concurrency | C-01 to C-04, C-06, C-07, C-09, C-10 | P0 | no | 21, 25, 26 |
| Add at 4,999 maximum-size books | C-11 (a) and (b) bounds: 201 or 409 within 2 s, never 500 | P0 | **yes, B has P1**: the 2-second add bound is never cut | 21 |
| Mixed load at 4,949 books | C-11 (c) assertions: all 201 and 200, no 500, no refused connection, nothing changed by a failure, process alive | P0 | **yes, B has P1** (the assertions are never cut; only timing figures are cut) | 46 |
| List size bound | C-05 body below 16,000,000 bytes | P0 | **yes (split)**: B has the whole row P1; the time of C-05 is cut list, hence P1 | 46 |
| Recorded timing figures | C-05 time, M-2 figures (300 ms single book, 1 s list), C-11 durations | P1 | no | 56 |
| Add and delete loop | C-08 | P1 | no | 56 |
| Second instance | C-12 | P0 | **yes, B has P1**: US-009.7 is a Must criterion and the guarantees of BR-009 and BR-013 depend on it (data corruption on the local model) | 44 |
| Durability | C-13 (5 iterations) | P0 | no; 50 iterations P1 | 37, 56 |
| Invariant sequences | S-01 to S-07, S-10, S-11; S-08 with 20 iterations | P0 | no | 35, 36 |
| Concurrent status, rating and delete | S-09 with 20 iterations | P0 | **yes, B has P1**: INV-1 and INV-4 under concurrency; 200 iterations P1 | 36, 56 |
| Long runs | S-08 at 200 iterations, S-09 at 200 | P1 | no | 56 |
| Grammar corners | E-34 rows 2 to 7, E-35, E-24b, E-25 (application rows) | P1 | no | 55 |
| Pre-application answers | E-10 (last two rows), E-15 container rows, E-20, E-25 container rows, E-28 (both TRACE rows, `FOO`, `get`), E-36 (first two rows), E-37 | P1 | no | 54 |
| Range, conditional, Upgrade | E-30, E-31 (Upgrade rows) | P1 | no (E-30 is also asserted by the Task 09 static tests) | 09, 54 |
| Log injection | T-33 (live), log pattern at configuration level is P0 | P1 and P0 | no | 05, 55 |
| Start-up criteria without an Appendix D row | US-009.1, .2, .3, .5, .6, .7 | P0 | **yes**: B gave no tier | 03, 37, 44, 45 |
| Accepted limits | Section 6 of `spec.md`: confusables, lower-case versus case folding, final sigma, "Straße", Unicode table version, R-16 to R-18 | P2 | no | documented, no test beyond T-40 |

Note on controls: build and static-analysis controls (SC-07, SC-14, SC-19, SC-20, SC-21, SC-27, SC-36) are P0 in every increment through Tasks 01, 02 and 47; the tier of every other `SC-NN` follows the rows that verify it.

### 7.2 UI acceptance criteria

| Criterion | Tier | Differs | Task |
|---|---|---|---|
| US-010.1, .3, .4, .5, .6, .7, .8, .10 | P0 | no | 12, 28, 31 |
| US-010.2 (loading text) | P1 | no (F) | 12, 31 |
| US-010.9 1,000 entries within 1,000 ms | P1 (cut list) | no | 57 |
| US-010.9 5,000 entries, recorded only | P2 | no | 57 |
| US-011.1 to .10 | P0 | F wrote "US-011.1 to .11"; there is no US-011.11 (typing slip) | 12, 29, 30, 32 |
| US-012.1 to .7, .9 | P0 | no | 39 to 43 |
| US-012.8 ("No longer matches this filter") | P1 | no (F) | 39, 42 |
| US-013.1 to .6 | P0 | no | 27, 29, 49 |
| US-014.1 (accessibility check, serious and critical) | P0 | no, but conditional on the Sponsor and Security decision about the checker and the content-security policy | 48 |
| US-014.2 (labels, keyboard, focus visible, dialogs trap and return focus) | P0 | no | 30, 41, 52 |
| US-014.3 (contrast; not colour alone) | P1 (cut list); the contrast rules inside the Task 48 scan are never cut, and the focus-ring ratio of US-014.2 is P0 | no | 48, 51 |
| US-014.4 lang, title, one `h1`, main | P0 | split | 12 |
| US-014.4 320-pixel clause | P1 (cut list) | split | 57 |
| US-014.5 (reduced motion) | P1 (cut list) | no | 51 |
| US-014.6 inert text and no content-security violation | P0 | no | 14, 50 |
| US-014.6 stylesheet containment rules (read from the stylesheet) | P0 (never cut) | split | 51 |
| US-014.6 bounding-box measurement | P1 (cut list) | split | 57 |
| US-014.7, US-014.8 | P0 | no | 12, 50 |
| SC-38 Trusted Types test | P0 | no | 50 |
| Rows exercised through the page: T-03 (one case), T-07, T-08, T-12, T-40, S-01 to S-03, S-05, S-06, C-01 (stub), E-16, E-21 (stub) | P0 | no | 14, 32, 42, 43, 49 |
| Page display of boundary texts (T-06, T-10, T-22, T-24), S-04, S-11, E-38, E-39 | P1 as page checks; the API row keeps its tier of 7.1 | not a conflict: the page repeats an API row | 32, 42, 58 |

### 7.3 Differences to confirm (summary)

| # | Row | Architect | PM | Reason |
|---|---|---|---|---|
| D1 | E-33 | P1 | P0 | named never cut in spec section 1 |
| D2 | E-22 live run | P1 | P0 | named never cut; about 6 s |
| D3 | C-11 (a), (b), (c) bounds and assertions | P1 | P0 | 2-second add bound, no `500`, nothing changed: never cut; figures stay P1 |
| D4 | C-05 | P1 | P0 for the size bound, P1 for the time | size bound is SC-17; time is cut list |
| D5 | C-12 | P1 | P0 | US-009.7; data protection |
| D6 | S-09 | P1 | P0 (20 iterations) | INV-1 and INV-4 under concurrency |
| D7 | T-23 to T-26, T-24b | P1 | P0 unit, P1 real-port copy; T-26 P0 | BR-006 is a Must rule; automated anyway |
| D8 | E-36 third row | unclassified | P0 | query-shape rule of BR-012 step 4 |
| D9 | US-009.5 to .7 | unclassified | P0 | start-up safety and data integrity |
| D10 | F: US-011.11 | typing slip | none | no such criterion |

## 8. Traceability

### 8.1 Task 1 proofs of the constitution

| Proof | Task |
|---|---|
| Failsafe and Surefire split, merged JaCoCo data, coverage check with a failing threshold run | 01 |
| `-Xlint:all -Werror` | 01 |
| Maven wrapper with checksum and `--strict-checksums` | 01 |
| Dependency tree against the ban list | 01 (and 47) |
| Checkstyle pins; SpotBugs and Find Security Bugs pins | 02 |
| Every mandatory property effective | 05 |
| Real-port security class with Host, header, body-cap and error cases (Origin and CSRF completed in Task 22) | 07, 22 |
| `node --test` on the pinned Node | 11 |
| Node and Playwright installs on the pinned Node; MCP pin | 10 |
| E2E isolation (SC-33) | 13 |
| Integration checkpoints (start, database initializes, round trip, edge cases) with logs | 03, 15 |
| `@axe-core/playwright` pin | 48 |

### 8.2 Stories, rules and endpoints to tasks

| FR | Story | Rules | Endpoint | Tasks | Plan section |
|---|---|---|---|---|---|
| FR1 | US-001 | BR-001, BR-006, BR-008, BR-009 | `POST /api/v1/books` | 08, 16 to 21, 26 | backend plan parts 1, 5; `TextPolicy`, `BookService` |
| FR1 | US-002 | BR-010 | `GET /api/v1/books` | 08, 24, 25 | backend part 5 |
| FR1 | US-004 | BR-003, BR-013 | `POST /api/v1/books` | 16, 21, 26 | backend parts 1, 4 |
| FR3 | US-003 | BR-002, BR-007, BR-004 | `PUT`, `DELETE /api/v1/books/{id}/rating` | 33, 34, 35 | backend part 5 |
| FR2 | US-006 | BR-001, BR-004, BR-008 | `PUT /api/v1/books/{id}/status` | 34, 35 | backend part 5 |
| FR1 | US-007 | BR-011 | `GET /api/v1/books/{id}` | 34, 35 | backend part 5 |
| CRUD | US-008 | BR-011 | `DELETE /api/v1/books/{id}` | 34, 35 | backend part 5 |
| Success metric M-3 | US-009 | BR-013 | all writes | 03, 37, 44, 45 | backend part 4 |
| Scope: frontend | US-010 | n/a | static content | 09, 12, 28, 31 | frontend F1 to F3 |
| FR1 | US-011 | n/a | `POST` | 12, 27, 29, 30, 32 | frontend F2, F3 |
| FR2, FR3 | US-012 | BR-004 | status, rating, delete | 38 to 43 | frontend F3 |
| Constitution: frontend standards | US-013 | n/a | all | 11, 27, 29, 49 | frontend F2.3, F3 |
| Constitution: frontend standards | US-014 | n/a | static content | 12, 14, 48, 50, 51, 52, 57 | frontend F3, F4 |
| Cross-cutting | BR-012 (order of evaluation) | | all | 07, 22, 23, 24, 20, 34 | backend part 1 pipeline |
| Cross-cutting | INV-1 to INV-8 | | all | 04, 21, 34, 36, 46 | backend parts 1, 4 |

FR4 has no downstream items by design (DEC-003). Every endpoint has a story and every story has at least one backend or frontend task and one test task. The plan-section column refers to the sections of handoffs 020 and 021 until `plan.md` is merged; it is re-pointed to `plan.md` headings at that point.

## 9. Merge options (if the Sponsor prefers fewer, larger tasks)

Merging changes the number of reviews, not the hours. Each merged task would run 60 to 90 minutes and waives the 45-minute INVEST limit.

| Merge | Gives | Saves |
|---|---|---|
| 03 + 04 | SQLite and Book persistence | 1 |
| 06 + 07 | error envelope and gate | 1 |
| 10 + 13 | E2E toolchain and harness | 1 |
| 11 + 12 | logic, client and page shell | 1 |
| 17 + 18 | text policy | 1 |
| 19 + 20 | strict JSON and validation | 1 |
| 23 + 24 | routing, accept, query, id | 1 |
| 25 + 26 | list filter, capacity, add stories, concurrency | 1 |
| 33 + 34 | rating value and domain rules | 1 |
| 42 + 43 | E2E status, rating, delete | 1 |
| 44 + 45 | start-up safety | 1 |
| 55 + 56 | P1 backend runs | 1 |

With all twelve merges the total is 46 tasks (Increment 0 would be 12; reaching the Sponsor's figure of about 8 would need three more merges of tasks that touch different layers, which is not recommended).

## Changelog
- 0.1 (2026-10-30): First version for the Phase 3 review (handoff 023). Built from handoffs 020 and 021 and the Sponsor decisions of 2026-10-30. Handoff 022 not yet merged.
