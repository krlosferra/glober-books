# GloberBooks Task Backlog

- Owner: PM (`sdd-product-manager`)
- Version: 0.2
- Last updated: 2026-10-30
- Status: Draft (Phase 3, review round 1 of 3). Awaiting the Liaison and Security reviews and the Sponsor gate. Definitions only: task state lives in `status.md`.
- Inputs: `delivery-strategy.md` v1.0, `spec.md` v0.4.2 (draft change requests CR-1 to CR-9 on top of the approved 0.4.1), `constitution.md` 1.1, `security.md`, handoff 022 (lean backend plan, tasks B01 to B27; it replaces handoff 020) and handoff 021 (frontend plan, candidates F0.1 to F4.3).
- Sponsor decisions applied (2026-10-30): spec frozen except change requests; exercise release = Increments 0 to 3 with every P0 row; Increment 4 = P1 hardening, later and time permitting; Increment 0 is a thin walking skeleton; SQLite uses the default rollback journal with exclusive locking (no WAL) and the schema of handoff 022 part A.4; static content by our own controller; `TRACE` answered by the container with a pre-application `405`. Still open and not decided here: whether to accept about 21 h of P0 backend work against the indicative 6 to 8 h (022 question S-1), and where the tag `v0.1` goes (022 question S-2).

## 0. How to read this backlog

**Numbering.** Tasks are numbered 01 to 53 in build order (branch `<type>/task-NN-<slug>`, commit `type(scope): subject [Task-N]`, `git-workflow.md`). The backend architect's ids are kept as "Source: Bnn" and the frontend's as "Source: Fn.n", so handoffs 021 and 022 stay traceable. The constitution's "Task 1" is Increment 0 as a whole (Tasks 01 to 14); section 8 maps each Task 1 proof to its task.

**Files rule (accepted from 022, question L-1).** A task lists at most 4 production and resource entries it creates or edits; its own tests are listed separately ("Tests:") and belong to the task. An entry is one file or a group of closely coupled files in one folder (for example a set of small modules); the build files and the generated Maven wrapper count as one entry. PM remark: this rule hides test volume, so the 45-minute estimate is the binding limit; a task that overruns it is split, never extended. The same rule is applied to the frontend (handoff 021 counted tests as files).

**Estimates** are agent working minutes for build plus own tests, excluding Liaison and Security reviews and the Sponsor gate. **Tier** is P0, P1 or P2 (`delivery-strategy.md` section 4); the classification per row is in section 7.

**Parallelism.** Every contract clause the frontend needs is final in `spec.md`, so frontend tasks may start before the backend tasks they will be tested against; a frontend E2E task starts only when the backend tasks it lists are merged. Backend and frontend tasks never edit the same files.

**Definition of Ready.** Spec clause approved (text marked CR-n counts only after the Sponsor approves the change request), acceptance criteria testable, dependencies DONE, Liaison task brief written.

**Definition of Done** (constitution plus this backlog): `cd backend && ./mvnw verify` green (backend); `cd e2e && npx playwright test` and `node --test "frontend/**/*.test.js"` green (frontend); real output attached; coverage at least 80% (backend); every acceptance criterion mapped to a test; no new dependency, schema change or file outside the scope; `progress.md` and affected documents updated in the same branch; `hygiene` green; Liaison ACCEPT, Security PASS (or PASS WITH CONDITIONS with every condition closed), PM acceptance, Sponsor approval.

**Increment gate.** Before the Sponsor tries an increment: the P0 rows listed for it, `./mvnw verify`, `node --test "frontend/**/*.test.js"`, `npx playwright test` (smoke first), `hygiene`, and the `progress.md` note. Where the tag `v0.N` goes for Increment 0 is a Sponsor decision (section 1).

## 1. Totals and honest estimates

| Increment | Tasks | Backend | Frontend | Effort (sum of estimates) | Elapsed, two agents in parallel, no review waits |
|---|---|---|---|---|---|
| 0 Walking skeleton | 14 (01 to 14) | 9 | 5 | 585 min, about 9.8 h | about 7.2 h |
| 1 Core library | 15 (15 to 29) | 9 | 6 | 625 min, about 10.4 h | about 6.9 h |
| 2 Progress, rating, durability | 11 (30 to 40) | 5 | 6 | 420 min, 7 h | about 3.8 h |
| 3 Interface quality and release gate | 8 (41 to 48) | 2 | 6 | 305 min, about 5.1 h | about 3.9 h |
| **Exercise release (0 to 3)** | **48** | **25** | **23** | **1,935 min, about 32 h** | **about 22 h** |
| 4 P1 hardening | 5 (49 to 53) | 3 | 2 | 210 min, 3.5 h | about 2.5 h |
| All | 53 | 28 | 25 | about 36 h | |

Backend P0 work of Increments 0 to 3 is 1,035 minutes (17.25 h; 022 gives 16.75 h plus the 30-minute sweep of Task 42); with the rework the architect expects (about 25%) it is about 21 h, and the whole release with frontend rework is about 39 h of task time (about 25 to 27 h elapsed).

Findings the Sponsor must see:
1. **Budget (open decision, not decided by the PM).** The indicative 6 to 8 h of DEC-022 is exceeded by a factor of about 3 to 5 with the spec as frozen; planning cannot close the gap, and merging tasks changes the number of reviews, not the hours. Only scope can: the cut list of `spec.md` section 1, or a change-request round that drops Must items (for example BR-006 items 6b to 6d). The PM offers no scope cut without a Sponsor decision.
2. **Increment 0 has 14 tasks, not about 8.** The 8 backend tasks of 022 (B01 to B08) end with an API and no page. Adding what the Sponsor required (the page served, one Playwright smoke, README run instructions, the E2E toolchain and isolation) needs B09 and 5 frontend tasks. Every attempt to merge below 14 breaks the 45-minute limit: the floor is 12 (merging Tasks 05 and 06, and 10 and 13, each to about 75 minutes). The PM's objection to "about 8" is therefore on the number only; features are already minimal (no validation beyond basic, no duplicates, no filter, no dialogs). Alternative that keeps eight tasks: Increment 0 = Tasks 01 to 08 (API skeleton, the 022 map exactly) and a "0b" of Tasks 09 to 14 for the page and smoke, with the tag after Task 14.
3. **Tag `v0.1` (open, Sponsor).** 022 proposes after B09 (page served). Task 14 (smoke, README) is the last task whose gate matches "done when" of Increment 0 in `delivery-strategy.md`; the three options are after Task 08 (API only), after Task 09, after Task 14. The backlog works with any; the PM recommends after Task 14 because the strategy's "done when" includes the smoke and the README.
4. **Total 53 tasks, not about 40.** The merges of section 9 give 41 tasks at 60 to 90 minutes each; recommended only for the pairs marked "low risk".
5. **PM deviation from 022 (for the Liaison and Security):** the release sweep (SpotBugs triage, grep gates, dependency tree against baseline, coverage review) is split out of B27 into Task 42, in Increment 3, because the release definition (criteria 5 and 6) needs it before the Security pre-release verdict; the long runs stay in Increment 4 (Task 51).
6. **P1 rows after the release (022 question P-2).** `spec.md` section 1 allows only measurements to be cut. Tasks 49 and 50 are not measurements. The PM recommends running Tasks 49 and 50 (90 minutes) before the release tag; otherwise the delivery report must list those rows as open, as the spec requires.

## 2. Increment 0: walking skeleton (Tasks 01 to 14)

Goal: a running application that adds and lists books through the API and the page, with every gate green. Gate rows (P0): E-04, E-07 to E-11, E-12 to E-14, E-16, E-21, E-23, E-09 (declared and chunked rows), E-15 and E-39 (application rows) and E-30 as exercised by Task 09, mandatory properties effective, US-010.1, US-011.1, US-011.2, US-014.6 (smoke), `hygiene` green. Deferred to Increment 1: route table, `Accept`, query and id checks, container error path, text policy, duplicates, capacity, filter, dialogs.

### Task 01: Maven scaffold and build proofs
- Source: B01 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: none | Branch: `chore/task-01-maven-scaffold`
- Description: Create the Maven project with the checksum-verified wrapper, pinned parent and plugins, the Surefire and Failsafe split, the merged JaCoCo gate, `-Xlint:all -Werror`, the `frontend/` to `static/` resources entry, the committed dependency tree and the classpath ban test.
- Production: (1) build files (`backend/pom.xml`, wrapper set, `.mvn/**`); (2) `GloberBooksApplication.java`; (3) `backend/config/dependency-tree.txt`. Tests: `BuildSplitProofIntegrationTest`, `BuildSplitProofTest`, `RuntimeClasspathBanTest`.
- Acceptance criteria:
  1. `cd backend && ./mvnw verify` is green on the pinned JDK; wrapper Maven 3.9.16 with `distributionSha256Sum`, `--strict-checksums` in `.mvn/maven.config`; a run with a wrong checksum fails (recorded) (SC-19, SC-36).
  2. A `*Test` runs in Surefire and a `*IntegrationTest` in Failsafe; JaCoCo merges both and `check` (line coverage 80%) is bound to `verify`; a recorded run with an impossible threshold fails `verify`.
  3. A deliberate compiler warning fails the build (`-Xlint:all -Werror`, recorded).
  4. The committed tree equals `dependency:tree`, holds no banned coordinate, names every transitive exception, and `RuntimeClasspathBanTest` passes.
  5. The jar is `globerbooks.jar`, contains `frontend/` as `static/` without `*.test.js`, `*.map` or dotfiles, and no test class.
- Traces: constitution Task 1 proofs, SC-19, SC-36.

### Task 02: Quality gates
- Source: B02 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 01 | Branch: `chore/task-02-quality-gates`
- Description: Add Checkstyle 14.3.0 (plugin 3.6.0) with the constitution rules and regex bans, and SpotBugs with Find Security Bugs, verifying the unverified pins or requesting Sponsor re-approval, with a justified exclusion file.
- Production: (1) build files; (2) `config/checkstyle/checkstyle.xml` and `suppressions.xml`; (3) `config/spotbugs/exclude.xml`. Tests: none (seeded violations are recorded runs).
- Acceptance criteria:
  1. Checkstyle runs in `verify` with naming (3 characters, `id` exception), 300-line and 50-line limits, imports, Javadoc, line length and regex bans for wall-clock calls, `System.out`, `printStackTrace` and `select *`; one seeded violation of each ban fails the build (recorded).
  2. Checkstyle 14.3.0 with plugin 3.6.0 is shown to work together, or a re-approval request with working pins is in the delivery (SC-19).
  3. The SpotBugs and Find Security Bugs pins are chosen after checking publishers and Java 21 support, bound to `verify`; every exclusion has a written justification (SC-21).
  4. `verify` is green with the gates active.
- Traces: SC-07, SC-14, SC-19, SC-21, SC-27.

### Task 03: SQLite store, configuration and clock
- Source: B03 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 01 | Branch: `feature/task-03-sqlite-store`
- Description: Build the single-connection SQLite data source with fixed settings (rollback journal never changed, `synchronous=FULL`, exclusive locking and claim, immediate transactions), creation of the directory (0700) and file (0600), the private native-library directory, `schema.sql`, `application.yml` with every mandatory property and the limits, and the clock.
- Production: (1) `config/sqlite/SqliteDataSourceProvider.java`; (2) `config/ClockConfig.java`; (3) `schema.sql`; (4) `application.yml`. Tests: `SqliteStoreIntegrationTest`.
- Acceptance criteria:
  1. A missing directory is created 0700 and a missing file 0600 before the first connection; after a write, every file the storage creates next to it is not wider (US-009.3, SC-13).
  2. Settings read back on the live connection equal the plan: default rollback journal (any other value stops the start), `synchronous` FULL, `locking_mode` EXCLUSIVE, `foreign_keys` on, `busy_timeout` 5000, `trusted_schema` off, pool 1 with `max-lifetime 0`; loading an extension fails (SC-32, N-3).
  3. The claim (`BEGIN EXCLUSIVE`, `COMMIT`, no change) makes a second connection fail with "database is locked" and leaves the file hash unchanged (US-009.7, N-2); if it cannot, the fallback is reported to the PM.
  4. `org.sqlite.tmpdir` is `<data dir>/tmp` (0700); if the driver cannot load from it the property is dropped and the risk goes to `security.md` through a handoff (X-10).
  5. `schema.sql` runs at every start; `application.yml` holds every mandatory property of the constitution and the Tomcat and Hikari limits.
- Traces: US-009.3, US-009.7, M-3, SC-01, SC-13, SC-32, SC-37.

### Task 04: Domain and repository
- Source: B04 | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 03 | Branch: `feature/task-04-domain-repository`
- Description: Add the `Book` entity, the status enum with converter, the repository and the book response, and prove Hibernate `validate` on the STRICT table.
- Production: (1) `domain/Book.java`; (2) `domain/BookStatus.java`; (3) `repository/BookRepository.java`; (4) `dto/BookResponse.java`. Tests: `BookRepositoryTest`.
- Acceptance criteria:
  1. `ddl-auto: validate` passes with the STRICT table and CHECKs (spike result recorded; fallback of the plan applied and reported if it fails) (X-06).
  2. The database rejects a bad id, title or author length, status, rating range, a rating on a non-completed book, `date_completed` not tied to completed status, and a malformed timestamp (INV-1 to INV-4).
  3. A saved book returns with the same millisecond timestamps and lower-case status; the ordered query gives `date_added` descending then `id` ascending, ties included (US-002.2, BR-010).
  4. `Book` has no public setters; no query concatenates SQL or uses `select *` (SC-07).
- Traces: Section 4, INV-1 to INV-4, BR-010.

### Task 05: Error foundation
- Source: B05 | Layer: backend | Tier: P0 | Estimate: 35 | Depends on: 03 | Branch: `feature/task-05-error-foundation`
- Description: Add the 16 error codes with their statuses, the envelope records, the single factory and the writer used by filters.
- Production: (1) `error/ErrorCode.java`; (2) `error/ErrorBody.java` (with nested field problem); (3) `error/ErrorBodyFactory.java`; (4) `error/ErrorResponseWriter.java`. Tests: `ErrorBodyFactoryTest`.
- Acceptance criteria:
  1. `ErrorCode` holds the 16 codes with the HTTP status of Appendix A; a body has exactly `error`, `code`, `timestamp`, plus `details` only in the shapes of Appendix A (INV-6).
  2. The timestamp comes from the injected clock in the fixed format.
  3. No message contains user input, class names, SQL or paths; no `details` field is of type `Object` (SC-08, SC-11).
  4. The writer sets `application/json; charset=UTF-8` explicitly and writes only the envelope (SC-29).
- Traces: Appendix A, INV-6, SC-29.

### Task 06: Thin gate and exception advice
- Source: B06 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 05 | Branch: `feature/task-06-thin-gate`
- Description: Add the request filter that runs first: security headers, Host, Origin, Sec-Fetch-Site, Content-Type and the 16 KiB body cap (declared and chunked), and the exception advice with the generic 500.
- Production: (1) `config/transport/RequestGateFilter.java`; (2) `config/transport/HeaderPolicy.java`; (3) `config/transport/BoundedBodyRequest.java`; (4) `error/GlobalExceptionHandler.java`. Tests: `HeaderPolicyTest`, `RequestGateFilterTest`, `GlobalExceptionHandlerTest`.
- Acceptance criteria:
  1. The security headers are set on every response and again after `reset` or `sendError` (SC-10).
  2. Forged Host forms give `400 INVALID_HOST`; allowed forms pass; a bad `Origin` or `Sec-Fetch-Site` gives `403 FORBIDDEN`; a non-GET request without a valid `Content-Type` gives `415` (BR-012 step 1, SC-03, SC-04, SC-05, SC-28).
  3. A declared body over 16,384 bytes is `413` before any read, no `100 Continue`; a chunked or unread body is read into a buffer and stopped at byte 16,385 (SC-06).
  4. An unexpected exception gives `500 INTERNAL_ERROR` "Unexpected error." with one stack-trace log line and no user text; the advice answers JSON even for `Accept: text/xml` (SC-11, SC-29).
- Traces: BR-012 step 1, SC-03 to SC-06, SC-10, SC-11, SC-28.

### Task 07: Add and list API (thin)
- Source: B07 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 04, 06 | Branch: `feature/task-07-add-and-list-thin`
- Description: Implement `POST /api/v1/books` and `GET /api/v1/books` with basic validation only (replaced in Task 19) and `allow_duplicate` accepted without effect (until Task 23).
- Production: (1) `service/BookService.java`; (2) `controller/BookController.java`; (3) `dto/CreateBookRequest.java`; (4) `dto/BookListResponse.java`. Tests: `BookServiceTest`, `BookControllerTest`.
- Acceptance criteria:
  1. A valid title and author give `201` with the seven members, `unread`, null rating and `date_completed`, lower-case UUID `id`, millisecond `date_added` (US-001.1, US-001.2, 5.2).
  2. The list gives `200` with `books` and `total`, newest first, ties by `id`; empty gives `[]` and 0 (US-002.1 to .3, .5).
  3. Empty or over-length title or author gives `400 VALIDATION_ERROR` with the field; an unknown member gives `400 MALFORMED_REQUEST`.
  4. Add is one transactional service method, list a read-only one, and a read writes nothing (X-07); controllers hold no rule.
- Traces: US-001.1, US-001.9 (via list), US-002.1, 5.1, 5.2.

### Task 08: Real-port baseline and mandatory-property proofs
- Source: B08 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 07 | Branch: `test/task-08-real-port-baseline`
- Description: Add the raw-socket client and the first real-port security class, a round trip with checkpoint logs, and a test per mandatory property.
- Production: none. Tests: `RawHttpClient`, `TransportSecurityIntegrationTest`, `MandatoryPropertiesIntegrationTest`.
- Acceptance criteria:
  1. Headers of the constitution on every response class that exists (200, 201, 400, 403, 404, 413, 415, 500 by trigger), and no `Server`, `Set-Cookie`, `Location` or `Access-Control-*` (E-21, E-23, SC-10).
  2. Host, Origin, Fetch-Metadata, Content-Type and body-cap rows give the stated results, including a chunked body crossing 16,384 bytes (E-04, E-07 to E-14, E-09, SC-28, SC-30).
  3. A `POST` then `GET` round trip passes on the real port; the checkpoint logs contain no book data.
  4. Every mandatory property is shown effective, with Boot 4 and Jackson 3 key names for naming and unknown fields; a wrong key is corrected and reported (constitution, SC-31 partial); the log pattern removes CR, LF, U+2028, U+2029, U+0085 and ESC (SC-37 pattern).
- Traces: SC-30, E-04, E-07 to E-14, E-21, E-23, integration checkpoints.

### Task 09: Static content
- Source: B09 | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 08 | Branch: `feature/task-09-static-content`
- Description: Serve the page and its assets from a fixed catalog built at start from the configured static location, with the framework's resource handler off.
- Production: (1) `config/transport/StaticAssetCatalog.java`; (2) `controller/StaticContentController.java`; (3) `application.yml`. Tests: `StaticContentIntegrationTest` (fixture directory chosen by property).
- Acceptance criteria:
  1. `GET /` and `/index.html` return `200` with the same bytes; `.html`, `.css` and `.js` return `text/html;charset=UTF-8`, `text/css;charset=UTF-8`, `text/javascript;charset=UTF-8`, with the constitution headers (E-16, 5.8).
  2. No `ETag`, `Last-Modified` or `Accept-Ranges`; `Range`, `If-None-Match` and `If-Modified-Since` are ignored and give `200` with the full body (E-30).
  3. Directories, `/favicon.ico`, a `*.test.js` path, `/error`, `/.git/config`, `/data/globerbooks.db` and traversal forms give `404 NOT_FOUND` in the envelope (E-15 application rows, E-39, SC-12).
  4. Any file with an extension other than `.html`, `.css`, `.js` under the static location stops the start; the catalog is fixed at start.
- Traces: 5.8, SC-12, E-15, E-16, E-30, E-39.

### Task 10: E2E toolchain and pins
- Source: F0.1 | Layer: frontend | Tier: P0 | Estimate: 30 | Depends on: none | Branch: `chore/task-10-e2e-toolchain`
- Description: Create the E2E package with exact pins and install rules, check the Node pin and verify the `@playwright/mcp` pin in one run.
- Production: (1) `e2e/package.json` and `e2e/package-lock.json`; (2) `e2e/.npmrc`; (3) `.nvmrc`. Tests: none.
- Acceptance criteria:
  1. `.nvmrc` and `engines.node` hold the pinned Node and `node --version` equals it.
  2. `npm ci` with `.npmrc` (`ignore-scripts`, `save-exact`, `engine-strict`) installs `@playwright/test` 1.64.1; `npx playwright --version` and `npx playwright install chromium` succeed; no `typescript` package and no `tsconfig` (SC-19).
  3. `npm audit --audit-level=high` is clean; all lockfile `resolved` URLs are on `https://registry.npmjs.org/` (SC-20, SC-21, SC-36).
  4. `@playwright/mcp` 0.0.83 runs once with the explicit flags; browser disk use and tool names are recorded (SC-23).
- Traces: constitution pins, SC-19 to SC-21, SC-23, SC-36.

### Task 11: Frontend logic and API client (thin)
- Source: F0.3 + F0.4 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: none | Branch: `feature/task-11-logic-and-client-thin`
- Description: Add the DOM-free modules for exact copy, formatting and id checks and the single network module with list and add, proving the unit test command on the pinned Node.
- Production: (1) `frontend/js/logic/` (`copy.js`, `format.js`, `ids.js`); (2) `frontend/js/api/http.js`; (3) `frontend/js/api/booksApi.js`. Tests: next to each module, and `frontend/js/sources.test.js`.
- Acceptance criteria:
  1. `node --test "frontend/**/*.test.js"` runs the ES-module tests on the pinned Node without a `package.json`; if not, the delivery reports a constitution issue.
  2. Status labels, "n books" ("1 book", "0 books", "1000 books"), `Added: YYYY-MM-DD` from a UTC value in the local time zone (test sets `TZ`), UUID check and encoded path builder (US-010.3, US-010.4, BR-011).
  3. The client sends only root-relative `/api/v1/books` URLs with `Accept` and, on non-GET, `Content-Type: application/json`, `allow_duplicate` only when true; maps snake_case to camelCase; never throws for an HTTP outcome and returns `ok` or a failure of kind `api`, `network` or `unreadable` (US-013.1, US-013.3).
  4. A request without a complete response after 15 seconds counts as `network` (US-013.6 as changed by CR-4).
  5. The sources contain none of the forbidden sinks (SC-09).
- Traces: US-010.3, US-010.4, US-013, BR-011, SC-09.

### Task 12: Page shell with minimal list and add
- Source: F0.5 | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 11 | Branch: `feature/task-12-page-shell`
- Description: Build the single page with its semantics and stylesheet, the list from a `<template>` and the add form; filter, errors and dialogs follow in Increment 1.
- Production: (1) `frontend/index.html` and `frontend/css/styles.css`; (2) `frontend/js/ui/dom.js` and `listView.js`; (3) `frontend/js/ui/addForm.js`; (4) `frontend/js/ui/app.js` and `frontend/js/main.js`. Tests: none beyond Task 11 logic tests (page behavior is tested in Task 14).
- Acceptance criteria:
  1. `lang="en"`, the title and the single `h1` "GloberBooks", a main landmark, "Your books", a list named "Books", the form with Title, Author, Status (default Unread) and "Add book", focus on Title (US-010.1, US-011.1, US-014.4 without the 320 clause).
  2. Entries show title, author, status label, "Added: YYYY-MM-DD" and the rating rules through `textContent` only; count "n books"; API order; "Loading your books…" in a status element (US-010.2, .3, .4, .8).
  3. Title, Tab, author, Enter sends exactly one `POST`, prepends the book, clears the form, announces "Book added: <title>" and returns focus to Title (US-011.2, .4, .5).
  4. Only `/css/styles.css` and `/js/**` are requested; no inline script, style or handler, no other origin (US-014.7, SC-10).
- Traces: US-010.1 to .4, .8, US-011.1, .2, .5, US-014.4, .7.

### Task 13: E2E harness (config, launcher, isolation)
- Source: F0.2 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 10, 08 | Branch: `test/task-13-e2e-harness`
- Description: Add the Playwright configuration, the launcher that starts the jar on a temporary database directory and the global setup asserting the isolation rules, with a placeholder test.
- Production: (1) `e2e/playwright.config.ts`; (2) `e2e/global-setup.ts`; (3) `e2e/support/start-server.mjs`. Tests: `e2e/tests/harness.spec.ts` (placeholder: `GET /api/v1/books` is `200`).
- Acceptance criteria:
  1. `baseURL` is the literal `http://127.0.0.1:18080`; the setup asserts host, port and that the jar exists; neither is read from the environment; a busy port fails the run (SC-33).
  2. The launcher creates an unpredictable directory 0700, asserts the database path inside it, sets `SERVER_ADDRESS`, `SERVER_PORT`, `SPRING_DATASOURCE_URL` explicitly, forwards termination, deletes only its directory, and never opens the database file (it is locked).
  3. The order of web-server start and global setup in the pinned Playwright is established by a run and recorded as the evidence for Q-F7.
  4. The placeholder gets `200` from the readiness URL.
- Traces: SC-33, SC-23, plan frontend F4.2.

### Task 14: Smoke tests, guard fixture and README
- Source: F0.6 (+ README of Increment 0) | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 12, 13, 09 | Branch: `test/task-14-smoke-guard-readme`
- Description: Add the guard fixture and the three Increment 0 smoke tests, explore the page once with the browser tool and write the run instructions, proving them on a clean clone.
- Production: (1) `e2e/support/fixtures.ts`; (2) `e2e/support/builders.ts` and `library.ts`; (3) `README.md`. Tests: `e2e/tests/us-010-library-view.spec.ts` (smoke).
- Acceptance criteria:
  1. `npx playwright test --grep @smoke` is green in under 30 s: page loads with title and one `h1` (US-010.1); keyboard-only add shows the entry and count and the time is recorded (US-011.2, M-1); `<script>alert(1)</script>` is inert text (US-014.6, T-12).
  2. The guard fails a test on a `securitypolicyviolation`, a Trusted Types console error, a request to another origin or a page error (US-014.6, .7, SC-10) and checks `Content-Type` and the constitution headers of every asset (E-16).
  3. README commands for build, run (`cd backend && ./mvnw spring-boot:run`, page at `http://127.0.0.1:8080/`), E2E setup, smoke, logic tests and the note that the database is locked while running work on a clean clone with only the pinned JDK and Node (executed, output attached).
  4. `./mvnw verify`, `node --test "frontend/**/*.test.js"`, the smoke run and `hygiene` are green; `progress.md` gets the Task 1 proof table with evidence and the pins that remain unverified, if any.
- Traces: US-010.1, US-011.2, US-014.6, US-014.7, M-1, SC-10, SC-23, Task 1 proofs.

## 3. Increment 1: core library (Tasks 15 to 29)

Goal: the reader manages a list: full validation, filter, duplicates, capacity, dialogs. Gate rows (P0): all P0 families of section 7.1 whose tasks are in this increment, in particular E-22 live, E-33, T-31, C-01 to C-04, C-06, C-07, C-09, C-10, C-11 (a) and (b), US-010.1 to .8, .10 and US-011.1 to .10.

### Task 15: Route table, methods and Accept
- Source: B10 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 09 | Branch: `feature/task-15-routing-accept`
- Description: Decide route and method from the raw request target, answer `404` and `405` with the exact `Allow`, and apply the `Accept` grammar on API paths.
- Production: (1) `config/transport/RequestTarget.java`; (2) `config/transport/RouteTable.java`; (3) `config/transport/AcceptPolicy.java`; (4) `config/transport/RequestGateFilter.java` (edit). Tests: `RouteTableTest`, `AcceptPolicyTest`.
- Acceptance criteria:
  1. Unknown paths, trailing slash, suffix, `;`, encoded variants of fixed segments and encoded dot segments give `404 NOT_FOUND`; HEAD, OPTIONS and other methods on a defined path give `405` (E-01 to E-03, E-05, E-17, E-24 rows 2 and 3, E-27).
  2. Every `405` carries `Allow` with exactly the defined methods and never HEAD or OPTIONS (E-29).
  3. `Accept: text/xml` gives `406`; absent, empty, `*/*`, `application/json` give `200`; error bodies stay JSON (E-06).
  4. `RouteTableTest` enumerates the controllers' mappings and fails on any difference (X-11, SC-18, SC-40).
- Traces: 5.0 routing, methods, Accept, BR-012 step 2.

### Task 16: Query, id and empty-body rules; transport limits
- Source: B11 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 15 | Branch: `feature/task-16-query-id-limits`
- Description: Add the query grammar, the id check judged as received, the no-body rule for GET and DELETE, the early rejection of large declared bodies and the live slow-request test.
- Production: (1) `config/transport/QueryPolicy.java`; (2) `config/transport/RequestGateFilter.java` (edit); (3) `config/transport/BoundedBodyRequest.java` (edit). Tests: `QueryPolicyTest`, `TransportLimitsIntegrationTest`.
- Acceptance criteria:
  1. A malformed id (upper case, wrong length, any `%`) gives `400 MALFORMED_REQUEST` after route and method are known (T-14, E-24 row 1, E-27).
  2. A non-empty query on any endpoint but the list, unknown or repeated parameters, empty pairs, malformed percent-encoding and invalid UTF-8 give `400 MALFORMED_REQUEST`; `?` and `?status=%72eading` pass; 2,000 pairs give `400` (E-19, E-26, E-36 third row, E-38, T-20).
  3. Any body byte on GET or DELETE gives `400 MALFORMED_REQUEST`; a chunked body over the cap gives `413` on any route (E-18, E-32, T-39).
  4. `Expect: 100-continue` with an oversized length gets `413` and no `100`; a 415-rejected request declaring 10 MiB answers within 2 s while others are served; a connection with no complete request within 5 s is closed while others are served (E-31 `Expect` row, E-33, E-22 live).
- Traces: BR-011, BR-012 steps 3 to 5, E-18, E-19, E-22, E-26, E-31, E-32, E-33, E-38, T-20, T-39.

### Task 17: Container error path and fail-closed start
- Source: B12 | Layer: backend | Tier: P0 | Estimate: 35 | Depends on: 06 | Branch: `feature/task-17-error-path-startup-policy`
- Description: Turn any `sendError` inside the application into the envelope and refuse to start on a non-loopback bind address or a datasource URL that is not `jdbc:sqlite:<file>`.
- Production: (1) `config/transport/EnvelopeResponseWrapper.java`; (2) `config/sqlite/StartupPolicy.java`; (3) a Tomcat customizer only if the error valve needs it. Tests: `EnvelopeResponseWrapperTest`, `StartupPolicyTest`.
- Acceptance criteria:
  1. A `sendError` raised inside the application leaves as the envelope with the security headers, never as a container page (SC-02, SC-11, INV-6).
  2. `SERVER_ADDRESS=0.0.0.0`, `:memory:`, a URL with parameters or another scheme stop the start with a fixed reason without book data (SC-39, SC-01).
  3. `GET /error` is `404 NOT_FOUND` in the envelope.
- Traces: SC-02, SC-11, SC-29, SC-39, INV-6.

### Task 18: Text policy, part 1, and the Unicode fixture check
- Source: B13 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 01 | Branch: `feature/task-18-text-policy-1`
- Description: Prove the Unicode data on the pinned JDK, build the code-point fixtures and implement items 1 to 6 of BR-006.
- Production: (1) `service/TextPolicy.java`. Tests: `TextPolicyTest`, `UnicodeFixtureCheckTest`, `support/UnicodeFixtures`.
- Acceptance criteria:
  1. The fixture check asserts JDK at least 21.0.12, Unicode 15.0 (U+1E030 assigned, U+2FFC not), the category of every code point the spec names and the NFC result of the T-24b and T-25 inputs; a mismatch stops the task and goes to the PM.
  2. Trimming uses Unicode White_Space; a tab or line feed inside is rejected, at an end trimmed; stored text is NFC (T-02, T-08, US-001.5).
  3. Length in code points after NFC: 500/501 title, 200/201 author, 500 decomposed "é" accepted, 300 times U+1D15E rejected (T-06, T-07, T-11, T-22).
  4. NUL and other Cc, Cf except U+200C and U+200D, Zl, Zp, private use, noncharacters, unassigned (15.0) and tag code points are rejected; U+FFFD between letters is accepted (T-01, T-03, T-09, T-26).
- Traces: BR-006 items 1 to 6, SC-16.

### Task 19: Text policy, part 2
- Source: B14 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 18 | Branch: `feature/task-19-text-policy-2`
- Description: Implement items 6b to 6d of BR-006: visible content, mark runs and joiner adjacency.
- Production: (1) `service/TextPolicy.java` (edit). Tests: `TextPolicyMarksTest`, `UnicodeFixtures` (edit).
- Acceptance criteria:
  1. Blank-rendering text and text beginning with a mark are rejected; "ab", U+3164, "cd" is accepted (T-23).
  2. A run of 9, or with two variation selectors, is rejected; 8, and one selector, accepted (T-24, T-24b).
  3. Joiner positional rule: the Persian word with U+200C and a family emoji sequence accepted; first, last, next to a joiner or a space rejected (T-10, T-25).
  4. The 50-repetition input of T-24b finishes without timeout or unbounded regular expression.
- Traces: BR-006 items 6b to 6d, DEC-029, SC-16.

### Task 20: Validation wiring and field-error mapping
- Source: B15 | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 19, 06 | Branch: `feature/task-20-validation-wiring`
- Description: Wire the text policy into Bean Validation without expression language and choose `VALIDATION_ERROR`, `INVALID_STATUS` or `INVALID_RATING` by BR-012 step 6; the service repeats the checks.
- Production: (1) `config/BookText.java`; (2) `config/ValidatorConfig.java`; (3) `error/GlobalExceptionHandler.java` (edit); (4) `dto/CreateBookRequest.java` (edit). Tests: `BookTextTest`, `GlobalExceptionHandlerTest` (edit).
- Acceptance criteria:
  1. Title and author failures give `400 VALIDATION_ERROR` with one `details` entry per failing field ordered title, author, status; `{}` gives title and author entries (US-001.4, .10, T-09).
  2. Only `status` failing gives `INVALID_STATUS`; `status` plus another field gives `VALIDATION_ERROR`; absent status is `unread`, explicit `null` is `INVALID_STATUS` (T-34, T-35, BR-001).
  3. A 501-character title containing `${7*7}` is rejected and no message contains `${7*7}` or `49` (T-13).
  4. The thin checks of Task 07 are removed; the service throws the same exception type.
- Traces: US-001.4 to .6, .10, BR-001, BR-012 step 6.

### Task 21: Strict body reading and parser limits
- Source: B16 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 07 | Branch: `feature/task-21-strict-json`
- Description: Decode the buffered body with a reporting UTF-8 decoder and parse it strictly, mapping every failure to `MALFORMED_REQUEST` within 300 ms under low read constraints.
- Production: (1) `config/json/JacksonConfig.java`; (2) `config/json/StrictJsonBodyResolver.java`; (3) `config/json/StrictStringDeserializer.java`; (4) `config/WebMvcConfig.java`. Tests: `JacksonStrictnessTest`, `StrictJsonBodyResolverTest`.
- Acceptance criteria:
  1. Invalid UTF-8, BOM, comments, trailing comma, single quotes, unquoted names, raw control character in a string, trailing content, zero-byte body, non-object value each give `400 MALFORMED_REQUEST` (T-05, T-28, T-29, T-39).
  2. Unpaired surrogate, unknown member, duplicate member (also after unescaping), wrong type, `allow_duplicate` as `"true"`, `1`, `null` give `400 MALFORMED_REQUEST` (T-04, T-16, T-21, T-27, T-30).
  3. A string up to the 16 KiB cap is a length error, not a parse error (T-11); deep nesting and huge exponents answer within 300 ms, never `500` (T-31).
  4. Each Jackson 3 switch is proved effective by a test; a key the framework does not recognize is corrected and reported (SC-31, SC-08).
- Traces: 5.0 strict parsing, BR-012 step 5, SC-31, T-04, T-05, T-16, T-21, T-27 to T-31, T-39.

### Task 22: Duplicate key and service exceptions
- Source: B17 | Layer: backend | Tier: P0 | Estimate: 30 | Depends on: 18 | Branch: `feature/task-22-duplicate-key`
- Description: Implement the BR-003 key and the service exceptions with the duplicate body.
- Production: (1) `service/DuplicateKey.java`; (2) `error/ApiException.java` and `error/DuplicateBookException.java`; (3) `error/DuplicateErrorBody.java`. Tests: `DuplicateKeyTest`.
- Acceptance criteria:
  1. NFKC, `toLowerCase(Locale.ROOT)`, collapsing of white-space runs and trim: "Clean Code" matches " clean   code " and the full-width "Ｃlean Code" (US-004.5).
  2. "Straße" and "STRASSE" do not match; "Clean Code" and "CLEAN CODE" by one author do (T-40).
  3. The ASCII fast path equals the general path on a generated set.
  4. The duplicate body carries `existing_book` as one book and nothing else (Appendix A).
- Traces: BR-003, US-004.5, T-40.

### Task 23: Add rules and list
- Source: B18 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 20, 21, 22 | Branch: `feature/task-23-add-rules-and-list`
- Description: Implement capacity and duplicate detection inside the one add transaction, the override, the 409 bodies, the list filter and order, and the end-to-end add stories with a seeded library.
- Production: (1) `service/BookService.java`; (2) `repository/BookRepository.java` and `BookKeyRow.java`; (3) `controller/BookController.java`. Tests: `BookServiceTest`, `DuplicateAndCapacityIntegrationTest`, `support/LibrarySeeder`.
- Acceptance criteria:
  1. A matching key gives `409 DUPLICATE_BOOK` with the earliest match and stores nothing; `allow_duplicate: true` stores a separate book; `false` is absent; status, rating and dates do not matter (US-004.1 to .4, .6, .7).
  2. At 5,000 books every add is `409 LIBRARY_FULL` before the duplicate check; list, status change, rating and delete still work; one delete frees one slot (C-01 to C-04, US-001.7, US-004.9, US-008.3).
  3. `?status=` filters in the same order with `total` of the result; `READING`, empty and `reading+` give `400 INVALID_STATUS`; the list never exceeds 5,000 entries (US-002.4 to .8, BR-010).
  4. Injection payloads are stored unchanged and the count grows by one each time (T-12).
  5. With 4,999 books of maximum size, a non-matching add completes in 2 s with `201` and a duplicate is `409` in 2 s, never `500`; if not, stop and escalate the key-column schema change (C-11 a and b, X-04).
- Traces: US-001, US-002, US-004, BR-003, BR-009, BR-010, INV-5, INV-7, T-12, T-40.

### Task 24: Frontend text and failure logic
- Source: F1.1 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 11 | Branch: `feature/task-24-text-and-failures-logic`
- Description: Add the code-point length check and the table mapping every failure to the action and exact text of Section 5.9.
- Production: (1) `frontend/js/logic/text.js`; (2) `frontend/js/logic/failures.js`; (3) `frontend/js/logic/copy.js` (edit). Tests: next to each module.
- Acceptance criteria:
  1. Length after trim and NFC: 500 emoji count 500, 500 decomposed "é" count 500, 300 times U+1D15E count 600; empty/whitespace `empty`; 501 and 201 `length` (US-011.3, T-07, T-22).
  2. Every `code` of Appendix A and the three non-envelope cases map to the action and exact text of 5.9; an unknown code is `INTERNAL_ERROR` (US-013.1, .3).
  3. `VALIDATION_ERROR` without usable `details` maps to the `MALFORMED_REQUEST` text (CR-7); the server's `error` text is never displayed (US-013.5).
  4. Every quoted string of 5.9 and US-011 to US-013 is asserted exactly.
- Traces: US-011.3, .6, US-013.1, .3, .5, 5.9.

### Task 25: List state, filter and list states
- Source: F1.2 + F1.3 | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 24, 12 | Branch: `feature/task-25-list-state-filter`
- Description: Add the pure page-state model and use it for the filter, the loading, empty, filter-empty and failed states, the count and stale-response discarding.
- Production: (1) `frontend/js/logic/listState.js`; (2) `frontend/js/ui/app.js` and `listView.js` (edit); (3) `frontend/index.html` and `css/styles.css` (edit). Tests: `listState.test.js`.
- Acceptance criteria:
  1. Radio group "Filter by status" with All, Unread, Reading, Completed, All checked; choosing loads with the matching `status` (US-010.5).
  2. A new load removes entries and count and shows only "Loading your books…"; an earlier response is discarded (US-010.2 as changed by CR-2, US-010.10).
  3. Empty library and empty filter result messages with "Show all books", decided by the count (US-010.6).
  4. A failure shows the 5.9 text and "Try again" repeating the active filter, zero list items, no count; an add before the first success, during a load or after a failure is not inserted (US-010.7 as changed by CR-2).
  5. The count is `total` plus matching adds minus deletes and is unchanged by a non-matching add (US-010.4).
- Traces: US-010.2, .4 to .7, .10, CR-2.

### Task 26: Add form errors and in-flight guard
- Source: F1.4 | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 24, 25 | Branch: `feature/task-26-add-form-errors`
- Description: Add the client checks, field messages, the single-request guard, the capacity and generic failure messages and the page-level message area.
- Production: (1) `frontend/js/ui/addForm.js`; (2) `frontend/js/ui/messages.js`; (3) `frontend/index.html` and `css/styles.css` (edit). Tests: logic tests only.
- Acceptance criteria:
  1. Empty or too long input shows an inline message linked to its field (informative wording, CR-3) and sends no request (US-011.3).
  2. While a request runs the button is disabled and a second click or Enter sends nothing, enforced in the handler (US-011.4).
  3. `VALIDATION_ERROR` shows each message beside its field, the status text beside Status, and focus goes to the first field in error; `INVALID_STATUS` shows the status text (US-011.6).
  4. `LIBRARY_FULL` shows "Your library is full (5,000 books). Delete a book to add another." and any other failure its 5.9 text; typed values are kept (US-011.9, .10, US-013.2).
  5. Each failure replaces the text of the "Messages" status area once (US-013.4 as defined by CR-5).
- Traces: US-011.3, .4, .6, .9, .10, US-013.2, .4.

### Task 27: Duplicate dialog
- Source: F1.5 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 26 | Branch: `feature/task-27-duplicate-dialog`
- Description: Add the modal "Possible duplicate" dialog with the existing book, Cancel and Add anyway flows and the focus rules.
- Production: (1) `frontend/js/ui/dialogs.js`; (2) `frontend/js/ui/addForm.js` (edit); (3) `frontend/index.html` and `css/styles.css` (edit). Tests: none beyond Task 29.
- Acceptance criteria:
  1. `409 DUPLICATE_BOOK` opens the dialog with "This book may already exist. Add anyway?" and the existing title, author, status label and "Added: YYYY-MM-DD" as text; initial focus on Cancel; Escape cancels (US-011.7).
  2. Cancel closes, keeps values and focuses Title (US-011.8).
  3. Add anyway closes, resends with `allow_duplicate: true` and then behaves as an add; a failure leaves focus on Title (US-011.8, US-011.5, CR-1).
  4. Hostile and 500-code-point texts in the dialog are inert (US-014.6).
- Traces: US-011.7, .8, CR-1, US-014.6.

### Task 28: E2E library view, filter and states
- Source: F1.6 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 25, 23 | Branch: `test/task-28-e2e-library-view`
- Description: Test the list stories against the real backend and with stubbed responses for states the page cannot provoke.
- Production: (1) `e2e/support/stubs.ts`; (2) `e2e/support/library.ts` (edit). Tests: `e2e/tests/us-010-library-view.spec.ts` (edit).
- Acceptance criteria:
  1. Titles, one `h1`, list name, loading text, entry content with the local date for a fixed UTC value in a far time zone, rating texts (US-010.1 to .3, .8).
  2. Order, count texts, filter options, empty and filter-empty messages and "Show all books" (US-010.4 to .6).
  3. A stubbed failure shows the 5.9 text and "Try again"; no list item exists until a load succeeds (US-010.7).
  4. Overlapping loads (held responses) show the last request's result (US-010.10).
- Traces: US-010, T-12 display.

### Task 29: E2E add, validation and duplicate flows
- Source: F1.7 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 27, 23 | Branch: `test/task-29-e2e-add-and-duplicate`
- Description: Test the add stories end to end, the stubbed validation and capacity paths and the duplicate flow; add the fourth smoke test.
- Production: (1) `e2e/support/pages.ts`. Tests: `us-011-add-book.spec.ts`, `us-011-duplicate.spec.ts`.
- Acceptance criteria:
  1. Defaults and focus, keyboard add, client checks without a request, double click and double Enter send one request (held response), confirmation and focus (US-011.1 to .5).
  2. A forbidden character shows the server's message beside the field; stubbed status entry, `INVALID_STATUS`, `LIBRARY_FULL` and 500 keep the values (US-011.6, .9, .10).
  3. Duplicate dialog text, focus on Cancel, Escape, Cancel keeps values, Add anyway adds a second book (smoke test 4, US-011.7, .8).
  4. 500 code points, decomposed "é" and a leading U+00A0 are accepted and shown trimmed (T-07, T-08, T-40 via the page).
- Traces: US-011, T-03, T-07, T-08, T-40, M-1.

## 4. Increment 2: progress, rating, durability (Tasks 30 to 40)

Gate rows (P0): S-01 to S-07, S-08 and S-09 (20 iterations), S-10, S-11, T-15, T-17 to T-19, T-31 numeric rows, T-32, T-36 to T-38, C-03, C-12, C-13 (5 iterations), US-009.1 to .3, .5 to .7, US-012.1 to .7, .9.

### Task 30: Book transitions and value types
- Source: B19 | Layer: backend | Tier: P0 | Estimate: 35 | Depends on: 21 | Branch: `feature/task-30-book-transitions`
- Description: Add the transitions that keep INV-1 to INV-4 and the rating literal judged from token and text.
- Production: (1) `domain/Book.java` (edit); (2) `dto/RatingValue.java` and `RatingRequest.java`; (3) `dto/StatusRequest.java`. Tests: `BookTest`, `RatingValueDeserializerTest`.
- Acceptance criteria:
  1. Entering completed sets `date_completed` from the clock; leaving clears rating and `date_completed` together; a same-state change changes nothing (BR-004, BR-008, US-006.3 to .5).
  2. A rating on a non-completed book is refused by the domain; the schema CHECKs repeat the rule (BR-007, INV-1).
  3. 1 to 5 valid; 0, 6, -0, 4.9, 5.0, 1e0, 1E1, `null`, `{}`, 100-digit integer give `INVALID_RATING`; strings, `true`, arrays, objects, `01`, `+5`, `NaN`, `5.`, `.5`, `5e`, 101 digits give `MALFORMED_REQUEST`; `1e999999999` gives `INVALID_RATING` within 300 ms (T-17, T-18, T-31, T-32, T-37).
- Traces: BR-002, BR-004, BR-007, BR-008, INV-1 to INV-4.

### Task 31: Get, delete, status, rating, clear rating
- Source: B20 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 23, 30 | Branch: `feature/task-31-remaining-endpoints`
- Description: Implement the five remaining endpoints in the order of BR-012 steps 7 and 8 and test the stories and invariant sequences on the real port.
- Production: (1) `service/BookService.java` (edit); (2) `controller/BookController.java` (edit). Tests: `BookControllerTest`, `BookStoryIntegrationTest`.
- Acceptance criteria:
  1. 5.3 to 5.7 answer as written; a `201` is followed by a `GET` of the same book; delete returns the removed book and a second delete is `BOOK_NOT_FOUND` (US-001.9, US-007, US-008).
  2. S-01 to S-06 and S-11 give the stated results; after any failed request the library content is identical (S-07).
  3. Field values are judged before existence (T-36, T-38); `PUT status` with `"Completed"`, `""`, `null`, `{}` is `INVALID_STATUS` and with `5` or `true` `MALFORMED_REQUEST` (T-19).
  4. `DELETE` with `Content-Type` and no body passes the gate; a one-space body is `400` (E-18).
- Traces: 5.3 to 5.7, US-003, US-006 to US-008, S-01 to S-07, S-11, T-14, T-15, T-19, T-36, T-38.

### Task 32: Concurrency and fault injection
- Source: B21 | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 31 | Branch: `test/task-32-concurrency-faults`
- Description: Test concurrent adds and updates and a forced failure with a trigger on the test database.
- Production: none. Tests: `ConcurrencyIntegrationTest`, `support/FaultInjection`.
- Acceptance criteria:
  1. C-06, C-07, C-09 and C-10 give the stated outcomes (one `201` and one `409`; exactly 10 and 10; 1 and 19; then 20).
  2. S-08 (20 iterations) and S-09 (20 iterations) end in a state allowed by INV-1 and INV-4; the allowed set of answers is asserted, never an interleaving.
  3. A failure forced after the first of the two changes of BR-004 gives `500 INTERNAL_ERROR` and the book is exactly as before (S-10, E-21).
  4. No production class contains a test-only route or switch (A-09).
- Traces: BR-013, C-06, C-07, C-09, C-10, S-08, S-09, S-10, E-21.

### Task 33: Durability across restart and kill
- Source: B22 | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 31 | Branch: `test/task-33-durability`
- Description: Start the packaged application as a separate process and prove acknowledged writes survive a normal stop and a forced kill.
- Production: none. Tests: `support/AppProcess`, `DurabilityIntegrationTest`.
- Acceptance criteria:
  1. After an acknowledged add, status change, rating change and delete, a normal stop and start show the same data (US-009.1).
  2. After a forced kill right after each of the four operations returned `2xx`, 5 iterations each, the restart shows the acknowledged change and opens normally (US-009.2, C-13, M-3).
  3. Modes of the directory, file and companion files stay 0700 and 0600 or narrower (SC-13).
  4. The child process console contains no book data.
- Traces: US-009.1, .2, M-3, C-13.

### Task 34: Directory trust, stored-library validation, second instance
- Source: B23 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 33 | Branch: `feature/task-34-startup-safety`
- Description: Refuse an untrustworthy directory or file, validate an existing file read-only before anything writes, fail the second instance cleanly and report any refusal without book data.
- Production: (1) `config/sqlite/DataDirectoryGuard.java`; (2) `config/sqlite/StoredLibraryValidator.java`; (3) `config/sqlite/StartupFailureAnalyzer.java`; (4) `config/sqlite/SqliteDataSourceProvider.java` (edit). Tests: `StoredLibraryValidatorTest`, `DataDirectoryGuardTest`, `StartupRefusalIntegrationTest`.
- Acceptance criteria:
  1. On POSIX a symbolic link, another owner or any group or other bit makes the start fail non-zero with a fixed reason and the file untouched; a swap between check and open is detected (US-009.6, SC-13).
  2. A non-database, a WAL-marked file, damaged file, other structure, no tables, a view, trigger or virtual table, a row violating a format, limit or INV-1 to INV-4, and more than 5,000 rows each fail non-zero, serve no request, give a reason without book data, and leave the bytes unchanged by hash (US-009.5 as changed by CR-9).
  3. A valid database left with an unfinished journal by a forced kill is recovered by the storage and then judged valid, the one allowed earlier write (CR-9, N-1); an absent or empty file is created (US-009.3).
  4. A second instance exits non-zero with the same reporting while the first keeps serving with its data unchanged (US-009.7, C-12).
- Traces: US-009.3, .5, .6, .7, SC-13, SC-39, C-12, DEC-028, DEC-032.

### Task 35: Client endpoints for status, rating, clear and delete
- Source: F2.1 | Layer: frontend | Tier: P0 | Estimate: 25 | Depends on: 11 | Branch: `feature/task-35-client-endpoints`
- Description: Add the client functions for get, set status, set rating, clear rating and delete.
- Production: (1) `frontend/js/api/booksApi.js` (edit). Tests: `booksApi.test.js` (edit).
- Acceptance criteria:
  1. Each function builds its path only with the validated id builder; `DELETE` carries `Content-Type: application/json` and no body (E-07, 5.6, 5.7).
  2. Responses map to the camelCase book; failures map to `api`, `network` and `unreadable` with field messages and the existing book where present.
  3. An id that is not a lower-case UUID is refused before any request (BR-011).
- Traces: 5.3 to 5.7, BR-011.

### Task 36: Entry view, status control and confirmation
- Source: F2.2 | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 35, 25 | Branch: `feature/task-36-entry-status`
- Description: Build one entry from the template with a status control and the confirmation dialog, using the server's returned values.
- Production: (1) `frontend/index.html` and `css/styles.css` (edit); (2) `frontend/js/ui/entryView.js`; (3) `frontend/js/ui/entryActions.js`. Tests: none beyond Task 39.
- Acceptance criteria:
  1. A single-choice control named "Status of <title>" with Unread, Reading, Completed; choosing another value sends `PUT .../status`; the entry shows the returned values and its controls are disabled while the request runs (US-012.1, .6).
  2. With a rating shown, choosing unread or reading opens "Confirm status change" with the exact text, Continue and Cancel, focus on Cancel; Cancel restores the choice without a request (US-012.3).
  3. The rating control exists only on completed entries (US-012.2).
  4. With a filter active an entry that no longer matches stays and shows "No longer matches this filter" (US-012.8).
- Traces: US-012.1 to .3, .6, .8.

### Task 37: Rating group and clear rating
- Source: F2.3 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 36 | Branch: `feature/task-37-rating-group`
- Description: Add the labelled rating group, the clear button and the failure paths.
- Production: (1) `frontend/js/ui/entryView.js` and `entryActions.js` (edit); (2) `frontend/js/logic/failures.js` (edit); (3) `frontend/css/styles.css` (edit). Tests: `failures.test.js` (edit).
- Acceptance criteria:
  1. A group "Rating of <title>" offers 1 to 5 by keyboard and shows the current value as text; "Clear rating of <title>" exists only while a rating exists; selecting sends `PUT`, clearing `DELETE` (US-012.4).
  2. A failed request returns the control to the server's last known value and shows the 5.9 text beside the entry; `INVALID_RATING` shows "Choose a rating from 1 to 5." (US-012.7).
  3. `RATING_NOT_ALLOWED` shows "Only completed books can be rated." and reloads the entry; `BOOK_NOT_FOUND` removes it, reloads the list and announces "This book no longer exists." (US-012.7, US-010.10).
  4. Focus returns to the control used after a request that does not remove the entry (US-014.2).
- Traces: US-012.4, .7, US-013.4, 5.9.

### Task 38: Delete with confirmation
- Source: F2.4 | Layer: frontend | Tier: P0 | Estimate: 35 | Depends on: 36 | Branch: `feature/task-38-delete-entry`
- Description: Add the delete button and confirmation dialog with the focus rules.
- Production: (1) `frontend/js/ui/entryActions.js` (edit); (2) `frontend/js/ui/dialogs.js` (edit); (3) `frontend/index.html` (edit). Tests: none beyond Task 40.
- Acceptance criteria:
  1. "Delete <title>" opens "Delete book" with "Delete <title>? This cannot be undone.", Delete and Cancel; initial focus on Cancel; Escape and Cancel send nothing and focus the Delete button (US-012.5).
  2. On success the entry disappears, the count follows US-010.4, "Book deleted: <title>" is announced and focus moves to the "Your books" heading; the last delete shows the empty message (US-010.6).
  3. A failed delete shows its 5.9 text beside the entry and focus returns to the Delete button.
  4. No control edits a title or author (US-012.9).
- Traces: US-012.5, .9, US-010.4, .6.

### Task 39: E2E status and rating
- Source: F2.5 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 37, 31 | Branch: `test/task-39-e2e-status-rating`
- Description: Test status changes, clearing of rating and date, rating set, replace and clear, and the stale-state failures; add smoke test 5.
- Production: none. Tests: `us-012-status-rating.spec.ts`.
- Acceptance criteria:
  1. Change to completed, rate, replace, clear (smoke test 5); the rating control only on completed entries (US-012.1, .2, .4).
  2. A rated book moved back shows the confirmation; Continue clears rating and date as returned; Cancel restores without a request (US-012.3, S-02, S-03).
  3. A same-state choice changes nothing (S-04, S-11); the "No longer matches this filter" text shows with a filter (US-012.8).
  4. After the book is changed or deleted through the API behind the page, acting shows "Only completed books can be rated." with the entry reloaded, or "This book no longer exists." (US-012.7, S-01, S-05).
- Traces: US-012, S-01 to S-05, S-11.

### Task 40: E2E delete
- Source: F2.6 | Layer: frontend | Tier: P0 | Estimate: 30 | Depends on: 38, 31 | Branch: `test/task-40-e2e-delete`
- Description: Test delete and the stale 404; add smoke test 6.
- Production: none. Tests: `us-012-delete.spec.ts`.
- Acceptance criteria:
  1. Delete with confirmation removes the entry, updates the count, announces "Book deleted: <title>" and focuses the "Your books" heading (smoke test 6, US-012.5).
  2. Cancel and Escape send no request and focus the Delete button.
  3. Deleting a book already deleted through the API shows "This book no longer exists.", removes the entry and reloads the list (US-012.7, S-06).
  4. Deleting the last book shows the empty message (US-010.6).
- Traces: US-012.5, .7, US-010.6, S-06.

## 5. Increment 3: interface quality and release gate (Tasks 41 to 48)

Gate rows (P0): C-05 size bound, C-08, C-11 (c) assertions, US-013.1 to .6, US-014.1, .2, .4 (without 320), .6 (inert, no violation, stylesheet containment), .7, .8, SC-38, the sweep of Task 42, and every earlier P0 row. Afterwards the release criteria of `delivery-strategy.md` section 6 are checked.

### Task 41: Capacity and performance at maximum size
- Source: B24 | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 31 | Branch: `test/task-41-capacity-performance`
- Description: Prove the response bound, the add loop and the mixed load at maximum size and tune the limits if needed.
- Production: (1) `application.yml` (edit only if tuning). Tests: `CapacityPerformanceIntegrationTest`.
- Acceptance criteria:
  1. With 5,000 books of maximum size the list is `200`, `total` 5000, body below 16,000,000 bytes; the time is recorded without a gate (C-05).
  2. A loop of 20 add and delete pairs at 4,999 books gives only `201` and `200` and never more than 5,000 books (C-08).
  3. With 4,949 books, 50 concurrent adds with 50 concurrent lists give all `201` and `200`, 4,999 books after, no `500`, no refused connection, a failed request changes nothing and the process serves afterwards (C-11 c).
  4. Peak heap is recorded; if the mix cannot pass, the task stops and the Sponsor decides (no weakened test).
- Traces: C-05, C-08, C-11, SC-17, M-2, X-03.

### Task 42: Backend release sweep
- Source: part of B27, moved here by the PM | Layer: backend | Tier: P0 | Estimate: 30 | Depends on: 34, 41 | Branch: `chore/task-42-backend-release-sweep`
- Description: Run the repository-wide checks needed for the pre-release Security review and fix or report each finding.
- Production: (1) `config/spotbugs/exclude.xml` (edit); (2) `config/dependency-tree.txt` (edit if approved). Tests: none (recorded runs in `progress.md`).
- Acceptance criteria:
  1. SpotBugs and Find Security Bugs report nothing unexplained; every exclusion keeps its justification.
  2. Grep gates are clean in `backend/src/main`: `select *`, outbound HTTP clients, `System.out`, `printStackTrace`, wall-clock calls, `CrossOrigin`, `getSession`, `sendRedirect`, websocket classes (SC-07, SC-14, SC-18, SC-27, SC-41).
  3. `dependency:tree` equals the committed baseline or the difference is approved; coverage is at least 80%.
  4. The README states that the database cannot be opened by another tool while the application runs (N-9).
- Traces: SC-07, SC-14, SC-18, SC-19, SC-27, SC-36, SC-41.

### Task 43: Accessibility check (axe) and its pin
- Source: F3.1 | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 40, 39; decision of the Sponsor and Security on the checker and the content-security policy | Branch: `test/task-43-accessibility-check`
- Description: Verify and pin the accessibility checker, then test the WCAG 2.1 A and AA rules in the data state of US-014.1.
- Production: (1) `e2e/package.json` and `package-lock.json`. Tests: `us-014-accessibility.spec.ts`.
- Acceptance criteria:
  1. Publisher, registry entry, absence of install scripts, peer range for the pinned Playwright and a clean audit are recorded; the exact pin is proposed for the constitution table (SC-21).
  2. The scan reports zero violations of impact serious or critical with at least three books (one completed, rated), two field errors on the add form and each dialog open (US-014.1).
  3. The policy of the guard tests is not weakened; any relaxed context is decided beforehand by the Sponsor and Security and used for this scan only.
  4. Every control has a programmatic label (US-014.2).
- Traces: US-014.1, .2, DEC-026, SC-21.

### Task 44: Error matrix
- Source: F3.2 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 39, 26 | Branch: `test/task-44-error-matrix`
- Description: Test every error code and the non-envelope cases with stubbed responses, the timeout and the live-region update count.
- Production: none. Tests: `us-013-errors.spec.ts`.
- Acceptance criteria:
  1. Each Appendix A code shows the action and text of 5.9; not JSON, missing code and unknown code show the `INTERNAL_ERROR` text; no response shows the US-013.2 text and keeps input (US-013.1 to .3).
  2. No raw `error` text, status, stack, URL or id is displayed except the book's own data (US-013.5).
  3. A held request abandoned at 15 s by the browser clock re-enables its controls and shows the US-013.2 text (US-013.6 as changed by CR-4).
  4. Exactly one replacement of the live-region text occurs per failure, observed with a mutation observer (US-013.4 as defined by CR-5).
- Traces: US-013, 5.9, E-21 page text.

### Task 45: Safe rendering
- Source: F3.3 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 39 | Branch: `test/task-45-safe-rendering`
- Description: Test that hostile and worst-case texts are inert everywhere and that the page reads and stores nothing it should not.
- Production: none. Tests: `us-014-safe-rendering.spec.ts`, `frontend/js/sources.test.js` (edit).
- Acceptance criteria:
  1. The hostile texts, a 500-code-point word and a title of base characters each followed by 8 combining marks are inert in the list, each dialog and each error path, with no content-security violation (US-014.6, SC-09).
  2. A string assignment to `innerHTML` throws under Trusted Types (SC-38).
  3. The forbidden-sink grep over the served sources passes (SC-09).
  4. Nothing is read from address, `window.name`, referrer or messages; nothing is stored in web storage, cookies or IndexedDB; no other origin is requested (US-014.7, .8).
- Traces: US-014.6 to .8, SC-09, SC-38.

### Task 46: Styles polish
- Source: F3.4 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 45 | Branch: `feature/task-46-styles-polish`
- Description: Finish the stylesheet: colour tokens, visible focus ring, containment rules and reduced motion, with a test that reads the stylesheet.
- Production: (1) `frontend/css/styles.css`. Tests: `frontend/css/tokens.test.js`.
- Acceptance criteria:
  1. Token pairs reach 4.5:1 for text and 3:1 for control boundaries and the 3 px focus ring by the WCAG formula (US-014.2, .3).
  2. The stylesheet wraps or clips overflowing text in the list item and in each dialog, and the test fails if the rules are removed (US-014.6 containment; never cut).
  3. No animation or transition is defined, and a reduced-motion block removes any motion (US-014.5).
  4. Status and errors always carry text as well as colour (US-014.3).
- Traces: US-014.2, .3, .5, .6.

### Task 47: Keyboard and focus rules
- Source: F3.6 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 40, 27 | Branch: `test/task-47-keyboard-focus`
- Description: Test keyboard-only operation, the focus return table and the testable reading of focus trapping for every dialog.
- Production: none. Tests: edits to `us-011-add-book.spec.ts`, `us-012-status-rating.spec.ts`, `us-014-accessibility.spec.ts`.
- Acceptance criteria:
  1. Every operation is completed with the keyboard alone and the add sequence of M-1 works (US-014.2, M-1).
  2. Focus lands where the stories say: Title after Cancel and after Add anyway success or failure, the "Your books" heading after delete, the opener after the confirmation dialogs (US-011.5, .8, US-012.5, CR-1).
  3. With each dialog open, repeated Tab and Shift+Tab never put focus on a page element outside it (US-014.2 as defined by CR-6).
  4. The focused control has a computed outline that is neither none nor zero width (US-014.2).
- Traces: US-011.5, .8, US-012.5, US-014.2, M-1.

### Task 48: Release documentation
- Source: F4.2 | Layer: frontend | Tier: P0 | Estimate: 30 | Depends on: 47, 42 | Branch: `docs/task-48-release-docs`
- Description: Align the testing documents with the delivered code and add the story-to-test table so that documents match the final code before the release.
- Production: (1) `docs/sdd/frontend-testing.md`; (2) `README.md` (frontend notes); (3) `docs/sdd/progress.md`. Tests: none.
- Acceptance criteria:
  1. The layout and accessibility row of `frontend-testing.md` match the delivered tree and the constitution.
  2. A table maps every P0 criterion of US-010 to US-014 to a test title and every P1 or cut-list item to done or reported open.
  3. The README frontend commands are executed on a clean clone.
  4. The Liaison confirms that constitution, spec, plan, tasks and the status board describe the final code; the orchestrator updates the board.
- Traces: M-5, delivery-strategy section 6.

## 6. Increment 4: P1 hardening (Tasks 49 to 53), later and time permitting

Every task is P1 or P2 and never blocks the release (P0 rows that touch the never-cut list are already in Increments 1 to 3). See finding 6 of section 1 about running Tasks 49 and 50 before the tag.

### Task 49: Raw-socket run (pre-application answers)
- Source: B25 (first part) | Layer: backend | Tier: P1 | Estimate: 45 | Depends on: 16 | Branch: `test/task-49-pre-application-run`
- Description: Run the container-level cases once on raw sockets and record the result.
- Production: none. Tests: `PreApplicationIntegrationTest`.
- Acceptance criteria:
  1. No Host (HTTP/1.0 and 1.1), two Host headers, an invalid request line, version `9.9`, duplicate or negative `Content-Length`, both `Content-Length` and `Transfer-Encoding`, lines or headers over 8 KiB: status in {400, 414, 431, 505}, no product, version or echo, the next request is served (E-10 last two, E-20, E-36 first two, E-37).
  2. Both `TRACE` requests answer `405` before the application; `FOO` and `get` answer `405`, 400 or 501 (E-28, DEC-033).
  3. Container rows with an encoded `/` or `\`, `%00`, a literal `\` or a dot segment above the root accept the envelope or a pre-application answer without disclosure (E-15, E-24, E-24b, E-25).
  4. `Upgrade: websocket` and `Upgrade: h2c` give `200`, never `101` (E-31 upgrade rows).
- Traces: E-10, E-15, E-20, E-24, E-24b, E-25, E-28, E-31, E-36, E-37, SC-18, SC-29.

### Task 50: Grammar corners, hidden-channel repeat and log hygiene
- Source: B25 (second part) + B26 | Layer: backend | Tier: P1 | Estimate: 45 | Depends on: 19, 16 | Branch: `test/task-50-grammar-hidden-logs`
- Description: Test the remaining header grammar rows, repeat the hidden-channel text rules on the real port and test log neutralization.
- Production: none. Tests: `GrammarCornersIntegrationTest`, `TextAndParserIntegrationTest`, `LogHygieneIntegrationTest`.
- Acceptance criteria:
  1. `Content-Type` rows 2 to 7 of E-34 and all rows of E-35 give the stated statuses; the application rows of E-24b and E-25 give the stated envelopes.
  2. T-23 to T-26 and T-24b give the stated results on the real port.
  3. Hostile ids, query values and titles leave one log line per event with no raw CR, LF, U+2028, U+2029, U+0085 or ESC and no title text (T-33, SC-37).
- Traces: E-24b, E-25, E-34, E-35, T-23 to T-26, T-24b, T-33, SC-37.

### Task 51: Long runs and timing figures
- Source: B27 (long runs) | Layer: backend | Tier: P1 | Estimate: 40 | Depends on: 33, 32, 41 | Branch: `test/task-51-long-runs-and-figures`
- Description: Run the long loops once and record the measured figures of M-2.
- Production: none (notes in `progress.md`). Tests: edits to `DurabilityIntegrationTest`, `ConcurrencyIntegrationTest`, `CapacityPerformanceIntegrationTest`.
- Acceptance criteria:
  1. The kill loop of C-13 runs 50 iterations; S-08 and S-09 run 200 iterations; all pass.
  2. Single-book operation, list at 5,000 typical books, add at 4,999 maximum-size books and the C-05 list time are recorded once on the reference machine (DEC-020).
  3. A figure over its threshold is reported open; the P0 bounds already passed in Tasks 23 and 41.
- Traces: C-13, S-08, S-09, M-2, DEC-020.

### Task 52: Measurements and responsive layout
- Source: F3.5 + F4.1 | Layer: frontend | Tier: P1 | Estimate: 45 | Depends on: 47 | Branch: `test/task-52-measurements-responsive`
- Description: Add the cut-list measurements, the mobile project and the layout checks.
- Production: (1) `e2e/playwright.config.ts` (edit). Tests: `measurements.spec.ts`, `us-014-accessibility.spec.ts` (edit).
- Acceptance criteria:
  1. With 1,000 books all entries are present 1,000 ms after the `load` event (recorded once); with 5,000 books the time is recorded with no threshold (US-010.9).
  2. At 320 CSS pixels there is no horizontal scrolling; the worst-case texts of US-014.6 stay inside the bounding box of their list item or dialog and the buttons remain visible and operable (US-014.4, US-014.6).
  3. An item that is not written is reported open in the delivery, never skipped silently.
- Traces: US-010.9, US-014.4, US-014.6, M-2.

### Task 53: Final pass
- Source: F4.3 | Layer: frontend | Tier: P1 | Estimate: 35 | Depends on: 52 | Branch: `docs/task-53-final-pass`
- Description: Explore the finished page at 320 and 360 pixels and close the traceability and cut-list report.
- Production: (1) `docs/sdd/progress.md`. Tests: none.
- Acceptance criteria:
  1. An exploration at 320 and 360 pixels is recorded with deviations and spec gaps.
  2. The story-to-test table is complete and every open item is listed with its tier.
  3. The cut-list report states which measurements are done and which are open.
- Traces: M-5, spec section 1.

## 7. Test and control tiers (for Security confirmation)

Rule (`delivery-strategy.md` section 4): P0 stops a realistic local attack or protects a core rule and blocks an increment; P1 is verified once and recorded; P2 is documented. Per the Sponsor, items on the never-cut list of `spec.md` section 1 are P0. The table starts from 022 (AP-5) and the frontend proposal (021 F4.6). Column "PM vs architect" flags every difference for Security.

### 7.1 Appendix D families

| Family | Rows | Tier | PM vs architect | Tasks |
|---|---|---|---|---|
| Envelope and headers on every response class, 500 without detail | E-21, E-23 | P0 | agree | 05, 06, 08, 32 |
| Host, Origin, Fetch-Metadata | E-04, E-10 (first five), E-11 to E-14 | P0 | agree | 06, 08 |
| Content type, body cap, early rejection | E-05, E-07, E-08, E-09, E-31 (`Expect`), E-32, E-33, E-34 row 1 | P0 | agree (E-33 and E-22 live were P1 in handoff 020; moved by the architect in 022, PM concurs) | 06, 08, 15, 16 |
| Slow request | E-22 (configuration and live 5 s run) | P0 | agree (moved from P1 in 020) | 16 |
| Routing, methods, Accept, query, id | E-01 to E-03, E-06, E-17 to E-19, E-24, E-26, E-27, E-29, E-38 | P0 | agree | 15, 16 |
| E-36 third row (2,000 query pairs, `400 MALFORMED_REQUEST`) | E-36 row 3 | P0 | **differs**: 022 files all of E-36 under P1 pre-application rows (B25); the third row is answered by the application and is the query-shape rule of BR-012 step 4 | 16 |
| Static content | E-15 (application rows), E-16, E-30, E-39 | P0 | agree (E-30 was P1 in 020) | 09 |
| Strict parsing and value domains | T-04, T-05, T-14, T-14b, T-15 to T-21, T-27 to T-30, T-32, T-34 to T-40 | P0 | agree | 16, 20, 21, 30, 31 |
| Parser limits never 500, under 300 ms | T-31 | P0 | agree (never cut) | 21, 30 |
| Text policy core and injection payloads | T-01 to T-03, T-06 to T-13, T-22, T-26 | P0 | agree (T-26 was P1 in 020) | 18, 23 |
| Hidden-channel text rules: blank, marks, joiners | T-23 to T-25, T-24b | P0 at unit level; P1 for the real-port repeat | agree (BR-006 is a Must rule) | 19 (P0), 50 (P1) |
| Capacity, duplicates | C-01 to C-04, C-11 (a) and (b) bounds | P0 | agree | 23 |
| Concurrency | C-06, C-07, C-09, C-10 | P0 | agree | 32 |
| Size bound and load | C-05 size, C-08, C-11 (c) assertions | P0 | agree (moved from P1 in 020) | 41 |
| Recorded timing figures | C-05 time, M-2 figures, C-11 durations | P1 | agree | 51 |
| Second instance | C-12 | P0 | agree (moved from P1 in 020) | 34 |
| Durability | C-13 (5 iterations) | P0; 50 iterations P1 | agree | 33, 51 |
| Invariant sequences | S-01 to S-07, S-10, S-11; S-08 with 20 iterations | P0 | agree | 31, 32 |
| Concurrent status, rating and delete | S-09 with 20 iterations | P0 | **differs**: 022 lists S-09 only under P1 long runs (B27) and B21 omits it; INV-1 and INV-4 under concurrency are invariants, so the 20-iteration run is P0; 200 iterations stay P1 | 32, 51 |
| Long runs | S-08 and S-09 at 200 iterations | P1 | agree | 51 |
| Grammar corners | E-34 rows 2 to 7, E-35, E-24b, E-25 (application rows) | P1 | agree | 50 |
| Pre-application answers | E-10 (last two), E-15 container rows, E-20, E-25 container rows, E-28 (both TRACE rows, `FOO`, `get`), E-31 (Upgrade rows), E-36 (first two), E-37 | P1 | agree | 49 |
| Log injection | T-33 live run; the log pattern at configuration level | P1; P0 | agree | 08 (P0), 50 (P1) |
| Start-up criteria without an Appendix D row | US-009.1 to .3, .5 to .7 | P0 | agree (022 B22, B23) | 03, 33, 34 |
| Release sweep (grep gates, static analysis, tree baseline) | SC-07, SC-14, SC-18, SC-27, SC-36, SC-41 | P0 | **differs in placement**: 022 puts the sweep in B27 (Increment 4, P1); the PM puts it in Increment 3 (Task 42) because release criterion 6 needs it | 42 |
| Accepted limits | spec Section 6 | P2 | agree | documented |

Build and static-analysis controls (SC-07, SC-14, SC-19, SC-20, SC-21, SC-27, SC-36) are P0 in every increment through Tasks 01, 02 and 42. Other `SC-NN` controls follow the rows that verify them.

### 7.2 UI acceptance criteria

| Criterion | Tier | PM vs frontend proposal | Tasks |
|---|---|---|---|
| US-010.1, .3 to .8, .10 | P0 | agree | 12, 25, 28 |
| US-010.2 (loading text) | P1 | agree | 12, 28 |
| US-010.9, 1,000 entries within 1,000 ms | P1 (cut list) | agree | 52 |
| US-010.9, 5,000 entries recorded only | P2 | agree | 52 |
| US-011.1 to .10 | P0 | the proposal wrote "US-011.1 to .11"; there is no US-011.11 | 12, 26, 27, 29 |
| US-012.1 to .7, .9 | P0 | agree | 36 to 40 |
| US-012.8 ("No longer matches this filter") | P1 | agree | 36, 39 |
| US-013.1 to .6 | P0 | agree | 24, 26, 44 |
| US-014.1 | P0 | agree, conditional on the Sponsor and Security decision about the checker and the content-security policy | 43 |
| US-014.2 | P0 | agree | 27, 38, 47 |
| US-014.3 | P1 (cut list); contrast rules inside the Task 43 scan are never cut and the focus-ring ratio is P0 under US-014.2 | agree | 43, 46 |
| US-014.4 lang, title, `h1`, main | P0 | agree (split) | 12 |
| US-014.4 320-pixel clause | P1 (cut list) | agree (split) | 52 |
| US-014.5 | P1 (cut list) | agree | 46 |
| US-014.6 inert text and no content-security violation | P0 | agree | 14, 45 |
| US-014.6 stylesheet containment rules | P0 (never cut) | agree | 46 |
| US-014.6 bounding-box measurement | P1 (cut list) | agree | 52 |
| US-014.7, US-014.8, SC-38 | P0 | agree | 12, 45 |
| Rows through the page: T-03 (one case), T-07, T-08, T-12, T-40, S-01 to S-03, S-05, S-06, C-01 (stub), E-16, E-21 (stub) | P0 | agree | 14, 29, 39, 40, 44 |
| Page display of T-06, T-10, T-22, T-24, S-04, S-11, E-38, E-39 | P1 as page checks; the API row keeps its tier | agree | 29, 39, 52 |

### 7.3 Differences to confirm

| # | Row | Architect | PM | Reason |
|---|---|---|---|---|
| D1 | E-36 third row | P1 (with E-36) | P0 | application-answered query-shape rule |
| D2 | S-09, 20 iterations | P1 | P0 | INV-1 and INV-4 under concurrency |
| D3 | Release sweep | Increment 4, P1 | Increment 3, P0 (Task 42) | release criteria 5 and 6 |
| D4 | US-011.11 in the frontend proposal | typing slip | none | no such criterion |
| Moves from the delivery-strategy starting classification that Security should confirm (made by the architect in 022, PM concurs) | E-22 live, E-30, E-33, T-23 to T-26 and T-24b (unit), T-31, C-05 size, C-08, C-11, C-12, C-13 (5 iterations) | P1 to P0 | each is on the never-cut list or is the only proof of a Must criterion |

## 8. Traceability

### 8.1 Task 1 proofs of the constitution

| Proof | Task |
|---|---|
| Failsafe and Surefire split, merged JaCoCo data, coverage check with a failing-threshold run | 01 |
| `-Xlint:all -Werror` | 01 |
| Maven wrapper checksum and `--strict-checksums` | 01 |
| Dependency tree against the ban list | 01 (re-checked in 42) |
| Checkstyle pins; SpotBugs and Find Security Bugs pins | 02 |
| Every mandatory property effective (Boot 4 and Jackson 3 key names) | 03 (set), 08 (proved); strictness switches in 21 |
| Real-port security class with Host, CSRF, header, body-cap and error-path cases | 08; container error path in 17 (note N-4 of 022: Security to accept the placement) |
| `node --test` on the pinned Node | 11 |
| Node and Playwright installs; MCP pin | 10 |
| E2E isolation (SC-33) | 13 |
| Integration checkpoints with logs | 08, 14 |
| `@axe-core/playwright` pin | 43 |

### 8.2 Stories, rules and endpoints to tasks

| FR | Story | Rules | Endpoint | Tasks |
|---|---|---|---|---|
| FR1 | US-001 | BR-001, BR-006, BR-008, BR-009 | `POST /api/v1/books` | 07, 18, 19, 20, 21, 23 |
| FR1 | US-002 | BR-010 | `GET /api/v1/books` | 07, 16, 23 |
| FR1 | US-004 | BR-003, BR-013 | `POST /api/v1/books` | 22, 23, 32 |
| FR3 | US-003 | BR-002, BR-007, BR-004 | `PUT`, `DELETE .../rating` | 30, 31 |
| FR2 | US-006 | BR-001, BR-004, BR-008 | `PUT .../status` | 30, 31 |
| FR1 | US-007 | BR-011 | `GET /api/v1/books/{id}` | 31 |
| CRUD | US-008 | BR-011 | `DELETE /api/v1/books/{id}` | 31 |
| Metric M-3 | US-009 | BR-013 | all writes | 03, 17, 33, 34 |
| Scope: frontend | US-010 | n/a | static content | 09, 12, 25, 28 |
| FR1 | US-011 | n/a | `POST` | 12, 24, 26, 27, 29 |
| FR2, FR3 | US-012 | BR-004 | status, rating, delete | 35 to 40 |
| Constitution | US-013 | n/a | all | 11, 24, 26, 44 |
| Constitution | US-014 | n/a | static content | 12, 14, 43, 45, 46, 47, 52 |
| Cross-cutting | BR-012 | | all | 06, 15, 16, 20, 21, 31 |
| Cross-cutting | INV-1 to INV-8 | | all | 04, 23, 30, 32, 41 |

FR4 has no downstream items by design (DEC-003). Plan sections: backend Part A and appendix of handoff 022, frontend F1 to F7 of handoff 021, until `plan.md` and `plan-appendix.md` are merged.

## 9. Merge options (if the Sponsor prefers fewer, larger tasks)

Merging changes the number of reviews, not the hours; each merged task would run 60 to 90 minutes and waives the 45-minute limit. "Low risk" pairs share a folder and a purpose.

| Merge | Risk | Saves |
|---|---|---|
| 03 + 04 | medium (SQLite spike and STRICT spike together) | 1 |
| 05 + 06 | low | 1 |
| 10 + 13 | low | 1 |
| 11 + 12 | low | 1 |
| 18 + 19 | low | 1 |
| 20 + 21 | medium | 1 |
| 15 + 16 | low | 1 |
| 30 + 31 | low | 1 |
| 39 + 40 | low | 1 |
| 41 + 42 | low | 1 |
| 49 + 50 | low | 1 |

With all eleven merges the total is 42 tasks and Increment 0 has 12. Reaching about 8 in Increment 0 is possible only by splitting it as in finding 2 of section 1 (eight backend tasks first, the page and smoke as "0b").

## Changelog
- 0.2 (2026-10-30): Re-baselined on handoff 022 (27 backend tasks, files rule accepted) and 021; Increment 0 redrawn to 14 tasks with the page and the smoke; sweep split out of B27 into Increment 3; tier table rebuilt from 022 AP-5 with differences flagged; spec change request CR-9 acknowledged.
- 0.1 (2026-10-30): First draft on handoff 020 (superseded).
