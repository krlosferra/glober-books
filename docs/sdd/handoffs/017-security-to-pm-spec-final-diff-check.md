# Handoff 017: Security final diff check of spec v0.4 (round 3 of 3)
- From: security
- To: pm
- Phase: 2   Round: 3 of 3
- Artifacts: `docs/sdd/spec.md` v0.4 and `docs/sdd/requirements.md` v0.4 (branch `spec/phase-2-specify`, commit 76a1365); answers handoffs 014 and 015
- Type: AUDIT_REPORT

## 1. Scope and method

Diff check only, scope frozen. Read: spec Sections 1, 2 (US-007, US-009, US-010, US-011, US-014), BR-006, BR-011, BR-012, 5.0, 6, Appendix B, Appendix D (introduction, T-23 to T-30, C-11, E-15, E-24, E-24b, E-35, S-09), DEC-029 to DEC-032, `requirements.md` (REQ-026, cut paragraph), changelog. Checked the raw bytes of the Appendix D rows with `grep` and `cat -A` (read-only). I recomputed each T-24b and T-25 row against rules 6 to 6d by hand, including NFC composition. Not run: there is no code in this phase.

## 2. Findings

| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-060 (remaining part, D-1) | LOW | Test definition | T-27 row 1; T-30 row 2 | The fix claimed in handoff 015 is not in the file. T-27 row 1 contains the literal emoji character (bytes F0 9F 98 80) while the same cell says "written as the 12 bytes of two JSON escapes, not as the character"; T-30 row 2 is byte-identical to row 1 (`"title"` written plainly) while the cell says it is written with an escape. Almost certainly the escape sequences were decoded when the text was written. | `cat -A` of lines 661 and 664 of `spec.md`; `grep -c` for the surrogate escape text returns 0 in HEAD and in the working copy. The reversed pair row (`\uDE00\uD83D`) survived because it is not a valid pair. | An implementer reads the row, sends the character, and the escape-decoding path (surrogate pairing in the JSON parser, SC-31) and the duplicate-after-unescaping path are never tested. | Rewrite both cells without relying on escape sequences that can be decoded. T-27 row 1: "the title is the text backslash, u, D83D, backslash, u, DE00 (two six-character JSON escapes, 12 bytes, written verbatim in the JSON text) repeated 500 times". T-30 row 2: "the second member name is the text backslash, u, 0074 followed by itle (six characters for the first letter), so it equals `title` only after unescaping". Optionally add a Task 1 fixture note that the row bytes are checked with a hex dump. | SC-16, SC-31 |

No other defect found. Everything below is verification.

## 3. The three conditions

| Condition | Result | Evidence |
|---|---|---|
| SEC-F-057 (mark runs) | CLOSED at spec level | BR-006 6c: maximal sequence of Mn, Mc, Me, U+200C or U+200D; at most 8 code points; at most one variation selector, which counts toward the 8. 6d: joiner not first, last, next to another joiner of either kind, or next to Zs. T-24b rows re-derived: x + 8 U+0301 + U+200D + U+0301 is a run of 10 (400); the 50-segment Zalgo string is one run of 450 (400); a + U+FE0F + U+200D + U+FE0F has two variation selectors (400); a + U+0301 + U+FE0F + U+0301 + U+FE0F composes to U+00E1 and still leaves two variation selectors in one run (400); x + 7 U+0301 + U+200D + y is a run of 8 (201); U+2764 + U+FE0F + U+200D + U+1F525 is a run of 2 with one selector (201). Appendix B marks row, DEC-029 and REQ-026 carry the same run definition. Emoji keycap and ZWJ family sequences still pass (each run is at most 3). |
| SEC-F-058 (M-2, Appendix B, C-11) | CLOSED at spec level | M-2, Appendix B last row and C-11 are stated at 4,999 books of maximum size: (a) a non-matching add 201 within 2 s (library reaches 5,000); (b) delete that book, then a duplicate add 409 within 2 s at 4,999; (c) 4,949 books, 50 concurrent adds with 50 concurrent lists, all 201 and 200, 4,999 after, no 500, no refused connection, a failed request changes nothing. Counts are consistent. Memory bound for 50 x 15.5 MB is a Phase 3 plan item with the escalation rule recorded in handoff 015 section 5. |
| SEC-F-062 (cut list) | CLOSED at spec level | Section 1: step 1 and 2 list only measurements; E-22, stylesheet containment, the 2-second add bound with the C-11 assertions, the 300 ms parser bound and E-33 are in the never-cut list; E-22 does not appear in any cut step. US-014.6 last sentence: the stylesheet containment is checked by reading and is not cut. `requirements.md` cut paragraph and DEC-030 say the same. R-19 resolved. |

## 4. The smaller items

| Finding | Result |
|---|---|
| SEC-F-059 | CLOSED. Raw-socket list is the union I asked for; pre-application list adds the encoded `/` or `\`, the literal `\` and the leaving-root dot segment; E-15, E-24, E-24b carry the accepted-pre-application sentence; the envelope check is split from the pre-application answers. |
| SEC-F-060 | PARTLY CLOSED: S-09 initial state is in (`reading`). T-27 row 1 and T-30 row 2: see D-1. |
| SEC-F-061 | CLOSED. US-009.5 states the predicate (no other table, view, trigger or virtual table; formats and lengths; INV-1 to INV-4; 5,000) and the unchanged-bytes outcome; US-009.7 holds the claim for the whole run; US-009.3 orders the permission check first. |
| SEC-F-063 | CLOSED as R-18 (accepted by the Sponsor; Section 6 and DEC-031). Rule 6e not applied. |
| SEC-F-064 | CLOSED. BR-011 and US-007.2 carry the not-routable exception; dot segment defined with `%2e%2e%2e` as not a dot segment; Accept `q` grammar defined; I re-derived the eleven E-35 expectations against it (406, 200, 200, 406, 406, 406, 200, 406, 406, 200, 200) and they match; `charset` white space defined. |
| SEC-F-065 | CLOSED. Recorded in Section 6 (usability limits, U+1D159). |

## 5. Rulings on the open points

1. **The 2-second add bound (reconciliation 3).** I rule that it is a security bound and stays uncut. Reason: it is the only test that detects the algorithmic denial of service on the add path (key normalization of up to 4,999 maximum-size books under a single write connection, SEC-F-054, SEC-F-058); the pass or fail is a ceiling on a hostile input, not a layout or comfort figure. Honest limit: it is literally a timing figure, so the Sponsor's sentence "the cut list removes only measurements" can be read either way. The Sponsor already listed it among the never-cut items in the v0.4 changelog, but handoff 015 section 8 says the reading was not confirmed. Recommendation to the Sponsor: confirm in one line at the gate that the 2-second add bound of M-2 and C-11 is a security bound. If the Sponsor sides with the Liaison, the assertions that no response is `500` and that a failed request changes nothing stay uncut (they are in the never-cut list already), and the cost of the add path is then untested; that would be a new residual risk for the Sponsor to accept in writing. The 1 s and 300 ms figures are measurements and may be cut.
2. **The added literal backslash.** Confirmed, not struck. A stock container refuses a literal `\` in the request target as an invalid request line, so without the wording E-24b's last row would fail on a correct build. The allowance is bounded (status 400, no product, no version, no echo, the application keeps serving) and confined to container answers.

## 6. Hostile re-read fixes

- T-25 with U+200D next to U+200D: five failing rows, each hits a different clause of 6d (first, last, 200C+200D, 200D+200D, next to Zs); no bypass, no contradiction with T-24b.
- US-010.6 and US-010.4: the empty messages and the count are not shown in the load-failure state; consistent with US-010.7 (no list items, no insertion after a failed load). No contradiction found.
- BR-006 examples: failing and passing examples match 6c and 6d (the run of 8 passes; the run of 10 with a joiner fails; two selectors fail).
- No new security defect introduced by v0.4 beyond D-1 (a text defect, not a behavior defect).

## 7. Controls (spec level)

| Control | Status | Note |
|---|---|---|
| SC-16 | VERIFIED at spec level | Condition on SEC-F-060 text (D-1) for the T-27 and T-30 rows; implementation verification in Phase 4 |
| SC-17 | VERIFIED at spec level | Implementation in Phase 4 (including the memory bound) |
| SC-29 | VERIFIED at spec level | Implementation in Phase 4 |
| SC-30 | VERIFIED at spec level | Implementation in Phase 4 |

`docs/sdd/security.md` updated to 0.14: control statuses, findings SEC-F-057 to SEC-F-065, threat-model row, changelog. The Sponsor-approval column was not touched (the R-18 and R-19 entries were already filled by the orchestrator).

## 8. Not verified

No application exists yet, so nothing is verified at run time. Unicode category assignments and the NFC compositions in T-24b were derived by hand, not with a tool on the pinned JDK; the Task 1 fixture check (handoff 015 section 5) must confirm them. I did not check advisories or the registries.

## 9. Residual risks and Sponsor decisions

- R-16, R-17, R-18 accepted; R-19 resolved. No new risk.
- Sponsor decision requested: confirm the 2-second add bound as a security bound (ruling 1).

## 10. Verdict: PASS WITH CONDITIONS

No CRITICAL or HIGH finding is open. One LOW condition, to be closed before the Sponsor gate: apply the D-1 rewrite of T-27 row 1 and T-30 row 2 (two cells, no behavior change). Everything else is closed at spec level. Next step: PM applies D-1; Security confirms by reading the two cells (a grep for the literal emoji and for the plain duplicate in those rows); Liaison verdict; Sponsor gate.
