# Handoff 084: Security review, Task 12 (page shell with minimal list and add)
- From: security
- To: orchestrator
- Phase: 4
- Type: AUDIT_REPORT
- Branch: `feature/task-12-page-shell-minimal`

## 1. Scope and method
Read: `frontend/index.html`, `css/styles.css` (grep for url/@import), `js/main.js`, `js/ui/{dom,listView,addForm,app}.js`, `copy.js` diff (constants only), `js/sources.test.js`, handoff 082 (incl. exploration evidence), `HeaderPolicy.java`. Ran a grep for the SC-09 sinks over `frontend/` excluding tests (zero matches) and `node --test "frontend/**/*.test.js"` (43 of 43 pass, including "production sources contain no forbidden sink"). I did NOT re-run the browser; the XSS and CSP evidence is the scripted Playwright run in 082 (see section 4).

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-130 | LOW | Headers | `backend/.../config/transport/HeaderPolicy.java` line 21-22 (`PERMISSIONS`), constitution line 197 | Chromium logs `Unrecognized feature: 'bluetooth'`. Exact value sent: `camera=(), microphone=(), geolocation=(), payment=(), usb=(), serial=(), bluetooth=(), clipboard-read=(), display-capture=(), fullscreen=()`. The directive `bluetooth` is ignored by Chromium, so it gives no protection there (the other nine parse), and it makes the console non-clean. The constitution and SC-10 name `bluetooth` explicitly, so the code matches the constitution; the constitution value is the defect. | HeaderPolicy.java lines 21-22; 082 console line | None (no data exposure; the other policy members are applied). Risk is a false assurance and a console warning that can mask a real warning in E2E "zero console errors/warnings" checks. | Sponsor decision to amend constitution line 197 and SC-10: drop `bluetooth` from the required list (or make it "where supported"), then change `PERMISSIONS` and `HeaderPolicy` tests (`HeaderPolicyTest` line 124). Until then, E2E console assertions must filter this one known warning by exact text, not by pattern. Needs the Java architect, so a change request. | SC-10 |
| SEC-F-131 | INFO | Process | 082 "Browser exploration" | The exploration used a scripted Playwright run, not the tool server (DoD deviation). Safe: throwaway script outside the repo, pinned 1.64.1, local port, temp dir deleted. The Liaison rules on the deviation. | 082 | n/a | Evidence is taken as reported; see Not verified. | SC-12 |
| SEC-F-132 | LOW | Carry-over | `addForm.js`, `app.js` | SEC-F-094/095 conditions (server `fields[].message` and error text via `textContent` only) are not exercisable here: no server error text is rendered in this slice (add failure is silent, load failure clears the loading text). Nothing unsafe exists, nothing is verified. | Reading `addForm.js` (no error branch), `app.js` | None now | Carry both conditions to the task that adds error display (Increment 1): render only via `setText`, never `fields[].message` into attributes, URLs or `title`; add an XSS test with the payload in a `message`. | SC-09 |

No CRITICAL, HIGH or MEDIUM finding.

## 3. Controls checked
- **SC-09 VERIFIED (for this slice):** grep of production `frontend/` for `innerHTML|outerHTML|insertAdjacentHTML|document.write|eval(|new Function|string timers|import(|localStorage|sessionStorage|location|setAttribute|http(s)://|on*=|style=` returned no match. All book text and the announcement are written with `textContent` (`dom.js` `setText`/`announce`; `listView.js` entries via `<template>` clone and `setText`). Book data goes through `replaceChildren`/`prepend` of DOM nodes only. No dynamic import, no storage, no location read. `sources.test.js` enforces this as a test (it also self-tests the checker). Residual: it is a heuristic regex check; bracket access built by concatenation is flagged only heuristically (its own test says so).
- **SC-10 / CSP VERIFIED for the page:** `index.html` has one same-origin stylesheet and one module script (`/js/main.js`), no inline script, style, `style=` or handler, no `<base>`, no external URL; CSS has no `url(` or `@import`. The header value in `HeaderPolicy.java` matches the constitution (script-src 'self', style-src 'self', form-action 'self', base-uri 'none', `require-trusted-types-for 'script'`, frame-ancestors 'none'). Runtime: 082 reports CSP violations `[]` after load and add. Trusted Types is satisfied because no sink is used.
- **Form VERIFIED by reading:** `method="post"`, no `action`, `novalidate`; `submit` handler calls `event.preventDefault()` first, then the `inFlight` guard, button disabled while busy, reset on success. Enter sent exactly one POST (082). A form that fell through would still post to its own URL under `form-action 'self'` (data stays same-origin; there is no cookie).
- **XSS evidence (from 082, not re-run):** payload `<img src=x onerror=alert(1)> Dune` shown as literal text in the entry and in the `Book added:` status region; injected `img` count 0; no dialog reported.
- **No third-party request:** only `/css/styles.css`, `/js/main.js`, its module imports and `/api/v1/books` (082 criterion 4; code has no other URL). No cache-busting query strings or timestamps in the page (grep: none); `Cache-Control: no-store` is the backend's job (SC-10, verified in Tasks 06 and 08).
- **Focus handling:** `title.focus()` after each add attempt; `autofocus` on Title; `h2#books-heading` has `tabindex="-1"` (programmatic focus only). No `tabindex` greater than 0. Not a security issue; no focus trap.
- **Permissions-Policy warning:** see SEC-F-130.

## 4. Not verified
- No browser run by me: the XSS inertness, CSP console, single POST and 360 px results rest on the scripted run in 082 (a trusted but unrechecked report). Not covered by that run either: entries with a rating, failed load, 320 px.
- No server error or `fields[].message` rendering (SEC-F-132).
- Public advisory databases were not queried (no dependency change in this task; `frontend/` has no npm runtime dependency).
- The commit hygiene (no attribution, identity) was not audited in this review; the pre-push guard covers it.

## 5. Residual risks for the Sponsor
- SEC-F-130: choose between amending the constitution (my recommendation: drop `bluetooth`, since Chromium ignores it) or accepting a permanent known console warning. LOW.

## 6. Verdict
**PASS WITH CONDITIONS.**
1. Liaison rules on the scripted-run deviation (SEC-F-131); if it requires a tool-server run, I re-check the XSS and CSP lines against that output.
2. SEC-F-130 is raised as a change request / Sponsor decision; it does not block this merge. E2E tasks (13, 14, 28) must filter that exact warning until it is fixed.
3. SEC-F-132 carried into the error-display task.

## Appendix: exact text for `security.md` (not applied by me)
- Findings log (§8), add rows: `SEC-F-130 | LOW | Permissions-Policy lists bluetooth, which Chromium ignores with a console warning ("Unrecognized feature: 'bluetooth'"); header value in HeaderPolicy.java line 21-22; constitution line 197 names it | OPEN (Sponsor decision, change request) | SC-10`; `SEC-F-131 | INFO | Task 12 exploration done by scripted Playwright, not the tool server | OPEN (Liaison rules) | SC-12`; `SEC-F-132 | LOW | SEC-F-094/095 conditions not exercisable until error display exists; carry to Increment 1 | OPEN | SC-09`.
- SC-09 row, append: `2026-11-05 Task 12 (084): VERIFIED for the page shell: grep of production frontend/ finds no forbidden sink; all book text and the announcement via textContent; sources.test.js green (43 of 43); XSS payload inert per scripted browser run (082), not re-run by Security.`
- SC-10 row, append: `2026-11-05 Task 12 (084): page side VERIFIED: no inline script or style, no external URL, CSP violations [] in the scripted run; Permissions-Policy bluetooth warning, see SEC-F-130.`
- Status: Task 12 Security column: `PASS WITH CONDITIONS (084)`.
