# Handoff 027: PM response to Phase 3 review round 1 (handoffs 025 and 026)
- From: pm
- To: orchestrator (copy to liaison, security, java, frontend)
- Phase: 3   Round: 2 of 3
- Artifacts: `docs/sdd/tasks.md` 0.4, `docs/sdd/spec.md` 0.4.3 (draft), this handoff
- Type: RESPONSE (diff-check request limited to the items below)

## 1. Summary
`tasks.md` 0.4 resolves every PM item. Three tasks were split (03 into 03a and 03b, 23 into 23a and 23b, 34 into 34a and 34b); lettered ids keep every other number stable. Release scope is now Increments 0 to 3 plus Tasks 49 and 50: **53 tasks, 2,150 minutes (about 35.8 h) of task time, backend 1,245 minutes (20.75 h, about 26 h with the architect's 25% rework allowance)**, whole release about 45 h with rework; elapsed about 25.5 h with two agents and no review waits. The Sponsor accepted "about 32 h and about 21 h of backend"; the splits and the added work raise it by about 12%: the figure needs the Sponsor's confirmation (section 5). `spec.md` 0.4.3 changes only marked change-request text and housekeeping. `plan.md` and `plan-appendix.md` were not edited; the changes they need are in section 4.

## 2. Answers by finding

### Liaison (handoff 025)
| Finding | Resolution |
|---|---|
| F-01 | Dependencies added as required: 11 on 10; 17 on 06, 08, 09; 20 on 07; 22 on 04, 05, 06; 28 on 14; 29 on 28; 39 and 40 on 14; 44 on 28; 45 on 27, 38; 48 on 49, 50; 31 on 15, 16. Also 05 on 03a, 04 on 03a and 03b, 23b on 23a and 16. |
| F-02 | Task 15 AC4: the route table holds the routes that exist then; Task 31 lists `RouteTable.java` (edit), depends on 15 and 16, and its AC says `RouteTableTest` still passes with the seven routes. The E-18 and T-14 behaviour is Task 16; Task 31 AC5 says so. |
| F-03 | Task 16 AC2 owns the `INVALID_STATUS` check of the list value (production `QueryPolicy`); Task 23b keeps filtering and order only and depends on 16. |
| F-04 | Task 23a AC2 is limited to add at capacity (C-01, C-02). C-03, C-04 and US-008.3 moved to Task 31 AC4; the Increment 1 gate lists C-01 and C-02, the Increment 2 gate lists C-03 and C-04. |
| F-05 | Task 22 now lists `error/GlobalExceptionHandler.java` (edit) and AC4: an `ApiException` becomes its code and status and the duplicate carries `details.existing_book`. |
| F-06 | Task 23 split into 23a (add rules) and 23b (filter plus the 2-second bound, C-11 a and b). Task 34 split into 34a (directory trust, analyzer) and 34b (validator, hot journal, second instance). The swap-between-check-and-open claim has no mechanism that respects A-09: 34a AC2 proves the identity comparison with two distinct files and records the race as a documented limit; Sponsor agreement requested (section 5). Tasks 03 and 14 are added to the dense list and every task carries the rule "the first failed spike or unplanned fixture problem stops the task for a split" (section 0). 03 was also split (Security, SEC-F-067, SEC-F-069). The ceiling list in section 0 is 01, 03a, 06, 08, 14, 16, 21, 31, 34b; plan N-6 should match. |
| F-07 | Definition of Done rule added once in section 0: a UI task whose behaviour a later task tests is accepted on logic tests, MCP exploration evidence and the named later task; its spec-coverage rows stay not `VERIFIED` until that task is DONE. Tasks 12, 25, 26, 27, 36, 37, 38 say "Tested by Task N" and carry an MCP evidence criterion. |
| F-08 | Release scope = Increments 0 to 3 plus Tasks 49 and 50; totals table and findings restated (53 tasks, 35.8 h). Task 48 now depends on 49 and 50 and is built last in the release; its AC2 shows 49 and 50 as done and 51 to 53 as open. Plan section 7 must repeat the figures (section 4). |
| F-09 | Task 43 drops the pending decision. It states the mechanism of SEC-F-076: first `page.evaluate` without bypass; a bypass only on a dedicated context inside `us-014-accessibility.spec.ts`, never in the config, never for a guard test; server headers never changed; reported to Security and the Sponsor before a bypass is merged. Task 45 runs without bypass. If the scan cannot run without relaxing the application, the Sponsor decides. |
| F-10 | Sponsor item (section 5). Task 13 lists the amendment as a Definition-of-Ready condition and its AC records the observed start order. |
| F-11 | Plan and Sponsor item (sections 4 and 5). On the tasks side Task 04 AC1 now reads "stop and request Sponsor approval", and Task 23b AC3 says the same for the C-11 key-column fallback. |
| F-12 | `tasks.md` section 7 is declared the single tier register; E-04 is assigned to Tasks 06 and 08, E-34 row 1 and T-14b are listed, D1 to D4 and the moves M are marked confirmed by Security. The US-011.11 typing slip is recorded (D4); plan AP-5 and AP-9.6 must be regenerated (section 4). |
| F-13 | Spec CR-5 reworded with the window: the clearing at the start of a new action is not part of a failure's update; one replacement between the arrival of the outcome and the end of its handling. Task 26 AC5 and Task 44 AC4 name the window. Plan AP-8 may keep the separate clear. |
| F-14 | CR-9 closing clause added exactly as proposed (invalid journal ignored; validation judged on the file after the rollback). Task 34b AC3 has the three cases (valid plus journal after a growing add; invalid plus genuine hot journal; planted garbage journal beside a valid and an invalid file). |
| F-15 | Spec: US-010.7 now says the book appears "with the first successful list load that was requested after the add completed"; US-010.10 and US-011.5 name the load-in-progress case; DEC-034 and the changelog name CR-2 as a behaviour change. Plan AP-7.1 needs the rule for a replaced or removed event on an entry already cleared (section 4); Task 25 AC4 states it. |
| F-16 | Plan AP-8 table (section 4). Task 27 AC3 already splits Title versus US-011.6. |
| F-17 | CR-7 definition of a usable entry added to Section 5.9 (field in the three names; for title and author a non-empty string message). Task 24 AC3 and Task 44 AC1 follow. |
| F-18 | Spec: BR-012 step 1(c) excepts pre-application answers (marked housekeeping); header date updated; US-013.6 states that an abandoned request may have taken effect; Section 6 gains the accepted limit (also SEC-F-075). |
| F-19 | (a) and (b) are marked plan assumptions not asserted by tests (Tasks 37 AC4 and 38 AC3). (c) Task 12 AC4 now reads "every request the page makes is same-origin and answered from the static catalog or the API". |
| F-20 | Task 42 AC5 is a check of the README (written in Task 14); Task 08 lists `application.yml` (edit) and `progress.md`; Task 43 leaves the `frontend-testing.md` alignment to Task 48 (listed there); branch types corrected: 13, 14, 43, 52 use `chore/`. |
| F-21 | Task 08 AC1 names the 404 and 500 triggers that exist at 0a, AC6 and section 2 give the exact try-it commands in the `progress.md` note, and Task 14 AC4 does the same for 0b. Recording the 0a/0b split in `delivery-strategy.md` section 5 is for the orchestrator (section 4). The configuration that keeps product and version out of container pages is now Task 06 (SEC-F-066). |
| F-22 | Plan and appendix tidiness is for the architect (section 4). Traceability 8.2 updated here: Task 31 on US-001, Task 52 on US-010, Tasks 37 and 44 on US-013, `RouteTable` on BR-012; N-6 additions in section 4. |
| F-23 | Section 0 Definition of Ready: the Phase 3 gate approves CR-1 to CR-9 and DEC-034 before Task 11 is briefed. |
| F-24 | Tier register 7.2 splits US-010.2: the clause "no entry and no count until the load ends" is P0, the loading text is P1. |

### Security (handoff 026)
| Finding | Resolution |
|---|---|
| SEC-F-066 | Tomcat error-report customizer (`showReport=false`, `showServerInfo=false`, unconditional) moved to Task 06 production; the advice maps not found, method not supported, not acceptable and unsupported media type (Task 06 AC5); Task 08 AC3 adds the raw-socket check (invalid request line, `FOO`, oversize header: status in {400, 405, 414, 431, 501}, no `Tomcat`, version or HTML); Task 08 AC1 names the 404/405 triggers. The wrapper stays in Task 17 with a test that an error dispatch cannot bypass the headers. |
| SEC-F-067 | `StartupPolicy` moved to the new Task 03b (Increment 0, 0a); the URL form is fixed with no parameters and only the file is configurable (AC1). Plan wording to be changed (section 4). |
| SEC-F-068 | Task 34b AC1 and AC2: validator order (`sqlite_master` first with DDL or named-CHECK comparison, then `quick_check`, then rows with `LIMIT 5001`, SQL `length()` and `typeof` before fetching text, Java code-point lengths, no cell value in a reason) and the cases table without CHECKs, extra index, NUL in a title, oversized cell. |
| SEC-F-069 | Task 03a: child-JVM claim proof (AC3), claim and settings at every connection initialization with pool `minimum-idle` 1, `idle-timeout` 0, `keepalive-time` 0, `max-lifetime` 0 and a forced-eviction read-back (AC2), `stat`-only access after the claim and hashing only when no instance holds the file (AC1). |
| SEC-F-070 | Task 13 AC2 and AC3: command-line arguments, allowlisted environment, deletion checks of the `mkdtemp` path, hostile-environment run. The SC-33 sentence is a Sponsor item. |
| SEC-F-071 | Task 41 AC4: fixed `-Xmx` (512 MB proposed, confirmed by the measurement, written into the README run command), eight slow list readers, no `OutOfMemoryError`; Task 07 AC5: the list is written to the response stream, never an intermediate `byte[]`. Plan section 7 must state the bound (section 4). |
| SEC-F-072 | Task 09: catalog takes its resources as a constructor argument, production passes `classpath:/static/`, the test builds the catalog directly, no property; Task 03b AC2 states there is no such property. |
| SEC-F-073 | Task 16 AC5: drip variant on request line, headers and body of a valid `POST`, result recorded, deadline in `BoundedBodyRequest` or an accepted limit reported to the Sponsor; Task 03b AC2: explicit low `max-swallow-size` (16 KiB proposed). |
| SEC-F-074 | Task 34b AC3 (i) to (iii); the header size test is skipped when a non-empty `-journal` exists. CR-9 carries the Liaison's closing clause. R-20 is a Sponsor acceptance item. |
| SEC-F-075 | Accepted. Spec Section 6 limit and US-013.6 sentence added; Task 11 AC5 requires the sources grep to accept `setTimeout(function, 15000)` and flag a string argument. |
| SEC-F-076 | Task 43 AC3 and Task 45 AC2. |
| SEC-F-077 | Task 08 AC6 (progress note) and Task 14 AC3 (README): `v0.1` and `v0.2` are checkpoints not for real data; delete the database before Increment 1. |
| SEC-F-078 | Wording fix is plan work (section 4); Task 03b AC3 and Task 41 state that any change of SC-06 values is ASK FIRST and returns to Security. |
| SEC-F-079 | Task 49 AC2: the test does not assert the `Allow` list of a container answer. |
| Section 5 (D3) | Task 42 AC4 adds `npm audit --audit-level=high` in `e2e/` and the lockfile `resolved` hosts. |

## 3. Spec 0.4.3 edits (each marked "[CR-n, awaiting diff check]" or "[housekeeping, ...]")
| Location | Change | Finding |
|---|---|---|
| Header | Version 0.4.3, date 2026-11-02 | F-18 |
| US-010.7 | "it appears with the first successful list load that was requested after the add completed" | F-15 |
| US-010.10 | add "and no list load is in progress (US-011.5, US-010.7)" | F-15 |
| US-011.5 | add "and no list load is in progress (US-010.7)" | F-15 |
| US-013.4 | window of "one update" | F-13 |
| US-013.6 | the abandoned request may have taken effect; accepted | F-18, SEC-F-075 |
| Section 5.9, `VALIDATION_ERROR` | definition of a usable `details` entry | F-17 |
| US-009.5 | journal not valid for the file changes nothing; validation judged after the rollback | F-14 |
| BR-012 step 1(c) | except pre-application answers | F-18 |
| Section 6 | accepted limit for the abandoned request | SEC-F-075 |
| DEC-034, changelog | CR-2 named as behaviour change; v0.4.3 entry | F-15 |
No other sentence changed. The orchestrator's `git diff` against the 0.4.1 merge commit should show only marked lines, the header, DEC-033, DEC-034 and the changelog.

## 4. Plan changes needed (plan.md and plan-appendix.md; not edited by the PM)
1. Task ids: AP-1 and every task reference follow the lettered ids 03a, 03b, 23a, 23b, 34a, 34b; `StartupPolicy` is created in Task 03b; the Tomcat error-report customizer in Task 06; the step-2 header reader needs a named class (F-22 c; it lives in `SqliteDataSourceProvider`, Task 34b).
2. Plan section 7 and the effort table: release scope = Increments 0 to 3 plus Tasks 49 and 50, 53 tasks, 35.8 h, backend 20.75 h (F-08).
3. D1 and X-11 (F-02): the route table holds the existing routes at Task 15; Task 31 extends it.
4. Native SQL and JDBC paragraph (F-11): each use (pragmas, validator reads, key-row scan if native), explicit column lists, why JPA cannot do it, and that the `SqliteDataSourceProvider` settings are the URL keys or an amendment request.
5. Section 4 and AP-9.2 (F-10, SEC-F-070): launcher with command-line arguments and allowlisted environment; the SC-33 wording after the amendment.
6. AP-5 and AP-9.6 regenerated from `tasks.md` section 7 (F-12); E-04 at Task 08; remove "US-011.1 to .11".
7. CR-9 plan text (F-14, SEC-F-074): outcome for a hot journal beside an invalid file and for a planted journal; skip the header size test when a non-empty `-journal` exists.
8. AP-7.1 (F-15 item 4): replaced or removed events for an entry already cleared are ignored for the list, still announced, no focus move. AP-8 (F-13, F-16): window of the live-region update; Add anyway focus table split between Title and US-011.6.
9. Start order, claim, pool (SEC-F-067, 068, 069): URL form fixed with no parameters, file configurable; validator order (step 4); claim at every connection with `minimum-idle` 1, `idle-timeout` 0, `keepalive-time` 0; `stat`-only access after the claim.
10. D5 and section 7 (SEC-F-071, 073, 078): `-Xmx` bound, response written to the stream, explicit `max-swallow-size`, queue wording ("wait in the Tomcat executor queue", not the OS backlog), the Hikari-timeout caveat at 4,949 to 4,999 books.
11. AP-1 and AP-9 (SEC-F-072): static catalog constructor argument; no property.
12. AP-9.2 and AP-10 (F-09, SEC-F-076): axe decision recorded; stale references (F0.1, F0.2, F0.3, F0.4, F3.1, A1, A2, F4.4, F5, F6, "Task 18 is Task 18", "Task 01 to Tasks 42, 51") replaced by task numbers; `harness.spec.ts` in the AP-9.1 layout (F-22 a, b).
13. N-6 gains Tasks 03a, 14, 23a and 34b (F-22 e). Plan wording that the appendix is part of the plan (F-22 f).
14. Orchestrator: record the 0a/0b split in `delivery-strategy.md` section 5 (F-21); run the `git diff` of `spec.md` (handoff 025).

## 5. Items for the Sponsor (not decided here)
1. **SC-33 amendment** in `constitution.md` and `security.md` (F-10): "created by `global-setup` or, when the web server starts before global setup, by the launcher that is the `webServer.command`", with the launcher conditions of SEC-F-070. Needed before Task 13 starts.
2. **Native SQL and JDBC justification** in `plan.md` (F-11): the constitution allows native queries only when the plan justifies them; the plan author drafts, the Sponsor approves with the plan.
3. **Schema fallbacks are ASK FIRST**: `typeof` CHECKs (X-06) and a stored duplicate key with an index (C-11) change `schema.sql`; Tasks 04 and 23b now stop and ask instead of applying them.
4. **Release scope figure**: 35.8 h of task time for the release scope, backend 20.75 h (about 26 h with rework), against the accepted "about 32 h and 21 h". Please confirm.
5. **Documented limit** for the race between the directory check and the pool opening (Task 34a AC2), and **R-20** (hot-journal recovery before validation) as an accepted residual risk.
6. **Memory and Tomcat values** (ASK FIRST for SC-06 values): `-Xmx` 512 MB (confirmed or changed by the Task 41 measurement) and `max-swallow-size` 16 KiB.
7. **Approval of CR-1 to CR-9 and DEC-034** at the Phase 3 gate before Task 11 is briefed; the 15-second figure remains the only new number.
8. If the Task 43 scan cannot run without relaxing the application, the Sponsor decides (not the task).

## Requested response
Liaison and Security: diff check of `tasks.md` 0.4 and `spec.md` 0.4.3 against the findings above (round 2); the architects: the plan changes of section 4.
