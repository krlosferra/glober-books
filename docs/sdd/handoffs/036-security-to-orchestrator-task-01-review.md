# Handoff 036: Security review, Task 01 (Maven scaffold)
- From: security
- To: orchestrator
- Phase: 4
- Type: AUDIT_REPORT
- Branch: `chore/task-01-maven-scaffold` (HEAD e47d8b9), working tree clean

## 1. Scope and method
Read: `backend/pom.xml`, `backend/.mvn/wrapper/maven-wrapper.properties`, `backend/.mvn/maven.config`, `backend/mvnw` (grep for credentials and repositories), `backend/config/dependency-tree.txt`, `frontend/index.html`, handoff 034, constitution ban list and exception list.
Ran (read-only, scratchpad only):
- Downloaded the Maven 3.9.16 zip from Central into a scratch directory. `sha512sum` = `ed41650d...3454af3`, identical to the value recorded in handoff 034 as the published value. `sha256sum` = `5af3b743...e89ce`, identical to `distributionSha256Sum`. The `.sha256` URL returns 404.
- `./mvnw -q dependency:tree -DoutputFile=<scratch>`: `diff` against the committed `config/dependency-tree.txt` is empty (rc 0).
- grep for `<repositories>`, `<pluginRepositories>`, `<url>`, passwords and tokens in the pom and `mvnw`: none (only the documented `MVNW_USERNAME`/`MVNW_PASSWORD` env hooks of the stock script). No `~/.m2/settings.xml`, no `settings.xml` in the repository.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Remediation | Control |
|---|---|---|---|---|---|---|---|
| SEC-F-083 | LOW | Supply chain, documentation | constitution Dependencies section (exception list) | Nine transitive coordinates are in the committed tree but not in the named exception list: `log4j-api`, `log4j-to-slf4j` 2.25.5, `jakarta.activation-api` 2.1.4, `jakarta.inject-api` 2.0.1, `aspectjweaver` 1.9.25.1, `HikariCP` 7.0.2, `antlr4-runtime` 4.13.2, `byte-buddy` 1.18.15, `jackson-annotations` 2.21. The constitution states the rule as "any coordinate not in the committed Task 1 dependency tree is a finding", so they are compliant. The list is just incomplete as documentation. | Tree lines inspected; none banned (`log4j-core` absent). All come from approved starters: logging starter (log4j bridge only, routes to SLF4J/Logback), JPA/JDBC starter (HikariCP, aspectjweaver via spring-aspects), Hibernate (byte-buddy, antlr4, inject, activation), Jackson 3 (annotations 2.x by design). | PM adds a one-line reason for each to the constitution exception list at the next amendment; no code change. Not blocking. | SC-36 |
| SEC-F-084 | INFO | Build integrity | `maven-wrapper.properties` | SHA-256 is self-computed because Central publishes only SHA-512 (no `.sha256`). Judged acceptable: the same bytes match the published SHA-512 (developer run, and my local `sha512sum` equals that recorded value), and I independently recomputed the same SHA-256. Residual: checksum and zip come from the same origin, so this is trust-on-first-use over TLS; after commit the pin protects against later substitution. | Section 1. My own re-fetch of the published `.sha512` hit a Central 429 rate limit, so the cross-check against the publisher rests on handoff 034's recorded value plus my matching local hash. | None. Keep the SHA-512 in the handoff as provenance (done). | SC-36 |
| SEC-F-085 | INFO | Process | handoff 034 section 5 | P-02 (SpotBugs with Find Security Bugs at Task 1, Sponsor approved 2026-10-30) is deferred to Task 02 by the brief; JaCoCo gate is vacuous (0 classes) until the first non-excluded class. | pom has neither plugin; 034 section 5 items 4 and 6. | Task 02 must add SpotBugs with pins verified on Central; Task 04+ must confirm the JaCoCo gate counts real classes. | SC-19 |

No CRITICAL or HIGH.

## 3. Controls checked
- SC-36 wrapper integrity: VERIFIED for Task 01 (`distributionSha256Sum` present and equal to my recomputed hash, `distributionType=only-script`, no wrapper jar, no `wrapperSha256Sum` needed; `--strict-checksums` in `.mvn/maven.config`; Central only, no repositories in pom or settings; no secrets). Altered-checksum failure is shown in 034 (1c); I did not re-run it.
- SC-36 banned coordinates: VERIFIED against the fresh tree (no actuator, devtools, h2, security, lombok, httpcomponents, okhttp3, webflux, reactor-netty, jackson-dataformat-xml, starter-websocket, log4j-core). `tomcat-embed-websocket` is a listed exception (SC-18 applies later). Test `RuntimeClasspathBanTest` enforces the list at build time.
- SC-19 pins: VERIFIED for the backend: Boot 4.1.2 parent, Spring 7.0.10, Hibernate 7.4.13.Final (and community dialects), Validator 9.1.5.Final, Jackson 3.1.7, Tomcat 11.0.27, JUnit 6.0.3, Mockito 5.23.0, AssertJ 3.27.7, sqlite-jdbc 3.53.4.0, JaCoCo 0.8.16 all equal the constitution; no version overridden in the pom. npm pins not applicable yet.
- Build-packaging hygiene: resources exclude `*.test.js`, `*.map`, dotfiles and dot directories; only `frontend/` placed under `static`. Frontend placeholder is inert (no script, no external reference).

## 4. Not verified
- Public advisory databases (OSV, GHSA, NVD): not reached; no CVE check of the tree versions was made. The BOM-managed versions follow the constitution table as verified on 2026-10-29.
- Publisher `.sha512` re-fetch (rate limited, see SEC-F-084).
- The 1c negative test and the full `verify` run were not re-executed by me.
- Checkstyle, hooks, SpotBugs (later tasks).

## 5. Residual risks
Trust-on-first-use of the Maven distribution (SEC-F-084): recommend the Sponsor accepts; no register entry needed beyond this note.

## 6. Verdict
**PASS** (with LOW/INFO notes SEC-F-083 to 085, none blocking). Next step: Liaison verdict, then Sponsor gate. The PM should fold SEC-F-083 into the next constitution amendment.
