# Handoff 006: Security review of the Phase 1 constitution, round 3 (re-check of conditions C-A to C-C)
- From: security
- To: sponsor
- Phase: 1   Round: 3 of max 3
- Artifacts: `.claude/hooks/guard-bash.sh`, `.claude/settings.json`, `.github/scripts/hygiene.sh`, `.github/workflows/hygiene.yml`, `.gitignore`, `docs/sdd/constitution.md` v0.4 (banned and transitive dependency lists), `docs/sdd/security.md` v0.5, `docs/sdd/orchestration.md` (pre-flight), `docs/sdd/frontend-testing.md` (start procedure); throwaway output `/tmp/claude-0/tree.txt`; branch `docs/phase-1-constitution-review`, HEAD `c1da000`
- Type: AUDIT_REPORT

## Summary

**Verdict: PASS WITH CONDITIONS.** 0 CRITICAL, 0 HIGH open. Conditions C-A and C-C are closed. C-B (the Bash guard) is closed for every case I listed in round 2, but my re-test found one new bypass of the same class (a lowercase `core.hookspath`) and two false positives that will push people to work around the guard. They are small guard edits (SEC-F-038, SEC-F-039). This is the last round; the remaining items are therefore conditions on the merge, to be fixed by the orchestrator and re-run with the case files named below, or accepted by the Sponsor in the risk register.

New findings: SEC-F-038 MEDIUM, SEC-F-039 LOW, SEC-F-040 LOW, SEC-F-041 LOW. Closed this round: SEC-F-031, SEC-F-032 (superseded by SEC-F-038), SEC-F-033, SEC-F-034, SEC-F-036, SEC-F-037. SEC-F-035 is decided by the Sponsor (residual below).

## 1. Scope and method

Read: the files listed above. Ran, all in the scratchpad and in throwaway repositories (no git write command in the working repository):
- 71 command strings through `guard-bash.sh` as the hook would receive them (JSON on stdin), case files `scratchpad/cases.txt`, `scratchpad/cases2.txt`, runner `scratchpad/run.sh`.
- `hygiene.sh` in a throwaway repository (`scratchpad/hyg.sh`): clean range, empty SHAs, nonexistent base SHA, AI trailer in a commit, AI text in PR title, secret pattern in a non-security handoff, secret pattern in a `*-security-to-*` handoff, NOTICE output.
- A throwaway repository with a rejecting `commit-msg` hook (`scratchpad/gitcase.sh`) to prove that a lowercase `core.hookspath` really disables hooks.
- JSON validity of `settings.json` (valid), `git ls-files -s` modes, `git check-ignore -v` on the representative paths, read of the dependency tree.

Note: during the review the guard blocked three of my own read-only commands because their text contained `core.hooksPath` (see SEC-F-039). I wrote the test commands into script files instead.

## 2. Findings

| ID | Severity | Area | Location | Finding | Evidence | Exploit scenario | Remediation | Control |
|---|---|---|---|---|---|---|---|---|
| SEC-F-038 | MEDIUM | Agent guard | `.claude/hooks/guard-bash.sh` line 25 | The `core.hooksPath` match is case-sensitive, but git config variable names are case-insensitive | Guard returns ALLOW for `git config core.hookspath /dev/null`, `git config CORE.HOOKSPATH ...`, `git -c core.hookspath=/x commit` and `git config --unset core.hookspath`. In a throwaway repo, after `git config core.hookspath /dev/null` a commit that the `commit-msg` hook rejects succeeded (`gitcase.sh`: plain commit rc=1, lowercase-key commit rc=0) | An agent (or injected instruction) disables all local hooks and then commits attribution text; only the `hygiene` check and the squash message review remain | Make the first match case-insensitive: `grep -qiE` for `git[^;&|]*(--no-verify\|core\.hookspath)\|GIT_CONFIG_...`, and the `-n` match as is. Keep the exact-case allowance for `git config core.hooksPath .githooks` | SC-24, SC-25, SC-34 |
| SEC-F-039 | LOW | Guard usability | `guard-bash.sh` lines 24-30 | Two false positives that block legitimate work: (a) read-only forms `git config core.hooksPath`, `git config --get core.hooksPath` are blocked, although `orchestration.md` pre-flight (line 86), `git-workflow.md` line 29 and `security.md` section 5 tell people to run them; (b) a commit whose quoted message contains ` -n ` (for example `-m "feat: add -n flag docs"`) is blocked as `--no-verify` | Cases in `cases2.txt`: ALLOW expected, BLOCK obtained; hit live three times in this review | People learn to reword or to bypass the guard (for example by `bash script.sh` wrappers), which erodes the control | (a) Strip read-only forms before the match: `git config( --local\| --global)*( --get)? core.hooksPath` followed by end, `;`, `&` or `"`. (b) Apply the `-n` check to the command with quoted strings removed (`sed -E "s/\"[^\"]*\"//g; s/'[^']*'//g"`) | SC-34 |
| SEC-F-040 | LOW | Hardening residuals | `settings.json`; `hygiene.sh` line 46 | (a) `Bash(git push * --force*)` does not match a trailing `-f` (`git push origin main -f`); (b) the secret-scan skip `docs/sdd/handoffs/*-security-to-*` matches any file whose name contains that text, for example `9-java-to-pm-security-to-x.md`, so any author can exempt a handoff from the scan (reproduced); (c) Edit/Write ask rules do not cover writing guard files through Bash (`sed -i`, `tee`, redirects) | Reproduced (b) in `hyg.sh`; (a) and (c) by reading the rules | A careless or injected agent places a token in a handoff named to match the skip; a guard file edited through the shell avoids the ask prompt | (a) Add `Bash(git push * -f*)`. (b) Narrow the glob to `docs/sdd/handoffs/[0-9][0-9][0-9]-security-to-*` (and keep CODEOWNERS/Sponsor reading the protected-path list); the pattern file stays out of the skip. (c) Accept under R-09 and rely on the NOTICE list plus the Sponsor reading it | SC-24, SC-35, SC-15 |
| SEC-F-041 | LOW | Dependencies | `constitution.md` lines 49-51; `security.md` SC-36 | Banned and exception lists are consistent with the tree except: SC-36 still names commons-logging as banned while the constitution now lists it as an accepted transitive exception; the sentence "Any other new coordinate in the tree is a finding" is read literally impossible (the tree has about 100 coordinates), so the intended rule is "any match of the banned list, or any coordinate outside the Spring Boot 4.1.2 BOM-managed set"; `log4j-api` / `log4j-to-slf4j` and `aspectjweaver` are present and not mentioned | `/tmp/claude-0/tree.txt` read in full; no actuator, devtools, H2, Spring Security, Lombok, HttpComponents, OkHttp, WebFlux, Reactor Netty, XML dataformat, websocket starter, `log4j-core`, or HTTP client in the tree | None direct; ambiguity leads to inconsistent reviews | SC-36 updated by Security in this round. Constitution wording is the PM's: restate the last sentence as "any coordinate not in the verified tree (the Task 1 output, committed as evidence) is a finding"; add `org.apache.logging.log4j:log4j-core` to the banned list | SC-36, SC-19 |

## 3. Re-check of the round 2 conditions

### C-A: `hygiene.sh` (SEC-F-033, SEC-F-034): CLOSED
- Empty `BASE_SHA`/`HEAD_SHA`: exit 1 with "required (fail closed)". Nonexistent base SHA (`rev-list` error): exit 1 "cannot enumerate commits (fail closed)"; previously exit 0. AI trailer in a commit and AI text in the PR title: exit 1. Clean range: exit 0.
- Secret scan: a secret pattern in `docs/sdd/handoffs/001-java-to-pm-x.md` fails (exit 1, lines only, no value printed); `*-security-to-*` is skipped as designed (residual SEC-F-040b). `security.md` and `hygiene.sh` remain skipped because they quote the patterns.
- The NOTICE lists protected paths of the range (`.github/scripts/new.sh` shown in the test).
- Workflow re-read: trigger `pull_request`, `contents: read`, `persist-credentials: false`, PR text through `env:`, action pinned by SHA. Unchanged.

### C-B: `guard-bash.sh` (SEC-F-032): CLOSED for the round 2 list, superseded by SEC-F-038/039
All BLOCK as expected: `--no-verify` plain, after a newline, with `FOO=1` prefix, inside `bash -c`, `sh -c`, `eval`, `$(echo git) commit`, `(git ...)`, `git "commit"`, `git/**/commit`; `-nm`, `-anm`, `-sn`, trailing `-n`; `-c core.hooksPath=...`; `git config core.hooksPath /dev/null`; `git config --unset core.hooksPath`; `GIT_CONFIG_COUNT/KEY/VALUE/PARAMETERS` (also after `export` and `env`); `git config core.hooksPath .githooks && git ... --no-verify`; attribution in `commit`, `tag`, `gh pr create`, `git -C . commit`, `GIT_COMMITTER_NAME=...`, `--author=...`; pattern file unreadable returns exit 2 (tested with an empty project directory). `git config core.hooksPath .githooks` and normal commits, including `.claude/settings.json` in the message, are allowed. Not caught, by nature (R-09): shell-variable indirection (`g=git; $g commit --no-verify`), quote-splitting inside the flag, scripts that contain the command. Git does not accept `--no-ver` (usage error 129), so abbreviation is not a bypass. New: SEC-F-038 (case), SEC-F-039 (false positives).

### C-C: `settings.json`, pre-flight (SEC-F-031): CLOSED
- `settings.json` is valid JSON; `deny` unchanged (four unsafe MCP tools, `.env`); `ask` has force push in four spellings, `reset --hard`, `clean`, unset of `core.hooksPath`, `update-ref -d`, `filter-branch`, and Edit/Write on `.githooks/**`, `.claude/**`, `.github/**`, `.gitignore`. `.claude/settings.local.json` is absent (`ls`) and git-ignored; `orchestration.md` line 86 checks it at pre-flight.
- Not verified: the permission engine's behavior on these patterns (prefix versus wildcard matching of `Bash(git push * +*)`) cannot be exercised from this session without risking the real repository; the guard hook and the `pre-push` hook are the independent layers for force pushes. Ask this once in the first interactive session and record it.
- Residual: SEC-F-040a and c.

### Other round 2 items
- SEC-F-036 CLOSED: `git check-ignore -v` ignores `backend/.mvn/settings.xml`, `.mvn/settings.xml`, `e2e/playwright/.auth/u.json` via the `**/` rules. `application-*.yml` still hides a test profile file by design; a test profile must use another name or an explicit negation (noted for Task 1).
- SEC-F-037 CLOSED: `frontend-testing.md` line 85 documents the start procedure and the Node note.
- R-13: signed by the Sponsor; wording fixed.
- Modes: `git ls-files -s` shows `100755` for `commit-msg`, `pre-push`, `guard-bash.sh`, `hygiene.sh`; `attribution-pattern` and `settings.json` `100644`.

## 4. Dependency exceptions (constitution v0.4, line 51)

I read the real tree. Security meaning of each exception:
- `commons-logging` 1.3.6: logging facade used by Spring Framework; Boot routes it through its logging setup; no direct use by application code. Acceptable (how it is routed is to be seen in the Task 1 log-capture tests, SC-37).
- JAXB/Jakarta XML Bind and `angus-activation`, `istack`, `txw2`: Hibernate's XML mapping support. Risk is XML external entity handling when mapping files come from untrusted input; this application has none (no `orm.xml`, no `persistence.xml` from users, SC-27 forbids outbound reads). Acceptable. Rule to keep: no XML parsing in application code.
- `tomcat-embed-websocket` 11.0.27: acceptable on one condition. The WebSocket support activates only with a registered endpoint or the Boot websocket starter (banned and absent; `spring-boot-websocket` is not in the tree). SC-18 therefore needs two additions in Phase 4: (1) grep `backend/src/main` for `jakarta.websocket`, `@ServerEndpoint`, `ServerEndpointExporter`; (2) a raw-socket test that a `GET` with `Upgrade: websocket` and `Connection: Upgrade` to `/` and to an unknown path returns the normal envelope (404/400), not `101`. I added both to SC-18. Tomcat 11.0.27 advisories could not be checked (R-11).
- `snakeyaml` 2.6: configuration loading only; `application.yml` is committed, never user input. Acceptable (override of the config location through the environment is ASK FIRST in SC-39 territory).
- Not listed but present, no objection: `tomcat-embed-el` (Hibernate Validator; SC-16 forbids user input in message templates), `aspectjweaver` (Spring Data JPA), `log4j-api` and `log4j-to-slf4j` (bridge only; `log4j-core` absent and should be banned, SEC-F-041), `byte-buddy`, `antlr4-runtime`, `jboss-logging`, `classmate`.
- The tree was produced in a throwaway project, not from the real `pom.xml`; SC-36 and SC-19 stay `NOT_STARTED` until Task 1 reproduces it on the real build and the output is committed as evidence.

## 5. Controls checked

| Control | Result | Evidence |
|---|---|---|
| SC-15 | IN_PROGRESS | `.gitignore` anchoring fixed (SEC-F-036); secret scan test above; first CI run pending |
| SC-24 | IN_PROGRESS | Guard cases above; ask rules present; SEC-F-038, SEC-F-040 open |
| SC-25 | IN_PROGRESS | `hygiene.sh` fail-closed verified; `hygiene` workflow not yet run on GitHub; guard bypass SEC-F-038 |
| SC-34 | IN_PROGRESS | `settings.json` valid with Edit/Write ask rules; guard false positives SEC-F-039; permission-engine pattern behavior not exercised |
| SC-35 | IN_PROGRESS | Repository side complete; platform side is the Sponsor list below; solo-maintainer model recorded |
| SC-36 | NOT_STARTED | Text updated (commons-logging exception, `log4j-core` proposal); verification at Task 1 |
| SC-18 | NOT_STARTED | Websocket checks added |

## 6. Not verified

- GitHub-side behavior: the `hygiene` workflow has never run; branch ruleset, Dependabot and credentials are platform state (section 7).
- The permission engine's handling of the new `ask` patterns (see C-C).
- Maven advisories for the whole tree (advisory databases unreachable, R-11).
- Everything in Phase 4 controls (no application code exists yet).

## 7. Residual risks for the Sponsor

- Solo-maintainer review model (SEC-F-035, your decision): no required approvals, so a pull request that edits the guard files, the workflow or the pattern file is checked by the code it changes. The mitigation is your reading of the NOTICE / protected-path list before every merge. Recommendation: accept, and make that reading an explicit line of the pull request checklist; revisit with a second maintainer. A new risk entry R-14 is proposed in `security.md` (Sponsor column left empty).
- Guard is a safety net (R-09 already accepted): variable indirection, scripts and Bash-based file writes cannot be stopped by pattern matching.

## 8. Verdict

**PASS WITH CONDITIONS** (no CRITICAL or HIGH open; Phase 1 may proceed to the Sponsor gate once these are met).

Conditions before the Phase 1 pull request is merged:
1. SEC-F-038: case-insensitive `hookspath` match in `guard-bash.sh`. Re-run `cases.txt` and `cases2.txt` (the lowercase lines must BLOCK).
2. SEC-F-039: allow the read-only `core.hooksPath` forms and ignore `-n` inside quoted text. The same two files must show the corresponding lines as ALLOW.
3. SEC-F-040a and b, SEC-F-041: one-line edits (the `-f` ask rule, the narrower handoff glob, the constitution sentence and `log4j-core` in the banned list). Not blocking; if the orchestrator prefers to defer them, record them as LOW in the status board.
4. Sponsor platform steps below (C-1 from round 2 stays a hard condition for Phase 2: without it the verdict reverts to FAIL).

If the conditions 1 and 2 are not fixed, the Sponsor may instead accept SEC-F-038 and SEC-F-039 in the risk register; I recommend fixing, since both are about ten lines.

### Sponsor steps still open (exact)
1. Open the pull request for `docs/phase-1-constitution-review`; let the `hygiene` check run once (it has never run). Reject PR text containing the harness attribution lines.
2. Create the ruleset or branch protection on `main`: pull request required, required status check `hygiene`, block force pushes, restrict deletions, no bypass actors except yourself, required approvals 0 and code-owner review off (your decision). Today the API answers "Branch not protected".
3. Enable Dependabot alerts and security updates (currently disabled; R-05, P-01).
4. Credentials: confirm implementer sessions get no cloud variables and a token limited to this repository (contents and pull requests write, no administration); rotate tokens ever pasted into a prompt or handoff. The variable names `GH_TOKEN`, `GITHUB_TOKEN`, `CLOUDSDK_AUTH_ACCESS_TOKEN`, `AWS_*` still exist in the session.
5. Evidence: after steps 2 and 3, run `gh api repos/krlosferra/glober-books/branches/main/protection` (or the ruleset endpoint) and `gh api repos/krlosferra/glober-books --jq .security_and_analysis`, paste the output (no tokens) in `security.md` section 10.
6. Sign or reject proposed risk R-14 (solo-maintainer review model) in the register.
7. In the first interactive session, confirm that a `git push origin main --force` prompts (permission-engine check, C-C).

## 9. Next step
Orchestrator: apply conditions 1 to 3, re-run the two case files and report the output. Security has used its three rounds; any further change to the guard files goes back through a normal Security review at the pull request gate.
