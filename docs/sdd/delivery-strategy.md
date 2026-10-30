# Delivery Strategy: working software first, then iterate

- Owner: Orchestrator (proposals that change a binding rule go through the constitution's Amendment Procedure)
- Version: 0.1 (proposal for the Sponsor)
- Last updated: 2026-10-30
- Status: Draft, pending Sponsor decisions (section 8)

## 1. Why this document exists
GloberBooks is a course exercise: a personal reading list that runs **locally**, as one process with an **embedded SQLite file**, for one user on one machine. The governance built so far (constitution, security controls, a 57-requirement spec with a 104-row test appendix) is more rigorous than the product strictly needs. That rigor is useful for the exercise, but only if it ends in **running software we can iterate on**. A perfect specification of software that does not exist yet has no value. This strategy fixes the order of work and the definition of "good enough" so that development starts soon and improves in short loops.

## 2. Principles
1. **Walking skeleton first.** Prove the whole path end to end (build, start, API, database, page, tests, checks) with the thinnest possible feature before building any feature fully.
2. **Thin vertical slices.** Each increment delivers something a person can run and try, not a layer.
3. **Good enough to iterate beats perfect.** Every increment ends with software that runs and passes its gate. Improvement happens in the next increment, from evidence (a failing test, a defect, the Sponsor trying it), not from more upfront review.
4. **Risk-proportional rigor.** The threat model is a localhost, single-user, no-authentication tool. Controls that stop a real attack on that model are automated and mandatory; exotic cases are verified once or accepted and written down. See section 4.
5. **Spec is frozen per increment, not forever.** The spec stays the source of truth, but it changes through small, reviewed change requests discovered while building (Scenario B), not through more speculative rounds.
6. **Timeboxes.** The exercise budget (6 to 8 hours, indicative) is spent on delivering; reviews are timeboxed too.

## 3. What we deliberately do not do (local, embedded, single-user)
No authentication or sessions, no CORS, no pagination, no containers or deployment pipeline, no Actuator or metrics, no database server or migration tool until the first `ALTER`, no cloud services, no second instance on the same database file. These are already decisions or out-of-scope items; this list is a guard against gold plating.

## 4. Risk-proportional test and control tiers
Security and the PM classify every Appendix D row of `spec.md` and every `SC-NN` control into one tier during Phase 3. Starting classification (to be confirmed by the PM and the Security reviewer):

| Tier | Meaning | Examples (by theme) | How it is covered |
|---|---|---|---|
| **P0** | Stops a realistic attack or data corruption on the local model; or is a core business rule | localhost bind, Host allowlist, JSON-only and strict parsing, body cap, error envelope without internal detail, parameterized queries, inert rendering of user text (XSS), invariants (rating only when completed, no negative counts), duplicate and capacity rules, secrets and attribution hygiene | Automated in the real-port security test class, backend tests or E2E; part of every increment's gate |
| **P1** | Valid hardening whose failure is unlikely or low impact locally | container-level malformed requests answered before the application, range and conditional requests on static files, exotic header grammar, very large lists timing | Verified once when the relevant increment lands (a recorded run), then regression only if touched |
| **P2** | Exotic or theoretical cases, accepted limits | confusable characters, lower-casing versus case folding, Unicode table version differences | Documented as accepted risk (already recorded), no dedicated test |

Rule: a P1 or P2 row never blocks an increment. A P0 row always does.

## 5. Increments
Each increment is a branch series merged by pull request, tagged `v0.N`, runnable with `cd backend && ./mvnw spring-boot:run` (page at `http://127.0.0.1:8080/`), and ends with a short note in `progress.md`.

| Increment | Goal | Includes (by theme, exact tasks come from Phase 3) | Done when |
|---|---|---|---|
| **0. Walking skeleton** (this is Task 1) | Everything wired and green with one thin feature | Maven scaffold and pins verified, `schema.sql`, config and mandatory properties, transport filter (headers, Host, content type, body cap), error factory and global handler, add and list books through API and page, JaCoCo, Checkstyle, a minimal real-port security test, one Playwright smoke | `./mvnw verify` green, `npx playwright test --grep @smoke` green, the page lists and adds a book, `hygiene` green, README run instructions work on a clean clone |
| **1. Core library** | The reader can manage a list | Full add validation, list with filter and ordering, duplicate detection and confirmation, capacity rule | All Must stories of this slice pass their API and E2E tests |
| **2. Progress and rating** | Track reading and rate | Status change, rating set and clear, invariants on regression, delete, get by id | Invariant and rule tests green |
| **3. Interface quality** | Usable and accessible | Confirmations, error and empty states, loading states, accessibility and responsive criteria, remaining E2E breadth | P0 UI criteria green; cut-list items scheduled or cut |
| **4. Hardening and wrap-up** | Close P1 items and documentation | P1 verifications, README and API doc, final coverage, security pre-release review | Release checklist (section 6) |
| **Phase 5** | Drift exercise | Author bio through change control | As in the exercise brief |

Increments 1 to 3 can run backend and frontend tasks in parallel once the contract of the increment is stable (the orchestrator's existing rules apply).

## 6. Definition of "good enough" for the exercise release
1. All Must stories pass their automated tests; the cut list of `spec.md` section 1 is applied if time runs out.
2. `cd backend && ./mvnw verify` green with coverage at least 80%; `npx playwright test` green; frontend logic tests green.
3. All P0 rows green on the pinned environment; P1 rows verified once and recorded; P2 documented.
4. The application starts from a clean clone with the commands in `README.md` and no manual steps beyond installing the pinned JDK and Node.
5. Constitution, spec, plan, tasks and status board describe the final code (documents updated in the same branches).
6. Security pre-release verdict PASS or PASS WITH CONDITIONS with every condition closed; no open CRITICAL or HIGH.

Anything beyond this is the next iteration, recorded in the backlog, not a blocker.

## 7. The iteration loop
For each increment:
1. **Plan (Phase 3 for increment 0, then 15 minutes per increment):** the PM orders the slice from `tasks.md`; the Liaison writes the task briefs.
2. **Build:** implementers work task by task on short branches; the orchestrator verifies by running the build itself.
3. **Gate per task:** Liaison audit and Security review exactly as the constitution requires; rejections return to the same implementer (target at most two attempts on average).
4. **Try it:** at the end of the increment the Sponsor runs the application and tries it (about 10 minutes). Findings are classified as in `orchestration.md` section 5 (bad code: fix; bad spec: change request).
5. **Adjust:** the PM re-prioritizes the next increment from what was learned; the plan and tasks are amended only where reality demanded it.
6. **Tag and record:** tag `v0.N`, update `status.md` and `progress.md`.

Spec defects found while building are cheap to fix when found; that is the reason to start building before the spec is perfect.

## 8. Decisions requested from the Sponsor
| # | Decision | Recommendation |
|---|---|---|
| S-1 | Close Phase 2 after review round 2 unless a BLOCKER remains (no round 3 for minor findings; they become change requests during the build) | Yes |
| S-2 | Adopt the P0, P1, P2 tiers of section 4 and let the PM and Security classify the Appendix D rows in Phase 3 | Yes |
| S-3 | Adopt the increments of section 5, with the walking skeleton as Task 1 | Yes |
| S-4 | Adopt the "good enough" release definition of section 6 | Yes |
| S-5 | Lean planning: `plan.md` of 3 to 5 pages and tasks grouped by increment (as the exercise states) rather than a fully detailed plan for every increment upfront | Yes |
| S-6 | Review load per task (proposal, needs a constitution amendment if accepted): the Liaison audits every task; the Security reviewer reviews every task that touches input, output, configuration, dependencies, headers or the frontend (already the rule) and otherwise once per increment | Yes, after Phase 2 closes |

## 9. Next actions
1. Finish review round 2 of the spec; close Phase 2 per S-1.
2. Phase 3: backend plan (Java architect), frontend plan section (frontend developer), `tasks.md` grouped by increment (PM), tier classification (PM and Security); Liaison and Security review; Sponsor gate.
3. Start Increment 0.
