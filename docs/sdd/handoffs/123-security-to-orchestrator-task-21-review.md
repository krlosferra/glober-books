# Handoff 123: Security review, Task 21 (strict body reading and parser limits)
- From: security
- To: orchestrator
- Type: AUDIT_REPORT
- Branch: `feature/task-21-strict-body`, base dd226a9, head 0bb3866

## 1. Scope and method
Read: handoffs 120 and 121, `JacksonConfig`, `StrictJsonBodyResolver`, `StrictStringDeserializer`, `WebMvcConfig`, `MalformedRequestException`, logging calls in `src/main`. Built a scratch copy (`git archive HEAD`) and ran the jar on 127.0.0.1:18121 with a temporary SQLite file; sent 40 raw-socket probes; re-ran `JacksonLimitsTest`, `JacksonStrictnessTest`, `StrictJsonBodyResolverTest` (8 + 28 + 14, all pass). Did not run the full `verify` or the integration class (the delivery reports them green).

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-122 | LOW | Coercion | request body | CLOSED | `title` 5, 5.5, array, object; `status` 5, true: MALFORMED_REQUEST; absent status 201 unread; null status INVALID_STATUS | none left | none | SC-31 |
| SEC-F-159 | LOW | Test assurance | `@WebMvcTest` slices | Slices do not import `WebMvcConfig`, so they use the lenient framework processor | delivery "Deviations"; production probes all strict | A future slice test passes on lenient input and gives false assurance | Condition C-1: strictness is claimed only from unit and full-context tests; a slice needing strict bodies imports `WebMvcConfig` and `JacksonConfig` | SC-31 |
| SEC-F-160 | INFO | Media type | resolver | Resolver ignores Content-Type and always decodes UTF-8; the gate answers 415 for non-`application/json`, ISO-8859-1, UTF-16, `+json`, text/plain, form (probed); `charset=utf-8` in any case gives 201 | probes | none | none | SC-31 |
| SEC-F-161 | INFO | Limits | `JacksonConfig` | `MAX_TOKENS` 64 and `MAX_DEPTH` 16 are implementer choices | code | A later array body could hit them | Re-check at Task 30 | SC-06 |

## 3. Controls checked
- SC-31 VERIFIED (request body). Probes, all `400 MALFORMED_REQUEST` with the fixed 67-byte body, 1 to 7 ms (first request 53 ms): block comment, line comment, trailing comma, single quotes, BOM, duplicate `title`, duplicate after `title`, unknown member, `@class`, NaN, `allow_duplicate` as `"true"` and null, `\uD800`, `\uDC00x`, raw ED A0 80, byte FF, 10,000 `[`, 3,000 nested objects, `1e999999999`, 5,000-digit integer, 100-character name, 200 tokens, empty body. 16,300-character title: `400 VALIDATION_ERROR`. Switches are all set explicitly in code; pair tests pass. Key correction `EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS` compiles against the pinned Jackson 3.
- Decoder: `CodingErrorAction.REPORT` for malformed and unmappable input (code read; FF and ED A0 80 probed). Lone surrogates: `StrictStringDeserializer.hasUnpairedSurrogate` rejects high without low and low alone; names with such escapes fail as unknown.
- SC-11 / no echo: every `JacksonException` and `IOException` becomes `new MalformedRequestException()` with no cause and a fixed message; the server log of the whole session has no Jackson, parser or probe text (grep 0 hits for exception, parse, duplicate, surrogate, unrecognized, probe; no WARN or ERROR).
- Resolver ordering: `BeanPostProcessor` prepends the resolver to the adapter's list after initialization; proven live because `title` 5 (which the lenient processor accepted before) now fails. The resolver supports any `@RequestBody` parameter independent of media type, so no content-type variant reaches the framework processor. Slice path: see SEC-F-159.
- 16,385-byte bound: `readNBytes(LIMIT + 1)`; body of exactly 16,384 bytes is read and judged (400 VALIDATION_ERROR); 16,385 declared and a chunked oversize both give 413 from the gate.
- SC-08 request side VERIFIED, SC-06 Jackson constraints VERIFIED (evidence in `security.md`).
- Size: the json package is exactly 300 lines in total; each file is below 300.

## 4. Not verified
Full `./mvnw clean verify`, JaCoCo, SpotBugs and the integration test class were not run by Security (reported green in 121). No mutation run of each switch by Security beyond reading and re-running the pair tests. Public advisory databases were not queried (no dependency change in this task).

## 5. Residual risks
None new for the Sponsor to accept.

## 6. Verdict
PASS WITH CONDITIONS. No CRITICAL or HIGH open.
- C-1 (LOW, SEC-F-159): record in the Task 21 closure (or progress.md) that slice tests do not exercise strict parsing; later tasks that add bodies must prove strictness at unit or full-context level.
SEC-F-122 is closed. `security.md` is at 0.44 and `status.md` has the Task 21 row (IN_REVIEW, Security cell filled). Next step: Liaison audit, then Sponsor gate. Nothing was committed.
