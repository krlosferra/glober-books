# Handoff 115: Security review, Task 18+19 (text policy and Unicode fixture check)
- From: security
- To: orchestrator
- Type: AUDIT_REPORT
- Branch: feature/task-18-19-text-policy (f3919c1, 5f81d71)

## 1. Scope and method
Read: brief 112, delivery 113, `TextPolicy.java` in full, test file sizes and a grep of tests for `Thread.sleep`, `Pattern`, loggers, security.md SC-16 and the Unicode threat rows, R-18. Ran (read-only, scratch directory): the unmodified `TextPolicy.java` compiled with JDK 21.0.12.1 and driven by my own probe harness (worst-case inputs, bypass attempts, normalisation idempotence over every code point). Did not run `./mvnw clean verify` (offline; test counts and coverage are taken from delivery 113). `git diff b23924b..HEAD` touches no `pom.xml`, no `backend/config/**`; `TextPolicy` is referenced from no other main class yet.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-153 | INFO | DoS | `TextPolicy.check` (NFC before length) | NFC canonical reordering is super-linear in the JDK | Probe: 8,000 marks in descending combining class 166 ms; mixed order 54 ms; 5,000 "e"+U+0301 19 ms | Many requests with a worst-case 16 KiB title; bounded by the body cap and the drip-client acceptance R-24 | None required; optional timing row in Task 49 | SC-16, SC-06 |
| SEC-F-154 | INFO | Hygiene | `TextPolicy.isWhiteSpace` | A `Pattern` matcher on a one-code-point String per leading/trailing white-space code point | Code read; 16,000 leading spaces: 14 to 30 ms | None (linear, never applied to the text as a whole) | Optional: replace with a type-based check proven against `White_Space` | SC-16 |

## 3. Controls checked (SC-16, unit level)
- No regex over user text, single linear pass: VERIFIED. `charactersAllowed` is one loop over a code point array; the only `Pattern` is the white-space property on single code points (SEC-F-154). No backtracking construct exists. 16,000-space and 16,000-char inputs finish in under 40 ms.
- No logging of text: VERIFIED (no logger, `System.out` or exception message in the class or the tests; the enum carries no text).
- No mutation beyond trim and NFC: VERIFIED (`normalize` is trim then `Normalizer.NFC`; no replacement or stripping; idempotent for every non-surrogate code point between letters: 0 failures).
- Rejection set complete: VERIFIED by code and probe. `Cc` (including U+0085), `Cf` (all bidi controls U+061C, U+200E/F, U+202A to U+202E, U+2066 to U+2069, soft hyphen, U+2060, BOM, U+180E) except U+200C/U+200D, `Zl`, `Zp`, `Co`, `Cs`, `Cn` (Unicode 15 via the JDK), tags U+E0000 to U+E007F, noncharacters U+FDD0 to U+FDEF and every U+xxFFFE/FFFF (mask test is correct). Allowed by design and consistent with the spec and R-18: U+034F, U+17B4, U+180B, U+FFFC, U+FFFD, `Zs` inside text.
- Mark runs and joiners: VERIFIED. Run = Mn, Mc, Me, U+200C, U+200D; more than 8 or more than one variation selector (FE00 to FE0F, E0100 to E01EF) in a run rejected. "x" + 8 x U+0301 + U+200D + U+0301 rejected; 8 accepted; U+0301 first rejected; joiner first, last, next to a joiner or `Zs` rejected; "a" U+FE0F U+200D U+FE0F rejected. The run counter resets only on a non-mark, non-joiner, so joiners cannot restart it. Marks that NFC would compose are counted after NFC (length and runs judged on the stored form).
- Visible content: VERIFIED (blank-only U+3164, U+2800, U+115F, U+1160, U+FFA0 rejected; blank filler between letters accepted).
- Unpaired surrogates: VERIFIED. "ab" U+D800 "cd", lone low, trailing high, sole high all return FORBIDDEN_CHARACTERS without exception (trim and NFC tolerate them). The strict reader (Task 21) must still produce MALFORMED_REQUEST first.
- Huge input: bounded upstream (16 KiB cap in `RequestGateFilter`); within the cap the worst case measured is 166 ms (SEC-F-153).
- Fixture check ordering by `@BeforeAll` guard instead of an orderer: acceptable, a data mismatch fails every rule class.

## 4. Not verified
Full build, coverage figures and commit identity (author log shows the human identity, no trailers, in `git log main..HEAD`); wiring to the API (Task 20), request deserialisation (Task 21), duplicate key (comparison key NFKC, later task); homoglyph and mixed-script confusables are not in BR-006 (spec decision; outside this task); public advisory databases (no dependency change).

## 5. Residual risks
R-18 (hidden channel of about 250 bytes per field through the permitted marks and variation selectors) is unchanged and already accepted. No new risk for the Sponsor.

## 6. Verdict
PASS. No CRITICAL, HIGH or MEDIUM open. Carry to Task 20: call `check` on every title and author path and store `normalize` output; no user text in validation messages or logs; add one request-level test through the real port with the T-24b input. security.md bumped to 0.41; status.md row 18+19 added with the Security cell set (the State cell "IN_REVIEW" was a placeholder I had to create because no row existed; the PM or orchestrator owns it). Nothing committed.
