# Handoff 040: Task delivery, Task 02 Quality gates
- From: java
- To: liaison
- Phase: 4   Round: 1 of max 3
- Artifacts: `backend/pom.xml`, `backend/config/checkstyle/{checkstyle,suppressions}.xml`, `backend/config/spotbugs/exclude.xml`, `docs/sdd/progress.md`
- Type: TASK_DELIVERY

## Summary
Task 02 is implemented on `chore/task-02-quality-gates` (local commits only, not pushed). All pins exist, match their published checksums and work together. `./mvnw clean verify` is green with Checkstyle (phase `validate`) and SpotBugs + Find Security Bugs (phase `verify`) active. Every rule and ban was proven by a seeded violation (reverted). No pin failed, so there is no STOP. Command output below is trimmed to the relevant lines (the `Picked up JAVA_TOOL_OPTIONS` line is omitted everywhere).

## 1. Files
Created: `backend/config/checkstyle/checkstyle.xml`, `backend/config/checkstyle/suppressions.xml`, `backend/config/spotbugs/exclude.xml`.
Modified: `backend/pom.xml` (4 version properties, 2 plugins), `docs/sdd/progress.md` (Task 02 entry), and one Task 01 file, see deviation D-1: `backend/src/test/java/com/globant/globerbooks/RuntimeClasspathBanTest.java` (3 lines wrapped).
Nothing else changed. `status.md` untouched. Seed files never committed (`git status` after every seed shows only the files above).
Commit note: the pom change for SpotBugs went into the Checkstyle commit (one pom edit); `backend/config/spotbugs/exclude.xml` has its own commit. All subjects end with `[Task-2]`; see `git log`.

## 2. Evidence per row of section 5 of the brief

### Row 0: starting state
```
$ git branch --show-current
chore/task-02-quality-gates
$ ./mvnw -v
Apache Maven 3.9.16 ...  Java version: 21.0.12.1, vendor: Ubuntu
$ cd backend && ./mvnw clean verify      (before any change)
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0   (Failsafe; Surefire 7)
[INFO] Analyzed bundle 'globerbooks-backend' with 0 classes
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS      EXIT=0
```

### Row 2 and 3a: pin verification (SC-19, SC-21), Central `https://repo.maven.apache.org/maven2`
Central rate-limited the first requests (HTTP 429, "Your ip has exceeded rate limits"); I retried until 200. Checksums: published `.jar.sha1` versus `sha1sum` of the downloaded jar.

| Artifact (publisher by groupId) | Version | Release date | HTTP | Published SHA-1 | Computed SHA-1 |
|---|---|---|---|---|---|
| `org.apache.maven.plugins:maven-checkstyle-plugin` (Apache Maven) | 3.6.0 | n/a (see note) | jar/pom/sha1 200; `.sha512` 404 (not published) | `de1a7244e68b3ab863ec474ccccdd9cb0becfe39` | same |
| `com.puppycrawl.tools:checkstyle` (Checkstyle project) | 14.3.0 | n/a | jar/pom/sha1/sha512 200 | `fa8f599888ff93989517e3cf33327055ab896183` | same; SHA-512 `1ac345dd...4dfe1` published = computed |
| `com.github.spotbugs:spotbugs-maven-plugin` (SpotBugs organization; pom scm `github.com/spotbugs/spotbugs-maven-plugin`, url `spotbugs.github.io`) | 4.10.5.0 (metadata `latest` = `release`, lastUpdated 2026-10-12, no RC/milestone) | 2026-10-12 | 200 | `be271e6c079eb4e2ac25323b42e3775ab26909ef` | same |
| `com.github.spotbugs:spotbugs` (engine, pulled by the plugin; pom property `spotbugs.version` 4.10.5) | 4.10.5 (latest, lastUpdated 2026-10-12) | 2026-10-12 | 200 | `26ad5cb461c7bf744fbcba43885222dce3f48d7a` | same |
| `com.h3xstream.findsecbugs:findsecbugs-plugin` (Find Security Bugs project; plugin descriptor `provider="Find Security Bugs"`) | 1.14.0 (latest = release, lastUpdated 2025-04-20) | 2025-04-20 (jar entry dates) | 200 | `6cc9a0bbd3d1ff0920a1113bbcf0ed3782c91d23` | same |

Release dates are taken from Central `maven-metadata.xml` `lastUpdated` and jar entry dates, not from the project sites (I did not browse GitHub). The publisher conclusion rests on the groupId namespaces on Central, the pom `scm`/`url` fields and the FSB plugin descriptor; I did not check Sonatype namespace ownership separately. Maven Central metadata for Checkstyle 14.3.0 and plugin 3.6.0 release dates was not fetched.

Java baseline of the Checkstyle engine: `checkstyle-14.3.0.pom` has `<java.version>21</java.version>` and `<maven.compiler.release>${java.version}</maven.compiler.release>` (lines 223-224), so it needs Java 21; the build JDK is 21.0.12.1.

Java 21 bytecode (class file major 65) support of SpotBugs: engine 4.10.5 manifest `Class-Path` lists `asm-9.11` and `bcel-6.13.0` (both read Java 21 class files). Practical proof: the analysis ran over `GloberBooksApplication.class` and over the seeded classes, all compiled with `--release 21` (class major 65), with no analysis error ("BugInstance size is N", "errors" count 0 in the seeded failure line `failed with 1 bugs and 0 errors`).

FSB 1.14.0 and SpotBugs 4.10.5 compatibility: FSB declares `com.github.spotbugs:spotbugs` with `provided` scope and no pinned upper bound in its pom (version comes from its parent); the `findsecbugs-plugin-1.14.0.jar` descriptor is `FindbugsPlugin pluginid=com.h3xstream.findsecbugs`. I did not find a documented "requires SpotBugs x.y" statement in the artifacts I read; compatibility is demonstrated by running: the plugin loads under SpotBugs 4.10.5 and its detectors fire (see row 3b). Residual risk R-1 below.

Transitive additions to the plugin classpath (build tooling only): `spotbugs-maven-plugin` pulls `com.github.spotbugs:spotbugs:4.10.5` and `spotbugs-annotations:4.10.5` (plus the engine's own libraries listed in its manifest: asm 9.11, bcel 6.13.0, dom4j 2.2.0, gson 2.14.0, commons-text 1.15.0, log4j-core 2.26.1, Saxon-HE 12.10, and others); `maven-checkstyle-plugin` pulls `com.puppycrawl.tools:checkstyle:14.3.0`. The plugin override resolves the engine as 14.3.0 only (from `./mvnw -X validate`: `[DEBUG]    com.puppycrawl.tools:checkstyle:jar:14.3.0:runtime`, `Included: com.puppycrawl.tools:checkstyle:jar:14.3.0`; the plugin's default engine is not in the realm). `./mvnw dependency:resolve-plugins`:
```
[INFO]    com.github.spotbugs:spotbugs-maven-plugin:jar:4.10.5.0
[INFO]       com.github.spotbugs:spotbugs:jar:4.10.5
[INFO]       com.github.spotbugs:spotbugs-annotations:jar:4.10.5
[INFO]    org.apache.maven.plugins:maven-checkstyle-plugin:jar:3.6.0
[INFO]       com.puppycrawl.tools:checkstyle:jar:14.3.0
```
Pinning: exact versions via pom properties; no LATEST/RELEASE/ranges; no `<pluginRepositories>`.

pom excerpt:
```xml
<checkstyle.plugin.version>3.6.0</checkstyle.plugin.version>
<checkstyle.engine.version>14.3.0</checkstyle.engine.version>
<spotbugs.plugin.version>4.10.5.0</spotbugs.plugin.version>
<findsecbugs.version>1.14.0</findsecbugs.version>
...
maven-checkstyle-plugin ${checkstyle.plugin.version}; dependency com.puppycrawl.tools:checkstyle ${checkstyle.engine.version};
  configLocation config/checkstyle/checkstyle.xml; suppressionsLocation config/checkstyle/suppressions.xml;
  includeTestSourceDirectory true; failOnViolation true; failsOnError true; violationSeverity warning;
  execution checkstyle-check, phase validate, goal check
spotbugs-maven-plugin ${spotbugs.plugin.version}; effort Max; threshold Medium; failOnError true;
  excludeFilterFile config/spotbugs/exclude.xml; <plugins> findsecbugs-plugin ${findsecbugs.version};
  execution spotbugs-check, phase verify, goal check
```
Both new plugins are declared before Surefire so static analysis fails before the JaCoCo check (JaCoCo's check would otherwise mask a SpotBugs failure when a seeded class has no coverage; I hit this in practice).

### Row 1a and 4: Checkstyle and SpotBugs inside `verify`; final run
```
$ cd backend && ./mvnw clean verify ; echo EXIT=$?
[INFO] --- checkstyle:3.6.0:check (checkstyle-check) @ globerbooks-backend ---
[INFO] You have 0 Checkstyle violations.
[INFO] --- surefire:3.5.6:test (default-test) ...   [INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- failsafe:3.5.6:integration-test ...      [INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- spotbugs:4.10.5.0:spotbugs (spotbugs) @ globerbooks-backend ---
[INFO] --- spotbugs:4.10.5.0:check (spotbugs-check) @ globerbooks-backend ---
[INFO] BugInstance size is 0
[INFO] --- jacoco:0.8.16:check (check-merged) ...  Analyzed bundle 'globerbooks-backend' with 0 classes
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
EXIT=0
```
Phases: Checkstyle `validate` (earliest, fails fast without compiling), SpotBugs `verify`. Engine version used: 14.3.0 (see `-X` lines above); the Checkstyle summary line is `reported by Checkstyle 14.3.0` on failure (see seeds: "There are N errors reported by Checkstyle 14.3.0 with ... ruleset").
Files analyzed (so the Liaison can judge vacuity): Checkstyle 4 Java files (1 main: `GloberBooksApplication`; 3 test); SpotBugs 1 production class (`GloberBooksApplication`; the other `src/main` content is resources). The gates are therefore exercised only by the seeded runs below, as the brief anticipated. JaCoCo is still vacuous (0 classes); that is F-1, Task 03a.

### Row 1b: each rule fails when violated
Each run: seed file written to `src/main/java/com/globant/globerbooks/Seed.java`, `./mvnw checkstyle:check@checkstyle-check`, output filtered to `[ERROR] /path:line` and `BUILD`, then the file deleted. Paths shortened to `Seed.java`. Exit code is the Maven exit.

| Seed | Output (exit 1 for all) |
|---|---|
| local `int ab`, plus `int id` and `int abc` | `Seed.java:7:13: Name 'ab' must match pattern '^(id\|[a-z][a-z0-9][a-zA-Z0-9]+)$'. [LocalVariableName]` (only `ab` flagged; `id` and `abc` accepted) |
| parameter `int ab, int id` | `Seed.java:6:25: Name 'ab' must match ... [ParameterName]` (`id` accepted) |
| field `ab` and `id` | `Seed.java:5:23: Name 'ab' must match ... [MemberName]` (`id` accepted) |
| lambda `xx -> xx`, `catch (RuntimeException e)` | `Seed.java:10:20: Name 'xx' ... [LambdaParameterName]`; `Seed.java:11:35: Name 'e' ... [CatchParameterName]` |
| file of 300 lines / 301 lines | 300: `You have 0 Checkstyle violations`, exit 0. 301: `Seed.java:1: File length is 301 lines (max allowed is 300). [FileLength]` |
| method of 50 / 51 / 52 physical lines | 50 (48 statements): exit 0. 51: `Seed.java:6:5: Method run length is 51 lines (max allowed is 50). [MethodLength]`. 52: `... is 52 lines` |
| `import java.util.*;` unused `java.util.List`; `java.lang.String`; same-package import | `AvoidStarImport` (3:17), `UnusedImports` (4:8, 5:8, 6:8), `RedundantImport` (5:1 java.lang, 6:1 same package) |
| string constants of 124 and 121 characters | `Seed.java:5: Line is longer than 120 characters (found 124). [LineLength]` and `Seed.java:6: ... (found 121)` |
| public class and public method without Javadoc; package-private method and `@Override toString` without Javadoc | `Seed.java:3:1: Missing a Javadoc comment for 'Seed'. [MissingJavadocType]`; `Seed.java:4:5: ... for 'run'. [MissingJavadocMethod]`; package-private and `@Override` methods not flagged |
| second top-level types `Second`, `Third` in `Seed.java` | `Seed.java:8:1: Top-level class Second has to reside in its own source file. [OneTopLevelClass]` and the same for `Third` (two public types would not compile anyway) |

How `MethodLength` counts: from the line of the method declaration (not the Javadoc above it) to the closing brace, inclusive, blank and comment lines counted (`countEmpty` true). 48 body statements = 50 lines pass; 49 = 51 fail. Constitution "function length 50, physical lines" is matched for the declaration-to-brace span. Javadoc and annotations above the declaration are not counted (an annotation line directly above the declaration is not counted either). This is the only counting Checkstyle offers.
Boundary of 120 characters exactly was not seeded separately (the check is `max`, inclusive; 121 fails).

### Row 1c: the four regex bans
Wall-clock (all nine listed variants in one seed, lines 15-23, each flagged):
```
Seed.java:15: TIME-RULE: wall-clock calls are banned; inject java.time.Clock [RegexpSinglelineJava]   (Instant.now)
... lines 16..23 identical (LocalDate, LocalDateTime, ZonedDateTime, OffsetDateTime, System.currentTimeMillis, new Date, Clock.systemUTC, Clock.systemDefaultZone)
[INFO] BUILD FAILURE   EXIT=1
```
ClockConfig exemption (the only suppression for this ban; `suppressions.xml` has three suppressions, each with a written justification):
```
### src/main/java/.../config/ClockConfig.java using Clock.systemUTC()
You have 0 Checkstyle violations.   BUILD SUCCESS
### same file with System.out.println added
ClockConfig.java:10: SC-14: System.out and System.err are banned; use SLF4J [RegexpSinglelineJava]   BUILD FAILURE
```
The `ClockConfig` suppression matches only the message prefix `TIME-RULE:` and only the path `.../src/main/java/.../ClockConfig.java`; other bans and rules still apply there, and `Seed.java` with `Clock.systemUTC()` fails (above). Limitation: inside `ClockConfig.java` all nine wall-clock forms are allowed, not only `Clock.systemUTC()`, because one file-scoped message suppression cannot distinguish them. Review item for Task 03a.

`System.out`, `System.err`, `printStackTrace`:
```
Seed.java:7: SC-14: System.out and System.err are banned; use SLF4J [RegexpSinglelineJava]
Seed.java:8: SC-14: System.out and System.err are banned; use SLF4J [RegexpSinglelineJava]
Seed.java:9: SC-14: printStackTrace is banned; use SLF4J [RegexpSinglelineJava]
BUILD FAILURE
```
`select *` (single line, alias form and a text block spanning three lines):
```
Seed.java:5: SC-07: select * is banned; list the columns explicitly [RegexpMultiline]   ("SELECT * FROM books")
Seed.java:6: SC-07: ...                                                                   ("select b.* from books b")
Seed.java:8: SC-07: ...   (text block: `select` on line 8, `*` on line 9, matched across lines by (?i)select\s+([a-z_]+\.)?\*)
BUILD FAILURE
```
Multiline proof: `RegexpMultiline` runs over the whole file, so `\s+` spans the newline; the report line is the line of the `select`. Case-insensitive via `(?i)`.

Scope: bans apply to `src/main`; tests are scanned by Checkstyle (naming, length, imports, line length) but the SC-07/SC-14/TIME-RULE messages are suppressed for `src/test` (a test seeded with `System.out`, `select *` and `Instant.now` passes those bans while `int ab` in the same file still fails with `LocalVariableName`). Reason: SC-07, SC-14 and SC-27 define `src/main` as scope and tests may carry fixtures. Open point O-2 if the Liaison wants the wall-clock ban in tests too.

### Row 1d: no false positives
Seed passing with 0 violations (`BUILD SUCCESS`, exit 0): `Clock clock` field and constructor parameter, `clock.instant()`, `selectedItem`, `selectCount`, `"select id, title from books"`, a text block `select id,\n title\n from books\n where title like 'a*'`, `"selected * item"`, `1 * 2`, and `Instant.now()` / `System.out` written only inside comments.

### Row 3b: SpotBugs bound to `verify`, fails the build, Find Security Bugs active
Seed (class `Seed` with `MessageDigest.getInstance("MD5")` and a `Random` token; a throwaway password constant), full lifecycle:
```
$ ./mvnw clean verify ; EXIT=1
[INFO] --- checkstyle:3.6.0:check (checkstyle-check) ...
[INFO] --- spotbugs:4.10.5.0:check (spotbugs-check) @ globerbooks-backend ---
[INFO] BugInstance size is 1
[ERROR] High: This API MD5 (MDX) is not a recommended cryptographic hash function [com.globant.globerbooks.Seed] At Seed.java:[line 12] WEAK_MESSAGE_DIGEST_MD5
[INFO] BUILD FAILURE
[ERROR] Failed to execute goal com.github.spotbugs:spotbugs-maven-plugin:4.10.5.0:check (spotbugs-check) ...: failed with 1 bugs and 0 errors
```
Second seeded run (`goal` invocation `compile spotbugs:check@spotbugs-check`, with `new Random().nextLong()` as a token):
```
[ERROR] High: Random object created and used only once ... DMI_RANDOM_USED_ONLY_ONCE        (core SpotBugs)
[ERROR] Medium: This random generator (java.util.Random) is predictable ... PREDICTABLE_RANDOM   (Find Security Bugs)
[ERROR] High: This API MD5 (MDX) is not a recommended cryptographic hash function ... WEAK_MESSAGE_DIGEST_MD5   (Find Security Bugs)
```
`PREDICTABLE_RANDOM` and `WEAK_MESSAGE_DIGEST_MD5` are Find Security Bugs patterns (core SpotBugs has no such pattern names), so the FSB detectors are loaded. `-X` log: `findsecbugs-plugin` present in the plugin configuration and `[INFO] Deleting file .../target/spotbugs/findsecbugs-plugin-1.14.0.jar` (the plugin copies the FSB jar into `target/spotbugs/`), `findsecbugs.version: 1.14.0`. The hardcoded-password constant by itself was not flagged (the detector `HARD_CODE_PASSWORD` needs the value used as a password argument); I do not claim it.
Seed removed; `git status --short` afterwards lists only the intended changes.

### Row 3c: exclusions justified
`config/spotbugs/exclude.xml` contains no exclusions and says so, with the required comment format for future entries. `suppressions.xml` has three suppressions, each preceded by a comment with justification and approver: tests exempt from Javadoc (constitution Code Style); the four ban messages exempt for `src/test` (SC-07/SC-14/SC-27 scope); `TIME-RULE` exempt for `ClockConfig.java` only (plan D6).

### Row 5: no side effects
```
$ ./mvnw dependency:tree -DoutputFile=<scratch>/tree.txt ; diff <scratch>/tree.txt config/dependency-tree.txt && echo TREE_DIFF_EMPTY
TREE_DIFF_EMPTY
$ jar tf target/globerbooks.jar | grep -E "\.test\.js|\.map|Test\.class" ; echo exit=$?
exit=1        (nothing found)
```
`RuntimeClasspathBanTest` passes inside the Surefire count of 7.

## 3. Acceptance criteria map
1. Checkstyle in `verify` with naming, 300/50, imports, Javadoc, line length and four bans, one seeded violation each: rows 1a to 1d.
2. Checkstyle 14.3.0 with plugin 3.6.0 work together: row 2 (checksums, engine resolved, real run, Java 21 baseline).
3. SpotBugs and FSB pins chosen after publisher and Java 21 checks, bound to `verify`, exclusions justified: rows 3a to 3c.
4. `verify` green with gates: row 4.

## 4. Proposed pin-table text (for the Sponsor to apply; I did not edit the constitution)
- Checkstyle row: `maven-checkstyle-plugin` 3.6.0 and engine `com.puppycrawl.tools:checkstyle` 14.3.0, VERIFIED 2026-11-02 (Central, SHA-1 equal, engine release 21, resolved together under JDK 21.0.12.1 and Maven 3.9.16).
- New row: `com.github.spotbugs:spotbugs-maven-plugin` 4.10.5.0 (engine `com.github.spotbugs:spotbugs` 4.10.5), VERIFIED 2026-11-02 (Central, SHA-1 equal, latest stable, analyzes Java 21 class files).
- New row: `com.h3xstream.findsecbugs:findsecbugs-plugin` 1.14.0 (2025-04-20), VERIFIED 2026-11-02 (Central, SHA-1 equal, loaded by SpotBugs 4.10.5 and detects `WEAK_MESSAGE_DIGEST_MD5`, `PREDICTABLE_RANDOM`).
- Settings to record: SpotBugs `effort` Max, `threshold` Medium, bound to `verify`; Checkstyle bound to `validate`, severity warning and above fails.

## 5. Deviations and open questions
- D-1 (Task 01 file touched): `RuntimeClasspathBanTest.java` had three lines over 120 characters (124, 134, 124) flagged by the new gate. I wrapped them (one Javadoc turned into a multi-line block, one string literal split with `+`, one condition wrapped); no behavior change; Surefire still 7 green. Fixing was preferred over a suppression, per the brief.
- D-2: the single-line bans use `RegexpSinglelineJava` (inside `TreeWalker`) instead of `RegexpSingleline`: in 14.3.0 `RegexpSingleline` has no `ignoreComments` property (the build rejected it: "Property 'ignoreComments' does not exist"), and without it a comment mentioning `System.out` would fail the build. `select *` uses `RegexpMultiline` as requested.
- D-3: `LineLength` is a `Checker`-level module in 14.3.0 (the build rejected it under `TreeWalker`); `suppressionsLocation` replaces `${config_loc}` (not defined by plugin 3.6.0 for this property usage: build error "Property ${config_loc} has not been set").
- D-4: Checkstyle bound to `validate` rather than `verify`; allowed by the brief.
- D-5: `includeTestSourceDirectory` is true so naming, length and import rules cover tests (constitution does not limit them to `src/main`).
- O-1 (SC-27 question, not added): proposed pattern, if the PM and Security rule that SC-27's grep is part of Task 02: a `RegexpSinglelineJava` with format `\b(RestTemplate|RestClient|WebClient|HttpClient|HttpURLConnection)\b|\.openConnection\(` and message `SC-27: outbound HTTP clients are banned in src/main`, no test suppression needed beyond the existing `src/test` one. Not added: the Task 02 text lists four bans only. SC-27 itself ("Dependency tree and grep for HTTP clients") would allow it, but I treated adding a fifth ban as a scope extension as the brief instructs. Note that `import java.net.http.HttpClient` and Spring types would also be caught by the type name.
- O-2: wall-clock and other bans are suppressed for `src/test`; say if tests should be covered by the wall-clock ban.
- F-4: not implemented, noted in `progress.md` (a proposal without a new plugin, not built: a Surefire test that runs `dependency:tree` and compares it with `config/dependency-tree.txt`; it runs Maven inside a test, so slower and needs the wrapper available).
- R-1: no written "FSB 1.14.0 requires SpotBugs x" statement was found in the artifacts; compatibility rests on the successful run. FSB 1.14.0 is from April 2025 while SpotBugs is 4.10.5 (October 2026): a future SpotBugs bump may need an FSB check.
- R-2: Checkstyle and SpotBugs analyze 4 files and 1 class today; the seeded runs are the proof the gates fire. `ClockConfig` suppression covers all wall-clock forms in that one file (see row 1c).
- Commit atomicity: the Checkstyle commit also contains the SpotBugs part of `pom.xml` (single file edit).

## 6. Status
IN_REVIEW is to be set by the orchestrator (status.md untouched). STOP: Task 03a not started. Waiting for the Liaison audit, Security verdict, PM acceptance and Sponsor approval.

## 7. Follow-up round (handoffs 043 and 044), same branch, commits ending `[Task-2]`
- SEC-F-089: added `RegexpSinglelineJava` ban "SC-27: outbound HTTP clients are banned in src/main" for `RestTemplate|RestClient|WebClient|HttpClient|HttpURLConnection|URLConnection`, `.openConnection(`, `.openStream(`, `java.net.http`. `SC-27` added to the `src/test` suppression (the statement in O-1 that none was needed was wrong). Seeds: in `src/main` lines with `java.net.http.HttpClient` import, `new RestTemplate()`, `.openConnection()`, `.openStream()`, `java.net.HttpURLConnection` all fail with the SC-27 message; the same banned text in `src/test` passes (0 violations). Seeds reverted.
- SEC-F-090: time rule now also matches `new\s+(java\.util\.)?Date\s*\(` and `\.\s*now\s*\(`. Seeds in `src/main` (all flagged, reverted): `new java.util.Date()`, `new   Date ()`, `.now();` on a continuation line, `java.time.Year . now ()`, `Instant.now ()`.
- Liaison item 3: pom comment "SpotBugs must stay declared before the JaCoCo check so it fails first".
- SEC-F-092: the `src/test` suppression path is anchored to `[\\/]backend[\\/]src[\\/]test[\\/]`; behaviour otherwise unchanged.
- Corrected proposed pin-table text (replaces section 4):
  - Checkstyle: `maven-checkstyle-plugin` 3.6.0 (Central publishes SHA-1 only; no SHA-512 is published for the plugin jar) and engine `com.puppycrawl.tools:checkstyle` 14.3.0 (SHA-1 and SHA-512 equal), VERIFIED 2026-11-02, resolved together under JDK 21.0.12.1 and Maven 3.9.16.
  - `spotbugs-maven-plugin` 4.10.5.0 (engine 4.10.5), SHA-1 equal, latest stable; it analyzed Java 21 class files without error in the Task 02 run.
  - `findsecbugs-plugin` 1.14.0, SHA-1 equal; loaded by SpotBugs 4.10.5 and detects `WEAK_MESSAGE_DIGEST_MD5` and `PREDICTABLE_RANDOM`. Caveat R-1: no documented SpotBugs compatibility statement was found (compatibility rests on the run); a SpotBugs bump needs an FSB re-check. SpotBugs must stay declared before the JaCoCo check in `pom.xml`.
