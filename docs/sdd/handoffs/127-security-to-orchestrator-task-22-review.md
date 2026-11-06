# Handoff 127: Security review, Task 22 (duplicate key and service exceptions)
- From: security
- To: orchestrator
- Type: AUDIT_REPORT
- Branch: `feature/task-22-duplicates`, base 0d31888, head 811b60f

## 1. Scope and method
Read: handoffs 124 and 125, `DuplicateKey`, `ApiException`, `DuplicateBookException`, `DuplicateErrorBody`, the diff of `GlobalExceptionHandler`, `BookResponse`, `ErrorBody`. Scratch copy (`git archive HEAD`): compiled `DuplicateKey` alone and ran a probe harness (JDK 21); re-ran `DuplicateKeyTest` (16) and `GlobalExceptionHandlerTest` (16) offline, all pass. Did not run the full `verify` (reported green in 125).

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-109 | LOW | Error body typing | `DuplicateErrorBody` | CLOSED | record with `Details(BookResponse)`, no `Object`/map/tree; exact-string 409 test | none | none | SC-08 |
| SEC-F-162 | LOW | Duplicate key | `DuplicateKey` | Key is lower-casing, not folding: 101 BMP code points where key(upper) differs from key(lower) (U+00DF, U+0131, U+03C2 final sigma contextual, U+0345, U+1C80 to U+1C88, U+01F0) | probe over the BMP | Same Greek name typed with medial or final sigma is a different key: duplicate warning bypassed (no integrity loss) | C-1: record as spec-accepted (DEC-001, T-40 pins Straße/STRASSE) | SC-16 |
| SEC-F-163 | LOW | Coupling | Task 23a | Nothing in Task 22 forces stored (trimmed, NFC) input to `of` or the earliest match into the exception | code read | Wrong match or non-NFC input in 23a gives wrong existing_book or a bypass | C-2: Task 23a brief repeats the rule; test with two matches asserts the earliest in the 409 body | SC-16 |
| SEC-F-164 | INFO | Disclosure | 409 body | One full book disclosed to anyone guessing a key; no auth, loopback only | spec Appendix A | none new | none | SC-11 |
| SEC-F-165 | INFO | Duplicate key | key | Allowed joiners, U+034F, variation selectors are not stripped: "ab" vs "a"+U+200D+"b" differ | probe design | duplicate-warning bypass; already R-18 | none | SC-16 |

## 3. Controls checked
- DuplicateKey cannot throw or loop: single forward loop over code points, no regex, no recursion, no exception path. Probes: U+FDFA x500 gives 9,000 chars in 27 ms; U+33AF x500, U+FB03 x500, U+0130 x500, lone U+D800/U+DC00 x500, astral x500, 160,000-char mixed string 43 ms (linear); empty and blank input return empty. Lone surrogates pass unchanged (also rejected earlier by Task 21 and BR-006).
- Fast path vs general path: 128 x 128 x 6 strings (all ASCII pairs with separators incl. tab, CR, NUL, space), 0 divergences; code read agrees (ASCII is NFKC-invariant, `isWhiteSpace` shared, only A-Z lowered). Fast path cannot cause a false duplicate or a bypass.
- Locale.ROOT: default locale set to tr-TR and lt-LT, key unchanged (U+0130 gives i + U+0307, "TITLE I" gives "title i").
- Key properties over all non-surrogate code points: idempotent (0 failures), no double, leading or trailing space (0), NFD and NFKD of each give the same key (0 mismatches). Full-width and Straße/STRASSE behave as the spec states.
- No key or user text leaks: `toString` is `DuplicateKey[hidden]`; no logger or print call in the new classes (grep); `ApiException` message is the code name; `DuplicateBookException` stores only a `BookResponse` (transient), message is the code.
- 409 body: `DuplicateErrorBody(error, code, timestamp, details)` with `details` one member `existing_book`; no key, no count, no list. Exact JSON asserted by string equality (re-run, passes). `handleApi` returns the factory envelope without details; `handleDuplicate` is the more specific handler and wins in Spring resolution (tested). Status comes from `ErrorCode`, so no internal data in the mapping.
- Controls: SC-16, SC-08, SC-11, SC-14 updated in `security.md` (v0.45).

## 4. Not verified
Full `./mvnw clean verify`, JaCoCo and SpotBugs not run by Security. No integration probe of a live 409 (no controller or add path until Task 23a). Public advisory databases not queried (no dependency change). Final-sigma behaviour was checked by design reasoning and the case-gap probe, not by an end-to-end add.

## 5. Residual risks
None new for the Sponsor. SEC-F-162 and SEC-F-165 are duplicate-warning false negatives within R-18 and the spec; recommendation: accept as designed.

## 6. Verdict
PASS WITH CONDITIONS. No CRITICAL or HIGH open.
- C-1 (LOW, SEC-F-162): record the lower-casing (not folding) gaps as spec-accepted in progress.md or the Task 22 closure.
- C-2 (LOW, SEC-F-163): Task 23a brief and tests enforce stored NFC input and the earliest match.
SEC-F-109 closed. `security.md` is at 0.45; `status.md` has a new Task 22 row (IN_REVIEW, Security cell filled). Next step: Liaison audit, then Sponsor gate. Nothing was committed.
