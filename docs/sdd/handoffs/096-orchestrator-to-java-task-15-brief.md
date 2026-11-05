# Handoff 096: Task brief, Task 15 (route table, methods and Accept)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 15 of `docs/sdd/tasks.md` (v0.6.1): read its text, criteria 1 to 4 and traces. Branch `feature/task-15-routing-accept` is checked out; Tasks 01 to 14 are merged. Read also: `spec.md` 5.0 routing, methods and Accept rules, BR-012 steps 1 and 2, Appendix D rows E-01 to E-03, E-05, E-06, E-17, E-24 (rows 2 and 3), E-27, E-29; `plan.md` D1; `plan-appendix.md` AP-1 (RequestTarget, RouteTable, AcceptPolicy), AP-2 (stages 4, 6 and 7; the order is Content-Type grammar for non-GET/HEAD methods first, so 415 comes before 405 as E-05 states); `security.md` SC-18, SC-40, SEC-F-127, SEC-F-128; the existing `config/transport/RequestGateFilter`, `HeaderPolicy`, `StaticAssetCatalog`, `controller/*`, `error/*`.

## Scope
`config/transport/RequestTarget` (raw request target parser: no normalisation of `//`, `;`, `%2e`, trailing slash or case), `RouteTable` (route and method decision from the raw target), `AcceptPolicy` (Accept grammar on `/api/v1/` paths), edit `RequestGateFilter` to call them (stage 6: route and method, then Accept), tests `RouteTableTest` and `AcceptPolicyTest`. Out of scope: query, id and empty-body rules (Task 16), the container wrapper (17), any new endpoint (Task 31 extends the table).

## Binding constraints
1. 404 `NOT_FOUND` in the envelope for unknown paths, trailing slash, suffix, `;`, encoded variants of fixed segments and encoded dot segments (E-01, E-24 rows 2 and 3). 405 `METHOD_NOT_ALLOWED` for a wrong method on a defined path, including HEAD and OPTIONS on `/api/v1/books`, `/`, `/index.html` and every static asset path (E-02, E-03, E-05 second row, E-17; SEC-F-127: HEAD and OPTIONS currently answer 200 on static paths through the `GET /**` mapping: close that). The route and method come first, the id format second (E-27: this task does not add id rules, they are Task 16).
2. Every 405 carries `Allow` with exactly the defined methods, never HEAD or OPTIONS (`/api/v1/books`: `GET, POST`; `/` and static paths: `GET`) (E-29). Build the body through the existing `ErrorBodyFactory` and `ErrorResponseWriter`; the `Allow` header must also be present when the 405 is produced by the advice (the framework's not-supported exception), so there is one place that computes it.
3. Accept: `text/xml` gives `406 NOT_ACCEPTABLE`; absent, empty, `*/*`, `application/json`, `application/*` follow the grammar of 5.0 (read it; do not guess) and give 200; error bodies stay JSON whatever the Accept. Accept is checked on `/api/v1/` paths only (static content keeps ignoring it).
4. `RouteTableTest` must enumerate the controllers' real mappings (reflection on the request mappings and the `StaticAssetCatalog`) and fail on any difference with the table, so a new endpoint cannot be added without the table (X-11).
5. Reuse `MalformedRequestException` if you need a 400; no new error codes; no new dependency; no change under `backend/config/**` or `pom.xml`; no property can add routes.
6. Order of checks stays: Host, Origin, Sec-Fetch-Site, Content-Type grammar (non-GET/HEAD), body cap, then route and method, then Accept. Do not change the earlier stages.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; a table of requests on the real port with the real status, `Allow` and body for E-01, E-02, E-03, E-05 (both rows), E-06 (four rows), E-17, E-24 rows 2 and 3, E-27 row 3 and E-29, plus HEAD and OPTIONS on `/`, `/index.html` and a static asset; the `RouteTableTest` failing when a mapping is temporarily added (seeded, then reverted); real line coverage of the new classes; `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'` (paste both).

## Rules
STOP and report on any Boot 4.1.2 behaviour that differs from the plan or any spec gap. Commit after each finished step (`type(scope): subject [Task-15]`), local commits only, explicit paths, never `--no-verify`, nothing naming a tool, model or vendor as author or co-author. Update `docs/sdd/progress.md` in the same change. Write the delivery as `docs/sdd/handoffs/097-java-to-liaison-task-15-delivery.md` early with a "Resume point" section (done / next) kept updated so nothing is lost if you are cut off.
