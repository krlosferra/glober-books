# Handoff 041: REVIEW_REPORT, Task 10 audit (E2E toolchain and pins)

- From: liaison | To: orchestrator | Type: REVIEW_REPORT | Date: 2026-11-02
- Mode: F (semantic audit). Artifacts reviewed: handoffs 037, 038; `e2e/package.json`, `e2e/.npmrc`, `e2e/package-lock.json`, `.nvmrc` (by delivery), `docs/sdd/progress-task-10.md`; the `tools:` line of the frontend agent file and `frontend-testing.md` section 6 (grep only).
- Limits: I have no shell. Command outputs (`npm ci`, audit, `playwright install`, MCP run, `git status`) are taken from handoff 038 and are not independently re-run. I verified directly: `package.json`, `.npmrc`, and the lockfile `resolved` and `integrity` lines (3 entries, all `https://registry.npmjs.org/`).

## Verdict: ACCEPT WITH FOLLOW-UPS (Layers 1 and 2 pass; Security verdict still required)

Task 10 is ACCEPTED once fix F1 (progress merge) is done. AC4 is accepted as "recorded", which is what it asks. The mismatch it found is a new finding that belongs to the agent definition, not to this task.

## Acceptance criteria

| AC | Result | Evidence and notes |
|---|---|---|
| 1 | PASS | `.nvmrc` 24.22.2, `engines.node` 24.22.2 (file read), `node --version` v24.22.2 (reported). Node came from a supplied tarball (see point 4). |
| 2 | PASS | `.npmrc` has the three required lines only. `package.json` has an exact pin `1.64.1`, no scripts, no `typescript`. Lockfile has exactly 3 packages. `npm ci`, `--version` and chromium install are reported with exit 0. `tsconfig` absence is reported (find printed nothing). |
| 3 | PASS | Lockfile checked: 3 `resolved`, all `registry.npmjs.org`, each with sha512 integrity. Audit 0 vulnerabilities at high (reported). |
| 4 | PASS (recorded, with a defect found) | Pinned command with all flags, tools/list only, nothing navigated, nothing called. Version string, tool list, diff and disk use recorded. The "before" disk figure is reconstructed from mtimes (admitted, not proven). Acceptable: the brief asked for recording, and the sandbox browser cache is shared and pre-populated. |

Scope: the delivery lists only the section 4 files plus the progress file and handoff. No scope creep seen: no config, no tests, no MCP in `package.json`, no agent or `status.md` edits. `git status` and `diff --stat` output is not pasted verbatim in 038 (it is only described). Minor, see F6.

Layer 1b (hygiene): the commit subject matches `chore(e2e): ... [Task-10]`. I could not read the log. The orchestrator should confirm the message, author and absence of attribution with `.githooks` / `hygiene.sh` before merge.

## Rulings on the open points

### 1. AC4 tool-list mismatch (7 missing, 8 extra)
- Ruling: not a Task 10 defect. Task 10 did what SC-23 asks, which is to record the real surface so the allowlist can be derived from it. The finding is valid and shows the allowlist in the agent definition was derived from documentation, not from the pinned server (a hallucinated-surface risk, M2).
- The 7 missing names (`browser_check`, `browser_uncheck`, `browser_generate_locator`, `browser_verify_*`) are very probably behind an opt-in capability flag. That is unproven (the delivery did not retry, correctly, "run once"). Do NOT add a `--caps` flag to the launch command to make them appear without a Sponsor decision: enabling capabilities widens the tool surface and changes the SC-23 flag set.
- The 8 extra tools are not a permission problem by themselves: the agent's `tools:` list is the control. But two are high risk and must remain excluded explicitly: `browser_run_code_unsafe` and `browser_evaluate` (arbitrary code in the page), plus `browser_file_upload`. `browser_drag`, `browser_drop`, `browser_emulate_media`, `browser_find`, `browser_tabs` are low risk but unreviewed.
- Consequences:
  a. `.claude/agents/sdd-frontend-developer.md` `tools:` line (agent file, Sponsor/orchestrator owns): the 7 names are dead entries. They do no harm (they simply never resolve) but they make the documented workflow false. Re-derive the list from the real tool surface of the pinned version. Decision for the Sponsor: (i) drop the 7 and rewrite the workflow using `browser_snapshot` and Playwright's own codegen locators, or (ii) approve an explicit capability flag after the Security reviewer assesses what it enables. Recommendation: (i) now, (ii) only if a need appears.
  b. `docs/sdd/frontend-testing.md` section 6, line 75: it tells the agent to use `browser_generate_locator` and "the verify tools". These are not available with the pinned flags. The section must be amended by its owner, and must state the verified tool list and the MCP pin and flags it was verified against. Also add an invariant: the allowlist is re-checked against `tools/list` whenever the MCP pin changes.
  c. `security.md` SC-23: record the observed 25-tool surface and the 8 deliberate exclusions. Security reviewer owns that.
  d. Because the server is a pre-release build (1.64.0-alpha-...) that differs from the suite's 1.64.1 (SEC-F-019), the surface may change. The pin on 0.0.83 is what holds it stable, so no unpinned run.
- Not blocking Task 10. It blocks Task 11/13 only to the extent that their briefs rely on `browser_generate_locator` or `browser_verify_*`; do not cite those tools in any brief until the fix is merged.

### 2. Progress entry location
- Ruling: `progress.md` now exists on main (confirmed at `/home/user/glober-books/docs/sdd/progress.md`). The brief's condition for a separate file ("ONLY because progress.md is absent") no longer holds, and a second progress file is a document-map deviation (`README.md` lists one log). The deviation itself was correct at delivery time.
- Fix F1: merge main into the branch (merge, not rebase of a published history; the branch is unpushed so either is acceptable, the orchestrator chooses), append the Task 10 entry to `progress.md` in the same change, and delete `progress-task-10.md` in that same commit. Do not leave both. The merge must not rewrite other tasks' entries.

### 3. cdn.playwright.dev as an extra origin
- Ruling: accepted as a necessary and known origin, with conditions. `playwright install` downloads browser binaries from `cdn.playwright.dev`; this is outside the npm lockfile integrity chain (browser archives are not hashed in the lockfile), so SC-20/SC-21/SC-36 (which cover npm) do not cover it. The Sponsor allow-listed it in the sandbox, which is a runtime decision, not a documented one.
- Fix F2: the Security reviewer records it in `security.md` as an accepted residual risk or a new control (origin documented, TLS only, Chromium only, no other browser, version determined by the exact `@playwright/test` pin, which fixes the browser build). Only the Sponsor can accept the residual risk. Also document it in `frontend-testing.md` (setup section) and in the CI plan (Task 13 or CI task) so that a CI egress allowlist includes it.
- No change to the pin or the lockfile is needed.

### 4. Node 24 tarball provenance
- Ruling: the integrity check is weak evidence. The SHA-256 came from the same origin as the tarball, so it proves transfer integrity only, not authenticity (the delivery says this itself, correctly). The version 24.22.2 is a project pin, so the sole concern is whether the binary is genuine.
- Fix F3: verify the signature of `SHASUMS256.txt.sig` / `.asc` against the Node release team keys (or compare the checksum to a second independent source) and record the result in `progress.md`. If this cannot be done in the sandbox, record "not proven" and have the Sponsor accept it as a sandbox-only deviation. The tarball is in `/tmp` outside the repo and affects no committed artifact, so this does not reject Task 10. For developer machines and CI, the process is `nvm`/`actions/setup-node` from `.nvmrc`; no tarball route is documented or should be.
- Related, F4 below: the sandbox default is Node 22. Anyone running Task 11+ in the sandbox needs the same route; the orchestrator should make the route explicit, not implicit.

## Findings and follow-ups (numbered)

| # | Severity | Item | Owner |
|---|---|---|---|
| F1 | MAJOR (blocks merge) | Merge main; move the Task 10 entry into `progress.md`; remove `docs/sdd/progress-task-10.md`. | Orchestrator / frontend |
| F2 | MAJOR | Record `cdn.playwright.dev` in `security.md` (residual risk, Sponsor acceptance) and in `frontend-testing.md` setup. | Security reviewer, Sponsor |
| F3 | MAJOR | Prove or mark "not proven" the Node tarball signature or independent checksum; log in `progress.md`. | Orchestrator |
| F4 | MINOR | Document the Node 24 route for the sandbox in `orchestration.md` or the progress note (default is Node 22). | Orchestrator |
| F5 | MAJOR (not blocking Task 10) | Re-derive the frontend agent `tools:` allowlist from the real tool surface; amend `frontend-testing.md` section 6 line 75 (no `browser_generate_locator`/`browser_verify_*`); record the 8 excluded tools in SC-23; add a "re-check on pin change" rule. Do not add `--caps` without a Security assessment and Sponsor approval. | Sponsor (agent file, constitution), PM/Liaison (doc text), Security |
| F6 | MINOR | `status.md` has no Task 10 row; add it (audit column and DONE/REJECTED are mine to set once Sponsor approves). 038 describes rather than pastes `git status --short` and `git diff --stat`; confirm both before merge. | Orchestrator |
| F7 | MINOR | Chromium 1243 (pre-installed) coexists with 1248 (installed); Task 13 must use the one matching the installed library and must not hard-code a path. Also the MCP's own dependency tree was not audited: record in `security.md` as a note under SC-23 (agent-side only, not in the lockfile). | Security, Task 13 brief |
| F8 | INFO | AC4 "before" disk figure is reconstructed. Accept; no re-run (the brief says once, and a re-run adds nothing). | none |

## Security and gates
- Needs a `sdd-security-reviewer` verdict before DONE (I cannot override its veto). Items for it: F2, F5(c), F7, lockfile hosts, audit result, pre-release build SEC-F-019.
- Sponsor approval at the gate triggers the PR; no push or PR has been made, as required.

## Next step
Apply F1 (and confirm F6 evidence), obtain the Security verdict, then Sponsor approval for merge by squash via PR. Then Task 11 may proceed; keep F5 out of its brief until the allowlist is fixed. After merge, record in `progress.md`: `[Task-10] Description - ACCEPTED`.
