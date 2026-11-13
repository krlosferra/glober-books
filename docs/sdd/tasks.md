# GloberBooks Task Backlog

- Owner: PM (`sdd-product-manager`)
- Version: 0.6.8 (changelog: 0.6.8, 2026-11-13, Sponsor decisions of 2026-11-13: Task 54 (focus after `RATING_NOT_ALLOWED`, frontend, P1, Increment 2 after Task 39+40) and Task 55 (remove the inert `spring.web.resources.static-locations` property, backend, P1, Increment 3 after Task 41+42) added; both Security per task; they sit outside the release totals of section 1 (about 25 min each, not recomputed); 0.6.7, 2026-11-13, Sponsor approval of 2026-11-13: test-only tasks (Production none, or test support files only) move from `Security: per task` to `Security: increment`: Tasks 39+40, 44, 45 and 47; Tasks 43 (dependency pin) and 48 (documents) stay per task; no other change. 0.6.6, 2026-11-10, constitution 1.7 rule 5: Tasks 46 (styles polish) and 52 (measurements and responsive layout) move from `Security: per task` to `Security: increment` because they are layout and styling only; the Security ruling of handoff 087 predates 1.7 and the Sponsor decided otherwise for this narrow case; Security may upgrade either task at any time; Tasks 47, 48 and 53 stay per task. 0.6.5, 2026-11-06, Task 34a split into 34a-1 (complete `DataDirectoryGuard` plus file identity, unit-proved, not wired) and 34a-2 (wiring, `StartupFailureAnalyzer`, jar-level proofs) with the cut recommended by the Liaison (handoff 146 (d)) and agreed by Security (handoff 147, SEC-F-178), replacing the cut proposed in the 0.6.4 size note; dependencies, traceability, tier register, totals and ceiling list updated; tier sentence and extra support files for Tasks 32 and 33; 0.6.4, 2026-11-06, Task 33 criterion 3 narrowed to "modes stay no wider" with a pre-created 0700/0600 location; creation of the directory and file with these modes, wiring of `SqliteDataSourceProvider`/`DataDirectoryGuard` in the packaged jar and the clean failure on an uncreatable directory moved to Task 34a, criteria 4 and 5 and a split proposal; dependencies of 34a and 34b updated; handoff 145)
- Last updated: 2026-11-13
- Status: Draft (Phase 3, round 3 of 3, final edits). Round 1 findings (handoffs 025, 026) were resolved in 0.4 and the round 2 findings (handoffs 031, 032) in 0.5; the answers by finding number are in handoff 027 (sections 2 and "Round 3"). Definitions only: task state lives in `status.md`.
- Inputs: `delivery-strategy.md` v1.0, `spec.md` v0.4.3 (draft change requests CR-1 to CR-9 on top of the approved 0.4.1), `constitution.md` 1.1, `security.md`, `plan.md` and `plan-appendix.md` 0.2, handoff 022 (lean backend plan, tasks B01 to B27) and handoff 021 (frontend plan, candidates F0.1 to F4.3).
- Sponsor decisions applied. 2026-10-30: spec frozen except change requests; Increment 4 is P1 hardening; Increment 0 is a thin walking skeleton; SQLite uses the default rollback journal with exclusive locking (no WAL) and the schema of the plan; static content by our own controller; `TRACE` answered by the container with a pre-application `405`. 2026-11-02: the full P0 scope is accepted against the indicative 6 to 8 h (the figure accepted was about 32 h of task time; section 1 shows the figure after the splits of round 2 and asks the Sponsor to confirm it); Increment 0 is split into 0a (Tasks 01 to 08) and 0b (Tasks 09 to 14), with tag `v0.1` after Task 08 and `v0.2` after Task 14; Tasks 49 and 50 run before the release tag; the accessibility checker runs in tests only and the application CSP is never relaxed; change requests CR-1 to CR-9 go to review.

## 0. How to read this backlog

**Numbering.** Tasks are numbered 01 to 53 in build order; a task that was split in round 2 carries a letter (03a, 03b, 23a, 23b, 31a, 31b, 34a, 34b; Task 34a is further split into 34a-1 and 34a-2 since 0.6.5) so that no other number changes. Since 0.6 (Sponsor decision of 2026-11-05, option B of section 9) four pairs are merged and titled with both numbers: Task 18+19, Task 39+40, Task 41+42 and Task 49+50. Both numbers stay visible, every reference to a merged number points to the merged task, and the branch carries both numbers (for example `feature/task-18-19-text-policy`). A merged task waives the 45-minute limit (its estimate is the sum and it is flagged "long task"), but not the files rule. **A merged task that overruns is split back into its two original tasks at the point of the overrun; nothing is extended.** Branch `<type>/task-NN-<slug>`, commit `type(scope): subject [Task-N]` (`git-workflow.md`; `test/` branches contain tests only, anything that ships configuration, a README or documents uses `chore/`, `feature/` or `docs/`; the `progress.md` note every task updates is exempt on every branch). The architects' ids are kept as "Source: Bnn" and "Source: Fn.n". The constitution's "Task 1" is Increment 0 as a whole (Tasks 01 to 14); section 8 maps each Task 1 proof to its task.

**Release scope.** The exercise release is Increments 0 to 3 plus Task 49+50 (Sponsor decision of 2026-11-02). Within the release, Task 48 (documentation) is built after Task 49+50 even though it is numbered earlier; Tasks 51 to 53 follow the tag, time permitting.

**Files rule (accepted from 022, question L-1).** A task lists at most 4 production and resource entries it creates or edits; its own tests are listed separately ("Tests:") and belong to the task. An entry is one file or a group of closely coupled files in one folder; the build files and the generated Maven wrapper count as one entry. PM remark: the rule hides test volume, so the 45-minute estimate is the binding limit; a task that overruns it is split, never extended. In every task the first failed spike or the first unplanned fixture problem stops the task for a split. The same rule applies to the frontend.

**Estimates** are agent working minutes for build plus own tests, excluding Liaison and Security reviews and the Sponsor gate. Tasks 01, 03a, 06, 08, 14, 16, 21 and 34b are at the ceiling (34a-1 and 34a-2 are about 30 minutes each, below it) (plan N-6). **Tier** is P0, P1 or P2; section 7 is the tier register and is the single authority (the appendix of the plan is regenerated from it).

**Parallelism.** The contract in `spec.md` is final for every clause the frontend uses, so frontend tasks may start before the backend tasks they are tested against; a frontend E2E task starts only when the backend tasks it lists are merged. Backend and frontend tasks never edit the same files.

**Definition of Ready.** Spec clause approved; text marked CR-n counts only after the Sponsor approves the change request, and the Phase 3 gate approves CR-1 to CR-9 and DEC-034 before Task 11 is briefed (Task 11 cites CR-4); acceptance criteria testable; dependencies DONE; Liaison task brief written. Task 13 additionally needs the Sponsor's amendment of the SC-33 sentence (handoff 025, F-10).

**Definition of Done** (constitution plus this backlog): `cd backend && ./mvnw verify` green (backend); `cd e2e && npx playwright test` and `node --test "frontend/**/*.test.js"` green (frontend); real output attached; coverage at least 80% (backend); every acceptance criterion mapped to a test; no new dependency, schema change or file outside the scope; `progress.md` and affected documents updated in the same branch; `hygiene` green; Liaison ACCEPT, Security PASS (or PASS WITH CONDITIONS with every condition closed) where Security review applies (next paragraph), PM acceptance, Sponsor approval. **Review-load rule** (Sponsor-approved 2026-11-05, option A; the constitution amendment is in progress): the Liaison audits every task; the Security reviewer reviews every task that touches input, output, configuration, dependencies, headers or the frontend; every other task is covered by one Security review per increment. The PM marks each task in its header "Security: per task" or "Security: increment". Pure internal logic with no input, output, configuration, dependency, header or frontend effect is "increment"; tests-only tasks that exercise controls are "per task". The marks of Tasks 01 to 12 (done) are not set. **Rule for UI tasks whose behaviour is tested by a later task** (marked "Tested by Task N"): such a task is accepted on (a) its logic tests, (b) Playwright MCP exploration evidence per `frontend-testing.md` section 5 for the criteria that the real page can provoke against the backend tasks it lists as dependencies (the MCP profile allows no route, evaluate or storage tools), and (c) the named later test task. Criteria that only a stubbed response can provoke (a failed list load, `LIBRARY_FULL`, a network failure, `INVALID_RATING`, and similar) are evidenced by the logic tests and by the stub tests of the later test task, not by exploration. The spec-coverage rows the task implements are not `VERIFIED` until that later task is DONE.

**Increment gate.** Before the Sponsor tries an increment: the P0 rows listed for it, `./mvnw verify`, `node --test "frontend/**/*.test.js"`, `npx playwright test` (smoke first, once Task 14 exists), `hygiene`, and the `progress.md` note with the exact commands the Sponsor uses (Task 08 and Task 14 name them).

## 1. Totals and honest estimates

| Increment | Tasks | Backend | Frontend | Effort (sum of estimates) | Elapsed, two agents in parallel, no review waits |
|---|---|---|---|---|---|
| 0 Walking skeleton | 15 (01 to 14, 03a/03b) | 10 | 5 | 625 min, about 10.4 h | about 7.8 h |
| 1 Core library | 15 (15 to 29 with 18+19 merged, 23a/23b) | 9 | 6 | 660 min, 11 h | about 7.6 h |
| 2 Progress, rating, durability | 13 (30 to 40 with 39+40 merged, 31a/31b, 34a-1/34a-2, 34b) | 8 | 5 | 515 min, about 8.6 h | about 4.8 h (not recomputed at 0.6.2) |
| 3 Interface quality and release gate | 7 (41 to 48 with 41+42 merged) | 1 | 6 | 310 min, about 5.2 h | about 4 h |
| Increments 0 to 3 | 50 | 28 | 22 | 2,110 min, about 35.2 h | about 24 h |
| Release gate Task 49+50 | 1 | 1 | 0 | 90 min, 1.5 h (long task) | about 1.5 h |
| **Exercise release** | **51** | **29** | **22** | **2,200 min, about 36.7 h** | **about 25.5 h** |
| After the tag: Tasks 51 to 53 | 3 | 1 | 2 | 120 min, 2 h | about 1.5 h |
| All | 54 | 30 | 24 | 2,320 min, about 38.7 h | |

Merged tasks (0.6, estimate is the sum, all flagged long tasks): Task 18+19 = 90 min, Task 39+40 = 70 min, Task 41+42 = 75 min, Task 49+50 = 90 min. Effort does not shrink; the number of review cycles does (four fewer).

Backend work in the release is 1,295 minutes (about 21.6 h: 1,205 minutes, about 20.1 h, for Increments 0 to 3 plus 90 minutes, 1.5 h, for Task 49+50; Task 31 became 31a and 31b in 0.6.2, 80 minutes instead of 45; Task 34a became 34a-1 and 34a-2 in 0.6.5, 60 minutes instead of 40); frontend is 905 minutes (15.1 h). With the architect's rework allowance of about 25% the backend is about 26 h and the whole release about 45 h of task time. These are the figures the Sponsor is asked to confirm; the sums of the per-task estimates are the only source.

Findings the Sponsor must see:
1. **Budget.** On 2026-11-02 the Sponsor accepted the full P0 scope against the indicative 6 to 8 h, with about 32 h of task time for Increments 0 to 3 and about 21 h of backend work in view. After round 2 the release figures are 35.75 h (release scope) and 20.7 h (backend), because three tasks were split on the Liaison's and Security's findings (03, 23, 34), Task 31 and Task 42 gained work, and Tasks 49 and 50 joined the release (since 0.6 merged into Task 49+50). The increase is about 12% on the task time of the release scope. The PM asks the Sponsor to confirm the new figure; planning cannot reduce it, only scope can.
2. **Increment 0 has 15 tasks** (not about 8): 10 backend tasks and 5 frontend tasks, split into 0a (Tasks 01 to 08, eight backend tasks, API skeleton) and 0b (Tasks 09 to 14). The split follows the Sponsor's decision; 03b exists because the Security finding on fail-closed start (SEC-F-067) moved the start policy into Increment 0.
3. **Tags** (Sponsor decision): `v0.1` after Task 08 (0a), `v0.2` after Task 14 (0b); the release tag follows Task 48 and Task 49+50. Later tags are named at each increment plan.
4. **Total 52 tasks after the merges of 0.6** (56 before): the Sponsor applied option B of section 9 on 2026-11-05 to four pairs (18+19, 39+40, 41+42, 49+50); section 9 explains why 15+16 and 30+31 were not merged.
5. **PM deviation from 022 (accepted by the Liaison and Security):** the release sweep is Task 42 (now part of Task 41+42) in Increment 3; the long runs stay after the tag (Task 51).

## 2. Increment 0: walking skeleton (Tasks 01 to 14), split into 0a and 0b

**0a (Tasks 01 to 08): API skeleton.** Runs with `./mvnw spring-boot:run`; adds and lists books through the API; every gate green. The `v0.1` build has no README, no page, no container error wrapper and thin validation: it is a checkpoint for the Sponsor, not for real data, and its database is deleted before Increment 1. Acceptance of 0a by the Sponsor is: `./mvnw verify` green, the commands of the Task 08 `progress.md` note (a `curl` add and list on `127.0.0.1:8080`) give the stated results, the P0 gate rows below that exist at 0a pass. **0b (Tasks 09 to 14): page and smoke.** The page is served, one Playwright smoke passes, README run instructions work on a clean clone. Acceptance of 0b is: the commands of the Task 14 README work on a clean clone, `npx playwright test --grep @smoke` is green, the page adds and lists a book by hand. The record of this split in `delivery-strategy.md` section 5 is made by the orchestrator (handoff 027).

Gate rows (P0): E-04, E-07 to E-14, E-16 (0b), E-21, E-23, E-09 (declared and chunked), E-15 and E-39 (application rows, 0b) and E-30 (0b, as exercised by Task 09), mandatory properties effective, US-010.1, US-011.1, US-011.2, US-014.6 (smoke, 0b), `hygiene` green. Deferred to Increment 1: route table, `Accept`, query and id checks, the container error wrapper, text policy, duplicates, capacity, filter, dialogs.

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
- Description: Add Checkstyle 14.3.0 (plugin 3.6.0) with the constitution rules and regex bans (including the SC-27 ban on outbound HTTP clients in main sources), and SpotBugs with Find Security Bugs, verifying the unverified pins or requesting Sponsor re-approval, with a justified exclusion file.
- Production: (1) build files; (2) `config/checkstyle/checkstyle.xml` and `suppressions.xml`; (3) `config/spotbugs/exclude.xml`. Tests: none (seeded violations are recorded runs).
- Acceptance criteria:
  1. Checkstyle runs in `verify` with naming (3 characters, `id` exception), 300-line and 50-line limits, imports, Javadoc, line length and regex bans for wall-clock calls, `System.out`, `printStackTrace` and `select *`; one seeded violation of each ban fails the build (recorded). The time rule also catches `new java.util.Date()` and the spaced form `.now (` (SEC-F-090).
  1a. SC-27: a regex ban on outbound HTTP clients fails the build in `src/main` with a message naming SC-27 for `RestTemplate`, `RestClient`, `WebClient`, `HttpClient`, `HttpURLConnection`, `URLConnection`, `.openConnection(`, `.openStream(` and `java.net.http`; a narrow suppression in `src/test` allows the JDK client in test code only; one seeded run fails in `src/main` and passes in `src/test` (recorded) (SEC-F-089).
  2. Checkstyle 14.3.0 with plugin 3.6.0 is shown to work together, or a re-approval request with working pins is in the delivery (SC-19).
  3. The SpotBugs and Find Security Bugs pins are chosen after checking publishers and Java 21 support, bound to `verify`; every exclusion has a written justification (SC-21).
  4. `verify` is green with the gates active.
- Traces: SC-07, SC-14, SC-19, SC-21, SC-27.

### Task 03a: SQLite data source, claim and clock
- Source: B03 (first part) | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 01 | Branch: `feature/task-03a-sqlite-data-source`
- Description: Build the single-connection SQLite data source with the fixed settings (rollback journal never changed, `synchronous=FULL`, exclusive locking and the claim, immediate transactions), the creation of the directory (0700) and file (0600), the private native-library directory, `schema.sql` and the clock.
- Production: (1) `config/sqlite/SqliteDataSourceProvider.java`; (2) `config/ClockConfig.java`; (3) `schema.sql`. Tests: `SqliteStoreIntegrationTest`, `support/ClaimChildMain` (a small test main used as the second process).
- Acceptance criteria:
  1. A missing directory is created 0700 and a missing file 0600 before the first connection; after a write, every file the storage creates next to it is not wider (US-009.3, SC-13). After the claim the application only calls `stat`-type operations on the database path (a plain read would drop the lock, SEC-F-069 b); tests hash the file only when no instance holds it.
  2. Settings read back on the live connection equal the plan: default rollback journal (any other value stops the start), `synchronous` FULL, `locking_mode` EXCLUSIVE, `foreign_keys` on, `busy_timeout` 5000, `trusted_schema` off; loading an extension fails. Pool: size 1, `minimum-idle` 1, `idle-timeout` 0, `keepalive-time` 0, `max-lifetime` 0; the claim and the settings run at every connection initialization, and a test that forces the eviction of the connection reads them back (SC-32, N-3, SEC-F-069 c).
  3. The claim (`BEGIN EXCLUSIVE`, `COMMIT`, no change) makes a second process fail with "database is locked": the second process is a separate JVM (`support/ClaimChildMain`), not a second connection in the same JVM; the file hash is unchanged after both have stopped (US-009.7, N-2, SEC-F-069 a). If the claim cannot do this, the fallback (a no-op write transaction) is decided in this task with the PM, before Increment 2.
  4. `org.sqlite.tmpdir` is `<data dir>/tmp` (0700); if the driver cannot load from it the property is dropped and the risk goes to `security.md` through a handoff (X-10).
  5. `schema.sql` runs at every start; time comes only from the injected clock.
- Traces: US-009.3, US-009.7, M-3, SC-13, SC-32.

### Task 03b: Start policy and configuration
- Source: B03 (second part) and B12 (StartupPolicy, moved by SEC-F-067) | Layer: backend | Tier: P0 | Estimate: 35 | Depends on: 01 | Branch: `feature/task-03b-start-policy-config`
- Description: Refuse to start on a non-loopback bind address or a datasource URL that is not `jdbc:sqlite:<file>`, and write `application.yml` with every mandatory property and the Tomcat and Hikari limits.
- Production: (1) `config/sqlite/StartupPolicy.java`; (2) `application.yml`. Tests: `StartupPolicyTest`, `StartupPolicyIntegrationTest`.
- Acceptance criteria:
  1. `SERVER_ADDRESS=0.0.0.0`, `:memory:`, a URL with parameters (for example `?journal_mode=WAL`) and another scheme stop the start with a fixed reason without book data; the URL form is fixed to `jdbc:sqlite:<file>` with no parameters and only `<file>` is configurable, so a valid `SPRING_DATASOURCE_URL` with another path starts (SC-39, SC-01, SC-32).
  2. `application.yml` holds every mandatory property of the constitution, the Tomcat limits of the plan (`threads.max 8`, `min-spare 2`, `max-connections 128`, `accept-count 128`, `connection-timeout 5s`) and an explicit low `max-swallow-size` (16 KiB proposed, SEC-F-073), and the Hikari `connection-timeout` 5000. It holds no property that changes the directory scanned by the static catalog (SEC-F-072).
  3. A change of any SC-06 value after this task is ASK FIRST and returns to Security (SEC-F-078).
- Traces: SC-01, SC-06, SC-39, constitution mandatory properties.

### Task 04: Domain and repository
- Source: B04 | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 03a, 03b | Branch: `feature/task-04-domain-repository`
- Description: Add the `Book` entity, the status enum with converter, the repository and the book response, and prove Hibernate `validate` on the STRICT table.
- Production: (1) `domain/Book.java`; (2) `domain/BookStatus.java`; (3) `repository/BookRepository.java`; (4) `dto/BookResponse.java`. Tests: `BookRepositoryTest`.
- Acceptance criteria:
  1. `ddl-auto: validate` passes with the STRICT table and CHECKs, and a test shows that the DDL text stored in `sqlite_master` after the start equals the normalised `schema.sql` statement (the proof Task 34b relies on, plan X-06 and AP-12.1). If validation does not pass, the task stops and requests Sponsor approval of the fallback (`typeof` CHECKs change `schema.sql`; ASK FIRST); the fallback is not applied on the task's own authority (X-06).
  2. The database rejects a bad id, title or author length, status, rating range, a rating on a non-completed book, `date_completed` not tied to completed status, and a malformed timestamp (INV-1 to INV-4).
  3. A saved book returns with the same millisecond timestamps and lower-case status; the ordered query gives `date_added` descending then `id` ascending, ties included (US-002.2, BR-010).
  4. `Book` has no public setters; no query concatenates SQL or uses `select *` (SC-07).
- Traces: Section 4, INV-1 to INV-4, BR-010.

### Task 05: Error foundation
- Source: B05 | Layer: backend | Tier: P0 | Estimate: 35 | Depends on: 03a | Branch: `feature/task-05-error-foundation`
- Description: Add the 16 error codes with their statuses, the envelope records, the single factory and the writer used by filters.
- Production: (1) `error/ErrorCode.java`; (2) `error/ErrorBody.java` (with nested field problem); (3) `error/ErrorBodyFactory.java`; (4) `error/ErrorResponseWriter.java`. Tests: `ErrorBodyFactoryTest`.
- Acceptance criteria:
  1. `ErrorCode` holds the 16 codes with the HTTP status of Appendix A; a body has exactly `error`, `code`, `timestamp`, plus `details` only in the shapes of Appendix A (INV-6).
  2. The timestamp comes from the injected clock in the fixed format.
  3. No message contains user input, class names, SQL or paths; no `details` field is of type `Object` (SC-08, SC-11).
  4. The writer sets `application/json; charset=UTF-8` explicitly and writes only the envelope (SC-29).
- Traces: Appendix A, INV-6, SC-29.

### Task 06: Thin gate, exception advice and container error report
- Source: B06 | Layer: backend | Tier: P0 | Estimate: 45 (dense) | Depends on: 05 | Branch: `feature/task-06-thin-gate`
- Description: Add the request filter that runs first (security headers, Host, Origin, Sec-Fetch-Site, Content-Type, 16 KiB body cap declared and chunked), the exception advice including the mapping of the framework's routing exceptions, and the Tomcat error-report settings that keep the product and version out of container error pages.
- Production: (1) `config/transport/RequestGateFilter.java` and `HeaderPolicy.java`; (2) `config/transport/BoundedBodyRequest.java`; (3) `error/GlobalExceptionHandler.java`; (4) `config/transport/TomcatErrorReportCustomizer.java` (error valve with `showReport=false`, `showServerInfo=false`, unconditional). Tests: `HeaderPolicyTest`, `RequestGateFilterTest`, `GlobalExceptionHandlerTest`.
- Acceptance criteria:
  1. The security headers are set on every response and again after `reset` or `sendError` (SC-10).
  2. Forged Host forms give `400 INVALID_HOST`; allowed forms pass; a bad `Origin` or `Sec-Fetch-Site` gives `403 FORBIDDEN`; a non-GET request without a valid `Content-Type` gives `415` (BR-012 step 1, SC-03, SC-04, SC-05, SC-28).
  3. A declared body over 16,384 bytes is `413` before any read, no `100 Continue`; a chunked or unread body is read into a buffer and stopped at byte 16,385 (SC-06).
  4. An unexpected exception gives `500 INTERNAL_ERROR` "Unexpected error." with one stack-trace log line and no user text; the advice answers JSON even for `Accept: text/xml` (SC-11, SC-29).
  5. The advice maps the framework's not-found, method-not-supported, not-acceptable and unsupported-media-type exceptions to the envelope with the matching codes; an error page produced by the container shows no product, version or report (SC-02, SC-11, SEC-F-066 a and b).
- Traces: BR-012 step 1, SC-02, SC-03 to SC-06, SC-10, SC-11, SC-28.

### Task 07: Add and list API (thin)
- Source: B07 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 04, 06 | Branch: `feature/task-07-add-and-list-thin`
- Description: Implement `POST /api/v1/books` and `GET /api/v1/books` with basic validation only (replaced in Task 20) and `allow_duplicate` accepted without effect (until Task 23a).
- Production: (1) `service/BookService.java`; (2) `controller/BookController.java`; (3) `dto/CreateBookRequest.java`; (4) `dto/BookListResponse.java`. Tests: `BookServiceTest`, `BookControllerTest`.
- Acceptance criteria:
  1. A valid title and author give `201` with the seven members, `unread`, null rating and `date_completed`, lower-case UUID `id`, millisecond `date_added` (US-001.1, US-001.2, 5.2).
  2. The list gives `200` with `books` and `total`, newest first, ties by `id`; empty gives `[]` and 0 (US-002.1 to .3, .5).
  3. Empty or over-length title or author gives `400 VALIDATION_ERROR` with the field; an unknown member gives `400 MALFORMED_REQUEST`.
  4. Add is one transactional service method, list a read-only one, and a read writes nothing (X-07); controllers hold no rule.
  5. The list is written directly to the response stream, never to an intermediate `byte[]` (SEC-F-071).
- Traces: US-001.1, US-001.9 (via list), US-002.1, 5.1, 5.2.

### Task 08: Real-port baseline and mandatory-property proofs (closes 0a)
- Source: B08 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 07 | Branch: `chore/task-08-real-port-baseline`
- Description: Add the raw-socket client and the first real-port security class, a round trip with checkpoint logs, a test per mandatory property and the `progress.md` note with the commands the Sponsor uses to try 0a.
- Production: (1) `application.yml` (edit only if a wrong key is corrected); (2) `docs/sdd/progress.md` (the 0a note). Tests: `RawHttpClient`, `TransportSecurityIntegrationTest`, `MandatoryPropertiesIntegrationTest`.
- Acceptance criteria:
  1. Headers of the constitution on every response class reachable at 0a: `200`, `201`, `400` (Host and malformed body), `403` (Origin), `404` (an unknown path, mapped by the advice of Task 06), `405` (a wrong method on a defined path, mapped by the advice), `413`, `415`, and `500` (forced by a trigger created in the test database); none has `Server`, `Set-Cookie`, `Location` or `Access-Control-*` (E-21, E-23, SC-10).
  2. Host, Origin, Fetch-Metadata, Content-Type and body-cap rows give the stated results, including a chunked body crossing 16,384 bytes (E-04, E-07 to E-14, E-09, SC-28, SC-30).
  3. Raw-socket check: an invalid request line, the method `FOO` and a header over 8 KiB are answered with a status in {400, 405, 414, 431, 501} and a body that contains no `Tomcat`, no version and no HTML, and the next request is served (SEC-F-066 c).
  4. A `POST` then `GET` round trip passes on the real port; the checkpoint logs contain no book data.
  5. Every mandatory property is shown effective, with Boot 4 and Jackson 3 key names for naming and unknown fields; a wrong key is corrected and reported (constitution, SC-31 partial); the log pattern removes CR, LF, U+2028, U+2029, U+0085 and ESC (SC-37 pattern).
  6. The `progress.md` note holds the exact commands to try 0a (start with `./mvnw spring-boot:run`, `curl` add and list on `127.0.0.1:8080`, stop) and states that `v0.1` is a checkpoint, not for real data, and that its database is deleted before Increment 1 (SEC-F-077).
- Traces: SC-30, E-04, E-07 to E-14, E-21, E-23, integration checkpoints.

### Task 09: Static content (opens 0b)
- Source: B09 | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 08 | Branch: `feature/task-09-static-content`
- Description: Serve the page and its assets from a fixed catalog built at start from `classpath:/static/`, with the framework's resource handler off. The catalog takes its resources as a constructor argument; production wiring passes `classpath:/static/` only.
- Production: (1) `config/transport/StaticAssetCatalog.java`; (2) `controller/StaticContentController.java`. Tests: `StaticContentIntegrationTest` (builds the catalog directly with a fixture directory; no property, no profile).
- Acceptance criteria:
  1. `GET /` and `/index.html` return `200` with the same bytes; `.html`, `.css` and `.js` return `text/html;charset=UTF-8`, `text/css;charset=UTF-8`, `text/javascript;charset=UTF-8`, with the constitution headers (E-16, 5.8).
  2. No `ETag`, `Last-Modified` or `Accept-Ranges`; `Range`, `If-None-Match` and `If-Modified-Since` are ignored and give `200` with the full body (E-30).
  3. Directories, `/favicon.ico`, a `*.test.js` path, `/error`, `/.git/config`, `/data/globerbooks.db` and traversal forms give `404 NOT_FOUND` in the envelope (E-15 application rows, E-39, SC-12).
  4. Any file with an extension other than `.html`, `.css`, `.js` under the static location stops the start; the catalog is fixed at start; no production property or profile can point it at another directory (SEC-F-072).
- Traces: 5.8, SC-12, E-15, E-16, E-30, E-39.

### Task 10: E2E toolchain and pins
- Source: F0.1 | Layer: frontend | Tier: P0 | Estimate: 30 | Depends on: none | Branch: `chore/task-10-e2e-toolchain`
- Description: Create the E2E package with exact pins and install rules, create `.nvmrc`, and verify the `@playwright/mcp` pin in one run.
- Production: (1) `e2e/package.json` and `e2e/package-lock.json`; (2) `e2e/.npmrc`; (3) `.nvmrc`. Tests: none.
- Acceptance criteria:
  1. `.nvmrc` and `engines.node` hold the pinned Node and `node --version` equals it.
  2. `npm ci` with `.npmrc` (`ignore-scripts`, `save-exact`, `engine-strict`) installs `@playwright/test` 1.64.1; `npx playwright --version` and `npx playwright install chromium` succeed; no `typescript` package and no `tsconfig` (SC-19).
  3. `npm audit --audit-level=high` is clean; all lockfile `resolved` URLs are on `https://registry.npmjs.org/` (SC-20, SC-21, SC-36).
  4. `@playwright/mcp` 0.0.83 runs once with the explicit flags; browser disk use and tool names are recorded (SC-23).
- Traces: constitution pins, SC-19 to SC-21, SC-23, SC-36.

### Task 11: Frontend logic and API client (thin)
- Source: F0.3 + F0.4 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 10 | Branch: `feature/task-11-logic-and-client-thin`
- Description: Add the DOM-free modules for exact copy, formatting and id checks and the single network module with list and add, proving the unit test command on the pinned Node.
- Production: (1) `frontend/js/logic/` (`copy.js`, `format.js`, `ids.js`); (2) `frontend/js/api/http.js`; (3) `frontend/js/api/booksApi.js`. Tests: next to each module, and `frontend/js/sources.test.js`.
- Acceptance criteria:
  1. `node --test "frontend/**/*.test.js"` runs the ES-module tests on the pinned Node without a `package.json`; if not, the delivery reports a constitution issue.
  2. Status labels, "n books" ("1 book", "0 books", "1000 books"), `Added: YYYY-MM-DD` from a UTC value in the local time zone (test sets `TZ`), UUID check and encoded path builder (US-010.3, US-010.4, BR-011).
  3. The client sends only root-relative `/api/v1/books` URLs with `Accept` and, on non-GET, `Content-Type: application/json`, `allow_duplicate` only when true; maps snake_case to camelCase; never throws for an HTTP outcome and returns `ok` or a failure of kind `api`, `network` or `unreadable` (US-013.1, US-013.3).
  4. A request without a complete response after 15 seconds counts as `network` (US-013.6 as changed by CR-4).
  5. The sources contain none of the forbidden sinks of the constitution; the grep accepts `setTimeout(function, 15000)` with a function argument and flags a string argument (SC-09, SEC-F-075).
- Traces: US-010.3, US-010.4, US-013, BR-011, SC-09.

### Task 12: Page shell with minimal list and add
- Source: F0.5 | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 11, 09 | Branch: `feature/task-12-page-shell`
- Description: Build the single page with its semantics and stylesheet, the list from a `<template>` and the add form; filter, errors and dialogs follow in Increment 1. Tested by Task 14 (smoke) and Task 28.
- Production: (1) `frontend/index.html` and `frontend/css/styles.css`; (2) `frontend/js/ui/dom.js` and `listView.js`; (3) `frontend/js/ui/addForm.js`; (4) `frontend/js/ui/app.js` and `frontend/js/main.js`. Tests: none beyond Task 11 logic tests.
- Acceptance criteria:
  1. `lang="en"`, the title and the single `h1` "GloberBooks", a main landmark, "Your books", a list named "Books", the form with Title, Author, Status (default Unread) and "Add book", focus on Title (US-010.1, US-011.1, US-014.4 without the 320 clause).
  2. Entries show title, author, status label, "Added: YYYY-MM-DD" and the rating rules through `textContent` only; count "n books"; API order; "Loading your books…" in a status element (US-010.2, .3, .4, .8).
  3. Title, Tab, author, Enter sends exactly one `POST`, prepends the book, clears the form, announces "Book added: <title>" and returns focus to Title (US-011.2, .4, .5).
  4. Every request the page makes is same-origin and answered from the static catalog or the API; no inline script, style or handler (US-014.7, SC-10).
  5. MCP exploration evidence for AC1 to AC3 is recorded (Definition of Done rule for UI tasks).
- Traces: US-010.1 to .4, .8, US-011.1, .2, .5, US-014.4, .7.

### Task 13: E2E harness (config, launcher, isolation)
- Source: F0.2 | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 10, 08; Sponsor amendment of the SC-33 sentence (Definition of Ready) | Security: per task | Branch: `chore/task-13-e2e-harness`
- Description: Add the Playwright configuration, the launcher that starts the jar on a temporary database directory and the global setup asserting the isolation rules, with a placeholder test.
- Production: (1) `e2e/playwright.config.ts`; (2) `e2e/global-setup.ts`; (3) `e2e/support/start-server.mjs`. Tests: `e2e/tests/harness.spec.ts` (placeholder: `GET /api/v1/books` is `200`).
- Acceptance criteria:
  1. `baseURL` is the literal `http://127.0.0.1:18080`; the setup asserts host, port and that the jar exists; neither is read from the environment; a busy port fails the run (SC-33).
  2. The launcher passes `--server.address=127.0.0.1`, `--server.port=18080` and `--spring.datasource.url=jdbc:sqlite:<dir>/library.db` as command-line arguments of `java -jar` and starts the child with an allowlisted environment (`PATH`, `HOME`, `JAVA_HOME`, `LANG`, `TZ` only; no `SPRING_*`, no `JAVA_*OPTIONS`, no `SERVER_*`) and with the working directory set to its own `mkdtemp` directory, which holds no configuration file, so no `./config/application.*` or `./application.*` is read (SEC-F-080); it creates an unpredictable directory 0700, asserts the database path inside it, forwards termination, never opens the database file, and deletes only the path returned by its own `mkdtemp`, after the child has exited, after checking that its real path is a direct child of the real `os.tmpdir()` with its prefix and not a symlink (SEC-F-070).
  3. A run (tagged `@isolation`, executed by hand) with the five hostile variables of AP-9.2 set (`SPRING_DATASOURCE_URL` pointing at a decoy file, `SPRING_APPLICATION_JSON` setting another URL and bind, `JAVA_TOOL_OPTIONS` setting `-Dspring.datasource.url=...`, `SERVER_ADDRESS=0.0.0.0`, `SERVER_PORT` set to another port) leaves the decoy byte-identical (hashed only when no instance holds it), serves on `127.0.0.1:18080` only, listens on no other port or interface, and keeps the data in the launcher's directory.
  4. The order of web-server start and global setup in the pinned Playwright is established by a run and recorded as evidence (Q-F7).
  5. The placeholder gets `200` from the readiness URL.
- Traces: SC-33, SC-39, SC-23, plan frontend F4.2.

### Task 14: Smoke tests, guard fixture and README (closes 0b)
- Source: F0.6 (+ README of Increment 0) | Layer: frontend | Tier: P0 | Estimate: 45 (dense) | Depends on: 12, 13, 09 | Security: per task | Branch: `chore/task-14-smoke-guard-readme`
- Description: Add the guard fixture and the three Increment 0 smoke tests, explore the page once with the browser tool and write the run instructions, proving them on a clean clone.
- Production: (1) `e2e/support/fixtures.ts`; (2) `e2e/support/builders.ts` and `library.ts`; (3) `README.md`; (4) `docs/sdd/progress.md` (the 0b note and the Task 1 proof table). Tests: `e2e/tests/us-010-library-view.spec.ts` (smoke).
- Acceptance criteria:
  1. `npx playwright test --grep @smoke` is green in under 30 s: page loads with title and one `h1` (US-010.1); a keyboard-only add shows the entry and count and the time is recorded (US-011.2, M-1); `<script>alert(1)</script>` is inert text (US-014.6, T-12).
  2. The guard fails a test on a `securitypolicyviolation`, a Trusted Types console error, a request to another origin or a page error (US-014.6, .7, SC-10) and checks `Content-Type` and the constitution headers of every asset (E-16).
  3. README commands for build, run (`cd backend && ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments=-Xmx512m` and, for the jar, `java -Xmx512m -jar backend/target/globerbooks.jar`, with the memory bound confirmed or changed by Task 41+42; page at `http://127.0.0.1:8080/`; SEC-F-081), E2E setup, smoke and logic tests, the note that the database is locked while running, and the note that `v0.1` and `v0.2` are checkpoints not for real data whose database is deleted before Increment 1, work on a clean clone with only the pinned JDK and Node (executed, output attached; SEC-F-077).
  4. `./mvnw verify`, `node --test "frontend/**/*.test.js"`, the smoke run and `hygiene` are green; `progress.md` gets the exact commands to try 0b and the Task 1 proof table with evidence and the pins that remain unverified, if any.
- Traces: US-010.1, US-011.2, US-014.6, US-014.7, M-1, SC-10, SC-23, Task 1 proofs.

## 3. Increment 1: core library (Tasks 15 to 29)

Goal: the reader manages a list: full validation, filter, duplicates, capacity, dialogs. Gate rows (P0): all P0 families of section 7.1 whose tasks are in this increment, in particular E-22 live, E-33, T-31, C-01, C-02, C-06, C-07, C-09, C-10, C-11 (a) and (b), US-010.1 to .8, .10 and US-011.1 to .10.

### Task 15: Route table, methods and Accept
- Source: B10 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 09 | Security: per task | Branch: `feature/task-15-routing-accept`
- Description: Decide route and method from the raw request target, answer `404` and `405` with the exact `Allow`, and apply the `Accept` grammar on API paths. The route table holds the routes that exist at this point.
- Production: (1) `config/transport/RequestTarget.java`; (2) `config/transport/RouteTable.java`; (3) `config/transport/AcceptPolicy.java`; (4) `config/transport/RequestGateFilter.java` (edit). Tests: `RouteTableTest`, `AcceptPolicyTest`.
- Acceptance criteria:
  1. Unknown paths, trailing slash, suffix, `;`, encoded variants of fixed segments and encoded dot segments give `404 NOT_FOUND`; HEAD, OPTIONS and other methods on a defined path give `405` (E-01 to E-03, E-05, E-17, E-24 rows 2 and 3, E-27).
  2. Every `405` carries `Allow` with exactly the defined methods and never HEAD or OPTIONS (E-29).
  3. `Accept: text/xml` gives `406`; absent, empty, `*/*`, `application/json` give `200`; error bodies stay JSON (E-06).
  4. The route table holds the routes that exist at this task (`POST` and `GET /api/v1/books`, `/`, `/index.html` and the static paths); `RouteTableTest` enumerates the controllers' mappings and fails on any difference. Tasks 31a and 31b extend the table with the five endpoints they add (three and two) (X-11, SC-18, SC-40).
- Traces: 5.0 routing, methods, Accept, BR-012 step 2.

### Task 16: Query, id and empty-body rules; transport limits
- Source: B11 | Layer: backend | Tier: P0 | Estimate: 45 (dense) | Depends on: 15 | Security: per task | Branch: `feature/task-16-query-id-limits`
- Description: Add the query grammar and the `status` value check of the list, the id check judged as received, the no-body rule for GET and DELETE, the early rejection of large declared bodies and the live slow-request tests.
- Production: (1) `config/transport/QueryPolicy.java`; (2) `config/transport/RequestGateFilter.java` (edit); (3) `config/transport/BoundedBodyRequest.java` (edit). Tests: `QueryPolicyTest`, `TransportLimitsIntegrationTest`.
- Acceptance criteria:
  1. A malformed id (upper case, wrong length, any `%`) gives `400 MALFORMED_REQUEST` after route and method are known (T-14, E-24 row 1, E-27).
  2. A non-empty query on any endpoint but the list, unknown or repeated parameters, empty pairs, malformed percent-encoding and invalid UTF-8 give `400 MALFORMED_REQUEST`; `?` and `?status=%72eading` pass; 2,000 pairs give `400` (E-19, E-26, E-36 third row, E-38, T-20). A `status` value other than exactly `unread`, `reading`, `completed` (`READING`, empty, `?status`, `reading+`) gives `400 INVALID_STATUS` after the query shape check (US-002.6, T-20).
  3. Any body byte on GET or DELETE gives `400 MALFORMED_REQUEST`; a chunked body over the cap gives `413` on any route (E-18, E-32, T-39).
  4. `Expect: 100-continue` with an oversized length gets `413` and no `100`; a 415-rejected request declaring 10 MiB answers within 2 s while others are served (E-31 `Expect` row, E-33).
  5. A connection with no complete request within 5 s is closed while others are served (E-22 live); the drip variant (one byte every 2 s on the request line, on the headers and on the body of a valid `POST`) is run and its result recorded; if the connection survives, a total deadline for reading a request is added in `BoundedBodyRequest` or the limit is reported to the Sponsor as an accepted limit (SEC-F-073).
- Traces: BR-011, BR-012 steps 3 to 5, US-002.6, E-18, E-19, E-22, E-26, E-31, E-32, E-33, E-38, T-20, T-39.

### Task 17: Container error wrapper
- Source: B12 | Layer: backend | Tier: P0 | Estimate: 30 | Depends on: 06, 08, 09 | Security: per task | Branch: `feature/task-17-error-wrapper`
- Description: Turn any `sendError` inside the application into the envelope with the security headers (the start policy moved to Task 03b and the Tomcat error-report settings to Task 06).
- Production: (1) `config/transport/EnvelopeResponseWrapper.java`. Tests: `EnvelopeResponseWrapperTest`, a case in `TransportSecurityIntegrationTest`.
- Acceptance criteria:
  1. A `sendError` raised inside the application leaves as the envelope with the security headers, never as a container page (SC-02, SC-11, INV-6).
  2. `GET /error` is `404 NOT_FOUND` in the envelope.
  3. A test shows that an error dispatch cannot bypass the headers of the gate (Security review, item 1).
- Traces: SC-02, SC-11, SC-29, INV-6.

### Task 18+19: Text policy (parts 1 and 2) and the Unicode fixture check
- Merged 2026-11-05 (Sponsor, option B of section 9) from Task 18 (Source: B13) and Task 19 (Source: B14) | Layer: backend | Tier: P0 | Security: per task | Estimate: 90 (45 + 45, long task; the 45-minute limit is waived, the files rule is not; if it overruns it is split back into 18 and 19) | Depends on: 01 | Branch: `feature/task-18-19-text-policy`
- Description: Prove the Unicode data on the pinned JDK, build the code-point fixtures and implement all of BR-006: items 1 to 6 (criteria 1 to 4) and items 6b to 6d (criteria 5 to 8). Build order inside the task: criteria 1 to 4 first, then 5 to 8.
- Production: (1) `service/TextPolicy.java`. Tests: `TextPolicyTest`, `UnicodeFixtureCheckTest`, `TextPolicyMarksTest`, `support/UnicodeFixtures`.
- Acceptance criteria (1 to 4 from Task 18, 5 to 8 from Task 19):
  1. The fixture check asserts JDK at least 21.0.12, Unicode 15.0 (U+1E030 assigned, U+2FFC not), the category of every code point the spec names and the NFC result of the T-24b and T-25 inputs; a mismatch stops the task and goes to the PM.
  2. Trimming uses Unicode White_Space; a tab or line feed inside is rejected, at an end trimmed; stored text is NFC (T-02, T-08, US-001.5).
  3. Length in code points after NFC: 500/501 title, 200/201 author, 500 decomposed "é" accepted, 300 times U+1D15E rejected (T-06, T-07, T-11, T-22).
  4. NUL and other Cc, Cf except U+200C and U+200D, Zl, Zp, private use, noncharacters, unassigned (15.0) and tag code points are rejected; U+FFFD between letters is accepted (T-01, T-03, T-09, T-26).
  5. Blank-rendering text and text beginning with a mark are rejected; "ab", U+3164, "cd" is accepted (T-23).
  6. A run of 9, or with two variation selectors, is rejected; 8, and one selector, accepted (T-24, T-24b).
  7. Joiner positional rule: the Persian word with U+200C and a family emoji sequence accepted; first, last, next to a joiner or a space rejected (T-10, T-25).
  8. The 50-repetition input of T-24b finishes without timeout or unbounded regular expression.
- Traces: BR-006 items 1 to 6d, DEC-029, SC-16.

### Task 20: Validation wiring and field-error mapping
- Source: B15 | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 18+19, 06, 07 | Security: per task | Branch: `feature/task-20-validation`
- Description: Wire the text policy into Bean Validation without expression language and choose `VALIDATION_ERROR`, `INVALID_STATUS` or `INVALID_RATING` by BR-012 step 6; the service repeats the checks.
- Production: (1) `config/BookText.java`; (2) `config/ValidatorConfig.java`; (3) `error/GlobalExceptionHandler.java` (edit); (4) `dto/CreateBookRequest.java` (edit). Tests: `BookTextTest`, `GlobalExceptionHandlerTest` (edit).
- Acceptance criteria:
  1. Title and author failures give `400 VALIDATION_ERROR` with one `details` entry per failing field ordered title, author, status; `{}` gives title and author entries (US-001.4, .10, T-09).
  2. Only `status` failing gives `INVALID_STATUS`; `status` plus another field gives `VALIDATION_ERROR`; absent status is `unread`, explicit `null` is `INVALID_STATUS` (T-34, T-35, BR-001).
  3. A 501-character title containing `${7*7}` is rejected and no message contains `${7*7}` or `49` (T-13).
  4. The thin checks of Task 07 are removed; the service throws the same exception type.
- Traces: US-001.4 to .6, .10, BR-001, BR-012 step 6.

### Task 21: Strict body reading and parser limits
- Source: B16 | Layer: backend | Tier: P0 | Estimate: 45 (dense) | Depends on: 07 | Security: per task | Branch: `feature/task-21-strict-body`
- Description: Decode the buffered body with a reporting UTF-8 decoder and parse it strictly, mapping every failure to `MALFORMED_REQUEST` within 300 ms under low read constraints.
- Production: (1) `config/json/JacksonConfig.java`; (2) `config/json/StrictJsonBodyResolver.java`; (3) `config/json/StrictStringDeserializer.java`; (4) `config/WebMvcConfig.java`. Tests: `JacksonStrictnessTest`, `StrictJsonBodyResolverTest`.
- Acceptance criteria:
  1. Invalid UTF-8, BOM, comments, trailing comma, single quotes, unquoted names, raw control character in a string, trailing content, zero-byte body, non-object value each give `400 MALFORMED_REQUEST` (T-05, T-28, T-29, T-39).
  2. Unpaired surrogate, unknown member, duplicate member (also after unescaping), wrong type, `allow_duplicate` as `"true"`, `1`, `null` give `400 MALFORMED_REQUEST` (T-04, T-16, T-21, T-27, T-30).
  3. A string up to the 16 KiB cap is a length error, not a parse error (T-11); deep nesting and huge exponents answer within 300 ms, never `500` (T-31).
  4. Each Jackson 3 switch is proved effective by a test; a key the framework does not recognize is corrected and reported (SC-31, SC-08).
- Traces: 5.0 strict parsing, BR-012 step 5, SC-31, T-04, T-05, T-16, T-21, T-27 to T-31, T-39.

### Task 22: Duplicate key and service exceptions
- Source: B17 | Layer: backend | Tier: P0 | Estimate: 35 | Depends on: 18+19, 04, 05, 06 | Security: per task | Branch: `feature/task-22-duplicates`
- Description: Implement the BR-003 key, the service exceptions with the duplicate body and their mapping by the advice.
- Production: (1) `service/DuplicateKey.java`; (2) `error/ApiException.java` and `error/DuplicateBookException.java`; (3) `error/DuplicateErrorBody.java`; (4) `error/GlobalExceptionHandler.java` (edit). Tests: `DuplicateKeyTest`, a case in `GlobalExceptionHandlerTest`.
- Acceptance criteria:
  1. NFKC, `toLowerCase(Locale.ROOT)`, collapsing of white-space runs and trim: "Clean Code" matches " clean   code " and the full-width "Ｃlean Code" (US-004.5).
  2. "Straße" and "STRASSE" do not match; "Clean Code" and "CLEAN CODE" by one author do (T-40).
  3. The ASCII fast path equals the general path on a generated set.
  4. An `ApiException` becomes its code and status through the factory; the duplicate exception produces `409 DUPLICATE_BOOK` with `details.existing_book` as one book and nothing else (Appendix A, 5.2).
- Traces: BR-003, US-004.5, T-40, Appendix A.

### Task 23a: Add rules (capacity and duplicates)
- Source: B18 (first part) | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 20, 21, 22 | Security: per task | Branch: `feature/task-23-add-rules` (chunk branch shared with 23b)
- Description: Implement capacity and duplicate detection inside the one add transaction, the override and the 409 bodies, with a seeded library.
- Production: (1) `service/BookService.java`; (2) `repository/BookRepository.java` and `BookKeyRow.java`. Tests: `BookServiceTest`, `AddRulesIntegrationTest`, `support/LibrarySeeder`.
- Acceptance criteria:
  1. A matching key gives `409 DUPLICATE_BOOK` with the earliest match and stores nothing; `allow_duplicate: true` stores a separate book; `false` is absent; status, rating and dates do not matter (US-004.1 to .4, .6, .7).
  2. At 5,000 books every add gives `409 LIBRARY_FULL` before the duplicate check, with or without the override, and the count stays 5,000 (C-01, C-02, US-001.7, US-004.9). The clauses about status change, rating and delete at capacity move to Task 31.
  3. Count, duplicate scan and insert are one transaction with no query in a loop (BR-009, INV-5, INV-7).
  4. Injection payloads are stored unchanged and the count grows by one each time (T-12); "Straße" and "STRASSE" both store (T-40).
- Traces: US-001.7, US-004, BR-003, BR-009, INV-5, INV-7, C-01, C-02, T-12, T-40.

### Task 23b: List filter and the 2-second add bound
- Source: B18 (second part) | Layer: backend | Tier: P0 | Estimate: 35 | Depends on: 23a, 16 | Security: per task | Branch: `feature/task-23-add-rules` (chunk branch shared with 23a)
- Description: Apply the validated `status` filter to the list in the order of BR-010 and prove the 2-second add bound with 4,999 maximum-size books.
- Production: (1) `service/BookService.java` (edit); (2) `controller/BookController.java` (edit). Tests: `BookListIntegrationTest`, `AddBoundIntegrationTest`.
- Acceptance criteria:
  1. `?status=reading` returns only that status in the same order with `total` of the filtered result; an empty library or filter returns `200` with `[]` and 0; the list never exceeds 5,000 entries and has no pagination members (US-002.4, .5, .8, BR-010). The value check itself is Task 16.
  2. With 4,999 books of maximum size, a non-matching add completes in 2 s with `201` and a duplicate is `409 DUPLICATE_BOOK` in 2 s, never `500` (C-11 a and b, M-2).
  3. If the bound fails the task stops and the key-column schema change (ASK FIRST) goes to the Sponsor; no weaker test is written (X-04).
- Traces: US-002.4, .5, .8, BR-010, C-11 (a, b), M-2.

### Task 24: Frontend text and failure logic
- Source: F1.1 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 11 | Security: per task | Branch: `feature/task-24-text-and-failures-logic`
- Description: Add the code-point length check and the table mapping every failure to the action and exact text of Section 5.9.
- Production: (1) `frontend/js/logic/text.js`; (2) `frontend/js/logic/failures.js`; (3) `frontend/js/logic/copy.js` (edit). Tests: next to each module.
- Acceptance criteria:
  1. Length after trim and NFC: 500 emoji count 500, 500 decomposed "é" count 500, 300 times U+1D15E count 600; empty/whitespace `empty`; 501 and 201 `length` (US-011.3, T-07, T-22).
  2. Every `code` of Appendix A and the three non-envelope cases map to the action and exact text of 5.9; an unknown code is `INTERNAL_ERROR` (US-013.1, .3).
  3. `VALIDATION_ERROR` with `details` missing, non-array, or with no usable entry maps to the `MALFORMED_REQUEST` text; an entry is usable when `field` is `title`, `author` or `status` and, for the first two, `message` is a non-empty string (CR-7); the server's `error` text is never displayed (US-013.5).
  4. Every quoted string of 5.9 and US-011 to US-013 is asserted exactly.
- Traces: US-011.3, .6, US-013.1, .3, .5, 5.9.

### Task 25: List state, filter and list states
- Source: F1.2 + F1.3 | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 24, 12, 23b | Security: per task | Branch: `feature/task-25-list-state-filter`
- Description: Add the pure page-state model and use it for the filter, the loading, empty, filter-empty and failed states, the count and stale-response discarding. Tested by Task 28.
- Production: (1) `frontend/js/logic/listState.js`; (2) `frontend/js/ui/app.js` and `listView.js` (edit); (3) `frontend/index.html` and `css/styles.css` (edit). Tests: `listState.test.js`.
- Acceptance criteria:
  1. Radio group "Filter by status" with All, Unread, Reading, Completed, All checked; choosing loads with the matching `status` (US-010.5).
  2. A new load removes entries and count and shows only "Loading your books…"; an earlier response is discarded (US-010.2 as changed by CR-2, US-010.10).
  3. Empty library and empty filter result messages with "Show all books", decided by the count (US-010.6).
  4. A failure shows the 5.9 text and "Try again" repeating the active filter, zero list items, no count; an add before the first success, during a load or after a failure is not inserted by the form (US-010.7, US-010.10, US-011.5 as changed by CR-2). The state model ignores a replaced or removed event for an entry already cleared by a new load, still announces it and moves no focus (plan AP-7.1).
  5. The count is `total` plus matching adds minus deletes and is unchanged by a non-matching add (US-010.4); MCP exploration evidence for each criterion is recorded.
- Traces: US-010.2, .4 to .7, .10, US-011.5, CR-2.

### Task 26: Add form errors and in-flight guard
- Source: F1.4 | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 24, 25, 23a | Security: per task | Branch: `feature/task-26-add-form-errors`
- Description: Add the client checks, field messages, the single-request guard, the capacity and generic failure messages and the page-level message area. Tested by Task 29.
- Production: (1) `frontend/js/ui/addForm.js`; (2) `frontend/js/ui/messages.js`; (3) `frontend/index.html` and `css/styles.css` (edit). Tests: logic tests only.
- Acceptance criteria:
  1. Empty or too long input shows an inline message linked to its field (informative wording, CR-3) and sends no request (US-011.3).
  2. While a request runs the button is disabled and a second click or Enter sends nothing, enforced in the handler (US-011.4).
  3. `VALIDATION_ERROR` shows each message beside its field, the status text beside Status, and focus goes to the first field in error; `INVALID_STATUS` shows the status text (US-011.6).
  4. `LIBRARY_FULL` shows "Your library is full (5,000 books). Delete a book to add another." and any other failure its 5.9 text; typed values are kept (US-011.9, .10, US-013.2).
  5. Each failure replaces the text of the "Messages" status area once, between the arrival of its outcome and the end of its handling; clearing at the start of a new action is separate (US-013.4 as defined by CR-5). MCP exploration evidence is recorded.
- Traces: US-011.3, .4, .6, .9, .10, US-013.2, .4.

### Task 27: Duplicate dialog
- Source: F1.5 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 26, 23a | Security: per task | Branch: `feature/task-27-duplicate-dialog`
- Description: Add the modal "Possible duplicate" dialog with the existing book, Cancel and Add anyway flows and the focus rules. Tested by Task 29.
- Production: (1) `frontend/js/ui/dialogs.js`; (2) `frontend/js/ui/addForm.js` (edit); (3) `frontend/index.html` and `css/styles.css` (edit). Tests: none.
- Acceptance criteria:
  1. `409 DUPLICATE_BOOK` opens the dialog with "This book may already exist. Add anyway?" and the existing title, author, status label and "Added: YYYY-MM-DD" as text; initial focus on Cancel; Escape cancels (US-011.7).
  2. Cancel closes, keeps values and focuses Title (US-011.8).
  3. Add anyway closes, resends with `allow_duplicate: true` and then behaves as an add; a `LIBRARY_FULL`, network or generic failure leaves focus on Title, a `VALIDATION_ERROR` follows US-011.6 (US-011.8, US-011.5, CR-1).
  4. Hostile and 500-code-point texts in the dialog are inert (US-014.6); MCP exploration evidence is recorded.
- Traces: US-011.7, .8, CR-1, US-014.6.

### Task 28: E2E library view, filter and states
- Source: F1.6 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 25, 23b, 14 | Security: per task | Branch: `test/task-28-e2e-library-view`
- Description: Test the list stories against the real backend and with stubbed responses for states the page cannot provoke.
- Production: none (test support files only: `e2e/support/stubs.ts`, `e2e/support/library.ts` edit). Tests: `e2e/tests/us-010-library-view.spec.ts` (edit).
- Acceptance criteria:
  1. Titles, one `h1`, list name, loading text, entry content with the local date for a fixed UTC value in a far time zone, rating texts (US-010.1 to .3, .8).
  2. Order, count texts, filter options, empty and filter-empty messages and "Show all books" (US-010.4 to .6).
  3. A stubbed failure shows the 5.9 text and "Try again"; no list item exists until a load succeeds; an add made during a held load is not inserted by the form and appears with the first successful list load that was requested after the add completed (US-010.7).
  4. Overlapping loads (held responses) show the last request's result (US-010.10).
- Traces: US-010, T-12 display.

### Task 29: E2E add, validation and duplicate flows
- Source: F1.7 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 27, 23a, 28 | Security: per task | Branch: `test/task-29-e2e-add-and-duplicate`
- Description: Test the add stories end to end, the stubbed validation and capacity paths and the duplicate flow; add the fourth smoke test.
- Production: none (test support: `e2e/support/pages.ts`). Tests: `us-011-add-book.spec.ts`, `us-011-duplicate.spec.ts`.
- Acceptance criteria:
  1. Defaults and focus, keyboard add, client checks without a request, double click and double Enter send one request (held response), confirmation and focus (US-011.1 to .5).
  2. A forbidden character shows the server's message beside the field; stubbed status entry, `INVALID_STATUS`, `LIBRARY_FULL` and 500 keep the values (US-011.6, .9, .10).
  3. Duplicate dialog text, focus on Cancel, Escape, Cancel keeps values, Add anyway adds a second book (smoke test 4, US-011.7, .8).
  4. 500 code points, decomposed "é" and a leading U+00A0 are accepted and shown trimmed (T-07, T-08, T-40 via the page).
- Traces: US-011, T-03, T-07, T-08, T-40, M-1.

## 4. Increment 2: progress, rating, durability (Tasks 30 to 40)

Gate rows (P0): S-01 to S-07, S-08 and S-09 (20 iterations), S-10, S-11, T-15, T-17 to T-19, T-31 numeric rows, T-32, T-36 to T-38, C-03, C-04, C-12, C-13 (5 iterations), US-009.1 to .3, .5 to .7, US-012.1 to .7, .9.

### Task 30: Book transitions and value types
- Source: B19 | Layer: backend | Tier: P0 | Estimate: 35 | Depends on: 21 | Security: per task | Branch: `feature/task-30-rating`
- Description: Add the transitions that keep INV-1 to INV-4 and the rating literal judged from token and text.
- Production: (1) `domain/Book.java` (edit); (2) `dto/RatingValue.java` and `RatingRequest.java`; (3) `dto/StatusRequest.java`. Tests: `BookTest`, `RatingValueDeserializerTest`.
- Acceptance criteria:
  1. Entering completed sets `date_completed` from the clock; leaving clears rating and `date_completed` together; a same-state change changes nothing (BR-004, BR-008, US-006.3 to .5).
  2. A rating on a non-completed book is refused by the domain; the schema CHECKs repeat the rule (BR-007, INV-1).
  3. 1 to 5 valid; 0, 6, -0, 4.9, 5.0, 1e0, 1E1, `null`, `{}`, 100-digit integer give `INVALID_RATING`; strings, `true`, arrays, objects, `01`, `+5`, `NaN`, `5.`, `.5`, `5e`, 101 digits give `MALFORMED_REQUEST`; `1e999999999` gives `INVALID_RATING` within 300 ms (T-17, T-18, T-31, T-32, T-37).
- Traces: BR-002, BR-004, BR-007, BR-008, INV-1 to INV-4.

### Task 31a: Get, delete, status change
- Source: B20 (first part) | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 23b, 30, 15, 16 | Security: per task | Branch: `feature/task-31a-get-delete-status`
- Description: Implement `GET /api/v1/books/{id}`, `PUT .../status` and `DELETE /api/v1/books/{id}` in the order of BR-012 steps 3 to 7, extend the route table with these three routes, and test the stories and the capacity clauses on the real port. Split from Task 31 on the Liaison and Security findings of Task 30 (handoffs 134 and 135, version 0.6.2).
- Production: (1) `service/BookService.java` (edit); (2) `controller/BookController.java` (edit); (3) `config/transport/RouteTable.java` (edit). Tests: `BookControllerTest`, `BookStatusServiceTest` (fixed clock), `BookStoryIntegrationTest`.
- Acceptance criteria:
  1. 5.3, 5.4 and 5.7 answer as written; a `201` is followed by a `GET` of the same book; delete returns the removed book and a second delete is `BOOK_NOT_FOUND` (S-06); same-state status changes change nothing (S-04, S-11). `RouteTableTest` passes with the five routes (US-001.9, US-006, US-007, US-008).
  2. Field values are judged before existence: `PUT status` on an unknown well-formed id with `{"status":"Done"}` is `INVALID_STATUS` (T-38); `"Completed"`, `""`, `null`, `{}` give `INVALID_STATUS` and `5` or `true` `MALFORMED_REQUEST` (T-19); after any failed request the library content is identical (S-07).
  3. A service test with a fixed clock proves that entering completed sets `date_completed` to the clock value and that leaving completed clears the rating and `date_completed` in the same transaction as the status (BR-004, BR-008, DEC-023; the forced-failure proof is Task 32).
  4. At 5,000 books, list, status change and delete succeed; after one delete one add succeeds and the next gives `LIBRARY_FULL` (C-04, US-008.3). The rating step of C-03 is Task 31b.
  5. On the new endpoints, `DELETE` with `Content-Type` and no body passes the gate and a one-space body gives `400` (the rule is Task 16; E-18 is exercised here on real endpoints).
- Traces: 5.3, 5.4, 5.7, US-006 to US-008, BR-004, BR-012, S-04, S-06, S-07, S-11, T-14, T-15, T-19, T-38, C-04.

### Task 31b: Rating, clear rating and the rating error mapping
- Source: B20 (second part) | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 31a, 30 | Security: per task | Branch: `feature/task-31b-rating-endpoints`
- Description: Implement `PUT .../rating` and `DELETE .../rating`; map the invalid-rating marker of `RatingValue` to `400 INVALID_RATING` with a `details` entry for field `rating` ("must be an integer from 1 to 5", 5.5) and no echo of the received value; refuse a rating on a book that is not completed with `RATING_NOT_ALLOWED` before the domain method is called; extend the route table with the two rating routes (seven in all).
- Production: (1) `service/BookService.java` (edit); (2) `controller/BookController.java` (edit); (3) `config/transport/RouteTable.java` (edit); (4) `error/GlobalExceptionHandler.java` (edit) and, only if `ApiException` cannot carry the `details` entry, one typed exception in the same folder. Tests: `BookControllerTest` (edit), `BookRatingServiceTest`, `GlobalExceptionHandlerTest` (edit), `BookRatingIntegrationTest`.
- Acceptance criteria:
  1. The service judges the invalid-rating marker before the existence check and also for an unread or reading book: the marker gives `400 INVALID_RATING` with one `details` entry (`field` `rating`, fixed message, no value echo), also for an unknown well-formed id and for an unread book; a valid rating on an unknown id is `404 BOOK_NOT_FOUND` (T-36, T-37, US-003.3, US-003.8).
  2. The service checks the status first (BR-012 step 8): `unread` or `reading` gives `400 RATING_NOT_ALLOWED` without a call to `Book.assignRating`; the service does not catch `IllegalStateException`, so an unexpected domain refusal is `500 INTERNAL_ERROR`, never a mapped 400 (INV-1, BR-007).
  3. A set or clear of the rating is one transaction and leaves the book unchanged on any failure; a completed book accepts 1 to 5 and a replacement, and `DELETE .../rating` twice gives `200` with a null rating both times (S-01, S-02, S-03, S-05, BR-013).
  4. On the real port: `INVALID_RATING` (`7`, `null`, `5.0`, 100 digits), `RATING_NOT_ALLOWED` (valid value on an unread and on a reading book) and `BOOK_NOT_FOUND` are each answered with the stated code, and the ordering holds: field value before existence (invalid value on an unknown id is `INVALID_RATING`), existence before state (T-36, T-37, T-31 numeric rows).
  5. At 5,000 books a rating change succeeds (the rating step of C-03).
  6. Routing of the two new routes on the real port: `GET /api/v1/books/{id}/rating` gives `405 METHOD_NOT_ALLOWED` with the JSON content type and `Allow` listing exactly `DELETE, PUT`, never HEAD or OPTIONS (E-02 rating row, E-29; today it answers 404 because the rating routes do not exist); with `{bad}` each of the id in upper case, `not-a-uuid` and `1%27%20OR%20%271%27%3D%271`, `PUT` and `DELETE /api/v1/books/{bad}/rating` (valid JSON body on `PUT`) give `400 MALFORMED_REQUEST` (T-14 rating rows); `RouteTableTest` enumerates the controllers' mappings and passes with the seven routes of the API (SC-18).
- Traces: 5.5, 5.6, US-003, BR-002, BR-007, BR-012 steps 6 to 8, BR-013, S-01, S-02, S-03, S-05, T-14, T-31, T-36, T-37, E-02, E-29, C-03, SC-18.
- Notes (Liaison F-1 and F-2, handoff 138; Security review, handoff 139): (a) The new `BookService` method names (for example `assignRating` and `clearRating`) are checked early by the architect against the SpotBugs mutator-name heuristic (EI_EXPOSE_REP2); the 31a delete use case is named `discard` for that reason, and no exclusion may be added without Sponsor approval. (b) Task 31b keeps SEC-F-170 (`RATING_NOT_ALLOWED` for a non-completed book, the `RatingValue` marker to `INVALID_RATING`, rating and status handled in one transaction) and SEC-F-159 (strictness of the rating body proved in full-context tests, not only in slices). (c) Limits re-checked at 0.6.3: still 4 production entries (the route table is already entry 3) and the estimate of 40 minutes stays under the 45-minute limit; criterion 6 is routing assertions on the same files and adds no production entry. Criteria are now 6 (guide 3 to 5): accepted because criterion 6 is a table of rows on code already in scope; if the task overruns 45 minutes it is split, not extended.

### Task 32: Concurrency and fault injection
- Source: B21 | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 31a, 31b, 23a | Security: per task | Branch: `test/task-32-concurrency-faults`
- Description: Test concurrent adds and updates and a forced failure with a trigger on the test database.
- Production: none. Tests: `ConcurrencyIntegrationTest` (delivered as three test classes split by theme, handoffs 145 and 146), `support/FaultInjection`, and the shared helper `support/ApiCalls` (test support, no dependency; also used by Task 33).
- Acceptance criteria:
  1. C-06, C-07, C-09 and C-10 give the stated outcomes (one `201` and one `409`; exactly 10 and 10; 1 and 19; then 20).
  2. S-08 (20 iterations) and S-09 (20 iterations; the P0 tier of the gate, the 200 iterations are Task 51) end in a state allowed by INV-1 and INV-4; the allowed set of answers is asserted, never an interleaving.
  3. A failure forced after the first of the two changes of BR-004 gives `500 INTERNAL_ERROR` and the book is exactly as before (S-10, E-21).
  4. No production class contains a test-only route or switch (A-09).
- Traces: BR-013, C-06, C-07, C-09, C-10, S-08, S-09, S-10, E-21.

### Task 33: Durability across restart and kill
- Source: B22 | Layer: backend | Tier: P0 | Estimate: 40 | Depends on: 31a, 31b | Security: per task | Branch: `test/task-33-durability`
- Description: Start the packaged application as a separate process and prove acknowledged writes survive a normal stop and a forced kill.
- Production: none. Tests: `support/AppProcess`, `DurabilityIntegrationTest`, and the shared helper `support/ApiCalls` (created in Task 32, used here; test support, no dependency).
- Acceptance criteria:
  1. After an acknowledged add, status change, rating change and delete, a normal stop and start show the same data (US-009.1).
  2. After a forced kill right after each of the four operations returned `2xx`, 5 iterations each (the P0 tier of the gate; the 50 iterations of C-13 are Task 51), the restart shows the acknowledged change and opens normally (US-009.2, C-13, M-3).
  3. With the data directory pre-created with mode 0700 and the database file with mode 0600 (as the E2E launcher does), the modes of the directory and the file stay no wider than 0700 and 0600 after the start, after each of the four operations, after a normal stop, after a kill and after the restart, and the same holds for the companion files (`-journal` and any other), SC-13 as far as the application can widen them. The creation of the directory and the file with these modes by the application itself is unit-proved in Task 34a-1 and proved on the packaged jar in Task 34a-2, not here (the packaged jar uses the auto-configured data source until Task 34a-2 wires `SqliteDataSourceProvider`; handoff 145).
  4. The child process console contains no book data.
- Traces: US-009.1, .2, M-3, C-13, SC-13 (widening part only; creation part in Tasks 34a-1 and 34a-2).

### Task 34a-1: Complete directory guard and file identity (not wired)
- Source: B23 (first part, guard) | Layer: backend | Tier: P0 | Estimate: 30 | Depends on: 03b (the guard runs after `StartupPolicy` and is not the sole start path) | Security: per task | Branch: `feature/task-34a1-directory-guard`
- Description: Build `DataDirectoryGuard` complete, in one task: it creates a missing data directory with mode 0700 and a missing database file with mode 0600 before any connection, and it refuses an untrustworthy directory or file on POSIX (symbolic link, another owner, any group or other permission bit) with a fixed reason and the file untouched. Add the file-identity edit of `SqliteDataSourceProvider` (the identity remembered by the guard is compared with the identity after the pool opens). The guard is unit-proved and is **not wired** into the packaged application in this task: the jar still uses the auto-configured data source and is no weaker than today. The cut is the one recommended in handoff 146 (d) and agreed in handoff 147 (SEC-F-178). Task 34a-1 alone leaves the guard unwired, so there is no window in which the jar runs on a guard that only creates and does not refuse.
- Production: (1) `config/sqlite/DataDirectoryGuard.java`; (2) `config/sqlite/SqliteDataSourceProvider.java` (edit: file identity remembered and compared). Tests: `DataDirectoryGuardTest`, `SqliteFileIdentityTest` (two distinct files).
- Acceptance criteria:
  1. On an empty temporary location the guard creates the missing directory with mode 0700 and the missing database file with mode 0600 before any connection is opened, independent of the process umask (US-009.3, US-009.6, SC-13 creation part).
  2. A symbolic link as directory or file, another owner, or any group or other permission bit on the directory or file makes the guard refuse, with the file untouched (bytes and modes unchanged) (US-009.6, SC-13, SC-39).
  3. When the directory cannot be created or opened (a path that is not absolute after resolution, a symbolic link in the guarded path, the "unable to open database file" case of SEC-F-098 and SEC-F-140), the guard refuses with a fixed reason holding no path of a book or book data, and no file is created outside the guarded directory.
  4. Every refusal reason is a fixed string; no cell value or book data is in it (SC-11, SC-37). The console and log rule on the packaged application is Task 34a-2.
  5. The comparison of the remembered file identity with the identity after the pool opens is proved by a unit test with two distinct files. The race between check and open is not testable without a test-only switch (A-09); it is recorded as a documented limit and the Sponsor is asked to agree (handoff 027).
- Traces: US-009.3, US-009.6, SC-13 (creation and refusal at unit level), SC-39, DEC-028, SEC-F-098, SEC-F-140.
- Notes: (a) SEC-F-178 (MEDIUM, handoff 147) is not closed by this task: the guard is unwired, so SC-13 for the jar stays FAILED until Task 34a-2. (b) The brief says plainly that the companion-file clause of Task 33 was exercised vacuously (no `-journal` at the kill points; handoff 146 (d), SEC-F-181).

### Task 34a-2: Wire the packaged application to the guard and prove it on the jar
- Source: B23 (first part, wiring) | Layer: backend | Tier: P0 | Estimate: 30 | Depends on: 34a-1, 33 (for `support/AppProcess`; Task 33 itself does not depend on any Task 34 part, handoff 145) | Security: per task | Branch: `feature/task-34a2-wire-guard`
- Description: Make `SqliteDataSourceProvider` and `DataDirectoryGuard` the application's data source in place of Boot's auto-configured one (today the jar creates `library.db` with mode 0644, does not create the data directory, and a second instance on the same file starts; handoffs 145 to 147, SEC-F-178). Report every start refusal through a `StartupFailureAnalyzer` with a fixed reason and no book data, and prove all of it on the packaged jar, reusing `support/AppProcess` from Task 33.
- Production: (1) the data source configuration class that makes the provider and guard the application's data source (name fixed in the task brief from the plan); (2) `config/sqlite/StartupFailureAnalyzer.java`. Tests: `JarDataLocationIntegrationTest` (jar on an empty temporary location), `StartupRefusalIntegrationTest`; `support/AppProcess` is reused (edited only if needed).
- Acceptance criteria:
  1. The packaged application uses the provider and guard, not the auto-configured data source. Pragmas read back from the running jar: `foreign_keys` on, `trusted_schema` OFF, `busy_timeout` 5000, loading an extension is refused, the pool has size 1 (US-009.3, SC-32).
  2. Started as the jar on an empty temporary data location, it creates the directory with mode 0700 and the database file with mode 0600 before the first connection, and after an add every journal or companion file is no wider than 0600, under umask 022 and under umask 000 (US-009.6, SC-13).
  3. At jar level a symbolic link as directory or file, another owner (where the test run can provide a file of another owner; otherwise the limit is stated in the delivery and the unit proof of Task 34a-1 stands) and any group or other permission bit make the start fail non-zero with a fixed reason and the file untouched; an uncreatable directory fails cleanly with a fixed reason and no file is created outside the guarded directory (US-009.6, SC-13, SC-39, SEC-F-098, SEC-F-140).
  4. Every start refusal is reported through the analyzer with a fixed string; no cell value, path of a book or other book data appears on the console or in a log (SC-11, SC-37).
  5. A second instance on the same database file fails cleanly (non-zero exit, fixed reason) while the first keeps serving with its data unchanged (US-009.7, C-12, SEC-F-178); the validation and hot-journal proofs stay in Task 34b.
- Traces: US-009.3, US-009.6, US-009.7, SC-13 (jar), SC-32, SC-37, SC-39, DEC-028, DEC-032, SEC-F-098, SEC-F-140, SEC-F-178.
- Notes: **SEC-F-178 (MEDIUM) blocks any tag or release until Task 34a-2 is merged and Security has re-probed the packaged jar** (handoff 147, condition C-1, which named 34a-1 before the cut was moved; under this cut the condition is carried by 34a-2). Security's re-probe covers the modes under umask 022, a missing directory, a second instance, and the link and wide-mode refusals. Task 34a-1 alone leaves the guard unwired.

### Task 34b: Stored-library validation, hot journal and second instance
- Source: B23 (second part) | Layer: backend | Tier: P0 | Estimate: 45 (dense) | Depends on: 34a-2, 33 (Task 33 does not depend on 34a-1, 34a-2 or 34b) | Security: per task | Branch: `feature/task-34b-stored-library-validation`
- Description: Validate an existing database read-only before anything writes to it, in the order set by Security, and prove the hot-journal cases and the second-instance failure.
- Production: (1) `config/sqlite/StoredLibraryValidator.java`; (2) `config/sqlite/SqliteDataSourceProvider.java` (edit: header check, order of start). Tests: `StoredLibraryValidatorTest`, `StartupRefusalIntegrationTest` (edit), `SecondInstanceIntegrationTest`.
- Acceptance criteria:
  1. The validator reads `sqlite_master` first (exactly one table `books`, no other object except the key index, the stored DDL equals the plan's DDL or each named CHECK is present), then `quick_check`, then the rows with `LIMIT 5001`, a SQL `length()` and `typeof` pre-check before any text is fetched, and title and author lengths in code points in Java (SEC-F-068). A reason never contains a cell value.
  2. Each of these makes the start fail non-zero, serves no request, gives a fixed reason and leaves the bytes unchanged by hash: a non-database; a WAL-marked file (header bytes 18 and 19); a damaged file; other structure; no tables; a view, trigger or virtual table; a table `books` without its CHECKs; an extra index; a row violating a format, limit or INV-1 to INV-4; a NUL inside a title; one oversized cell; more than 5,000 rows (US-009.5 as changed by CR-9).
  3. Hot-journal cases (CR-9 as reworded, SEC-F-074): (i) a valid file plus the journal of a kill during an add that grew the file is recovered and accepted, the header size test being skipped when a non-empty `-journal` exists; (ii) an invalid main file plus a genuine hot journal: the start exits non-zero with a fixed reason that holds no book data, and the SHA-256 of the file after the rollback equals its SHA-256 after the refusal (no write after the refusal; CR-9 judges the file after the rollback); (iii) a planted garbage journal beside a valid and beside an invalid file is ignored and the bytes are unchanged.
  4. An absent or empty file is created; a valid database opens and no book is altered (US-009.3).
  5. A second instance exits non-zero with the same reporting while the first keeps serving with its data unchanged (US-009.7, C-12).
- Traces: US-009.3, .5, .7, DEC-028, DEC-032, SC-32, SC-39, C-12.

### Task 35: Client endpoints for status, rating, clear and delete
- Source: F2.1 | Layer: frontend | Tier: P0 | Estimate: 25 | Depends on: 11 | Security: per task | Branch: `feature/task-35-client-endpoints`
- Description: Add the client functions for get, set status, set rating, clear rating and delete.
- Production: (1) `frontend/js/api/booksApi.js` (edit). Tests: `booksApi.test.js` (edit).
- Acceptance criteria:
  1. Each function builds its path only with the validated id builder; `DELETE` carries `Content-Type: application/json` and no body (E-07, 5.6, 5.7).
  2. Responses map to the camelCase book; failures map to `api`, `network` and `unreadable` with field messages and the existing book where present.
  3. An id that is not a lower-case UUID is refused before any request (BR-011).
- Traces: 5.3 to 5.7, BR-011.

### Task 36: Entry view, status control and confirmation
- Source: F2.2 | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 35, 25, 31a | Security: per task | Branch: `feature/task-36-entry-status`
- Description: Build one entry from the template with a status control and the confirmation dialog, using the server's returned values. Tested by Task 39+40.
- Production: (1) `frontend/index.html` and `css/styles.css` (edit); (2) `frontend/js/ui/entryView.js`; (3) `frontend/js/ui/entryActions.js`. Tests: none.
- Acceptance criteria:
  1. A single-choice control named "Status of <title>" with Unread, Reading, Completed; choosing another value sends `PUT .../status`; the entry shows the returned values and its controls are disabled while the request runs (US-012.1, .6).
  2. With a rating shown, choosing unread or reading opens "Confirm status change" with the exact text, Continue and Cancel, focus on Cancel; Cancel restores the choice without a request (US-012.3).
  3. The rating control exists only on completed entries (US-012.2).
  4. With a filter active an entry that no longer matches stays and shows "No longer matches this filter" (US-012.8); MCP exploration evidence for each criterion is recorded.
- Traces: US-012.1 to .3, .6, .8.

### Task 37: Rating group and clear rating
- Source: F2.3 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 36, 31b | Security: per task | Branch: `feature/task-37-rating-group`
- Description: Add the labelled rating group, the clear button and the failure paths. Tested by Task 39+40.
- Production: (1) `frontend/js/ui/entryView.js` and `entryActions.js` (edit); (2) `frontend/js/logic/failures.js` (edit); (3) `frontend/css/styles.css` (edit). Tests: `failures.test.js` (edit).
- Acceptance criteria:
  1. A group "Rating of <title>" offers 1 to 5 by keyboard and shows the current value as text; "Clear rating of <title>" exists only while a rating exists; selecting sends `PUT`, clearing `DELETE` (US-012.4).
  2. A failed request returns the control to the server's last known value and shows the 5.9 text beside the entry; `INVALID_RATING` shows "Choose a rating from 1 to 5." (US-012.7).
  3. `RATING_NOT_ALLOWED` shows "Only completed books can be rated." and reloads the entry; `BOOK_NOT_FOUND` removes it, reloads the list and announces "This book no longer exists." (US-012.7, US-010.10).
  4. After a request that removes nothing, focus returns to the control used (a plan assumption, no story names it; not asserted by tests).
- Traces: US-012.4, .7, US-013.4, 5.9.

### Task 38: Delete with confirmation
- Source: F2.4 | Layer: frontend | Tier: P0 | Estimate: 35 | Depends on: 36, 31a | Security: per task | Branch: `feature/task-38-delete-entry`
- Description: Add the delete button and confirmation dialog with the focus rules. Tested by Task 39+40.
- Production: (1) `frontend/js/ui/entryActions.js` (edit); (2) `frontend/js/ui/dialogs.js` (edit); (3) `frontend/index.html` (edit). Tests: none.
- Acceptance criteria:
  1. "Delete <title>" opens "Delete book" with "Delete <title>? This cannot be undone.", Delete and Cancel; initial focus on Cancel; Escape and Cancel send nothing and focus the Delete button (US-012.5).
  2. On success the entry disappears, the count follows US-010.4, "Book deleted: <title>" is announced and focus moves to the "Your books" heading; the last delete shows the empty message (US-010.6).
  3. A failed delete shows its 5.9 text beside the entry and focus returns to the Delete button; focus after a `404` is a plan assumption (the heading), not asserted by tests.
  4. No control edits a title or author (US-012.9); MCP exploration evidence is recorded.
- Traces: US-012.5, .9, US-010.4, .6.

### Task 39+40: E2E status, rating and delete
- Merged 2026-11-05 (Sponsor, option B of section 9) from Task 39 (Source: F2.5) and Task 40 (Source: F2.6) | Layer: frontend | Tier: P0 | Security: increment | Estimate: 70 (40 + 30, long task; the 45-minute limit is waived; if it overruns it is split back into 39 and 40) | Depends on: 37, 38, 31a, 31b, 14 | Branch: `test/task-39-40-e2e-status-rating-delete`
- Description: Test status changes, clearing of rating and date, rating set, replace and clear, the stale-state failures, delete and the stale 404; add smoke tests 5 and 6. Build order: criteria 1 to 4 (status and rating) first, then 5 to 8 (delete).
- Production: none. Tests: `us-012-status-rating.spec.ts`, `us-012-delete.spec.ts`.
- Acceptance criteria (1 to 4 from Task 39, 5 to 8 from Task 40):
  1. Change to completed, rate, replace, clear (smoke test 5); the rating control only on completed entries (US-012.1, .2, .4).
  2. A rated book moved back shows the confirmation; Continue clears rating and date as returned; Cancel restores without a request (US-012.3, S-02, S-03).
  3. A same-state choice changes nothing (S-04, S-11); the "No longer matches this filter" text shows with a filter (US-012.8).
  4. After the book is changed or deleted through the API behind the page, acting shows "Only completed books can be rated." with the entry reloaded, or "This book no longer exists." (US-012.7, S-01, S-05).
  5. Delete with confirmation removes the entry, updates the count, announces "Book deleted: <title>" and focuses the "Your books" heading (smoke test 6, US-012.5).
  6. Cancel and Escape send no request and focus the Delete button.
  7. Deleting a book already deleted through the API shows "This book no longer exists.", removes the entry and reloads the list (US-012.7, S-06).
  8. Deleting the last book shows the empty message (US-010.6).
- Traces: US-012, US-010.6, S-01 to S-06, S-11.

### Task 54: Focus after RATING_NOT_ALLOWED
- Source: ledger Liaison F-37-1 (`active-conditions.md`); Sponsor approval of 2026-11-13 | Layer: frontend | Tier: P1 | Security: per task (production code in `frontend/js/ui` changes) | Estimate: 25 (small) | Depends on: 37, 39+40 | Branch: `feature/task-54-focus-after-rating-not-allowed`
- Description: After a rating request fails with `RATING_NOT_ALLOWED` the entry reloads as Reading or Unread and the rating group is hidden, so the focus given to a rating radio falls to the page body. Focus falls back to the entry's status control ("Status of <title>") when the control used no longer exists.
- Production: (1) `frontend/js/ui/entryActions.js` (edit); (2) `frontend/js/logic/entryOps.js` (edit: the pure focus decision). Tests: the entry operations unit test (one case for the pure decision); `e2e/tests/us-012-status-rating.spec.ts` (edit: one assertion).
- Acceptance criteria:
  1. After `RATING_NOT_ALLOWED` focus lands on the status select "Status of <title>" (US-012.7; one E2E assertion in `us-012-status-rating.spec.ts`).
  2. When the control used still exists, focus does not change (one unit test of the pure decision).
  3. The earlier tests are unchanged and green.
- Traces: US-012.7, plan assumption P-F2 (AP-8). PM debt note: the focus rule is a plan assumption with no spec or plan text; the PM requests of itself a spec line and a plan line for it (no spec text is invented here; to be raised through change control).

## 5. Increment 3: interface quality and release gate (Tasks 41 to 48)

Gate rows (P0): C-05 size bound, C-08, C-11 (c) assertions, US-013.1 to .6, US-014.1, .2, .4 (without 320), .6 (inert, no violation, stylesheet containment), .7, .8, SC-38, the sweep of Task 41+42, and every earlier P0 row. After this increment, Task 49+50 runs, then Task 48 (documentation), then the release criteria of `delivery-strategy.md` section 6 are checked and the release tag is set.

### Task 41+42: Capacity and performance at maximum size, and backend release sweep
- Merged 2026-11-05 (Sponsor, option B of section 9) from Task 41 (Source: B24) and Task 42 (Source: part of B27, moved here by the PM) | Layer: backend | Tier: P0 | Security: per task | Estimate: 75 (40 + 35, long task; the 45-minute limit is waived, the files rule is not; if it overruns it is split back into 41 and 42) | Depends on: 31a, 31b, 23b, 34b, 10 | Branch: `chore/task-41-42-capacity-and-release-sweep`
- Description: Prove the response bound, the add loop and the mixed load at maximum size, with a fixed heap, and tune the limits if needed (tuning of SC-06 values is ASK FIRST and returns to Security, SEC-F-078); then, last, run the repository-wide checks needed for the pre-release Security review, for both package managers, and fix or report each finding. Build order: criteria 1 to 4 (capacity), then 5 to 9 (sweep), so that the sweep sees the final tree.
- Production: (1) `application.yml` (edit only if tuning); (2) `README.md` (edit: the confirmed memory bound in both run commands); (3) `config/spotbugs/exclude.xml` (edit); (4) `config/dependency-tree.txt` (edit if approved). Tests: `CapacityPerformanceIntegrationTest`; sweep runs are recorded in `progress.md`.
- Acceptance criteria (1 to 4 from Task 41, 5 to 9 from Task 42):
  1. With 5,000 books of maximum size the list is `200`, `total` 5000, body below 16,000,000 bytes; the time is recorded without a gate (C-05).
  2. A loop of 20 add and delete pairs at 4,999 books gives only `201` and `200` and never more than 5,000 books (C-08).
  3. With 4,949 books, 50 concurrent adds with 50 concurrent lists give all `201` and `200`, 4,999 books after, no `500`, no refused connection, a failed request changes nothing and the process serves afterwards (C-11 c).
  4. The C-11 (c) test JVM runs with a fixed `-Xmx` (512 MB proposed, confirmed by this measurement and written into the README run command); eight slow list readers cause no `OutOfMemoryError` and the process serves afterwards; peak heap is recorded; the README gives the jar command with `-Xmx` and the `spring-boot:run` form with `-Dspring-boot.run.jvmArguments=-Xmx...`, both with the confirmed value (SEC-F-081). If the mix cannot pass, the task stops and the Sponsor decides (no weaker test) (SEC-F-071).
  5. SpotBugs and Find Security Bugs report nothing unexplained; every exclusion keeps its justification.
  6. Grep gates are clean in `backend/src/main`: `select *`, outbound HTTP clients, `System.out`, `printStackTrace`, wall-clock calls, `CrossOrigin`, `getSession`, `sendRedirect`, websocket classes, and native queries or JDBC statements outside `config.sqlite` (plan AP-12.3) (SC-07, SC-14, SC-18, SC-27, SC-41).
  7. `dependency:tree` equals the committed baseline or the difference is approved; coverage is at least 80%.
  8. In `e2e/`, `npm audit --audit-level=high` is clean and every lockfile `resolved` URL is on the registry (SC-20, SC-36, handoff 026 section 5).
  9. The README states that the database cannot be opened by another tool while the application runs (check only; written in Task 14) (N-9).
- Traces: C-05, C-08, C-11, SC-06, SC-07, SC-14, SC-17, SC-18, SC-19, SC-20, SC-27, SC-36, SC-41, M-2, X-03.

### Task 55: Remove the inert spring.web.resources.static-locations property
- Source: Sponsor decision of 2026-11-13 on SEC-F-126 | Layer: backend | Tier: P1 | Security: per task (protected surface, reviewed alone) | Estimate: 25 (small) | Depends on: 41+42 | Branch: `chore/task-55-remove-static-locations`
- Description: With `add-mappings` false no Spring resource handler is registered and the static catalog uses a literal classpath location, so the property is unused and is removed. The constitution mandatory-properties list is amended to 1.8 by the orchestrator in the same PR (cite it).
- Production: (1) `backend/src/main/resources/application.yml` (edit: remove the key). Tests (edit): `StartupPolicyIntegrationTest`, `MandatoryPropertiesIntegrationTest`, `StaticContentIntegrationTest`. The last overrides the property to prove it is inert: keep a proof that the frontend is still served from the literal catalog location and that an attempt to set the property does not change what is served, if still meaningful; otherwise remove that case and justify it in the handoff.
- Acceptance criteria:
  1. The key is absent from `application.yml`.
  2. The static catalog and the `404`/`405` behaviour for non-catalog paths are unchanged (existing static-content tests green).
  3. `cd backend && ./mvnw verify` is green.
- Traces: SEC-F-126, SC-12, constitution 1.8.

### Task 43: Accessibility check (axe) and its pin
- Source: F3.1 | Layer: frontend | Tier: P0 | Estimate: 45 | Depends on: 39+40 | Security: per task | Branch: `chore/task-43-accessibility-check`
- Description: Verify and pin the accessibility checker, then test the WCAG 2.1 A and AA rules in the data state of US-014.1. The checker runs in test code only; the application's content-security policy is never relaxed (Sponsor decision of 2026-11-02).
- Production: (1) `e2e/package.json` and `package-lock.json`. Tests: `us-014-accessibility.spec.ts`.
- Acceptance criteria:
  1. Publisher (Deque), the registry entries of the package and `axe-core`, absence of install scripts, a peer range that accepts the pinned Playwright, lockfile `resolved` hosts and a clean `npm audit --audit-level=high` are recorded; the exact pin is proposed for the constitution table and the alignment of `frontend-testing.md` is done in Task 48 (SC-21).
  2. The scan reports zero violations of impact serious or critical with at least three books (one completed, rated), two field errors on the add form and each dialog open (US-014.1); every control has a programmatic label (US-014.2).
  3. The scan first runs through `page.evaluate` without any bypass. If a bypass proves necessary, `bypassCSP: true` is set only on a dedicated context created inside `us-014-accessibility.spec.ts`, never in `playwright.config.ts`, never for a test that uses the `guard` fixture for CSP assertions, the server's headers are never changed, and cross-origin requests stay blocked; the need is reported to Security and the Sponsor before the bypass is merged (SEC-F-076).
  4. The Task 45 CSP and Trusted Types tests run in a context without bypass.
- Traces: US-014.1, .2, DEC-026, SC-10, SC-21, SC-38.

### Task 44: Error matrix
- Source: F3.2 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 39+40, 26, 28 | Security: increment | Branch: `test/task-44-error-matrix`
- Description: Test every error code and the non-envelope cases with stubbed responses, the timeout and the live-region update count.
- Production: none. Tests: `us-013-errors.spec.ts`.
- Acceptance criteria:
  1. Each Appendix A code shows the action and text of 5.9; not JSON, missing code and unknown code show the `INTERNAL_ERROR` text; no response shows the US-013.2 text and keeps input (US-013.1 to .3); a `VALIDATION_ERROR` with unusable `details` shows the `MALFORMED_REQUEST` text (CR-7).
  2. No raw `error` text, status, stack, URL or id is displayed except the book's own data (US-013.5).
  3. A held request abandoned at 15 s by the browser clock re-enables its controls and shows the US-013.2 text (US-013.6 as changed by CR-4).
  4. Observed with a mutation observer from the arrival of the failure's outcome to the end of its handling, exactly one replacement of the live-region text occurs per failure; the clearing at the start of a new action is outside that window (US-013.4 as defined by CR-5).
- Traces: US-013, 5.9, E-21 page text.

### Task 45: Safe rendering
- Source: F3.3 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 39+40, 27, 38 | Security: increment | Branch: `test/task-45-safe-rendering`
- Description: Test that hostile and worst-case texts are inert everywhere, including every dialog, and that the page reads and stores nothing it should not.
- Production: none. Tests: `us-014-safe-rendering.spec.ts`, `frontend/js/sources.test.js` (edit).
- Acceptance criteria:
  1. The hostile texts, a 500-code-point word and a title of base characters each followed by 8 combining marks are inert in the list, each dialog and each error path, with no content-security violation (US-014.6, SC-09).
  2. A string assignment to `innerHTML` throws under Trusted Types, in a context without CSP bypass (SC-38).
  3. The forbidden-sink grep over the served sources passes (SC-09).
  4. Nothing is read from address, `window.name`, referrer or messages; nothing is stored in web storage, cookies or IndexedDB; no other origin is requested (US-014.7, .8).
- Traces: US-014.6 to .8, SC-09, SC-38.

### Task 46: Styles polish
- Source: F3.4 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 45 | Security: increment | Branch: `feature/task-46-styles-polish`
- Description: Finish the stylesheet: colour tokens, visible focus ring, containment rules and reduced motion, with a test that reads the stylesheet.
- Production: (1) `frontend/css/styles.css`. Tests: `frontend/css/tokens.test.js`.
- Acceptance criteria:
  1. Token pairs reach 4.5:1 for text and 3:1 for control boundaries and the 3 px focus ring by the WCAG formula (US-014.2, .3).
  2. The stylesheet wraps or clips overflowing text in the list item and in each dialog, and the test fails if the rules are removed (US-014.6 containment; never cut).
  3. No animation or transition is defined, and a reduced-motion block removes any motion (US-014.5).
  4. Status and errors always carry text as well as colour (US-014.3).
- Traces: US-014.2, .3, .5, .6.

### Task 47: Keyboard and focus rules
- Source: F3.6 | Layer: frontend | Tier: P0 | Estimate: 40 | Depends on: 39+40, 27, 38 | Security: increment | Branch: `test/task-47-keyboard-focus`
- Description: Test keyboard-only operation, the focus return table and the testable reading of focus trapping for every dialog.
- Production: none. Tests: edits to `us-011-add-book.spec.ts`, `us-012-status-rating.spec.ts`, `us-014-accessibility.spec.ts`.
- Acceptance criteria:
  1. Every operation is completed with the keyboard alone and the add sequence of M-1 works (US-014.2, M-1).
  2. Focus lands where the stories say: Title after Cancel and after Add anyway success or failure (a `VALIDATION_ERROR` follows US-011.6), the "Your books" heading after delete, the opener after the confirmation dialogs (US-011.5, .8, US-012.5, CR-1).
  3. With each dialog open, repeated Tab and Shift+Tab never put focus on a page element outside it (US-014.2 as defined by CR-6).
  4. The focused control has a computed outline that is neither none nor zero width (US-014.2).
- Traces: US-011.5, .8, US-012.5, US-014.2, M-1.

### Task 48: Release documentation (built after Task 49+50)
- Source: F4.2 | Layer: frontend | Tier: P0 | Estimate: 30 | Depends on: 47, 41+42, 43, 49+50 | Security: per task | Branch: `docs/task-48-release-docs`
- Description: Align the testing documents with the delivered code and add the story-to-test table so that the documents match the final code before the release tag.
- Production: (1) `docs/sdd/frontend-testing.md` (layout and the accessibility row, aligned with the constitution); (2) `README.md` (frontend notes); (3) `docs/sdd/progress.md`. Tests: none.
- Acceptance criteria:
  1. The layout and accessibility row of `frontend-testing.md` match the delivered tree and the constitution.
  2. A table maps every P0 criterion of US-010 to US-014 to a test title; Task 49+50 is shown as done with its results; Tasks 51 to 53 and the other cut-list items are shown as open.
  3. The README frontend commands are executed on a clean clone.
  4. The Liaison confirms that constitution, spec, plan, tasks and the status board describe the final code; the orchestrator updates the board.
- Traces: M-5, delivery-strategy section 6.

## 6. Release gate tasks and Increment 4

Task 49+50 (merged from Tasks 49 and 50 in 0.6) is made of P1 rows that run before the release tag (Sponsor decision of 2026-11-02; Security requires them, handoff 026 section 5). Tasks 51 to 53 follow the tag, time permitting; P0 rows that touch the never-cut list are already in Increments 1 to 3.

### Task 49+50: Raw-socket run, grammar corners, hidden-channel repeat and log hygiene - before the tag
- Merged 2026-11-05 (Sponsor, option B of section 9) from Task 49 (Source: B25 first part) and Task 50 (Source: B25 second part + B26) | Layer: backend | Tier: P1 | Security: per task | Estimate: 90 (45 + 45, long task; the 45-minute limit is waived; if it overruns it is split back into 49 and 50) | Depends on: 16, 17, 18+19 | Branch: `test/task-49-50-raw-socket-and-grammar`
- Description: Run the container-level cases once on raw sockets and record the result (criteria 1 to 4); test the remaining header grammar rows, repeat the hidden-channel text rules on the real port and test log neutralization (criteria 5 to 7).
- Production: none. Tests: `PreApplicationIntegrationTest`, `GrammarCornersIntegrationTest`, `TextAndParserIntegrationTest`, `LogHygieneIntegrationTest`.
- Acceptance criteria (1 to 4 from Task 49, 5 to 7 from Task 50):
  1. No Host (HTTP/1.0 and 1.1), two Host headers, an invalid request line, version `9.9`, duplicate or negative `Content-Length`, both `Content-Length` and `Transfer-Encoding`, lines or headers over 8 KiB: status in {400, 414, 431, 505}, no product, version or echo, the next request is served (E-10 last two, E-20, E-36 first two, E-37).
  2. Both `TRACE` requests answer `405` before the application; `FOO` and `get` answer `405`, 400 or 501 (E-28, DEC-033). The test does not assert the `Allow` list of a container answer (SEC-F-079).
  3. Container rows with an encoded `/` or `\`, `%00`, a literal `\` or a dot segment above the root accept the envelope or a pre-application answer without disclosure (E-15, E-24, E-24b, E-25).
  4. `Upgrade: websocket` and `Upgrade: h2c` give `200`, never `101` (E-31 upgrade rows).
  5. `Content-Type` rows 2 to 7 of E-34 and all rows of E-35 give the stated statuses; the application rows of E-24b and E-25 give the stated envelopes.
  6. T-23 to T-26 and T-24b give the stated results on the real port.
  7. Hostile ids, query values and titles leave one log line per event with no raw CR, LF, U+2028, U+2029, U+0085 or ESC and no title text (T-33, SC-37).
- Traces: E-10, E-15, E-20, E-24, E-24b, E-25, E-28, E-31, E-34, E-35, E-36, E-37, T-23 to T-26, T-24b, T-33, SC-18, SC-29, SC-37.

### Task 51: Long runs and timing figures - after the tag
- Source: B27 (long runs) | Layer: backend | Tier: P1 | Estimate: 40 | Depends on: 33, 32, 41+42 | Security: per task | Branch: `test/task-51-long-runs-and-figures`
- Description: Run the long loops once and record the measured figures of M-2.
- Production: none (notes in `progress.md`). Tests: edits to `DurabilityIntegrationTest`, `ConcurrencyIntegrationTest`, `CapacityPerformanceIntegrationTest`.
- Acceptance criteria:
  1. The kill loop of C-13 runs 50 iterations; S-08 and S-09 run 200 iterations; all pass.
  2. Single-book operation, list at 5,000 typical books, add at 4,999 maximum-size books and the C-05 list time are recorded once on the reference machine (DEC-020).
  3. A figure over its threshold is reported open; the P0 bounds already passed in Tasks 23b and 41.
- Traces: C-13, S-08, S-09, M-2, DEC-020.

### Task 52: Measurements and responsive layout - after the tag
- Source: F3.5 + F4.1 | Layer: frontend | Tier: P1 | Estimate: 45 | Depends on: 47 | Security: increment | Branch: `chore/task-52-measurements-responsive`
- Description: Add the cut-list measurements, the mobile project and the layout checks.
- Production: (1) `e2e/playwright.config.ts` (edit). Tests: `measurements.spec.ts`, `us-014-accessibility.spec.ts` (edit).
- Acceptance criteria:
  1. With 1,000 books all entries are present 1,000 ms after the `load` event (recorded once); with 5,000 books the time is recorded with no threshold (US-010.9).
  2. At 320 CSS pixels there is no horizontal scrolling; the worst-case texts of US-014.6 stay inside the bounding box of their list item or dialog and the buttons remain visible and operable (US-014.4, US-014.6).
  3. An item that is not written is reported open in the delivery, never skipped silently.
- Traces: US-010.9, US-014.4, US-014.6, M-2.

### Task 53: Final pass - after the tag
- Source: F4.3 | Layer: frontend | Tier: P1 | Estimate: 35 | Depends on: 52 | Security: per task | Branch: `docs/task-53-final-pass`
- Description: Explore the finished page at 320 and 360 pixels and close the traceability and cut-list report.
- Production: (1) `docs/sdd/progress.md`. Tests: none.
- Acceptance criteria:
  1. An exploration at 320 and 360 pixels is recorded with deviations and spec gaps.
  2. The story-to-test table is complete and every open item is listed with its tier.
  3. The cut-list report states which measurements are done and which are open.
- Traces: M-5, spec section 1.

## 7. Tier register (single authority)

Rule (`delivery-strategy.md` section 4): P0 stops a realistic local attack or protects a core rule and blocks an increment; P1 is verified once and recorded; P2 is documented. Items on the never-cut list of `spec.md` section 1 are P0. This register was confirmed by Security in handoff 026 section 5 (differences D1 to D4 and the moves M); `plan-appendix.md` AP-5 and AP-9.6 are regenerated from it.

### 7.1 Appendix D families

| Family | Rows | Tier | Difference from the architect's proposal | Tasks |
|---|---|---|---|---|
| Envelope and headers on every response class, 500 without detail | E-21, E-23 | P0 | none | 05, 06, 08, 32 |
| Host, Origin, Fetch-Metadata | E-04, E-10 (first five), E-11 to E-14 | P0 | none (E-04 is an Origin check at gate stage 3 and runs in Task 08; AP-5 said "after Task 15") | 06, 08 |
| Content type, body cap, early rejection | E-05, E-07, E-08, E-09, E-31 (`Expect`), E-32, E-33, E-34 row 1 | P0 | E-34 row 1 made explicit | 06, 08, 15, 16 |
| Slow request | E-22 (configuration and live run, including the drip variant) | P0 | none | 16 |
| Routing, methods, Accept, query, id | E-01 to E-03, E-06, E-17 to E-19, E-24 (rows 1 to 3), E-26, E-27, E-29, E-38; T-14, T-14b | P0 | T-14b added (AP-5 omitted it) | 15, 16 |
| E-36 third row (2,000 query pairs) | E-36 row 3 | P0 | **D1**: architect P1 | 16 |
| Static content | E-15 (application rows), E-16, E-30, E-39 | P0 | none | 09 |
| Strict parsing and value domains | T-04, T-05, T-15 to T-21, T-27 to T-30, T-32, T-34 to T-40 | P0 | none | 16, 20, 21, 30, 31a, 31b |
| Parser limits never 500, under 300 ms | T-31 | P0 | none | 21, 30, 31b |
| Text policy core and injection payloads | T-01 to T-03, T-06 to T-13, T-22, T-26 | P0 | none | 18+19, 23a |
| Hidden-channel text rules | T-23 to T-25, T-24b | P0 at unit level; P1 real-port repeat | none | 18+19, 49+50 |
| Capacity and duplicates | C-01, C-02, C-11 (a) and (b) bounds | P0 | none | 23a, 23b |
| Capacity with other operations | C-03, C-04 | P0 | moved from Task 23 to Tasks 31a and 31b (endpoints exist there) | 31a, 31b |
| Concurrency | C-06, C-07, C-09, C-10 | P0 | none | 32 |
| Size bound and load | C-05 size, C-08, C-11 (c) assertions, memory bound | P0 | none | 41+42 |
| Recorded timing figures | C-05 time, M-2 figures, C-11 durations | P1 | none | 51 |
| Second instance | C-12 | P0 | none | 34a-2, 34b |
| Durability | C-13 (5 iterations) | P0; 50 iterations P1 | none | 33, 51 |
| Invariant sequences | S-01 to S-07, S-10, S-11; S-08 with 20 iterations | P0 | none | 31a, 31b, 32 |
| Concurrent status, rating and delete | S-09 with 20 iterations | P0 | **D2**: architect P1 | 32 |
| Long runs | S-08 and S-09 at 200 iterations | P1 | none | 51 |
| Grammar corners | E-34 rows 2 to 7, E-35, E-24b, E-25 (application rows) | P1 | none | 49+50 |
| Pre-application answers | E-10 (last two), E-15 container rows, E-20, E-25 container rows, E-28, E-31 (Upgrade rows), E-36 (first two), E-37 | P1, but run before the release tag | none | 49+50 |
| Log injection | T-33 live run P1; log pattern at configuration level P0 | P1; P0 | none | 08, 49+50 |
| Start-up criteria without an Appendix D row | US-009.1 to .3, .5 to .7 | P0 | none | 03a, 33, 34a-1, 34a-2, 34b |
| Release sweep and package audits | SC-07, SC-14, SC-18, SC-20, SC-27, SC-36, SC-41 | P0 | **D3**: architect Increment 4 | 41+42 |
| Accepted limits | Section 6 of `spec.md` | P2 | none | documented |

### 7.2 UI acceptance criteria

| Criterion | Tier | Difference from the frontend proposal | Tasks |
|---|---|---|---|
| US-010.1, .3 to .8, .10 | P0 | none | 12, 25, 28 |
| US-010.2, the clause "no entry and no count until the load ends" (CR-2) | P0 (US-010.7 and Task 25 rely on it) | **F-24**: the proposal had all of US-010.2 as P1 | 25, 28 |
| US-010.2, the loading text itself | P1 | none | 12, 28 |
| US-010.9, 1,000 entries within 1,000 ms | P1 (cut list) | none | 52 |
| US-010.9, 5,000 entries recorded only | P2 | none | 52 |
| US-011.1 to .10 | P0 | the proposal wrote ".1 to .11"; there is no US-011.11 (**D4**, typing slip) | 12, 26, 27, 29 |
| US-012.1 to .7, .9 | P0 | none | 36 to 38, 39+40 |
| US-012.8 | P1 | none | 36, 39+40 |
| Tasks 54 (focus after `RATING_NOT_ALLOWED`) and 55 (remove inert static-locations property) | P1 | none | 54, 55 |
| US-013.1 to .6 | P0 | none | 24, 26, 44 |
| US-014.1 | P0 (the checker runs in tests only) | none | 43 |
| US-014.2 | P0 | none | 27, 38, 47 |
| US-014.3 | P1 (cut list); contrast rules inside the Task 43 scan are never cut; focus-ring ratio P0 under US-014.2 | none | 43, 46 |
| US-014.4 lang, title, `h1`, main | P0 | split | 12 |
| US-014.4 320-pixel clause | P1 (cut list) | split | 52 |
| US-014.5 | P1 (cut list) | none | 46 |
| US-014.6 inert text and no content-security violation | P0 | none | 14, 45 |
| US-014.6 stylesheet containment rules | P0 (never cut) | none | 46 |
| US-014.6 bounding-box measurement | P1 (cut list) | none | 52 |
| US-014.7, US-014.8, SC-38 | P0 | none | 12, 45 |
| Rows through the page: T-03 (one case), T-07, T-08, T-12, T-40, S-01 to S-03, S-05, S-06, C-01 (stub), E-16, E-21 (stub) | P0 | none | 14, 29, 39+40, 44 |
| Page display of T-06, T-10, T-22, T-24, S-04, S-11, E-38, E-39 | P1 as page checks; the API row keeps its tier | none | 29, 39+40, 52 |

### 7.3 Differences to confirm
D1 E-36 third row P0; D2 S-09 P0 at 20 iterations; D3 release sweep in Increment 3; D4 US-011.11 does not exist; M (moves made by the architect in 022 and agreed by the PM): E-22 live, E-30, E-33, T-23 to T-26 and T-24b at unit level, T-31, C-05 size bound, C-08, C-11, C-12, C-13 (5 iterations). All were confirmed by Security in handoff 026 section 5; the Liaison accepts them from the governance side (handoff 025, F-12).

## 8. Traceability

### 8.1 Task 1 proofs of the constitution

| Proof | Task |
|---|---|
| Failsafe and Surefire split, merged JaCoCo data, coverage check with a failing-threshold run | 01 |
| `-Xlint:all -Werror` | 01 |
| Maven wrapper checksum and `--strict-checksums` | 01 |
| Dependency tree against the ban list | 01 (re-checked in 41+42) |
| Checkstyle pins; SpotBugs and Find Security Bugs pins | 02 |
| Every mandatory property effective (Boot 4 and Jackson 3 key names) | 03b (set), 08 (proved); strictness switches in 21 |
| Real-port security class with Host, CSRF, header, body-cap and error-path cases | 06 and 08; container error wrapper in 17 |
| `node --test` on the pinned Node | 11 |
| Node and Playwright installs; MCP pin | 10 |
| E2E isolation (SC-33) | 13 |
| Integration checkpoints with logs | 08, 14 |
| `@axe-core/playwright` pin | 43 |

### 8.2 Stories, rules and endpoints to tasks

| FR | Story | Rules | Endpoint | Tasks |
|---|---|---|---|---|
| FR1 | US-001 | BR-001, BR-006, BR-008, BR-009 | `POST /api/v1/books` | 07, 18+19, 20, 21, 23a, 31a (US-001.9) |
| FR1 | US-002 | BR-010 | `GET /api/v1/books` | 07, 16, 23b |
| FR1 | US-004 | BR-003, BR-013 | `POST /api/v1/books` | 22, 23a, 32 |
| FR3 | US-003 | BR-002, BR-007, BR-004 | `PUT`, `DELETE .../rating` | 30, 31b |
| FR2 | US-006 | BR-001, BR-004, BR-008 | `PUT .../status` | 30, 31a |
| FR1 | US-007 | BR-011 | `GET /api/v1/books/{id}` | 31a |
| CRUD | US-008 | BR-011 | `DELETE /api/v1/books/{id}` | 31a |
| Metric M-3 | US-009 | BR-013 | all writes | 03a, 03b, 33, 34a-1, 34a-2, 34b |
| Scope: frontend | US-010 | n/a | static content | 09, 12, 25, 28, 52 (US-010.9) |
| FR1 | US-011 | n/a | `POST` | 12, 24, 26, 27, 29 |
| FR2, FR3 | US-012 | BR-004 | status, rating, delete | 35 to 38, 39+40 |
| Constitution | US-013 | n/a | all | 11, 24, 26, 37, 44 |
| Constitution | US-014 | n/a | static content | 12, 14, 43, 45, 46, 47, 52 |
| Cross-cutting | BR-012 | | all | 06, 15, 16, 20, 21, 31a, 31b (route table) |
| Cross-cutting | INV-1 to INV-8 | | all | 04, 23a, 30, 32, 41+42 |

FR4 has no downstream items by design (DEC-003). Plan sections: `plan.md` and `plan-appendix.md` (until the appendix is regenerated, the task numbers of this document govern).

## 9. Merge options (if the Sponsor prefers fewer, larger tasks)

Merging changes the number of reviews, not the hours; each merged task runs 60 to 90 minutes and waives the 45-minute limit. Only low-risk pairs were listed: 10 + 13, 11 + 12, 18 + 19, 39 + 40, 41 + 42, 49 + 50. The tasks split in round 2 (03, 23, 34) should not be merged back.

**Decision of 2026-11-05 (Sponsor, option B).** Merged: 18+19, 39+40, 41+42 and 49+50 (the done or in-progress pairs 10+13 and 11+12 were not open and are not merged). Total: 52 tasks (4 merges from 56), effort unchanged at 2,265 minutes. A merged task that overruns is split back.

**PM assessment of two further pairs (rule: merge only if the merged task keeps a clean acceptance list and the same layer; the files rule of section 0 is not waived).** Not merged:
- **15 + 16** (route table and query/id rules): same layer, but the merged task would list 6 production entries (`RequestTarget`, `RouteTable`, `AcceptPolicy`, `QueryPolicy`, `BoundedBodyRequest`, `RequestGateFilter`) against the limit of 4, would hold 9 acceptance criteria, and both tasks are at the 45-minute ceiling ("dense"); Task 16 also carries a live drip-request spike that may force a decision (SEC-F-073), which is exactly the case where a merged task would have to be split back. Both edit `RequestGateFilter`, so they stay sequential and are cheap to review in order.
- **30 + 31** (transitions; get, delete, status, rating; Task 31 itself was later split into 31a and 31b, version 0.6.2): same layer, but the merge would list 5 production entries (`Book`, the `dto` files, `BookService`, `BookController`, `RouteTable`) against the limit of 4, would hold 8 criteria, and Task 31 is already at the ceiling with the invariant sequences and capacity clauses on the real port; Task 30 is the unit-level proof that 31 relies on and is better accepted first.
If the Sponsor still wants either merge, the PM would first need an approved exception to the files rule.

## Changelog
- 0.6.8 (2026-11-13): Sponsor decisions of 2026-11-13. Task 54 added (frontend, P1, Security per task, depends on 37 and 39+40; ledger Liaison F-37-1; PM debt note to request a spec line and a plan line for the focus rule, P-F2). Task 55 added (backend, P1, Security per task, protected surface, depends on 41+42; SEC-F-126; constitution 1.8 amendment by the orchestrator in the same PR). Tier register row added; totals of section 1 not recomputed.
- 0.6.5 (2026-11-06): Task 34a split into 34a-1 and 34a-2 on the decision of the Sponsor's delegate (orchestrator), using the cut recommended by the Liaison (handoff 146 (d)) and agreed by Security (handoff 147, SEC-F-178), not the cut proposed in the 0.6.4 size note (removed). 34a-1: complete `DataDirectoryGuard` (creation 0700/0600 and every refusal with fixed reasons) and the file-identity edit of `SqliteDataSourceProvider`, unit-proved, not wired (2 production entries, 5 criteria, about 30 min). 34a-2: data source configuration class, `StartupFailureAnalyzer`, the fixed-reason console and log rule and the jar-level tests reusing `support/AppProcess` (2 entries, 5 criteria, about 30 min); depends on 34a-1 and 33. 34b depends on 34a-2 and 33. SEC-F-178 (MEDIUM) blocks any tag or release until 34a-2 is merged and Security has re-probed the jar. Totals: Increment 2 is 13 tasks and 515 min; release 51 tasks, 2,200 min, about 36.7 h; all 54 tasks, 2,320 min. Tasks 32 and 33 list `support/ApiCalls` and the three Task 32 test classes (handoff 146, F-7) and name the P0 tier of the iteration counts (spec DEC-037, awaiting Sponsor confirmation). Tier register and traceability rows updated.
- 0.6.3 (2026-11-06): Task 31b amended after the Liaison audit of Task 31a (handoff 138, F-1, F-2) and the Security review (handoff 139): new criterion 6 (E-02 rating row with `Allow: DELETE, PUT` per E-29, T-14 rating rows for `PUT` and `DELETE .../rating`, `RouteTableTest` with the seven routes); traces gain T-14, E-02, E-29, SC-18; a note carries the SpotBugs method-name check (`discard` precedent), SEC-F-170 and SEC-F-159, and the limits check (4 production entries, 40 minutes, unchanged). No estimate, totals or dependency change.
- 0.6.2 (2026-11-06): Task 31 amended and split after the Liaison audit of Task 30 (handoff 134, L30-1 to L30-4, finding (c)) and the Security review (handoff 135, SEC-F-170). Required scope added: the invalid-rating marker of `RatingValue` maps to `400 INVALID_RATING` with a `details` entry for field `rating` and no value echo (`error/GlobalExceptionHandler.java` edit, and a typed exception only if `ApiException` cannot carry the `details`); the service judges the marker before the existence check and also for an unread book (T-36, T-38); it pre-checks status per BR-012 step 8 and returns `RATING_NOT_ALLOWED` before calling `Book.assignRating`, without catching `IllegalStateException`; status and rating changes are single transactions; a service test with a fixed clock covers `date_completed`; the real port proves `INVALID_RATING`, `RATING_NOT_ALLOWED` and `BOOK_NOT_FOUND` ordering. The result (5 production entries, 8+ criteria, over 45 minutes) exceeds the limits, so Task 31 is split at the status/rating boundary: 31a (get, delete, status; 40 min) and 31b (rating, clear rating, error mapping; 40 min; depends on 31a and 30). Dependencies of 32, 33, 36 to 38, 39+40 and 41+42, the traceability matrix, the tier register and the totals are repointed (53 tasks, 2,300 minutes, 50 in the release, 36.3 h). Both stay `Security: per task`. Branch names aligned to those used: Task 20 `feature/task-20-validation`, 21 `feature/task-21-strict-body`, 22 `feature/task-22-duplicates`, 23a and 23b `feature/task-23-add-rules` (chunk branch), 30 `feature/task-30-rating`. Task 51 depends on 32 and 41+42 and is unchanged. The plan N-6 list (ceiling tasks) still names Task 31: the Orchestrator is asked to remove it.
- 0.6.1 (2026-11-05): Orchestrator, applying the Security ruling of handoff 087: Tasks 23a, 48 and 53 move from `Security: increment` to `Security: per task`; Security accepts increment-level cover only for Tasks 22 and 30 and only if they change no error code, message or status and touch no control; both stay per task here. No task is marked increment.
- 0.6 (2026-11-05): Sponsor decision (option B of section 9): Tasks 18+19, 39+40, 41+42 and 49+50 merged (long tasks of 90, 70, 75 and 90 minutes, branches combined, dependencies, tier register and traceability repointed, totals recomputed to 52 tasks, 2,265 minutes, 49 in the release); 15+16 and 30+31 assessed and not merged (files rule); a merged task that overruns is split back. Definition of Done gains the Sponsor-approved review-load rule (option A; constitution amendment in progress) and every open task carries "Security: per task" or "Security: increment" in its header.
- 0.5.1 (2026-11-02): Task 02 amended (handoffs 043 and 044): SC-27 outbound-HTTP-client ban in the Checkstyle regex bans with a test-only suppression and one seeded run (SEC-F-089); time rule tightened to `new java.util.Date()` and `.now (` (SEC-F-090).
- 0.5 (2026-11-02): Round 3, final edits for handoffs 031 and 032. Task 12 depends on 09 (R2-01); the UI Definition-of-Done rule is limited to criteria the real page can provoke, stub-only criteria are evidenced by logic and stub tests, and the missing backend dependencies of Tasks 25, 26, 27, 36, 37, 38 are added (R2-02); totals corrected to 660, 2,055, 2,145 and 1,240 minutes, headline 35.75 h and 20.7 h (R2-04); plan 0.2 cited (R2-05); stored-DDL round trip in Task 04 and the native-SQL grep gate in Task 42 (R2-06); Task 13 working directory, argument names and five hostile variables (R2-07, SEC-F-080); branch types of Tasks 08 and 41 and the `progress.md` exemption (R2-08); sharper oracles in Tasks 34b and 28 (R2-09); README memory bound in Tasks 14 and 41 (SEC-F-081).
- 0.4 (2026-11-02): Round 2 of Phase 3, resolving handoffs 025 and 026 (answers by finding number in handoff 027). Dependencies corrected (F-01); Task 15 route table scope and Task 31 maintenance (F-02); `status` value check moved to Task 16 (F-03); Task 23 split into 23a and 23b and its capacity clauses limited to add (F-04, F-06); exception mapping in Task 22 (F-05); Task 34 split into 34a and 34b and Task 03 split into 03a and 03b (F-06, SEC-F-067, SEC-F-069); UI-task Definition of Done rule (F-07); release scope = Increments 0 to 3 plus Tasks 49 and 50, Task 48 built last (F-08); Task 43 per the Sponsor decision (F-09); tier register as single authority (F-12); file lists, branch types and README ownership (F-20); 0a/0b acceptance and try-it commands (F-21); CR approval before Task 11 (F-23); US-010.2 tier split (F-24); Security conditions SEC-F-066 to SEC-F-079 attached to Tasks 03a, 03b, 06, 07, 08, 09, 11, 13, 14, 16, 34b, 41, 42, 43, 49.
- 0.3 (2026-11-02): Sponsor decisions applied: budget accepted, Increment 0 split into 0a and 0b with tags after Task 08 and Task 14, Tasks 49 and 50 before the release tag, accessibility checker in tests only.
- 0.2 (2026-10-30): Re-baselined on handoff 022 (27 backend tasks, files rule accepted) and 021; Increment 0 redrawn to 14 tasks with the page and the smoke; sweep split out of B27 into Increment 3.
- 0.1 (2026-10-30): First draft on handoff 020 (superseded).
