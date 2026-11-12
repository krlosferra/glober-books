# Handoff 177: Audit of Task 28 (E2E library view, filter and states)
- From: liaison   To: orchestrator   Type: AUDIT_REPORT   Mode: F (with G)
- Artifacts: tasks.md Task 28, handoffs 175 and 176, commits `8951848` and `9efd95d`, the e2e specs and support files, `listState.js`, spec US-010.1 to .10 and 5.9.

## Verdict: ACCEPT WITH FOLLOW-UPS (Layer 1, 1b, 2 pass; Security verdict still required, `Security: per task`)

## Evidence basis
Re-run by me: `node --test frontend/js/logic/*.test.js` (54 of 54 pass, includes the SEC-F-197 test), `wc -l`, `git show`. Taken from the delivery, not re-run: the full E2E (27 passed, exit 0), the 3x flakiness run (25/25 each). I did not run Playwright.

## Criteria (each asserted, not vacuous)
| AC | Verdict | Basis |
|---|---|---|
| 1 | PASS | title, one h1, "Your books", list "Books"; loading text in `#list-status` with `role=status` while the held list request is pending, entries 0 and `#count` hidden; far zones checked by hand: Kiritimati (+14) gives 03-15 / 03-16, Pago_Pago (-11) gives 03-14 / 03-14, both match the stubbed UTC values; rating texts (rated, "Not rated", none for unread); hostile and 500-code-point strings with zero `img`/`script`, no dialog. |
| 2 | PASS | order compared with the API order, "1 book" / "3 books" / "1000 books"; four radios with names, All checked, `status` query sequence `['', '?status=completed', '']`; empty text, "0 books", no button; filter-empty text, "Show all books" checks All and reloads. |
| 3 | PASS | 7 failure kinds with the 5.9 literals plus "Try again", zero items, count hidden, recovery; active filter repeated; add during a held load and after a failure is announced and not inserted, then listed by the next load requested after the add (filter change, so the load post-dates the add). |
| 4 | PASS | both release orders; the early release of the older response leaves "Loading", zero entries and no count (stale discarded); final result is the last request's. |

Oracle quality: expected strings are literals typed from the spec; no import of `copy.js` anywhere in `e2e/` (grep). Request counts are asserted on the real page through the held-load controller (F-1 of 173 met for the list). The guard fixture is automatic per test, so zero CSP violations, console problems and foreign origins are asserted in every flow; stubs keep the real headers (`fulfillLikeReal`).

## Checks
- Determinism: no `waitForTimeout` or sleeps; `expect.poll` and web-first assertions. Sole fixed numbers are timeouts (120 s, 600 s) in the slow test.
- File limits: all at or under 300 lines (largest 206). Production `listState.js` 138.
- SEC-F-197: fix is correct and minimal (`typeof text === 'string' && text !== ''` else `SERVER_FAILED_TEXT`); unit test covers three codes; E2E case DUPLICATE_BOOK on a list gives the generic text (this is the case that was empty before). Note: the "VALIDATION_ERROR without details" case already had a text path (`requestFailed`), so it guards a neighbour, not the bug. Close SEC-F-197.
- SEC-F-198: measured (1,000 entries 0.57 s; 5,000 first load 2.0 s; re-render 1.7 s, above the 1 s trigger), `content-visibility` tried and reverted, no CSS change, CSP untouched, left to Task 48 as the brief says. Correct outcome. Keep open, retarget to Task 48.
- Hygiene (1b): branch name and `[Task-28]` Conventional Commits fine, explicit paths, no attribution seen in the commit author or subject lines, local only.
- Deviation 2 (two extra files `view.ts`, `us-010-library-states.spec.ts`): ACCEPTED. Justified by the 300-line limit (rule 6 of my doctrine, brief constraint 6), written down in the delivery; `tasks.md` file list should be amended by the PM for traceability (test support files only, no production effect).
- Deviation 1 (listState.js): allowed by the brief and my conditions.

## Findings
| ID | Sev | Finding | Resolution |
|---|---|---|---|
| F-1 | MINOR | The 5,000-entry test (about 3 min, 2.9 to 5.2 min observed) is in the default suite and makes every run slow; it is a measurement, not a requirement test. | Tag `@slow` in the title now and exclude it from the default run like `@isolation` (`grepInvert` in `playwright.config.ts`, a Task 13 file: change belongs to Task 48 or a one-line approved edit by the Sponsor). Run it on demand and at the gate. Until then accept it in the default run. |
| F-2 | MINOR | One-shot negative assertions (`toHaveCount(0)` right after an add during a held load) can pass before the page acts. | Mitigated: they are followed by a release and by positive checks of the exact set of entries. No change required. |
| F-3 | INFO | `#book-list > li` and `#count` used where no accessible locator exists. | Accepted; keep to those cases. |
| F-4 | INFO | Perf timings are single, noisy measurements. | Task 48 must repeat the measurement before choosing a fix. |

## Open items (active-conditions.md updated)
- Closed: SEC-F-197.
- Retargeted: SEC-F-198 to Task 48 (with the numbers above).
- Liaison F-1 to F-3 of handoff 173: `#list-status`, request counts of the list, spec literals and per-flow CSP checks are done; the Messages-update count, one POST on repeated submit, Tab and Shift+Tab after a click on the dialog text and the short-viewport containment remain for Task 29 only.

## Next
Security verdict on Task 28 (per task), then Sponsor gate; recommend the `@slow` tag before merge. Reminder: `progress.md` already updated by the delivery; commit message of acceptance `[Task-28] ... - ACCEPTED`.
