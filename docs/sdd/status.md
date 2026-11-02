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

## Phase gates

| Phase | State | Liaison verdict | Security verdict | Sponsor approval | Date |
|---|---|---|---|---|---|
| 1 Constitution | APPROVED: constitution 1.0 | round 3: APPROVED WITH CHANGES (handoff 004), MAJORs applied | round 3: PASS WITH CONDITIONS (handoff 006); guard fixes applied; platform steps verified (security.md section 10) | approved 2026-10-30 | 2026-10-30 |
| 2 Specify | APPROVED: spec 0.4.1 | round 3: APPROVED (handoff 016) | round 3: PASS at spec level (handoff 019) | approved 2026-10-30 (confirmed: 2 s add bound is a security bound and is never cut; 409 for DUPLICATE_BOOK and LIBRARY_FULL; RATING_NOT_ALLOWED stays 400) | 2026-10-30 |
| 3 Plan and tasks | APPROVED and merged: plan 0.2, tasks 0.5, spec 0.4.3, constitution 1.2 (pull request 4, squash) | round 3: APPROVED (handoff 031) | round 2: PASS at design level (handoff 032) | approved 2026-11-02 (35.75 h scope; CR-1 to CR-9 and DEC-034; SC-33 amendment; R-20 accepted) | 2026-11-02 |
| 4 Implement | in progress: Increment 0a (Tasks 01 to 08) | | | | |
| 5 Drift / change | not started | | | | |
| 6 Contrast | not started | | | | |

## Backlog board

| Task | Title | Layer | State | Owner agent | Branch | Attempts | Merge commit | Audit | Security | Acceptance | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 01 | Maven scaffold and build proofs | backend | DONE | sdd-java-architect | `chore/task-01-maven-scaffold` | 1 | | ACCEPT (handoff 035) | PASS (handoff 036) | | Merged as d6023e1 (pull request 5, squash); brief 033, delivery 034 |
| 02 | Quality gates | backend | IN_PROGRESS | sdd-java-architect | `chore/task-02-quality-gates` | 1 | | | | | Brief: handoff 039 |
| 10 | E2E toolchain and pins | frontend | IN_PROGRESS | sdd-frontend-developer | `chore/task-10-e2e-toolchain` | 1 | | | | | Parallel to Task 02; worktree /home/user/gb-task10; brief 037 |

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
| (see PM agent "Known open points") | | | |
