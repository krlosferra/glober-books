# Agent Collaboration Protocol

How the GloberBooks agents work together. Both agent definitions (`.claude/agents/`) refer to this file.

## 1. Roles

| Role | Who | Owns | Does NOT do |
|---|---|---|---|
| **Sponsor / HITL** | The human | Final approval at every phase gate; the Constitution; business risk, ethics, compliance; all ASK FIRST items | Write implementation code |
| **Product Manager (PM agent)** | `sdd-product-manager` | Business analysis, requirements, `spec.md`, Decision Log, backlog (`tasks.md` organization, priority, sequencing), traceability, change control | Decide technology or architecture; approve its own work; write code |
| **Spec Liaison** | `sdd-spec-liaison` | SDD doctrine and governance: reviews every artifact against the course, the Constitution and the spec; Clarify Loop; task audits; Test Gate; drift forensics; briefs for the coding agent | Decide business questions; author the spec; write code |
| **Security Reviewer** | `sdd-security-reviewer` | Application security: threat model, `security.md`, security verdict and **veto** at every phase gate, read-only security checks, supply-chain and agent-risk review | Writing application code; editing spec or constitution; accepting risk (only the Sponsor can) |
| **Java Architect** | `sdd-java-architect` | Backend implementation of ONE approved task at a time, with unit, slice, integration and contract tests | Anything outside the task; frontend; changing the spec |
| **Frontend Developer** | `sdd-frontend-developer` | UI implementation of ONE approved task at a time against the API contract | Anything outside the task; backend; changing the contract |
| **Orchestrator** | The main Claude session | Invoking the agents in the right order (in parallel when allowed), relaying handoff files, routing feedback, stopping at gates, repository hygiene | Overriding a verdict, skipping a gate, deciding business questions |

The two implementing agents are the "coding agents" referred to elsewhere. Details of the flow, parallelism and feedback routing are in `docs/sdd/orchestration.md`; branches, commits and the no-AI-attribution rule are in `docs/sdd/git-workflow.md`.

Subagents cannot invoke each other. All collaboration therefore happens through **handoff files** that the orchestrator relays.

## 2. Authority order (highest first)
1. `docs/sdd/constitution.md`
2. `docs/sdd/spec.md`
3. `docs/sdd/plan.md`
4. `docs/sdd/tasks.md`
5. Code and tests

`docs/sdd/exercise-brief.md` (BRD and exercise instructions) is the source for scope. `docs/course/` is the theoretical framework.

## 3. Handoff files
Location: `docs/sdd/handoffs/`
Name: `NNN-<from>-to-<to>-<topic>.md` (NNN = zero-padded sequence, e.g. `001-pm-to-liaison-spec-review.md`).
A handoff is append-only. Never rewrite an earlier one; add a new file.

Template:

```markdown
# Handoff NNN: <topic>
- From: <pm | liaison | security | java | frontend | sponsor | orchestrator>
- To: <pm | liaison | security | java | frontend | sponsor | orchestrator>
- Phase: <1-6>   Round: <n of max 3>
- Artifacts: <paths and versions or commit>
- Type: <REVIEW_REQUEST | REVIEW_REPORT | CLARIFICATION | DECISION_REQUEST | DECISION | TASK_BRIEF | TASK_DELIVERY | AUDIT_REPORT | CHANGE_REQUEST>

## Summary
## Content
## Open items / decisions needed
## Requested response
```

## 4. Standard loop for every artifact

```
PM drafts artifact
   └─► handoff REVIEW_REQUEST ─► Liaison
Liaison reviews (BLOCKER / MAJOR / MINOR findings, questions, verdict)
   └─► handoff REVIEW_REPORT ─► PM
PM resolves findings:
   - what it can resolve from the brief, the constitution or prior decisions: fix and log
   - business questions it cannot answer: DECISION_REQUEST ─► Sponsor
   └─► new REVIEW_REQUEST (next round)
Liaison and Security review in parallel. Repeat until the Liaison verdict is APPROVED and the Security verdict is PASS (max 3 rounds; then escalate the unresolved items to the Sponsor)
   └─► Sponsor gate: approve / change / reject
```

Rules:
- No artifact moves to the next phase without **Liaison verdict APPROVED**, **Security verdict PASS** (or `PASS WITH CONDITIONS` with every condition closed) and **Sponsor approval**. A Security `FAIL` (CRITICAL or HIGH finding) is a veto: only the Sponsor can accept the residual risk, in writing, in the risk register of `security.md`.
- The PM never edits its own artifact to "get around" a finding: it fixes the cause or records an explicit decision.
- The Liaison never edits `spec.md` or `tasks.md`; it proposes text in the review report.
- Decisions are written to the spec's Decision Log (DEC-NNN) by the PM only after the Sponsor, or an earlier Sponsor-approved rule, has confirmed them.
- A rejected or ambiguous item is resolved by changing the spec, never by patching code.

## 5. Phase gates

| Phase | Producer | Reviewer | Gate |
|---|---|---|---|
| 1. Constitution | Sponsor (drafted with Liaison help) | Liaison (Mode A) + Security | Liaison APPROVED + Security PASS, then Sponsor |
| 2. Specify | PM | Liaison (Modes B, C) + Security (abuse cases, limits) | Liaison APPROVED + Security PASS, then Sponsor |
| 3. Plan and tasks | Java Architect (backend) and Frontend Developer (frontend) draft `plan.md`; PM organizes `tasks.md` | Liaison (Mode D) + Security (design review) | Liaison APPROVED + Security PASS, then Sponsor |
| 4. Implement | Coding agent, one task at a time (Liaison writes the brief) | Liaison (Modes F, G) + Security (code and dependency review; pre-release full review) | Liaison ACCEPT + Security PASS, then PM acceptance and Sponsor |
| 5. Drift / change | PM (change request and impact analysis) | Liaison (Mode H) + Security | Liaison APPROVED + Security PASS, then Sponsor |
| 6. Contrast | Sponsor / any | n/a | Written comparison |

## 6. Escalation to the Sponsor
Escalate (do not decide) when: a business rule, scope or priority is unknown; a compliance, security or privacy trade-off appears; an ASK FIRST item is needed (new dependency, schema change, new entity, architectural change); two review rounds fail to converge; the brief and the Constitution conflict.

Every escalation must be decision-ready: context, options, consequences, recommendation.
