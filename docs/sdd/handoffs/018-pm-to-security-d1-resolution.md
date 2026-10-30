# Handoff 018: D-1 resolution (spec.md v0.4.1)
- From: pm
- To: security (copy to liaison)
- Phase: 2   Round: closing edit
- Artifacts: `docs/sdd/spec.md` v0.4.1, `docs/sdd/requirements.md`
- Type: REVIEW_REQUEST

## D-1 resolution

Defect D-1 (LOW, handoff 017): two Appendix D cells claimed a JSON escape sequence while holding the decoded character or an undistinguishable copy. Fixed in `docs/sdd/spec.md`:

1. Appendix D, row T-27, input 1: now "the title being the text backslash, u, D83D, backslash, u, DE00 (12 bytes written verbatim in the JSON text, not the decoded character), repeated 500 times". Expected result unchanged: 201 (title of 500 code points).
2. Appendix D, row T-30, input 2: now "a body of the same shape whose second member name is written as backslash, u, 0074 followed by the letters itle (so it equals title only after unescaping)". It differs from input 1 (literal duplicate names) and tests duplicate detection after unescaping. Expected result unchanged: 400 `MALFORMED_REQUEST`.

Byte-level check: after the edit, the non-ASCII characters left in `spec.md` are only intentional ones (full-width and accented letters in text-policy examples, Persian letters with a zero-width non-joiner in row T-25 and US text, em dashes, an ellipsis). No emoji remains. The remaining escape sequences (`\uD800`, `\u0000`, `\uDE00\uD83D`, `\uD83D`) are written as literal ASCII text. No other Appendix D cell claims an escape while holding the decoded character.

Other closing edits (no behavior change): Liaison O-2 (Section 1 and `requirements.md`: "Only the items above are cut, and no security item."); O-3 (Accept grammar: a `q` parameter without `=` is unparseable, treated as a malformed Accept, 406, which is what the existing `q` value rule already produced); header v0.4.1 with a changelog line.
