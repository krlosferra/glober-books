# Handoff 146: Audit report, review chunk 32+33 (Task 32 concurrency and fault injection, Task 33 durability)
- From: liaison
- To: orchestrator
- Phase: 4   Round: 1
- Type: AUDIT_REPORT
- Mode: F (with G for the oracles)
- Artifacts reviewed: brief 144, delivery 145, `git diff origin/main..HEAD` (base e8be07a, 6 commits), `ConcurrencyIntegrationTest`, `ConcurrencyRaceIntegrationTest`, `FaultInjectionIntegrationTest`, `DurabilityIntegrationTest`, `support/ApiCalls`, `support/AppProcess`, `support/FaultInjection`, `tasks.md` 0.6.4 (Tasks 32, 33, 34a, 34b), `spec.md` rows C-06, C-07, C-09, C-10, C-13, S-08, S-09, S-10, E-21, `plan.md` start order, `security.md` SC-13, `progress.md`, `status.md`.

## Verdict: ACCEPT WITH FOLLOW-UPS for Task 32 and Task 33 (chunk). Security verdict is separate and not replaced by this report.

ACCEPT Task 32. Compliance: all four criteria met, expected values taken from the spec rows, no production file touched, no `Thread.sleep`. Tests: written to the allowed-set oracle, not to an interleaving.
ACCEPT Task 33 as amended by tasks.md 0.6.4 (criterion 3 narrowed, option (a)). Compliance: criteria 1, 2, 3 (as reworded) and 4 are met. SC-13 creation modes are NOT proved by this chunk and remain open until Task 34a (see F-1).

Evidence caveat: I did not obtain a green `./mvnw -o clean verify` of my own. My run exceeded the 10-minute tool limit and was cut off (rc 144, no result); I did not use its output. The green results below (900 unit, 269 integration, coverage met, BUILD SUCCESS, 4 m 49 s) are the developer's claim in handoff 145. The orchestrator build check should supply the real output before the pull request. A Maven process and its app child that my interrupted run started may still be alive; they are mine, not the developer's (see Hygiene).

## Facts checked myself
- `git diff --name-only origin/main..HEAD`: only `backend/src/test/**`, `docs/sdd/**`. No `pom.xml`, no `backend/config/**`, no `src/main`.
- A-09 grep on `backend/src/main` (create trigger, raise(, faultinjection, appprocess, x-probe, test-only, fault.): no match. The in-test scan covers the same markers over every file of `src/main`.
- `Thread.sleep` in the seven new test files: none. Synchronisation is a `CyclicBarrier` (`ApiCalls.together`) and readiness polling with a deadline (`AppProcess.awaitReady`, 100 ms bounded interval).
- File sizes: 120, 169, 218, 167, 101, 178, 53 lines. All at or under 300.
- Commits: author `krlosferra <carlos.ferra@globant.com>` on all 6; grep of messages and of every added line for claude, anthropic, co-authored, generated, session, claude.ai, openai, copilot: no match. Conventional Commits with `[Task-32]`/`[Task-33]`. No trailer, no session link. Layer 1b passes.
- Leftover processes of the developer's runs: none found by `ps` (only the ones my own interrupted run started, and one unrelated process under the scratchpad).

## Key judgments

### (a) Spec expected values and the S-08 iteration count
- C-06 (1x201 + 1x409 LIBRARY_FULL, 5,000 books), C-07 (1x201 + 1x409 DUPLICATE_BOOK, 1 book), C-09 (10 + 10, 5,000 books), C-10 (1 + 19, then 20, 21 books): the literals in the assertions equal the spec rows. The oracle is the spec, not observed behaviour. PASS.
- S-08 allowed set (status call 200; rating call 200 or 400 `RATING_NOT_ALLOWED`; final always `reading`, rating null, date_completed null, plus INV-1..INV-4 through `assertInvariants`) equals the spec row. S-09 allowed set (every answer 200 or 404 `BOOK_NOT_FOUND`, INV-1 and INV-4 on a surviving book) equals the spec row. PASS.
- Which governs, 200 or 20: by the authority order the spec wins on content, but the iteration count is a delivery tier, and `tasks.md` section 7 is "the single authority" for tiers. The split is deliberate and documented: D2 in handoff 023 (S-09 20 iterations P0), `plan-appendix.md` line 74, handoff 022 line 175, `tasks.md` lines 709-714, and Task 51 criterion 1 ("S-08 and S-09 run 200 iterations"). So 20 is the correct P0 count for Task 32 and 200 is owed by Task 51, not here. The developer's 20 is right and his deviation note is accurate.
- Does it need a PM fix: yes, a small one, non-blocking (finding F-2). `spec.md` S-08 reads as if 200 were mandatory in the gate, and C-13 has the same pattern (spec 50 iterations, P0 gate 5 per operation, 50 in Task 51). Proposed text for the PM, spec rows S-08 and C-13: append "Gate tiers: 20 iterations at the P0 gate (Task 32); 200 in the release run (Task 51)" and "5 iterations per operation at the P0 gate (Task 33); 50 in the release run (Task 51)". The Sponsor should see the spec edit because `spec.md` is approved.

### (b) Rate against changeStatus race (SEC-F-177)
`ratingAgainstEveryStatusChangeOnOneCompletedBookKeepsInvariantsSecF177`: 60 iterations on one reused book, targets reading, unread, completed in rotation, both calls released by the barrier, status answer must be 200, rating answer in {200, 400 RATING_NOT_ALLOWED}, INV-1 to INV-4 asserted on the stored book, final status equals the target. No interleaving is asserted; no sleep. PASS. The observed counts in the delivery are consistent (for target `completed` the final rating is 4 in every run, which is also what the spec allows). Gap noted: S-09 ended ABSENT in 20 of 20 runs (delete always wins in the end), so the branch "book survives, check INV-1..INV-4" in S-09 never executed. Not a defect, the oracle is correct; see follow-up F-4.

### (c) Forced failure
- Trigger `AFTER UPDATE OF status ON books ... RAISE(FAIL, ...)` created through the application's own datasource, test side only. The row update has happened when the statement fails, and `FAIL` does not undo it, so only the transaction rollback restores the book. That is the right way to prove rollback.
- Answer `500 {"error":"Unexpected error.","code":"INTERNAL_ERROR",...}` without `details`; the test also asserts no marker, trigger, raise, sqlite, select, update, exception or books text. Book before and after compared as full body (including rating and dates), with rating 3 and `date_completed` asserted explicitly. After `disarm`, the same request gives 200 with `unread`, rating null, date null. 5 repeated failures each followed by a timed GET under the 5 s pool timeout: pool not wedged (SEC-F-167/168). A second case (unread to completed) is included. A-09 source scan present. PASS.
- Caveat for the record (MINOR, F-5): BR-004 says "after the first of the two changes", but the JPA update writes status, rating and date_completed in one statement, so the trigger fires after that single statement. The proof is still that a failure inside the unit of work leaves nothing behind; the test name says "after the first change", which is slightly more than what is exercised. Reword the display or comment, no code change.

### (d) Task 33 amendment, option (a), and the 34a split
- Finding behind it is real: the packaged jar uses Boot's auto-configured data source; `SqliteDataSourceProvider` is used only by tests; the jar creates `library.db` at 0644 and does not create the data directory. This was known in outline (status.md line 34: Task 14 depends on "the data directory is created by Task 34a"; plan.md line 43 puts creation in Task 34a), so Task 33 was never able to prove creation modes; the ordering gap was in the dependency list, not in the developer's work. The developer stopped correctly and did not weaken the test.
- Amendment of tasks.md 0.6.4: acceptable and honest. The new criterion 3 states exactly what the test asserts (modes never wider than 0700/0600 after start, each of four operations, normal stop, kill, restart; owner is the test user; companion files too), it says in the text itself that creation is moved to 34a, the Traces line says "widening part only", and 34a gets criteria 4 and 5 that carry the creation proof (empty temporary location, directory 0700, file 0600 before the first connection, journal/companions no wider than 0600, uncreatable directory and SEC-F-098/140 failure). Task 33 and 34a/34b dependencies are corrected ("Task 33 does not depend on 34a or 34b"). Nothing is silently dropped.
- Is Task 33's acceptance honestly met: yes, as reworded. Be explicit that the obligation did not disappear: SC-13 (directory 0700, file 0600 created before the first connection) is unmet by the shipped jar today. Until 34a lands, no start of the packaged application on a clean location is SC-13 compliant. Do not tag or release any increment before 34a; record SC-13 as NOT_STARTED/at risk in `security.md` through the Security reviewer, and make sure the 34a brief and gate include the mode assertions.
- Limits of what criterion 3 now proves: the pre-created 0600 file never gets widened, but no `-journal` file existed at any kill point (kill is after commit), so the "companion files" clause is exercised vacuously. That is acceptable here; the hot-journal case is Task 34b (R-20). Say so in the 34a/34b briefs rather than claiming it was tested.
- Governance points on the amendment:
  - F-3 (MINOR): the change to `tasks.md` is committed on a `test/` branch, whose rule is "tests only" (tasks.md section "Numbering", git-workflow). `tasks.md` is PM-owned; the commit identity is the human's, so I cannot tell who drafted the text. Confirm that the PM authored 0.6.4 (the "Awaiting the Sponsor's decision" wording suggests so) and that the Sponsor accepts a docs change travelling with the test pull request. Previous chunks did the same with handoffs and `progress.md`, so I do not block.
  - The header `Status:` line and the 0.6.4 changelog say "Draft (Phase 3, round 3 of 3)"; that line is stale and predates this work. PM housekeeping, not a finding on the chunk.
- The proposed split of 34a: the size diagnosis is right (4 production entries, 5 criteria, estimate 40 will be exceeded). Counter-proposal on the cut line (recommendation, not a decision):
  - Proposed by PM: 34a-1 = wiring + create-with-mode (criteria 4, 5), 34a-2 = refusals + reporting (criteria 1 to 3), 34b after 34a-2.
  - Concern: after 34a-1 the packaged application would run on `DataDirectoryGuard` that creates but does not yet refuse a symbolic link, foreign owner or wide modes. A wired guard that creates and trusts is a security regression window for SEC-F-098 and SC-39, and the Sponsor's branch/merge cadence means 34a-1 would be merged to `main` alone. It also makes the guard class change in two tasks.
  - Alternative: 34a-1 = `DataDirectoryGuard` complete (creation with 0700/0600 and every refusal), `SqliteDataSourceProvider` file-identity edit, unit tests (criteria 1 and 2); 34a-2 = the data source configuration class that replaces the auto-configured one, `StartupFailureAnalyzer`, the jar-start and refusal integration tests (criteria 3, 4, 5). The guard exists and is unit-proved before it governs the jar, and the first merged state is not weaker than today. Either way, 34b depends on the second part. I recommend the alternative; if the Sponsor prefers the PM's cut, require that the guard in 34a-1 already refuses link, owner and wide modes (i.e. do not move that part out). The Sponsor decides; the PM re-issues the criteria and the Security reviewer should be asked about the cut.

### (e) Kill tests
- 4 operations x 5 SIGKILL rounds (`Process.destroyForcibly`) right after the `2xx`, then restart on the same directory, comparison of the post-restart GET with the acknowledged body (full-body equality, an oracle taken from the acknowledged response, not from code behaviour), delete shown by 404 `BOOK_NOT_FOUND`, totals checked at the end. Normal stop test: SIGTERM, exit 0 or 143, list identical after add, status, rating and delete. Console: captured to a file; the marker `ZQXDATA` is inside every title and author and must not appear; the console must contain `Started GloberBooksApplication` so an empty file cannot pass. `@AfterEach` kills everything, asserts not alive, and checks the console; shutdown hook as safety net. No leftover processes after the developer's runs. Command `java -Xmx512m -jar ... --server.address=127.0.0.1`, a free port chosen by the OS (never 8080 or 18080), allowlisted environment. PASS.
- Scope notes: the kill is a process kill; it proves that acknowledged writes reached the OS, not behaviour under power loss (M-3 says process kill, fine). 20 rounds in total against C-13's 50 is the documented tier split (F-2).
- Run time: about 32 s per test (5.3 to 5.7 s per JVM start, 6 to 11 starts per test), 162 s for five, none over the 60 s threshold of the brief, whole `verify` 4 m 49 s versus 2 m 07 s before Task 33. Acceptable for a P0 gate that runs once in `verify`; I accept it. Follow-up F-6: Task 51 (50 rounds) would cost about 10 minutes of start-up alone; plan it as a separate or tagged run, not as part of every `verify`, and record the time in the Task 51 brief.

### (f) File sizes and helper
All files at or under 300 lines. The split of Task 32 into three test classes by theme is allowed by the brief ("split by theme if it passes 300 lines"). The shared helper `support/ApiCalls` goes beyond the files listed in Task 32/33, but it is test support, adds no dependency, is used by all four classes and by `AppProcess`, and is documented. Accepted as a deviation; the PM should list it in the file lists of Tasks 32 and 33 when next amending (F-7, MINOR). `support/FileFacts` already existed. The existing tests under `src/test` were not modified.

### (g) Attribution
None. See "Facts checked myself".

## Findings

| ID | Severity | Location | Finding | Evidence / rule | Proposed resolution |
|---|---|---|---|---|---|
| F-1 | MAJOR (carried, not a defect of this chunk) | packaged jar, SC-13, Task 34a | Shipped jar creates `library.db` 0644 and does not create the data directory; SC-13 creation is unproved until 34a | delivery 145 "Production gap"; security.md SC-13; plan.md line 43 | 34a carries criteria 4 and 5; no release or tag before 34a; Security to record SC-13 status |
| F-2 | MINOR | `spec.md` S-08 (200 times), C-13 (50 iterations) | Spec rows state the release counts; the P0 gate counts (20, and 5 per operation) live only in `tasks.md` and the plan appendix | tasks.md 409, 711-714; Task 51 criterion 1 | PM adds a tier sentence to both rows; Sponsor approves the spec edit. No code change; 200 and 50 are owed by Task 51 |
| F-3 | MINOR | `tasks.md` 0.6.4 on a `test/` branch | PM-owned file changed on a test-only branch; header Status line stale | tasks.md "Numbering": `test/` branches contain tests only | Confirm PM authorship and Sponsor acceptance; PM refreshes the Status line |
| F-4 | MINOR | `ConcurrencyRaceIntegrationTest` S-09 | Every run ended ABSENT, so the "book survives" branch with INV checks never ran; constant `SPEC_ITERATIONS` is named after the spec but is 20 | observed finals ABSENT x20 | Optional later: add a race without the delete (status completed against DELETE rating and PUT rating) and rename the constant `GATE_ITERATIONS`. Not blocking |
| F-5 | MINOR | `FaultInjectionIntegrationTest` display name, `FaultInjection` Javadoc | "After the first change" claims more than a single JPA UPDATE shows | BR-004 | Reword the name/comment to "failure inside the status change" |
| F-6 | MINOR | `DurabilityIntegrationTest`, run time | `verify` grew by about 2.7 minutes; Task 51 at 50 rounds would add about 10 minutes | delivery 145 timings | Keep 5 rounds in `verify`; separate/tagged run for Task 51 |
| F-7 | MINOR | tasks.md Tasks 32, 33 file lists | `support/ApiCalls`, split test classes, `support/AppProcess` Javadoc says the application creates the data directory when missing (false for the current jar) | `AppProcess.start` Javadoc | PM lists the extra files; developer corrects the Javadoc in the next backend change or in 34a when creation exists |

No BLOCKER. No Layer 1, Layer 2 or Layer 1b violation found. Layer 3: tests are readable, the helpers are small and single-purpose.

## Questions for PM / Sponsor
1. Cut line of Task 34a: PM's cut (wiring and creation first) or the Liaison alternative (guard complete first, then wiring and reporting)? Recommendation: the alternative, so the first merged state never weakens SEC-F-098/SC-39. Sponsor decides; PM re-issues 34a/34b criteria and dependencies.
2. Spec rows S-08 and C-13: accept the tier sentence (F-2)? Recommendation: yes.

## Follow-ups
1. PM: apply F-2 (spec rows), F-3 (Status line), F-7 (file lists), and fix Task 34a/34b per the decision above.
2. Security reviewer: review the chunk; note the server log (not the client) prints the trigger text on WARN and the exception class on ERROR in the forced-failure test (developer's note in 145), and decide on SC-13 status in `security.md`.
3. Developer (next backend change): correct the `AppProcess.start` Javadoc; optional F-4 and F-5.
4. Orchestrator: supply a green `./mvnw clean verify` output (I could not obtain mine within the tool time limit); then update `progress.md` and `status.md` audit columns (Tasks 32 and 33: ACCEPT WITH FOLLOW-UPS, handoff 146) and, after the Sponsor's approval at the gate, open the pull request. The commit message for acceptance bookkeeping: `[Task-32]`/`[Task-33] ... - ACCEPTED`.
5. Task 34a brief must carry: the mode assertions of criterion 4, reuse of `support/AppProcess` (change its start to accept a missing directory), and the note that the journal clause of Task 33 was exercised vacuously.

## Hygiene note
My attempt to run `./mvnw -o -q clean verify` was cut off by the tool time limit. It may have left a Maven JVM and an app child (`java -Xmx512m -jar .../backend/target/globerbooks.jar`) running, and it rewrote `backend/target/`. The orchestrator should check `ps` and stop them if still present (I did not kill them, since my Bash rules are read-only). I did not edit any file other than this handoff.
