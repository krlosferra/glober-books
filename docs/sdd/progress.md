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
- SC-27 HTTP-client regex ban is not added; raised as an open question in handoff 040.
