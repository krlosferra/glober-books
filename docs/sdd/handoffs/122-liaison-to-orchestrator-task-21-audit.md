# Handoff 122: Liaison audit, Task 21 (strict body reading and parser limits)
- From: liaison
- To: orchestrator
- Type: REVIEW_REPORT (Mode F)
- Branch: `feature/task-21-strict-body` (7 commits, base dd226a9, head 0bb3866)

## Verdict: ACCEPT (Layers 1, 1b, 2), with non-blocking follow-ups. Security verdict from `sdd-security-reviewer` still required.

ACCEPT Task 21. Compliance: criteria 1 to 4 met; constraints 1 to 6 met. Tests: my own re-run of `./mvnw -o clean verify` ended with fresh reports and no failures (StrictJsonBodyResolverTest 14, StrictBodyIntegrationTest 12 passed; the real-port file `backend/target/strict-body-real-port.txt` was regenerated). Proceed to Task 22 after Security and Sponsor approval.

## Layer 1 (specification compliance)
- C1 (decode and fixed message): `StrictJsonBodyResolver.decode` uses `CodingErrorAction.REPORT` for malformed and unmappable input. Every `IOException` and `JacksonException` becomes `MalformedRequestException` with no cause text and no body. Empty and over-cap bodies and a `null` document are also `MALFORMED_REQUEST`. OK.
- C2 (test row per input): the real-port table covers every listed input with the spec's code (T-04, T-05, T-16, T-21, T-27 to T-30, T-39). Expected values are literal in the test, copied from the spec, not read from the code. OK.
- C3 (SEC-F-122 and Task 20): the `String` deserializer accepts only `VALUE_STRING`. `CreateBookRequest.StatusDeserializer` now extends it and keeps `getNullValue` (explicit null becomes `INVALID_STATUS`) and the absent default. Real-port rows: `title` 5, `author` true, `status` 5 and true give `MALFORMED_REQUEST`; absent status gives 201 (`unread`); null status gives `INVALID_STATUS`. Task 20 behavior preserved. The Task 20 tests are part of the green build.
- C4 (limits): AP-3 values (depth 16, string 16,384, name 64, number 100, document 16,384) are used exactly. A 16,000-character title and a body of exactly 16,384 bytes give `VALIDATION_ERROR` (T-11). Nesting (8,000 arrays, 2,000 objects) and `1e999999999` answer in 2 to 3 ms with 400, never 500 (T-31).
- C5 (switch proof): 14 `JsonReadFeature` switches, strict duplicate detection, unknown members, trailing tokens, coercion, numeric enums, float-as-int, single-value arrays and the rest each have a mutation-style pair. Six constraint tests use a raised-limit control. The resolver-first ordering is proved by a config test and by the `title` 5 row (the lenient processor returned 201).
- C6: no logging added, no new error code. File sizes: main classes 135, 109, 56, 49 lines; tests 124, 263, 239, 219 (all under 300).
- Scope: `pom.xml`, `application.yml` and `backend/config/**` are untouched (`git diff --name-only`). No new dependency.
- Spec traceability: BR-012 steps 4 and 5, 5.0, BR-006 item 1 and DEC-027 (unpaired surrogate refused; 500 valid pairs accepted) and AP-3 are honoured.

## Layer 1b (repository hygiene)
All 7 commits use the Sponsor identity and Conventional Commits with `[Task-21]`. A grep of the log for tool, model, vendor, trailer and session text found nothing. The delivery's log block shows the same. No secrets or build output in the diff stat.

## Layer 2 (testing)
Tests exist for every rule and unhappy path; the AAA style and names are consistent. The delivery claim of 661 + 167 tests was not independently counted; the targeted classes and the build ran green on my re-run.

## Layer 3 and review of the deviations
| Deviation | Ruling |
|---|---|
| Two extra test classes (`JacksonLimitsTest`, `StrictBodyIntegrationTest`) | Accepted. The 300-line limit forces the split and the brief demands the real-port table. Tasks 21 text lists only two test classes; PM should note it in the `tasks.md` row (editorial). |
| `MAX_TOKENS` 64 without a plan value | Accepted; reported as the brief required. Legal bodies have about 10 tokens. Follow-up F1. |
| `BeanPostProcessor` installs the resolver first | Accepted. `addArgumentResolvers` would run after the built-in processor, so this is the only way to meet C1. Note it is an architectural detail not in `plan.md`; follow-up F2. |
| `StrictBooleanDeserializer` for `allow_duplicate` | Accepted. The `Nulls.FAIL` alternative was shown to break an absent member. Absent stays null, so the BR-003 default is kept. |
| `EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS` correction | Accepted; this is the reporting that criterion 4 asks for. All other keys exist. |

## Follow-ups (non-blocking)
- F1: PM to record `MAX_TOKENS = 64` in AP-3 (plan amendment, coding agent owns `plan.md`). Revisit in Task 30 (rating) and Task 31 (status body).
- F2: Add the `BeanPostProcessor` installer and the "resolver first" rule to `plan.md` as a Plan Amendment, and say in the Task 30/31 briefs that `PUT`/`PATCH` bodies go through it.
- F3: Timing flake risk: the first real-port row (zero bytes) takes 235 to 252 ms against the 300 ms assertion, because it includes warm-up. Recommend that Java adds a warm-up request before the measured rows (do not weaken the bound). Likely to fail on a slow CI runner. Raise as a small fix in the next backend task or before the PR.
- F4: Slice tests (`@WebMvcTest`) do not load `WebMvcConfig`, so they use the lenient processor. Any later slice test of a body must not claim strictness; strict proof stays in full-context tests. Put this in the Task 30/31 briefs.
- F5: `status.md` has no Task 21 row; the orchestrator should add it (READY/IN_REVIEW to DONE) and note the branch-name difference (`feature/task-21-strict-body` against `feature/task-21-strict-json` in `tasks.md`; PM should align the header).
- F6: The delivery's stat/log omits the last docs commit (0bb3866) by design ("before the final commit"); stat shows 12 files, the real diff has 13 (adds `progress.md`). No effect on the verdict.
- F7: Remind: update `progress.md` (done by Java) and merge by squash via pull request only after Sponsor approval.

## Next step
Security gate on Task 21 (SC-08, SC-31, SEC-F-122 closure), then Sponsor approval at the gate, then Task 22.
