# Handoff 075: Liaison audit, Task 08
- From: liaison
- To: orchestrator
- Phase: 4   Round: 1
- Type: REVIEW_REPORT
- Mode: F (with G for the property proofs)
- Artifacts reviewed: handoffs 073 and 074, tasks.md Task 08, progress.md 0a note, application.yml, evidence tables in 074 (test sources skimmed only through the evidence, not read line by line; the orchestrator's green `clean verify` is taken as reported)

## Verdict: ACCEPT WITH FOLLOW-UPS (one small fix to the 0a note is required before the Sponsor tries the app)

## Criteria
| # | Result | Note |
|---|---|---|
| 1 | Met | 200, 201, 400 Host, 400 body, 403, 404, 405, 413, 415, 500 (trigger, no detail) asserted; no Server/Set-Cookie/Location/ACAO. SEC-F-121 rows on `/api/v1/books` GET and POST present. |
| 2 | Met | E-04, E-07 to E-14, E-09 (16384 ok, 16385 and chunked 413) in the table. See (b) for the comma Host. |
| 3 | Met with reported deviations | See (b), (c), (d). Next request served after each case. |
| 4 | Met | Round trip POST/GET, 47 checkpoint log lines, no book data. |
| 5 | Met | 20 properties proven; snake_case and unknown field observed over HTTP; sanitisation proven on real log output (MSG-A: all separators replaced; MSG-B exception on one line). |
| 6 | Not met as written | See follow-up 1. |
Layer 1b: 8 commits, human identity, Conventional Commits with [Task-8], no attribution seen. Scope: only listed files plus the authorised application.yml.

## Rulings
(a) Logging pattern and levels: ACCEPT. It is a missing mandatory control (constitution Security Standards, Logging; SC-37), not a wrong key, so it exceeds the task's "edit only if a wrong key is corrected" wording; the orchestrator's authorisation and the report cover the process. Required paper trail: a Plan Amendment line (owner: coding agent/plan.md) and a one-line correction of the Task 08 Production wording ("or a missing mandatory key is added") so tasks.md matches what was done. Also record in progress.md that Task 03b's acceptance missed the gap (a lesson: 03b's checklist should have diffed yml against the constitution list). Residual: the regex contains raw U+2028/U+2029 characters in YAML (not escapes); fragile under editors. Proven by test today, so non-blocking. Logger name and thread are not inside %replace; low risk, note for Security.
(b) Comma Host (E-10): ACCEPT as an R-23 case, but the spec text "exactly, from the application, for the first five" is now false for that row. The PM should amend Appendix D E-10 to: "the comma form is rejected by the container (400, bare page, R-23); the other four rows are answered by the application". Do not change the code.
(c) Container 400 HTML: this is a spec/task wording defect, not an implementation defect. R-23 already accepts a bare container page, and the test asserts the security substance (no Tomcat, no version, no report text, no Server). Amend criterion 3 (PM, tasks.md) to "no product name, no version, no report text, no Server header; a bare container HTML page is accepted for container-generated errors (R-23)". The Increment 1 container error wrapper (if still planned) is where HTML would disappear; link the follow-up to it.
(d) FOO without Content-Type -> 415: ACCEPT. The gate checks Content-Type before method for body-bearing candidates; the response is a safe envelope answer and the server keeps serving. Amend criterion 3 status set to {400, 405, 414, 415, 431, 501} (or add "415 when no JSON content type") and ask the PM to confirm in the 5.0 gate-order rules that content-type precedes method handling for unknown methods. Not a security issue.
(e) Two test classes: ACCEPT. Matches the brief's split rule (security class, properties class), the tasks.md Tests list names them, and one concern per class is cleaner. No action.
(f) 0a note: command set is correct except the jar path. See follow-up 1.

## Follow-ups (numbered)
1. REQUIRED, fix before the Sponsor gate (progress.md, owner coding agent/orchestrator): the note says `java -jar target/globerbooks-backend-0.0.1-SNAPSHOT.jar`, but `pom.xml` sets `<finalName>globerbooks</finalName>`, so the jar is `target/globerbooks.jar`. The `spring-boot:run` path (primary) is correct. Also add: run from `backend/` (the database path `./data` is relative to the working directory), the first start creates `backend/data/`, and port 8080 must be free. State what the Sponsor should see on duplicate or invalid input only if wanted; not required by criterion 6.
2. Plan Amendment and tasks.md wording for (a) (coding agent for plan.md; PM for tasks.md).
3. PM edits for (b), (c), (d): spec Appendix D E-10; tasks.md Task 08 criterion 3 (and the Increment 1 wrapper task if one exists, so "no HTML" is verified there).
4. Ask `sdd-security-reviewer` for the Task 08 verdict (not yet in the record) including the unescaped Unicode in the log regex and thread/logger name outside %replace.
5. The 1 skipped integration test in `SqliteStoreIntegrationTest` pre-dates Task 08; confirm it is intentional (Test Gate: no silent skips). Non-blocking.
6. Evidence quality: the evidence shows `???` where the dash is encoded (console charset); cosmetic only.

## Next step
After follow-up 1 is corrected and Security gives a verdict: Sponsor gate for 0a, tag `v0.1`, then PR per git-workflow. Remind: update progress.md and commit `[Task-8]`.
