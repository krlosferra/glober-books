# SDD Document Map

Single entry point to the project's living documents. **Read this first, then `status.md`, then only the sections you need.** Do not load whole documents when a section identified by an ID (`US-003`, `BR-002`, `Task 07`, `DEC-004`) is enough: search for the ID, read around it.

## 1. Documents, owners and edit rights

| Document | Purpose | Owner (may edit) | Others |
|---|---|---|---|
| `status.md` | Live board: what is done, in progress, blocked; spec coverage. **Check before taking any work.** | Orchestrator (all rows); implementers (only the row of their own task); Liaison (audit columns); Security Reviewer (security column); PM (acceptance columns, coverage, reopen) | Read |
| `constitution.md` | Immutable rules: stack, tiers, standards | Sponsor (amendments approved by the Sponsor are applied by the orchestrator) | Read; propose changes by handoff |
| `spec.md` | Source of truth for WHAT and WHY: stories, rules, data model, API contract, decision log | PM | Read; propose changes by handoff (`CLARIFICATION` / `CHANGE_REQUEST`) |
| `plan.md` | Architecture: backend section (Java Architect), frontend section (Frontend Developer), shared contract references | Java Architect (backend section), Frontend Developer (frontend section) after Liaison approval | Read |
| `tasks.md` | Backlog definitions: description, files, criteria, dependencies | PM | Read. **State lives in `status.md`, not here.** |
| `progress.md` | Exercise log: time spent, attempts, rejection log, insights | Orchestrator | Liaison may append audit notes |
| `handoffs/` | Append-only message trail between agents | The sender of each handoff | Read |
| `security.md` | Threat model, security controls (`SC-NN`), standard checks, risk register, open proposals | Security Reviewer (the Sponsor alone changes the risk-acceptance column) | Read; propose through handoffs |
| `frontend-testing.md` | Process for UI end-to-end testing with Playwright | Frontend Developer (after Liaison review) | Read |
| `git-workflow.md` | Branches, commits, PRs, no AI attribution | Sponsor | Read |
| `delivery-strategy.md` | Order of work and iteration: walking skeleton, increments, test tiers, "good enough" release definition | Orchestrator (binding changes go through the Sponsor and the Amendment Procedure) | Read |
| `orchestration.md`, `agent-protocol.md` | How the team works together | Sponsor / orchestrator | Read |
| `exercise-brief.md` | The BRD and the exercise instructions (scope source) | Nobody (frozen reference) | Read |
| `approach.md` | Initial delivery approach and rationale | Sponsor | Read |
| `../course/module-*.md` | SDD theory (frozen) | Nobody | Read on demand, cite |

An agent that finds an error in a document it does not own **does not fix it**: it raises a handoff to the owner.

## 2. Recommended reading order by role

| Role | Always | On demand |
|---|---|---|
| PM | `status.md`, `exercise-brief.md`, `constitution.md` | `spec.md` (own), `tasks.md` (own), `plan.md` for coverage, latest handoffs addressed to `pm` |
| Liaison | `status.md`, `agent-protocol.md` | Artifact under review, `constitution.md`, course module cited by the finding |
| Java Architect | `status.md`, the task brief, the task in `tasks.md` | `constitution.md` (Tech Stack, Testing, API sections), `spec.md` sections cited by the task, `plan.md` backend section, `git-workflow.md` |
| Frontend Developer | `status.md`, the task brief, the task in `tasks.md` | `spec.md` (stories and API contract), `plan.md` frontend section, `frontend-testing.md`, `constitution.md` (Security, API sections) |
| Security Reviewer | `status.md`, `security.md` | The artifact under review, `constitution.md` (security sections), `spec.md` (limits and abuse cases), `plan.md`, the delivery handoff, the code diff |
| Orchestrator | `status.md`, `orchestration.md` | Everything else as routing requires |

## 3. Keeping documents organic (living)

1. **One owner per document.** Changes go through the owner, who records them (Decision Log for `spec.md`; changelog line for the others).
2. **Spec first.** If code and spec disagree, the spec is corrected first (with Sponsor approval), then the plan, then tasks, then code.
3. **Each document starts with a metadata header**: `Owner`, `Last updated` (date), `Status` (Draft / Under review / Approved) and, for `spec.md`, `tasks.md` and `plan.md`, `Version` with a short changelog at the bottom.
4. **Stable IDs.** `FR`, `US`, `BR`, `DEC`, `Task NN` identifiers are never reused or renumbered; retired items are marked `SUPERSEDED by ...`.
5. **Status lives in one place** (`status.md`). Definitions live in `spec.md` and `tasks.md` and are not annotated with state, so they never contradict the board.
6. **Document changes ride with the work**: a branch that changes behavior also updates the affected row in `status.md`, and any document change goes through the proper owner in the same pull request or an earlier `spec/` branch.

## 4. No rework: how completed work is protected

- A task or spec item is **DONE** only after: merge to `main`, Liaison ACCEPT, PM business acceptance and Sponsor approval. The row then records the merge commit and the date.
- **Before dispatching or starting any work**, check `status.md`:
  - `DONE`: do not start. The only way to change it is a `CHANGE_REQUEST` through the PM; if approved, the PM marks the row `REOPENED` with a reason and the work re-enters the backlog.
  - `IN_PROGRESS` / `IN_REVIEW`: another agent or branch owns it. Do not start.
  - `BLOCKED`: read the reason; do not work around it.
  - Only `READY` can be taken, and only by the orchestrator dispatching it.
- The orchestrator additionally lists remote branches (`git ls-remote --heads origin '*/task-*'`) before dispatch: a branch named after the task is a visible claim.
- Behavior that already exists on `main` and is covered by a `DONE` row must not be re-implemented, "improved" or duplicated. If an agent believes it is wrong, it raises a finding; it does not rewrite it.
