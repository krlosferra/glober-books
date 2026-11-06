# Handoff 121: Delivery, Task 21 (strict body reading and parser limits)
- From: java
- To: liaison
- Type: TASK_DELIVERY
- Branch: `feature/task-21-strict-body` (the task header says `feature/task-21-strict-json`; the checked-out name was kept, as the brief says). Local commits only, not pushed.

## Resume point
- Done: all production classes, four test classes, `./mvnw clean verify` green, `progress.md`, this delivery.
- Next: nothing for the implementer; the orchestrator pushes and the Liaison and Security audit.

## Files
Created: `config/json/JacksonConfig`, `config/json/StrictJsonBodyResolver`, `config/json/StrictStringDeserializer`, `config/WebMvcConfig`; tests `JacksonStrictnessTest`, `StrictJsonBodyResolverTest` and two extra test classes, `JacksonLimitsTest` (the file limit of 300 lines forced a split) and `StrictBodyIntegrationTest` (the real-port table). Modified: `dto/CreateBookRequest` (the nested `StatusDeserializer` now extends `StrictStringDeserializer`, same null and absent semantics), `docs/sdd/progress.md`. `pom.xml`, `application.yml`, `backend/config/**`: untouched. No new dependency.

## Design notes
- `WebMvcConfig` installs `StrictJsonBodyResolver` in front of the framework's `@RequestBody` processor through a `BeanPostProcessor` on `RequestMappingHandlerAdapter` (`addArgumentResolvers` would put it after the built-in processor, which would win). The resolver reads at most 16,385 bytes (the gate has already capped the body), decodes with `CodingErrorAction.REPORT`, parses with the Boot mapper (customized by `JsonFactoryBuilderCustomizer` and `JsonMapperBuilderCustomizer` beans from `JacksonConfig`, so `spring.jackson.*` still applies), then runs `@Valid` through the binder and throws `MethodArgumentNotValidException`, so Task 20's mapping to `VALIDATION_ERROR`/`INVALID_STATUS` is unchanged.
- Read constraints come from AP-3 (depth 16, string 16,384, name 64, number 100, document 16,384). The plan gives no token count; I chose 64 (a legal body has about a dozen tokens). Say if you prefer another value.
- Boolean: a `Nulls.FAIL` config override was tried first and rejected because it also fails an absent `allow_duplicate` (creator property). `StrictBooleanDeserializer` (nested in `JacksonConfig`) fails text, numbers and an explicit null, and keeps an absent member null.
- Jackson 3.1.7 key correction (criterion 4): `FAIL_ON_NUMBERS_FOR_ENUMS` is `tools.jackson.databind.cfg.EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS`, not a `DeserializationFeature` (the compiler refused the first form). All other keys exist as named.
- Jackson 3 already defaults every `JsonReadFeature` to off, so for those the pair test proves the switch is the controlling cause, and the explicit setting guards against a library change. For the features whose default is lenient (strict duplicate detection, trailing tokens, scalar coercion, float-as-int) the control mapper with library defaults accepts the input.
- Not enabled anywhere: default typing, polymorphic handling. Unknown members (including `@class`) fail.

## Acceptance criteria to tests
| Criterion | Tests |
|---|---|
| 1 invalid UTF-8, BOM, comments, trailing comma, single quotes, unquoted names, raw control character, trailing content, zero bytes, non-object | `StrictBodyIntegrationTest` (T-05/T-28, T-29, T-16 trailing x, T-39); `StrictJsonBodyResolverTest` (invalid UTF-8, truncated, overlong, BOM, zero byte, null, non-object); `JacksonStrictnessTest` pairs |
| 2 unpaired surrogate, unknown, duplicate (also unescaped), wrong type, allow_duplicate `"true"`, `1`, `null` | `StrictBodyIntegrationTest` (T-04, T-27, T-16, T-30, T-21, SEC-F-122 rows); `JacksonStrictnessTest.refusesUnpairedSurrogates...` is in `JacksonLimitsTest`; duplicate pair in `JacksonStrictnessTest` |
| 3 16 KiB string is a length error; nesting and exponents fast, never 500 | `StrictBodyIntegrationTest` (T-11 two rows, T-31 four rows, every row asserted under 300 ms and not 500) |
| 4 every switch proved | table below |

Spec row to test: T-04, T-05, T-11, T-16, T-21, T-27 (including 500 escaped pairs giving 201), T-28, T-29, T-30, T-31, T-39: `StrictBodyIntegrationTest`. SEC-F-122 (`title` 5, `author` true, `status` 5 and true give `MALFORMED_REQUEST`; absent status is `unread`; null status is `INVALID_STATUS`): same class. No echo of body or parser text (SC-31): `neverEchoesBodyContentOrParserMessages`, `failureCarriesAFixedMessageWithNoBodyContent`.

## Jackson switches and the test that proves each (pair: strict refuses, only that switch flipped accepts)
| Switch | Test |
|---|---|
| `JsonReadFeature.ALLOW_JAVA_COMMENTS`, `ALLOW_YAML_COMMENTS`, `ALLOW_SINGLE_QUOTES`, `ALLOW_UNQUOTED_PROPERTY_NAMES`, `ALLOW_UNESCAPED_CONTROL_CHARS`, `ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER`, `ALLOW_RS_CONTROL_CHAR`, `ALLOW_TRAILING_COMMA`, `ALLOW_MISSING_VALUES`, `ALLOW_NON_NUMERIC_NUMBERS`, `ALLOW_LEADING_ZEROS_FOR_NUMBERS`, `ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS`, `ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS`, `ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS` | `JacksonStrictnessTest.refuses...OnlyBecauseTheSwitchIsOff` (one per switch) |
| `StreamReadFeature.STRICT_DUPLICATE_DETECTION` | `refusesDuplicateMemberAndDuplicateAfterUnescaping` (plain, escaped, and a library-default control that accepts) |
| `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` | `refusesUnknownMemberOnlyBecauseTheSwitchIsOff`, `refusesMemberNameWithDifferentCaseAsUnknown` |
| `FAIL_ON_TRAILING_TOKENS` | `refusesTrailingContentOnlyBecauseTheSwitchIsOff` |
| `MapperFeature.ALLOW_COERCION_OF_SCALARS` | `refusesScalarCoercionOnlyBecauseTheSwitchIsOff` |
| `EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS` | `refusesNumericEnumOnlyBecauseTheSwitchIsOn` |
| `ACCEPT_FLOAT_AS_INT`, `ACCEPT_SINGLE_VALUE_AS_ARRAY`, `UNWRAP_SINGLE_VALUE_ARRAYS`, `ACCEPT_EMPTY_STRING_AS_NULL_OBJECT`, `ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT`, `FAIL_ON_NULL_FOR_PRIMITIVES` | one test each in `JacksonStrictnessTest` |
| `StrictStringDeserializer` (no coercion, surrogates) | `refusesNumberAndBooleanAndContainerWhereTextIsExpected`, `refusesUnpairedSurrogatesAndKeepsValidPairs` (`JacksonLimitsTest`) |
| `StrictBooleanDeserializer` | `refusesNullBooleanAndKeepsAbsentBooleanNull`, `refusesNumberAsBooleanAndBooleanTextAsWrongType` |
| Constraints: depth, string, name, number, document, token count | `JacksonLimitsTest.limits...OnlyBecauseOfTheConstraint` (six tests, each with a raised-limit control) |
| Resolver installed first | `StrictJsonBodyResolverTest.configPutsTheStrictResolverBeforeTheFrameworkOnes`, and every real-port row (the lenient processor would have answered 201 for `title` 5) |

## Real-port table (`StrictBodyIntegrationTest`, random port, raw socket; file `backend/target/strict-body-real-port.txt`)
Rows are `label | status | code | elapsed`. Every row is asserted under 300 ms and not 500; the first row of a run includes warm-up.
```
T-39 zero bytes | 400 | MALFORMED_REQUEST | 252 ms
T-39 body null | 400 | MALFORMED_REQUEST | 52 ms
T-39 body [] | 400 | MALFORMED_REQUEST | 5 ms
T-39 body "x" | 400 | MALFORMED_REQUEST | 3 ms
T-39 body 5 | 400 | MALFORMED_REQUEST | 4 ms
no echo | 400 | MALFORMED_REQUEST | 12 ms
no echo syntax | 400 | MALFORMED_REQUEST | 3 ms
T-31 8,000 nested arrays | 400 | MALFORMED_REQUEST | 3 ms
T-31 2,000 nested objects | 400 | MALFORMED_REQUEST | 3 ms
T-31 exponent 1e999999999 | 400 | MALFORMED_REQUEST | 3 ms
T-31 exponent 1e-999999999 | 400 | MALFORMED_REQUEST | 3 ms
T-29 BOM | 400 | MALFORMED_REQUEST | 5 ms
T-29 comment | 400 | MALFORMED_REQUEST | 8 ms
T-29 trailing comma | 400 | MALFORMED_REQUEST | 2 ms
T-29 single quotes | 400 | MALFORMED_REQUEST | 2 ms
T-29 unquoted names | 400 | MALFORMED_REQUEST | 2 ms
T-29 raw line feed | 400 | MALFORMED_REQUEST | 2 ms
T-11 16,000 character title | 400 | VALIDATION_ERROR | 110 ms
T-05 byte FF | 400 | MALFORMED_REQUEST | 3 ms
T-28 C0 80 | 400 | MALFORMED_REQUEST | 3 ms
T-28 ED A0 80 | 400 | MALFORMED_REQUEST | 2 ms
T-28 F4 90 80 80 | 400 | MALFORMED_REQUEST | 3 ms
T-28 E2 82 truncated | 400 | MALFORMED_REQUEST | 2 ms
T-21 allow_duplicate "true" | 400 | MALFORMED_REQUEST | 5 ms
T-21 allow_duplicate 1 | 400 | MALFORMED_REQUEST | 3 ms
T-21 allow_duplicate null | 400 | MALFORMED_REQUEST | 2 ms
SEC-F-122 title number | 400 | MALFORMED_REQUEST | 3 ms
SEC-F-122 author boolean | 400 | MALFORMED_REQUEST | 3 ms
SEC-F-122 status number | 400 | MALFORMED_REQUEST | 2 ms
SEC-F-122 status boolean | 400 | MALFORMED_REQUEST | 3 ms
absent status | 201 | - | 168 ms
null status | 400 | INVALID_STATUS | 5 ms
T-04 uD800 | 400 | MALFORMED_REQUEST | 8 ms
T-27 reversed pair | 400 | MALFORMED_REQUEST | 3 ms
T-27 uD83D then letter | 400 | MALFORMED_REQUEST | 2 ms
T-27 500 pairs | 201 | - | 18 ms
T-11 body of exactly 16,384 bytes | 400 | VALIDATION_ERROR | 7 ms
T-16 member id | 400 | MALFORMED_REQUEST | 2 ms
T-16 member date_added | 400 | MALFORMED_REQUEST | 2 ms
T-16 member date_completed | 400 | MALFORMED_REQUEST | 3 ms
T-16 member rating | 400 | MALFORMED_REQUEST | 2 ms
T-16 member Title | 400 | MALFORMED_REQUEST | 2 ms
T-16 trailing x | 400 | MALFORMED_REQUEST | 2 ms
T-30 duplicate title | 400 | MALFORMED_REQUEST | 2 ms
T-30 duplicate after unescaping | 400 | MALFORMED_REQUEST | 2 ms
T-30 duplicate allow_duplicate | 400 | MALFORMED_REQUEST | 2 ms
```

## Build evidence
`cd backend && ./mvnw clean verify`: BUILD SUCCESS (compile with `-Werror`, Checkstyle, SpotBugs, unit and slice tests 661 run, integration tests 167 run, 0 failures, 0 errors, 1 skipped which is the pre-existing one; JaCoCo gate "All coverage checks have been met").
New test classes: `JacksonStrictnessTest` 28, `JacksonLimitsTest` 8, `StrictJsonBodyResolverTest` 14, `StrictBodyIntegrationTest` 12, all passing.
Line coverage of the new classes (JaCoCo csv, covered/missed): `StrictJsonBodyResolver` 35/1 (the null servlet request guard), `StrictStringDeserializer` 17/2, `JacksonConfig.StrictBooleanDeserializer` 6/2, `WebMvcConfig` installer 4/0, `CreateBookRequest.StatusDeserializer` 3/0. Project total 96.5%.
SpotBugs first failed on `EI_EXPOSE_REP2` (storing the mapper) and a possible null; fixed by keeping an `ObjectReader` and null-checking the adapter list, not by suppression.

## git diff --stat origin/main..HEAD (before the final commit of this file and progress.md)
```
 .../globant/globerbooks/config/WebMvcConfig.java   |  49 ++++
 .../globerbooks/config/json/JacksonConfig.java     | 135 +++++++++++
 .../config/json/StrictJsonBodyResolver.java        | 109 +++++++++
 .../config/json/StrictStringDeserializer.java      |  56 +++++
 .../globant/globerbooks/dto/CreateBookRequest.java |   9 +-
 .../globerbooks/config/json/JacksonLimitsTest.java | 124 ++++++++++
 .../config/json/JacksonStrictnessTest.java         | 263 +++++++++++++++++++++
 .../config/json/StrictBodyIntegrationTest.java     | 239 +++++++++++++++++++
 .../config/json/StrictJsonBodyResolverTest.java    | 219 +++++++++++++++++
 .../120-orchestrator-to-java-task-21-brief.md      |  25 ++
 .../121-java-to-liaison-task-21-delivery.md        |  10 +
 docs/sdd/status.md                                 |   2 +-
 12 files changed, 1235 insertions(+), 5 deletions(-)
```

## git log origin/main..HEAD --format='%an <%ae>%n%B---'
```
krlosferra <carlos.ferra@globant.com>
fix(json): satisfy static analysis for the resolver and its installer [Task-21]
---
krlosferra <carlos.ferra@globant.com>
test(json): resolver unit tests and real-port strict body proofs [Task-21]
---
krlosferra <carlos.ferra@globant.com>
feat(json): strict boolean reading; test pairs for every switch [Task-21]
---
krlosferra <carlos.ferra@globant.com>
test(json): prove every Jackson switch and read constraint [Task-21]
---
krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 21 delivery resume point [Task-21]
---
krlosferra <carlos.ferra@globant.com>
feat(json): strict body resolver, string deserializer and Jackson switches [Task-21]
---
krlosferra <carlos.ferra@globant.com>
docs(handoff): Task 21 brief, Task 20 done [Task-21]
---
```

## Deviations, risks, follow-ups
- Deviation: two extra test classes (`JacksonLimitsTest`, `StrictBodyIntegrationTest`); the brief required the real-port table.
- Slice tests (`@WebMvcTest`) do not import `WebMvcConfig`, so they still use the framework processor; strictness is proved by unit and full-context tests. A later task that adds `PUT` bodies can import it.
- `MAX_TOKENS` 64 is my choice; revisit if a later body (Task 30 rating, status) needs more.
- Rating (Task 30) will need its own literal deserializer; this task leaves `rating` as an unknown member of the create body, as T-16 requires.
- `status.md` has no Task 21 row; I did not add one (orchestrator owns it).
