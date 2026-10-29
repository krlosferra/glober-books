# GloberBooks: Repository Instructions

This repository follows **Spec-Driven Development (SDD)** with a Human-in-the-Loop (HITL) PM. Do not write implementation code unless a task from `docs/sdd/tasks.md` is being executed.

## Authority order
1. `docs/sdd/constitution.md`
2. `docs/sdd/spec.md`
3. `docs/sdd/plan.md`
4. `docs/sdd/tasks.md`
5. Code and tests

If a lower document conflicts with a higher one, the higher one wins. Update the spec first; never hand-patch code to hide drift.

## Spec Liaison agent
Use the `sdd-spec-liaison` subagent (`.claude/agents/sdd-spec-liaison.md`) as the intermediate layer between the PM and the coding agent. It analyzes specs, runs the Clarify Loop, reviews plan and tasks, audits delivered tasks, reviews the Test Gate and investigates drift. It does not write code.

## Knowledge base
Course modules are in `docs/course/` (modules 1 to 5).

## Workflow rules
- One task at a time; stop after each task and wait for PM approval.
- One commit per accepted task: `[Task-N] Description - ACCEPTED`.
- Update `docs/sdd/progress.md` and any affected spec/plan/tasks in the same change as the code.
