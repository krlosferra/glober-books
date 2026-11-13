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

## Task 16: Query, id and empty-body rules; transport limits (IN_REVIEW)
- Branch `feature/task-16-query-id-limits`; delivery handoff 105. `QueryPolicy` (query grammar, `status` value, id rule as a pure function), gate stage 9 (id, query, no-body) in `RequestGateFilter`, total 10 s body read deadline in `BoundedBodyRequest`, `ErrorResponseWriter` details overload for `INVALID_STATUS`.
- Findings: F-1 Tomcat answers its own HTML 400 for a raw `[` or `]` in the query (E-26 row 5 expects `MALFORMED_REQUEST`); F-2 drip clients on the request line and headers hold a connection past 20 s (accepted limit candidate, SEC-F-073), and after the body deadline Tomcat keeps swallowing the drip. Both go to the Sponsor (SC-06 values are ASK FIRST).
- Id endpoint rows (E-19, E-24 row 1, E-27 rows 1 and 2, T-14) are proved at gate level with a mocked route table and complete on a real port in Task 31.

## Task 17: Container error wrapper (IN_REVIEW)
- Delivery handoff 109. `EnvelopeResponseWrapper` turns any `sendError` inside the application into the envelope with the security headers; the whole body of `RequestGateFilter` is inside the catch for `INTERNAL_ERROR` (SEC-F-113). `GET /error` is 404 NOT_FOUND in the envelope. SEC-F-128: Tomcat answers `/%2e%2e/`, `/..;/`, `%2f`, `%00` and `/WEB-INF/web.xml` itself with a minimal HTML page without the security headers; covered by R-13 and R-23 (constitution 1.5), Task 49 raw-socket rows remain.

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
- SEC-F-130: the `Permissions-Policy` header value was corrected (constitution 1.4, `bluetooth=()` removed) and the console-message filter was removed from `fixtures.ts`; the guard now tolerates no Permissions-Policy console message.
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

## Task 18+19: Text policy and Unicode fixture check (IN_REVIEW)

- Branch: `feature/task-18-19-text-policy`; delivery handoff 113. `TextPolicy` implements BR-006 items 1 to 6d as a single linear pass over code points (no regular expression on the text; trim by Unicode White_Space); `UnicodeFixtureCheckTest` proves JDK 21.0.12, Unicode 15.0 and the spec's categories and NFC results; rule tests call `UnicodeFixtures.requirePinnedData()` in `@BeforeAll`. Wiring into Bean Validation stays with Task 20.

## Task 20: Validation wiring and field-error mapping (IN_REVIEW)

- Branch: `feature/task-20-validation` (the task header names `feature/task-20-validation-wiring`; the checked-out name was kept); delivery handoff 117. `BookText` (constraint plus validator calling `TextPolicy.check`), `ValidatorConfig` (validator factory with a literal message interpolator, no expression language), `CreateBookRequest` (title, author, status order; status pattern; explicit JSON null read as the empty text so it is `INVALID_STATUS`, absent stays `unread`), advice ordering title, author, status with `INVALID_STATUS` only when status alone fails; the service repeats `TextPolicy.check`, stores `TextPolicy.normalize` and throws the same exception type. Thin checks of Task 07 removed.

## Task 21: Strict body reading and parser limits (IN_REVIEW)

- Branch: `feature/task-21-strict-body` (the task header names `feature/task-21-strict-json`; the checked-out name was kept); delivery handoff 121. `StrictJsonBodyResolver` (a `@RequestBody` argument resolver installed first by `WebMvcConfig`) decodes the buffered body with a reporting UTF-8 decoder and parses it with the strict mapper; every failure is `MalformedRequestException` (fixed message, no parser text, no body content), `@Valid` still runs afterwards. `JacksonConfig` sets every switch in code (read constraints depth 16, string 16,384, name 64, number 100, document 16,384, tokens 64; strict duplicate detection; no comments, single quotes, unquoted names, raw control characters, trailing commas or lenient numbers; unknown members, trailing tokens, numeric enums, float-to-integer, single-value arrays, empty-string objects and null primitives fail; scalar coercion off). `StrictStringDeserializer` (no coercion, unpaired surrogates refused; SEC-F-122 closed) and a nested `StrictBooleanDeserializer` (`allow_duplicate` as text, number or null fails, absent stays null). Finding: `FAIL_ON_NUMBERS_FOR_ENUMS` is `EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS` in Jackson 3.1.7, not a `DeserializationFeature`.

Task 21 note (SEC-F-159): `@WebMvcTest` slices do not import `WebMvcConfig`, so they use the framework's `@RequestBody` processor and do not exercise strict parsing. Later tasks that add request bodies (Task 30 rating, status) must prove strictness in unit or full-context tests, not in slices.

## Task 22: Duplicate key and service exceptions (IN_REVIEW)

- Branch: `feature/task-22-duplicates` (the task header names `feature/task-22-duplicate-key`; the checked-out name was kept); delivery handoff 125. `DuplicateKey` (record of title key and author key; NFKC, `toLowerCase(Locale.ROOT)`, White_Space collapse and trim; ASCII fast path proven equal to the general path on 10,000 seeded pairs; `toString` hides the keys), `ApiException` (code, status, message is the code name), `DuplicateBookException` and `DuplicateErrorBody` (typed, `details.existing_book` is one book), edit of `GlobalExceptionHandler`. SEC-F-109 addressed: no `Object`, map or tree. `ErrorBodyFactory` is unchanged; the duplicate body is made from its envelope.

Task 22 note (SEC-F-162, accepted by the spec): the duplicate key lower-cases with the Unicode default mapping and does not case-fold (DEC-001, T-40). About 101 BMP code points (for example U+00DF, U+0131, final sigma U+03C2) have different keys in upper and lower case, so a duplicate can be missed; this is the spec's decision, not a defect. Task 23a must pass the stored (trimmed, NFC) title and author to `DuplicateKey.of` for the incoming pair and for the scanned rows, put the earliest match (`date_added`, then lowest `id` by lexical compare) in the 409, and add a test with two matches asserting the earliest appears (SEC-F-163); it should also check `DuplicateKey` white space against `\p{IsWhite_Space}`.

## Task 23a: Add rules (IN_REVIEW, review chunk 23 with Task 23b)

- Branch: `feature/task-23-add-rules` (the task headers name `feature/task-23a-add-rules` and `feature/task-23b-list-filter-add-bound`; the checked-out chunk branch was kept); delivery handoff 129. `BookService.create` runs validation, then `count()` (at 5,000: `LIBRARY_FULL` through `ApiException`, before any duplicate check, with or without `allow_duplicate`), then, unless `allow_duplicate` is true, one query for the key columns (`BookRepository.findAllBy()` returning the `BookKeyRow` projection), then the insert, in one transaction. The scan computes the keys from the stored text (`DuplicateKey.textKey`), reports the earliest match (`date_added`, then lowest `id` by text compare) and loads that one book for `DuplicateBookException`. Closes SEC-F-163. `DuplicateKey.isWhiteSpace` became package-private so a test can compare it with `\p{IsWhite_Space}` over all code points.

## Task 23b: List filter and the 2-second add bound (IN_REVIEW, review chunk 23)

- Branch: `feature/task-23-add-rules`; delivery handoff 129. `BookController.list` reads the checked `status` value from the request attribute set by the gate (never `getParameter*`); `BookService.list(String)` uses the filtered repository query in the BR-010 order and refuses a value that is not one of the three (defence in depth). The 2-second bound held on the first run: with 4,999 maximum-size books (500 and 200 code points, non-ASCII) a non-matching add took 143 ms (201), cold duplicates 928 ms, then 112 ms and 143 ms (409), on 4 CPUs, Linux amd64, Java 21.0.12.1; no schema change needed.

Task 23b follow-up round (handoffs 130, 131): worst-case bound test added (`AddBoundWorstCaseIntegrationTest`, 4,999 books with one shared 200-code-point author and distinct 500-code-point titles, so a title key is computed for every row): non-matching add 442 ms then 150 ms, duplicates 218 to 239 ms then 151 to 156 ms; the bound holds. `BookKeyRow` Javadoc corrected. Notes carried, no code: (1) plan wording "JPQL constructor projection" for `BookKeyRow` needs the same amendment (Spring Data class-based projection, `findAllBy()`); (2) SEC-F-166: Task 41 must record cold 2 s figures on the reference machine (Security measured 1.64 s for a cold first duplicate on its own machine); (3) SEC-F-168: the capacity and duplicate rules depend on the one-connection pool, so any pool or locking change needs a Security review; (4) SEC-F-167: the owner of C-11 c must prove a pool timeout becomes a generic error envelope; (5) PM question on BR-009: validation runs before capacity, so an invalid body at 5,000 books gives 400, not 409 LIBRARY_FULL.

## Task 30: Book transitions and value types (IN_REVIEW)

- Branch: `feature/task-30-rating` (the task header names `feature/task-30-book-transitions`; the checked-out name was kept); delivery handoff 133. `Book` gains `changeStatus(target, clock)`, `assignRating(int)` and `clearRating()` (entering completed reads the clock and truncates to milliseconds; leaving completed clears rating and date together; a same-state change is a no-op; a rating on a non-completed book throws `IllegalStateException`, which the Task 31 service maps to `RATING_NOT_ALLOWED`). `RatingValue` reads the token and its text (only the integer token `1` to `5` is valid; every other number and `null` is the invalid marker; other tokens are mismatches). `RatingRequest` turns a missing member into the marker; `StatusRequest` reuses the Task 20 status reading and turns a missing member into the empty text.
- Finding for Security and the PM: the Jackson number-length read constraint counts digits only, so `-` plus 100 digits (101 characters as written) passed it. Spec BR-002 counts the literal as written, so `RatingValue.Reader` also refuses any number text over 100 characters (`MALFORMED_REQUEST`).
- Carried to Task 31: the advice maps only `status` to a single-field code (`GlobalExceptionHandler`); the rating marker needs the `INVALID_RATING` mapping with the `details` entry `rating`, and the service maps `IllegalStateException` from `assignRating` to `RATING_NOT_ALLOWED`. The plan names no domain exception type.

## Task 31a: Get, delete, status change (IN_REVIEW, protected surface, reviewed alone)

- Branch: `feature/task-31a-get-delete-status`; delivery handoff 137. `BookController` gains `get`, `changeStatus` (`@Valid @RequestBody StatusRequest`, so the body is judged before the service looks for the book) and `delete`; `BookService` gains `get`, `changeStatus` (status parsed first, then `findById`, then `Book.changeStatus(target, clock)`, `saveAndFlush`) and `discard` (find, delete, flush, return the removed book). The delete use case is called `discard` because SpotBugs (EI_EXPOSE_REP2, no exclusion allowed without approval) treats a service with a method called `delete` as a mutable value and flags `BookController(BookService)`. `RouteTable` now holds the templates `/api/v1/books/{id}` (GET, DELETE) and `/api/v1/books/{id}/status` (PUT); any single non-empty segment stands for the id, so the route and method are decided before the id format (E-27 rows 1 and 2); `Allow` is computed from the template. The Task 15 placeholder `RoutingIntegrationTest.e02...UntilTask31` now expects the spec's 405.
- Gap noted for the PM and Task 31b: E-02 also lists `GET /api/v1/books/{id}/rating` (405); the rating routes arrive in Task 31b, so until then that path is `404 NOT_FOUND`. T-14 rows for `PUT` and `DELETE .../rating` also wait for 31b.

## Task 31b: Rating, clear rating and the rating error mapping (IN_REVIEW, protected surface, reviewed alone)

- Branch: `feature/task-31b-rating-endpoints`; delivery handoff 141. `BookController` gains `PUT /{id}/rating` (`@Valid @RequestBody RatingRequest`) and `DELETE /{id}/rating`; `BookService` gains `rate` and `unrate` (names chosen against the SpotBugs mutator heuristic; SpotBugs reports 0 bugs, no exclusion). `rate` judges the `RatingValue` marker first (`BookValidationException` with code `INVALID_RATING` and the one fixed detail, never the value), then `findById` (`BOOK_NOT_FOUND`), then the status (`RATING_NOT_ALLOWED` before `assignRating`), and does not catch `IllegalStateException` (a stubbed domain refusal gives `500`). `unrate` is idempotent. `RouteTable` holds the seventh route `/api/v1/books/{id}/rating` (DELETE, PUT), so `GET .../rating` is `405` with `Allow: DELETE, PUT` (E-02, E-29). The two fixed detail messages are now constants of `BookValidationException` shared by the service and the advice allowlist.
- Deviation: the rating slice tests are in the new `BookRatingControllerTest` because `BookControllerTest` (287 lines) would pass the 300-line limit.

## Task 32: Concurrency and fault injection (IN_REVIEW, review chunk 32+33)

- Branch: `test/task-32-33-concurrency-durability` (the header names `test/task-32-concurrency-faults`); delivery handoff 145. Test-only: `ConcurrencyIntegrationTest` (C-06, C-07, C-09, C-10), `ConcurrencyRaceIntegrationTest` (S-08 and S-09 at 20 iterations, rate against status change at 60 iterations on one book, SEC-F-177), `FaultInjectionIntegrationTest` (S-10, E-21, pool not wedged, A-09 source scan), `support/FaultInjection` (trigger with `RAISE(FAIL)` created through the application's own pool on the test database) and `support/ApiCalls` (HTTP helper and barrier runner). The split by theme keeps every file under 300 lines. No production change; `./mvnw clean verify` green (900 unit, 264 integration, coverage checks met).

## Task 33: Durability across restart and kill (IN_REVIEW, review chunk 32+33)

- Branch: `test/task-32-33-concurrency-durability`; delivery handoff 145. `support/AppProcess` starts the packaged jar as a separate process (`-Xmx512m`, loopback, free port, allowlisted environment, console to a file, shutdown hook) and `DurabilityIntegrationTest` proves a normal stop and start after add, status, rating and delete, and 5 SIGKILL rounds after each of the four operations (full-body equality after restart), the console free of book data, and that modes never widen. Decision (coordinator, option a): the packaged application does not yet create the directory and file with 0700/0600 (the provider is not wired; Task 34a), so the test pre-creates them and the creation modes are proved in Task 34a (SC-13). `./mvnw clean verify` green (900 unit, 269 integration).

## Task 34a-1: Complete directory guard and file identity, not wired (IN_REVIEW, protected surface, reviewed alone)

- Branch: `feature/task-34a1-directory-guard`; delivery handoff 149. New `config/sqlite/DataDirectoryGuard` (creates the missing data directory 0700, native directory `tmp` 0700 and database file 0600 with an explicit permission set after creation, so the umask cannot change them; refuses symbolic link anywhere in the path, wrong type, another owner, any group or other bit, relative or non-normalized path, missing or non-directory parent, uncreatable parts; fixed `Reason` texts; inspections come before any creation) and `SqliteDataSourceProvider.create(Guarded)` (compares the remembered device and inode with the file after the pool opens, closes the pool on a mismatch). `create(Path)` and the existing tests are unchanged. Not wired: the jar still uses the auto-configured data source; SEC-F-178 stays open until Task 34a-2. The check-to-open race is a documented limit (A-09).

## Task 34a-2: Wire the packaged application to the guard and prove it on the jar (IN_REVIEW, protected surface, reviewed alone)

- Branch: `feature/task-34a2-wire-guard`; delivery handoff 153. New `config/sqlite/SqliteDataSourceConfig` (the only data source: `DataDirectoryGuard.check` then `SqliteDataSourceProvider.create(Guarded)`; Boot's data source backs off; path from `spring.datasource.url` through `toAbsolutePath().normalize()`, never `toRealPath()`), `StartupFailureAnalyzer` (fixed description and action for guard refusals and the three start-policy reasons; registered in `spring.factories`), `OwnerOnlyCheck` (owner, mode and hard-link count, moved out of the guard), `NativeDirectoryModes` (the driver's extracted files in `tmp` narrowed to 0600). Guard: hard-link refusal (SEC-F-182) and cleanup of what a failed creation created (SEC-F-184 a); provider: `create(Path)` and `create(String)` package-private, a failure to open or claim is a fixed refusal. Proofs on the jar: `JarDataLocationIntegrationTest`, `StartupRefusalIntegrationTest`; in process: `SqliteWiringIntegrationTest`. README: the directory is created by the application. `./mvnw clean verify` green.

## Task 34b: Stored-library validation, hot journal and second instance (IN_REVIEW, protected surface, reviewed alone)

- Branch: `feature/task-34b-stored-library-validation`; delivery handoff 157. New `config/sqlite/StoredLibraryValidator` (read-only, `query_only` on: `sqlite_master` and the stored DDL equal to the normalised `schema.sql`, then `quick_check`, then a row pre-check by `typeof` and `length(CAST(.. AS BLOB))` with `LIMIT 5001` before any text is fetched, then the rows judged in Java: code points, NUL, formats, INV-1 to INV-4; nine fixed reasons) and `StoredLibraryHeader` (plain-I/O header check before the pool: magic, bytes 18 and 19, page size, size test skipped when a non-empty `-journal` exists, a shared lock that reports "in use" instead of a false "damaged"). `SqliteDataSourceProvider.create(Guarded)` runs header, claim (SQLite's hot-journal rollback), validator, schema, in that order; a zero-length file skips the validator; the extracted native files are narrowed in a `finally`. `StartupFailureAnalyzer` reports the new refusals. Tests: unit (`StoredLibraryValidatorTest`, `StoredLibraryRowCheckTest`) and jar-level (`StoredLibraryRefusalIntegrationTest`, `HotJournalIntegrationTest` with a genuine journal from a SIGKILLed writer JVM, `StoredLibraryAcceptIntegrationTest`, `SecondInstanceIntegrationTest`, `NativeFilesOnRefusalIntegrationTest`). Observations: quick_check stops rows that break a CHECK (reported as damaged); SQLite deletes an ignored garbage journal; stale extracted files are not removed (a second instance could not tell them from live ones).
- Follow-up round (handoffs 158 and 159): SEC-F-189 fixed (a `-wal` or `-shm` beside the database, links included, is refused as WAL_MARKED before the pool opens, whatever the journal) and SEC-F-190 fixed (without a non-empty journal a header with page count 0 or bytes 24 and 92 different is refused as damaged, so the claim cannot rewrite page 1); tests `WalPlantingIntegrationTest`, `StoredLibraryHeaderTest`. The `finally narrow` of SEC-F-186 is a defensive branch with no test that fails without it (pool-open failure is rare after the lock check). Untested residuals: a zero-length main file plus a non-empty journal, a journal that is a symbolic link (`hasHotJournal` follows links), a non-POSIX file system.

## Task 41+42: Capacity and performance at maximum size, and backend release sweep (IN_REVIEW)

- Branch: `test/task-41-42-capacity-performance`; delivery handoff 161. Tests only (no production change, no `pom.xml`, `application.yml`, README or `config/` edit): `CapacityPerformanceIntegrationTest` (C-05 list of 5,000 books outside the basic plane, 14,641,684 bytes in 668 to 764 ms, recorded; C-08 20 add and delete pairs, 201 and 200 only, never above 5,000), `CapacityMixedLoadIntegrationTest` (C-11 (c) on the jar in a separate JVM with `-Xmx512m`: 50 adds 201, 50 lists 200, 4,999 books after; eight slow list readers, no `OutOfMemoryError`, process serves afterwards; peak heap before collection 205 to 229 MB, peak resident set about 520 to 560 MB; 512 MB confirmed, README already carries it in both commands), `ColdStartBoundIntegrationTest` (SEC-F-166: fresh JVM, first add 387 to 392 ms, first duplicate 376 to 488 ms, later duplicates 108 to 129 ms; all under 2 s). Support: `CapacityFixtures`, `SlowReader`; `AppProcess` gains `pid()` and extra JVM options; `ApiCalls` gains `port()`.
- Sweep: SpotBugs with Find Security Bugs "BugInstance size is 0", the exclusion filter is empty (no exclusion exists); grep gates empty in `backend/src/main` (the only wall-clock hit is `Clock.systemUTC()` in `ClockConfig`, the clock bean; JDBC statements only in `config.sqlite`); `dependency:tree` equals `config/dependency-tree.txt` (diff empty); coverage 95.55 percent of lines; `npm audit --audit-level=high` found 0 vulnerabilities, all 3 lockfile `resolved` URLs are on `registry.npmjs.org`; the README states that the database is locked while the application runs (line 42). Open for the Liaison and Security: longest request of the mixed load 7.05 to 7.08 s end to end (above the 5 s pool timeout although no request failed), slow readers were not cut by the server within 12 s, SEC-F-126 (`static-locations`) left unchanged because removing it is a production edit outside the task's file list.

## Task 49+50: Raw-socket run, grammar corners, hidden-channel repeat and log hygiene (IN_REVIEW)

- Branch: `test/task-49-50-raw-socket-release`; delivery handoff 164. Tests only: `PreApplicationIntegrationTest`, `GrammarCornersIntegrationTest`, `TextAndParserIntegrationTest`, `LogHygieneIntegrationTest`, `DripAndCostIntegrationTest` and support `FreshApp`, `RawRules`, `DripClient`. All five classes passed inside the full failsafe run; the 9-minute cap ended the full build before its end (no coverage figure). Findings: E-37 two-space request line answered 200 (spec: 400); E-28 FOO without content type answered 415 (spec: 405 or pre-application). Drip clients (30 and 60 s windows) not cut on request line and headers; 12 held connections did not stop the service; NFC at 8,000 marks 170 to 205 ms.

## Task 24: Frontend text and failure logic (IN_REVIEW)

- Branch: `feature/task-24-text-failure-logic`; delivery handoff 168. New `frontend/js/logic/text.js` (trim by White_Space, NFC, code-point length) and `failures.js` (Section 5.9 table), exact texts added to `copy.js`, unit tests next to each; 69 frontend unit tests pass. Two findings for the Liaison (title/author field message vs fixed-text-only wording; http text split for the source gate), see the handoff.

## Task 25: List state, filter and list states (IN_REVIEW)

- Branch: `feature/task-24-text-failure-logic` (chunk branch); delivery handoff 170. New pure model `frontend/js/logic/listState.js` with tests, list texts in `copy.js`; filter radio group, loading, empty, filter-empty and failed states, count arithmetic and stale-response discarding in `app.js`, `listView.js`, `index.html`, `styles.css`. 79 frontend unit tests pass. Exploration was a scripted run (no MCP tool server).

## Task 26 and 27: Add form errors and duplicate dialog (IN_REVIEW)

- Branch: `feature/task-24-text-failure-logic` (chunk branch); delivery handoff 172. Task 26: `ui/addForm.js` (client checks, field messages with `aria-describedby`, in-flight guard, failure handling), new `ui/messages.js` and pure `logic/addCheck.js` with tests, copy strings, field error slots in `index.html`, `css/styles.css`; minimal `api/http.js` fix keeping a `status` detail without a message. Task 27: new `ui/dialogs.js` (native modal `<dialog>`, Cancel first focus, Escape cancels, Tab wrap), `addForm.js` resend with `allow_duplicate: true`. 85 frontend unit tests pass; smoke 3 of 3 pass (against the jar's baked-in frontend, see handoff); scripted exploration recorded in handoff 172.

## Task 28: E2E library view, filter and states (IN_REVIEW)

- Branch: `test/task-28-e2e-library-view`; delivery handoff 176. New `e2e/support/stubs.ts` (held, failed and fixed list answers) and `view.ts`, `library.ts` edit, two specs for US-010; SEC-F-197 fixed in `logic/listState.js` with a unit test. 86 frontend unit tests and 27 E2E tests pass. SEC-F-198 measured: 5,000 maximum-size entries re-render in about 1.7 s; `content-visibility` did not help and was reverted (left to Task 48).

## Task 29: E2E add, validation and duplicate flows (IN_REVIEW)

- Branch: `test/task-29-e2e-add-and-duplicate`; delivery handoff 180. New `e2e/support/pages.ts` (form and dialog locators, POST tracking, held and failed adds with real headers, Messages-area update counter) and three specs (`us-011-add-book`, `us-011-add-failures`, `us-011-duplicate`); smoke test 4 added to `us-010-library-view.spec.ts`. 69 E2E tests pass (full run); no production change. One finding: Shift+Tab after a click on non-interactive dialog text leaves the two buttons (pinned with `test.fail()`, awaiting a decision).
- F-29-1 fixed (Task 27 follow-up): focus trap in the duplicate dialog now keeps Tab and Shift+Tab on the two buttons via pure `nextTabTarget`; `test.fail()` removed; 91 unit and 69 E2E tests pass.

## Task 35: Client endpoints for status, rating, clear and delete (IN_REVIEW)

- Branch: `feature/task-35-client-endpoints`; delivery handoff 184. `booksApi.js` gains `getBook`, `setStatus`, `setRating`, `clearRating`, `deleteBook` (ids only through `bookPath`; invalid id, status or rating rejects with TypeError before any request; DELETE carries Content-Type and no body). 99 frontend unit tests pass. SEC-F-097 asserted in tests.

## Task 36: Entry view, status control and confirmation (IN_REVIEW)

- Branch: `feature/task-36-entry-status`; delivery handoff 188. New `ui/entryView.js` (entry from the template, updated in place, status select, mismatch note, rating mount point), `ui/entryActions.js` (delegated change listener, one request per entry, confirmation, failures beside the entry, 404 removal), `logic/statusChange.js` with tests, copy strings, a second `<dialog>` served by a shared `runDialog` in `ui/dialogs.js`. 103 unit tests and the 4 smoke tests pass. Exploration was a scripted run (Playwright browser tools not exposed). Deviation: the control is a `select` (plan AP-8), not a radio group as the brief says.

## Tasks 37 and 38: Rating group, clear rating, delete with confirmation (IN_REVIEW)

- Branch: `feature/task-37-rating-group` (chunk branch); delivery handoff 192. Rating group (five native radios in a `role="group"`), clear button, rating failure paths (`INVALID_RATING`, `RATING_NOT_ALLOWED` with entry reload, `BOOK_NOT_FOUND` removal), delete button with the "Delete book" dialog and the focus rules; new `logic/entryOps.js` with tests, copy strings. 109 unit tests and the 4 smoke tests pass. Exploration was a scripted run (Playwright browser tools not exposed). F-35-1, F-35-2 and SEC-F-200 handled.

## Tasks 39 and 40: E2E status, rating and delete (IN_REVIEW)

- Branch: `test/task-39-40-e2e-status-rating-delete` (stacked on the chunk 37+38 branch); delivery handoff 196. New `us-012-status-rating.spec.ts` and `us-012-delete.spec.ts`, helpers `support/requests.ts` and `support/storage.ts`, smoke tests 5 and 6, storage assertions in the add and delete flows (SEC-F-138). Two older locators repaired (add-form Status control, status label vs option) because the entry status select of Task 36 made them ambiguous. F-37-1 focus landing recorded: body. 109 unit and 95 E2E tests pass.

## Task 54: Focus after RATING_NOT_ALLOWED (IN_REVIEW)

- Branch: `feature/task-54-focus-after-rating-not-allowed`; delivery handoff 200. Pure `focusKeyAfterReload` in `logic/entryOps.js`; `entryActions.js` applies it after the entry reload (focus falls back to the status select when the control used is hidden or gone). F-37-1 annotation replaced by an assertion. 110 unit and 95 E2E tests pass.

## Task 55: Remove the inert static-locations property (IN_REVIEW)

- Branch: `chore/task-55-remove-static-locations`; delivery handoff 204. Key removed from `application.yml` (`add-mappings: false` kept); the three tests no longer expect it (absent by default); catalog served from the literal classpath location, proven unchanged. Full build: 351 tests, 0 failures, 0 errors, 1 skipped, exit 0 (SEC-F-126).
