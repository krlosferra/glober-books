# Handoff 105: Task delivery, Task 16 (query, id and empty-body rules; transport limits)
- From: java
- To: liaison
- Type: TASK_DELIVERY (in progress)
- Branch: feature/task-16-query-id-limits

## Resume point
Done: step 1 QueryPolicy + test; steps 2 and 3 (id rule, query rule, no-body rule wired in the gate, RequestGateRulesTest with a mocked test-only route table; ErrorResponseWriter got a details overload for INVALID_STATUS), committed.
Next: step 4 TransportLimitsIntegrationTest (E-18, E-19, E-26, E-31, E-32, E-33, E-36, E-38, T-20, T-39 on real port); step 5 slow-request and drip tests; step 6 evidence; step 7 delivery.
