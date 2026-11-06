# Handoff 121: Delivery, Task 21 (strict body reading and parser limits)
- From: java
- To: liaison
- Type: TASK_DELIVERY (in progress)
- Branch: `feature/task-21-strict-body` (the task header says `feature/task-21-strict-json`; the checked-out name is kept as the brief says)

## Resume point
- Done: production classes `config/json/JacksonConfig`, `StrictJsonBodyResolver`, `StrictStringDeserializer`, `config/WebMvcConfig`; `CreateBookRequest.StatusDeserializer` now extends `StrictStringDeserializer` (commit 6c6df49).
- Next: `JacksonStrictnessTest`, `StrictJsonBodyResolverTest`, real-port integration test, `./mvnw clean verify`, evidence sections below, `progress.md`.
- Finding so far: `DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS` does not exist in Jackson 3.1.7; the key is `tools.jackson.databind.cfg.EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS` (corrected).
