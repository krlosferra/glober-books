# Handoff 063: Liaison audit, Task 06 (thin gate, exception advice, container error report)
- From: liaison
- To: orchestrator
- Type: REVIEW_REPORT (Mode F)
- Artifacts reviewed: handoffs 061, 062; RequestGateFilter, HeaderPolicy, BoundedBodyRequest, TomcatErrorReportCustomizer, GlobalExceptionHandler; application.yml; test names skimmed through the delivery map. Build/commit facts taken from the orchestrator (clean verify green; 15 files, 1386 insertions; all commits by the human identity, no attribution). Not independently re-run.

## Verdict: ACCEPT WITH FOLLOW-UPS (Security verdict still required before the Sponsor gate)

## Layers
- Layer 1 compliance: PASS. Criteria 1-5 map to code and tests. Binding constraints: (1) one mapper, `ErrorResponseWriter`/`ErrorBodyFactory` reused, no `getWriter` in the filter, committed-response guard in `reject` plus test; (2) HIGHEST_PRECEDENCE, no `getParameter*`; (3) Host judged on raw `getHeaders`, exactly one value, `localhost|127.0.0.1:<local port>`, case-insensitive; (4) declared length > 16384 refused before any `getInputStream` (test counts 0 calls), chunked stopped by `readNBytes(16385)`, `Connection: close` on 413, no `100` observed on the real socket; (5) error valve registered unconditionally through `WebServerFactoryCustomizer`, container page shows no product/version/report; (6) catch-all logs one line (class + top frame, no message), JSON even for `Accept: text/xml`; (7) no new dependency.
- Layer 2 testing: PASS on the evidence stated (155 unit, 37 integration, SpotBugs 0, coverage gate met). Real-port classes give contract-level proof.
- Layer 1b hygiene: PASS per orchestrator.
- Layer 3: non-blocking notes below.

## Rulings on deviations
1. `spring.servlet.encoding.enabled=false`: ACCEPTED. Boot 4.1.2 key differs from the plan text; behaviour (AP-2 stage 1, Spring `CharacterEncodingFilter` off) is what the plan requires. Follow-up: PM/plan owner records the key as a Plan Amendment (plan.md D1/AP-2).
2. `continueResponseTiming=onRead` in the customizer: ACCEPTED. It is the mechanism that makes "no 100 Continue before the gate" true (observed failing with the default), is not an SC-06 value change, and sits in the transport customizer. Follow-up: Security to confirm in its verdict; note it in plan.md. Limitation: for a chunked body with `Expect: 100-continue` the `100` is sent when the filter starts reading, before the 413; the constraint only requires the declared-length case, but record it.
3. Final class with a lambda over the writer (SpotBugs): ACCEPTED, no exclusion needed; reasonable.
4. Two extra integration test classes: ACCEPTED as additive evidence; they are in tasks.md scope spirit (criteria 3 and 5 need a real port). Follow-up: tell Task 08 to reuse or absorb them, not duplicate.
5. No negative control for the error valve: ACCEPTED WITH FOLLOW-UP. The positive evidence (bare page, no report/server info) is the assertion that matters, but without a control the test could pass if Tomcat's default already hid details. Follow-up (cheap, before Task 08 or in it): a one-off check or test that the default valve (customizer removed) shows the version/report, to prove the test discriminates. Not blocking.
6. Delivery lacks `git diff --stat` / `git log`: ACCEPTED, orchestrator supplied equivalent facts (15 files, 1386 insertions, human identity). Record the figures in progress.md. Note: 15 files vs the delivery's listed set (5 main, 5 test, application.yml, progress.md, status.md, 062) is consistent.

## Follow-ups (numbered)
1. Plan Amendment: encoding key `spring.servlet.encoding.enabled`, and `continueResponseTiming=onRead` (owner: plan.md author; approve via Sponsor).
2. Security verdict on Task 06 (veto holder) before merge; ask it to confirm item 2 above and the Host/Origin grammar against SC-03..SC-06.
3. Negative control for the error valve (see ruling 5).
4. MINOR: in `RequestGateFilter.bufferedIfBody`, a non-`TooLarge` `IOException` (e.g. client reset while reading) propagates outside `proceed`'s catch, so it is not mapped to `INTERNAL_ERROR`. Likely acceptable (client is gone) but decide and test in Task 17/E-22 (slow body) together.
5. MINOR: `reject` swallows only `IllegalStateException` from the writer; fine, but add the error-dispatch bypass test already scheduled for Task 17.
6. Housekeeping: progress.md entry with real counts and the diff stat; commit format `[Task-6]`/status row to DONE only after the Sponsor gate; PR opens only after Sponsor approval.
