# Handoff 086: Liaison to Orchestrator, constitution 1.3 amendment (review load per task, S-6 option A)

- Type: REVIEW_REPORT (amendment draft)
- Date: 2026-11-05
- Authority: Sponsor decision of 2026-11-05, constitution Amendment Procedure. Doctrine: Module 3 (governance, Ask First), Module 5 (HITL per phase).
- Status: DRAFT. Not applied. The orchestrator applies after the Security reviewer's diff check.

## 1. Constitution

### 1a. Definition of Done item 5 (line 101), replace with

```
5. Liaison ACCEPT (every task). Security: a task marked `Security: per task` in its `tasks.md` header needs a Security verdict `PASS` (or `PASS WITH CONDITIONS` with every condition closed) before merge. A task marked `Security: increment` is covered by the single Security review of its increment (see Security Standards, "Security review cadence"). Then PM acceptance and Sponsor approval.
```

### 1b. Constitution line 273 (Governance), replace

Old: `- Every phase gate needs a Liaison verdict, a Security verdict and Sponsor approval (see agent-protocol.md).`
New: `- Every phase gate needs a Liaison verdict, a Security verdict and Sponsor approval (see agent-protocol.md). Task-level review follows "Security review cadence" in the Security Standards.`

### 1c. New paragraph, append to the Security Standards section after line 194 (the intro paragraph)

```
**Security review cadence.** The Liaison audits every task. The PM marks each task header `Security: per task` or `Security: increment`. `Security: per task` is mandatory for every task that touches input, output, configuration, dependencies, headers, the frontend or any control (`SC-NN`) of `docs/sdd/security.md`. For every other task, one Security review per increment covers them; it is completed before the Sponsor tries the increment. The veto is unchanged: a Security `FAIL` blocks the increment tag and the release, and only the Sponsor may accept the residual risk in writing. The Security reviewer may upgrade any task to `per task` at any time, and the PM then updates the header. The pre-release review is unchanged. Automated checks (CI hygiene workflow, secrets scan) still run on every merge.
```

Untouched on purpose: line 194 (veto "at every phase gate"), line 206 (CI and secrets check on every merge, automated), line 296 history.

### 1d. Changelog, insert above the 1.2 line

```
- 1.3 (2026-11-05): Amendment: review load per task (delivery strategy S-6, option A). The Liaison audits every task; the Security reviewer reviews per task every task that touches input, output, configuration, dependencies, headers, the frontend or a `security.md` control, and covers all other tasks with one review per increment before the Sponsor tries it; the veto and the pre-release review are unchanged; tasks are marked `Security: per task` or `Security: increment` (handoff 086; Sponsor decision 2026-11-05).
```

Also update the constitution header version/date to 1.3 if it carries one (orchestrator to check).

## 2. Other documents

**orchestration.md line 50**, replace the Security sentence with:
`**Security review follows the task header:** for `Security: per task` tasks (mandatory for any task that touches input, output, configuration, dependencies, headers, the frontend or a `security.md` control) the Security Reviewer audits in parallel and returns PASS, PASS WITH CONDITIONS or FAIL; for `Security: increment` tasks, a single Security review per increment runs before the Sponsor tries the increment. The Security Reviewer may upgrade any task to per task.`

**orchestration.md line 42**, append: `Each task header carries `Security: per task` or `Security: increment` (PM).`

**orchestration.md line 93**, replace with:
`- [ ] Liaison ACCEPT and PM acceptance recorded; Security verdict PASS recorded for `Security: per task` tasks (increment tasks: covered by the increment review); `progress.md` updated`

**orchestration.md**, increment close checklist or the flow section: add `- [ ] Security increment review done (covers all `Security: increment` tasks) before the Sponsor tries the increment; a FAIL blocks the increment tag and the release.`

**agent-protocol.md line 12** (Security Reviewer row): after "verdict and **veto** at every phase gate" add ", per-task verdicts for `Security: per task` tasks, one review per increment for the rest, and the right to upgrade any task to per task".
**agent-protocol.md lines 62 and 67**: no change (they govern artifact/phase gates, which stay as is). Add one sentence at the end of line 67: `Task-level Security review follows the cadence in the constitution (Security Standards).`

**delivery-strategy.md**:
- Line 7: replace "S-6 adopted in principle and needs a constitution amendment proposal after Phase 2 closes." with "S-6 adopted and applied through constitution 1.3 (Sponsor decision 2026-11-05, handoff 086)."
- Line 77, replace the row with: `| S-6 | Review load per task (adopted and applied, constitution 1.3, 2026-11-05): the Liaison audits every task; the Security reviewer reviews per task every task that touches input, output, configuration, dependencies, headers, the frontend or a `security.md` control, and covers the others with one review per increment before the Sponsor tries it; veto unchanged; the PM marks `Security: per task` or `Security: increment` | Adopted |`
- Line 6 changelog style: add a changelog entry "2026-11-05, S-6 adopted and applied (handoff 086)."; bump version to 1.1.

**README.md**: states no per-task Security rule (lines 9, 16, 35 describe access only). No edit required.

**tasks.md** (PM): add the `Security:` header field to each task; needs a separate PM handoff, not part of this amendment.

## 3. security.md consistency check

- No row says "Security review on every task". "every merge" in SC rows (lines 78, 88, 98) refers to the CI hygiene or secrets check, which is automated and unchanged. Line 149 is a Sponsor condition (protected-path list read before every merge), also unchanged.
- Residual points for the Security reviewer's diff check: (a) confirm SC verification columns that name "Security reviewer" as the per-task verifier are not displaced by increment batching; (b) confirm lines 78, 88, 98 are CI-only; (c) the "any `SC-NN` control" trigger makes security.md the source of the per-task trigger, so new controls widen it automatically.
- Verdict: APPROVED WITH CHANGES (apply sections 1 and 2 as drafted; Security diff check gates application).
