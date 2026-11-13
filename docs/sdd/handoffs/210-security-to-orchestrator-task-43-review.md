# 210 Security to Orchestrator: Task 43 review

From: security. Type: AUDIT_REPORT. Scope: branch chore/task-43-accessibility-check, commit 08b32b3 (local), diff vs main. Phase 4, Security: per task.

## Verified myself (read-only)
- Dependency: `@axe-core/playwright` 4.14.0 (exact pin, no caret) and transitive `axe-core` 4.14.0 only; lockfile adds exactly these two entries. `npm view`: maintainers are the Deque team (dqlabs, npmdeque, wilcofiers, dylanb, ...); 4.14.0 published by GitHub Actions; integrity from the registry equals the lockfile hash for both packages.
- All 5 `resolved` entries in `e2e/package-lock.json` use https://registry.npmjs.org/ (0 other hosts).
- No install-time scripts in either package (no preinstall/install/postinstall). `@axe-core/playwright` has `prepare` (a dev-repo script; npm does not run it for registry installs); `e2e/.npmrc` still has ignore-scripts=true, save-exact=true, engine-strict=true. Engines (node >=22.12 / >=4) satisfied by the node 24.22.2 pin.
- `npm audit --audit-level=high` in e2e: found 0 vulnerabilities.
- Diff vs main touches no file under `frontend/` or `backend/`, nor `playwright.config.ts`; no header, CSP or filter change. Changed files: 2 handoffs, progress.md, status.md, e2e package.json and lock, the new spec.
- `e2e/tests/us-014-accessibility.spec.ts`: no `bypassCSP`, no `disableRules`/`withRules`/`exclude`; only a comment states "no bypassCSP". Scan uses `withTags(TAGS)` and asserts passes > 0. No `route`, `addScriptTag`, CDN or external URL.
- Commit 08b32b3: no AI attribution, no Co-Authored-By, no secret values. The delivery mentions an "expired mirror token (401)" only as prose, no value. Author is the human identity.
- Containment: the package is a devDependency in `e2e/` only; nothing under `frontend/` or backend resources or the jar build references it, so it is not served. axe is injected by the Playwright runner into the test browser, not shipped to users.

## Not verified
- Did not run the E2E suite (as instructed), so the delivery's pass counts and "no CSP violation under the application policy" are the delivery's figures, not mine. Static review shows no CSP bypass, which is the security-relevant part.
- Public advisory databases beyond `npm audit` not consulted. axe-core is MPL-2.0 (licence note only, not a security matter).

## Findings
None opened. No change to security-log.md or active-conditions.md.

## Controls
SC-10 (exact pins, lockfile, registry only): VERIFIED. SC-20/SC-21/SC-36/SC-38 (CSP and headers unchanged, no bypass): VERIFIED by diff scope and grep. SEC-F-076 pattern (ignore-scripts kept): VERIFIED.

## Verdict
PASS. No conditions. Next step: Liaison verdict and Sponsor gate.
