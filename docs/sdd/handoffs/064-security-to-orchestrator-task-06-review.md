# Handoff 064: Security review of Task 06 (thin gate)
- From: security
- To: orchestrator
- Type: AUDIT_REPORT
- Branch: feature/task-06-thin-gate

## 1. Scope and method
Read: handoff 062, RequestGateFilter, HeaderPolicy, BoundedBodyRequest, TomcatErrorReportCustomizer, GlobalExceptionHandler, application.yml. Built the jar offline (`./mvnw -o -q -DskipTests package`), ran it on 127.0.0.1:18081 with a scratch database, sent raw-socket requests, stopped it (verified). I did not re-run the test suite; delivery numbers are the Java agent's claims, and I did not read the integration test sources in full.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-112 | LOW | Headers | Tomcat-level rejections (duplicate Host, malformed Host such as `localhost:18081.evil.com`, bad request line) | The container answers 400 `text/html` before the filter runs, with none of the 8 security headers (no `nosniff`, `no-store`, CSP). SC-10 says "every response". The page is static, no reflected input, no Server or product. | Raw socket on 18081: duplicate Host and malformed Host gave `HTTP/1.1 400`, `Content-Type: text/html;charset=utf-8`, no security headers. Absent Host on HTTP/1.0 got the gate JSON 400 with all headers. | None practical (static page, no user data). It is a spec deviation, not an exploit. | Accept as residual risk (Sponsor) and word SC-10 as "every response produced by the application", or add the headers at connector level in a later task. Also confirm the duplicate Host case is a 400 and not a rebinding bypass: it is a 400. | SC-10 |
| SEC-F-113 | LOW | Fail closed | RequestGateFilter.doFilterInternal | `checkHeaders`, `declaredBodyRefusal` and `bufferedIfBody` run outside the try block. An unchecked exception there (or a non-TooLarge IOException) escapes to the container valve (a bare HTML 500, security headers still present because the wrapper set them first). It still fails closed (no request reaches the chain), but it is not the JSON envelope. | Code reading; not triggered at runtime. | None found; only the format differs. | Wrap the whole body in the same catch that yields INTERNAL_ERROR, or add a test. | SC-11 |
| SEC-F-114 | INFO | Defense in depth | RequestGateFilter | The filter extends OncePerRequestFilter, so the ERROR dispatch is skipped by default. Headers set on the original response survive `sendError` in Tomcat, so this is fine today. The bypass test for the error dispatch is deferred to Task 17 by the delivery. | Code reading. | n/a | Keep the Task 17 test. | SC-10 |
| SEC-F-115 | INFO | Config | TomcatErrorReportCustomizer | No negative control that the valve is the one used (delivery admits it). My raw-socket check showed a bare page with no product, version, report or Server header, which is sufficient evidence for SEC-F-066. `continueResponseTiming=onRead` is a connector setting, not a change of an SC-06 value; I accept it. | Output of malformed request line on 18081. | n/a | Optional: assert in the integration test that the error report valve has showReport=false. | SEC-F-066 |

No CRITICAL or HIGH finding.

## 3. Controls checked
- SC-02 Host allowlist: VERIFIED by code (exactly one Host value, lower-cased, equals `localhost:<local port>` or `127.0.0.1:<local port>`; `[::1]` and no-port are refused) and by probe (absent Host on HTTP/1.0 gives 400 INVALID_HOST; duplicate Host and suffix tricks give 400). Case-insensitive match is correct. CR/LF cannot reach the filter (Tomcat refuses them) and the Host value is never echoed.
- SC-03/SC-04/SC-05: VERIFIED. `Origin: null` gave 403 FORBIDDEN (probe). Repeated Origin or Sec-Fetch-Site is refused; Sec-Fetch-Site is case-sensitive, fail closed. Content-Type is required on every method except GET and HEAD, so DELETE, PATCH, OPTIONS and TRACE need it. An OPTIONS preflight with a foreign Origin gave 403, with no `Access-Control-*` headers.
- SC-06: VERIFIED for the live checks. Declared 20000 with `Expect: 100-continue` and no body sent gave 413, `Connection: close`, no `100 Continue`. Code reads at most 16,385 bytes with `readNBytes`, so chunked bodies are bounded. No body byte is read before the declared-length 413.
- SC-10: VERIFIED for application responses (all 8 headers, no Server, no Set-Cookie); the wrapper reapplies after `reset` and `sendError`. FAILED as worded for container-level rejections: SEC-F-112.
- SC-11/SC-29: VERIFIED. `Accept: text/xml` gave a JSON 404 envelope; the advice returns fixed messages and logs only the class and top frame. No `getWriter` conflict seen (the unit tests cover it, per the delivery, not re-run by me).
- SC-28: VERIFIED by code (security header map and CSP as in the constitution; not diffed character by character against the constitution).
- SEC-F-066: VERIFIED by probe (see SEC-F-115).
- Ordering: `@Order(HIGHEST_PRECEDENCE)`; `spring.servlet.encoding.enabled=false`, `formcontent`, `hiddenmethod` and multipart filters disabled in application.yml; `forward-headers-strategy: none`; bind `127.0.0.1`.
- Fail closed: partly (SEC-F-113).

## 4. Not verified
Test suite and SpotBugs not re-run; concurrent slow-body behaviour (E-22, deferred); HTTP/2 and TLS (not enabled); ERROR-dispatch bypass (Task 17); exact CSP text against the constitution; public advisories (not in this task's scope).

## 5. Residual risks for the Sponsor
SEC-F-112 (LOW): container-level 400 pages lack the security headers. Recommendation: accept and reword SC-10, or close it in a later hardening task.

## 6. Verdict
PASS WITH CONDITIONS. Conditions before merge: (a) Sponsor accepts or the orchestrator schedules SEC-F-112; (b) SEC-F-113 fixed or accepted as LOW. Neither blocks the merge technically; no CRITICAL or HIGH is open.

## Appendix: exact text for security.md (not applied by me)
- §8 findings log: add SEC-F-112 to SEC-F-115 as in the table above (LOW, LOW, INFO, INFO).
- §3 control status: SC-02, SC-03, SC-04, SC-05, SC-06, SC-11, SC-28, SC-29 = VERIFIED (Task 06, handoff 064); SC-10 = VERIFIED for application responses, open LOW SEC-F-112 for container-level responses.
- Risk register: R-new "Container-generated 400 pages (duplicate or malformed Host, bad request line) carry no security headers. Static page, no reflected data. Recommend accept." Sponsor approval: pending.
- SEC-F-066: close with evidence from the probe on port 18081 (handoff 064).
