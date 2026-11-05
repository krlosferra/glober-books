# Progress Log

- Owner: Orchestrator (entries appended per task)
- Format: one entry per task attempt; factual, no attribution.

## Task 01: Maven scaffold and build proofs

- Date: 2026-11-02
- Branch: `chore/task-01-maven-scaffold`
- Attempt: 1
- Commits: see `git log chore/task-01-maven-scaffold` (all subjects end with `[Task-1]`)
- Verification commands (from `backend/`): `./mvnw -v`; `./mvnw clean verify`; `./mvnw dependency:tree -DoutputFile=config/dependency-tree.txt` (regenerated to a scratch file and compared with `diff`: empty)
- Result: `verify` green. Surefire 7 tests (`BuildSplitProofTest`, `RuntimeClasspathBanTest`), Failsafe 6 tests (`BuildSplitProofIntegrationTest`), JaCoCo merge, report and check (line 0.80) bound to `verify`; jar `target/globerbooks.jar`.
- Pins verified: JDK 21.0.12.1 (`--release 21`); Maven 3.9.16 and maven-wrapper 3.3.4 (only-script form, so no `wrapperSha256Sum`); Spring Boot parent 4.1.2; `jacoco-maven-plugin` 0.8.16; compiler 3.15.0 and Surefire 3.5.6 as managed by the BOM. Maven distribution SHA-256 `5af3b743dd8b876b5c45da33b676251e5f1687712644abb4ee519ca56e1d89ce` computed from the downloaded zip; Central publishes only `.sha512` (`ed41650d...454af3`), and the SHA-512 computed from the same zip equals it.
- Recorded negative runs (all reverted): wrong `distributionSha256Sum` fails; impossible JaCoCo threshold fails; raw-type warning fails under `-Xlint:all -Werror`; banned coordinate in the committed tree fails `RuntimeClasspathBanTest`; test scripts, source maps and dotfiles in `frontend/` are absent from the jar.
- Deviations: placeholder `frontend/index.html` (orchestrator ruling); JaCoCo gate is vacuous (0 classes) while `GloberBooksApplication` is excluded; SC-30 class and mandatory-property proofs deferred to Tasks 08 and 03b (orchestrator ruling). Details in handoff 034.

## Task 02: Quality gates (Checkstyle, SpotBugs with Find Security Bugs)

- Date: 2026-11-02
- Branch: `chore/task-02-quality-gates`
- Attempt: 1
- Commits: see `git log chore/task-02-quality-gates` (all subjects end with `[Task-2]`)
- Verification commands (from `backend/`): `./mvnw -v`; `./mvnw clean verify` (before and after); `./mvnw checkstyle:check@checkstyle-check` once per seeded violation; `./mvnw clean verify` with a seeded Find Security Bugs finding; `./mvnw dependency:tree` compared with `config/dependency-tree.txt` (`diff` empty); `jar tf target/globerbooks.jar` grep for `.test.js`, `.map`, `Test.class` (nothing).
- Result: `verify` green with both gates. Checkstyle 14.3.0 (plugin 3.6.0, phase `validate`) reports 0 violations over 4 Java files; SpotBugs 4.10.5.0 with Find Security Bugs 1.14.0 (phase `verify`, effort Max, threshold Medium) reports 0 bugs over the only production class. Surefire 7, Failsafe 6, JaCoCo check met. Every rule and ban fails the build when violated (seeded runs, all reverted); details in handoff 040.
- Pins verified on Maven Central (SHA-1 computed equals published; SHA-512 also equal for the Checkstyle engine): `maven-checkstyle-plugin` 3.6.0, `com.puppycrawl.tools:checkstyle` 14.3.0 (release 21 baseline), `com.github.spotbugs:spotbugs-maven-plugin` 4.10.5.0 (pulls engine `spotbugs` 4.10.5), `com.h3xstream.findsecbugs:findsecbugs-plugin` 1.14.0.
- Deviations: three lines in `RuntimeClasspathBanTest.java` wrapped to satisfy the 120-character limit (no behavior change); the single-line bans use `RegexpSinglelineJava` (the Java-aware variant of `RegexpSingleline`, which has `ignoreComments`); Checkstyle bound to `validate`; `LineLength` is a `Checker`-level module in 14.3.0.
- F-4 (nothing enforces that the committed dependency tree equals `dependency:tree`) remains open: it is outside Task 02 and was routed to the PM and Sponsor by the Liaison. This task adds only build-plugin classpath; the `diff` against `config/dependency-tree.txt` is empty.
- SC-27 HTTP-client regex ban added after the Security review (SEC-F-089); time rule widened to `new Date(` variants and `.now(` (SEC-F-090). Details in handoff 040 section 7.

## Task 10: E2E toolchain and pins

- Date: 2026-11-02
- Branch: `chore/task-10-e2e-toolchain`
- Attempt: 1
- Commits: see `git log chore/task-10-e2e-toolchain` (all subjects end with `[Task-10]`)
- Verification commands (from `e2e/`, Node v24.22.2): `npm ci`; `npx playwright --version`; `npx playwright install chromium`; `npm audit --audit-level=high`; `npm ls --all`; lockfile `resolved` host check; one run of `npx @playwright/mcp@0.0.83` with the explicit flags (`initialize` and `tools/list` only).
- Result: `@playwright/test` 1.64.1 exact, `engines.node` 24.22.2, `.nvmrc` 24.22.2, `.npmrc` with `ignore-scripts`, `save-exact`, `engine-strict`. `npm ci` installs 3 packages; audit 0 vulnerabilities; 3 `resolved` entries, all `registry.npmjs.org`; no `typescript`, no `tsconfig`. Chromium v1248 installed (about 681M added).
- MCP: pre-release Playwright `1.64.0-alpha-1790635538000`; 25 tools exposed; 17 match the 24-name allowlist, 7 allowlisted names not exposed with these flags, 8 exposed names not allowlisted (follow-up routed by the orchestrator).
- Deviations: sandbox default Node was v22.22.0; the pinned Node was supplied from the official tarball (checksum from the same origin). `cdn.playwright.dev` allow-listed by the Sponsor for the browser download. Details in handoff 038.

## Task 03a: SQLite data source, claim and clock

- Date: 2026-11-02
- Branch: `feature/task-03a-sqlite-data-source`
- Attempt: 1
- Commits: see `git log main..feature/task-03a-sqlite-data-source` (all subjects end with `[Task-3a]`)
- Built: `SqliteDataSourceProvider` (single-connection pool, settings, claim, 0700/0600 storage, `<data dir>/tmp` native directory, `schema.sql` at every start), `ClockConfig`, `schema.sql` (AP-6 verbatim), `SqliteStoreIntegrationTest`, `support/ClaimChildMain` and three test helpers.
- Evidence (E1 to E10): `./mvnw clean verify` green; Surefire 7, Failsafe 22 (1 skipped: unwritable-parent case, needs a non-root user); Checkstyle 0 violations over 11 files; SpotBugs 0 bugs, 4 classes (5 with the nested data source); JaCoCo `SqliteDataSourceProvider` 52 of 56 lines, project 73 of 79 (92.4%), check met. Claim from a second JVM: "database is locked", exit 2, negative control exit 0, SHA-256 equal; same after a forced eviction. Settings read back equal the plan before and after eviction. One `Clock.systemUTC()`, only in `ClockConfig`. Native SQL only in `config/sqlite`. Dependency tree unchanged.
- Deviations / notes: `trusted_schema` has no `SQLiteConfig` key in sqlite-jdbc 3.53.4.0, so it is set by `PRAGMA trusted_schema = OFF` inside the data source Hikari wraps (same effect, still not in the URL, not `connection-init-sql`). Hikari `connection-timeout` not set (belongs to 03b `application.yml`; will be a duplicate-free addition there). `schema.sql` is run through Spring's `ResourceDatabasePopulator` (a JDBC `Statement` fed with a loaded string is a Find Security Bugs `SQL_INJECTION_JDBC` finding).
- Open questions: ClockConfig suppression left file-scoped (see delivery); test-side time rule proposal to the PM (delivery section 6); X-10 outcome: the driver loads from `<data dir>/tmp`, no drop, no `security.md` risk entry needed.
- [Task-3a] SQLite data source, claim and clock - submitted for audit
- Review fixes (handoffs 051, 052): extension test asserts "not authorized" (SEC-F-101); existing-mode test uses 0750/0640 and asserts unchanged (F4); storage created with the permission attribute at creation (F1, SEC-F-102). Deferred without code: SEC-F-098 (provider follows symlinks, so the Task 34a guard must run before `create()`; the 03b/04 brief must say so); SEC-F-099, SEC-F-100 and F6 (`?` and `#` rejected by `StartupPolicy` in Task 03b).

## Task 11: Frontend logic and API client (thin)

- Date: 2026-11-02
- Branch: `feature/task-11-logic-and-client-thin`
- Attempt: 1
- Commits: see `git log feature/task-11-logic-and-client-thin` (all subjects end with `[Task-11]`)
- Verification commands (Node v24.22.2, from the repository root): `node --test "frontend/**/*.test.js"`; the same command with a glob for a non-existing directory; a negative run of `frontend/js/sources.test.js` on a scratch copy outside the repository with a string-argument `setTimeout` added to `api/http.js`.
- Result: 43 tests, 43 pass, 0 fail, exit 0, no `package.json` in the root or `frontend/` (only `e2e/package.json`), no module-type warning. Negative run fails on `api/http.js` as required (scratch copy deleted). A glob that matches no file prints 0 tests and returns exit 0.
- Deviations: none against the brief. Details in handoff 048.

## Task 03b: Start policy and configuration

- Date: 2026-11-02
- Branch: `feature/task-03b-start-policy-config`
- Attempt: 1
- Commits: see `git log main..feature/task-03b-start-policy-config` (all subjects end with `[Task-3b]`)
- Built: `StartupPolicy` (an `EnvironmentPostProcessor` registered in `META-INF/spring.factories`, so a refusal happens before any bean or file access), `application.yml` (mandatory properties, Tomcat limits, `max-swallow-size 16KB`, Hikari `connection-timeout 5000`), `StartupPolicyTest`, `StartupPolicyIntegrationTest`.
- Carried conditions: SEC-F-103 (`?` and `#` refused, fixed reasons); SEC-F-100 (ClockConfig suppression scoped to `[\\/]config[\\/]ClockConfig\.java$`, clock test compares the relative path, seeded second `ClockConfig` flagged then reverted); SEC-F-099 (source scan for `Files`, `FileInputStream`, `FileChannel` outside `config/sqlite`).
- SEC-F-098 (recorded for Task 34a): the `DataDirectoryGuard` must run before `SqliteDataSourceProvider`, and the provider is not the sole start path (it follows symlinks and creates storage itself); the guard, not the provider, is the control.
- Note: the provider is not yet wired as the application's `DataSource`; until Task 34a/34b the context uses Boot's own pool (the integration test pre-creates the directory of a valid path).
- Details: handoff 054.

## Task 05: Error foundation

- Date: 2026-11-02
- Branch: `feature/task-05-error-foundation`
- Attempt: 1
- Commits: see `git log main..feature/task-05-error-foundation` (all subjects end with `[Task-5]`)
- Built: `ErrorCode` (16 codes, statuses and fixed texts of Appendix A), `ErrorBody` (with nested `FieldProblem`; `details` is a typed list, never `Object`), `ErrorBodyFactory` (the one factory, injected `Clock`, format `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`), `ErrorResponseWriter` (explicit `application/json; charset=UTF-8`, writes only the envelope), `ErrorBodyFactoryTest` (33 cases).
- Carried cleanups from the 03b audit: stray `System.out` removed from `StartupPolicyIntegrationTest`; the file-API scan in `StartupPolicyTest` now resolves `src/main/java` from the class location, not the working directory.
- Notes: Appendix A gives informative text for 8 codes only; the texts of the other 8 are new fixed strings chosen here (flagged in handoff 058). The writer owns a private `JsonMapper` (SpotBugs EI_EXPOSE_REP2 on an injected mapper). `DuplicateErrorBody` and `ApiException` stay with Task 22.
- Details: handoff 058.

## Task 06: Thin gate, exception advice and container error report

- Date: 2026-11-02
- Branch: `feature/task-06-thin-gate`
- Attempt: 1
- Commits: see `git log main..feature/task-06-thin-gate` (all subjects end with `[Task-6]`)
- Built: `GlobalExceptionHandler` (500 and the four routing exceptions, JSON even for `Accept: text/xml`, one log line without the message), `TomcatErrorReportCustomizer` (error valve with `showReport=false`, `showServerInfo=false`; connector `continueResponseTiming=onRead`), `HeaderPolicy`, `BoundedBodyRequest`, `RequestGateFilter` (highest precedence, `final`, security headers re-applied on `reset` and `sendError`).
- Notes: Boot 4.1.2 differs from the plan: the encoding switch is `spring.servlet.encoding.enabled=false` (plan: `server.servlet.encoding...`); the web server classes live in `org.springframework.boot.tomcat.servlet`. Tomcat sends `100 Continue` before the application runs by default, so `continueResponseTiming=onRead` is set to meet SC-06. SpotBugs (no exclusion allowed) needed the filter to be `final` and to hold a lambda, not the writer.
- Details: handoff 062.

## Task 04: Domain and repository

- Date: 2026-11-04
- Branch: `feature/task-04-domain-repository`
- Attempt: 1
- Commits: see `git log main..feature/task-04-domain-repository` (all subjects end with `[Task-4]`)
- Built: `domain/BookStatus` (wire value, `fromWire` returning `Optional`, nested converter), `domain/Book` (no public setters, nested millisecond UTC instant converter), `dto/BookResponse` (seven members), `repository/BookRepository` (derived ordered queries), `BookStatusTest`, `BookRepositoryTest` (45 cases).
- Result: Hibernate `validate` passes on the STRICT table with its CHECKs (X-06 spike succeeded, no fallback); the stored DDL text equals the normalised `schema.sql` statement (AP-12.1 comparison stays whole-statement).
- Notes: STRICT converts a numeric text such as `'3'` to an integer for `rating`, so only non-numeric text is rejected there. The test sets the community dialect by property; the production dialect setting is left to a later task.
- Details: handoff 066.

## Task 07: Add and list API (thin)

- Date: 2026-11-04
- Branch: `feature/task-07-add-and-list-thin`
- Attempt: 1
- Commits: see `git log main..feature/task-07-add-and-list-thin` (all subjects end with `[Task-7]`)
- Built: `service/BookService` (`create` one transaction, `list` read-only), `service/BookValidationException`, `config/IdConfig`, `controller/BookController`, `dto/CreateBookRequest`, `dto/BookListResponse`, advice mappings for validation, unreadable body and Bean Validation, SQLite dialect in `application.yml`, `BookServiceTest`, `BookControllerTest`, `BookApiIntegrationTest`, `ApplicationStartupIntegrationTest`
- Result: `./mvnw verify` green; real-port POST and GET round trip, wire format with 24-character dates, a list writes nothing (database hash unchanged).
- Notes: Jackson 3 writes an `Instant` with zero milliseconds without the fraction, so `BookResponse` dates carry a `@JsonFormat` pattern. The service method is named `create` because SpotBugs treats a class with an `add*` method as mutable (EI_EXPOSE_REP2 in the controller).
- Details: handoff 070.

## Task 08: 0a note, commands to try the API skeleton (v0.1)

- Date: 2026-11-04
- Branch: `chore/task-08-real-port-baseline`
- Warning: `v0.1` is a checkpoint for the Sponsor, **not for real data**. It has no page, no README, no container error wrapper and thin validation. Its database file (`backend/data/globerbooks.db`) is **deleted before Increment 1** (SEC-F-077).
- The database file is exclusively locked while the application runs: no other tool (no `sqlite3`, no viewer) can open it. Stop the application first.
- Try 0a (loopback only; the application listens on `127.0.0.1:8080`):
  0. Prepare: `mkdir -p backend/data` (the data directory must exist; the application does not create it yet, Task 34a does).
  1. Start: `cd backend && ./mvnw spring-boot:run` (run from `backend/`; port 8080 must be free) (or `./mvnw package` then `java -jar target/globerbooks.jar`). Wait for the line `Started GloberBooksApplication`.
  2. Add: `curl -i -X POST http://127.0.0.1:8080/api/v1/books -H 'Content-Type: application/json' -d '{"title":"Dune","author":"Frank Herbert"}'` gives `201` and a JSON book with `id`, `status` `unread`, `rating` `null`.
  3. List: `curl -i http://127.0.0.1:8080/api/v1/books` gives `200` and `{"books":[...],"total":1}` with the book added.
  4. Stop: press `Ctrl+C` in the terminal that runs the application.
  5. Clean up: delete `backend/data/globerbooks.db` when finished.
- Details: handoff 074.

## Task 09: Static content (IN_REVIEW)

- Branch: `feature/task-09-static-content` (local commits, subjects end with `[Task-9]`)
- Built: `config/transport/StaticAssetCatalog` (fixed at start, constructor location, fixed start-failure reason), `controller/StaticContentController`, `config/StaticContentConfig` (passes `classpath:/static/` only), `error/MalformedRequestException` with an advice mapping, `spring.web.resources.add-mappings: false`, `StaticContentIntegrationTest`.
- Gaps reported (not fixed, Task 15 or container wrapper): `HEAD` and `OPTIONS` on a static path give 200 (spec 5.8: 405 with `Allow: GET`); the 405 carries no `Allow`; `DELETE`, `PUT`, `PATCH` without `Content-Type` give 415 before the method check; Tomcat answers 400 HTML (no security headers) for `/%2e%2e/...` and `/..;/` and 404 HTML for `/WEB-INF/...`.
- Details: handoff 078.

## Task 15: Route table, methods and Accept (IN_REVIEW)
- Branch `feature/task-15-routing-accept`; delivery handoff 097. `RequestTarget`, `RouteTable` and `AcceptPolicy` added; `RequestGateFilter` decides route and method (404, 405 with `Allow`), then `Accept` on `/api/v1/` paths. The advice takes `Allow` from `RouteTable` too. HEAD and OPTIONS on static paths now answer 405 (SEC-F-127 closed).
- `RouteTableTest` reflects the controllers' mappings and the catalog and fails when a mapping is added (seeded and reverted, recorded in the delivery). Id endpoints are not routes until Task 31, so their rows answer 404 for now.
- Test change: the gate integration test used probe controllers that are not in the table; it now uses the real `/api/v1/books` with a mocked service.

## Task 12: Page shell with minimal list and add

- Date: 2026-11-05
- Branch: `feature/task-12-page-shell-minimal`
- Attempt: 1
- Result: page, stylesheet and five modules; 43 frontend tests pass; criterion 5 evidenced by a scripted browser run (the tool server was not available); details and gaps in handoff 082.


## Task 13: E2E harness (config, launcher, isolation)

- Date: 2026-11-05
- Branch: `chore/task-13-e2e-harness`
- Attempt: 1
- Result: Playwright configuration, launcher, global setup and placeholder test; the placeholder is green; Playwright 1.64.1 starts the web server before the global setup (Q-F7); the busy-port check and the hostile-environment run are evidenced in handoff 089.

## Task 14: 0b note, smoke tests, guard fixture and README (closes Increment 0b, v0.2)

- Date: 2026-11-05
- Branch: `chore/task-14-smoke-guard-readme`
- Attempt: 1
- Built: `e2e/support/fixtures.ts` (auto `guard`: `securitypolicyviolation` events, CSP, Trusted Types and Permissions-Policy console messages, page errors, requests to another origin, `Content-Type` and the constitution security headers of every same-origin asset), `e2e/support/builders.ts`, `e2e/support/library.ts` (list, create, delete, reset through the public API), `e2e/tests/us-010-library-view.spec.ts` (three `@smoke` tests), `README.md`.
- Warning: `v0.2` is a checkpoint for the Sponsor, **not for real data**. Its database (`backend/data/globerbooks.db`) is deleted before Increment 1 (SEC-F-077). The database file is exclusively locked while the application runs.
- Try 0b (from the repository root; JDK 21 and Node 24.22.2 on the `PATH`; ports 8080 and 18080 free):
  1. Build: `cd backend && ./mvnw verify`
  2. Logic tests: `node --test "frontend/**/*.test.js"`
  3. E2E setup once: `cd e2e && npm ci --ignore-scripts` and `npx playwright install chromium`; smoke: `npx playwright test --grep @smoke` (it starts the application itself on 18080 with a temporary database)
  4. By hand: `mkdir -p backend/data`, then `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments=-Xmx512m` (or `cd backend && java -Xmx512m -jar target/globerbooks.jar`); open `http://127.0.0.1:8080/`, add a book with the keyboard, reload and see it; stop with `Ctrl+C`; delete `backend/data/globerbooks.db` when finished.
- SEC-F-130: the guard tolerates exactly one console message, `Error with Permissions-Policy header: Unrecognized feature: 'bluetooth'.`, until the Sponsor decides the header list; the filter is marked in `fixtures.ts`.
- Finding for the Liaison: the database path is `./data/globerbooks.db` (relative), so the jar must be started from `backend/`; the literal command of Task 14 criterion 3 (`java -Xmx512m -jar backend/target/globerbooks.jar` from the root) fails with "unable to open database file". The README documents `cd backend && java -Xmx512m -jar target/globerbooks.jar`.
- Details: handoff 093.

### Task 1 proof table (constitution, "Task 1 proofs")

| Proof | Evidence | Status |
|---|---|---|
| Failsafe and Surefire split, merged JaCoCo data, coverage check in `verify` | Task 01 (handoff 034); `verify` green in the Task 14 clean copy (Tests run 120, check met) | Proved |
| `-Xlint:all -Werror` | Task 01 (handoff 034), seeded raw-type warning failed the build | Proved |
| Checkstyle 14.3.0 with `maven-checkstyle-plugin` 3.6.0 | Task 02 (handoff 040) | Proved, pins verified |
| Maven wrapper 3.9.16 with `distributionSha256Sum` and `--strict-checksums` | Task 01 (handoff 034): wrong `distributionSha256Sum` fails; `backend/.mvn/maven.config` holds `--strict-checksums`; wrapper 3.3.4 is the only-script form (no `wrapperSha256Sum`) | Proved |
| Every mandatory property effective | `MandatoryPropertiesIntegrationTest` (Task 08, handoff 074; Task 03b, handoff 054), 15 tests green in the Task 14 verify | Proved |
| Dependency tree against the ban list | Task 01 `RuntimeClasspathBanTest` and Task 02 (handoffs 034, 040); `config/dependency-tree.txt` compared by `diff` | Proved (F-4: no gate enforces the committed tree; open, routed in Task 02) |
| Real-port security test class (SC-30): Host, CSRF, header, body-cap, error-path | `TransportSecurityIntegrationTest` (Task 08, handoff 074), 8 tests green in the Task 14 verify | Proved for Increment 0a; later tasks add rows |
| `node --test` on the pinned Node | Task 11 (handoff 048), 43 tests; Task 14 clean copy 43 of 43 | Proved |
| Playwright install on the pinned Node | Task 10 (handoff 038) install; Task 13 (handoff 089) and Task 14 smoke run, 3 of 3 | Proved |

Pins that remain unverified: `@playwright/mcp` 0.0.83 was started and its tool list read (Task 10, handoff 038: 17 of the 24 allowlisted tools exposed) but no page exploration ran through it (not exposed to the Task 12 and Task 14 runs); Eclipse Temurin (the JDK pin was verified on OpenJDK 21.0.12.1 only). Pins verified here for the first time on a clean copy: Maven 3.9.16 and wrapper 3.3.4 (full `verify`), `@playwright/test` 1.64.1 (`npm ci`, smoke run).
