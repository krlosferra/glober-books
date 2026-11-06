# Handoff 154: Audit, Task 34a-2 (wire the packaged application to the guard)
- From: liaison
- To: orchestrator
- Phase: 4   Round: 1
- Type: REVIEW_REPORT

Mode: F (with G for oracles). Artifacts reviewed: `git diff origin/main..HEAD` (6 commits), handoffs 152 and 153, `tasks.md` Task 34a-2 (v0.6.5), the new and edited production and test files.

## Verdict: ACCEPT WITH FOLLOW-UPS (no blocking finding)
Layer 1 compliance, Layer 1b hygiene, Layer 2 testing: pass. Layer 3: minor notes. The Security re-probe of the jar (SEC-F-178 closure) and the Sponsor gate remain. I do not replace the Security verdict.

## Evidence I produced myself
Focused re-run (offline, `./mvnw -o verify` with the three unit and three integration classes selected, jacoco skipped): `StartupRefusalIntegrationTest` 13/13 (48.5 s), `SqliteWiringIntegrationTest` 4/4, the unit classes green, BUILD SUCCESS. Console of each refusal shows the "APPLICATION FAILED TO START" block with the fixed Description and the one fixed Action. I did not re-run the full verify (6 min); the delivery's full-green claim is the implementer's evidence, not mine. Git: 6 commits, one human identity, no trailer, no vendor or tool name (grep over changed non-handoff files: only a pre-existing README line naming `CLAUDE.md`). `application.yml`, `pom.xml`, `backend/config/**` untouched. Largest file 283 lines. No `Thread.sleep` seen in the new tests (deadline polling).

## Criteria
| # | Result | Note |
|---|---|---|
| 1 wiring | Met | see (a). Pragma clause: met in process only, see (b) |
| 2 modes, umask 022 and 000, journal | Met | jar test asserts 0700/0700/0600 once the app answers, plus the native files; journal is observed at 0600 in the in-process held-write test; the jar watcher records modes but does not assert that a journal was seen (MINOR, F-3) |
| 3 refusals | Met | 0755 dir, 0644 file, link as dir, link as file, hard link, foreign uid (file and dir, real chown as root), file as dir, file as parent, missing parent, 300-byte name; each compares the tree snapshot before and after; non-zero exit |
| 4 reporting | Met | see (f) |
| 5 second instance | Met | see (h) |

## Key judgments
(a) Wiring: accepted. `SqliteDataSourceConfig` is the only `DataSource` bean; it does `check` then `create(Guarded)` (the scan test even asserts the literal chain); same `spring.datasource.url` property as README, `AppProcess` and the launcher; smoke E2E 3/3 reported. `create(Path)` and `create(String)` are package-private; the test requires exactly one public `create` taking `Guarded` and scans `src/main` for other construction (SEC-F-185 item 5 met; it is a text scan, so it is a tripwire, not proof against reflection, which is acceptable). `toAbsolutePath().normalize()`, no `toRealPath()`; NUL and foreign URL become `LOCATION_NOT_ABSOLUTE`. Relative `data/` is proved on the jar from a temporary working directory.

(b) Pragmas: criterion 1 as written ("read back from the running jar") is NOT literally met, and cannot be without a test-only switch, which the brief forbade (constraint 3, A-09). The implementer stopped at the limit and said so, which is the right behaviour. The in-process proof goes through the real configuration class and the application's own `DataSource` and shows the same single bean is in Hibernate's factory bean, so wiring plus values are honestly proved; what is not proved is that the jar's classpath start uses the same bean (the jar behaviour tests, modes and second-instance claim, cover that indirectly). Recommend: PM amends `tasks.md` criterion 1 to "Pragmas read back from the application's own data source in a Spring Boot test with the real configuration; the jar proves the wiring through modes, the claim and the refusals (A-09)", via the change-request route, rather than leaving the task formally unmet. Security to confirm it accepts this.

(c) `NativeDirectoryModes`: accept as a documented deviation, not in the file list. Grounding: SC-13 and SEC-F-181 ("every file under the data directory no wider than 0600") and Task 33's `assertPrivate`, which the wiring would otherwise break; the driver writes under the guarded `tmp` because the provider redirects the temp dir. Window: a kill between extraction and `narrow` leaves a 0644 file inside a 0700 directory until the next start, where the next start narrows it again; nobody else can traverse the directory, so the exposure is nil unless the owner or root, and the content is the public driver library. Acceptable. Two small gaps (F-2): `narrow` is not called on the failure path of `openPool` (second instance), so a refused second start can leave 0644 extracted files in `tmp` (the second-instance test reports the tree unchanged, because the first instance already extracted them); and the failures are swallowed silently by design. Neither raises the risk. Needs Security acknowledgement and a line in `tasks.md`/plan so the file list stays truthful.

(d) `OwnerOnlyCheck` and SEC-F-184 b: accept. Name comparison with an honest Javadoc limit is the stated fallback in the brief ("otherwise keep the name check and say so"). Residual: two accounts with the same name cannot occur on a normal system; root owning the files compares as another owner only by name. Record in security.md as a limit (Security's call). Moving the check out of the guard also keeps the guard under 300 lines. The `nlink` check is skipped silently where the attribute is absent (documented, platform limit).

(e) Changed expectations: all three are justified.
- `DataDirectoryGuardTest` (300-byte name): expected "data remains", now "root empty". This follows from SEC-F-184 a, which the brief itself ordered; the old assertion encoded the SEC-F-184 defect. The new one is the spec-side oracle ("nothing left behind"). Justified.
- `StartupPolicyIntegrationTest` fixture: 0700 directory because the wired guard refuses 0755 (SC-13). Fixture, not an oracle. Justified.
- `ClaimChildMain` moved to the `config.sqlite` test package so `create(Path)` stays package-private; brief constraint 1 allowed exactly this. The `DurabilityIntegrationTest` was not edited and passes on the wired jar, which is the useful regression.
No assertion was weakened.

(f) Reporting: accepted. The analyzer returns a fixed description and one fixed action with a null cause, and also covers the three `StartupPolicy` reasons. `ConsoleRules` asserts no temp path, no `library.db`, no `Caused by`, no `.java:`, no frame pattern, no marker. Spring's WARN about `entityManagerFactory` carries class and bean names and the fixed text only (delivery evidence; the "Caused by" and path assertions on the whole console file pass, which covers it). Acceptable under SC-11/SC-37 (no path, book data, stack). Notes: (F-1, MINOR) the book-data marker `ZQXDATA` is never planted in these refusal runs, so "no cell value" is asserted vacuously; the second-instance case could add a book with the marker and assert it is absent from the second console. (F-4, MINOR) all open or schema failures collapse into `DATABASE_UNAVAILABLE`, which hides a corrupt-file diagnosis from the operator; this is correct for leakage and is a usability note for Task 34b (stored-library validation).

(g) README: accurate. Claims modes 0700/0600, creation on first start, link/owner/wide-mode/two-link refusals, "pass the real path": all tested. One imprecision (F-5, MINOR): "without touching anything" is true for inspection refusals, but a `CANNOT_CREATE` removes what the guard itself created (consistent with L-1); and "(the parent of `data/` must exist)" is trivially true for the default relative case. Suggest rewording; not blocking. The README still names the default file `globerbooks.db` consistently with `application.yml`.

(h) Second instance: met. First instance answered an add (201), the second exits 1 with `DATABASE_UNAVAILABLE` text after the 5 s busy timeout, the first still answers with the identical list body and the tree snapshot is equal. Cleanup in `finally` via `kill`; no leftover processes reported.

(i) Test time: acceptable for the gate. `StartupRefusalIntegrationTest` ~44 to 49 s (13 jar starts), `JarDataLocationIntegrationTest` ~19 s, `DurabilityIntegrationTest` 169 s is Task 33's, total ~6 min. The gate is a verify, not a pre-commit loop. Record the 6 min in the plan so it is not a surprise; consider tagging the jar-level classes for a faster dev loop (follow-up, no change now).

(j) File sizes, `application.yml`/`pom.xml`/`backend/config` unchanged: pass.
(k) Attribution: pass.

## Findings
| ID | Severity | Location | Finding | Resolution |
|---|---|---|---|---|
| F-0 | MAJOR (wording) | tasks.md 34a-2 criterion 1 | "from the running jar" cannot be met without a test switch; delivered in process | PM change request to reword (see (b)); Security to accept |
| F-1 | MINOR | `ConsoleRules` usage | marker never planted, so "no cell value" is vacuous | Plant a marker book in the second-instance test; can ride on Task 34b |
| F-2 | MINOR | `SqliteDataSourceProvider.create(Guarded)` | `narrow` not called when `openPool` fails | Call it in a `finally`/on the failure path, or accept and record |
| F-3 | MINOR | `JarDataLocationIntegrationTest` | watcher does not require that a journal was seen | Keep (flaky otherwise); the in-process held-write test is the deterministic proof |
| F-4 | MINOR | provider | all failures map to one reason | Revisit in 34b |
| F-5 | MINOR | README Run | "without touching anything" and the parent sentence | Reword |
| F-6 | MINOR | plan/tasks | `NativeDirectoryModes` and `OwnerOnlyCheck` are outside the file list | PM adds them to the Task 34a-2 production list (traceability) |

## Follow-ups
1. Security: re-probe the jar (umask 022 modes, missing directory, second instance, link and wide-mode refusals), confirm acceptance of the in-process pragma proof, the `NativeDirectoryModes` window and the name-only owner check; close SEC-F-178, SEC-F-181, SEC-F-182, SEC-F-184 if satisfied.
2. PM: items F-0 and F-6 as a change request; keep the Task 34b list for F-1, F-4.
3. Implementer (optional, same PR if cheap): F-2, F-5.
4. Orchestrator: update `progress.md` audit note and `status.md` audit column on Sponsor acceptance; commit `[Task-34a-2] ... - ACCEPTED`; open the pull request only after the Sponsor approves.

Next step: Security verdict, then the Sponsor gate for Task 34a-2 (protected surface, reviewed alone).
