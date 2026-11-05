# Handoff 074: Task 08 delivery (complete)
- From: java
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY

## Resume point
- Done: all steps. Commits: RawHttpClient, TransportSecurityIntegrationTest, progress 0a note, application.yml logging keys, MandatoryPropertiesIntegrationTest, delivery.
- Next: Liaison audit. Nothing pending on the java side.
- `cd backend && ./mvnw clean verify`: BUILD SUCCESS. Unit 234 tests, 0 failures. Integration 72 tests, 0 failures, 1 skipped (in the earlier `SqliteStoreIntegrationTest`, not from Task 08). JaCoCo "All coverage checks have been met"; line coverage 94.9% (375 covered, 20 missed, 26 classes).

## Findings (STOP items)
1. **Task 03b gap, RESOLVED (authorised by the coordinator).** `application.yml` has no `logging.pattern.console` `%replace` and no pinned log levels, though the constitution (Security Standards, Logging) and Task 08 AC5 require the pattern to strip CR, LF, U+2028, U+2029, U+0085 and ESC. All 19 other mandatory properties are present in `application.yml`. Adding the pattern is a new key, not a "wrong key" correction, so it exceeds the brief's scope for `application.yml`. Added to `application.yml`: `logging.level` root, `org.hibernate`, `org.springframework.web`, `org.apache.tomcat` all INFO, and `logging.pattern.console` with `%replace(%m%wEx){'[\r\n\u2028\u2029\u0085\u001B]', '_'}`. No SC-06 value or limit changed. Stack traces become one line.
2. **Spec E-10, first five rows.** Host `127.0.0.1:<port>, evil.example` (one header with a comma) is answered by the container, not the application: status 400 with `text/html`, no `INVALID_HOST` envelope and only 5 headers. Spec says "400 INVALID_HOST (exactly, from the application, for the first five)". R-23 covers it, so the test asserts status 400 and no disclosure only.
3. **Criterion 3, "no HTML".** Container 400 bodies (invalid request line, header over 8 KiB, request line over 8 KiB, two Host headers, comma Host) ARE a full HTML page (`<!doctype html>... HTTP Status 400 - Bad Request`, with a style block). No Tomcat, no version, no `Server` header, no report text (asserted). Reported as the deviation required by carried condition 2.
4. **Criterion 3, `FOO`.** `FOO /api/v1/books` without a content type is answered by the gate with `415 UNSUPPORTED_MEDIA_TYPE` (not in {400, 405, 414, 431, 501}). With `Content-Type: application/json` and `Content-Length: 0` it gives `405 METHOD_NOT_ALLOWED` and the next request is served. Both are tested; the 415 case is named as a deviation.

## Evidence: `TransportSecurityIntegrationTest` (real port, raw socket; no Server header in any row; 8 envelope headers on every application answer)
```
EVIDENCE 500 forced by a database trigger => 500 | Content-Type: application/json;charset=UTF-8 | 5c {"error":"Unexpected error.","code":"INTERNAL_ERROR","timestamp":"2026-11-04T16:10:11.489Z"} 0  
EVIDENCE 200 GET => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE 201 POST => 201 | Content-Type: application/json;charset=UTF-8 | ad {"id":"a7965f91-bbd4-45dc-870c-1da9a43b8605","title":"Dune","author":"Herbert","status":"unread","rating":null,"date_...
EVIDENCE 400 Host => 400 | Content-Type: application/json;charset=UTF-8 | {"error":"The host is not allowed.","code":"INVALID_HOST","timestamp":"2026-11-04T16:10:12.060Z"}
EVIDENCE 400 malformed body => 400 | Content-Type: application/json;charset=UTF-8 | 67 {"error":"The request is malformed.","code":"MALFORMED_REQUEST","timestamp":"2026-11-04T16:10:12.068Z"} 0  
EVIDENCE 403 Origin => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.073Z"}
EVIDENCE 404 unknown path => 404 | Content-Type: application/json;charset=UTF-8 | 59 {"error":"Resource not found.","code":"NOT_FOUND","timestamp":"2026-11-04T16:10:12.082Z"} 0  
EVIDENCE 405 DELETE on books => 405 | Content-Type: application/json;charset=UTF-8 | 62 {"error":"Method not allowed.","code":"METHOD_NOT_ALLOWED","timestamp":"2026-11-04T16:10:12.088Z"} 0  
EVIDENCE 413 chunked 16385 => 413 | Content-Type: application/json;charset=UTF-8 | {"error":"The request body is too large.","code":"PAYLOAD_TOO_LARGE","timestamp":"2026-11-04T16:10:12.093Z"}
EVIDENCE 415 text/plain => 415 | Content-Type: application/json;charset=UTF-8 | {"error":"The content type is not supported.","code":"UNSUPPORTED_MEDIA_TYPE","timestamp":"2026-11-04T16:10:12.097Z"}
EVIDENCE E-10 HTTP/1.0 without Host => 400 | Content-Type: application/json;charset=UTF-8 | {"error":"The host is not allowed.","code":"INVALID_HOST","timestamp":"2026-11-04T16:10:12.147Z"}
EVIDENCE E-10 two Host headers => 400 | Content-Type: text/html;charset=utf-8 | <!doctype html><html lang="en"><head><title>HTTP Status 400 ??? Bad Request</title><style type="text/css">body {font-fam...
EVIDENCE next request => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE E-09 16384 bytes => 201 | Content-Type: application/json;charset=UTF-8 | ad {"id":"7f5b8401-2c1c-45b0-b4bb-c41a7eabf297","title":"Dune","author":"Herbert","status":"unread","rating":null,"date_...
EVIDENCE E-09 16385 bytes => 413 | Content-Type: application/json;charset=UTF-8 | {"error":"The request body is too large.","code":"PAYLOAD_TOO_LARGE","timestamp":"2026-11-04T16:10:12.190Z"}
EVIDENCE E-09 chunked 16385 => 413 | Content-Type: application/json;charset=UTF-8 | {"error":"The request body is too large.","code":"PAYLOAD_TOO_LARGE","timestamp":"2026-11-04T16:10:12.193Z"}
EVIDENCE E-09 16385 to unknown path => 413 | Content-Type: application/json;charset=UTF-8 | {"error":"The request body is too large.","code":"PAYLOAD_TOO_LARGE","timestamp":"2026-11-04T16:10:12.195Z"}
EVIDENCE next request => 200 | Content-Type: application/json;charset=UTF-8 | c3 {"books":[{"id":"7f5b8401-2c1c-45b0-b4bb-c41a7eabf297","title":"Dune","author":"Herbert","status":"unread","rating":n...
EVIDENCE invalid request line => 400 | Content-Type: text/html;charset=utf-8 | <!doctype html><html lang="en"><head><title>HTTP Status 400 ??? Bad Request</title><style type="text/css">body {font-fam...
EVIDENCE invalid request line body contains HTML tag: true
EVIDENCE next request after invalid request line => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE method FOO with JSON content type => 405 | Content-Type: application/json;charset=UTF-8 | 62 {"error":"Method not allowed.","code":"METHOD_NOT_ALLOWED","timestamp":"2026-11-04T16:10:12.242Z"} 0  
EVIDENCE method FOO with JSON content type body contains HTML tag: false
EVIDENCE next request after method FOO with JSON content type => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE header over 8 KiB => 400 | Content-Type: text/html;charset=utf-8 | <!doctype html><html lang="en"><head><title>HTTP Status 400 ??? Bad Request</title><style type="text/css">body {font-fam...
EVIDENCE header over 8 KiB body contains HTML tag: true
EVIDENCE next request after header over 8 KiB => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE request line over 8 KiB => 400 | Content-Type: text/html;charset=utf-8 | <!doctype html><html lang="en"><head><title>HTTP Status 400 ??? Bad Request</title><style type="text/css">body {font-fam...
EVIDENCE request line over 8 KiB body contains HTML tag: true
EVIDENCE next request after request line over 8 KiB => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE E-04 OPTIONS evil origin => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.282Z"}
EVIDENCE E-11 Host LOCALHOST:41967 => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE E-11 Host 127.0.0.1:41967 => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE E-11 Host localhost:41967 => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE E-13 Origin: http://localhost:41967 => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE E-13 Origin: http://127.0.0.1:41967 => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE E-13 Sec-Fetch-Site: same-origin => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE E-13 Sec-Fetch-Site: none => 200 | Content-Type: application/json;charset=UTF-8 | 16 {"books":[],"total":0} 0  
EVIDENCE GET books Host evil.example => 400 | Content-Type: application/json;charset=UTF-8 | {"error":"The host is not allowed.","code":"INVALID_HOST","timestamp":"2026-11-04T16:10:12.340Z"}
EVIDENCE POST books Host evil.example => 400 | Content-Type: application/json;charset=UTF-8 | {"error":"The host is not allowed.","code":"INVALID_HOST","timestamp":"2026-11-04T16:10:12.343Z"}
EVIDENCE GET books Host localhost => 400 | Content-Type: application/json;charset=UTF-8 | {"error":"The host is not allowed.","code":"INVALID_HOST","timestamp":"2026-11-04T16:10:12.346Z"}
EVIDENCE POST books Host localhost => 400 | Content-Type: application/json;charset=UTF-8 | {"error":"The host is not allowed.","code":"INVALID_HOST","timestamp":"2026-11-04T16:10:12.348Z"}
EVIDENCE GET books Host localhost:1 => 400 | Content-Type: application/json;charset=UTF-8 | {"error":"The host is not allowed.","code":"INVALID_HOST","timestamp":"2026-11-04T16:10:12.350Z"}
EVIDENCE POST books Host localhost:1 => 400 | Content-Type: application/json;charset=UTF-8 | {"error":"The host is not allowed.","code":"INVALID_HOST","timestamp":"2026-11-04T16:10:12.352Z"}
EVIDENCE GET books Host [::1]:41967 => 400 | Content-Type: application/json;charset=UTF-8 | {"error":"The host is not allowed.","code":"INVALID_HOST","timestamp":"2026-11-04T16:10:12.355Z"}
EVIDENCE POST books Host [::1]:41967 => 400 | Content-Type: application/json;charset=UTF-8 | {"error":"The host is not allowed.","code":"INVALID_HOST","timestamp":"2026-11-04T16:10:12.358Z"}
EVIDENCE GET books Origin http://evil.example => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.359Z"}
EVIDENCE POST books Origin http://evil.example => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.361Z"}
EVIDENCE GET books Origin null => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.365Z"}
EVIDENCE POST books Origin null => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.367Z"}
EVIDENCE GET books Origin http://localhost:1 => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.373Z"}
EVIDENCE POST books Origin http://localhost:1 => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.376Z"}
EVIDENCE GET books Sec-Fetch-Site cross-site => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.379Z"}
EVIDENCE POST books Sec-Fetch-Site cross-site => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.382Z"}
EVIDENCE GET books Sec-Fetch-Site same-site => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.384Z"}
EVIDENCE POST books Sec-Fetch-Site same-site => 403 | Content-Type: application/json;charset=UTF-8 | {"error":"The request is not allowed.","code":"FORBIDDEN","timestamp":"2026-11-04T16:10:12.387Z"}
EVIDENCE POST books Content-Type: text/plain => 415 | Content-Type: application/json;charset=UTF-8 | {"error":"The content type is not supported.","code":"UNSUPPORTED_MEDIA_TYPE","timestamp":"2026-11-04T16:10:12.389Z"}
EVIDENCE POST books Content-Type: application/jsonx => 415 | Content-Type: application/json;charset=UTF-8 | {"error":"The content type is not supported.","code":"UNSUPPORTED_MEDIA_TYPE","timestamp":"2026-11-04T16:10:12.392Z"}
EVIDENCE POST books Content-Type: application/json; charset=ISO-8859-1 => 415 | Content-Type: application/json;charset=UTF-8 | {"error":"The content type is not supported.","code":"UNSUPPORTED_MEDIA_TYPE","timestamp":"2026-11-04T16:10:12.394Z"}
EVIDENCE POST books no content type => 415 | Content-Type: application/json;charset=UTF-8 | {"error":"The content type is not supported.","code":"UNSUPPORTED_MEDIA_TYPE","timestamp":"2026-11-04T16:10:12.395Z"}
EVIDENCE E-10 one Host header 127.0.0.1:port, evil.example (container answer, R-23) => 400 | Content-Type: text/html;charset=utf-8 | <!doctype html><html lang="en"><head><title>HTTP Status 400 ??? Bad Request</title><style type="text/css">body {font-fam...
EVIDENCE POST books charset=utf-8 => 201 | Content-Type: application/json;charset=UTF-8 | ad {"id":"cf85f608-9262-477b-ab62-94791c5e6e64","title":"Dune","author":"Herbert","status":"unread","rating":null,"date_...
EVIDENCE method FOO without content type (gate answer, reported deviation) => 415 | Content-Type: application/json;charset=UTF-8 | {"error":"The content type is not supported.","code":"UNSUPPORTED_MEDIA_TYPE","timestamp":"2026-11-04T16:10:12.430Z"}
EVIDENCE round trip POST => 201 | Content-Type: application/json;charset=UTF-8 | cc {"id":"8f118d79-b983-4d6a-9bbd-075d7fd2b15a","title":"Zanzibar Secret Title","author":"Quentin Hidden Author","status...
EVIDENCE round trip GET => 200 | Content-Type: application/json;charset=UTF-8 | e2 {"books":[{"id":"8f118d79-b983-4d6a-9bbd-075d7fd2b15a","title":"Zanzibar Secret Title","author":"Quentin Hidden Autho...
EVIDENCE checkpoint log lines: 47, book data in logs: false
```

Criterion 1 (envelope and no `Server`, `Set-Cookie`, `Location`, `Access-Control-*`) is asserted on 200, 201, 400 Host, 400 malformed body, 403, 404, 405, 413, 415 and 500 (trigger `boom BEFORE INSERT ON books`, dropped in `finally`, body has no detail). Carried condition 1 rows (forged Host, bad Origin, cross-site, same-site, wrong or missing Content-Type on POST and GET `/api/v1/books`) are in the table. Checkpoint logs: 47 lines, no book data (the test's own `EVIDENCE` lines are excluded from the log text).

## Evidence: `MandatoryPropertiesIntegrationTest` (15 tests, real output)
```
2026-11-04T16:13:51.881Z  WARN [main] proof.sanitisation : MSG-A first__FORGED-LINE_two_three_four_five end
2026-11-04T16:13:51.881Z ERROR [main] proof.sanitisation : MSG-B_java.lang.IllegalStateException: cause one_FORGED-CAUSE_end_	at com.globant.globerbooks.MandatoryPropertiesIntegrationTest.logPatternNeutralizesLineBreaksSeparatorsA
PROOF 9000-byte header over HTTP => 400
PROOF GET /error => 404 59 {"error":"Resource not found.","code":"NOT_FOUND","timestamp":"2026-11-04T16:13:51.860Z"} 0  
PROOF forwarded headers over HTTP => 200
PROOF logging.level ROOT => INFO
PROOF logging.level org.apache.tomcat => INFO
PROOF logging.level org.hibernate => INFO
PROOF logging.level org.springframework.web => INFO
PROOF logging.pattern.console exception => 2026-11-04T16:13:51.881Z ERROR [main] proof.sanitisation : MSG-B_java.lang.IllegalStateException: cause one_FORGED-CAUSE_end_	at com.globant.globerbooks.Mandato
PROOF logging.pattern.console message => 2026-11-04T16:13:51.881Z  WARN [main] proof.sanitisation : MSG-A first__FORGED-LINE_two_three_four_five end
PROOF server.address => /127.0.0.1
PROOF server.error.whitelabel.enabled and ErrorMvcAutoConfiguration excluded => basicErrorController present=false
PROOF server.forward-headers-strategy => NONE
PROOF server.max-http-request-header-size => 8192B
PROOF server.tomcat.accesslog.enabled => false
PROOF server.tomcat.connection-timeout => PT5S
PROOF spring.datasource.hikari.connection-timeout => 5000
PROOF spring.datasource.hikari.maximum-pool-size => 1
PROOF spring.jackson.deserialization.fail-on-unknown-properties => true
PROOF spring.jackson.property-naming-strategy => {"date_added":"x"}
PROOF spring.jpa.hibernate.ddl-auto => validate
PROOF spring.jpa.open-in-view => interceptor bean present=false
PROOF spring.jpa.show-sql => JpaProperties=false, hibernate.show_sql=null
PROOF spring.mvc.hiddenmethod / formcontent / spring.servlet.multipart => hidden=false form=false multipart=false
PROOF spring.mvc.log-request-details => false
PROOF spring.sql.init.mode => books table created from schema.sql in a new file, count=1
PROOF spring.web.resources.static-locations => classpath:/static/
PROOF unknown field over HTTP => 400 67 {"error":"The request is malformed.","code":"MALFORMED_REQUEST","timestamp":"2026-11-04T16:13:51.197Z"} 0  
```

## Evidence: git (final)
```
 backend/src/main/resources/application.yml         |   9 +
 .../MandatoryPropertiesIntegrationTest.java        | 254 ++++++++++++++++++++
 .../TransportSecurityIntegrationTest.java          | 258 +++++++++++++++++++++
 .../globant/globerbooks/support/RawHttpClient.java | 130 +++++++++++
 .../073-orchestrator-to-java-task-08-brief.md      |  24 ++
 .../074-java-to-liaison-task-08-delivery.md        | 122 ++++++++++
 docs/sdd/progress.md                               |  14 ++
 docs/sdd/status.md                                 |   5 +-
 8 files changed, 814 insertions(+), 2 deletions(-)
krlosferra <carlos.ferra@globant.com>
docs(sdd): complete the Task 08 delivery [Task-8]
---
krlosferra <carlos.ferra@globant.com>
test(config): prove every mandatory property is effective [Task-8]
---
krlosferra <carlos.ferra@globant.com>
fix(config): add log sanitising pattern and pinned log levels [Task-8]
---
krlosferra <carlos.ferra@globant.com>
docs(sdd): add the partial Task 08 delivery [Task-8]
---
krlosferra <carlos.ferra@globant.com>
docs(sdd): add the 0a note to progress [Task-8]
---
krlosferra <carlos.ferra@globant.com>
test(transport): add real-port transport security baseline [Task-8]
---
krlosferra <carlos.ferra@globant.com>
test(support): add raw-socket HTTP client [Task-8]
---
krlosferra <carlos.ferra@globant.com>
docs(sdd): add the Task 08 brief and update the resume point [Task-8]
---
```
