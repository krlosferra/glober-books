# Handoff 080: Task 09 security review (static content)
- From: security
- To: orchestrator
- Phase: 4   Round: 1
- Type: AUDIT_REPORT
- Scope: branch feature/task-09-static-content vs main; handoff 078

## Method
Read handoff 078, StaticAssetCatalog, StaticContentController, StaticContentConfig, MalformedRequestException, the GlobalExceptionHandler and application.yml diffs. Code review only; the integration test was skimmed through the delivery's own table. No live probe was run (the real-port table in 078 is the delivery's evidence, not re-executed by me).

## Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-126 | LOW | Config | application.yml `static-locations` | Key kept while `add-mappings: false`. Inert: no ResourceHttpRequestHandler is registered, and the catalog location is the literal `classpath:/static/` in StaticContentConfig. | Diff shows only `add-mappings: false` added; catalog constructor takes a string argument, not a property. | None today. If someone sets `add-mappings: true` the handler would serve `classpath:/static/` with ETag, Last-Modified and Range, which breaks E-30. | Optional: keep, since a startup test pins `add-mappings=false`. Accept. | SC-12 |
| SEC-F-127 | LOW | Transport | Controller `GET /**` | HEAD returns 200 (Spring) and OPTIONS with a Content-Type returns 200. Spec 5.8 says 405 with `Allow: GET`. No data beyond what GET gives, no state change. | 078 gap 1 | None (information-equivalent to GET). | Track in the Task 15 route table. | SC-10 |
| SEC-F-128 | LOW | Transport | Tomcat 400/404 pages (`/%2e%2e/`, `/..;/`, `%2f`, `%00`, `/WEB-INF/web.xml`) | These are answered by the container, as HTML, without the security headers or the envelope. Content is a fixed Tomcat page (no path reflected, per 078). Version string not confirmed by me. | 078 table row and gap 3 | Missing CSP or nosniff on an error page with no user data: negligible. Fingerprinting only. | Container error wrapper (R-23 scope, Increment 1). Confirm `server.error.include-*` and the Tomcat `ErrorReportValve` show no version or path. | SC-10 |
| SEC-F-129 | INFO | Hardening | StaticAssetCatalog.load | `URLDecoder.decode` treats `+` as space and decodes `%xx` in file names. A packaged file named with `+` or `%` is keyed by the decoded name. Not attacker reachable (build-time files), and request matching is raw equality, so it can only make a file unreachable, never reachable under another path. | Code | None. | Optional: use a URI-based decode. | SC-12 |

No CRITICAL or HIGH.

## Controls checked
- SC-12 VERIFIED by code. The catalog is fixed at start. Lookup is a map get on the raw `getRequestURI()`, so there is no filesystem access at request time and traversal cannot occur. Encoded slash, dot segments, `;`, trailing slash, double slash, case, and `%2e` forms never equal a key. Unicode: no normalisation is applied, so NFC and NFD forms do not match (fail closed). Only html, css, js are accepted. Dotfile segments, `.map`, `*.test.js` and any other extension stop the start with a fixed message that has no path. There is no directory listing (directory URIs are skipped). 404 uses the envelope. Gap: the 19-path 404 test is cited from 078, not re-run by me.
- SEC-F-072 VERIFIED by code: the location is a literal in `StaticContentConfig`, and the constructor argument is the only input. No `@Value`, `Environment` or property binding.
- SC-10 VERIFIED per 078 for 200, 400, 404 and 405 (CSP present on app responses). Content-Type carries a charset. The `nosniff` header is the existing filter's job; I did not re-run it. Container-generated pages are excluded (SEC-F-128).
- E-30 VERIFIED by code: the controller returns only Content-Type. It does not read Range or conditional headers, and `add-mappings: false` removes the resource handler that adds ETag, Last-Modified and Accept-Ranges. Spring's `ResponseEntity` ok-path does not add validators.
- Query or body on a static GET: 400 `MALFORMED_REQUEST` with a fixed message and no echo. An unknown path with a query gives 404 first, so there is no existence oracle beyond the 404 vs 400 difference. This is minor, because the catalog is public.
- Error messages: `NoResourceFoundException("static","not in catalog")` and the fixed MalformedRequestException message carry no path, and the envelope is built by the advice.

## Not verified
Live probes (no raw-socket run). The Unicode and encoded-form behaviour at the Tomcat layer is taken from 078 and from reasoning on raw equality. I did not inspect the full integration test or re-run `verify`.

## Residual risks for the Sponsor
HEAD or OPTIONS 200 against spec 405 (SEC-F-127), and the container error pages without headers (SEC-F-128). Both are LOW and tracked for Task 15 and the container wrapper. Recommendation: accept until those tasks.

## Verdict
PASS WITH CONDITIONS
- C1: SEC-F-127 and SEC-F-128 must be carried as open items into the Task 15 brief and the container error wrapper task (named in tasks.md). They do not block the Task 09 merge.

## Appendix: exact security.md text (security.md not edited by me)
- Findings log: SEC-F-126 (LOW, `static-locations` inert with add-mappings false; accepted), SEC-F-127 (LOW, HEAD/OPTIONS 200 vs spec 405; owner Task 15), SEC-F-128 (LOW, container 400/404 HTML pages lack headers and envelope; owner container wrapper, R-23), SEC-F-129 (INFO, URLDecoder `+` handling for packaged file names).
- Controls: SC-12 VERIFIED (Task 09: fixed catalog, raw-path equality, no filesystem lookup at request time, fixed start-failure reason). SEC-F-072 VERIFIED for the static location (literal in StaticContentConfig).
- Threat model delta: static GET `/**` catch-all returns 404 envelope for every unmapped GET; abuse cases traversal, dotfile, source map, test file, directory listing, Range or conditional probing are all closed by the fixed catalog.
