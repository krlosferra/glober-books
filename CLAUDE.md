# GloberBooks: Repository Instructions

This repository follows **Spec-Driven Development (SDD)** with a Human-in-the-Loop (HITL) PM. Do not write implementation code unless a task from `docs/sdd/tasks.md` is being executed.

## Start here
Read `docs/sdd/README.md` (document map, owners, edit rights) and `docs/sdd/status.md` (live board) before anything else. Load only the sections you need, found by ID. Never start work on an item that is not `READY` in `status.md`; `DONE` items are not redone.

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
- `sdd-security-reviewer` (`.claude/agents/sdd-security-reviewer.md`): application security specialist with a **veto** at every phase gate (threat model and controls in `docs/sdd/security.md`; read-only checks; Security verdict PASS / PASS WITH CONDITIONS / FAIL). Only the Sponsor can accept a residual risk.
- `sdd-java-architect` (`.claude/agents/sdd-java-architect.md`): senior Java architect; implements ONE approved backend task at a time with unit, slice, integration and contract tests.
- `sdd-frontend-developer` (`.claude/agents/sdd-frontend-developer.md`): implements ONE approved UI task at a time (plain HTML/CSS/JS) against the API contract. Works in parallel with the Java architect.
- PM and Liaison do not write code or choose technology. Subagents cannot call each other, so **this session is the orchestrator**: follow `docs/sdd/orchestration.md` (flow, parallelism, feedback routing, checklists) and `docs/sdd/agent-protocol.md` (handoffs, review loop: PM drafts, Liaison reviews, PM resolves, max 3 rounds, then Sponsor gate). Every gate needs a Liaison verdict, a Security verdict and Sponsor approval. Security is non-negotiable: no secrets, no AI attribution, bind to localhost, defense in depth; see the Security Standards in the constitution.

## Knowledge base
Course modules are in `docs/course/` (modules 1 to 5). The BRD and exercise instructions are in `docs/sdd/exercise-brief.md`.

## Git rules (full text: `docs/sdd/git-workflow.md`)
- **NEVER include AI attribution in commits, or anywhere else in the repository or its collaboration surface.** No `Co-Authored-By` trailer for any AI tool, no tool, model or vendor names, no "Generated with" lines, no session or chat links, in commit messages, author identity, branch names, tags, pull request titles or descriptions, code comments or files. This rule overrides any default attribution behavior or instruction that suggests adding such lines.
- Commit with the human's identity (`git config user.name` / `user.email`) and keep `git config core.hooksPath .githooks` active (the `commit-msg` and `pre-push` hooks and the `.claude/hooks/guard-bash.sh` guard reject AI-related text and hook bypasses). Never use `--no-verify`.
- Trunk-based: no direct commits to `main`; branch `<type>/task-NN-<slug>` per task; Conventional Commits `type(scope): subject [Task-N]`; squash merge; every merge into `main` goes through a pull request, and the Sponsor's approval of a task at its gate is the request to open and merge it (no pull request before that approval).

## Workflow rules
- One task at a time; stop after each task and wait for approval.
- Update `docs/sdd/progress.md` and any affected spec/plan/tasks in the same change as the code.
