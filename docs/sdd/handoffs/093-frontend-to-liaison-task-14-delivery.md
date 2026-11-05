# Handoff 093: Task 14 delivery (smoke tests, guard fixture and README)
- From: frontend
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY
- Task: 14, branch `chore/task-14-smoke-guard-readme` (local commits, not pushed)

## Resume point
Done: all steps. Nothing left except the orchestrator push and the Liaison and Security audits.
Open: Playwright tool-server run not done (see item 6 below); the orchestrator decides.

## Evidence
### Smoke run (desktop)
```
  3 passed (8.8s)   (M-1 keyboard add took 294 ms; tests 333, 578, 248 ms)
```
### Guard negative control (throwaway spec `zz-control.spec.ts`, deleted after the run; fixtures.ts restored with git checkout)
Seeded, each failed the guard with the quoted detail:
- inline script via a script element: `securitypolicyviolation events` = `require-trusted-types-for trusted-types-sink` (Trusted Types blocked the sink)
- fetch to `http://127.0.0.1:1/x`: violation `connect-src http://127.0.0.1:1/x` and the console CSP messages
- navigation to `http://127.0.0.1:1/x`: `requests to another origin` = `http://127.0.0.1:1/x`
- uncaught error in a timer: `page errors` = `seeded`
- inline style attribute and style element: `style-src-attr inline`, `style-src-elem inline`
- SEC-F-130: with the filter constant replaced by a dummy, all three smoke tests failed with `Error with Permissions-Policy header: Unrecognized feature: 'bluetooth'.` (exact text, so the filter constant is the real message); with the constant restored the smoke is green. The guard treats Permissions-Policy console messages as guarded, so only that exact string is tolerated.
- Not done: no seeded wrong-header control (the header check passes on every asset of the real page; a wrong value was not provoked).


### Other evidence
- `cd backend && ./mvnw verify` on a clean copy (committed tree via `git archive` into the scratchpad): BUILD SUCCESS in 1:10 min, `Tests run: 120, Failures: 0, Errors: 0, Skipped: 1`, Checkstyle, SpotBugs (0 bugs) and JaCoCo checks met, `target/globerbooks.jar` built.
- `node --test "frontend/**/*.test.js"` (working tree and clean copy): tests 43, pass 43, fail 0.
- Full suite in the working tree: `4 passed (8.8s)` (harness plus 3 smoke); smoke alone `3 passed (8.8s)`, wall 10 s.
- `hygiene.sh` with BASE_SHA=main and HEAD_SHA=HEAD: no output, exit 0.
- Clean-copy proof (README commands, only JDK 21.0.12.1 and Node 24.22.2): `mkdir -p backend/data`; `cd e2e && npm ci --ignore-scripts` (added 3 packages, 0 vulnerabilities); `npx playwright install chromium` (already present); smoke `3 passed (8.6s)`; `cd backend && java -Xmx512m -jar target/globerbooks.jar` started (`Started GloberBooksApplication`), `POST` gave `HTTP/1.1 201` with the Dune book (`status` `unread`, `rating` null), `GET` gave `200` with `"total":1`, page `200 text/html;charset=UTF-8`; stopped, port 8080 answered nothing afterwards. `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments=-Xmx512m` also started, answered the list and the page, and was stopped.
- First attempt of the jar command as the brief wrote it (`java -Xmx512m -jar backend/target/globerbooks.jar` from the root) failed: `SQLiteException ... unable to open database file`, because the URL is `jdbc:sqlite:./data/globerbooks.db` (relative to the working directory). The README now says to run both commands from `backend/`. This is a finding against Task 14 criterion 3 text and the Task 08 note (which already says "run from `backend/`"); the Liaison or PM decides whether the criterion text or the path handling (Task 34a) changes.
- Processes started were stopped (checked: nothing answers on 8080 or 18080); the clean-copy directory was deleted.

### Acceptance criteria
1. Smoke green under 30 s (9-10 s): `fixtures.ts`, `tests/us-010-library-view.spec.ts` (titles carry US-010.1 AC-1, US-011.2 and M-1, US-014.6 T-12). M-1 recorded as an annotation and a console line (about 300 ms), bound 10 s. The empty library message and `library.reset()` are not used in the smoke: the backend has no `DELETE` yet (Task 31 area), so the tests count from `GET` before the add (expected wording from US-010.4). `reset()` exists and is untested until DELETE exists.
2. Guard: `e2e/support/fixtures.ts`, negative control above.
3. README: `README.md`, clean-copy proof above (deviation: jar from `backend/`).
4. verify, node tests, smoke, hygiene green; `progress.md` has the 0b commands and the Task 1 proof table.

### Item 6: Playwright tool-server exploration
Not done. The Playwright tool-server tools are not exposed to this run (no such tool is available to me). As instructed, no script was substituted. The orchestrator decides. The smoke tests cover the keyboard add and the inert title with a real browser, but not the snapshot, console and network inspection or the 360 px resize.

### Other notes
- Guard also treats Permissions-Policy console messages as guarded (otherwise the SEC-F-130 filter would be meaningless); the foreign-origin check sees navigations, while a blocked fetch shows as a `connect-src` violation.
- Headers are compared for equality with the constitution list (CSP exact, Permissions-Policy by feature, `Cache-Control: no-store`); `/favicon.ico` is skipped. The hostile test uses a hostile title with a unique suffix.
- `unique` and `stubs.ts`/`pages.ts` of AP-9.1 are not created (not in the Task 14 scope).

### Diff and log
```
README.md | 48 ; brief 092 | 27 ; delivery 093 | 26 ; progress.md | 32 ; status.md | 5 ; builders.ts | 27 ; fixtures.ts | 109 ; library.ts | 47 ; us-010-library-view.spec.ts | 57
9 files changed, 375 insertions(+), 3 deletions(-)   (before this final commit)
```
Author of every commit: `krlosferra <carlos.ferra@globant.com>`; subjects:
```
docs(sdd): add the Task 14 note and the Task 1 proof table [Task-14]
docs(readme): run the jar from backend because the database path is relative [Task-14]
docs(readme): add prerequisites, build, run and test instructions [Task-14]
docs(sdd): record the guard negative control for Task 14 [Task-14]
test(e2e): add the three Increment 0 smoke tests [Task-14]
test(e2e): add the auto guard fixture [Task-14]
test(e2e): add builders and API-only library helper [Task-14]
docs(sdd): add the Task 14 brief and update the resume point [Task-14]
```
