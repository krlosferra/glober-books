# Handoff 194: Security review, chunk Tasks 37 and 38
- From: security | To: orchestrator | Type: AUDIT_REPORT | Branch `feature/task-37-rating-group`, head 9031665 | Phase 4, chunk (frontend: rating, clear, delete, dialog)

## Scope and method
Read `entryActions.js`, `entryView.js`, `entryOps.js`, `dialogs.js`, `booksApi.js`, `http.js`, `ids.js`, the template and dialogs in `index.html`, handoffs 191 and 192. Grepped all non-test JS and HTML for `innerHTML`, `outerHTML`, `insertAdjacent`, `eval`, `new Function`, `document.write`, storage, `console.`, inline handlers, `javascript:`: no match. Jar `backend/target/globerbooks.jar`: `cmp` of every non-test file of `frontend/` against the jar's `static/` is identical (tree clean at 9031665), so no rebuild. Ran it on 127.0.0.1:18947 (own port), 0700 temp dir under the scratchpad, driven by the pinned `@playwright/test` and `/opt/pw-browsers/chromium-1248` with route stubs for failures. Server and browser stopped (`pgrep` empty), scratch data removed.

## Findings
None new. SEC-F-200 (INFO) closed. No LOW/INFO acceptance used (1 of 5 for the phase remains R-27).

## Evidence (jar, headless Chromium, 1000 px viewport)
- Rendering: titles go through `setText` (textContent) and `setAttribute('aria-label', ...)` only. Hostile title `<img src=x onerror=...>"'` + backtick + `&`: group name is the plain string, 0 `img` elements, `window.__x` stays null in entry, delete dialog, and the "Book deleted: <title>" message. 500-code-point title: dialog `scrollWidth == clientWidth`, no page overflow, Delete button inside the viewport.
- Ids and values: forged `value` (`../x`, `99`, `3.5`, `1e0`, ` 4`, empty) and forged `data-book-id` (`../../x`, empty, `ABCDEF`, `"><img>`, all-zero uuid) with change and click on rating, clear and delete: zero mutating requests, no dialog. `parseRating` accepts only `^[1-5]$`; `setRating` also throws TypeError outside integer 1 to 5; `entryOfEvent` validates the id and looks it up in state.
- Requests: ArrowRight sent one `PUT .../rating {"rating":4}`; clear sent `DELETE .../rating` with `Content-Type: application/json` and no body; delete sent `DELETE` with the same header and no body (captured in the route handler).
- Double clicks and held responses: with a held PUT, repeated clicks, dispatched clicks and ArrowRight sent one request; group disabled while held and enabled afterwards. With a held DELETE, a real click, two scripted clicks and Enter: one request, button disabled, no second dialog.
- Failures (PUT rating): `INVALID_RATING`, `RATING_NOT_ALLOWED`, 500 with JSON, 500 with HTML, malformed 200, network abort: only fixed texts ("Choose a rating from 1 to 5.", "Only completed books can be rated.", generic, "Cannot reach the server..."); server `message` text never shown; radios and delete enabled each time; Messages area untouched. Clear failure (500, network): fixed text, button enabled. Rating 404: "This book no longer exists." in Messages, entry removed, focus on the "Your books" heading. Delete failure (500, network): fixed text beside the entry, Delete enabled, focus on the Delete button, count unchanged. Delete of an already removed book: DELETE 404, one list reload, "This book no longer exists.", focus on heading. Delete of the last book: empty message "Your reading list is empty...", "0 books".
- Dialog: initial focus Cancel; six Tab and Shift+Tab presses and a Tab from `body` stay inside the dialog; Escape and Cancel: zero requests, focus on the "Delete <title>" button; a click outside (backdrop) leaves the dialog open (no outcome without a button or Escape); `dialog.close` returns `confirm` only from the Delete button.
- Storage and console: localStorage 0, sessionStorage 0, document.cookie and context cookies empty. Console held only two browser lines "Failed to load resource ... 404" (the stubbed and real 404s): no title, no server text.
- F-35-1: `send` wraps `request()` in try/catch and maps a rejection to the generic text; `reloadEntry` does the same for `getBook`; the final `ctx.ui.set(..., busy:false)` runs on every path. Verified in code; a real TypeError cannot be forced from the DOM because the guards stop before the call (proved by the forged-value probe: no request, no stuck control).
- SEC-F-200: `setUi` deletes the record when the entry is gone and `removeGone` deletes it; verified in code and by the 404 and delete paths.

## Controls checked (jar level, desktop viewport)
SC rendering/XSS, CSP, dialog and id validation controls for these paths: VERIFIED. No status in `security.md` changed.

## Not verified
Real screen reader and physical keyboard; 320 px viewport (the Liaison F-29-2 item stands); a TypeError rejection injected into the real module graph (covered by unit tests and code reading); 5,000-entry render cost (R-27); public advisory databases (no dependency change in this chunk); SEC-F-094 was not worked: `sources.test.js` is unchanged.

## Residual risks
None new. SEC-F-094 (LOW) stays assigned to Task 45 or earlier. A forged `data-book-id` naming another book in the same page acts on that book through the user's own console: no privilege gained (same-origin, no auth).

## Verdict
PASS. Ledger and `security-log.md` (0.61) updated in the same change; `status.md` Security cells for Tasks 37 and 38 left for the orchestrator to set to PASS. Next: Sponsor gate, then Tasks 39+40 (E2E).
