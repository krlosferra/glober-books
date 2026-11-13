# Handoff 203: Task brief, Task 55 (remove the inert `static-locations` property)
- From: orchestrator (direct dispatch, constitution 1.7)
- To: java-architect
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 55 of `docs/sdd/tasks.md` (v0.6.8): the task block is the specification (criteria 1 to 3, traces SEC-F-126, SC-12, constitution 1.8). Branch `chore/task-55-remove-static-locations` (checked out from main after PR 56). Commits tagged `[Task-55]`. Protected surface (`application.yml`): reviewed alone. The constitution amendment 1.8 (the property leaves the mandatory-properties list) is already applied by the orchestrator in the working tree and committed with this brief; do not edit `constitution.md`. Path rule: `docs/sdd/README.md` section 5; the Maven project root is `backend/`.

## Read (by ID, not whole files)
`security-log.md` SEC-F-126 (by ID); the three tests named in the task (`StartupPolicyIntegrationTest`, `MandatoryPropertiesIntegrationTest`, `StaticContentIntegrationTest`, under `backend/src/test/java/com/globant/globerbooks/`; find them with `Glob`); `backend/src/main/resources/application.yml` (the `spring.web.resources` block); the code that serves the static catalog (find where the literal classpath location is used with `Grep`; it must not depend on the property). No other documents.

## Open items assigned
- **SEC-F-126 (LOW):** the key is inert (`add-mappings: false`, literal catalog location); remove it from `application.yml`. Keep `add-mappings: false`.
- **Standing rules:** SEC-F-159 (slice tests do not load the web configuration: prove behaviour in full-context tests) and SEC-F-180 (logging levels untouched).

## Scope
`backend/src/main/resources/application.yml` (remove the one key and, if `resources:` is left with only `add-mappings`, keep that key unchanged) and the three tests (edit). `StaticContentIntegrationTest` currently overrides the property to prove it is inert: keep a proof that the frontend is still served from the literal catalog location and that setting the property does not change what is served, if still meaningful (the same test can set it and assert the response is identical); otherwise remove that case and justify it in the delivery. `MandatoryPropertiesIntegrationTest` and `StartupPolicyIntegrationTest` must no longer expect the key as mandatory (and, if the second pins the effective environment, expect it absent or unchanged by default). No other production file, no `pom.xml`, no new dependency, no frontend change.

## Binding constraints
1. The behaviour of the static catalog is unchanged: the same files served with the same headers; `404`/`405` for non-catalog paths unchanged.
2. Run the focused tests first, then `cd backend && ./mvnw verify` once (it takes several minutes; run nothing else at the same time). Rebuild nothing else. Report the summary lines of the build (tests run, failures, errors, skipped, exit code).
3. Plain evidence, compact delivery; never use `--no-verify`.

## Delivery (compact, constitution 1.7)
Write `docs/sdd/handoffs/204-java-to-liaison-task-55-delivery.md` early with a "Resume point": task, branch, commit hashes; files edited; a table mapping each criterion to its test; summary lines of the focused tests and of `./mvnw verify` (tests, failures, errors, skipped, exit code); deviations; risks. No raw terminal output, no `git diff --stat`, no author log. Commit locally with explicit paths and `type(scope): subject [Task-55]`, nothing naming a tool, model or vendor, no trailers. Update `docs/sdd/progress.md`.

## Rules
STOP and report on any spec gap or contradiction, any need for a dependency, any change outside the listed files, or any behaviour that differs from the contract. Do not push.
