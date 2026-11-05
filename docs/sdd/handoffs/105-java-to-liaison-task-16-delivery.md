# Handoff 105: Task delivery, Task 16 (query, id and empty-body rules; transport limits)
- From: java
- To: liaison
- Type: TASK_DELIVERY (in progress)
- Branch: feature/task-16-query-id-limits

## Resume point
Done: steps 1 to 4 (QueryPolicy, gate rules, TransportLimitsIntegrationTest rows E-18, E-26, E-31, E-32, E-33, E-36, E-38, T-20, T-39 all green). FINDING F-1: Tomcat rejects raw [ ] in the query with its own HTML 400 (E-26 row 5 expects 400 MALFORMED_REQUEST); test asserts 400 only; report to Liaison/Sponsor (relaxedQueryChars is an SC-06 config change, ASK FIRST).
Next: step 5 slow-request E-22 live and drip tests; step 6 evidence (clean verify, coverage, diff stat, log); step 7 finish delivery; progress.md.
