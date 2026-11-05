# Handoff 101: Delivery, header value fix (SEC-F-130, constitution 1.4)
- From: java
- To: liaison
- Type: TASK_DELIVERY
- Branch: chore/permissions-policy-bluetooth (local commits only, not pushed)
- Brief: handoff 100

## Resume point
- Done: all steps (backend header and test, e2e fixture, progress note, seeded control, delivery).
- Next: Liaison audit and Security review. Nothing pending for java.

## Files changed by this work
- `backend/src/main/java/com/globant/globerbooks/config/transport/HeaderPolicy.java` (bluetooth=() removed from the value)
- `backend/src/test/java/com/globant/globerbooks/config/transport/HeaderPolicyTest.java` (expected list per constitution 1.4, plus `doesNotContain("bluetooth")`)
- `e2e/support/fixtures.ts` (SEC-F-130 constant and its use removed; bluetooth removed from required features)
- `docs/sdd/progress.md` (SEC-F-130 line replaced)
- this delivery. Nothing else changed (no other header, dependency, `backend/config/**`, `pom.xml`).

## Evidence
1. `cd backend && ./mvnw -q clean verify`: green. Surefire: 376 tests, 0 failures, 0 errors; JaCoCo line coverage 95.8% (gate 80%).
2. `grep -rn "bluetooth\|SEC_F" backend/src e2e/support e2e/tests`: only
   `backend/src/test/.../HeaderPolicyTest.java:125: .doesNotContain("bluetooth");`
3. `npx playwright test --grep @smoke` on port 18080 (jar built with `-DskipTests package`): 3 passed, no Permissions-Policy console message (the guard now tolerates none).
4. Seeded control: `bluetooth=()` put back in `HeaderPolicy.java`, jar rebuilt, test "US-010.1 AC-1" ran: FAILED in the guard with
   `CSP, Trusted Types or Permissions-Policy console messages` received `["Error with Permissions-Policy header: Unrecognized feature: 'bluetooth'."]`.
   Reverted with `git checkout -- <file>`; `git status --short` empty; jar rebuilt from the reverted source. This closes the SEC-F-138 seeded wrong-header control.
   No application process left running (`pgrep -f globerbooks.jar` = 0).

## git diff --stat main..HEAD
```
 .../globerbooks/config/transport/HeaderPolicy.java  |  2 +-
 .../config/transport/HeaderPolicyTest.java          |  3 ++-
 docs/sdd/constitution.md                            |  5 +++--
 ...0-orchestrator-to-java-bluetooth-header-brief.md | 21 +++++++++++++++++++++
 .../101-java-to-liaison-header-fix-delivery.md      | (this file)
 docs/sdd/progress.md                                |  2 +-
 docs/sdd/security.md                                |  2 +-
 docs/sdd/status.md                                  |  3 ++-
 e2e/support/fixtures.ts                             |  9 ++-------
```
(constitution, security, status and brief come from the earlier orchestrator commit 4c9a963.)

## git log main..HEAD
```
49f1f43 docs(progress): record SEC-F-130 header correction [Header-Fix]
1295220 test(e2e): remove SEC-F-130 console filter and bluetooth requirement [Header-Fix]
a315a28 fix(security): drop bluetooth from Permissions-Policy value [Header-Fix]
4c9a963 docs(sdd): amend the Permissions-Policy header list, constitution 1.4, and add the header fix brief [Header-Fix]
```
Author of all four: krlosferra <carlos.ferra@globant.com>; messages contain no attribution.

## Deviations, risks
None. The e2e edit is the one authorised by the brief.
