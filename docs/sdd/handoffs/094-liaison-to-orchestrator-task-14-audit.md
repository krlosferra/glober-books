# Handoff 094: Liaison audit, Task 14
- From: liaison
- To: orchestrator
- Phase: 4   Round: 1
- Type: REVIEW_REPORT
- Mode: F (with G for the guard oracle)
- Artifacts reviewed: handoffs 092, 093; tasks.md Task 14; README.md; progress.md (0b note, Task 1 proof table); e2e/support/fixtures.ts, library.ts; e2e/tests/us-010-library-view.spec.ts. Orchestrator re-run (node 43 pass, smoke 3 passed, no leftovers, commits clean) taken as stated. I did not re-run anything. builders.ts was not read.

## Verdict: ACCEPT WITH FOLLOW-UPS (Liaison only; Security verdict and Sponsor gate still owed)

## Criteria
1. Smoke: met. Expected values come from the spec (US-010.4 count wording, US-011.2 keyboard flow). The xss test asserts no element, no dialog, and the guard covers CSP. M-1 is recorded and bounded. Reset is not used (see item 3).
2. Guard: met. Violations, CSP, Trusted Types, page errors, foreign origins, Content-Type and headers are all checked. Negative controls cover each class except a wrong header (not provoked: see item 8).
3. README: met, with the jar-path deviation (item 1). Content matches brief constraint 4. No tool or vendor names.
4. verify, node, smoke and hygiene green: per delivery and the orchestrator re-run. The progress.md note and the proof table are present.

## Rulings
1. Jar command: ACCEPT the README form (`cd backend && java -Xmx512m -jar target/globerbooks.jar`). The criterion text is wrong against the actual behavior, so the PM must amend Task 14 criterion 3 (and the brief wording) to the `backend/` form. A path fix is NOT owed to Task 34a unless the Sponsor wants root-relative start. Task 34a is "Directory trust and clean start failure", so it should at least make the "unable to open database file" failure a clean, explained message. Recommend the PM record this as a note in Task 34a. Do not add it to Task 14.
2. Playwright tool-server exploration: recommend the Sponsor ACCEPT the deviation for 0b. The deviation is not a substitute run. The delivery obeyed the brief (no script substituted). The smoke tests plus the guard cover keyboard add, inert title and CSP in a real browser. Not covered: snapshot, console and network inspection, and 360 px. The proof table honestly keeps `@playwright/mcp` 0.0.83 as "no page exploration". Carry the 360 px and tool-server exploration forward as an open obligation to the first task with tool exposure (suggest Task 12 audit item plus this one merged into one named follow-up). The Sponsor decides. This is a recommendation.
3. `library.reset()`: acceptable as written but it is dead, untested code. Binding rule: it must get its first real use and test in the first task that has DELETE (Task 31 area). Record it as a follow-up in tasks.md or progress.md. Do not delete it now. The `remove` function is likewise untested.
4. Permissions-Policy guard: ACCEPT. It is the only way the SEC-F-130 filter means anything, and the constant is exact, the comment names SEC-F-130 and the removal condition. The negative control with a dummy constant proves the constant matches the real message. Risk: a Chromium wording change fails every test loudly (fail-closed). That is acceptable.
5. Proof table: every "Proved" row names real evidence. Handoffs 034, 038, 040, 048, 054, 074, 089 all exist, and the wrapper checksum and `--strict-checksums` are visible in `backend/.mvn`. I did not open the handoffs to confirm the quoted claims (e.g. the seeded raw-type failure, the 15 and 8 test counts). Spot-check those if the Sponsor wants. The two unverified pins (`@playwright/mcp` page exploration, Temurin) are stated honestly. Note the `-Xlint:all -Werror` row cites only Task 01. The proof table says F-4 stays open: fine.
6. Clean-copy proof: ACCEPT. It used `git archive` of the committed tree, only the pinned JDK and Node, and attached real output (verify, smoke, jar start, POST 201, GET 200, process stopped). It also found a defect in the brief's own command, which is the purpose of the proof. Note the clean copy was built from the tree before the final docs commit, which is irrelevant to code.
7. Diff stat: the delivery states "before this final commit". Minor. The orchestrator should include the final `git diff --stat main..HEAD` in the PR text or the closing note. Not blocking. It also lists brief 092 and handoff 093 as part of the diff, which is normal.

## Follow-ups
- F-1 (PM): amend Task 14 criterion 3 wording to the `backend/` jar form. Add a note to Task 34a on the clean failure of a missing data directory or unusable relative path.
- F-2 (Sponsor): accept or reject the tool-server exploration deviation; record as a DEC or progress note, with the open obligation (snapshot, console, network, 360 px) assigned to a named later task.
- F-3 (frontend): first use and test of `library.reset()` and `remove()` when DELETE exists.
- F-4 (frontend, later): a seeded wrong-header control for the header check (not provoked in Task 14), cheap to add with a Playwright route override.
- F-5 (Sponsor): the SEC-F-130 filter is removed in the task that corrects the header value; PM to add a removal line to that task.
- F-6 (orchestrator): remind of `progress.md` and a `[Task-14]` squash commit, and that Security `per task` verdict is required before merge (tasks.md header: Security per task). Branch is `chore/task-14-smoke-guard-readme` and the brief said `feature/...`: the branch matches tasks.md, so the brief was wrong, not the delivery.

## Next step
Security review of Task 14, then Sponsor gate, then PR. Unlocks the close of 0b (v0.2).
