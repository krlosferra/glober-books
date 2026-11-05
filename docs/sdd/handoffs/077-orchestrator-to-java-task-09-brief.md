# Handoff 077: Task brief, Task 09 (static content, opens 0b)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 09 of `docs/sdd/tasks.md` (v0.5.1): read its text, criteria 1 to 4 and traces. Branch `feature/task-09-static-content` is checked out; Tasks 01 to 08, 10, 11 are merged (tag `v0.1` exists). Read also: `spec.md` 5.8 (static content), E-15, E-16, E-30, E-39 rows of Appendix D, BR-011 note on paths; `plan.md` D4 (own static controller, Spring's resource handler off, catalog from `classpath:/static/`, extensions `.html .css .js` only, `/` equals `/index.html`, no `ETag`, `Last-Modified`, `Accept-Ranges`; `Range` and conditionals ignored); `plan-appendix.md` AP-1 (StaticAssetCatalog, StaticContentController), AP-2 stage 8 (a non-empty query on an asset or on `/` is 400 `MALFORMED_REQUEST`; `/index.html?` with an empty query is 200; a body byte on a static GET is 400); `security.md` SC-12, SEC-F-072, SEC-F-098 note (not relevant here); the existing `config/transport/RequestGateFilter` and `error/*` which you reuse.

## Scope
`config/transport/StaticAssetCatalog` (constructor argument for the resource location; no property, no profile; fixed at start; any file under the location with an extension other than `.html`, `.css`, `.js`, any `*.test.js`, `*.map` or dotfile stops the start with a fixed reason), `controller/StaticContentController` (serves catalog entries; `/` equals `/index.html`; everything else 404 `NOT_FOUND` in the envelope; content types `text/html;charset=UTF-8`, `text/css;charset=UTF-8`, `text/javascript;charset=UTF-8`), production wiring that passes `classpath:/static/` only (a small config class is allowed and counts as wiring; excluded from JaCoCo like the other `*Config` classes), `application.yml`: set `spring.web.resources.add-mappings: false` (report it), and `StaticContentIntegrationTest` (builds the catalog directly with a fixture directory under `@TempDir`; no property, no profile). The placeholder `frontend/index.html` (Task 01) is the production fixture today: do not edit `frontend/`. Out of scope: route table (Task 15), `Accept` (15), query grammar for the API (16), the page itself (Task 12).

## Binding constraints
1. No production property, profile or environment variable may change which directory the catalog reads (SEC-F-072): prove it by a test that sets plausible keys (`spring.web.resources.static-locations`, a custom key) and shows the catalog unchanged.
2. Responses carry the constitution security headers (the gate sets them) and no `ETag`, `Last-Modified`, `Accept-Ranges`; with `Range: bytes=0-1`, `If-None-Match` and `If-Modified-Since` the answer is `200` with the full body.
3. 404 (envelope, JSON) for: directories (`/js/`, `/css/`), `/favicon.ico`, a `*.test.js` path, `/error`, `/.git/config`, `/data/globerbooks.db`, `/index.html/`, double slashes and encoded dot-segment traversal forms (`/%2e%2e/...`, `/..;/`). A query on an asset or `/` is 400 `MALFORMED_REQUEST` (empty query after `?` is 200); a body byte on a static GET is 400.
4. `HEAD` and other methods on static paths follow the existing method rules (405 with the envelope); do not invent new behaviour: report any gap in the spec.
5. No new dependency; no change under `backend/config/**` or `pom.xml`.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; a table of requests with real status, content type, relevant headers and body for criteria 1 to 3 (use the real port); the fixture-directory start-failure case (a `.txt` file stops the start with the fixed reason, no path in the message); real line coverage of the new classes; `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'` (paste both).

## Rules
STOP and report on any Boot 4.1.2 behaviour that differs from the plan or any spec gap. Commit after each finished step (`type(scope): subject [Task-9]`), local commits only, explicit paths, no AI attribution, never `--no-verify`. Update `docs/sdd/progress.md` in the same change. Write the delivery as `docs/sdd/handoffs/078-java-to-liaison-task-09-delivery.md` early with a "Resume point" section (done / next) kept updated so nothing is lost if you are cut off.
