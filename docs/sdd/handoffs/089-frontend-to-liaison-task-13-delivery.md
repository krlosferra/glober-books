# Handoff 089: Task 13 delivery (PARTIAL: stopped on a tool failure)
- From: frontend | To: liaison | Type: TASK_DELIVERY | Branch: `chore/task-13-e2e-harness`

## Resume point
- Done: launcher, config, global setup, placeholder test (commit 1a9a904); default run green; Q-F7 established; busy-port behaviour tested.
- STOPPED: the hostile-environment run needs `ss` (brief, constraint 4), which is not installed in the sandbox (`ss: command not found`). `lsof` exists. Waiting for a decision: use `lsof -nP -iTCP -sTCP:LISTEN` plus `/proc/net/tcp*` as the substitute, or install `iproute2`.
- Next: hostile run (decoy hash before/after, listeners, data path), `progress.md` update, final commit, paste `git log` after commits.

## Evidence so far
Default run (`cd e2e && npx playwright test`): `1 passed (7.4s)`, `[desktop] tests/harness.spec.ts:3:5 Task 13 AC5: the readiness URL answers 200`. The launcher's directory was removed afterwards (`/tmp/globerbooks-e2e-*` empty; the two `/tmp/globerbooks-it*` directories pre-date the run and were not touched).

Q-F7 (AC4): a probe `globalSetup` (temporary, deleted) fetched `http://127.0.0.1:18080/api/v1/books` and printed `PROBE globalSetup: server answers 200`. In Playwright 1.64.1 the `webServer` starts BEFORE `globalSetup`, as AP-9.2 states. Consequence: the temporary directory must be made by the launcher; `global-setup` keeps host, port and jar assertions (these run after the server is up, so a wrong jar would already have failed in the launcher, which also checks the jar).

Busy port (AC1):
- A responsive listener on 127.0.0.1:18080: `Error: http://127.0.0.1:18080/api/v1/books is already used, make sure that nothing is running on the port/url or set reuseExistingServer:true in config.webServer.` Run fails, as planned.
- FINDING (differs from plan wording): a listener that accepts the connection but never answers makes Playwright wait; the run took 29.4 s and passed once the listener exited (25 s) and the jar started. The port check is URL based, so a silent listener does not fail the run immediately. Not a defect in the harness, but "a busy port fails the run" holds only for listeners that answer HTTP. Needs a Liaison decision (a raw TCP check in the launcher would close it; not added, out of brief).

Other notes
- `global-setup.ts` uses `__dirname` (Playwright transpiles to CommonJS because `package.json` has no `"type": "module"`; `import.meta` failed). `package.json` untouched.
- `-Xmx512m`: not passed by the launcher; per brief, report only. The plan does not fix a flag for it.
- `@isolation` test is excluded by a top-level `grepInvert` and runs with `--grep @isolation` (not yet verified, part of the hostile run).
- A commit message carrying a session trailer is rejected by the repo guard; commits carry none, as the repository rules require.
