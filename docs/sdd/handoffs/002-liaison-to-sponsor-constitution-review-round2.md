# Handoff 002: Constitution review, round 2 (Phase 1, Mode A)
- From: liaison
- To: sponsor
- Phase: 1   Round: 2 of max 3
- Artifacts: `docs/sdd/constitution.md` (v0.2 draft), `docs/sdd/git-workflow.md`, `docs/sdd/frontend-testing.md`, `docs/sdd/orchestration.md`, `docs/sdd/agent-protocol.md`, `docs/sdd/README.md`, `docs/sdd/status.md`, `docs/sdd/security.md` (gate noted only), `docs/sdd/approach.md`, `.claude/agents/sdd-java-architect.md`, `.claude/agents/sdd-frontend-developer.md`, `.githooks/commit-msg`, `.githooks/pre-push`, `.gitignore`; baseline: handoff 001; branch `docs/phase-1-constitution-review`
- Type: REVIEW_REPORT

Mode: A (Constitution review), with the Layer 1b repository-hygiene checks that apply to Phase 1.

## Summary

**Liaison verdict: APPROVED WITH CHANGES.** All 6 round-1 BLOCKERs are closed. Every round-1 MAJOR (C-07 to C-22, A-01, A-02) and MINOR is closed, accepted or explicitly deferred. v0.2 is a large improvement and now meets the five Module 3 §3 components (stack with versions, structure map, executable commands, testing strategy, style with snippet and git workflow) plus the three tiers.

Round 2 found **0 BLOCKER, 4 MAJOR, 11 MINOR** new or residual items (N-01 to N-15). None needs a Sponsor business decision except S1 to S3 below. The four MAJORs are text-level fixes. If the orchestrator applies fixes F1 to F4 verbatim (and F5 to F9 at its discretion), no round 3 is needed for the Liaison. **This is the Liaison verdict only: the Security verdict (first review) and the Sponsor gate are still required** (`agent-protocol.md` §5, Phase 1 row). I did not review security substance.

Not independently verified by me (no shell or network in this review): the Spring Boot support calendar, every version number, the claim that `node --test frontend/` works on Node 24 (I believe it does not, see N-04), the executable bit of `.githooks/*`. Where it matters it is stated as "verify".

## Content

### 1. Closure of round 1

Legend: CLOSED = resolved as proposed; CLOSED+N = closed with a residual tracked as a new finding; DEFERRED = explicitly postponed in the document.

| Round 1 ID | Status | Evidence in v0.2 / note |
|---|---|---|
| C-01 pins | CLOSED+N-05 | Tech Stack "Pinned versions" table. Residuals: JDK pin is a floor, distribution verified differs; no per-pin verification marker; Maven 3.9.16 and Checkstyle 14.3.0 / plugin 3.6.0 unverified (flagged in the table: DEFERRED to Task 1, acceptable only with S1) |
| C-02 structure | CLOSED+N-10 | "Project Structure" |
| C-03 commands, DoD | CLOSED+N-04, N-06 | "Executable Commands", "Definition of Done"; `./mvnw verify` everywhere |
| C-04 schema | CLOSED | Persistence Standards (`schema.sql`, `ddl-auto: validate`, ASK FIRST for change) |
| C-05 config keys | CLOSED | Configuration and Secrets (keys, defaults, one file) |
| C-06 envelope | CLOSED+N-01, N-03 | Optional `details` (D4 B) |
| C-07 framework errors | CLOSED+N-01 | Reserved codes added; gaps for 406, 413, Host rejection, and filter-originated bodies |
| C-08 unknown JSON | CLOSED | Property named, contract test required |
| C-09 NEVER/ASK overlap | CLOSED | ASK FIRST line removed; NEVER names CHANGE_REQUEST |
| C-10 `SELECT *` | CLOSED+N-13 | Reworded, grep audit; regex too narrow (N-13) |
| C-11 case-insensitive keys | CLOSED+N-13 | Computed in Java; normalization form unnamed |
| C-12 SQLite concurrency | CLOSED+N-13 | Pool 1, temp file only, options recorded in plan; "mandatory vs unless justified" wording |
| C-13 time, ids | CLOSED | Fixed ms format, `Clock`, UUID v4. Tie-break and BRD examples (seconds) routed to PM |
| C-14 test protection | CLOSED | NEVER line, ALWAYS "expected values from spec", flaky policy |
| C-15 test layers | CLOSED+N-03 | Layer table, `replace = NONE`; the Java agent still allows in-memory |
| C-16 coverage | CLOSED | Scope, exclusions, merged data, branch reported only |
| C-17 mechanical style | CLOSED (Java) +N-08 | `-Xlint:all -Werror` + Checkstyle (D6 C). JavaScript/CSS rules have no tool |
| C-18 workflow contradictions | CLOSED+N-11 | Single wording; `git-workflow.md` §2/§4 PR contradiction fixed; force-push scope differs (N-11) |
| C-19 governance | CLOSED+N-02 | Header, amendment procedure, changelog, deviations; hole for referenced documents (N-02) |
| C-20 web security | CLOSED | Headers, CORS, limits, supply chain present. Substance belongs to the Security reviewer; N-01 is the only interaction with my scope |
| C-21 frontend standards | CLOSED | "Frontend Standards" section |
| C-22 hygiene | CLOSED+N-07, N-14 | (a) `.gitignore` extended; (b) activation is Executable Commands step 1, pre-flight in `git-workflow.md` §1; (c) `pre-push` added; (d) CI = accepted risk R-04 (register still Pending, N-07); (e) scanner = proposal P-03 pending (DEFERRED) |
| C-23 to C-31 | CLOSED | Identifier rule explicit (no `i`); API versioning/content type/enums/pagination; lengths from spec; snippet and mechanical rules; "Not approved" list; capacity and N+1; credentials and logging; deviations table; package root and coordinates confirmed |
| A-01 pin MCP/Playwright | CLOSED | `@playwright/mcp` 0.0.83, `@playwright/test` 1.64.1, no `@latest` (the retained `npx -y` with an exact pin is acceptable) |
| A-02 E2E isolation | CLOSED+N-12 | Port 18080, temp DB, `reuseExistingServer: false`, same origin in MCP |
| A-03 reporters | CLOSED | Built-in reporters and `toHaveScreenshot` approved |
| A-04 TS vs JS | CLOSED | TypeScript, built-in transpilation, no extra package |
| A-05 visual baselines | CLOSED+N-13 | Reference environment named but not defined (which image, fonts) |
| A-06 Node pin | CLOSED | 24.22.2, `.nvmrc`, `npm ci` |
| A-07 JS unit runner | CLOSED+N-04 | `node:test` approved; the command is doubtful |
| A-08 fold Amendment | CLOSED+N-03 | No separate section in the constitution; stale references remain in other files |

### 2. Cross-document matrix (round 1 matrix re-run)

| Topic | Result in v0.2 |
|---|---|
| Definition of done command | RESOLVED in constitution, `orchestration.md`, `git-workflow.md`. Residual in `sdd-java-architect.md` §4 ("Prefer `mvn verify`", N-03) and `approach.md` §6 (`mvn test`, N-15) |
| Module layout | RESOLVED. Unmapped top-level items (N-10) |
| DB path key | RESOLVED (`SPRING_DATASOURCE_URL`, no longer circular) |
| Test levels | RESOLVED, except the Java agent's "temp file or in-memory" (N-03) |
| Commit cadence | RESOLVED (atomic on branch, squash on `main`) |
| Direct commits to `main` | RESOLVED (bootstrap exception in both) |
| PR use | RESOLVED (`git-workflow.md` §2 and §4 now state the no-PR merge path) |
| Reporters/plugins | RESOLVED |
| Version pinning | RESOLVED for MCP and Playwright. New: pins are copied into 5 documents while the constitution says "Versions live here and nowhere else" (N-05) |
| Server reuse / port | RESOLVED (18080, no 8080 leftover anywhere; 8080 remains only as the dev default) |
| Frontend unit tests | RESOLVED in text, command doubtful (N-04) |
| Envelope on 409 | RESOLVED by `details`; `frontend-testing.md` §4 rule 7 and the frontend agent §4 still list only three keys (N-03) |
| No AI attribution | CONSISTENT. Enforcement: message, identity, branch and tag names. File content and code comments are manual (N-14) |
| Lengths | RESOLVED (spec only) |
| Status codes | PARTIAL: 406 and 413 appear nowhere in the status list (N-01) |

Leftover scan you requested: `Spring Boot 3.x/3` remains in `sdd-java-architect.md` (description) and `approach.md` §2/§3; `mvn test` in `approach.md` §6; `mvn verify` (no wrapper) in `sdd-java-architect.md` §4; `@latest` only appears in prohibitions (clean); port 8080 for E2E: none (clean); "Amendment 1" as a section: none in the constitution, but `frontend-testing.md` §1 table (two rows) and `sdd-frontend-developer.md` §0 still cite it; `spring-boot-starter-web`: none (only `-webmvc`, clean); "JUnit 5": `constitution.md` Stack bullet, `sdd-java-architect.md` §5, `approach.md` §2.

### 3. New and residual findings

| ID | Sev | Location | Finding | Evidence / rule | Proposed resolution |
|---|---|---|---|---|---|
| N-01 | MAJOR | Error Handling Standards; Security Standards (Host allowlist, request limits) | The rule "One `@RestControllerAdvice` produces every error response... Controllers never build error bodies" cannot hold. The Host-allowlist rejection (400) and the 16 KiB body cap are enforced "by the filter, before parsing", and header-size failures happen in the container before Spring MVC. A `@RestControllerAdvice` does not see filter-level or container-level errors. Also: the Host rejection has no `code`; the body-cap rejection has no status or `code` (413 is absent from the status list); 406 is mentioned in API Standards but has no reserved code and is not in the status list. Result: three engineers would invent three codes, or emit a default HTML/JSON error body. | Module 3 §3 (be specific); constitution NEVER "expose stack traces... internal paths"; Error Handling bullets 4 and 5; Security bullets 3 and 4. | F1 |
| N-02 | MAJOR | Header paragraph "Referenced documents are incorporated by reference and bind as if written here"; `README.md` §1 owners | Binding rules now live in documents owned by non-Sponsor roles: `security.md` (Security reviewer edits controls and the register), `frontend-testing.md` (Frontend Developer, reviewed by the Liaison), `orchestration.md`. A control or rule can therefore be added, removed or relaxed without the Amendment Procedure, which defeats "Treat the Constitution as Code" and the ASK FIRST gate. `security.md` is also still `Draft v0.1, pending first review` while already binding. | Module 3 §5 and Summary 1 (versioned, governed); `README.md` §1. | F2 |
| N-03 | MAJOR | `.claude/agents/sdd-java-architect.md`, `.claude/agents/sdd-frontend-developer.md`, `docs/sdd/frontend-testing.md` | Drift between the constitution and the documents the coding agents actually read. Concrete, each would produce wrong code or a false audit: Java agent: description says "Spring Boot 3"; §4 "Prefer `mvn verify`" (system `mvn`, wrapper mandated); §5 integration row allows "temp file or in-memory" (constitution: never `:memory:`, which also breaks with pooled connections); §4 "`final` where practical" and "prefer immutability" (constitution: `final` unless JPA requires; entities have no public setters); §4 "never `SELECT *`" in "hand-written queries" (constitution: no `*` select list in any native SQL or JPQL under `backend/src/main`); §4 and §5 omit `details`, the reserved codes, the `@DataJpaTest` `replace = NONE` rule, Checkstyle, and the mandatory properties list; §5 "JUnit 5". Frontend agent: §0 cites "Amendment 1"; §4 Security says "no inline event handlers if a CSP is planned" (CSP is now mandatory); §4 API integration lists the envelope as three keys (no `details`, so field-level messages would be missed); §4 "`final`" not applicable. `frontend-testing.md`: §1 "Approved (Amendment 1)" twice; §1 rule 1 "Node.js (current LTS)" (pinned 24.22.2); §4 rule 7 envelope with three keys; §4 numbering has two items "10". | Module 2 §2 (context drift); Module 5 (constraint injection); constitution line 10 ("constitution wins") does not help an agent that reads its own file first. | F3 |
| N-04 | MAJOR | Executable Commands: `node --test frontend/`; Project Structure; Frontend Standards | (a) Since Node 21 positional arguments of `node --test` are file/glob patterns; I believe a bare directory is not searched recursively and errors out on Node 24. Verify. A Definition-of-Done command that fails on day one is a C-03 regression. (b) The naming convention for frontend test files is undefined (the deviations table covers only backend `.test.`/`.spec.`). (c) `frontend/` is copied wholesale to the served `static/`, so any `*.test.js` placed there would be served to the browser, widening the surface that SC-12 says is limited to "mapped `frontend/` content". | Module 3 §3.3 (exact commands); Module 4 §4. | F4 |
| N-05 | MINOR | Pinned versions table | (a) "Versions live here and nowhere else" is false: versions are copied in the Stack bullets (4.1.2, Jupiter 6.0.3), `frontend-testing.md`, both agents, and the MCP version must be repeated in the agent frontmatter (unavoidable). SC-19 diff list (`pom.xml`, `package.json`, lockfile) omits the agent frontmatter. (b) JDK row: "21.0.12 or a later 21.0.x" is a floor, not an exact pin, and the verified runtime was "OpenJDK 21.0.12.1" while the pin names Eclipse Temurin: the verified build is not the pinned distribution. (c) Only JaCoCo is marked "Verified"; the table gives no marker for Boot/BOM versions (verified per your brief), Node, `@playwright/test` and `@playwright/mcp` (not stated as verified). (d) Maven 3.9.16 and Checkstyle 14.3.0 / plugin 3.6.0 are unverified and deferred to Task 1: acceptable if the Sponsor accepts the risk (S1), but a one-hour throwaway build now is cheaper than a pin-change loop later. | Module 3 §3.1; round 1 C-01 ruling 3. | F5 |
| N-06 | MINOR | Executable Commands (last paragraph); Approved dependencies | The constitution labels `./mvnw test` "Backend unit tests only" but `*IntegrationTest` matches Surefire's default `*Test` pattern, so `test` runs everything unless the build is configured otherwise; the document defers the Surefire/Failsafe choice to `plan.md` yet Failsafe is not in the parenthesized approved plugin list (it is inherited from the Boot parent, so is it approved or not?). Also `node --test` is missing from Definition of Done item 1 for frontend tasks. | Module 3 §2 (ASK FIRST must be unambiguous). | F6 |
| N-07 | MINOR | Security Standards "Accepted risk... recorded in `security.md`"; Frontend Standards "accepted risk, recorded in `security.md`"; DoD | The statements are not true yet: R-01, R-03, R-04 are "Proposed / Pending" and SC-26 is `ACCEPTED_PENDING`. The Sponsor accepted D7 and D10 in the round-1 answers, but the register's Sponsor column is the Sponsor's to sign (`security.md` §6). | Module 5 (humans own risk); evidence over claims. | F7 |
| N-08 | MINOR | Code Style "Enforcement" | The enforcement sentence covers Java only. JavaScript/CSS/HTML rules (3-character identifiers, 50/300 lines, 2-space indent, JSDoc on exports) have no tool (no linter is approved), and the Java rules "DTOs are records", "no public setters", "fields final" are not Checkstyle checks either. "Rules that no tool checks (SRP, readability)" understates this. Round 1 ruling 1: binary and verifiable. | Module 3 Summary 2. | F8 |
| N-09 | MINOR | NEVER "`innerHTML`/`outerHTML`/`insertAdjacentHTML` with data" | `security.md` SC-09 and the frontend agent self-audit forbid them outright and verify by grep. "With data" is looser and cannot be grep-verified (`innerHTML = ''` would be flagged). | Round 1 ruling 1. | Drop "with data": forbid the three sinks outright (use `replaceChildren()` to clear). |
| N-10 | MINOR | Project Structure | "A new top-level directory is an architectural change (ASK FIRST)", but existing top-level items are unmapped: `.claude/`, `CLAUDE.md`, `.nvmrc` (location of the file is never stated), `.gitignore`, `README.md`; runtime `backend/data/` (default DB path is relative to the working directory of `./mvnw spring-boot:run`). | Module 3 §3.2. | Add them to the map; state `.nvmrc` at the repository root. |
| N-11 | MINOR | `orchestration.md`, `agent-protocol.md`, `README.md`, `git-workflow.md` | (a) `orchestration.md` §2.3 "no phase advances without Liaison APPROVED and Sponsor approval" omits the Security PASS; the Phase 1 line in §3 omits Security. (b) Escalation after "two review rounds" (`orchestration.md` §5, `agent-protocol.md` §6) vs "max 3 rounds" (`agent-protocol.md` §4, `orchestration.md` §2.4). (c) The pre-dispatch claim check `git ls-remote --heads origin 'feature/*' 'fix/*' 'test/*'` (`README.md` §4, `orchestration.md` §3.1) misses task branches of type `chore/` (the example `chore/task-01-maven-scaffold` is a task), `refactor/`, `spec/`. (d) `orchestration.md` §6 pre-flight lacks the hook executable bit and identity depth that `git-workflow.md` §1 requires. (e) Force push: constitution NEVER names `main` only; `git-workflow.md` §6 forbids any shared branch (the constitution wins, which relaxes the stricter rule). | `README.md` §4; Module 5 (HITL gates). | F9 |
| N-12 | MINOR | E2E isolation; `security.md` §5 "Runtime" | Port 18080 is both the E2E port (`reuseExistingServer: false`) and the port where the Security reviewer starts the app for runtime checks. Orchestration runs Security audits in parallel with other work, so two processes can race for 18080 and either the E2E run fails to start or attaches to the wrong instance. | `frontend-testing.md` §3; `orchestration.md` §3.5 and §4. | Reserve 18081 for Security runtime checks (state it in the constitution E2E isolation bullet) or require serialization. Prefer one literal origin string (`http://127.0.0.1:18080`) in Playwright and MCP: the app binds IPv4 only and `localhost` can resolve to `::1` first. |
| N-13 | MINOR | various wording | (a) Audit grep `select *` misses `select t.*` and multi-line selects: use a regex such as `select\s+([a-z_]+\.)?\*` (case-insensitive, multiline). (b) Unicode normalization form not named (NFC vs NFKC): say "as defined in `spec.md`; Java `java.text.Normalizer`" and route the form to the PM. (c) `maximum-pool-size: 1` is listed as mandatory in Configuration but "unless `plan.md` justifies otherwise" in Persistence: pick one (recommend mandatory 1, change is ASK FIRST). (d) `Instant.now()` ban should read "any wall-clock call (`Instant.now()`, `LocalDate.now()`, `System.currentTimeMillis()`)". (e) "Not approved: any dependency-vulnerability or static-analysis plugin" collides with the approved Checkstyle (a static analyzer): say "security static-analysis". (f) The Code Style snippet declares `rating` and never uses it; use a body that uses all parameters. (g) A-05: define the visual-baseline reference environment (image or OS, fonts) in the plan, and state it is outside the constitution until then. (h) `.gitignore` entry `data/` is unanchored and would silently ignore any directory named `data` (for example test fixtures); use `/data/` and `/backend/data/`. | Module 3 Summary 2. | F10 |
| N-14 | MINOR | `.githooks/`, `git-workflow.md` §1 | (a) Two divergent pattern lists: `commit-msg` has `ai[- ](generated\|assisted\|agent)` and `generated (with\|by)`, `pre-push` message scan lacks the first. Use one shared pattern file sourced by both. (b) Neither hook scans file content, but the rule covers code comments and files: add the Layer 1b/Security audit command (scan added lines of `git diff main...HEAD` with the same pattern, excluding documentation that must quote the rule). (c) Conventional Commit format (`type(scope): subject [Task-N]`) is not checked by any hook: optional, the Liaison audits it. (d) I could not verify the executable bit of both hooks (no shell). Orchestrator pre-flight: `git ls-files -s .githooks` must show mode `100755`, then run each hook once on a sample bad message and a sample bad ref. (e) A fresh clone has no `core.hooksPath`: documented as step 1 of setup, accepted. | Module 3 §2 (never commit secrets) by analogy; Layer 1b. | Optional improvements except (d), which is a pre-flight to run now. |
| N-15 | MINOR | `docs/sdd/approach.md` | Sponsor-owned, not incorporated by reference, but it still says Spring Boot 3.x (twice), `mvn test`, JUnit 5, `SELECT *` "scoped to hand-written queries", a package layout (`api/`) that differs from the constitution map (`controller/`, `dto/`), and one commit per task `- ACCEPTED`. A reader following it would contradict v0.2. | `README.md` §3.1. | Add a header line "Historical. Superseded by constitution v0.2 where they differ" (Sponsor's file). |

### 4. Remaining decisions for the Sponsor

| # | Decision | Options | Recommendation |
|---|---|---|---|
| S1 | Unverified pins at approval time: Maven 3.9.16 / wrapper 3.3.4, Checkstyle 14.3.0 with plugin 3.6.0, and Node / `@playwright/test` / `@playwright/mcp` if they were not part of the throwaway build | (a) approve as is, Task 1 verifies or re-approves; (b) verify now in the throwaway project and mark them Verified; (c) pin to the verified Maven 3.9.11 now | (b), or (c) for Maven. If (a), record it in writing in the changelog as an accepted risk |
| S2 | Risk register sign-off at the gate: R-01 (unencrypted file), R-03 (Chromium-only verification), R-04 (no CI), R-02 and R-06 as proposed | Accept / change | Accept at the Phase 1 gate in the register's Sponsor column (you alone edit it) so the constitution statements become true (N-07) |
| S3 | Governance rule for referenced documents (F2) | Adopt the text in F2 / keep as is | Adopt: otherwise a non-Sponsor role can change a binding rule |
| S4 | Approve `maven-failsafe-plugin` (inherited from the Boot parent) and the Surefire/Failsafe split in F6 | (a) Surefire only, `*Test` and `*IntegrationTest` both run in `test`; (b) Failsafe for `*IntegrationTest` in `verify` | (b): keeps `./mvnw test` fast and meaningful as "unit and slice only"; it needs JaCoCo merge configuration that Task 1 proves |
| S5 | Security proposals P-01 to P-04 (Maven vulnerability scanner, static analyzer, secrets scanner, axe) | Security reviewer owns the recommendation | Not a Liaison matter; decide with the Security verdict |

Routed to the PM (unchanged from round 1; Phase 2 Clarify Loop): non-UUID id behavior, invalid `status` filter, "newest first" tie-break, whitespace and trimming semantics, normalization form, enum case on input, the `details` shape for 409 and validation, update the BRD timestamp examples from seconds to the fixed millisecond format (D5), maximum collection size (SC-17), and open points 1 to 10 of my brief.

## 5. Proposed document changes (exact text; I do not apply them)

The orchestrator applies these on the Sponsor's instruction. Files are named per change.

### F1 (N-01) `constitution.md`, Error Handling Standards and Security Standards
```diff
-- Reserved framework-level codes (a spec code with the same meaning takes precedence): `VALIDATION_ERROR` 400, `MALFORMED_REQUEST` 400 (...), `NOT_FOUND` 404 (unknown route or resource), `METHOD_NOT_ALLOWED` 405, `UNSUPPORTED_MEDIA_TYPE` 415, `INTERNAL_ERROR` 500.
+- Reserved framework-level codes (a spec code with the same meaning takes precedence): `VALIDATION_ERROR` 400, `MALFORMED_REQUEST` 400 (unreadable JSON, unknown field, wrong type, invalid path or query value), `INVALID_HOST` 400 (Host allowlist), `NOT_FOUND` 404 (unknown route or resource), `METHOD_NOT_ALLOWED` 405, `NOT_ACCEPTABLE` 406, `PAYLOAD_TOO_LARGE` 413, `UNSUPPORTED_MEDIA_TYPE` 415, `INTERNAL_ERROR` 500.
-- One `@RestControllerAdvice` produces every error response, including framework exceptions (...) and the catch-all for `Exception`. The default Spring error page and body are disabled. Controllers never build error bodies.
+- Every error body is built by one error-body factory. The `@RestControllerAdvice` uses it for controller and framework exceptions and the catch-all for `Exception`; the servlet filter (Host allowlist, body cap) and the container error path (failures raised before Spring MVC, such as an oversized header) use the same factory. No response carries a default Spring or Tomcat error page or body. A security integration test covers each of these paths. Controllers never build error bodies.
-Status codes: 200 success with data; 201 created; 400 validation or malformed request; 404 not found; 405 method not allowed; 409 conflict (...); 415 unsupported media type; 500 unexpected error (generic message only).
+Status codes: 200 success with data; 201 created; 400 validation, malformed request or invalid Host; 404 not found; 405 method not allowed; 406 not acceptable; 409 conflict (duplicate book, as `spec.md` defines); 413 payload too large; 415 unsupported media type; 500 unexpected error (generic message only).
```
Security Standards: "rejected with 400" becomes "rejected with 400 `INVALID_HOST`"; "body capped at 16 KiB (by the filter, before parsing)" becomes "...(by the filter, before parsing; 413 `PAYLOAD_TOO_LARGE`)". The mechanism (filter ordering, `ErrorController`) is for `plan.md`.

### F2 (N-02) `constitution.md`, header paragraph
```diff
 Referenced documents are incorporated by reference and bind as if written here: `docs/sdd/git-workflow.md`, ... If any of them conflicts with this document, this document wins.
+Owners of referenced documents may change wording, examples, evidence and status columns. Any change that adds, removes or relaxes a binding rule (an ALWAYS / ASK FIRST / NEVER item, a pinned version, a gate, an `SC-NN` control, a risk acceptance) is an amendment and follows the Amendment Procedure. Only the Sponsor changes risk-acceptance columns. A referenced document that is still `Draft` binds from the Sponsor's approval of the phase gate in which it is first reviewed.
```
Also add to the Amendment Procedure step 1: "the Liaison and the Security reviewer review it" already present; add "the owner of the referenced document proposes, never applies, a rule change".

### F3 (N-03) agent and process documents (one pass, owners: Sponsor applies via orchestrator; the Frontend Developer owns `frontend-testing.md` but rule 2 of `README.md` §3 requires the owner to change it)
`sdd-java-architect.md`:
- description: "Spring Boot 3" becomes "Spring Boot 4.1.2 (per the constitution pin table)".
- §4 Java: replace "`final` where practical" / "prefer immutability" with "fields `final` unless JPA requires otherwise; entities have no public setters unless `plan.md` justifies them".
- §4 Persistence: replace "Explicit column lists in hand-written queries (never `SELECT *`)" with "No native SQL or JPQL select list with `*` anywhere under `backend/src/main`; native queries only when `plan.md` justifies them".
- §4 Integrated-environment readiness: "Prefer `mvn verify`" becomes "`./mvnw verify` from `backend/` is the single entry point".
- §4 API: add "error body per the constitution (optional `details`, reserved codes, fixed millisecond timestamp)".
- §5 table: "JUnit 5" becomes "JUnit (Jupiter 6)"; Integration row "(temp file or in-memory)" becomes "(temporary file database; never `:memory:`)"; Slice row add "`@DataJpaTest` declares `@AutoConfigureTestDatabase(replace = NONE)`".
- §3 step 6 and §8: "`./mvnw verify`" becomes "`cd backend && ./mvnw verify` (includes Checkstyle)".

`sdd-frontend-developer.md`:
- §0 constitution row: remove "and Amendment 1"; say "Frontend Standards, Security, API, Error Handling, Testing sections".
- §4 Security: "no inline event handlers if a CSP is planned" becomes "no inline event handlers, no inline script or style (the CSP is mandatory)"; add "`innerHTML`, `outerHTML`, `insertAdjacentHTML` are forbidden outright; clear with `replaceChildren()`".
- §4 API integration: "the error envelope (`error`, `code`, `timestamp`)" becomes "(`error`, `code`, `timestamp`, and optional `details` as `spec.md` defines it); use `details[].field` to attach messages to inputs".

`frontend-testing.md`:
- §1 table: "Approved (Amendment 1)" (two rows) becomes "Approved (constitution v0.2)".
- §1 rule 1: "Node.js (current LTS)" becomes "Node.js 24.22.2 (see the constitution pin table)".
- §4 rule 7: add "and the optional `details` member".
- §4: renumber the second "10" as 11 and the following item as 12.

### F4 (N-04) `constitution.md`, Executable Commands, Project Structure, Frontend Standards
```diff
-- Frontend logic unit tests: `node --test frontend/` (DOM-free modules only)
+- Frontend logic unit tests: `node --test "frontend/**/*.test.js"` (DOM-free modules only; verify the glob form on Node 24.22.2 in the throwaway project before the gate)
```
Project Structure note: "Frontend unit tests are named `*.test.js` and live next to their module. The Maven `<resources>` entry for `frontend/` excludes `**/*.test.js`, so test files are never served." Add to the Deviations table: "Test file naming `.test.`/`.spec.` | `*Test`/`*IntegrationTest` (backend); `*.test.js` (frontend unit); `*.spec.ts` (E2E) | Language conventions".

### F5 (N-05) `constitution.md`, pinned versions table
- Add a column "Verification" with values `Verified (throwaway build 2026-10-29)` or `Unverified: Task 1` per row, filled by the orchestrator with the real facts (backend rows verified per the Sponsor; Maven 3.9.16 and Checkstyle unverified; Node, Playwright and MCP as the Sponsor confirms).
- JDK row: "Eclipse Temurin 21.0.12 or a later 21.0.x patch (floor, not exact)" and name the distribution that was actually used in the verification build; if it was not Temurin, either verify on Temurin or change the pin to the distribution used.
- Replace "Versions live here and nowhere else. The build files must match this table." with "This table is authoritative. Other documents may repeat a version only for convenience and must match it; the Security reviewer's pin check (SC-19) also compares agent frontmatter."

### F6 (N-06) `constitution.md`, Executable Commands and Approved dependencies (subject to S4)
```diff
-- Backend unit tests only: `cd backend && ./mvnw test`
+- Backend unit and slice tests (no `*IntegrationTest`): `cd backend && ./mvnw test`
...
-The plan states whether `*IntegrationTest` runs under Surefire in `test` or Failsafe in `verify`. The JaCoCo `check` goal is bound to `verify`.
+Surefire runs `*Test` (excluding `*IntegrationTest`) in `test`; Failsafe runs `*IntegrationTest` in `verify`; JaCoCo merges both execution files and its `check` goal is bound to `verify`. Task 1 proves the configuration.
-- Build plugins: those inherited from the Spring Boot parent (compiler, resources, Surefire, Spring Boot Maven plugin), ...
+- Build plugins: those inherited from the Spring Boot parent (compiler, resources, jar, Surefire, Failsafe, Spring Boot Maven plugin), ...
```
Definition of Done item 1: add "`node --test` green when the task touches frontend logic".

### F7 (N-07) `constitution.md`
Replace "recorded in `security.md`" in the Security "Accepted risk" bullet and the Frontend Standards browser bullet with "tracked in the risk register of `security.md` (R-01, R-03) and effective once the Sponsor signs it". Orchestrator: after S2, record the Sponsor's decision in `security.md` only through the Sponsor.

### F8 (N-08) `constitution.md`, Code Style "Enforcement"
```diff
-Rules that no tool checks (SRP, readability) are review checklist items.
+Rules that no tool checks are review checklist items verified by the Liaison audit: SRP, readability, "DTOs are records", "no public setters", "fields final", and every JavaScript, CSS and HTML rule (identifier length, 50/300 lines via `wc -l`, indentation, JSDoc on exports). No JavaScript linter is approved.
```

### F9 (N-09, N-10, N-11) smaller edits
- NEVER bullet: "`innerHTML`/`outerHTML`/`insertAdjacentHTML` with data" becomes "`innerHTML`, `outerHTML`, `insertAdjacentHTML`".
- Project Structure: add `.claude/` (agent definitions), `CLAUDE.md`, `README.md`, `.gitignore`, `.nvmrc` at the root; note the runtime directory `backend/data/` is git-ignored.
- `orchestration.md` §2.3: "no phase advances without Liaison APPROVED, Security PASS and Sponsor approval"; §3 Phase 1 line: "Sponsor + Liaison and Security (review)"; §5 last row and `agent-protocol.md` §6: "Two review rounds without convergence" becomes "Disagreement after two rounds is escalated; three rounds is the hard limit".
- `README.md` §4 and `orchestration.md` §3.1: claim check becomes `git ls-remote --heads origin '*/task-*'` (all types).
- `orchestration.md` §6 "Before invoking an implementer": add "hook executable bit (`git ls-files -s .githooks` shows 100755)".
- Constitution NEVER force-push bullet: "...force push (including `--force-with-lease`) to `main` or any shared branch...".

### F10 (N-13 to N-15) wording polish
As listed in N-13 (a) to (h), N-14 and N-15. `.gitignore` is outside my edit rights: replace `data/` by `/data/` and add `/backend/data/`.

## Open items / decisions needed

Gating for the Liaison verdict to become final `APPROVED`: F1, F2, F3, F4 applied (the four MAJORs), and S1 and S3 answered. F5 to F10 and S2, S4 may ride in the same revision or be logged. The orchestrator pre-flight in N-14(d) (hook mode and a dry run) and the Node glob check in N-04 are the only verification tasks.

Liaison rulings on doctrine (for the record, no Sponsor input needed):
1. A rule that names a component that cannot enforce it (an advice for filter errors, a Java linter for JavaScript) is wrong text, not a stylistic issue (Module 3 Summary 2).
2. Documents incorporated by reference carry the authority of the constitution, so changes to their binding rules must follow the constitution's amendment path (Module 3 §5).
3. A document that agents read first (agent definitions) must not contradict the constitution; the fix is to the agent file, not a reliance on "constitution wins" (Module 2 §2, context drift).

Reminders unchanged: exercise phase times exceed the stated 6 to 8 hours (open point 10).

## Requested response

1. Orchestrator: apply F1 to F4 (and F5 to F10 as decided) on the `docs/phase-1-constitution-review` branch, bump the changelog line (still v0.2 until the Sponsor approves; then 1.0 per the Amendment Procedure), and update the `status.md` Phase 1 row (Liaison: `APPROVED WITH CHANGES` round 2).
2. Orchestrator: run the pre-flight checks in N-14(d) and the Node 24 glob check in N-04 and report the real output.
3. Sponsor: answer S1 to S5; sign the risk register (S2) at the gate.
4. Security reviewer: first review of the constitution and `security.md` in parallel; the gate needs its verdict.
5. Liaison (round 3, only if requested): re-verify the diff of F1 to F4 and issue final `APPROVED`.
