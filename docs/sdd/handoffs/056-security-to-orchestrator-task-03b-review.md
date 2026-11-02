# Handoff 056: Security review of Task 03b
- From: security  To: orchestrator  Phase: 4  Round: 1  Type: AUDIT_REPORT
- Scope: branch `feature/task-03b-start-policy-config`; handoff 054, `StartupPolicy.java`, `application.yml`, `META-INF/spring.factories`, `StartupPolicyTest`, `StartupPolicyIntegrationTest` (read), `suppressions.xml` diff.
- Method: code and config read; `./mvnw -o test -Dtest=StartupPolicyTest` run (passed, quiet). Integration refusal cases not re-run by me; I relied on the delivery log (6 real context starts) and on the code path. The full build was not re-run.

## Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-105 | LOW | Config | StartupPolicy.postProcessEnvironment | Only `spring.datasource.url` is checked. Alternative sources (`spring.datasource.hikari.jdbc-url`, `data-source-class-name`, `jndi-name`) are not. | Code reads two keys only | An operator or environment sets a Hikari key that wins over the checked URL. Needs control of the environment, so low. | Refuse any of those keys when present, or state in Task 34b that the app builds the DataSource itself from the checked URL (then the keys are inert). | SC-39 |
| SEC-F-106 | LOW | SC-01 | StartupPolicy.isLiteral | The IPv4 regex accepts `999.999.999.999`. `InetAddress.getByName` then treats it as a hostname and may do a DNS lookup. The result is a refusal unless DNS returns loopback. | Regex `\d{1,3}(\.\d{1,3}){3}`; JDK parsing fallback | Config-controlled only; a DNS answer of 127.0.0.1 would pass a non-literal. | Check each octet is 0..255 before `getByName`, or use a parse that never resolves. | SC-01 |
| SEC-F-107 | INFO | SC-01 | same | `::ffff:127.0.0.1` is accepted (mapped loopback; it is loopback). `127.1`, `localhost`, hostnames, `[::1]` and a scope id are refused (fail closed). | Regexes plus `isLoopbackAddress` | None | None | SC-01 |

## Controls checked
- SC-01: loopback check by literal only, in code, VERIFIED apart from SEC-F-106. `server.address: 127.0.0.1` in the yml. Null is refused.
- SC-06 values: threads 8/2, max-connections 128, accept-count 128, connection-timeout 5s, max-swallow-size 16KB, header 8KB, Hikari 5000 and pool 1, multipart off, form and hidden-method filters off, all match plan D5 and SC-06. VERIFIED in the yml and read back from the running context (054). The runtime behavior (413, slow body) stays in Tasks 16 and 17.
- SC-32 and SC-39: fixed form, `?`, `#`, `:memory:` (leading colon), `file:` and `resource:` (case-insensitive), blank, control characters (<0x20 and 0x7f) refused. Three constant reasons, with no URL, path or address. VERIFIED. Case: `JDBC:SQLITE:` is refused (fail closed).
- Refusal before any bean or file access: `EnvironmentPostProcessor` at LOWEST_PRECEDENCE, registered through `spring.factories`, so it runs after the config files are loaded and before the context. VERIFIED by code. The delivery reports the database file was not created.
- Mandatory properties: all in the constitution list present, including `forward-headers-strategy: none`, `whitelabel` off, `accesslog` off, `static-locations: classpath:/static/`, `log-request-details: false`, and `show-sql`/`open-in-view` off. VERIFIED. The `max-swallow-size` key name was proved by the read-back.
- SEC-F-072: no property moves the static directory (test `definesNoPropertyThatChangesTheStaticCatalogDirectory`). The yml only has the fixed value. CLOSED.

## Carried findings
- SEC-F-103: `?` and `#` refused with a fixed reason, no cause text. CLOSED.
- SEC-F-100: suppression anchored to `[\\/]config[\\/]ClockConfig\.java$`, message-scoped to `^TIME-RULE:`. The seeded `other/ClockConfig.java` failed the build (per 054); the test asserts `config/ClockConfig.java`. CLOSED.
- SEC-F-099: source-scan test `mainSourcesOutsideConfigSqliteDoNotUseFileApis` exists. CLOSED (I read that it exists; I did not review its patterns in depth).
- SEC-F-098: still OPEN, conditional on Task 34a (the guard before the provider). Recorded in progress.md. Not live while the provider is unwired.

## Not verified
The integration refusal run by me; the full build and coverage; the runtime limit behavior; advisory databases (not touched by this task).

## Residual risks
None new for the Sponsor. SEC-F-105 and 106 are LOW and are fixed in Task 34b (105) or at the next touch of this class (106).

## Verdict: PASS
No CRITICAL or HIGH finding. Next step: Liaison audit, then the gate. Track SEC-F-105 to 107 and keep SEC-F-098 OPEN until Task 34a.

## Appendix: exact text for security.md (not edited by me)
Findings log rows:
| SEC-F-105 | LOW | Start policy checks only `spring.datasource.url`; Hikari `jdbc-url`, `data-source-class-name`, `jndi-name` are not checked | OPEN, refuse these keys or make them inert in Task 34b | 056 |
| SEC-F-106 | LOW | IPv4 regex accepts out-of-range octets; `getByName` may resolve them as a hostname | OPEN, check octets 0..255 first | 056 |
| SEC-F-107 | INFO | `::ffff:127.0.0.1` accepted (mapped loopback); `127.1`, hostnames, `[::1]` refused | INFO | 056 |
Also set CLOSED (056): SEC-F-099, SEC-F-100, SEC-F-103. SEC-F-072 stays CLOSED with code evidence. SEC-F-098 stays OPEN (Task 34a).
Control rows: SC-01 `PARTIAL (Task 03b verified 2026-11-02, handoff 056: literal loopback only, yml address; runtime bind test in Phase 4 later)`. SC-06 `PARTIAL (configuration values verified in Task 03b, handoff 056; behavior tests pending Tasks 16 and 17)`. SC-39 `PARTIAL (Task 03b verified: URL form, refusal reasons; wiring pending Tasks 34a and 34b)`.
Changelog: `0.24 (2026-11-02): Task 03b review (handoff 056): SEC-F-099, 100, 103 closed; SEC-F-105 to 107 added (2 LOW, 1 INFO); verdict PASS.`
