# Handoff 204: Task 55 delivery (remove the inert `static-locations` property)
- From: java-architect   To: liaison   Type: TASK_DELIVERY

## Resume point
Done: implementation, focused tests, full `./mvnw verify`, local commit (not pushed). Next: Liaison and Security review (protected surface `application.yml`, reviewed alone).
Task 55, branch `chore/task-55-remove-static-locations`, commit: see `git log -1` on the branch (tagged `[Task-55]`).

## Files edited
- `backend/src/main/resources/application.yml` (removed `spring.web.resources.static-locations`; `add-mappings: false` unchanged)
- `backend/src/test/java/com/globant/globerbooks/MandatoryPropertiesIntegrationTest.java`
- `backend/src/test/java/com/globant/globerbooks/config/sqlite/StartupPolicyIntegrationTest.java`
- `docs/sdd/handoffs/204-...`, `docs/sdd/progress.md`. Nothing else changed.

## Criteria to tests
| Criterion | Test |
|---|---|
| Key absent from `application.yml`, `add-mappings` kept false | `StartupPolicyIntegrationTest.definesNoPropertyThatChangesTheStaticCatalogDirectory` (key null, add-mappings false) |
| No longer a mandatory property | `StartupPolicyIntegrationTest` effective-environment table (entry removed); `MandatoryPropertiesIntegrationTest.staticLocationsKeyIsAbsentAndRequestDetailLoggingIsOff` (key null, log-request-details false) |
| Catalog behaviour unchanged, property inert | `StaticContentIntegrationTest.productionWiringReadsOnlyTheClasspathStaticLocation` kept unchanged (sets the key and asserts identical catalog); the other 47 cases (headers, 404/405) pass untouched |

## Results
| Run | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| StartupPolicyIntegrationTest | 9 | 0 | 0 | 0 |
| MandatoryPropertiesIntegrationTest | 15 | 0 | 0 | 0 |
| StaticContentIntegrationTest | 48 | 0 | 0 | 0 |
| `cd backend && ./mvnw verify` | 351 | 0 | 0 | 1 | exit 0, JaCoCo check passed, BUILD SUCCESS |

## Deviations
None. No production Java file needed a change (the catalog location is a literal).

## Risks
With the key gone, Boot's default `WebProperties` static locations apply to the bean value, but `add-mappings: false` registers no resource handler, so they are unused; the proof is the unchanged StaticContent suite. The one skipped test pre-exists.
