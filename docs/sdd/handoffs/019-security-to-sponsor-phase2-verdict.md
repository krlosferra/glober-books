# Handoff 019: Phase 2 security verdict (spec v0.4.1)
- From: security
- To: sponsor (copy to pm, liaison)
- Phase: 2
- Artifacts: `docs/sdd/spec.md` v0.4.1, `docs/sdd/handoffs/018-pm-to-security-d1-resolution.md`, `docs/sdd/security.md` 0.15
- Type: AUDIT_REPORT

## 1. Scope and method
Read handoff 018, the T-27 and T-30 cells, the Section 1 cut-list sentence and the Accept grammar; ran `git diff 76a1365 -- docs/sdd/spec.md` (read-only). Nothing was run; no application exists yet.

## 2. Checks
1. D-1 closed as prescribed. T-27 input 1 is described as backslash, u, D83D, backslash, u, DE00 (12 bytes verbatim, not the decoded character), 500 times; T-30 input 2 writes the second member name as backslash, u, 0074 plus "itle", so it differs from input 1 (literal duplicate) and tests duplicate detection after unescaping. Expected results unchanged (201; 400 `MALFORMED_REQUEST`). SEC-F-060 CLOSED.
2. The diff against 76a1365 touches 6 lines: version, Section 1 sentence, Accept parenthetical, T-27, T-30, changelog. No expected status or code changed anywhere.
3. O-2 ("Only the items above are cut, and no security item") narrows the cut list and strengthens the never-cut list. O-3 states that a `q` without `=` is unparseable, giving 406, which the existing `q` value rule already produced. No security-relevant change.

## 3. Controls
SC-16, SC-17, SC-29, SC-30: VERIFIED at spec level (SC-16 note removed). Implementation verification is Phase 4.

## 4. Conditions carried into Phase 3 and 4
- Task 1 fixture check on the pinned JDK of the hand-derived Unicode categories and NFC compositions in T-24b and T-25 (handoff 017 section 8).
- Phase 4 implementation verification of SC-16, SC-17 (including the memory bound), SC-29, SC-30, SC-31 and SC-13/SC-39 (data directory refusal rules, US-009.6, US-009.7).
- Phase 3 plan: raw-socket and pre-application answers of E-15, E-24, E-24b as specified (including the literal backslash allowance, confined to container answers); filter order and body cap; E-22 as a configuration assertion (5-second connection timeout); stylesheet containment rules.
- Phase 4: the 2 s add bound (M-2, C-11), the 300 ms parser-failure bound (T-31), the 2 s response of E-33, and the assertions that no response is `500` and that a failed request changed nothing.
- Dependencies, supply chain and advisories were not checked in Phase 2; due at Phase 3 and the pre-release full review.

## 5. Residual risks
| Risk | Status |
|---|---|
| R-16 (other local OS users reach the open API) | Accepted by the Sponsor 2026-10-30 (DEC-031) |
| R-17 (stored free text read by agents) | Accepted by the Sponsor 2026-10-30 |
| R-18 (hidden-data channel, about 250 bytes per field under BR-006 caps) | Accepted by the Sponsor 2026-10-30 |
| R-19 (cut list dropping security checks) | Not accepted as written; resolved by the v0.4 cut-list change, reinforced by O-2 in v0.4.1 |

No new risk.

## 6. Sponsor decision still needed
Confirm in one line at the gate that the 2-second add bound of M-2 and C-11 is a security bound and stays uncut (my ruling, handoff 017 section 5). It is the only test that detects algorithmic denial of service on the add path (SEC-F-054, SEC-F-058). If the Sponsor sides with the Liaison's reading, the cost of the add path is untested and that becomes a new residual risk to accept in writing.

## 7. Verdict: PASS (spec level)
No CRITICAL, HIGH or MEDIUM finding open; no condition remains for the spec itself. The items in section 4 are conditions for later phases, not blockers for the Phase 2 gate. Next step: Liaison verdict, Sponsor gate.
