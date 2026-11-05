# Handoff 078: Task 09 delivery (static content)
- From: java
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY
- Brief: handoff 077

## Resume point
- Done: catalog, controller, wiring, `add-mappings: false`, advice mapping, all tests, `./mvnw clean verify` green, real-port evidence, status row `IN_REVIEW`, progress note.
- Next: Liaison audit and Security review; nothing else open on the java side. Not pushed (per instruction).

## Files
Created: `backend/src/main/java/com/globant/globerbooks/config/transport/StaticAssetCatalog.java`, `.../controller/StaticContentController.java`, `.../config/StaticContentConfig.java`, `.../error/MalformedRequestException.java`, `backend/src/test/java/com/globant/globerbooks/config/transport/StaticContentIntegrationTest.java`.
Modified: `.../error/GlobalExceptionHandler.java` (one handler: `MalformedRequestException` to 400 `MALFORMED_REQUEST`), `backend/src/main/resources/application.yml` (`spring.web.resources.add-mappings: false` added; `static-locations` kept, it is inert with mappings off and existing tests pin it), `.../config/sqlite/StartupPolicyIntegrationTest.java` (line asserting `add-mappings` is null now expects `false`), `docs/sdd/status.md` (own row), `docs/sdd/progress.md`. No change in `frontend/`, `pom.xml`, `backend/config/**`. No new dependency.

## Criteria to code to test (all in `StaticContentIntegrationTest`)
1. `rootAndIndexReturnTheSameBytesWithHtmlType`, `cssAndJsHaveTheirContentTypes`, `staticResponsesCarryTheSecurityHeaders`.
2. `staticResponsesHaveNoValidatorsOrRanges`, `rangeAndConditionalHeadersAreIgnored`.
3. `everythingOutsideTheCatalogIsNotFoundInTheEnvelope` (19 paths: directories, favicon, test.js, /error, /.git/config, db, trailing slash, double slashes, dot segments, case, /.env, `;jsessionid`), `preApplicationFormsAreNotFoundOrAnEchoFreeBadRequest` (E-15 allowance), `queryOnAnAssetOrRootIsMalformed`, `anEmptyQueryIsAccepted`, `queryOnAnUnknownPathIsStillNotFound`, `bodyByteOnAStaticGetIsMalformed`, `nonGetMethodsWithAContentTypeAreRefusedWith405`.
4. `anUnsupportedFileStopsTheStartWithAFixedReason` (11 cases: .txt, .json, .png, .map, *.test.js, nested test.js, dotfiles, no extension, uppercase ext, .htm; message equals `StaticAssetCatalog.REJECTED_FILE`, no file name, no `/`), `theCatalogListsOnlyFixtureFiles`, `productionWiringReadsOnlyTheClasspathStaticLocation` (SEC-F-072: system property, environment-style and `spring.web.resources.static-locations` keys do not change the catalog).

## Real-port evidence (production jar, port 8080, curl --path-as-is)
| Request | Status | Content-Type | ETag/Last-Modified/Accept-Ranges | CSP | Body start |
|---|---|---|---|---|---|
| GET / | 200 | text/html;charset=UTF-8 | none | yes | `<!DOCTYPE html>` |
| GET /index.html | 200 | text/html;charset=UTF-8 | none | yes | same |
| GET /index.html + Range, If-None-Match, If-Modified-Since | 200 | text/html;charset=UTF-8 | none | yes | full page |
| /js/, /css/, /favicon.ico, /error, /.git/config, /data/globerbooks.db, /index.html/, //index.html | 404 | application/json;charset=UTF-8 | none | yes | `{"error":"Resource not found.","code":"NOT_FOUND",...}` |
| /index.html?a=1 | 400 | application/json;charset=UTF-8 | none | yes | `MALFORMED_REQUEST` |
| /? and /index.html? | 200 | text/html;charset=UTF-8 | none | yes | page |
| POST /index.html (with Content-Type) | 405 | application/json | none | yes | `METHOD_NOT_ALLOWED` |
| /%2e%2e/etc/passwd, /..;/x | 400 | text/html;charset=utf-8 (Tomcat) | none | no | Tomcat page (allowed by E-15 as pre-application; lacks security headers: container wrapper, Increment 1) |
Fixture-directory start failure: `IllegalStateException: static content: unsupported file in the static location` (test above; no path in the message).

## Gaps and differences (reported, not fixed)
1. HEAD on a static path: 200 (Spring adds HEAD to a GET mapping); OPTIONS with a Content-Type: 200. Spec 5.8: 405 with `Allow: GET`. The 405 envelope has no `Allow` header. Belongs to the Task 15 route table.
2. DELETE, PUT, PATCH without `Content-Type` give 415 (gate stage) before the 405 (Task 08 behaviour; relates to the open E-10 amendment).
3. `/WEB-INF/web.xml`: Tomcat answers 404 with an HTML page, not the envelope (spec E-15 lists it); excluded from my test, belongs to the container error wrapper (deferred). Same wrapper would add security headers to the Tomcat 400 pages.
4. Boot 4.1.2: `NoResourceFoundException` has a three-argument constructor (method, path, reason); no behavioural difference from the plan. `add-mappings: false` alone was enough: no handler for unmapped paths other than my controller `/**` GET mapping (the catch-all is how the controller answers 404 for every other GET path). Tomcat rejects `/%2e%2e/`, `/..;/`, `%2f` and `%00` forms with its own 400 before the application.
5. Added `error/MalformedRequestException` and one advice handler (needed for 400; the controller must not build bodies). Asset bytes are copied in and out (SpotBugs EI_EXPOSE_REP).
6. Handoff 067 carries "Task 09 tests cover a JSON string rating"; it is not in the Task 09 criteria or brief and was not done. Please confirm where it belongs (Task 16 or 30).

## Build
`cd backend && ./mvnw clean verify`: BUILD SUCCESS; unit 234 tests, integration 120 tests (1 skipped, pre-existing in SqliteStoreIntegrationTest), 0 failures; "All coverage checks have been met". Line coverage of new classes (JaCoCo csv): StaticAssetCatalog 29/32 (unreached lines: the IOException branch), StaticAssetCatalog.Asset 6/6, StaticContentController 10/10, MalformedRequestException 2/2; `StaticContentConfig` excluded as `*Config`.

## Git
GIT_PLACEHOLDER
