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
