# GloberBooks: Active Conditions Ledger

- Owner: orchestrator (writes), Spec Liaison and Security reviewer (add and close items in the same change as their report)
- Created: 2026-11-10 (constitution 1.7). Built from `security.md`, `security-log.md`, `status.md`, the Decision Log of `spec.md` and the handoffs up to 161.
- **Purpose:** the single living list of what is still open. Read this instead of old handoffs and instead of the historical findings log. A task brief copies from here the items whose target is that task.
- **Authority:** when a row of `security-log.md` or an old handoff disagrees with this file on whether something is open, this file wins. When you close an item, delete its row here and write one line in "Closed since the ledger was created".
- **Not a log:** closed items are not kept (they live in the handoffs and `security-log.md`).

## 1. Sponsor decisions and actions

| ID | Item | Why it is open |
|---|---|---|
| S-1 | Tag `v0.2` (Increment 0b) on `main` after trying the page | The platform does not let the orchestrator push tags. The security block (SEC-F-178) is lifted. |
| S-2 | Make the GitHub repository private, or accept that it is public | `private: false`: all 188 documents, including the findings and accepted risks, are public. |
| S-3 | Confirm DEC-035, DEC-036 and DEC-037 of `spec.md` at the next gate | Clarifications with no behaviour change, written by the PM, waiting for confirmation. DEC-036: an invalid body at 5,000 books answers 400, not 409. |
| S-4 | Decide SEC-F-126: remove `static-locations` or keep it | Production edit outside Task 41+42; the board says Task 42 decides. |
| S-5 | Platform protection (SEC-F-003, SEC-F-005): protected `main`, `hygiene` as a required check, credential scoping | Repository side done; platform settings are the Sponsor's. |
| S-7 | Delete the leftovers in the container root: `/x` and `/run1.log` | Created by review probes; the environment blocked their removal. Not part of the repository. |

## 2. Conditions assigned to a task

| ID | Severity | Target | Condition |
|---|---|---|---|
| Slow-reader test | LOW | any later backend test change | Assert that the eight slow readers got `HTTP/1.1 200` and more than zero bytes (Liaison F-3 of handoff 162). |
| SEC-F-106 | LOW | backlog, before the tag only if the Sponsor wants it | Handoff 166: `127.0.0.256` is refused (Host 400, bind refused) here, but the start policy still hands an invalid dotted literal to `getByName`, which may do a name lookup. Check octets 0..255 first (a few lines in `StartupPolicy`, a protected surface: reviewed alone). Not accepted by Security (loopback binding). |
| SEC-F-148, SEC-F-196 | LOW, INFO | 49+50 (Liaison) | The body is read up to 16 KiB (10 s) before route, id and query are judged (`RequestGateFilter.gate`); delivery 164 section 7 says the opposite. Correct the sentence; the order is the known body-before-route debt item. |
| T-33 wording, E-28 prelude | INFO | PM (see section 6) | SEC-F-195: Tomcat's first parse error logs one INFO line with the bad header or target. E-28: `FOO` without `Content-Type` gives 415, the Appendix D prelude sends JSON so the row is met. |
| SEC-F-174 | INFO | 51 | Pin one cold-start run for nested JSON (first request of a JVM took 200 to 306 ms; T-31 bound is 300 ms; the PM words T-31 "after the first request of the JVM"). |
| SEC-F-094 | LOW | 45 or earlier (frontend) | Extend the `sources.test.js` rules (template literals, array join, `Reflect.get`, `window.open`, `location`); the E2E XSS test is the real control. |
| SEC-F-095, SEC-F-132 | LOW | 26 (frontend) | Render `fields[].message` with `textContent` only when the error display is built. |
| SEC-F-097 | INFO | 35 (frontend) | Re-verify the non-GET `Content-Type` rule when the DELETE and PUT wrappers are added. |
| SEC-F-138 | LOW | a later E2E task | `Set-Cookie` and `Location` absence assertions in the guard; the foreign-origin check sees http(s) only. |
| SEC-F-133, SEC-F-134, SEC-F-135 | LOW | backlog | E2E launcher: assert mode 0700 and the owner of its temp dir; remove the empty temp dir when its own check fails; SIGHUP is not forwarded and a SIGKILL of the launcher orphans the java process. |

## 3. Standing rules (no task; every reviewer keeps them in mind)

| ID | Rule |
|---|---|
| SEC-F-159 | `@WebMvcTest` slices do not load `WebMvcConfig` and use the lenient `@RequestBody` processor: strict-body claims are proved only by unit or full-context tests. |
| SEC-F-169 | Any future numeric JSON field needs the same text-length check as the rating reader (Jackson `maxNumberLength` counts digits, not the sign). |
| SEC-F-168 | The capacity and duplicate rules depend on the one-connection pool and the exclusive lock: any change of pool size or locking needs a new Security review. |
| SEC-F-180 | Keep `org.hibernate` at INFO; never raise SQL or `org.hibernate.orm.jdbc.error` logging (SC-14, SC-37). |
| SEC-F-093 | Re-run the seeded MD5 check of Find Security Bugs on every SpotBugs bump. |
| SEC-F-083 | Nine transitive coordinates are tree-present and listed; any new transitive coordinate is a finding (baseline `config/dependency-tree.txt`). |
| SEC-F-162 | The duplicate key lower-cases without case folding by decision (DEC-001, T-40): false negatives are spec-accepted. |

## 4. Accepted limits (state them in the release notes)

| ID | Limit |
|---|---|
| R-25 | Race between the guard check and the database open: detected after the open, not prevented (accepted by the Sponsor 2026-11-06). |
| R-26 (E-37 row 1) | `GET  /  HTTP/1.1` (repeated spaces, also a tab, a leading CRLF or a bare LF line end) is served as if it had one separator; Tomcat 11.0.27 cannot be set to refuse it and a filter cannot see the raw line; no gate versus container disagreement was found (accepted by the Sponsor 2026-11-12; the PM amends the spec row). |
| Slow readers (SEC-F-194) | A non-reading or trickle reader is cut at about 5 s by Tomcat; eight reconnecting non-readers delay each request by 4.5 to 5 s; no response-write timeout (accepted by the Sponsor 2026-11-12 with R-24; mention it in the release notes). |
| R-24 | Slow-sending (drip) clients hold worker threads beyond the 5 s timer (accepted for Tasks 16 to 48; renewed by the Sponsor 2026-11-12 for the release). Figures of handoff 166: 8 body drips delay every request 2 to 8 s; 130 request-line drips deny the service while held. |
| R-13, R-23 family | Tomcat-first pages (SEC-F-151, SEC-F-128, SEC-F-172): HTML 400, 404 or 405 without the security headers, no product, version or echo; pinned by `PreApplicationIntegrationTest` (Accepted by Security under constitution 1.5, 1 of 5 this phase). |
| SEC-F-186 | A kill between the extraction of the driver's native library and its narrowing leaves a 0644 file inside the 0700 `tmp` until the next start. |
| SEC-F-184 b, c | The owner is compared by name, not by numeric uid; modes with set-uid or ACLs are invisible to the guard. |
| SEC-F-191, SEC-F-192 | `quick_check` loads an oversized cell into native memory (about 2.5 s for 400 MB, never on the Java heap); `application_id` and `user_version` are not checked; invalid UTF-8 in a stored title is served as U+FFFD. |
| SEC-F-181, SEC-F-188 | SIGKILL is not power loss; pragmas are read in the application's own `DataSource`, not inside the launched jar (that would need a test-only switch, A-09); reviews ran as root. |
| Untested | A journal that is a symlink, an empty main file plus a non-empty journal, a non-POSIX or lock-less file system. |

## 5. Optional hardening (no deadline)

SEC-F-152, SEC-F-154 (replace the regex matcher in `isWhiteSpace`), SEC-F-123 (log pattern strips only six characters), SEC-F-129 (URI-based decode in the static catalog), SEC-F-115 (negative control that the error valve is used), SEC-F-040 and SEC-F-041 (the remaining parts of hook and wording items), SEC-F-026 (the session's Git identity is the tool's; unreachable commits; `useConfigOnly` unset).

## 6. Debt for the PM (documents, no code)

| Item | Where |
|---|---|
| Reword criterion 1 of Task 34a-2 ("pragmas read back in the application's own `DataSource` with the real configuration"); list `NativeDirectoryModes`, `OwnerOnlyCheck` and `StoredLibraryHeader` in the file lists of 34a-2 and 34b | `tasks.md` |
| Reword criterion 3(iii) of Task 34b: the main file bytes are unchanged; SQLite may delete a garbage journal; record that a row breaking INV-1, INV-4 or any CHECK is reported as damaged because `quick_check` runs first; plan note on the stricter DDL equality and the shared-lock header step | `tasks.md`, `plan.md` |
| Reword T-17 and T-19 (the body `{}`) and add `{"rating":{}}` under T-18; T-31 "after the first request of the JVM" | `spec.md` |
| E-37 row 1 (`GET  /  HTTP/1.1`): amend to "the container may accept repeated SP or HTAB; normal answer" (R-26, S-9); T-33 "no application log line" (a malformed header gives one container line); the full `verify` does not fit 9 minutes (about 60 s more with Task 49+50) | `spec.md`, `plan.md` |
| List `BookRatingControllerTest` and the message constants in Task 31b; branch name of 41+42 | `tasks.md` |
| Amend E-10, E-26 row 5, a spec line for the 10 s total read deadline, accept duplicate `q` and quoted parameters (SEC-F-143), empty-state and h2 "Add a book" wording, Task 14 criterion 3 jar-command wording, body-before-route order for GET, file-list deviation of Task 16 | `spec.md`, `tasks.md` |
| Plan amendments: logging keys, `GlobalExceptionHandler` takes `RouteTable`, encoding key and `onRead`, the 6-minute full `verify` time | `plan.md` |
| Resolver, `StatusDeserializer`, projection and `discard` plan notes are DONE (plan.md of 2026-11-06) | |
| `HeaderPolicyTest` exact equality; `library.reset()` first use at the first DELETE task | tests |

## Closed since the ledger was created

Task 41+42 merged (pull request 45): the README memory bound sentence is fixed (Liaison F-1). Sponsor decisions S-6, S-8 and S-9 accepted on 2026-11-12 (moved to the accepted limits).

Handoff 166 (Task 49+50 Security review): closed SEC-F-151, SEC-F-128, SEC-F-172 (moved to accepted limits), SEC-F-153, SEC-F-105, SEC-F-166, SEC-F-167 and the Task 41+42 Security close-out; SEC-F-147 moved to S-8 (renewal); SEC-F-106 kept; SEC-F-148 corrected.
