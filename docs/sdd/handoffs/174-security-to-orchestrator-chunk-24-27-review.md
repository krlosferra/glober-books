# Handoff 174: Security review, chunk Tasks 24 to 27
- From: security | To: orchestrator | Type: AUDIT_REPORT | Phase 4

## Scope and method
Branch `feature/task-24-text-failure-logic`, HEAD 5c30262, diff against 074b8a6 (frontend only: `logic/text.js`, `failures.js`, `listState.js`, `addCheck.js`, `copy.js`, `api/http.js`, `ui/addForm.js`, `app.js`, `dialogs.js`, `listView.js`, `messages.js`, `index.html`, `styles.css`). Read all production files. Built the jar from `git archive HEAD` (`./mvnw -o -q -DskipTests package`, frontend baked in), ran it on loopback port 18977 with a 0700 scratch data directory, drove the real page with the pinned `@playwright/test` and Chromium 1248, with route interception for hostile responses. Ran `node --test "frontend/**/*.test.js"`: 85 of 85 pass, source gates included. All processes stopped, scratch files removed.

## Findings
| ID | Severity | Area | Location | Finding | Evidence | Remediation | Control |
|---|---|---|---|---|---|---|---|
| SEC-F-197 | LOW | Failure handling | `listState.loadFailed` | A failed list load with `VALIDATION_ERROR` or `DUPLICATE_BOOK` stores no text: empty status next to "Try again". The real server never sends these on the list GET; no injection path. | Stubbed 400 and 409 on GET: `#list-status` empty, Try again visible | Fall back to the fixed generic text when the decision has no string text | SC-11 |
| SEC-F-198 | LOW | DoS (UI) | `listView.render`, `replaceChildren` of all entries | 5,000 entries of maximum-size text: 5.9 s first render, 5.3 s filter round trip, 4.8 s one add (headless Chromium). 5,000 typical: 1.0 s; JS long task only 51 to 67 ms, layout dominates. Own tab only, at the capacity limit. | Playwright timings with 5,000 stubbed entries | `content-visibility: auto` plus `contain-intrinsic-size`, or insert only the added entry; measure in Task 28 | SC-09 (availability) |
| SEC-F-199 | INFO | Source gate | `copy.js` HOST_TEXT uses `${'http'}://` | Splits a literal to pass the external-URL gate; static text, not a used URL. Shows the heuristic gate is passable (SEC-F-094). | read | none | SC-09 |

Closed: SEC-F-095, SEC-F-132. Kept: SEC-F-094 (LOW, extend gates; the new files pass and use no forbidden sink: grep shows only `dataset.bookId` after `isBookId` and `setAttribute('aria-describedby'|'aria-invalid')` with constants).

## What was verified (real page through the jar unless marked)
- A XSS and DOM: 15 hostile books (markup, script, handler attributes, `javascript:`, `data:`, template and dialog breakouts, bidi controls, zero-width, NUL, lone surrogates, 16 KiB, 500 marks, ids with markup) rendered as inert text; DOM audit shows no extra element, no unexpected attribute, no `style` attribute; ids that fail the UUID pattern get no `data-book-id`; `window.__pwn` never set. Field messages with markup, 16 KiB, bidi render as text. Hostile `field` names (`__proto__`, `constructor`, `toString`), `details` as object, `__proto__` member in a detail, non-string and array messages, duplicate fields, status without message: no pollution (`({}).polluted` undefined), generic request-defect text or the first usable message; status detail gives the fixed text (CR-7 edit in `parseFields` correct). Unknown or hostile `code` gives the generic text; the server `error` text is never read or shown. Duplicate dialog: hostile title and author with correct shape are text only (9 child elements, no img or script); `existing_book` with non-string fields, an array, a bad status, or a missing id gives the generic text, no dialog; a bad date gives an empty or odd local date as text.
- B CSP: exact CSP header on `/`; 0 `securitypolicyviolation` events in all flows; native `dialog` needs no relaxed policy; no inline style or script made by the code; no request to another origin.
- C State and races: a slow Unread response released after Reading was discarded (list and count stayed "FRESH", 1 book). Click plus five Enter presses plus `requestSubmit` plus synthetic click and submit events while held: exactly 1 POST, button disabled, re-enabled after, focus Title. Add anyway body is exactly `{"title","author","status","allow_duplicate":true}` from the form fields; nothing from the 409 body (hostile `allow_duplicate`, `title`, extra members ignored). Escape (also twice) and Cancel close with false, button re-enabled, no extra POST; outside click leaves the dialog open.
- D Information handling: storage empty (local, session, IndexedDB, cookies); no title on the console; client length checks only produce UX messages and the body is sent unchanged to the server (authority unchanged).
- E Tab wrap: Tab and Shift+Tab cycle Cancel and Add anyway six and four times with no escape to the document; script `focus()` on the inert title field does not take focus; no hidden or inert element receives focus.
- G 5,000 entries: see SEC-F-198.
- F `addCheck.js` is pure and uses `checkTitle` and `checkAuthor`; no sink.

## Not verified
Headed browsers, other engines, screen readers; a real server answering the add flows with a real hostile stored title through the dialog (stubs used; real `409` was only shown by the delivery); heuristic gate bypass paths (SEC-F-094, not extended); the Playwright MCP tool server (scripted Playwright used); timing figures are headless Chromium on this host only; ran as root.

## Residual risks
SEC-F-198 (self-inflicted slowdown at 5,000 large entries) and SEC-F-197 (empty failure text on an impossible server answer): both LOW, carried to Task 28; I do not accept them under constitution 1.5, they are fixed by the next frontend task.

## Verdict
**PASS WITH CONDITIONS.** No CRITICAL, HIGH or MEDIUM finding. Conditions, carried by name to Task 28 and not blocking the merge: SEC-F-197, SEC-F-198 (ledger section 2). Next step: Sponsor gate; the Task 28 and 29 E2E tests must assert the XSS payloads, the zero CSP violations and the single POST on repeated submit on the real jar.
