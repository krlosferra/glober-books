# Handoff 089: Task 13 delivery
- From: frontend | To: liaison | Type: TASK_DELIVERY | Branch: `chore/task-13-e2e-harness`

## Resume point
- Done: everything in the brief (launcher, config, global setup, placeholder, busy-port fix, @isolation tag check, hostile run, progress.md). Next: the orchestrator audits and pushes.

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

## Coordinator decisions applied
1. `ss` was not installed; used `lsof -nP -iTCP -sTCP:LISTEN` and `/proc/net/tcp` (no `/proc/net/tcp6` exists). No package installed.
2. Busy port. The launcher now makes a raw TCP connect to 127.0.0.1:18080 before `mkdtemp` and exits 1 with `port 18080 on 127.0.0.1 is already in use; refusing to start`. Run through Playwright the launcher check alone was NOT enough: with a silent listener the run still took 35 s, because Playwright probes the readiness URL and waits before it starts the command. So `playwright.config.ts` also runs `node support/start-server.mjs --check-port` once at config load, in the runner process only (`TEST_WORKER_INDEX` undefined; workers skip it, as the server is already up). Results:
   - silent listener (accepts, never answers): fails in about 1 s, `Error: port 18080 on 127.0.0.1 is already in use; stop that process first` (config.ts:10);
   - HTTP-answering listener: fails in about 1 s with the same message (before, Playwright's own `is already used` error);
   - free port: `1 passed (7.1s)`.
3. `__dirname` kept; `e2e/package.json` unchanged.
4. The launcher does not pass `-Xmx`.
5. Tag selection. Config uses `grepInvert: process.argv.includes('@isolation') ? undefined : /@isolation/` (a CLI `--grep` combined with a plain `grepInvert` found no test). Outputs:
   - `npx playwright test --list`: `[desktop] harness.spec.ts:3:5 Task 13 AC5: the readiness URL answers 200`, Total: 1 test.
   - `npx playwright test --grep @isolation --list`: `[desktop] harness.spec.ts:9:5 Task 13 AC3: ... under hostile variables @isolation`, Total: 1 test.
   - the project `mobile` runs only `@mobile` tests (none yet).

## Hostile-environment run (AC3, by hand)
Variables exported: `SPRING_DATASOURCE_URL=jdbc:sqlite:<decoy.db>`, `SPRING_APPLICATION_JSON={"spring.datasource.url":"jdbc:sqlite:<decoy2.db>","server.address":"0.0.0.0","server.port":18999}`, `JAVA_TOOL_OPTIONS=-Dspring.datasource.url=jdbc:sqlite:<decoy3.db>`, `SERVER_ADDRESS=0.0.0.0`, `SERVER_PORT=18998`. Decoy in the session scratchpad.
- Held by an instance before hashing: `lsof` found no holder (exit 1).
- SHA-256 before and after (launcher run, then `npx playwright test --grep @isolation`): `352db2c55477d02af9b2c440ffa186f4f8bf3e069eb7e0c336a7ea2b48c4cefb`, identical three times. decoy2.db and decoy3.db were never created.
- Listeners before: `127.0.0.1:39237` and `127.0.0.1:42775` (sandbox tools). During: the same plus `java 14064 127.0.0.1:18080 (LISTEN)`; the only java listener. `/proc/net/tcp` LISTEN entries: `0.0.0.0:2024` (pre-existing, not java: lsof shows no java socket there), 127.0.0.1:39237, :42775, :18080. Probes to `0.0.0.0:18999` and `0.0.0.0:18998` get no connection (`000`).
- Data path: java cwd `/proc/14064/cwd -> /tmp/globerbooks-e2e-12StpG` (mode `drwx------`) holds `library.db` only (no config file); child environment keys: `HOME JAVA_HOME PATH`.
- SIGTERM to the launcher: exit 143, java gone, `/tmp/globerbooks-e2e-*` removed, no listener left on 18080.
- Playwright `--grep @isolation` under the hostile variables: `1 passed (7.2s)`.
- Not done: the run does not read the decoy through the application, as AP-9.2 says.
