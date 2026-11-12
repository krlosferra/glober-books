# 186 Security to orchestrator: Task 35 review

- From: security. Type: AUDIT_REPORT. Branch `feature/task-35-client-endpoints`, commit 190decb. Phase 4, task `Security: per task` (frontend, input to paths).

## Scope and method
Read `frontend/js/api/booksApi.js` (getBook, setStatus, setRating, clearRating, deleteBook), `logic/ids.js`, `api/http.js`, handoffs 183/184; ran `node --test js/api/booksApi.test.js` (17 pass, 0 fail). Static review only; no jar was started (the change is client-only and the server rules were read in spec.md, Appendix on content type and bodies).

## Findings
None new (no SEC-F opened).

## Checks
- Path: every new path is `bookPath(id) + literal suffix` (`''`, `/status`, `/rating`). `bookPath` throws TypeError unless the id matches `^[0-9a-f]{8}-...-[0-9a-f]{12}$` (lower-case hex and hyphens only; no dots, slash, percent, `?`, `#`, whitespace, newline since the regex has no `m` flag and `$` with a string ending in `\n` is the only JS-regex edge: JS `$` without `m` does not match before a trailing newline). Hence no traversal, no encoded dots, no query injection. `encodeURIComponent` is a no-op on the validated value.
- Bodies: PUT status sends `{status}` only after the `STATUSES` Set membership check (three literals); PUT rating sends `{rating}` only after `Number.isInteger` and 1..5 (`Number.isInteger` rejects strings, NaN, Infinity, floats, objects). No caller-supplied object is spread into a body. `deps` is spread into `requestJson` options but is a test/seam parameter, and `method`, `path`, `body` come after the spread so a deps key cannot override them (`...deps` is first).
- Content-Type: `http.js` sets `Content-Type: application/json` for every method other than GET, including DELETE without body, which matches the spec rule (every method other than GET/HEAD must send Content-Type, DELETE included). `body` is undefined for GET and both DELETEs, so `init.body` is never set: DELETE has no body byte (spec: any body byte on GET/DELETE is MALFORMED_REQUEST). Tests assert this.
- Server error text: not read. Failures expose only `kind`, `status`, a `code` taken from the fixed `CODES` set, and `fields` (existing behaviour, unchanged); no `message`, `Error` text or response header is read in the new code.
- Refusal: the five wrappers are `async`, so validation throws become rejected promises, raised before `requestJson` runs: no timer, no AbortController, no fetch, so no partial request. The TypeError messages are constant strings that do not include the input value (no leak into logs or UI). Callers must handle the rejection (they pass ids that come from `toBook`, and values from fixed controls); noted as a UI-task integration point, not a finding.
- Sinks: grep of `js/api/` for innerHTML, eval, new Function, document.write, localStorage, sessionStorage returns nothing. No new import beyond `bookPath`. `redirect: 'error'`, `credentials: 'omit'`, `cache: 'no-store'` unchanged.

## Controls
Client path and id handling (BR-011 id format, SC-10 injection/traversal at the client edge): VERIFIED at unit level (source read plus the 17 tests); not exercised against a jar. Content-Type/no-body rule on the client: VERIFIED at unit level; the server side was verified in earlier reviews.

## Not verified
Behaviour in a real browser against the jar (Task 36+ E2E covers it); SEC-F-094 source-scan rule extension still pending and unaffected by this change.

## Closed
SEC-F-097 (INFO): the DELETE and PUT wrappers were added and the non-GET Content-Type rule applies to them (evidence above). CLOSED.

## Residual risk
None new.

## Verdict: PASS
Next step: Liaison ACCEPT, then Sponsor gate. Conditions carried: SEC-F-094 (unchanged).
