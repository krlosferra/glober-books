# Handoff 031: Liaison diff check of Phase 3, round 2
- From: liaison
- To: orchestrator (copy to pm, java, frontend)
- Phase: 3   Round: 2 of 3
- Artifacts: `plan.md` 0.2, `plan-appendix.md` 0.2, `tasks.md` 0.4, `spec.md` 0.4.3, `delivery-strategy.md` (0a/0b row), handoffs 025, 027, 028, 029, 030
- Type: REVIEW_REPORT

Mode: D (plan and task review) with B (spec diff check) and G (oracle check). Doctrine: Module 1 (spec above plan above tasks), Module 3 (ASK FIRST tier), Module 4 (oracle), Module 5 (granularity and dependency review). Scope as set by handoff 030: my 24 round 1 findings plus anything the edits introduced. Sponsor decisions of 2026-11-02 are not reopened.

## Verdict
**APPROVED WITH CHANGES.** No BLOCKER. Two MAJOR findings, both new and both caused by the round 2 edits to `tasks.md` (R2-01, R2-02); eight MINOR. All 24 round 1 findings are closed in the documents, except F-10 which stays open until the Sponsor amends SC-33 (correctly framed as a Definition-of-Ready condition of Task 13).
- `tasks.md` 0.4: APPROVED WITH CHANGES (R2-01, R2-02, R2-04, R2-05, R2-06, R2-07, R2-08, R2-09).
- `plan.md` 0.2: APPROVED WITH CHANGES (R2-03).
- `plan-appendix.md` 0.2: APPROVED (R2-10 is cosmetic).
- `spec.md` 0.4.3 CR text (CR-1 to CR-9): ACCEPT, conditional on the mechanical `git diff` below.

## 1. Spec diff check (the item I could not do in round 1)
**Limit.** My toolset in this session has no shell and no git, so I could not run `git diff 98bad0d -- docs/sdd/spec.md`. I compared by reading. An unmarked edit therefore cannot be excluded by me; the orchestrator must run the command and apply the expectation below. Do not treat this section as the mechanical confirmation.

**What I verified by reading.** Every edit named in handoff 027 section 3 exists and carries a marker, and the wording matches what I proposed in round 1:
| Location | Marker found | Matches round 1 proposal |
|---|---|---|
| Header (version 0.4.3, date 2026-11-02, status line) | listed housekeeping | n/a |
| US-009.5 (CR-9, line 143) | `[CR-9, awaiting diff check]` | yes, F-14 closing clause: invalid journal ignored, outcome judged after the rollback |
| US-010.2 (CR-2) | marked | yes |
| US-010.7 (CR-2) | marked | yes, "first successful list load that was requested after the add completed" (F-15.1) |
| US-010.10 (CR-2) | marked | yes, "no list load is in progress" (F-15.2) |
| US-011.3 (CR-3), US-011.5 (CR-2) | marked | yes (F-15.2) |
| US-013.4 (CR-5) | marked | yes, window "between the arrival of its outcome and the end of its handling" (F-13) |
| US-013.6 (CR-4) | marked | yes, abandoned request may have taken effect (F-18, SEC-F-075) |
| US-014.2 (CR-1 and CR-6) | marked | yes |
| BR-012 step 1(c) | `[housekeeping, awaiting diff check]` | yes (F-18) |
| Section 5.9 `VALIDATION_ERROR` (CR-7) | marked | yes, usable entry defined (F-17) |
| Section 6, accepted limit | cites SEC-F-075, CR-4 | yes |
| E-28 (CR-8) | marked | n/a |
| DEC-033, DEC-034, changelog 0.4.3 and 0.4.2 | DEC-033 "awaiting diff check (CR-8)", DEC-034 PROPOSED names CR-2 as a behaviour change | yes (F-15.3) |

**Expectation for the mechanical diff.** Hunks only in: header lines 4 to 6 (version, date, status), US-009.5, US-010.2, US-010.7, US-010.10, US-011.3, US-011.5, US-013.4, US-013.6, US-014.2, BR-012 step 1, Section 5.9 `VALIDATION_ERROR` row, Section 6 (one bullet), E-28, DEC-033, DEC-034, the changelog. Counts must stay 102 criteria and 106 Appendix D rows. Lines I could not attribute by reading and that the diff must show as unchanged against 0.4.1: line 8 (the "Pre-application answer" convention), BR-012 step 2 (TRACE wording), and the Appendix D introduction. They carry no marker; if they changed at all they are CR-8 text from 0.4.2 and must carry one.

## 2. Round 1 findings, one by one
| ID | Status | Reason |
|---|---|---|
| F-01 | CLOSED | All ten dependencies present in `tasks.md` (11 on 10; 17 on 06, 08, 09; 20 on 07; 22 on 04, 05, 06; 28 on 14; 29 on 28; 39 and 40 on 14; 44 on 28; 45 on 27, 38; 48 on 49, 50; 31 on 15, 16). New gaps are R2-01, R2-02. |
| F-02 | CLOSED | Task 15 AC4 and plan X-11: table holds existing routes; Task 31 lists `RouteTable.java` (edit), depends on 15 and 16, AC1 keeps `RouteTableTest` green. |
| F-03 | CLOSED | `INVALID_STATUS` of the list is Task 16 AC2 (`QueryPolicy`), AP-2 row 8 names the owner, Task 23b depends on 16 and keeps filter and order only. |
| F-04 | CLOSED | Task 23a AC2 limited to add at capacity; C-03, C-04 and US-008.3 in Task 31 AC4; Increment gates split accordingly. |
| F-05 | CLOSED | Task 22 lists `GlobalExceptionHandler` (edit) with AC4; AP-1 matches. |
| F-06 | CLOSED | Splits 03a/03b, 23a/23b, 34a/34b; the dense-task rule is in section 0; plan N-6 and section 0 list the same nine tasks. The swap-between-check-and-open claim is a documented limit in 34a AC2 (A-09 respected); agreement is a Sponsor matter. |
| F-07 | CLOSED, with R2-02 | The Definition-of-Done rule for UI tasks is in section 0 and the six tasks say "Tested by Task N". The rule it created cannot be met as written (R2-02). |
| F-08 | CLOSED, with R2-04 | Release scope, Task 48 order and dependencies, totals restated in `tasks.md` and plan section 7. Arithmetic slip in the totals table (R2-04). |
| F-09 | CLOSED | Task 43 drops the pending decision; mechanism stated (no bypass first; dedicated context only inside `us-014-accessibility.spec.ts`; never for `guard` tests; Task 45 without bypass). AP-10, AP-9.3 and plan F-axe agree. |
| F-10 | OPEN (Sponsor) | Governance side done: Task 13 Definition of Ready requires the SC-33 amendment; launcher design recorded in plan section 4 and AP-9.2; Task 13 AC4 records the observed start order. `constitution.md` is unchanged, which is expected. Correctly framed as plan section 8 item 1. |
| F-11 | CLOSED in text, Sponsor approval pending | Plan has the "Native SQL and JDBC" paragraph and the statement that the provider settings use the URL keys; fallbacks read "stop and request approval" in plan, Task 04 AC1 and Task 23b AC3. Correctly framed as plan section 8 items 2 and 3. Two plan promises are not mirrored in tasks (R2-06). |
| F-12 | CLOSED | `tasks.md` section 7 is the only register; AP-5 and AP-9.6 are pointers with the corrections listed (E-04 at Task 08, E-34 row 1, T-14b, no US-011.11). |
| F-13 | CLOSED | Spec US-013.4 states the window; AP-8 Live regions, Task 26 AC5 and Task 44 AC4 name the same window. |
| F-14 | CLOSED, with R2-09 | Spec closing clause present; Task 34b AC3 has cases (i) to (iii). Oracle of (ii) can be sharper (R2-09). R-20 is a Sponsor item. |
| F-15 | CLOSED | Items 1 to 4 done: spec US-010.7, US-010.10, US-011.5; DEC-034 names the behaviour change; AP-7.1 has the rule for replaced or removed events on cleared entries; Task 25 AC4 tests it. |
| F-16 | CLOSED | AP-8 focus table split between Title and US-011.6; Task 27 AC3 and Task 47 AC2 agree. |
| F-17 | CLOSED | Spec 5.9 defines a usable entry; Task 24 AC3 and Task 44 AC1 follow. |
| F-18 | CLOSED | BR-012 step 1(c), header date, US-013.6 sentence, Section 6 limit. |
| F-19 | CLOSED | (a) and (b) are plan assumptions P-F1 and P-F2, not asserted (Tasks 37 AC4, 38 AC3; plan section 8 item 10 offers the Sponsor the change-request route); (c) Task 12 AC4 reworded, `guard` checks headers without a list of names. |
| F-20 | PARTIALLY CLOSED | Task 42 AC5, Task 08 `application.yml`, Task 43 and branch types 13, 14, 43, 52 fixed. Task 08 and Task 41 still ship non-test files on `test/` branches (R2-08). |
| F-21 | CLOSED | Task 08 AC1 names the 404 and 500 triggers at 0a; AC6 and section 2 give the try-it commands; Task 14 AC4 does the same for 0b; `delivery-strategy.md` changelog and section 5 record 0a/0b; Task 06 owns the error-report configuration. |
| F-22 | CLOSED | Stale ids replaced in the appendix; `harness.spec.ts` in AP-9.1; header reader is named as "inside the provider" (AP-1, plan step 2, Task 34b); traceability 8.2 updated; N-6 extended; appendix declared part of the plan. |
| F-23 | CLOSED | Section 0 Definition of Ready: the Phase 3 gate approves CR-1 to CR-9 and DEC-034 before Task 11 is briefed. |
| F-24 | CLOSED | Tier register 7.2 splits US-010.2. |

## 3. New findings caused by the round 2 edits

| ID | Severity | Location | Finding | Required resolution |
|---|---|---|---|---|
| R2-01 | MAJOR | `tasks.md` Task 12 | `Depends on: 11` only. AC4 ("answered from the static catalog") and AC5 (MCP exploration of the served page) need the static controller of Task 09. Task 25 depends on 12, so the chain from 09 exists only by numbering, which Definition of Done forbids relying on (F-01). | `Depends on: 11, 09`. |
| R2-02 | MAJOR | `tasks.md` section 0 (rule for UI tasks) and Tasks 25, 26, 27, 36, 37, 38 | The rule demands "MCP exploration evidence for each criterion", but (a) `plan-appendix.md` AP-9.7 allows no route, evaluate or storage tools, so criteria that only a stub can provoke (US-010.7 failed load, US-011.9 `LIBRARY_FULL`, US-011.10 network failure, US-012.7 `INVALID_RATING`) cannot be explored; (b) the criteria that a real backend can provoke need backend tasks the UI tasks do not depend on (Task 25 filter: 23b; Tasks 26 and 27 duplicate: 23a; Tasks 36 to 38: 31). Module 4: an acceptance rule that cannot be run is a gate that gets waived. | PM: (1) in section 0 limit the MCP evidence to criteria reachable through the real page against the backend tasks that exist when the task is audited, and say that stub-only criteria are accepted on logic tests plus the named later test task; (2) add the backend dependencies (25 on 23b, 26 and 27 on 23a, 36 to 38 on 31), or state that the evidence is taken at the later test task for those criteria. Frontend (AP-9.7): confirm item (a). |
| R2-03 | MINOR | `plan.md` N-1 | "Spec change CR-8 states it is not a violation": the hot-journal exception is CR-9 (spec US-009.5); CR-8 is the TRACE answer. | Replace CR-8 with CR-9. |
| R2-04 | MINOR | `tasks.md` section 1 | Per-task estimates give Increment 1 = 660 min (backend 410, frontend 250), not 665; so Increments 0 to 3 = 2,055 (not 2,060), release = 2,145 (not 2,150), backend release = 1,240 (not 1,245; 19.25 h in the text is 1,155). Increment 0 (625), 2 (460), 3 (310) and the tasks after the tag (120) add up. The headline 35.8 h and 20.75 h are unchanged by rounding (35.75 h), but the Sponsor is asked to confirm these figures. | Correct the table or the estimate that differs; keep one set. |
| R2-05 | MINOR | `tasks.md` header, Inputs | Cites `plan.md` and `plan-appendix.md` 0.1; both are 0.2. | Update. |
| R2-06 | MINOR | `tasks.md` Tasks 04 and 42 | Two plan commitments have no owning acceptance criterion: (a) plan X-06 and AP-12.1 say the Task 04 spike proves the stored DDL text equals the normalised `schema.sql` statement (Task 34b AC1 compares it, so the proof must exist first); (b) AP-12.3 says a grep gate in Task 42 forbids native queries and JDBC statements outside `config.sqlite` (handoff 028 section C asked for it; Task 42 AC2 does not list it). The latter is what backs the F-11 justification. | Add (a) to Task 04 AC1 and (b) to Task 42 AC2. |
| R2-07 | MINOR | Task 13 AC3 against AP-9.2 and plan section 4 | The hostile run in `tasks.md` sets three variables; AP-9.2 sets five (`SPRING_DATASOURCE_URL`, `SPRING_APPLICATION_JSON`, `JAVA_TOOL_OPTIONS`, `SERVER_ADDRESS=0.0.0.0`, `SERVER_PORT`). Handoff 029 proposed the five. Judgement of sufficiency is Security's. | Align Task 13 AC3 with AP-9.2. |
| R2-08 | MINOR | Task 08 and Task 41 | Section 0 says `test/` branches hold tests only. Task 08 (branch `test/`) ships `progress.md` and a conditional `application.yml` edit; Task 41 (branch `test/`) ships a conditional `application.yml` edit. | State once that `progress.md` is exempt on every branch, and use `chore/` for Task 08 and Task 41 or drop the conditional configuration edit from the `test/` task. |
| R2-09 | MINOR | Task 34b AC3 (ii), Task 28 AC3 | (ii) says the outcome is "judged on the file after the rollback" but does not name the assertion; as an oracle it is weak. Task 28 AC3 says the book "appears with a later load", weaker than US-010.7 ("first successful load requested after the add completed"). | (ii): assert that the hash after the rollback equals the hash after the refusal and that the exit is non-zero with no book data. Task 28 AC3: use the spec wording. |
| R2-10 | MINOR | `plan-appendix.md` AP-7.1 `loadStarted`, AP-11.1 items 4 and 5 | Still label the empty list during a load and the 15 s timeout as assumptions (Q-F3, Q-F6). They are now spec text (CR-2, CR-4, pending approval). | Cite CR-2 and CR-4 instead of Q-F3 and Q-F6. |

## 4. Items for the Sponsor: framing check
Plan section 8 lists eleven items and is more complete than handoff 027 section 5 (eight items). All eleven are framed as decisions, none is resolved in the documents, and each states what it blocks (item 1 blocks Task 13, item 8 blocks Task 11). Remarks:
- Item 4 (35.8 h against the accepted 32 h): ask the Sponsor only after R2-04 fixes the arithmetic.
- Item 11 (U+0000 in a stored cell): BR-006 already rejects NUL (T-01) and US-009.5 requires rows to satisfy "the formats and limits of Section 4", so the validator rule looks like an application of existing text, not a new rule. The PM should confirm that; if the PM agrees, the Sponsor need not decide it.
- Items 5 and 6 bundle several decisions (limit, R-20, CR-9 clause; `-Xmx`, `max-swallow-size`). Acceptable for one gate; list them as separate lines on the gate sheet so each can be answered.
- Item 10 (focus after 404 and after a non-removing request) is correctly framed as "plan assumption or change request"; it is not blocking.

## 5. What passed
Plan 0.2 against the constitution: layering, no new technology, native SQL justified, rollback-journal mode recorded, duplicate key in Java. AP-2 order still matches BR-012 step by step. Plan, appendix and `tasks.md` agree on class names, task numbers and ownership (`TomcatErrorReportCustomizer` Task 06, `StartupPolicy` Task 03b, `ClaimChildMain` Task 03a, `RouteTable` Tasks 15 and 31, `GlobalExceptionHandler` Tasks 06, 20, 22). Task estimates in Increments 0, 2, 3 and the post-tag tasks add up. No scope was added: the only new numbers are the 15 s figure (CR-4) and the Security proposals (`-Xmx`, `max-swallow-size`), both listed for the Sponsor.

## 6. Requested response
- PM: R2-01, R2-02, R2-04 to R2-09 in `tasks.md`; confirm item 11 of section 4.
- Frontend developer: R2-02 (a), R2-10.
- Java architect: R2-03.
- Orchestrator: run `git diff 98bad0d -- docs/sdd/spec.md` against the expectation in section 1 and report; route the above; round 3 is then a check of R2-01 and R2-02 only (the rest can be confirmed by diff at the gate).
- Next gate unlocked after those: Phase 3 gate (Liaison and Security verdicts, Sponsor approval of the items in plan section 8).
