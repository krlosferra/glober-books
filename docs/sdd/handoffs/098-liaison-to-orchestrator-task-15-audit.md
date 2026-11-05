# Handoff 098: Audit of Task 15 (route table, methods and Accept)
- From: liaison | To: orchestrator | Type: REVIEW_REPORT | Branch: `feature/task-15-routing-accept`

## Verdict: ACCEPT WITH FOLLOW-UPS (Liaison). Security verdict and Sponsor approval still required at the gate.

Scope of the audit: read of handoffs 096 and 097, Task 15 text, `RouteTable`, `AcceptPolicy` and the stage-6 code of `RequestGateFilter` in full; the tests were taken from the delivery's inventory and the orchestrator's green `./mvnw clean verify` (not re-run by me).

## Criteria
1. 404/405 for unknown, trailing slash, suffix, `;`, encoded and dot-segment paths, and HEAD/OPTIONS/other methods: met. `decide` returns NOT_FOUND when `!target.routable()` or the path is undefined; methods are compared case-sensitively. Evidence: real-port table in 097 (E-01, E-02 books rows, E-03, E-05, E-17, E-24, E-27 row 3).
2. `Allow` exact, never HEAD or OPTIONS: met. `allowFor` is the single computation over a sorted set of GET/POST; used by the filter and the advice.
3. Accept: met. E-06 and the E-35 rows I checked by hand against `AcceptPolicy` agree with spec (`;;;`, `q=1.5`, `Q=0`, `application/json;q=0, */*`, `text/html,,application/json`). Accept runs after route and method (405 before 406), only under `/api/v1/`, and the error bodies are JSON.
4. Table holds only the existing routes; `RouteTableTest` compares against the controller mappings by reflection plus the catalog. The seeded failure (an extra `@GetMapping`) was shown to fail, then reverted. Met.

Brief constraints: files are the four listed plus tests; no change to `pom.xml`, `config/**` or the frontend; commits use the human identity and have no attribution (per the orchestrator's check).

## Rulings on the deviations
1. `RequestGateFilterIntegrationTest` rewiring: ACCEPTED. The `/probe/*` controllers cannot remain routes (fixed table, no property adds routes, criterion 4), so rewiring is forced. At-limit chunked body now answering 400 MALFORMED_REQUEST is consistent with spec: the cap test asserts "not 413", and the 400 is the downstream refusal of a body that passed the cap. Confirm the body is exactly the at-limit size and that an over-limit companion still gives 413 (follow-up F1). The 500 row with `Accept: application/json` is consistent: `text/xml` on an API path is 406 by E-06, so the old row could no longer reach the 500 path. Spec-wise, error bodies stay JSON regardless.
2. `GlobalExceptionHandler` takes `RouteTable`: ACCEPTED. It keeps `Allow` computed in one place (E-29). The cost is the error package depending on `config.transport`; record this in `plan.md` "Plan Amendments" (F2) so it is not seen as undeclared drift. No layering cycle was found, since `RouteTable` depends only on `StaticAssetCatalog`.
3. Id endpoints answering 404 until Task 31: ACCEPTED and correctly deferred. Task 15 criterion 4 and the Task 31 line say the table grows with the five endpoints. Condition (F3): Task 31 must add the E-02 id rows and E-27 rows 1 and 2 as failing-first rows, and `RouteTableTest` forces the table update. Add to the Task 31 brief.
4. Accept naive splitting (quoted values give 406; whitespace around `q` accepted): ACCEPTED as a limit, no PM clarification needed to proceed. Reason: the spec's grammar (5.0, E-35) lists no quoted-string or whitespace rows, the failure is toward 406 (fail-closed), and only `q` is read. Do not change the code. F4: the PM should add one sentence to 5.0 or section 6 ("quoted parameter values are unparseable and give 406; optional whitespace around `q=` is accepted") so the behavior is specified, not just observed. Low priority, P2.
5. `Content-Disposition: inline;filename=f.txt` on static 200: not caused by Task 15. I found no mention of `Content-Disposition` in `spec.md` (not in 5.8 or anywhere), so it is not a violation of a stated rule. It is still a defect in intent: it names a wrong filename (`f.txt`) for HTML/CSS/JS, which looks like the Tomcat/Spring default for the Task 09 static handler, and sets a file-name hint the spec never asked for. Security-relevant observation: refer to `sdd-security-reviewer`, which owns the question of whether the header is wanted (it relates to the SC/E-15/E-16 static rows, where `nosniff` and content types are the specified controls). Recommendation (F5): open a follow-up against Task 09 (reopening needs a change request), either to suppress the header or to have the PM record it in spec 5.8 as accepted. Do not fix it inside Task 15.

## Follow-ups
- F1: confirm the at-limit and over-limit chunked pair in `RequestGateFilterIntegrationTest` (an assertion of 400 AND of 413 for limit+1).
- F2: add the `error -> config.transport` dependency to `plan.md` Plan Amendments (coding agent).
- F3: Task 31 brief must include the deferred id rows (E-02, E-27 rows 1 and 2).
- F4: PM, one-line spec clarification on quoted Accept parameters and whitespace around `q` (P2).
- F5: Security reviewer ruling on `Content-Disposition` for static responses; PM change request against Task 09 if it is to be fixed.

## Reminders
After the Security verdict and Sponsor approval: update `progress.md`, set `status.md` to DONE, squash merge by pull request with `feat(transport): ... [Task-15]`, then brief Task 16.
