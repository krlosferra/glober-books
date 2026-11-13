# Project Status Board

- Owner: Orchestrator (see `README.md` for edit rights per column)
- Last updated: 2026-11-02
- Phase: 1 approved (constitution 1.0). Phase 2 approved and merged (spec 0.4.1, pull request 3, merged by the Sponsor with a merge commit; the ruleset still allows merge and rebase). Phase 3 reviewed (Liaison APPROVED, Security PASS), awaiting the Sponsor gate (plan, tasks, tiers P0 P1 P2, change requests CR-1 to CR-9). Sponsor decisions 2026-11-02: full P0 scope accepted (about 32 h), Increment 0 split into 0a and 0b, change requests go to review, Tasks 49 and 50 before the release tag, accessibility checker in tests only. Open items carried forward: set the required check source to GitHub Actions and squash-only merges in the ruleset, R-15 signature, `ask` rule check, attribution setting re-test in a new session (security.md section 10). 

**Check this file before taking any work.** Definitions of stories, rules and tasks live in `spec.md` and `tasks.md`; this file only tracks state.

## State machine (tasks and spec items)

```
TODO ──► READY ──► IN_PROGRESS ──► IN_REVIEW ──► DONE
              ▲          │  ▲           │
              │          ▼  └─ REJECTED ◄┘
              └──── BLOCKED
DONE ──(approved CHANGE_REQUEST)──► REOPENED ──► READY
Any ──► SUPERSEDED (replaced by another item; never deleted)
```

| State | Meaning | Who may set it |
|---|---|---|
| TODO | Defined, dependencies not met or not yet reviewed | PM |
| READY | Definition of Ready met (spec approved, criteria testable, dependencies DONE) | PM, confirmed by Liaison |
| IN_PROGRESS | Dispatched to an agent; branch exists | Orchestrator (at dispatch) |
| IN_REVIEW | Delivery handed off; audit and acceptance pending | Implementer (own row) or Orchestrator |
| REJECTED | Audit failed; back to the same implementer; attempt count increases | Liaison / Security / Orchestrator |
| DONE | Merged to `main`; Liaison ACCEPT; PM accepted; Sponsor approved. **Do not redo.** | Orchestrator, at merge |
| BLOCKED | Waiting on a decision or dependency (reason required) | Anyone, with reason |
| REOPENED | Approved change request invalidates a DONE item | PM |
| SUPERSEDED | Replaced; point to the replacement | PM |

## Resume point (updated by the orchestrator at every step)

Work can stop at any time for lack of usage. Resume with: `git fetch`, read this section, `docs/sdd/active-conditions.md` (what is open), the backlog board below, and the open pull requests. Do not read old handoffs unless a task, the ledger or a report cites one.

State on 2026-11-10: `main` holds everything up to Task 34b (pull request 42) and the Liaison and Security definitions of pull request 43. Backend Tasks 01 to 34b are DONE (Increments 0a and 0b done and tried by the Sponsor; tag `v0.1` exists; tag `v0.2` is the Sponsor's to create, ledger S-1). SEC-F-178 (the packaged jar did not use the guard) is CLOSED, so nothing blocks a tag.

Next: Task 41+42 (implemented on `test/task-41-42-capacity-performance`, handoff 161, review and merge pending), then 49+50 (raw-socket run and release sweep, before the tag) and 51 (long runs, after the tag). The frontend is untouched since Increment 0b: Tasks 24 to 29 (Increment 1) and 35 to 48, 52, 53 remain.

Governance in force: constitution 1.7 (direct dispatch, compact deliveries, ledger, review cadence of 1.3 to 1.6). One task at a time for implementation; review chunks of 2 to 4 related non-protected tasks; protected-surface tasks are reviewed alone. Merges only on the Sponsor's explicit order.

## Phase gates

| Phase | State | Liaison verdict | Security verdict | Sponsor approval | Date |
|---|---|---|---|---|---|
| 1 Constitution | APPROVED: constitution 1.0 | round 3: APPROVED WITH CHANGES (handoff 004), MAJORs applied | round 3: PASS WITH CONDITIONS (handoff 006); guard fixes applied; platform steps verified (security.md section 10) | approved 2026-10-30 | 2026-10-30 |
| 2 Specify | APPROVED: spec 0.4.1 | round 3: APPROVED (handoff 016) | round 3: PASS at spec level (handoff 019) | approved 2026-10-30 (confirmed: 2 s add bound is a security bound and is never cut; 409 for DUPLICATE_BOOK and LIBRARY_FULL; RATING_NOT_ALLOWED stays 400) | 2026-10-30 |
| 3 Plan and tasks | APPROVED and merged: plan 0.2, tasks 0.5, spec 0.4.3, constitution 1.2 (pull request 4, squash) | round 3: APPROVED (handoff 031) | round 2: PASS at design level (handoff 032) | approved 2026-11-02 (35.75 h scope; CR-1 to CR-9 and DEC-034; SC-33 amendment; R-20 accepted) | 2026-11-02 |
| 4 Implement | in progress: Increments 0a and 0b DONE; backend complete up to 34b; frontend Increment 1 not started | | | | |
| 5 Drift / change | not started | | | | |
| 6 Contrast | not started | | | | |

## Backlog board

| Task | Title | Layer | State | Owner agent | Branch | Attempts | Merge commit | Audit | Security | Acceptance | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 01 | Maven scaffold and build proofs | backend | DONE | sdd-java-architect | `chore/task-01-maven-scaffold` | 1 | d6023e1 | ACCEPT (035) | PASS (036) |  | PR 5; brief 033; delivery 034 |
| 02 | Quality gates | backend | DONE | sdd-java-architect | `chore/task-02-quality-gates` | 1 |  | ACCEPT (043) | PASS (044) |  | brief 039; delivery 040 |
| 10 | E2E toolchain and pins | frontend | DONE | sdd-frontend-developer | `chore/task-10-e2e-toolchain` | 1 | c731108 | ACCEPT WITH FOLLOW-UPS (041) | PASS WITH CONDITIONS (042) |  | brief 037; delivery 038 |
| 03a | SQLite data source, claim and clock | backend | DONE | sdd-java-architect | `feature/task-03a-sqlite-data-source` | 1 | 2fce30a | ACCEPT WITH FOLLOW-UPS (051) | PASS WITH CONDITIONS (052) |  | PR 9; brief 045; delivery 047 |
| 11 | Frontend logic and API client (thin) | frontend | DONE | sdd-frontend-developer | `feature/task-11-logic-and-client-thin` | 1 | d8cdb7f | ACCEPT WITH FOLLOW-UPS (049) | PASS WITH CONDITIONS (050) |  | PR 8; brief 046; delivery 048 |
| 03b | Start policy and configuration | backend | DONE | sdd-java-architect | `feature/task-03b-start-policy-config` | 1 | 172e1c3 | ACCEPT WITH FOLLOW-UPS (055) | PASS (056) |  | PR 10; brief 053; delivery 054 |
| 05 | Error foundation | backend | DONE | sdd-java-architect | `feature/task-05-error-foundation` | 1 | 7a34188 | ACCEPT WITH FOLLOW-UPS (059) | PASS WITH CONDITIONS (060) |  | PR 11; brief 057; delivery 058 |
| 06 | Thin gate, exception advice and container error report | backend | DONE | sdd-java-architect | `feature/task-06-thin-gate` | 1 | 91702a9 | ACCEPT WITH FOLLOW-UPS (063) | PASS WITH CONDITIONS (064) |  | PR 12; brief 061; delivery 062 |
| 04 | Domain and repository | backend | DONE | sdd-java-architect | `feature/task-04-domain-repository` | 1 | 6a4f35e | ACCEPT WITH FOLLOW-UPS (067) | PASS WITH CONDITIONS (068) |  | PR 13; brief 065; delivery 066 |
| 07 | Add and list API (thin) | backend | DONE | sdd-java-architect | `feature/task-07-add-and-list-thin` | 1 | 4506bdf | ACCEPT WITH FOLLOW-UPS (071) | PASS WITH CONDITIONS (072) |  | PR 14; brief 069; delivery 070 |
| 08 | Real-port baseline and mandatory-property proofs (closes 0a) | backend | DONE | sdd-java-architect | `chore/task-08-real-port-baseline` | 1 | d764a5b | ACCEPT WITH FOLLOW-UPS (075) | PASS (076) |  | PR 15; brief 073; delivery 074 |
| 09 | Static content (opens 0b) | backend | DONE | sdd-java-architect | `feature/task-09-static-content` | 1 | 25c8035 | ACCEPT WITH FOLLOW-UPS (079) | PASS WITH CONDITIONS (080) |  | PR 17; brief 077; delivery 078 |
| 12 | Page shell with minimal list and add | frontend | DONE | sdd-frontend-developer | `feature/task-12-page-shell-minimal` | 1 | 625230f | ACCEPT WITH FOLLOW-UPS (083) | PASS WITH CONDITIONS (084) |  | PR 18; brief 081; delivery 082 |
| 13 | E2E harness (config, launcher, isolation) | frontend | DONE | sdd-frontend-developer | `chore/task-13-e2e-harness` | 1 | 8759c1f | ACCEPT WITH FOLLOW-UPS (090) | PASS (091) |  | PR 20; brief 088; delivery 089 |
| 14 | Smoke tests, guard fixture and README (closes 0b) | frontend | DONE | sdd-frontend-developer | `chore/task-14-smoke-guard-readme` | 1 | be3c235 | ACCEPT WITH FOLLOW-UPS (094) | PASS WITH CONDITIONS (095) |  | PR 21; brief 092; delivery 093 |
| 15 | Route table, methods and Accept | backend | DONE | sdd-java-architect | `feature/task-15-routing-accept` | 1 | 470e842 | ACCEPT WITH FOLLOW-UPS (098) | PASS WITH CONDITIONS (099) |  | PR 22; brief 096; delivery 097 |
| H1 | Header value fix (SEC-F-130, constitution 1.4) | backend + e2e | DONE | sdd-java-architect | `chore/permissions-policy-bluetooth` | 1 | ce729be | ACCEPT WITH FOLLOW-UPS (102) | PASS (103) |  | PR 23; brief 100; delivery 101 |
| 16 | Query, id and empty-body rules; transport limits | backend | DONE | sdd-java-architect | `feature/task-16-query-id-limits` | 1 |  | ACCEPT WITH FOLLOW-UPS (106) | PASS WITH CONDITIONS (107) |  | PR 24; brief 104; delivery 105 |
| 17 | Container error wrapper | backend | DONE | sdd-java-architect | `feature/task-17-container-errors` | 1 |  | ACCEPT WITH FOLLOW-UPS (110) | PASS WITH CONDITIONS (111) |  | PR 28; brief 108; delivery 109 |
| 18+19 | Text policy and Unicode fixture check (merged task) | backend | DONE | sdd-java-architect | `feature/task-18-19-text-policy` | 1 |  | ACCEPT WITH FOLLOW-UPS (114) | PASS (115) |  | PR 29; brief 112; delivery 113 |
| 20 | Validation wiring and field-error mapping | backend | DONE | sdd-java-architect | `feature/task-20-validation` | 1 |  | ACCEPT WITH FOLLOW-UPS (118) | PASS (119)* |  | PR 30; brief 116; delivery 117 |
| 21 | Strict body reading and parser limits | backend | DONE | sdd-java-architect | `feature/task-21-strict-body` | 1 |  | ACCEPT (122) | PASS WITH CONDITIONS (123)* |  | PR 31; brief 120; delivery 121 |
| 22 | Duplicate key and service exceptions | backend | DONE | sdd-java-architect | `feature/task-22-duplicates` | 1 |  | ACCEPT WITH FOLLOW-UPS (126) | PASS WITH CONDITIONS (127)* |  | PR 32; brief 124; delivery 125 |
| 23a | Add rules: capacity and duplicate detection | backend | DONE | sdd-java-architect | `feature/task-23-add-rules` (chunk branch) | 1 |  | ACCEPT (130) | PASS WITH CONDITIONS (131) |  | PR 34; brief 128; delivery 129 |
| 23b | List status filter and the 2-second add bound | backend | DONE | sdd-java-architect | `feature/task-23-add-rules` (chunk branch) | 1 |  | ACCEPT (130) | PASS WITH CONDITIONS (131) |  | PR 34; brief 128; delivery 129 |
| 30 | Book transitions and value types (rating literal) | backend | DONE | sdd-java-architect | `feature/task-30-rating` | 1 |  | ACCEPT WITH FOLLOW-UPS (134) | PASS WITH CONDITIONS (135) |  | PR 35; brief 132; delivery 133 |
| 31a | Get, delete, status change | backend | DONE | sdd-java-architect | `feature/task-31a-get-delete-status` | 1 |  | ACCEPT WITH FOLLOW-UPS (138) | PASS (139) |  | PR 37; brief 136; delivery 137 |
| 31b | Rating, clear rating and rating error mapping | backend | DONE | sdd-java-architect | `feature/task-31b-rating-endpoints` | 1 |  | ACCEPT (142) | PASS (143) |  | PR 38; brief 140; delivery 141 |
| 32 | Concurrency and fault injection (tests only) | backend | DONE | sdd-java-architect | `test/task-32-33-concurrency-durability` (chunk branch) | 1 |  | ACCEPT WITH FOLLOW-UPS (146) | PASS WITH CONDITIONS (147) |  | PR 39; brief 144; delivery 145 |
| 33 | Durability across restart and kill (tests only) | backend | DONE | sdd-java-architect | `test/task-32-33-concurrency-durability` (chunk branch) | 1 |  | ACCEPT WITH FOLLOW-UPS (146) | PASS WITH CONDITIONS (147) |  | PR 39; brief 144; delivery 145 |
| 34a-1 | Complete directory guard and file identity (not wired) | backend | DONE | sdd-java-architect | `feature/task-34a1-directory-guard` | 1 | | ACCEPT (handoff 150) | PASS WITH CONDITIONS (handoff 151) | Delivery handoff 149; pull request 40. Split from 34a (tasks 0.6.5; cut of handoff 146 (d), agreed in handoff 147). Depends on 03b. Guard unit-proved, unwired: SEC-F-178 stays open; Carried to 34a-2: decide the fate of create(Path)/create(String) (remove or package-private), normalize never resolve links and README says to pass the realpath, prove modes and -journal on the jar under umask 022/000, no path on the console, InvalidPathException to a fixed refusal, guard after StartupPolicy and before the first connection, name `tmp` in the criteria, Javadoc L-1 wording; Sponsor signs R-25 (check-to-open race) at the gate |
| 34a-2 | Wire the packaged application to the guard and prove it on the jar | backend | DONE | sdd-java-architect | `feature/task-34a2-wire-guard` | 1 |  | ACCEPT WITH FOLLOW-UPS (154) | PASS WITH CONDITIONS (155) |  | PR 41 |
| 34b | Stored-library validation, hot journal and second instance | backend | DONE | sdd-java-architect | `feature/task-34b-stored-library-validation` | 1 |  | ACCEPT WITH FOLLOW-UPS (158) | PASS (159)* |  | PR 42 |
| 41+42 | Capacity and performance at maximum size, and backend release sweep (merged task) | backend | DONE | sdd-java-architect | `test/task-41-42-capacity-performance` | 1 | | ACCEPT WITH FOLLOW-UPS (162) | rides on 49+50 (ledger) | | brief 160, delivery 161; Liaison audited; Security close-out in active-conditions.md |
| 49+50 | Raw-socket run, grammar corners, hidden-channel repeat and log hygiene (merged task, before the tag) | backend | DONE | sdd-java-architect | `test/task-49-50-raw-socket-release` | 1 | | ACCEPT WITH FOLLOW-UPS (165) | PASS WITH CONDITIONS (166) | | brief 163, delivery 164 (full verify not completed in 9 min; findings 1 and 2) |
| 24 | Frontend text and failure logic | frontend | DONE | sdd-frontend-developer | PR 47 (merged) | 1 | | ACCEPT WITH FOLLOW-UPS (173) | PASS WITH CONDITIONS (174) | | brief 167; delivery 168; chunk 24 to 27 (one review, one PR) |
| 25 | List state, filter and list states | frontend | DONE | sdd-frontend-developer | PR 47 (merged) | 1 | | ACCEPT WITH FOLLOW-UPS (173) | PASS WITH CONDITIONS (174) | | brief 169; delivery 170 |
| 26 | Add form errors and in-flight guard | frontend | DONE | sdd-frontend-developer | PR 47 (merged) | 1 | | ACCEPT WITH FOLLOW-UPS (173) | PASS WITH CONDITIONS (174) | | brief 171; delivery 172 |
| 27 | Duplicate dialog | frontend | DONE | sdd-frontend-developer | PR 47 (merged) | 1 | | ACCEPT WITH FOLLOW-UPS (173) | PASS WITH CONDITIONS (174) | | brief 171; delivery 172 |
| 28 | E2E library view, filter and states | frontend | DONE | sdd-frontend-developer | PR 48 (merged) | 1 | | ACCEPT WITH FOLLOW-UPS (177) | PASS (178) | | brief 175; delivery 176 |
| 29 | E2E add, validation and duplicate flows | frontend | DONE | sdd-frontend-developer | PR 49 (merged) | 1 | | ACCEPT (181) | PASS (182) | | brief 179 |
| 35 | Client endpoints for status, rating, clear and delete | frontend | DONE | sdd-frontend-developer | PR 51 (merged) | 2 | | ACCEPT (185) | PASS (186) | | brief 183 |
| 36 | Entry view, status control and confirmation | frontend | DONE | sdd-frontend-developer | PR 52 (merged) | 2 | | ACCEPT (189) | PASS (190) | | brief 187; delivery 188 |
| 37 | Rating group and clear rating | frontend | IN_PROGRESS | sdd-frontend-developer | `feature/task-37-rating-group` (chunk branch) | 2 | | | | | brief 191; chunk 37+38 (one review, one PR) |
| 38 | Delete with confirmation | frontend | IN_PROGRESS | sdd-frontend-developer | `feature/task-37-rating-group` (chunk branch) | 2 | | | | | brief 191; chunk 37+38 |

## Spec coverage

One row per FR, user story, business rule and endpoint. A row is `VERIFIED` only when every task that implements it is DONE and its tests pass on `main`.

| Item | Description | Tasks | Backend | Frontend | E2E test | State |
|---|---|---|---|---|---|---|
| FR1 | Add Book | | | | | NOT_STARTED |
| FR2 | Track Status | | | | | NOT_STARTED |
| FR3 | Rate Books | | | | | NOT_STARTED |
| FR4 | Author Bio (via change control) | | | | | NOT_STARTED |

Coverage states: `NOT_STARTED`, `IN_PROGRESS`, `IMPLEMENTED` (code merged), `VERIFIED` (all tests incl. E2E green on `main`), `REOPENED`.

## Open decisions and blockers

| ID | Item | Owner | Needed by |
|---|---|---|---|
| (see `docs/sdd/active-conditions.md`, sections 1 and 6) | | | |
