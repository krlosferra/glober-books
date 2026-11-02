# Handoff 059: Task 05 audit (error foundation)
- From: liaison | To: orchestrator | Type: REVIEW_REPORT | Task: 05 | Branch: `feature/task-05-error-foundation`
- Mode F (Layers 1, 1b, 2, 3). Security verdict still required from `sdd-security-reviewer`; not replaced here.

## Verdict: ACCEPT WITH FOLLOW-UPS

Criteria 1-4 of tasks.md Task 05 are met against Appendix A. I checked the 16 codes, statuses, the 8 Appendix A texts (exact), the envelope (`error`, `code`, `timestamp`, `details` only when non-null), the clock-based UTC `.SSS'Z'` timestamp, the explicit `application/json; charset=UTF-8`, and the body-only write. The orchestrator's green `verify` and clean commits are taken as given (Layers 2 and 1b).

## Rulings
1. **8 invented texts: acceptable, not a spec gap.** Appendix A says "Text below is informative" and the `error` text is never used for logic (US-013.1 maps by `code`). All 8 are fixed, free of input, class names and paths, and the tests cover them. Optional, non-blocking: the PM may add them to the Appendix A text list so the catalog is complete (MINOR; PM-owned `spec.md`). Not a gate.
2. **Writer with its own `JsonMapper`: acceptable.** It keeps the envelope independent of global Jackson settings, which supports 5.0 (snake_case, UTF-8 text). The SpotBugs reason is legitimate. Condition: Task 06 must not configure a second mapper for errors, and the Task 06 tests must show the writer output equals the advice output.
3. **`getOutputStream()` constraint: accept, record as a Task 06 rule.** Put in the Task 06 brief: filters, the wrapper and the advice must never call `getWriter()` before the writer. Task 06 needs a test with a committed or partially written response (`resetBuffer` does not help once committed) to prove a single envelope.
4. **Carried cleanups (System.out removed, scan independent of working directory): in scope as brief-authorized carried items; accepted.** Anything beyond those two stays out of scope. No new cleanup is requested.

## Follow-ups (non-blocking, route to the owners)
5. **DUPLICATE_BOOK `details.existing_book` has no shape yet.** `ErrorBody` only carries a list of `{field,message}`. Criterion 1 says "the shapes of Appendix A", and `DUPLICATE_BOOK` needs `{existing_book}`. Tasks.md line 320 gives it to the duplicate task, so Task 05 is not at fault. Condition: that task must add a typed record (no `Object`, criterion 3) and a factory overload. The PM should confirm that the task text names the `ErrorBody`/factory change as part of its file list.
6. **The factory does not enforce "details only for the codes that have them".** `create(RATING_NOT_ALLOWED, list)` is accepted, and the 1 to 3 and exactly-one size rules are not checked. This is MINOR here because callers are the Task 06+ code. Recommend a guard (details allowed only for VALIDATION_ERROR, INVALID_STATUS, INVALID_RATING, with the size rules) to be added when the first caller arrives, or covered by the advice tests.
7. **Process.** Security verdict, then push and PR by the orchestrator after Sponsor approval. Progress.md and status.md were updated by the implementer; the orchestrator sets the status to DONE after approval.
