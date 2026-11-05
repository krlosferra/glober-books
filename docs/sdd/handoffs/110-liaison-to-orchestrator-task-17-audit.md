# Handoff 110: Audit, Task 17 (container error wrapper)
- From: liaison
- To: orchestrator
- Phase: 4   Round: 1
- Type: REVIEW_REPORT
- Verdict: **ACCEPT WITH FOLLOW-UPS**

Mode: F (with G for the oracles). Artifacts: brief 108, delivery 109, tasks.md Task 17, `git diff origin/main..HEAD` (backend files: `EnvelopeResponseWrapper`, `RequestGateFilter`, `EnvelopeResponseWrapperTest`, `RequestGateFailureTest`, `TransportSecurityIntegrationTest`, `support/ErrorProbeConfig`), `progress.md`, `status.md`. I re-ran `./mvnw -o clean verify` offline: it completed and every report in `target/surefire-reports` and `target/failsafe-reports` shows 0 failures, 0 errors (`EnvelopeResponseWrapperTest` 15, `TransportSecurityIntegrationTest` 10, `RequestGateFailureTest` 1). I did not re-probe port 18080; the real-port table is the developer's claim, and its Tomcat-answered rows are consistent with Task 06 behaviour.

## Layer 1: specification compliance
| Item | Result |
|---|---|
| Crit. 1 `sendError` leaves as the envelope with headers | PASS. The wrapper turns both `sendError` overloads into the envelope through the writer and never calls the container's `sendError`, so no container page is produced. The message is dropped (`sendErrorNeverEchoesTheMessage`; the real-port test sends "secret probe text" and asserts it is absent). |
| Crit. 2 `GET /error` is 404 NOT_FOUND | PASS. Real-port GET and POST via the gate's route table. |
| Crit. 3 / SEC-F-114 | PASS WITH A NOTE (F-1). All 8 headers asserted on the sendError and `/error` paths, and on reset (unit and real port). |
| SEC-F-113 | PASS. The whole of `doFilterInternal` is inside one catch: header checks, body buffering, route and request rules, then the chain. `RequestGateFailureTest` uses a mocked `RouteTable` as the seam (no property). It asserts 500 INTERNAL_ERROR, the headers, that the chain is not reached, and that the exception text is not echoed. |
| SEC-F-128 | PASS. Five paths probed and recorded. Tomcat answers them itself with no version or path reflection. No connector setting was added (ASK FIRST respected). Covered by R-13 and R-23 under constitution 1.5. The Task 49 rows are listed. |
| Reuse (6) | PASS. `ErrorResponseWriter` and `ErrorBodyFactory` are reused, `HeaderPolicy.SECURITY_HEADERS` stays the single source, and the duplicate private `HeaderResponse` was removed. |
| Scope | PASS. No change under `backend/config/**`, `pom.xml`, or the dependencies. No new error code. |
| Javadoc on public API | PASS. |

## Layer 1b: repository hygiene
- Three Task 17 commits plus the brief commit, all with identity `krlosferra <carlos.ferra@globant.com>`. I grepped the message bodies and the full diff for claude, anthropic, co-authored, generated, session and vendor names: no matches. No AI attribution. Conventional Commits with `[Task-17]`. The commits are atomic (production, tests, docs).
- F-6 below: the branch name differs from tasks.md.
- Working tree clean after my build (`git status` empty).

## Layer 2: tests
- 100% of the new class is covered (30 of 30 lines, 12 of 12 branches, as delivered). The status-to-code mapping is parameterised over 11 statuses, including unknown statuses (401, 418, 503).
- The oracles are reasonable. They assert envelope code, status, content type and headers, not mirrored implementation. A committed-response case is included.
- Deviation 1 (two test files split for the 300-line Checkstyle limit): ACCEPTED. `RequestGateFilterTest` is at exactly 300 lines and `TransportSecurityIntegrationTest` at 289, so the split is justified. Note: `RequestGateFailureTest` and `ErrorProbeConfig` are files beyond the brief's scope list. They are test-only and low-risk.
- Deviation 2 (wrapper takes a `Rejecter` method reference, not the writer): ACCEPTED. It is consistent with the existing filter pattern and with the `config/**` freeze (SpotBugs EI_EXPOSE_REP2). See F-5 for a small cleanup.

## Findings and follow-ups
| ID | Sev | Finding | Follow-up |
|---|---|---|---|
| F-1 | MINOR | SEC-F-114 is proved indirectly. The wrapper writes the envelope itself, so no ERROR dispatch ever happens for an application `sendError`. No test forces a real container ERROR dispatch, for example an exception after the gate or a `sendError` made on the unwrapped response. `OncePerRequestFilter` skips ERROR dispatches by default, so such a dispatch would reach `/error` (Boot's error controller, if present) outside the gate. | Security reviewer to confirm that argument. Optionally, Task 49 adds a case showing that Boot's `BasicErrorController` cannot be reached with a body other than the envelope. If Boot's controller is still registered, record that in the plan appendix. |
| F-2 | MINOR | The IOException behaviour changed. An I/O failure in body buffering (client disconnect or reset) is now logged as "Unexpected error" at ERROR and answered 500. Before, it propagated. | PM or Sponsor to confirm that 500 is wanted. Consider logging at WARN and not writing at all for a disconnect. Delivery 109 disclosed this. Not blocking. |
| F-3 | MINOR | Statuses other than 400, 403, 404, 405, 406, 413 and 415 map to 500 INTERNAL_ERROR, including 401 and 4xx values such as 409 and 429. A client-side status becoming a 500 is semantically odd, but the brief allowed the generic mapping and these statuses cannot occur in the current app. | Record the mapping table in `plan-appendix.md` AP-1. |
| F-4 | MINOR | `envelope()` calls `resetBuffer` plus the writer, not `reset()`. Headers set by the application before `sendError` (`Allow`, `Content-Disposition`, `Location`, `Set-Cookie`) survive onto the envelope. Standard servlet behaviour, but SC-02 says the envelope is clean. `Content-Length` and `Content-Type` are overwritten by the writer, so there is no corruption. | Optional: the Task 31 or Task 49 check asserts that no application header leaks onto an error. No fix needed now. |
| F-5 | MINOR | `this.rejecter = delegate::write` wraps a `Rejecter` in a `Rejecter`. The indirection exists to get past SpotBugs. | Add a one-line comment, or simplify if SpotBugs allows. Cosmetic. |
| F-6 | MINOR | The branch is `feature/task-17-container-errors`, tasks.md says `feature/task-17-error-wrapper`. | The PM aligns the tasks.md branch name, or accepts the actual one. Not blocking, and the git-workflow pattern `<type>/task-NN-<slug>` is met. |
| F-7 | MINOR | `sendError` on a committed response is a silent no-op. The servlet API says IllegalStateException. | Accepted and documented. This is safer for the envelope. |
| F-8 | OPEN (Sponsor) | The Tomcat-answered HTML 400 and 404 pages carry none of the 8 security headers (no CSP or nosniff). Constitution 1.5 allows this, because the pages have no version, no reflection and no state change. The developer raised it. | Sponsor decision. Recommendation (not a decision): accept as residual risk with R-13 and R-23, and keep the Task 49 raw-socket rows so any change in the page is caught. Any connector change is ASK FIRST. |

## Carried to later tasks
- Task 49 raw-socket rows: `/%2e%2e/`, `/..;/`, `%2f`, `%00`, `/WEB-INF/web.xml` (status in 400/404, no product, version or echo, next request served).
- The Security gate for Task 17 is still pending from `sdd-security-reviewer`. I do not replace it.

## Verdict
**ACCEPT WITH FOLLOW-UPS.** Criteria 1 to 3 and the binding constraints SEC-F-113, SEC-F-114 (see F-1) and SEC-F-128 are met. The build is green, there is no scope creep, and there is no AI attribution. Gating items before merge: the Security verdict (needed), and the Sponsor decision on F-8 (can ride on the merge approval, as for R-24 in Task 16). The other findings are non-blocking.

Next step: route to `sdd-security-reviewer`. After the Sponsor approves, record the audit in `status.md` (the orchestrator updates the Liaison audit column to "ACCEPT WITH FOLLOW-UPS (handoff 110)"), then open the pull request, squash merge, and commit as `[Task-17] Container error wrapper - ACCEPTED`.
