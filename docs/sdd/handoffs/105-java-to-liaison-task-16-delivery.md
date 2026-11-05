# Handoff 105: Task delivery, Task 16 (query, id and empty-body rules; transport limits)
- From: java
- To: liaison
- Type: TASK_DELIVERY (in progress)
- Branch: feature/task-16-query-id-limits

## Resume point
Done: steps 1 to 5. Drip result: request line and headers survive 20 s (accepted limit, container-level); body drip: total deadline 10 s added in BoundedBodyRequest, 400 sent at about 12 s but Tomcat keeps the connection open swallowing the drip (not closed within 20 s more): report to Sponsor. FINDING F-1 (Tomcat HTML 400 on raw brackets in query) stands.
Next: step 6 clean verify, coverage, diff stat, log; step 7 finish delivery body; progress.md.
