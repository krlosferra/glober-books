# Handoff 119: Security review, Task 20 (validation wiring and field-error mapping)
- From: security
- To: orchestrator
- Type: AUDIT_REPORT
- Branch: feature/task-20-validation (base d23be69, head 7f24ac2)

## 1. Scope and method
Read: brief 116, delivery 117, security.md (SC-16, SC-02, SC-11, SC-29, SC-31), and in full `BookText`, `ValidatorConfig`, `CreateBookRequest`, `GlobalExceptionHandler`, `BookService`, `BookValidationException`, `BookValidationIntegrationTest`, the T-13 tests of `BookTextTest`. Ran (read-only): `./mvnw -o dependency:tree`; `git diff --stat` on `pom.xml` and `src/main/resources` (no change); git log author/trailer grep (human identity, no attribution). Probe: a scratch copy of the repository (not the working tree) with one extra test started the full context on a random port with a temporary SQLite database and sent 9 hostile bodies. I did not rerun the full `clean verify`; the delivery figures are the implementer's claim.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-155 | LOW | Dependencies / EL | `pom.xml` via `spring-boot-starter-validation` | `tomcat-embed-el` 11.0.27 is on the classpath (dependency:tree line 46). EL is not reachable today (literal interpolator, constant templates) | dependency:tree; code read | A future constraint with user text in a template, or a second validator built with `Validation.buildDefaultValidatorFactory()`, would evaluate `${...}` | C-1: add a full-context test that the `Validator` bean returns a `${1+1}` template unchanged. Later, ASK FIRST: exclude the artifact | SC-16 |
| SEC-F-156 | LOW | Test oracle | `BookValidationIntegrationTest.hostileTitle...` | Asserts the whole body lacks `"49"`, but the body carries `timestamp` (my probe bodies show `...24.849Z`, `...24.897Z`), so the test fails randomly | Probe output | Flaky gate hides real failures or trains people to rerun | C-2: assert on the `details` messages or strip the timestamp; keep `doesNotContain("${7*7}")` | SC-16 |
| SEC-F-122 (update) | LOW | Deserialization | Jackson string coercion | Confirmed: `{"title":5,"author":"b"}` gives 201 and stores "5"; `"status":5` and `true` give INVALID_STATUS (not MALFORMED_REQUEST) | Probe | Type confusion, spec/SC-31 drift | Task 21 (no scalar coercion) | SC-31 |
| SEC-F-157 | INFO | Stored text | BR-006 | `${7*7}` or `{title}` inside limits is valid and stored verbatim | Probe (`author` `{title}` accepted) | None server-side; UI must encode | SC-09 | SC-09 |
| SEC-F-158 | INFO | Logging | advice | `MethodArgumentNotValidException.getMessage()` holds rejected values; never logged now; `org.springframework.web` is INFO | Code read, application.yml | A DEBUG level would log request text | Keep INFO | SC-14 |
No CRITICAL, HIGH or MEDIUM.

## 3. Controls checked
- No EL evaluation: VERIFIED with the SEC-F-155 caveat. `LiteralMessageInterpolator` returns the template unchanged for both overloads; `LocalValidatorFactoryBean` is the bean in the full context (probe); `BookText.Validator` uses `buildConstraintViolationWithTemplate` only with `messageFor`, constants plus the integer limit. `@Pattern` message is a constant. No `ScriptAssert`, `buildDefaultValidatorFactory` or `ResourceBundleMessageInterpolator` in `src/main`. Brief item "no `jakarta.el`": no `jakarta.el` coordinate, but `tomcat-embed-el` is present (C-1).
- No user text in messages, logs or exception messages: VERIFIED. `BookValidationException` message is `code.name()`; the advice maps without logging; the catch-all logs class and top frame; new code has no logger. Probe bodies with `${7*7}`, `${1+1}`, `{x}`, `{title}` and NUL echo nothing in responses.
- Handler allowlist: VERIFIED. Six fixed messages in a `Set`; null or any other message becomes "is invalid"; unit test present per delivery. Field names come from record components, not input.
- StatusDeserializer: VERIFIED as safe. Absent gives null (unread), explicit null gives "" (INVALID_STATUS, probe), `[]` and `{}` give 400 MALFORMED_REQUEST, `"unread\n"` and `"${7*7}"` give INVALID_STATUS with the fixed message. No exception text leaks. Number and boolean coercion is SEC-F-122 (Task 21).
- Service re-check: VERIFIED. `create` computes status with `fromWire`, runs `TextPolicy.check` for title and author and the status check before any save; the same selection rule (only status gives INVALID_STATUS); stores `normalize` output; `@Transactional`. A caller that skips the controller cannot persist invalid data. `allowDuplicate` ignored.
- Details ordering and selection: VERIFIED by probe: `title` null plus bad author plus bad status gives title, author, status in that order; `{}` gives title, author; status alone gives INVALID_STATUS.
- SC-16 wiring, SC-02, SC-11, SC-29 on the validation path: VERIFIED/REVERIFIED (see security.md).
- No pom or resource change, human identity and no attribution in the five commits: VERIFIED.

## 4. Not verified
Full `./mvnw clean verify`, coverage and the 611/154 test counts (delivery claims only; I ran only the scratch probe). Advisory databases for hibernate-validator 9.1.5.Final and tomcat-embed-el 11.0.27 (no advisory source reachable; SC-20 runs elsewhere). Behaviour of `validator` under methods other than `POST /books` (none exist yet).

## 5. Residual risks
None new for the Sponsor. SEC-F-155 is a build-time hardening choice; I recommend excluding `tomcat-embed-el` in a later task once the Sponsor approves a `pom.xml` change.

## 6. Verdict
PASS WITH CONDITIONS. C-1 (SEC-F-155 test) and C-2 (SEC-F-156 flaky assertion), both LOW, closed before merge; both are test-only changes. No application-code change is required. Next step: Java applies C-1 and C-2, I re-verify by reading the diff, then the Sponsor gate. Under constitution 1.5 the LOW items may be delegated to the Liaison if the orchestrator prefers.

## 7. Re-verification (2026-11-05)
Method: read `git diff ec30c51..HEAD` (commits 63d6aa6, 609b900, 0bac2f6). Changed files: two test classes (`BookValidationIntegrationTest`, `GlobalExceptionHandlerTest`) and a note in handoff 117. No `pom.xml` and no production code changed.
- C-2 / SEC-F-156: CLOSED. The "49" check now runs per `"message"` value (and via `jsonPath("$.details[0].message")`), so the `timestamp` is not inspected. `doesNotContain("${7*7}")` remains on the whole body, and the test asserts exactly one message was found.
- C-1 / SEC-F-155: CLOSED. `validatorBeanReturnsAnExpressionTemplateUnchanged` autowires the full-context `jakarta.validation.Validator` and validates a probe whose messages are `${1+1}` and `{min} #{1+1}`; it requires them back verbatim (an evaluating interpolator would return `2`). This proves templates are not evaluated by the wired bean.
- Not re-run: I did not execute the build; the 611/155 counts are the delivery's claim. Residual: `tomcat-embed-el` stays on the classpath (exclusion remains an optional ASK FIRST hardening).
- Verdict: PASS. Next step: Sponsor gate.
