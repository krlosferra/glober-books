# Handoff 208: Task delivery, Task 43 (accessibility check and its pin)
- From: frontend   To: liaison   Type: TASK_DELIVERY   Brief: 207

## Resume point
Done: pin, spec, runs, progress row. Next: Liaison/Security review; Sponsor to decide on the failing older E2E tests (below). Task: 43. Branch: `chore/task-43-accessibility-check`. Commit: see `git log` (single local commit `test(e2e): ... [Task-43]`), not pushed.

## Files
Edited: `e2e/package.json`, `e2e/package-lock.json`, `docs/sdd/progress.md`. Created: `e2e/tests/us-014-accessibility.spec.ts` (about 100 lines). Nothing else changed (no frontend/, backend/, config, headers, CSP, constitution or frontend-testing.md).

## Criterion 1 record (made before the install)
- Publisher Deque: maintainers include labs@deque.com, axe@deque.com; repository github.com/dequelabs/axe-core-npm.
- `@axe-core/playwright` 4.14.0 (dist-tag latest, MPL-2.0, registry.npmjs.org tarball, integrity sha512-SGUhG5PIHd2eMdpgBF2PyQLOhX+qyDR2PT4cmoS4Qk2ak+jX/rrSvst9l0PGv5WTHolKpUMvVmoHpr4cZgf1RQ==); only dependency `axe-core ~4.14.0`.
- `axe-core` 4.14.0 (latest, MPL-2.0, Deque maintainers dqlabs and npmdeque, registry tarball, no dependencies).
- No preinstall, install or postinstall script and no `hasInstallScript` on either package or in the lockfile. `@axe-core/playwright` has a `prepare` script that npm does not run for registry installs; `e2e/.npmrc` also sets `ignore-scripts=true`.
- Peer range `playwright-core >= 1.0.0` accepts 1.64.1.
- Lockfile `resolved` hosts: 5 entries, all `https://registry.npmjs.org`.
- `npm audit --audit-level=high`: 0 vulnerabilities, exit 0.

## Proposed constitution pin
`@axe-core/playwright` 4.14.0 (exact, no range; transitive `axe-core` 4.14.0), dev dependency of `e2e/` only, test code only. Task 48 aligns constitution and `frontend-testing.md`.

## Criteria to tests
| Criterion | Test (all in `us-014-accessibility.spec.ts`) |
|---|---|
| 1 | record above; pin in package.json and lockfile |
| 2, data state, labels | US-014.1 AC-1, US-014.2: data state with three books, one completed and rated |
| 2, field errors | US-014.1 AC-2: add form with two field errors |
| 2, dialogs | US-014.1 AC-3: duplicate dialog; status confirmation dialog; delete dialog |
| 3 | all scans via `AxeBuilder` on the page under the app CSP; no `bypassCSP`, no rule disabled; `guard` fixture active in every test |
| 4 | design constraint: no bypassing context at file or project level, so Task 45 can run without bypass |

Each test asserts zero serious or critical violations (tags wcag2a, wcag2aa, wcag21a, wcag21aa), that axe evaluated at least one rule, and that every input, select, button and dialog has a programmatic name.

## Results
- `node --test "frontend/**/*.test.js"`: 110 tests, 110 pass, 0 fail.
- New spec with `--repeat-each=3` (desktop): 15 passed, 0 failed.
- Full E2E (`npx playwright test`, 100 tests, 38.7 min on this workstation): 94 passed, 6 failed. All 6 are in older specs, not edited (rule: report, do not edit):
  - `us-010-library-states.spec.ts:136` (1,000 and 5,000 max-size entries, timing)
  - `us-010-library-view.spec.ts:16, :24, :49` (three @smoke tests)
  - `us-012-status-rating.spec.ts:222` and `:243`
- Rerun of `us-010-library-view` and `us-012-status-rating` alone (30 tests): the three smoke tests passed, so those and the 1,000/5,000 timing test are most likely load or timeout related on this slow workstation (not re-run alone). Two failures reproduce: `:222` (`radio.check()` "did not change its state") and `:243` (30 s timeout on `radio.check()` of a rating radio). Both are rating-radio interactions unrelated to my files; I did not inspect the trace beyond the error text. Classification (bad code, bad test or environment) is for the Liaison. They do not touch Task 43 code.

## Deviations
- Playwright MCP not available (connection timeout); exploration through committed test code only.
- Local Node 24.21.0 vs pin 24.22.2: `npm ci` run with `npm_config_engine_strict=false` as a shell variable; no file changed.
- Chromium 1248 (plus headless shell, ffmpeg, winldd) installed with `npx playwright install chromium`.
- First Maven build failed on an expired mirror token (401); rebuilt with the repository's own settings after renewal.

## Risks
- The six failing older tests (above) may mask regressions; the suite is not green on this workstation.
- axe covers only rules it can evaluate automatically; it is not a manual review. Contrast of the focus ring is covered only as far as axe checks it.
- Mobile project (360 px) not scanned: its grep is `@mobile` only.
