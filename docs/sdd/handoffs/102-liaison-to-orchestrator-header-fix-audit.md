# Handoff 102: Audit, header value fix (SEC-F-130, constitution 1.4)
- From: liaison
- To: orchestrator
- Type: REVIEW_REPORT
- Mode: F (Layers 1, 1b by evidence in 101 and orchestrator checks, 2)
- Artifacts reviewed: handoffs 100 and 101; `HeaderPolicy.java`, `HeaderPolicyTest.java`, `e2e/support/fixtures.ts`, `progress.md` line 174, `constitution.md` (version 1.4, security-header list line 199, changelog line 294), `security.md` (SC-10 row, SEC-F-130, SEC-F-139). Read from the working tree; no git access in this session, so the diff stat in 101 is taken as reported.

## Verdict: ACCEPT WITH FOLLOW-UPS

## Checks
1. Scope: ACCEPT. The files in 101 match brief 100 items 1 to 4 (plus the orchestrator commit 4c9a963). No other header, feature, dependency, `pom.xml` or `backend/config/**` change; `Map.of` still has 8 entries and the test keeps `hasSize(8)`.
2. Constitution 1.4: ACCEPT. Version is 1.4; line 199 lists the nine features camera, microphone, geolocation, payment, usb, serial, clipboard-read, display-capture, fullscreen; changelog line 294 states bluetooth removal only, "other features unchanged", cites SEC-F-130 and the Sponsor decision of 2026-11-05. Matches the decision.
3. Constitution vs code: ACCEPT. `HeaderPolicy.PERMISSIONS`, the constitution list, `PERMISSION_FEATURES` in fixtures.ts and the test list are the same nine features in the same order. `bluetooth` appears outside handoffs only in the test negative assertion and in historical or finding text (progress, status, changelog, SEC-F-130, SEC-F-139).
4. E2E guard: ACCEPT. `GUARDED_CONSOLE` no longer pairs with any tolerance constant; every console error or warning matching CSP, Trusted Type or Permissions-Policy is collected and asserted empty. No message is filtered. Comments updated.
5. Seeded control: ACCEPT. Real warning text `Unrecognized feature: 'bluetooth'` caught by the guard, then reverted with a clean `git status`. It is a stronger control than the earlier dummy-constant one, and closes the SEC-F-138 seeded wrong-header follow-up.
6. SC-10 row of security.md: no bluetooth left, consistent with the constitution.
7. progress.md line 174: ACCEPT. Matches brief item 4.
8. Repository hygiene: ACCEPT on the orchestrator's check (human identity, no attribution). Commit messages follow `type(scope): subject [Header-Fix]`.

## Follow-ups (non-blocking)
1. Weak oracle (MINOR): `HeaderPolicyTest` asserts `contains(...)` of each feature plus `doesNotContain("bluetooth")`. An extra feature added by mistake would still pass. The constitution says "at least", so this is spec-consistent; an exact-equality assertion on the whole value would be tighter. Suggest only if the Sponsor wants the value pinned.
2. Orchestrator, after the Security verdict: mark SEC-F-130 closed and SEC-F-139 closed (filter removed) in `security.md`; add SC-10 evidence (seeded control, handoff 101); close SEC-F-138's seeded control follow-up; update the status.md H1 row and remove the "Pending Sponsor: SEC-F-130" text on line 34.
3. Merge by pull request after the Sponsor approves at the gate; the commit titled with `[Header-Fix]` has no task number, so use the squash message `chore(security): drop bluetooth from Permissions-Policy [Header-Fix]`.
4. The delivery's "git diff --stat" lists 101 as "(this file)"; not a defect, noted only.

Security verdict: pending (`sdd-security-reviewer`); the Liaison does not substitute for it.
