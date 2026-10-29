# GloberBooks: Delivery Approach (Senior Java Developer / AI Architect view)

> Companion to the SDD workflow document. It describes how the project is organized and run. It contains no implementation code.

## 1. Guiding principle
Phases 1–3 (about 4.5 hours) are the real work. Code is the cheap, replaceable output. No implementation starts until `constitution.md`, `spec.md`, `plan.md` and `tasks.md` have been reviewed and approved by the human. The human's role throughout is architect and auditor, not typist.

## 2. Stack choices

| Concern | Choice | Why |
|---|---|---|
| Language / runtime | Java 21 (LTS) | Records, modern switch, long-term support |
| Framework | Spring Boot 3.x | Matches the template's "Java + Spring Boot" option |
| Build | Maven (with wrapper) | Familiar; dependency changes are easy to audit in `pom.xml` |
| Persistence | SQLite via Spring Data JPA / Hibernate | Constitution template mandates SQLite + simple ORM |
| Tests | JUnit 5, Mockito, AssertJ, JaCoCo | JaCoCo provides a hard 80% coverage gate |
| Frontend | Plain HTML + JS served by Spring Boot | One deployable, no Node toolchain, fewer dependencies to approve |
| Validation | Jakarta Bean Validation | Covers "validate all inputs" |

## 3. Adaptations made to the constitution template
The original template is language-neutral. These are the deliberate changes made in the Java draft:

| Template item | Adaptation | Reason |
|---|---|---|
| Backend/Frontend placeholders | Java 21 + Spring Boot 3.x; plain HTML/JS | Concrete choices make rules checkable |
| "Use async/await (not callbacks)" | Replaced with "synchronous, blocking business logic; no reactive/ad-hoc threading without approval" | async/await does not exist in Java |
| Test naming `[filename].test.[ext]` | `[ClassName]Test` / `[ClassName]IntegrationTest` | Java/JUnit convention |
| "Never use SELECT *" | Scoped to hand-written queries | ORMs generate SQL; the rule must be enforceable |
| "Min 3-character names" | Exception for `id` | Avoids a rule that the data model itself violates |
| Status codes list | Added `409` | `spec.md` already uses 409 for duplicates |
| "Mock external dependencies (DB, APIs)" | Unit tests mock; integration tests use real SQLite | Resolves conflict with integration testing |
| Naming (camelCase) vs spec JSON (`date_added`) | Java camelCase; JSON snake_case via Jackson | Matches spec contract |
| (missing) | Approved dependency list, Security Standards, Configuration and Secrets, Workflow Rules | Fills gaps the Phase 1 validation prompt would flag |

## 4. Decisions to settle early (log them in `spec.md` Decision Log)
The source spec has gaps. Expect the Clarify Loop to surface these:

1. **Storage:** BRD says in-memory; constitution says SQLite. Decision: file-based SQLite (also satisfies "zero data loss on restart").
2. **Duplicate override:** spec returns `409` but defines no way to override. Add an explicit confirm flag on POST.
3. **Status update endpoint:** FR2 exists but no endpoint does. Add one.
4. **Rating when status regresses** (completed → reading): clear the rating or block the transition.
5. **Author bio (Phase 5):** "shared across books by the same author" implies an author concept, which is an ASK FIRST item (new entity/table).
6. **JSON naming:** snake_case (resolved in the constitution draft).
7. **Missing CRUD pieces:** get-by-id and delete are implied by "CRUD" but not specified. Include or mark out of scope.

## 5. Repository organization

```
globerbooks/
├── docs/sdd/
│   ├── constitution.md
│   ├── approach.md
│   ├── spec.md
│   ├── plan.md
│   ├── tasks.md
│   ├── progress.md
│   └── drift-exercise.md
├── backend/        # Maven project
│   └── src/main/java/com/globant/globerbooks/
│       ├── api/         # controllers, DTOs
│       ├── service/     # business rules
│       ├── repository/  # data access
│       ├── domain/      # Book entity, enums
│       └── error/       # error envelope, global handler
├── frontend/           # static assets (or under backend resources)
└── README.md
```

- One git repo; work on branches, `main` kept clean.
- One commit per accepted task: `[Task-N] Description - ACCEPTED`.
- Phase 5 drift work happens on a throwaway branch, with the `pre-drift-baseline` tag on `main`, so `git reset --hard` never touches history that matters.

## 6. Phase-by-phase plan

| Phase | What I do | Exit gate |
|---|---|---|
| **1. Constitution** | Adapt template to Java, run the validation prompt, resolve conflicts | Agent can summarize the constitution back; zero ambiguous rules |
| **2. Spec** | Fill template, run Clarify Loop until no substantive questions remain, run Final Validation | 4+ stories, 3+ Given-When-Then rules, API examples, 2+ decisions logged |
| **3. Plan & tasks** | Generate `plan.md` and `tasks.md`, review against the PDF checklists myself | All checklist items pass; I can explain the architecture in 5 minutes |
| **4. Execution** | Execute → 3-layer audit → accept/reject → update `progress.md` → commit | All tests green, coverage ≥ 80%, avg ≤ 2 attempts/task |
| **5. Drift** | Tag baseline, inject drift, detect, roll back, fix docs first, re-implement | Drift detected < 5 min, clean rollback, original tests pass |
| **6. Negative experience** | Build from same spec with no methodology; measure the same metrics | Comparison written up with real numbers |

### Expected task shape (≈12–14 tasks, each < 5 files, 15–45 min)
- **Setup:** project scaffold + build + JaCoCo gate; SQLite config + schema; error envelope + global handler.
- **Core features (vertical slices):** Add Book (validation + duplicate detection); List Books (filter + sort); status update; rating with its rules.
- **Frontend & polish:** static UI (list, add, duplicate dialog, rate); README + API docs; coverage and cleanup.

### Phase 4 audit discipline
- Read the diffs; do not rely on the agent's summary.
- Run `mvn test` myself and inspect the JaCoCo report.
- Check that tests assert behavior, not just coverage.
- Run both integration checkpoints (after Task 3 and after Add Book).
- Keep the rejection log honest.

## 7. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Agent scope creep | Audit item "only files listed in the task modified"; constitution NEVER rule |
| ORM auto-behavior vs. schema / `SELECT *` rules | Constitution scopes the rule to hand-written queries; schema changes are ASK FIRST |
| Coverage gaming | Review assertions, not only percentages; each BR needs pass and fail tests |
| Doc/code desync | Update docs in the same commit as the affecting code |
| Timeline overrun | Phase times sum to ~9.5 h vs. the promised 6–8 h; confirm whether Phase 6 is optional |
| Unapproved tooling | Confirm the AI agent/plugin situation with the trainer or IT before starting |

## 8. Immediate next steps
1. Run the Phase 1 validation prompt against `constitution.md` and iterate.
2. `git init` and commit the constitution as the first commit.
3. Start `spec.md` from the template, pre-loading the decisions in §4 as candidates for the Clarify Loop.
