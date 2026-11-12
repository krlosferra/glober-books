# 190 Security to orchestrator: Task 36 review

- From: security. Type: AUDIT_REPORT. Branch `feature/task-36-entry-status`, commit 3bb4b0c. Phase 4, per task (frontend, rendering, dialog).

## Scope and method
Read `entryView.js`, `entryActions.js`, `dialogs.js`, `statusChange.js`, `listView.js`, `app.js`, `copy.js`, the template and dialog in `index.html`, `booksApi.setStatus`, `ids.js`, handoffs 187 and 188. Grepped the non-test JS. Confirmed the jar `backend/target/globerbooks.jar` serves frontend files byte-identical to the tree (cmp on every non-test file). Ran that jar on 127.0.0.1:18933 (own port), temp dir mode 0700 under the scratchpad, driven with the pinned `@playwright/test` and `/opt/pw-browsers/chromium-1248`. Server, browser and scratch data stopped and removed (`pgrep` empty).

## Findings
| ID | Severity | Finding |
|---|---|---|
| SEC-F-200 | INFO | `send` can write a `busy` record in the per-entry Map for an id removed while the dialog was open; no DOM or security effect. NOTED, not accepted under 1.5. |

## Evidence (jar, real page)
- Sinks: grep finds no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `eval`, `new Function`, `document.write`, inline handlers, storage or `console.*` in non-test code. Only `setAttribute` use for entries: `aria-label` with `Status of <title>`. An attribute value is not parsed as markup or script; the probe with a title `A" onfocus="window.__x=3` and `<img src=x onerror=...>` gave 0 injected elements/attributes, `window.__x` stayed unset, labels literal.
- Text: titles, authors, status, dialog text via `textContent`; hostile and 500-code-point (non-BMP-free Cyrillic + `<b>`) titles inert, no horizontal scroll at 1280.
- Ids: `dataset.bookId` set only if `isBookId`; `onChange` re-validates the dataset id and looks the book up in state. Forged `data-book-id="../../x"`, forged option values (`"><img>`, `../x`, `delete`, empty) dispatched through DOM: zero requests, no dialog. PUT body only `{status}` from `STATUSES` (setStatus throws TypeError otherwise).
- Dialog: modal (`:modal` true), initial focus Cancel, Tab/Shift+Tab cycle Cancel/Continue only, backdrop click does not close, Escape and a scripted `close()` resolve false (value restored, 0 PUT). Only the Continue click resolves true.
- Races: held PUT, then a forged re-enable plus second change: exactly one PUT (the busy flag from the registry rejects it); the entry shows the server result.
- Failure paths: network abort gives "Cannot reach the server..." beside the entry, select enabled, value restored; stubbed 500 with a hostile message body: only the fixed text, nothing from the body; stubbed 200 with a hostile malformed body: fixed generic text, control enabled; stubbed 404: "This book no longer exists." in Messages, focus on "Your books", list reloaded. F-35-1 met for `setStatus`: `send` catches rejection into a generic failure.
- Storage and console: localStorage, sessionStorage, cookies empty; console only the three "Failed to load resource" lines from my stubs, no title text; no foreign request; no native `alert`.
- Note: one early probe line (abort with no message) was my own artifact (I had appended forged options and the value was already the target, so no request was made); an isolated rerun gave the expected message.

## Controls
SC rendering/XSS/CSP controls for the entry and dialog: VERIFIED at jar level for the paths above (headless Chromium, desktop viewport only; no 320 px run by me). Not changed in `security.md`.

## Not verified
Real keyboard/screen reader behavior; mobile viewport; 5,000-entry render cost (SEC-F-198, R-27); the E2E suite for status (Tasks 39/40 not built); public advisory databases.

## Residual risks
None new. SEC-F-094 (source-gate rules) stays open to Task 45; its real control (hostile titles inert in the browser) held here. F-35-1 remains for Task 37 (`setRating`, `clearRating`, `deleteBook`).

## Verdict
PASS. No condition. Acceptances under 1.5 this phase: still 1 of 5 (R-27). Ledger: F-35-1 retargeted to Task 37; status.md Security cell filled; log line SEC-F-200 and changelog 0.60 added. Nothing committed.
