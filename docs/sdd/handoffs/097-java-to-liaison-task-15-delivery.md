# Handoff 097: Task delivery, Task 15 (route table, methods and Accept)
- From: java | To: liaison | Type: TASK_DELIVERY | Branch: `feature/task-15-routing-accept` (local, not pushed)

## Resume point
- Done: all steps (RequestTarget, RouteTable, AcceptPolicy, gate and advice edit, real-port evidence, seeded failure, progress.md, status.md row).
- Next: Liaison audit. Nothing left for the architect.

## 1. Commits
`git log main..HEAD --oneline` (section 5 lists the subjects).

## 2. Files
Created: `config/transport/RequestTarget`, `RouteTable`, `AcceptPolicy`; tests `RequestTargetTest`, `RouteTableTest`, `AcceptPolicyTest`, `RoutingIntegrationTest`.
Edited: `RequestGateFilter` (stage 6), `error/GlobalExceptionHandler` (Allow from `RouteTable`; constructor now takes it), tests `RequestGateFilterTest`, `GlobalExceptionHandlerTest`, `BookControllerTest` (wiring of `RouteTable`), `RequestGateFilterIntegrationTest` (deviation 1), `progress.md`, `status.md` row. No change under `backend/config/**`, `pom.xml` or any frontend file.

## 3. Criteria
1. 404/405: `RouteTable.decide` + `RequestTarget.routable`; `RouteTableTest`, `RoutingIntegrationTest` (E-01, E-02, E-03, E-05, E-17, E-24, E-27 row 3).
2. `Allow`: `RouteTable.allowFor` is the only computation; used by the filter and by the advice; tests `e29...` and `frameworkMethodNotSupportedCarriesTheAllowOfTheRouteTable`.
3. Accept: `AcceptPolicy` (grammar of 5.0), called by the gate for paths starting `/api/v1/` after route and method; `AcceptPolicyTest` (59 cases), `e06AcceptGrammarOnTheList`, `methodIsDecidedBeforeAccept`, `staticContentIgnoresAccept`.
4. `RouteTableTest.equalsTheMappingsOfTheControllersAndTheCatalog` scans the controller package by reflection plus the catalog. Seeded check: a `@GetMapping("/seeded")` added to `BookController` made it fail (expected map contained `/api/v1/books/seeded=[GET]`), then reverted (working tree clean).

## 4. Test inventory
Unit: `RequestTargetTest` (31), `RouteTableTest` (51), `AcceptPolicyTest` (59), gate and advice tests updated. Integration (real port): `RoutingIntegrationTest` (13). Order proved: Content-Type grammar before 405 (E-05 row 1), 405 before 406.

## 5. Real output
`cd backend && ./mvnw clean verify`: Checkstyle 0 violations; unit `Tests run: 376, Failures: 0, Errors: 0, Skipped: 0`; integration `Tests run: 133, Failures: 0, Errors: 0, Skipped: 1` (the skip is not from this task); `All coverage checks have been met`; `BUILD SUCCESS`. Line coverage: RequestTarget 19/19, RouteTable 21/21, AcceptPolicy 32/32 (+ Range 2/2), RequestGateFilter 60/62, GlobalExceptionHandler 36/36; project total 95.8%.

Real-port table (`RoutingIntegrationTest`, status / Allow / body code):

| Row | Request | Status | Allow | Code |
|---|---|---|---|---|
| E-01 | GET `/api/v1/unknown`; `/api/v1/books/`; `/api/v1/books/{id}/`; `/api/v1/books/{id};x=1` | 404 | none | NOT_FOUND |
| E-02 | PUT, DELETE, PATCH `/api/v1/books` | 405 | `GET, POST` | METHOD_NOT_ALLOWED |
| E-02 | GET `.../{id}/status`; POST `.../{id}` | 404 (risk 1) | none | NOT_FOUND |
| E-03 | HEAD `/api/v1/books` | 405, empty body | `GET, POST` | |
| E-05 | OPTIONS no Content-Type; then with `application/json` | 415; 405 | none; `GET, POST` | UNSUPPORTED_MEDIA_TYPE; METHOD_NOT_ALLOWED |
| E-06 | Accept `text/xml`; `application/json`; none; `*/*` | 406; 200; 200; 200 | none | NOT_ACCEPTABLE (JSON body) |
| E-17 | POST `/` JSON | 405 | `GET` | METHOD_NOT_ALLOWED |
| E-24 | GET `/api/v1/%62ooks`; `/api/v1/books/%2e%2e` | 404; 404 | none | NOT_FOUND |
| E-27 | GET `/api/v1/books/not-a-uuid/other` | 404 | none | NOT_FOUND |
| E-29 | the 405s above (and POST `/index.html`) | 405 | exactly `GET, POST` or `GET`; no HEAD or OPTIONS | |
| SEC-F-127 | HEAD and OPTIONS on `/`, `/index.html`, `/css/app.css` | 405 | `GET` | METHOD_NOT_ALLOWED |

Raw `EVIDENCE` lines are printed by the test (`./mvnw verify -Dit.test=RoutingIntegrationTest`).

`git diff --stat main..HEAD` (before the final docs commit):
```
 16 files changed, 840 insertions(+), 42 deletions(-)
 AcceptPolicy 78, RequestGateFilter 35, RequestTarget 73, RouteTable 103, GlobalExceptionHandler 22,
 AcceptPolicyTest 63, RequestGateFilterIntegrationTest 41, RequestGateFilterTest 3, RequestTargetTest 45,
 RouteTableTest 168, RoutingIntegrationTest 179, BookControllerTest 9, GlobalExceptionHandlerTest 27,
 handoffs 096 and 097, status.md
```
`git log main..HEAD --format='%an <%ae>%n%B---'`: every commit has author `krlosferra <carlos.ferra@globant.com>` and an empty body; subjects:
```
feat(transport): decide route, method and Accept in the request gate [Task-15]
style(transport): wrap long test line [Task-15]
feat(transport): add Accept grammar policy [Task-15]
feat(transport): add route and method table [Task-15]
feat(transport): add raw request target parser [Task-15]
docs(sdd): add the Task 15 brief and update the resume point [Task-15]
```

## 6. Deviations
1. `RequestGateFilterIntegrationTest` used two probe controllers (`/probe/boom`, `/probe/echo`) that are not routes and would now be 404, and no property may add routes. It now uses `/api/v1/books` with a `@MockitoBean BookService` (500 test) and the real POST (the at-limit chunked body now ends `400 MALFORMED_REQUEST`, which shows it passed the cap). The "500 with Accept text/xml" row became Accept `application/json`, because `text/xml` is now 406 by design.
2. `GlobalExceptionHandler` constructor takes `RouteTable` (the error package now depends on config.transport).

## 7. Risks and follow-ups
1. Spec rows that name id endpoints (E-02 id rows, E-27 rows 1 and 2) expect 405; they answer 404 until Task 31 adds those routes (as the task text says).
2. `Accept` parameters are split naively on `,` and `;`, so a quoted parameter value containing either is judged unparseable (406). The spec does not define quoted values.
3. Whitespace around the `q` value (`q = 1`) is trimmed and accepted; the spec is silent.
4. Observed, pre-existing (Task 09): static 200 responses carry `Content-Disposition: inline;filename=f.txt`. Not touched; suggest the Liaison route it.
