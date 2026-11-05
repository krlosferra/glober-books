# Handoff 106: Task 16 audit
- From: liaison
- To: orchestrator
- Type: REVIEW_REPORT
- Mode: F (semantic audit). Artifacts: handoffs 104 and 105, tasks.md Task 16, QueryPolicy, RequestGateFilter and BoundedBodyRequest (stage 9 and deadline), ErrorResponseWriter overload (read through grep and the delivery table; not every test body re-read).

## Verdict: ACCEPT WITH FOLLOW-UPS (Security verdict still required; not replaced here)

## Layers
- Layer 1: criteria 1 to 5 are met as far as the code allows (see rulings). `QueryPolicy` is pure, judges the id as received (no decoding, lower-case 8-4-4-4-12), checks the shape before the status value (MALFORMED before INVALID_STATUS), refuses 2,000 pairs, treats `?` as no query, and never uses the servlet parameter API. The `INVALID_STATUS` `details` entry follows BR-001. No change to `config/**` or `pom.xml`. No dependency added.
- Layer 2: orchestrator ran `clean verify` (green). The delivery gives a real-port table for E-18, E-22, E-26, E-31, E-32, E-33, E-36, E-38, T-20 and T-39, and the oracles are the spec's rows, not current behavior. I did not re-run it.
- Layer 1b: commits clean per the orchestrator and the delivery (human author, Conventional Commits with `[Task-16]`, no trailers).

## Rulings
1. F-1: ACCEPT as a container-rejected row (R-23 family). Do not touch `relaxedQueryChars` (SC-06, ASK FIRST). The PM must amend E-26 row 5 so that the raw `[` or `]` gets a container `400`, and the encoded form `%5B%5D` gets the envelope `MALFORMED_REQUEST`. The test already asserts only the status for the raw row, which is correct once the spec says so. The HTML body of that `400` is a gap for Task 17 and Task 06 (SC-02, INV-6); record it there.
2. F-2: ACCEPT as a documented residual risk, and criterion 5 is met by its own alternative ("or the limit is reported to the Sponsor as an accepted limit", SEC-F-073). The Sponsor must record it, because only the Sponsor accepts a residual risk. The record must name three facts. (a) Request-line and header drip are open at 20 s: the gate runs after the head is parsed, so `BoundedBodyRequest` cannot cover it, and the fix is Tomcat connector configuration (ASK FIRST). (b) Body drip gets `400` at about 12 s with `Connection: close`. (c) Tomcat keeps swallowing the rest, so the worker thread stays held. The exposure is bounded by the 8 threads, loopback only. Also ask Security whether the swallow behavior needs a connector setting (`maxSwallowSize` is bytes, not time) in the hardening increment.
3. (a) Ruling on the 10 s deadline: acceptable value (twice the 5 s idle timeout, enough for a 16 KiB body on loopback). It is an assumption, so the PM adds one line to BR-012 or Appendix D ("total time to read a request body: 10 s, then `400 MALFORMED_REQUEST`, connection closed") and a Decision Log entry. Until then the code comment "an assumption of Task 16" is honest. Check that the constant sits in one place and a test pins it (`BoundedBodyRequestTest` appears to do so).
4. (b) Id rule at gate level with a mocked route table: criterion 1 is met as far as possible, since the rule, the "after route and method are known" order (`routeAndMethodAreJudgedBeforeTheId`) and the `400` are proven. The real-port rows E-24 row 1, E-27 and T-14 cannot exist before an id route exists. Carry them to Task 31 acceptance criteria and make sure they are on its checklist; the mocked route is test-only and must not leak into production (A-09).
5. (c) The `ErrorResponseWriter` overload and the extra test classes: ACCEPT. The overload is required by BR-001 (`details` on `INVALID_STATUS`) and cannot be done in the three listed production files. The three extra test classes are justified by the 300-line limit and the 45 s slow tests. The PM records the file-list deviation in tasks.md Task 16 (Production list plus `ErrorResponseWriter`; Tests list plus `RequestGateRulesTest`, `BoundedBodyRequestTest`, `SlowRequestIntegrationTest`). Tell the Java agent that a file outside the list needs a handoff before the work, not in the delivery. Confirm the overload is covered by a test (the delivery says `INVALID_STATUS` rows assert `details`).
6. (d) The optional `HeaderPolicyTest` tightening: carry to Task 17 or the next transport test task. It is not blocking, and it should be a numbered line in status.md or progress.md so it is not lost.

## Follow-ups (numbered)
1. PM: amend E-26 row 5 (F-1) and add the 10 s total read deadline line plus Decision Log entry (3).
2. PM: record the Task 16 file-list deviation in tasks.md (5).
3. Sponsor: record the F-2 residual risk; Security to confirm the residual-risk wording.
4. Task 17 and Task 06: the container HTML `400` for raw `[` `]` and similar target rejections is outside the envelope.
5. Task 31: add real-port id rows (E-24 row 1, E-27, T-14) to its acceptance checklist.
6. Carry the HeaderPolicyTest tightening to a later task.
7. Minor observation, non-blocking: the delivery notes that a body on GET is read up to the cap before the route is decided (existing order, E-18/E-32). Make sure a spec row confirms that order; if it does not, ask the PM to state it.

## Next step
Security verdict on Task 16, then Sponsor approval. After that: update `progress.md`, commit `[Task-16] ... - ACCEPTED`, then open and merge the pull request per git-workflow.md. Proceed to the next READY task only after those two items.
