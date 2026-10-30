#!/usr/bin/env bash
# PreToolUse guard for Bash tool calls (see docs/sdd/security.md, SC-24, SC-25 and SEC-F-032).
# Blocks: hook bypasses, and AI attribution inside commit, tag and pull-request commands.
# Reads the tool call as JSON on stdin; exit code 2 blocks the call and shows stderr to the agent.
# Only the value of "command" is inspected (other fields such as file paths are not part of the command).
# It is a safety net, not a security boundary (R-09): server-side protection and the hooks are the anchors.
project_dir="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
block() { echo "guard-bash: blocked. $1 (see docs/sdd/git-workflow.md)" >&2; exit 2; }

pattern=$(cat "$project_dir/.githooks/attribution-pattern" 2>/dev/null)
[ -n "$pattern" ] || block "The attribution pattern file is missing or unreadable (fail closed)"

input=$(cat)
# Extract the command string; newlines become command separators; undo the most common JSON escapes.
cmd=$(printf '%s' "$input" | tr '\n' ' ' | grep -oE '"command" *: *"([^"\\]|\\.)*"' | head -1 \
  | sed -E 's/^"command" *: *"//; s/"$//' \
  | sed -e 's/\\n/ ; /g' -e 's/\\t/ /g' -e 's/\\"/"/g' | LC_ALL=C tr -d '\200-\377')

# A git or gh invocation starts at the beginning of the command, after a separator, a quote or a subshell,
# and may be preceded by VAR=value words.
start='(^|[;&|("'"'"'`]) *([A-Za-z_][A-Za-z0-9_]*=[^ ]* +)*'

# 1. Hook bypasses, anywhere inside a git command (this also covers bash -c, sh -c and eval).
allowed_stripped=$(printf '%s' "$cmd" | sed -E 's/git +config +core\.hooksPath +\.githooks( |$|;|&|")/ \1/g')
if printf '%s' "$allowed_stripped" | grep -qE 'git[^;&|]*(--no-verify|core\.hooksPath)|GIT_CONFIG_(COUNT|KEY|VALUE|PARAMETERS)'; then
  block "Bypassing git hooks is forbidden (--no-verify, core.hooksPath, GIT_CONFIG_*); the only allowed form is: git config core.hooksPath .githooks"
fi
if printf '%s' "$cmd" | grep -qE "${start}git( +[^ ;&|]+)* +commit( [^;&|]*)? -[a-zA-Z]*n[a-zA-Z]*( |\$|\")"; then
  block "git commit -n is --no-verify; bypassing hooks is forbidden"
fi

# 2. Attribution in commit, tag, notes and pull-request commands.
if printf '%s' "$cmd" | grep -qE "${start}(git( +[^ ;&|]+)* +(commit|tag|notes)( |\$|\")|gh +(pr|issue|api)( |\$|\"))"; then
  scrubbed=$(printf '%s' "$cmd" | sed -E 's#\.claude/##g; s#CLAUDE\.md##g')
  if printf '%s' "$scrubbed" | grep -qiE "$pattern"; then
    block "The command text reveals an AI tool, agent or co-author (commit, tag or pull-request text must not contain attribution)"
  fi
fi
exit 0
