# Handoff 083: Audit, Task 12 (page shell with minimal list and add)
- From: liaison
- To: orchestrator
- Phase: 4   Round: 1
- Type: REVIEW_REPORT (Mode F)
- Verdict: **ACCEPT WITH FOLLOW-UPS** (Liaison recommendation; Security verdict and Sponsor approval still needed)

## Compliance (read: index.html, app.js, addForm.js, listView.js, dom.js; stylesheet and copy.js diff not line-audited, relied on 43 green tests and the scripted run)
- AC1: lang, title, one h1, main, "Your books" h2 (tabindex -1), `ul` aria-label "Books", form with Title, Author, Status (Unread default), "Add book", autofocus on Title. Met.
- AC2: template clone, `textContent` only (setText), count via `countText`, API order, loading text in a `role=status` element. Met.
- AC3: `preventDefault`, `inFlight` flag, one POST (evidenced), `form.reset()`, prepend, announcement "Book added: <title>", focus to Title. Met. The XSS-shaped title stayed inert (0 injected img, no CSP violation).
- AC4: one module script, one stylesheet, no inline script, style or handler, no base or icon link; `sources.test.js` green. Met.
- AC5: met with a documented deviation (item 1).
- Scope: only the listed files plus the two strings in copy.js; nothing under backend/. Hygiene: author is the human, no trailers (the session trailer was rightly not added; CLAUDE.md and git-workflow forbid it).

## Rulings
1. Criterion 5 by scripted Chromium run: ACCEPT as a documented deviation. The evidence covers the same items (snapshot, keyboard sequence, single POST, focus, CSP console, 360 px). Do not require a tool-server rerun for this task. Task 14 AC (explore the page once with the browser tool) is the place to make up the tool-server exploration; record that in the Task 14 brief. Also record the deviation in progress.md.
2. No empty-state text: ACCEPT. US-010.6 is not in this slice; "0 books" is not misleading. Task 25 or the Increment 1 task owning US-010.6 must add it. Keep the gap on the backlog.
3. Failed load shows nothing: ACCEPT for this slice only (failure messages and "Try again" are Increment 1). Silent failure is not acceptable at the Increment 1 gate.
4. Add during or after a failed load announced but not inserted: ACCEPT with a follow-up. The announcement says "Book added" while the list is missing the book. This is bounded by US-010.7 and the slice, but the owning Increment 1 task must define the reconcile (reload after failure or insert). Record as a Task 25 input.
5. No listState.js: ACCEPT. The single `loaded` flag is small and fits "Tasks 25 owns state"; Task 25 replaces it, do not add a second mechanism then.
6. Status "Messages" region and empty status elements: ACCEPT. Two empty live regions registered before the content is set is the correct pattern for announcements. Minor: the empty `#loading` status exposes an unnamed region in the snapshot; give it an aria-label only if the Task 28 accessibility checker flags it.
7. Visually hidden h2 "Add a book" and the title "GloberBooks": ACCEPT. Not in the spec (AC1 lists only the h1 and "Your books"), but a heading before the form helps navigation and breaks no criterion. Follow-up for the PM: add it to US-011.1 or the section 5.9 texts so the wording is traceable and the Task 28 tests can assert it.
8. Permissions-Policy 'bluetooth' warning: not a Task 12 defect and not a CSP violation. It belongs to the backend header value (Security finding already reported). Route to Security and the backend owner (a small backend task or amendment; header value is a constitution item, so Sponsor approval if the value changes). Task 14's guard fixture (checks console errors) must not turn this warning into a failure; decide before Task 14 whether it is fixed first or allow-listed (the fix is preferred, an allow-list hides drift).

## Follow-ups (numbered, non-blocking)
F1. Before the status moves to IN_REVIEW or DONE: write the progress.md entry (including the deviation of ruling 1) and set the status.md row. The delivery says neither was touched, and CLAUDE.md requires progress.md in the same change as the code.
F2. Not covered by the run: list at 320 px (the 320 clause is excluded from AC1 and belongs to a later task), entries with a rating, a failed load. These must be covered by Tasks 14 and 28 (rating text per US-010.3 is covered only by the format.js logic tests).
F3. Delivery 082 reproduces `git log main..HEAD` only after the final commit; the orchestrator should paste it, as promised.
F4. Layer 3 (non-blocking): after add, focus returns to Title even on failure with the form re-enabled, which is consistent with the brief; revisit when Increment 1 adds error messages (focus should then follow US-011 failure rules).

## Next step
Security verdict on Task 12 (include SEC-F-094, SEC-F-095 and the header warning), then Sponsor approval, then open the pull request per git-workflow, and proceed to Task 13 or 14 per dependencies.
