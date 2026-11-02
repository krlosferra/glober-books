# Handoff 042: AUDIT_REPORT, Task 10 (E2E toolchain and pins)

- From: security | To: orchestrator | Type: AUDIT_REPORT | Date: 2026-11-02
- Scope: branch `chore/task-10-e2e-toolchain`, commit d717722 (files: `.nvmrc`, `e2e/package.json`, `e2e/.npmrc`, `e2e/package-lock.json`, handoff 038, progress record). Agent definition `.claude/agents/sdd-frontend-developer.md` unchanged by the commit.

## 1. Method
Read all files; ran in a scratch copy (outside the repo) with Node v24.22.2: `npm ci --ignore-scripts`, `npm audit`, `npm ls --all`, `npm view <pkg>@1.64.1 dist.integrity maintainers`; diffed the agent `tools:` list against the tool list reported in 038.

## 2. Findings

| ID | Severity | Area | Location | Finding | Evidence | Remediation | Control |
|---|---|---|---|---|---|---|---|
| SEC-F-086 | LOW | Agent tooling | `.claude/agents/sdd-frontend-developer.md` `tools:` | 7 allowlisted MCP tools (`browser_check`, `browser_uncheck`, `browser_generate_locator`, `browser_verify_*` x4) are not exposed by `@playwright/mcp@0.0.83` with the current flags (probably a `--caps` option). Fail-safe (missing, not extra) but the allowlist is stale. | Handoff 038 AC4 (self-reported; I did not re-run the MCP) | Re-derive the list at the pin (SC-23). If a `--caps` flag is added, review the new tools one by one before allowing them; or drop the 7 names. Orchestrator or Sponsor decision; agent files are guarded (SC-34). | SC-23, SC-34 |
| SEC-F-087 | INFO | Supply chain | download of `cdn.playwright.dev` | Extra origin for browser binaries; no checksum in the lockfile. Browsers installed by `playwright install` run only in the test sandbox. | 038 sections 3 and 4 | Accept as residual (proposal R-21 and R-22 below); allow-list only for `playwright install`, not for the agent at runtime. | SC-21 |
| SEC-F-088 | INFO | Supply chain | Node 24.22.2 tarball | Checksum came from the same origin (nodejs.org SHASUMS256.txt), so it proves transport integrity only; SHASUMS signature (GPG) not verified. Tarball lives in `/tmp`, outside the repo; `.nvmrc` and `engines` + `engine-strict` pin the version for everyone. | 038 section 2 | Accept as residual for the sandbox (proposal R-21 and R-22); on a developer machine use a package manager or verify the signed SHASUMS. | SC-19 |

No CRITICAL or HIGH finding.

## 3. Controls
- SC-19 VERIFIED (Task 10 scope): `@playwright/test` 1.64.1 exact (no caret) in `package.json` and lockfile; `playwright` and `playwright-core` 1.64.1; Node 24.22.2 in `.nvmrc` and `engines`. Maven pins not in scope.
- SC-20 VERIFIED (npm): `npm audit` 0 vulnerabilities (re-run by me, exit 0). Public advisory DB beyond npm audit not checked.
- SC-21 VERIFIED for `@playwright/test`, `playwright`, `playwright-core` 1.64.1: published by GitHub Actions under the Microsoft/Playwright maintainers; `dist.integrity` from the registry equals the lockfile hashes for all three. `@playwright/mcp@0.0.83` not re-checked (previous review). No extra package: `npm ls --all` shows exactly 3 packages, no typescript.
- SC-36 VERIFIED (npm part): `e2e/.npmrc` has `ignore-scripts=true`, `save-exact=true`, `engine-strict=true`; no token, no registry override; 3 of 3 `resolved` URLs on `https://registry.npmjs.org/`; `npm ci --ignore-scripts` succeeds with lockfile (3 packages). Playwright has no install scripts of consequence.
- SC-22 unaffected.
- SC-23 PARTIAL: the agent file keeps the required options (`--headless --isolated --block-service-workers --no-webmcp --output-dir .playwright-mcp --allowed-origins http://127.0.0.1:18080`, pinned 0.0.83, `.playwright-mcp/` is git-ignored). `tools:` contains none of `browser_evaluate`, `browser_run_code_unsafe`, `browser_file_upload`, `browser_drag`, `browser_drop`, `browser_emulate_media`, `browser_find`, `browser_tabs`; the 8 exposed-but-unlisted tools are therefore unavailable by construction (the only `browser_evaluate` / `run_code_unsafe` text in the file is prose forbidding them). Note: `--allowed-origins` is not a security boundary by itself (redirects and sub-resources), so the tool minimization remains the control. Stale allowlist: SEC-F-086.

## 4. Not verified
- MCP tool list itself (relied on 038; not re-run).
- `npm audit signatures`: failed in my sandbox ("Failed to download", registry signature keys unreachable through the proxy), so provenance attestations are unchecked.
- Node tarball GPG signature; browser binary hashes from `cdn.playwright.dev`; GitHub/OSV advisories outside npm audit.

## 5. Residual risks (Sponsor)
Proposed R-21 (cdn.playwright.dev) and R-22 (Node tarball): browser binaries from `cdn.playwright.dev` and Node tarball verified only by same-origin checksum, accepted for the sandbox and local test use. Recommendation: accept.

## 6. Verdict: PASS WITH CONDITIONS
Condition (before merge or tracked): resolve SEC-F-086 (re-derive the agent allowlist; any added capability flag needs a Security re-review), and the Sponsor decides R-21 and R-22 (Sponsor column left "Pending Sponsor"). security.md was updated in the main checkout (v0.20). Evidence for SC-19..23 and SC-36, SEC-F-086..088 and R-21, R-22 are recorded in security.md (main checkout, branch chore/task-02-quality-gates, uncommitted); add the Task 10 Security cell in `status.md`.
