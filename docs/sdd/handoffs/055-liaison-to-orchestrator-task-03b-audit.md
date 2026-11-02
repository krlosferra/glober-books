# Handoff 055: Liaison audit, Task 03b
- From: liaison  To: orchestrator  Phase: 4  Round: 1  Type: REVIEW_REPORT
- Verdict: **ACCEPT WITH FOLLOW-UPS** (no blocking item). Security verdict is still required before the Sponsor gate.

## Criteria
1. Met. Refusal cases (0.0.0.0, :memory:, ?journal_mode=WAL, bare ?, #, other scheme) run as real context starts, fixed reasons, no URL/address in the message, database file not created; a valid other path starts. Oracle quality is good (unit plus integration, each reason asserted exactly).
2. Met. `application.yml` holds the Tomcat limits (8/2/128/128/5s), `max-swallow-size 16KB`, Hikari `connection-timeout 5000`, and no property that moves the static directory; effective values are read back from the running context and bound beans.
3. Met (rule recorded in progress.md; no code).

## Carried conditions
- SEC-F-103 met (`?` and `#` refused with a fixed reason).
- SEC-F-100 met: suppression is `[\\/]config[\\/]ClockConfig\.java$`, TIME-RULE only; seeded `other/ClockConfig.java` was flagged and the build failed, then removed.
- SEC-F-098 met (progress.md entry).
- SEC-F-099 met (source-scan test).

## Deviations
1. StartupPolicy as `EnvironmentPostProcessor`: ACCEPTED. It matches plan.md start order step 1 ("StartupPolicy, then DataDirectoryGuard"): it runs before any bean or file access, which is stronger than a bean. Boot 4 registration under `org.springframework.boot.EnvironmentPostProcessor` in `META-INF/spring.factories` is the correct 4.x key (the interface moved to `org.springframework.boot`), and the integration tests prove it fires. The extra resource file is not a dependency and is in the task's "StartupPolicy" scope.
2. Datasource wiring left to 34a/34b: ACCEPTED, as the brief puts the guard and provider out of scope.
3. Compact orchestrator-issued brief: ACCEPTED. It listed scope, carried conditions and required evidence and added nothing beyond the spec. Do not make it a precedent for tasks with open design points.

## Follow-ups
1. Task 34a brief must state: `DataDirectoryGuard` runs after `StartupPolicy` (order higher than LOWEST_PRECEDENCE is impossible, so it must be a later post-processor or the provider path), and that StartupPolicy is not the only start path.
2. `StartupPolicyIntegrationTest` prints `EFFECTIVE ...` with `System.out`; harmless, remove when convenient (MINOR).
3. `StartupPolicyTest` file-API scan assumes the working directory is `backend`; fine under Maven, note it if the layout changes (MINOR).
4. Security reviewer verdict on Task 03b is pending; I did not re-read the diff of `SqliteStoreIntegrationTest` line by line and rely on the green build plus the delivery's seeded-run evidence.
5. Remind: update progress.md and status.md audit column; SC-06 values are now ASK FIRST (SEC-F-078).
