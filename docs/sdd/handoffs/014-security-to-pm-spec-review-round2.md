# Handoff 014: Security review of spec v0.3, Phase 2, round 2
- From: security
- To: pm
- Phase: 2   Round: 2 of 3
- Type: AUDIT_REPORT
- Artifacts reviewed: `docs/sdd/spec.md` v0.3 (all sections, Appendices A to D), `docs/sdd/requirements.md` v0.3 (cut list only), handoff 012 (read first), handoff 011 (my round 1), `constitution.md` v1.0 (Error Handling, API Standards, Security Standards), `security.md` v0.12. Repository `/home/user/glober-books`, branch `spec/phase-2-specify`, commit `5aac0a3`.

## 1. Scope and method
Design review only; no code exists. I re-read every cited location of SEC-F-043 to SEC-F-056 and re-derived the behavior, compared each landed test row with the text I proposed in handoff 011, and attacked BR-006, BR-011, BR-012, Section 5.0, 5.8, Appendix B and Appendix D as a hostile reader. Checks run with the system Python (`unicodedata`, Unicode 15.1, `python3 -I`): categories of U+3164, U+115F, U+1160, U+FFA0, U+2800, U+034F, U+17B4, U+17B5, U+180B to U+180F, U+16FE4, U+1D159, U+FFFC; NFC of "x"+U+0301 (stays 2 code points, so the PM's base "x" in T-24 is right) and of "e"+9 x U+0301 (9: 1 composed + 8 marks); NFC of 5 x U+0344 (10 marks: counting after NFC is the right order); NFC of the joiner-split string of SEC-F-057 (452 code points, 400 marks); a scan of all L/N/P/S code points whose NFKC is only space, format or marks (52, all "space + mark" compatibility forms such as U+00A8: harmless, the key is advisory); byte arithmetic for Appendix B and T-31 (8,000 nested `[` = 16,011 bytes; 2,000 `{"x":` = 12,000 bytes; 700 x 24 = 16,800).

Verdict summary: **PASS WITH CONDITIONS**. No CRITICAL, no HIGH. One MEDIUM (a real bypass of the mark cap that I accepted in round 1) and eight LOW/INFO findings. Thirteen of the fourteen round 1 findings are closed or partly closed at spec level; none closes by claim alone, each is closed by the text cited in section 3.

## 2. Findings (new in this round)

| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-057 | MEDIUM | Text policy | BR-006 6c with 6d; T-24; US-014.6; Appendix B | The run counted by 6c is "consecutive code points of category Mn, Mc or Me". U+200C and U+200D are category Cf, allowed by 6d, so they end the run. A joiner between marks restarts the count. | Python: `'x' + ('́'*8 + '‍')*50 + 'y'` has 452 code points after NFC, passes 6 to 6d as written (no run above 8, no joiner first, last, doubled or beside a space), and stacks 400 marks on one base. The same trick defeats "one variation selector in a row": `a FE0F ZWJ FE0F ZWJ ...` | (a) Zalgo stack of 400 marks over one base in a title paints over neighbouring rows and over "This cannot be undone." in the delete dialog (browsers keep marks after a joiner in the cluster, UAX #29 GB9; not exercised here). (b) one hidden byte per variation selector, 50 or more per field. T-24 tests only unbroken runs, and the only layout check (US-014.6 containment) is in cut step 1. | Replace 6c as in section 4.1; add T-24b; US-014.6 worst case unchanged (8 marks per base) | SC-16, SC-09, SC-22 |
| SEC-F-058 | LOW | Cost bound, tests | M-2; Appendix B last row; C-11; US-010.9 | M-2, Appendix B and C-11 put the worst-case add at "5,000 books of maximum size". At 5,000 books `LIBRARY_FULL` is answered before the duplicate check (BR-012 step 8, BR-009), so no key is normalized and the test passes trivially. The expensive path is an add at 4,999 books. The text came from my round 1 proposal; the flaw is mine. Also no memory expectation for 50 concurrent list responses of 15.5 MB. | BR-012 step 8 order `RATING_NOT_ALLOWED`, `LIBRARY_FULL`, `DUPLICATE_BOOK`; arithmetic 4,999 x 700 code points x NFKC | The 2-second bound and the "no 500 under flood" bound are unverified for the one path that costs; a pool wait of 5 s (Hikari) under 50 serialized slow adds ends in generic 500s; 50 x 15.5 MB of list output can exhaust the heap | Section 4.2 | SC-17, SC-06 |
| SEC-F-059 | LOW | Test executability (SC-29, SC-30, SC-40) | Appendix D header; E-15, E-24, E-25, E-18, E-34, E-38, T-39 | (a) The raw-socket list omits rows a normal HTTP client cannot send: E-15 (rows with `%2e`, `..%2f`, `;`), E-24, E-25 (`%zz`, `%`, `%00`), E-34 (two `Content-Type` headers), E-18, E-38 and T-39 (a body on GET or DELETE). (b) The container acts before the filter: Tomcat rejects an encoded `/` or `\` and a dot segment that leaves the root with its own 400, normalizes `//`, `.`, `..` and strips `;` path parameters before mapping. Rows that expect `404 NOT_FOUND` "each" (E-15, E-24) can fail on a correct build, and the pre-application list does not name these cases. | Framework behavior from knowledge, not tested | A correct build fails a test, or an implementer weakens the container setting to pass it; or the filter judges the normalized path and routes `/index.html;jsessionid=1` to the page | Section 4.3 | SC-29, SC-30, SC-40 |
| SEC-F-060 | LOW | Test text | T-27, T-30, S-09 | T-27 row 1 shows the emoji character, not "an escaped valid pair": the 12-byte `😀` form (6,000 bytes for 500) is not exercised. T-30 lists `{"title":"a","title":"b","author":"c"}` twice; the case I proposed, a duplicate that exists only after unescaping, was lost. S-09 gives no initial state. | file bytes of line 656 (`M-pM-^_M-^XM-^@`); lines 659 | The escape decoder and the duplicate check after unescaping go untested | Section 4.4 | SC-16, SC-31 |
| SEC-F-061 | LOW | Start-up trust | US-009.5, US-009.6, US-009.7 | (a) "may contain no other tables" lets a foreign file bring views, triggers and virtual tables. (b) The predicate checks INV-1 to INV-4 and the count but not lengths and formats, yet the response bound (3,100 bytes a book, 15.5 MB) depends on them. (c) US-009.7 does not say the claim lasts for the whole run; a write lock held only inside transactions lets a second instance start between them. (d) US-009.6 is a check then open; the plan must not leave a replace window. (e) Appendix D has no rows for US-009.5 to 009.7 except C-12. | spec text; SC-32, SC-39 | Another local user who can write the project directory swaps `data/` after the check; a second instance starts and both write | Section 4.5 | SC-13, SC-32, SC-39 |
| SEC-F-062 | LOW | Cut list | Section 1 "Delivery priorities"; DEC-030; `requirements.md` line 74 | Step 1 removes three security-relevant checks and the text says "the security criteria ... are never cut": E-22 (the only behavioral test of the 5-second connection timeout, SC-06), the containment clause of US-014.6 (the only check of the layout-overlay defense of SEC-F-044), and the 2-second add bound of M-2 (SEC-F-054; C-11 itself is not cut, its timing line is). Step 2 (US-014.3 contrast, US-014.5 reduced motion) removes nothing security-relevant. | spec text | A schedule overrun silently drops the slow-request protection and the overlay check | Section 4.6; ruling on PM point 4 in section 5 | SC-06, SC-16, SC-17 |
| SEC-F-063 | LOW | Hidden channel (residual) | BR-006 6c | The caps reduce the hidden capacity, they do not remove it. Default-ignorable marks outside the variation selectors (U+034F, U+17B4, U+17B5, U+180B to U+180D, U+180F, U+16FE4) form an alphabet; with 8-code-point runs and one variation selector per run, a 500-code-point field can carry about 250 bytes (one selector byte per visible carrier, plus run patterns). Joiners add one bit per gap. This is smaller than the uncapped case (500 bytes plus tags) and needs an explicit decoder instruction to be read by an agent, but it is not zero. | arithmetic; categories checked in Python | Hidden instruction for an agent that reads stored text (R-17 class; agents explore only the temporary database, SC-23) | Record as R-18 (Sponsor) or apply the optional rule 6e of section 4.7 | SC-16, SC-22 |
| SEC-F-064 | LOW | Wording | BR-011, US-007.2, 5.0 Routing, 5.0 Accept, 5.0 Content type | (a) BR-011 and US-007.2 say that an id containing `%` is malformed (400) without the exception of 5.0 (encoded `/`, `\`, `;` and dot segments are 404 in every position): `/api/v1/books/%2f` reads 400 in one place and 404 in the other (5.0 says "other single segment", which gives 404, but a reader of BR-011 alone will not see it). (b) "dot segment (literal or encoded)" is undefined for `.%2e`, `%2e%2e%2e`. (c) The `Accept` q grammar is open (`q=1.5`, `q=`); and `application/json;q=0, */*` is 200 here while RFC 9110 specificity gives 406 (a stated deviation is enough). (d) `charset` with white space around `=` or after the value is not decided. | spec text | Two implementations answer differently; a test has no expected value | Section 4.8 | SC-40, SC-29 |
| SEC-F-065 | INFO | Usability and advisory limits | BR-006 6d, 6b, BR-003 | (a) Rule 6d rejects legacy Malayalam chillu written as consonant + virama + U+200D at the end of a word or before a space (for example the end of "അവന്‍") and a Persian U+200C typed before a space; the atomic chillu code points (Unicode 5.1) are accepted. (b) U+1D159 (symbol, no glyph in most fonts) counts as visible content. (c) The duplicate key is not stripped of joiners, fillers and ignorable marks, so an interior U+200C or U+3164 makes "Clean Code" a different key; the warning is advisory. | Python categories; Unicode text | none (not exploitable) | Add one line to Section 6 "Accepted limitations" (text in section 4.9) | SC-16 |

## 3. Verification of round 1 findings (re-read, re-derived)

| Finding | Result | Evidence in v0.3 |
|---|---|---|
| SEC-F-043 | CLOSED | BR-006 6b (categories L, N, P, S minus the five blank-rendering code points; no leading Mn, Mc, Me); T-23 has U+3164 only, U+2800 only, U+0301 only, U+0301 + "abc", U+FE0F only (400 `VALIDATION_ERROR`, field `title`) and the accepted row "ab" U+3164 "cd" (201). I checked the default-ignorable set: every Cf member is rejected by 6, the Lo members are the four excluded, the Mn members are held by 6b/6c (but see SEC-F-063). Not weakened. |
| SEC-F-044 | PARTLY CLOSED | Cap 8, one variation selector, no leading mark and the containment criterion are present, T-24 has the four rows with exact expectations (400, 201, 400, 201). The cap is bypassable (SEC-F-057); the containment check is cuttable (SEC-F-062). |
| SEC-F-045 | CLOSED | Rule 6d, DEC-029, T-25 (Persian word 201; first, last, ZWNJ+ZWJ, next to a space: 400). Definable and testable (section 5, ruling 1). |
| SEC-F-046 | CLOSED (round 1) | BR-006 item 2 uses White_Space; plan note in handoff 012 section 7. |
| SEC-F-047 | CLOSED at spec level | 5.0 Strict parsing states: every parser, limit or decoder failure is `MALFORMED_REQUEST`, never `INTERNAL_ERROR`, within 300 ms; invalid constructs listed; names compared after unescaping; 100-character number rule. T-22 to T-33 landed with exact status and code. Text regressions in T-27 and T-30 (SEC-F-060). Corrections verified: T-24 base "x"; T-31 sizes (8,000 nested `[` inside `{"rating":...}` is 16,011 bytes, fits; 2,000 `{"x":` plus closers is 12,000 bytes); the three number rows `INVALID_RATING` (a plan hazard remains: `1e999999999` must not be converted to an integer; the 300 ms bound in T-31 catches it). |
| SEC-F-048 | CLOSED at spec level | 5.0 Routing and Query strings; BR-011; BR-012 steps 2 to 4; E-24 to E-27, E-34. Wording refinements: SEC-F-064. |
| SEC-F-049 | CLOSED at spec level | 5.0 Methods (HEAD, OPTIONS, TRACE, CONNECT, unknown and lower-case names to 405 with exact `Allow` lists), 5.8 (no `ETag`, `Last-Modified`, `Accept-Ranges`; `Range` and conditional headers ignored), 5.0 (no 101, 206, 304, 416; `Expect`); E-28 to E-31 with exact expectations. |
| SEC-F-050 | CLOSED at spec level | Overview states R-16 (DEC-031); US-009.6 as I proposed (symlink, foreign owner, group/other access refused; 0700/0600 creation; Windows limit stated). Refinements: SEC-F-061. |
| SEC-F-051 | CLOSED at spec level | BR-013 (race sentences, "any other writer is outside this specification"), INV-1/INV-4 "in every final state", S-08 (re-derived: both orders end `reading`, null, null; the rating call is 200 or `RATING_NOT_ALLOWED`), S-09, S-10, C-09 (4,990 + 20 gives exactly 10 and 10), C-10 (1 + 19, then 20 more gives 21 books), C-12, C-13. Plan items recorded (section 7 below). |
| SEC-F-052 | PARTLY CLOSED | E-23 asserts the constitution headers (list equals constitution line 197: CSP "as specified", nosniff, X-Frame-Options DENY, no-referrer, COOP, CORP, Permissions-Policy, no-store); pre-application defined; E-10 split (the one-header comma case is exactly `INVALID_HOST`); E-20 and E-36 allow 400, 414, 431; E-21 reworded; E-24 to E-37 landed. Gaps: SEC-F-059. |
| SEC-F-053 | CLOSED | Appendix B body row re-derived: 700 x 12 = 8,400 (typical under 9 KiB); 500 x 3 x 6 = 9,000 and about 12.7 KB with a 200-letter author; U+1F82 4 x 6 = 24 bytes, 700 x 24 = 16,800 bytes, 413; padding to 16,384 accepted. |
| SEC-F-054 | PARTLY CLOSED | M-2 qualification, C-05 time recorded, US-010.9 measured limit present; the tests are at the wrong book count (SEC-F-058). |
| SEC-F-055 | CLOSED at spec level | US-011.4 (any further submission by any means), US-014.6 (500-code-point unbroken word, worst-case marks, containment), US-014.8 (no address, `window.name`, referrer, `postMessage`; no storage), E-38 (query on `/` is 400). |
| SEC-F-056 | CLOSED | Section 6 accepted limitations, DEC-031, BR-003 definition, DEC-001. |

## 4. Required spec changes (exact text)

### 4.1 SEC-F-057 (condition)
Replace BR-006 item 6c with:
"6c. marks: a mark run is a maximal sequence of consecutive code points each of which is of category Mn, Mc or Me, or is U+200C or U+200D. A mark run has at most 8 code points and contains at most one variation selector (U+FE00 to U+FE0F, U+E0100 to U+E01EF)."
Also replace "at most 8 combining marks in a row; one variation selector in a row" in Appendix B (row "Combining marks") with "a run of marks and joiners has at most 8 code points and at most one variation selector (BR-006 item 6c)", and in US-001.6 and DEC-029 keep the wording "too many combining marks".
Add to Appendix D:
| T-24b | POST title: "x" + 8 x U+0301 + U+200D + U+0301 (run of 10); "x" + (8 x U+0301 + U+200D) x 50 + "y"; "a" + U+FE0F + U+200D + U+FE0F; "a" + U+0301 + U+FE0F + U+0301 + U+FE0F; then, accepted: "x" + 7 x U+0301 + U+200D + "y" (run of 8); U+2764 + U+FE0F + U+200D + U+1F525 | 400 `VALIDATION_ERROR` field `title` (the first four); 201 (the last two) |
Check on legitimate use, so the rule does not break real text: emoji sequences have runs of 1 to 2 (FE0F, ZWJ); Indic virama + ZWJ/ZWNJ runs are 2 to 4; Persian with vocalization and ZWNJ is under 6; Hebrew with cantillation and points under 6; Tibetan stacks under 8.

### 4.2 SEC-F-058 (condition)
- M-2: replace "with 5,000 books of maximum size (title 500 and author 200 code points) an add completes within 2 seconds and never returns `500`" with "with 4,999 books of maximum size (title 500 and author 200 code points) an add of a book that matches none completes within 2 seconds and an add that duplicates a stored book is answered `409 DUPLICATE_BOOK` within 2 seconds; neither returns `500`". Same change in the last row of Appendix B.
- Replace C-11 with: "| C-11 | 4,999 books of maximum size: (a) one POST of a book that matches none; (b) DELETE of that book, then one POST that duplicates a stored book without `allow_duplicate`; then 4,949 books of maximum size: (c) 50 concurrent POSTs of different valid books together with 50 concurrent list requests | (a) 201 within 2 s; (b) 200, then 409 `DUPLICATE_BOOK` within 2 s; (c) all 50 POSTs 201 and all 50 lists 200 (4,999 books after), no response with an undocumented status, none a 500 or a refused connection, a failed request changed nothing, the process is still running and serves normally afterwards |". If the plan cannot meet (c), the PM escalates to the Sponsor; the test is not weakened and no 503 is introduced (not in the constitution's list).
- Plan note: bound the heap or stream the list response so that 50 concurrent responses of up to 15.5 MB do not exhaust memory.

### 4.3 SEC-F-059
- Appendix D header, raw-socket list: add "E-15 (rows with `%2e`, `%2f`, `;`), E-18, E-24, E-25, E-34 (two `Content-Type` headers), E-38 (body on GET), T-39 (body on GET and DELETE)".
- Pre-application list (D header): add "an encoded `/` or `\` and a dot segment that leaves the root, where the server rejects it first".
- E-15 and E-24: append "A pre-application answer (status 400, no product, no version, no echo) is accepted for a row that contains an encoded `/` or `\`, `%00`, or a dot segment that leaves the root."
- Plan note for Phase 3 (not spec): the filter judges the raw request target (not the container's normalized path): `//`, `.`, `..` and `;parameters` are normalized or stripped by the container before mapping, so E-01 (`;x=1`), E-15 (`/index.html;jsessionid=1`), E-24 and E-27 are tests of that choice.

### 4.4 SEC-F-060
- T-27 row 1: "POST with the title `😀` written as the 12 bytes of two JSON escapes (not as the character) 500 times".
- T-30: second row `{"title":"a","title":"b","author":"c"}` (the duplicate exists only after unescaping); keep the allow_duplicate row.
- S-09: "a book with status `reading`" as the initial state.

### 4.5 SEC-F-061
- US-009.5, replace "contains the Book structure of Section 4 (and may contain no other tables), and its rows satisfy INV-1 to INV-4 and the 5,000 limit" with "contains the Book structure of Section 4 and no other table, view, trigger or virtual table, and its rows satisfy the formats and limits of Section 4 (id, timestamp format, status, rating, title and author length counted in code points), INV-1 to INV-4 and the 5,000 limit".
- US-009.7, append: "The first instance holds its claim on the file from before its first query until it stops, not only during a transaction."
- Phase 3 test list (record in the handoff to the Liaison): symlinked directory, symlinked file, file mode 0644, directory mode 0770, foreign owner (skip where the test cannot change ownership, stated), missing path creates 0700/0600, each failure leaves the bytes of an existing file unchanged (hash before and after), view/trigger present, row with a 600-character title.
- Plan notes: open without following links and check the opened descriptor, not the path (no replace window); the claim of US-009.7 is `locking_mode=EXCLUSIVE` or an equivalent file lock; `BEGIN IMMEDIATE`, CHECK constraints, capacity trigger as already recorded.

### 4.6 SEC-F-062 (condition)
- Section 1 "Delivery priorities", step (1): replace "E-22, and the visual layout checks (the 320-pixel clause of US-014.4 and the containment clause of US-014.6)" with "and the visual layout measurements (the 320-pixel clause of US-014.4 and the bounding-box measurement of US-014.6)". Delete "E-22" from step 1 and from `requirements.md` line 74. Add the sentence: "The slow-request protection (E-22), the containment rules of the stylesheet and the 2-second bound of C-11 are not cut." If the Sponsor prefers to keep E-22 cuttable, add instead: "If E-22 is cut, a configuration test asserting the 5-second connection timeout takes its place and the cut is reported open."
- US-014.6, append: "The stylesheet clips or wraps overflowing text in the list item and in each dialog (long words broken, content clipped to the box); this is checked by reading the stylesheet and is not part of the cut."
- State in Section 1 that the cut items of step 1 are security-relevant to the extent above, so the Sponsor sees the effect.

### 4.7 SEC-F-063 (decision for the Sponsor)
Recommended: accept as R-18 (proposed in `security.md`, Sponsor column pending); no spec change except the Section 6 line "Hidden channels are cut by BR-006" becoming "Hidden channels are reduced, not removed, by BR-006 (R-18)".
Optional hardening if the Sponsor wants to cut it further: "6e. a variation selector U+E0100 to U+E01EF must directly follow a code point with the Unicode property Ideographic; U+FE00 to U+FE0F must directly follow a code point of category So, Sk, Sm, N or P; the code points U+034F, U+17B4, U+17B5, U+180B to U+180D, U+180F and U+16FE4 are not allowed." Cost: rejects variation sequences on other bases and Mongolian free variation selectors; the capacity falls to about one bit per carrier. I do not require it.

### 4.8 SEC-F-064
(a) BR-011 and US-007.2: after "any text containing `%`" add "(except the segments that 5.0 declares not routable, which are `404 NOT_FOUND`)".
(b) 5.0 Routing: after "a dot segment (literal or encoded)" add "(a segment that equals `.` or `..` after percent-decoding; `%2e%2e%2e` is not one and, in the id position, is `400`)".
(c) 5.0 Accept: add "A `q` value is `0` or `0.` followed by up to three digits, or `1` or `1.` followed by up to three zeros; any other `q` makes the header unparseable (`406`). Range exclusion is by the listed ranges only: `application/json;q=0, */*` is acceptable (a stated deviation from RFC 9110 specificity)."
(d) 5.0 Content type: add "white space is allowed around `;` and after the value, not around `=`".

### 4.9 SEC-F-065
Section 6 Accepted limitations, add: "Rule 6d rejects text that ends a word with U+200D or U+200C before a space or at the end (for example Malayalam written with the legacy chillu sequence; the atomic code points are accepted) and the duplicate key does not ignore joiners, filler characters or ignorable marks, so such text can bypass the advisory duplicate warning."

## 5. Rulings

**Ruling 1, ZWNJ and ZWJ under the positional rule: accepted.** The rule is precisely definable (first and last refer to the text after trimming and NFC; adjacent means the previous or next code point; Zs is a general category), testable (T-25, T-24b) and safe with the SEC-F-057 fix. Checks: the joiners are Cf and never count as visible content, so they cannot rescue a blank title; NFC and NFKC do not compose or remove them; trimming does not remove them (not White_Space), so " ZWJ a" is judged after the trim and fails; the rule runs after NFC and the counts are those of the stored text. Scripts: Persian, Urdu, Kurdish, Sinhala, Bengali, Devanagari and emoji sequences are inside the rule; Malayalam legacy chillu at a word end is not (SEC-F-065). Bypasses that remain: a bit per gap (SEC-F-063), and the advisory duplicate key (SEC-F-065).
**Tag characters rejected: accepted** (also rejected as Cf; limitation stated in Section 6).
**Combining mark caps: accepted after SEC-F-057.** NFC is applied before counting and the stored text is NFC, so the count equals what is rendered; NFC can only shorten runs (composition) or lengthen them (U+0344 gives two marks), and both are counted correctly. Hangul V and T jamo are category Lo and Hebrew/Arabic/Thai marks are Mn: the first are not capped (rendering is font dependent and was not exercised), which is why the stylesheet containment must not be cut (SEC-F-062).
**R-16 and R-17 stated: accepted.** R-18 and R-19 proposed in `security.md`.

**Reconciliations (section 4 of handoff 012):**
- R-1 encoded dot segment 404: accepted, secure and deterministic once SEC-F-064 (a) and (b) are applied. Both 400 and 404 fail closed.
- R-2 malformed percent-encoding by position: accepted (id position and query: 400; elsewhere the segment cannot equal a fixed segment: 404).
- R-3 query or body on the static page is 400: accepted. One rule for the application, no reflected-parameter surface; the cost (`/?utm=x` gives a JSON 400) should stay in Section 6 as an accepted limitation; E-38 proves it.
- R-4 quoted `charset` is 415: accepted (smaller surface).
- R-5 `q=0` excludes: accepted with SEC-F-064 (c); E-35 rows re-derived: 406, 200, 200, 406, 406, 406, 200.
- R-6 numbering: accepted; Appendix C columns match the rows.
- R-7 Appendix B: accepted (arithmetic verified).
- R-8 statuses 414, 501, 505: see ruling on point 1 below.
- R-9, R-10: accepted.
- Four corrections to my test text: T-24 base "x": correct (NFC of "x"+U+0301 stays two code points). T-31 nesting sizes: correct (48,000 bytes would not fit; 2,000 levels is 12,000 bytes). T-31 number rows `INVALID_RATING` and nested rows `MALFORMED_REQUEST`: correct. Confirmation texts "Book added: <title>" and "Book deleted: <title>": no security effect (text content only).

**PM point 1 (414, 501, 505): no constitution amendment is required.** The constitution's status list (Error Handling, "Status codes: 200 ... 500") governs responses the application produces. Its Error Handling text and the signed R-13 cover "protocol-level errors that Tomcat answers before any application code runs", with the oversized header and the invalid request line as examples, and require only a generic body, no version, and allow missing security headers. R-13 does not enumerate statuses, and it does not name 400 or 431 (I searched `constitution.md` and `security.md`: the number 431 is not in either). 414 (request line), 431 (header), 501 (method) and 505 (version) are the status codes HTTP assigns to those same protocol errors, so they are instances of R-13, not new classes. The spec text is sufficient: Appendix A note and the D header limit them to pre-application answers with no product, no version and no echo. Optional, for clarity only (the Sponsor decides; it is not a condition): add after the status list of the constitution Error Handling: "Protocol-level answers given by the container before the application runs (R-13) may use 400, 414, 431, 501 or 505; they carry no product, no version and no echo of the request."

**PM point 2 (quoted charset 415, q=0 excludes): confirmed.**
**PM point 3 (query and body on static GET are 400): confirmed.**
**PM point 4 (cut list): the US-014.6 containment clause as a visual check is acceptable only as split in 4.6.** The bounding-box measurement is cuttable; the stylesheet containment is not. The inertness, console-CSP and no-address-data clauses stay uncut, as the PM says. With SEC-F-057 fixed, the worst case is bounded at 8 marks per base, which makes an uncut stylesheet a sufficient defense and the measurement a confirmation. The cut list also removes E-22 and the 2-second bound (SEC-F-062); explicit answer to "does cutting step 1 or 2 remove a security check": step 1 yes (E-22, containment measurement, 2 s add bound), step 2 no.
**PM point 5 (DEC-030 promotion): no security objection.**

**Other checks of section 4 of the request:**
- Contradictions among BR-006, BR-011, BR-012, 5.0, Appendix D: one found that matters (BR-011/US-007.2 versus 5.0, SEC-F-064 (a)); the rest agree. Order of checks leaks nothing: Host and Origin precede routing for every path, so an unknown path with a bad Host is `INVALID_HOST`; an invalid body for an unknown id reports the body error (ruling 9 of round 1 stands); `Accept` is judged after the route, so it reveals no path.
- Error messages echo nothing: Appendix A fixed list, T-13, 5.0 "Messages never echo input"; the page shows the response `message` only from that fixed list (US-011.6).
- Response size and flood at capacity: bound 15.5 MB, UTF-8 not escapes (C-05); flood expectations are right but at the wrong book count (SEC-F-058).
- `requirements.md` carries the same cut list text as the spec (line 74), so 4.6 changes both.

## 6. Controls checked (spec level; none is VERIFIED before the conditions close)

| Control | Result | Evidence |
|---|---|---|
| SC-16 | IN_PROGRESS | BR-006 6 to 6d, T-01 to T-26, T-40; open SEC-F-057 (condition), SEC-F-060, SEC-F-063 |
| SC-17 | IN_PROGRESS | Appendix B, BR-009, C-01 to C-13; open SEC-F-058 (condition), SEC-F-062 |
| SC-29 | IN_PROGRESS | D.3 E-01 to E-39, E-23 headers; open SEC-F-059, SEC-F-064 |
| SC-30 | IN_PROGRESS | D header raw-socket list; incomplete (SEC-F-059) |
| SC-31 | IN_PROGRESS | 5.0 Strict parsing, T-16 to T-33; open SEC-F-060 |
| SC-40 | IN_PROGRESS | 5.0 Routing, E-01, E-15, E-24 to E-27; open SEC-F-059, SEC-F-064 |
| SC-06 | spec consistent; E-22 is cuttable (SEC-F-062) | 5.0, E-09, E-22, E-32, E-33 |
| SC-13, SC-32, SC-39 | spec consistent | US-009.5 to 009.7; refinements SEC-F-061 |
| SC-18, SC-41 | VERIFIED at spec level | 5.0: no redirects, cookies, CORS, 101/206/304/416; `Allow` lists; E-28 to E-31 |
| SC-03, SC-04, SC-05, SC-28 | spec consistent | BR-012 step 1; E-04 to E-14 |
| SC-09, SC-10, SC-38 | spec consistent | US-010.8, US-014.6 to 014.8 |
| SC-37 | spec consistent | T-33 |

I could not set SC-16, SC-17, SC-29, SC-30 to a spec-level VERIFIED; the reasons are the findings above, and each is a small text change.

## 7. Plan notes for Phase 3 (kept out of the spec)
- Trim and classify with a code-point loop over `White_Space` and `Character.getType`, not `strip()`; validate T-03, T-23 to T-26, T-24b fixtures on the pinned JDK (Unicode 15.0).
- Judge the raw request target in the filter; the container normalizes `//`, dot segments and `;` parameters first (SEC-F-059).
- Never convert a huge-exponent number to an integer or `BigInteger` (T-31); the 300 ms bound is the guard.
- Exclusive claim on the database for the whole run, immediate write lock, CHECK constraints (status values, rating 1 to 5, rating only when completed, `date_completed` iff completed), capacity trigger, `trusted_schema` off, open without following links (SEC-F-051, SEC-F-061).
- `max-swallow-size` low (64 KiB) for E-33; heap bound or streaming for the 15.5 MB list.

## 8. Not verified
- No code exists: Tomcat/Spring behavior (container 400 for encoded slash, normalization of `//` and `;`, `TRACE`, `Range`, status for over-long request line, Jackson limit key names, handling of huge exponents) is from knowledge of the frameworks, not from a test. Phase 4 tests must confirm each.
- Browser behavior: stacking of marks after a joiner, rendering of Hangul V/T jamo runs, clipping, dialog focus, `Sec-Fetch-Site` on links were not exercised.
- Unicode categories were checked with Unicode 15.1 tables, not the JDK 21 (15.0) tables; the classes cited are stable between the two. Fixtures must be validated on the pinned JDK in Task 1.
- Public advisory databases are unreachable from this environment (R-11 unchanged).
- Traceability (`requirements.md`, Appendix C) is the Liaison's scope; I read only the cut list.

## 9. Residual risks for the Sponsor (recommendation)
- R-18 (new, proposed): hidden-data capacity of about 250 bytes a field remains under the caps. Recommend accept with R-17, or apply optional rule 6e.
- R-19 (new, proposed): the cut list may drop security checks. Recommend: do not accept as written; apply 4.6.
- Accepted limits to add to Section 6 (SEC-F-065): Malayalam legacy chillu and ZWNJ before a space; duplicate key not ignoring joiners and fillers. Recommend accept.
- Decisions for the Sponsor at the gate: (1) E-22 uncut with the stylesheet containment kept out of the cut (my recommendation) or E-22 replaced by a configuration test; (2) accept R-18 or apply 6e; (3) optional constitution sentence on pre-application statuses (not needed).
- Existing R-01, R-06, R-13, R-16, R-17 unchanged. The Sponsor-approval column of the register was not touched.

## 10. Verdict
**PASS WITH CONDITIONS** (no CRITICAL, no HIGH).
Conditions to close in round 3 (each with evidence in v0.4):
1. SEC-F-057: BR-006 6c as in 4.1 and T-24b (MEDIUM).
2. SEC-F-058: M-2, Appendix B and C-11 as in 4.2.
3. SEC-F-062: cut list as in 4.6 (spec Section 1, US-014.6, `requirements.md`).
Expected in round 3, not blocking: SEC-F-059, SEC-F-060, SEC-F-061, SEC-F-064 (small text edits, 4.3, 4.4, 4.5, 4.8), SEC-F-063 and SEC-F-065 (record, 4.7 and 4.9).
Next step: PM issues v0.4; Security re-verifies the listed locations in round 3 and then can set SC-16, SC-17, SC-29, SC-30 to a spec-level VERIFIED for the Phase 2 gate. The `Security` column of `status.md` was not edited (outside the files I was allowed to change in this task); the orchestrator should record PASS WITH CONDITIONS, round 2.
