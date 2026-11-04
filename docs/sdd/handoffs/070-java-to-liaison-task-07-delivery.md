# Handoff 070: Task 07 delivery (add and list API, thin)
- From: java
- To: liaison
- Type: TASK_DELIVERY (IN PROGRESS)

## Resume point
- Done: DTOs; BookService+test; BookController+advice mappings+BookControllerTest (green). Finding: Jackson 3 writes Instant with zero millis as 2026-02-19T10:00:00Z; fixed with @JsonFormat pattern on BookResponse dates (needs contract test + report).
- Next: application.yml dialect + startup test; BookApiIntegrationTest (wire format, ordering, streaming, read writes nothing, real statuses); full verify; evidence; status.md and progress.md.
