# Handoff 053: Task brief, Task 03b (start policy and configuration)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits the delivery against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 03b of `docs/sdd/tasks.md` (v0.5.1): read the task text, its acceptance criteria 1 to 3 and its traces there. Branch `feature/task-03b-start-policy-config` is checked out. Tasks 01, 02 and 03a are merged. Read also: `plan.md` section 2 (start order, SQLite settings, limits), `constitution.md` Persistence and mandatory properties, `security.md` SC-01, SC-06, SC-39, SC-32, and SEC-F-067, 072, 073, 078, 098, 100, 103.

## Scope
`config/sqlite/StartupPolicy.java`, `backend/src/main/resources/application.yml`, tests `StartupPolicyTest` and `StartupPolicyIntegrationTest`. Out of scope: directory guard and stored-library validation (Task 34a/34b), error foundation, the gate, any controller.

## Carried conditions (from the 03a reviews; must be in this task)
1. SEC-F-103: `StartupPolicy` rejects `?` and `#` in the datasource URL (and any parameters); the failure reason is a fixed string with no URL, path or book data.
2. SEC-F-100: tighten the `ClockConfig` suppression in `backend/config/checkstyle/suppressions.xml` to the `config` package path (`[\\/]config[\\/]ClockConfig\.java$`) and make the clock test compare the relative path; seed a second `ClockConfig.java` in another package to prove it is flagged, then revert. This is the only edit allowed under `backend/config/**`.
3. SEC-F-098: record in `progress.md` that the `DataDirectoryGuard` of Task 34a must run before the provider and that the provider is not the sole start path.
4. SEC-F-099 (optional, cheap): a source-scan test that fails if `src/main` outside `config/sqlite` uses `java.nio.file.Files`, `FileInputStream` or `FileChannel`.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; one run per refusal case of criterion 1 (non-loopback address, `:memory:`, `?journal_mode=WAL`, other scheme, `?` and `#`) with the fixed reason; a valid URL with another path starts; the effective value of every property in `application.yml` read back from the running context; the seeded `ClockConfig` run; `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'`.

## Rules
STOP and report on any property key that Spring Boot 4.1.2 does not recognise or any behaviour that differs from the plan (SC-06 values are ASK FIRST). Commit after each finished step (`type(scope): subject [Task-3b]`), local commits only, add files by explicit path, no AI attribution anywhere, never `--no-verify`. The orchestrator pushes. Update `docs/sdd/progress.md` in the same change. Write the delivery as `docs/sdd/handoffs/054-java-to-liaison-task-03b-delivery.md`. If you are interrupted, the last commit and a short "Resume point" section at the end of the delivery file (what is done, what is next) must let another session continue.
