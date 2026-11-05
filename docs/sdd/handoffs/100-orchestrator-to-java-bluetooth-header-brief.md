# Handoff 100: Task brief, header value fix (SEC-F-130, constitution 1.4)
- From: orchestrator (compact brief; the Liaison audits against it)
- To: java (this micro-task also touches one e2e file: the orchestrator authorises that single edit because it is the mirror of the backend change)
- Phase: 4   Round: 1
- Type: TASK_BRIEF

## Task
Sponsor decision of 2026-11-05 on SEC-F-130: remove `bluetooth=()` from the `Permissions-Policy` response header value, because Chromium does not recognise the feature and logs a console warning on every page load. The constitution is already amended on this branch (1.4: `docs/sdd/constitution.md` and the SC-10 row of `security.md` no longer list bluetooth). Branch `chore/permissions-policy-bluetooth` is checked out in `/home/user/glober-books`; Tasks 01 to 15 are merged. Security review: per task (it touches headers).

## Scope (exactly these edits)
1. `backend/src/main/java/com/globant/globerbooks/config/transport/HeaderPolicy.java`: the `Permissions-Policy` value becomes `camera=(), microphone=(), geolocation=(), payment=(), usb=(), serial=(), clipboard-read=(), display-capture=(), fullscreen=()`.
2. `backend/src/test/java/com/globant/globerbooks/config/transport/HeaderPolicyTest.java` (and any other test that asserts the old value: grep `bluetooth` under `backend/src`): expected values follow the constitution 1.4 list; add or keep an assertion that the header value does not contain `bluetooth`.
3. `e2e/support/fixtures.ts`: remove the SEC-F-130 filter constant and its use (the guard no longer tolerates any Permissions-Policy console message), remove `bluetooth` from the list of features the guard requires in the header, and update the comments.
4. `docs/sdd/progress.md`: replace the line about the guard tolerating exactly one console message (SEC-F-130) with a note that the header value was corrected (constitution 1.4) and the filter removed.
Nothing else: no other header, no other feature, no new dependency, no change under `backend/config/**` or `pom.xml`.

## Evidence required (real output, in the delivery)
`cd backend && ./mvnw clean verify` green; `grep -rn bluetooth backend/src e2e/support e2e/tests` showing only the negative assertion; `npx playwright test --grep @smoke` (Node on PATH: `export PATH=/tmp/claude-0/node24/node-v24.22.2-linux-x64/bin:$PATH`; build the jar first with `cd backend && ./mvnw -q -DskipTests package`) green with NO console warning from the page about Permissions-Policy; a seeded control: temporarily put `bluetooth=()` back in `HeaderPolicy.java`, rebuild, run one smoke test and show that the guard now fails with the real warning text, then revert and show `git status` clean for that file (this also closes the SEC-F-138 follow-up of a seeded wrong-header control); `git diff --stat main..HEAD`; `git log main..HEAD --format='%an <%ae>%n%B---'` (paste both).

## Rules
Commit after each finished step (`type(scope): subject [Header-Fix]`), local commits only, explicit paths, never `--no-verify`, nothing naming a tool, model or vendor as author or co-author. Run `git commit` commands separately WITHOUT the PATH export above (the commit guard rejects any command whose text contains that path). Use port 18080 only for E2E; stop everything you start. Write the delivery as `docs/sdd/handoffs/101-java-to-liaison-header-fix-delivery.md` early with a "Resume point" section (done / next).
