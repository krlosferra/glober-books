#!/usr/bin/env bash
# Read-only repository hygiene check run by CI on pull requests (docs/sdd/security.md, SC-15, SC-25, SEC-F-033).
# Fails when commits, the branch name or the pull-request text reveal an AI tool, when a secret pattern is found,
# or when the commits cannot be enumerated (fail closed). Prints a NOTICE listing protected-path changes.
# Inputs (environment): BASE_SHA, HEAD_SHA, PR_TITLE, PR_BODY, HEAD_REF. Never prints secret values.
set -u
fail=0
pattern=$(cat .githooks/attribution-pattern 2>/dev/null)
if [ -z "$pattern" ]; then echo "hygiene: attribution pattern file is missing or unreadable" >&2; exit 1; fi
scrub() { LC_ALL=C tr -d '\200-\377' | sed -E 's#\.claude/##g; s#CLAUDE\.md##g'; }
check_text() { # label, text
  if printf '%s\n' "$2" | scrub | grep -qiE "$pattern"; then
    echo "hygiene: $1 reveals an AI tool" >&2
    fail=1
  fi
}
check_text "pull request title" "${PR_TITLE:-}"
check_text "pull request body" "${PR_BODY:-}"
check_text "branch name" "${HEAD_REF:-}"

if [ -z "${BASE_SHA:-}" ] || [ -z "${HEAD_SHA:-}" ]; then
  echo "hygiene: BASE_SHA and HEAD_SHA are required (fail closed)" >&2
  fail=1
else
  if commits=$(git rev-list "$BASE_SHA..$HEAD_SHA"); then
    for sha in $commits; do
      if git show -s --format='%an <%ae>%n%cn <%ce>%n%B' "$sha" | scrub | grep -qiE "$pattern"; then
        echo "hygiene: commit $sha reveals an AI tool (author, committer or message)" >&2
        fail=1
      fi
    done
  else
    echo "hygiene: cannot enumerate commits $BASE_SHA..$HEAD_SHA (fail closed)" >&2
    fail=1
  fi
  protected=$(git diff --name-only "$BASE_SHA..$HEAD_SHA" 2>/dev/null | grep -E '^(\.githooks/|\.claude/|\.github/|CLAUDE\.md$|\.gitignore$|\.gitattributes$|\.nvmrc$|docs/sdd/(constitution|security|git-workflow|agent-protocol|orchestration|frontend-testing)\.md$|backend/(pom\.xml|\.mvn/|config/)|e2e/(package\.json|package-lock\.json|\.npmrc))' || true)
  if [ -n "$protected" ]; then
    echo "NOTICE: protected paths changed in this pull request (the Sponsor reads these before merging):"
    printf '%s\n' "$protected" | sed 's/^/  - /'
  fi
fi

secrets='BEGIN (RSA|EC|OPENSSH|PRIVATE) KEY|AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{20,}|xox[baprs]-[A-Za-z0-9-]{10,}|_authToken|(password|secret|api[_-]?key|token)[[:space:]]*[:=][[:space:]]*["'"'"']?[A-Za-z0-9/+_=-]{16,}'
while IFS= read -r file; do
  case "$file" in
    docs/sdd/security.md|.github/scripts/hygiene.sh|docs/sdd/handoffs/*-security-to-*) continue ;;
  esac
  if [ -f "$file" ] && grep -IiEn "$secrets" "$file" >/dev/null 2>&1; then
    lines=$(grep -IiEn "$secrets" "$file" | cut -d: -f1 | tr '\n' ' ')
    echo "hygiene: possible secret pattern in $file (lines: $lines)" >&2
    fail=1
  fi
done < <(git ls-files)
exit $fail
