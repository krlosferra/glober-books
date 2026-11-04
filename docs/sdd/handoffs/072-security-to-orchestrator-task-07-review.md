# Handoff 072: Task 07 security review
- From: security
- To: orchestrator
- Type: AUDIT_REPORT
- Branch: `feature/task-07-add-and-list-thin` (phase 4, commits 4714c16..33693f1)

## 1. Scope and method
Read handoff 070, BookService, BookController, CreateBookRequest, BookListResponse, BookResponse, IdConfig, BookValidationException, the GlobalExceptionHandler and application.yml diffs, and grepped BookApiIntegrationTest and the gate tests. I did not run the application or the build; I did not probe a live port (no raw-socket check done). Delivery claims about test results were not re-run.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-120 | LOW | Resource exhaustion | `BookService.list`, `BookListResponse.of` | `list()` has no row bound. The 5,000-row cap is not enforced in Task 07; it depends on the capacity rule of Task 23a. The list is held in memory three times (entities, mapped list, `List.copyOf`) before the converter writes it. | `findAllByOrderByDateAddedDescIdAsc().stream().map().toList()`, then `List.copyOf`. | Until 23a, an unbounded number of POSTs makes GET heavier (local, single user, 1-connection pool). | Condition on Task 23a: enforce the cap at create. Keep the list unpaged only while that cap holds. | SC-10/SC-16 |
| SEC-F-121 | INFO | Gate | tests | The gate (Host, Origin, Content-Type) is a path-generic filter (no `/api` match in `RequestGateFilter`), and `RequestGateFilterIntegrationTest` proves it on the real port for other paths. No test hits `/api/v1/books` with a forged Host, Origin or wrong Content-Type. | grep of the two test classes. | None found; coverage gap only. | Add a real-port gate row for POST/GET `/api/v1/books` in Task 34b or the next API task. | SC-01..SC-05 |
| SEC-F-122 | INFO | Input | `CreateBookRequest` | `String title` may accept a JSON number through Jackson scalar coercion. Not verified (not run). Harmless: the value becomes text and passes the same length checks. | code read | none | Optionally disable `ALLOW_COERCION_OF_SCALARS` for strings in Task 20. | SC-08 |

No CRITICAL or HIGH finding.

## 3. Controls checked
- SC-07/SC-08 (DTOs only, no entity in controller signatures): VERIFIED by reading `BookController` (takes `CreateBookRequest`, returns `ResponseEntity<BookResponse|BookListResponse>`). `BookResponse.from` is the only entity-to-wire path. Reflection test is claimed in 070 but not re-run. SEC-F-119 condition for Task 07: met.
- Unknown members: VERIFIED by reading. `HttpMessageNotReadableException` maps to `respond(MALFORMED_REQUEST)` with no argument, so no member name, no Jackson message and no details are echoed.
- Mass assignment: VERIFIED. The request record has only title, author, status and allowDuplicate. The id comes from the supplier, dates from the clock, rating is hard-coded null, `date_completed` is set only when status is completed. A client `id`, `rating` or `date_added` is an unknown member and returns 400.
- SC-11: VERIFIED by reading. Bean Validation messages come from a fixed `FIELD_MESSAGES` map, not from `error.getDefaultMessage()` or the rejected value. `BookValidationException` carries only the code and fixed messages. The service and controller have no logger. The catch-all logs only the class name and a location, not the body or SQL (the class name appears in the log, not in the response).
- Id generation: VERIFIED by reading. `UUID::randomUUID` is a v4 UUID backed by `SecureRandom`, behind an injectable `Supplier<UUID>` bean.
- Clock: VERIFIED. The injected `Clock` is used once per create, truncated to milliseconds.
- Transactions: VERIFIED by reading. `create` is `@Transactional`; `list` is `@Transactional(readOnly = true)` and writes nothing. The test asserting the database hash stays unchanged around a list is claimed, not re-run. `open-in-view` is false.
- SEC-F-071 (streaming): PARTIAL. The controller returns a DTO, so the converter writes to the response stream and the controller adds no `String` or `byte[]` buffer. The type-level evidence is adequate for the controller. Hidden buffering remains in the service (three in-memory copies). Tomcat's response buffer is small and flushes. The 5,000-row bound is not enforced here (SEC-F-120).
- SEC-F-117 (NUL and control characters): Task 07 does not make it worse. It strips and checks code-point length only. Text is stored verbatim, which is the same exposure as before. It stays open for Tasks 18 to 20 (and 15/30).
- Stored XSS surface: VERIFIED by reading. Responses are `application/json; charset=UTF-8` and the header policy already sets nosniff. Text is returned verbatim; the frontend must render it with `textContent` (SC-for-DOM, to check in the frontend tasks).
- SEC-F-118 (dialect): `spring.jpa.database-platform` is now set in `application.yml`. The startup test is claimed in 070 and not re-run; `hibernate-community-dialects` is on the pom. Condition met pending the build.
- SEC-F-119: met for Task 07 (see SC-07/SC-08).
- Gate on new endpoints: filter is path-generic (SEC-F-121). Not probed live.

## 4. Not verified
Build and tests were not re-run. No live probe of the endpoints (Host/Origin/Content-Type, raw socket, oversized body). Jackson coercion (SEC-F-122) and dependency advisories were not checked.

## 5. Residual risks
None needing Sponsor acceptance. SEC-F-117 and SEC-F-120 stay as conditions on later tasks.

## 6. Verdict
PASS WITH CONDITIONS. Conditions to carry (none blocks merge of Task 07): (a) SEC-F-120 closed in Task 23a with the capacity cap; (b) SEC-F-121 gate row for `/api/v1/books` by Task 34b; (c) SEC-F-117 stays OPEN for Tasks 18 to 20. Mark SEC-F-118 and SEC-F-119 as met once the Liaison confirms `./mvnw verify` is green. Next step: Sponsor gate.

## Appendix: exact text for `security.md` (orchestrator to apply; I did not edit it)
Findings log rows:
- `| SEC-F-120 | LOW | Task 07 list has no row bound and holds the list in memory three times; the 5,000-row cap arrives with Task 23a | OPEN, condition on Task 23a | 072 |`
- `| SEC-F-121 | INFO | Gate filter is path-generic but no real-port test hits /api/v1/books with forged Host, Origin or Content-Type | OPEN, add a row by Task 34b | 072 |`
- `| SEC-F-122 | INFO | String fields may accept JSON numbers through Jackson scalar coercion (not verified) | OPEN, optional hardening in Task 20 | 072 |`
Updates: SEC-F-118 -> `MET in Task 07 (database-platform set in application.yml; startup test), 072`; SEC-F-119 -> `MET for Task 07 (controller signatures are DTO only), 072`; SEC-F-071 -> `PARTIAL in Task 07: controller type-level evidence adequate, service copies the list, bound pending Task 23a`; SEC-F-117 -> note `Task 07 does not worsen it`.
Changelog: `- 0.29 (2026-11-04): Task 07 review (handoff 072): SEC-F-120 to SEC-F-122 added. Verdict PASS WITH CONDITIONS.`
Status.md Security cell for Task 07: `PASS WITH CONDITIONS (072)`.
