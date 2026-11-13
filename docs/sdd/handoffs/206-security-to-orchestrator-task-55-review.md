# 206 Security to orchestrator: Task 55 review

From: security. Type: AUDIT_REPORT. Commit d09e0cb (branch chore/task-55-remove-static-locations). Phase 4, per task (protected surface).

## Scope and method
Read the diff 60a36d4..d09e0cb (application.yml, `MandatoryPropertiesIntegrationTest`, `StartupPolicyIntegrationTest`) and `StaticContentIntegrationTest` (unchanged). Ran `./mvnw -q -DskipTests package` (success; the jar file mtime is 03:01 but its `BOOT-INF/classes/application.yml` has no `static-locations` line and keeps `add-mappings: false`). Started the jar on 127.0.0.1:19431 and 19432 (0700 temp data dir in the scratchpad), probed with curl, stopped both (`pgrep` clean), removed temp files.

## Findings
None new. SEC-F-126 closed.

## Evidence (jar, loopback)
- Catalog files `/`, `/index.html`, `/css/styles.css`, `/js/ui/app.js`: 200 with the full header set (CSP, nosniff, no-store, X-Frame-Options, CORP, COOP, Referrer-Policy, Permissions-Policy, `Content-Disposition: inline;filename=f.txt`).
- Outside the catalog, 404 JSON envelope `NOT_FOUND` (89 bytes, same headers): `/application.yml`, `/static/application.yml`, `/static/index.html`, `/static/`, `/static/css/styles.css`, `/schema.sql`, `/BOOT-INF/classes/application.yml`, a `.class` path, `/css/`, `/js`, `/webjars/`, `/favicon.ico`, `/css/styles.css.map`, `//application.yml`, `/index.html/`, `/%2e/application.yml`, `/static/%2e%2e/application.yml`.
- Container pages (unchanged behavior, R-13): `/META-INF/*` 404 html; `%2e%2e`, `%2f` forms 400 html (431/435 bytes).
- Override attempt on the jar: `SPRING_WEB_RESOURCES_STATIC_LOCATIONS=classpath:/` plus `-Dspring.web.resources.static-locations=classpath:/`: `/application.yml`, `/schema.sql`, `/static/application.yml` still 404 envelope; `/index.html` 200 from the catalog. `add-mappings: false` keeps the handler unregistered whatever the location.
- Startup log shows the Boot welcome-page mapping reading `static/index.html`; it uses the default `classpath:/static/` and was present before (it serves only the catalog's own index, which the catalog already serves). Note, not a finding.
- `StaticContentIntegrationTest.productionWiringReadsOnlyTheClasspathStaticLocation` is unchanged: it sets the property by system property and by a first-priority property source and shows the catalog does not change. Still valid, since the catalog reads a literal.
- Edited tests: the key is now asserted absent (`isNull()`) in both tests, and removed from the expected-property map; `add-mappings=false`, `globerbooks.static.root` null, `log-request-details=false` assertions kept. Only the `WebProperties` import and bean lookup were dropped. No other assertion weakened. A re-introduced key would fail the null assertions (stronger than before for drift).
- Not run: the full test suite (I read the tests, did not execute them; the delivery states the gate result).

## Controls
SC-12 (static resources only from the catalog, no listing): VERIFIED on the jar, evidence above. Headers on static responses (SC headers family): VERIFIED on the jar. No change to bind address, persistence or secrets.

## Residual risks
None added.

## Verdict: PASS
SEC-F-126 closed in `security-log.md`; S-4 row removed from `active-conditions.md` with one line under Closed. No LOW/INFO acceptance used (1 of 5 stays).
