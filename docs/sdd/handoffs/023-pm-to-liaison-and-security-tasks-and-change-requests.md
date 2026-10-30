# Handoff 023: Tasks, tiers, frontend answers and spec change requests (Phase 3)
- From: pm
- To: liaison (copy to security, orchestrator)
- Phase: 3   Round: 1 of 3
- Artifacts: `docs/sdd/tasks.md` v0.2 (new), `docs/sdd/spec.md` v0.4.2 draft (change requests CR-1 to CR-9), inputs: handoff 022 (lean backend plan, replaces 020), handoff 021 (frontend plan), `delivery-strategy.md` v1.0, `constitution.md` 1.1
- Type: REVIEW_REQUEST
- Nothing else was edited. `requirements.md` is unchanged (no requirement changed status). `status.md` is not touched: the orchestrator adds the backlog rows.

## 1. Summary of `tasks.md`

Tasks are merged per increment from the 27 backend tasks of 022 (kept as "Source: Bnn") and the 28 frontend candidates of 021 (kept as "Source: Fn.n"), numbered 01 to 53 in build order.

| Increment | Tasks | Backend | Frontend | Effort | Elapsed (two agents, no review waits) |
|---|---|---|---|---|---|
| 0 Walking skeleton | 14 (01 to 14) | 9 | 5 | 9.8 h | about 7.2 h |
| 1 Core library | 15 (15 to 29) | 9 | 6 | 10.4 h | about 6.9 h |
| 2 Progress, rating, durability | 11 (30 to 40) | 5 | 6 | 7 h | about 3.8 h |
| 3 Interface quality, release gate | 8 (41 to 48) | 2 | 6 | 5.1 h | about 3.9 h |
| **Release (0 to 3)** | **48** | 25 | 23 | **about 32 h** (backend 17.25 h, about 21 h with the architect's rework; whole release about 39 h with rework) | **about 22 h** (25 to 27 h with rework) |
| 4 P1 hardening | 5 (49 to 53) | 3 | 2 | 3.5 h | about 2.5 h |

Decisions and objections the reviewers must check:
1. **Files rule (022 question L-1): accepted.** At most 4 production and resource entries per task, own tests listed separately. My remark: it hides test volume, so the 45-minute estimate is the binding limit and an overrunning task is split. I applied the same rule to the frontend (021 counted tests as files). An "entry" may be a group of closely coupled files in one folder.
2. **Increment 0 has 14 tasks, not about 8.** 022's eight tasks B01 to B08 end at an API with no page. The Sponsor's thin skeleton also needs the page served (B09), the E2E toolchain, a logic and client base, the page shell, the harness and the smoke with the README (5 frontend tasks). I tried every merge: the floor at 45 minutes per task is 12; below that the 45-minute limit is broken. Objection stated, not hidden. Alternative that keeps eight: Increment 0 = Tasks 01 to 08 exactly as in 022, and a "0b" (Tasks 09 to 14) for the page and smoke.
3. **Total 53 tasks, not about 40.** Section 9 of `tasks.md` lists eleven merges giving 42 (60 to 90 minutes each).
4. **Deviation from 022:** the release sweep (SpotBugs triage, grep gates, tree against baseline, coverage review, README lock note) is split out of B27 into Task 42, Increment 3, P0, because release criteria 5 and 6 need it before the Security pre-release verdict. Long runs stay in Increment 4 (Task 51). 022's B25 and B26 were regrouped into Tasks 49 and 50.
5. **Reordering for dependencies:** frontend tasks 10, 11 and 12 can run in parallel with the backend from the start (the contract is final); the harness (13) needs the jar (08), the smoke (14) needs the static controller (09). Backend task 17 (B12, container error path and `StartupPolicy`) stays in Increment 1 as in 022 (note N-4 of 022: Security to accept the placement).
6. **P1 rows after the release (022 question P-2):** the spec's cut sentence allows only measurements to be cut. Tasks 49 and 50 are not measurements, so I recommend running them (90 minutes) before the release tag; otherwise the delivery report lists those rows as open.

## 2. Tier table: differences for Security

`tasks.md` section 7 holds the full tables (Appendix D families by task; every UI criterion). 022 adopted most of my round-1 differences, so what remains:

| # | Row | Architect (022 / 021) | PM | Reason |
|---|---|---|---|---|
| D1 | E-36 third row (2,000 query pairs: `400 MALFORMED_REQUEST`) | P1 with the rest of E-36 (B25) | P0 | application-answered query-shape rule of BR-012 step 4, built in Task 16 |
| D2 | S-09, 20 iterations | P1 (B27); B21 omits it | P0 | INV-1 and INV-4 under concurrency are invariants; 200 iterations stay P1 |
| D3 | Release sweep | Increment 4, P1 | Increment 3, P0 (Task 42) | release criteria 5 and 6 |
| D4 | US-011.11 in 021 F4.6 | listed | none | typing slip; US-011 has criteria .1 to .10 |
| M | Moves from the starting classification of `delivery-strategy.md`, made by the architect in 022 and agreed by the PM: E-22 live, E-30, E-33, T-23 to T-26 and T-24b at unit level, T-31, C-05 size bound, C-08, C-11 bounds and assertions, C-12, C-13 (5 iterations) | P1 to P0 | PM concurs | on the never-cut list, or the only proof of a Must criterion; Security to confirm |

UI criteria: I agree with 021 except the typing slip; US-014.1 stays P0 but conditional on the Sponsor and Security decision about the accessibility checker and the content-security policy (Task 43). US-014.3, .4 (320 clause), .5, US-014.6 bounding box, US-010.2, US-010.9 and US-012.8 are P1 (cut list or low impact); the stylesheet containment rules and the contrast rules inside the accessibility scan are P0 and never cut.

## 3. Answers to the frontend questions (handoff 021 F6.2) and to 022

| ID | Decision | Spec change? |
|---|---|---|
| Q-F1 Control types | Yes: a `select` for the status of an entry and of the add form, a radio group for the rating, native radios for the filter, native `<dialog>`. The spec words ("single-choice control", "labelled group") allow it and the locators depend on it; this belongs to the plan | No (plan matter; the frontend section of `plan.md` records it) |
| Q-F2 Focus after a failure | After "Add anyway" ends in any failure, focus goes to Title (the opener is closed; US-011.8 already returns to Title). Failed delete: the Delete button (opener); failed status change: the status control (opener). Both follow the default rule of US-014.2 | Yes, CR-1 (US-014.2, one clause for the only case no story covers) |
| Q-F3 Loading and adds | While a list load runs, entries and count are removed and only "Loading your books…" shows; an add completed while a load is in progress is not inserted by the form (same as before the first success); a successful list load shows it. Alternative rejected: re-inserting the book into a load that did not contain it (more state, more tests, no story needs it) | Yes, CR-2 (US-010.2 and US-010.7) |
| Q-F4 Client-check wording | Informative; tests assert presence, link to the field, and that no request was sent. The spec convention already makes unquoted text informative; one sentence makes it explicit | Yes, CR-3 (US-011.3) |
| Q-F5 `VALIDATION_ERROR` with unusable `details` | The page shows the `MALFORMED_REQUEST` text of 5.9 (it signals a defect or a tampered page). `INVALID_STATUS` needs no change: its text does not depend on `details` | Yes, CR-7 (Section 5.9 row) |
| Q-F6 Request timeout | 15 seconds, then handled as no response (US-013.2) so no control stays disabled. This is the only new number (UX limit, not a business fact); flagged DEC-034 PROPOSED for the Sponsor | Yes, CR-4 (US-013.6) |
| Q-F7 SC-33 launcher | The wording lives in `constitution.md` and `security.md` (SC-33), not in the spec. My reading: a launcher started by the web-server command that creates the 0700 temporary directory and sets the three variables satisfies the intent if every listed assertion holds and the order is proved by a run (Task 13 AC 3). I do not edit the spec; I propose the wording "`global-setup` or the launcher it delegates to" for the Liaison to carry through the constitution's Amendment Procedure if the run shows the order is the other way round | No spec change; constitution and `security.md` wording proposal |
| F6.3 US-013.4 "one update" | Defined: one replacement of the live-region text; a failure causes exactly one | Yes, CR-5 (US-013.4) |
| F6.3 US-014.2 "trap focus" | Testable reading: repeated Tab and Shift+Tab presses never put focus on a page element outside the open dialog | Yes, CR-6 (US-014.2) |
| F6.3 other items | Contrast of the focus indicator, colour-only, local date, timing, bounding box, double submit, M-1: test strategy only, no wording needed | No |
| 022 P-1 | Add to US-009.5 that a WAL-marked file is "not of the kind used by the application" and that the storage's own rollback of an unfinished journal left by a killed instance is not a violation of "bytes unchanged" | Yes, CR-9 (US-009.5) |
| 022 P-1 / X-01 | E-28: both TRACE rows are pre-application `405`; the `415` expectation is dropped | Yes, CR-8 (Appendix D E-28), logged as DEC-033 (Sponsor decision of 2026-10-30) |
| 022 P-2 | See section 2 and finding 6 above | none |
| 022 S-1, S-2 | Sponsor decisions, not decided here (21 h of P0 backend work against 6 to 8 h; placement of the tag `v0.1`). I recommend the tag after Task 14 because the strategy's "done when" for Increment 0 includes the smoke and the README; options in `tasks.md` section 1 | none |

Rollback journal instead of WAL: no sentence of the spec names a journal mode, so US-009.5 and US-009.7 need no edit for it beyond CR-9. US-009.7 ("claim held from before the first query until it stops") is compatible with exclusive locking.

## 4. Spec change list, v0.4.2 (each carries the inline marker "[CR-n, awaiting diff check]" until the Sponsor approves)

| CR | Location in `docs/sdd/spec.md` | Change | Reason |
|---|---|---|---|
| CR-1 | US-014.2 | after "except where a story names another target (US-011.5, US-011.8, US-012.5)" add: when "Add anyway" ends in a failure (US-011.9, US-011.10) focus is on Title | Q-F2 |
| CR-2 | US-010.2 | add: no book entry and no count is shown until the load ends (a new load removes the entries of the earlier one) | Q-F3 |
| CR-2 | US-010.7 (last sentence) | "Until a list load has succeeded, while a list load is in progress, and again after a load has failed, a book added through the form is not inserted into the page by the form: it is shown by a successful list load, and its confirmation (US-011.5) is still announced" | Q-F3 |
| CR-3 | US-011.3 | add: the wording of the inline messages is informative (tests assert presence, link to the field, no request sent) | Q-F4 |
| CR-4 | US-013.6 | add: a request with no complete response 15 seconds after it was sent is abandoned and handled as US-013.2 | Q-F6 |
| CR-5 | US-013.4 | "one update" defined as one replacement of the text of the live region, not a clearing followed by a filling | F6.3 |
| CR-6 | US-014.2 | "dialogs trap focus (while a dialog is open, repeated Tab and Shift+Tab presses never move focus to an element of the page outside the dialog)" | F6.3 |
| CR-7 | Section 5.9, row `VALIDATION_ERROR` | if `details` holds no usable entry, the page shows the `MALFORMED_REQUEST` text; entries with another `field` are ignored | Q-F5 |
| CR-8 | Appendix D, E-28 | both TRACE rows: pre-application `405`, content type not evaluated; `FOO` and `get`: `405` or 400, 405, 501; the `415` expectation is removed | Sponsor decision; X-01 |
| CR-9 | US-009.5 | "(a file marked as using write-ahead logging is not of that kind)" after "a database of the kind used by the application"; and, inside the parenthesis on companion files, the storage's own rollback of an unfinished transaction left in a journal file by a forcibly terminated instance, restoring the last committed state of a file that was valid before the termination, is not a change in the sense of the criterion | 022 P-1 |

Also changed: header (Version 0.4.2, Status, the note that 0.4.1 governs until approval), Decision Log DEC-033 (CONFIRMED by the Sponsor for the TRACE decision) and DEC-034 (PROPOSED: CR-1 to CR-7 and CR-9, with the 15-second figure the only new number), and the changelog entry for v0.4.2 with one line per change request. Criteria numbering and counts are unchanged (102 criteria, 106 Appendix D rows). No endpoint, limit, code or status other than the E-28 expectation changed.

## 5. What each reviewer must check

**Liaison.** (a) Each CR changes only the sentence listed and only what the answer to its question requires (diff against 0.4.1); (b) CR-2 does not conflict with US-010.4 ("count shown whenever the list is shown") or US-012.7 (reload after `404`); (c) CR-9 wording is correct for a hot journal on a file that is otherwise invalid (the exception is stated for a file that was valid before the termination; confirm that is the intended reading); (d) the files rule and the 14-task Increment 0; (e) the placement of the sweep (Task 42) and the long runs; (f) INVEST size of the dense tasks (01, 03, 06, 08, 14, 21, 34) and the dependencies; (g) the Q-F7 wording proposal for the constitution; (h) that the traceability in section 8 has no gap.
**Security.** (a) The tier tables of `tasks.md` section 7 and the differences D1 to D4 and the moves M; (b) that CR-8 (TRACE answered by the container) and CR-9 (WAL-marked file refused, hot-journal rollback allowed) weaken no control, in particular that CR-9 does not let an invalid file be changed; (c) that Tasks 49 and 50 may stay after the release or must run before the tag (finding 6); (d) US-014.1 conditional on the checker and the content-security policy (Task 43); (e) the 15-second timeout (CR-4) and the focus rules (CR-1, CR-6) for any abuse case.

## 6. For the Sponsor (not decided by the PM)
1. Whether to accept about 21 h of P0 backend work (about 32 h of task time for the release, about 39 h with rework) against the indicative 6 to 8 h; only a scope change could close the gap.
2. Where `v0.1` goes (after Task 08, Task 09 or Task 14; recommended Task 14).
3. Increment 0 with 14 tasks, or the split 0a/0b with eight backend tasks first.
4. The 15-second request timeout (DEC-034) and approval of CR-1 to CR-9 after the diff checks.
5. Whether Tasks 49 and 50 run before the release tag.
6. The accessibility checker and content-security interaction (Task 43) with Security.

## Requested response
Liaison: verdict on `tasks.md` v0.2 and on the diff of `spec.md` v0.4.2 (findings by severity; round 2 only if needed). Security: tier confirmation and the diff check of CR-8 and CR-9, with the other change requests read for abuse cases.
