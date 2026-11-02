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
