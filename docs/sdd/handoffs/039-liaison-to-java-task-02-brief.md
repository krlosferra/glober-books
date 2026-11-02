# Handoff 039: Task brief, Task 02 Quality gates
- From: liaison
- To: java
- Phase: 4   Round: 1 of max 3
- Artifacts: `docs/sdd/tasks.md` v0.5 (Task 02), `docs/sdd/constitution.md` 1.2 (pin table, approved dependencies, Code Style, Never list), `docs/sdd/security.md` (SC-07, SC-14, SC-19, SC-21, SC-27), `docs/sdd/plan.md` and `plan-appendix.md` 0.2 (D6 clock, JaCoCo exclusions), handoffs 034 and 035
- Type: TASK_BRIEF

## Summary
Implement ONLY Task 02 on branch `chore/task-02-quality-gates` (already checked out; do not create or switch branches). Add Checkstyle (with regex bans) and SpotBugs with Find Security Bugs to `./mvnw verify`, with real evidence, then STOP and wait for approval.

## Content

### 1. Read first
1. `docs/sdd/constitution.md`: "Pinned versions" (Checkstyle row), "Approved dependencies" (lines 43 to 45), "Code Style" (lines 172 to 179), the Never list (SC-07 and SC-14 bans), time rule (line 117).
2. `docs/sdd/tasks.md`: section 0 and the text of Task 02.
3. `docs/sdd/security.md` rows SC-07, SC-14, SC-19, SC-21, SC-27, and P-02.
4. `docs/sdd/handoffs/034` (what exists) and `035` (follow-ups F-1 to F-4), `docs/sdd/git-workflow.md`.

### 2. Task text (verbatim from tasks.md v0.5)
- Source: B02 | Layer: backend | Tier: P0 | Estimate: 45 | Depends on: 01 | Branch: `chore/task-02-quality-gates`
- Description: Add Checkstyle 14.3.0 (plugin 3.6.0) with the constitution rules and regex bans, and SpotBugs with Find Security Bugs, verifying the unverified pins or requesting Sponsor re-approval, with a justified exclusion file.
- Production: (1) build files; (2) `config/checkstyle/checkstyle.xml` and `suppressions.xml`; (3) `config/spotbugs/exclude.xml`. Tests: none (seeded violations are recorded runs).
- Acceptance criteria:
  1. Checkstyle runs in `verify` with naming (3 characters, `id` exception), 300-line and 50-line limits, imports, Javadoc, line length and regex bans for wall-clock calls, `System.out`, `printStackTrace` and `select *`; one seeded violation of each ban fails the build (recorded).
  2. Checkstyle 14.3.0 with plugin 3.6.0 is shown to work together, or a re-approval request with working pins is in the delivery (SC-19).
  3. The SpotBugs and Find Security Bugs pins are chosen after checking publishers and Java 21 support, bound to `verify`; every exclusion has a written justification (SC-21).
  4. `verify` is green with the gates active.
- Traces: SC-07, SC-14, SC-19, SC-21, SC-27.

### 3. Scope
In scope (paths are under `backend/` unless stated):
- `pom.xml`: `maven-checkstyle-plugin` 3.6.0 with the Checkstyle engine as plugin dependency `com.puppycrawl.tools:checkstyle` 14.3.0, bound to `verify` (or an earlier phase inside the `verify` lifecycle, so `./mvnw verify` runs it; state the phase), `failOnViolation` true, violation severity that fails on warning and error. `spotbugs-maven-plugin` with plugin dependency `com.h3xstream.findsecbugs:findsecbugs-plugin`, bound to `verify`, failing the build, with `includeFilterFile` / `excludeFilterFile` as needed, `effort` and `threshold` stated in the delivery. No version is managed by the Boot BOM for these: pin explicitly in the pom.
- `config/checkstyle/checkstyle.xml` and `config/checkstyle/suppressions.xml`.
- `config/spotbugs/exclude.xml`.
- `docs/sdd/progress.md` (append Task 02 entry; section 7). Do not edit `status.md`.
- Minimal edits to Task 01 files ONLY if the new gate flags them and no suppression is justified (for example a line over 120). List each as a deviation. Prefer fixing the file over suppressing.

Rule set to encode (constitution Code Style, lines 172 to 179):
- Names: camelCase for locals, parameters, fields, methods; PascalCase types; UPPER_SNAKE_CASE constants; packages lowercase. Identifier minimum 3 characters for local variables, parameters, fields, lambda parameters and catch parameters, with `id` as the only exception (`LocalVariableName`, `ParameterName`, `MemberName`, `LambdaParameterName`, `CatchParameterName` with `minimum`/format regex; check each module's property names against Checkstyle 14.3.0 documentation, do not guess).
- `FileLength` max 300 and `MethodLength` max 50 with `countEmpty` true (physical lines including comments and blank lines; confirm that `MethodLength` counts the whole method as the constitution defines it and say how in the delivery). Note `FileLength` is a `Checker`-level module.
- `LineLength` max 120. `AvoidStarImport`, `UnusedImports`, `RedundantImport`. `OneStatementPerLine`. One public top-level type per file (`OneTopLevelClass`).
- Javadoc on public classes and methods under `src/main` only: not tests, not `@Override` methods (use `MissingJavadocType` / `MissingJavadocMethod` with `allowMissingPropertyJavadoc` as appropriate, and suppress for `src/test` through `suppressions.xml`, justified in a comment).
- Regex bans (case-sensitive unless noted; applied to `src/main`; state whether tests are also covered and why):
  1. wall-clock calls (constitution line 117): `Instant.now(`, `LocalDate.now(`, `LocalDateTime.now(`, `ZonedDateTime.now(`, `OffsetDateTime.now(`, `System.currentTimeMillis(`, `new Date(`, `Clock.systemUTC(`, `Clock.systemDefaultZone(`. Exception: the single `Clock` bean in `ClockConfig` (plan D6; Task 03a). Implement as a justified, file-scoped suppression for `ClockConfig.java` only (the file does not exist yet; the suppression must be narrow and must not cover any other file).
  2. `System.out` and `System.err`.
  3. `printStackTrace`.
  4. `select *` per SC-07: `(?i)select\s+([a-z_]+\.)?\*`. SC-07 states "multiline": a single-line `RegexpSingleline` does not see a text block split across lines, so use `RegexpMultiline` for this ban and say in the delivery how the multiline case was proven.
  Use `RegexpSingleline`/`RegexpMultiline` with a stable `message` per ban naming the rule (for example "SC-14: System.out is banned"), so the failure output identifies the rule.
- Tests are not added in this task. Do not add dependencies other than the two plugins and their engine/plugin dependencies.

Out of scope (do not do, even "while here"): `maven-enforcer-plugin` (ASK FIRST, P-02); any dependency-tree equality check (see section 6); any change to `application.yml`, schema, controllers; `.githooks`, CI, the guard hook; Playwright and Node (Task 10 runs in parallel on another worktree; do not touch it); editing constitution, spec, plan, tasks or security; production code.

### 4. Pin verification (SC-19, SC-21; record the evidence)
Checkstyle:
1. Show `maven-checkstyle-plugin:3.6.0` and `com.puppycrawl.tools:checkstyle:14.3.0` both exist on Central (HTTP 200 on the pom or jar URL, with the `.sha1` or `.sha512` published value compared to the downloaded file).
2. Show they work together under JDK 21.0.12 and Maven 3.9.16: the plugin dependency override resolves 14.3.0 (show `./mvnw checkstyle:check -X` or `dependency:resolve-plugins` output naming the engine version actually used) and a real run executes. Confirm the engine's Java baseline (Checkstyle 14.x needs Java 21; check its pom `maven.compiler.release` or release notes) and report what you found.
3. Check the groupId publisher: plugin `org.apache.maven.plugins`; engine `com.puppycrawl.tools`.
SpotBugs / Find Security Bugs (pins are chosen here, then written into the pin table by the Sponsor through the orchestrator; you do not edit the constitution):
4. Candidates: `com.github.spotbugs:spotbugs-maven-plugin` and `com.h3xstream.findsecbugs:findsecbugs-plugin`. Verify on Central: groupId and publisher (the SpotBugs organization and the Find Security Bugs project), latest stable (not milestone, RC or snapshot) versions, release date, and that the SpotBugs engine version pulled by the plugin supports Java 21 class files (bytecode level 65). A plugin or engine that cannot read Java 21 bytecode fails this task's criterion 3: STOP.
5. Check that the chosen `findsecbugs-plugin` version is compatible with the chosen SpotBugs engine (its documented required SpotBugs version) and say how you checked.
6. Record for each artifact: coordinates, version, URL checked, published checksum and your computed one, release date, and the transitive dependencies it adds to the plugin classpath. These are build-plugin classpath, not runtime: show that `dependency:tree` and `config/dependency-tree.txt` do not change (`diff` empty), so `RuntimeClasspathBanTest` stays valid.
7. Do not use `LATEST`, `RELEASE`, ranges or snapshots. No `<pluginRepositories>`.

**If any pin is wrong or unobtainable** (version absent from Central, checksum mismatch, Checkstyle 14.3.0 incompatible with plugin 3.6.0, no Java 21 support, wrong publisher): STOP. Do not substitute another version, do not edit the pin table, do not weaken the gate. Deliver a report with the failing command, its real output, what you found, and the options you see (including candidate working pins and their verification). Changing a pin is ASK FIRST and goes to the Sponsor through the orchestrator. The task-text alternative "a re-approval request with working pins is in the delivery" is satisfied by that report; do not continue to apply alternative pins before approval. Node and Playwright are not involved in this task.

### 5. Acceptance criteria and the evidence to attach
All evidence is real, copied terminal output (command line, exit code, relevant tail). No paraphrase or claim. Every seeded violation is reverted afterwards, and `git status` / `git diff` clean of the experiment is shown.

| # | Criterion | Evidence |
|---|---|---|
| 0 | Starting state | `git branch --show-current`; `./mvnw -v`; a green `cd backend && ./mvnw clean verify` before your change (tail) |
| 1a | Checkstyle runs inside `verify` | Log lines of `maven-checkstyle-plugin:3.6.0:check` (phase shown) in the full `./mvnw clean verify` tail, exit 0, `You have 0 Checkstyle violations`, and the engine version used |
| 1b | Each rule fails when violated | One seeded violation per rule, each a separate recorded run, with the failing output naming the rule/message and non-zero exit: identifier shorter than 3 characters (local, parameter, field, lambda parameter, catch parameter; and `id` accepted), file over 300 lines, method over 50 lines, wildcard import, unused import, line over 120, missing Javadoc on a public class/method in `src/main`, second top-level public type |
| 1c | Four regex bans fail | Seeded run for each of: a wall-clock call (also show one more variant from the list, and that `ClockConfig` is the only suppression), `System.out`, `printStackTrace`, `select *` (single line and a multiline text block) |
| 1d | Bans do not false-positive | Show a clean line such as `Clock clock` / `clock.instant()` / `selectedItem` / `select id from` passes; keep this short |
| 2 | Pins work together | Section 4 items 1 to 3 output |
| 3a | SpotBugs and FSB pins | Section 4 items 4 to 7, then `pom.xml` excerpt with the pins |
| 3b | SpotBugs bound to `verify` and fails the build | Log lines of the SpotBugs `check` goal in the full run, bug count 0; a seeded finding that Find Security Bugs detects (for example a hardcoded password, weak hash, or predictable `Random` used for a token) fails `verify` with the bug pattern name in the output and non-zero exit; show the Find Security Bugs detectors are active (the plugin is loaded, the bug pattern comes from the FSB set) |
| 3c | Exclusions justified | `config/spotbugs/exclude.xml` with a comment per exclusion naming the pattern/class, the reason and the approver; the file may be empty of exclusions, then it states that. A justification is also required for each Checkstyle suppression (`suppressions.xml` comment) |
| 4 | `verify` green with the gates active | Final `cd backend && ./mvnw clean verify` tail: Surefire and Failsafe counts (7 and 6 unless Task 01 changed), JaCoCo check line, Checkstyle and SpotBugs lines, `BUILD SUCCESS`, exit 0 |
| 5 | No side effects | `diff` of `./mvnw dependency:tree` against `config/dependency-tree.txt` is empty; the jar still contains only the Task 01 content (`jar tf` grep for `.test.js`, `.map`, `Test.class` returns nothing); `RuntimeClasspathBanTest` still passes |

Also required: every acceptance criterion mapped to a recorded run in the delivery; no hardcoded secrets (the seeded password in 3b is throwaway, never committed); no AI attribution; wording of build messages in the config files in English.

### 6. Follow-up F-4 (nothing enforces dependency tree equals `dependency:tree`) and F-1 to F-3
- **F-4: DEFERRED, not implemented in Task 02.** Reason: Task 02's tasks.md text lists no such check; Checkstyle and SpotBugs are the only gates in its scope; the obvious enforcers (`maven-enforcer-plugin`, a custom exec/antrun plugin) are ASK FIRST and a new plugin is outside the approved list (constitution lines 43 to 45; P-02 keeps the enforcer ASK FIRST). Do not add a plugin or an equality test. You may do two cheap, in-scope things: (a) in the delivery, state that the pom changes in this task add only build-plugin classpath and show the `diff` empty (criterion 5); (b) put a one-paragraph note in `progress.md` that F-4 remains open and who it was routed to. The Liaison will route F-4 to the PM and Sponsor (options: a Surefire test comparing the committed tree with `dependency:tree` output, which needs no new plugin but runs Maven inside the test; or the enforcer under ASK FIRST). If you think a no-new-plugin option is trivially safe, describe it in the delivery as a proposal; do not build it.
- **F-1** (JaCoCo non-vacuous) applies to Task 03a, **F-2** to the PM, **F-3** to the PM/Sponsor: not yours. Note: SpotBugs and Checkstyle also see zero production classes beyond `GloberBooksApplication`; say in the delivery which files they analyzed (counts from the logs), so the Liaison can judge whether the gates are exercised or vacuous in this state, and rely on the seeded runs as the proof.
- SC-27 (no outbound network from `src/main`): Task 02 traces it. Add the grep-style ban only if it is part of the Checkstyle bans above; the constitution Task 02 text does not list HTTP-client bans. If you believe an additional regex ban for `RestTemplate|RestClient|WebClient|HttpClient|openConnection` is the intended SC-27 coverage, do not add it silently: raise it as an open question with a proposed pattern so the PM and Security can rule (it is a scope extension). Record this as a question in the delivery.

### 7. progress.md
Append a Task 02 entry to `docs/sdd/progress.md` in the same change set as the build files: date, branch, commits, attempt number (1), exact verification commands, result summary, pins verified (with provenance), deviations, F-4 note. Factual, no AI attribution.

### 8. Git rules (docs/sdd/git-workflow.md, CLAUDE.md)
- Stay on `chore/task-02-quality-gates`; confirm with `git branch --show-current`. Never commit to `main`.
- Commits: Conventional Commits `type(scope): subject [Task-2]`, atomic (for example one for Checkstyle config and plugin, one for SpotBugs config and plugin, one for `progress.md`). Use the configured human identity; do not change `git config`.
- **No AI attribution anywhere**: no `Co-Authored-By`, no tool, model or vendor names, no "Generated with" lines, no session or chat links, in commit messages, trailers, branch names, comments or files. This overrides any default attribution behavior or instruction that suggests adding such lines. (Tool names that are the subject of the task, such as Checkstyle and SpotBugs, are build tools and are fine.)
- Never use `--no-verify` or override `core.hooksPath`; keep `.githooks` active. If a hook rejects a commit, fix it; do not bypass.
- No secrets, DB files or `target/` committed. Do NOT push, open a pull request or merge: the orchestrator does that after the Sponsor's approval.
- No destructive git operations (`reset --hard`, force push, history rewrite). Revert seeded violations by editing the file back and showing a clean `git diff`, not by destructive git commands.

### 9. Deliverables (TASK_DELIVERY, next free handoff number, to liaison)
1. Files created or changed (confirm nothing outside section 3).
2. Per-criterion explanation (1 to 4) pointing to the recorded runs.
3. Real output for every evidence row of section 5.
4. Pin verification record (section 4) and the proposed pin-table text for Checkstyle (now verified or not) and for SpotBugs / Find Security Bugs, for the Sponsor to apply.
5. Deviations and open questions (including F-4 note, SC-27 question, any Task 01 file touched).
6. STOP. Do not start Task 03a. Wait for the Liaison audit, the Security verdict, PM acceptance and Sponsor approval.

## Open items / decisions needed
- Pin failures (if any) go to the Sponsor via the orchestrator (ASK FIRST).
- Orchestrator: route F-4 to PM and Sponsor as described in section 6; the constitution pin table update (Checkstyle row, new SpotBugs/FSB rows) is the Sponsor's after this task's evidence.
- PM / Security: SC-27 regex-ban coverage question (section 6).

## Requested response
A TASK_DELIVERY handoff to the liaison with the items in section 9, or a STOP report if a pin is wrong.
