# Handoff 103: Security review, header value fix (SEC-F-130, constitution 1.4)
- From: security
- To: orchestrator
- Type: AUDIT_REPORT
- Branch: chore/permissions-policy-bluetooth (diff against main)
- Phase: 4, per task (headers)

## Scope and method
Read handoffs 100 and 101; read `git diff main` of `HeaderPolicy.java`, `HeaderPolicyTest.java`, `e2e/support/fixtures.ts`, the constitution 1.4 amendment and the SC-10 row of `security.md`; grep for `bluetooth`/`SEC_F` in `backend/src` and `e2e`. I did not re-run the build, the smoke or the seeded control (not re-executed; evidence from handoff 101 is the delivery claim, consistent with the diff). Not verified: a real Firefox/Safari behaviour (not available here).

## Checks
- HeaderPolicy diff is one line: `bluetooth=(), ` removed from PERMISSIONS. The CSP constant and the other 6 header constants are not in the diff (byte-identical). Remaining features: camera, microphone, geolocation, payment, usb, serial, clipboard-read, display-capture, fullscreen, all `()`.
- Java test: the 9 remaining features are required plus `doesNotContain("bluetooth")` (pins the new value).
- Guard (`fixtures.ts`): the SEC_F_130 constant and the `!== SEC_F_130_WARNING` exclusion are gone; the console regex `Content Security Policy|Trusted Type|Permissions-Policy` now records every error or warning matching it, so nothing is tolerated. PERMISSION_FEATURES still requires the 9 remaining features; CSP and the other 6 headers (EXACT_HEADERS) untouched.
- Constitution 1.4 and the SC-10 row are consistent with the code (constitution says "at least"; the 9 features remain). The SC-10 row's Evidence cell carries no 1.4 note yet (see appendix).
- Seeded control (handoff 101, item 4): re-adding `bluetooth=()` made the guard fail on the exact warning, then reverted; the diff confirms the reverted state. This proves the guard now fails on that warning.

## Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-145 | INFO | Header policy | `HeaderPolicy.java` PERMISSIONS | Removing `bluetooth=()` drops the explicit deny of Web Bluetooth for browsers that might honour it. Chromium logs "Unrecognized feature" so the entry never had effect there; Firefox and Safari do not implement Web Bluetooth. No browser in scope loses protection. Compensating controls: CSP `script-src 'self'` and `require-trusted-types-for`, no third-party script, `frame-src 'none'`, no user-supplied script path. Web Bluetooth also needs a user gesture and a chooser prompt. | diff; handoff 101 item 4 | None realistic; only a future browser that adds a `bluetooth` policy token would lack the explicit deny. | No new policy needed. Optionally re-check the Chromium feature list on Playwright upgrades; if Chromium adds the token, re-add it by a constitution change. Residual risk recorded as accepted by the Sponsor's 2026-11-05 decision (the amendment). | SC-10 |
| SEC-F-146 | INFO | Documentation | `security.md` SC-10 row | The Evidence cell does not yet mention constitution 1.4 and the removed feature. | row read | None | Add the sentence in the appendix. | SC-10 |

No CRITICAL, HIGH, MEDIUM or LOW findings.

## Closure
- SEC-F-130: can be closed (value corrected, guard tolerates nothing, seeded control).
- SEC-F-139: can be closed (the filter was removed; `grep` finds no `SEC_F`).
- SEC-F-138: only the "no seeded wrong-header control" part is closed. The Set-Cookie/Location absence assertion and the non-http(s) request gap remain; keep it OPEN as LOW with the reduced text in the appendix.

## Controls checked
SC-10: VERIFIED for the new value (code, test and guard read; seeded control per handoff 101). No other control touched.

## Residual risks
SEC-F-145 only (INFO); recommendation: accept, no action.

## Verdict
PASS. Next step: the Liaison audit, then the Sponsor gate; the orchestrator applies the appendix text to `security.md`.

## Appendix: exact `security.md` text
1. SC-10 Evidence cell, append: `2026-11-05 Header fix (handoff 103): Permissions-Policy value no longer lists bluetooth=() (constitution 1.4, SEC-F-130 closed); the guard tolerates no console message and requires the other 9 features; seeded wrong-header control fails the guard (SEC-F-138 part).`
2. Findings log: `| SEC-F-130 | LOW | (unchanged text) | CLOSED 2026-11-05 (handoff 103): bluetooth=() removed from the value, constitution 1.4 | 103 |`
3. `| SEC-F-139 | INFO | (unchanged text) | CLOSED 2026-11-05 (handoff 103): filter removed | 103 |`
4. `| SEC-F-138 | LOW | Guard: the foreign-origin check sees only http(s) requests; no Set-Cookie or Location absence assertion | OPEN: covered by the CSP violation channel; hardening in a later e2e task. The seeded wrong-header control part is CLOSED (handoff 103) | 095, 103 |`
5. `| SEC-F-145 | INFO | Dropping bluetooth=() removes the explicit Web Bluetooth deny; Chromium ignored it, Firefox and Safari have no Web Bluetooth; compensating CSP and Trusted Types | ACCEPTED by the Sponsor decision of 2026-11-05 (constitution 1.4); re-check on Playwright upgrades | 103 |`
6. `| SEC-F-146 | INFO | SC-10 evidence text lacked constitution 1.4 | CLOSED by item 1 | 103 |`
7. Changelog: `- 0.35 (2026-11-05): header fix review (handoff 103): SEC-F-130, SEC-F-139 closed; SEC-F-138 seeded-control part closed; SEC-F-145 and SEC-F-146 added. Verdict PASS.`
