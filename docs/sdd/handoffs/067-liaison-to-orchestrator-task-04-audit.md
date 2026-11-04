# Handoff 067: Task 04 audit
- From: liaison
- To: orchestrator
- Type: REVIEW_REPORT
- Verdict: ACCEPT WITH FOLLOW-UPS (Security verdict still required)

## Method
Read handoffs 065/066, tasks.md Task 04, spec INV-2 and rating rules, plan X-06, and Book, BookStatus, BookResponse, BookRepository; listed BookRepositoryTest cases. I did not rerun the build; I rely on the orchestrator's green `clean verify` and on the delivery's cited outputs.

## Criteria
1. PASS. Validate on STRICT with the community dialect passed with no fallback, and a test compares the stored `sqlite_master` text with the normalised `schema.sql` (AP-12.1). The typeof fallback (ASK FIRST, X-06) was not used, and neither `schema.sql` nor `application.yml` was touched.
2. PASS. 28 rejection cases cover every item in the criterion, plus 6 boundary acceptances and the emoji code-point tests at 500/501 and 200/201. Each rejection case asserts both the exception and zero rows.
3. PASS. Ms timestamps and lower-case status round-trip; the tie test covers equal `date_added` (ids ascending) and the older row last; the derived query orders `dateAdded desc, id asc` (BR-010).
4. PASS with note. `Book` has no setters (protected JPA constructor, getters only). `BookRepository` uses derived queries only, with no `@Query` and no `select *`, and the test plus Checkstyle guard this.

## Rulings
1. Public all-args constructor and getters only: ACCEPT. "No public setters" is satisfied: state cannot be changed after construction until Task 30 adds named mutation methods. The constructor performs no validation by design, and the service and the CHECKs carry the rules. Follow-up: Task 30 must add mutation methods, not setters.
2. `BookResponse` with `Instant` members: ACCEPT as deferred. Task 07 must prove the 24-character `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` wire format and the snake_case names (`date_added`, `date_completed`) with a contract test. The record's camelCase components give the wrong names unless Jackson is configured. Follow-up F1: put this in Task 07's acceptance check.
3. Dialect not in `application.yml`: not a Task 04 defect, because the brief forbade touching `application.yml`. But the plan has no owner (grep of plan.md and tasks.md found only X-06). Follow-up F2: the PM assigns it explicitly to the first task that starts the full application with JPA (Task 07 or earlier, before 34a/34b). Otherwise the full app may start on the default dialect and fail or diverge from the tested configuration. Task 34b should assert the dialect in effect.
4. Rating test changed from `'3'` to `abc`: ACCEPT. STRICT losslessly coerces numeric text, so the original oracle contradicted SQLite semantics, not the spec. Spec INV-2 (integer 1 to 5) is unaffected. A JSON string rating is rejected at the API layer as INVALID_RATING (spec line 237), which is the correct place. Follow-up F3: the Task 09 test list (rating) must cover a JSON string `"3"`, because the database will not catch it.
5. No git diff/log in the delivery: ACCEPT because the orchestrator supplied the facts (11 files, 685 insertions, human identity, no attribution). Process note: deliveries should include `git diff --stat main..HEAD` and `git log --format=%an,%ae,%s main..HEAD`. The extra security.md commit for R-23 is the orchestrator's and is outside Task 04's scope; the PR description must say so.

## Other observations (non-blocking)
- `InstantConverter` line 4/5 coverage: the single uncovered branch is acceptable (JaCoCo gate green).
- `WireConverter` throws `IllegalStateException("Unknown stored status")` on unknown stored data; the message carries no user input and is unreachable behind the CHECK. Fine.
- Layer 1b is blocking: confirm the commit subjects follow `type(scope): subject [Task-4]` (I did not inspect the log).

## Next
Obtain the Security verdict, then the Sponsor's gate, then the PR and squash merge. Update progress.md to ACCEPTED.
