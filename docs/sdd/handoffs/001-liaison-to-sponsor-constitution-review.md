# Handoff 001: Constitution review (Phase 1, Mode A)
- From: liaison
- To: sponsor
- Phase: 1   Round: 1 of max 3
- Artifacts: `docs/sdd/constitution.md` (DRAFT v0.1 + Amendment 1), cross-checked against `docs/sdd/exercise-brief.md`, `docs/sdd/git-workflow.md`, `docs/sdd/frontend-testing.md`, `docs/sdd/orchestration.md`, `docs/sdd/agent-protocol.md`, `docs/sdd/README.md`, `docs/sdd/status.md`, `.claude/agents/sdd-java-architect.md`, `.claude/agents/sdd-frontend-developer.md`, `.githooks/commit-msg`, `.gitignore`; branch `docs/phase-1-constitution-review`
- Type: REVIEW_REPORT

Mode: A (Constitution review), with the Amendment 1 review and the Layer 1b repository-hygiene checks that apply to Phase 1.

## Summary

**Verdict: REJECTED for v0.1 as final (round 1). Path to approval is short: a v0.2 that closes the 6 BLOCKERs and the Sponsor decisions in section 4.**
**Amendment 1 (Playwright): APPROVED WITH CHANGES** (see section 3; two MAJOR items must be fixed before the pinning task).

The draft is a good start. It is already stronger than the exercise template (layering rules, error envelope rules, DTO/entity separation, no-AI-attribution rule, spec-derived tests). It fails the Module 3 completeness list on three of the five required components and leaves the persistence and error-handling layers underspecified in ways that would make two engineers produce different code.

| Module 3 §3 component | Status in v0.1 |
|---|---|
| 1. Tech stack and tooling with versions | PARTIAL: names present, no versions pinned (C-01) |
| 2. Project structure map | MISSING (C-02) |
| 3. Executable commands | MISSING, and the one implied command conflicts with the other documents (C-03) |
| 4. Testing strategy | PARTIAL: layers, coverage definition, flaky and skip policy missing (C-14, C-15, C-16) |
| 5. Code style (with a snippet) and git workflow | PARTIAL: no snippet, rules not mechanically verifiable, contradictions with `git-workflow.md` (C-17, C-18, C-26) |
| Three-tier boundaries | PRESENT, with one overlap and two gaps (C-09, C-14) |

Findings: **39 total = 6 BLOCKER, 18 MAJOR, 15 MINOR** (constitution C-01 to C-31 = 6/16/9; Amendment 1 A-01 to A-08 = 0/2/6).

Doctrine references: Module 3 §3 (components), §2 (tiers), §5 and Summary ("Be specific, not vague"; "if the AI keeps violating a rule, fix the wording"); Module 4 §3 (oracle problem) and §4 (test levels); Module 2 §4 (slopsquatting, destructive autonomy) and §3 (authentication theater); Module 5 (HITL owns security and compliance).

I did not verify external facts (current Spring Boot support calendar, library versions, compatibility of the community dialect with a given Hibernate version): I had no network lookup in this review. Where that matters, it is stated as "verify" and the Sponsor decision includes the check.

## Content

### 1. Findings: constitution

Severity: BLOCKER = cannot proceed (would produce inconsistent or wrong code); MAJOR = likely defect or rework; MINOR = clarity.

| ID | Sev | Location | Finding | Evidence / rule | Proposed resolution |
|---|---|---|---|---|---|
| C-01 | BLOCKER | Tech Stack; Approved dependencies | No version is pinned anywhere: "Java 21 (LTS)" has no patch level or distribution; "Spring Boot 3.x" is a moving target (and the 3.x line may be past its open-source support window as of this review date, and its Hibernate version determines which `hibernate-community-dialects` works: verify); Maven, `sqlite-jdbc`, `hibernate-community-dialects`, JaCoCo, Node, `@playwright/test`, Chromium and `@playwright/mcp` are all unversioned. | Module 3 §3.1 ("frameworks, versions, and dependencies"), Summary 2 ("Be specific, not vague"). Amendment 1 itself says "Versions are pinned (exact)" but defers to a task and `plan.md`, which are lower in the authority order and cannot define what the highest document leaves open. | Add a version table (section 5, diff D-1). Sponsor decides the Spring Boot line (D1). Rule: versions live in the constitution; the build file must match; changing one is ASK FIRST. |
| C-02 | BLOCKER | (absent) Project Structure | No directory map. Documents disagree: `git-workflow.md` and both agents assume `backend/`, `frontend/`, `e2e/`; the constitution says the frontend is "served as static resources by the Spring Boot application" with "single module" Maven; `orchestration.md` runs `./mvnw verify` with no directory. If `frontend/` is a sibling of `backend/`, Spring cannot serve it without a mapping; if it lives in `src/main/resources/static`, the parallel-work rule "never edit the same files / separate file areas" is weakened. Where `pom.xml` and `mvnw` live is undefined. | Module 3 §3.2 ("exact directories"). Module 2 §2 (context drift: agents invent layouts). `git-workflow.md` §2 "Parallel work". | Add Project Structure (diff D-2). Sponsor decides D2 (recommended: `backend/` Maven root; `frontend/` copied to `static/` by a Maven `<resources>` entry; no frontend build step). |
| C-03 | BLOCKER | Testing Requirements; (absent) Executable Commands | No commands section. The only command is "`mvn test` (including the coverage check)". Conflicts: both agents and `orchestration.md` use `./mvnw verify`; JaCoCo `check` binds to `verify` by default, so `mvn test` would not enforce the gate; `*IntegrationTest` matches Surefire's default `*Test` pattern so it runs in `test`, which makes the unit/integration split meaningless unless stated; `mvn` vs `./mvnw` (wrapper is mandated). Frontend task completion (Playwright green) is not in the Definition of Done at all. | Module 3 §3.3 ("exact CLI commands with flags"). `sdd-java-architect.md` §3 step 6; `orchestration.md` §3 Phase 4 step 4; `frontend-testing.md` §7. | Add Executable Commands and a single Definition of Done (diff D-3). Replace "mvn test" by "`./mvnw verify` from `backend/`". |
| C-04 | BLOCKER | (absent) Schema management; ASK FIRST "modifying the database schema" | Nothing says how the schema is created or evolved. `ddl-auto=create`/`create-drop` loses data on restart (violates the BRD success metric "zero data loss on app restart"); `update` silently alters the schema, which defeats the ASK FIRST gate because the change happens without a diff and is unreliable on SQLite; `validate` needs a schema source. No migration tool is approved (Flyway/Liquibase would be a new dependency). FR4/US-005 will need a schema change in Phase 5. "Schema" is also undefined as a gate: is the first schema approved when `plan.md` is approved? | Module 3 §2 Tier 2 (schema changes are the canonical Ask-First example). `sdd-java-architect.md` §4 ("Any schema change... plan must already approve it"). | Sponsor decision D3. Recommended: `ddl-auto=validate` + a versioned `schema.sql` executed at startup (`CREATE TABLE IF NOT EXISTS`), so every schema change is a reviewable diff; initial schema is approved with `plan.md`; later changes ASK FIRST; a migration tool decision is taken when the first `ALTER` is needed (Phase 5). Diff D-4. |
| C-05 | BLOCKER | Configuration and Secrets | The database path key is "set by configuration" but unnamed. `frontend-testing.md` §3 needs "the application's configuration key/environment variable defined in `plan.md`", so the E2E isolation design depends on a lower document that does not exist yet. Also "`application.properties`/`application.yml`" allows two formats; no default value, port key, profile policy, or logging config is stated. | Module 3 §3.1/3.3; Module 5 (constraint injection before Plan). | Name the keys and defaults in the constitution (diff D-4): `spring.datasource.url` (env `SPRING_DATASOURCE_URL`), default `jdbc:sqlite:./data/globerbooks.db`; `server.port` default 8080; one format (`application.yml`); mandatory properties listed. |
| C-06 | BLOCKER | Error Handling Standards | "For validation errors, `error` must identify the offending field(s)" gives no format. Two engineers will write different strings; the frontend cannot attach messages to fields without parsing text, which `sdd-frontend-developer.md` §4 forbids ("branch on `code`, never on the message text"). Field names must be the API names (`date_added`), not Java names, which Bean Validation reports by default. Separately the BRD says the 409 "returns existing book details", but the envelope says "All errors must return" three keys, so there is no place for that payload. | Exercise template: "400: Validation errors (return field-specific messages)". `sdd-spec-liaison` open points 2 and 5. | Sponsor decision D4: keep the envelope strictly 3-key with a deterministic string format, or allow one optional additive member `details`. Recommended: optional `details` (diff D-5). |
| C-07 | MAJOR | Error Handling Standards (status list) | The status list and "single global exception handler" omit every framework-generated failure: unknown route under `/api/v1/` (with static resources mapped to `/**`, Spring raises a resource-not-found error, not a controller 404), 405, 415, 406, malformed JSON, wrong type in path/query (for example a non-UUID id or a bad `status` filter), oversized body. Without generic codes these either leak the default Spring error body or get ad-hoc codes. The rule "`code`... defined by spec.md must be used exactly" does not cover them. | Constitution NEVER "expose stack traces... internal class names"; Module 2 §3. | Add a reserved generic code table and map all framework exceptions through the one handler (diff D-5). PM confirms any code the spec wants to rename. |
| C-08 | MAJOR | Security Standards: "Reject unknown or malformed JSON bodies with 400" | Not achievable by default: Spring Boot sets `FAIL_ON_UNKNOWN_PROPERTIES` to false, so unknown fields are silently ignored. The rule as written would pass review but not behave. | Module 3 Summary 2; rules must be mechanically verifiable. | State the property (`spring.jackson.deserialization.fail-on-unknown-properties: true`) and make it a contract test (diff D-4). |
| C-09 | MAJOR | NEVER "Never add features not in `spec.md`" vs ASK FIRST "adding or changing an API endpoint, request field or response field not defined in `spec.md`" | Same trigger in two tiers with opposite outcomes (hard stop vs ask the Sponsor). Also the correct path is neither: it is a `CHANGE_REQUEST` to the PM, spec first. An agent could read the ASK FIRST item as permission to ask and then build. | Module 3 §2 (tiers must be unambiguous); Module 3 §5 (reword rules that get violated). `orchestration.md` §5 "Contract gap". | Delete the ASK FIRST line; extend the NEVER line: "...Propose it as a CHANGE_REQUEST; the spec changes first." (diff D-7). |
| C-10 | MAJOR | NEVER "Never use `SELECT *` in hand-written queries" | Under JPA almost no query is hand-written: Hibernate generates explicit column lists, JPQL has no `*`. The rule is untestable as written ("explicit projections" is undefined) and lets `nativeQuery = true` with `SELECT *`, `schema.sql` data scripts, or `JdbcTemplate` pass. The exercise original says "production code"; the draft narrowed it without recording why. | Module 3 §2 Tier 3 and §3.3. | Reword: no native SQL or JPQL containing `*` as a select list in any file under `backend/src/main`; native queries are avoided unless the plan justifies them; verification = grep in the audit checklist (diff D-7). Record the deviation from the template in the Decision table (C-30). |
| C-11 | MAJOR | (absent) SQLite case-insensitive comparison | SQLite `LOWER()` and `COLLATE NOCASE` fold ASCII only. US-004/BR-003 need case-insensitive, trimmed title+author comparison; "Émile Zola" vs "émile zola" would not match in SQL. Hibernate's `lower()` on SQLite inherits this. Trimming, internal whitespace and Unicode normalization are unspecified. | `sdd-java-architect.md` §4 ("correct for SQLite"); Module 2 §4. | Constitution fixes the technique, spec fixes the rule: comparison keys are computed in Java (`trim`, Unicode normalization, `toLowerCase(Locale.ROOT)`), never with SQL functions or collations; any stored key column is a schema change (ASK FIRST, decided in `plan.md`). Exact normalization rule routed to PM. |
| C-12 | MAJOR | (absent) SQLite concurrency and connection settings | SQLite is single-writer; default `SQLITE_BUSY` behavior, no `busy_timeout`, Hikari default pool of 10, and `:memory:` (each pooled connection gets its own empty database) are classic failure sources. The constitution explicitly permits "in-memory instance" for tests. Foreign keys are off by default. Check-then-insert duplicate detection races under concurrent requests. | Module 4 §4 (levels must be reliable); Module 2 §2. | Pool size and busy timeout stated; test databases are temp files only (`@TempDir`), no `:memory:`; the race is documented as an accepted risk or solved in the spec (diff D-4). |
| C-13 | MAJOR | API Standards; Error Handling (timestamp) | (a) "ISO-8601" has no fixed precision: `Instant.toString()` emits 0, 3, 6 or 9 fractional digits depending on the value, so output is non-deterministic and test oracles flake. (b) With second precision, "newest first" has ties, so order is nondeterministic; spec must define a tie-break. (c) No `Clock` rule although "no dependence on wall-clock time" is required. (d) ID format ("UUID" in the BRD) is not in the constitution: version, canonical lowercase hyphenated string, storage as TEXT, who generates it. | Module 4 §3 (deterministic oracles). | Decision D5 on precision. Add: inject `java.time.Clock`; UUID v4 generated in the service layer, stored and serialized as 36-char lowercase string. Tie-break routed to PM (diff D-6). |
| C-14 | MAJOR | NEVER list (testing) | Module 3's own Tier 3 example is "Never remove a failing test without explicit approval". The backend has no equivalent (`@Disabled`, deleting, loosening an assertion, editing an expected value to turn green). Only Amendment 1 protects frontend tests, and only the Amendment: the NEVER tier does not. No flaky-test policy for the backend. No rule that oracles come from the spec. | Module 3 §2 Tier 3; Module 4 §3 (oracle problem; overfitting); `sdd-java-architect.md` §5 "Oracle discipline" (agent-level only). | Add to NEVER: never delete, skip, disable, or weaken a failing test, or edit an expected value to make it pass, without Sponsor approval; add to ALWAYS: expected values come from `spec.md`. Flaky = defect, no retries (diff D-8). |
| C-15 | MAJOR | Testing Requirements (layers) | Only "unit" and "integration" exist; the Java agent adds slice (`@WebMvcTest`, `@DataJpaTest`) and contract tests. Not stated: which layer verifies which thing; that `@DataJpaTest` replaces the datasource with an embedded database by default (H2 is not approved: a hidden dependency and a wrong database) so `@AutoConfigureTestDatabase(replace = NONE)` is mandatory; that "mock the repository" in unit tests and "real SQLite" in slices do not contradict. The BRD template says "Mock external dependencies (DB, APIs)": the draft deviates deliberately but does not say so. | Module 4 §4 (levels); `sdd-java-architect.md` §5 table. | Add a test-layer table (diff D-8) and record the deviation from the template. |
| C-16 | MAJOR | Testing Requirements (coverage) | "Minimum 80% line coverage" does not define: scope (bundle vs class), what is excluded (`Application` class, config, records), whether integration tests count (merged exec data), or branch coverage. Un-scoped coverage is gameable. Frontend: no rule; "unit tests for all business logic" is silent for client-side logic. Definition of Done does not include E2E. | Module 4 §3 ("coverage is necessary but not sufficient"); exercise Phase 4 audit Layer 2. | Define as in D-8 (Sponsor confirms D13). Frontend gate = every acceptance criterion maps to a green E2E test (`frontend-testing.md` §7), not a line percentage. |
| C-17 | MAJOR | Code Style; Architecture Principles | Several rules cannot be verified mechanically and so will be reviewed inconsistently: SRP, "where practical" (immutability), "descriptive", "Javadoc for all public" (tests? records? overrides?), max function/file length (physical lines? comments?), no wildcard/unused imports, "no dead code", NEVER hardcode secrets. No linter, formatter or compiler-warning policy exists, and none is approved (a Checkstyle plugin would be a build dependency: ASK FIRST). | Module 3 Summary 2; Module 5 (invariants, evidence over claims). | Sponsor decision D6. Recommended: compiler `-Xlint:all -Werror` (no new dependency) plus Checkstyle with a small committed rule set (naming, file/method length, imports, Javadoc, wildcard). Replace subjective wording with measurable proxies (diff D-9). |
| C-18 | MAJOR | Workflow Rules (SDD); ALWAYS list | Internal and cross-document contradictions: (1) ALWAYS "commit after each accepted task" vs "one squashed commit per task" vs `git-workflow.md` (atomic commits on the branch, squash on merge; ACCEPTED status deliberately not in the message). (2) NEVER "modify files outside the scope of the current task" vs ALWAYS "update `progress.md`" and the agents' rights to edit `status.md` rows, handoffs, `plan.md` sections. (3) "No direct commits to `main`" has an exception in `git-workflow.md` §2 (initial bootstrap) that the constitution lacks. (4) `git-workflow.md` §2 says work is merged "through a pull request" but §4 says a PR is opened "only when the Sponsor asks": the merge path without a PR is undefined (note for the Sponsor, who owns that file). (5) Forbidden operations in `git-workflow.md` §6 (history rewrite, `reset --hard`, `--force-with-lease`) are only partly in NEVER. (6) The branch rule `<type>/task-NN-<slug>` does not cover `spec/`, `docs/`, `chore/` branches without a task. | Module 3 §2; `git-workflow.md` §2, §3 note, §4, §6; `orchestration.md` §6. | Single wording in the constitution that points to `git-workflow.md` for detail and defines "task scope = files listed in the task + own `status.md` row + `progress.md` + own handoffs" (diff D-10). Sponsor fixes the PR/merge wording in `git-workflow.md` (item 4). |
| C-19 | MAJOR | Header; Amendments; Workflow | Governance of the constitution itself: no Owner, date, version or changelog header (`README.md` §3.3 requires one); no amendment procedure (who proposes, who approves, how versions bump, when an amendment is folded into the body); `git-workflow.md`, `frontend-testing.md`, `orchestration.md` are referenced as binding but sit outside the declared authority order, so conflicts have no rule. Amendment 1 duplicates content already in Tech Stack and approved dependencies. | Module 3 §5 and Summary 1 ("Treat the Constitution as Code: version-controlled... living"). | Header, "Referenced documents are incorporated by reference; on conflict the constitution wins", amendment procedure; fold Amendment 1 into the body when approved (diff D-1, D-11). |
| C-20 | MAJOR | Security Standards (web) | Missing: Content-Security-Policy and other response headers (`X-Content-Type-Options`, `Referrer-Policy`, `frame-ancestors`); CORS stance (the app is same-origin, so none is needed and `@CrossOrigin("*")` must be forbidden); maximum request body size; HTTP method semantics for state-changing calls; the frontend agent says "no inline event handlers if a CSP is planned", so the CSP decision gates the frontend design. Adding headers without Spring Security needs a small servlet filter (an architectural element: approve it here). Dependency integrity (slopsquatting, Module 2 §4): no rule on lockfiles, `npm ci`, or checking a package exists and is the official one before approval. | Module 2 §3, §4; Module 5 (humans own security). | Decision D8. Recommended: strict CSP (`default-src 'self'`, no inline script or style, no CDN or web fonts), `nosniff`, no CORS, body-size cap, lockfile + `npm ci` (diff D-12). |
| C-21 | MAJOR | Tech Stack / Code Style (frontend) | The constitution's Code Style is Java-only. Frontend standards live only in the agent file: WCAG 2.1 AA target, ES modules, JSDoc, native `<dialog>`, mobile-first, "current evergreen browsers". Browser support is undefined (Playwright verifies Chromium only, so Firefox and Safari support would be an unverified claim); UI language/locale (English strings) unstated; no CSS or JS style rules; client-side validation policy ("UX only, server is authoritative") is agent-level. | Module 3 §3.5; Module 2 §3 (client-side-only validation = authentication theater). | Promote to a Frontend Standards section (diff D-12). Sponsor decides D10 (accessibility target and browser support). |
| C-22 | MAJOR | Repository hygiene (`.gitignore`, hooks) | (a) `.gitignore` lists `*.db`, `*.sqlite`, `*.sqlite3` but not SQLite sidecar files (`*.db-wal`, `*.db-shm`, `*.db-journal`), which appear as soon as WAL mode or an interrupted transaction occurs; no `data/` (the default DB directory proposed above), `.env`, `*.log`, `logs/`. (b) `core.hooksPath` is a local git setting: it is not versioned and not enforced, so a fresh clone has no guard; I could not verify the hook's executable bit (no shell in this review). (c) The hook scans the commit message only, but the no-attribution rule also covers author identity, branch names, tags and PR text; its word list contains ordinary English/technical words that can block legitimate messages (for example "cursor", as in cursor-based pagination). (d) There is no CI, so "`main` is always green" and "coverage gate met" depend entirely on the orchestrator running the build; CI/CD is an ASK FIRST item. (e) No secret scanner; "Never hardcode secrets" is verified by eyes only. | Module 3 §2 (Never commit secrets: "most commonly helpful constraint"); `git-workflow.md` §1, §2; `orchestration.md` §6 checklists. | Proposed `.gitignore` additions (section 5, D-13). Hook activation becomes step 1 of Executable Commands plus an orchestrator pre-flight check (`git config core.hooksPath`, executable bit, identity). Sponsor decision D7 (CI) and D16 (hook scope). |
| C-23 | MINOR | Code Style: "minimum 3 characters... only exception is `id`" | Ambiguous for: loop counters (`i`), lambda parameters, `catch (Exception e)`, two-letter domain words (`db`, `ui`, `ok`), record components, test names, JS and TS code. Applies to variables only, or also parameters, fields, methods, classes? Not mechanically checkable without a linter (C-17). | Module 3 §5 (ambiguity = inconsistent code). | "Applies to local variables, parameters and fields in all Java and JS code; no exceptions except `id`; lambda parameters and `catch` parameters follow the same rule." If the Sponsor wants `i` for loops, say so explicitly (diff D-9). |
| C-24 | MINOR | API Standards | Not stated: pagination (the list returns everything plus `total`: an explicit "none in v1, unbounded by design for a single-user list" avoids drift); versioning policy (additive changes only within `/v1`, breaking change means `/v2`); content negotiation (`application/json; charset=UTF-8` only; 415/406); CORS (C-20); path-id format and what a malformed id returns (400 vs 404: spec/PM); trailing-slash behavior (Spring 6 does not match `/books/`); enum wire format (lower-case `unread|reading|completed`, case-sensitive; Java enums are UPPER_SNAKE: mapping is a rule); `:id` in the BRD vs `{id}` in Spring notation. | Module 3 §3 (be specific). | Add to API Standards (diff D-6); spec-level items (id errors, tie-break, enum case) routed to the PM. |
| C-25 | MINOR | Security Standards: "title 500, author 200, bio 500 once specified" | The limits duplicate the BRD/spec: two sources of truth, and the constitution outranks the spec. The bio limit belongs to Phase 5 drift material ("once specified" pre-announces a feature that "Never add features not in spec" forbids today). Length unit unspecified (UTF-16 `@Size` vs code points: an emoji counts as 2). | Authority order; `spec.md` is the source for field rules. | Remove the numbers; say "maximum lengths exactly as in `spec.md`, counted in Unicode code points after trimming, enforced by Bean Validation at the boundary" (diff D-6). |
| C-26 | MINOR | Code Style | No code snippet (Module 3 §3.5 asks for "a brief, real code snippet"), no indentation, line-length, brace or formatter rule, no import ordering. | Module 3 §3.5. | Add a short snippet and four mechanical rules (diff D-9). |
| C-27 | MINOR | Approved dependencies | Gaps that would otherwise be decided by the agent: Lombok is not approved (state it: DTOs are records, entities have explicit accessors); Spring Boot Actuator is not approved, so the Playwright health check must use a static page; build plugins implied by the stack (`spring-boot-maven-plugin`, Surefire, `maven-compiler-plugin`, `maven-resources-plugin`) are not listed though they arrive with the Boot parent; whether `hibernate-community-dialects` is version-managed by the Boot BOM must be verified, otherwise its version must be pinned equal to Hibernate's. | Module 3 §2 Tier 2; Module 2 §4 (slopsquatting). | Add an explicit "Not approved" list and "build plugins inherited from the Spring Boot parent are approved" (diff D-1). |
| C-28 | MINOR | (absent) Performance and capacity | No stance. Spec success metrics (add a book in <10 s, "instantly") are not measurable and belong to the spec, but the constitution should state capacity assumptions (single user, order of thousands of rows), forbid N+1 patterns, and say unbounded list is deliberate. | Module 4 §4 (non-functional coverage). | One bullet each under Architecture and API (diff D-6). |
| C-29 | MINOR | Security Standards; NEVER | "Never store passwords in plain text" is inherited from the template but meaningless here (auth is out of scope); a dead rule invites speculative code. Logging rule is policy-only: no mechanism (no `System.out`, no `printStackTrace`, SQL/bind logging off, 500s logged server-side with a correlation-free generic client message). SQLite file contains the user's data unencrypted: acceptable, but say so. | Module 3 Summary 2. | Keep the line but mark "not applicable while auth is out of scope; any credential storage is ASK FIRST"; add logging mechanics (diff D-12). |
| C-30 | MINOR | Whole document vs `exercise-brief.md` | Deliberate deviations from the template are not recorded, so the next reader cannot tell intent from error: "Add comments" became Javadoc; async/await rule dropped (Java); "Mock external dependencies (DB, APIs)" replaced by real SQLite in integration tests; `SELECT *` narrowed; 409 added; file naming `.test.`/`.spec.` replaced. | Module 5 (invariants and traceability); Module 3 Summary 1. | Add a short "Deviations from the exercise template" table (diff D-11). |
| C-31 | MINOR | Workflow Rules: package root `com.globant.globerbooks` | The package root, Maven `groupId`/`artifactId` and JDK distribution are unverified choices that every file will inherit. | Module 3 §3.1. | Sponsor confirms D14. |

### 2. Cross-document consistency matrix

| Topic | Constitution v0.1 | Other document | Result |
|---|---|---|---|
| Definition of done command | `mvn test` | `./mvnw verify` (`sdd-java-architect.md` §3, §8; `orchestration.md` §3) | CONFLICT (C-03) |
| Module layout | undefined | `backend/`, `frontend/`, `e2e/` (agents, `git-workflow.md`) | GAP (C-02) |
| DB path key | "set by configuration" | "defined in `plan.md`" (`frontend-testing.md` §3) | CIRCULAR (C-05) |
| Test levels | unit, integration | + slice, contract (Java agent) | GAP (C-15) |
| Commit cadence | "commit after each accepted task"; "one squashed commit" | atomic commits, squash on merge (`git-workflow.md` §3, §4) | AMBIGUOUS (C-18) |
| Direct commits to `main` | never | bootstrap exception (`git-workflow.md` §2) | MISMATCH (C-18) |
| PR use | silent | merge "through a PR" vs PR "only when asked" (`git-workflow.md` §2, §4) | INTERNAL CONFLICT in a Sponsor-owned file (C-18) |
| Reporters/plugins | "reporters or plugins" are ASK FIRST (Amendment 1) | `list`, `html`, machine-readable reporters prescribed (`frontend-testing.md` §3) | AMBIGUOUS (A-03) |
| Version pinning | "pinned (exact)" | MCP defined with `@playwright/mcp@latest` and `npx -y` (`sdd-frontend-developer.md` frontmatter) | CONFLICT (A-01) |
| Server reuse / port | port from configuration | `reuseExistingServer: !CI`; MCP origin fixed to `http://localhost:8080` | RISK (A-02) |
| Frontend unit tests | "unit tests for all business logic" | no JS runner approved (`sdd-frontend-developer.md` §5) | TENSION (A-07) |
| Envelope on 409 | three keys for all errors | BRD: 409 returns existing book details | CONFLICT (C-06) |
| No-AI-attribution | present in NEVER and Workflow | `git-workflow.md` §1, `.githooks/commit-msg` (message only) | CONSISTENT in text; enforcement partial (C-22) |
| Lengths | title 500, author 200 | BRD template: same (spec to confirm) | CONSISTENT but duplicated (C-25) |
| Status codes | 200/201/400/404/409/500 | BRD template: 200/201/400/404/500 | ACCEPTABLE superset; 405/415 missing (C-07) |

### 3. Findings: Amendment 1 (Playwright)

The decision to add Playwright for frontend E2E is sound and consistent with Module 4 §4 (a real-browser layer for requirements-based tests) and Module 5 (spec-derived oracles). The scoping of the MCP to one agent, the ban on `browser_run_code_unsafe`, treating page content as data, and `retries: 0` are all good. Changes needed:

| ID | Sev | Finding | Evidence | Proposed resolution |
|---|---|---|---|---|
| A-01 | MAJOR | The agent frontmatter launches `npx -y @playwright/mcp@latest`: unpinned, auto-confirming install, executed every time the agent starts. That contradicts "Versions are pinned (exact)" and is exactly the supply-chain exposure Module 2 §4 warns about (slopsquatting / unreviewed remote code). `frontend-testing.md` §1 tolerates `latest` "until pinned" but the constitution has no sunset date for the exception. | Amendment 1 bullet 6; `sdd-frontend-developer.md` frontmatter; Module 2 §4. | Pin now (Sponsor chooses the version after checking the official package page; D11), drop `-y` if the tool allows a pre-installed package, record the version in the constitution table, and make "no `@latest`, no unpinned `npx`" a rule. |
| A-02 | MAJOR | `reuseExistingServer: !process.env.CI` means that locally the E2E suite may attach to an already running developer instance (default port 8080) that uses the developer's real database; tests would then create and read data in it. The MCP `--allowed-origins` is hardcoded to `http://localhost:8080`, the same port the dev server uses. DB isolation is described ("temporary file") but the mechanism (env var name, path, cleanup, port) is deferred to `plan.md`. | `frontend-testing.md` §3; frontend agent frontmatter; C-05. | Dedicated E2E port (for example 18080, configured via `SERVER_PORT`), `reuseExistingServer: false`, DB path via `SPRING_DATASOURCE_URL` to a fresh temp directory in `global-setup`, and the MCP origin aligned to the same port. Put these four facts in the constitution; the plan only chooses values. |
| A-03 | MINOR | "reporters or plugins" are ASK FIRST, yet `frontend-testing.md` prescribes `list`, `html` and a machine-readable reporter. These are Playwright built-ins, not packages. | Amendment 1 bullet 3 vs `frontend-testing.md` §3. | Clarify: built-in reporters (`list`, `html`, `json`, `junit`) and built-in `toHaveScreenshot` are approved; third-party reporters are ASK FIRST. |
| A-04 | MINOR | The suite is described as TypeScript (`playwright.config.ts`, `*.spec.ts`) but no language decision or style rule exists; the project otherwise bans build steps. Playwright transpiles TS itself, so no extra package is needed, but `@types/node` or `typescript` would be additions. | `frontend-testing.md` §2. | State: tests are TypeScript run by Playwright's built-in transpilation, `strict` type-check not enforced, no extra dev dependency; or choose plain JavaScript (Sponsor, D11). Apply the same naming and size rules as frontend code. |
| A-05 | MINOR | `@visual` baselines (`toHaveScreenshot`) are platform- and font-specific (file names carry the OS). Baselines committed from one machine fail on another, producing the flakiness the policy forbids. | `frontend-testing.md` §4 rule 10. | Generate and verify baselines in one pinned environment only (the cloud/CI Linux image), or exclude `@visual` until CI exists. Sponsor decision D11. |
| A-06 | MINOR | "Node.js (current LTS)" is a moving target; no `.nvmrc`/`engines`, no `npm ci` rule. | Amendment 1 bullet 1. | Pin Node by version in the table; commit `e2e/package-lock.json`; always `npm ci`. |
| A-07 | MINOR | Frontend task completion (Playwright green, `frontend-testing.md` §7) is not in the constitution's Definition of Done, and pure JS logic has no unit-test runner ("only after the Sponsor approves a runner") while the constitution says "unit tests for all business logic". Node ships a built-in runner (`node --test`) that needs no package. | `sdd-frontend-developer.md` §5; constitution ALWAYS. | Decision D9: approve the built-in `node:test` for DOM-free ES modules (no new dependency). |
| A-08 | MINOR | Amendment 1 is written as a trailing section with "Status: pending". Once approved it should be folded into Tech Stack / Testing and removed, with a changelog line, otherwise two places define the same rule. | Module 3 Summary 1. | Fold in v0.2; keep a changelog. |

### 4. Questions and decisions for the Sponsor

Only the Sponsor can decide these (constitution owner; ASK FIRST items). Each is marked as a recommendation, not a decision. Items marked PM are not Sponsor decisions and are listed at the end.

| # | Decision | Options | Recommendation | Why only the Sponsor |
|---|---|---|---|---|
| D1 | Spring Boot line and exact pins (Java patch + distribution, Maven wrapper version, Boot, `sqlite-jdbc`, community dialect, JaCoCo, Node, Playwright, Chromium, MCP) | (a) latest patch of the 3.x line still in support; (b) current 4.x line (requires verifying dialect compatibility with its Hibernate and Jackson versions); (c) keep "3.x" unpinned (rejected: violates Module 3) | Pin exact versions in the constitution after one verification pass (support calendar, dialect compatibility, `mvnw verify` on the pins). Choose (a) or (b) based on that check; I have not verified current versions. | Stack and dependency approval is ASK FIRST |
| D2 | Project structure | (A) everything in `backend/src/main/resources/static`; (B) `backend/` Maven root + `frontend/` mapped into the jar via a Maven `<resources>` entry (no frontend build step) + `e2e/`; (C) pom at repository root | B: keeps parallel work on disjoint directories, satisfies "no build step", matches both agents and `git-workflow.md` | Architectural pattern |
| D3 | Schema management | (A) `ddl-auto=update`; (B) `validate` + versioned `schema.sql` at startup; (C) approve Flyway now | B now; decide a migration tool when Phase 5 (author bio) needs the first `ALTER` | Schema and dependency are ASK FIRST |
| D4 | Error envelope extensibility | (A) strict 3 keys, validation messages as a deterministic string `field: message; field: message` (sorted by API field name); (B) 3 required keys + optional `details` member defined only by the spec for specific codes (validation: list of `{field, message}`; 409: `existing_book`) | B: lets the UI associate messages with inputs for accessibility without parsing text, and resolves the 409 payload conflict; additive, so stays within the template's spirit | Changes the BRD template's envelope |
| D5 | Timestamp precision | (A) seconds (matches BRD examples), spec defines a tie-break; (B) fixed milliseconds `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` | B, with spec examples updated by the PM; fewer ordering ties | Cross-cutting API format |
| D6 | Mechanical enforcement of style rules | (A) review only; (B) compiler `-Xlint:all -Werror` only; (C) B + `maven-checkstyle-plugin` with a small committed rule set | C (turns about 7 rules from opinion into build failures) | New build plugin = ASK FIRST |
| D7 | CI | (A) none, orchestrator runs `./mvnw verify` and Playwright and attaches output; (B) minimal CI workflow | A for the exercise scope, recorded as an accepted risk; revisit before any release tag | CI/CD change = ASK FIRST |
| D8 | Web security baseline | Strict CSP + `nosniff` + no CORS + body-size cap via one small servlet filter / config (no new dependency) vs none | Adopt; it is cheap and removes a whole class of XSS exposure; it also fixes the frontend design (no inline script/style, no CDN) | Security and compliance stay with the human (Module 5) |
| D9 | Frontend unit-test runner | (A) none, E2E only; (B) built-in `node:test` for DOM-free modules | B (no new package) | Tooling approval |
| D10 | Frontend accessibility target and browser support | WCAG 2.1 AA (as in the agent) vs lower; support "current evergreen Chrome/Edge/Firefox/Safari" with automated verification on Chromium only vs declare Chromium-only | WCAG 2.1 AA; evergreen support stated, with "verified on Chromium only" as an accepted risk | Compliance/business risk |
| D11 | Amendment 1 follow-ups | Pin MCP and Playwright versions now (A-01); E2E port and `reuseExistingServer: false` (A-02); built-in reporters allowed (A-03); TS vs JS (A-04); visual baselines policy (A-05); Node pin (A-06) | Accept all as written in the diffs | Amendment owner |
| D12 | Pagination in v1 | none (documented, unbounded, `total` returned) vs paging parameters | None | Spec-adjacent; constitution only states the stance |
| D13 | Coverage definition | Line coverage of the whole backend bundle >= 80% from unit + integration merged, exclusions limited to the `Application` class and pure configuration classes; branch coverage reported, not gated | As stated | Quality gate |
| D14 | Identity choices | Confirm package root `com.globant.globerbooks`, Maven coordinates, JDK distribution | Confirm or replace | Naming the Sponsor owns |
| D15 | Governance | Version the constitution (v0.2 ...), changelog, fold Amendment 1, amendment procedure, referenced documents incorporated by reference | Adopt | Constitution owner |
| D16 | Hook scope | Keep commit-msg only; or add a `pre-push` check on author identity, branch and tag names; relax overly generic words | Add the pre-push identity/branch check and review the word list; no CI needed | Owner of `git-workflow.md` |

Routed to the PM (not Sponsor decisions; for the Phase 2 Clarify Loop): malformed or non-UUID id behavior (400 vs 404); `status` filter with an invalid value; sort tie-break for "newest first"; whitespace-only titles and trimming-before-length semantics; Unicode normalization rule for duplicates; enum case sensitivity on input; the 409 payload; open points 1 to 10 in my brief remain as stated and are not resolved by this review.

Also for the Sponsor to note (not constitution items): exercise phase times exceed the stated 6 to 8 hours (open point 10), and `git-workflow.md` has the PR contradiction in C-18 item 4.

## 5. Proposed document changes (exact text; I do not apply them)

The Sponsor (or the orchestrator on the Sponsor's instruction) applies these to `docs/sdd/constitution.md`. `<PIN>` marks values the Sponsor must fill after D1.

### D-1: header, stack, dependencies, governance
```diff
+ # Project Constitution: GloberBooks
+ - Owner: Sponsor    Version: 0.2 (draft)    Last updated: <date>    Status: Draft
+ - Referenced documents (`git-workflow.md`, `frontend-testing.md`, `orchestration.md`, `agent-protocol.md`) are incorporated by reference. If any of them conflicts with this document, this document wins.

 ## Tech Stack
-- **Language/Runtime:** Java 21 (LTS)
-- **Backend framework:** Spring Boot 3.x (web, validation, data-jpa)
-- **Build tool:** Maven (single module, Maven Wrapper committed)
+| Component | Pinned version | Notes |
+|---|---|---|
+| JDK | 21.0.<PIN> (<distribution>) | `--release 21` |
+| Spring Boot | <PIN> | parent POM; manages Hibernate, Jackson, JUnit, Mockito, AssertJ |
+| Maven (via wrapper) | <PIN> | always `./mvnw`, never a system `mvn` |
+| org.xerial:sqlite-jdbc | <PIN> | |
+| org.hibernate.orm:hibernate-community-dialects | <equals the Hibernate version resolved by the Boot parent> | |
+| JaCoCo | <PIN> | |
+| Node.js (dev only) | <PIN> via `.nvmrc` | |
+| @playwright/test (dev only) | <PIN> exact, lockfile committed | |
+| @playwright/mcp (agent only) | <PIN> exact, never `@latest` | |
+| Chromium | the build installed by the pinned Playwright | |
+Changing any pinned version is ASK FIRST. The build file and this table must match.

 ### Approved dependencies
+Build plugins inherited from the Spring Boot parent (compiler, resources, Surefire, Spring Boot Maven plugin) are approved.
+Not approved (ASK FIRST): Lombok, Spring Boot Actuator, Spring Security, H2 or any second database, Flyway or Liquibase (see Persistence), Checkstyle (until D6 is decided), any mutation-testing plugin.
```

### D-2: project structure (new section after Tech Stack)
```diff
+## Project Structure
+```
+glober-books/
+├── backend/                   # Maven project root: pom.xml, mvnw, .mvn/
+│   ├── src/main/java/com/globant/globerbooks/
+│   │   ├── controller/  service/  repository/  domain/  dto/  error/  config/
+│   ├── src/main/resources/    # application.yml, schema.sql
+│   └── src/test/java/...      # mirrors main packages
+├── frontend/                  # index.html, css/, js/ (api, logic, ui); no build step
+│                              # copied to classpath `static/` by a Maven <resources> entry
+├── e2e/                       # dev-only Playwright project (own package.json, lockfile)
+├── docs/                      # sdd/ (living documents), course/ (frozen)
+└── .githooks/                 # commit-msg guard
+```
+Files go where this map says. A new top-level directory or package is an architectural change (ASK FIRST).
```

### D-3: executable commands and Definition of Done (new section)
```diff
+## Executable Commands
+Run from the repository root unless stated.
+- Activate hooks (once per clone): `git config core.hooksPath .githooks`
+- Backend full gate (compile, unit + integration tests, coverage check): `cd backend && ./mvnw verify`
+- Backend unit tests only: `cd backend && ./mvnw test`
+- One test class: `cd backend && ./mvnw -Dtest=BookServiceTest test`
+- Build the runnable jar: `cd backend && ./mvnw -DskipTests package`
+- Run the application: `cd backend && ./mvnw spring-boot:run`
+- Coverage report: `backend/target/site/jacoco/index.html`
+- E2E setup: `cd e2e && npm ci && npx playwright install chromium`
+- E2E smoke / full: `cd e2e && npx playwright test --grep @smoke` / `cd e2e && npx playwright test`
+
+## Definition of Done (per task)
+1. `cd backend && ./mvnw verify` green (backend tasks); `cd e2e && npx playwright test` green (frontend tasks); real output attached.
+2. Coverage gate met; every acceptance criterion mapped to a test.
+3. No new dependency, schema change or file outside the task scope.
+4. `progress.md` and affected documents updated in the same branch.
-- A task is not complete until `mvn test` (including the coverage check) passes.
```
(Surefire/Failsafe split: the plan states whether `*IntegrationTest` runs under Surefire in `test` or Failsafe in `verify`; the JaCoCo `check` goal is bound to `verify`.)

### D-4: persistence, configuration keys, schema management
```diff
-## Configuration and Secrets
-- All environment-specific values (database file path, server port) come from `application.properties`/`application.yml` or environment variables, with safe defaults for local development.
+## Configuration and Secrets
+- One configuration file: `backend/src/main/resources/application.yml`. Environment variables override it.
+- Database location: `spring.datasource.url`, env `SPRING_DATASOURCE_URL`, default `jdbc:sqlite:./data/globerbooks.db`. Server port: `server.port`, env `SERVER_PORT`, default 8080.
+- Mandatory properties: `spring.jpa.hibernate.ddl-auto: validate`; `spring.jpa.open-in-view: false`; `spring.jpa.show-sql: false`; `spring.jackson.property-naming-strategy: SNAKE_CASE`; `spring.jackson.deserialization.fail-on-unknown-properties: true`; `spring.datasource.hikari.maximum-pool-size: 1` (unless `plan.md` justifies otherwise).

+## Persistence Standards
+- Schema is created by a versioned `schema.sql` run at startup (`CREATE TABLE IF NOT EXISTS`); Hibernate never creates or alters tables. The initial schema is approved with `plan.md`; any later change, new table or new column is ASK FIRST and arrives with the spec change that justifies it.
+- Transactions are declared on service methods only (`@Transactional`; `readOnly = true` for reads). Controllers and repositories do not open transactions.
+- Comparison keys for case-insensitive matching (duplicate detection) are computed in Java (trim, Unicode normalization, `toLowerCase(Locale.ROOT)`); SQL `LOWER()` and `COLLATE NOCASE` are not used for business comparisons.
+- Timestamps are injected from a `java.time.Clock` bean, stored as UTC ISO-8601 text, never from `Instant.now()` in business code. Ids are UUID v4 generated in the service layer, stored and serialized as lowercase 36-character strings.
+- SQLite connection options (journal mode, busy timeout, foreign keys) are set in the JDBC URL or a single configuration class and recorded in `plan.md`.
+- Unit and integration tests use a temporary file database (`@TempDir`), never `:memory:`. Slice tests with `@DataJpaTest` must set `@AutoConfigureTestDatabase(replace = NONE)`.
```

### D-5: error handling
```diff
 ## Error Handling Standards
 All errors must return:
 ...
+Optional member (D4, option B): `details`, present only for codes whose shape `spec.md` defines (validation errors: array of `{ "field": "<api_field_name>", "message": "..." }`; duplicate conflict: the existing book). Clients must not depend on `error` text.
-- For validation errors, `error` must identify the offending field(s).
+- For validation errors, `error` is a summary and `details` lists every offending field using API names (`date_added`), never Java names.
+- Reserved framework-level codes (a spec code with the same meaning takes precedence): `VALIDATION_ERROR` 400, `MALFORMED_REQUEST` 400 (unreadable JSON, unknown field, wrong type, invalid path or query value), `NOT_FOUND` 404 (unknown route or resource), `METHOD_NOT_ALLOWED` 405, `UNSUPPORTED_MEDIA_TYPE` 415, `INTERNAL_ERROR` 500.
-- A single global exception handler produces all error responses. Controllers must not build error bodies by hand.
+- A single `@RestControllerAdvice` produces every error response, including framework exceptions (unknown route, 405, 415, binding and conversion errors) and the catch-all for `Exception`. The default Spring error page and body are disabled. Controllers must not build error bodies.
+- `timestamp` uses one fixed format (D5): <PIN>.
 Status codes:
+- 405 Method not allowed; 415 Unsupported media type
```

### D-6: API standards
```diff
 ## API Standards
+- Versioning: additive changes only inside `/api/v1/`; a breaking change creates `/api/v2/`.
+- Content type: `application/json; charset=UTF-8` for all request and response bodies; other types yield 415/406.
+- Same-origin only: no CORS configuration; `@CrossOrigin` is forbidden.
+- The list endpoint is deliberately unpaginated in v1 (single-user, capacity assumption: thousands of rows) and returns `total`; adding paging is a spec change.
+- Enum values on the wire are lower-case and case-sensitive exactly as in `spec.md`; mapping to Java enums is one converter.
+- Maximum string lengths are exactly those in `spec.md`, counted in Unicode code points after trimming, enforced with Bean Validation at the boundary.
+- No N+1 query patterns; no query inside a loop.
-- Apply Bean Validation constraints to every request DTO, including maximum lengths (title 500, author 200, bio 500 once specified).   (Security Standards)
+- Apply Bean Validation constraints to every request DTO; limits come from `spec.md`.
```

### D-7: boundaries
```diff
 ### ⚠️ ASK FIRST:
-- Before adding or changing an API endpoint, request field or response field not defined in `spec.md`
+- Before changing a pinned version (Tech Stack table), the schema, `schema.sql`, or the project structure map
 ### 🚫 NEVER DO:
-- Never add features not in `spec.md`
+- Never add features, endpoints, request or response fields not defined in `spec.md`. A missing behavior is raised as a CHANGE_REQUEST; the spec changes first.
-- Never use `SELECT *` in hand-written queries (list explicit columns or use explicit projections)
+- Never write a native SQL or JPQL select list with `*` anywhere under `backend/src/main`. Native queries are used only when `plan.md` justifies them. Audit check: grep for `select *` (case-insensitive).
```

### D-8: testing
```diff
 ## Testing Requirements
+| Layer | Tooling | Verifies | Rules |
+|---|---|---|---|
+| Unit (`*Test`) | JUnit 5, Mockito, AssertJ | services, validators, mappers, error mapping | mock all collaborators |
+| Slice (`*Test`) | `@WebMvcTest`, `@DataJpaTest` | binding, validation, status codes; repository queries | controller slice mocks the service; repository slice uses a real SQLite temp file |
+| Integration (`*IntegrationTest`) | `@SpringBootTest` | each story end to end, HTTP to SQLite | no mocks of own code; fresh DB per test class |
+| Contract (inside integration) | same | every endpoint's exact path, payload, status, error code | expected values copied from `spec.md`, never from observed output |
+| E2E | `@playwright/test` | UI flows against the real app | see `frontend-testing.md` |
-- Minimum 80% line coverage, enforced by a JaCoCo check that fails the Maven build below the threshold.
+- Coverage: JaCoCo line coverage of the whole backend bundle >= 80% from unit and integration data merged; check bound to `verify` and failing the build. Exclusions are limited to the Spring Boot `Application` class and configuration classes without logic. Branch coverage is reported, not gated. Frontend has no percentage gate: every acceptance criterion maps to a green E2E test.
+- Expected values in tests come from `spec.md`. A test that asserts what the code currently does is a defect.
+- A flaky test is a defect to fix. No retries (`rerunFailingTestsCount`, `retries`), no `@RepeatedTest` workarounds, no quarantine without Sponsor approval.
 ### 🚫 NEVER DO (add):
+- Never delete, skip, disable (`@Disabled`, `test.skip`, `test.fixme`) or weaken a failing test, or edit an expected value to make it pass, without Sponsor approval. Decide instead: fix the code (Scenario A) or change the spec (Scenario B).
```

### D-9: code style (after D6)
```diff
 ## Code Style
-- Descriptive variable names (minimum 3 characters). The only exception is the identifier `id`.
+- Identifier names (local variables, parameters, fields, lambda and catch parameters) have at least 3 characters in Java and JavaScript. The only exception is `id`.
-- Prefer immutable types (records for DTOs) and `final` fields where practical
+- DTOs are records. Entities have no public setters unless the plan justifies them. Fields are `final` unless a mutable field is required by JPA.
+- Max function length 50 and max file length 300, counted as physical lines including comments and blank lines.
+- Indentation 4 spaces (Java), 2 spaces (HTML, CSS, JS); line length at most 120; one statement per line.
+- Javadoc applies to public classes and public methods under `src/main`; not to tests or `@Override` methods.
+```java
+/** Rejects a rating unless the book is completed (BR-002). */
+public void assignRating(String bookId, int rating) {
+    Book book = bookRepository.findById(bookId)
+            .orElseThrow(() -> new BookNotFoundException(bookId));
+    if (book.getStatus() != BookStatus.COMPLETED) {
+        throw new RatingNotAllowedException(bookId);
+    }
+}
+```
+- Compiler runs with `-Xlint:all -Werror`. (If D6 option C: Checkstyle enforces naming, imports, lengths, Javadoc.)
```

### D-10: workflow rules
```diff
 ### ✅ ALWAYS DO:
-- Update `progress.md` and commit after each accepted task
+- Update `progress.md` in the task's branch; commits follow `git-workflow.md` (atomic on the branch, one squashed commit per accepted task on `main`)
 ### 🚫 NEVER DO:
-- Never modify files outside the scope of the current task
+- Never modify files outside the scope of the current task. Task scope = files listed in the task + the agent's own `status.md` row + `progress.md` + its own handoff files.
-- Never bypass git hooks (`--no-verify`) or force push `main`
+- Never bypass git hooks (`--no-verify`), force push (including `--force-with-lease`) to `main`, rewrite shared history, or run `git reset --hard` outside a `drift/` branch, without Sponsor approval.
 ## Workflow Rules (SDD)
-- One branch per task and one squashed commit per accepted task, following `docs/sdd/git-workflow.md` (trunk-based branches `<type>/task-NN-<slug>`, ...no direct commits to `main`).
+- Branches `<type>/<ref>-<slug>` as defined in `git-workflow.md` (`<ref>` is `task-NN`, `us-NNN`, or omitted for docs/chore). No direct commits to `main` except the initial repository bootstrap.
```

### D-11: governance and deviations (new)
```diff
+## Amendment Procedure
+1. Anyone proposes a change by handoff; the Liaison reviews it; the Sponsor approves it.
+2. The orchestrator applies it on a `spec/` branch, bumps the version and adds a changelog line below. Approved amendments are folded into the relevant section and the "Amendments" section is emptied.
+
+## Deviations from the exercise template
+| Template | This project | Reason |
+|---|---|---|
+| "Add comments for public interfaces" | Javadoc on public classes and methods | Java |
+| "Use async/await" | Synchronous blocking services | Java, MVC stack |
+| "Mock external dependencies (DB, APIs)" | Mock the repository in unit tests; real SQLite in slice and integration tests | Oracle quality (Module 4 §3) |
+| "No SELECT * in production code" | No `*` select list in native SQL or JPQL | JPA generates its own column lists |
+| Status codes without 409 | 409 conflict added | BR-003 duplicate handling |
+## Changelog
+- 0.2: ...
```

### D-12: security and frontend standards
```diff
 ## Security Standards
+- Every response carries `Content-Security-Policy: default-src 'self'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff` and `Referrer-Policy: no-referrer`, set by one filter or configuration class. No inline script or style, no external script, font or stylesheet (CDN).
+- Request body size is capped (value in `plan.md`).
+- Dependency integrity: lockfile committed, `npm ci` only; before approving any new package, confirm it exists on the official registry under the expected publisher (hallucinated-package risk, Module 2 §4).
+- Logging: SLF4J only; no `System.out`, `printStackTrace` or SQL/bind-parameter logging; unexpected exceptions are logged server-side and returned as a generic `INTERNAL_ERROR`.
-- Never store passwords in plain text
+- Never store passwords in plain text (not applicable while authentication is out of scope; any credential storage is ASK FIRST)

+## Frontend Standards
+- Plain HTML, CSS and ES-module JavaScript; no build step, no framework, no CDN. `js/api` is the only place that calls `fetch`; relative URLs only.
+- Accessibility target: <WCAG 2.1 AA, per D10>: semantic HTML, labelled controls, keyboard operation, visible focus, native `<dialog>`, `aria-live` for results and errors, contrast >= 4.5:1, `prefers-reduced-motion`.
+- Browser support: <per D10>.
+- Client-side validation is for UX only; the server is authoritative and every server rejection is handled.
+- UI strings are English; user text is rendered with `textContent`.
+- JS and CSS follow the naming, size and Javadoc/JSDoc rules above.
```

### D-13: `.gitignore` additions (file not in my edit scope)
```diff
 # Local databases and config overrides
 *.db
 *.sqlite
 *.sqlite3
+*.db-wal
+*.db-shm
+*.db-journal
+data/
+.env
+.env.*
 application-local.*
+
+# Logs
+*.log
+logs/
+hs_err_pid*
```

### Amendment 1 replacement text (fold into the body after D11)
```diff
-- Versions are pinned (exact) with a lockfile in the first frontend task and recorded in `plan.md`.
+- Versions are pinned (exact) in the Tech Stack table now; `e2e/package-lock.json` is committed; installs use `npm ci`. `@latest` and unpinned `npx` are forbidden, including in agent definitions.
+- E2E isolation: the suite runs the application on a dedicated port (`SERVER_PORT=<PIN>`, not 8080) with `SPRING_DATASOURCE_URL` pointing to a fresh temporary directory created in `global-setup`; `reuseExistingServer` is `false`. The MCP `--allowed-origins` uses the same origin.
+- Playwright built-in reporters and `toHaveScreenshot` are approved; third-party reporters and plugins remain ASK FIRST. Visual baselines are generated only in the pinned reference environment.
+- E2E tests are <TypeScript run by Playwright's built-in transpilation | JavaScript>, with no additional dev dependency.
```

## Open items / decisions needed

Gating items for verdict APPROVED (round 2): C-01 to C-06 closed in a v0.2 (requires Sponsor decisions D1 to D5); the MAJOR items C-07 to C-22 and A-01, A-02 either applied or explicitly accepted by the Sponsor as risks in writing; the remaining MINOR items may be applied in the same revision or logged.

Sponsor-only decisions: D1 to D16 above. Everything else in this report is wording that the Sponsor can accept as proposed text.

Liaison rulings made on doctrine (no Sponsor input needed, for the record):
1. Items in the NEVER tier must be binary and verifiable; subjective wording ("where practical", SRP) moves to the review checklist (Module 3 Summary 2).
2. A rule that depends on a default of the framework (unknown JSON fields) must name the setting (Module 3 §3).
3. Version pins and configuration keys belong to the highest document that depends on them, never to `plan.md` (authority order).
4. Frontend test protection is a constitutional rule, not an amendment detail (Module 3 §2 Tier 3).

## Requested response

1. Sponsor: answer D1 to D16 (accept the recommendation or choose another option). D1, D2, D3 and D4 unblock the most work.
2. Orchestrator: after the Sponsor's answers, open a `spec/` branch, apply the accepted diffs to produce constitution v0.2 (Amendment 1 folded in), update `status.md` Phase 1 row, and send a REVIEW_REQUEST (round 2) to the Liaison.
3. Sponsor: apply or reject the `.gitignore` additions (D-13) and the PR wording fix in `git-workflow.md` (C-18 item 4); these files are outside the Liaison's edit rights.
4. Liaison (round 2): verify each gating item closed and each cross-document conflict resolved; then issue APPROVED for the Sponsor gate.
