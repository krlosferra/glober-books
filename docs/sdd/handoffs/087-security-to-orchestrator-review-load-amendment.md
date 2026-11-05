# Handoff 087: Security to Orchestrator, diff check of the constitution 1.3 amendment (handoff 086)

- From: security. Type: AUDIT_REPORT (diff check). Date: 2026-11-05.
- Verdict: **PASS WITH CONDITIONS** (apply handoff 086 with conditions C1 to C4 below).

## 1. Veto and pre-release (question 1)
Confirmed. The new paragraph keeps `Security: per task` mandatory for input, output, configuration, dependencies, headers, the frontend and any `SC-NN` control. A Security FAIL blocks the increment tag and the release. The Sponsor alone accepts residual risk. I may upgrade any task. The pre-release review and the phase-gate veto (line 194) are untouched.

## 2. security.md reliance (question 2)
- Lines 78 (SC-?? secrets/history), 88 (hooks) and 98 (CODEOWNERS/hygiene) say "every merge" in the verification-frequency column. They refer to automated checks (hygiene workflow, secrets scan, hook tests) and are CI-only. Not displaced.
- Line 149 (R-14) is a Sponsor condition (protected-path list read before merge). It is unchanged.
- SC-19 and SC-36 are verified "every pin change", not "every task". Batching would displace them unless pin changes are per task (see C2).
- No SC row or risk row says "Security review on every task". Nothing else relies on per-task review for increment tasks.

## 3. Open tasks I would not accept as increment (question 3)
Under the rule itself, essentially all open tasks 13 to 47 are per task: 13, 14 (harness, isolation, guard fixture); 15 to 17, 20, 21 (routes, limits, errors, body parsing); 18, 19, 23a, 23b (input/text policy, rules); 24 to 29, 35 to 40, 45 to 47 (frontend); 31, 32, 33, 34a, 34b (tests-only or exercising controls, persistence); 41 to 44 (limits, matrix, axe pin = dependency); 45 (safe rendering). Also 48 (release documentation) per task, because it makes security claims. Tasks 49 and 50 (before the tag) are per task. I would accept as increment only pure internal logic with no output change: Task 22 (service exceptions) and Task 30 (transitions and value types), provided they change no error code, message or status and touch no control. Tasks 51 to 53 (after the tag): 51 per task if it is a control test, 52 per task (frontend); 53 is the final pass (pre-release equivalent, unchanged).

## 4. Wording (question 4)
Mostly unambiguous. Gaps to close:

## Conditions
- C1: Add "tests-only tasks that exercise a control are per task" to the constitution paragraph, as tasks.md already states it (the constitution text does not).
- C2: Add that any change to a pin, lockfile, `.mvn`/wrapper properties, `.npmrc`, workflow, hook, guard or pattern file is always `Security: per task`, regardless of the header. This keeps SC-19 and SC-36 checked on every pin change.
- C3: The PM's marks are proposals. The Security reviewer reviews all headers when `tasks.md` carries them (before dispatch). An unmarked task counts as `per task`. The increment review must be a diff review of every increment task from the previous tag, with the SC rows touched re-verified, recorded as an AUDIT_REPORT, done before the Sponsor tries the increment (tag blocked on FAIL).
- C4: The 086 text "any control (`SC-NN`)" should say "touches or exercises"; and the changelog should note that "Security: per task" tasks still need a Security verdict recorded in `status.md`.

SC-19/SC-36 are not weakened once C2 is applied.

## Not verified
I did not read the constitution or orchestration.md line by line (only 086, security.md and tasks.md); line numbers cited in 086 for those files were not re-checked.
