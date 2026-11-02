# Handoff 024: Phase 3 review request (plan, tasks, tiers, spec change requests)
- From: orchestrator
- To: liaison and security (two independent reviews, run in parallel)
- Phase: 3   Round: 1 of 3
- Artifacts: `docs/sdd/plan.md` 0.1, `docs/sdd/plan-appendix.md` 0.1, `docs/sdd/tasks.md` 0.3, `docs/sdd/spec.md` 0.4.2 (draft; change requests CR-1 to CR-9 on top of approved 0.4.1), handoffs 021, 022, 023
- Type: REVIEW_REQUEST

## Sponsor decisions of 2026-11-02 (do not re-litigate)
1. The full P0 scope is accepted: about 32 h of task time for Increments 0 to 3 against the indicative 6 to 8 h.
2. Increment 0 is split: 0a = Tasks 01 to 08 (API skeleton, tag `v0.1`), 0b = Tasks 09 to 14 (page and smoke, tag `v0.2`).
3. CR-1 to CR-9 and the tier differences D1 to D4 (handoff 023) go to review.
4. Tasks 49 and 50 (P1 raw-socket and grammar rows) run before the release tag. The accessibility checker of Task 43 runs in test code only; the application CSP is never relaxed.

## What to review
**Liaison:** `plan.md` against constitution and spec (no new technology, no conflict with a higher document); `plan-appendix.md` consistency with `plan.md`; `tasks.md` (size, order, dependencies, testable acceptance, trace to spec ids, Definition of Ready); the nine change requests as a diff check against spec 0.4.1 (minimal, no new scope, no conflict); whether any text asserted by a test lacks an oracle in the spec. Findings by severity; verdict APPROVED, APPROVED WITH CHANGES or CHANGES REQUIRED.

**Security:** design review of pipeline order (AP-2), start order and validation without WAL, the exclusive-lock claim, `query_only` validation, Tomcat limits, wrapper placement (the container error path lands in Task 17 after the real-port class of Task 08: handoff 022 N-4), static catalog rules, the tier moves and D1 to D4 of handoff 023, CR-1 to CR-9 for security effect (15 s client timeout, TRACE pre-application 405, hot-journal wording, error text), the frontend E2E isolation (launcher versus `global-setup`, SC-33) and the accessibility checker decision. Update `security.md` (findings, control statuses). Verdict PASS, PASS WITH CONDITIONS or FAIL.

## Output
Write your response as a handoff `docs/sdd/handoffs/NNN-<you>-to-orchestrator-phase3-review.md` (Liaison 025, Security 026), edit only your own documents, and list every change you need from the PM or the architects as a numbered finding. Do not edit `plan.md`, `tasks.md` or `spec.md`; the orchestrator routes the fixes.
