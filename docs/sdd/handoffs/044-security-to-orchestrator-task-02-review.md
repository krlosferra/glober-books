# Handoff 044: Security review, Task 02 Quality gates
- From: security
- To: orchestrator
- Phase: 4   Round: 1
- Artifacts: branch `chore/task-02-quality-gates` (HEAD 1d2abb3), handoffs 039 and 040, `backend/pom.xml`, `backend/config/checkstyle/{checkstyle,suppressions}.xml`, `backend/config/spotbugs/exclude.xml`
- Type: AUDIT_REPORT

## 1. Scope and method
Read the artifacts above. Ran: `./mvnw -o clean verify` (EXIT 0, Checkstyle and SpotBugs in the lifecycle); SHA-1 of the cached Maven jars versus the values in 040; Central HEAD/sha1 requests; one seeded Checkstyle run (file created, run, deleted; `git status --short` clean afterwards). Central rate-limited (HTTP 429) part of my requests.

## 2. Findings

| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-089 | MEDIUM | Gate coverage | `checkstyle.xml`, `suppressions.xml` | SC-27 (no outbound HTTP client in `src/main`) has no automated gate. O-1 should be decided YES: Task 02 is the gate task, P-02 lists SC-27 as automated by the analyzers, and until Task 04 there is no other guard. | `grep SC-27 backend/config` returns nothing. Existing test suppression regex is `^(SC-07\|SC-14\|TIME-RULE):`, so 040's claim "no test suppression needed beyond the existing one" is wrong: a new SC-27 message would also fail in `src/test` unless added to it. | A later task (or a prompt-injected agent) adds `RestClient`/`HttpClient`/`URL.openConnection` and the build stays green. | Add `RegexpSinglelineJava`, `ignoreComments` true, message `SC-27: outbound HTTP clients are banned in src/main`, format `\b(RestTemplate\|RestClient\|WebClient\|HttpClient\|HttpURLConnection\|URLConnection)\b\|\.openConnection\(\|\.openStream\(\|\bjava\.net\.http\b`; add `SC-27` to the test suppression regex and update its comment. Seed it once (expect fail in main, pass in test). | SC-27 |
| SEC-F-090 | LOW | Regex ban evasion | `checkstyle.xml` TIME-RULE | `new java.util.Date()` is not caught (format has `new Date\(`). Verified by seed: only `MethodName` was reported on that line; `java.time.Instant.now()` was caught. Also `Instant . now()` style spacing is not matched. | Seed output lines 8 vs 10. | Business code reads the wall clock and defeats the injected `Clock`. | Use `new\s+(java\.util\.)?Date\(` and `\.\s*now\s*\(`. | Constitution time rule |
| SEC-F-091 | INFO | Ban over-reach | `checkstyle.xml` TIME-RULE | `Instant.now(clock)` (legitimate, injected clock) is flagged too. Strict and safe, but will push developers toward `clock.instant()`; acceptable, document it. | Seed line 9 flagged. | None. | Keep; mention in plan D6. | |
| SEC-F-092 | LOW | Suppression scope | `suppressions.xml`, SpotBugs | (a) Test suppression `files="[\\/]src[\\/]test[\\/]"` is unanchored: any path containing `/src/test/` matches (a `src/main/.../src/test/` directory would exempt bans). (b) SpotBugs honors `@SuppressFBWarnings` matched by simple name, an inline bypass outside the SC-21 exclusion governance. | Config read. | Hardening only; needs a deliberate edit that review would see. | (a) Anchor: `files="[\\/]backend[\\/]src[\\/]test[\\/]java[\\/]"`, or rely on `src/test/java` plus review of new directories. (b) Add a review rule (checklist or grep `SuppressFBWarnings` in `src/main`) in security.md section 5. | SC-21 |
| SEC-F-093 | INFO | Verification limits | pins | FSB 1.14.0 (2025-04-20) is untested against SpotBugs 4.10.5 beyond the successful run; Checkstyle 14.3.0 and plugin 3.6.0 release dates not fetched; no CVE/advisory database reachable. R-1 and R-2 of 040 accepted as INFO. | See 3 and 4. | A future SpotBugs bump may silently disable FSB detectors. | On any SpotBugs bump re-run the MD5 seed and expect `WEAK_MESSAGE_DIGEST_MD5`. | SC-19, SC-21 |

## 3. Controls checked
- SC-19 (exact pins): VERIFIED. pom uses properties 3.6.0, 14.3.0, 4.10.5.0, 1.14.0; no ranges, LATEST, RELEASE or `pluginRepositories`. Local jar SHA-1 `fa8f5998...` (Checkstyle) and `6cc9a0bb...` (FSB) equal the values published in 040; I independently confirmed from Central: plugin 3.6.0 sha1 `de1a7244...` (HTTP 200 for the jar) and SpotBugs plugin 4.10.5.0 sha1 `be271e6c...`, both equal to 040. Checkstyle and FSB sha1 from Central returned 429 for me; not re-fetched, see 4.
- SC-21 (new packages, publisher): VERIFIED with limit. groupIds `com.puppycrawl.tools`, `com.github.spotbugs`, `com.h3xstream.findsecbugs`, `org.apache.maven.plugins` are the known publishers; all four exist on Central. Java 21: Checkstyle 14.3.0 requires 21, build JDK 21.0.12.1; SpotBugs analysed class files at release 21 with 0 errors (040 row 2/3b).
- Strictness: VERIFIED. effort Max, threshold Medium, failOnError true, `failOnViolation`/`failsOnError` true, `violationSeverity` warning, severity "error" at Checker. Exclusion file empty (`FindBugsFilter` with no Match). Checkstyle at `validate`, SpotBugs at `verify`, both declared before Surefire/JaCoCo (order correct). No `SuppressionCommentFilter` or `SuppressWarningsFilter` in `checkstyle.xml`, so no inline `// CHECKSTYLE:OFF` bypass.
- Suppressions: VERIFIED. Three, each justified. ClockConfig suppression matches only the `TIME-RULE:` prefix and only a file named `ClockConfig.java` under `src/main/java`; other bans still apply there (040 seed). Test suppression covers exactly SC-07, SC-14, TIME-RULE (intended scope). See SEC-F-090, 092.
- SC-07: VERIFIED for the gate (multiline, case-insensitive, alias form seeded in 040); status stays IN_PROGRESS until real queries exist.
- SC-14: VERIFIED for System.out/err and printStackTrace bans (040 seeds; I re-ran the full build green).
- SC-27: FAILED as a gate, no automated check (SEC-F-089).

## 4. Not verified
Central sha1/sha512 for Checkstyle 14.3.0 and FSB 1.14.0 (429); release dates of Checkstyle and plugin 3.6.0; GitHub release pages and Sonatype namespace ownership; CVE/advisory status of the pinned versions and their transitive tooling (log4j-core 2.26.1, Saxon-HE, dom4j are in SpotBugs' own classpath, build time only); no network from this host to advisory databases.

## 5. Residual risks
Build-tool supply chain (plugins run at build time with the developer's privileges): accepted by pins plus checksums; recommend the Sponsor record it under the existing supply-chain risk. FSB freshness (SEC-F-093) is INFO.

## 6. Verdict: PASS WITH CONDITIONS
Conditions, closed before merge:
1. Add the SC-27 ban and extend the test suppression (SEC-F-089), with one seeded main run (fails) and one test run (passes), reverted.
2. Fix the TIME-RULE regex for `new java.util.Date(` and spacing (SEC-F-090).
SEC-F-091 to 093 do not block. Next step: Java applies the conditions, I re-verify by seed, then the Sponsor gate.

## Appendix: exact text for `docs/sdd/security.md` (not applied; security.md is edited on another branch)

Evidence rows (append to the Evidence column):
- SC-07: "2026-11-02 Task 02 (handoff 044): Checkstyle `RegexpMultiline` `(?i)select\s+([a-z_]+\.)?\*` active at `validate`, seeded (single line, alias, text block) fails; src/test suppressed by design. Status stays IN_PROGRESS until queries exist."
- SC-14: "2026-11-02 Task 02 (044): `RegexpSinglelineJava` bans `System.out/err` and `printStackTrace` in src/main, seeded and failing; comments ignored; SpotBugs+FSB at `verify`, effort Max, threshold Medium."
- SC-19: "2026-11-02 Task 02 (044): Checkstyle plugin 3.6.0 + engine 14.3.0, SpotBugs plugin 4.10.5.0 (engine 4.10.5), FSB 1.14.0 pinned by property; SHA-1 of jars equal to Central; no ranges."
- SC-21: "2026-11-02 Task 02 (044): publishers by groupId on Central; Java 21 verified (Checkstyle requires 21; SpotBugs analyses release-21 classes); advisories not checkable (no database access); release dates for Checkstyle/plugin 3.6.0 not fetched."
- SC-27: "2026-11-02 Task 02 (044): no automated gate yet; condition SEC-F-089 requires the HTTP-client ban (proposed pattern in 040 O-1, extended with `URLConnection`, `openStream`, `java.net.http`) plus `SC-27` in the test suppression."

Findings log rows:
| SEC-F-089 | MEDIUM | No automated SC-27 gate; add HTTP-client ban and extend test suppression | OPEN, condition of Task 02 | 044 |
| SEC-F-090 | LOW | TIME-RULE misses `new java.util.Date()` and spaced `.now (` | OPEN, condition of Task 02 | 044 |
| SEC-F-091 | INFO | `Instant.now(clock)` also flagged (strict, acceptable) | ACCEPTED | 044 |
| SEC-F-092 | LOW | Unanchored test suppression path; `@SuppressFBWarnings` inline bypass not governed | OPEN, hardening | 044 |
| SEC-F-093 | INFO | FSB 1.14.0 vs SpotBugs 4.10.5 compatibility by run only; advisories and some release dates unchecked | OPEN, re-seed on every SpotBugs bump | 044 |

Changelog line:
- 0.20 (2026-11-02): Task 02 review (handoff 044): SC-07, SC-14, SC-19, SC-21 evidence for the quality gates; SC-27 gate gap; SEC-F-089 to 093 added (1 MEDIUM, 2 LOW, 2 INFO). Verdict PASS WITH CONDITIONS.

## Confirmation (re-verification of conditions, commits 33fe549, 168f47f, cc2ea5c)

Method: read `checkstyle.xml`, `suppressions.xml`, `pom.xml`; ran `./mvnw validate` with throwaway seeds under `src/main/java/zz` and `src/test/java/zz`, then deleted them (working tree has no seed residue; clean run exit 0).

- SEC-F-089 (SC-27): CLOSED. `RegexpSinglelineJava` bans `RestTemplate|RestClient|WebClient|HttpClient|HttpURLConnection|URLConnection`, `.openConnection(`, `.openStream(`, `java.net.http` (comments ignored). `SC-27` is in the test suppression message list. Seed in src/main (`new java.net.URL(..).openStream()`) fails the build (SC-27 reported among 4 errors); the same text in src/test raises no SC-27 or TIME-RULE error. The only error in the test seed was `OneStatementPerLine`, from my own seed layout, which also shows non-ban rules still apply to tests.
- SEC-F-090 (time rule): CLOSED. Regex now covers `new\s+(java\.util\.)?Date\s*\(` and `\.\s*now\s*\(`. Seeds `new java.util.Date ()` and `java.time.Instant . now ()` fail in src/main (main-only seed `new java.util.Date ()` rc=1); both pass in src/test. Broad `.now(` also flags `Instant.now(clock)`, accepted as SEC-F-091.
- SEC-F-092 (anchored path): CLOSED. Suppression files pattern is `[\\/]backend[\\/]src[\\/]test[\\/]`, and ClockConfig suppression is `[\\/]src[\\/]main[\\/]java[\\/].*[\\/]ClockConfig\.java$` limited to `^TIME-RULE:`. Residual hardening (inline `@SuppressFBWarnings` not governed) stays LOW, non-blocking.
- pom: only one added line (`checkstyle`-related, no version change); pins still exact properties, no ranges.

Not verified: pattern hits from fully-qualified/aliased static imports (`import static ...Instant.now` then bare `now()`): the `\.\s*now` rule needs a dot, so a bare `now()` after a static import is not caught. LOW, recommend a reviewer note; not blocking.

### Final verdict: PASS
Conditions 1 and 2 are met with evidence. Next step: Sponsor gate for Task 02. security.md update (SEC-F-089/090/092 to CLOSED, SC-27 gate evidence) is left to the owner branch, per the appendix text above.
