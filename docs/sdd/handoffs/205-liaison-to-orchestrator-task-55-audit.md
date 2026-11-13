# Handoff 205: Liaison audit, Task 55 (remove the inert `static-locations` property)
- From: liaison   To: orchestrator   Type: AUDIT_REPORT
- Mode F. Artifacts: commit d09e0cb (3 non-doc files), commit 60a36d4 (constitution 1.8), handoffs 203 and 204, SEC-F-126, tasks.md v0.6.8 Task 55.

## Verdict: ACCEPT Task 55 (Layers 1, 1b, 2 pass; Security verdict is separate, handoff 206)

## Criteria
| # | Result | Evidence |
|---|---|---|
| 1 Key absent, `add-mappings` false | PASS | `git diff`: `application.yml` loses exactly one line (`static-locations`), `add-mappings: false` untouched; no other application*.yml exists. `StartupPolicyIntegrationTest` asserts key null and add-mappings `false`. |
| 2 Catalog and 404/405 unchanged | PASS | `StaticContentIntegrationTest` untouched (not in the diff). `productionWiringReadsOnlyTheClasspathStaticLocation` sets the key itself (system property and property source), so its inertness proof stays meaningful with the key absent from the file; it now also shows that an operator-set key still cannot change the catalog. No case removal needed. |
| 3 `./mvnw verify` green | PASS (partly re-run) | I re-ran the three focused classes (`./mvnw -q -Dtest=...`, exit 0). Surefire XML: StartupPolicy 9/0 failures/0 errors/0 skipped; MandatoryProperties 15/0/0/0; StaticContent 48/0/0/0. The full-build figures (351 tests, 0 failures, 0 errors, 1 skipped, exit 0, JaCoCo check) are taken from delivery 204, not re-run: the change is one removed key plus two test edits, the full run was unnecessary for this verdict. Orchestrator may run the full build before the PR if it wants. |

## Test edits (do not weaken anything else)
- `StartupPolicyIntegrationTest`: table entry for the key removed; the SEC-F-072 test now asserts `isNull()` instead of the old value. `globerbooks.static.root` null and add-mappings false assertions kept. The key's disappearance is the only loss, and the `isNull()` assertion replaces it. OK.
- `MandatoryPropertiesIntegrationTest`: the test now reads the environment property (null) instead of the `WebProperties` bean; `log-request-details` false assertion kept. The oracle is correct: the spec says absent, the test asserts absent (a bean default would have been a weak oracle). The unused `WebProperties` import is removed. Minor: the method name still says "staticLocationsKey..." which is accurate. OK.

## Constitution 1.8
Version 1.8 and date 2026-11-13 in the header; changelog line present, cites SEC-F-126, Task 55 and the reason; the key is gone from the mandatory properties list and from nowhere else in the constitution (grep: only the changelog line names it). Consistent.

## grep `static-locations` in docs/sdd and code (each hit)
- `constitution.md:318` changelog (intended). `tasks.md:4,596,767,838-area`, `status.md:104`, `active-conditions.md:14`, `progress.md:272,319`: task bookkeeping, intended.
- `security-log.md:138` SEC-F-126: owner Security, to be updated by Security.
- `spec.md`, `plan.md`, `README.md`, `plan-appendix.md`: no hit. Nothing lists the property as mandatory outside the constitution.
- Historical handoffs 003, 020 (A-02 "the mandatory property `static-locations` stays"), 054, 056, 074, 077, 078, 079, 080, 161, 162: immutable records, no action. Handoff 020 (plan basis) is stale on this point but `plan.md` itself has no hit, so no plan amendment needed.
- Code: only the three test files named in the task (StartupPolicy, MandatoryProperties, StaticContent proof).

## Risk in delivery
Accepted. With the key gone, `WebProperties` still carries Boot's default locations, but `add-mappings: false` registers no `ResourceHttpRequestHandler`, so they are unused; the unchanged 48-case StaticContent suite (200 from the catalog, 404/405 elsewhere) is the evidence. Residual: a future change that sets `add-mappings: true` would expose Boot's defaults (`classpath:/static/` among them, plus META-INF/resources); that is already blocked by the Task 1 property proof of `add-mappings: false` and is a Security matter, not a Task 55 defect.

## Layer 1b and scope
Commit `chore(config): remove inert static-locations property [Task-55]` follows the format; atomic; no AI attribution seen in the message or diff (the session trailer asked by the harness is not in the commit). Files: application.yml, two tests, handoff 204, progress.md: all within the task list. No production Java change. The one skipped test is pre-existing: `StartupRefusalIntegrationTest` uses `assumeTrue(uid == 0)` (two cases) and is untouched by this commit; I did not run it.
Note: the working tree was clean (no stray files) after my focused run.

## Layer 3
None.

## Follow-ups for the orchestrator
1. Security verdict (handoff 206) must close SEC-F-126; row S-4 in `active-conditions.md` stays open until then (I did not touch it).
2. After Sponsor approval: open the PR, squash merge, `progress.md` note `[Task-55] ... - ACCEPTED`.
