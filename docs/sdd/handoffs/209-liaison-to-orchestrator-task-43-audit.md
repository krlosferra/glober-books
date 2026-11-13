# Handoff 209: Audit report, Task 43 (accessibility check and its pin)
- From: liaison   To: orchestrator   Type: AUDIT_REPORT   Re: 207, 208, commit 08b32b3

## Verdict: ACCEPT WITH FOLLOW-UPS
Task 43 meets criteria 1 to 4. Nothing in the six failing older tests blocks it. Follow-ups are small and none changes the delivered files.

## Verified by me (read of the diff and files)
- Scope: the commit touches only the five listed paths (`e2e/package.json`, `e2e/package-lock.json`, `e2e/tests/us-014-accessibility.spec.ts`, `docs/sdd/progress.md`, handoff 208). The brief and `status.md` also appear in `main..HEAD` (earlier brief commit 815ebf7); that is in line with the repository's docs practice. No change under `frontend/`, `backend/`, `playwright.config.ts`, the constitution or `frontend-testing.md`.
- Pin: `@axe-core/playwright` is `4.14.0`, exact, with no range, dev dependency only. In the lockfile all `resolved` entries are `registry.npmjs.org`. There is no `hasInstallScript`. The peer is `playwright-core >= 1.0.0`, which accepts 1.64.1. `axe-core` is `4.14.0`, with no dependencies. This matches the criterion 1 record.
- Criterion 3 and 4: the spec file contains no `bypassCSP`, no `disableRules` and no new context. It uses the existing `guard` fixture, which is `auto: true` (`fixtures.ts:101`), so it stays active in every test and a CSP violation would fail them. The scan runs under the app's CSP through `AxeBuilder`. Task 45 stays free to run without bypass.
- Criterion 2: five tests. Data state: 3 books, one completed and rated 4, asserted checked. Add form: both `#title-error` and `#author-error` visible. Three dialogs: duplicate, status confirmation, delete, each asserted visible before the scan. Each test asserts no serious or critical violations on wcag2a, wcag2aa, wcag21a and wcag21aa, and asserts that axe evaluated at least one rule. It also runs the programmatic-name check (US-014.2). Assertions are web-first with no fixed sleeps. The file is 96 lines, under 300.
- The tests seed with the existing `library` helper and `reset()`. No fixtures were duplicated.

## Taken from the delivery (not re-run by me)
Registry metadata and publisher (Deque), `npm audit` 0 vulnerabilities, 110 unit tests, 15 of 15 with `--repeat-each=3`, full E2E 94 of 100 over 38.7 min, Chromium install, Node 24.21.0 against the 24.22.2 pin (the delivery reports it and does not edit the pin). I did not run a build or the browser.

## Findings
| ID | Sev | Finding | Resolution |
|---|---|---|---|
| F-43-1 | MINOR | The name check `unlabelledControls` is a hand-written heuristic. It accepts any non-empty `aria-labelledby` without resolving it, and does not cover `input type=image`, and so on. Axe's own `label` and `button-name` rules already cover the same ground (they are in the wcag2a set), so the risk is low. | Accept; optional hardening later. |
| F-43-2 | MINOR | Mobile (360 px) is not scanned; the task block does not require it (orchestrator context 2). The delivery lists it as a risk. | Record as a candidate item for Task 46 or a later task if US-014.3 needs it. |
| F-43-3 | MINOR | Only serious and critical impacts are asserted, as the task states. Moderate and minor are not reported. | None; matches criterion 2. |
| F-43-4 | MINOR | Handoff 208 says "about 100 lines" for the spec and "five scans"; both are accurate. The proposed pin text is given; Task 48 aligns the constitution table and `frontend-testing.md`. | Carry to Task 48. |

## The six older failures (classification)
- `us-012-status-rating.spec.ts:222` and `:243` (3 variants): these are rating-radio tests where the server state changes behind the page. The action is `radio.check()` on a radio that the app then removes or replaces (reload of the entry, or removal of the entry after a 404). Playwright's `check()` verifies the final state of the element it clicked, and so races the re-render. The error "did not change its state" and the 30 s timeout on `:243` fit this pattern. Classification: test fragility (a race between `check()` and the re-render) that is made visible by load. It is not a product defect, because the same tests passed 6 of 6 with `--repeat-each=3` on the same build and the assertions on the app's behavior passed. `:222` was extended by Task 54 (focus fallback), so it is the newest. Suggested follow-up for a test-hardening task owned by the frontend developer: use `click()` with an explicit post-condition assertion for those actions instead of `check()` where the radio is meant to disappear. I read the test source only; I did not inspect traces, so this is a reasoned classification, not a measured one.
- `us-010-library-view` smoke tests (3): they passed when re-run alone. Environment, load or timeout (slow Windows workstation, 38.7 min run). Not a defect.
- `us-010-library-states.spec.ts:136` (1,000 and 5,000 timing): not re-run, by Sponsor decision. Unverified; the likely cause is load, as it is a timing test. Do not report it as green.
- None of the six touches a Task 43 file, and all six ran against code unchanged by this commit. They do not block the acceptance of Task 43. They do mean the E2E suite has not been shown green end to end on this workstation. Before the final E2E gate a clean run on the CI-like environment is needed (this is a gate matter, not a Task 43 matter).

## Hygiene (Layer 1b)
Commit message `test(e2e): pin axe checker and scan WCAG 2.1 A and AA states [Task-43]` follows the format; author is the human identity; no trailers, no tool or vendor names; branch name matches the task block; one atomic commit; no secrets or build output.

## Security
Task 43 is `Security: per task`: it needs the `sdd-security-reviewer` verdict (supply-chain pin, SEC-F-076 no bypass). I do not replace it. Points for Security: the new dev dependency is MPL-2.0, `ignore-scripts=true` is set in `e2e/.npmrc`, and no bypass was used.

## Next steps
1. Security verdict on the pin.
2. Sponsor gate, then the pull request. The orchestrator should write the six failures as an open item (hardening of the two rating tests; clean-run requirement) in `docs/sdd/active-conditions.md` if the Sponsor wants them tracked (that file is the orchestrator's or owner's to edit; I did not change it).
3. Task 48 aligns the constitution table and `frontend-testing.md` with the pin.
