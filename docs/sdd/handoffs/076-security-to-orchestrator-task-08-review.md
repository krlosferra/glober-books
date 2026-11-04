# Handoff 076: Security review of Task 08
- From: security
- To: orchestrator
- Phase: 4   Round: 1
- Type: AUDIT_REPORT

## 1. Scope and method
Branch `chore/task-08-real-port-baseline` against `main`: handoff 074, `application.yml` diff, progress.md 0a note, `RawHttpClient`, `TransportSecurityIntegrationTest`, `MandatoryPropertiesIntegrationTest`. Read-only: file reads, grep, a byte check of the `%replace` regex. I did not rerun the build or start the application (no raw-socket probe on 18081); the evidence is the delivery's printed test output plus the test source.

## 2. Findings
| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-123 | LOW | Logging | `application.yml` `logging.pattern.console` | The class strips only CR, LF, U+2028, U+2029, U+0085, ESC, exactly the constitution list. Other C0/C1 controls (BS, VT, FF, DEL, U+009B CSI) and TAB pass through; `%thread` and `%logger` are not wrapped. | Byte check: class holds `\r`, `\n`, literal U+2028, U+2029, `\u0085`, `\u001B`. Proof output keeps the tab before `at`. | Only if user text is logged, which the constitution forbids beyond ids. Terminal tricks via U+009B are possible in theory. | Optional: widen to `[\p{Cntrl}  \u0085]` (keep TAB if wanted) in a later logging task. Not a blocker. | SC-37 |
| SEC-F-124 | INFO | Transport | `methodFooWithoutContentTypeIsRefusedByTheGateWith415` | `FOO` without content type gets `415 UNSUPPORTED_MEDIA_TYPE` from the gate, not 405. Safe, application envelope, next request served. | Delivery finding 4. | None. | Accept. | SC-10 |
| SEC-F-125 | INFO | Test evidence | Delivery table | The hash suffix `0` after chunked lengths in the evidence lines is a raw chunk framing artifact, not a body leak. | Evidence lines for 404/405/500. | None. | None. | none |

## 3. Controls checked
- SC-10: VERIFIED. `assertEnvelope` runs on 200, 201, 400 Host, 400 malformed, 403, 404, 405, 413, 415, 500 (real port, raw socket). It asserts 8 headers with exact values, and no `Server`, `Set-Cookie`, `Location` or `access-control-*`. Container 400s lack the envelope by design (R-23).
- SC-28: VERIFIED. Host allowlist rows on GET and POST `/api/v1/books` (evil, bare `localhost`, wrong port, `[::1]`), accepted `LOCALHOST:port`, no Host, and two Host headers; Origin, `null` and Sec-Fetch rows 403.
- SC-30 (real-port class): VERIFIED. Raw sockets cover body cap at 16384 and 16385 (Content-Length, chunked, unknown path), 415 variants and header-size limits. Each is followed by a "next request served" check.
- SC-37: VERIFIED with SEC-F-123. The pattern is the single `logging.pattern.console` (YAML). The regex holds the literal U+2028/U+2029 and escapes for the rest. `%replace(%m%wEx)` wraps both message and exception, so stack traces become one line. The proof test logs CR/LF/U+0085/ESC in the message and the cause, and sends U+2028 in the URL. It asserts none survives and that the forged line stays on one line. No `logback*.xml`, no log4j, no file appender exists in the repo. Only the default console appender is present. Checkpoint log: 47 lines, no book data.
- SEC-F-066: CLOSED. Container pages carry no product, version, "Type Exception Report" or description text (`assertNoDisclosure`, with `Server:` and "exception" excluded).
- SEC-F-112/R-23: CONFIRMED as accepted. The container 400 pages (invalid line, header over 8 KiB, request line over 8 KiB, two Host headers, E-10 comma Host) are full HTML with a style block, no envelope headers, and nothing disclosed. No new risk.
- SEC-F-121: CLOSED. Forged Host, Origin, Sec-Fetch and Content-Type rows hit `/api/v1/books` with GET and POST.
- SEC-F-077: CLOSED. The 0a note says v0.1 is a checkpoint, not for real data, and that `backend/data/globerbooks.db` is deleted before Increment 1. `data/globerbooks.db` is git-ignored.
- Mandatory properties: all 20 proven effective (15 tests). Values match the constitution: loopback bind, forward-headers NONE, 8 KB header, access log off, whitelabel off and `/error` 404, pool 1, 5 s timeouts, unknown fields fail, snake_case, validate, open-in-view off, no SQL logging, hidden-method/form/multipart off, static `classpath:/static/`, and levels INFO. None missing, none wrong. The logging addition is authorised, lowers no limit, and the added keys match the constitution Logging text.
- 0a note commands: VERIFIED. They are loopback only (`127.0.0.1:8080`), `curl` at 127.0.0.1, no exposing flag or `0.0.0.0`, and the cleanup step is present. Minor: the note says the database "is locked", which is fine and honest.

## 4. Not verified
Build and tests were not rerun by me. No live probe on 18081. Public advisory databases were not queried (no dependency change in this task).

## 5. Residual risks
R-23 stays accepted. SEC-F-123 is optional hardening; I recommend a later logging task.

## 6. Verdict
PASS. No open CRITICAL, HIGH or MEDIUM. Next step: Sponsor gate, then the pull request.

## Appendix: exact text for security.md (not edited by me)
- SC-10, SC-28, SC-30 (real-port class), SC-37: status VERIFIED 2026-11-04, Task 08 (handoff 076).
- SEC-F-077 and SEC-F-121: CLOSED 2026-11-04 (076).
- SEC-F-123 | LOW | Log pattern strips only the six constitution characters; TAB, BS, VT, FF, DEL and U+009B pass; `%thread`/`%logger` not wrapped | OPEN, optional hardening | 076
- SEC-F-124 | INFO | `FOO` without content type answered 415 by the gate | ACCEPTED | 076
- SEC-F-125 | INFO | Evidence artifact: chunk terminator `0` after bodies is framing, not leakage | CLOSED | 076
- Changelog: 0.30 (2026-11-04): Task 08 review (handoff 076): SEC-F-123 to SEC-F-125 added; SEC-F-077 and SEC-F-121 closed; SC-37 verified. Verdict PASS.
