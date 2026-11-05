# Handoff 085: PM to Orchestrator, task merges and Security classification

- From: pm | To: orchestrator | Date: 2026-11-05 | Type: REPORT
- Document changed: `docs/sdd/tasks.md` 0.5.1 to 0.6 (definitions only; no state edited)

## Merged tasks (Sponsor, option B of section 9)

| Merged task | Estimate | Branch | Depends on | Layer |
|---|---|---|---|---|
| 18+19 text policy | 90 (long) | `feature/task-18-19-text-policy` | 01 | backend |
| 39+40 E2E status, rating, delete | 70 (long) | `test/task-39-40-e2e-status-rating-delete` | 37, 38, 31, 14 | frontend |
| 41+42 capacity and release sweep | 75 (long) | `chore/task-41-42-capacity-and-release-sweep` | 31, 23b, 34b, 10 | backend |
| 49+50 raw-socket and grammar | 90 (long) | `test/task-49-50-raw-socket-and-grammar` | 16, 17, 18+19 | backend |

Dependents repointed: 20, 22 (to 18+19); 43, 44, 45, 47 (to 39+40); 48 (to 41+42 and 49+50); 51 (to 41+42). Tier register, traceability, Tasks 36 to 38 "Tested by" and section 1 updated. A merged task that overruns is split back (noted in section 0 and section 9).

Not merged (files rule, section 0, is not waived): 15+16 (6 production entries, 9 criteria, both at ceiling, drip-request spike in 16); 30+31 (5 entries, 8 criteria, 31 at ceiling). Reasons are in section 9.

## Totals (recomputed from estimates)

Task count 52 (was 56): 28 backend, 24 frontend; release scope 49 tasks (27 backend, 22 frontend). Increment 0: 15, Increment 1: 15, Increment 2: 11, Increment 3: 7, Task 49+50: 1, after tag: 3. Total minutes unchanged at 2,265 (release 2,145, about 35.75 h; Increments 0 to 3 2,055; backend release 1,240). Review cycles are four fewer.

## Security classification (open tasks; Tasks 01 to 12 not marked)

| Security: per task | Security: increment |
|---|---|
| 13, 14, 15, 16, 17, 18+19, 20, 21, 22, 23b, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34a, 34b, 35, 36, 37, 38, 39+40, 41+42, 43, 44, 45, 46, 47, 49+50, 51, 52 | 23a (internal add rules in service and repository), 48 (documents only), 53 (progress note only) |

Judgement calls for the Sponsor or Liaison to challenge: 18+19 and 30 are logic but define accepted input (per task); 22 shapes the 409 body (output, per task); 23a is internal (increment) while 23b carries the 2 s add bound and the filter (per task); 32 and 33 are tests-only controls (per task).

## Notes

The Definition of Done (section 0) now holds the review-load rule, marked as Sponsor-approved with the constitution amendment in progress; handoffs 086 and 087 concern that amendment and were not edited. `status.md` rows for merged tasks are not yet created (orchestrator, at dispatch).
