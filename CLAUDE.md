# GloberBooks: Repository Instructions

This repository follows **Spec-Driven Development (SDD)** with a Human-in-the-Loop (HITL) PM. Do not write implementation code unless a task from `docs/sdd/tasks.md` is being executed.

## Authority order
1. `docs/sdd/constitution.md`
2. `docs/sdd/spec.md`
3. `docs/sdd/plan.md`
4. `docs/sdd/tasks.md`
5. Code and tests

If a lower document conflicts with a higher one, the higher one wins. Update the spec first; never hand-patch code to hide drift.

## Agents
- `sdd-product-manager` (`.claude/agents/sdd-product-manager.md`): business analysis, requirements, `spec.md`, decision log, backlog organization, traceability and change control. Escalates unknown business decisions to the human Sponsor.
- `sdd-spec-liaison` (`.claude/agents/sdd-spec-liaison.md`): SDD theory and governance. Reviews every artifact, runs the Clarify Loop, audits tasks, reviews the Test Gate, investigates drift, writes task briefs for the coding agent.
- Neither agent writes code or chooses technology. Subagents cannot call each other, so the orchestrator (main session) relays handoff files per `docs/sdd/agent-protocol.md`: PM drafts, Liaison reviews, PM resolves, repeat (max 3 rounds), then Sponsor gate.

## Knowledge base
Course modules are in `docs/course/` (modules 1 to 5). The BRD and exercise instructions are in `docs/sdd/exercise-brief.md`.

## Workflow rules
- One task at a time; stop after each task and wait for PM approval.
- One commit per accepted task: `[Task-N] Description - ACCEPTED`.
- Update `docs/sdd/progress.md` and any affected spec/plan/tasks in the same change as the code.
