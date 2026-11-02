# Handoff 034: Task delivery, Task 01 Maven scaffold and build proofs
- From: java
- To: liaison
- Phase: 4   Round: 1 of max 3 (attempt 1)
- Type: TASK_DELIVERY
- Branch: `chore/task-01-maven-scaffold` (`git branch --show-current` printed exactly that)
- Commits: bc6dcbd (wrapper and pom), d15ff07 (application class, tests, frontend placeholder), e6ddbb1 (dependency tree), plus the commit holding `progress.md` and this handoff. Local only, not pushed.

## 1. Files
Created: `backend/pom.xml`, `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`, `backend/.mvn/maven.config`, `backend/src/main/java/com/globant/globerbooks/GloberBooksApplication.java`, `backend/config/dependency-tree.txt`, tests `BuildSplitProofTest`, `BuildSplitProofIntegrationTest`, `RuntimeClasspathBanTest` under `backend/src/test/java/com/globant/globerbooks/`, `frontend/index.html` (placeholder, ruled by the orchestrator), `docs/sdd/progress.md`, this handoff. Nothing else changed (`status.md` untouched, no spec/plan/constitution/tasks edit). Package: `com.globant.globerbooks` (constitution Project Structure).

## 2. Pin verification (brief section 4)
- Maven zip: `https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip` (HTTP 200). `.sha256` is NOT published (HTTP 404); `.sha512` is.
- Published `.sha512`: `ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3`
- Computed `sha512sum` of the downloaded zip: identical value. Computed `sha256sum`: `5af3b743dd8b876b5c45da33b676251e5f1687712644abb4ee519ca56e1d89ce`, written as `distributionSha256Sum`. (The SHA-256 has no publisher value; its provenance is the SHA-512 match of the same file.)
- maven-wrapper 3.3.4: `org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.pom` HTTP 200 on Central. The wrapper was generated with `maven-wrapper-plugin:3.3.4:wrapper -Dmaven=3.9.16 -Dtype=only-script`. This form has no downloaded wrapper jar, so there is no `wrapperSha256Sum`.
- Spring Boot parent 4.1.2 and `jacoco-maven-plugin` 0.8.16 resolved from Central. Build log shows `compiler:3.15.0`, `surefire:3.5.6`, `failsafe:3.5.6`, `jacoco:0.8.16`, `resources:3.5.0`, `jar:3.5.1`, `spring-boot:4.1.2:repackage`, `dependency:3.10.0`. No pin was wrong; no version overridden.
- `./mvnw -v`:
```
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Java version: 21.0.12.1, vendor: Ubuntu, runtime: /usr/lib/jvm/java-21-openjdk-amd64
```
- Note: the first `./mvnw -v` of the session failed once with "wget: Failed to fetch" (before I exported `https_proxy`), and succeeded on later runs; this is the sandbox proxy, not a pin problem.

## 3. Criteria, tests and evidence
Final run `cd backend && ./mvnw clean verify`, exit 0:
```
surefire:3.5.6:test  Running ...RuntimeClasspathBanTest  Tests run: 6, Failures: 0
                     Running ...BuildSplitProofTest      Tests run: 1, Failures: 0
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
failsafe:3.5.6:integration-test  Running ...BuildSplitProofIntegrationTest  Tests run: 6, Failures: 0
jacoco:0.8.16:merge  (loads target/jacoco.exec and target/jacoco-it.exec -> target/jacoco-merged.exec)
failsafe:3.5.6:verify
jacoco:0.8.16:report / check: Analyzed bundle 'globerbooks-backend' with 0 classes
All coverage checks have been met.
BUILD SUCCESS
```
- **1a**: above. **1b**: `maven-wrapper.properties` = `wrapperVersion=3.3.4`, `distributionType=only-script`, `distributionUrl=...apache-maven-3.9.16-bin.zip`, `distributionSha256Sum=5af3b743...56ce`; `.mvn/maven.config` = `--strict-checksums`.
- **1c** (altered sum `0000b743...`, `MAVEN_USER_HOME` = scratch dir): `Error: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised.` exit=1. Restored value, `MAVEN_USER_HOME` = another empty scratch dir: `Apache Maven 3.9.16`, exit=0.
- **2a**: log lines above; `*Test` only in `surefire:test`, `*IntegrationTest` only in `failsafe:integration-test`. Oracle: each class asserts the `globerbooks.test.runner` system property that the pom sets per plugin (`surefire` / `failsafe`), so either class running in the wrong plugin fails.
- **2b**: pom has `prepare-agent`, `prepare-agent-integration`, `merge` (post-integration-test), `report` and `check` (verify, on `jacoco-merged.exec`) with `LINE COVEREDRATIO` minimum `${jacoco.line.minimum}` = 0.80. Because the plan excludes `GloberBooksApplication` and `*Config` from JaCoCo, the committed gate sees 0 classes. To show the merge, a recorded run with the exclusion temporarily removed (reverted, `diff` against backup empty) produced:
```
jacoco.exec    (Surefire): GloberBooksApplication  LINE_MISSED 3, LINE_COVERED 0
jacoco-it.exec (Failsafe): GloberBooksApplication  LINE_MISSED 2, LINE_COVERED 1
merged:                    GloberBooksApplication  LINE_MISSED 2, LINE_COVERED 1   (Analyzed bundle with 1 classes)
```
  The class is covered only by the integration test (`executesApplicationClassOnlyFromIntegrationPhaseForMergedCoverage`) and the merged data contains it.
- **2c**: same experiment, default 0.80: `Rule violated for bundle globerbooks-backend: lines covered ratio is 0.33, but expected minimum is 0.80` / `Coverage checks have not been met` / BUILD FAILURE, exit 1. With `-Djacoco.line.minimum=1.01` on the committed pom: `given minimum ratio is 1.01, but must be between 0.0 and 1.0`, BUILD FAILURE, exit 1 (also a failure of the gate, but a configuration error rather than a coverage miss, so the 0.33 run is the stronger proof).
- **3**: throwaway raw-type class: `[WARNING] ...ThrowawayWarning.java:[10,9] found raw type: java.util.List` ... `[ERROR] ... warnings found and -Werror specified`, BUILD FAILURE, exit 1. File removed.
- **4a**: `./mvnw dependency:tree -DoutputFile=config/dependency-tree.txt` (generation command, also in `RuntimeClasspathBanTest` Javadoc); regenerated to a scratch file, `diff` empty. The project line is the first line of the file.
- **4b**: no banned coordinate in the tree. Transitive exceptions present, all from the constitution list: `commons-logging:commons-logging:1.3.6` (Spring Framework 7 core), `org.glassfish.jaxb:jaxb-runtime`, `jaxb-core`, `txw2` 4.0.9, `jakarta.xml.bind:jakarta.xml.bind-api:4.0.5`, `com.sun.istack:istack-commons-runtime:4.1.2`, `org.eclipse.angus:angus-activation:2.0.3` (Hibernate XML mapping), `tomcat-embed-websocket:11.0.27` (Tomcat starter, no endpoint may be registered, SC-18), `org.yaml:snakeyaml:2.6` (configuration loading). Coordinates in the tree that are NOT on the constitution's exception list but are ordinary Boot starter transitives, for the Liaison to rule on: `org.apache.logging.log4j:log4j-api` and `log4j-to-slf4j` 2.25.5 (not `log4j-core`, which is banned), `jakarta.activation-api`, `jakarta.inject-api`, `org.aspectj:aspectjweaver`, `com.zaxxer:HikariCP`, `org.antlr:antlr4-runtime`, `net.bytebuddy:byte-buddy`, `com.fasterxml.jackson.core:jackson-annotations:2.21`. The tree file itself is the baseline ("any coordinate not in the committed tree is a finding"). The file holds no comments (it is `dependency:tree` output, diff-equal), so the one-line reasons are here only.
- **4c**: `RuntimeClasspathBanTest` (6 tests) reads `config/dependency-tree.txt`, compares coordinates with the constitution ban list (wildcards implemented), checks every jar on the test classpath (`surefire.test.class.path`) is in the committed tree, checks banned classes are absent, and self-checks the matcher with banned and allowed sample lines. Recorded negative run: appended `+- org.projectlombok:lombok:jar:1.18.30:provided` to the tree, then `committedTreeContainsNoBannedCoordinate` FAILED (`Expecting empty but was: [Coordinate[group=org.projectlombok, artifact=lombok ...]]`), exit 1; reverted.
- **5**: `-rw-r--r-- 64429791 target/globerbooks.jar`. `jar tf` of the committed state: `BOOT-INF/classes/static/index.html` and `BOOT-INF/classes/com/globant/globerbooks/GloberBooksApplication.class`; zero entries matching `.test.js`, `.map`, a dot segment or `Test.class`. Exclusion experiment (temporary files `app.test.js`, `app.map`, `.hidden`, `.d/y.txt`, `css/site.css.map`, `css/.gitkeep` plus `css/site.css`): the jar held only `static/index.html` and `static/css/site.css`; temporary files removed afterwards. Permanent proof: `BuildSplitProofIntegrationTest` (packagesJarNamedGloberbooksWithApplicationClass, packagesFrontendDirectoryAsStaticResources, excludesTestScriptsSourceMapsAndDotfilesFromStaticResources, containsNoTestClassesInJar); it runs after `package` because Failsafe executes in `integration-test`.

## 4. Test specification and inventory
Unit (Surefire): `BuildSplitProofTest.runsUnderSurefireWhenNamedWithTestSuffix`; `RuntimeClasspathBanTest`: committedTreeContainsNoBannedCoordinate, committedTreeHoldsEveryApprovedDirectDependency, everyJarOnTheTestClasspathIsInTheCommittedTree, detectsBannedCoordinatesInTreeLines, acceptsApprovedAndExceptionCoordinatesInTreeLines, bannedClassesAreAbsentFromTheTestClasspath. Integration (Failsafe): the six `BuildSplitProofIntegrationTest` methods above. Expected values come from the constitution (ban list, pins, jar exclusions).

## 5. Deviations and decisions
1. `frontend/index.html`: single neutral placeholder (orchestrator ruling) so the resources entry has content; exclusions proved by a temporary experiment, not committed fixtures. Later tasks replace it.
2. pom `<resources>` has the `frontend/` entry plus a second entry for `src/main/resources` (declaring `<resources>` replaces the Boot default, which Tasks 03a/03b need). Filtering off for both.
3. Failsafe `classesDirectory` is set to `target/classes` so Failsafe does not run against the repackaged Boot jar.
4. The JaCoCo gate is vacuous in the committed state (0 classes) because of the plan's exclusions; it becomes real when the first non-excluded class arrives. The executions are configured and proven with the experiment in 2b/2c.
5. Not built, per ruling: SC-30 real-port test class and "every mandatory property effective" (Tasks 08 and 03b); `node --test` and Playwright installs (later tasks per brief). The PM should confirm the mapping.
6. Checkstyle, SpotBugs and `.githooks` untouched (Task 02). Commits carry no trailer lines.

## 6. Risks and follow-ups
Nothing implemented beyond the task. The `ErrorMvcAutoConfiguration` import path (`org.springframework.boot.webmvc.autoconfigure.error`) compiles on Boot 4.1.2. The application cannot start yet (no data source configuration); that is Task 03a/03b. STOPPED; Task 02 not started.
