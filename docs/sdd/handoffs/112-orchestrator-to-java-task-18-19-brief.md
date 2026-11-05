# Handoff 112: Task brief, Task 18+19 (text policy and Unicode fixture check)
- From: orchestrator (compact brief issued by the orchestrator to save usage; the Liaison audits against it)
- To: java
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Task 18+19 of `docs/sdd/tasks.md` (v0.6.1, merged task, 90 minutes): read its text, criteria 1 to 8 and traces. Branch `feature/task-18-19-text-policy` is checked out; Tasks 01 to 17 are merged. Read also: `spec.md` BR-006 (items 1 to 6d, the passing and failing examples), US-001.4, US-001.6, DEC-027, DEC-029, Appendix tests T-01 to T-11, T-22 to T-26 and T-24b; `plan.md` and `plan-appendix.md` AP-1 (TextPolicy row) and the paragraph on `UnicodeFixtureCheckTest`; `security.md` SC-16; `constitution.md` Code Style (300-line file limit), Persistence and Security Standards for text.

## Scope
`service/TextPolicy.java` (nested problem enum, no framework types, pure functions, no I/O), tests `TextPolicyTest`, `UnicodeFixtureCheckTest`, `TextPolicyMarksTest`, `support/UnicodeFixtures`. Out of scope: Bean Validation wiring, field-error mapping and request deserialization (Task 20), duplicate keys (later task), the controller, any new dependency, any change under `backend/config/**` or `pom.xml`.

## Binding constraints
1. Build order inside the task: criteria 1 to 4 first (commit), then 5 to 8 (commit). If it overruns, split back into 18 and 19 at that boundary and report.
2. `UnicodeFixtureCheckTest` runs before any rule test (class ordering or the plan's mechanism) and asserts, independently of `TextPolicy`: JDK 21.0.12 or later; Unicode 15.0 (U+1E030 assigned, U+2FFC unassigned); the category of every code point the spec names; the NFC result of the T-24b and T-25 inputs. A mismatch stops the task and goes to the PM; do not weaken an assertion to pass.
3. Implement the check order of BR-006 exactly: trim (Unicode White_Space, not `String.strip` unless proven identical for every White_Space code point; test U+0085, U+00A0, U+2028, U+2029, U+3000), NFC, length in code points, then the characters rules. The first failing check order for messages is: required, empty, length, characters. The problem enum reports which one failed; the message text belongs to Task 20.
4. Unicode 15.0 tables are those of the pinned JDK; never hand-copy tables. Noncharacters: U+FDD0 to U+FDEF and every U+xxFFFE and U+xxFFFF. Joiner, variation selector and mark-run rules follow items 6b to 6d literally, including "x" + 8 times U+0301 + U+200D + U+0301 (rejected) and the 8-run boundary.
5. Criterion 8: the T-24b 50-repetition input finishes without timeout; implement with a single linear pass over code points, no backtracking regular expression; add a test with a generous time bound that does not use `Thread.sleep`.
6. Reject-only behaviour for invalid text: no sanitising, no replacement, stored text is exactly the trimmed NFC form with case and internal spacing preserved. U+FFFD between letters is accepted.
7. Expected values come from the spec examples, not from the code. Every passing and failing example listed in BR-006 becomes a test row. Keep every file at or under 300 lines (split tests by theme).
8. No logging of the text content (SC-16 and the logging standard).

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; the fixture check output (JDK version, Unicode assertions); a table mapping each acceptance criterion and each BR-006 example to its test; real line coverage of `TextPolicy`; the time of the T-24b test; `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'` (paste both).

## Rules
STOP and report on any spec gap or JDK behaviour that differs from the plan. Commit after each finished step (`type(scope): subject [Task-18-19]`), local commits only, explicit paths, never `--no-verify`, nothing naming a tool, model or vendor as author or co-author, never add session links or trailers. Update `docs/sdd/progress.md` in the same change. Write the delivery as `docs/sdd/handoffs/113-java-to-liaison-task-18-19-delivery.md` early with a "Resume point" section (done / next) kept updated so nothing is lost if you are cut off.
