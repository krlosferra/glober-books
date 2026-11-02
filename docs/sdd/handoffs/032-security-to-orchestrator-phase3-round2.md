# Handoff 032: Phase 3 Security round 2 (diff check)
- From: security
- To: orchestrator (copy to liaison, pm)
- Phase: 3   Round: 2 of 3
- Artifacts: `plan.md` 0.2, `plan-appendix.md` 0.2, `tasks.md` 0.4, `spec.md` 0.4.3, handoffs 026 to 030, `security.md` 0.16
- Type: AUDIT_REPORT

## 1. Scope and method
Diff check limited to SEC-F-066 to SEC-F-079 and to what the edits introduced. Read-only. I read the applied plan, appendix and tasks (Tasks 03a, 03b, 06 to 09, 11, 13, 14, 16, 17, 34a, 34b, 41 to 43, 45, 49), and ran `git diff` of `spec.md`. Nothing was executed; the application does not exist, so every closure below is "closed at design" and is proved by test in the named task.

Spec diff base: handoff 030 names 98bad0d, but that commit already holds the 0.4.2 text (the diff against it shows "-Version 0.4.2"). I therefore diffed against 6016a53 (spec 0.4.1, the approved text): 30 insertions, 15 deletions. Every changed line carries a CR marker, a "[housekeeping, awaiting diff check]" marker, is a DEC or changelog row, or is the header ("Last updated" date, version, status). No unmarked change of a rule, endpoint, limit, status or Appendix D row other than E-28 (marked CR-8). The CR-9 line of US-009.5 and the closing clause match the Liaison's proposal. Section 6 limit and US-013.6 sentence match SEC-F-075.

## 2. Verdict: PASS (design level) for the Phase 3 gate
No CRITICAL, no HIGH, and no MEDIUM open as a gate condition. Seven MEDIUM conditions of round 1 are closed at design level except SEC-F-071, which is partly closed (a LOW remainder is a condition on Task 41). Two new LOW findings are conditions on Tasks 13 and 41, not on the gate. Sponsor decisions remain open (section 5).

## 3. Round 1 findings
| ID | Result | Reason |
|---|---|---|
| SEC-F-066 | CLOSED | Plan D2 and Task 06 (production 4, AC5): unconditional `showReport=false` / `showServerInfo=false`, MVC framework exceptions mapped; Task 08 AC1 names the 404 and 405 triggers and AC3 is the raw-socket check; Task 17 AC3 tests that an error dispatch cannot bypass the headers. Proof is by test in 08 and 49. |
| SEC-F-067 | CLOSED | `StartupPolicy` in Task 03b (0a, before `v0.1`); URL form fixed, only the file configurable (plan section 2, Task 03b AC1). |
| SEC-F-068 | CLOSED | AP-12.1 order (structure with DDL comparison, `quick_check`, bounded rows with `LIMIT 5001` and byte pre-checks, code points in Java, fixed reasons); Task 34b AC1 and AC2 carry all five cases. The U+0000 rule is a Sponsor confirmation (plan section 8, item 11), correctly framed. |
| SEC-F-069 | CLOSED | Task 03a AC1 (stat-only), AC2 (settings at every connection, pool 1/1/0/0/0, forced-eviction read-back), AC3 (child JVM `ClaimChildMain`, fallback decided in 03a); AP-12.2. |
| SEC-F-070 | CLOSED | Task 13 AC2 (command-line arguments, allowlisted environment, `mkdtemp` checks, deletion rules) and AC3 (hostile run); AP-9.2. Residual details in SEC-F-080. SC-33 sentence stays a Sponsor amendment before Task 13, correctly framed (plan section 8, item 1). |
| SEC-F-071 | PARTIALLY CLOSED | Plan D7 and X-12, Task 07 AC5 (stream, no `byte[]`), Task 41 AC4 (fixed `-Xmx`, eight slow readers, no `OutOfMemoryError`, serves afterwards). Remainder in SEC-F-081. |
| SEC-F-072 | CLOSED | Plan D4, Task 09 (constructor argument; test builds the catalog directly), Task 03b AC2 (no property). |
| SEC-F-073 | CLOSED | Task 16 AC5 (drip variants; deadline or reported limit), explicit `max-swallow-size` in Task 03b AC2 and plan D5. Sponsor items 6 and 7 correctly framed. |
| SEC-F-074 | CLOSED | Spec CR-9 clause, plan step 2 (size test skipped with a non-empty `-journal`), Task 34b AC3 (i) to (iii). R-20 stays a Sponsor item, correctly framed. |
| SEC-F-075 | CLOSED | Spec US-013.6 and Section 6; Task 11 AC5 (grep accepts a function argument, flags a string). |
| SEC-F-076 | CLOSED | Task 43 AC1, AC3, AC4 and Task 45 AC2 carry conditions (a) to (d). Pin verification still happens at Task 43 under SC-21. |
| SEC-F-077 | CLOSED | Task 08 AC6 and Task 14 AC3. |
| SEC-F-078 | CLOSED | Plan D5 wording corrected; X-03 caveat; Task 03b AC3 and Task 41 state ASK FIRST. |
| SEC-F-079 | CLOSED | Task 49 AC2 asserts no `Allow` list for a container answer. |
| Section 5 of handoff 026 (D3) | CLOSED | Task 42 AC4 covers `e2e/` audit and lockfile hosts. |

## 4. New findings (caused by the edits)
| ID | Severity | Location | Finding | Remediation | Control |
|---|---|---|---|---|---|
| SEC-F-080 | LOW | Task 13 AC2 and AC3, AP-9.2 | The launcher fixes arguments and environment but not the working directory of the child. Spring Boot reads `./config/application.*` and `./application.*` from it; they cannot override the three command-line values but can add other properties. AC3 also lists only three hostile variables while AP-9.2 lists five (`SERVER_ADDRESS=0.0.0.0`, `SERVER_PORT`). AC2 names the three settings with their environment-variable spellings; the plan's argument names are `--server.address`, `--server.port`, `--spring.datasource.url`. | Task 13 AC2: child `cwd` set to a directory the launcher owns (its `mkdtemp` directory) holding no config files; AC2 uses the argument names; AC3 includes the five variables of AP-9.2. | SC-33 |
| SEC-F-081 | LOW | Task 41 AC4, Task 14 AC3, AP-12.4 | The memory bound is applied to the C-11 (c) test JVM. The README run command of Task 14 is `./mvnw spring-boot:run` with the default heap, and Task 41 AC4 says the value is "written into the README run command" without listing the README in its production files. AP-12.4 says `java -Xmx512m -jar`. A user following the README has no bound, which was the point of SEC-F-071. | Task 41 lists `README.md` (edit); the README gives both the jar command with `-Xmx` and the `spring-boot:run` form with `-Dspring-boot.run.jvmArguments=-Xmx...`; Task 14 may carry the first value so that `v0.2` is not lower than the later text. | SC-17, SC-06 |
| SEC-F-082 | INFO | plan.md risk N-1 | Cites "CR-8" for the hot-journal rule; that change is CR-9 (CR-8 is TRACE). | Fix at the next plan edit. | none |

INFO notes (no action required now): (a) when Hikari evicts and replaces the single connection after a fatal error, the old connection's close releases the lock before the new claim, so a second instance could win in that window (availability only, fatal-error path); (b) Task 41 AC4 runs the application in the test JVM, so test data shares the heap, which is conservative for the bound; (c) the definition of "loopback" in `StartupPolicy` (literal `127.0.0.1`, `::1`, `localhost`) should be fixed in the Task 03b test cases; `0.0.0.0` and empty are the named refusals.

## 5. Controls checked (design level)
| Control | Result | Evidence |
|---|---|---|
| SC-01, SC-39 | design VERIFIED on paper | Task 03b |
| SC-02, SC-11, SC-29 | design VERIFIED on paper | Tasks 06, 08 AC3, 17, 49 |
| SC-06 | design VERIFIED | Task 03b AC2 and AC3, Task 16 AC5 |
| SC-12 | design VERIFIED on paper | Task 09, plan D4 |
| SC-17 | design open (small) | SEC-F-081 |
| SC-32 | design VERIFIED on paper | Tasks 03a, 34b, AP-12 |
| SC-33 | design VERIFIED on paper, SC-33 amendment pending (Sponsor) | Task 13, SEC-F-080 |
| SC-21 (axe) | pending at Task 43 | Task 43 AC1 |
All statuses in `security.md` stay `NOT_STARTED` or `IN_PROGRESS`; nothing is `VERIFIED` without running code.

## 6. Sponsor items (plan section 8; handoff 027 section 5): are they correctly framed
- Item 1 (SC-33 amendment): yes; Security accepts the wording of handoff 029 M-1. Needed before Task 13.
- Item 2 (native SQL paragraph; per-connection keys through the wrapped data source): yes. Security view: acceptable, because the keys and their effect equal the URL parameters and the claim cannot be one `connection-init-sql` statement; constitution line 119 may still need the Amendment Procedure if the Sponsor reads it literally.
- Item 3 (schema fallbacks ASK FIRST): yes.
- Item 4 (35.8 h): not a security matter.
- Item 5 (race between directory check and open; R-20): yes. Recommendation: accept both. The race needs same-user write access to the data directory (R-08 actor) and is documented, not tested, because A-09 forbids a test-only switch.
- Item 6 (`-Xmx`, `max-swallow-size`): yes. See SEC-F-081.
- Item 7 (drip deadline): yes.
- Item 8 (approval of CR-1 to CR-9 and DEC-034): Security has no objection to any CR (diff in section 1).
- Item 9 (axe bypass): yes, it matches the Sponsor decision of 2026-11-02.
- Item 10 (focus assumptions): no security effect.
- Item 11 (U+0000 in a stored cell): yes. Recommendation: confirm it as covered by "invalid format"; refusing is the fail-closed choice.

## 7. Not verified
Everything about Tomcat 11, sqlite-jdbc, Hikari, Spring Boot 4 and Playwright behaviour stays knowledge, not test (listed as assumptions in AP-12.5 and proved in Tasks 03a, 04, 06, 08, 13, 43). Public advisory databases for Maven are unreachable (R-11). I did not read the Task 18 to 33 or 36 to 40 acceptance criteria beyond those cited, because round 1 did not flag them and the edits did not touch them in the handoffs I was given.

## 8. Residual risks for the Sponsor
R-20 (hot journal beside an invalid file): recommend accept. The documented limit of Task 34a AC2: recommend accept. P-05 (MCP from a lockfile) can be decided at Task 10; interim R-12 stands.

## 9. Next step
The PM adds SEC-F-080 and SEC-F-081 to Tasks 13, 14 and 41 (acceptance criteria only) and the plan owner corrects the N-1 reference at the next edit. The Liaison verdict (handoff 031) and the Sponsor decisions in section 6 then complete the gate. I update `security.md` (0.17) and the Security cell of `status.md`. I edited no plan, task or spec file.
