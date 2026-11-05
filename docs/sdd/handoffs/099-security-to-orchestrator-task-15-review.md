# Handoff 099: Security review, Task 15 (route table, methods, Accept)
- From: security | To: orchestrator | Type: AUDIT_REPORT | Branch: feature/task-15-routing-accept | Verdict: PASS WITH CONDITIONS

## 1. Scope and method
Read RequestTarget, RouteTable, AcceptPolicy, RequestGateFilter and GlobalExceptionHandler diffs, handoff 097. Did not re-read the test sources in full. Ran the built jar on port 18081 with a scratch database (stopped afterwards) and probed with curl (--path-as-is) and one raw-socket request.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Remediation | Control |
|---|---|---|---|---|---|---|---|
| SEC-F-142 | LOW | Headers | static 200 responses | `Content-Disposition: inline;filename=f.txt` is sent on every static 200. It is not in the code (grep of src finds nothing), so it is Spring's RFD-protection header. It is not in the constitution header list. Impact: no leak (fixed constant name), `inline` means no download, and nosniff plus the right Content-Type are present. Cosmetic noise only. | live probe of `/css/styles.css` | Document it as intended framework behaviour in plan.md or constitution (or remove it). Do not rely on it as a control. | SC-10 |
| SEC-F-143 | LOW | Accept | AcceptPolicy.parse | Duplicate `q` parameters: the last one wins (`q=0;q=1` is accepted). Naive split on `,` and `;` makes a quoted value containing either a 406 (fails closed). Whitespace in `q = 1` is tolerated. No security impact. | code read | Define in the spec (5.0) or add a test. | SC-18 |
| SEC-F-144 | INFO | Resource use | AcceptPolicy | No ReDoS. The patterns are bounded character classes with no nested quantifiers, and the parse is linear. Header size is capped by Tomcat's 8 KiB default (a 2.8 KB list of `a/b` gave 406 in a normal time). | code and probe | none | SC-18 |

No CRITICAL or HIGH.

## 3. Controls
- SC-18 and SC-40 VERIFIED. The decision uses `getRequestURI()` (raw) and an exact map lookup. Live results: `//api/v1/books` 404, `/API/v1/books` 404, `/api/v1/books%3Bx` 404. `RequestTarget.routable` rejects `;`, `\`, `%2f`, `%5c`, `%3b` and literal or encoded dot segments, with case folding. Trailing slash and case are not in the map, so they are 404. The method is compared case-sensitively. Raw-path probes for `%2e` and `%62` (encoded fixed segment) are only covered by the integration test E-24, which I did not re-run.
- SEC-F-127 CLOSED. HEAD on `/` gave 405 with `Allow: GET`. OPTIONS on `/api/v1/books` with a JSON Content-Type gave 405 with `Allow: GET, POST`. Neither header lists HEAD or OPTIONS. Static and API paths come from the same table. Allow is built only from the fixed method sets (constants, comma-space joined), so it cannot carry CRLF or request data.
- Order VERIFIED. In `doFilterInternal` the order is Host, Origin and Fetch-Metadata, Content-Type (`checkHeaders`), then the declared-length cap, then the buffered body cap, then `routeRefusal`. A 404 or 405 cannot skip an earlier check. The Accept check comes after route and method, on `/api/v1/` paths only, and fails closed (406 on an unparseable header or a bad q).
- Error bodies VERIFIED. The 404, 405 and 406 bodies are the generic envelope with fixed messages. They contain no path or method. The raw-socket 406 carried the full security headers.
- The advice (`handleMethod`) takes Allow from `RouteTable.allowFor` and adds none for an unknown path. This is a single source.
- X-11 (route table against controller mappings): the test is by reflection and the seeded failure is reported in 097 (section 3, criterion 4). I did not re-run it.

## 4. Not verified
Did not re-run the test suite or the seeded failure. Did not read the test sources line by line. Did not probe encoded dot segments live (my curl probes did not include them). Tomcat may reject some raw forms with its own 400 before the filter. That is acceptable but outside this table.

## 5. Residual risks
- The id routes answer 404 where the spec rows (E-02, E-27) expect 405, until Task 31. This is not a security weakness (it is fail closed) but it is a spec deviation the Liaison owns.

## 6. Verdict
PASS WITH CONDITIONS. Condition C-1: the Liaison or PM records SEC-F-142 (Content-Disposition) as intended framework behaviour, or asks for its removal, before merge. SEC-F-143 and SEC-F-144 need no gate.

## Appendix: exact text for security.md (I did not edit it)
- Findings log: SEC-F-142 LOW static 200 carries Spring's `Content-Disposition: inline;filename=f.txt`; no leak; document or remove. SEC-F-143 LOW Accept duplicate q last wins, quoted parameters fail closed; define in spec. SEC-F-144 INFO Accept parsing linear, no ReDoS, header bounded by Tomcat. SEC-F-127 CLOSED (Task 15, evidence: HEAD and OPTIONS give 405 with Allow GET or GET, POST).
- SC-18 and SC-40: Task 15 VERIFIED (raw target, exact map, no normalisation, gate order, generic bodies).
