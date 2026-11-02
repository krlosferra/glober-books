# Handoff 038: TASK_DELIVERY, Task 10 (E2E toolchain and pins)

- From: frontend | To: liaison | Type: TASK_DELIVERY | Date: 2026-11-02
- Task: Task 10 | Branch: `chore/task-10-e2e-toolchain` | Commit: single commit `chore(e2e): pin Playwright toolchain and Node [Task-10]` (hash in `git log`). Not pushed, no PR.

## 1. Files

Created: `.nvmrc`, `e2e/package.json`, `e2e/.npmrc`, `e2e/package-lock.json`, `docs/sdd/progress-task-10.md`, this handoff. Nothing else changed. `.gitignore` already covers `node_modules/`; nothing from the browser cache or the MCP output directory is staged.

Progress record (updated after review fix F1): main with Task 01 was merged into the branch, the Task 10 entry was appended to `docs/sdd/progress.md` and the temporary `docs/sdd/progress-task-10.md` was deleted (one commit, `docs(progress): ... [Task-10]`). `status.md` has no Task 10 row; I proceeded on the orchestrator's READY ruling and did not edit `status.md`.

## 2. How the pinned Node was supplied (deviation)

The sandbox default Node is v22.22.0 (`/opt/node22/bin/node`, `/usr/local/bin/node`). The first attempt stopped on that. The orchestrator then supplied the official linux-x64 tarball from nodejs.org, SHA-256 checked against the published SHASUMS256.txt from the same origin (so the checksum proves download integrity, not independent authenticity), unpacked outside the repository at `/tmp/claude-0/node24/node-v24.22.2-linux-x64`. Every command ran with `export PATH=/tmp/claude-0/node24/node-v24.22.2-linux-x64/bin:$PATH`. A second stop happened because `cdn.playwright.dev` was blocked by the sandbox proxy (403); the Sponsor allow-listed it and I resumed. No pin was changed.

## 3. Evidence

### AC1
- `cat .nvmrc` -> `24.22.2`
- `package.json`: `"engines": { "node": "24.22.2" }`
- `node --version` -> `v24.22.2` (npm 11.19.1)

### AC2
`.npmrc`:
```
ignore-scripts=true
save-exact=true
engine-strict=true
```
`package.json`: private, `devDependencies: { "@playwright/test": "1.64.1" }`, no scripts, no other dependency.

- `npm ci`: `added 3 packages, and audited 4 packages in 1s` / `found 0 vulnerabilities` (exit 0).
- `npx playwright --version` -> `Version 1.64.1`
- `npx playwright install chromium`, first attempt before the allow-list: exit 1, `server returned code 403 ... host "cdn.playwright.dev"`. After the allow-list: exit 0; downloaded FFmpeg v1013, Chrome for Testing 156.0.8078.4 (chromium v1248) and Chrome Headless Shell 156.0.8078.4 (v1248) into `/opt/pw-browsers` (`PLAYWRIGHT_BROWSERS_PATH` is set in the sandbox).
- `npm ls --all`:
```
glober-books-e2e@0.0.0
`-- @playwright/test@1.64.1
  `-- playwright@1.64.1
    `-- playwright-core@1.64.1
```
No `typescript`. `find /home/user/gb-task10 -name 'tsconfig*' -not -path '*/node_modules/*'` printed nothing.

### AC3
- `npm audit --audit-level=high` -> `found 0 vulnerabilities`, exit 0. The lockfile was not changed by the browser install.
- Non-`registry.npmjs.org` `resolved` entries: 0. Total `resolved` entries: 3 (`@playwright/test`, `playwright`, `playwright-core`, each `https://registry.npmjs.org/.../-/...-1.64.1.tgz`).
- Not run: `npm audit signatures` (not required by the brief).

### AC4
Command, run once through a throwaway stdio client kept in the scratch area outside the repository (it sent `initialize` and `tools/list` only; no page was opened, no tool was called, no navigation):
```
npx @playwright/mcp@0.0.83 --headless --isolated --block-service-workers --no-webmcp --output-dir /tmp/claude-0/mcp-out --allowed-origins http://127.0.0.1:18080
```
- Server info: `{"name":"Playwright","version":"1.64.0-alpha-1790635538000"}`. This is the pre-release Playwright build the MCP pulls (SEC-F-019); it differs from the `@playwright/test` 1.64.1 used by the suite.
- Tools exposed (25): browser_click, browser_close, browser_console_messages, browser_drag, browser_drop, browser_emulate_media, browser_evaluate, browser_file_upload, browser_fill_form, browser_find, browser_handle_dialog, browser_hover, browser_navigate, browser_navigate_back, browser_network_request, browser_network_requests, browser_press_key, browser_resize, browser_run_code_unsafe, browser_select_option, browser_snapshot, browser_tabs, browser_take_screenshot, browser_type, browser_wait_for.
- Diff against the 24-name `tools:` allowlist of the frontend developer definition file:
  - In the allowlist but NOT exposed with these flags (7): browser_check, browser_uncheck, browser_generate_locator, browser_verify_element_visible, browser_verify_text_visible, browser_verify_list_visible, browser_verify_value. Probably behind an optional capability flag (for example `--caps`); not verified, I did not retry, to respect "run once".
  - Exposed but NOT in the allowlist (8, must stay unused): browser_drag, browser_drop, browser_emulate_media, browser_evaluate, browser_file_upload, browser_find, browser_run_code_unsafe, browser_tabs.
  - 17 names are common to both.
- Disk use of the browser cache `/opt/pw-browsers`, per directory. Pre-existing (mtime 2026-10-22, untouched by me): chromium-1243 393M, chromium_headless_shell-1243 261M, ffmpeg-1011 4.9M. Added by my `playwright install chromium`: chromium-1248 403M, chromium_headless_shell-1248 273M, ffmpeg-1013 5.1M (about 681M). The MCP run added no directory (no browser was launched). I did not take a `du` before my install, so the "before" is reconstructed from directory mtimes and not fully proven.
- npx downloaded the MCP package from the registry during this run; its dependency tree was not audited (it is not in `package.json`).

### Scope
Commit author line: `krlosferra <carlos.ferra@globant.com>` (configured human identity, unchanged).

`git diff --stat FETCH_HEAD...HEAD` (branch against main with Task 01, taken before the F1 commit; the F1 commit then removes `progress-task-10.md` and adds 11 lines to `progress.md`, and this handoff edit adds a few lines):
```
 .../037-liaison-to-frontend-task-10-brief.md       | 97 ++++++++++++++++++++++
 .../038-frontend-to-liaison-task-10-delivery.md    | 73 ++++++++++++++++
 docs/sdd/progress-task-10.md                       |  7 ++
 e2e/.npmrc                                         |  3 +
 e2e/package-lock.json                              | 63 ++++++++++++++
 e2e/package.json                                   | 12 +++
 6 files changed, 255 insertions(+)
```
`git status --short` before the F1 commit (the two untracked files are the Liaison and Security handoffs 041 and 042, not mine and not committed by me):
```
D  docs/sdd/progress-task-10.md
M  docs/sdd/progress.md
?? docs/sdd/handoffs/041-liaison-to-orchestrator-task-10-audit.md
?? docs/sdd/handoffs/042-security-to-orchestrator-task-10-review.md
```
The `.nvmrc` already existed on main with `24.22.2`, so it is unchanged.

### Setup note: how the pinned Node was supplied
The sandbox default is Node v22.22.0, not the pin. The pinned Node v24.22.2 came from the official linux-x64 tarball on nodejs.org, unpacked outside the repository, with `PATH` prefixed for every command. Its SHA-256 was checked against the SHASUMS256.txt published at the same origin. Provenance beyond that same-origin checksum is not proven (no independent signature or second source was verified).

## 4. Issues for the Liaison and Sponsor
1. Allowlist vs server tool names mismatch (7 missing, 8 extra exposed), see AC4. The `tools:` list probably needs a capability flag or an update (SC-23 re-derivation). I did not edit the agent files.
2. `cdn.playwright.dev` is a required download host for `playwright install`, an extra supply-chain origin beyond the npm registry.
3. A pre-installed Chromium v1243 coexists with the v1248 one that `@playwright/test` 1.64.1 installed; Task 13 must rely on the one matching the installed library.
4. `status.md` has no Task 10 row.

## 5. Security note
Lockfile: 3 entries, all `registry.npmjs.org`. Audit: 0 vulnerabilities at high level. `.npmrc` has `ignore-scripts`, no token, no registry override. MCP: pinned 0.0.83, flags as specified, no unsafe tool called, no page opened, pre-release Playwright 1.64.0-alpha-1790635538000. Browser download from `cdn.playwright.dev`. Node supplied by a tarball whose checksum came from the same origin.
