# Handoff 060: Security review of Task 05 (error foundation)
- From: security | To: orchestrator | Type: AUDIT_REPORT | Branch: `feature/task-05-error-foundation`

## 1. Scope and method
Read: handoff 058, `error/{ErrorCode,ErrorBody,ErrorBodyFactory,ErrorResponseWriter}.java`, spec Appendix A, grep for default typing, `@JsonTypeInfo`, `Object` and wall-clock calls under `backend/src`. Did not re-run the build and did not probe a running app (no Spring context exists for this package yet). I did not read ErrorBodyFactoryTest in full; I relied on the grep hits and on handoff 058 for its content. The optional seeded check was not run.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-108 | LOW | Design / SC-11 | `ErrorBody.FieldProblem`, `ErrorBodyFactory.create(code, details)` | `FieldProblem.message` is a free `String` and `create(code, details)` accepts any code. Nothing in the type stops a later caller from putting user input into a message, or from adding details to a code that has none. | Source read | A later validation task builds the message from the offending value. This is reflected content in an error body (SC-11, BR-012 "never echo input"). | Task 07 and later: take the message from a closed set (enum of the six Appendix A detail messages) and `field` from an enum. In the factory, reject details for codes other than VALIDATION_ERROR, INVALID_STATUS and INVALID_RATING. Add a test. | SC-11 |
| SEC-F-109 | LOW | Contract / INV-6 | `ErrorBody` | Appendix A defines `details` for `DUPLICATE_BOOK` as an object `{existing_book}`. `ErrorBody.details` is `List<FieldProblem>` only. This is a gap to close in the duplicate-book task, not a defect in Task 05. | Spec Appendix A row DUPLICATE_BOOK vs source | A developer widens `details` to `Object` or `Map<String,Object>` to fit it, which breaks SC-08. | Use a separate typed record (for example a `DuplicateDetails` record holding a `BookResponse`) or a sealed typed hierarchy. Never `Object`, `Map` or `JsonNode`. Record the choice in plan/spec. | SC-08, INV-6 |
| SEC-F-110 | INFO | Robustness | `ErrorResponseWriter.write` | The writer calls `getOutputStream()`. If a caller has already called `getWriter()`, it throws IllegalStateException, which falls into the 500 path. Handoff 058 notes this. | Source read | None direct. A Task 06 filter that uses `getWriter()` would turn an error into a worse error. | Task 06 filters must not use `getWriter()`. Add a test there. | SC-29 |
| SEC-F-111 | INFO | Fail-closed | `ErrorResponseWriter.write` | The body is serialized before `resetBuffer()`, so a serialization failure leaves the response untouched and the caller's catch-all decides. This is correct. Keep it. | Source read | none | none | SC-29 |

No CRITICAL or HIGH findings.

## 3. Controls checked
- SC-08 (no `Object`, `Map`, `JsonNode` or interface member): VERIFIED for this package. `ErrorBody` has String, String, String and `List<FieldProblem>`. `FieldProblem` has two Strings. A reflection test (`noDetailsMemberIsOfTypeObject`) exists. Grep finds no `enableDefaultTyping`, `activateDefaultTyping` or `@JsonTypeInfo` in main or test. The mapper is `JsonMapper.builder().build()` (Jackson 3, `tools.jackson`). It uses no default typing and no polymorphic features, it is private to the writer, and it only serializes records. The full SC-08 stays open for the request side (Phase 4). See SEC-F-109.
- SC-11 (no internal detail in messages): VERIFIED for the 16 fixed texts in `ErrorCode`. None has a placeholder, and none contains a class name, SQL, a path, a version or a product name. The generic text is "Unexpected error.". The 8 non-Appendix texts are fixed strings without input. Appendix A calls texts informative, so I accept them. Hardening is in SEC-F-108. The behavior check (force 500, 405, 413 and so on) is Task 06 and later.
- SC-29 (writer): VERIFIED by code read. `setStatus(code.status())`, `setContentType("application/json; charset=UTF-8")`, `setContentLength`, and only the envelope bytes are written. `resetBuffer()` clears leftover content. No exception text, no input and no header echo. The status comes from the enum, not from the body. Whether headers, `Allow` and similar are correct across the whole chain is for Task 06.
- INV-6: PARTLY. The envelope and the 16 codes match Appendix A (statuses and texts compared line by line). Null `details` is omitted with `NON_NULL`. Not yet covered: the `DUPLICATE_BOOK` details shape (SEC-F-109).
- Product and version disclosure: VERIFIED. No product or version string in any message or in the writer.
- Timestamp from the injected clock: VERIFIED. `ErrorBodyFactory` formats `clock.instant()` in UTC with milliseconds and `Z`. The only wall-clock source is `ClockConfig` (`Clock.systemUTC()`), and `SqliteStoreIntegrationTest` has a scan that bans wall-clock calls elsewhere.

## 4. Not verified
- Build, SpotBugs and coverage figures from 058 (I did not re-run them). Behavior through a real servlet container (no Spring-context test until Task 06). Content of ErrorBodyFactoryTest beyond the grep hits. The optional seeded check.

## 5. Residual risks
None new for the Sponsor.

## 6. Verdict
**PASS WITH CONDITIONS.** Conditions are carried forward, not blocking this merge:
1. SEC-F-108: constrain detail fields and messages to closed sets and details to the three codes that define them, in the first task that produces details.
2. SEC-F-109: type `DUPLICATE_BOOK` details without `Object`, `Map` or `JsonNode`, in the duplicate-book task.
3. SEC-F-110: Task 06 filters must not call `getWriter()` before this writer.

Next: Liaison audit, then Sponsor gate. The orchestrator updates `security.md` and the status Security cell (I was told not to edit `security.md`).

## Appendix: exact text for `security.md`
Findings log rows (§8):
- `| SEC-F-108 | LOW | FieldProblem.message and field are free strings and the factory accepts details for any code; restrict to closed sets and to VALIDATION_ERROR, INVALID_STATUS, INVALID_RATING (SC-11) | OPEN, condition, first task that builds details | 060 |`
- `| SEC-F-109 | LOW | DUPLICATE_BOOK details object {existing_book} not representable in ErrorBody; type it without Object, Map or JsonNode (SC-08, INV-6) | OPEN, condition, duplicate-book task | 060 |`
- `| SEC-F-110 | INFO | ErrorResponseWriter uses getOutputStream(); Task 06 filters must not call getWriter() first | OPEN, note for Task 06 | 060 |`
- `| SEC-F-111 | INFO | Writer serializes before resetBuffer, so serialization failure leaves the response untouched; keep | CLOSED | 060 |`

Control evidence (§3, append to Notes):
- SC-08: `Task 05 (060): error package VERIFIED (typed records, private JsonMapper, no default typing); request side still Phase 4.`
- SC-11: `Task 05 (060): 16 fixed texts VERIFIED by code read; behavior tests in Task 06+.`
- SC-29: `Task 05 (060): ErrorResponseWriter sets status, application/json; charset=UTF-8, writes envelope only, VERIFIED by code read; chain tests in Task 06.`
