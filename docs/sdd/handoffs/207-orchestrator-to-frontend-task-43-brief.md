# Handoff 207: Task brief, Task 43 (accessibility check and its pin)
- From: orchestrator (direct dispatch, constitution 1.8)
- To: frontend
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 43 of `docs/sdd/tasks.md` (v0.6.8): the task block is the specification (criteria 1 to 4, traces US-014.1, US-014.2, DEC-026, SC-10, SC-21, SC-38). Branch `chore/task-43-accessibility-check` (checked out from main after PR 58). Commits tagged `[Task-43]`. Path rule: `docs/sdd/README.md` section 5.

## Environment (this is a Windows workstation, not the earlier container)
- JDK 21 is at `C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot` (`JAVA_HOME` already points to it; the default `java` on PATH may be 1.8, so check `java -version` in the shell you use). Maven 3.9.10 is at `/c/apache-maven-3.9.10`. Node is 24.21.0 locally while `e2e/package.json` pins `engines.node` 24.22.2: report the mismatch, do not edit the engine pin.
- `e2e/node_modules` and the Playwright browsers are not installed. Use `npm ci` in `e2e/` (lockfile as is), then install Chromium for the pinned Playwright through its own CLI. Record what was installed. If a download is blocked, STOP and report.
- The jar bakes the frontend at build time: build it from `backend/` (`mvnw package -DskipTests`, or `mvn -o` when offline) before running E2E. Port 18080 only, never 8080.
- The Playwright MCP server is not available to agents: explore with committed `@playwright/test` code only. Record that as a deviation.

## Read (by ID, not whole files)
Spec US-014.1, US-014.2, DEC-026; constitution SC-10, SC-21, SC-38 and the pin table; `docs/sdd/frontend-testing.md` (accessibility row, to be aligned in Task 48, not now); `e2e/support/fixtures.ts`, `pages.ts`, `builders.ts`, `view.ts` (reuse the existing fixtures and helpers; do not duplicate them); `e2e/tests/us-012-status-rating.spec.ts` for the pattern of opening each dialog. No other documents.

## Open items assigned (`docs/sdd/active-conditions.md`)
None assigned. SEC-F-094 (sources.test.js rules) is Task 45 or earlier, not here. Standing rule SEC-F-083 applies to the backend only.

## Scope
`e2e/package.json` and `e2e/package-lock.json` (edit: add the pinned accessibility checker as a dev dependency, exact version, no range); new `e2e/tests/us-014-accessibility.spec.ts`; `docs/sdd/progress.md`. No production code under `frontend/` or `backend/`, no `playwright.config.ts` change, no change to the server headers or CSP, no change to `docs/sdd/frontend-testing.md` or the constitution (the pin is only PROPOSED in the delivery; Task 48 aligns the documents).

## Binding constraints
1. Criterion 1 first, before installing anything: record publisher (Deque), the registry entries of the checker package and of `axe-core`, absence of install scripts, a peer range that accepts Playwright 1.64.1, the lockfile `resolved` hosts (registry only) and a clean `npm audit --audit-level=high`. If any of these fails, STOP and report; do not pin.
2. The checker runs in test code only. The application's content-security policy is never relaxed. Run the scan through `page.evaluate` first, without any bypass. If a bypass proves necessary: `bypassCSP: true` only on a dedicated context created inside `us-014-accessibility.spec.ts`, never in the config, never in a test using the `guard` fixture; STOP and report before using it (SEC-F-076).
3. Scan states (criterion 2): at least three books (one completed and rated), two field errors on the add form, and each dialog open. Zero violations of impact serious or critical on WCAG 2.1 A and AA tags; every control has a programmatic label (US-014.2). Do not disable rules to make it pass: a real violation is reported as a finding, not suppressed, and the frontend is not edited in this task.
4. Deterministic assertions only (web-first, no fixed sleeps). Tests that need data use the existing library helpers and isolation. Every file at or under 300 lines.
5. Criterion 4 (the Task 45 CSP and Trusted Types tests in a context without bypass) is a constraint on this file's design: do not create a bypassing context at file or project level.

## Delivery (compact, constitution 1.8)
Write `docs/sdd/handoffs/208-frontend-to-liaison-task-43-delivery.md` early with a "Resume point": task, branch, commit hashes; files edited; the criterion 1 record; a table mapping each criterion to its test; the exact proposed pin for the constitution table; summary lines of `node --test "frontend/**/*.test.js"` and of the full E2E run (tests, passed, failed, exit code; run the new spec with `--repeat-each=3` once for flakiness; unit tests and E2E counted separately); deviations; risks. No raw terminal output, no `git diff --stat`, no author log. Commit locally with explicit paths and `type(scope): subject [Task-43]`, never `--no-verify`, nothing naming a tool, model or vendor, no trailers.

## Rules
STOP and report on any spec gap or contradiction, any install script, any audit finding at high or above, any need for a bypass, any change outside the listed files, or any backend behaviour that differs from the contract. Do not push.
