# Handoff 054: Task 03b delivery
- From: java  To: liaison  Phase: 4  Round: 1  Type: TASK_DELIVERY
- Branch: `feature/task-03b-start-policy-config` (local commits only, not pushed); list: `git log main..HEAD`

## Files
Created: `config/sqlite/StartupPolicy.java`, `src/main/resources/application.yml`, `src/main/resources/META-INF/spring.factories` (one line, registers the post-processor), `StartupPolicyTest`, `StartupPolicyIntegrationTest`. Modified: `backend/config/checkstyle/suppressions.xml` (ClockConfig suppression only), `SqliteStoreIntegrationTest` (clock test compares relative path), `docs/sdd/progress.md`, `status.md` row.

## Criteria
1. Refusals: StartupPolicyTest (loopback literals, 0.0.0.0, :memory:, ?, #, other scheme, file:/resource:, control chars, fixed reasons without URL/address) and StartupPolicyIntegrationTest.refusesToStartWithAFixedReason (6 real context starts: 0.0.0.0, :memory:, ?journal_mode=WAL, bare ?, #frag, jdbc:h2; database file not created); startsWithAnotherDatabasePath (valid other path starts).
2. effectiveConfigurationMatchesTheMandatoryProperties reads every property back from the running context plus bound TomcatServerProperties/ServerProperties/HikariDataSource; definesNoPropertyThatChangesTheStaticCatalogDirectory.
3. Recorded in progress.md; no code.
Carried: SEC-F-103 (? and # refused, fixed reason; exception logs checked: no URL), SEC-F-100 (suppression scoped; seeded second `other/ClockConfig.java` was flagged: "TIME-RULE: wall-clock calls are banned" and the build failed; seed removed), SEC-F-098 (progress.md), SEC-F-099 (test `mainSourcesOutsideConfigSqliteDoNotUseFileApis`).

## Evidence (`cd backend && ./mvnw clean verify`: BUILD SUCCESS, 0 Checkstyle violations, SpotBugs done, "All coverage checks have been met")
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0 [INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 1 
Refusal reasons (fixed): "Start refused: the bind address must be a loopback address." / "...datasource URL must be jdbc:sqlite:<file>." / "...must not carry parameters or a fragment."
Effective values read from the running context:
- spring.jpa.hibernate.ddl-auto = validate
- spring.jpa.open-in-view = false
- spring.jpa.show-sql = false
- spring.sql.init.mode = always
- spring.jackson.property-naming-strategy = SNAKE_CASE
- spring.jackson.deserialization.fail-on-unknown-properties = true
- spring.datasource.hikari.maximum-pool-size = 1
- spring.datasource.hikari.connection-timeout = 5000
- server.address = 127.0.0.1
- server.forward-headers-strategy = none
- server.max-http-request-header-size = 8KB
- server.tomcat.connection-timeout = 5s
- server.tomcat.accesslog.enabled = false
- server.error.whitelabel.enabled = false
- spring.mvc.hiddenmethod.filter.enabled = false
- spring.mvc.formcontent.filter.enabled = false
- spring.servlet.multipart.enabled = false
- spring.web.resources.static-locations = classpath:/static/
- spring.mvc.log-request-details = false
- server.tomcat.threads.max = 8
- server.tomcat.threads.min-spare = 2
- server.tomcat.max-connections = 128
- server.tomcat.accept-count = 128
- server.tomcat.max-swallow-size = 16KB
Bound beans: threads 8/2, max-connections 128, accept-count 128, connection-timeout 5 s, max-swallow-size 16 KiB, header 8 KiB, Hikari pool 1 / 5000.
`git diff --stat main..HEAD` and `git log main..HEAD --format='%an <%ae>%n%B---'` were run: all commits by krlosferra, subjects end with [Task-3b], no attribution.

## Deviations
- StartupPolicy is an EnvironmentPostProcessor (not a bean) registered in spring.factories (an extra resource file): a bean ran after Boot's own datasource initializer had already created the database file. This makes the refusal happen before any bean or file access.
- All keys in the constitution list were recognised by Boot 4.1.2 (effective values read back). `max-swallow-size` is written `16KB` (= 16384 bytes).
- `SqliteDataSourceProvider` is not yet the application DataSource (Tasks 34a/34b); the valid-path test pre-creates the directory.
- No new dependency.

## Resume point
All steps done and committed. Next: Liaison audit; orchestrator pushes.
