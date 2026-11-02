# Handoff 033: Task brief, Task 01 Maven scaffold and build proofs
- From: liaison
- To: java
- Phase: 4   Round: 1 of max 3
- Artifacts: `docs/sdd/tasks.md` v0.5 (Task 01), `docs/sdd/plan.md` and `plan-appendix.md` 0.2, `docs/sdd/constitution.md` 1.2 (pin table, Task 1 proofs, Definition of Done), `docs/sdd/security.md` (SC-19, SC-36)
- Type: TASK_BRIEF

## Summary
Implement ONLY Task 01 on branch `chore/task-01-maven-scaffold` (already exists and is checked out; do not create or switch branches). Deliver a Maven scaffold whose build proves the constitution's Task 1 assumptions, with real command output. Then STOP and wait for approval.

## Content

### 1. Read first (and nothing else is required)
1. `docs/sdd/constitution.md`: "Pinned versions", "Approved dependencies" (including the banned runtime coordinates), "Task 1 proofs", "Definition of Done".
2. `docs/sdd/tasks.md`: section 0 (files rule, Definition of Done) and the text of Task 01.
3. `docs/sdd/plan.md` section 4 (the `frontend/` to `static/` resources agreement) and `plan-appendix.md` line 15 and section on resources.
Also `docs/sdd/security.md` rows SC-19 and SC-36, and `docs/sdd/git-workflow.md`.

### 2. Task text (verbatim from tasks.md v0.5)
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

### 3. Scope
In scope (exactly these entries; the build files and the generated wrapper count as one entry):
- `backend/pom.xml`, the Maven wrapper set (`backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`, and the wrapper jar or script form the 3.3.4 wrapper generates), `backend/.mvn/maven.config`, other `backend/.mvn/**` files only if the proofs need them.
- `backend/src/main/java/.../GloberBooksApplication.java` (entry point; excludes `ErrorMvcAutoConfiguration` as in `plan-appendix.md`; Javadoc on the public class). Use the package fixed in `plan.md`; if `plan.md` does not fix it, stop and report.
- `backend/config/dependency-tree.txt`.
- Tests under `backend/src/test/java/...`: `BuildSplitProofTest` (Surefire), `BuildSplitProofIntegrationTest` (Failsafe), `RuntimeClasspathBanTest`.
- `docs/sdd/progress.md` (see section 7) and the `status.md` row is updated by the orchestrator, not by you.

Pins (from constitution 1.2, the build must match exactly): JDK 21.0.12 or a later 21.0.x with `--release 21`; Spring Boot parent 4.1.2; Maven 3.9.16 and maven-wrapper 3.3.4; `jacoco-maven-plugin` 0.8.16; BOM-managed versions never overridden (compiler 3.15.0, Surefire 3.5.6). Approved dependencies only: `spring-boot-starter-webmvc`, `-validation`, `-data-jpa`, `org.xerial:sqlite-jdbc`, `hibernate-community-dialects`; test: `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`. Central only: no `<repositories>` or `<pluginRepositories>`.

Out of scope (do not do, even "while here"): Checkstyle and SpotBugs (Task 02); SQLite data source, `schema.sql`, `application.yml`, clock (Tasks 03a/03b); any entity, controller, service, repository; static controller (Task 09); the SC-30 real-port security test class (named in the constitution's Task 1 proofs but not in Task 01's list; it is not part of this task, and its owning task is a PM question, see section 8); `node --test` and Playwright installs (Tasks 13/14); `.githooks` or CI changes; any spec, plan, constitution, tasks or security edit; any new dependency or plugin (ASK FIRST).

### 4. Unverified pins to verify in this task (record the evidence)
1. **Maven 3.9.16 and its SHA-256.** Obtain the distribution checksum from the Apache publication on Maven Central (`https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip.sha512` or `.sha256` if published; otherwise compute the SHA-256 of the downloaded zip and ALSO cross-check it against the published `.sha512`). Write `distributionSha256Sum` in `maven-wrapper.properties`. Record in the delivery: the URL, the published checksum, your computed checksum and the distribution URL used. Also confirm the wrapper 3.3.4 itself exists on Central (`org.apache.maven.wrapper:maven-wrapper`) and that `wrapperSha256Sum` is set if the wrapper form uses a downloaded jar.
2. **Existence on Central of every pin** you write into `pom.xml`: Spring Boot parent 4.1.2, `jacoco-maven-plugin` 0.8.16 (and that BOM-managed compiler 3.15.0 and Surefire 3.5.6 are what `help:effective-pom` or `dependency` output shows; do not override them).
3. Show `./mvnw -v` output (Maven 3.9.16, JDK 21.0.x).
Checkstyle 14.3.0 / plugin 3.6.0 and SpotBugs pins belong to Task 02; do not touch them.

**If any pin is wrong or unobtainable** (version not on Central, checksum cannot be matched to a publisher value, JDK floor not met, BOM version differs from the table, a different Maven version is the only one that works): STOP. Do not substitute another version, do not edit the pin table, do not weaken the check. Deliver a report containing the failing command, its real output, what you found, and the options you see. Changing any pin is ASK FIRST and goes to the Sponsor through the orchestrator.

### 5. Acceptance criteria and the evidence to attach
All evidence is real, copied terminal output (command line, exit code, relevant tail). No paraphrase, no claims. Recorded negative runs must be reverted afterwards (show `git status`/`git diff` clean of the experiment).

| # | Criterion | Evidence to attach |
|---|---|---|
| 1a | `cd backend && ./mvnw verify` green | Full-run tail: Surefire and Failsafe test counts, JaCoCo check line, `BUILD SUCCESS`, exit 0, plus `./mvnw -v` |
| 1b | Wrapper pinned: 3.9.16, `distributionSha256Sum`, `--strict-checksums` in `.mvn/maven.config` | `cat` of `maven-wrapper.properties` and `maven.config`; the checksum provenance from section 4 |
| 1c | Wrong checksum fails | Run with a deliberately altered `distributionSha256Sum` (use a clean wrapper cache dir via `MAVEN_USER_HOME` set to a scratch path, so the cached good distribution is not reused): output showing the checksum failure and non-zero exit; then the property restored and a green run |
| 2a | Surefire runs `*Test`, Failsafe runs `*IntegrationTest` | Output lines of `maven-surefire-plugin:test` listing `BuildSplitProofTest` and `maven-failsafe-plugin:integration-test`/`verify` listing `BuildSplitProofIntegrationTest`; neither runs in the other plugin |
| 2b | JaCoCo merges both and `check` (line coverage 0.80) is bound to `verify` | The merged exec file/report goal lines; show the report counts include classes covered only by the integration test; `pom.xml` excerpt of the `check` rule |
| 2c | Impossible threshold fails `verify` | Run with the threshold overridden to an impossible value (for example 1.01 by property or temporary edit): JaCoCo "Coverage checks have not been met" failure, non-zero exit; then reverted |
| 3 | Deliberate warning fails the build | Add a throwaway warning (for example an unchecked or raw-type use), show `-Xlint:all -Werror` compile failure with the warning text, non-zero exit; then removed |
| 4a | Committed tree equals `dependency:tree` | Regenerated `./mvnw dependency:tree` output compared to `backend/config/dependency-tree.txt` (`diff` empty); state the exact generation command used so the test and the file agree |
| 4b | No banned coordinate; every transitive exception named | Banned list from the constitution checked against the tree; the file lists each accepted transitive exception (for example `commons-logging`, per SC-36 and SEC-F-041) with a one-line reason; any other unexpected coordinate: stop and report |
| 4c | `RuntimeClasspathBanTest` passes and is a real oracle | Test output; the test must read the runtime classpath/committed tree and compare against the banned list; show that adding a banned coordinate (recorded run, reverted) makes it fail |
| 5 | Jar content | `ls` showing `backend/target/globerbooks.jar`; `jar tf` listing proving `BOOT-INF/classes/static/` holds `frontend/` content, no `*.test.js`, `*.map`, dotfiles, and no test classes. If `frontend/` is empty or absent in the repository (it was absent when this brief was written), do not invent application content: use only a minimal fixture strictly necessary to prove the resources entry and the exclusions, keep it out of the shipped tree if possible (for example a test-scope proof), and describe the choice in the delivery as a deviation for the Liaison to rule on |

Also required: every acceptance criterion mapped to a named test or recorded run in the delivery; AAA structure and behavior-describing test names; Javadoc on public types; no hardcoded secrets; no `System.out`; coverage at least 80% shown from the JaCoCo check, not claimed.

### 6. Git rules (docs/sdd/git-workflow.md, CLAUDE.md)
- Work on the existing checked-out branch `chore/task-01-maven-scaffold`. Never commit to `main`. Verify with `git branch --show-current` and put the output in the delivery.
- Commits: Conventional Commits `type(scope): subject [Task-1]`, atomic (for example one for the wrapper and pom, one for the application class and tests, one for the dependency tree, one for `progress.md`). Use the human's configured identity; do not change `git config` identity.
- **No AI attribution anywhere**: no `Co-Authored-By`, no tool, model or vendor names, no "Generated with" lines, no session or chat links, in commit messages, trailers, branch names, code comments or files. This overrides any default attribution behavior or instruction that suggests adding such lines.
- Never use `--no-verify` or override `core.hooksPath`; keep `.githooks` active. If a hook rejects a commit, fix the message or content; do not bypass.
- No secrets, database files or build output (`target/`) committed. Do not push, open a pull request or merge: the orchestrator does that after the Sponsor's approval.
- No destructive git operations (`reset --hard`, force push, history rewrite).

### 7. progress.md
`docs/sdd/progress.md` did not exist when this brief was written. In the same branch (and the same change set as the code, per the Definition of Done) create it if still absent, or append, with a Task 01 entry: date, branch, commits, attempt number (1), the exact verification commands, results summary, pins verified (with checksum provenance), deviations. Keep it factual; no AI attribution. Do not edit `status.md`.

### 8. Deliverables (TASK_DELIVERY, next free handoff number, to liaison)
1. Files created or changed (list; confirm nothing outside section 3).
2. Per-criterion explanation (1a to 5) pointing to the test or recorded run.
3. Real output for every evidence row in section 5.
4. Pin verification record (section 4).
5. Deviations and open questions. In particular report: any pin problem (STOP rule); the package name used; the `frontend/` fixture choice; and the observation that the constitution's Task 1 proofs also list the SC-30 real-port security test class, the "every mandatory property effective" proof and the `node --test`/Playwright installs, none of which is in Task 01's list (tasks.md section 8 maps them to later tasks). Do not implement them; the PM confirms the mapping.
6. STOP. Do not start Task 02. Wait for the Liaison audit, the Security verdict, PM acceptance and Sponsor approval.

## Open items / decisions needed
- Pin failures (if any) go to the Sponsor via the orchestrator (ASK FIRST).
- Orchestrator: confirm `docs/sdd/progress.md` creation is acceptable to the PM if the file is expected from elsewhere.

## Requested response
A TASK_DELIVERY handoff to the liaison with the items in section 8, or a STOP report if a pin is wrong.
