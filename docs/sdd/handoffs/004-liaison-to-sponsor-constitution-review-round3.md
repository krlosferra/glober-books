# Handoff 004: Constitution review, round 3 (Phase 1, Mode A)
- From: liaison
- To: sponsor
- Phase: 1   Round: 3 of 3 (hard limit)
- Artifacts: `docs/sdd/constitution.md` (v0.3 draft), `docs/sdd/git-workflow.md`, `docs/sdd/frontend-testing.md`, `docs/sdd/orchestration.md`, `docs/sdd/agent-protocol.md`, `docs/sdd/README.md`, `docs/sdd/status.md`, `docs/sdd/security.md` (gate consistency only), `docs/sdd/approach.md` (header), `.claude/agents/sdd-java-architect.md`, `.claude/agents/sdd-frontend-developer.md`, `.claude/settings.json`, `.claude/hooks/guard-bash.sh`, `.githooks/*`, `.github/*`, `.gitignore`, `.gitattributes`, `.nvmrc`, `CLAUDE.md`; baselines: handoffs 002 and 003; branch `docs/phase-1-constitution-review`, commit `b964787`
- Type: REVIEW_REPORT

Mode: A (Constitution review) with the Layer 1b hygiene checks that apply to Phase 1.

## Summary

**Liaison verdict: APPROVED WITH CHANGES.** 0 BLOCKER, 4 MAJOR, 12 MINOR (R3-01 to R3-16). All of F1 to F10 and S1 to S5 are applied as specified or with a stated, acceptable difference. Of the 15 N-findings, 11 are fully closed and 4 have a residual that is tracked below (N-03, N-04, N-05, N-13). The 4 MAJORs are text-level (one wrong cross-reference, one process contradiction, two stale agent rules, one unverifiable ban list). No new Sponsor business decision is needed for them.

This is the last Liaison round. If the orchestrator applies the exact text in section 5 for R3-01 to R3-04 (and the MINORs at its discretion), I regard the Liaison verdict as final `APPROVED` without a round 4; the Sponsor can confirm with the grep checks in section 6. **Gates still outstanding, not mine:** the Security round 2 verdict and the Sponsor gate (`agent-protocol.md` section 5). I did not review security substance.

Not independently verified (no shell, no network in this review): the Node 24 glob result, every version pin, the executable bit of `.githooks/*` and `.claude/hooks/guard-bash.sh`, the Surefire/Failsafe/JaCoCo configuration, Boot 4 property names, and the dependency tree. These are stated as the orchestrator's facts or as Task 1 proofs; I treat them as claims until the evidence is attached.

## 1. F1 to F10 and S1 to S5: applied?

| Item | Status | Evidence / difference |
|---|---|---|
| F1 error bodies | APPLIED, one defect | Reserved codes now include `INVALID_HOST`, `FORBIDDEN`, `NOT_ACCEPTABLE`, `PAYLOAD_TOO_LARGE`; status list has 403, 406, 413; one error-body factory used by advice, filter and container path (constitution, Error Handling). Defect: the residual-risk reference "R-09 family" is wrong (R3-03) |
| F2 referenced-document governance | APPLIED verbatim | Header paragraph; Amendment Procedure step 1 says the owner proposes, never applies |
| F3 agent and process files | PARTIAL | Java agent: description, `final`, `select *`, `./mvnw verify`, JUnit Jupiter 6, never `:memory:`, `replace = NONE`, section 3 and 8 all applied. Not applied: Java agent section 4 Architecture still says one advice "builds every error body" with three keys; frontend agent section 5 still says `node --test frontend/`. Frontend agent section 0, section 4 (CSP, sinks, `details`) applied. `frontend-testing.md` Amendment 1, Node pin, rule 7, numbering applied (R3-02) |
| F4 `node --test` glob | APPLIED | Quoted glob in Executable Commands; Maven `<resources>` excludes `**/*.test.js`, `**/*.map`, dotfiles, filtering off; Deviations row added. Residual: DoD item 1 says bare `node --test`; frontend agent (R3-02, R3-05) |
| F5 pin table | APPLIED, deliberate difference | Verification column added; authoritative-table sentence added. JDK row is "21.0.12 or later 21.0.x, any OpenJDK-based distribution, Temurin recommended", verified on OpenJDK 21.0.12.1, Temurin unverified. I accept this: the verified build and the pin now agree, and the row says what is not verified. Residual: SC-19 text in `security.md` does not yet list agent definitions (R3-06) |
| F6 Surefire/Failsafe | APPLIED | `test` excludes `*IntegrationTest`; Failsafe in `verify`; JaCoCo merge and `check` in `verify`; Failsafe in the plugin list; DoD names `node --test` |
| F7 accepted-risk wording | APPLIED | "tracked in the risk register ... effective once the Sponsor signs it"; the register now shows R-01 to R-12 signed 2026-10-30 (`security.md` section 6), so the statements are true |
| F8 enforcement sentence | APPLIED | Review-checklist list present. My own wording "50 and 300 lines via `wc -l`" was imprecise: `wc -l` cannot measure function length (R3-10) |
| F9 smaller edits | APPLIED | NEVER sinks without "with data"; structure map adds `.claude/`, `.github/`, `.githooks/`, `CLAUDE.md`, `.nvmrc`; `orchestration.md` 2.3 and Phase 1 line include Security; two-rounds wording aligned; claim check `'*/task-*'` in `README.md` and `orchestration.md`; exec-bit pre-flight; force-push covers `main` or any shared branch |
| F10 polish (a to h) | MOSTLY | (a) audit regex only partly (R3-10); (b) NFKC named, deliberate: better than routing it; (c) pool size mandatory 1; (d) wall-clock list non-exhaustive (R3-10); (e) wording fixed; (f) snippet uses `rating`; (g) reference environment named, fonts undefined, acceptable until `plan.md`; (h) `/data/` and `/backend/data/` anchored in `.gitignore` |
| S1 unverified pins | ACCEPTED as option (a), not written in the changelog | Maven 3.9.16, Checkstyle 14.3.0 / plugin 3.6.0, Playwright, MCP are marked Unverified in the table. The changelog should say so (R3-16) |
| S2 risk register | DONE | R-01 to R-12 have Sponsor entries dated 2026-10-30 |
| S3 governance | DONE | See F2 |
| S4 Failsafe split | DONE | See F6 |
| S5 proposals P-01 to P-04 | DONE in the constitution | SpotBugs + Find Security Bugs approved for Task 1, `@axe-core/playwright` for the first accessibility task, Dependabot as a Sponsor setting. Residuals in R3-09 |

## 2. Closure of the 15 N-findings

| ID | Status | Note |
|---|---|---|
| N-01 | CLOSED | Except the wrong R-09 reference (R3-03) |
| N-02 | CLOSED | `README.md` does not repeat the amendment rule for owners (R3-14, optional) |
| N-03 | RESIDUAL | R3-02 (two stale agent rules), R3-11 |
| N-04 | RESIDUAL | R3-02 (frontend agent), R3-05 (DoD) |
| N-05 | RESIDUAL | R3-06 (SC-19 text), R3-09 |
| N-06 | CLOSED | |
| N-07 | CLOSED | Register signed |
| N-08 | CLOSED | Wording refinement in R3-10 |
| N-09 | CLOSED | |
| N-10 | CLOSED | Planned directories and the absent root `README.md`: R3-08 |
| N-11 | CLOSED | New contradiction in the same area: R3-01 |
| N-12 | CLOSED in the constitution | 18080 (E2E), 18081 (Security runtime), `127.0.0.1` literal everywhere in agent, `frontend-testing.md` and constitution; `security.md` section 5 still says "a port other than 18080" (R3-06) |
| N-13 | RESIDUAL | R3-10 |
| N-14 | PARTIAL | (a) done: one `attribution-pattern` file read by both hooks, `guard-bash.sh` and `hygiene.sh`. (b), (c) not done (optional): R3-15. (d) hook modes: `security.md` section 10 still says "to be confirmed"; not verified by me. (e) accepted |
| N-15 | CLOSED | Historical header on `approach.md` |

## 3. Stale-term scan

| Term | Result |
|---|---|
| Spring Boot 3.x | Only in `approach.md` body (historical header present). Clean elsewhere, including the Java agent description |
| `mvn test`, `mvn verify` without wrapper | Only `approach.md` line 93. Clean elsewhere |
| `@latest` | Only in prohibitions (constitution NEVER, `frontend-testing.md`, security agent text) and the frontend agent pin sentence. Clean |
| "Amendment 1" as a section | Only in the constitution changelog line 0.2 (history). Clean in `frontend-testing.md` and the frontend agent |
| JUnit 5 | Only `approach.md` (historical) |
| `spring-boot-starter-web` | None (only `-webmvc`) |
| `localhost` origins | None stale. `localhost` appears only in the Host/Origin allowlist rule and as a security-test case. Agent MCP origin, `baseURL` and `frontend-testing.md` use `http://127.0.0.1:18080` |
| `node --test frontend/` | STALE in `sdd-frontend-developer.md` line 109 (R3-02) |
| `:memory:` | Only prohibitions and `security.md` tests. Java agent now says never |
| "PR only if the Sponsor asked for one" | STALE in `orchestration.md` section 3 step 7 (R3-01) |

## 4. Findings

| ID | Sev | Location | Finding | Evidence / rule | Resolution |
|---|---|---|---|---|---|
| R3-01 | MAJOR | `orchestration.md` section 3.4 step 7 | Contradicts the new rule. It says "merges following git-workflow section 4 (PR only if the Sponsor asked for one)". The constitution (Workflow Rules, Security "Repository integrity"), `git-workflow.md` sections 2 and 4 and `CLAUDE.md` line 31 say every merge goes through a pull request and the Sponsor's gate approval is the request. An orchestrator following this file would merge without a PR and skip the required `hygiene` check and CODEOWNERS review. `CLAUDE.md`, `git-workflow.md` and the Java agent ("do not open a pull request unless asked" is for the implementer, consistent) are fine | Constitution line 266; Module 3 section 5 (one wording) | F-R3-01 |
| R3-02 | MAJOR | `sdd-frontend-developer.md` line 109; `sdd-java-architect.md` line 74 | (a) Frontend agent still instructs `node --test frontend/`, the form the orchestrator found failing on Node 24. An agent that reads its own file first would run a failing DoD command (N-04 regression). (b) Java agent Architecture: "A single global exception handler (`@RestControllerAdvice`) builds every error body ... (`error`, `code`, `timestamp`)". The constitution now says the advice, the servlet filter and the container error path all use one factory, and `details` is optional. An implementer following the agent would put the Host and CSRF rejections in the advice, which cannot see them (original N-01) | Module 2 section 2 (context drift); round 2 ruling 3 | F-R3-02 |
| R3-03 | MAJOR | Constitution, Error Handling bullet 6 ("R-09 family in `security.md`") and bullet 5 ("No response carries a default Spring or Tomcat error page") | The cited residual risk does not exist. In the register R-09 is "pattern-based AI-attribution hooks are incomplete". No R entry covers protocol-level errors that Tomcat answers before application code. The accepted-risk chain is therefore broken (the Sponsor signed nothing about it), and bullet 5 is absolute while bullet 6 admits an exception | Constitution line 189 ("only the Sponsor may accept a residual risk, in writing, in the risk register"); SEC-F-009 asked for this residual to be defined | F-R3-03 (Security reviewer adds R-13, Sponsor signs; constitution cites it) |
| R3-04 | MAJOR | Constitution, Approved dependencies "Banned in the runtime classpath" | A rule that cannot be checked as written: "any HTTP client, any XML or other data-format binding" is not an enumerable list, and `dependency:tree` cannot test a category. Two bans may also be unsatisfiable on day one: `commons-logging` (Spring Framework 7 may bring it transitively; unverified) and an XML binding (Hibernate ORM has historically pulled a JAXB runtime; unverified). The throwaway build's `dependency:tree` is the evidence and was not reported. If these appear, Task 1 would fail its own gate | Module 3 section 2 and Summary 2 (verifiable rules) | F-R3-04 |
| R3-05 | MINOR | Definition of Done item 1 | Says "`node --test` green", bare. Bare `node --test` uses Node's default discovery and is not the command that was verified | Module 3 section 3.3 | Use "the frontend logic unit test command in Executable Commands" |
| R3-06 | MINOR | `security.md` header, SC-19, section 5 "Runtime"; `frontend-testing.md` header | Owner-side drift: SC-19 verification still lists only `pom.xml`, `package.json`, lockfile, but the constitution relies on it to compare agent definitions too; section 5 says "a port other than 18080" instead of 18081; header says Version 0.2 / 2026-10-29 while the changelog is 0.3 / 2026-10-30; `frontend-testing.md` Status still says "constitution v0.2" while its table says v0.3 | Constitution Pinned versions intro and E2E isolation | Owners edit wording (no amendment needed): Security reviewer, Frontend Developer |
| R3-07 | MINOR | Constitution ALWAYS ("Update `progress.md` in the task's branch"), DoD 4, NEVER scope; `README.md` section 1; Java agent section 0 | Who writes `progress.md`? README: the Orchestrator (Liaison appends audit notes). Java agent map: Edit "no", yet its self-audit says "progress.md entries updated". Constitution: part of every task's scope | Round 1 C-18 (single wording) | Say "the orchestrator updates `progress.md` in the task's branch before the pull request; implementers do not edit it" and align the two agent self-audits |
| R3-08 | MINOR | Constitution Project Structure | Mixes existing and planned entries. Root `README.md` does not exist; `backend/`, `frontend/`, `e2e/` are planned. A reader cannot tell which paths are claims | Module 3 section 3.2 | Add "Planned layout. Directories and files marked in the scaffold tasks are created by the first task that needs them; `README.md` is created at the end of Phase 4" or remove `README.md` |
| R3-09 | MINOR | Approved dependencies | (a) Checkstyle 14.3.0 is pinned with plugin 3.6.0, which normally needs the Checkstyle version supplied as a `<dependency>` of the plugin; `com.puppycrawl.tools:checkstyle` is not named as approved. (b) SpotBugs/Find Security Bugs and `@axe-core/playwright` are approved without a pin; introducing the pin changes the table, which is an amendment. (c) The `verify` description omits SpotBugs; if approved it must say whether it runs in `verify` and its failure threshold, or it is dead text | Module 3 section 2 | (a) add "(with `com.puppycrawl.tools:checkstyle` as the plugin dependency)"; (b) add "the task that introduces them adds the verified pin to the table (an amendment)"; (c) "SpotBugs, once added, runs in `verify` and fails on its configured threshold" |
| R3-10 | MINOR | Code Style Enforcement; NEVER `select *`; Persistence time rule | (a) "50 and 300 lines via `wc -l`": `wc -l` measures file length only; JavaScript function length has no tool. (b) The `select *` audit as described misses `select t.*` and multi-line selects (Security's regex `select\s+([a-z_]+\.)?\*` is better and is in `security.md` section 5). (c) Review-only list omits "no dead code", "no N+1 / no query in a loop". (d) The wall-clock ban lists four calls; `LocalDate.now()`, `ZonedDateTime.now()`, `OffsetDateTime.now()`, `Clock.systemUTC()` are unlisted. (e) `-Xlint:all -Werror` was not reported as proven on the Boot 4 stack (annotation processors and classfile warnings can fail `-Werror`) | Round 2 N-08, N-13 | See F-R3-10 |
| R3-11 | MINOR | `frontend-testing.md` section 3 "Cloud-session note" | Tells the agent to point `launchOptions.executablePath` at a preinstalled Chromium and not reinstall. The constitution pins "the build installed by the pinned `@playwright/test`" and ties visual baselines to the pinned Chromium; R-10 says the preinstalled build is unverified. Evidence from an unpinned browser is not gate evidence | Constitution Pinned versions (Chromium row), Testing E2E isolation | State that runs on a preinstalled Chromium are exploratory only, not Definition-of-Done evidence |
| R3-12 | MINOR | Constitution: all "Task 1 proves ..." sentences | Task 1 does not exist yet. The constitution loads it with: Failsafe/JaCoCo merge, 18 mandatory-property tests, the Checkstyle pin pair, SpotBugs pin, wrapper checksum, `-Werror`, banned-dependency check, Playwright run. The Playwright pin ("not yet run: Task 1") and the MCP ("first frontend task") are in different tasks, and a backend scaffold cannot run Playwright | Module 5, Tasks phase (dependency review) | Add one sentence: "Phase 3 places each 'Task 1 proves' item in the acceptance criteria of the scaffold task that can verify it (E2E and MCP items in the first frontend task); the Liaison checks this at the Phase 3 gate" |
| R3-13 | MINOR | Constitution AI-agent security; `frontend-testing.md` section 5; `.claude/settings.json` | (a) The exploration procedure is undefined: the MCP is limited to origin `127.0.0.1:18080` and "the temporary E2E database only", but the temporary database and the 18080 server exist only while the Playwright run lasts. How the agent starts the app for exploration (command, temp directory, teardown) is not stated. (b) "denies the unsafe tools": settings deny 4 tools (`browser_run_code_unsafe`, `browser_evaluate`, `browser_file_upload`, `browser_tabs`) plus `Read(./.env*)`; the rest are excluded only by the allowlist, and Bash can still `cat .env`. Claim and mechanism differ. (c) The 26-name allowlist includes names that I cannot verify for 0.0.83 (for example `browser_network_request`); the frontend agent already says to re-derive at each pin change | Module 3 section 3.3 | (a) `plan.md` or `frontend-testing.md` section 5 defines the exploration start command; (b) word the constitution as "denies the listed unsafe tools by name and `Read` of `.env*`; the allowlist is the primary control"; (c) first frontend task lists the tool names from the installed version. Security reviewer owns (b) and (c) |
| R3-14 | MINOR | `.github/CODEOWNERS`; constitution header | (a) CODEOWNERS does not list `docs/sdd/orchestration.md` or `docs/sdd/frontend-testing.md`, although the header makes them binding and a rule change there is an amendment. (b) Every protected path has one owner, `@krlosferra`, who is also the author of every pull request: GitHub does not let an author approve their own PR, so "CODEOWNERS review required" cannot be satisfied by a solo maintainer unless the Sponsor is a bypass actor or a second owner exists. This is a Sponsor setting (see section 7) | Module 3 section 5 | (a) add the two paths; (b) Sponsor decision |
| R3-15 | MINOR | `git-workflow.md` section 1; `orchestration.md` section 6 | (a) Attribution in code comments and files, which the constitution forbids, has no mechanical check: hooks and the hygiene workflow scan messages, identities, refs and PR text only (N-14(b), optional, still open). State that it is a Layer 1b manual audit: `git diff main...HEAD` scanned with `.githooks/attribution-pattern`, excluding documents that quote the rule. (b) Pre-flight checks the mode of `.githooks/*` but not of `.claude/hooks/guard-bash.sh`; settings.json invokes it directly, so a lost executable bit silently disables the guard. (c) Conventional Commit format is not enforced by any hook (accepted; the Liaison audits it) | N-14 | Add to the pre-flight: `git ls-files -s .githooks .claude/hooks` shows `100755`; add the manual scan sentence |
| R3-16 | MINOR | Constitution header and changelog | (a) Changelog 0.3 does not record that Maven 3.9.16, Checkstyle/plugin, Playwright and the MCP package are approved unverified (S1 option a); the Verification column does, but the changelog is the decision trail. (b) Status stays `Draft`/`0.3`; the Amendment Procedure bumps the version on approval. (c) This package is applied on `docs/phase-1-constitution-review` although `git-workflow.md` reserves `spec/` for constitution changes and the Amendment Procedure says "on a `spec/` branch" | Module 3 section 5 | (a) add the sentence; (b) on Sponsor approval set Status Approved and version 1.0; (c) Sponsor accepts the branch name once as the bootstrap exception, or renames it |

## 5. Proposed document changes (exact text; I do not apply them)

### F-R3-01 `docs/sdd/orchestration.md` section 3, Phase 4 step 7
```diff
-7. **Gate:** Sponsor approves; orchestrator updates `progress.md`, merges following git-workflow section 4 (PR only if the Sponsor asked for one), then sets the row to `DONE` ...
+7. **Gate:** Sponsor approves; orchestrator updates `progress.md` in the task's branch, opens the pull request (the Sponsor's approval is the request) and squash-merges it after the required `hygiene` check and CODEOWNERS review, following git-workflow section 4, then sets the row to `DONE` ...
```

### F-R3-02 agent files
`sdd-frontend-developer.md` section 5, "Not yet approved" bullet:
```diff
-(`node --test frontend/`, approved, no package)
+(`node --test "frontend/**/*.test.js"`, approved, no package; the quoted glob is required, the directory form fails on Node 24)
```
`sdd-java-architect.md` section 4 Architecture:
```diff
-A single global exception handler (`@RestControllerAdvice`) builds every error body in the constitution envelope (`error`, `code`, `timestamp`); controllers never build error bodies.
+Every error body comes from the one error-body factory in the constitution (`error`, `code`, `timestamp`, optional `details`). The `@RestControllerAdvice` uses it for controller and framework exceptions and the catch-all; the servlet filter (Host allowlist, CSRF checks, body cap) and the container error path use it too. Controllers never build error bodies.
```

### F-R3-03 constitution Error Handling Standards, bullet 5 and bullet 6; `security.md`
```diff
-No response carries a default Spring or Tomcat error page or body. A security integration test covers each of these paths; protocol-level errors that Tomcat answers before any application code runs are a stated residual risk (R-09 family in `security.md`).
+No response produced by the application carries a default Spring or Tomcat error page or body. A security integration test covers each of these paths. Protocol-level errors that Tomcat answers before any application code runs are a stated residual risk (R-13 in `security.md`): the body is generic, carries no version and may lack the security headers.
```
`security.md` (Security reviewer proposes, Sponsor signs): add register row R-13 "Protocol-level errors answered by the container before application code (oversized header, invalid request line, bad percent-encoding): generic body without version, security headers may be absent; test with raw sockets", Proposed: accept; Sponsor column empty until signed.

### F-R3-04 constitution Approved dependencies, "Banned" paragraph
The orchestrator runs `cd backend && ./mvnw dependency:tree` on the throwaway project and replaces the paragraph with an enumerated list (group:artifact), for example:
```diff
-Banned in the runtime classpath (checked with `dependency:tree`): Actuator, devtools, H2, Spring Security, Lombok, `commons-logging`, any HTTP client, any XML or other data-format binding.
+Banned in the runtime classpath (checked with `dependency:tree`; any match fails review): `org.springframework.boot:spring-boot-starter-actuator`, `org.springframework.boot:spring-boot-devtools`, `com.h2database:h2`, `org.springframework.security:*`, `org.projectlombok:lombok`, `org.apache.httpcomponents.*:*`, `*:jackson-dataformat-xml`. Transitive exceptions found in the verified build are listed here: <coordinates, or "none">.
```
If the tree shows `commons-logging` or a JAXB runtime, list them as transitive exceptions (Sponsor sees the list at the gate); do not ban what the pinned stack itself brings. Replace the enumerated coordinates if the real tree differs.

### R3-05 to R3-16 (wording)
- DoD 1: "`node --test` green" becomes "the frontend logic unit test command green".
- R3-10: Code Style Enforcement: "identifier length, indentation, JSDoc on exports, file length (`wc -l`, 300) and function length (read in review)"; review list adds "no dead code, no N+1 or query in a loop"; `select *` audit: "case-insensitive multiline grep `select\s+([a-z_]+\.)?\*`"; time rule: "Business code never calls any wall-clock method (for example `Instant.now()`, `LocalDate.now()`, `LocalDateTime.now()`, `ZonedDateTime.now()`, `System.currentTimeMillis()`, `new Date()`, `Clock.systemUTC()`)"; `-Xlint:all -Werror` added to the Task 1 proofs.
- R3-07: ALWAYS bullet "Update `progress.md` in the task's branch" becomes "The orchestrator updates `progress.md` in the task's branch; implementers update only their `status.md` row"; remove "progress.md entries" from the Java and frontend agent self-audits.
- R3-12 sentence as in the table. R3-16(a): changelog 0.3 adds "Maven 3.9.16 / wrapper 3.3.4, Checkstyle 14.3.0 with plugin 3.6.0, `@playwright/test` 1.64.1 and `@playwright/mcp` 0.0.83 are approved unverified; Task 1 or the first frontend task verifies them or the Sponsor re-approves other pins."

## 6. Checks the orchestrator runs to close this round (no judgment needed)
1. `grep -n "PR only if" docs/sdd/orchestration.md` returns nothing.
2. `grep -rn "node --test frontend/" .claude docs/sdd` returns nothing.
3. `grep -n "R-09 family" docs/sdd/constitution.md` returns nothing; `grep -n "R-13" docs/sdd/security.md docs/sdd/constitution.md` returns the row and the citation.
4. The banned-dependency paragraph contains `group:artifact` coordinates and an explicit "transitive exceptions" entry taken from a real `dependency:tree` output attached to the Phase 1 evidence.
5. `git ls-files -s .githooks .claude/hooks` shows `100755` for every script (attach the output).
6. Re-run the `node --test "frontend/**/*.test.js"` check only after a test file exists (zero matches may exit non-zero; unverified).

## 7. Decisions only the Sponsor can take

| # | Decision | Options | Recommendation |
|---|---|---|---|
| D1 | Sign R-13 (protocol-level errors) after the Security reviewer adds it | Accept / change | Accept; it is the only honest statement of the container limit |
| D2 | Code-owner review on a solo repository | (a) Sponsor is a bypass actor on the `main` ruleset; (b) second reviewer or owner account; (c) drop "CODEOWNERS review" from the constitution | (a) or (b); keep the rule. Open Sponsor settings stay: enable `main` protection; the required `hygiene` check can only be selected after it has run once, so open the first pull request, let it run, then enable the rule |
| D3 | Banned-dependency transitive exceptions, if the real tree shows `commons-logging` or a JAXB runtime | Accept as listed exceptions / ask for exclusions in the POM | Accept as listed exceptions unless an exclusion is trivial; either needs your approval |
| D4 | Branch type of this package (`docs/` instead of `spec/`) | Accept once as bootstrap / rename | Accept once and record it |
| D5 | Version label on approval | 1.0 Approved / stay 0.x | 1.0 Approved when the Security PASS and your gate are in |
| D6 | Written acceptance of the unverified pins (S1 option a) | Record in changelog / verify now | Record; Task 1 verifies |

Unchanged reminders: exercise phase times exceed the stated 6 to 8 hours (open point 10); Phase 2 routes (non-UUID id behavior, invalid `status` filter, tie-break, trimming, enum case on input, `details` shape for 409, maximum collection size and response bound) are for the PM and not touched here.

## Open items / decisions needed

Gating for final Liaison `APPROVED`: F-R3-01, F-R3-02, F-R3-03 and F-R3-04 applied (the four MAJORs). The MINORs may ride along or be logged against the Phase 3 gate (R3-09, R3-12, R3-13 naturally belong there). The Liaison does not need to see this again unless a MAJOR text differs from the above.

Liaison rulings on doctrine (for the record):
1. A residual risk cited by a binding rule must exist in the register before the rule is approved (Module 3 section 5; only the Sponsor accepts risk).
2. A ban or check expressed as a category rather than coordinates is not mechanically verifiable (Module 3 Summary 2).
3. Every document that says how a merge happens must say the same thing; a stale process document defeats a gate even when the constitution is right (Module 2 section 2, context drift).

## Requested response

1. Orchestrator: apply F-R3-01 to F-R3-04 and the MINORs it accepts on the Phase 1 branch; attach the outputs for checks 4 and 5 in section 6; update the `status.md` Phase 1 row: Liaison `APPROVED WITH CHANGES` round 3, final `APPROVED` once the four MAJORs are verified by the greps above.
2. Security reviewer (round 2, in parallel): add R-13; own R3-06 (SC-19, port 18081, header) and R3-13 (b), (c).
3. Sponsor: D1 to D6; enable `main` protection after the first `hygiene` run; sign the Phase 1 gate when the Liaison text is final and the Security verdict is `PASS` or `PASS WITH CONDITIONS`.
