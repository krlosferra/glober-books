# Handoff 107: Security review, Task 16
- From: security
- To: orchestrator
- Type: AUDIT_REPORT
- Branch: feature/task-16-query-id-limits

## 1. Scope and method
Read: handoff 105, diff main..HEAD of QueryPolicy, RequestGateFilter, BoundedBodyRequest, ErrorResponseWriter, RequestTarget (grep), and the real-port evidence table. Grepped main sources for `getParameter`/`getParameterMap`: none. I did not re-run the suite or probe port 18081 (economy); the table of 105 is taken as the delivery's claim and is marked below as not independently re-run.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-147 | MEDIUM | DoS | Tomcat connector, `BoundedBodyRequest` | Drip clients hold worker threads (SEC-F-073 confirmed). The 10 s deadline is checked only after a `read` returns, and a read waits up to the connector timeout (5 s), so the real cut is 10 to 15 s. After the 400 + `Connection: close`, Tomcat swallows the rest of the body bounded by bytes (maxSwallowSize), not time; at one byte per 2 s the thread can stay held for a very long time. Request line and header drips have no cap at all. | 105 F-2: open at 20 s on line and headers; 400 at 12.1 s then still open | A local process opens 8 drip connections: the 8-thread pool is exhausted until they are closed | Connector change (minimum data rate / `connectionUploadTimeout` + low `maxSwallowSize`), ASK FIRST, in Task 49+50 | SC-06, SC-12 |
| SEC-F-148 | LOW | Ordering | `RequestGateFilter` | The no-body rule reads the body up to the cap (16 KiB) before judging route, id and query, instead of "at most one byte". Bounded by SC-06, so no new exposure; spec wording differs. | diff `requestRules` uses `forwarded.getContentLengthLong() > 0` | none beyond 16 KiB per request | Liaison to confirm that bounded read is acceptable; otherwise change later | SC-06 |
| SEC-F-149 | INFO | Parsing | `QueryPolicy.decode` | Raw characters 0x80-0xFF are written as bytes (Tomcat's ISO-8859-1 view) and may form valid UTF-8; harmless because only the exact `status` name and three ASCII values pass. | code review | none | none | SC-18 |
| SEC-F-150 | INFO | Container | Tomcat HTML 400 for raw `[` `]` in the query | F-1 of 105. Same family as R-23 (static container page, no reflected data, no header set). | 105 row E-26 5 | none | accept (see section 5) | SC-06, SC-10 |

No CRITICAL or HIGH.

## 3. Controls checked
- SC-06: VERIFIED by code. The cap reads at most 16,385 bytes, chunked and declared; declared over-cap is refused by the existing early check before the read; the Expect case is answered 413 with no `100 Continue` (105 table, test `e31...`; not re-run by me). The 10 s deadline is new and partial (SEC-F-147).
- SC-18: VERIFIED by code. `idWellFormed` is applied to the raw URI segment (`getRequestURI`, no decoding), exact lower-case 36-char regex; any `%` or `;` fails. It is applied only after route and method (404/405 first, as specified).
- Query grammar: VERIFIED by code. Raw `getQueryString`, no servlet parameter API. Empty pair, unknown name, repeat, bad escape, invalid UTF-8 and chars above 0xFF are MALFORMED. Names are decoded before the unknown and repeat checks (so `%73tatus` equals `status`; consistent with E-26 row 6). Cost is linear: one split, a HashSet, a 2,000-pair bound checked after the split (an 16 KiB cap bounds the work anyway). Non-list or non-GET with a query is MALFORMED.
- INVALID_STATUS details: VERIFIED. The text is fixed ("must be one of: unread, reading, completed"), the submitted value is never echoed.
- Ordering: body/size, route, then id, query, no-body. Consistent with BR-012. A TooSlow result sets `Connection: close`.
- SC-10 (headers on new 400/413 paths): same `reject` path as before, headers re-set by `HeaderResponse`; real-port evidence relies on 105.
- Deadline and threads: it does not free the thread (see SEC-F-147). `Connection: close` is set but Tomcat swallows first.

## 4. Not verified
Suite and real-port rows not re-run by me; no probe of port 18081; Tomcat swallow behaviour taken from the delivery's measurement; public advisories not checked.

## 5. Rulings on the Sponsor decisions
F-1: accept as a container-rejected row in the R-23 family. Changing `relaxedQueryChars` would let `[` `]` reach the application, which widens the SC-06 value for a cosmetic gain; keep it. Spec E-26 row 5 and Appendix D need an annotation (status 400, container page). The Liaison must record this.

F-2: record R-24. Honest assessment: this is a real, unremediated DoS, not a theory. 8 slow local connections starve the app indefinitely, and the deadline added in Task 16 narrows only the body variant and does not free the thread. Realistic impact is low: loopback only, the attacker already runs code as a local process, and a browser page cannot drip over HTTP/1.1 (Host and CORS rules also apply). The connector change is medium risk and belongs with the Task 49+50 re-check. Merging with the risk pending is acceptable because nothing in Task 16 makes the previous position worse and the new deadline is an improvement. Condition: R-24 stays "Proposed" until the Sponsor accepts, and the Task 16 merge (a Sponsor act anyway) waits for that acceptance.

## 6. Verdict
PASS WITH CONDITIONS.
1. The Sponsor accepts R-24 (Appendix) and the F-1 ruling before merge.
2. The Liaison annotates E-26 row 5 for the container page and confirms SEC-F-148.
3. Task 49+50 re-check the drip behaviour and decide the connector change (SEC-F-147).

## Appendix: exact text for security.md (not applied by me)
Risk register row:

| R-24 | Slow-sending clients (one byte every 2 s on the request line, headers or body) keep a worker thread beyond the 5 s connector timeout, because the timer restarts on every byte; the 10 s body deadline of Task 16 closes only the body variant and Tomcat then swallows the rest by bytes, not time (SEC-F-073, SEC-F-147). The pool has 8 threads, so 8 such connections can stop the service until they close. The server listens on loopback only, so the attacker must run code on the machine | Medium / low | Recommend accept for Tasks 16 to 48; re-check in Tasks 49 and 50 (connector minimum data rate, upload timeout, small `maxSwallowSize`); the change is a connector setting, ASK FIRST | Sponsor: (pending) |

Also: add R-23 note "F-1 of Task 16: raw `[` `]` in the query are answered by Tomcat's HTML 400 (SEC-F-150)"; log SEC-F-147 to SEC-F-150; set SC-06 evidence note "Task 16: deadline partial"; add changelog line 0.36 "Task 16 review (handoff 107): SEC-F-147 to SEC-F-150 added; R-24 proposed. Verdict PASS WITH CONDITIONS."
