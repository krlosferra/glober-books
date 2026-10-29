# Git Workflow

Binding for every contributor (human or automated). It refines the "Workflow Rules" of `constitution.md`.

## 1. No AI attribution (absolute rule)

Nothing in the repository's history or collaboration surface may reveal that an AI tool, assistant or agent was used. This applies to:
- commit messages (subject, body, trailers)
- commit author and committer identity (`user.name`, `user.email`)
- branch names, tag names and tag messages
- pull request titles, descriptions and comments
- code comments and generated file headers

Forbidden examples (non-exhaustive): `Co-Authored-By:` naming any AI tool; `Generated with ...`; names of AI products, models or vendors; session or chat URLs; `noreply@` addresses of AI vendors; phrases such as "AI-generated" or "AI-assisted".

Commits are authored by the human contributor's own configured identity. Configure it locally and verify before committing:

```bash
git config user.name  "<your name>"
git config user.email "<your email>"
git config core.hooksPath .githooks     # enables the commit-msg guard (once per clone)
```

Two guards live in `.githooks/` and are enabled by `core.hooksPath`:
- `commit-msg` rejects messages that match specific AI-attribution patterns (trailers, tool, model and vendor names, session links).
- `pre-push` rejects a push when any outgoing commit has an author or committer identity that matches those patterns, when the branch or tag name does, or when the configured identity is empty.

They are a safety net, not a substitute for the rule. Do not bypass them (`--no-verify` is forbidden). The orchestrator checks `git config core.hooksPath`, the executable bit of the hooks and the identity before dispatching any work.

If a violating commit is found in history, report it to the Sponsor; rewriting shared history requires the Sponsor's explicit approval.

## 2. Branching model: trunk-based with short-lived branches

- `main` is always releasable: builds, all tests green, coverage gate met, docs in sync.
- No direct commits to `main`, with one exception: the initial bootstrap of the repository.
- All work happens on a short-lived branch created from the latest `main`. The branch is pushed and integrated into `main` by a squash merge that the Sponsor approves: through a pull request when the Sponsor asks for one (section 4), otherwise by the orchestrator performing the same squash merge after the Sponsor's approval. Branches are deleted after merging.
- One branch per task (or per tightly related set of spec changes). Target lifetime: under one day.

### Branch names
`<type>/<ref>-<short-kebab-description>`

| Type | Use | Example |
|---|---|---|
| `feature` | New behavior from `tasks.md` | `feature/task-05-duplicate-detection` |
| `fix` | Defect against the spec | `fix/task-12-rating-null-on-regress` |
| `test` | Tests only | `test/task-09-rating-integration` |
| `refactor` | Behavior-preserving restructuring | `refactor/task-14-extract-error-mapper` |
| `spec` | Changes to `spec.md` / `tasks.md` / `plan.md` / `constitution.md` | `spec/us-005-author-bio` |
| `docs` | Documentation only | `docs/readme-api-section` |
| `chore` | Build, tooling, config | `chore/task-01-maven-scaffold` |
| `drift` | Phase 5 exercise only (throwaway) | `drift/author-bio-experiment` |

Rules: lowercase, hyphens, no spaces; `<ref>` is the task number (`task-NN`), the story (`us-NNN`) or omitted for docs/chore without a task.

### Parallel work (backend and frontend)
- Separate branches, separate file areas (`backend/` vs `frontend/`). They never edit the same files.
- The API contract in `spec.md` is the only coupling. If it must change, stop and raise a `spec` change first.
- Merge order is decided by the orchestrator by dependency; integrate by rebasing the later branch on `main`.

## 3. Commit message convention (Conventional Commits)

```
<type>(<scope>): <subject> [Task-N]

<body: what and why, wrapped at 72 columns>

Refs: US-00X, BR-00X
```

- **type:** `feat`, `fix`, `test`, `refactor`, `docs`, `spec`, `chore`, `build`, `revert`
- **scope (optional):** `api`, `service`, `repository`, `domain`, `error`, `frontend`, `db`, `build`, `sdd`
- **subject:** imperative mood, lowercase, no trailing period, at most 72 characters
- **`[Task-N]`:** the task from `tasks.md` the commit belongs to (omit only for non-task work)
- **body:** explains motivation and notable decisions, not a line-by-line diff. Mention deviations from the plan.
- **footer:** `Refs:` with story, rule or decision identifiers; `BREAKING CHANGE:` when the API contract changes incompatibly.

Examples:
```
feat(service): reject rating when book is not completed [Task-07]

Enforces BR-002: ratings are accepted only for completed books and
signal RATING_NOT_ALLOWED otherwise.

Refs: US-003, BR-002
```
```
spec(sdd): add author bio story and validation rule

Refs: US-005, BR-005, DEC-003
```

Hygiene:
- Atomic commits: one logical change, build and tests green at every commit.
- Never mix spec changes with code in the same commit. The spec change comes first.
- Update `progress.md` and any affected document in the same branch as the code.
- Never commit secrets, local database files, build output or IDE files.

Note on the original exercise: the exercise suggests `[Task-N] [Description] - ACCEPTED`. This repository keeps the task reference but follows Conventional Commits. The ACCEPTED status is recorded in `progress.md` and in the pull request, not in the commit message.

## 4. Pull requests and merging

1. Push the branch: `git push -u origin <branch>`.
2. A pull request is opened only when the Sponsor asks for it (or approves the orchestrator proposing it). Without a pull request the orchestrator squash-merges the approved branch, using the same message rules. Title follows the commit convention. The description lists: task, spec references, acceptance criteria status, test evidence (command and result), deviations. No AI attribution lines.
3. Merge requirements: build and tests green, coverage ≥ 80%, Liaison `ACCEPT`, Security verdict `PASS` (or `PASS WITH CONDITIONS` with every condition closed), PM business acceptance (feature work), Sponsor approval.
4. **Squash merge** into `main`, so history shows one commit per accepted task. The squashed message follows section 3.
5. Delete the branch after merging.

## 5. Tags
- `pre-drift-baseline` before the Phase 5 exercise (per the exercise instructions).
- `phase-N-complete` at the end of each approved phase.
- `v0.x.0` for releases.
Tag messages follow the same no-attribution rule.

## 6. Forbidden operations (without explicit Sponsor approval)
`git push --force` (and `--force-with-lease`) on any shared branch, and always on `main`; history rewriting on shared branches; `git reset --hard` outside a `drift/` branch; `--no-verify`; deleting tags or branches that are not yours.
