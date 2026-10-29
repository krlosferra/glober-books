# Orchestration Guide

The main Claude Code session is the **orchestrator** of the whole analysis, development and testing flow. Subagents cannot call each other, so the orchestrator invokes them one by one (or in parallel), relays handoff files, enforces gates and routes feedback. This guide complements `agent-protocol.md` (roles and review loop) and `git-workflow.md` (branches and commits).

## 1. The team

| Agent | File | Responsibility |
|---|---|---|
| PM | `sdd-product-manager` | Business analysis, spec, decision log, backlog, business acceptance, change control |
| Spec Liaison | `sdd-spec-liaison` | SDD theory and governance, reviews, Clarify Loop, task briefs, audits, Test Gate, drift forensics |
| Java Architect | `sdd-java-architect` | Backend implementation with unit, slice, integration and contract tests |
| Frontend Developer | `sdd-frontend-developer` | UI implementation against the API contract, accessibility, UI verification |
| Sponsor | the human | Final approval at every gate; constitution; ASK FIRST decisions |

## 2. Orchestrator responsibilities
1. Invoke the right agent for the current phase and pass it **only** the validated context (paths to the documents, the handoff to answer).
2. Create the next handoff file when an agent's output has to be relayed (keep numbering sequential).
3. Enforce gates: no phase advances without Liaison `APPROVED` and Sponsor approval.
4. Route feedback (section 5), count review rounds (max 3, then escalate to the Sponsor), keep `progress.md` current.
5. Own repository hygiene: correct branch, no AI attribution (git-workflow section 1), no forbidden operations.
6. Run builds and tests independently to verify evidence; never accept claims without output.
7. Maintain a visible task list for the current phase (what is in progress, blocked, waiting for the Sponsor).

The orchestrator does not decide business questions, does not override a Liaison verdict and does not write production code.

## 3. End-to-end flow

```
Phase 1  Constitution   Sponsor + Liaison (review)
Phase 2  Specify        PM ⇄ Liaison  → Sponsor gate
Phase 3  Plan + Tasks   Java Architect drafts plan (backend) + Frontend Developer drafts frontend plan section
                        PM organizes tasks.md ⇄ Liaison review → Sponsor gate
Phase 4  Implement      per task: Liaison brief → Java and/or Frontend implement → Liaison audit
                        → PM business acceptance → Sponsor gate (merge)
Phase 5  Drift/Change   PM change request ⇄ Liaison → Sponsor → back through Phases 2-4 for the change
Phase 6  Contrast       Sponsor
```

### Phase 3 detail (plan and tasks)
1. Orchestrator gives the Java Architect the spec and constitution and asks for the **backend plan**; gives the Frontend Developer the spec and asks for the **frontend plan section** (structure, modules, test approach, tooling proposals flagged ASK FIRST). Both proposals go to `plan.md` (via one handoff each; the orchestrator merges them into a single document on a `spec/` branch).
2. Liaison reviews the plan (Mode D). PM then builds `tasks.md`: vertical slices, each task tagged `backend`, `frontend` or `integration`, with dependencies and the API contract clauses it relies on.
3. Liaison reviews the backlog; Sponsor approves.

### Phase 4 detail (per task)
1. **Select:** orchestrator picks the next ready tasks (Definition of Ready met, dependencies done).
2. **Brief:** Liaison writes a TASK_BRIEF per task (Mode E).
3. **Implement:** Java Architect and/or Frontend Developer work **in parallel** when the tasks are independent (see section 4). Each works on its own branch and delivers a TASK_DELIVERY.
4. **Verify independently:** orchestrator checks out the branch and runs the build and tests (`./mvnw verify` for backend; the approved frontend checks) and compares real output with the report.
5. **Audit:** Liaison runs the semantic audit (Mode F) and Test Gate review (Mode G) and returns ACCEPT or REJECT with evidence.
6. **Business acceptance:** PM verifies the acceptance criteria and invariants from the spec's point of view.
7. **Gate:** Sponsor approves; orchestrator updates `progress.md`, then merges following git-workflow section 4 (PR only if the Sponsor asked for one).
8. **Integration checkpoint:** after foundation tasks and after the first end-to-end feature (and after each slice that joins backend and frontend), run the integration checks in the exercise and record the logs.
9. On REJECT: return the audit report to the same agent for correction. Count attempts (target average ≤ 2 per task).

## 4. Parallelism rules
- Backend and frontend tasks run in parallel **only if** the API contract clauses they depend on are final in the approved spec.
- Never run two agents on the same files or the same branch.
- Frontend may start against a contract stub; it cannot be accepted until its integration smoke check passes against the real backend.
- Two backend tasks run in parallel only if they touch disjoint files and neither depends on the other. When in doubt, serialize.
- The orchestrator rebases or merges in dependency order and re-runs the full build after each merge.

## 5. Feedback routing (normal and expected)

| Signal | Classification | Route |
|---|---|---|
| Test fails; code does not match the spec | Scenario A (bad code) | Back to the implementing agent with the audit report; refine context; fix |
| Test fails; expected value is wrong or the spec is ambiguous | Scenario B (bad spec) | Handoff to PM → change the spec (Decision Log, Sponsor approval) → Liaison review → update plan and tasks → re-implement |
| Implementer reports ambiguity or missing behavior | Spec gap | CLARIFICATION → PM; answer or escalate to Sponsor; update spec first |
| Contract cannot support a UI behavior or backend need | Contract gap | CHANGE_REQUEST → PM → Liaison → Sponsor; both implementers notified once approved |
| Frontend/backend integration mismatch | Classify with Liaison (A or B) | A: fix the deviating side; B: spec change as above |
| Plan flaw discovered | Plan defect | Plan amendment by the architect, Liaison review, tasks updated by PM |
| ASK FIRST item needed (dependency, schema, entity, architecture, frontend tooling) | Governance | Sponsor decision; if approved, update constitution or plan first |
| Drift found (code not in spec) | Drift | Stop; Liaison forensics (Mode H); PM change request; spec-first recovery |
| Coverage below gate or weak oracle found | Test defect | Back to implementer; Liaison Test Gate review |
| Two review rounds without convergence | Escalation | Sponsor decides |

The default response to any spec/code disagreement is to **fix the spec first**, then regenerate or correct the code. Never patch code to hide a spec problem.

## 6. Orchestrator checklists

**Before invoking an implementer**
- [ ] Task ready; brief written by the Liaison; branch name per git-workflow
- [ ] `git config user.name` / `user.email` are the human's identity; `core.hooksPath` is `.githooks`
- [ ] Context limited to constitution, spec, plan, the task and relevant code

**Before accepting a delivery**
- [ ] Commits follow the convention; no AI attribution anywhere (`git log --format=%B` and author fields reviewed)
- [ ] Build and tests re-run by the orchestrator; output matches the report
- [ ] Only the task's files changed (`git diff --stat main...HEAD`)
- [ ] Liaison ACCEPT and PM acceptance recorded; `progress.md` updated

**Before merging**
- [ ] Sponsor approval; squash message follows the convention; branch up to date with `main`; full build green after the merge
