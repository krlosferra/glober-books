# Project Status Board

- Owner: Orchestrator (see `README.md` for edit rights per column)
- Last updated: 2026-10-30
- Phase: 1 (Constitution v0.3 applied; Liaison round 3 and Security round 2 pending). Open Sponsor settings: enable `main` protection and Dependabot alerts (evidence in `security.md` section 10). No spec, plan or tasks exist yet.

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
| 1 Constitution | v0.3 applied; round 3 re-review pending | round 2: APPROVED WITH CHANGES (handoff 002); round 3 pending | round 1: FAIL, 3 HIGH (handoff 003); round 2 pending | not yet | |
| 2 Specify | not started | | | | |
| 3 Plan and tasks | not started | | | | |
| 4 Implement | not started | | | | |
| 5 Drift / change | not started | | | | |
| 6 Contrast | not started | | | | |

## Backlog board

| Task | Title | Layer | State | Owner agent | Branch | Attempts | Merge commit | Audit | Security | Acceptance | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|
| (none yet) | | | | | | | | | | | |

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
